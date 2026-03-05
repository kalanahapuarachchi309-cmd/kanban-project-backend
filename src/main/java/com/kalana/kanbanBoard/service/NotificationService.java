package com.kalana.kanbanBoard.service;

import com.kalana.kanbanBoard.dto.NotificationDto;
import com.kalana.kanbanBoard.entity.Notification;
import com.kalana.kanbanBoard.entity.NotificationType;
import com.kalana.kanbanBoard.entity.User;
import com.kalana.kanbanBoard.exception.ResourceNotFoundException;
import com.kalana.kanbanBoard.repository.NotificationRepository;
import com.kalana.kanbanBoard.util.AuthUtil;
import com.kalana.kanbanBoard.util.Mapper;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final SimpMessagingTemplate messagingTemplate;
    private final AuthUtil authUtil;

    @Transactional
    public void sendNotification(User recipient, NotificationType type, String title, String body) {
        Notification notification = Notification.builder()
                .user(recipient)
                .type(type)
                .title(title)
                .body(body)
                .build();
        notification = notificationRepository.save(notification);

        // Push real-time via WebSocket STOMP
        NotificationDto dto = Mapper.toNotificationDto(notification);
        messagingTemplate.convertAndSendToUser(
                recipient.getUsername(),
                "/queue/notifications",
                dto);
    }

    public List<NotificationDto> getMyNotifications() {
        Long userId = authUtil.getCurrentUserId();
        return notificationRepository.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(Mapper::toNotificationDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public NotificationDto markRead(Long notificationId) {
        Long userId = authUtil.getCurrentUserId();
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));

        if (!notification.getUser().getId().equals(userId)) {
            throw new com.kalana.kanbanBoard.exception.AccessDeniedException("Not your notification");
        }

        notification.setRead(true);
        return Mapper.toNotificationDto(notificationRepository.save(notification));
    }

    @Transactional
    public void markAllRead() {
        Long userId = authUtil.getCurrentUserId();
        List<Notification> unread = notificationRepository.findAllByUserIdAndIsReadFalse(userId);
        for (Notification notification : unread) {
            notification.setRead(true);
        }
        notificationRepository.saveAll(unread);
    }
}
