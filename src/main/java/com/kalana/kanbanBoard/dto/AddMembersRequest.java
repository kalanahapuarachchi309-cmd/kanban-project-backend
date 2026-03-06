package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.Role;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class AddMembersRequest {

    @NotEmpty
    private List<Long> userIds;

    @NotNull
    private Role role;
}
