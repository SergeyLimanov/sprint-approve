package org.example.task.client.dto;

import lombok.Data;

@Data
public class NotificationRequest {
    private Long userId;
    private String message;
    private String type;
    private Long relatedEntityId;
}
