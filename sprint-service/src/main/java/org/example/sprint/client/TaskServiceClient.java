package org.example.sprint.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

@FeignClient(name = "task-service")
public interface TaskServiceClient {
    
    @GetMapping("/api/tasks/sprint/{sprintId}")
    List<TaskDto> getTasksBySprintId(@PathVariable Long sprintId);
}
