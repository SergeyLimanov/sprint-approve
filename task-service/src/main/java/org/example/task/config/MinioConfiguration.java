package org.example.task.config;

import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * MINIO КОНФИГУРАЦИЯ - Подключение к объектному хранилищу
 * 
 * НАЗНАЧЕНИЕ:
 * MinIO - это S3-совместимое объектное хранилище для файлов.
 * Используется для хранения артефактов задач (screenshots, documents, и т.д.).
 * 
 * ПОЧЕМУ MINIO, А НЕ ФАЙЛОВАЯ СИСТЕМА:
 * - Масштабируемость (можно добавить ноды)
 * - Presigned URLs (безопасный доступ без проксирования через backend)
 * - S3-совместимость (легко мигрировать на AWS S3)
 * - Версионирование файлов
 * - Метаданные и теги
 * 
 * ПАРАМЕТРЫ (из MinioProperties):
 * - endpoint: http://localhost:9000 (MinIO server URL)
 * - accessKey: minioadmin (логин)
 * - secretKey: minioadmin (пароль)
 * 
 * ВАЖНО ДЛЯ PRODUCTION:
 * - Изменить креденшелы (minioadmin → сложные пароли)
 * - Использовать HTTPS
 * - Настроить bucket policies
 */
@Configuration
@RequiredArgsConstructor
public class MinioConfiguration {
    private final MinioProperties minioProperties;

    /**
     * Создать MinIO клиент для работы с файлами
     * 
     * КЛИЕНТ ИСПОЛЬЗУЕТСЯ В:
     * - FileStorageService - загрузка/удаление файлов
     * - ArtifactService - управление артефактами задач
     * 
     * ОПЕРАЦИИ:
     * - putObject() - загрузить файл
     * - getPresignedObjectUrl() - получить временную ссылку
     * - removeObject() - удалить файл
     * - listObjects() - список файлов
     * 
     * @return MinioClient для работы с хранилищем
     */
    @Bean
    public MinioClient minioClient() {
        return MinioClient.builder()
                .endpoint(minioProperties.getEndpoint())
                .credentials(minioProperties.getAccessKey(), minioProperties.getSecretKey())
                .build();
    }
}
