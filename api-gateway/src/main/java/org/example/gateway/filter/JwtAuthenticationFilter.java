package org.example.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.example.gateway.security.JwtTokenProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * JWT AUTHENTICATION FILTER - Ключевой компонент безопасности API Gateway
 * 
 * НАЗНАЧЕНИЕ:
 * Проверяет JWT токен в каждом запросе и добавляет информацию о пользователе в заголовки.
 * 
 * АЛГОРИТМ РАБОТЫ:
 * 1. Пропустить /api/auth/** без проверки (публичные эндпоинты)
 * 2. Извлечь токен из заголовка Authorization: Bearer <token>
 * 3. Валидировать токен (подпись, срок действия)
 * 4. Извлечь данные: userId, email, role
 * 5. Добавить заголовки для микросервисов:
 *    - X-User-Id: 1
 *    - X-User-Email: user@example.com
 *    - X-User-Role: DEVELOPER
 * 6. Передать запрос дальше в микросервис
 * 
 * ЗАЧЕМ ДОБАВЛЯТЬ ЗАГОЛОВКИ:
 * - Микросервисы не имеют доступа к JWT токену
 * - Заголовки передают информацию о пользователе
 * - Микросервисы могут проверять права доступа по роли
 * 
 * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
 * Frontend → GET /api/tasks/1 + Authorization: Bearer eyJhbGc...
 * Gateway → Проверка токена → Добавление заголовков
 * Gateway → Task Service: GET /api/tasks/1 + X-User-Id: 1 + X-User-Role: DEVELOPER
 */
@Component
@Slf4j
public class JwtAuthenticationFilter extends AbstractGatewayFilterFactory<JwtAuthenticationFilter.Config> {

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    public JwtAuthenticationFilter() {
        super(Config.class);
    }

    /**
     * Основной метод фильтра - вызывается для каждого HTTP запроса
     * 
     * @param config - конфигурация фильтра (не используется)
     * @return GatewayFilter - функция, которая обрабатывает запрос
     */
    @Override
    public GatewayFilter apply(Config config) {
        return (exchange, chain) -> {
            ServerHttpRequest request = exchange.getRequest();

            // ШАГ 1: Пропустить публичные эндпоинты без проверки JWT
            // /api/auth/** - регистрация и логин не требуют токена
            if (isAuthEndpoint(request)) {
                return chain.filter(exchange);
            }

            // ШАГ 2: Извлечь токен из заголовка Authorization
            // Формат: Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
            String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                log.warn("Missing or invalid Authorization header");
                return onError(exchange, "Missing or invalid Authorization header", HttpStatus.UNAUTHORIZED);
            }

            // Убрать "Bearer " и получить чистый токен
            String token = authHeader.substring(7);

            // ШАГ 3: Валидировать токен (проверка подписи и срока действия)
            if (!jwtTokenProvider.validateToken(token)) {
                log.warn("Invalid JWT token");
                return onError(exchange, "Invalid or expired token", HttpStatus.UNAUTHORIZED);
            }

            // ШАГ 4: Извлечь информацию о пользователе из токена
            try {
                Long userId = jwtTokenProvider.extractUserId(token);      // Из claim "userId"
                String email = jwtTokenProvider.extractEmail(token);      // Из subject
                String role = jwtTokenProvider.extractRole(token);        // Из claim "role"

                // ШАГ 5: Добавить заголовки для микросервисов
                // Эти заголовки будут доступны в Team Service, Sprint Service и т.д.
                // Микросервисы используют их для проверки прав доступа
                ServerHttpRequest modifiedRequest = request.mutate()
                        .header("X-User-Id", String.valueOf(userId))     // ID пользователя
                        .header("X-User-Email", email)                   // Email пользователя
                        .header("X-User-Role", role)                     // Роль: DEVELOPER, APPROVER, etc.
                        .build();

                ServerWebExchange modifiedExchange = exchange.mutate()
                        .request(modifiedRequest)
                        .build();

                log.debug("Authenticated user: {} ({})", email, role);
                
                // ШАГ 6: Передать запрос дальше в микросервис
                return chain.filter(modifiedExchange);
            } catch (Exception e) {
                log.error("Error extracting user info from token: {}", e.getMessage());
                return onError(exchange, "Invalid token", HttpStatus.UNAUTHORIZED);
            }
        };
    }

    /**
     * Проверяет, является ли эндпоинт публичным (не требует JWT)
     * 
     * ПУБЛИЧНЫЕ ЭНДПОИНТЫ:
     * - /api/auth/** - регистрация, логин, refresh токена
     * - /swagger-ui/** - документация API
     * - /v3/api-docs/** - OpenAPI спецификация
     * - /actuator/** - мониторинг (health check)
     * 
     * @param request - HTTP запрос
     * @return true если эндпоинт публичный, false если требуется JWT
     */
    private boolean isAuthEndpoint(ServerHttpRequest request) {
        String path = request.getURI().getPath();
        return path.startsWith("/api/auth/") || 
               path.contains("/swagger-ui") || 
               path.contains("/v3/api-docs") ||
               path.contains("/actuator");
    }

    /**
     * Возвращает ошибку клиенту при неудачной аутентификации
     * 
     * @param exchange - текущий HTTP обмен
     * @param message - сообщение об ошибке (не отправляется клиенту для безопасности)
     * @param status - HTTP статус (обычно 401 UNAUTHORIZED)
     * @return Mono<Void> - реактивный ответ
     */
    private Mono<Void> onError(ServerWebExchange exchange, String message, HttpStatus status) {
        exchange.getResponse().setStatusCode(status);
        return exchange.getResponse().setComplete();
    }

    /**
     * Конфигурация фильтра (пока не используется, но может быть расширена)
     */
    public static class Config {
        // Configuration properties if needed
    }
}
