package org.example.task.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistoryDto {
    private Long id;
    private Long taskId;
    private String previousStatus;
    private String newStatus;
    private String comment;
    private Long changedBy;
    private String changedByName;
    private LocalDateTime changedAt;
}
