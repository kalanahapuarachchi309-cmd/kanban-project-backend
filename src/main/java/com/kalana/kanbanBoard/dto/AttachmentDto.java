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
public class AttachmentDto {
    private Long id;
    private Long workItemId;
    private String url;
    private String publicId;
    private String fileType;
    private String originalName;
    private LocalDateTime createdAt;
}
