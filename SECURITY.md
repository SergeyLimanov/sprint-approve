# Безопасность и аутентификация

## Обзор

Система использует JWT (JSON Web Tokens) для аутентификации и role-based авторизацию для контроля доступа.

## Архитектура безопасности

```
Клиент → API Gateway (JWT проверка) → Микросервисы (Role проверка)
```

### Компоненты

1. **auth-service** (порт 8084) - Аутентификация и выдача JWT токенов
2. **API Gateway** (порт 8080) - Проверка JWT токенов для всех запросов
3. **security-common** - Общая библиотека для работы с Security Context
4. **Микросервисы** - Используют Security Context для проверки ролей

## Роли пользователей

- **TEAM_LEAD** - Руководитель команды
- **DEVELOPER** - Разработчик
- **MANAGER** - Менеджер
- **APPROVER** - Утверждающий (может одобрять/отклонять задачи)

## API Endpoints

### Аутентификация

#### Регистрация
```http
POST /api/auth/register
Content-Type: application/json

{
  "name": "John Doe",
  "email": "john@example.com",
  "password": "password123",
  "teamId": 1,
  "role": "DEVELOPER"
}
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "tokenType": "Bearer",
  "userId": 1,
  "email": "john@example.com",
  "name": "John Doe",
  "role": "DEVELOPER"
}
```

#### Вход
```http
POST /api/auth/login
Content-Type: application/json

{
  "email": "john@example.com",
  "password": "password123"
}
```

**Ответ:** Аналогичен регистрации

#### Обновление токена
```http
POST /api/auth/refresh
Content-Type: application/json

{
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

#### Проверка токена
```http
POST /api/auth/validate?token=eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Ответ:**
```json
{
  "valid": true
}
```

## Использование токенов

### Все запросы к защищенным endpoints должны включать JWT токен в заголовке:

```http
GET /api/teams
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

### Незащищенные endpoints (не требуют токена):

- `POST /api/auth/login`
- `POST /api/auth/register`
- `POST /api/auth/refresh`
- `POST /api/auth/validate`
- Swagger UI endpoints

### Защищенные endpoints (требуют токена):

- Все endpoints в `/api/teams/**`
- Все endpoints в `/api/users/**`
- Все endpoints в `/api/sprints/**`
- Все endpoints в `/api/tasks/**`
- Все endpoints в `/api/artifacts/**`
- Все endpoints в `/api/comments/**`

## JWT Token Structure

### Access Token (срок действия: 24 часа)

```json
{
  "userId": 1,
  "email": "john@example.com",
  "role": "DEVELOPER",
  "sub": "john@example.com",
  "iat": 1713264000,
  "exp": 1713350400
}
```

### Refresh Token (срок действия: 7 дней)

```json
{
  "userId": 1,
  "email": "john@example.com",
  "sub": "john@example.com",
  "iat": 1713264000,
  "exp": 1713868800
}
```

## Security Context в микросервисах

API Gateway добавляет следующие заголовки к запросам:

- `X-User-Id` - ID пользователя
- `X-User-Email` - Email пользователя
- `X-User-Role` - Роль пользователя

Микросервисы могут получить доступ к этой информации через `SecurityContext`:

```java
import org.example.security.SecurityContext;

// Получить текущего пользователя
SecurityContext context = SecurityContext.get();
Long userId = context.getUserId();
String email = context.getEmail();
String role = context.getRole();

// Проверить роль
if (context.hasRole("APPROVER")) {
    // Логика для approver
}

// Проверить несколько ролей
if (context.hasAnyRole("TEAM_LEAD", "MANAGER")) {
    // Логика для team lead или manager
}
```

## Примеры использования

### 1. Регистрация и вход

```bash
# Регистрация
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "john@example.com",
    "password": "password123",
    "teamId": 1,
    "role": "DEVELOPER"
  }'

# Сохраните accessToken из ответа
```

### 2. Использование токена

```bash
# Получить все спринты (с токеном)
curl -X GET http://localhost:8080/api/sprints \
  -H "Authorization: Bearer YOUR_ACCESS_TOKEN"
```

### 3. Обновление токена

```bash
# Когда access token истекает, используйте refresh token
curl -X POST http://localhost:8080/api/auth/refresh \
  -H "Content-Type: application/json" \
  -d '{
    "refreshToken": "YOUR_REFRESH_TOKEN"
  }'
```

## Конфигурация

### JWT Secret

По умолчанию используется секрет из конфигурации. **В production обязательно измените его!**

Установите переменную окружения:
```bash
export JWT_SECRET="your-super-secret-key-minimum-256-bits-required"
```

Или в `application.yml`:
```yaml
jwt:
  secret: ${JWT_SECRET:default-secret-key}
  expiration: 86400000  # 24 hours
  refresh-expiration: 604800000  # 7 days
```

## Безопасность в production

### Обязательные меры:

1. **Измените JWT secret** на криптографически стойкий ключ (минимум 256 бит)
2. **Используйте HTTPS** для всех запросов
3. **Настройте CORS** в API Gateway
4. **Включите rate limiting** для защиты от brute-force атак
5. **Логируйте все попытки аутентификации**
6. **Используйте secure cookies** для refresh tokens (опционально)
7. **Настройте password policy** (минимальная длина, сложность)
8. **Добавьте 2FA** для критичных операций (опционально)

### Рекомендации:

- Храните refresh tokens в secure storage (HttpOnly cookies или secure storage)
- Реализуйте механизм отзыва токенов (token blacklist)
- Используйте короткие сроки жизни для access tokens
- Логируйте все изменения критичных данных
- Регулярно обновляйте зависимости

## Миграция существующих данных

Если у вас уже есть пользователи без паролей:

```sql
-- Установить временный пароль для всех пользователей
-- Пароль: "changeme" (хешированный с BCrypt)
UPDATE users 
SET password = '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy'
WHERE password IS NULL OR password = '';
```

Пользователи должны будут сменить пароль при первом входе.

## Troubleshooting

### 401 Unauthorized

- Проверьте, что токен не истек
- Убедитесь, что токен передается в заголовке `Authorization: Bearer <token>`
- Проверьте, что JWT secret одинаковый в auth-service и api-gateway

### 403 Forbidden

- Проверьте роль пользователя
- Убедитесь, что у пользователя есть права на выполнение операции

### Токен не валидируется

- Проверьте логи API Gateway
- Убедитесь, что все сервисы используют один и тот же JWT secret
- Проверьте, что токен не был изменен

## Тестирование

Используйте файл `api-examples-with-auth.http` для тестирования с аутентификацией.
