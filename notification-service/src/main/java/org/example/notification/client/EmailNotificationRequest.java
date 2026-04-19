package org.example.notification.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EmailNotificationRequest {
    private String userEmail;
    private String userName;
    private String message;
    private String type;
    private Long taskId;
}
