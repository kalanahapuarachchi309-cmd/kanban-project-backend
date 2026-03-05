package com.kalana.kanbanBoard.repository;

import com.kalana.kanbanBoard.entity.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    List<Notification> findAllByUserIdAndIsReadFalse(Long userId);

    long countByUserIdAndIsReadFalse(Long userId);
}
