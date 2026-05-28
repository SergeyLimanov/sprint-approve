package org.example.task.service;

import org.example.task.dto.TaskDto;
import org.example.task.dto.TaskHistoryDto;
import org.example.task.entity.Task;
import org.example.task.entity.TaskStatus;

import java.util.List;

public interface ITaskService {
    List<TaskDto> getAllTasks();
    
    TaskDto getTaskById(Long id);
    
    Task getTaskEntityById(Long id);
    
    List<TaskDto> getTasksBySprintId(Long sprintId);
    
    List<TaskDto> getTasksByStatus(TaskStatus status);
    
    List<TaskDto> getTasksByAssignedTo(Long userId);
    
    TaskDto createTask(TaskDto taskDto);
    
    TaskDto updateTask(Long id, TaskDto taskDto);
    
    TaskDto submitForReview(Long id, Long userId, String comment);
    
    TaskDto approveTask(Long id, Long approverId, String comment);
    
    TaskDto rejectTask(Long id, Long approverId, String comment);
    
    void deleteTask(Long id);
    
    List<TaskHistoryDto> getTaskHistory(Long taskId);
}
