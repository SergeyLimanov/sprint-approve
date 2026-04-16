package org.example.task.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "sprint-service")
public interface SprintServiceClient {
    
    @GetMapping("/api/sprints/{id}")
    SprintDto getSprintById(@PathVariable Long id);
    
    @PatchMapping("/api/sprints/{id}/approve")
    SprintDto approveSprint(@PathVariable Long id);
    
    @PatchMapping("/api/sprints/{id}/recalculate-status")
    SprintDto recalculateSprintStatus(@PathVariable Long id);
}
