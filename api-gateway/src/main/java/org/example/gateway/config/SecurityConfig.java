package org.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

/**
 * SECURITY КОНФИГУРАЦИЯ API GATEWAY - Отключение встроенной аутентификации Spring Security
 * 
 * НАЗНАЧЕНИЕ:
 * API Gateway НЕ использует стандартную аутентификацию Spring Security.
 * Вместо этого используется кастомный JwtAuthenticationFilter.
 * 
 * ПОЧЕМУ CSRF ОТКЛЮЧЕН:
 * - CSRF защита нужна для session-based аутентификации (cookies)
 * - Мы используем JWT в Authorization заголовке (stateless)
 * - JWT не подвержен CSRF атакам
 * 
 * ПОЧЕМУ permitAll():
 * - Проверка JWT выполняется в JwtAuthenticationFilter
 * - Spring Security не должен блокировать запросы
 * - Фильтр сам решает, пропускать или отклонять
 * 
 * АРХИТЕКТУРА БЕЗОПАСНОСТИ:
 * Spring Security (disabled) → JwtAuthenticationFilter → Микросервисы
 */
@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    /**
     * Настроить Security Filter Chain для Gateway
     * 
     * КОНФИГУРАЦИЯ:
     * 1. CSRF отключен (используем JWT, не cookies)
     * 2. Все запросы permitAll (проверка в JwtAuthenticationFilter)
     * 
     * ВАЖНО:
     * Это НЕ означает, что все эндпоинты открыты!
     * JwtAuthenticationFilter проверяет JWT и отклоняет неавторизованные запросы.
     * 
     * @param http - ServerHttpSecurity для настройки
     * @return SecurityWebFilterChain
     */
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .csrf(csrf -> csrf.disable())
                .authorizeExchange(exchange -> exchange
                        .anyExchange().permitAll()
                )
                .build();
    }
}
