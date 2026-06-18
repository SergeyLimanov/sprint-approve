# 🏗️ Подробное описание микросервисов Sprint Approve - Часть 1

## Автор: Сергей Лиманов

---

## 📋 Содержание

### Часть 1 (этот файл):
1. [Eureka Server](#1️⃣-eureka-server)
2. [API Gateway](#2️⃣-api-gateway)
3. [Auth Service](#3️⃣-auth-service)

### Часть 2 (`MICROSERVICES_PART2.md`):
4. Team Service
5. Sprint Service
6. Task Service
7. Notification Service

---

# 1️⃣ EUREKA SERVER

## 📌 Назначение
**Service Discovery** — реестр всех микросервисов. Позволяет сервисам находить друг друга по имени, а не по IP-адресу.

## 🎯 Зачем нужен
- Автоматическая регистрация сервисов при запуске
- Балансировка нагрузки между инстансами
- Health check — проверка доступности сервисов
- Динамическое обнаружение сервисов (не нужно хардкодить URL)

## 📂 Структура

```
eureka-server/
├── src/main/java/org/example/eureka/
│   └── EurekaServerApplication.java
└── src/main/resources/
    └── application.yml
```

---

## 📝 Классы

### `EurekaServerApplication.java`

**Назначение:** Точка входа в приложение Eureka Server

**Код:**
```java
package org.example.eureka;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.netflix.eureka.server.EnableEurekaServer;

@SpringBootApplication
@EnableEurekaServer  // ← Включает Eureka Server
public class EurekaServerApplication {
    public static void main(String[] args) {
        SpringApplication.run(EurekaServerApplication.class, args);
    }
}
```

**Аннотации:**
- `@SpringBootApplication` — стандартная Spring Boot аннотация
- `@EnableEurekaServer` — **включает функционал Eureka Server**

**Что делает:**
1. Запускает Eureka Server на порту 8761
2. Предоставляет REST API для регистрации сервисов
3. Предоставляет Dashboard на http://localhost:8761

---

## ⚙️ Конфигурация (`application.yml`)

```yaml
server:
  port: 8761  # Стандартный порт Eureka

spring:
  application:
    name: eureka-server

eureka:
  client:
    register-with-eureka: false  # Не регистрировать себя
    fetch-registry: false        # Не загружать реестр
  server:
    enable-self-preservation: false  # Отключить self-preservation для dev
```

**Зачем `register-with-eureka: false`?**
- Eureka Server сам не является клиентом
- Он не должен регистрировать себя в реестре

---

## 🔄 Как работает

### 1. Запуск Eureka Server
```bash
cd eureka-server
mvn spring-boot:run
```

### 2. Регистрация клиента (например, Task Service)
```yaml
# task-service/application.yml
eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/  # ← URL Eureka Server
```

### 3. Task Service при запуске:
```
1. Отправляет POST /eureka/apps/TASK-SERVICE
2. Eureka регистрирует сервис
3. Task Service отправляет heartbeat каждые 30 секунд
4. Если heartbeat не приходит 90 секунд → сервис удаляется
```

### 4. API Gateway ищет Task Service:
```java
// API Gateway
@LoadBalanced  // ← Eureka Client
RestTemplate restTemplate;

// Вместо http://localhost:8083
String url = "http://task-service/api/tasks";  // ← Имя из Eureka!
```

---

## 🌐 Eureka Dashboard

**URL:** http://localhost:8761

**Что показывает:**
- Список зарегистрированных сервисов
- Статус (UP, DOWN)
- Количество инстансов
- IP-адреса и порты

**Пример:**
```
Application         AMIs        Availability Zones    Status
TASK-SERVICE        n/a (1)     (1) (1)              UP (1) - localhost:task-service:8083
SPRINT-SERVICE      n/a (1)     (1) (1)              UP (1) - localhost:sprint-service:8082
```

---

## 💡 Для защиты проекта

**Вопрос:** Зачем нужен Eureka Server?

**Ответ:**
> "Eureka Server — это Service Discovery. Он позволяет микросервисам находить друг друга по имени, а не по IP. Когда Task Service запускается, он регистрируется в Eureka. API Gateway через Eureka находит Task Service и маршрутизирует запросы. Это позволяет динамически масштабировать сервисы — можно запустить 3 инстанса Task Service на разных портах, и Eureka автоматически распределит нагрузку."

---

# 2️⃣ API GATEWAY

## 📌 Назначение
**Единая точка входа** для всех клиентских запросов. Маршрутизирует запросы к нужным микросервисам и проверяет JWT токены.

## 🎯 Зачем нужен
- Единый URL для клиента (http://localhost:8080)
- Аутентификация и авторизация (проверка JWT)
- Маршрутизация запросов к микросервисам
- CORS настройки
- Rate limiting (опционально)
- Логирование запросов

## 📂 Структура

```
api-gateway/
├── src/main/java/org/example/gateway/
│   ├── ApiGatewayApplication.java
│   ├── config/
│   │   ├── CorsConfig.java
│   │   └── SecurityConfig.java
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java
│   └── security/
│       └── JwtTokenProvider.java
└── src/main/resources/
    └── application.yml
```

---

## 📝 Классы

### 1. `ApiGatewayApplication.java`

**Назначение:** Точка входа в API Gateway

**Код:**
```java
@SpringBootApplication
@EnableDiscoveryClient  // ← Регистрируется в Eureka
public class ApiGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(ApiGatewayApplication.class, args);
    }
}
```

**Аннотации:**
- `@EnableDiscoveryClient` — регистрирует Gateway в Eureka

---

### 2. `JwtAuthenticationFilter.java`

**Назначение:** Проверяет JWT токен в каждом запросе и добавляет информацию о пользователе в заголовки

**Расположение:** `filter/JwtAuthenticationFilter.java`

**Код (упрощенно):**
```java
@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<Config> {
    
    @Autowired
    private JwtTokenProvider jwtTokenProvider;
    
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();
            
            // 1. Пропустить /api/auth/** без проверки
            if (isAuthEndpoint(request)) {
                return chain.filter(exchange);
            }
            
            // 2. Извлечь токен из заголовка Authorization
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return onError(exchange, "Missing token", HttpStatus.UNAUTHORIZED);
            }
            
            String token = authHeader.substring(7);
            
            // 3. Валидация токена
            if (!jwtTokenProvider.validateToken(token)) {
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }
            
            // 4. Извлечь данные пользователя
            Long userId = jwtTokenProvider.extractUserId(token);
            String email = jwtTokenProvider.extractEmail(token);
            String role = jwtTokenProvider.extractRole(token);
            
            // 5. Добавить в заголовки для микросервисов
            ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", String.valueOf(userId))
                .header("X-User-Email", email)
                .header("X-User-Role", role)
                .build();
            
            // 6. Передать дальше
            return chain.filter(exchange.mutate().request(modifiedRequest).build());
        };
    }
    
    private boolean isAuthEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.startsWith("/api/auth/") || 
               path.contains("/swagger-ui") || 
               path.contains("/actuator");
    }
}
```

**Что делает:**
1. **Проверяет наличие токена** в заголовке `Authorization: Bearer <token>`
2. **Валидирует токен** (подпись, срок действия)
3. **Извлекает данные** (userId, email, role)
4. **Добавляет заголовки** для микросервисов:
   - `X-User-Id: 1`
   - `X-User-Email: user@example.com`
   - `X-User-Role: DEVELOPER`
5. **Пропускает** `/api/auth/**` без проверки (для логина/регистрации)

**Зачем добавлять заголовки?**
- Микросервисы не имеют доступа к JWT токену
- Заголовки передают информацию о пользователе
- Микросервисы могут проверять права доступа

---

### 3. `JwtTokenProvider.java`

**Назначение:** Работа с JWT токенами (валидация, извлечение данных)

**Расположение:** `security/JwtTokenProvider.java`

**Код:**
```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    // Валидация токена
    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT token: {}", e.getMessage());
            return false;
        }
    }
    
    // Извлечь userId
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("userId", Long.class);
    }
    
    // Извлечь email
    public String extractEmail(String token) {
        return extractAllClaims(token).getSubject();
    }
    
    // Извлечь роль
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
            .setSigningKey(getSigningKey())
            .build()
            .parseClaimsJws(token)
            .getBody();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**Методы:**
- `validateToken()` — проверяет подпись и срок действия
- `extractUserId()` — извлекает ID пользователя из claim `userId`
- `extractEmail()` — извлекает email из `subject`
- `extractRole()` — извлекает роль из claim `role`

---

### 4. `CorsConfig.java`

**Назначение:** Настройка CORS для frontend

**Код:**
```java
@Configuration
public class CorsConfig {
    
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        config.addAllowedOrigin("http://localhost:3000");  // Frontend URL
        config.addAllowedHeader("*");
        config.addAllowedMethod("*");
        
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        
        return new CorsWebFilter(source);
    }
}
```

**Зачем:**
- Разрешает frontend (React на порту 3000) делать запросы к API Gateway
- Без CORS браузер блокирует запросы

---

### 5. `SecurityConfig.java`

**Назначение:** Настройка Spring Security для Gateway

**Код:**
```java
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
            .csrf().disable()  // Отключить CSRF (используем JWT)
            .authorizeExchange()
                .pathMatchers("/api/auth/**").permitAll()  // Разрешить без токена
                .pathMatchers("/actuator/**").permitAll()
                .anyExchange().authenticated()  // Остальное требует токен
            .and()
            .build();
    }
}
```

---

## ⚙️ Конфигурация (`application.yml`)

```yaml
server:
  port: 8080  # Единая точка входа

spring:
  application:
    name: api-gateway
  cloud:
    gateway:
      routes:
        # Auth Service
        - id: auth-service
          uri: lb://auth-service  # lb = load balanced через Eureka
          predicates:
            - Path=/api/auth/**
          filters:
            - RewritePath=/api/(?<segment>.*), /api/$\{segment}
        
        # Task Service
        - id: task-service
          uri: lb://task-service
          predicates:
            - Path=/api/tasks/**, /api/artifacts/**, /api/comments/**
          filters:
            - JwtAuthenticationFilter  # ← Проверка JWT!
        
        # Sprint Service
        - id: sprint-service
          uri: lb://sprint-service
          predicates:
            - Path=/api/sprints/**
          filters:
            - JwtAuthenticationFilter
        
        # Team Service
        - id: team-service
          uri: lb://team-service
          predicates:
            - Path=/api/teams/**, /api/users/**
          filters:
            - JwtAuthenticationFilter

jwt:
  secret: ${JWT_SECRET}  # Из .env файла

eureka:
  client:
    service-url:
      defaultZone: http://localhost:8761/eureka/
```

**Маршрутизация:**
- `GET /api/tasks/1` → `lb://task-service/api/tasks/1`
- `POST /api/auth/login` → `lb://auth-service/api/auth/login` (без JWT!)

---

## 🔄 Как работает

### Пример: Получить задачу

```
1. Frontend: GET http://localhost:8080/api/tasks/1
   Headers: Authorization: Bearer eyJhbGc...

2. API Gateway:
   - Проверяет токен (JwtAuthenticationFilter)
   - Извлекает userId=1, role=DEVELOPER
   - Добавляет заголовки: X-User-Id, X-User-Role
   - Маршрутизирует: lb://task-service/api/tasks/1

3. Eureka:
   - Находит Task Service (localhost:8083)
   - Возвращает адрес

4. API Gateway → Task Service:
   GET http://localhost:8083/api/tasks/1
   Headers:
     X-User-Id: 1
     X-User-Role: DEVELOPER

5. Task Service:
   - Получает задачу из БД
   - Возвращает JSON

6. API Gateway → Frontend:
   - Передает ответ клиенту
```

---

## 💡 Для защиты проекта

**Вопрос:** Зачем нужен API Gateway?

**Ответ:**
> "API Gateway — единая точка входа для всех запросов. Он выполняет несколько функций:
> 1. **Аутентификация** — проверяет JWT токен в каждом запросе
> 2. **Маршрутизация** — направляет запросы к нужным микросервисам через Eureka
> 3. **Обогащение запросов** — добавляет заголовки X-User-Id, X-User-Role для микросервисов
> 4. **CORS** — разрешает frontend делать запросы
> 5. **Централизованная безопасность** — микросервисы не проверяют JWT, они доверяют Gateway"

---

# 3️⃣ AUTH SERVICE

## 📌 Назначение
**Аутентификация и авторизация** — регистрация, логин, генерация JWT токенов, refresh токенов.

## 🎯 Зачем нужен
- Регистрация новых пользователей
- Логин (проверка email/password)
- Генерация JWT access и refresh токенов
- Обновление токенов (refresh)
- Хеширование паролей (BCrypt)

## 📂 Структура

```
auth-service/
├── src/main/java/org/example/auth/
│   ├── AuthServiceApplication.java
│   ├── client/
│   │   ├── TeamServiceClient.java  # Feign клиент
│   │   └── UserDto.java
│   ├── config/
│   │   └── SecurityConfig.java
│   ├── controller/
│   │   └── AuthController.java
│   ├── dto/
│   │   ├── AuthResponse.java
│   │   ├── LoginRequest.java
│   │   ├── RefreshTokenRequest.java
│   │   └── RegisterRequest.java
│   ├── security/
│   │   └── JwtTokenProvider.java
│   └── service/
│       └── AuthService.java
└── src/main/resources/
    └── application.yml
```

---

## 📝 Классы

### 1. `AuthServiceApplication.java`

**Назначение:** Точка входа

**Код:**
```java
@SpringBootApplication
@EnableDiscoveryClient
@EnableFeignClients  // ← Включает Feign клиенты
public class AuthServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }
}
```

**Аннотации:**
- `@EnableFeignClients` — включает Feign для вызова Team Service

---

### 2. `AuthController.java`

**Назначение:** REST API для аутентификации

**Расположение:** `controller/AuthController.java`

**Endpoints:**

```java
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    
    private final AuthService authService;
    
    // Регистрация
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }
    
    // Логин
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }
    
    // Обновление токена
    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }
}
```

**Endpoints:**
1. `POST /api/auth/register` — регистрация
2. `POST /api/auth/login` — логин
3. `POST /api/auth/refresh` — обновление токена

---

### 3. `AuthService.java`

**Назначение:** Бизнес-логика аутентификации

**Расположение:** `service/AuthService.java`

**Методы:**

#### 3.1 Регистрация

```java
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    
    private final TeamServiceClient teamServiceClient;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    
    public AuthResponse register(RegisterRequest request) {
        // 1. Создать UserDto
        UserDto userDto = new UserDto();
        userDto.setEmail(request.getEmail());
        userDto.setName(request.getName());
        userDto.setPassword(passwordEncoder.encode(request.getPassword()));  // ← BCrypt!
        userDto.setTeamId(request.getTeamId());
        userDto.setRole(request.getRole());
        
        // 2. Вызвать Team Service через Feign
        UserDto createdUser = teamServiceClient.createUser(userDto);
        
        // 3. Генерация JWT токенов
        String accessToken = jwtTokenProvider.generateAccessToken(
            createdUser.getId(),
            createdUser.getEmail(),
            createdUser.getRole()
        );
        String refreshToken = jwtTokenProvider.generateRefreshToken(
            createdUser.getId(),
            createdUser.getEmail()
        );
        
        // 4. Вернуть токены
        return new AuthResponse(
            accessToken,
            refreshToken,
            createdUser.getId(),
            createdUser.getEmail(),
            createdUser.getName(),
            createdUser.getRole()
        );
    }
}
```

**Что делает:**
1. Хеширует пароль с помощью BCrypt
2. Вызывает Team Service для создания пользователя
3. Генерирует access и refresh токены
4. Возвращает токены клиенту

---

#### 3.2 Логин

```java
public AuthResponse login(LoginRequest request) {
    // 1. Получить пользователя из Team Service
    UserDto user = teamServiceClient.getUserByEmail(request.getEmail());
    
    if (user == null) {
        throw new RuntimeException("User not found");
    }
    
    // 2. Проверить пароль
    if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
        throw new RuntimeException("Invalid password");
    }
    
    // 3. Генерация токенов
    String accessToken = jwtTokenProvider.generateAccessToken(
        user.getId(),
        user.getEmail(),
        user.getRole()
    );
    String refreshToken = jwtTokenProvider.generateRefreshToken(
        user.getId(),
        user.getEmail()
    );
    
    return new AuthResponse(accessToken, refreshToken, user);
}
```

**Что делает:**
1. Получает пользователя по email из Team Service
2. Сравнивает хеши паролей (BCrypt)
3. Генерирует новые токены
4. Возвращает токены

---

#### 3.3 Refresh Token

```java
public AuthResponse refreshToken(RefreshTokenRequest request) {
    String refreshToken = request.getRefreshToken();
    
    // 1. Валидация refresh токена
    if (!jwtTokenProvider.validateToken(refreshToken)) {
        throw new RuntimeException("Invalid refresh token");
    }
    
    // 2. Извлечь данные
    Long userId = jwtTokenProvider.extractUserId(refreshToken);
    String email = jwtTokenProvider.extractEmail(refreshToken);
    
    // 3. Получить актуальные данные пользователя
    UserDto user = teamServiceClient.getUserById(userId);
    
    // 4. Генерация новых токенов
    String newAccessToken = jwtTokenProvider.generateAccessToken(
        user.getId(),
        user.getEmail(),
        user.getRole()
    );
    String newRefreshToken = jwtTokenProvider.generateRefreshToken(
        user.getId(),
        user.getEmail()
    );
    
    return new AuthResponse(newAccessToken, newRefreshToken, user);
}
```

**Зачем refresh токен?**
- Access токен живет 24 часа
- Refresh токен живет 7 дней
- Когда access истекает, клиент использует refresh для получения нового access
- Не нужно каждый раз вводить пароль

---

### 4. `JwtTokenProvider.java`

**Назначение:** Генерация и валидация JWT токенов

**Код:**
```java
@Component
public class JwtTokenProvider {
    
    @Value("${jwt.secret}")
    private String jwtSecret;
    
    @Value("${jwt.expiration}")
    private long jwtExpiration;  // 86400000 мс = 24 часа
    
    @Value("${jwt.refresh-expiration}")
    private long refreshExpiration;  // 604800000 мс = 7 дней
    
    // Генерация access токена
    public String generateAccessToken(Long userId, String email, String role) {
        return Jwts.builder()
            .setSubject(email)
            .claim("userId", userId)
            .claim("role", role)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + jwtExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    // Генерация refresh токена
    public String generateRefreshToken(Long userId, String email) {
        return Jwts.builder()
            .setSubject(email)
            .claim("userId", userId)
            .setIssuedAt(new Date())
            .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
            .signWith(getSigningKey(), SignatureAlgorithm.HS256)
            .compact();
    }
    
    private Key getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(jwtSecret);
        return Keys.hmacShaKeyFor(keyBytes);
    }
}
```

**JWT Payload (access token):**
```json
{
  "sub": "user@example.com",
  "userId": 1,
  "role": "DEVELOPER",
  "iat": 1234567890,
  "exp": 1234654290
}
```

---

### 5. `TeamServiceClient.java`

**Назначение:** Feign клиент для вызова Team Service

**Код:**
```java
@FeignClient(name = "team-service")  // ← Имя из Eureka
public interface TeamServiceClient {
    
    @PostMapping("/api/users")
    UserDto createUser(@RequestBody UserDto userDto);
    
    @GetMapping("/api/users/email/{email}")
    UserDto getUserByEmail(@PathVariable String email);
    
    @GetMapping("/api/users/{id}")
    UserDto getUserById(@PathVariable Long id);
}
```

**Как работает:**
1. `@FeignClient(name = "team-service")` — Feign ищет сервис в Eureka
2. Eureka возвращает URL: `http://localhost:8081`
3. Feign делает HTTP запрос: `POST http://localhost:8081/api/users`

---

### 6. DTO классы

#### `RegisterRequest.java`
```java
@Data
public class RegisterRequest {
    private String email;
    private String name;
    private String password;
    private Long teamId;
    private String role;  // "DEVELOPER", "APPROVER", etc.
}
```

#### `LoginRequest.java`
```java
@Data
public class LoginRequest {
    private String email;
    private String password;
}
```

#### `AuthResponse.java`
```java
@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private Long userId;
    private String email;
    private String name;
    private String role;
}
```

---

## 🔄 Как работает

### Сценарий 1: Регистрация

```
1. Frontend: POST /api/auth/register
   Body: { email, name, password, teamId, role }

2. Auth Service:
   - Хеширует пароль: BCrypt.hash("password123")
   - Вызывает Team Service: POST /api/users
   - Team Service создает пользователя в БД
   - Генерирует JWT токены
   - Возвращает токены

3. Frontend:
   - Сохраняет токены в localStorage
   - Перенаправляет на главную страницу
```

### Сценарий 2: Логин

```
1. Frontend: POST /api/auth/login
   Body: { email, password }

2. Auth Service:
   - Получает пользователя из Team Service
   - Сравнивает хеши: BCrypt.compare(input, stored)
   - Генерирует новые токены
   - Возвращает токены

3. Frontend:
   - Сохраняет токены
   - Перенаправляет на главную
```

---

## 💡 Для защиты проекта

**Вопрос:** Как работает аутентификация?

**Ответ:**
> "При регистрации пользователь отправляет email, пароль и роль. Auth Service хеширует пароль с помощью BCrypt и вызывает Team Service через Feign для создания пользователя в БД. Затем генерируются два JWT токена: access (24 часа) и refresh (7 дней). При логине Auth Service получает пользователя из Team Service, сравнивает хеши паролей и генерирует новые токены. Access токен содержит userId, email и role. API Gateway проверяет токен и добавляет эти данные в заголовки для микросервисов."

---

**Продолжение в `MICROSERVICES_PART2.md`**
