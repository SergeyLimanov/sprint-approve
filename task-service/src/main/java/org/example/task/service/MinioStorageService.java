package org.example.task.service;

import io.minio.*;
import io.minio.http.Method;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task.config.MinioConfig;
import org.springframework.core.io.InputStreamResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioStorageService {
    private final MinioClient minioClient;
    private final MinioConfig minioConfig;

    @PostConstruct
    public void init() {
        try {
            // Проверяем существование bucket
            boolean found = minioClient.bucketExists(
                BucketExistsArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .build()
            );
            
            if (!found) {
                // Создаём bucket если не существует
                minioClient.makeBucket(
                    MakeBucketArgs.builder()
                        .bucket(minioConfig.getBucket())
                        .build()
                );
                log.info("MinIO bucket created: {}", minioConfig.getBucket());
            } else {
                log.info("MinIO bucket already exists: {}", minioConfig.getBucket());
            }
        } catch (Exception e) {
            throw new RuntimeException("Could not initialize MinIO bucket", e);
        }
    }

    /**
     * Сохраняет файл в MinIO
     * @param file загружаемый файл
     * @return уникальное имя файла в MinIO
     */
    public String storeFile(MultipartFile file) {
        String originalFileName = StringUtils.cleanPath(file.getOriginalFilename());
        
        try {
            // Защита от path traversal атак
            if (originalFileName.contains("..")) {
                throw new RuntimeException("Invalid file path: " + originalFileName);
            }

            // Генерируем уникальное имя файла
            String fileExtension = "";
            int dotIndex = originalFileName.lastIndexOf('.');
            if (dotIndex > 0) {
                fileExtension = originalFileName.substring(dotIndex);
            }
            String fileName = UUID.randomUUID().toString() + fileExtension;

            // Загружаем файл в MinIO
            minioClient.putObject(
                PutObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(fileName)
                    .stream(file.getInputStream(), file.getSize(), -1)
                    .contentType(file.getContentType())
                    .build()
            );

            log.info("File uploaded to MinIO: {}", fileName);
            return fileName;
            
        } catch (Exception e) {
            throw new RuntimeException("Could not store file " + originalFileName, e);
        }
    }

    /**
     * Генерирует временную ссылку на файл (presigned URL)
     * @param fileName имя файла в MinIO
     * @param expiryMinutes время жизни ссылки в минутах
     * @return временный URL для скачивания
     */
    public String getPresignedUrl(String fileName, int expiryMinutes) {
        try {
            String url = minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.GET)
                    .bucket(minioConfig.getBucket())
                    .object(fileName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build()
            );
            
            log.debug("Generated presigned URL for {}, expires in {} minutes", fileName, expiryMinutes);
            return url;
            
        } catch (Exception e) {
            throw new RuntimeException("Could not generate presigned URL for " + fileName, e);
        }
    }

    /**
     * Генерирует временную ссылку на 1 час (по умолчанию)
     */
    public String getPresignedUrl(String fileName) {
        return getPresignedUrl(fileName, 60);
    }

    /**
     * Генерирует временную ссылку для загрузки файла (PUT)
     * @param fileName имя файла
     * @param expiryMinutes время жизни ссылки
     * @return временный URL для загрузки
     */
    public String getPresignedUploadUrl(String fileName, int expiryMinutes) {
        try {
            return minioClient.getPresignedObjectUrl(
                GetPresignedObjectUrlArgs.builder()
                    .method(Method.PUT)
                    .bucket(minioConfig.getBucket())
                    .object(fileName)
                    .expiry(expiryMinutes, TimeUnit.MINUTES)
                    .build()
            );
        } catch (Exception e) {
            throw new RuntimeException("Could not generate upload URL", e);
        }
    }

    /**
     * Загружает файл как Resource (для прямой отдачи через контроллер)
     */
    public Resource loadFileAsResource(String fileName) {
        try {
            InputStream stream = minioClient.getObject(
                GetObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(fileName)
                    .build()
            );
            
            return new InputStreamResource(stream);
            
        } catch (Exception e) {
            throw new RuntimeException("File not found: " + fileName, e);
        }
    }

    /**
     * Удаляет файл из MinIO
     */
    public void deleteFile(String fileName) {
        try {
            minioClient.removeObject(
                RemoveObjectArgs.builder()
                    .bucket(minioConfig.getBucket())
                    .object(fileName)
                    .build()
            );
            log.info("File deleted from MinIO: {}", fileName);
        } catch (Exception e) {
            log.error("Could not delete file: {}", fileName, e);
        }
    }

    /**
     * Получает публичный URL файла (если bucket публичный)
     */
    public String getFileUrl(String fileName) {
        return String.format("%s/%s/%s", 
            minioConfig.getEndpoint(), 
            minioConfig.getBucket(), 
            fileName);
    }
}
