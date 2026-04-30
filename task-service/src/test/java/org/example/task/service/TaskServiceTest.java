package org.example.task.service;

import org.example.task.client.NotificationDto;
import org.example.task.client.NotificationServiceClient;
import org.example.task.client.SprintServiceClient;
import org.example.task.client.UserDto;
import org.example.task.client.UserServiceClient;
import org.example.task.dto.TaskDto;
import org.example.task.entity.Task;
import org.example.task.entity.TaskStatus;
import org.example.task.repository.TaskHistoryRepository;
import org.example.task.repository.TaskRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private TaskHistoryRepository taskHistoryRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private SprintServiceClient sprintServiceClient;

    @Mock
    private NotificationServiceClient notificationServiceClient;

    @InjectMocks
    private TaskService taskService;

    private Task testTask;
    private TaskDto testTaskDto;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        testTask = new Task();
        testTask.setId(1L);
        testTask.setTitle("Test Task");
        testTask.setDescription("Test Description");
        testTask.setSprintId(1L);
        testTask.setStatus(TaskStatus.CREATED);
        testTask.setAssignedTo(2L);
        testTask.setCreatedBy(1L);
        testTask.setCreatedAt(LocalDateTime.now());
        testTask.setUpdatedAt(LocalDateTime.now());

        testTaskDto = new TaskDto();
        testTaskDto.setTitle("Test Task");
        testTaskDto.setDescription("Test Description");
        testTaskDto.setSprintId(1L);
        testTaskDto.setStatus(TaskStatus.CREATED);
        testTaskDto.setAssignedTo(2L);
        testTaskDto.setCreatedBy(1L);

        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testGetAllTasks() {
        // Given
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findAll()).thenReturn(tasks);

        // When
        List<TaskDto> result = taskService.getAllTasks();

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTask.getTitle(), result.get(0).getTitle());
        verify(taskRepository).findAll();
    }

    @Test
    void testGetTaskById() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));

        // When
        TaskDto result = taskService.getTaskById(1L);

        // Then
        assertNotNull(result);
        assertEquals(testTask.getTitle(), result.getTitle());
        assertEquals(testTask.getDescription(), result.getDescription());
        verify(taskRepository).findById(1L);
    }

    @Test
    void testGetTaskByIdNotFound() {
        // Given
        when(taskRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            taskService.getTaskById(999L);
        });

        assertTrue(exception.getMessage().contains("Task not found"));
        verify(taskRepository).findById(999L);
    }

    @Test
    void testGetTasksBySprintId() {
        // Given
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findBySprintId(1L)).thenReturn(tasks);

        // When
        List<TaskDto> result = taskService.getTasksBySprintId(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testTask.getSprintId(), result.get(0).getSprintId());
        verify(taskRepository).findBySprintId(1L);
    }

    @Test
    void testGetTasksByStatus() {
        // Given
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findByStatus(TaskStatus.CREATED)).thenReturn(tasks);

        // When
        List<TaskDto> result = taskService.getTasksByStatus(TaskStatus.CREATED);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(TaskStatus.CREATED, result.get(0).getStatus());
        verify(taskRepository).findByStatus(TaskStatus.CREATED);
    }

    @Test
    void testGetTasksByAssignedUser() {
        // Given
        List<Task> tasks = Arrays.asList(testTask);
        when(taskRepository.findByAssignedTo(2L)).thenReturn(tasks);

        // When
        List<TaskDto> result = taskService.getTasksByAssignedUser(2L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(2L, result.get(0).getAssignedTo());
        verify(taskRepository).findByAssignedTo(2L);
    }

    @Test
    void testCreateTask() {
        // Given
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);
        when(userServiceClient.getUserById(anyLong())).thenReturn(testUser);
        doNothing().when(notificationServiceClient).createNotification(any(NotificationDto.class));

        // When
        TaskDto result = taskService.createTask(testTaskDto);

        // Then
        assertNotNull(result);
        assertEquals(testTaskDto.getTitle(), result.getTitle());
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void testUpdateTask() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        when(taskRepository.save(any(Task.class))).thenReturn(testTask);

        testTaskDto.setId(1L);
        testTaskDto.setTitle("Updated Task");

        // When
        TaskDto result = taskService.updateTask(1L, testTaskDto);

        // Then
        assertNotNull(result);
        verify(taskRepository).findById(1L);
        verify(taskRepository).save(any(Task.class));
    }

    @Test
    void testDeleteTask() {
        // Given
        when(taskRepository.findById(1L)).thenReturn(Optional.of(testTask));
        doNothing().when(taskRepository).delete(testTask);
        doNothing().when(sprintServiceClient).recalculateSprintStatus(anyLong());

        // When
        taskService.deleteTask(1L);

        // Then
        verify(taskRepository).findById(1L);
        verify(taskRepository).delete(testTask);
        verify(sprintServiceClient).recalculateSprintStatus(testTask.getSprintId());
    }
}
