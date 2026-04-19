package org.example.notification.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.notification.client.EmailNotificationRequest;
import org.example.notification.client.EmailServiceClient;
import org.example.notification.client.UserDto;
import org.example.notification.client.UserServiceClient;
import org.example.notification.dto.NotificationDto;
import org.example.notification.entity.Notification;
import org.example.notification.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserServiceClient userServiceClient;
    private final EmailServiceClient emailServiceClient;

    @Transactional(readOnly = true)
    public List<NotificationDto> getUserNotifications(Long userId) {
        return notificationRepository.findByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<NotificationDto> getUnreadNotifications(Long userId) {
        return notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public long getUnreadCount(Long userId) {
        return notificationRepository.countByUserIdAndIsReadFalse(userId);
    }

    @Transactional
    public NotificationDto createNotification(NotificationDto dto) {
        Notification notification = new Notification();
        notification.setUserId(dto.getUserId());
        notification.setMessage(dto.getMessage());
        notification.setType(dto.getType());
        notification.setRelatedEntityId(dto.getRelatedEntityId());
        notification.setIsRead(false);

        Notification saved = notificationRepository.save(notification);
        log.info("Created notification for user {}: {}", dto.getUserId(), dto.getMessage());
        
        // Send email notification
        sendEmailNotification(saved);
        
        return convertToDto(saved);
    }
    
    private void sendEmailNotification(Notification notification) {
        try {
            // Fetch user details
            UserDto user = userServiceClient.getUserById(notification.getUserId());
            
            if (user.getEmail() == null || user.getEmail().isEmpty()) {
                log.warn("User {} has no email, skipping email notification", notification.getUserId());
                return;
            }
            
            EmailNotificationRequest emailRequest = new EmailNotificationRequest();
            emailRequest.setUserEmail(user.getEmail());
            emailRequest.setUserName(user.getName());
            emailRequest.setMessage(notification.getMessage());
            emailRequest.setType(notification.getType());
            emailRequest.setTaskId(notification.getRelatedEntityId());
            
            emailServiceClient.sendNotificationEmail(emailRequest);
            log.info("Email notification sent to {}", user.getEmail());
        } catch (Exception e) {
            log.error("Failed to send email notification: {}", e.getMessage());
            // Don't fail the notification creation if email fails
        }
    }

    @Transactional
    public void markAsRead(Long notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new RuntimeException("Notification not found"));
        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        List<Notification> notifications = notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        notifications.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(notifications);
    }

    private NotificationDto convertToDto(Notification notification) {
        NotificationDto dto = new NotificationDto();
        dto.setId(notification.getId());
        dto.setUserId(notification.getUserId());
        dto.setMessage(notification.getMessage());
        dto.setType(notification.getType());
        dto.setRelatedEntityId(notification.getRelatedEntityId());
        dto.setIsRead(notification.getIsRead());
        dto.setCreatedAt(notification.getCreatedAt());
        return dto;
    }
}
