package com.playflow.insights;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class PaymentEventListener {
    private final JdbcTemplate jdbc;
    private final GeminiInsightsService gemini;
    public PaymentEventListener(JdbcTemplate jdbc, GeminiInsightsService gemini) { this.jdbc = jdbc; this.gemini = gemini; }

    @KafkaListener(topics = "${app.kafka.payment-topic}", groupId = "payflow-insights")
    public void onPayment(PaymentEvent event) {
        if (!"SUCCESS".equals(event.status()) || event.description() == null || event.description().toLowerCase().contains("wallet")) return;
        String category = gemini.classify(event.description());
        jdbc.update("UPDATE payments SET category=? WHERE transaction_id=?", category, event.transactionId());
    }
}
