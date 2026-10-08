package com.playflow.repository;
import com.playflow.entity.Entities.JourneyEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface JourneyRepository extends JpaRepository<JourneyEvent,Long> { List<JourneyEvent> findByTransactionIdOrderByOccurredAtAsc(String transactionId); }
