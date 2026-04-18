# 🚀 Sprint Approve - Краткая презентация (5 минут)

## 📋 Что это?

**Sprint Approve** — система управления задачами с механизмом одобрения для команд разработки.

**Проблема:** Нет контроля качества выполненных задач в спринте  
**Решение:** Workflow с обязательным одобрением + автоматическая синхронизация статусов

---

## 🏗️ Архитектура

### Микросервисы (7 сервисов):

```
Frontend (React) → API Gateway → Микросервисы
                                      ↓
                        ┌─────────────┼─────────────┐
                        │             │             │
                   Eureka Server  Auth Service  Team Service
                                      │
                        ┌─────────────┼─────────────┐
                        │             │             │
                  Sprint Service  Task Service  Notification
                        │             │             Service
                        ↓             ↓
                   PostgreSQL     MinIO + PostgreSQL
```

---

## 💻 Технологический стек

### Backend:
- **Java 17** + **Spring Boot 3.2.0**
- **Spring Cloud** (Gateway, Eureka, OpenFeign)
- **PostgreSQL 15** (4 базы данных)
- **JWT** (аутентификация)
- **MinIO** (файловое хранилище)
- **Firebase Cloud Messaging** (push-уведомления)

### Frontend:
- **React 18** + **TypeScript**
- **Vite** (сборка)
- **TailwindCSS** (стилизация)
- **Axios** (HTTP клиент)

### Infrastructure:
- **Docker** + **Docker Compose**
- **4 PostgreSQL** контейнера
- **MinIO** контейнер

---

## 🔧 Микросервисы

| Сервис | Порт | Роль |
|--------|------|------|
| **Eureka Server** | 8761 | Service Discovery |
| **API Gateway** | 8080 | Маршрутизация + JWT валидация |
| **Auth Service** | 8084 | Аутентификация (JWT) |
| **Team Service** | 8081 | Пользователи и команды |
| **Sprint Service** | 8082 | Спринты + автосинхронизация |
| **Task Service** | 8083 | Задачи + файлы + комментарии |
| **Notification Service** | 8085 | Push-уведомления (FCM) |

---

## 🔗 Интеграции (OpenFeign)

### 5 Feign клиентов для межсервисного взаимодействия:

1. **task-service → sprint-service**  
   → Автоматический пересчёт статуса спринта

2. **task-service → team-service**  
   → Обогащение данных (имена пользователей)

3. **sprint-service → task-service**  
   → Получение задач для пересчёта статуса

4. **sprint-service → team-service**  
   → Валидация команд

5. **auth-service → team-service**  
   → Аутентификация и регистрация

---

## 🔐 Безопасность

### JWT Authentication:
```
1. POST /api/auth/login
2. Получение Access Token (24h) + Refresh Token (7d)
3. Все запросы: Authorization: Bearer <token>
4. API Gateway валидирует JWT
5. Микросервисы получают X-User-Id, X-User-Email, X-User-Role
```

### Защита:
- ✅ **BCrypt** хеширование паролей
- ✅ **JWT** токены (HS256)
- ✅ **.gitignore** защита секретов
- ✅ **Presigned URLs** для файлов (временные ссылки)

---

## ✨ Ключевые функции

### 1. Автоматическая синхронизация статусов
```
Задача одобрена → Task Service → Sprint Service → Статус спринта обновлён
```

### 2. Файловое хранилище (MinIO)
- Загрузка файлов (изображения, документы, видео)
- Хранение в S3-совместимом хранилище
- Временные ссылки для скачивания (60 мин)

### 3. Push-уведомления (FCM)
- Задача назначена
- Задача одобрена/отклонена
- Новый комментарий
- Новый файл
- Фоновые уведомления (Service Worker)

### 4. Комментарии и артефакты
- Комментирование задач
- Прикрепление файлов
- История изменений

---

## 🎬 Демо: Создание и одобрение задачи

```bash
# 1. Регистрация
POST /api/auth/register
→ Получение JWT токена

# 2. Создание задачи
POST /api/tasks
→ Статус: CREATED
→ Push-уведомление исполнителю

# 3. Загрузка файла
POST /api/artifacts/upload
→ Файл в MinIO
→ Presigned URL для скачивания

# 4. Отправка на проверку
PATCH /api/tasks/1/submit
→ Статус задачи: ON_REVIEW
→ Статус спринта: ON_REVIEW (автоматически!)
→ Push-уведомление аппруверу

# 5. Одобрение
PATCH /api/tasks/1/approve
→ Статус задачи: APPROVED
→ Статус спринта: APPROVED (если все задачи одобрены)
→ Push-уведомление создателю
```

---

## 📊 Метрики

- **7 микросервисов**
- **~50 REST endpoints**
- **4 PostgreSQL базы данных**
- **5 Feign интеграций**
- **7 типов push-уведомлений**
- **10+ backend технологий**
- **8+ frontend технологий**

---

## 🎯 Что демонстрирует проект?

### Архитектура:
✅ Микросервисная архитектура  
✅ Service Discovery (Eureka)  
✅ API Gateway паттерн  
✅ Database per Service  
✅ Inter-service communication (REST)

### Технологии:
✅ Spring Boot + Spring Cloud  
✅ JWT аутентификация  
✅ MinIO (S3-совместимое хранилище)  
✅ Firebase Cloud Messaging  
✅ Docker + Docker Compose

### Best Practices:
✅ Автоматическая синхронизация  
✅ Безопасность (JWT, BCrypt, .gitignore)  
✅ Документация (Swagger, README)  
✅ Persistent storage (Docker volumes)

---

## 🚀 Запуск

```bash
# 1. Инфраструктура
docker-compose up -d

# 2. Backend (7 сервисов)
mvn spring-boot:run  # в каждом сервисе

# 3. Frontend
cd frontend && npm run dev
```

**Доступ:**
- Frontend: http://localhost:3000
- API: http://localhost:8080
- Eureka: http://localhost:8761
- MinIO: http://localhost:9001

---

## 📚 Документация

- `PRESENTATION.md` - полная презентация
- `README.md` - общая информация
- `ARCHITECTURE.md` - архитектура
- `SECURITY.md` - безопасность
- `FCM_INTEGRATION_GUIDE.md` - FCM интеграция

---

## 🎓 Итого

**Sprint Approve** — это:
- ✅ Production-ready микросервисная архитектура
- ✅ Полный стек технологий (Backend + Frontend + Infrastructure)
- ✅ Современные best practices
- ✅ Готовность к масштабированию

**Идеально для:**
- Портфолио разработчика
- Курсовой/дипломный проект
- Стартап MVP

---

**GitHub:** https://github.com/SergeyLimanov/sprint-approve

**Спасибо за внимание!** 🚀
