# 🚀 FCM Quick Start Guide

## ✅ Что уже готово

- ✅ **notification-service** создан
- ✅ **pom.xml** обновлён (добавлен модуль)
- ✅ **Структура проекта** готова
- ✅ **Документация** в `FCM_INTEGRATION_GUIDE.md`

---

## 📋 Что нужно сделать (5 шагов)

### Шаг 1: Настройте Firebase (15 минут)

1. **Создайте проект Firebase:**
   - https://console.firebase.google.com/
   - Нажмите "Add project"
   - Имя: `sprint-approve`

2. **Добавьте Web App:**
   - Project Settings → Your apps → Web (</> иконка)
   - App nickname: `Sprint Approve Web`
   - Скопируйте Firebase Config

3. **Получите Service Account Key:**
   - Project Settings → Service accounts
   - Generate new private key
   - Сохраните как `firebase-service-account.json`
   - Положите в: `notification-service/src/main/resources/`

4. **Получите VAPID Key:**
   - Project Settings → Cloud Messaging
   - Web Push certificates → Generate key pair
   - Скопируйте ключ

---

### Шаг 2: Backend - Создайте недостающие файлы (30 минут)

Все файлы описаны в `FCM_INTEGRATION_GUIDE.md`, создайте:

#### 2.1 Entity
```
notification-service/src/main/java/org/example/notification/entity/
└── UserFcmToken.java
```

#### 2.2 Repository
```
notification-service/src/main/java/org/example/notification/repository/
└── UserFcmTokenRepository.java
```

#### 2.3 Services
```
notification-service/src/main/java/org/example/notification/service/
├── FcmService.java
└── UserTokenService.java
```

#### 2.4 Config
```
notification-service/src/main/java/org/example/notification/config/
└── FirebaseConfig.java
```

#### 2.5 Controllers
```
notification-service/src/main/java/org/example/notification/controller/
└── NotificationController.java
```

#### 2.6 DTOs
```
notification-service/src/main/java/org/example/notification/dto/
├── NotificationRequest.java
└── RegisterTokenRequest.java
```

---

### Шаг 3: Интеграция с Task Service (15 минут)

#### 3.1 Создайте Feign Client

```java
// task-service/src/main/java/org/example/task/client/NotificationServiceClient.java
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    // См. FCM_INTEGRATION_GUIDE.md для полного кода
}
```

#### 3.2 Обновите TaskService

Добавьте вызовы `notificationClient` в методы:
- `createTask()` → уведомить assignedTo
- `approveTask()` → уведомить createdBy
- `rejectTask()` → уведомить createdBy
- `submitForReview()` → уведомить approverId

#### 3.3 Обновите CommentService

```java
public CommentDto createComment(CommentDto dto) {
    // ... создание комментария
    
    // Уведомить создателя задачи
    Task task = taskRepository.findById(dto.getTaskId());
    if (task.getCreatedBy() != null && !task.getCreatedBy().equals(dto.getAuthorId())) {
        notificationClient.notifyNewComment(...);
    }
}
```

#### 3.4 Обновите ArtifactService

```java
public ArtifactDto createArtifact(ArtifactDto dto) {
    // ... создание артефакта
    
    // Уведомить аппрувера
    Task task = taskRepository.findById(dto.getTaskId());
    if (task.getApproverId() != null) {
        notificationClient.notifyNewArtifact(...);
    }
}
```

---

### Шаг 4: Frontend - React Integration (20 минут)

#### 4.1 Установите Firebase

```bash
cd frontend
npm install firebase
```

#### 4.2 Создайте файлы

```
frontend/src/firebase/
├── firebase.ts          # Firebase config и функции
└── NotificationManager.tsx  # React компонент
```

Код в `FCM_INTEGRATION_GUIDE.md`

#### 4.3 Создайте Service Worker

```
frontend/public/firebase-messaging-sw.js
```

#### 4.4 Обновите firebase.ts

Замените:
- `YOUR_API_KEY` → из Firebase Console
- `YOUR_VAPID_KEY` → из Cloud Messaging

#### 4.5 Добавьте NotificationManager в App

```typescript
// src/App.tsx
import { NotificationManager } from './firebase/NotificationManager';

function App() {
  const userId = getCurrentUserId(); // Получите из auth context
  
  return (
    <div>
      <NotificationManager userId={userId} />
      {/* остальной код */}
    </div>
  );
}
```

---

### Шаг 5: Docker & Запуск (10 минут)

#### 5.1 Обновите docker-compose.yml

```yaml
services:
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

#### 5.2 Запустите БД

```bash
docker-compose up -d notification-db
```

#### 5.3 Запустите notification-service

```bash
cd notification-service
mvn spring-boot:run
```

#### 5.4 Проверьте Eureka

http://localhost:8761 → должен появиться NOTIFICATION-SERVICE

---

## 🧪 Тестирование

### 1. Включите уведомления

1. Откройте frontend: http://localhost:5173
2. Нажмите "Включить уведомления"
3. Разрешите в браузере

### 2. Создайте задачу

```bash
POST http://localhost:8080/api/tasks
Authorization: Bearer YOUR_TOKEN

{
  "title": "Test Task",
  "description": "Test",
  "sprintId": 1,
  "assignedTo": 2,
  "createdBy": 1
}
```

**Результат:** Пользователь ID=2 получит push "Новая задача назначена"

### 3. Одобрите задачу

```bash
PATCH http://localhost:8080/api/tasks/1/approve?approverId=2
Authorization: Bearer YOUR_TOKEN
```

**Результат:** Пользователь ID=1 получит push "Задача одобрена ✅"

---

## 📊 Типы уведомлений

| Событие | Кто получает | Триггер |
|---------|--------------|---------|
| Задача назначена | assignedTo | `POST /api/tasks` |
| Задача на проверке | approverId | `PATCH /api/tasks/{id}/submit` |
| Задача одобрена | createdBy | `PATCH /api/tasks/{id}/approve` |
| Задача отклонена | createdBy | `PATCH /api/tasks/{id}/reject` |
| Новый комментарий | createdBy | `POST /api/comments` |
| Новый артефакт | approverId | `POST /api/artifacts/upload` |

---

## 🔧 Troubleshooting

### Уведомления не приходят

1. **Проверьте Firebase Config:**
   - Правильный ли `apiKey`?
   - Правильный ли `vapidKey`?

2. **Проверьте Service Worker:**
   - Открыть DevTools → Application → Service Workers
   - Должен быть зарегистрирован `firebase-messaging-sw.js`

3. **Проверьте токен:**
   - DevTools → Console → должен быть "FCM Token: ..."
   - Проверьте в БД: `SELECT * FROM user_fcm_tokens;`

4. **Проверьте логи notification-service:**
   ```bash
   # Должно быть:
   INFO: Successfully sent FCM message to user 2: ...
   ```

### Firebase ошибки

- **"Firebase not initialized"** → проверьте `firebase-service-account.json`
- **"Invalid token"** → пользователь отключил уведомления, токен удалён
- **"UNREGISTERED"** → токен устарел, нужно перерегистрировать

---

## 📚 Полная документация

См. **`FCM_INTEGRATION_GUIDE.md`** для детального описания всех файлов и кода.

---

## ✅ Checklist

- [ ] Firebase проект создан
- [ ] Service Account Key скачан и положен в `notification-service/src/main/resources/`
- [ ] VAPID Key получен
- [ ] notification-service файлы созданы
- [ ] task-service обновлён (Feign Client)
- [ ] Frontend Firebase SDK установлен
- [ ] firebase.ts создан и настроен
- [ ] Service Worker создан
- [ ] NotificationManager добавлен в App
- [ ] docker-compose.yml обновлён
- [ ] notification-db запущена
- [ ] notification-service запущен и зарегистрирован в Eureka
- [ ] Тестирование пройдено

**После выполнения всех пунктов — FCM готов к работе!** 🚀
