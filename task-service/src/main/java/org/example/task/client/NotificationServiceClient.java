package org.example.task.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    
    @PostMapping("/api/notifications")
    NotificationDto createNotification(@RequestBody NotificationDto dto);
}
