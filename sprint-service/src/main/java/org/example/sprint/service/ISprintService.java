package org.example.sprint.service;

import org.example.sprint.dto.SprintDto;
import org.example.sprint.entity.SprintStatus;

import java.util.List;

public interface ISprintService {
    List<SprintDto> getAllSprints();
    
    SprintDto getSprintById(Long id);
    
    List<SprintDto> getSprintsByTeamId(Long teamId);
    
    List<SprintDto> getSprintsByStatus(SprintStatus status);
    
    SprintDto createSprint(SprintDto sprintDto);
    
    SprintDto updateSprint(Long id, SprintDto sprintDto);
    
    SprintDto updateSprintStatus(Long id, SprintStatus status);
    
    SprintDto submitForReview(Long id);
    
    SprintDto approveSprint(Long id, Long approverId);
    
    SprintDto rejectSprint(Long id, Long approverId);
    
    void deleteSprint(Long id);
    
    SprintDto recalculateSprintStatus(Long id);
}
