package com.kalana.kanbanBoard.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateProjectRequest {

    @NotBlank
    private String name;

    private String description;
}
