package com.playflow.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

public final class ApiDtos {
    private ApiDtos() {}
    public record RegisterRequest(@NotBlank @Size(max=120) String name,@NotBlank @Email @Size(max=254) String email,@NotBlank @Size(min=8,max=72) String password,@NotBlank @Pattern(regexp="^(?:\\+91)?[6-9][0-9]{9}$",message="Enter a valid 10-digit Indian mobile number") String phoneNumber) {}
    public record LoginRequest(@NotBlank @Email String email,@NotBlank String password) {}
    public record UserView(Long id,String name,String email,String role,String kycStatus,String phoneNumber) {}
    public record RecipientView(Long id,String name,String phoneNumber) {}
    public record AuthResponse(String token,UserView user) {}
    public record WalletView(Long userId,BigDecimal balance,String currency) {}
    public record AmountRequest(@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount) {}
    public record TransferRequest(@NotNull Long receiverId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@Size(max=1000) String description) {}
    public record PaymentView(String transactionId,Long senderId,Long receiverId,BigDecimal amount,String status,String description,String category,Instant createdAt) {}
    public record PaymentEvent(String transactionId,Long senderId,Long receiverId,BigDecimal amount,String status,String description,Instant createdAt) {}
    public record JourneyView(String stage,String note,Instant occurredAt) {}
    public record RecoveryView(String transactionId,String failureReason,String action,String recommendation,boolean automated) {}
    public record ReconciliationItem(String code,String transactionId,String detail) {}
    public record ReconciliationReport(Instant checkedAt,int paymentsChecked,int walletsChecked,int issueCount,List<ReconciliationItem> issues) {}
    public record NotificationView(Long id,String message,String transactionId,Instant createdAt,boolean read) {}
    public record InsightSummary(String period,BigDecimal totalSpent,Map<String,BigDecimal> categoryTotals,List<PaymentView> transactions,String suggestion) {}
    public record DashboardView(WalletView wallet,long transactionCount,long successCount,long failedCount,long pendingCount,List<PaymentView> recentTransactions,BigDecimal monthSpent,List<TrendPoint> monthTrend) {}
    public record TrendPoint(String month,BigDecimal amount) {}
    public record ProfileRequest(String name) {}
    public record SpendingLimitRequest(@NotNull @DecimalMin("1.00") @Digits(integer=17,fraction=2) BigDecimal dailyLimit,
                                       @NotNull @DecimalMin("1.00") @Digits(integer=17,fraction=2) BigDecimal monthlyLimit) {}
    public record SpendingLimitView(Long userId,BigDecimal dailyLimit,BigDecimal monthlyLimit,Instant updatedAt) {}
}
