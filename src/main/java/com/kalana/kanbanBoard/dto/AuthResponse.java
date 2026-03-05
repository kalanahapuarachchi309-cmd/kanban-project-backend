package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {
    private String token;
    private String username;
    private Role role;
    private boolean mustChangePassword;
}
