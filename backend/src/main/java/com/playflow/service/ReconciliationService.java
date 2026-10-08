package com.playflow.service;

import com.playflow.dto.ApiDtos.ReconciliationItem;
import com.playflow.dto.ApiDtos.ReconciliationReport;
import com.playflow.entity.Entities.JourneyEvent;
import com.playflow.entity.Entities.Payment;
import com.playflow.entity.Entities.Wallet;
import com.playflow.repository.JourneyRepository;
import com.playflow.repository.PaymentRepository;
import com.playflow.repository.UserRepository;
import com.playflow.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ReconciliationService {
    private final PaymentRepository payments;
    private final JourneyRepository journeys;
    private final WalletRepository wallets;
    private final UserRepository users;

    public ReconciliationService(PaymentRepository payments, JourneyRepository journeys,
                                 WalletRepository wallets, UserRepository users) {
        this.payments = payments;
        this.journeys = journeys;
        this.wallets = wallets;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public ReconciliationReport reconcile() {
        List<Payment> allPayments = payments.findAll();
        List<Wallet> allWallets = wallets.findAll();
        Set<Long> userIds = users.findAll().stream().map(u -> u.id).collect(Collectors.toSet());
        Map<String, List<JourneyEvent>> eventsByTransaction = journeys.findAll().stream()
                .collect(Collectors.groupingBy(e -> e.transactionId));
        Map<Long, BigDecimal> expectedBalances = new HashMap<>();
        List<ReconciliationItem> issues = new ArrayList<>();

        for (Payment payment : allPayments) {
            List<JourneyEvent> events = eventsByTransaction.getOrDefault(payment.transactionId, List.of());
            boolean successEvent = events.stream().anyMatch(e -> "SUCCESS".equals(e.stage));
            boolean failedEvent = events.stream().anyMatch(e -> "FAILED".equals(e.stage));
            if (events.isEmpty()) add(issues, "MISSING_JOURNEY", payment.transactionId, "Payment has no lifecycle events.");
            if ("SUCCESS".equals(payment.status) && !successEvent)
                add(issues, "SUCCESS_WITHOUT_EVENT", payment.transactionId, "Payment is successful but has no SUCCESS journey event.");
            if ("FAILED".equals(payment.status) && !failedEvent)
                add(issues, "FAILED_WITHOUT_EVENT", payment.transactionId, "Payment is failed but has no FAILED journey event.");
            if (Set.of("PENDING", "PROCESSING").contains(payment.status))
                add(issues, "NON_TERMINAL_PAYMENT", payment.transactionId, "Payment remains " + payment.status + "; review its current outcome.");
            if (!userIds.contains(payment.senderId) || !userIds.contains(payment.receiverId))
                add(issues, "UNKNOWN_PAYMENT_USER", payment.transactionId, "Sender or receiver account does not exist.");
            if (payment.amount == null || payment.amount.signum() <= 0)
                add(issues, "INVALID_AMOUNT", payment.transactionId, "Payment amount must be greater than zero.");

            if (!"SUCCESS".equals(payment.status) || payment.amount == null || payment.amount.signum() <= 0) continue;
            if ("Wallet".equals(payment.category)) {
                String description = payment.description == null ? "" : payment.description.toLowerCase(Locale.ROOT);
                if (description.contains("deposit")) {
                    credit(expectedBalances, payment.receiverId, payment.amount);
                } else if (description.contains("withdraw")) {
                    debit(expectedBalances, payment.senderId, payment.amount);
                } else {
                    add(issues, "UNKNOWN_WALLET_ENTRY", payment.transactionId, "Wallet ledger entry is not marked as a deposit or withdrawal.");
                }
            } else {
                debit(expectedBalances, payment.senderId, payment.amount);
                credit(expectedBalances, payment.receiverId, payment.amount);
            }
        }

        Map<Long, Wallet> walletByUser = allWallets.stream().collect(Collectors.toMap(w -> w.userId, w -> w, (a, b) -> a));
        for (Long userId : userIds) {
            Wallet wallet = walletByUser.get(userId);
            if (wallet == null) {
                add(issues, "MISSING_WALLET", null, "User " + userId + " has no wallet.");
                continue;
            }
            BigDecimal expected = expectedBalances.getOrDefault(userId, BigDecimal.ZERO);
            if (wallet.balance.compareTo(expected) != 0) {
                add(issues, "WALLET_BALANCE_MISMATCH", null,
                        "User " + userId + " wallet is " + wallet.balance + " but successful payment ledger totals " + expected + ".");
            }
        }

        return new ReconciliationReport(Instant.now(), allPayments.size(), allWallets.size(), issues.size(), List.copyOf(issues));
    }

    private void credit(Map<Long, BigDecimal> balances, Long userId, BigDecimal amount) {
        balances.merge(userId, amount, BigDecimal::add);
    }

    private void debit(Map<Long, BigDecimal> balances, Long userId, BigDecimal amount) {
        balances.merge(userId, amount.negate(), BigDecimal::add);
    }

    private void add(List<ReconciliationItem> issues, String code, String transactionId, String detail) {
        issues.add(new ReconciliationItem(code, transactionId, detail));
    }
}
