package org.example.task.client;

import org.example.task.client.dto.SprintResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "sprint-service")
public interface SprintServiceClient {
    
    @GetMapping("/api/sprints/{id}")
    SprintResponse getSprintById(@PathVariable Long id);
    
    @PatchMapping("/api/sprints/{id}/approve")
    SprintResponse approveSprint(@PathVariable Long id);
    
    @PatchMapping("/api/sprints/{id}/recalculate-status")
    SprintResponse recalculateSprintStatus(@PathVariable Long id);
}
