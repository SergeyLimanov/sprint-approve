# Инструкция по тестированию автоматической синхронизации статусов

## Подготовка

1. Запустите все сервисы:
```bash
# Запуск баз данных
docker-compose up -d

# Запуск сервисов (в отдельных терминалах или используйте start-all.bat)
cd eureka-server && mvn spring-boot:run
cd team-service && mvn spring-boot:run
cd sprint-service && mvn spring-boot:run
cd task-service && mvn spring-boot:run
cd api-gateway && mvn spring-boot:run
```

2. Убедитесь, что все сервисы зарегистрированы в Eureka: http://localhost:8761

## Тестовые сценарии

### Сценарий 1: Автоматическое одобрение спринта

**Цель:** Проверить, что спринт автоматически переходит в APPROVED, когда все задачи одобрены.

```bash
# 1. Создать команду
POST http://localhost:8080/api/teams
{
  "name": "Test Team",
  "description": "Test"
}
# Запомните teamId

# 2. Создать пользователя
POST http://localhost:8080/api/users
{
  "name": "Test User",
  "email": "test@test.com",
  "teamId": 1,
  "role": "DEVELOPER"
}
# Запомните userId

# 3. Создать спринт
POST http://localhost:8080/api/sprints
{
  "name": "Sprint 1",
  "description": "Test Sprint",
  "teamId": 1,
  "type": "SPRINT",
  "createdBy": 1,
  "startDate": "2026-04-16",
  "endDate": "2026-04-30"
}
# Запомните sprintId
# Проверьте: status должен быть CREATED

# 4. Создать задачу 1
POST http://localhost:8080/api/tasks
{
  "title": "Task 1",
  "description": "Test task 1",
  "sprintId": 1,
  "assignedTo": 1,
  "approverId": 1,
  "createdBy": 1
}
# Проверьте спринт: GET http://localhost:8080/api/sprints/1
# status должен остаться CREATED

# 5. Создать задачу 2
POST http://localhost:8080/api/tasks
{
  "title": "Task 2",
  "description": "Test task 2",
  "sprintId": 1,
  "assignedTo": 1,
  "approverId": 1,
  "createdBy": 1
}

# 6. Отправить задачу 1 на рассмотрение
PATCH http://localhost:8080/api/tasks/1/submit
# Проверьте спринт: status должен стать ON_REVIEW

# 7. Отправить задачу 2 на рассмотрение
PATCH http://localhost:8080/api/tasks/2/submit
# Проверьте спринт: status должен остаться ON_REVIEW

# 8. Одобрить задачу 1
PATCH http://localhost:8080/api/tasks/1/approve?approverId=1
# Проверьте спринт: status должен остаться ON_REVIEW (т.к. задача 2 еще не одобрена)

# 9. Одобрить задачу 2
PATCH http://localhost:8080/api/tasks/2/approve?approverId=1
# Проверьте спринт: status должен стать APPROVED ✅
```

### Сценарий 2: Автоматическое отклонение спринта

**Цель:** Проверить, что спринт переходит в REJECTED при отклонении задачи.

```bash
# Используйте спринт из сценария 1 (status = APPROVED)

# 1. Отклонить задачу 1
PATCH http://localhost:8080/api/tasks/1/reject?approverId=1

# 2. Проверить спринт
GET http://localhost:8080/api/sprints/1
# status должен стать REJECTED ✅
```

### Сценарий 3: Возврат задачи на доработку

**Цель:** Проверить, что спринт переходит в ON_REVIEW при возврате задачи.

```bash
# Используйте спринт со статусом REJECTED

# 1. Отправить отклоненную задачу снова на рассмотрение
PATCH http://localhost:8080/api/tasks/1/submit

# 2. Проверить спринт
GET http://localhost:8080/api/sprints/1
# status должен стать ON_REVIEW ✅
```

### Сценарий 4: Удаление задачи

**Цель:** Проверить пересчет статуса при удалении задачи.

```bash
# Одобрите обе задачи, чтобы спринт был APPROVED

# 1. Удалить задачу 1
DELETE http://localhost:8080/api/tasks/1

# 2. Проверить спринт
GET http://localhost:8080/api/sprints/1
# status должен остаться APPROVED (т.к. задача 2 одобрена) ✅

# 3. Удалить задачу 2
DELETE http://localhost:8080/api/tasks/2

# 4. Проверить спринт
GET http://localhost:8080/api/sprints/1
# status должен стать CREATED (т.к. задач нет) ✅
```

### Сценарий 5: Ручной пересчет статуса

**Цель:** Проверить работу endpoint для ручного пересчета.

```bash
# 1. Создать новую задачу
POST http://localhost:8080/api/tasks
{
  "title": "Task 3",
  "description": "Test task 3",
  "sprintId": 1,
  "assignedTo": 1,
  "approverId": 1,
  "createdBy": 1
}

# 2. Вызвать ручной пересчет
PATCH http://localhost:8080/api/sprints/1/recalculate-status

# 3. Проверить спринт
GET http://localhost:8080/api/sprints/1
# status должен соответствовать статусам задач ✅
```

## Проверка логов

Проверьте логи сервисов на наличие сообщений о синхронизации:

**Sprint Service:**
```
INFO: Sprint 1 status automatically changed from CREATED to ON_REVIEW
INFO: Sprint 1 status automatically changed from ON_REVIEW to APPROVED
```

**Task Service:**
```
INFO: Sprint 1 status recalculated based on tasks
```

## Ожидаемые результаты

✅ Статус спринта автоматически обновляется при изменении статусов задач
✅ Логика приоритетов работает корректно (REJECTED > ON_REVIEW > CREATED > APPROVED)
✅ Удаление задач корректно пересчитывает статус спринта
✅ Ручной пересчет работает через endpoint
✅ Все операции логируются

## Возможные проблемы

1. **Сервисы не видят друг друга**
   - Проверьте Eureka Dashboard: http://localhost:8761
   - Убедитесь, что все сервисы зарегистрированы

2. **Статус не обновляется**
   - Проверьте логи на ошибки
   - Убедитесь, что Feign клиент работает корректно
   - Попробуйте ручной пересчет через `/recalculate-status`

3. **Ошибки при вызове межсервисных API**
   - Проверьте, что все сервисы запущены
   - Проверьте порты в конфигурации
