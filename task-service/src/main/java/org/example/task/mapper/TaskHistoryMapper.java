package org.example.task.mapper;

import org.example.task.dto.TaskHistoryDto;
import org.example.task.entity.TaskHistory;

public final class TaskHistoryMapper {
    
    private TaskHistoryMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static TaskHistoryDto toDto(TaskHistory history) {
        if (history == null) {
            return null;
        }
        
        TaskHistoryDto dto = new TaskHistoryDto();
        dto.setId(history.getId());
        dto.setTaskId(history.getTaskId());
        dto.setPreviousStatus(history.getPreviousStatus());
        dto.setNewStatus(history.getNewStatus());
        dto.setComment(history.getComment());
        dto.setChangedBy(history.getChangedBy());
        dto.setChangedByName(history.getChangedByName());
        dto.setChangedAt(history.getChangedAt());
        return dto;
    }
}
