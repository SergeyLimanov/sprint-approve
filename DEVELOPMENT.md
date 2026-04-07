# Руководство по разработке

## Настройка окружения разработки

### Требования
- **JDK 17** или выше
- **Maven 3.6+**
- **Docker Desktop** (для баз данных)
- **IntelliJ IDEA** (рекомендуется Ultimate Edition)
- **Git**

### Установка и настройка

#### 1. Клонирование проекта
```bash
git clone <repository-url>
cd sprint-approve
```

#### 2. Импорт в IntelliJ IDEA

1. Откройте IntelliJ IDEA
2. File → Open → выберите папку `sprint-approve`
3. IDEA автоматически распознает Maven multi-module проект
4. Дождитесь индексации и загрузки зависимостей

#### 3. Настройка Lombok

1. File → Settings → Plugins
2. Найдите и установите "Lombok"
3. File → Settings → Build, Execution, Deployment → Compiler → Annotation Processors
4. Включите "Enable annotation processing"

#### 4. Настройка Run Configurations

Создайте конфигурации запуска для каждого сервиса:

**Eureka Server:**
- Name: Eureka Server
- Main class: `org.example.eureka.EurekaServerApplication`
- Working directory: `$MODULE_WORKING_DIR$`
- Use classpath of module: `eureka-server`

**Team Service:**
- Name: Team Service
- Main class: `org.example.team.TeamServiceApplication`
- Working directory: `$MODULE_WORKING_DIR$`
- Use classpath of module: `team-service`

**Sprint Service:**
- Name: Sprint Service
- Main class: `org.example.sprint.SprintServiceApplication`
- Working directory: `$MODULE_WORKING_DIR$`
- Use classpath of module: `sprint-service`

**Task Service:**
- Name: Task Service
- Main class: `org.example.task.TaskServiceApplication`
- Working directory: `$MODULE_WORKING_DIR$`
- Use classpath of module: `task-service`

**API Gateway:**
- Name: API Gateway
- Main class: `org.example.gateway.ApiGatewayApplication`
- Working directory: `$MODULE_WORKING_DIR$`
- Use classpath of module: `api-gateway`

#### 5. Создайте Compound Configuration

Для запуска всех сервисов одновременно:
1. Run → Edit Configurations
2. Add New Configuration → Compound
3. Name: "All Services"
4. Добавьте все конфигурации в порядке:
   - Eureka Server
   - Team Service
   - Sprint Service
   - Task Service
   - API Gateway

## Структура проекта

```
sprint-approve/
├── eureka-server/              # Service Discovery
│   ├── src/main/java/
│   │   └── org/example/eureka/
│   │       └── EurekaServerApplication.java
│   └── src/main/resources/
│       └── application.yml
│
├── team-service/               # Team & User Management
│   ├── src/main/java/
│   │   └── org/example/team/
│   │       ├── controller/     # REST контроллеры
│   │       ├── dto/            # Data Transfer Objects
│   │       ├── entity/         # JPA сущности
│   │       ├── repository/     # Spring Data репозитории
│   │       └── service/        # Бизнес-логика
│   └── src/main/resources/
│       └── application.yml
│
├── sprint-service/             # Sprint & MVP Management
│   ├── src/main/java/
│   │   └── org/example/sprint/
│   │       ├── client/         # Feign клиенты
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── repository/
│   │       └── service/
│   └── src/main/resources/
│       └── application.yml
│
├── task-service/               # Task, Artifact & Comment Management
│   ├── src/main/java/
│   │   └── org/example/task/
│   │       ├── client/
│   │       ├── controller/
│   │       ├── dto/
│   │       ├── entity/
│   │       ├── repository/
│   │       └── service/
│   └── src/main/resources/
│       └── application.yml
│
├── api-gateway/                # API Gateway
│   ├── src/main/java/
│   │   └── org/example/gateway/
│   │       └── ApiGatewayApplication.java
│   └── src/main/resources/
│       └── application.yml
│
├── docker-compose.yml          # Docker конфигурация для БД
├── pom.xml                     # Родительский POM
├── README.md                   # Основная документация
├── ARCHITECTURE.md             # Архитектурная документация
├── DEVELOPMENT.md              # Это руководство
└── api-examples.http           # Примеры API запросов
```

## Работа с базами данных

### Запуск PostgreSQL через Docker

```bash
# Запуск всех БД
docker-compose up -d

# Проверка статуса
docker ps

# Просмотр логов
docker logs team-db
docker logs sprint-db
docker logs task-db

# Остановка
docker-compose down

# Остановка с удалением данных
docker-compose down -v
```

### Подключение к БД

**Team DB:**
- Host: localhost
- Port: 5432
- Database: team_db
- User: postgres
- Password: postgres

**Sprint DB:**
- Host: localhost
- Port: 5433
- Database: sprint_db
- User: postgres
- Password: postgres

**Task DB:**
- Host: localhost
- Port: 5434
- Database: task_db
- User: postgres
- Password: postgres

### Использование DataGrip/Database Tools

1. Откройте Database panel (View → Tool Windows → Database)
2. Добавьте новый Data Source → PostgreSQL
3. Введите параметры подключения
4. Test Connection → OK

## Разработка нового функционала

### 1. Создание новой сущности

```java
@Entity
@Table(name = "entity_name")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityName {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(nullable = false)
    private String field;
    
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
```

### 2. Создание Repository

```java
@Repository
public interface EntityRepository extends JpaRepository<EntityName, Long> {
    List<EntityName> findByField(String field);
}
```

### 3. Создание DTO

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class EntityDto {
    private Long id;
    
    @NotBlank(message = "Field is required")
    private String field;
    
    private LocalDateTime createdAt;
}
```

### 4. Создание Service

```java
@Service
@RequiredArgsConstructor
public class EntityService {
    private final EntityRepository repository;
    
    @Transactional(readOnly = true)
    public List<EntityDto> getAll() {
        return repository.findAll().stream()
            .map(this::convertToDto)
            .collect(Collectors.toList());
    }
    
    @Transactional
    public EntityDto create(EntityDto dto) {
        EntityName entity = new EntityName();
        entity.setField(dto.getField());
        
        EntityName saved = repository.save(entity);
        return convertToDto(saved);
    }
    
    private EntityDto convertToDto(EntityName entity) {
        EntityDto dto = new EntityDto();
        dto.setId(entity.getId());
        dto.setField(entity.getField());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }
}
```

### 5. Создание Controller

```java
@RestController
@RequestMapping("/api/entities")
@RequiredArgsConstructor
@Tag(name = "Entities", description = "Entity management API")
public class EntityController {
    private final EntityService service;
    
    @GetMapping
    @Operation(summary = "Get all entities")
    public ResponseEntity<List<EntityDto>> getAll() {
        return ResponseEntity.ok(service.getAll());
    }
    
    @PostMapping
    @Operation(summary = "Create entity")
    public ResponseEntity<EntityDto> create(@Valid @RequestBody EntityDto dto) {
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(service.create(dto));
    }
}
```

## Тестирование

### Запуск тестов

```bash
# Все тесты
mvn test

# Тесты конкретного модуля
cd team-service
mvn test

# Пропустить тесты при сборке
mvn clean install -DskipTests
```

### Тестирование API через HTTP Client

1. Откройте файл `api-examples.http` в IntelliJ IDEA
2. Запустите все сервисы
3. Нажмите на зеленую стрелку рядом с запросом
4. Просмотрите результат в панели справа

### Тестирование через Swagger UI

Каждый сервис имеет Swagger UI:
- Team Service: http://localhost:8081/swagger-ui.html
- Sprint Service: http://localhost:8082/swagger-ui.html
- Task Service: http://localhost:8083/swagger-ui.html

### Тестирование через Postman

Импортируйте примеры из `api-examples.http` или создайте коллекцию:

1. Создайте Environment с переменной `baseUrl = http://localhost:8080`
2. Создайте запросы для каждого endpoint
3. Используйте Tests для автоматической проверки

## Отладка

### Отладка в IntelliJ IDEA

1. Установите breakpoint (клик на номере строки)
2. Запустите сервис в режиме Debug (Shift+F9)
3. Выполните запрос к API
4. Используйте Debug панель для анализа

### Просмотр логов

Логи отображаются в консоли каждого сервиса. Для более удобного просмотра:

1. View → Tool Windows → Services
2. Все запущенные сервисы будут в списке
3. Выберите сервис для просмотра логов

### Уровни логирования

Измените в `application.yml`:

```yaml
logging:
  level:
    org.example: DEBUG
    org.springframework.web: DEBUG
    org.hibernate.SQL: DEBUG
```

## Общие проблемы и решения

### Проблема: Порт уже занят

```
Error: Port 8080 is already in use
```

**Решение:**
```bash
# Windows
netstat -ano | findstr :8080
taskkill /PID <PID> /F

# Или измените порт в application.yml
server:
  port: 8090
```

### Проблема: Сервис не регистрируется в Eureka

**Решение:**
1. Убедитесь, что Eureka Server запущен
2. Проверьте `eureka.client.service-url.defaultZone`
3. Проверьте, что `@EnableDiscoveryClient` присутствует
4. Подождите 30 секунд для регистрации

### Проблема: Cannot connect to database

**Решение:**
1. Проверьте, что Docker контейнеры запущены: `docker ps`
2. Проверьте параметры подключения в `application.yml`
3. Проверьте логи контейнера: `docker logs team-db`

### Проблема: Feign Client ошибки

**Решение:**
1. Убедитесь, что целевой сервис запущен и зарегистрирован в Eureka
2. Проверьте имя сервиса в `@FeignClient(name = "service-name")`
3. Добавьте логирование Feign:
```yaml
logging:
  level:
    org.example.sprint.client: DEBUG
```

## Coding Standards

### Именование

- **Классы:** PascalCase (`TeamService`, `UserController`)
- **Методы:** camelCase (`createTeam`, `getUserById`)
- **Константы:** UPPER_SNAKE_CASE (`MAX_RETRY_COUNT`)
- **Пакеты:** lowercase (`org.example.team.service`)

### Структура кода

1. **Controller** - только маршрутизация и валидация
2. **Service** - бизнес-логика и транзакции
3. **Repository** - доступ к данным
4. **DTO** - передача данных между слоями

### Обработка ошибок

Используйте осмысленные сообщения об ошибках:

```java
throw new RuntimeException("Team not found with id: " + id);
```

Для продакшена создайте кастомные исключения:

```java
public class TeamNotFoundException extends RuntimeException {
    public TeamNotFoundException(Long id) {
        super("Team not found with id: " + id);
    }
}
```

### Транзакции

- Используйте `@Transactional` для методов, изменяющих данные
- Используйте `@Transactional(readOnly = true)` для чтения

```java
@Transactional
public TeamDto createTeam(TeamDto dto) {
    // ...
}

@Transactional(readOnly = true)
public List<TeamDto> getAllTeams() {
    // ...
}
```

## Git Workflow

### Ветки

- `main` - стабильная версия
- `develop` - разработка
- `feature/feature-name` - новый функционал
- `bugfix/bug-name` - исправление бага

### Commit Messages

Используйте понятные сообщения:

```
feat: добавлен endpoint для создания команды
fix: исправлена ошибка валидации email
refactor: рефакторинг TeamService
docs: обновлена документация API
```

## Полезные команды Maven

```bash
# Очистка и сборка
mvn clean install

# Сборка без тестов
mvn clean install -DskipTests

# Запуск конкретного сервиса
mvn spring-boot:run -pl team-service

# Обновление зависимостей
mvn clean install -U

# Проверка зависимостей
mvn dependency:tree

# Форматирование кода
mvn spotless:apply
```

## Дополнительные ресурсы

- [Spring Boot Documentation](https://spring.io/projects/spring-boot)
- [Spring Cloud Documentation](https://spring.io/projects/spring-cloud)
- [Spring Data JPA](https://spring.io/projects/spring-data-jpa)
- [OpenFeign](https://spring.io/projects/spring-cloud-openfeign)
- [Netflix Eureka](https://spring.io/guides/gs/service-registration-and-discovery/)
