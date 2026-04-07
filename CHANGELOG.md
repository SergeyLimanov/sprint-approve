# Changelog

Все значимые изменения в проекте Sprint Approve будут документированы в этом файле.

Формат основан на [Keep a Changelog](https://keepachangelog.com/ru/1.0.0/),
и этот проект придерживается [Semantic Versioning](https://semver.org/lang/ru/).

## [1.0.0] - 2026-04-07

### Добавлено

#### Инфраструктура
- Настроен multi-module Maven проект
- Добавлен Eureka Server для service discovery
- Добавлен API Gateway для маршрутизации запросов
- Настроен Docker Compose для PostgreSQL баз данных
- Добавлены GitHub Actions для CI/CD

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
- Автоматическое одобрение спринта при одобрении всех задач
- Фильтрация по команде и статусу

#### Task Service
- CRUD операции для задач
- CRUD операции для артефактов
- CRUD операции для комментариев
- Статусы задач: CREATED, ON_REVIEW, APPROVED, REJECTED
- Назначение исполнителей и аппруверов
- Интеграция с Team Service для получения информации о пользователях
- Интеграция с Sprint Service для автоматического обновления статуса спринта
- Фильтрация задач по спринту, статусу, исполнителю

#### Документация
- README.md с полным описанием проекта
- ARCHITECTURE.md с описанием архитектуры
- DEVELOPMENT.md с руководством по разработке
- USAGE_EXAMPLES.md с примерами использования
- QUICKSTART.md для быстрого старта
- api-examples.http с примерами API запросов

#### Утилиты
- Скрипты start-all.bat и stop-all.bat для Windows
- Файл .env.example с примерами переменных окружения
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

## [Unreleased]

### Планируется

#### Безопасность
- Добавить Spring Security
- Реализовать JWT аутентификацию
- Настроить HTTPS
- Добавить rate limiting

#### Функциональность
- Сервис уведомлений (email, push)
- Сервис аналитики и отчетности
- Интеграция с S3 для хранения файлов
- Поддержка вложенных комментариев
- История изменений задач
- Теги и метки для задач
- Поиск по задачам и спринтам

#### Улучшения
- Event-Driven Architecture с Kafka
- Circuit Breaker с Resilience4j
- Distributed tracing с Sleuth и Zipkin
- Централизованное логирование с ELK Stack
- Мониторинг с Prometheus и Grafana
- Кеширование с Redis

#### Тестирование
- Unit тесты для всех сервисов
- Integration тесты
- E2E тесты
- Performance тесты

---

## Типы изменений

- **Добавлено** - для новой функциональности
- **Изменено** - для изменений в существующей функциональности
- **Устарело** - для функциональности, которая скоро будет удалена
- **Удалено** - для удаленной функциональности
- **Исправлено** - для исправления ошибок
- **Безопасность** - для изменений, связанных с безопасностью
