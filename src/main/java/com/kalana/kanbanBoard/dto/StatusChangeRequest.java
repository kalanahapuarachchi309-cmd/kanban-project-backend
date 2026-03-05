package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.WorkItemStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StatusChangeRequest {

    @NotNull
    private WorkItemStatus status;
}
