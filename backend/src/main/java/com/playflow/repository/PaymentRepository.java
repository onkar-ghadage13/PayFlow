package com.playflow.repository;
import com.playflow.entity.Entities.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface PaymentRepository extends JpaRepository<Payment,Long> {
    Optional<Payment> findByIdempotencyKey(String key);
    Optional<Payment> findByTransactionId(String transactionId);
    List<Payment> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(Long senderId, Long receiverId);
}
