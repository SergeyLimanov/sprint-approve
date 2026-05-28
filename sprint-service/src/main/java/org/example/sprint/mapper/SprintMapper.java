package org.example.sprint.mapper;

import org.example.sprint.dto.SprintDto;
import org.example.sprint.entity.Sprint;

public final class SprintMapper {
    
    private SprintMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static SprintDto toDto(Sprint sprint) {
        if (sprint == null) {
            return null;
        }
        
        SprintDto dto = new SprintDto();
        dto.setId(sprint.getId());
        dto.setName(sprint.getName());
        dto.setDescription(sprint.getDescription());
        dto.setTeamId(sprint.getTeamId());
        dto.setType(sprint.getType());
        dto.setStatus(sprint.getStatus());
        dto.setStartDate(sprint.getStartDate());
        dto.setEndDate(sprint.getEndDate());
        dto.setCreatedBy(sprint.getCreatedBy());
        dto.setCreatedAt(sprint.getCreatedAt());
        dto.setUpdatedAt(sprint.getUpdatedAt());
        return dto;
    }
}
