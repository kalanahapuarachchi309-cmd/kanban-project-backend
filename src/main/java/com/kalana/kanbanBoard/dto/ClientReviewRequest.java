package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.ClientReviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ClientReviewRequest {

    @NotNull
    private ClientReviewStatus reviewStatus;
}
