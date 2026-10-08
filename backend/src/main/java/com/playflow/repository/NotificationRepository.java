package com.playflow.repository;
import com.playflow.entity.Entities.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface NotificationRepository extends JpaRepository<AppNotification,Long> { List<AppNotification> findByUserIdOrderByCreatedAtDesc(Long userId); }
