package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.Priority;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class UpdateWorkItemRequest {

    private String title;
    private String description;
    private Priority priority;
    private LocalDateTime dueAt;
}
