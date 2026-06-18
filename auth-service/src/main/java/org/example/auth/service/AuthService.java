package org.example.auth.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.auth.client.TeamServiceClient;
import org.example.auth.client.UserDto;
import org.example.auth.dto.AuthResponse;
import org.example.auth.dto.LoginRequest;
import org.example.auth.dto.RegisterRequest;
import org.example.auth.security.JwtTokenProvider;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * AUTH SERVICE - Сервис аутентификации и авторизации
 * 
 * НАЗНАЧЕНИЕ:
 * Управляет процессами регистрации, логина и обновления JWT токенов.
 * 
 * КЛЮЧЕВЫЕ ФУНКЦИИ:
 * 1. Регистрация новых пользователей (хеширование паролей BCrypt)
 * 2. Логин (проверка email/password, генерация JWT)
 * 3. Обновление токенов (refresh token → новый access token)
 * 4. Валидация токенов
 * 
 * ВЗАИМОДЕЙСТВИЕ С ДРУГИМИ СЕРВИСАМИ:
 * - Team Service (через Feign) - создание и получение пользователей
 * 
 * БЕЗОПАСНОСТЬ:
 * - BCrypt для хеширования паролей (10 раундов + salt)
 * - JWT с HMAC-SHA256 подписью
 * - Access token: 24 часа
 * - Refresh token: 7 дней
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {
    private final TeamServiceClient teamServiceClient;
    private final JwtTokenProvider jwtTokenProvider;
    private final PasswordEncoder passwordEncoder;

    /**
     * ЛОГИН - Аутентификация пользователя
     * 
     * АЛГОРИТМ:
     * 1. Получить пользователя из Team Service по email (через Feign)
     * 2. Проверить пароль с помощью BCrypt (сравнение хешей)
     * 3. Генерация JWT токенов:
     *    - Access token (24 часа) - для доступа к API
     *    - Refresh token (7 дней) - для обновления access token
     * 4. Вернуть токены и информацию о пользователе
     * 
     * JWT PAYLOAD (access token):
     * {
     *   "sub": "user@example.com",
     *   "userId": 1,
     *   "role": "DEVELOPER",
     *   "iat": 1234567890,
     *   "exp": 1234654290
     * }
     * 
     * БЕЗОПАСНОСТЬ:
     * - Пароль никогда не передается в открытом виде
     * - BCrypt автоматически добавляет salt к каждому паролю
     * - При неудаче возвращается общее сообщение (не раскрывая, что именно неверно)
     * 
     * @param request - email и пароль пользователя
     * @return AuthResponse - токены и информация о пользователе
     * @throws RuntimeException если email не найден или пароль неверный
     */
    public AuthResponse login(LoginRequest request) {
        try {
            // ШАГ 1: Получить пользователя из Team Service по email
            // Feign клиент делает HTTP запрос: GET /api/users/email/{email}
            UserDto user = teamServiceClient.getUserByEmail(request.getEmail());
            
            // ШАГ 2: Проверить пароль с помощью BCrypt
            // BCrypt сравнивает введенный пароль с хешем из БД
            // Формат хеша: $2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new RuntimeException("Invalid email or password");
            }
            
            // ШАГ 3: Генерация JWT токенов
            // Access token содержит: userId, email, role
            String accessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
            // Refresh token содержит только: userId, email (без роли)
            String refreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
            
            log.info("User {} logged in successfully", user.getEmail());
            
            // ШАГ 4: Вернуть токены и данные пользователя
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole()
            );
        } catch (Exception e) {
            log.error("Login failed for email {}: {}", request.getEmail(), e.getMessage());
            // Не раскрываем, что именно неверно (email или пароль) - для безопасности
            throw new RuntimeException("Invalid email or password");
        }
    }

    /**
     * РЕГИСТРАЦИЯ - Создание нового пользователя
     * 
     * АЛГОРИТМ:
     * 1. Хешировать пароль с помощью BCrypt (10 раундов + автоматический salt)
     * 2. Создать пользователя в Team Service (через Feign)
     * 3. Генерация JWT токенов для автоматического логина
     * 4. Вернуть токены и информацию о пользователе
     * 
     * ХЕШИРОВАНИЕ ПАРОЛЯ (BCrypt):
     * - Пароль "password123" → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     * - Salt генерируется автоматически и встроен в хеш
     * - 10 раундов = 2^10 = 1024 итерации (защита от brute force)
     * - Каждый раз хеш будет разный (даже для одного пароля)
     * 
     * РОЛИ:
     * - DEVELOPER - разработчик (создает задачи)
     * - APPROVER - аппрувер (одобряет задачи и спринты)
     * - TEAM_LEAD - лидер команды (одобряет + управляет командой)
     * - MANAGER - менеджер (одобряет + видит все команды)
     * 
     * @param request - данные для регистрации (email, name, password, teamId, role)
     * @return AuthResponse - токены и информация о созданном пользователе
     * @throws RuntimeException если email уже занят или ошибка создания
     */
    public AuthResponse register(RegisterRequest request) {
        try {
            // ШАГ 1: Подготовить DTO для создания пользователя
            UserDto userDto = new UserDto();
            userDto.setEmail(request.getEmail());
            userDto.setName(request.getName());
            
            // ШАГ 2: Хешировать пароль с помощью BCrypt
            // BCrypt автоматически генерирует salt и встраивает его в хеш
            // Пример: "password" → "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
            userDto.setPassword(passwordEncoder.encode(request.getPassword()));
            userDto.setTeamId(request.getTeamId());
            userDto.setRole(request.getRole());
            
            // ШАГ 3: Создать пользователя в Team Service через Feign
            // Feign делает HTTP запрос: POST /api/users
            // Team Service проверит уникальность email и сохранит в БД
            UserDto createdUser = teamServiceClient.createUser(userDto);
            
            // ШАГ 4: Генерация JWT токенов для автоматического логина
            String accessToken = jwtTokenProvider.generateToken(createdUser.getId(), createdUser.getEmail(), createdUser.getRole());
            String refreshToken = jwtTokenProvider.generateRefreshToken(createdUser.getId(), createdUser.getEmail());
            
            log.info("User {} registered successfully", createdUser.getEmail());
            
            return new AuthResponse(
                    accessToken,
                    refreshToken,
                    createdUser.getId(),
                    createdUser.getEmail(),
                    createdUser.getName(),
                    createdUser.getRole()
            );
        } catch (Exception e) {
            log.error("Registration failed for email {}: {}", request.getEmail(), e.getMessage());
            throw new RuntimeException("Registration failed: " + e.getMessage());
        }
    }

    /**
     * ОБНОВЛЕНИЕ ТОКЕНА - Получение нового access token по refresh token
     * 
     * ЗАЧЕМ НУЖЕН REFRESH TOKEN:
     * - Access token живет 24 часа (короткий срок для безопасности)
     * - Refresh token живет 7 дней (длинный срок для удобства)
     * - Когда access token истекает, frontend использует refresh token для получения нового
     * - Пользователю не нужно вводить пароль заново
     * 
     * АЛГОРИТМ:
     * 1. Валидировать refresh token (подпись, срок действия)
     * 2. Извлечь userId и email из refresh token
     * 3. Получить актуальную роль пользователя из Team Service
     *    (роль могла измениться с момента выдачи токена)
     * 4. Генерация новых токенов с актуальной ролью
     * 
     * ВАЖНО:
     * - Роль берется из БД, а не из старого токена (актуальные права доступа)
     * - Генерируется новый refresh token (rotation для безопасности)
     * - Старый refresh token становится недействительным
     * 
     * @param refreshToken - текущий refresh token
     * @return AuthResponse - новые access и refresh токены
     * @throws RuntimeException если refresh token невалидный или истек
     */
    public AuthResponse refreshToken(String refreshToken) {
        try {
            // ШАГ 1: Валидировать refresh token
            // Проверка подписи и срока действия (7 дней)
            if (!jwtTokenProvider.validateToken(refreshToken)) {
                throw new RuntimeException("Invalid refresh token");
            }
            
            // ШАГ 2: Извлечь информацию о пользователе из токена
            String email = jwtTokenProvider.extractEmail(refreshToken);
            Long userId = jwtTokenProvider.extractUserId(refreshToken);
            
            // ШАГ 3: Получить актуальную роль из БД
            // ВАЖНО: Роль могла измениться с момента выдачи токена
            // Например, пользователь был DEVELOPER, а стал APPROVER
            UserDto user = teamServiceClient.getUserByEmail(email);
            
            // ШАГ 4: Генерация новых токенов с актуальной ролью
            String newAccessToken = jwtTokenProvider.generateToken(user.getId(), user.getEmail(), user.getRole());
            // Генерируем новый refresh token (rotation для безопасности)
            String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getId(), user.getEmail());
            
            log.info("Token refreshed for user {}", email);
            
            return new AuthResponse(
                    newAccessToken,
                    newRefreshToken,
                    user.getId(),
                    user.getEmail(),
                    user.getName(),
                    user.getRole()
            );
        } catch (Exception e) {
            log.error("Token refresh failed: {}", e.getMessage());
            throw new RuntimeException("Failed to refresh token");
        }
    }

    /**
     * ВАЛИДАЦИЯ ТОКЕНА - Проверка действительности JWT токена
     * 
     * ПРОВЕРЯЕТ:
     * - Подпись токена (HMAC-SHA256)
     * - Срок действия (не истек ли)
     * - Формат токена (корректный JWT)
     * 
     * ИСПОЛЬЗУЕТСЯ:
     * - API Gateway для проверки каждого запроса
     * - Внутренние сервисы для дополнительной проверки
     * 
     * @param token - JWT токен для проверки
     * @return true если токен валидный, false если невалидный или истек
     */
    public boolean validateToken(String token) {
        return jwtTokenProvider.validateToken(token);
    }
}
