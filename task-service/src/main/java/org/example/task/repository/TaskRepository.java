package org.example.task.repository;

import org.example.task.entity.Task;
import org.example.task.entity.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TaskRepository extends JpaRepository<Task, Long> {
    List<Task> findBySprintId(Long sprintId);
    List<Task> findByStatus(TaskStatus status);
    List<Task> findByAssignedTo(Long assignedTo);
    List<Task> findBySprintIdAndStatus(Long sprintId, TaskStatus status);
}
