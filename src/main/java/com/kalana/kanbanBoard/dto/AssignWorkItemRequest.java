package com.kalana.kanbanBoard.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignWorkItemRequest {

    @NotNull
    private Long userId;
}
