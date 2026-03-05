package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.ClientReviewStatus;
import com.kalana.kanbanBoard.entity.Priority;
import com.kalana.kanbanBoard.entity.WorkItemStatus;
import com.kalana.kanbanBoard.entity.WorkItemType;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class WorkItemFilterRequest {

    private WorkItemType type;
    private WorkItemStatus status;
    private Priority priority;
    private Long assignedTo;
    private Long createdBy;
    private LocalDateTime createdFrom;
    private LocalDateTime createdTo;
    private LocalDateTime dueFrom;
    private LocalDateTime dueTo;
    private ClientReviewStatus clientReviewStatus;
    private String search;
    private String sortBy; // newest | oldest | due_soon | priority
}
