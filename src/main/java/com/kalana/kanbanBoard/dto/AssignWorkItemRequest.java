package com.kalana.kanbanBoard.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssignWorkItemRequest {

    @NotNull
    @JsonAlias("userId")
    private Long developerId;
}
