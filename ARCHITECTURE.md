# Архитектура системы Sprint Approve

## Обзор

Sprint Approve - это микросервисная система для управления и согласования задач в спринтах и МВП. Система построена на основе Spring Cloud и использует паттерны микросервисной архитектуры.

## Диаграмма архитектуры

```
┌─────────────────────────────────────────────────────────────┐
│                         Клиенты                              │
│                    (Web, Mobile, API)                        │
└────────────────────────┬────────────────────────────────────┘
                         │
                         ▼
┌─────────────────────────────────────────────────────────────┐
│                    API Gateway (8080)                        │
│              Маршрутизация и балансировка                    │
└────────────┬──────────────┬──────────────┬──────────────────┘
             │              │              │
    ┌────────▼─────┐ ┌─────▼──────┐ ┌────▼──────────┐
    │ Team Service │ │   Sprint   │ │ Task Service  │
    │   (8081)     │ │  Service   │ │    (8083)     │
    │              │ │   (8082)   │ │               │
    └──────┬───────┘ └─────┬──────┘ └───────┬───────┘
           │               │                 │
    ┌──────▼───────┐ ┌────▼────────┐ ┌──────▼────────┐
    │   team_db    │ │  sprint_db  │ │   task_db     │
    │ PostgreSQL   │ │ PostgreSQL  │ │  PostgreSQL   │
    └──────────────┘ └─────────────┘ └───────────────┘

┌─────────────────────────────────────────────────────────────┐
│              Eureka Server (8761)                            │
│              Service Discovery                               │
└─────────────────────────────────────────────────────────────┘
```

## Компоненты системы

### 1. Eureka Server
**Назначение:** Service Discovery и регистрация сервисов

**Функции:**
- Регистрация всех микросервисов
- Мониторинг состояния сервисов
- Предоставление информации о доступных сервисах

**Технологии:** Spring Cloud Netflix Eureka

### 2. API Gateway
**Назначение:** Единая точка входа для всех клиентских запросов

**Функции:**
- Маршрутизация запросов к соответствующим микросервисам
- Балансировка нагрузки
- Централизованная обработка CORS
- Логирование запросов

**Технологии:** Spring Cloud Gateway

**Маршруты:**
- `/api/teams/**`, `/api/users/**` → Team Service
- `/api/sprints/**` → Sprint Service
- `/api/tasks/**`, `/api/artifacts/**`, `/api/comments/**` → Task Service

### 3. Team Service
**Назначение:** Управление командами и пользователями

**Основные сущности:**
- **Team** - команды разработки
- **User** - пользователи системы с ролями

**Роли пользователей:**
- `TEAM_LEAD` - руководитель команды
- `DEVELOPER` - разработчик
- `MANAGER` - менеджер
- `APPROVER` - ответственный за согласование

**База данных:** team_db (PostgreSQL)

**API endpoints:**
- CRUD операции для команд
- CRUD операции для пользователей
- Получение пользователей по команде

### 4. Sprint Service
**Назначение:** Управление спринтами и МВП

**Основные сущности:**
- **Sprint** - спринт или МВП

**Типы спринтов:**
- `SPRINT` - обычный спринт
- `MVP` - минимально жизнеспособный продукт

**Статусы:**
- `CREATED` - создан
- `ON_REVIEW` - на рассмотрении
- `APPROVED` - одобрен
- `REJECTED` - отклонен

**База данных:** sprint_db (PostgreSQL)

**Интеграции:**
- Team Service (получение информации о команде и создателе)
- Task Service (проверка статусов задач для автоматического одобрения)

**API endpoints:**
- CRUD операции для спринтов
- Изменение статусов спринтов
- Фильтрация по команде и статусу

### 5. Task Service
**Назначение:** Управление задачами, артефактами и комментариями

**Основные сущности:**
- **Task** - задача в спринте
- **Artifact** - артефакт (файл, ссылка) прикрепленный к задаче
- **Comment** - комментарий к задаче

**Статусы задач:**
- `CREATED` - создана
- `ON_REVIEW` - на рассмотрении
- `APPROVED` - одобрена
- `REJECTED` - отклонена

**База данных:** task_db (PostgreSQL)

**Интеграции:**
- Team Service (получение информации о пользователях)
- Sprint Service (автоматическое одобрение спринта)

**API endpoints:**
- CRUD операции для задач
- CRUD операции для артефактов
- CRUD операции для комментариев
- Изменение статусов задач
- Фильтрация по спринту, статусу, исполнителю

## Паттерны и принципы

### 1. Database per Service
Каждый микросервис имеет свою собственную базу данных:
- Изоляция данных
- Независимое масштабирование
- Технологическая гибкость

### 2. Service Discovery
Использование Eureka для динамического обнаружения сервисов:
- Автоматическая регистрация сервисов
- Health checks
- Load balancing

### 3. API Gateway Pattern
Единая точка входа для всех клиентов:
- Упрощение клиентской логики
- Централизованная маршрутизация
- Возможность добавления cross-cutting concerns

### 4. Synchronous Communication
Межсервисное взаимодействие через OpenFeign:
- Декларативные REST клиенты
- Интеграция с Eureka для service discovery
- Автоматическая балансировка нагрузки

## Бизнес-процессы

### Процесс согласования спринта

```
1. Создание спринта (статус: CREATED)
   ↓
2. Добавление задач в спринт
   ↓
3. Назначение исполнителей и аппруверов
   ↓
4. Отправка задач на рассмотрение (статус: ON_REVIEW)
   ↓
5. Согласование задач аппруверами
   ├─→ APPROVED
   └─→ REJECTED
   ↓
6. Автоматическое одобрение спринта
   (когда все задачи одобрены)
```

### Процесс работы с задачей

```
1. Создание задачи (статус: CREATED)
   ↓
2. Добавление артефактов и комментариев
   ↓
3. Отправка на рассмотрение (статус: ON_REVIEW)
   ↓
4. Проверка аппрувером
   ├─→ Одобрение (APPROVED)
   │   ↓
   │   Проверка всех задач спринта
   │   ↓
   │   Автоматическое одобрение спринта
   │
   └─→ Отклонение (REJECTED)
       ↓
       Возврат на доработку
```

## Модель данных

### Team Service

```
Team
├── id (PK)
├── name (unique)
├── description
├── created_at
└── updated_at

User
├── id (PK)
├── email (unique)
├── name
├── team_id (FK → Team)
├── role (enum)
├── created_at
└── updated_at
```

### Sprint Service

```
Sprint
├── id (PK)
├── name
├── description
├── team_id (reference to Team Service)
├── type (SPRINT | MVP)
├── status (CREATED | ON_REVIEW | APPROVED | REJECTED)
├── start_date
├── end_date
├── created_by (reference to User)
├── created_at
└── updated_at
```

### Task Service

```
Task
├── id (PK)
├── title
├── description
├── sprint_id (reference to Sprint Service)
├── status (CREATED | ON_REVIEW | APPROVED | REJECTED)
├── assigned_to (reference to User)
├── approver_id (reference to User)
├── created_by (reference to User)
├── created_at
└── updated_at

Artifact
├── id (PK)
├── name
├── url
├── file_type
├── file_size
├── task_id (FK → Task)
├── uploaded_by (reference to User)
└── created_at

Comment
├── id (PK)
├── content
├── task_id (FK → Task)
├── author_id (reference to User)
├── created_at
└── updated_at
```

## Безопасность

### Текущая реализация
- Базовая валидация данных через Bean Validation
- Проверка прав на уровне бизнес-логики (например, только автор может удалить комментарий)

### Рекомендации для продакшена
- Добавить Spring Security
- Реализовать JWT аутентификацию
- Настроить HTTPS
- Добавить rate limiting в API Gateway
- Реализовать RBAC (Role-Based Access Control)

## Масштабирование

### Горизонтальное масштабирование
Каждый микросервис может быть масштабирован независимо:
```bash
# Запуск нескольких экземпляров
java -jar team-service.jar --server.port=8081
java -jar team-service.jar --server.port=8091
java -jar team-service.jar --server.port=8101
```

API Gateway автоматически распределит нагрузку между экземплярами.

### Вертикальное масштабирование
Увеличение ресурсов для отдельных сервисов:
```bash
java -Xmx2g -Xms1g -jar service.jar
```

## Мониторинг и логирование

### Рекомендуемые инструменты
- **Spring Boot Actuator** - метрики и health checks
- **Sleuth + Zipkin** - distributed tracing
- **ELK Stack** - централизованное логирование
- **Prometheus + Grafana** - мониторинг метрик

## Развертывание

### Docker
Каждый сервис может быть упакован в Docker контейнер:
```dockerfile
FROM openjdk:17-jdk-slim
COPY target/*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]
```

### Kubernetes
Для продакшена рекомендуется использовать Kubernetes:
- Автоматическое масштабирование
- Self-healing
- Service discovery
- Load balancing

## Дальнейшее развитие

### Возможные улучшения
1. **Event-Driven Architecture** - добавить Kafka/RabbitMQ для асинхронной коммуникации
2. **CQRS** - разделение команд и запросов
3. **Circuit Breaker** - добавить Resilience4j для fault tolerance
4. **API Versioning** - версионирование API
5. **Caching** - добавить Redis для кеширования
6. **File Storage** - интеграция с S3 для хранения артефактов
7. **Notifications** - сервис уведомлений (email, push)
8. **Analytics** - сервис аналитики и отчетности
