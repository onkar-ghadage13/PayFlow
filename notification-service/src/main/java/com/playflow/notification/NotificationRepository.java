package com.playflow.notification;

import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<NotificationRecord, Long> {
    boolean existsByTransactionIdAndUserIdAndMessage(String transactionId, Long userId, String message);
}
