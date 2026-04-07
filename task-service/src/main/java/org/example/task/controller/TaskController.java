package org.example.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.task.dto.TaskDto;
import org.example.task.entity.TaskStatus;
import org.example.task.service.TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
@Tag(name = "Tasks", description = "Task management API")
public class TaskController {
    private final TaskService taskService;

    @GetMapping
    @Operation(summary = "Get all tasks")
    public ResponseEntity<List<TaskDto>> getAllTasks() {
        return ResponseEntity.ok(taskService.getAllTasks());
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get task by ID")
    public ResponseEntity<TaskDto> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @GetMapping("/sprint/{sprintId}")
    @Operation(summary = "Get tasks by sprint ID")
    public ResponseEntity<List<TaskDto>> getTasksBySprintId(@PathVariable Long sprintId) {
        return ResponseEntity.ok(taskService.getTasksBySprintId(sprintId));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get tasks by status")
    public ResponseEntity<List<TaskDto>> getTasksByStatus(@PathVariable TaskStatus status) {
        return ResponseEntity.ok(taskService.getTasksByStatus(status));
    }

    @GetMapping("/assigned/{userId}")
    @Operation(summary = "Get tasks assigned to user")
    public ResponseEntity<List<TaskDto>> getTasksByAssignedTo(@PathVariable Long userId) {
        return ResponseEntity.ok(taskService.getTasksByAssignedTo(userId));
    }

    @PostMapping
    @Operation(summary = "Create new task")
    public ResponseEntity<TaskDto> createTask(@Valid @RequestBody TaskDto taskDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(taskService.createTask(taskDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update task")
    public ResponseEntity<TaskDto> updateTask(@PathVariable Long id, @Valid @RequestBody TaskDto taskDto) {
        return ResponseEntity.ok(taskService.updateTask(id, taskDto));
    }

    @PatchMapping("/{id}/submit")
    @Operation(summary = "Submit task for review")
    public ResponseEntity<TaskDto> submitForReview(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.submitForReview(id));
    }

    @PatchMapping("/{id}/approve")
    @Operation(summary = "Approve task")
    public ResponseEntity<TaskDto> approveTask(@PathVariable Long id, @RequestParam Long approverId) {
        return ResponseEntity.ok(taskService.approveTask(id, approverId));
    }

    @PatchMapping("/{id}/reject")
    @Operation(summary = "Reject task")
    public ResponseEntity<TaskDto> rejectTask(@PathVariable Long id, @RequestParam Long approverId) {
        return ResponseEntity.ok(taskService.rejectTask(id, approverId));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete task")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
