# 🔒 Что НЕ должно попадать в Git

## ⚠️ КРИТИЧНО: Чувствительные данные

Эти файлы **НИКОГДА** не должны попадать в Git, так как содержат секреты, пароли и приватные ключи.

---

## 🔐 1. Environment Variables (Переменные окружения)

### ❌ НЕ коммитить:
```
.env
.env.local
.env.production
.env.development
.env.test
```

### ✅ Коммитить:
```
.env.example  ← Шаблон БЕЗ реальных значений
```

### Что содержат:
```bash
# .env (НЕ коммитить!)
JWT_SECRET=real-secret-key-xyz123
POSTGRES_PASSWORD=super-secret-password
MINIO_ROOT_PASSWORD=admin-password-123

# .env.example (можно коммитить)
JWT_SECRET=your-jwt-secret-here
POSTGRES_PASSWORD=your-password-here
MINIO_ROOT_PASSWORD=your-minio-password-here
```

**Защита в `.gitignore`:**
```gitignore
.env
.env.local
.env.*.local
*.env
!.env.example
```

---

## 🔥 2. Firebase Credentials

### ❌ НЕ коммитить:
```
firebase-service-account.json
notification-service/src/main/resources/firebase-service-account.json
firebase-adminsdk-*.json
google-services.json
GoogleService-Info.plist
```

### Что содержат:
```json
{
  "type": "service_account",
  "project_id": "sprint-approve",
  "private_key_id": "abc123...",
  "private_key": "-----BEGIN PRIVATE KEY-----\nMIIE...",
  "client_email": "firebase-adminsdk@sprint-approve.iam.gserviceaccount.com",
  "client_id": "123456789",
  "auth_uri": "https://accounts.google.com/o/oauth2/auth",
  "token_uri": "https://oauth2.googleapis.com/token"
}
```

**⚠️ Если попадёт в Git:**
- Любой может отправлять уведомления от вашего имени
- Доступ к Firebase проекту
- Возможность кражи данных

**Защита в `.gitignore`:**
```gitignore
firebase-service-account.json
**/firebase-service-account.json
firebase-adminsdk-*.json
google-services.json
```

---

## 🔑 3. SSL/TLS Certificates & Keys

### ❌ НЕ коммитить:
```
*.pem          # Private keys
*.key          # Private keys
*.crt          # Certificates
*.cer          # Certificates
*.p12          # PKCS12 keystores
*.pfx          # PKCS12 keystores
*.jks          # Java keystores
*.keystore     # Java keystores
*.truststore   # Java truststores
```

### Что это:
- SSL сертификаты для HTTPS
- Приватные ключи для шифрования
- Java keystores для Spring Boot

**Защита в `.gitignore`:**
```gitignore
*.pem
*.key
*.crt
*.p12
*.jks
*.keystore
```

---

## 💾 4. Database Backups & Dumps

### ❌ НЕ коммитить:
```
*.sql.gz       # Сжатые дампы БД
*.sql.zip      # Сжатые дампы БД
*.dump         # PostgreSQL dumps
*.backup       # Бэкапы
backups/       # Папка с бэкапами
dumps/         # Папка с дампами
```

### Почему:
- Могут содержать пользовательские данные
- Пароли пользователей (даже хешированные)
- Персональные данные (GDPR)
- Большой размер файлов

**Защита в `.gitignore`:**
```gitignore
*.sql.gz
*.dump
*.backup
/backups/
/dumps/
```

---

## 📝 5. Logs (Логи)

### ❌ НЕ коммитить:
```
*.log
logs/
application.log
spring.log
error.log
```

### Почему:
- Могут содержать чувствительные данные
- Stack traces с внутренней информацией
- SQL запросы с данными
- JWT токены в логах
- IP адреса пользователей

**Пример опасного лога:**
```
2024-01-15 10:30:45 INFO  - User login: email=admin@example.com, password=secret123
2024-01-15 10:30:46 DEBUG - JWT Token: eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
2024-01-15 10:30:47 DEBUG - SQL: SELECT * FROM users WHERE email='admin@example.com'
```

**Защита в `.gitignore`:**
```gitignore
*.log
logs/
*.log.*
/log/
```

---

## 🐳 6. Docker & Kubernetes Secrets

### ❌ НЕ коммитить:
```
docker-compose.override.yml   # Локальные переопределения
docker-compose.local.yml      # Локальная конфигурация
*.secret.yml                  # Kubernetes secrets
secrets/                      # Папка с секретами
k8s-secrets/                  # Kubernetes secrets
```

### Пример опасного файла:
```yaml
# docker-compose.override.yml (НЕ коммитить!)
services:
  team-db:
    environment:
      POSTGRES_PASSWORD: real-production-password-123
  
  minio:
    environment:
      MINIO_ROOT_PASSWORD: real-minio-password-xyz
```

**✅ Вместо этого используйте:**
```yaml
# docker-compose.yml (можно коммитить)
services:
  team-db:
    environment:
      POSTGRES_PASSWORD: ${POSTGRES_PASSWORD}
```

**Защита в `.gitignore`:**
```gitignore
docker-compose.override.yml
docker-compose.local.yml
*.secret.yml
secrets/
```

---

## 📁 7. Uploaded Files (MinIO)

### ❌ НЕ коммитить:
```
uploads/           # Локальная папка с файлами
minio-data/        # MinIO data directory
**/uploads/        # Uploads в любой папке
```

### Почему:
- Пользовательские файлы (могут быть приватными)
- Большой размер
- Не относятся к коду
- Должны храниться в MinIO/S3

**Защита в `.gitignore`:**
```gitignore
uploads/
minio-data/
**/uploads/
```

---

## 🧪 8. Test Data & Coverage

### ❌ НЕ коммитить:
```
coverage/          # Code coverage reports
*.coverage         # Coverage files
.nyc_output/       # NYC coverage
htmlcov/           # HTML coverage reports
```

**Защита в `.gitignore`:**
```gitignore
coverage/
*.coverage
htmlcov/
```

---

## 📦 9. Node Modules & Dependencies

### ❌ НЕ коммитить:
```
node_modules/      # Frontend зависимости
```

### Почему:
- Огромный размер (сотни МБ)
- Устанавливаются через `npm install`
- Описаны в `package.json`

**Защита в `.gitignore`:**
```gitignore
node_modules/
```

---

## ⚠️ 10. Временные файлы IDE

### ❌ НЕ коммитить:
```
.idea/workspace.xml        # IntelliJ workspace
.idea/tasks.xml            # IntelliJ tasks
.idea/usage.statistics.xml # IntelliJ statistics
.vscode/settings.json      # VS Code settings (если персональные)
```

**Защита в `.gitignore`:**
```gitignore
.idea/workspace.xml
.idea/tasks.xml
.vscode/
```

---

## ✅ Что МОЖНО коммитить

### Конфигурационные файлы:
```
✅ application.yml              # С переменными окружения ${VAR}
✅ docker-compose.yml           # С переменными окружения
✅ .env.example                 # Шаблон БЕЗ реальных значений
✅ pom.xml                      # Maven конфигурация
✅ package.json                 # NPM конфигурация
```

### Документация:
```
✅ README.md
✅ SECURITY.md
✅ CHANGELOG.md
✅ *.md файлы
```

### Исходный код:
```
✅ src/**/*.java
✅ src/**/*.ts
✅ src/**/*.tsx
```

---

## 🔍 Как проверить, что попало в Git

### 1. Проверить текущий статус:
```bash
git status
```

### 2. Проверить, что будет закоммичено:
```bash
git add .
git status
```

### 3. Проверить историю (если уже закоммитили):
```bash
git log --all --full-history -- "*firebase*"
git log --all --full-history -- "*.env"
```

---

## 🚨 Что делать, если секрет попал в Git

### 1. Удалить из последнего коммита (если ещё не запушили):
```bash
# Удалить файл
git rm --cached .env

# Изменить последний коммит
git commit --amend
```

### 2. Удалить из истории (если уже запушили):
```bash
# ⚠️ ОПАСНО! Переписывает историю
git filter-branch --force --index-filter \
  "git rm --cached --ignore-unmatch .env" \
  --prune-empty --tag-name-filter cat -- --all

# Force push
git push origin --force --all
```

### 3. **КРИТИЧНО: Смените секреты!**
```bash
# Если JWT_SECRET попал в Git:
# 1. Сгенерируйте новый
openssl rand -base64 64

# 2. Обновите на всех серверах
export JWT_SECRET="новый-ключ"

# 3. Все старые токены станут невалидными
```

---

## 📋 Checklist перед коммитом

- [ ] Проверил `git status` - нет ли `.env` файлов?
- [ ] Проверил `git status` - нет ли `firebase-service-account.json`?
- [ ] Проверил `git status` - нет ли `*.log` файлов?
- [ ] Проверил `git status` - нет ли `*.key`, `*.pem` файлов?
- [ ] Проверил `git diff` - нет ли паролей в коде?
- [ ] Все секреты в переменных окружения `${VAR}`?
- [ ] `.gitignore` актуален?

---

## 🎯 ИТОГО: Что в `.gitignore`

| Категория | Файлы | Почему |
|-----------|-------|--------|
| **Environment** | `.env`, `.env.local` | Пароли, токены |
| **Firebase** | `firebase-service-account.json` | Приватные ключи |
| **Certificates** | `*.pem`, `*.key`, `*.jks` | SSL ключи |
| **Database** | `*.dump`, `*.backup` | Данные пользователей |
| **Logs** | `*.log`, `logs/` | Чувствительная информация |
| **Docker** | `docker-compose.override.yml` | Локальные пароли |
| **Uploads** | `uploads/`, `minio-data/` | Пользовательские файлы |
| **Node** | `node_modules/` | Зависимости (большой размер) |

**Правило:** Если файл содержит пароли, токены, ключи или пользовательские данные → НЕ коммитить! 🔒
