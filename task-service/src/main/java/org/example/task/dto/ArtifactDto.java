package org.example.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ArtifactDto {
    private Long id;

    @NotBlank(message = "Artifact name is required")
    private String name;

    @NotBlank(message = "Artifact URL is required")
    private String url;

    private String fileType;
    private Long fileSize;

    @NotNull(message = "Task ID is required")
    private Long taskId;

    private Long uploadedBy;
    private String uploadedByName;

    private LocalDateTime createdAt;
}
