package com.kalana.kanbanBoard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FollowerDto {
    private Long id;
    private UserDto follower;
    private UserDto following;
    private LocalDateTime createdAt;
}
