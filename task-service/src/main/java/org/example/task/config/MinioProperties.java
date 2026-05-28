package org.example.task.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "minio")
@Data
public class MinioProperties {
    private String endpoint = "http://localhost:9000";
    private String accessKey = "admin";
    private String secretKey = "password123";
    private String bucket = "task-files";
}
