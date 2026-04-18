# 🔐 JWT Secret Key - Руководство

## ✅ Текущее состояние

JWT Secret Key **УЖЕ настроен** в вашем приложении:

### Где используется:
1. **auth-service** - генерация JWT токенов
2. **api-gateway** - валидация JWT токенов

### Конфигурация:
```yaml
# auth-service/src/main/resources/application.yml
jwt:
  secret: ${JWT_SECRET:sprint-approve-secret-key-change-this-in-production-minimum-256-bits-required-for-hs256-algorithm}
  expiration: 86400000  # 24 hours
  refresh-expiration: 604800000  # 7 days
```

---

## 🎯 Как это работает

### Приоритет значений:
```yaml
${JWT_SECRET:default-value}
```

1. **Переменная окружения** `JWT_SECRET` (если установлена)
2. **Значение по умолчанию** (если переменная не установлена)

### Пример:
```bash
# Если установлена переменная окружения:
export JWT_SECRET="my-super-secret-key"
# → Используется "my-super-secret-key"

# Если НЕ установлена:
# → Используется "sprint-approve-secret-key-change-this-in-production-minimum-256-bits-required-for-hs256-algorithm"
```

---

## 🚀 Для разработки (Development)

### Вариант 1: Использовать значение по умолчанию (проще)

**Ничего не делать!** Приложение будет использовать встроенный secret key.

```bash
# Просто запустите сервисы
mvn spring-boot:run
```

✅ **Плюсы:** Просто, работает сразу  
⚠️ **Минусы:** Secret key в коде (не критично для разработки)

---

### Вариант 2: Использовать .env файл (рекомендуется)

1. **Создайте `.env` файл** (уже создан):
```bash
JWT_SECRET=sprint-approve-secret-key-change-this-in-production-minimum-256-bits-required-for-hs256-algorithm
```

2. **Загрузите переменные окружения:**

#### Windows (PowerShell):
```powershell
# Загрузить из .env
Get-Content .env | ForEach-Object {
    if ($_ -match '^([^=]+)=(.*)$') {
        [Environment]::SetEnvironmentVariable($matches[1], $matches[2], "Process")
    }
}

# Проверить
echo $env:JWT_SECRET
```

#### Windows (CMD):
```cmd
set JWT_SECRET=sprint-approve-secret-key-change-this-in-production-minimum-256-bits-required-for-hs256-algorithm
```

#### Linux/Mac:
```bash
export $(cat .env | xargs)

# Проверить
echo $JWT_SECRET
```

3. **Запустите сервисы:**
```bash
mvn spring-boot:run
```

✅ **Плюсы:** Переменные окружения, как в production  
⚠️ **Минусы:** Нужно загружать .env перед каждым запуском

---

### Вариант 3: IntelliJ IDEA / IDE (удобнее всего)

1. **Run/Debug Configurations** → Edit Configurations
2. **Environment variables** → добавьте:
```
JWT_SECRET=sprint-approve-secret-key-change-this-in-production-minimum-256-bits-required-for-hs256-algorithm
```

3. **Apply** → **Run**

✅ **Плюсы:** Удобно, автоматически загружается  
✅ **Рекомендуется для разработки!**

---

## 🔒 Для production

### ⚠️ КРИТИЧНО: Смените secret key!

**НЕ используйте** значение по умолчанию в production!

### Сгенерируйте случайный secret key:

#### Вариант 1: OpenSSL (рекомендуется)
```bash
openssl rand -base64 64
```

**Результат:**
```
XK7vN2mP9qR4sT8uV1wY3zA5bC6dE7fG8hI9jK0lM1nO2pQ3rS4tU5vW6xY7zA8bC9dE0fG1hI2jK3lM4nO5pQ6rS7tU8vW9xY0zA1bC2dE3fG4hI5jK6lM7nO8pQ9rS0tU1vW2xY3zA4bC5dE6fG7hI8jK9lM0nO1pQ2rS3tU4vW5xY6zA7bC8dE9fG0hI1jK2lM3nO4pQ5rS6tU7vW8xY9zA0bC1dE2fG3hI4jK5lM6nO7pQ8rS9tU0vW1xY2zA3bC4dE5fG6hI7jK8lM9nO0pQ1rS2tU3vW4xY5zA6bC7dE8fG9hI0jK1lM2nO3pQ4rS5tU6vW7xY8zA9bC0dE1fG2hI3jK4lM5nO6pQ7rS8tU9vW0xY1zA2bC3dE4fG5hI6jK7lM8nO9pQ0rS1tU2vW3xY4zA5bC6dE7fG8hI9jK0lM1nO2pQ3rS4tU5vW6xY7zA8bC9dE0fG1hI2jK3lM4nO5pQ6rS7tU8vW9xY0zA1bC2dE3fG4hI5jK6lM7nO8pQ9rS0tU1vW2xY3zA4bC5dE6fG7hI8jK9lM0nO1pQ2rS3tU4vW5xY6zA7bC8dE9fG0hI1jK2lM3nO4pQ5rS6tU7vW8xY9zA0bC1dE2fG3hI4jK5lM6nO7pQ8rS9tU0vW1xY2zA3bC4dE5fG6hI7jK8lM9nO0pQ1rS2tU3vW4xY5zA6bC7dE8fG9hI0jK1lM2nO3pQ4rS5tU6vW7xY8zA9bC0dE1fG2hI3jK4lM5nO6pQ7rS8tU9vW0xY1zA2bC3dE4fG5hI6jK7lM8nO9pQ0rS1tU2vW3xY4zA5bC6dE7fG8hI9jK0lM1nO2pQ3rS4tU5vW6xY7zA8bC9dE0fG==
```

#### Вариант 2: Python
```python
import secrets
print(secrets.token_urlsafe(64))
```

#### Вариант 3: Node.js
```javascript
require('crypto').randomBytes(64).toString('base64')
```

### Установите в production:

#### Docker:
```yaml
# docker-compose.yml
services:
  auth-service:
    environment:
      - JWT_SECRET=${JWT_SECRET}
```

```bash
# .env (на production сервере)
JWT_SECRET=XK7vN2mP9qR4sT8uV1wY3zA5bC6dE7fG8hI9jK0lM1nO2pQ3rS4tU5vW6xY7zA8bC9dE0fG==
```

#### Kubernetes:
```yaml
apiVersion: v1
kind: Secret
metadata:
  name: jwt-secret
type: Opaque
data:
  JWT_SECRET: WEs3dk4ybVA5cVI0c1Q4dVYxd1kzekE1YkM2ZEU3Zkc4aEk5aks...
```

#### Systemd:
```ini
# /etc/systemd/system/auth-service.service
[Service]
Environment="JWT_SECRET=XK7vN2mP9qR4sT8uV1wY3zA5bC6dE7fG8hI9jK0lM1nO2pQ3rS4tU5vW6xY7zA8bC9dE0fG=="
```

---

## 🔍 Проверка

### Убедитесь, что secret key загружен:

```bash
# Запустите auth-service с DEBUG логами
mvn spring-boot:run

# В логах должно быть:
# "JWT secret loaded successfully"
# или
# "Using JWT secret from environment variable"
```

### Проверьте генерацию токена:

```bash
# Зарегистрируйте пользователя
POST http://localhost:8080/api/auth/register
{
  "email": "test@example.com",
  "password": "password123",
  "name": "Test User",
  "role": "DEVELOPER"
}

# Должен вернуть:
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "test@example.com",
  "role": "DEVELOPER"
}
```

---

## ⚠️ Безопасность

### ✅ DO (Делайте):
- ✅ Используйте переменные окружения в production
- ✅ Генерируйте случайный secret key (минимум 256 бит)
- ✅ Храните secret key в секретах (Kubernetes Secrets, AWS Secrets Manager)
- ✅ Используйте разные secret keys для dev/staging/production
- ✅ Ротируйте secret key периодически

### ❌ DON'T (НЕ делайте):
- ❌ НЕ коммитьте secret key в Git
- ❌ НЕ используйте одинаковый secret key везде
- ❌ НЕ используйте короткие secret keys (<256 бит)
- ❌ НЕ храните secret key в коде
- ❌ НЕ передавайте secret key по незащищенным каналам

---

## 📊 Сравнение подходов

| Подход | Безопасность | Удобство | Рекомендация |
|--------|--------------|----------|--------------|
| **Значение по умолчанию** | ⚠️ Низкая | ✅ Высокое | ✅ Development |
| **Переменная окружения** | ✅ Высокая | ⚠️ Среднее | ✅ Production |
| **Kubernetes Secret** | ✅✅ Очень высокая | ⚠️ Среднее | ✅ Production (K8s) |
| **AWS Secrets Manager** | ✅✅ Очень высокая | ⚠️ Низкое | ✅ Production (AWS) |

---

## 🎯 ИТОГО

| Вопрос | Ответ |
|--------|-------|
| **Нужен ли secret key?** | ✅ **ДА**, уже используется |
| **Где настроен?** | `auth-service` и `api-gateway` |
| **Для разработки?** | Значение по умолчанию работает |
| **Для production?** | ⚠️ **Сгенерируйте новый!** |
| **Как установить?** | Переменная окружения `JWT_SECRET` |

**Secret key УЖЕ настроен, но для production нужно сгенерировать новый!** 🔐
