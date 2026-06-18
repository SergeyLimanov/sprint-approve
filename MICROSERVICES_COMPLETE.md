# 🏗️ ПОЛНОЕ ОПИСАНИЕ ВСЕХ МИКРОСЕРВИСОВ Sprint Approve

**Автор:** Сергей Лиманов  
**Дата:** Июнь 2026  
**Версия:** 1.0

---

## 📋 Содержание

1. [Eureka Server](#1-eureka-server-порт-8761)
2. [API Gateway](#2-api-gateway-порт-8080)
3. [Auth Service](#3-auth-service-порт-8084)
4. [Team Service](#4-team-service-порт-8081)
5. [Sprint Service](#5-sprint-service-порт-8082)
6. [Task Service](#6-task-service-порт-8083)
7. [Notification Service](#7-notification-service-порт-8085)
8. [Сводная таблица](#сводная-таблица)

---

# 1. EUREKA SERVER (порт 8761)

## 📌 Назначение
**Service Discovery** — центральный реестр всех микросервисов в системе.

## 🎯 Зачем нужен
- Автоматическая регистрация сервисов при запуске
- Динамическое обнаружение сервисов (не нужно хардкодить IP)
- Health check — проверка доступности сервисов
- Балансировка нагрузки между инстансами

## 📂 Структура классов

```
eureka-server/
└── src/main/java/org/example/eureka/
    └── EurekaServerApplication.java  (1 класс)
```

## 📝 Классы

### `EurekaServerApplication.java`

**Назначение:** Точка входа в Eureka Server

```java
@SpringBootApplication
@EnableEurekaServer  // ← Включает Eureka Server
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**Аннотации:**
- `@EnableEurekaServer` — активирует функционал Service Discovery

**Что делает:**
1. Запускает Eureka Server на порту 8761
2. Предоставляет REST API для регистрации сервисов
3. Предоставляет Dashboard: http://localhost:8761

## 🌐 Eureka Dashboard

**Что показывает:**
- Список зарегистрированных сервисов
- Статус (UP, DOWN)
- IP-адреса и порты
- Количество инстансов

---

# 2. API GATEWAY (порт 8080)

## 📌 Назначение
**Единая точка входа** для всех клиентских запросов. Проверяет JWT и маршрутизирует запросы.

## 🎯 Зачем нужен
- Единый URL для клиента (http://localhost:8080)
- Аутентификация (проверка JWT токенов)
- Маршрутизация к микросервисам через Eureka
- CORS настройки для frontend
- Обогащение запросов заголовками (X-User-Id, X-User-Role)

## 📂 Структура классов

```
api-gateway/
└── src/main/java/org/example/gateway/
    ├── ApiGatewayApplication.java
    ├── config/
    │   ├── CorsConfig.java
    │   └── SecurityConfig.java
    ├── filter/
    │   └── JwtAuthenticationFilter.java  ⭐ КЛЮЧЕВОЙ
    └── security/
        └── JwtTokenProvider.java
```

**Всего:** 5 классов

## 📝 Ключевые классы

### 1. `JwtAuthenticationFilter.java` ⭐

**Назначение:** Проверяет JWT токен в каждом запросе

**Алгоритм работы:**
```
1. Пропустить /api/auth/** без проверки (логин/регистрация)
2. Извлечь токен из заголовка Authorization: Bearer <token>
3. Валидировать токен (подпись, срок действия)
4. Извлечь данные: userId, email, role
5. Добавить заголовки для микросервисов:
   - X-User-Id: 1
   - X-User-Email: user@example.com
   - X-User-Role: DEVELOPER
6. Передать запрос дальше
```

**Зачем добавлять заголовки?**
- Микросервисы не имеют доступа к JWT токену
- Заголовки передают информацию о пользователе
- Микросервисы могут проверять права доступа

### 2. `JwtTokenProvider.java`

**Методы:**
- `validateToken(token)` — проверяет подпись и срок
- `extractUserId(token)` — извлекает ID из claim
- `extractEmail(token)` — извлекает email из subject
- `extractRole(token)` — извлекает роль из claim

### 3. `CorsConfig.java`

**Назначение:** Разрешает frontend (React на 3000) делать запросы к API

## ⚙️ Маршрутизация (application.yml)

```yaml
spring:
  cloud:
    gateway:
      routes:
        - id: task-service
          uri: lb://task-service  # lb = load balanced через Eureka
          predicates:
            - Path=/api/tasks/**
          filters:
            - JwtAuthenticationFilter  # ← Проверка JWT!
```

**Пример:**
- Frontend: `GET http://localhost:8080/api/tasks/1`
- Gateway → Eureka: "Где task-service?"
- Eureka: "localhost:8083"
- Gateway → Task Service: `GET http://localhost:8083/api/tasks/1` + заголовки

---

# 3. AUTH SERVICE (порт 8084)

## 📌 Назначение
**Аутентификация и авторизация** — регистрация, логин, JWT токены.

## 🎯 Зачем нужен
- Регистрация новых пользователей
- Логин (проверка email/password)
- Генерация JWT access и refresh токенов
- Обновление токенов (refresh)
- Хеширование паролей (BCrypt)

## 📂 Структура классов

```
auth-service/
└── src/main/java/org/example/auth/
    ├── AuthServiceApplication.java
    ├── client/
    │   ├── TeamServiceClient.java  ⭐ Feign
    │   └── UserDto.java
    ├── controller/
    │   └── AuthController.java
    ├── dto/
    │   ├── AuthResponse.java
    │   ├── LoginRequest.java
    │   ├── RefreshTokenRequest.java
    │   └── RegisterRequest.java
    ├── security/
    │   └── JwtTokenProvider.java
    └── service/
        └── AuthService.java  ⭐ КЛЮЧЕВОЙ
```

**Всего:** 12 классов

## 📝 Ключевые классы

### 1. `AuthService.java` ⭐

**Методы:**

#### Регистрация
```java
public AuthResponse register(RegisterRequest request) {
    // 1. Хешировать пароль
    String hashedPassword = passwordEncoder.encode(request.getPassword());
    
    // 2. Создать пользователя через Team Service (Feign)
    UserDto user = teamServiceClient.createUser(userDto);
    
    // 3. Генерация JWT токенов
    String accessToken = jwtTokenProvider.generateAccessToken(
        user.getId(), user.getEmail(), user.getRole()
    );
    String refreshToken = jwtTokenProvider.generateRefreshToken(
        user.getId(), user.getEmail()
    );
    
    // 4. Вернуть токены
    return new AuthResponse(accessToken, refreshToken, user);
}
```

#### Логин
```java
public AuthResponse login(LoginRequest request) {
    // 1. Получить пользователя из Team Service
    UserDto user = teamServiceClient.getUserByEmail(request.getEmail());
    
    // 2. Проверить пароль (BCrypt)
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new RuntimeException("Invalid password");
    }
    
    // 3. Генерация новых токенов
    return new AuthResponse(accessToken, refreshToken, user);
}
```

### 2. `JwtTokenProvider.java`

**Генерация access токена:**
```java
public String generateAccessToken(Long userId, String email, String role) {
    return Jwts.builder()
        .setSubject(email)
        .claim("userId", userId)
        .claim("role", role)
        .setExpiration(new Date(System.currentTimeMillis() + 86400000))  // 24 часа
        .signWith(getSigningKey(), SignatureAlgorithm.HS256)
        .compact();
}
```

**JWT Payload:**
```json
{
  "sub": "user@example.com",
  "userId": 1,
  "role": "DEVELOPER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

### 3. `TeamServiceClient.java` (Feign)

```java
@FeignClient(name = "team-service")  // ← Имя из Eureka
public interface TeamServiceClient {
    @PostMapping("/api/users")
    UserDto createUser(@RequestBody UserDto userDto);
    
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable String email);
}
```

**Как работает Feign:**
1. `@FeignClient(name = "team-service")` — ищет сервис в Eureka
2. Eureka возвращает URL: `http://localhost:8081`
3. Feign делает HTTP запрос: `POST http://localhost:8081/api/users`

## 🔐 Безопасность

- **BCrypt** для хеширования паролей (salt + 10 раундов)
- **JWT** с HMAC-SHA256 подписью
- **Access токен:** 24 часа
- **Refresh токен:** 7 дней

---

# 4. TEAM SERVICE (порт 8081)

## 📌 Назначение
**Управление командами и пользователями** — CRUD операции, хранение ролей.

## 🎯 Зачем нужен
- Создание и управление командами
- Создание и управление пользователями
- Хранение ролей пользователей (DEVELOPER, APPROVER, TEAM_LEAD, MANAGER)
- Предоставление данных о пользователях другим сервисам

## 📂 Структура классов

```
team-service/
└── src/main/java/org/example/team/
    ├── TeamServiceApplication.java
    ├── controller/
    │   ├── TeamController.java
    │   └── UserController.java
    ├── dto/
    │   ├── TeamDto.java
    │   └── UserDto.java
    ├── entity/
    │   ├── Team.java
    │   ├── User.java  ⭐ КЛЮЧЕВОЙ
    │   └── UserRole.java  ⭐ ENUM
    ├── mapper/
    │   ├── TeamMapper.java
    │   └── UserMapper.java
    ├── repository/
    │   ├── TeamRepository.java
    │   └── UserRepository.java
    └── service/
        ├── TeamService.java
        └── UserService.java
```

**Всего:** 16 классов

## 📝 Ключевые классы

### 1. `User.java` (Entity) ⭐

```java
@Entity
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String email;
    private String name;
    private String password;  // BCrypt hash
    
    @Enumerated(EnumType.STRING)  // ← Хранится как строка в БД
    private UserRole role;
    
    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;
}
```

### 2. `UserRole.java` (Enum) ⭐

```java
public enum UserRole {
    TEAM_LEAD,    // Лидер команды (может одобрять)
    DEVELOPER,    // Разработчик (создает задачи)
    MANAGER,      // Менеджер (может одобрять)
    APPROVER      // Аппрувер (может одобрять)
}
```

**Где хранится роль:**
- В БД: таблица `users`, колонка `role` (VARCHAR)
- В JWT: claim `role` (строка)
- В HTTP заголовке: `X-User-Role` (от API Gateway)

### 3. `UserService.java`

**Методы:**
- `createUser(UserDto)` — создание пользователя
- `getUserById(Long)` — получение по ID
- `getUserByEmail(String)` — получение по email
- `updateUser(Long, UserDto)` — обновление
- `updateUserRole(Long, String)` — изменение роли

### 4. `TeamService.java`

**Методы:**
- `createTeam(TeamDto)` — создание команды
- `getTeamById(Long)` — получение по ID
- `getAllTeams()` — список всех команд
- `updateTeam(Long, TeamDto)` — обновление
- `deleteTeam(Long)` — удаление

## 🗄️ База данных

**Таблица `users`:**
```sql
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    email VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL,  -- 'DEVELOPER', 'APPROVER', etc.
    team_id BIGINT REFERENCES teams(id),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

**Таблица `teams`:**
```sql
CREATE TABLE teams (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

---

# 5. SPRINT SERVICE (порт 8082)

## 📌 Назначение
**Управление спринтами** — CRUD операции, автоматическая синхронизация статусов.

## 🎯 Зачем нужен
- Создание и управление спринтами
- Автоматическая синхронизация статусов спринтов на основе задач
- Одобрение/отклонение спринтов
- Проверка прав доступа (только APPROVER может одобрять)

## 📂 Структура классов

```
sprint-service/
└── src/main/java/org/example/sprint/
    ├── SprintServiceApplication.java
    ├── client/
    │   ├── TaskServiceClient.java  ⭐ Feign
    │   ├── TeamServiceClient.java  ⭐ Feign
    │   ├── TaskDto.java
    │   ├── TeamDto.java
    │   └── UserDto.java
    ├── controller/
    │   └── SprintController.java
    ├── dto/
    │   └── SprintDto.java
    ├── entity/
    │   ├── Sprint.java
    │   ├── SprintStatus.java  ⭐ ENUM
    │   └── SprintType.java
    ├── mapper/
    │   └── SprintMapper.java
    ├── repository/
    │   └── SprintRepository.java
    └── service/
        └── SprintService.java  ⭐ КЛЮЧЕВОЙ
```

**Всего:** 15 классов

## 📝 Ключевые классы

### 1. `Sprint.java` (Entity)

```java
@Entity
@Table(name = "sprints")
public class Sprint {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String name;
    private String description;
    private Long teamId;
    
    @Enumerated(EnumType.STRING)
    private SprintStatus status;  // CREATED, ON_REVIEW, APPROVED, REJECTED
    
    @Enumerated(EnumType.STRING)
    private SprintType type;  // REGULAR, HOTFIX, RELEASE
    
    private LocalDate startDate;
    private LocalDate endDate;
    private Long createdBy;
}
```

### 2. `SprintStatus.java` (Enum) ⭐

```java
public enum SprintStatus {
    CREATED,     // Создан
    ON_REVIEW,   // На рассмотрении
    APPROVED,    // Одобрен
    REJECTED     // Отклонен
}
```

### 3. `SprintService.java` ⭐

**Ключевой метод: `recalculateSprintStatus()`**

```java
@Transactional
public SprintDto recalculateSprintStatus(Long id) {
    Sprint sprint = sprintRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Sprint not found"));
    
    // Получить все задачи спринта через Feign
    List<TaskDto> tasks = taskServiceClient.getTasksBySprintId(id);
    
    if (tasks.isEmpty()) {
        sprint.setStatus(SprintStatus.CREATED);
    } else {
        boolean allApproved = tasks.stream()
            .allMatch(task -> "APPROVED".equals(task.getStatus()));
        boolean anyRejected = tasks.stream()
            .anyMatch(task -> "REJECTED".equals(task.getStatus()));
        boolean anyOnReview = tasks.stream()
            .anyMatch(task -> "ON_REVIEW".equals(task.getStatus()));
        
        // ПРИОРИТЕТ: REJECTED > ON_REVIEW > CREATED > APPROVED
        if (anyRejected) {
            sprint.setStatus(SprintStatus.REJECTED);
        } else if (anyOnReview) {
            sprint.setStatus(SprintStatus.ON_REVIEW);
        } else if (allApproved) {
            sprint.setStatus(SprintStatus.APPROVED);
        } else {
            sprint.setStatus(SprintStatus.CREATED);
        }
    }
    
    return SprintMapper.toDto(sprintRepository.save(sprint));
}
```

**Когда вызывается:**
- После изменения статуса задачи (Task Service → Sprint Service)
- Через Feign клиент: `sprintServiceClient.recalculateSprintStatus(sprintId)`

**Логика:**
1. Если **хотя бы одна** задача REJECTED → спринт REJECTED
2. Если **хотя бы одна** задача ON_REVIEW → спринт ON_REVIEW
3. Если **все** задачи APPROVED → спринт APPROVED
4. Иначе → спринт CREATED

### 4. `approveSprint()` — Одобрение спринта

```java
public SprintDto approveSprint(Long id, Long approverId) {
    Sprint sprint = sprintRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Sprint not found"));
    
    // Проверка роли аппрувера
    UserDto approver = teamServiceClient.getUserById(approverId);
    if (!"APPROVER".equals(approver.getRole()) && 
        !"TEAM_LEAD".equals(approver.getRole()) && 
        !"MANAGER".equals(approver.getRole())) {
        throw new RuntimeException("Only APPROVER can approve sprints");
    }
    
    // Проверка, что все задачи одобрены
    List<TaskDto> tasks = taskServiceClient.getTasksBySprintId(id);
    boolean allApproved = tasks.stream()
        .allMatch(task -> "APPROVED".equals(task.getStatus()));
    
    if (!allApproved) {
        throw new RuntimeException("Cannot approve: not all tasks approved");
    }
    
    sprint.setStatus(SprintStatus.APPROVED);
    return SprintMapper.toDto(sprintRepository.save(sprint));
}
```

## 🔄 Resilience4j

**Circuit Breaker и Retry для Feign клиентов:**

```java
@CircuitBreaker(name = "taskService", fallbackMethod = "getTasksFallback")
@Retry(name = "taskService")
private List<TaskDto> getTasksBySprintIdWithResilience(Long sprintId) {
    return taskServiceClient.getTasksBySprintId(sprintId);
}

private List<TaskDto> getTasksFallback(Long sprintId, Exception e) {
    log.error("Failed to fetch tasks: {}", e.getMessage());
    return Collections.emptyList();  // Пустой список при ошибке
}
```

**Зачем:**
- Если Task Service недоступен → не падать, вернуть пустой список
- Retry: 3 попытки с задержкой 1 секунда
- Circuit Breaker: после 5 ошибок → открыть цепь на 60 секунд

---

# 6. TASK SERVICE (порт 8083)

## 📌 Назначение
**Управление задачами, файлами и комментариями** — самый сложный микросервис.

## 🎯 Зачем нужен
- CRUD операции с задачами
- Загрузка файлов в MinIO (presigned URLs)
- Комментарии к задачам
- История изменений задач
- Одобрение/отклонение задач
- Push-уведомления через Notification Service
- Автоматическая синхронизация статусов спринтов

## 📂 Структура классов

```
task-service/
└── src/main/java/org/example/task/
    ├── TaskServiceApplication.java
    ├── client/
    │   ├── NotificationServiceClient.java  ⭐ Feign
    │   ├── SprintServiceClient.java  ⭐ Feign
    │   ├── UserServiceClient.java  ⭐ Feign
    │   └── dto/
    ├── config/
    │   ├── MinioConfiguration.java
    │   └── MinioProperties.java
    ├── controller/
    │   ├── TaskController.java
    │   ├── ArtifactController.java
    │   └── CommentController.java
    ├── dto/
    │   ├── TaskDto.java
    │   ├── ArtifactDto.java
    │   ├── CommentDto.java
    │   └── TaskHistoryDto.java
    ├── entity/
    │   ├── Task.java  ⭐
    │   ├── Artifact.java
    │   ├── Comment.java
    │   ├── TaskHistory.java
    │   └── TaskStatus.java  ⭐ ENUM
    ├── mapper/
    │   ├── TaskMapper.java
    │   ├── ArtifactMapper.java
    │   ├── CommentMapper.java
    │   └── TaskHistoryMapper.java
    ├── repository/
    │   ├── TaskRepository.java
    │   ├── ArtifactRepository.java
    │   ├── CommentRepository.java
    │   └── TaskHistoryRepository.java
    └── service/
        ├── TaskService.java  ⭐ КЛЮЧЕВОЙ
        ├── ArtifactService.java
        ├── CommentService.java
        └── FileStorageService.java  ⭐ MinIO
```

**Всего:** 35+ классов

## 📝 Ключевые классы

### 1. `Task.java` (Entity)

```java
@Entity
@Table(name = "tasks")
public class Task {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String title;
    private String description;
    private Long sprintId;
    
    @Enumerated(EnumType.STRING)
    private TaskStatus status;  // CREATED, ON_REVIEW, APPROVED, REJECTED
    
    private Long createdBy;
    private Long assigneeId;
    private Long approverId;
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<Artifact> artifacts;  // Файлы
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<Comment> comments;  // Комментарии
    
    @OneToMany(mappedBy = "task", cascade = CascadeType.ALL)
    private List<TaskHistory> history;  // История изменений
}
```

### 2. `TaskStatus.java` (Enum)

```java
public enum TaskStatus {
    CREATED,     // Создана
    ON_REVIEW,   // На рассмотрении
    APPROVED,    // Одобрена
    REJECTED     // Отклонена
}
```

### 3. `TaskService.java` ⭐

**Метод: `submitForReview()` — Отправка на проверку**

```java
public TaskDto submitForReview(Long id) {
    Task task = taskRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Task not found"));
    
    // 1. Изменить статус задачи
    task.setStatus(TaskStatus.ON_REVIEW);
    Task updated = taskRepository.save(task);
    
    // 2. Сохранить в историю
    saveHistory(task, "Submitted for review");
    
    // 3. Отправить уведомление аппруверу
    notificationServiceClient.createNotification(new NotificationRequest(
        task.getApproverId(),
        "New task for review: " + task.getTitle(),
        "TASK_REVIEW",
        task.getId()
    ));
    
    // 4. Синхронизировать статус спринта ⭐
    sprintServiceClient.recalculateSprintStatus(task.getSprintId());
    
    return TaskMapper.toDto(updated);
}
```

**Метод: `approveTask()` — Одобрение задачи**

```java
public TaskDto approveTask(Long id, Long approverId) {
    Task task = taskRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("Task not found"));
    
    // 1. Проверка роли ⭐
    UserResponse approver = userServiceClient.getUserById(approverId);
    if (!"APPROVER".equals(approver.getRole()) && 
        !"TEAM_LEAD".equals(approver.getRole()) && 
        !"MANAGER".equals(approver.getRole())) {
        throw new RuntimeException("Only APPROVER can approve tasks");
    }
    
    // 2. Изменить статус
    task.setStatus(TaskStatus.APPROVED);
    Task updated = taskRepository.save(task);
    
    // 3. История
    saveHistory(task, "Approved by " + approver.getName());
    
    // 4. Уведомление исполнителю
    notificationServiceClient.createNotification(new NotificationRequest(
        task.getAssigneeId(),
        "Your task was approved: " + task.getTitle(),
        "TASK_APPROVED",
        task.getId()
    ));
    
    // 5. Синхронизация спринта ⭐
    sprintServiceClient.recalculateSprintStatus(task.getSprintId());
    
    return TaskMapper.toDto(updated);
}
```

### 4. `FileStorageService.java` ⭐ (MinIO)

**Метод: `uploadFile()` — Загрузка файла**

```java
public String uploadFile(MultipartFile file, Long taskId) {
    String fileName = taskId + "_" + System.currentTimeMillis() + "_" + file.getOriginalFilename();
    
    // Загрузить в MinIO
    minioClient.putObject(
        PutObjectArgs.builder()
            .bucket("task-artifacts")
            .object(fileName)
            .stream(file.getInputStream(), file.getSize(), -1)
            .contentType(file.getContentType())
            .build()
    );
    
    return fileName;
}
```

**Метод: `getPresignedUrl()` — Получить временную ссылку**

```java
public String getPresignedUrl(String fileName) {
    return minioClient.getPresignedObjectUrl(
        GetPresignedObjectUrlArgs.builder()
            .bucket("task-artifacts")
            .object(fileName)
            .expiry(60, TimeUnit.MINUTES)  // ← 60 минут
            .build()
    );
}
```

**Зачем presigned URL:**
- Файл хранится в MinIO (не в БД)
- Frontend получает временную ссылку (60 минут)
- Можно скачать файл напрямую из MinIO (без прокси через backend)

### 5. `Artifact.java` (Entity) — Файл

```java
@Entity
@Table(name = "artifacts")
public class Artifact {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String fileName;
    private String fileUrl;  // Путь в MinIO
    private Long fileSize;
    private String contentType;
    
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;
    
    private Long uploadedBy;
}
```

### 6. `Comment.java` (Entity) — Комментарий

```java
@Entity
@Table(name = "comments")
public class Comment {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private String content;
    
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;
    
    private Long authorId;
    private LocalDateTime createdAt;
}
```

### 7. `TaskHistory.java` (Entity) — История

```java
@Entity
@Table(name = "task_history")
public class TaskHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @ManyToOne
    @JoinColumn(name = "task_id")
    private Task task;
    
    private String action;  // "Created", "Submitted", "Approved", etc.
    private Long performedBy;
    private LocalDateTime performedAt;
}
```

## 🔄 Межсервисное взаимодействие

**Task Service вызывает:**
1. **Sprint Service** — `recalculateSprintStatus(sprintId)`
2. **User Service** — `getUserById(userId)` для проверки роли
3. **Notification Service** — `createNotification(request)` для push

---

# 7. NOTIFICATION SERVICE (порт 8085)

## 📌 Назначение
**Push-уведомления через Firebase Cloud Messaging (FCM)**.

## 🎯 Зачем нужен
- Отправка push-уведомлений в браузер
- Хранение истории уведомлений
- Отметка уведомлений как прочитанных
- Подсчет непрочитанных уведомлений

## 📂 Структура классов

```
notification-service/
└── src/main/java/org/example/notification/
    ├── NotificationServiceApplication.java
    ├── client/
    │   ├── UserServiceClient.java  ⭐ Feign
    │   ├── EmailServiceClient.java
    │   ├── UserDto.java
    │   └── EmailNotificationRequest.java
    ├── controller/
    │   └── NotificationController.java
    ├── dto/
    │   └── NotificationDto.java
    ├── entity/
    │   └── Notification.java
    ├── repository/
    │   └── NotificationRepository.java
    └── service/
        └── NotificationService.java  ⭐ КЛЮЧЕВОЙ
```

**Всего:** 11 классов

## 📝 Ключевые классы

### 1. `Notification.java` (Entity)

```java
@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    private Long userId;
    private String message;
    private String type;  // "TASK_REVIEW", "TASK_APPROVED", etc.
    private Long relatedEntityId;  // ID задачи
    private Boolean isRead;
    private LocalDateTime createdAt;
}
```

### 2. `NotificationService.java` ⭐

**Метод: `createNotification()` — Создание уведомления**

```java
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
    
    // Отправить push через Firebase (если настроен)
    // sendPushNotification(saved);
    
    return convertToDto(saved);
}
```

**Метод: `getUnreadNotifications()` — Непрочитанные**

```java
public List<NotificationDto> getUnreadNotifications(Long userId) {
    return notificationRepository
        .findByUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
        .stream()
        .map(this::convertToDto)
        .collect(Collectors.toList());
}
```

**Метод: `markAsRead()` — Отметить как прочитанное**

```java
@Transactional
public void markAsRead(Long notificationId) {
    Notification notification = notificationRepository.findById(notificationId)
        .orElseThrow(() -> new RuntimeException("Notification not found"));
    notification.setIsRead(true);
    notificationRepository.save(notification);
}
```

## 🔔 Firebase Cloud Messaging (FCM)

**Настройка:**
1. Создать проект в Firebase Console
2. Скачать `serviceAccountKey.json`
3. Положить в `src/main/resources/`
4. Настроить `FirebaseMessaging` bean

**Отправка push:**
```java
public void sendPushNotification(Notification notification) {
    Message message = Message.builder()
        .setToken(userFcmToken)  // FCM токен пользователя
        .setNotification(
            com.google.firebase.messaging.Notification.builder()
                .setTitle("Sprint Approve")
                .setBody(notification.getMessage())
                .build()
        )
        .build();
    
    FirebaseMessaging.getInstance().send(message);
}
```

---

# СВОДНАЯ ТАБЛИЦА

| Микросервис | Порт | Классов | Назначение | Ключевые технологии |
|-------------|------|---------|------------|---------------------|
| **Eureka Server** | 8761 | 1 | Service Discovery | Spring Cloud Eureka |
| **API Gateway** | 8080 | 5 | Маршрутизация, JWT | Spring Cloud Gateway, JWT |
| **Auth Service** | 8084 | 12 | Аутентификация | JWT, BCrypt, Feign |
| **Team Service** | 8081 | 16 | Команды, пользователи | JPA, PostgreSQL |
| **Sprint Service** | 8082 | 15 | Спринты, автосинхронизация | JPA, Feign, Resilience4j |
| **Task Service** | 8083 | 35+ | Задачи, файлы, комментарии | JPA, MinIO, Feign |
| **Notification Service** | 8085 | 11 | Push-уведомления | Firebase FCM, JPA |

**Всего:** 7 микросервисов, **95+ классов**

---

# КЛЮЧЕВЫЕ ПАТТЕРНЫ

## 1. Database per Service
Каждый сервис имеет свою БД:
- `team-db` (PostgreSQL)
- `sprint-db` (PostgreSQL)
- `task-db` (PostgreSQL)
- `notification-db` (PostgreSQL)

## 2. Service Discovery (Eureka)
Сервисы находят друг друга по имени, а не по IP.

## 3. API Gateway Pattern
Единая точка входа, проверка JWT, маршрутизация.

## 4. Synchronous Communication (Feign)
Межсервисное взаимодействие через REST API:
- Task Service → Sprint Service (синхронизация статусов)
- Task Service → Notification Service (push)
- Auth Service → Team Service (создание пользователя)

## 5. Circuit Breaker & Retry (Resilience4j)
Устойчивость к сбоям:
- 3 попытки с задержкой 1 секунда
- Circuit Breaker после 5 ошибок
- Fallback методы

## 6. Автоматическая синхронизация статусов
Task Service → Sprint Service:
```
Задача изменила статус → recalculateSprintStatus() → Спринт изменил статус
```

---

# ДЛЯ ЗАЩИТЫ ПРОЕКТА

## Вопрос 1: Сколько классов в проекте?

**Ответ:**
> "В проекте 7 микросервисов, суммарно около 95+ Java классов. Самый простой — Eureka Server (1 класс), самый сложный — Task Service (35+ классов), так как он отвечает за задачи, файлы, комментарии и интеграцию с MinIO."

## Вопрос 2: Как работает автоматическая синхронизация статусов?

**Ответ:**
> "Когда задача меняет статус (например, на ON_REVIEW), Task Service вызывает Sprint Service через Feign клиент: `sprintServiceClient.recalculateSprintStatus(sprintId)`. Sprint Service получает все задачи спринта и пересчитывает статус по приоритету: REJECTED > ON_REVIEW > CREATED > APPROVED. Если хотя бы одна задача на рассмотрении — весь спринт переходит в ON_REVIEW. Это реализовано в методе `SprintService.recalculateSprintStatus()` на строке 198."

## Вопрос 3: Зачем так много микросервисов?

**Ответ:**
> "Каждый микросервис отвечает за свою бизнес-область: Team Service — команды и пользователи, Sprint Service — спринты, Task Service — задачи и файлы. Это позволяет:
> 1. Независимо масштабировать (Task Service нагружен больше)
> 2. Независимо разрабатывать (разные команды)
> 3. Изолировать сбои (если Task Service упал, Sprint Service продолжает работать)
> 4. Использовать разные технологии (Task Service использует MinIO, Notification Service — Firebase)"

---

**Файл создан:** Июнь 2026  
**Версия:** 1.0  
**Автор:** Сергей Лиманов
