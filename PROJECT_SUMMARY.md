# Sprint Approve - Сводка проекта

## 📋 Обзор

**Sprint Approve** - это полнофункциональная микросервисная система для управления и согласования задач в спринтах и МВП (минимально жизнеспособных продуктах).

## 🎯 Основные возможности

### Управление командами
- ✅ Создание и управление командами
- ✅ Управление пользователями с ролями
- ✅ Роли: Team Lead, Developer, Manager, Approver

### Управление спринтами
- ✅ Создание спринтов и МВП
- ✅ Статусы: Created, On Review, Approved, Rejected
- ✅ Автоматическое одобрение при одобрении всех задач
- ✅ Фильтрация по команде и статусу

### Управление задачами
- ✅ Создание и назначение задач
- ✅ Прикрепление артефактов (файлы, ссылки)
- ✅ Комментарии к задачам
- ✅ Процесс согласования с аппруверами
- ✅ Фильтрация по различным критериям

## 🏗️ Архитектура

### Микросервисы

1. **Eureka Server** (8761) - Service Discovery
2. **API Gateway** (8080) - Единая точка входа
3. **Team Service** (8081) - Управление командами и пользователями
4. **Sprint Service** (8082) - Управление спринтами
5. **Task Service** (8083) - Управление задачами, артефактами, комментариями

### Базы данных

- 3 отдельные PostgreSQL базы данных (Database per Service pattern)
- Автоматическое создание схемы через Hibernate DDL

### Технологии

- **Backend:** Java 17, Spring Boot 3.2.0, Spring Cloud 2023.0.0
- **Database:** PostgreSQL 15
- **Service Discovery:** Netflix Eureka
- **API Gateway:** Spring Cloud Gateway
- **Inter-service Communication:** OpenFeign
- **Documentation:** Springdoc OpenAPI (Swagger)
- **Containerization:** Docker & Docker Compose

## 📁 Структура проекта

```
sprint-approve/
├── eureka-server/          # Service Discovery
├── api-gateway/            # API Gateway
├── team-service/           # Команды и пользователи
├── sprint-service/         # Спринты и МВП
├── task-service/           # Задачи, артефакты, комментарии
├── docker-compose.yml      # БД конфигурация
├── pom.xml                 # Родительский POM
├── README.md               # Основная документация
├── QUICKSTART.md           # Быстрый старт
├── ARCHITECTURE.md         # Архитектура
├── DEVELOPMENT.md          # Руководство разработчика
├── USAGE_EXAMPLES.md       # Примеры использования
├── CHANGELOG.md            # История изменений
└── api-examples.http       # HTTP примеры
```

## 🚀 Быстрый старт

### 1. Запуск баз данных
```bash
docker-compose up -d
```

### 2. Сборка проекта
```bash
mvn clean install -DskipTests
```

### 3. Запуск сервисов
```bash
# Windows
start-all.bat

# Или вручную в 5 терминалах
cd eureka-server && mvn spring-boot:run
cd team-service && mvn spring-boot:run
cd sprint-service && mvn spring-boot:run
cd task-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

### 4. Проверка
- Eureka: http://localhost:8761
- API Gateway: http://localhost:8080
- Swagger UI: http://localhost:8081/swagger-ui.html

## 📊 Статистика проекта

### Микросервисы: 5
- Eureka Server
- API Gateway
- Team Service
- Sprint Service
- Task Service

### Entities: 7
- Team
- User
- Sprint
- Task
- Artifact
- Comment
- + Enums (UserRole, SprintType, SprintStatus, TaskStatus)

### REST Endpoints: 40+
- Teams: 5 endpoints
- Users: 6 endpoints
- Sprints: 9 endpoints
- Tasks: 10 endpoints
- Artifacts: 4 endpoints
- Comments: 5 endpoints

### Файлов документации: 8
- README.md
- QUICKSTART.md
- ARCHITECTURE.md
- DEVELOPMENT.md
- USAGE_EXAMPLES.md
- CHANGELOG.md
- PROJECT_SUMMARY.md
- api-examples.http

## 🔑 Ключевые особенности

### Микросервисная архитектура
- Независимое развертывание сервисов
- Изолированные базы данных
- Service Discovery через Eureka
- API Gateway для маршрутизации

### Бизнес-логика
- Автоматическое одобрение спринта
- Контроль прав доступа
- Валидация данных
- Аудит изменений (created_at, updated_at)

### Интеграции
- Feign для межсервисного взаимодействия
- Автоматическое обогащение данных
- Graceful degradation при недоступности сервисов

### Документация
- Swagger UI для каждого сервиса
- Подробные примеры использования
- Архитектурная документация
- Руководство разработчика

## 📈 Возможности расширения

### Краткосрочные
- [ ] Spring Security + JWT
- [ ] Unit и Integration тесты
- [ ] Docker образы для сервисов
- [ ] Kubernetes манифесты

### Среднесрочные
- [ ] Event-Driven Architecture (Kafka)
- [ ] Circuit Breaker (Resilience4j)
- [ ] Distributed Tracing (Sleuth + Zipkin)
- [ ] Централизованное логирование (ELK)

### Долгосрочные
- [ ] Сервис уведомлений
- [ ] Сервис аналитики
- [ ] Интеграция с S3
- [ ] Мобильное приложение

## 🎓 Обучающая ценность

Проект демонстрирует:
- ✅ Микросервисную архитектуру
- ✅ Service Discovery
- ✅ API Gateway pattern
- ✅ Database per Service
- ✅ RESTful API design
- ✅ Spring Cloud ecosystem
- ✅ Docker containerization
- ✅ Multi-module Maven project
- ✅ OpenAPI documentation
- ✅ Clean Architecture principles

## 📞 Поддержка

### Документация
- [README.md](README.md) - Основная документация
- [QUICKSTART.md](QUICKSTART.md) - Быстрый старт
- [ARCHITECTURE.md](ARCHITECTURE.md) - Архитектура системы
- [DEVELOPMENT.md](DEVELOPMENT.md) - Руководство разработчика
- [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md) - Примеры использования

### Полезные ссылки
- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080
- Team Service Swagger: http://localhost:8081/swagger-ui.html
- Sprint Service Swagger: http://localhost:8082/swagger-ui.html
- Task Service Swagger: http://localhost:8083/swagger-ui.html

## 📝 Лицензия

MIT License - см. файл [LICENSE](LICENSE)

---

**Проект готов к использованию и дальнейшему развитию! 🚀**

*Создано: 07 апреля 2026*
*Версия: 1.0.0*
