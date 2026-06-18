# 🏗️ Краткая сводка всех микросервисов Sprint Approve

## 📋 Все микросервисы

### 1️⃣ Eureka Server (порт 8761)
**Назначение:** Service Discovery - реестр сервисов
**Классы:** 1 класс - `EurekaServerApplication.java`
**Ключевая аннотация:** `@EnableEurekaServer`

### 2️⃣ API Gateway (порт 8080)
**Назначение:** Единая точка входа, проверка JWT, маршрутизация
**Классы:** 5 классов
- `JwtAuthenticationFilter` - проверка токена
- `JwtTokenProvider` - валидация JWT
- `CorsConfig` - настройка CORS

### 3️⃣ Auth Service (порт 8084)
**Назначение:** Регистрация, логин, JWT токены
**Классы:** 12 классов
- `AuthService` - бизнес-логика
- `AuthController` - REST API
- `JwtTokenProvider` - генерация токенов
- `TeamServiceClient` - Feign клиент

### 4️⃣ Team Service (порт 8081)
**Назначение:** Управление командами и пользователями
**Классы:** 16 классов
**Entities:** User, Team, UserRole (enum)
**Repositories:** UserRepository, TeamRepository
**Services:** UserService, TeamService

### 5️⃣ Sprint Service (порт 8082)
**Назначение:** Управление спринтами
**Классы:** 15 классов
**Entities:** Sprint, SprintStatus, SprintType
**Feign клиенты:** TaskServiceClient, TeamServiceClient
**Ключевой метод:** `recalculateSprintStatus()` - автосинхронизация

### 6️⃣ Task Service (порт 8083)
**Назначение:** Управление задачами, файлами, комментариями
**Классы:** 35+ классов
**Entities:** Task, Artifact, Comment, TaskHistory
**Services:** TaskService, ArtifactService, CommentService, MinioService
**Feign клиенты:** SprintServiceClient, UserServiceClient, NotificationServiceClient

### 7️⃣ Notification Service (порт 8085)
**Назначение:** Push-уведомления через Firebase
**Классы:** 11 классов
**Entity:** Notification
**Service:** NotificationService (Firebase FCM)

---

## 📊 Детальное описание в файле `MICROSERVICES_PART1.md`

Полное описание первых 3 сервисов с примерами кода.
