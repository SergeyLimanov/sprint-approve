package org.example.task.client;

import lombok.Data;

@Data
public class NotificationDto {
    private Long userId;
    private String message;
    private String type;
    private Long relatedEntityId;
}
