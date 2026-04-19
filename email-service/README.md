# Email Service

Email notification service для Sprint Approve.

## Установка

```bash
npm install
```

## Настройка

1. Скопируйте `.env.example` в `.env`:
```bash
cp .env.example .env
```

2. Настройте переменные окружения в `.env`:

### Для Gmail:
- Включите двухфакторную аутентификацию
- Создайте App Password: https://myaccount.google.com/apppasswords
- Используйте App Password в `SMTP_PASS`

```env
PORT=3001
SMTP_HOST=smtp.gmail.com
SMTP_PORT=587
SMTP_USER=your-email@gmail.com
SMTP_PASS=your-app-password
FROM_EMAIL=your-email@gmail.com
FROM_NAME=Sprint Approve
APP_URL=http://localhost:3000
```

### Для других SMTP серверов:
Измените `SMTP_HOST` и `SMTP_PORT` соответственно.

## Запуск

```bash
# Production
npm start

# Development (с автоперезагрузкой)
npm run dev
```

## API Endpoints

### Health Check
```
GET /health
```

### Send Email
```
POST /api/email/send
Content-Type: application/json

{
  "to": "user@example.com",
  "subject": "Test Email",
  "text": "Plain text content",
  "html": "<p>HTML content</p>",
  "taskId": 123 // optional
}
```

### Send Notification Email
```
POST /api/email/notification
Content-Type: application/json

{
  "userEmail": "user@example.com",
  "userName": "John Doe",
  "message": "Вам назначена новая задача",
  "type": "TASK_ASSIGNED",
  "taskId": 123
}
```

## Типы уведомлений

- `TASK_ASSIGNED` - Назначена новая задача
- `TASK_SUBMITTED_FOR_REVIEW` - Задача отправлена на рассмотрение
- `TASK_APPROVED` - Задача одобрена
- `TASK_REJECTED` - Задача отклонена
