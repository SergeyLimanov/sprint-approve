package org.example.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

/**
 * SECURITY КОНФИГУРАЦИЯ AUTH SERVICE - Публичные эндпоинты и BCrypt
 * 
 * НАЗНАЧЕНИЕ:
 * Auth Service - это сервис аутентификации, поэтому его эндпоинты должны быть доступны без JWT.
 * Пользователь не может получить JWT, не залогинившись сначала.
 * 
 * ПУБЛИЧНЫЕ ЭНДПОИНТЫ:
 * - /api/auth/login - вход (получение JWT)
 * - /api/auth/register - регистрация
 * - /api/auth/refresh - обновление токена
 * - /api/auth/validate - проверка токена
 * 
 * STATELESS SESSION:
 * - sessionCreationPolicy = STATELESS
 * - Не используем HTTP сессии (cookies)
 * - Вся информация в JWT токене
 * 
 * CSRF ОТКЛЮЧЕН:
 * - JWT в Authorization заголовке, не в cookies
 * - CSRF защита не нужна
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    /**
     * Настроить Security Filter Chain для Auth Service
     * 
     * КОНФИГУРАЦИЯ:
     * 1. CSRF отключен (JWT, не cookies)
     * 2. STATELESS сессии (не храним состояние на сервере)
     * 3. /api/auth/** - публичные (без JWT)
     * 4. /swagger-ui/** - публичные (документация API)
     * 5. Остальные - требуют аутентификации
     * 
     * ПОЧЕМУ /api/auth/** ПУБЛИЧНЫЕ:
     * Пользователь еще не имеет JWT токена, поэтому не может аутентифицироваться.
     * Чтобы получить токен, нужно сначала залогиниться.
     * 
     * @param http - HttpSecurity для настройки
     * @return SecurityFilterChain
     * @throws Exception при ошибке конфигурации
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                        .anyRequest().authenticated()
                );

        return http.build();
    }

    /**
     * Создать BCrypt Password Encoder для хеширования паролей
     * 
     * BCRYPT АЛГОРИТМ:
     * - Автоматически добавляет salt (случайную строку)
     * - 10 раундов хеширования (по умолчанию)
     * - Медленный алгоритм (защита от brute-force)
     * - Один и тот же пароль → разные хеши (благодаря salt)
     * 
     * ПРИМЕР:
     * password: "mypassword123"
     * hash: "$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
     *        ^^^ ^^^ ^^^^^^^^^^^^^^^^^^^^^^ ^^^^^^^^^^^^^^^^^^^^^^^^^^^^^^
     *        |   |   salt (22 символа)     hash (31 символ)
     *        |   rounds (10)
     *        алгоритм (2a = BCrypt)
     * 
     * ИСПОЛЬЗОВАНИЕ:
     * - AuthService.register() - хеширует пароль перед сохранением
     * - AuthService.login() - проверяет пароль
     * 
     * @return BCryptPasswordEncoder
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
