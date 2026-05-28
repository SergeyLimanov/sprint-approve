package org.example.task.mapper;

import org.example.task.dto.TaskDto;
import org.example.task.entity.Task;

public final class TaskMapper {
    
    private TaskMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static TaskDto toDto(Task task) {
        if (task == null) {
            return null;
        }
        
        TaskDto dto = new TaskDto();
        dto.setId(task.getId());
        dto.setTitle(task.getTitle());
        dto.setDescription(task.getDescription());
        dto.setSprintId(task.getSprintId());
        dto.setStatus(task.getStatus());
        dto.setAssignedTo(task.getAssignedTo());
        dto.setApproverId(task.getApproverId());
        dto.setCreatedBy(task.getCreatedBy());
        dto.setCreatedAt(task.getCreatedAt());
        dto.setUpdatedAt(task.getUpdatedAt());
        return dto;
    }
}
