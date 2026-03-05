package com.kalana.kanbanBoard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ResendPasswordSetupRequest {

    @NotBlank
    private String usernameOrEmail;
}
