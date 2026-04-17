package org.example.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.task.dto.ArtifactDto;
import org.example.task.service.ArtifactService;
import org.example.task.service.MinioStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
@Tag(name = "Artifacts", description = "Artifact management API")
public class ArtifactController {
    private final ArtifactService artifactService;
    private final MinioStorageService minioStorageService;

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get artifacts by task ID with presigned URLs")
    public ResponseEntity<List<ArtifactDto>> getArtifactsByTaskId(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "60") int urlExpiryMinutes) {
        
        List<ArtifactDto> artifacts = artifactService.getArtifactsByTaskId(taskId);
        
        // Добавляем временные ссылки для скачивания
        artifacts.forEach(artifact -> {
            if (artifact.getUrl() != null && !artifact.getUrl().startsWith("http")) {
                // Это файл в MinIO, генерируем presigned URL
                String presignedUrl = minioStorageService.getPresignedUrl(
                    artifact.getUrl(),
                    urlExpiryMinutes
                );
                artifact.setDownloadUrl(presignedUrl);
            } else {
                // Это внешняя ссылка, используем как есть
                artifact.setDownloadUrl(artifact.getUrl());
            }
        });
        
        return ResponseEntity.ok(artifacts);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get artifact by ID with presigned URL")
    public ResponseEntity<ArtifactDto> getArtifactById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "60") int urlExpiryMinutes) {
        
        ArtifactDto artifact = artifactService.getArtifactById(id);
        
        // Генерируем временную ссылку
        if (artifact.getUrl() != null && !artifact.getUrl().startsWith("http")) {
            String presignedUrl = minioStorageService.getPresignedUrl(
                artifact.getUrl(),
                urlExpiryMinutes
            );
            artifact.setDownloadUrl(presignedUrl);
        } else {
            artifact.setDownloadUrl(artifact.getUrl());
        }
        
        return ResponseEntity.ok(artifact);
    }

    @PostMapping
    @Operation(summary = "Create new artifact")
    public ResponseEntity<ArtifactDto> createArtifact(@Valid @RequestBody ArtifactDto artifactDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artifactService.createArtifact(artifactDto));
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload file to MinIO")
    public ResponseEntity<ArtifactDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskId") Long taskId,
            @RequestParam("uploadedBy") Long uploadedBy,
            @RequestParam(value = "name", required = false) String customName) {
        
        // Загружаем файл в MinIO
        String fileName = minioStorageService.storeFile(file);

        ArtifactDto artifactDto = new ArtifactDto();
        artifactDto.setName(customName != null && !customName.trim().isEmpty() 
                ? customName 
                : file.getOriginalFilename());
        artifactDto.setUrl(fileName);  // Сохраняем только имя файла в MinIO
        artifactDto.setTaskId(taskId);
        artifactDto.setUploadedBy(uploadedBy);
        artifactDto.setFileType(file.getContentType());
        artifactDto.setFileSize(file.getSize());

        ArtifactDto created = artifactService.createArtifact(artifactDto);
        
        // Генерируем временную ссылку для ответа
        String presignedUrl = minioStorageService.getPresignedUrl(fileName, 60);
        created.setDownloadUrl(presignedUrl);
        
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @GetMapping("/files/{fileName:.+}")
    @Operation(summary = "Download/view file from MinIO")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = minioStorageService.loadFileAsResource(fileName);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + fileName + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/download-url")
    @Operation(summary = "Get temporary download URL for artifact")
    public ResponseEntity<Map<String, String>> getDownloadUrl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "60") int expiryMinutes) {
        
        ArtifactDto artifact = artifactService.getArtifactById(id);
        
        if (artifact.getUrl() == null || artifact.getUrl().startsWith("http")) {
            // Внешняя ссылка
            return ResponseEntity.ok(Map.of(
                "url", artifact.getUrl() != null ? artifact.getUrl() : "",
                "expiresIn", "permanent"
            ));
        }
        
        // Генерируем presigned URL для файла в MinIO
        String presignedUrl = minioStorageService.getPresignedUrl(
            artifact.getUrl(),
            expiryMinutes
        );
        
        return ResponseEntity.ok(Map.of(
            "url", presignedUrl,
            "expiresIn", expiryMinutes + " minutes"
        ));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete artifact")
    public ResponseEntity<Void> deleteArtifact(@PathVariable Long id) {
        artifactService.deleteArtifact(id);
        return ResponseEntity.noContent().build();
    }
}
