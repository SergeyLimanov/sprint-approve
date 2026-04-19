package org.example.notification.dto;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class NotificationDto {
    private Long id;
    private Long userId;
    private String message;
    private String type;
    private Long relatedEntityId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
