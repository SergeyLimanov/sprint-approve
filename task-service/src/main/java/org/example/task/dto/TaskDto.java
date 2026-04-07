package org.example.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.task.entity.TaskStatus;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskDto {
    private Long id;

    @NotBlank(message = "Task title is required")
    private String title;

    private String description;

    @NotNull(message = "Sprint ID is required")
    private Long sprintId;

    private TaskStatus status;

    private Long assignedTo;
    private String assignedToName;

    private Long approverId;
    private String approverName;

    private Long createdBy;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
