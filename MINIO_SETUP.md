# MinIO Integration - Руководство по настройке

## 🎯 Что было сделано

Интегрировано MinIO для хранения файлов с поддержкой временных ссылок (presigned URLs).

---

## 📋 Изменённые файлы

### 1. **docker-compose.yml**
- Добавлен сервис MinIO
- Порты: 9000 (API), 9001 (Web Console)
- Volume: `minio-data` для persistent storage

### 2. **task-service/pom.xml**
- Добавлена зависимость `io.minio:minio:8.5.7`

### 3. **Новые файлы:**
- `MinioConfig.java` - конфигурация MinIO клиента
- `MinioStorageService.java` - сервис для работы с файлами

### 4. **Обновлённые файлы:**
- `ArtifactDto.java` - добавлено поле `downloadUrl`
- `ArtifactController.java` - обновлены endpoints для работы с MinIO
- `application.yml` - добавлена конфигурация MinIO

---

## 🚀 Запуск

### 1. Запустите MinIO
```bash
docker-compose up -d minio
```

### 2. Откройте MinIO Console
```
URL: http://localhost:9001
Login: admin
Password: password123
```

### 3. Запустите task-service
```bash
cd task-service
mvn spring-boot:run
```

При старте автоматически создастся bucket `task-files`.

---

## 📝 API Endpoints

### Загрузка файла
```http
POST /api/artifacts/upload
Content-Type: multipart/form-data

file: [binary]
taskId: 1
uploadedBy: 1
name: "screenshot.jpg" (optional)
```

**Response:**
```json
{
  "id": 1,
  "name": "screenshot.jpg",
  "url": "123e4567-e89b-12d3-a456-426614174000.jpg",
  "downloadUrl": "http://localhost:9000/task-files/123e4567-...-000.jpg?X-Amz-Signature=...",
  "fileType": "image/jpeg",
  "fileSize": 524288,
  "taskId": 1
}
```

### Получить артефакты задачи с временными ссылками
```http
GET /api/artifacts/task/1?urlExpiryMinutes=60
```

**Response:**
```json
[
  {
    "id": 1,
    "name": "screenshot.jpg",
    "url": "123e4567-e89b-12d3-a456-426614174000.jpg",
    "downloadUrl": "http://localhost:9000/task-files/123e4567-...-000.jpg?X-Amz-Signature=...",
    "fileType": "image/jpeg",
    "fileSize": 524288
  }
]
```

### Получить временную ссылку на файл
```http
GET /api/artifacts/5/download-url?expiryMinutes=30
```

**Response:**
```json
{
  "url": "http://localhost:9000/task-files/123e4567-...-000.jpg?X-Amz-Signature=...",
  "expiresIn": "30 minutes"
}
```

---

## 🔧 Конфигурация

### Переменные окружения

```bash
# MinIO endpoint
MINIO_ENDPOINT=http://localhost:9000

# Credentials
MINIO_ACCESS_KEY=admin
MINIO_SECRET_KEY=password123

# Bucket name
MINIO_BUCKET=task-files
```

### application.yml
```yaml
minio:
  endpoint: ${MINIO_ENDPOINT:http://localhost:9000}
  access-key: ${MINIO_ACCESS_KEY:admin}
  secret-key: ${MINIO_SECRET_KEY:password123}
  bucket: ${MINIO_BUCKET:task-files}
```

---

## 📊 Как это работает

### Загрузка файла:
```
1. Клиент → POST /api/artifacts/upload (файл)
2. MinioStorageService → сохраняет в MinIO bucket
3. Генерируется UUID имя: 123e4567-...-000.jpg
4. В БД сохраняется только UUID имя
5. Возвращается presigned URL (действует 1 час)
```

### Скачивание файла:
```
1. Клиент → GET /api/artifacts/5/download-url
2. Генерируется presigned URL
3. Клиент → GET presigned URL (напрямую к MinIO)
4. MinIO → возвращает файл
```

---

## 🔐 Временные ссылки (Presigned URLs)

### Преимущества:
- ✅ Безопасно - ссылка действует ограниченное время
- ✅ Производительно - файлы отдаются напрямую из MinIO
- ✅ Масштабируемо - разгружает task-service

### Время жизни:
- По умолчанию: **60 минут**
- Минимум: **1 минута**
- Максимум: **7 дней** (ограничение S3/MinIO)

### Пример использования:
```javascript
// Frontend: Получаем временную ссылку
const response = await fetch('/api/artifacts/5/download-url?expiryMinutes=5');
const { url } = await response.json();

// Скачиваем файл напрямую из MinIO
window.open(url);
```

---

## 🗄️ Структура хранения

### В MinIO:
```
Bucket: task-files
├── 123e4567-e89b-12d3-a456-426614174000.jpg
├── 987fcdeb-51a2-43f7-9c8d-1234567890ab.pdf
└── a1b2c3d4-e5f6-7890-abcd-ef1234567890.mp4
```

### В PostgreSQL (task_db.artifacts):
```sql
id | name           | url                                      | file_type  | file_size
---|----------------|------------------------------------------|------------|----------
1  | screenshot.jpg | 123e4567-e89b-12d3-a456-426614174000.jpg | image/jpeg | 524288
2  | spec.pdf       | 987fcdeb-51a2-43f7-9c8d-1234567890ab.pdf | application/pdf | 1048576
```

---

## 🚨 Важно для production

### 1. Измените credentials
```yaml
minio:
  access-key: ${MINIO_ACCESS_KEY}  # Из secrets
  secret-key: ${MINIO_SECRET_KEY}  # Из secrets
```

### 2. Настройте HTTPS
```yaml
minio:
  endpoint: https://minio.yourdomain.com
```

### 3. Настройте backup
```bash
# Backup MinIO data
docker run --rm -v minio-data:/data -v $(pwd):/backup \
  alpine tar czf /backup/minio-backup.tar.gz /data
```

### 4. Настройте репликацию (опционально)
MinIO поддерживает multi-node deployment для high availability.

---

## 🔄 Миграция существующих файлов

Если у вас уже есть файлы в `uploads/`:

```bash
# 1. Запустите MinIO
docker-compose up -d minio

# 2. Установите MinIO Client
wget https://dl.min.io/client/mc/release/linux-amd64/mc
chmod +x mc

# 3. Настройте alias
./mc alias set local http://localhost:9000 admin password123

# 4. Создайте bucket
./mc mb local/task-files

# 5. Скопируйте файлы
./mc cp --recursive task-service/uploads/* local/task-files/

# 6. Обновите БД (замените пути на UUID имена)
```

---

## 📈 Мониторинг

### MinIO Console
- URL: http://localhost:9001
- Метрики, логи, управление buckets

### Проверка здоровья
```bash
curl http://localhost:9000/minio/health/live
```

---

## 🎯 Итого

✅ **Файлы хранятся в MinIO** (не в контейнере task-service)  
✅ **Временные ссылки** для безопасного доступа  
✅ **Масштабируемо** - можно запустить несколько task-service  
✅ **Production-ready** - MinIO используется в крупных проектах  
✅ **Файлы НЕ удалятся** при обновлении task-service  

**Проект готов к деплою на стенд!** 🚀
