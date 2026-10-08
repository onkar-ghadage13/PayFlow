package com.playflow.notification;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;

@Component
public class PaymentEventListener {
    private final NotificationRepository notifications;
    public PaymentEventListener(NotificationRepository notifications) { this.notifications = notifications; }

    @KafkaListener(topics = "${app.kafka.payment-topic}", groupId = "payflow-notifications")
    @Transactional
    public void onPayment(PaymentEvent event) {
        Instant createdAt = event.createdAt() == null ? Instant.now() : event.createdAt();
        boolean self = event.senderId().equals(event.receiverId());
        if ("SUCCESS".equals(event.status())) {
            if (self) {
                String action = event.description().toLowerCase().contains("withdraw") ? "withdrawn from" : "added to";
                save(event.senderId(), "₹" + event.amount().toPlainString() + " " + action + " your wallet", event, createdAt);
            } else {
                save(event.senderId(), "You sent ₹" + event.amount().toPlainString() + " successfully", event, createdAt);
                save(event.receiverId(), "You received ₹" + event.amount().toPlainString(), event, createdAt);
            }
        } else if ("FAILED".equals(event.status())) {
            save(event.senderId(), "Payment of ₹" + event.amount().toPlainString() + " failed: " + event.description(), event, createdAt);
        }
    }

    private void save(Long userId, String message, PaymentEvent event, Instant createdAt) {
        if (notifications.existsByTransactionIdAndUserIdAndMessage(event.transactionId(), userId, message)) return;
        NotificationRecord record = new NotificationRecord(userId, message, event.transactionId(), createdAt);
        notifications.save(record);
    }
}
