# 🔒 Security Checklist - Что НЕ коммитить в Git

## ❌ НИКОГДА не коммитить:

### 🔐 Секреты и пароли:
- [ ] `.env` - переменные окружения с паролями
- [ ] `.env.local`, `.env.production` - локальные env файлы
- [ ] `firebase-service-account.json` - Firebase приватные ключи
- [ ] `*.pem`, `*.key` - SSL/TLS приватные ключи
- [ ] `*.jks`, `*.keystore` - Java keystores

### 💾 Данные и бэкапы:
- [ ] `*.dump`, `*.backup` - дампы баз данных
- [ ] `*.sql.gz`, `*.sql.zip` - сжатые SQL файлы
- [ ] `uploads/` - загруженные пользователями файлы
- [ ] `minio-data/` - данные MinIO

### 📝 Логи:
- [ ] `*.log` - лог файлы (могут содержать токены)
- [ ] `logs/` - папка с логами

### 🐳 Docker:
- [ ] `docker-compose.override.yml` - локальные переопределения с паролями

### 📦 Зависимости:
- [ ] `node_modules/` - NPM зависимости
- [ ] `target/` - Maven build (уже в .gitignore)

---

## ✅ МОЖНО коммитить:

- [x] `.env.example` - шаблон БЕЗ реальных значений
- [x] `application.yml` - с переменными `${VAR}`
- [x] `docker-compose.yml` - с переменными окружения
- [x] `pom.xml`, `package.json` - конфигурация зависимостей
- [x] `src/**/*.java`, `src/**/*.ts` - исходный код
- [x] `README.md`, `*.md` - документация

---

## 🚨 Перед каждым коммитом:

```bash
# 1. Проверить статус
git status

# 2. Проверить diff
git diff

# 3. Убедиться, что нет:
#    - .env файлов
#    - firebase-service-account.json
#    - *.log файлов
#    - паролей в коде

# 4. Только потом коммитить
git add .
git commit -m "Your message"
```

---

## 🔥 Если секрет попал в Git:

### 1. Удалить из staging (до коммита):
```bash
git reset HEAD .env
```

### 2. Удалить из последнего коммита (до push):
```bash
git rm --cached .env
git commit --amend
```

### 3. **КРИТИЧНО: Смените секрет!**
```bash
# Сгенерируйте новый JWT_SECRET
openssl rand -base64 64

# Обновите везде
```

---

## 📚 Полная документация:

- **Подробное руководство:** `SECURITY_GITIGNORE_GUIDE.md`
- **JWT Secret:** `JWT_SECRET_GUIDE.md`
- **Общая безопасность:** `SECURITY.md`

**Помните: Лучше перестраховаться, чем потом удалять секреты из истории Git!** 🔒
