# 🔄 Новая логика создания пользователей и команд

## 📋 Изменения:

### ✅ Что изменилось:

1. **teamId теперь опциональный** при регистрации
2. **Пользователь может быть создан БЕЗ команды**
3. **Команда назначается позже** через обновление пользователя
4. **Логичная последовательность:** Пользователь → Команда → Назначение

---

## 🎯 Новая последовательность:

### Шаг 1: Зарегистрировать пользователя БЕЗ команды

```bash
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "admin@example.com",
  "name": "Admin User",
  "password": "admin123",
  "role": "TEAM_LEAD"
  // teamId НЕ указываем!
}
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "...",
  "userId": 1,
  "email": "admin@example.com",
  "name": "Admin User",
  "role": "TEAM_LEAD"
}
```

**Скопируйте `accessToken`!**

---

### Шаг 2: Создать команду (с JWT токеном)

```bash
POST http://localhost:8080/api/teams
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "name": "Development Team",
  "description": "Main development team"
}
```

**Ответ:**
```json
{
  "id": 1,
  "name": "Development Team",
  "description": "Main development team",
  "createdAt": "2026-04-19T13:20:00",
  "updatedAt": "2026-04-19T13:20:00"
}
```

**Запомните `id: 1`!**

---

### Шаг 3: Назначить пользователя в команду

```bash
PUT http://localhost:8080/api/users/1
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "email": "admin@example.com",
  "name": "Admin User",
  "role": "TEAM_LEAD",
  "teamId": 1
}
```

**Ответ:**
```json
{
  "id": 1,
  "email": "admin@example.com",
  "name": "Admin User",
  "role": "TEAM_LEAD",
  "teamId": 1,
  "teamName": "Development Team",
  "createdAt": "2026-04-19T13:15:00",
  "updatedAt": "2026-04-19T13:21:00"
}
```

---

## 👥 Роли и права:

### APPROVER (Проверяющий)
- ✅ Может одобрять задачи **во всех командах**
- ✅ Может одобрять задачи **во всех спринтах**
- ✅ Не привязан к конкретной команде
- ✅ `teamId` может быть `null`

**Пример:**
```json
{
  "email": "approver@example.com",
  "name": "Global Approver",
  "password": "approve123",
  "role": "APPROVER"
  // teamId не указываем - может работать со всеми командами
}
```

---

### DEVELOPER (Разработчик)
- ✅ Создаёт задачи **только для своей команды**
- ⚠️ Должен быть назначен в команду
- ⚠️ `teamId` обязателен для работы

**Пример:**
```json
{
  "email": "developer@example.com",
  "name": "Developer",
  "password": "dev123",
  "role": "DEVELOPER",
  "teamId": 1  // Обязательно для DEVELOPER
}
```

---

### TEAM_LEAD (Лидер команды)
- ✅ Управляет **своей командой**
- ✅ Создаёт спринты для своей команды
- ✅ Одобряет задачи в своей команде
- ⚠️ `teamId` обязателен

**Пример:**
```json
{
  "email": "lead@example.com",
  "name": "Team Lead",
  "password": "lead123",
  "role": "TEAM_LEAD",
  "teamId": 1  // Обязательно
}
```

---

### MANAGER (Менеджер)
- ✅ Просматривает **все команды и спринты**
- ✅ Создаёт команды
- ✅ Управляет пользователями
- ✅ `teamId` опциональный

**Пример:**
```json
{
  "email": "manager@example.com",
  "name": "Project Manager",
  "password": "manager123",
  "role": "MANAGER"
  // teamId не обязателен
}
```

---

## 📝 Postman примеры:

### 1. Регистрация первого пользователя (TEAM_LEAD)

**Request:**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "admin@example.com",
  "name": "Admin User",
  "password": "admin123",
  "role": "TEAM_LEAD"
}
```

**Response:** Сохраните `accessToken`

---

### 2. Создание команды

**Request:**
```
POST http://localhost:8080/api/teams
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "name": "Backend Team",
  "description": "Backend development"
}
```

**Response:** Запомните `id`

---

### 3. Назначение пользователя в команду

**Request:**
```
PUT http://localhost:8080/api/users/1
Authorization: Bearer YOUR_ACCESS_TOKEN
Content-Type: application/json

{
  "email": "admin@example.com",
  "name": "Admin User",
  "role": "TEAM_LEAD",
  "teamId": 1
}
```

---

### 4. Регистрация APPROVER (без команды)

**Request:**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "approver@example.com",
  "name": "Global Approver",
  "password": "approve123",
  "role": "APPROVER"
}
```

**Approver НЕ нужна команда** - может работать со всеми!

---

### 5. Регистрация DEVELOPER (с командой)

**Request:**
```
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "dev@example.com",
  "name": "Developer",
  "password": "dev123",
  "role": "DEVELOPER",
  "teamId": 1
}
```

**Developer привязан к команде** - создаёт задачи только для неё.

---

## 🔄 Миграция существующих пользователей:

Если у вас уже есть пользователи с `teamId = null`, они могут:

1. **APPROVER, MANAGER** - работать без команды ✅
2. **DEVELOPER, TEAM_LEAD** - нужно назначить команду через `PUT /api/users/{id}`

---

## 🎯 Итого:

| Роль | teamId обязателен? | Может работать без команды? |
|------|-------------------|----------------------------|
| **APPROVER** | ❌ НЕТ | ✅ ДА (все команды) |
| **MANAGER** | ❌ НЕТ | ✅ ДА (все команды) |
| **DEVELOPER** | ✅ ДА | ❌ НЕТ (только своя команда) |
| **TEAM_LEAD** | ✅ ДА | ❌ НЕТ (только своя команда) |

---

## ✅ Преимущества новой логики:

1. ✅ **Логичная последовательность:** Пользователь → Команда → Назначение
2. ✅ **Гибкость:** APPROVER может работать со всеми командами
3. ✅ **Простота:** Не нужно создавать команду до первого пользователя
4. ✅ **Безопасность:** Команды создаются только авторизованными пользователями

**Теперь всё логично!** 🎉
