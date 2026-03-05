package com.kalana.kanbanBoard.dto;

import com.kalana.kanbanBoard.entity.Role;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProjectMemberDto {
    private Long id;
    private Long projectId;
    private UserDto user;
    private Role role;
    private LocalDateTime createdAt;
}
