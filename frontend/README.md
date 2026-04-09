# Sprint Approve - Frontend

Современный веб-интерфейс для системы согласования задач Sprint Approve, построенный на React + TypeScript + Vite.

## Технологии

- **React 18** - UI библиотека
- **TypeScript** - типизация
- **Vite** - сборщик и dev-сервер
- **React Router** - маршрутизация
- **Axios** - HTTP клиент
- **TailwindCSS** - стилизация
- **Lucide React** - иконки

## Установка

```bash
# Установить зависимости
npm install

# Запустить dev-сервер
npm run dev

# Собрать для продакшена
npm run build
```

## Запуск

### Development режим

```bash
npm run dev
```

Приложение будет доступно по адресу: http://localhost:3000

API запросы автоматически проксируются на http://localhost:8080 (API Gateway).

### Production сборка

```bash
npm run build
npm run preview
```

## Структура проекта

```
frontend/
├── src/
│   ├── api/
│   │   └── client.ts          # API клиент для бэкенда
│   ├── components/
│   │   └── Layout.tsx         # Основной layout с навигацией
│   ├── pages/
│   │   ├── Dashboard.tsx      # Главная страница с статистикой
│   │   ├── Teams.tsx          # Управление командами
│   │   ├── Users.tsx          # Управление пользователями
│   │   ├── Sprints.tsx        # Список спринтов
│   │   ├── SprintDetail.tsx   # Детали спринта
│   │   └── Tasks.tsx          # Управление задачами
│   ├── types/
│   │   └── index.ts           # TypeScript типы
│   ├── App.tsx                # Главный компонент
│   ├── main.tsx               # Точка входа
│   └── index.css              # Глобальные стили
├── index.html
├── package.json
├── vite.config.ts
├── tailwind.config.js
└── tsconfig.json
```

## Основные функции

### Dashboard
- Статистика по командам, пользователям, спринтам и задачам
- Быстрые действия для создания сущностей
- Статус задач

### Команды
- Создание, редактирование и удаление команд
- Просмотр списка команд с описаниями

### Пользователи
- Управление пользователями
- Назначение ролей (Team Lead, Developer, Manager, Approver)
- Привязка к командам

### Спринты
- Создание спринтов и МВП
- Просмотр деталей спринта
- Статистика по задачам спринта
- Одобрение/отклонение спринтов

### Задачи
- Создание задач с назначением исполнителя и аппрувера
- Отправка на рассмотрение
- Одобрение/отклонение задач
- Фильтрация по статусам

## API Endpoints

Все запросы проксируются через `/api` на бэкенд:

- `/api/teams` - Команды
- `/api/users` - Пользователи
- `/api/sprints` - Спринты
- `/api/tasks` - Задачи
- `/api/artifacts` - Артефакты
- `/api/comments` - Комментарии

## Требования

- Node.js 18+
- npm или yarn
- Запущенный бэкенд (API Gateway на порту 8080)

## Разработка

### Добавление новой страницы

1. Создайте компонент в `src/pages/`
2. Добавьте роут в `src/App.tsx`
3. Добавьте пункт в навигацию в `src/components/Layout.tsx`

### Добавление нового API endpoint

1. Добавьте метод в `src/api/client.ts`
2. Используйте в компонентах через `useEffect` и `useState`

## Стили

Проект использует TailwindCSS с кастомными утилитами:

- `.btn` - базовая кнопка
- `.btn-primary`, `.btn-secondary`, `.btn-success`, `.btn-danger` - варианты кнопок
- `.card` - карточка с тенью
- `.input` - поле ввода
- `.badge` - бейдж со статусом
- `.badge-created`, `.badge-on-review`, `.badge-approved`, `.badge-rejected` - цвета статусов

## Troubleshooting

### Ошибка подключения к API

Убедитесь, что:
1. Бэкенд запущен на порту 8080
2. API Gateway работает
3. Все микросервисы зарегистрированы в Eureka

### Ошибки сборки

```bash
# Очистить node_modules и переустановить
rm -rf node_modules package-lock.json
npm install
```

## Лицензия

MIT
