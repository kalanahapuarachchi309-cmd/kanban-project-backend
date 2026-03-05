package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.Priority;
import com.kalana.kanbanBoard.entity.WorkItemType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

@Data
public class CreateWorkItemRequest {

    @NotNull
    private WorkItemType type;

    @NotBlank
    private String title;

    private String description;

    @NotNull
    private Priority priority;

    private Long assignedTo;

    private LocalDateTime dueAt;
}
