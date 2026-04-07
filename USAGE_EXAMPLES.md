# Примеры использования системы Sprint Approve

## Сценарий 1: Создание команды и добавление пользователей

### Шаг 1: Создание команды

```http
POST http://localhost:8080/api/teams
Content-Type: application/json

{
  "name": "Mobile Development Team",
  "description": "Команда разработки мобильных приложений"
}
```

**Ответ:**
```json
{
  "id": 1,
  "name": "Mobile Development Team",
  "description": "Команда разработки мобильных приложений",
  "createdAt": "2026-04-07T16:00:00",
  "updatedAt": "2026-04-07T16:00:00"
}
```

### Шаг 2: Добавление Team Lead

```http
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "email": "alice.lead@company.com",
  "name": "Alice Johnson",
  "teamId": 1,
  "role": "TEAM_LEAD"
}
```

### Шаг 3: Добавление разработчиков

```http
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "email": "bob.dev@company.com",
  "name": "Bob Smith",
  "teamId": 1,
  "role": "DEVELOPER"
}
```

```http
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "email": "carol.dev@company.com",
  "name": "Carol Williams",
  "teamId": 1,
  "role": "DEVELOPER"
}
```

### Шаг 4: Добавление аппрувера

```http
POST http://localhost:8080/api/users
Content-Type: application/json

{
  "email": "david.approver@company.com",
  "name": "David Brown",
  "teamId": 1,
  "role": "APPROVER"
}
```

### Шаг 5: Проверка созданной команды

```http
GET http://localhost:8080/api/users/team/1
```

**Ответ:**
```json
[
  {
    "id": 1,
    "email": "alice.lead@company.com",
    "name": "Alice Johnson",
    "teamId": 1,
    "teamName": "Mobile Development Team",
    "role": "TEAM_LEAD",
    "createdAt": "2026-04-07T16:00:00",
    "updatedAt": "2026-04-07T16:00:00"
  },
  // ... остальные пользователи
]
```

---

## Сценарий 2: Создание и согласование спринта

### Шаг 1: Создание спринта

```http
POST http://localhost:8080/api/sprints
Content-Type: application/json

{
  "name": "Sprint 15 - Mobile App Features",
  "description": "Разработка новых функций для мобильного приложения",
  "teamId": 1,
  "type": "SPRINT",
  "startDate": "2026-04-07T00:00:00",
  "endDate": "2026-04-21T00:00:00",
  "createdBy": 1
}
```

**Ответ:**
```json
{
  "id": 1,
  "name": "Sprint 15 - Mobile App Features",
  "description": "Разработка новых функций для мобильного приложения",
  "teamId": 1,
  "teamName": "Mobile Development Team",
  "type": "SPRINT",
  "status": "CREATED",
  "startDate": "2026-04-07T00:00:00",
  "endDate": "2026-04-21T00:00:00",
  "createdBy": 1,
  "createdByName": "Alice Johnson",
  "createdAt": "2026-04-07T16:00:00",
  "updatedAt": "2026-04-07T16:00:00"
}
```

### Шаг 2: Добавление задач в спринт

**Задача 1:**
```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Реализовать push-уведомления",
  "description": "Добавить поддержку push-уведомлений для iOS и Android",
  "sprintId": 1,
  "assignedTo": 2,
  "approverId": 4,
  "createdBy": 1
}
```

**Задача 2:**
```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Оптимизировать загрузку изображений",
  "description": "Реализовать ленивую загрузку и кеширование изображений",
  "sprintId": 1,
  "assignedTo": 3,
  "approverId": 4,
  "createdBy": 1
}
```

**Задача 3:**
```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Добавить темную тему",
  "description": "Реализовать переключение между светлой и темной темой",
  "sprintId": 1,
  "assignedTo": 2,
  "approverId": 4,
  "createdBy": 1
}
```

### Шаг 3: Работа над задачей

**Добавление комментария:**
```http
POST http://localhost:8080/api/comments
Content-Type: application/json

{
  "content": "Начал работу над задачей. Изучаю документацию Firebase Cloud Messaging.",
  "taskId": 1,
  "authorId": 2
}
```

**Добавление артефакта:**
```http
POST http://localhost:8080/api/artifacts
Content-Type: application/json

{
  "name": "Диаграмма архитектуры push-уведомлений",
  "url": "https://drive.google.com/file/d/abc123/architecture-diagram.png",
  "fileType": "image/png",
  "fileSize": 156789,
  "taskId": 1,
  "uploadedBy": 2
}
```

**Еще комментарий:**
```http
POST http://localhost:8080/api/comments
Content-Type: application/json

{
  "content": "Реализовал базовый функционал. Готов к ревью.",
  "taskId": 1,
  "authorId": 2
}
```

### Шаг 4: Отправка задачи на рассмотрение

```http
PATCH http://localhost:8080/api/tasks/1/submit
```

**Ответ:**
```json
{
  "id": 1,
  "title": "Реализовать push-уведомления",
  "status": "ON_REVIEW",
  // ...
}
```

### Шаг 5: Проверка и одобрение задачи

**Комментарий от аппрувера:**
```http
POST http://localhost:8080/api/comments
Content-Type: application/json

{
  "content": "Проверил код и документацию. Все выглядит хорошо. Одобряю.",
  "taskId": 1,
  "authorId": 4
}
```

**Одобрение задачи:**
```http
PATCH http://localhost:8080/api/tasks/1/approve?approverId=4
```

**Ответ:**
```json
{
  "id": 1,
  "title": "Реализовать push-уведомления",
  "status": "APPROVED",
  // ...
}
```

### Шаг 6: Одобрение остальных задач

```http
PATCH http://localhost:8080/api/tasks/2/submit
PATCH http://localhost:8080/api/tasks/2/approve?approverId=4

PATCH http://localhost:8080/api/tasks/3/submit
PATCH http://localhost:8080/api/tasks/3/approve?approverId=4
```

### Шаг 7: Автоматическое одобрение спринта

После одобрения всех задач, спринт автоматически получает статус APPROVED.

**Проверка статуса спринта:**
```http
GET http://localhost:8080/api/sprints/1
```

**Ответ:**
```json
{
  "id": 1,
  "name": "Sprint 15 - Mobile App Features",
  "status": "APPROVED",
  // ...
}
```

---

## Сценарий 3: Отклонение задачи и доработка

### Шаг 1: Создание задачи

```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Интеграция с платежной системой",
  "description": "Добавить поддержку оплаты через Stripe",
  "sprintId": 1,
  "assignedTo": 3,
  "approverId": 4,
  "createdBy": 1
}
```

### Шаг 2: Отправка на рассмотрение

```http
PATCH http://localhost:8080/api/tasks/4/submit
```

### Шаг 3: Отклонение задачи

```http
POST http://localhost:8080/api/comments
Content-Type: application/json

{
  "content": "Не хватает обработки ошибок и тестов. Необходимо доработать.",
  "taskId": 4,
  "authorId": 4
}
```

```http
PATCH http://localhost:8080/api/tasks/4/reject?approverId=4
```

### Шаг 4: Доработка

```http
POST http://localhost:8080/api/comments
Content-Type: application/json

{
  "content": "Добавил обработку ошибок и unit-тесты. Готов к повторному ревью.",
  "taskId": 4,
  "authorId": 3
}
```

### Шаг 5: Повторная отправка

```http
PATCH http://localhost:8080/api/tasks/4/submit
PATCH http://localhost:8080/api/tasks/4/approve?approverId=4
```

---

## Сценарий 4: Создание МВП

### Шаг 1: Создание МВП

```http
POST http://localhost:8080/api/sprints
Content-Type: application/json

{
  "name": "MVP - Базовый функционал приложения",
  "description": "Минимально жизнеспособная версия продукта с основными функциями",
  "teamId": 1,
  "type": "MVP",
  "startDate": "2026-04-07T00:00:00",
  "endDate": "2026-05-07T00:00:00",
  "createdBy": 1
}
```

### Шаг 2: Добавление ключевых задач

```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Авторизация пользователей",
  "description": "Реализовать регистрацию и вход через email/password",
  "sprintId": 2,
  "assignedTo": 2,
  "approverId": 4,
  "createdBy": 1
}
```

```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "Основной экран приложения",
  "description": "Реализовать главный экран с навигацией",
  "sprintId": 2,
  "assignedTo": 3,
  "approverId": 4,
  "createdBy": 1
}
```

```http
POST http://localhost:8080/api/tasks
Content-Type: application/json

{
  "title": "API для работы с данными",
  "description": "Создать REST API для основных операций",
  "sprintId": 2,
  "assignedTo": 2,
  "approverId": 4,
  "createdBy": 1
}
```

---

## Сценарий 5: Мониторинг и отчетность

### Получение всех спринтов команды

```http
GET http://localhost:8080/api/sprints/team/1
```

### Получение задач по статусу

**Задачи на рассмотрении:**
```http
GET http://localhost:8080/api/tasks/status/ON_REVIEW
```

**Одобренные задачи:**
```http
GET http://localhost:8080/api/tasks/status/APPROVED
```

**Отклоненные задачи:**
```http
GET http://localhost:8080/api/tasks/status/REJECTED
```

### Получение задач конкретного пользователя

```http
GET http://localhost:8080/api/tasks/assigned/2
```

### Получение всех комментариев к задаче

```http
GET http://localhost:8080/api/comments/task/1
```

### Получение всех артефактов задачи

```http
GET http://localhost:8080/api/artifacts/task/1
```

---

## Сценарий 6: Управление командой

### Изменение роли пользователя

```http
PUT http://localhost:8080/api/users/2
Content-Type: application/json

{
  "email": "bob.dev@company.com",
  "name": "Bob Smith",
  "teamId": 1,
  "role": "TEAM_LEAD"
}
```

### Перевод пользователя в другую команду

**Создание новой команды:**
```http
POST http://localhost:8080/api/teams
Content-Type: application/json

{
  "name": "Backend Team",
  "description": "Команда backend разработки"
}
```

**Перевод пользователя:**
```http
PUT http://localhost:8080/api/users/3
Content-Type: application/json

{
  "email": "carol.dev@company.com",
  "name": "Carol Williams",
  "teamId": 2,
  "role": "DEVELOPER"
}
```

### Обновление информации о команде

```http
PUT http://localhost:8080/api/teams/1
Content-Type: application/json

{
  "name": "Mobile Development Team",
  "description": "Команда разработки iOS и Android приложений (обновлено)"
}
```

---

## Сценарий 7: Работа с артефактами

### Добавление различных типов артефактов

**Документация:**
```http
POST http://localhost:8080/api/artifacts
Content-Type: application/json

{
  "name": "Техническая документация",
  "url": "https://docs.company.com/tech-spec.pdf",
  "fileType": "application/pdf",
  "fileSize": 2456789,
  "taskId": 1,
  "uploadedBy": 2
}
```

**Скриншот:**
```http
POST http://localhost:8080/api/artifacts
Content-Type: application/json

{
  "name": "Скриншот UI",
  "url": "https://storage.company.com/screenshots/ui-v1.png",
  "fileType": "image/png",
  "fileSize": 345678,
  "taskId": 1,
  "uploadedBy": 2
}
```

**Видео демонстрация:**
```http
POST http://localhost:8080/api/artifacts
Content-Type: application/json

{
  "name": "Демо видео функционала",
  "url": "https://youtube.com/watch?v=abc123",
  "fileType": "video/mp4",
  "fileSize": 15678900,
  "taskId": 1,
  "uploadedBy": 2
}
```

### Удаление артефакта

```http
DELETE http://localhost:8080/api/artifacts/1
```

---

## Полезные запросы для администрирования

### Получение статистики

**Все команды:**
```http
GET http://localhost:8080/api/teams
```

**Все пользователи:**
```http
GET http://localhost:8080/api/users
```

**Все спринты:**
```http
GET http://localhost:8080/api/sprints
```

**Все задачи:**
```http
GET http://localhost:8080/api/tasks
```

### Фильтрация спринтов по статусу

**Спринты на рассмотрении:**
```http
GET http://localhost:8080/api/sprints/status/ON_REVIEW
```

**Одобренные спринты:**
```http
GET http://localhost:8080/api/sprints/status/APPROVED
```

**Отклоненные спринты:**
```http
GET http://localhost:8080/api/sprints/status/REJECTED
```

---

## Типичные ошибки и их обработка

### Попытка одобрить задачу не назначенным аппрувером

```http
PATCH http://localhost:8080/api/tasks/1/approve?approverId=2
```

**Ответ (ошибка):**
```json
{
  "error": "Only the assigned approver can approve this task"
}
```

### Попытка создать команду с существующим именем

```http
POST http://localhost:8080/api/teams
Content-Type: application/json

{
  "name": "Mobile Development Team",
  "description": "Дубликат"
}
```

**Ответ (ошибка):**
```json
{
  "error": "Team with name 'Mobile Development Team' already exists"
}
```

### Попытка отправить на ревью уже одобренную задачу

```http
PATCH http://localhost:8080/api/tasks/1/submit
```

**Ответ (ошибка):**
```json
{
  "error": "Only tasks with CREATED status can be submitted for review"
}
```

---

## Рекомендации по использованию

1. **Создавайте команды и пользователей до начала работы со спринтами**
2. **Назначайте аппруверов для каждой задачи**
3. **Используйте комментарии для коммуникации**
4. **Прикрепляйте артефакты для документирования работы**
5. **Отправляйте задачи на ревью только после завершения работы**
6. **Проверяйте все задачи перед одобрением спринта**
