package com.playflow.notification;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "notifications")
public class NotificationRecord {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) public Long id;
    @Column(name = "user_id", nullable = false) public Long userId;
    @Column(nullable = false, length = 300) public String message;
    @Column(name = "transaction_id", nullable = false, length = 36) public String transactionId;
    @Column(name = "created_at", nullable = false) public Instant createdAt;
    @Column(name = "is_read", nullable = false) public boolean read;

    protected NotificationRecord() {}
    NotificationRecord(Long userId, String message, String transactionId, Instant createdAt) {
        this.userId = userId; this.message = message; this.transactionId = transactionId; this.createdAt = createdAt;
    }
}
