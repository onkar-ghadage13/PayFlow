package com.playflow.insights;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentEvent(String transactionId, Long senderId, Long receiverId, BigDecimal amount,
                           String status, String description, Instant createdAt) {}
