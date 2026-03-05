package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.NotificationType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationDto {
    private Long id;
    private NotificationType type;
    private String title;
    private String body;
    private boolean isRead;
    private LocalDateTime createdAt;
}
