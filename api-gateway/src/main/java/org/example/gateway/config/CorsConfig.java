package org.example.gateway.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.reactive.CorsWebFilter;
import org.springframework.web.cors.reactive.UrlBasedCorsConfigurationSource;

import java.util.Arrays;

/**
 * CORS КОНФИГУРАЦИЯ - Разрешение кросс-доменных запросов от frontend
 * 
 * НАЗНАЧЕНИЕ:
 * Позволяет React frontend (http://localhost:3000) делать запросы к API Gateway.
 * Без CORS браузер блокирует запросы с другого домена (Same-Origin Policy).
 * 
 * ЧТО РАЗРЕШЕНО:
 * - Origin: http://localhost:3000 (React dev server)
 * - Methods: GET, POST, PUT, DELETE, PATCH, OPTIONS
 * - Headers: все (*)
 * - Credentials: да (cookies, Authorization headers)
 * 
 * ВАЖНО ДЛЯ PRODUCTION:
 * - Заменить localhost:3000 на реальный домен frontend
 * - Ограничить allowedHeaders конкретными заголовками
 * - Настроить exposedHeaders если нужно
 */
@Configuration
public class CorsConfig {

    /**
     * Создать CORS фильтр для Spring Cloud Gateway
     * 
     * АЛГОРИТМ:
     * 1. Настроить разрешенные origins (откуда можно делать запросы)
     * 2. Настроить разрешенные HTTP методы
     * 3. Настроить разрешенные заголовки
     * 4. Включить credentials (для JWT токенов в Authorization)
     * 5. Установить время кеширования preflight запросов (OPTIONS)
     * 6. Применить конфигурацию ко всем путям (/**)
     * 
     * PREFLIGHT REQUEST:
     * Браузер сначала отправляет OPTIONS запрос для проверки CORS.
     * MaxAge=3600 означает, что браузер кеширует результат на 1 час.
     * 
     * @return CorsWebFilter для Gateway
     */
    @Bean
    public CorsWebFilter corsWebFilter() {
        CorsConfiguration corsConfig = new CorsConfiguration();
        
        // Разрешаем запросы с frontend
        corsConfig.setAllowedOrigins(Arrays.asList("http://localhost:3000"));
        
        // Разрешаем все методы
        corsConfig.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        
        // Разрешаем все заголовки
        corsConfig.setAllowedHeaders(Arrays.asList("*"));
        
        // Разрешаем credentials (cookies, authorization headers)
        corsConfig.setAllowCredentials(true);
        
        // Максимальное время кеширования preflight запроса
        corsConfig.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", corsConfig);

        return new CorsWebFilter(source);
    }
}
