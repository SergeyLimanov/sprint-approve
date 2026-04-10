# Обновление: Комментарии к артефактам

## Описание изменений

Добавлена возможность комментировать артефакты задач. Теперь комментарии могут быть привязаны либо к задаче в целом, либо к конкретному артефакту.

## Изменения в бэкенде

### 1. Обновленные файлы

- **Comment.java** - добавлено поле `artifact` для связи с артефактом
- **CommentDto.java** - добавлено поле `artifactId`
- **CommentRepository.java** - добавлен метод `findByArtifactId()`
- **CommentService.java** - добавлена логика для работы с комментариями артефактов
- **CommentController.java** - добавлен endpoint `GET /api/comments/artifact/{artifactId}`

### 2. Изменения в базе данных

Hibernate автоматически обновит схему при запуске (используется `ddl-auto: update`).

Если нужно применить миграцию вручную, используйте скрипт:
```
task-service/src/main/resources/db/migration/V2__add_artifact_comments.sql
```

Или выполните SQL:
```sql
ALTER TABLE comments ALTER COLUMN task_id DROP NOT NULL;
ALTER TABLE comments ADD COLUMN artifact_id BIGINT;
ALTER TABLE comments ADD CONSTRAINT fk_comments_artifact 
    FOREIGN KEY (artifact_id) REFERENCES artifacts(id) ON DELETE CASCADE;
CREATE INDEX idx_comments_artifact_id ON comments(artifact_id);
```

## Изменения во фронтенде

### 1. Обновленные файлы

- **types/index.ts** - обновлен интерфейс `Comment` (добавлено `artifactId?`)
- **api/client.ts** - добавлен метод `commentsApi.getByArtifact()`
- **pages/TaskDetail.tsx** - полностью переработан UI артефактов с поддержкой комментариев

### 2. Новый функционал UI

- Каждый артефакт теперь имеет счетчик комментариев
- Клик по счетчику раскрывает секцию комментариев
- Можно добавлять комментарии прямо под артефактом
- Комментарии отображаются с автором и временем создания
- Поддержка удаления комментариев

## Запуск обновленной версии

### Бэкенд

1. Остановите `task-service`:
```bash
# Если запущен через Maven
Ctrl+C
```

2. Пересоберите проект:
```bash
cd task-service
mvn clean install
```

3. Запустите сервис:
```bash
mvn spring-boot:run
```

Hibernate автоматически обновит схему БД при старте.

### Фронтенд

1. Скопируйте обновленные файлы в вашу локальную копию проекта:
   - `frontend/src/types/index.ts`
   - `frontend/src/api/client.ts`
   - `frontend/src/pages/TaskDetail.tsx`

2. Перезапустите dev-сервер (если он запущен):
```bash
# Остановите (Ctrl+C) и запустите снова
npm run dev
```

## Использование

1. Перейдите на страницу задачи
2. В разделе "Артефакты" вы увидите счетчик комментариев рядом с каждым артефактом
3. Нажмите на счетчик (иконка сообщения), чтобы раскрыть комментарии
4. Введите текст комментария и нажмите Enter или кнопку отправки
5. Комментарии отображаются с информацией об авторе и времени

## API Endpoints

### Новый endpoint

**GET** `/api/comments/artifact/{artifactId}`
- Получить все комментарии артефакта
- Возвращает: `List<CommentDto>`

### Обновленный endpoint

**POST** `/api/comments`
- Теперь поддерживает создание комментариев как к задаче, так и к артефакту
- Body должен содержать либо `taskId`, либо `artifactId` (но не оба)

Пример для комментария к артефакту:
```json
{
  "content": "Отличный файл!",
  "artifactId": 1,
  "authorId": 1
}
```

## Проверка работы

1. Создайте задачу
2. Добавьте артефакт к задаче
3. Откройте детали задачи
4. Нажмите на счетчик комментариев артефакта
5. Добавьте комментарий
6. Убедитесь, что комментарий отображается

## Откат изменений

Если нужно откатить изменения в БД:

```sql
ALTER TABLE comments DROP CONSTRAINT IF EXISTS check_comment_target;
ALTER TABLE comments DROP CONSTRAINT IF EXISTS fk_comments_artifact;
DROP INDEX IF EXISTS idx_comments_artifact_id;
ALTER TABLE comments DROP COLUMN IF EXISTS artifact_id;
ALTER TABLE comments ALTER COLUMN task_id SET NOT NULL;
```

## Технические детали

- Комментарии к артефактам хранятся в той же таблице `comments`
- Используется дискриминатор: либо `task_id`, либо `artifact_id` заполнен
- Добавлен constraint для обеспечения целостности данных
- Каскадное удаление: при удалении артефакта удаляются его комментарии
