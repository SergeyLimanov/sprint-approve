package org.example.notification.client;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "email-service", url = "${email.service.url:http://localhost:3001}")
public interface EmailServiceClient {
    
    @PostMapping("/api/email/notification")
    void sendNotificationEmail(@RequestBody EmailNotificationRequest request);
}
