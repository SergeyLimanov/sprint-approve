package org.example.notification.service;

import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
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
        // Fetch user details with resilience
        UserDto user = getUserByIdWithResilience(notification.getUserId());
        
        if (user == null || user.getEmail() == null || user.getEmail().isEmpty()) {
            log.warn("User {} has no email, skipping email notification", notification.getUserId());
            return;
        }
        
        EmailNotificationRequest emailRequest = new EmailNotificationRequest();
        emailRequest.setUserEmail(user.getEmail());
        emailRequest.setUserName(user.getName());
        emailRequest.setMessage(notification.getMessage());
        emailRequest.setType(notification.getType());
        emailRequest.setTaskId(notification.getRelatedEntityId());
        
        // Send email with resilience
        sendEmailWithResilience(emailRequest);
    }
    
    @CircuitBreaker(name = "userService", fallbackMethod = "getUserByIdFallback")
    @Retry(name = "userService")
    private UserDto getUserByIdWithResilience(Long userId) {
        return userServiceClient.getUserById(userId);
    }
    
    private UserDto getUserByIdFallback(Long userId, Exception e) {
        log.error("Failed to fetch user {} after retries: {}", userId, e.getMessage());
        return null; // Skip email notification if user service is down
    }
    
    @CircuitBreaker(name = "emailService", fallbackMethod = "sendEmailFallback")
    @Retry(name = "emailService")
    private void sendEmailWithResilience(EmailNotificationRequest request) {
        emailServiceClient.sendNotificationEmail(request);
        log.info("Email notification sent to {}", request.getUserEmail());
    }
    
    private void sendEmailFallback(EmailNotificationRequest request, Exception e) {
        log.warn("Failed to send email to {} after retries: {}. Email notification lost.", 
                 request.getUserEmail(), e.getMessage());
        // TODO: Save to pending_emails table for later retry
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
