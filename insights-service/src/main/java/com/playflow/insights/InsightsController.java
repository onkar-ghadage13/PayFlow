package com.playflow.insights;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.*;
import java.util.*;

@RestController
@RequestMapping("/api/insights")
public class InsightsController {
    private static final ZoneId BUSINESS_ZONE = ZoneId.of("Asia/Kolkata");
    private final JdbcTemplate jdbc;
    private final JwtService jwt;
    private final GeminiInsightsService gemini;

    public InsightsController(JdbcTemplate jdbc, JwtService jwt, GeminiInsightsService gemini) {
        this.jdbc = jdbc;
        this.jwt = jwt;
        this.gemini = gemini;
    }

    @GetMapping("/summary")
    public Map<String,Object> summary(@RequestHeader(value = "Authorization", required = false) String authorization,
                                     @RequestParam(defaultValue = "month") String period) {
        String selected = period.toLowerCase(Locale.ROOT);
        if (!Set.of("day", "month", "year").contains(selected)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Period must be day, month, or year");
        }
        Long userId = authenticatedUserId(authorization);
        LocalDate today = LocalDate.now(BUSINESS_ZONE);
        LocalDate dayStart = today;
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDate yearStart = today.withDayOfYear(1);

        Map<String,Object> limitsRow = jdbc.queryForMap(
                "SELECT daily_limit, monthly_limit FROM spending_limits WHERE user_id=?", userId);
        BigDecimal dailyLimit = (BigDecimal) limitsRow.get("daily_limit");
        BigDecimal monthlyLimit = (BigDecimal) limitsRow.get("monthly_limit");

        BigDecimal todaySpent = totalSpent(userId, dayStart, dayStart.plusDays(1));
        BigDecimal monthSpent = totalSpent(userId, monthStart, monthStart.plusMonths(1));
        BigDecimal todayReceived = totalReceived(userId, dayStart, dayStart.plusDays(1));
        BigDecimal monthReceived = totalReceived(userId, monthStart, monthStart.plusMonths(1));

        LocalDate from = start(selected, today);
        LocalDate to = nextStart(selected, from);
        List<Map<String,Object>> categoryRows = jdbc.queryForList(
                "SELECT category, SUM(amount) AS amount FROM payments WHERE sender_id=? AND status='SUCCESS' AND category<>'Wallet' AND created_at>=? AND created_at<? GROUP BY category ORDER BY amount DESC",
                userId, timestamp(from), timestamp(to));
        Map<String,BigDecimal> categories = new LinkedHashMap<>();
        BigDecimal periodSpent = BigDecimal.ZERO;
        for (Map<String,Object> row : categoryRows) {
            BigDecimal amount = (BigDecimal) row.get("amount");
            categories.put((String) row.get("category"), amount);
            periodSpent = periodSpent.add(amount);
        }
        BigDecimal previousTotal = totalSpent(userId, start(selected, previousDate(selected, today)), from);
        List<Map<String,Object>> dailyTrend = dailyTrend(userId, today.minusDays(6), today.plusDays(1));
        String top = categories.isEmpty() ? "no category yet" : categories.keySet().iterator().next();
        String fallback = budgetSuggestion(todaySpent, monthSpent, dailyLimit, monthlyLimit, today, top, periodSpent, previousTotal, selected);
        String prompt = "Provide one short, supportive personal spending insight based only on these aggregated INR figures. "
                + "Today outgoing spending ₹" + todaySpent + " against daily limit ₹" + dailyLimit + ". "
                + "This month outgoing spending ₹" + monthSpent + " against monthly limit ₹" + monthlyLimit + ". "
                + "Current " + selected + " period spending ₹" + periodSpent + ", previous comparable period ₹" + previousTotal
                + ", largest category " + top + ". Highlight a limit only if the numbers justify it; never invent transactions.";
        String suggestion = gemini.suggestion(prompt, fallback);

        Map<String,Object> result = new LinkedHashMap<>();
        result.put("period", selected);
        result.put("totalSpent", periodSpent);
        result.put("categoryTotals", categories);
        result.put("transactions", List.of());
        result.put("suggestion", suggestion);
        result.put("insightSource", gemini.isConfigured() ? "gemini-or-fallback" : "local-budget-analysis");
        result.put("dailyLimit", dailyLimit);
        result.put("monthlyLimit", monthlyLimit);
        result.put("todaySpent", todaySpent);
        result.put("monthSpent", monthSpent);
        result.put("todayReceived", todayReceived);
        result.put("monthReceived", monthReceived);
        result.put("dailyUsagePercent", percent(todaySpent, dailyLimit));
        result.put("monthlyUsagePercent", percent(monthSpent, monthlyLimit));
        result.put("dailyBudgetStatus", budgetStatus(todaySpent, dailyLimit));
        result.put("monthlyBudgetStatus", budgetStatus(monthSpent, monthlyLimit));
        result.put("dailyTrend", dailyTrend);
        result.put("topCategory", top);
        return result;
    }

    @GetMapping("/categories")
    public Map<String,BigDecimal> categories(@RequestHeader(value = "Authorization", required = false) String authorization,
                                             @RequestParam(defaultValue = "month") String period) {
        @SuppressWarnings("unchecked") Map<String,BigDecimal> result =
                (Map<String,BigDecimal>) summary(authorization, period).get("categoryTotals");
        return result;
    }

    private Long authenticatedUserId(String authorization) {
        try {
            if (authorization == null || !authorization.startsWith("Bearer ")) throw new IllegalArgumentException("Missing bearer token");
            String email = jwt.subject(authorization.substring(7));
            return jdbc.queryForObject("SELECT id FROM users WHERE LOWER(email)=LOWER(?)", Long.class, email);
        } catch (RuntimeException error) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Valid sign-in required");
        }
    }

    private BigDecimal totalSpent(Long userId, LocalDate from, LocalDate to) {
        BigDecimal value = jdbc.queryForObject(
                "SELECT COALESCE(SUM(amount),0) FROM payments WHERE sender_id=? AND status='SUCCESS' AND category<>'Wallet' AND created_at>=? AND created_at<?",
                BigDecimal.class, userId, timestamp(from), timestamp(to));
        return value == null ? BigDecimal.ZERO : value;
    }

    private BigDecimal totalReceived(Long userId, LocalDate from, LocalDate to) {
        return total("SELECT COALESCE(SUM(amount),0) FROM payments WHERE receiver_id=? AND sender_id<>? AND status='SUCCESS' AND category<>'Wallet' AND created_at>=? AND created_at<?",
                userId, from, to);
    }

    private BigDecimal total(String sql, Long userId, LocalDate from, LocalDate to) {
        BigDecimal value = jdbc.queryForObject(sql, BigDecimal.class, userId, userId, timestamp(from), timestamp(to));
        return value == null ? BigDecimal.ZERO : value;
    }

    private List<Map<String,Object>> dailyTrend(Long userId, LocalDate from, LocalDate to) {
        List<Map<String,Object>> result = new ArrayList<>();
        Map<LocalDate,BigDecimal> byDay = new HashMap<>();
        jdbc.query("SELECT DATE(created_at) AS spent_day, SUM(amount) AS amount FROM payments WHERE sender_id=? AND status='SUCCESS' AND category<>'Wallet' AND created_at>=? AND created_at<? GROUP BY DATE(created_at)",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> byDay.put(rs.getDate("spent_day").toLocalDate(), rs.getBigDecimal("amount")), userId, timestamp(from), timestamp(to));
        for (LocalDate day = from; day.isBefore(to); day = day.plusDays(1)) {
            result.add(Map.of("date", day.toString(), "amount", byDay.getOrDefault(day, BigDecimal.ZERO)));
        }
        return result;
    }

    private BigDecimal percent(BigDecimal amount, BigDecimal limit) {
        if (limit.signum() <= 0) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.valueOf(100)).divide(limit, 1, RoundingMode.HALF_UP);
    }

    private String budgetStatus(BigDecimal amount, BigDecimal limit) {
        if (amount.compareTo(limit) >= 0) return "OVER_BUDGET";
        if (amount.compareTo(limit.multiply(new BigDecimal("0.80"))) >= 0) return "NEAR_LIMIT";
        return "ON_TRACK";
    }

    private String budgetSuggestion(BigDecimal todaySpent, BigDecimal monthSpent, BigDecimal dailyLimit,
                                    BigDecimal monthlyLimit, LocalDate today, String top, BigDecimal current,
                                    BigDecimal previous, String period) {
        if (todaySpent.compareTo(dailyLimit) >= 0) {
            String overBy = todaySpent.compareTo(dailyLimit) == 0 ? "You reached today's spending limit."
                    : "You're " + money(todaySpent.subtract(dailyLimit)) + " over today's limit.";
            return "You've used " + percent(todaySpent, dailyLimit).toPlainString() + "% of today's limit. "
                    + overBy + " Consider pausing non-essential spending today.";
        }
        if (monthSpent.compareTo(monthlyLimit) >= 0) {
            String overBy = monthSpent.compareTo(monthlyLimit) == 0 ? "You reached this month's spending limit."
                    : "You're " + money(monthSpent.subtract(monthlyLimit)) + " over this month's limit.";
            return "You've used " + percent(monthSpent, monthlyLimit).toPlainString() + "% of this month's limit. "
                    + overBy + " Review the remaining days in your budget.";
        }
        if (todaySpent.compareTo(dailyLimit.multiply(new BigDecimal("0.80"))) >= 0) {
            return "You've used " + percent(todaySpent, dailyLimit).toPlainString() + "% of today's limit. "
                    + "About ₹" + dailyLimit.subtract(todaySpent).toPlainString() + " remains for today.";
        }
        if (monthSpent.compareTo(monthlyLimit.multiply(new BigDecimal("0.80"))) >= 0) {
            return "You've used " + percent(monthSpent, monthlyLimit).toPlainString() + "% of this month's limit. "
                    + "About ₹" + monthlyLimit.subtract(monthSpent).toPlainString() + " remains this month.";
        }
        String label = period.equals("day") ? "today" : period.equals("year") ? "this year" : "this month";
        if (current.signum() == 0) return "No outgoing payments recorded " + label + " yet. You're within your current spending limits.";
        String comparison = "";
        if (previous.signum() > 0) {
            BigDecimal delta = current.subtract(previous).multiply(BigDecimal.valueOf(100)).divide(previous, 0, RoundingMode.HALF_UP);
            comparison = " That's " + delta.abs().toPlainString() + "% " + (delta.signum() >= 0 ? "more" : "less") + " than the previous period.";
        }
        return "You're within your spending limits. Most of your spending " + label + " went to " + top + "." + comparison;
    }

    private Timestamp timestamp(LocalDate date) { return Timestamp.from(date.atStartOfDay(BUSINESS_ZONE).toInstant()); }
    private String money(BigDecimal amount) { return "₹" + amount.setScale(2, RoundingMode.HALF_UP).toPlainString(); }
    private LocalDate previousDate(String period, LocalDate date) { return switch (period) { case "day" -> date.minusDays(1); case "year" -> date.minusYears(1); default -> date.minusMonths(1); }; }
    private LocalDate start(String period, LocalDate date) { return switch (period) { case "day" -> date; case "year" -> date.withDayOfYear(1); default -> date.withDayOfMonth(1); }; }
    private LocalDate nextStart(String period, LocalDate date) { return switch (period) { case "day" -> date.plusDays(1); case "year" -> date.plusYears(1); default -> date.plusMonths(1); }; }
}
