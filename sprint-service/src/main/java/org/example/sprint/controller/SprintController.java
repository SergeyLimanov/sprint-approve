package org.example.sprint.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.sprint.dto.SprintDto;
import org.example.sprint.entity.SprintStatus;
import org.example.sprint.service.SprintService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/sprints")
@RequiredArgsConstructor
@Tag(name = "Sprints", description = "Sprint and MVP management API")
public class SprintController {
    private final SprintService sprintService;

    @GetMapping
    @Operation(summary = "Get all sprints")
    public ResponseEntity<List<SprintDto>> getAllSprints() {
        return ResponseEntity.ok(sprintService.getAllSprints());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get sprint by ID")
    public ResponseEntity<SprintDto> getSprintById(@PathVariable Long id) {
        return ResponseEntity.ok(sprintService.getSprintById(id));
    }

    @GetMapping("/team/{teamId}")
    @Operation(summary = "Get sprints by team ID")
    public ResponseEntity<List<SprintDto>> getSprintsByTeamId(@PathVariable Long teamId) {
        return ResponseEntity.ok(sprintService.getSprintsByTeamId(teamId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get sprints by status")
    public ResponseEntity<List<SprintDto>> getSprintsByStatus(@PathVariable SprintStatus status) {
        return ResponseEntity.ok(sprintService.getSprintsByStatus(status));
    }

    @PostMapping
    @Operation(summary = "Create new sprint")
    public ResponseEntity<SprintDto> createSprint(@Valid @RequestBody SprintDto sprintDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(sprintService.createSprint(sprintDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update sprint")
    public ResponseEntity<SprintDto> updateSprint(@PathVariable Long id, @Valid @RequestBody SprintDto sprintDto) {
        return ResponseEntity.ok(sprintService.updateSprint(id, sprintDto));
    }

    @PatchMapping("/{id}/submit")
    @Operation(summary = "Submit sprint for review")
    public ResponseEntity<SprintDto> submitForReview(@PathVariable Long id) {
        return ResponseEntity.ok(sprintService.submitForReview(id));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve sprint")
    public ResponseEntity<SprintDto> approveSprint(@PathVariable Long id) {
        return ResponseEntity.ok(sprintService.approveSprint(id));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject sprint")
    public ResponseEntity<SprintDto> rejectSprint(@PathVariable Long id) {
        return ResponseEntity.ok(sprintService.rejectSprint(id));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete sprint")
    public ResponseEntity<Void> deleteSprint(@PathVariable Long id) {
        sprintService.deleteSprint(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/recalculate-status")
    @Operation(summary = "Recalculate sprint status based on tasks")
    public ResponseEntity<SprintDto> recalculateSprintStatus(@PathVariable Long id) {
        return ResponseEntity.ok(sprintService.recalculateSprintStatus(id));
    }
}
