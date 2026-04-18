# 👥 Создание пользователей и распределение ролей

## 📋 Содержание
1. [Как создаются пользователи](#как-создаются-пользователи)
2. [Роли в системе](#роли-в-системе)
3. [Процесс регистрации](#процесс-регистрации)
4. [Примеры создания пользователей](#примеры-создания-пользователей)
5. [Изменение ролей](#изменение-ролей)
6. [FAQ](#faq)

---

## 🔧 Как создаются пользователи?

### Способ 1: Самостоятельная регистрация (Self-Registration)

**Endpoint:** `POST /api/auth/register`

**Процесс:**
```
1. Пользователь заполняет форму регистрации
2. Frontend отправляет POST /api/auth/register
3. Auth Service получает запрос
4. Auth Service хеширует пароль (BCrypt)
5. Auth Service вызывает Team Service (Feign)
6. Team Service создаёт пользователя в БД
7. Auth Service генерирует JWT токены
8. Пользователь получает токены и может войти
```

**Что нужно указать:**
- ✅ **Email** (уникальный)
- ✅ **Имя** (Name)
- ✅ **Пароль** (минимум 6 символов)
- ✅ **Роль** (DEVELOPER, APPROVER, TEAM_LEAD, MANAGER)
- ✅ **Team ID** (ID команды)

---

### Способ 2: Создание администратором

**Endpoint:** `POST /api/users` (напрямую в Team Service)

**Процесс:**
```
1. Администратор отправляет POST /api/users
2. Team Service создаёт пользователя
3. Пароль должен быть уже хеширован
```

**⚠️ Внимание:** Этот способ требует JWT токена администратора.

---

## 👔 Роли в системе

### Enum UserRole:
```java
public enum UserRole {
    TEAM_LEAD,    // Лидер команды
    DEVELOPER,    // Разработчик
    MANAGER,      // Менеджер
    APPROVER      // Проверяющий (аппрувер)
}
```

---

### 1. **DEVELOPER** (Разработчик)

**Права:**
- ✅ Создавать задачи
- ✅ Редактировать свои задачи
- ✅ Загружать файлы (артефакты)
- ✅ Комментировать задачи
- ✅ Отправлять задачи на проверку
- ❌ Одобрять/отклонять задачи

**Типичный workflow:**
```
1. Создаёт задачу
2. Прикрепляет файлы
3. Пишет комментарии
4. Отправляет на проверку (submit)
5. Ждёт одобрения от APPROVER
```

**Пример:**
```json
{
  "email": "developer@example.com",
  "name": "John Developer",
  "password": "password123",
  "teamId": 1,
  "role": "DEVELOPER"
}
```

---

### 2. **APPROVER** (Проверяющий)

**Права:**
- ✅ Просматривать задачи
- ✅ Одобрять задачи (approve)
- ✅ Отклонять задачи (reject)
- ✅ Комментировать задачи
- ✅ Просматривать артефакты
- ❌ Создавать задачи (обычно)

**Типичный workflow:**
```
1. Получает уведомление о новой задаче
2. Просматривает задачу и файлы
3. Оставляет комментарии
4. Одобряет или отклоняет задачу
```

**Уведомления:**
- 🔔 Новая задача в спринте
- 🔔 Задача отправлена на проверку
- 🔔 Новый артефакт прикреплён

**Пример:**
```json
{
  "email": "approver@example.com",
  "name": "Jane Approver",
  "password": "password123",
  "teamId": 1,
  "role": "APPROVER"
}
```

---

### 3. **TEAM_LEAD** (Лидер команды)

**Права:**
- ✅ Все права DEVELOPER
- ✅ Все права APPROVER
- ✅ Управление командой
- ✅ Создание спринтов
- ✅ Одобрение спринтов
- ✅ Назначение задач

**Типичный workflow:**
```
1. Создаёт спринт
2. Назначает задачи разработчикам
3. Назначает аппруверов
4. Контролирует прогресс
5. Одобряет спринт (когда все задачи готовы)
```

**Пример:**
```json
{
  "email": "teamlead@example.com",
  "name": "Alice Team Lead",
  "password": "password123",
  "teamId": 1,
  "role": "TEAM_LEAD"
}
```

---

### 4. **MANAGER** (Менеджер)

**Права:**
- ✅ Просмотр всех задач и спринтов
- ✅ Создание команд
- ✅ Управление пользователями
- ✅ Отчёты и аналитика
- ❌ Одобрение задач (не техническая роль)

**Типичный workflow:**
```
1. Создаёт команды
2. Добавляет пользователей в команды
3. Просматривает прогресс спринтов
4. Генерирует отчёты
```

**Пример:**
```json
{
  "email": "manager@example.com",
  "name": "Bob Manager",
  "password": "password123",
  "teamId": 1,
  "role": "MANAGER"
}
```

---

## 🔄 Процесс регистрации (детально)

### Шаг 1: Frontend отправляет запрос

```http
POST http://localhost:8080/api/auth/register
Content-Type: application/json

{
  "email": "user@example.com",
  "name": "Test User",
  "password": "password123",
  "teamId": 1,
  "role": "DEVELOPER"
}
```

---

### Шаг 2: API Gateway пропускает (без JWT)

```java
// api-gateway/application.yml
routes:
  - id: auth-service
    uri: lb://auth-service
    predicates:
      - Path=/api/auth/**
    filters:
      - RewritePath=/api/(?<segment>.*), /api/$\{segment}
      # ❌ НЕТ JwtAuthenticationFilter!
```

**Почему:** `/api/auth/**` endpoints не требуют JWT токена.

---

### Шаг 3: Auth Service обрабатывает

```java
// AuthService.java
public AuthResponse register(RegisterRequest request) {
    // 1. Создаём UserDto
    UserDto userDto = new UserDto();
    userDto.setEmail(request.getEmail());
    userDto.setName(request.getName());
    
    // 2. Хешируем пароль (BCrypt)
    userDto.setPassword(passwordEncoder.encode(request.getPassword()));
    // Пример: "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
    
    userDto.setTeamId(request.getTeamId());
    userDto.setRole(request.getRole());
    
    // 3. Вызываем Team Service через Feign
    UserDto createdUser = teamServiceClient.createUser(userDto);
    
    // 4. Генерируем JWT токены
    String accessToken = jwtUtil.generateToken(
        createdUser.getId(), 
        createdUser.getEmail(), 
        createdUser.getRole()
    );
    String refreshToken = jwtUtil.generateRefreshToken(
        createdUser.getId(), 
        createdUser.getEmail()
    );
    
    // 5. Возвращаем токены
    return new AuthResponse(
        accessToken,
        refreshToken,
        createdUser.getId(),
        createdUser.getEmail(),
        createdUser.getName(),
        createdUser.getRole()
    );
}
```

---

### Шаг 4: Team Service создаёт пользователя

```java
// UserService.java
@Transactional
public UserDto createUser(UserDto userDto) {
    // 1. Проверка уникальности email
    if (userRepository.existsByEmail(userDto.getEmail())) {
        throw new RuntimeException("User with email already exists");
    }

    // 2. Создание User entity
    User user = new User();
    user.setEmail(userDto.getEmail());
    user.setName(userDto.getName());
    user.setRole(userDto.getRole());  // ← Роль из запроса!
    user.setPassword(userDto.getPassword());  // Уже хеширован!

    // 3. Привязка к команде
    if (userDto.getTeamId() != null) {
        Team team = teamRepository.findById(userDto.getTeamId())
            .orElseThrow(() -> new RuntimeException("Team not found"));
        user.setTeam(team);
    }

    // 4. Сохранение в БД
    User savedUser = userRepository.save(user);
    
    return convertToDto(savedUser);
}
```

---

### Шаг 5: Пользователь получает токены

```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "user@example.com",
  "name": "Test User",
  "role": "DEVELOPER"
}
```

**Access Token содержит:**
```json
{
  "userId": 1,
  "email": "user@example.com",
  "role": "DEVELOPER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

---

## 📝 Примеры создания пользователей

### Пример 1: Разработчик

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "dev1@example.com",
    "name": "Developer One",
    "password": "dev123456",
    "teamId": 1,
    "role": "DEVELOPER"
  }'
```

**Ответ:**
```json
{
  "accessToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "refreshToken": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
  "userId": 1,
  "email": "dev1@example.com",
  "name": "Developer One",
  "role": "DEVELOPER"
}
```

---

### Пример 2: Аппрувер

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "approver1@example.com",
    "name": "Approver One",
    "password": "approve123",
    "teamId": 1,
    "role": "APPROVER"
  }'
```

---

### Пример 3: Team Lead

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "lead@example.com",
    "name": "Team Lead",
    "password": "lead123456",
    "teamId": 1,
    "role": "TEAM_LEAD"
  }'
```

---

### Пример 4: Менеджер

```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "manager@example.com",
    "name": "Project Manager",
    "password": "manager123",
    "teamId": 1,
    "role": "MANAGER"
  }'
```

---

## 🔄 Изменение ролей

### Способ 1: Обновление пользователя (требует JWT)

```bash
curl -X PUT http://localhost:8080/api/users/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "email": "user@example.com",
    "name": "User Name",
    "role": "APPROVER",
    "teamId": 1
  }'
```

**Процесс:**
```java
// UserService.java
@Transactional
public UserDto updateUser(Long id, UserDto userDto) {
    User user = userRepository.findById(id)
        .orElseThrow(() -> new RuntimeException("User not found"));

    user.setEmail(userDto.getEmail());
    user.setName(userDto.getName());
    user.setRole(userDto.getRole());  // ← Изменение роли!

    if (userDto.getTeamId() != null) {
        Team team = teamRepository.findById(userDto.getTeamId())
            .orElseThrow(() -> new RuntimeException("Team not found"));
        user.setTeam(team);
    }

    User updatedUser = userRepository.save(user);
    return convertToDto(updatedUser);
}
```

---

### Способ 2: Напрямую в БД (для администраторов)

```sql
-- Изменить роль пользователя
UPDATE users 
SET role = 'APPROVER' 
WHERE id = 1;

-- Проверить
SELECT id, email, name, role FROM users WHERE id = 1;
```

**⚠️ Внимание:** После изменения роли в БД, пользователь должен перелогиниться, чтобы получить новый JWT токен с обновлённой ролью.

---

## ❓ FAQ

### 1. Кто может создавать пользователей?

**Ответ:**
- ✅ **Любой** может зарегистрироваться через `/api/auth/register`
- ✅ **Администратор** может создать через `/api/users` (требует JWT)

---

### 2. Можно ли зарегистрироваться без команды?

**Ответ:**
- ❌ **НЕТ**, `teamId` обязательное поле
- Сначала нужно создать команду через `/api/teams`

---

### 3. Как создать первого пользователя?

**Ответ:**

**Шаг 1:** Создайте команду
```bash
curl -X POST http://localhost:8080/api/teams \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Development Team",
    "description": "Main development team"
  }'
```

**Шаг 2:** Зарегистрируйте пользователя
```bash
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{
    "email": "admin@example.com",
    "name": "Admin User",
    "password": "admin123",
    "teamId": 1,
    "role": "TEAM_LEAD"
  }'
```

---

### 4. Можно ли изменить роль после регистрации?

**Ответ:**
- ✅ **ДА**, через `PUT /api/users/{id}`
- ⚠️ Требует JWT токен администратора
- ⚠️ Пользователь должен перелогиниться для обновления токена

---

### 5. Какая роль по умолчанию?

**Ответ:**
- ❌ **НЕТ** роли по умолчанию
- ✅ Роль **обязательна** при регистрации
- ✅ Нужно явно указать: `DEVELOPER`, `APPROVER`, `TEAM_LEAD` или `MANAGER`

---

### 6. Может ли пользователь иметь несколько ролей?

**Ответ:**
- ❌ **НЕТ**, в текущей реализации только одна роль
- ✅ Но `TEAM_LEAD` имеет права и DEVELOPER, и APPROVER

---

### 7. Как проверить роль пользователя?

**Ответ:**

**Вариант 1:** Из JWT токена
```javascript
// Frontend
const token = localStorage.getItem('accessToken');
const payload = JSON.parse(atob(token.split('.')[1]));
console.log(payload.role);  // "DEVELOPER"
```

**Вариант 2:** Из API
```bash
curl -X GET http://localhost:8080/api/users/1 \
  -H "Authorization: Bearer YOUR_JWT_TOKEN"
```

**Вариант 3:** В микросервисе
```java
SecurityContext context = SecurityContext.get();
String role = context.getRole();
boolean isApprover = context.hasRole("APPROVER");
```

---

### 8. Что происходит при смене роли?

**Процесс:**
```
1. PUT /api/users/{id} (новая роль)
2. БД обновляется
3. Старый JWT токен ещё валиден (содержит старую роль!)
4. Пользователь должен перелогиниться
5. Новый JWT токен содержит новую роль
```

---

## 📊 Таблица ролей и прав

| Действие | DEVELOPER | APPROVER | TEAM_LEAD | MANAGER |
|----------|-----------|----------|-----------|---------|
| **Создать задачу** | ✅ | ❌ | ✅ | ❌ |
| **Редактировать задачу** | ✅ (свою) | ❌ | ✅ | ❌ |
| **Одобрить задачу** | ❌ | ✅ | ✅ | ❌ |
| **Отклонить задачу** | ❌ | ✅ | ✅ | ❌ |
| **Загрузить файл** | ✅ | ✅ | ✅ | ✅ |
| **Комментировать** | ✅ | ✅ | ✅ | ✅ |
| **Создать спринт** | ❌ | ❌ | ✅ | ✅ |
| **Одобрить спринт** | ❌ | ❌ | ✅ | ✅ |
| **Создать команду** | ❌ | ❌ | ❌ | ✅ |
| **Управлять пользователями** | ❌ | ❌ | ⚠️ | ✅ |

---

## 🎯 Итого

| Вопрос | Ответ |
|--------|-------|
| **Как создаются пользователи?** | `POST /api/auth/register` (самостоятельно) |
| **Кто указывает роль?** | Пользователь при регистрации |
| **Можно ли изменить роль?** | ✅ Да, через `PUT /api/users/{id}` |
| **Сколько ролей?** | 4 роли: DEVELOPER, APPROVER, TEAM_LEAD, MANAGER |
| **Роль по умолчанию?** | ❌ Нет, роль обязательна |
| **Нужна ли команда?** | ✅ Да, `teamId` обязателен |

**Пользователи создаются через самостоятельную регистрацию с указанием роли!** 🚀
