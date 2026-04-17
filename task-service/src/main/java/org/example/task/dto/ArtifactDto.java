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

    private String url; // Имя файла в MinIO или внешний URL

    private String downloadUrl; // Временная ссылка для скачивания (presigned URL)

    private String fileType;
    private Long fileSize;

    @NotNull(message = "Task ID is required")
    private Long taskId;

    private Long uploadedBy;
    private String uploadedByName;

    private LocalDateTime createdAt;
}
