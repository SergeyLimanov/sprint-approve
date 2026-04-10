# Быстрая инструкция по обновлению

## Что изменилось?

✅ Теперь можно комментировать артефакты задач (не только саму задачу)

## Шаги для обновления

### 1. Скопируйте файлы в локальный проект

Скопируйте эти файлы из `C:\work\sprint-approve` в вашу локальную копию `sprint-approve2`:

**Бэкенд:**
- `task-service/src/main/java/org/example/task/entity/Comment.java`
- `task-service/src/main/java/org/example/task/dto/CommentDto.java`
- `task-service/src/main/java/org/example/task/repository/CommentRepository.java`
- `task-service/src/main/java/org/example/task/service/CommentService.java`
- `task-service/src/main/java/org/example/task/controller/CommentController.java`

**Фронтенд:**
- `frontend/src/types/index.ts`
- `frontend/src/api/client.ts`
- `frontend/src/pages/TaskDetail.tsx`

### 2. Перезапустите бэкенд

```bash
# Остановите task-service (Ctrl+C)
cd task-service
mvn spring-boot:run
```

БД обновится автоматически при старте.

### 3. Перезапустите фронтенд

```bash
# Остановите (Ctrl+C) и запустите
cd frontend
npm run dev
```

## Готово!

Теперь на странице задачи под каждым артефактом можно:
- Видеть количество комментариев
- Раскрывать секцию комментариев (клик по счетчику)
- Добавлять комментарии
- Удалять комментарии

---

Подробная документация: `ARTIFACT_COMMENTS_UPDATE.md`
