# 📊 Sprint Approve - Технический обзор проекта

## 🎨 Frontend

### Технологии:
- **React 18.2.0** - UI библиотека
- **TypeScript** - типизация
- **Vite 5.0.8** - сборщик (быстрее Webpack)
- **React Router 6.20.0** - маршрутизация
- **TailwindCSS 3.3.6** - utility-first CSS фреймворк
- **Axios 1.6.2** - HTTP клиент для API запросов
- **Lucide React 0.294.0** - иконки
- **date-fns 3.0.0** - работа с датами

### Структура:
```
frontend/
├── src/
│   ├── api/          # API клиенты (axios)
│   ├── components/   # React компоненты
│   ├── pages/        # Страницы приложения
│   ├── types/        # TypeScript типы
│   ├── App.tsx       # Главный компонент
│   └── main.tsx      # Entry point
├── public/           # Статические файлы
└── package.json
```

---

## 🔧 Backend Интеграции

### Межсервисное взаимодействие (OpenFeign):

#### 1. **auth-service** ↔ **team-service**
```java
@FeignClient(name = "team-service")
public interface TeamServiceClient {
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable Long id);
}
```
**Зачем:** Проверка существования пользователя при регистрации

---

#### 2. **task-service** ↔ **sprint-service**
```java
@FeignClient(name = "sprint-service")
public interface SprintServiceClient {
    @PatchMapping("/api/sprints/{id}/recalculate-status")
    void recalculateSprintStatus(@PathVariable Long id);
}
```
**Зачем:** Автоматический пересчет статуса спринта при изменении задач

---

#### 3. **task-service** ↔ **team-service** (UserServiceClient)
```java
@FeignClient(name = "team-service")
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable Long id);
}
```
**Зачем:** Получение информации о пользователях (assignedTo, createdBy, approver)

---

#### 4. **sprint-service** ↔ **team-service**
```java
@FeignClient(name = "team-service")
public interface TeamServiceClient {
    @GetMapping("/api/teams/{id}")
    TeamDto getTeamById(@PathVariable Long id);
    
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable Long id);
}
```
**Зачем:** Проверка существования команды и пользователей при создании спринта

---

#### 5. **sprint-service** ↔ **task-service**
```java
@FeignClient(name = "task-service")
public interface TaskServiceClient {
    @GetMapping("/api/tasks/sprint/{sprintId}")
    List<TaskDto> getTasksBySprintId(@PathVariable Long sprintId);
}
```
**Зачем:** Получение задач спринта для пересчета статуса

---

#### 6. **task-service** ↔ **notification-service** (НОВОЕ!)
```java
@FeignClient(name = "notification-service")
public interface NotificationServiceClient {
    @PostMapping("/api/notifications/task-assigned")
    void notifyTaskAssigned(...);
    
    @PostMapping("/api/notifications/task-approved")
    void notifyTaskApproved(...);
    
    // и т.д.
}
```
**Зачем:** Отправка push-уведомлений при событиях

---

## 🔔 Уведомления FCM

### Сценарии для APPROVER:

#### 1. Новая задача в команде/спринте
```java
// TaskService.java
public TaskDto createTask(TaskDto dto) {
    Task task = taskRepository.save(...);
    
    // Уведомляем всех APPROVER команды
    List<User> approvers = teamService.getApproversByTeam(task.getTeamId());
    approvers.forEach(approver -> {
        notificationClient.notifyNewTaskInSprint(
            approver.getId(),
            task.getId(),
            task.getTitle()
        );
    });
}
```

**Триггер:** `POST /api/tasks`  
**Получатель:** Все пользователи с ролью APPROVER в команде  
**Уведомление:** "Новая задача в спринте: {title}"

---

#### 2. Задача отправлена на рассмотрение
```java
// TaskService.java
public TaskDto submitForReview(Long id) {
    Task task = findById(id);
    task.setStatus(TaskStatus.ON_REVIEW);
    taskRepository.save(task);
    
    // Уведомляем назначенного аппрувера
    if (task.getApproverId() != null) {
        notificationClient.notifyTaskForReview(
            task.getApproverId(),
            task.getId(),
            task.getTitle()
        );
    }
}
```

**Триггер:** `PATCH /api/tasks/{id}/submit`  
**Получатель:** Назначенный approver (approverId)  
**Уведомление:** "Требуется проверка: {title}"

---

#### 3. Новые артефакты прикреплены
```java
// ArtifactService.java
public ArtifactDto createArtifact(ArtifactDto dto) {
    Artifact artifact = artifactRepository.save(...);
    
    // Получаем задачу
    Task task = taskRepository.findById(dto.getTaskId());
    
    // Уведомляем аппрувера
    if (task.getApproverId() != null) {
        notificationClient.notifyNewArtifact(
            task.getApproverId(),
            task.getId(),
            task.getTitle(),
            artifact.getName()
        );
    }
}
```

**Триггер:** `POST /api/artifacts/upload`  
**Получатель:** Approver задачи  
**Уведомление:** "Новый артефакт: {fileName} к задаче {title}"

---

### Сценарии для СОЗДАТЕЛЯ задачи:

#### 1. Новый комментарий под задачей
```java
// CommentService.java
public CommentDto createComment(CommentDto dto) {
    Comment comment = commentRepository.save(...);
    
    // Получаем задачу
    Task task = taskRepository.findById(dto.getTaskId());
    
    // Уведомляем создателя (если комментарий не от него)
    if (task.getCreatedBy() != null && 
        !task.getCreatedBy().equals(dto.getAuthorId())) {
        
        User author = userService.getUserById(dto.getAuthorId());
        notificationClient.notifyNewComment(
            task.getCreatedBy(),
            task.getId(),
            task.getTitle(),
            author.getName()
        );
    }
}
```

**Триггер:** `POST /api/comments`  
**Получатель:** Создатель задачи (createdBy)  
**Уведомление:** "{authorName} оставил комментарий к задаче {title}"

---

#### 2. Задача одобрена
```java
// TaskService.java
public TaskDto approveTask(Long id, Long approverId) {
    Task task = findById(id);
    task.setStatus(TaskStatus.APPROVED);
    task.setApproverId(approverId);
    taskRepository.save(task);
    
    // Уведомляем создателя
    if (task.getCreatedBy() != null) {
        notificationClient.notifyTaskApproved(
            task.getCreatedBy(),
            task.getId(),
            task.getTitle()
        );
    }
}
```

**Триггер:** `PATCH /api/tasks/{id}/approve`  
**Получатель:** Создатель задачи  
**Уведомление:** "Задача одобрена ✅: {title}"

---

#### 3. Задача отклонена
```java
// TaskService.java
public TaskDto rejectTask(Long id, Long approverId) {
    Task task = findById(id);
    task.setStatus(TaskStatus.REJECTED);
    task.setApproverId(approverId);
    taskRepository.save(task);
    
    // Уведомляем создателя
    if (task.getCreatedBy() != null) {
        notificationClient.notifyTaskRejected(
            task.getCreatedBy(),
            task.getId(),
            task.getTitle()
        );
    }
}
```

**Триггер:** `PATCH /api/tasks/{id}/reject`  
**Получатель:** Создатель задачи  
**Уведомление:** "Задача отклонена ❌: {title}"

---

### Сценарии для ИСПОЛНИТЕЛЯ:

#### 1. Задача назначена
```java
// TaskService.java
public TaskDto createTask(TaskDto dto) {
    Task task = taskRepository.save(...);
    
    // Уведомляем исполнителя
    if (task.getAssignedTo() != null) {
        notificationClient.notifyTaskAssigned(
            task.getAssignedTo(),
            task.getId(),
            task.getTitle()
        );
    }
}
```

**Триггер:** `POST /api/tasks` или `PUT /api/tasks/{id}` (при изменении assignedTo)  
**Получатель:** Исполнитель (assignedTo)  
**Уведомление:** "Вам назначена задача: {title}"

---

## 📊 Таблица всех уведомлений

| # | Событие | Триггер | Получатель | Тип уведомления |
|---|---------|---------|------------|-----------------|
| 1 | Задача назначена | `POST /api/tasks` | assignedTo | TASK_ASSIGNED |
| 2 | Новая задача в спринте | `POST /api/tasks` | Все APPROVER команды | NEW_TASK_IN_SPRINT |
| 3 | Задача на проверке | `PATCH /tasks/{id}/submit` | approverId | TASK_FOR_REVIEW |
| 4 | Задача одобрена | `PATCH /tasks/{id}/approve` | createdBy | TASK_APPROVED |
| 5 | Задача отклонена | `PATCH /tasks/{id}/reject` | createdBy | TASK_REJECTED |
| 6 | Новый комментарий | `POST /api/comments` | createdBy | NEW_COMMENT |
| 7 | Новый артефакт | `POST /api/artifacts/upload` | approverId | NEW_ARTIFACT |

---

## 🏗️ Архитектура уведомлений

```
┌─────────────────────────────────────────────────────────┐
│                    Frontend (React)                      │
│  - Firebase SDK                                          │
│  - Service Worker (фоновые уведомления)                 │
│  - NotificationManager компонент                         │
└────────────────────┬────────────────────────────────────┘
                     │ FCM Token
                     ↓
┌─────────────────────────────────────────────────────────┐
│              Notification Service (8085)                 │
│  - Firebase Admin SDK                                    │
│  - UserFcmToken entity (БД: notification_db)            │
│  - FcmService (отправка уведомлений)                    │
└────────────────────▲────────────────────────────────────┘
                     │ Feign Client
                     │
┌────────────────────┴────────────────────────────────────┐
│               Task Service (8083)                        │
│  - NotificationServiceClient                             │
│  - Вызовы при событиях (create, approve, reject)        │
└─────────────────────────────────────────────────────────┘
```

---

## 🎯 Итого

### Frontend:
- ✅ **React + TypeScript** - современный стек
- ✅ **Vite** - быстрая сборка
- ✅ **TailwindCSS** - красивый UI

### Backend интеграции:
- ✅ **5 Feign клиентов** между сервисами
- ✅ **Автоматическая синхронизация** статусов
- ✅ **Обогащение данных** через межсервисные вызовы

### Уведомления:
- ✅ **7 типов уведомлений**
- ✅ **3 роли** (APPROVER, CREATOR, ASSIGNEE)
- ✅ **Firebase Cloud Messaging** для web push
- ✅ **Notification Service** - отдельный микросервис

**Проект демонстрирует полноценную микросервисную архитектуру с современным стеком технологий!** 🚀
