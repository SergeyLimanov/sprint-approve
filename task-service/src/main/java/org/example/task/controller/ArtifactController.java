package org.example.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.task.dto.ArtifactDto;
import org.example.task.service.ArtifactService;
import org.example.task.service.FileStorageService;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
@Tag(name = "Artifacts", description = "Artifact management API")
public class ArtifactController {
    private final ArtifactService artifactService;
    private final FileStorageService fileStorageService;

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get artifacts by task ID")
    public ResponseEntity<List<ArtifactDto>> getArtifactsByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(artifactService.getArtifactsByTaskId(taskId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get artifact by ID")
    public ResponseEntity<ArtifactDto> getArtifactById(@PathVariable Long id) {
        return ResponseEntity.ok(artifactService.getArtifactById(id));
    }

    @PostMapping
    @Operation(summary = "Create new artifact")
    public ResponseEntity<ArtifactDto> createArtifact(@Valid @RequestBody ArtifactDto artifactDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(artifactService.createArtifact(artifactDto));
    }

    @PostMapping("/upload")
    @Operation(summary = "Upload file as artifact")
    public ResponseEntity<ArtifactDto> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("taskId") Long taskId,
            @RequestParam("uploadedBy") Long uploadedBy) {
        
        String fileName = fileStorageService.storeFile(file);
        
        String fileDownloadUri = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/artifacts/files/")
                .path(fileName)
                .toUriString();

        ArtifactDto artifactDto = new ArtifactDto();
        artifactDto.setName(file.getOriginalFilename());
        artifactDto.setUrl(fileDownloadUri);
        artifactDto.setTaskId(taskId);
        artifactDto.setUploadedBy(uploadedBy);
        artifactDto.setFileType(file.getContentType());
        artifactDto.setFileSize(file.getSize());

        return ResponseEntity.status(HttpStatus.CREATED).body(artifactService.createArtifact(artifactDto));
    }

    @GetMapping("/files/{fileName:.+}")
    @Operation(summary = "Download/view file")
    public ResponseEntity<Resource> downloadFile(@PathVariable String fileName) {
        Resource resource = fileStorageService.loadFileAsResource(fileName);

        String contentType = "application/octet-stream";
        try {
            contentType = Files.probeContentType(Paths.get(resource.getFile().getAbsolutePath()));
        } catch (Exception ex) {
            // Use default content type
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete artifact")
    public ResponseEntity<Void> deleteArtifact(@PathVariable Long id) {
        artifactService.deleteArtifact(id);
        return ResponseEntity.noContent().build();
    }
}
