package org.example.task.config;

import feign.Client;
import feign.hc5.ApacheHttp5Client;
import org.springframework.cloud.client.loadbalancer.LoadBalancerClient;
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory;
import org.springframework.cloud.openfeign.loadbalancer.FeignBlockingLoadBalancerClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * FEIGN КОНФИГУРАЦИЯ - Настройка HTTP клиента для межсервисного взаимодействия
 * 
 * НАЗНАЧЕНИЕ:
 * Task Service взаимодействует с другими микросервисами через Feign:
 * - UserServiceClient → Team Service (получение данных пользователей)
 * - SprintServiceClient → Sprint Service (синхронизация статусов спринтов)
 * - NotificationServiceClient → Notification Service (отправка уведомлений)
 * 
 * ПОЧЕМУ ApacheHttp5Client:
 * - Более производительный, чем стандартный HttpURLConnection
 * - Поддержка connection pooling (повторное использование соединений)
 * - Лучшая обработка таймаутов
 * - Поддержка HTTP/2
 * 
 * ПОЧЕМУ LoadBalancer:
 * - Автоматическое обнаружение сервисов через Eureka
 * - Балансировка нагрузки между экземплярами сервиса
 * - Retry при сбоях
 */
@Configuration
public class FeignConfig {

    /**
     * Создать Feign клиент с Apache HTTP Client 5 и Load Balancer
     * 
     * АРХИТЕКТУРА:
     * ApacheHttp5Client (HTTP клиент) → LoadBalancer (Eureka) → Микросервис
     * 
     * ПРИМЕР ИСПОЛЬЗОВАНИЯ:
     * @FeignClient(name = "team-service")
     * interface UserServiceClient {
     *     @GetMapping("/api/users/{id}")
     *     UserDto getUserById(@PathVariable Long id);
     * }
     * 
     * Feign автоматически:
     * 1. Найдет team-service в Eureka
     * 2. Выберет экземпляр (если их несколько)
     * 3. Сделает HTTP GET запрос
     * 4. Десериализует JSON в UserDto
     * 
     * @param loadBalancerClient - клиент для балансировки
     * @param loadBalancerClientFactory - фабрика для создания клиентов
     * @return Feign Client с LoadBalancer
     */
    @Bean
    public Client feignClient(LoadBalancerClient loadBalancerClient,
                              LoadBalancerClientFactory loadBalancerClientFactory) {
        Client delegate = new ApacheHttp5Client();
        return new FeignBlockingLoadBalancerClient(delegate, loadBalancerClient, loadBalancerClientFactory);
    }
}
