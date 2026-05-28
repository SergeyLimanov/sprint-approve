package org.example.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.task.dto.ArtifactDto;
import org.example.task.service.IArtifactService;
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
    private final IArtifactService artifactService;

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get artifacts by task ID with presigned URLs")
    public ResponseEntity<List<ArtifactDto>> getArtifactsByTaskId(
            @PathVariable Long taskId,
            @RequestParam(defaultValue = "60") int urlExpiryMinutes) {
        return ResponseEntity.ok(artifactService.getArtifactsByTaskId(taskId, urlExpiryMinutes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get artifact by ID with presigned URL")
    public ResponseEntity<ArtifactDto> getArtifactById(
            @PathVariable Long id,
            @RequestParam(defaultValue = "60") int urlExpiryMinutes) {
        return ResponseEntity.ok(artifactService.getArtifactById(id, urlExpiryMinutes));
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
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(artifactService.uploadFile(file, taskId, uploadedBy, customName));
    }

    @GetMapping("/files/{fileName:.+}")
    @Operation(summary = "Download/view file from MinIO")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = artifactService.downloadFile(fileName);

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
        String url = artifactService.getDownloadUrl(id, expiryMinutes);
        return ResponseEntity.ok(Map.of(
            "url", url,
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
