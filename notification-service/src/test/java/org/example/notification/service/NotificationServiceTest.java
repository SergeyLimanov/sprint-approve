package org.example.notification.service;

import org.example.notification.client.EmailNotificationRequest;
import org.example.notification.client.EmailServiceClient;
import org.example.notification.client.UserDto;
import org.example.notification.client.UserServiceClient;
import org.example.notification.dto.NotificationDto;
import org.example.notification.entity.Notification;
import org.example.notification.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserServiceClient userServiceClient;

    @Mock
    private EmailServiceClient emailServiceClient;

    @InjectMocks
    private NotificationService notificationService;

    private Notification testNotification;
    private NotificationDto testNotificationDto;
    private UserDto testUser;

    @BeforeEach
    void setUp() {
        testNotification = new Notification();
        testNotification.setId(1L);
        testNotification.setUserId(1L);
        testNotification.setMessage("Test notification");
        testNotification.setType("TASK_ASSIGNED");
        testNotification.setRelatedEntityId(1L);
        testNotification.setIsRead(false);
        testNotification.setCreatedAt(LocalDateTime.now());

        testNotificationDto = new NotificationDto();
        testNotificationDto.setUserId(1L);
        testNotificationDto.setMessage("Test notification");
        testNotificationDto.setType("TASK_ASSIGNED");
        testNotificationDto.setRelatedEntityId(1L);

        testUser = new UserDto();
        testUser.setId(1L);
        testUser.setName("Test User");
        testUser.setEmail("test@example.com");
    }

    @Test
    void testGetUserNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdOrderByCreatedAtDesc(1L)).thenReturn(notifications);

        // When
        List<NotificationDto> result = notificationService.getUserNotifications(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(testNotification.getMessage(), result.get(0).getMessage());
        verify(notificationRepository).findByUserIdOrderByCreatedAtDesc(1L);
    }

    @Test
    void testGetUnreadNotifications() {
        // Given
        List<Notification> notifications = Arrays.asList(testNotification);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(notifications);

        // When
        List<NotificationDto> result = notificationService.getUnreadNotifications(1L);

        // Then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertFalse(result.get(0).getIsRead());
        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L);
    }

    @Test
    void testGetUnreadCount() {
        // Given
        when(notificationRepository.countByUserIdAndIsReadFalse(1L)).thenReturn(5L);

        // When
        long count = notificationService.getUnreadCount(1L);

        // Then
        assertEquals(5L, count);
        verify(notificationRepository).countByUserIdAndIsReadFalse(1L);
    }

    @Test
    void testCreateNotificationWithEmail() {
        // Given
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        // Email sending is currently disabled
        // when(userServiceClient.getUserById(1L)).thenReturn(testUser);
        // doNothing().when(emailServiceClient).sendNotificationEmail(any(EmailNotificationRequest.class));

        // When
        NotificationDto result = notificationService.createNotification(testNotificationDto);

        // Then
        assertNotNull(result);
        assertEquals(testNotificationDto.getMessage(), result.getMessage());
        assertEquals(testNotificationDto.getType(), result.getType());
        verify(notificationRepository).save(any(Notification.class));
        // Email sending is currently disabled
        // verify(userServiceClient).getUserById(1L);
        // verify(emailServiceClient).sendNotificationEmail(any(EmailNotificationRequest.class));
    }

    @Test
    void testCreateNotificationWithoutEmail() {
        // Given
        UserDto userWithoutEmail = new UserDto();
        userWithoutEmail.setId(1L);
        userWithoutEmail.setName("Test User");
        userWithoutEmail.setEmail(null);

        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);
        // Email sending is currently disabled
        // when(userServiceClient.getUserById(1L)).thenReturn(userWithoutEmail);

        // When
        NotificationDto result = notificationService.createNotification(testNotificationDto);

        // Then
        assertNotNull(result);
        verify(notificationRepository).save(any(Notification.class));
        // Email sending is currently disabled
        // verify(userServiceClient).getUserById(1L);
        verify(emailServiceClient, never()).sendNotificationEmail(any(EmailNotificationRequest.class));
    }

    @Test
    void testMarkAsRead() {
        // Given
        when(notificationRepository.findById(1L)).thenReturn(Optional.of(testNotification));
        when(notificationRepository.save(any(Notification.class))).thenReturn(testNotification);

        // When
        notificationService.markAsRead(1L);

        // Then
        verify(notificationRepository).findById(1L);
        verify(notificationRepository).save(any(Notification.class));
    }

    @Test
    void testMarkAsReadNotFound() {
        // Given
        when(notificationRepository.findById(999L)).thenReturn(Optional.empty());

        // When & Then
        RuntimeException exception = assertThrows(RuntimeException.class, () -> {
            notificationService.markAsRead(999L);
        });

        assertTrue(exception.getMessage().contains("Notification not found"));
        verify(notificationRepository).findById(999L);
        verify(notificationRepository, never()).save(any(Notification.class));
    }

    @Test
    void testMarkAllAsRead() {
        // Given
        Notification notification1 = new Notification();
        notification1.setId(1L);
        notification1.setUserId(1L);
        notification1.setIsRead(false);

        Notification notification2 = new Notification();
        notification2.setId(2L);
        notification2.setUserId(1L);
        notification2.setIsRead(false);

        List<Notification> notifications = Arrays.asList(notification1, notification2);
        when(notificationRepository.findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L))
                .thenReturn(notifications);
        when(notificationRepository.saveAll(anyList())).thenReturn(notifications);

        // When
        notificationService.markAllAsRead(1L);

        // Then
        verify(notificationRepository).findByUserIdAndIsReadFalseOrderByCreatedAtDesc(1L);
        verify(notificationRepository).saveAll(anyList());
    }
}
