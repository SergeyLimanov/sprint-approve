package org.example.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task.client.NotificationDto;
import org.example.task.client.NotificationServiceClient;
import org.example.task.client.SprintServiceClient;
import org.example.task.client.UserDto;
import org.example.task.client.UserServiceClient;
import org.example.task.dto.TaskDto;
import org.example.task.entity.Task;
import org.example.task.entity.TaskStatus;
import org.example.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TaskService {
    private final TaskRepository taskRepository;
    private final UserServiceClient userServiceClient;
    private final SprintServiceClient sprintServiceClient;
    private final NotificationServiceClient notificationServiceClient;

    @Transactional(readOnly = true)
    public List<TaskDto> getAllTasks() {
        return taskRepository.findAll().stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public TaskDto getTaskById(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        return convertToDto(task);
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksBySprintId(Long sprintId) {
        return taskRepository.findBySprintId(sprintId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByStatus(TaskStatus status) {
        return taskRepository.findByStatus(status).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<TaskDto> getTasksByAssignedTo(Long userId) {
        return taskRepository.findByAssignedTo(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional
    public TaskDto createTask(TaskDto taskDto) {
        Task task = new Task();
        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setSprintId(taskDto.getSprintId());
        task.setStatus(TaskStatus.CREATED);
        task.setAssignedTo(taskDto.getAssignedTo());
        task.setApproverId(taskDto.getApproverId());
        task.setCreatedBy(taskDto.getCreatedBy());

        Task savedTask = taskRepository.save(task);
        
        // Send notification to approver
        if (savedTask.getApproverId() != null) {
            sendNotification(
                savedTask.getApproverId(),
                "Вам назначена новая задача: " + savedTask.getTitle(),
                "TASK_ASSIGNED",
                savedTask.getId()
            );
        }
        
        // Send notification to assignee
        if (savedTask.getAssignedTo() != null && !savedTask.getAssignedTo().equals(savedTask.getApproverId())) {
            sendNotification(
                savedTask.getAssignedTo(),
                "Вам назначена задача: " + savedTask.getTitle(),
                "TASK_ASSIGNED",
                savedTask.getId()
            );
        }
        
        // Recalculate sprint status
        recalculateSprintStatus(savedTask.getSprintId());
        
        return convertToDto(savedTask);
    }

    @Transactional
    public TaskDto updateTask(Long id, TaskDto taskDto) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        task.setTitle(taskDto.getTitle());
        task.setDescription(taskDto.getDescription());
        task.setAssignedTo(taskDto.getAssignedTo());
        task.setApproverId(taskDto.getApproverId());

        Task updatedTask = taskRepository.save(task);
        return convertToDto(updatedTask);
    }

    @Transactional
    public TaskDto submitForReview(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        if (task.getStatus() != TaskStatus.CREATED && task.getStatus() != TaskStatus.REJECTED) {
            throw new RuntimeException("Only tasks with CREATED or REJECTED status can be submitted for review");
        }

        boolean isResubmission = task.getStatus() == TaskStatus.REJECTED;
        task.setStatus(TaskStatus.ON_REVIEW);
        Task updatedTask = taskRepository.save(task);
        
        // Send notification to approver
        if (task.getApproverId() != null) {
            String message = isResubmission 
                ? "Задача \"" + task.getTitle() + "\" повторно отправлена на рассмотрение"
                : "Задача \"" + task.getTitle() + "\" отправлена на рассмотрение";
            
            sendNotification(
                task.getApproverId(),
                message,
                "TASK_SUBMITTED_FOR_REVIEW",
                task.getId()
            );
        }
        
        // Recalculate sprint status
        recalculateSprintStatus(task.getSprintId());
        
        return convertToDto(updatedTask);
    }

    @Transactional
    public TaskDto approveTask(Long id, Long approverId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        // Check approver role
        try {
            UserDto approver = userServiceClient.getUserById(approverId);
            if (!"APPROVER".equals(approver.getRole()) && 
                !"TEAM_LEAD".equals(approver.getRole()) && 
                !"MANAGER".equals(approver.getRole())) {
                throw new RuntimeException("Only APPROVER, TEAM_LEAD or MANAGER can approve tasks. Your role: " + approver.getRole());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not verify approver role: {}", e.getMessage());
            throw new RuntimeException("Could not verify approver permissions");
        }

        if (task.getApproverId() != null && !task.getApproverId().equals(approverId)) {
            throw new RuntimeException("Only the assigned approver can approve this task");
        }

        task.setStatus(TaskStatus.APPROVED);
        Task updatedTask = taskRepository.save(task);

        // Send notification to task creator
        if (task.getCreatedBy() != null) {
            sendNotification(
                task.getCreatedBy(),
                "Задача \"" + task.getTitle() + "\" одобрена",
                "TASK_APPROVED",
                task.getId()
            );
        }
        
        // Send notification to assignee
        if (task.getAssignedTo() != null && !task.getAssignedTo().equals(task.getCreatedBy())) {
            sendNotification(
                task.getAssignedTo(),
                "Задача \"" + task.getTitle() + "\" одобрена",
                "TASK_APPROVED",
                task.getId()
            );
        }

        // Recalculate sprint status
        recalculateSprintStatus(task.getSprintId());

        return convertToDto(updatedTask);
    }

    @Transactional
    public TaskDto rejectTask(Long id, Long approverId) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));

        // Check approver role
        try {
            UserDto approver = userServiceClient.getUserById(approverId);
            if (!"APPROVER".equals(approver.getRole()) && 
                !"TEAM_LEAD".equals(approver.getRole()) && 
                !"MANAGER".equals(approver.getRole())) {
                throw new RuntimeException("Only APPROVER, TEAM_LEAD or MANAGER can reject tasks. Your role: " + approver.getRole());
            }
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            log.error("Could not verify approver role: {}", e.getMessage());
            throw new RuntimeException("Could not verify approver permissions");
        }

        if (task.getApproverId() != null && !task.getApproverId().equals(approverId)) {
            throw new RuntimeException("Only the assigned approver can reject this task");
        }

        task.setStatus(TaskStatus.REJECTED);
        Task updatedTask = taskRepository.save(task);
        
        // Send notification to task creator
        if (task.getCreatedBy() != null) {
            sendNotification(
                task.getCreatedBy(),
                "Задача \"" + task.getTitle() + "\" отклонена",
                "TASK_REJECTED",
                task.getId()
            );
        }
        
        // Send notification to assignee
        if (task.getAssignedTo() != null && !task.getAssignedTo().equals(task.getCreatedBy())) {
            sendNotification(
                task.getAssignedTo(),
                "Задача \"" + task.getTitle() + "\" отклонена",
                "TASK_REJECTED",
                task.getId()
            );
        }
        
        // Recalculate sprint status
        recalculateSprintStatus(task.getSprintId());
        
        return convertToDto(updatedTask);
    }

    @Transactional
    public void deleteTask(Long id) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + id));
        
        Long sprintId = task.getSprintId();
        taskRepository.deleteById(id);
        
        // Recalculate sprint status after task deletion
        recalculateSprintStatus(sprintId);
    }

    private void recalculateSprintStatus(Long sprintId) {
        if (sprintId == null) {
            return;
        }
        
        try {
            sprintServiceClient.recalculateSprintStatus(sprintId);
            log.info("Sprint {} status recalculated based on tasks", sprintId);
        } catch (Exception e) {
            log.error("Failed to recalculate sprint {} status: {}", sprintId, e.getMessage());
        }
    }
    
    private void sendNotification(Long userId, String message, String type, Long relatedEntityId) {
        try {
            NotificationDto notification = new NotificationDto();
            notification.setUserId(userId);
            notification.setMessage(message);
            notification.setType(type);
            notification.setRelatedEntityId(relatedEntityId);
            
            notificationServiceClient.createNotification(notification);
            log.info("Notification sent to user {}: {}", userId, message);
        } catch (Exception e) {
            log.error("Failed to send notification to user {}: {}", userId, e.getMessage());
        }
    }

    private TaskDto convertToDto(Task task) {
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

        // Fetch user names
        if (task.getAssignedTo() != null) {
            try {
                UserDto user = userServiceClient.getUserById(task.getAssignedTo());
                dto.setAssignedToName(user.getName());
            } catch (Exception e) {
                log.warn("Could not fetch user name for user id {}: {}", task.getAssignedTo(), e.getMessage());
            }
        }

        if (task.getApproverId() != null) {
            try {
                UserDto user = userServiceClient.getUserById(task.getApproverId());
                dto.setApproverName(user.getName());
            } catch (Exception e) {
                log.warn("Could not fetch approver name for user id {}: {}", task.getApproverId(), e.getMessage());
            }
        }

        if (task.getCreatedBy() != null) {
            try {
                UserDto user = userServiceClient.getUserById(task.getCreatedBy());
                dto.setCreatedByName(user.getName());
            } catch (Exception e) {
                log.warn("Could not fetch creator name for user id {}: {}", task.getCreatedBy(), e.getMessage());
            }
        }

        return dto;
    }
}
