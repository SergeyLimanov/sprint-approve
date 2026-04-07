package org.example.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.task.dto.ArtifactDto;
import org.example.task.service.ArtifactService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/artifacts")
@RequiredArgsConstructor
@Tag(name = "Artifacts", description = "Artifact management API")
public class ArtifactController {
    private final ArtifactService artifactService;

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

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete artifact")
    public ResponseEntity<Void> deleteArtifact(@PathVariable Long id) {
        artifactService.deleteArtifact(id);
        return ResponseEntity.noContent().build();
    }
}
