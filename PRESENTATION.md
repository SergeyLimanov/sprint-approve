# 🚀 Sprint Approve - Презентация проекта

## 📋 Содержание
1. [Обзор проекта](#обзор-проекта)
2. [Архитектура](#архитектура)
3. [Технологический стек](#технологический-стек)
4. [Микросервисы](#микросервисы)
5. [Интеграции](#интеграции)
6. [Безопасность](#безопасность)
7. [Инфраструктура](#инфраструктура)
8. [Функциональность](#функциональность)
9. [Демонстрация](#демонстрация)

---

## 🎯 Обзор проекта

### Что это?
**Sprint Approve** — система управления задачами с механизмом одобрения для команд разработки.

### Проблема:
- Нет контроля качества выполненных задач
- Отсутствие прозрачности в процессе review
- Сложно отслеживать статус задач в спринте
- Нет централизованного хранения артефактов

### Решение:
- ✅ Workflow с обязательным одобрением задач
- ✅ Автоматическая синхронизация статусов
- ✅ Прикрепление файлов и комментариев
- ✅ Push-уведомления о событиях
- ✅ JWT аутентификация и авторизация

---

## 🏗️ Архитектура

### Микросервисная архитектура (Microservices)

```
                    ┌─────────────────────────────────────┐
                    │   Frontend (React + TypeScript)    │
                    │   http://localhost:3000             │
                    └──────────────┬──────────────────────┘
                                   │
                    ┌──────────────▼──────────────────────┐
                    │   API Gateway (Spring Cloud)        │
                    │   http://localhost:8080             │
                    │   - JWT Validation                  │
                    │   - Routing                         │
                    └──────────────┬──────────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
┌───────▼────────┐      ┌──────────▼─────────┐      ┌────────▼────────┐
│ Eureka Server  │      │  Auth Service      │      │  Team Service   │
│ :8761          │◄─────┤  :8084             │◄─────┤  :8081          │
│ Service        │      │  - Login           │      │  - Users        │
│ Discovery      │      │  - Register        │      │  - Teams        │
└────────────────┘      │  - JWT Generation  │      │  - Roles        │
                        └────────────────────┘      └─────────────────┘
                                   │
        ┌──────────────────────────┼──────────────────────────┐
        │                          │                          │
┌───────▼────────┐      ┌──────────▼─────────┐      ┌────────▼────────┐
│ Sprint Service │      │  Task Service      │      │ Notification    │
│ :8082          │◄────►│  :8083             │      │ Service :8085   │
│ - Sprints      │      │  - Tasks           │      │ - FCM Push      │
│ - Status Sync  │      │  - Artifacts       │      │ - User Tokens   │
└────────┬───────┘      │  - Comments        │      └─────────────────┘
         │              └────────┬───────────┘
         │                       │
         │                       │
    ┌────▼────┐            ┌────▼────┐           ┌──────────┐
    │sprint_db│            │task_db  │           │MinIO     │
    │PostgreSQL            │PostgreSQL           │File      │
    └─────────┘            └─────────┘           │Storage   │
                                                  └──────────┘
```

### Ключевые принципы:
- **Service Discovery** (Eureka) - автоматическое обнаружение сервисов
- **API Gateway** - единая точка входа
- **Database per Service** - каждый сервис имеет свою БД
- **Inter-service Communication** - OpenFeign (REST)
- **Distributed Security** - JWT токены

---

## 💻 Технологический стек

### Backend

#### Core Framework:
```
Spring Boot 3.2.0
├── Spring Cloud 2023.0.0
│   ├── Spring Cloud Gateway
│   ├── Netflix Eureka (Service Discovery)
│   └── OpenFeign (HTTP Client)
├── Spring Security
├── Spring Data JPA
└── Spring Web
```

#### Языки и инструменты:
- **Java 17** - основной язык
- **Maven** - сборка и управление зависимостями
- **Lombok** - уменьшение boilerplate кода

#### Базы данных:
- **PostgreSQL 15** - основная БД (4 инстанса)
- **Hibernate** - ORM
- **Flyway** - миграции БД (опционально)

#### Безопасность:
- **Spring Security** - фреймворк безопасности
- **JWT (JJWT 0.12.3)** - токены аутентификации
- **BCrypt** - хеширование паролей

#### Файловое хранилище:
- **MinIO** - S3-совместимое хранилище
- **MinIO SDK 8.5.7** - Java клиент

#### Уведомления:
- **Firebase Admin SDK 9.2.0** - push-уведомления
- **FCM (Firebase Cloud Messaging)** - доставка уведомлений

#### Документация API:
- **SpringDoc OpenAPI 2.3.0** - Swagger UI
- **OpenAPI 3.0** - спецификация API

---

### Frontend

#### Core Framework:
```
React 18.2.0
├── TypeScript 5.2.2
├── Vite 5.0.8 (Build Tool)
└── React Router 6.20.0
```

#### UI/UX:
- **TailwindCSS 3.3.6** - utility-first CSS
- **Lucide React 0.294.0** - иконки
- **PostCSS** - CSS processing

#### HTTP & State:
- **Axios 1.6.2** - HTTP клиент
- **date-fns 3.0.0** - работа с датами

#### Push Notifications:
- **Firebase SDK** - FCM для веб
- **Service Workers** - фоновые уведомления

---

### Infrastructure

#### Containerization:
- **Docker** - контейнеризация
- **Docker Compose** - оркестрация контейнеров

#### Databases:
```yaml
PostgreSQL 15 (Alpine):
├── team-db (port 5432)
├── sprint-db (port 5433)
├── task-db (port 5434)
└── notification-db (port 5435)
```

#### Storage:
- **MinIO** (latest)
  - API: port 9000
  - Console: port 9001

#### Networking:
- **Docker Network** - изолированная сеть сервисов
- **Docker Volumes** - persistent storage

---

## 🔧 Микросервисы

### 1. Eureka Server (Service Discovery)
**Порт:** 8761  
**Роль:** Регистрация и обнаружение сервисов

**Технологии:**
- Spring Cloud Netflix Eureka Server

**Функции:**
- Регистрация всех микросервисов
- Health checks
- Load balancing metadata

---

### 2. API Gateway
**Порт:** 8080  
**Роль:** Единая точка входа, маршрутизация, аутентификация

**Технологии:**
- Spring Cloud Gateway
- JWT Validation

**Функции:**
- Маршрутизация запросов к сервисам
- JWT валидация (кроме `/api/auth/**`)
- Добавление заголовков `X-User-Id`, `X-User-Email`, `X-User-Role`
- CORS configuration

**Routes:**
```yaml
/api/auth/**      → auth-service (без JWT)
/api/users/**     → team-service (с JWT)
/api/teams/**     → team-service (с JWT)
/api/sprints/**   → sprint-service (с JWT)
/api/tasks/**     → task-service (с JWT)
/api/artifacts/** → task-service (с JWT)
/api/comments/**  → task-service (с JWT)
```

---

### 3. Auth Service
**Порт:** 8084  
**База данных:** Нет (использует team-service)

**Технологии:**
- Spring Security
- JJWT
- BCrypt
- OpenFeign

**Функции:**
- Регистрация пользователей
- Аутентификация (login)
- Генерация JWT токенов (access + refresh)
- Обновление токенов (refresh)
- Валидация токенов

**API Endpoints:**
```
POST /api/auth/register
POST /api/auth/login
POST /api/auth/refresh
POST /api/auth/validate
```

**JWT Payload:**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "role": "DEVELOPER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

---

### 4. Team Service
**Порт:** 8081  
**База данных:** team_db (PostgreSQL)

**Технологии:**
- Spring Data JPA
- PostgreSQL
- Security Common Library

**Entities:**
- **User** - пользователи системы
- **Team** - команды разработки

**Функции:**
- CRUD пользователей
- CRUD команд
- Управление ролями (DEVELOPER, APPROVER, TEAM_LEAD)
- Назначение пользователей в команды

**API Endpoints:**
```
GET    /api/users
GET    /api/users/{id}
GET    /api/users/email/{email}
POST   /api/users
PUT    /api/users/{id}
DELETE /api/users/{id}

GET    /api/teams
GET    /api/teams/{id}
POST   /api/teams
PUT    /api/teams/{id}
DELETE /api/teams/{id}
```

**User Roles:**
- `DEVELOPER` - создаёт задачи
- `APPROVER` - одобряет задачи
- `TEAM_LEAD` - управляет командой

---

### 5. Sprint Service
**Порт:** 8082  
**База данных:** sprint_db (PostgreSQL)

**Технологии:**
- Spring Data JPA
- OpenFeign (интеграция с task-service, team-service)

**Entities:**
- **Sprint** - спринты разработки

**Функции:**
- CRUD спринтов
- Автоматический пересчёт статуса спринта
- Одобрение спринта (если все задачи одобрены)
- Интеграция с task-service для получения задач

**API Endpoints:**
```
GET    /api/sprints
GET    /api/sprints/{id}
GET    /api/sprints/team/{teamId}
POST   /api/sprints
PUT    /api/sprints/{id}
DELETE /api/sprints/{id}
PATCH  /api/sprints/{id}/approve
PATCH  /api/sprints/{id}/recalculate-status
```

**Sprint Statuses:**
- `CREATED` - создан
- `ON_REVIEW` - на проверке (есть задачи на review)
- `APPROVED` - одобрен (все задачи одобрены)
- `REJECTED` - отклонён (есть отклонённые задачи)

**Автоматическая синхронизация:**
```
Task создана/изменена
    ↓
Task Service вызывает Sprint Service
    ↓
Sprint Service пересчитывает статус
    ↓
Статус спринта обновляется автоматически
```

---

### 6. Task Service
**Порт:** 8083  
**База данных:** task_db (PostgreSQL)

**Технологии:**
- Spring Data JPA
- MinIO SDK
- OpenFeign (интеграция с sprint-service, team-service)

**Entities:**
- **Task** - задачи
- **Artifact** - файлы (документы, скриншоты, видео)
- **Comment** - комментарии к задачам

**Функции:**
- CRUD задач
- Загрузка и хранение файлов в MinIO
- Комментарии к задачам
- Одобрение/отклонение задач
- Автоматическая синхронизация со Sprint Service

**API Endpoints:**
```
# Tasks
GET    /api/tasks
GET    /api/tasks/{id}
GET    /api/tasks/sprint/{sprintId}
POST   /api/tasks
PUT    /api/tasks/{id}
DELETE /api/tasks/{id}
PATCH  /api/tasks/{id}/approve
PATCH  /api/tasks/{id}/reject
PATCH  /api/tasks/{id}/submit

# Artifacts
GET    /api/artifacts/task/{taskId}
GET    /api/artifacts/{id}
POST   /api/artifacts/upload
DELETE /api/artifacts/{id}
GET    /api/artifacts/{id}/download-url

# Comments
GET    /api/comments/task/{taskId}
GET    /api/comments/{id}
POST   /api/comments
PUT    /api/comments/{id}
DELETE /api/comments/{id}
```

**Task Statuses:**
- `CREATED` - создана
- `ON_REVIEW` - на проверке
- `APPROVED` - одобрена
- `REJECTED` - отклонена

**File Upload Flow:**
```
1. Frontend отправляет файл (multipart/form-data)
2. Task Service сохраняет в MinIO
3. Генерируется presigned URL (временная ссылка)
4. Метаданные сохраняются в PostgreSQL
5. Frontend получает URL для скачивания
```

---

### 7. Notification Service
**Порт:** 8085  
**База данных:** notification_db (PostgreSQL)

**Технологии:**
- Firebase Admin SDK
- Spring Data JPA

**Entities:**
- **UserFcmToken** - FCM токены пользователей

**Функции:**
- Регистрация FCM токенов
- Отправка push-уведомлений
- Управление подписками

**API Endpoints:**
```
POST   /api/notifications/register-token
DELETE /api/notifications/token/{userId}
POST   /api/notifications/send
```

**Типы уведомлений:**
1. `TASK_ASSIGNED` - задача назначена
2. `TASK_APPROVED` - задача одобрена
3. `TASK_REJECTED` - задача отклонена
4. `NEW_COMMENT` - новый комментарий
5. `NEW_ARTIFACT` - новый файл
6. `TASK_FOR_REVIEW` - задача на проверке
7. `NEW_TASK_IN_SPRINT` - новая задача в спринте

---

## 🔗 Интеграции

### 1. OpenFeign (Inter-service Communication)

#### auth-service → team-service
```java
@FeignClient(name = "team-service")
public interface TeamServiceClient {
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable String email);
    
    @PostMapping("/api/users")
    UserDto createUser(@RequestBody UserDto userDto);
}
```

**Зачем:** Аутентификация и регистрация пользователей

---

#### task-service → sprint-service
```java
@FeignClient(name = "sprint-service")
public interface SprintServiceClient {
    @PatchMapping("/api/sprints/{id}/recalculate-status")
    SprintDto recalculateSprintStatus(@PathVariable Long id);
}
```

**Зачем:** Автоматический пересчёт статуса спринта при изменении задач

---

#### task-service → team-service
```java
@FeignClient(name = "team-service")
public interface UserServiceClient {
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable Long id);
}
```

**Зачем:** Обогащение данных задач именами пользователей

---

#### sprint-service → task-service
```java
@FeignClient(name = "task-service")
public interface TaskServiceClient {
    @GetMapping("/api/tasks/sprint/{sprintId}")
    List<TaskDto> getTasksBySprintId(@PathVariable Long sprintId);
}
```

**Зачем:** Получение задач для пересчёта статуса спринта

---

#### sprint-service → team-service
```java
@FeignClient(name = "team-service")
public interface TeamServiceClient {
    @GetMapping("/api/teams/{id}")
    TeamDto getTeamById(@PathVariable Long id);
    
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable Long id);
}
```

**Зачем:** Валидация команд и обогащение данных

---

### 2. MinIO Integration

**Workflow загрузки файла:**
```
1. POST /api/artifacts/upload (multipart/form-data)
2. MinioStorageService.storeFile(file)
3. MinIO сохраняет файл с UUID именем
4. Генерируется presigned URL (срок действия 60 мин)
5. Метаданные сохраняются в PostgreSQL
6. Возвращается ArtifactDto с downloadUrl
```

**Преимущества:**
- ✅ S3-совместимое хранилище
- ✅ Масштабируемость
- ✅ Временные ссылки (presigned URLs)
- ✅ Безопасность (файлы не в контейнере)
- ✅ Persistent storage (Docker volumes)

---

### 3. Firebase Cloud Messaging (FCM)

**Workflow уведомлений:**
```
1. Frontend регистрирует FCM токен
2. POST /api/notifications/register-token
3. Токен сохраняется в notification_db
4. При событии (задача одобрена):
   - Task Service вызывает Notification Service (Feign)
   - Notification Service отправляет FCM уведомление
   - FCM доставляет push в браузер пользователя
```

**Service Worker (фоновые уведомления):**
```javascript
// public/firebase-messaging-sw.js
messaging.onBackgroundMessage((payload) => {
  self.registration.showNotification(
    payload.notification.title,
    {
      body: payload.notification.body,
      icon: '/logo.png',
      data: payload.data
    }
  );
});
```

---

## 🔐 Безопасность

### 1. JWT Authentication

**Flow:**
```
1. POST /api/auth/login
   ↓
2. Auth Service проверяет пароль (BCrypt)
   ↓
3. Генерируется Access Token (24h) + Refresh Token (7d)
   ↓
4. Frontend сохраняет токены
   ↓
5. Все запросы: Authorization: Bearer <token>
   ↓
6. API Gateway валидирует JWT
   ↓
7. Добавляет заголовки X-User-Id, X-User-Email, X-User-Role
   ↓
8. Микросервисы используют SecurityContext
```

**JWT Payload:**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "role": "DEVELOPER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

**Алгоритм:** HS256 (HMAC with SHA-256)  
**Secret Key:** Хранится в переменной окружения `JWT_SECRET`

---

### 2. Security Common Library

**Компоненты:**
- `SecurityFilter` - извлекает данные из заголовков
- `SecurityContext` - ThreadLocal контекст пользователя
- `MicroserviceSecurityConfig` - базовая конфигурация

**Использование в сервисах:**
```java
SecurityContext context = SecurityContext.get();
Long userId = context.getUserId();
String role = context.getRole();
boolean isApprover = context.hasRole("APPROVER");
```

---

### 3. Password Security

**BCrypt hashing:**
```java
// При регистрации
String hashedPassword = passwordEncoder.encode(plainPassword);

// При входе
boolean matches = passwordEncoder.matches(plainPassword, hashedPassword);
```

**Пример хеша:**
```
Пароль: "changeme"
Хеш: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
```

---

### 4. .gitignore Protection

**Защищённые файлы:**
```gitignore
.env                          # JWT_SECRET, пароли БД
firebase-service-account.json # Firebase приватные ключи
*.pem, *.key                  # SSL сертификаты
*.jks, *.keystore             # Java keystores
*.log                         # Логи
*.dump, *.backup              # Дампы БД
uploads/                      # Файлы пользователей
```

---

## 🐳 Инфраструктура

### Docker Compose

**Сервисы:**
```yaml
services:
  team-db:        # PostgreSQL для team-service
  sprint-db:      # PostgreSQL для sprint-service
  task-db:        # PostgreSQL для task-service
  notification-db: # PostgreSQL для notification-service
  minio:          # Файловое хранилище
```

**Volumes (Persistent Storage):**
```yaml
volumes:
  team-db-data:
  sprint-db-data:
  task-db-data:
  notification-db-data:
  minio-data:
```

**Network:**
```yaml
networks:
  sprint-approve-network:
    driver: bridge
```

---

### Запуск приложения

**1. Запуск инфраструктуры:**
```bash
docker-compose up -d
```

**2. Запуск микросервисов (по порядку):**
```bash
# 1. Service Discovery
cd eureka-server && mvn spring-boot:run

# 2. Auth Service
cd auth-service && mvn spring-boot:run

# 3. Business Services
cd team-service && mvn spring-boot:run
cd sprint-service && mvn spring-boot:run
cd task-service && mvn spring-boot:run
cd notification-service && mvn spring-boot:run

# 4. API Gateway
cd api-gateway && mvn spring-boot:run
```

**3. Запуск Frontend:**
```bash
cd frontend
npm install
npm run dev
```

**Доступ:**
- Frontend: http://localhost:3000
- API Gateway: http://localhost:8080
- Eureka Dashboard: http://localhost:8761
- MinIO Console: http://localhost:9001

---

## ✨ Функциональность

### 1. Управление пользователями
- Регистрация и аутентификация
- Роли: DEVELOPER, APPROVER, TEAM_LEAD
- Управление профилем

### 2. Управление командами
- Создание команд
- Назначение пользователей в команды
- Просмотр участников команды

### 3. Управление спринтами
- Создание спринтов
- Привязка к команде
- Автоматический пересчёт статуса
- Одобрение спринта

### 4. Управление задачами
- Создание задач
- Назначение исполнителя и аппрувера
- Отправка на проверку
- Одобрение/отклонение
- Прикрепление файлов
- Комментирование

### 5. Файловое хранилище
- Загрузка файлов (изображения, документы, видео)
- Хранение в MinIO
- Временные ссылки для скачивания
- Метаданные в БД

### 6. Push-уведомления
- Уведомления о назначении задачи
- Уведомления об одобрении/отклонении
- Уведомления о новых комментариях
- Уведомления о новых файлах
- Фоновые уведомления (Service Worker)

---

## 🎬 Демонстрация

### Сценарий 1: Создание и одобрение задачи

**1. Регистрация пользователей:**
```bash
POST /api/auth/register
{
  "email": "developer@example.com",
  "password": "password123",
  "name": "John Developer",
  "role": "DEVELOPER"
}

POST /api/auth/register
{
  "email": "approver@example.com",
  "password": "password123",
  "name": "Jane Approver",
  "role": "APPROVER"
}
```

**2. Создание команды:**
```bash
POST /api/teams
{
  "name": "Backend Team",
  "description": "Backend development team"
}
```

**3. Создание спринта:**
```bash
POST /api/sprints
{
  "name": "Sprint 1",
  "teamId": 1,
  "startDate": "2024-01-15",
  "endDate": "2024-01-29"
}
```

**4. Создание задачи:**
```bash
POST /api/tasks
{
  "title": "Implement user authentication",
  "description": "Add JWT authentication",
  "sprintId": 1,
  "assignedTo": 1,
  "approverId": 2,
  "createdBy": 1
}
```

**5. Загрузка артефакта:**
```bash
POST /api/artifacts/upload
Content-Type: multipart/form-data

file: screenshot.png
taskId: 1
uploadedBy: 1
```

**6. Добавление комментария:**
```bash
POST /api/comments
{
  "taskId": 1,
  "authorId": 1,
  "content": "Authentication implemented, please review"
}
```

**7. Отправка на проверку:**
```bash
PATCH /api/tasks/1/submit
```
→ Статус задачи: `ON_REVIEW`  
→ Статус спринта: `ON_REVIEW`  
→ Push-уведомление аппруверу

**8. Одобрение задачи:**
```bash
PATCH /api/tasks/1/approve?approverId=2
```
→ Статус задачи: `APPROVED`  
→ Статус спринта: `APPROVED` (если все задачи одобрены)  
→ Push-уведомление создателю

---

### Сценарий 2: Автоматическая синхронизация

**Создана задача в спринте:**
```
1. POST /api/tasks (status: CREATED)
2. Task Service → Sprint Service (Feign)
3. Sprint Service пересчитывает статус
4. Sprint status: CREATED
```

**Задача отправлена на проверку:**
```
1. PATCH /api/tasks/1/submit (status: ON_REVIEW)
2. Task Service → Sprint Service
3. Sprint status: ON_REVIEW
```

**Задача одобрена:**
```
1. PATCH /api/tasks/1/approve (status: APPROVED)
2. Task Service → Sprint Service
3. Если все задачи APPROVED → Sprint status: APPROVED
```

---

## 📊 Метрики проекта

### Код:
- **7 микросервисов**
- **~50 REST endpoints**
- **~30 Java классов** (entities, services, controllers)
- **~20 DTO классов**
- **5 Feign клиентов**

### База данных:
- **4 PostgreSQL инстанса**
- **7 таблиц** (users, teams, sprints, tasks, artifacts, comments, user_fcm_tokens)

### Технологии:
- **Backend:** 10+ (Spring Boot, Spring Cloud, JWT, MinIO, Firebase, PostgreSQL)
- **Frontend:** 8+ (React, TypeScript, Vite, TailwindCSS, Axios)
- **Infrastructure:** 3+ (Docker, Docker Compose, MinIO)

---

## 🎯 Ключевые достижения

### Архитектурные:
✅ Микросервисная архитектура  
✅ Service Discovery (Eureka)  
✅ API Gateway с JWT валидацией  
✅ Database per Service  
✅ Inter-service communication (OpenFeign)

### Функциональные:
✅ JWT аутентификация и авторизация  
✅ Автоматическая синхронизация статусов  
✅ Файловое хранилище (MinIO)  
✅ Push-уведомления (FCM)  
✅ Комментарии и артефакты

### Безопасность:
✅ JWT токены (Access + Refresh)  
✅ BCrypt хеширование паролей  
✅ Защита секретов (.gitignore)  
✅ Временные ссылки для файлов (presigned URLs)

### DevOps:
✅ Docker Compose для инфраструктуры  
✅ Persistent storage (Docker volumes)  
✅ Изолированная сеть сервисов

---

## 🚀 Будущие улучшения

### Функциональность:
- [ ] Email уведомления
- [ ] Дедлайны и напоминания
- [ ] Kanban доска
- [ ] Отчёты и аналитика
- [ ] Экспорт данных (PDF, Excel)

### Технологии:
- [ ] Kafka для event-driven архитектуры
- [ ] Redis для кэширования
- [ ] Elasticsearch для поиска
- [ ] Grafana + Prometheus для мониторинга
- [ ] HTTPS (Let's Encrypt)

### DevOps:
- [ ] Kubernetes для оркестрации
- [ ] CI/CD (GitHub Actions)
- [ ] Автоматическое тестирование
- [ ] Blue-Green deployment

---

## 📚 Документация

- `README.md` - общая информация
- `ARCHITECTURE.md` - архитектура проекта
- `SECURITY.md` - безопасность
- `FCM_INTEGRATION_GUIDE.md` - интеграция FCM
- `JWT_SECRET_GUIDE.md` - настройка JWT
- `SECURITY_GITIGNORE_GUIDE.md` - защита секретов
- `DEVELOPMENT.md` - руководство разработчика
- `USAGE_EXAMPLES.md` - примеры использования API

---

## 🎓 Выводы

**Sprint Approve** демонстрирует:
- ✅ Современную микросервисную архитектуру
- ✅ Полный стек технологий (Backend + Frontend + Infrastructure)
- ✅ Best practices безопасности
- ✅ Интеграцию с внешними сервисами (Firebase, MinIO)
- ✅ Готовность к production deployment

**Проект подходит для:**
- Портфолио разработчика
- Курсовой/дипломный проект
- Стартап MVP
- Обучение микросервисам

---

## 📞 Контакты

**GitHub:** https://github.com/SergeyLimanov/sprint-approve  
**Автор:** Sergey Limanov

---

**Спасибо за внимание!** 🚀
