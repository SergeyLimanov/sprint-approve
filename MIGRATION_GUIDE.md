# Руководство по миграции на версию с Security

## Что изменилось

### Новые компоненты
1. **auth-service** - новый микросервис для аутентификации
2. **security-common** - общая библиотека безопасности
3. JWT аутентификация на уровне API Gateway
4. Поле `password` в таблице `users`

### Изменения в существующих компонентах
1. **User entity** - добавлено поле `password`
2. **API Gateway** - добавлен JWT фильтр
3. **Все микросервисы** - добавлена зависимость на `security-common`

## Шаги миграции

### 1. Обновление базы данных

Добавьте поле `password` в таблицу `users`:

```sql
-- Для team-service БД
ALTER TABLE users ADD COLUMN password VARCHAR(255);

-- Установить временный пароль для существующих пользователей
-- Пароль: "changeme" (хешированный с BCrypt)
UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE password IS NULL OR password = '';
```

### 2. Пересборка проекта

```bash
# Из корневой директории
mvn clean install
```

### 3. Запуск сервисов

Запустите сервисы в следующем порядке:

```bash
# 1. Eureka Server
cd eureka-server && mvn spring-boot:run

# 2. Auth Service (новый!)
cd auth-service && mvn spring-boot:run

# 3. Team Service
cd team-service && mvn spring-boot:run

# 4. Sprint Service
cd sprint-service && mvn spring-boot:run

# 5. Task Service
cd task-service && mvn spring-boot:run

# 6. API Gateway
cd api-gateway && mvn spring-boot:run
```

### 4. Создание первого пользователя

После запуска всех сервисов, создайте пользователя:

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Admin User",
    "email": "admin@example.com",
    "password": "admin123",
    "teamId": 1,
    "role": "TEAM_LEAD"
  }'
```

Сохраните `accessToken` из ответа.

### 5. Тестирование

Проверьте, что аутентификация работает:

```bash
# Без токена - должна быть ошибка 401
curl -X GET http://localhost:8080/api/sprints

# С токеном - должен вернуть данные
curl -X GET http://localhost:8080/api/sprints \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

## Обратная совместимость

### Что продолжает работать:
- Все существующие API endpoints (но теперь требуют аутентификации)
- Структура данных в БД (кроме добавленного поля `password`)
- Межсервисное взаимодействие через Feign

### Что изменилось:
- **Все API запросы теперь требуют JWT токен** (кроме `/api/auth/**`)
- Создание пользователей теперь требует пароль
- Существующие пользователи имеют временный пароль "changeme"

## Настройка JWT Secret

⚠️ **ВАЖНО для production!**

По умолчанию используется тестовый secret. Измените его:

### Вариант 1: Переменная окружения

```bash
export JWT_SECRET="your-super-secret-key-minimum-256-bits-required-for-hs256"
```

### Вариант 2: В application.yml

```yaml
# auth-service/src/main/resources/application.yml
jwt:
  secret: your-super-secret-key-minimum-256-bits-required-for-hs256
  expiration: 86400000  # 24 hours
  refresh-expiration: 604800000  # 7 days
```

```yaml
# api-gateway/src/main/resources/application.yml
jwt:
  secret: your-super-secret-key-minimum-256-bits-required-for-hs256
```

**Убедитесь, что secret одинаковый в auth-service и api-gateway!**

## Откат изменений

Если нужно вернуться к версии без Security:

1. Откатите изменения в Git:
```bash
git checkout <commit-before-security>
```

2. Удалите поле `password` из БД (опционально):
```sql
ALTER TABLE users DROP COLUMN password;
```

3. Пересоберите проект:
```bash
mvn clean install
```

## Частые проблемы

### 401 Unauthorized на всех запросах

**Причина:** Не передан JWT токен

**Решение:** Добавьте заголовок `Authorization: Bearer <token>`

### 401 даже с токеном

**Причина:** Разные JWT secrets в auth-service и api-gateway

**Решение:** Убедитесь, что `jwt.secret` одинаковый в обоих сервисах

### Пользователи не могут войти

**Причина:** Не выполнена миграция БД (нет поля password)

**Решение:** Выполните SQL миграцию из шага 1

### Auth-service не запускается

**Причина:** Порт 8084 уже занят

**Решение:** Измените порт в `auth-service/src/main/resources/application.yml`

## Дополнительные ресурсы

- [SECURITY.md](SECURITY.md) - Полная документация по безопасности
- [api-examples-with-auth.http](api-examples-with-auth.http) - Примеры API запросов с аутентификацией
- [README.md](README.md) - Основная документация

## Поддержка

При возникновении проблем:
1. Проверьте логи всех сервисов
2. Убедитесь, что все сервисы зарегистрированы в Eureka (http://localhost:8761)
3. Проверьте, что JWT secret одинаковый в auth-service и api-gateway
4. Убедитесь, что миграция БД выполнена успешно
