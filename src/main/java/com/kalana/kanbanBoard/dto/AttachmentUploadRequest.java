package com.kalana.kanbanBoard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AttachmentUploadRequest {

    @NotBlank
    private String url;

    @NotBlank
    private String publicId;

    @NotBlank
    private String fileType;

    @NotBlank
    private String originalName;
}
