package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.Role;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AddMemberRequest {

    @NotNull
    private Long userId;

    @NotNull
    private Role role;
}
