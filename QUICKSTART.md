# Быстрый старт Sprint Approve

## За 5 минут до запуска

### 1. Проверьте требования

```bash
java -version    # Должна быть версия 17+
mvn -version     # Должна быть версия 3.6+
docker --version # Для запуска БД
```

### 2. Запустите базы данных

```bash
docker-compose up -d
```

Подождите 10 секунд, пока PostgreSQL запустится.

### 3. Соберите проект

```bash
mvn clean install -DskipTests
```

### 4. Запустите сервисы

**Вариант А: Автоматический запуск (Windows)**
```bash
start-all.bat
```

**Вариант Б: Ручной запуск**

Откройте 5 терминалов и выполните:

```bash
# Терминал 1 - Eureka Server
cd eureka-server
mvn spring-boot:run

# Терминал 2 - Team Service (подождите 15 сек после Eureka)
cd team-service
mvn spring-boot:run

# Терминал 3 - Sprint Service (подождите 10 сек)
cd sprint-service
mvn spring-boot:run

# Терминал 4 - Task Service (подождите 10 сек)
cd task-service
mvn spring-boot:run

# Терминал 5 - API Gateway (подождите 10 сек)
cd api-gateway
mvn spring-boot:run
```

### 5. Проверьте запуск

Откройте в браузере:
- **Eureka Dashboard:** http://localhost:8761
- **API Gateway:** http://localhost:8080

Все 4 сервиса должны быть зарегистрированы в Eureka.

---

## Первые шаги

### Создайте команду

```bash
curl -X POST http://localhost:8080/api/teams \
  -H "Content-Type: application/json" \
  -d '{
    "name": "My Team",
    "description": "Моя первая команда"
  }'
```

### Создайте пользователя

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Content-Type: application/json" \
  -d '{
    "email": "user@example.com",
    "name": "John Doe",
    "teamId": 1,
    "role": "DEVELOPER"
  }'
```

### Создайте спринт

```bash
curl -X POST http://localhost:8080/api/sprints \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Sprint 1",
    "description": "Первый спринт",
    "teamId": 1,
    "type": "SPRINT",
    "createdBy": 1
  }'
```

### Создайте задачу

```bash
curl -X POST http://localhost:8080/api/tasks \
  -H "Content-Type: application/json" \
  -d '{
    "title": "Моя первая задача",
    "description": "Описание задачи",
    "sprintId": 1,
    "assignedTo": 1,
    "createdBy": 1
  }'
```

---

## Использование в IntelliJ IDEA

### Открытие проекта

1. File → Open
2. Выберите папку `sprint-approve`
3. Дождитесь индексации

### Запуск всех сервисов

1. Run → Edit Configurations
2. Add New → Compound
3. Добавьте все 5 сервисов
4. Run → Run 'All Services'

### Тестирование API

1. Откройте файл `api-examples.http`
2. Нажмите зеленую стрелку рядом с запросом
3. Просмотрите результат

---

## Swagger UI

Документация API доступна по адресам:

- Team Service: http://localhost:8081/swagger-ui.html
- Sprint Service: http://localhost:8082/swagger-ui.html
- Task Service: http://localhost:8083/swagger-ui.html

---

## Остановка

### Остановка сервисов
Нажмите Ctrl+C в каждом терминале

### Остановка баз данных
```bash
docker-compose down
```

---

## Что дальше?

- Прочитайте [README.md](README.md) для полной документации
- Изучите [ARCHITECTURE.md](ARCHITECTURE.md) для понимания архитектуры
- Посмотрите [USAGE_EXAMPLES.md](USAGE_EXAMPLES.md) для примеров использования
- Прочитайте [DEVELOPMENT.md](DEVELOPMENT.md) для разработки

---

## Проблемы?

### Порт занят
```bash
# Измените порт в application.yml
server:
  port: 8090
```

### Сервис не регистрируется в Eureka
- Подождите 30 секунд
- Перезапустите сервис
- Проверьте логи

### Ошибка подключения к БД
```bash
# Проверьте статус контейнеров
docker ps

# Перезапустите БД
docker-compose restart
```

---

## Полезные ссылки

- Eureka Dashboard: http://localhost:8761
- API Gateway: http://localhost:8080
- Team Service Swagger: http://localhost:8081/swagger-ui.html
- Sprint Service Swagger: http://localhost:8082/swagger-ui.html
- Task Service Swagger: http://localhost:8083/swagger-ui.html

**Готово! Система запущена и готова к использованию! 🚀**
