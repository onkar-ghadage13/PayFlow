package com.playflow.service;

import com.playflow.dto.ApiDtos.RecoveryView;
import com.playflow.entity.Entities.JourneyEvent;
import com.playflow.entity.Entities.Payment;
import com.playflow.repository.JourneyRepository;
import com.playflow.repository.PaymentRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Locale;

@Service
public class PaymentRecoveryService {
    private final PaymentRepository payments;
    private final JourneyRepository journeys;

    public PaymentRecoveryService(PaymentRepository payments, JourneyRepository journeys) {
        this.payments = payments;
        this.journeys = journeys;
    }

    public RecoveryView assess(String transactionId, Long userId) {
        Payment payment = payments.findByTransactionId(transactionId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found"));
        if (!payment.senderId.equals(userId) && !payment.receiverId.equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Transaction not found");
        }
        List<JourneyEvent> events = journeys.findByTransactionIdOrderByOccurredAtAsc(transactionId);
        String notes = events.stream().map(e -> e.note == null ? "" : e.note)
                .reduce("", (left, right) -> left + " " + right).toLowerCase(Locale.ROOT);

        if ("SUCCESS".equals(payment.status)) {
            boolean hasFailedEvent = events.stream().anyMatch(e -> "FAILED".equals(e.stage));
            if (hasFailedEvent) return decision(payment, "Conflicting success and failure events", "REVERSE",
                    "Payment and journey records disagree. Freeze further handling and review the ledger before any reversal.");
            return decision(payment, "No payment failure", "NONE", "Payment completed successfully; no recovery action is needed.");
        }

        if (notes.contains("insufficient") || notes.contains("balance check failed")) {
            return decision(payment, "Insufficient wallet balance", "WAIT",
                    "Add funds, then create a new transfer. The original failed payment is retained for history.");
        }
        if (notes.contains("duplicate") || notes.contains("idempotency")) {
            return decision(payment, "Duplicate request or idempotency conflict", "MANUAL_REVIEW",
                    "Keep the original idempotency key attached to its original result. Verify the transaction before creating another payment.");
        }
        if (notes.contains("timeout") || notes.contains("timed out")) {
            return decision(payment, "Payment processor timeout", "RETRY",
                    "Check the provider and wallet ledger first, then retry once with a new request only if no debit was recorded.");
        }
        if ("PENDING".equals(payment.status) || "PROCESSING".equals(payment.status)) {
            boolean stale = payment.createdAt != null && Duration.between(payment.createdAt, Instant.now()).toMinutes() >= 5;
            return decision(payment, stale ? "Payment is still processing beyond the expected window" : "Payment is still processing",
                    stale ? "MANUAL_REVIEW" : "WAIT", stale
                            ? "Check payment and wallet records before retrying; do not submit a second debit while the outcome is uncertain."
                            : "Wait for the current payment attempt to finish before taking another action.");
        }
        return decision(payment, "Failure reason could not be classified", "MANUAL_REVIEW",
                "Review the payment journey and wallet records. No automatic retry or reversal was performed.");
    }

    private RecoveryView decision(Payment p, String reason, String action, String recommendation) {
        return new RecoveryView(p.transactionId, reason, action, recommendation, false);
    }
}
