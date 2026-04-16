# Changelog

All notable changes to this project will be documented in this file.

## [2.0.0] - 2026-04-16

### 🔒 Security Features Added

#### New Components
- **auth-service** - Новый микросервис для аутентификации и авторизации
  - JWT токены (access и refresh)
  - BCrypt хеширование паролей
  - Endpoints для login, register, refresh, validate
- **security-common** - Общая библиотека для работы с Security Context
  - SecurityContext для доступа к информации о пользователе
  - SecurityFilter для извлечения данных из заголовков

#### Enhanced Components
- **api-gateway** - Добавлена JWT аутентификация
  - JwtAuthenticationFilter для проверки токенов
  - Автоматическое добавление заголовков X-User-Id, X-User-Email, X-User-Role
  - Защита всех endpoints (кроме /api/auth/**)
- **team-service** - Добавлена поддержка паролей
  - Поле password в User entity
  - Endpoint GET /api/users/email/{email} для аутентификации
- **All microservices** - Интеграция с security-common

#### Documentation
- **SECURITY.md** - Полная документация по безопасности
- **MIGRATION_GUIDE.md** - Руководство по миграции
- **api-examples-with-auth.http** - Примеры API запросов с аутентификацией
- **migration.sql** - SQL скрипт для миграции БД

### ⚠️ Breaking Changes
- Все API endpoints (кроме /api/auth/**) теперь требуют JWT аутентификацию
- User entity требует поле password
- Необходима миграция БД для существующих пользователей

### 📝 Migration Required
См. [MIGRATION_GUIDE.md](MIGRATION_GUIDE.md) для инструкций по миграции.

---

## [1.1.0] - 2026-04-16

### ✨ Features

#### Auto Status Synchronization
- **sprint-service** - Автоматический пересчет статуса спринта
  - Метод `recalculateSprintStatus()` для пересчета на основе задач
  - Endpoint PATCH /api/sprints/{id}/recalculate-status
  - Логика приоритетов: APPROVED > REJECTED > ON_REVIEW > CREATED
- **task-service** - Автоматический вызов пересчета при изменении задач
  - Пересчет при создании, обновлении, удалении задачи
  - Пересчет при изменении статуса (submit, approve, reject)

#### Documentation
- **AUTO_STATUS_SYNC.md** - Документация по автосинхронизации
- **TESTING_AUTO_SYNC.md** - Инструкции по тестированию

### 🐛 Bug Fixes
- Исправлена проблема с несинхронизированными статусами спринтов и задач

---

## [1.0.0] - Initial Release

### Добавлено

#### Инфраструктура
- Настроен multi-module Maven проект
- Добавлен Eureka Server для service discovery
- Добавлен API Gateway для маршрутизации запросов
- Настроен Docker Compose для PostgreSQL баз данных

#### Team Service
- CRUD операции для команд
- CRUD операции для пользователей
- Роли пользователей: TEAM_LEAD, DEVELOPER, MANAGER, APPROVER
- Связь пользователей с командами
- Валидация email и уникальности имен

#### Sprint Service
- CRUD операции для спринтов
- Поддержка типов: SPRINT и MVP
- Статусы: CREATED, ON_REVIEW, APPROVED, REJECTED
- Интеграция с Team Service через Feign
- Интеграция с Task Service для проверки задач
- Фильтрация по команде и статусу

#### Task Service
- CRUD операции для задач
- CRUD операции для артефактов
- CRUD операции для комментариев
- Статусы задач: CREATED, ON_REVIEW, APPROVED, REJECTED
- Назначение исполнителей и аппруверов
- Интеграция с Team Service для получения информации о пользователях
- Фильтрация задач по спринту, статусу, исполнителю

#### Документация
- README.md с полным описанием проекта
- ARCHITECTURE.md с описанием архитектуры
- DEVELOPMENT.md с руководством по разработке
- USAGE_EXAMPLES.md с примерами использования
- api-examples.http с примерами API запросов

#### Утилиты
- Скрипты start-all.bat и stop-all.bat для Windows
- Swagger UI для всех микросервисов

### Технологии
- Java 17
- Spring Boot 3.2.0
- Spring Cloud 2023.0.0
- Spring Data JPA
- PostgreSQL
- Netflix Eureka
- Spring Cloud Gateway
- OpenFeign
- Lombok
- Springdoc OpenAPI
