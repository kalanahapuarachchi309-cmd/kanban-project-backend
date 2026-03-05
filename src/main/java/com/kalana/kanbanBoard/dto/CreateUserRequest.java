package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateUserRequest {

    @NotBlank
    private String username;

    @NotBlank
    private String email;

    private String temporaryPassword;

    @NotNull
    private Role role;
}
