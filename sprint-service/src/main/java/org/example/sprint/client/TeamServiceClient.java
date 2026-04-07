package org.example.sprint.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "team-service")
public interface TeamServiceClient {
    
    @GetMapping("/api/teams/{id}")
    TeamDto getTeamById(@PathVariable Long id);
    
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable Long id);
}
