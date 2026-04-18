# 🔔 Firebase Cloud Messaging Integration Guide

## 📋 Обзор

Интеграция FCM для отправки push-уведомлений в веб-приложение Sprint Approve.

### Ваш стек:
- **Frontend**: React 18 + TypeScript + Vite + TailwindCSS
- **Backend**: Spring Boot 3.2.0 + Spring Cloud
- **Интеграции**: OpenFeign между микросервисами

---

## 🎯 Сценарии уведомлений

### 1. **Для APPROVER (роль)**
| Событие | Когда | Кто получает |
|---------|-------|--------------|
| Новая задача в спринте | `POST /api/tasks` | Все APPROVER команды |
| Задача на рассмотрении | `PATCH /api/tasks/{id}/submit` | Назначенный approver |
| Новый артефакт | `POST /api/artifacts/upload` | Approver задачи |

### 2. **Для СОЗДАТЕЛЯ задачи**
| Событие | Когда | Кто получает |
|---------|-------|--------------|
| Новый комментарий | `POST /api/comments` | Создатель задачи (createdBy) |
| Задача одобрена | `PATCH /api/tasks/{id}/approve` | Создатель задачи |
| Задача отклонена | `PATCH /api/tasks/{id}/reject` | Создатель задачи |

### 3. **Для ИСПОЛНИТЕЛЯ**
| Событие | Когда | Кто получает |
|---------|-------|--------------|
| Задача назначена | `POST /api/tasks` или `PUT /api/tasks/{id}` | Исполнитель (assignedTo) |

---

## 🏗️ Архитектура

```
Frontend (React)
    ↓ (FCM Token)
Notification Service
    ↑ (Events via Feign)
Task Service / Sprint Service
```

### Поток уведомлений:
```
1. Task Service → создает задачу
2. Task Service → вызывает Notification Service (Feign)
3. Notification Service → отправляет FCM уведомление
4. FCM → доставляет push в браузер пользователя
```

---

## 📦 Шаг 1: Настройка Firebase

### 1.1 Создайте Firebase проект

1. Перейдите на https://console.firebase.google.com/
2. Нажмите **Add project**
3. Имя проекта: `sprint-approve`
4. Отключите Google Analytics (не нужно для FCM)

### 1.2 Добавьте Web App

1. В Firebase Console → **Project Settings**
2. Вкладка **General** → **Your apps**
3. Нажмите **Web** (</> иконка)
4. App nickname: `Sprint Approve Web`
5. Включите **Firebase Hosting** (опционально)
6. Скопируйте **Firebase Config**:

```javascript
const firebaseConfig = {
  apiKey: "AIzaSy...",
  authDomain: "sprint-approve.firebaseapp.com",
  projectId: "sprint-approve",
  storageBucket: "sprint-approve.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123"
};
```

### 1.3 Получите Server Key (для backend)

1. **Project Settings** → **Cloud Messaging**
2. Вкладка **Cloud Messaging API (Legacy)**
3. Скопируйте **Server key**
4. **ИЛИ** (рекомендуется) скачайте **Service Account Key**:
   - **Project Settings** → **Service accounts**
   - **Generate new private key**
   - Сохраните JSON файл как `firebase-service-account.json`

---

## 📦 Шаг 2: Backend - Notification Service

### 2.1 Структура сервиса

```
notification-service/
├── src/main/java/org/example/notification/
│   ├── NotificationServiceApplication.java
│   ├── config/
│   │   └── FirebaseConfig.java
│   ├── controller/
│   │   └── NotificationController.java
│   ├── service/
│   │   ├── FcmService.java
│   │   └── UserTokenService.java
│   ├── entity/
│   │   └── UserFcmToken.java
│   ├── repository/
│   │   └── UserFcmTokenRepository.java
│   └── dto/
│       ├── NotificationRequest.java
│       └── RegisterTokenRequest.java
├── src/main/resources/
│   ├── application.yml
│   └── firebase-service-account.json  ← Положите сюда
└── pom.xml
```

### 2.2 Создайте сущность UserFcmToken

```java
// UserFcmToken.java
@Entity
@Table(name = "user_fcm_tokens")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserFcmToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "fcm_token", nullable = false, length = 500)
    private String fcmToken;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
```

### 2.3 Firebase Configuration

```java
// FirebaseConfig.java
@Configuration
public class FirebaseConfig {

    @PostConstruct
    public void initialize() {
        try {
            InputStream serviceAccount = getClass()
                .getClassLoader()
                .getResourceAsStream("firebase-service-account.json");

            if (serviceAccount == null) {
                throw new RuntimeException("Firebase service account file not found");
            }

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccount))
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase initialized successfully");
            }
        } catch (Exception e) {
            log.error("Failed to initialize Firebase", e);
            throw new RuntimeException("Firebase initialization failed", e);
        }
    }
}
```

### 2.4 FCM Service

```java
// FcmService.java
@Service
@RequiredArgsConstructor
@Slf4j
public class FcmService {
    private final UserTokenService userTokenService;

    public void sendNotification(Long userId, String title, String body, Map<String, String> data) {
        String fcmToken = userTokenService.getUserToken(userId);
        if (fcmToken == null) {
            log.warn("No FCM token found for user {}", userId);
            return;
        }

        try {
            Message message = Message.builder()
                .setToken(fcmToken)
                .setNotification(Notification.builder()
                    .setTitle(title)
                    .setBody(body)
                    .build())
                .putAllData(data != null ? data : Map.of())
                .setWebpushConfig(WebpushConfig.builder()
                    .setNotification(WebpushNotification.builder()
                        .setIcon("/logo.png")
                        .setBadge("/badge.png")
                        .build())
                    .build())
                .build();

            String response = FirebaseMessaging.getInstance().send(message);
            log.info("Successfully sent FCM message to user {}: {}", userId, response);
        } catch (FirebaseMessagingException e) {
            log.error("Failed to send FCM message to user {}", userId, e);
            
            // Если токен невалидный - удаляем
            if (e.getErrorCode().equals("INVALID_ARGUMENT") || 
                e.getErrorCode().equals("UNREGISTERED")) {
                userTokenService.deleteUserToken(userId);
            }
        }
    }

    // Специфичные методы для разных событий
    
    public void sendTaskAssignedNotification(Long userId, Long taskId, String taskTitle) {
        sendNotification(
            userId,
            "Новая задача назначена",
            "Вам назначена задача: " + taskTitle,
            Map.of(
                "type", "TASK_ASSIGNED",
                "taskId", taskId.toString()
            )
        );
    }

    public void sendTaskApprovedNotification(Long userId, Long taskId, String taskTitle) {
        sendNotification(
            userId,
            "Задача одобрена ✅",
            "Ваша задача \"" + taskTitle + "\" одобрена",
            Map.of(
                "type", "TASK_APPROVED",
                "taskId", taskId.toString()
            )
        );
    }

    public void sendTaskRejectedNotification(Long userId, Long taskId, String taskTitle) {
        sendNotification(
            userId,
            "Задача отклонена ❌",
            "Ваша задача \"" + taskTitle + "\" отклонена",
            Map.of(
                "type", "TASK_REJECTED",
                "taskId", taskId.toString()
            )
        );
    }

    public void sendNewCommentNotification(Long userId, Long taskId, String taskTitle, String authorName) {
        sendNotification(
            userId,
            "Новый комментарий 💬",
            authorName + " оставил комментарий к задаче \"" + taskTitle + "\"",
            Map.of(
                "type", "NEW_COMMENT",
                "taskId", taskId.toString()
            )
        );
    }

    public void sendNewArtifactNotification(Long userId, Long taskId, String taskTitle, String artifactName) {
        sendNotification(
            userId,
            "Новый артефакт 📎",
            "К задаче \"" + taskTitle + "\" прикреплен файл: " + artifactName,
            Map.of(
                "type", "NEW_ARTIFACT",
                "taskId", taskId.toString()
            )
        );
    }

    public void sendTaskForReviewNotification(Long approverId, Long taskId, String taskTitle) {
        sendNotification(
            approverId,
            "Требуется проверка 🔍",
            "Задача \"" + taskTitle + "\" отправлена на рассмотрение",
            Map.of(
                "type", "TASK_FOR_REVIEW",
                "taskId", taskId.toString()
            )
        );
    }
}
```

### 2.5 Controller для регистрации токенов

```java
// NotificationController.java
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "FCM Notifications API")
public class NotificationController {
    private final UserTokenService userTokenService;
    private final FcmService fcmService;

    @PostMapping("/register-token")
    @Operation(summary = "Register FCM token for user")
    public ResponseEntity<Void> registerToken(@RequestBody RegisterTokenRequest request) {
        userTokenService.saveUserToken(request.getUserId(), request.getFcmToken());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/token/{userId}")
    @Operation(summary = "Delete FCM token")
    public ResponseEntity<Void> deleteToken(@PathVariable Long userId) {
        userTokenService.deleteUserToken(userId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/send")
    @Operation(summary = "Send notification (for testing)")
    public ResponseEntity<Void> sendNotification(@RequestBody NotificationRequest request) {
        fcmService.sendNotification(
            request.getUserId(),
            request.getTitle(),
            request.getBody(),
            request.getData()
        );
        return ResponseEntity.ok().build();
    }
}
```

---

## 📦 Шаг 3: Интеграция с Task Service

### 3.1 Добавьте Feign Client в task-service

```java
// NotificationServiceClient.java
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    
    @PostMapping("/api/notifications/task-assigned")
    void notifyTaskAssigned(
        @RequestParam Long userId,
        @RequestParam Long taskId,
        @RequestParam String taskTitle
    );

    @PostMapping("/api/notifications/task-approved")
    void notifyTaskApproved(
        @RequestParam Long userId,
        @RequestParam Long taskId,
        @RequestParam String taskTitle
    );

    @PostMapping("/api/notifications/task-rejected")
    void notifyTaskRejected(
        @RequestParam Long userId,
        @RequestParam Long taskId,
        @RequestParam String taskTitle
    );

    @PostMapping("/api/notifications/new-comment")
    void notifyNewComment(
        @RequestParam Long userId,
        @RequestParam Long taskId,
        @RequestParam String taskTitle,
        @RequestParam String authorName
    );

    @PostMapping("/api/notifications/new-artifact")
    void notifyNewArtifact(
        @RequestParam Long userId,
        @RequestParam Long taskId,
        @RequestParam String taskTitle,
        @RequestParam String artifactName
    );

    @PostMapping("/api/notifications/task-for-review")
    void notifyTaskForReview(
        @RequestParam Long approverId,
        @RequestParam Long taskId,
        @RequestParam String taskTitle
    );
}
```

### 3.2 Обновите TaskService

```java
@Service
@RequiredArgsConstructor
public class TaskService {
    private final TaskRepository taskRepository;
    private final NotificationServiceClient notificationClient;

    public TaskDto createTask(TaskDto taskDto) {
        Task task = // ... создание задачи
        Task saved = taskRepository.save(task);

        // Уведомляем исполнителя
        if (saved.getAssignedTo() != null) {
            try {
                notificationClient.notifyTaskAssigned(
                    saved.getAssignedTo(),
                    saved.getId(),
                    saved.getTitle()
                );
            } catch (Exception e) {
                log.error("Failed to send notification", e);
            }
        }

        return toDto(saved);
    }

    public TaskDto approveTask(Long id, Long approverId) {
        Task task = findById(id);
        task.setStatus(TaskStatus.APPROVED);
        task.setApproverId(approverId);
        Task updated = taskRepository.save(task);

        // Уведомляем создателя
        if (task.getCreatedBy() != null) {
            try {
                notificationClient.notifyTaskApproved(
                    task.getCreatedBy(),
                    task.getId(),
                    task.getTitle()
                );
            } catch (Exception e) {
                log.error("Failed to send notification", e);
            }
        }

        return toDto(updated);
    }

    public TaskDto submitForReview(Long id) {
        Task task = findById(id);
        task.setStatus(TaskStatus.ON_REVIEW);
        Task updated = taskRepository.save(task);

        // Уведомляем аппрувера
        if (task.getApproverId() != null) {
            try {
                notificationClient.notifyTaskForReview(
                    task.getApproverId(),
                    task.getId(),
                    task.getTitle()
                );
            } catch (Exception e) {
                log.error("Failed to send notification", e);
            }
        }

        return toDto(updated);
    }
}
```

---

## 📦 Шаг 4: Frontend - React Integration

### 4.1 Установите Firebase SDK

```bash
cd frontend
npm install firebase
```

### 4.2 Создайте firebase.ts

```typescript
// src/firebase/firebase.ts
import { initializeApp } from 'firebase/app';
import { getMessaging, getToken, onMessage } from 'firebase/messaging';

const firebaseConfig = {
  apiKey: "YOUR_API_KEY",
  authDomain: "sprint-approve.firebaseapp.com",
  projectId: "sprint-approve",
  storageBucket: "sprint-approve.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123"
};

const app = initializeApp(firebaseConfig);
const messaging = getMessaging(app);

export { messaging };

// Запрос разрешения и получение токена
export const requestNotificationPermission = async (): Promise<string | null> => {
  try {
    const permission = await Notification.requestPermission();
    
    if (permission === 'granted') {
      const token = await getToken(messaging, {
        vapidKey: 'YOUR_VAPID_KEY' // Получите в Firebase Console
      });
      
      console.log('FCM Token:', token);
      return token;
    } else {
      console.log('Notification permission denied');
      return null;
    }
  } catch (error) {
    console.error('Error getting FCM token:', error);
    return null;
  }
};

// Слушаем входящие уведомления (когда приложение открыто)
export const onMessageListener = () =>
  new Promise((resolve) => {
    onMessage(messaging, (payload) => {
      console.log('Message received:', payload);
      resolve(payload);
    });
  });
```

### 4.3 Создайте Service Worker

```javascript
// public/firebase-messaging-sw.js
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-app-compat.js');
importScripts('https://www.gstatic.com/firebasejs/10.7.1/firebase-messaging-compat.js');

firebase.initializeApp({
  apiKey: "YOUR_API_KEY",
  authDomain: "sprint-approve.firebaseapp.com",
  projectId: "sprint-approve",
  storageBucket: "sprint-approve.appspot.com",
  messagingSenderId: "123456789",
  appId: "1:123456789:web:abc123"
});

const messaging = firebase.messaging();

// Обработка фоновых уведомлений
messaging.onBackgroundMessage((payload) => {
  console.log('Background message received:', payload);
  
  const notificationTitle = payload.notification.title;
  const notificationOptions = {
    body: payload.notification.body,
    icon: '/logo.png',
    badge: '/badge.png',
    data: payload.data
  };

  self.registration.showNotification(notificationTitle, notificationOptions);
});

// Обработка клика по уведомлению
self.addEventListener('notificationclick', (event) => {
  event.notification.close();
  
  const taskId = event.notification.data?.taskId;
  if (taskId) {
    event.waitUntil(
      clients.openWindow(`/tasks/${taskId}`)
    );
  }
});
```

### 4.4 Компонент для управления уведомлениями

```typescript
// src/components/NotificationManager.tsx
import { useEffect, useState } from 'react';
import { requestNotificationPermission, onMessageListener } from '../firebase/firebase';
import { Bell, BellOff } from 'lucide-react';
import axios from 'axios';

export const NotificationManager = ({ userId }: { userId: number }) => {
  const [isEnabled, setIsEnabled] = useState(false);
  const [loading, setLoading] = useState(false);

  useEffect(() => {
    // Проверяем, есть ли разрешение
    setIsEnabled(Notification.permission === 'granted');

    // Слушаем входящие уведомления
    onMessageListener()
      .then((payload: any) => {
        console.log('Notification received:', payload);
        
        // Показываем toast уведомление
        showToast(payload.notification.title, payload.notification.body);
        
        // Можно обновить список задач или показать badge
      })
      .catch((err) => console.error('Failed to listen for messages:', err));
  }, []);

  const enableNotifications = async () => {
    setLoading(true);
    
    const token = await requestNotificationPermission();
    
    if (token) {
      // Отправляем токен на backend
      try {
        await axios.post('/api/notifications/register-token', {
          userId,
          fcmToken: token
        });
        
        setIsEnabled(true);
        alert('Уведомления включены!');
      } catch (error) {
        console.error('Failed to register token:', error);
        alert('Ошибка регистрации токена');
      }
    }
    
    setLoading(false);
  };

  const disableNotifications = async () => {
    try {
      await axios.delete(`/api/notifications/token/${userId}`);
      setIsEnabled(false);
      alert('Уведомления отключены');
    } catch (error) {
      console.error('Failed to delete token:', error);
    }
  };

  return (
    <button
      onClick={isEnabled ? disableNotifications : enableNotifications}
      disabled={loading}
      className="flex items-center gap-2 px-4 py-2 rounded-lg bg-blue-500 text-white hover:bg-blue-600"
    >
      {isEnabled ? <Bell size={20} /> : <BellOff size={20} />}
      {loading ? 'Загрузка...' : isEnabled ? 'Отключить уведомления' : 'Включить уведомления'}
    </button>
  );
};

function showToast(title: string, body: string) {
  // Реализуйте toast уведомление (можно использовать react-hot-toast)
  console.log(`Toast: ${title} - ${body}`);
}
```

---

## 📦 Шаг 5: Docker Compose

```yaml
# docker-compose.yml
services:
  notification-service:
    build: ./notification-service
    container_name: notification-service
    ports:
      - "8085:8085"
    environment:
      - SPRING_DATASOURCE_URL=jdbc:postgresql://notification-db:5432/notification_db
      - EUREKA_CLIENT_SERVICE_URL_DEFAULTZONE=http://eureka-server:8761/eureka/
    volumes:
      - ./notification-service/src/main/resources/firebase-service-account.json:/app/firebase-service-account.json
    depends_on:
      - notification-db
      - eureka-server
    networks:
      - sprint-approve-network

  notification-db:
    image: postgres:15-alpine
    container_name: notification-db
    environment:
      POSTGRES_DB: notification_db
      POSTGRES_USER: postgres
      POSTGRES_PASSWORD: postgres
    ports:
      - "5435:5432"
    volumes:
      - notification-db-data:/var/lib/postgresql/data
    networks:
      - sprint-approve-network

volumes:
  notification-db-data:
```

---

## 🚀 Запуск

### 1. Положите firebase-service-account.json

```
notification-service/src/main/resources/firebase-service-account.json
```

### 2. Запустите базы данных

```bash
docker-compose up -d notification-db
```

### 3. Запустите notification-service

```bash
cd notification-service
mvn spring-boot:run
```

### 4. Обновите frontend конфигурацию

Замените в `firebase.ts`:
- `YOUR_API_KEY`
- `YOUR_VAPID_KEY` (получите в Firebase Console → Cloud Messaging → Web Push certificates)

### 5. Запустите frontend

```bash
cd frontend
npm run dev
```

---

## ✅ Тестирование

### 1. Включите уведомления в браузере

1. Откройте приложение
2. Нажмите кнопку "Включить уведомления"
3. Разрешите уведомления в браузере

### 2. Создайте задачу

```bash
POST /api/tasks
{
  "title": "Test Task",
  "assignedTo": 2,
  "createdBy": 1
}
```

Пользователь с ID=2 должен получить уведомление!

### 3. Одобрите задачу

```bash
PATCH /api/tasks/1/approve?approverId=2
```

Создатель (ID=1) должен получить уведомление!

---

## 🎯 Итого

✅ **Notification Service** - новый микросервис для FCM  
✅ **Firebase Admin SDK** - отправка push-уведомлений  
✅ **Feign интеграция** - Task Service → Notification Service  
✅ **React + Firebase SDK** - получение уведомлений в браузере  
✅ **Service Worker** - фоновые уведомления  
✅ **6 типов уведомлений** - для всех ролей  

**Проект готов к отправке push-уведомлений!** 🚀
