package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.ClientReviewStatus;
import com.kalana.kanbanBoard.entity.Priority;
import com.kalana.kanbanBoard.entity.WorkItemStatus;
import com.kalana.kanbanBoard.entity.WorkItemType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkItemDto {
    private Long id;
    private Long projectId;
    private WorkItemType type;
    private String title;
    private String description;
    private WorkItemStatus status;
    private Priority priority;
    private UserDto createdBy;
    private UserDto assignedTo;
    private LocalDateTime dueAt;
    private LocalDateTime publishedAt;
    private ClientReviewStatus clientReviewStatus;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AttachmentDto> attachments;
}
