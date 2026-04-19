package org.example.sprint.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.sprint.client.TaskDto;
import org.example.sprint.client.TaskServiceClient;
import org.example.sprint.client.TeamDto;
import org.example.sprint.client.TeamServiceClient;
import org.example.sprint.client.UserDto;
import org.example.sprint.dto.SprintDto;
import org.example.sprint.entity.Sprint;
import org.example.sprint.entity.SprintStatus;
import org.example.sprint.repository.SprintRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SprintService {
    private final SprintRepository sprintRepository;
    private final TeamServiceClient teamServiceClient;
    private final TaskServiceClient taskServiceClient;

    @Transactional(readOnly = true)
    public List<SprintDto> getAllSprints() {
        return sprintRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public SprintDto getSprintById(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));
        return convertToDto(sprint);
    }

    @Transactional(readOnly = true)
    public List<SprintDto> getSprintsByTeamId(Long teamId) {
        return sprintRepository.findByTeamId(teamId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<SprintDto> getSprintsByStatus(SprintStatus status) {
        return sprintRepository.findByStatus(status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public SprintDto createSprint(SprintDto sprintDto) {
        Sprint sprint = new Sprint();
        sprint.setName(sprintDto.getName());
        sprint.setDescription(sprintDto.getDescription());
        sprint.setTeamId(sprintDto.getTeamId());
        sprint.setType(sprintDto.getType());
        sprint.setStatus(SprintStatus.CREATED);
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());
        sprint.setCreatedBy(sprintDto.getCreatedBy());

        Sprint savedSprint = sprintRepository.save(sprint);
        return convertToDto(savedSprint);
    }

    @Transactional
    public SprintDto updateSprint(Long id, SprintDto sprintDto) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        sprint.setName(sprintDto.getName());
        sprint.setDescription(sprintDto.getDescription());
        sprint.setStartDate(sprintDto.getStartDate());
        sprint.setEndDate(sprintDto.getEndDate());

        Sprint updatedSprint = sprintRepository.save(sprint);
        return convertToDto(updatedSprint);
    }

    @Transactional
    public SprintDto updateSprintStatus(Long id, SprintStatus status) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        sprint.setStatus(status);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return convertToDto(updatedSprint);
    }

    @Transactional
    public SprintDto submitForReview(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        if (sprint.getStatus() != SprintStatus.CREATED) {
            throw new RuntimeException("Only sprints with CREATED status can be submitted for review");
        }

        sprint.setStatus(SprintStatus.ON_REVIEW);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return convertToDto(updatedSprint);
    }

    @Transactional
    public SprintDto approveSprint(Long id, Long approverId) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        // Check approver role
        try {
            UserDto approver = teamServiceClient.getUserById(approverId);
            if (!"APPROVER".equals(approver.getRole()) && 
                !"TEAM_LEAD".equals(approver.getRole()) && 
                !"MANAGER".equals(approver.getRole())) {
                throw new RuntimeException("Only APPROVER, TEAM_LEAD or MANAGER can approve sprints. Your role: " + approver.getRole());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not verify approver role: {}", e.getMessage());
            throw new RuntimeException("Could not verify approver permissions");
        }

        // Check if all tasks are approved
        try {
            List<TaskDto> tasks = taskServiceClient.getTasksBySprintId(id);
            boolean allTasksApproved = tasks.stream()
                    .allMatch(task -> "APPROVED".equals(task.getStatus()));

            if (!allTasksApproved) {
                throw new RuntimeException("Cannot approve sprint: not all tasks are approved");
            }
        } catch (Exception e) {
            log.warn("Could not verify tasks status: {}", e.getMessage());
        }

        sprint.setStatus(SprintStatus.APPROVED);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return convertToDto(updatedSprint);
    }

    @Transactional
    public SprintDto rejectSprint(Long id, Long approverId) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        // Check approver role
        try {
            UserDto approver = teamServiceClient.getUserById(approverId);
            if (!"APPROVER".equals(approver.getRole()) && 
                !"TEAM_LEAD".equals(approver.getRole()) && 
                !"MANAGER".equals(approver.getRole())) {
                throw new RuntimeException("Only APPROVER, TEAM_LEAD or MANAGER can reject sprints. Your role: " + approver.getRole());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not verify approver role: {}", e.getMessage());
            throw new RuntimeException("Could not verify approver permissions");
        }

        sprint.setStatus(SprintStatus.REJECTED);
        Sprint updatedSprint = sprintRepository.save(sprint);
        return convertToDto(updatedSprint);
    }

    @Transactional
    public void deleteSprint(Long id) {
        if (!sprintRepository.existsById(id)) {
            throw new RuntimeException("Sprint not found with id: " + id);
        }
        sprintRepository.deleteById(id);
    }

    @Transactional
    public SprintDto recalculateSprintStatus(Long id) {
        Sprint sprint = sprintRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Sprint not found with id: " + id));

        try {
            List<TaskDto> tasks = taskServiceClient.getTasksBySprintId(id);
            
            if (tasks.isEmpty()) {
                // Если нет задач, спринт возвращается в CREATED
                if (sprint.getStatus() != SprintStatus.CREATED) {
                    SprintStatus oldStatus = sprint.getStatus();
                    sprint.setStatus(SprintStatus.CREATED);
                    log.info("Sprint {} status changed from {} to CREATED - no tasks", id, oldStatus);
                }
            } else {
                // Проверяем статусы задач
                boolean allApproved = tasks.stream()
                        .allMatch(task -> "APPROVED".equals(task.getStatus()));
                boolean anyRejected = tasks.stream()
                        .anyMatch(task -> "REJECTED".equals(task.getStatus()));
                boolean anyOnReview = tasks.stream()
                        .anyMatch(task -> "ON_REVIEW".equals(task.getStatus()));
                boolean anyCreated = tasks.stream()
                        .anyMatch(task -> "CREATED".equals(task.getStatus()));

                SprintStatus newStatus = null;
                
                // ПРИОРИТЕТ: REJECTED > ON_REVIEW > CREATED > APPROVED
                // Спринт может быть APPROVED только если ВСЕ задачи APPROVED
                if (anyRejected) {
                    // Есть отклоненные задачи -> спринт отклонен
                    newStatus = SprintStatus.REJECTED;
                } else if (anyOnReview) {
                    // Есть задачи на рассмотрении -> спринт на рассмотрении
                    newStatus = SprintStatus.ON_REVIEW;
                } else if (anyCreated) {
                    // Есть созданные задачи -> спринт создан
                    newStatus = SprintStatus.CREATED;
                } else if (allApproved) {
                    // Все задачи одобрены -> спринт одобрен
                    newStatus = SprintStatus.APPROVED;
                }

                if (newStatus != null && sprint.getStatus() != newStatus) {
                    SprintStatus oldStatus = sprint.getStatus();
                    sprint.setStatus(newStatus);
                    log.info("Sprint {} status automatically changed from {} to {}", 
                            id, oldStatus, newStatus);
                }
            }
            
            Sprint updatedSprint = sprintRepository.save(sprint);
            return convertToDto(updatedSprint);
            
        } catch (Exception e) {
            log.error("Failed to recalculate sprint {} status: {}", id, e.getMessage());
            throw new RuntimeException("Failed to recalculate sprint status", e);
        }
    }

    private SprintDto convertToDto(Sprint sprint) {
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

        // Fetch team name
        try {
            TeamDto team = teamServiceClient.getTeamById(sprint.getTeamId());
            dto.setTeamName(team.getName());
        } catch (Exception e) {
            log.warn("Could not fetch team name for team id {}: {}", sprint.getTeamId(), e.getMessage());
        }

        // Fetch creator name
        if (sprint.getCreatedBy() != null) {
            try {
                UserDto user = teamServiceClient.getUserById(sprint.getCreatedBy());
                dto.setCreatedByName(user.getName());
            } catch (Exception e) {
                log.warn("Could not fetch user name for user id {}: {}", sprint.getCreatedBy(), e.getMessage());
            }
        }

        return dto;
    }
}
