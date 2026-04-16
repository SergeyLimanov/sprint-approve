# Sprint Approve - Система согласования задач в спринтах и МВП

Микросервисная система для управления командами, спринтами/МВП и задачами с возможностью согласования.

## Архитектура

Проект состоит из следующих микросервисов:

### 1. **eureka-server** (порт 8761)
Service Discovery сервер для регистрации и обнаружения микросервисов.

### 2. **api-gateway** (порт 8080)
Единая точка входа для всех API запросов. Маршрутизирует запросы к соответствующим микросервисам.

### 3. **team-service** (порт 8081)
Управление командами и пользователями:
- Создание, редактирование, удаление команд
- Управление пользователями
- Назначение ролей (TEAM_LEAD, DEVELOPER, MANAGER, APPROVER)

### 4. **sprint-service** (порт 8082)
Управление спринтами и МВП:
- Создание спринтов/МВП
- Управление статусами (CREATED, ON_REVIEW, APPROVED, REJECTED)
- Автоматическая синхронизация статуса спринта с задачами:
  - Все задачи APPROVED → спринт APPROVED
  - Есть REJECTED задачи → спринт REJECTED
  - Есть ON_REVIEW задачи → спринт ON_REVIEW
  - Только CREATED задачи → спринт CREATED

### 5. **task-service** (порт 8083)
Управление задачами, артефактами и комментариями:
- Создание и управление задачами
- Прикрепление артефактов к задачам
- Добавление комментариев
- Согласование задач ответственными лицами

## Технологический стек

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring Cloud 2023.0.0**
- **Spring Data JPA**
- **PostgreSQL** (3 отдельные БД для каждого сервиса)
- **Netflix Eureka** (Service Discovery)
- **Spring Cloud Gateway** (API Gateway)
- **OpenFeign** (межсервисное взаимодействие)
- **Lombok**
- **Swagger/OpenAPI** (документация API)

## Предварительные требования

- JDK 17 или выше
- Maven 3.6+
- Docker и Docker Compose (для баз данных)

## Запуск проекта

### 1. Запуск баз данных

```bash
docker-compose up -d
```

Это запустит 3 PostgreSQL контейнера:
- `team-db` на порту 5432
- `sprint-db` на порту 5433
- `task-db` на порту 5434

### 2. Сборка проекта

```bash
mvn clean install
```

### 3. Запуск микросервисов

Запускайте сервисы в следующем порядке:

#### 3.1. Eureka Server
```bash
cd eureka-server
mvn spring-boot:run
```
Откройте http://localhost:8761 для просмотра Eureka Dashboard.

#### 3.2. Team Service
```bash
cd team-service
mvn spring-boot:run
```

#### 3.3. Sprint Service
```bash
cd sprint-service
mvn spring-boot:run
```

#### 3.4. Task Service
```bash
cd task-service
mvn spring-boot:run
```

#### 3.5. API Gateway
```bash
cd api-gateway
mvn spring-boot:run
```

### 4. Открытие в IntelliJ IDEA

1. Откройте IntelliJ IDEA
2. File → Open → выберите корневую папку `sprint-approve`
3. IDEA автоматически распознает multi-module Maven проект
4. Все модули будут доступны в одном окне
5. Для запуска каждого сервиса создайте Run Configuration:
   - Run → Edit Configurations → Add New → Spring Boot
   - Выберите главный класс приложения для каждого модуля

## API Endpoints

Все запросы идут через API Gateway на порт **8080**.

### Team Service

#### Teams
- `GET /api/teams` - Получить все команды
- `GET /api/teams/{id}` - Получить команду по ID
- `POST /api/teams` - Создать команду
- `PUT /api/teams/{id}` - Обновить команду
- `DELETE /api/teams/{id}` - Удалить команду

#### Users
- `GET /api/users` - Получить всех пользователей
- `GET /api/users/{id}` - Получить пользователя по ID
- `GET /api/users/team/{teamId}` - Получить пользователей команды
- `POST /api/users` - Создать пользователя
- `PUT /api/users/{id}` - Обновить пользователя
- `DELETE /api/users/{id}` - Удалить пользователя

### Sprint Service

- `GET /api/sprints` - Получить все спринты
- `GET /api/sprints/{id}` - Получить спринт по ID
- `GET /api/sprints/team/{teamId}` - Получить спринты команды
- `GET /api/sprints/status/{status}` - Получить спринты по статусу
- `POST /api/sprints` - Создать спринт
- `PUT /api/sprints/{id}` - Обновить спринт
- `PATCH /api/sprints/{id}/submit` - Отправить на рассмотрение
- `PATCH /api/sprints/{id}/approve` - Одобрить спринт
- `PATCH /api/sprints/{id}/reject` - Отклонить спринт
- `PATCH /api/sprints/{id}/recalculate-status` - Пересчитать статус на основе задач
- `DELETE /api/sprints/{id}` - Удалить спринт

### Task Service

#### Tasks
- `GET /api/tasks` - Получить все задачи
- `GET /api/tasks/{id}` - Получить задачу по ID
- `GET /api/tasks/sprint/{sprintId}` - Получить задачи спринта
- `GET /api/tasks/status/{status}` - Получить задачи по статусу
- `GET /api/tasks/assigned/{userId}` - Получить задачи пользователя
- `POST /api/tasks` - Создать задачу
- `PUT /api/tasks/{id}` - Обновить задачу
- `PATCH /api/tasks/{id}/submit` - Отправить на рассмотрение
- `PATCH /api/tasks/{id}/approve?approverId={id}` - Одобрить задачу
- `PATCH /api/tasks/{id}/reject?approverId={id}` - Отклонить задачу
- `DELETE /api/tasks/{id}` - Удалить задачу

#### Artifacts
- `GET /api/artifacts/task/{taskId}` - Получить артефакты задачи
- `GET /api/artifacts/{id}` - Получить артефакт по ID
- `POST /api/artifacts` - Создать артефакт
- `DELETE /api/artifacts/{id}` - Удалить артефакт

#### Comments
- `GET /api/comments/task/{taskId}` - Получить комментарии задачи
- `GET /api/comments/{id}` - Получить комментарий по ID
- `POST /api/comments` - Создать комментарий
- `PUT /api/comments/{id}` - Обновить комментарий
- `DELETE /api/comments/{id}?authorId={id}` - Удалить комментарий

## Swagger UI

Каждый микросервис имеет свою Swagger документацию:

- Team Service: http://localhost:8081/swagger-ui.html
- Sprint Service: http://localhost:8082/swagger-ui.html
- Task Service: http://localhost:8083/swagger-ui.html

## Бизнес-логика

### Статусы

Все сущности (спринты и задачи) имеют следующие статусы:
- **CREATED** - создано
- **ON_REVIEW** - на рассмотрении
- **APPROVED** - одобрено
- **REJECTED** - отклонено

### Процесс согласования

1. Создается спринт/МВП со статусом CREATED
2. В спринт добавляются задачи
3. Задачи отправляются на рассмотрение (статус ON_REVIEW)
4. Ответственное лицо (approver) одобряет или отклоняет задачи
5. Статус спринта автоматически синхронизируется с задачами:
   - При создании, изменении или удалении задачи статус спринта пересчитывается
   - Если все задачи одобрены → спринт APPROVED
   - Если есть отклоненные задачи → спринт REJECTED
   - Если есть задачи на рассмотрении → спринт ON_REVIEW
   - Если только созданные задачи → спринт CREATED

## Структура проекта

```
sprint-approve/
├── eureka-server/          # Service Discovery
├── api-gateway/            # API Gateway
├── team-service/           # Управление командами и пользователями
├── sprint-service/         # Управление спринтами
├── task-service/           # Управление задачами, артефактами, комментариями
├── docker-compose.yml      # Конфигурация БД
├── pom.xml                 # Родительский POM
└── README.md
```

## Остановка проекта

Остановите все Spring Boot приложения (Ctrl+C в каждом терминале).

Остановите базы данных:
```bash
docker-compose down
```

Для полной очистки (включая данные):
```bash
docker-compose down -v
```

## Разработка

### Добавление нового микросервиса

1. Создайте новый модуль в корневом `pom.xml`
2. Создайте директорию с `pom.xml` для нового сервиса
3. Добавьте зависимости Spring Cloud Eureka Client
4. Настройте `application.yml` с уникальным портом
5. Добавьте маршрут в `api-gateway`

### Межсервисное взаимодействие

Используется OpenFeign для синхронного взаимодействия между сервисами. Примеры клиентов можно найти в пакетах `client` каждого сервиса.

## Troubleshooting

### Сервисы не регистрируются в Eureka
- Убедитесь, что Eureka Server запущен первым
- Проверьте, что `eureka.client.service-url.defaultZone` указывает на правильный URL

### Ошибки подключения к БД
- Проверьте, что Docker контейнеры запущены: `docker ps`
- Проверьте логи контейнеров: `docker logs team-db`

### Gateway не маршрутизирует запросы
- Убедитесь, что все сервисы зарегистрированы в Eureka
- Проверьте логи Gateway на наличие ошибок маршрутизации

## Лицензия

MIT
