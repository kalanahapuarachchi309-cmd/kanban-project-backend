package com.kalana.kanbanBoard.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SetPasswordWithTokenRequest {

    @NotBlank
    private String token;

    @NotBlank
    @Size(min = 6, max = 100)
    private String newPassword;
}
