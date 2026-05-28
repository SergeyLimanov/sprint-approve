package org.example.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task.client.dto.UserResponse;
import org.example.task.client.UserServiceClient;
import org.example.task.dto.ArtifactDto;
import org.example.task.entity.Artifact;
import org.example.task.entity.Task;
import org.example.task.mapper.ArtifactMapper;
import org.example.task.repository.ArtifactRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactService implements IArtifactService {
    private final ArtifactRepository artifactRepository;
    private final ITaskService taskService;
    private final UserServiceClient userServiceClient;
    private final FileStorageService fileStorageService;
    private final MinioStorageService minioStorageService;

    @Override
    @Transactional(readOnly = true)
    public List<ArtifactDto> getArtifactsByTaskId(Long taskId, int urlExpiryMinutes) {
        List<ArtifactDto> artifacts = artifactRepository.findByTaskId(taskId).stream()
                .map(this::enrichWithUserName)
                .collect(Collectors.toList());
        
        // Добавляем временные ссылки для скачивания
        artifacts.forEach(artifact -> {
            if (artifact.getUrl() != null && !artifact.getUrl().startsWith("http")) {
                // Это файл в MinIO, генерируем presigned URL
                String presignedUrl = minioStorageService.getPresignedUrl(
                    artifact.getUrl(),
                    urlExpiryMinutes
                );
                artifact.setDownloadUrl(presignedUrl);
            } else {
                // Это внешняя ссылка, используем как есть
                artifact.setDownloadUrl(artifact.getUrl());
            }
        });
        
        return artifacts;
    }

    @Override
    @Transactional(readOnly = true)
    public ArtifactDto getArtifactById(Long id, int urlExpiryMinutes) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artifact not found with id: " + id));
        ArtifactDto dto = enrichWithUserName(artifact);
        
        // Генерируем временную ссылку
        if (dto.getUrl() != null && !dto.getUrl().startsWith("http")) {
            String presignedUrl = minioStorageService.getPresignedUrl(
                dto.getUrl(),
                urlExpiryMinutes
            );
            dto.setDownloadUrl(presignedUrl);
        } else {
            dto.setDownloadUrl(dto.getUrl());
        }
        
        return dto;
    }

    @Override
    @Transactional(readOnly = true)
    public Artifact getArtifactEntityById(Long id) {
        return artifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artifact not found with id: " + id));
    }

    @Override
    @Transactional
    public ArtifactDto createArtifact(ArtifactDto artifactDto) {
        Task task = taskService.getTaskEntityById(artifactDto.getTaskId());

        Artifact artifact = new Artifact();
        artifact.setName(artifactDto.getName());
        artifact.setUrl(artifactDto.getUrl());
        artifact.setFileType(artifactDto.getFileType());
        artifact.setFileSize(artifactDto.getFileSize());
        artifact.setTask(task);
        artifact.setUploadedBy(artifactDto.getUploadedBy());

        Artifact savedArtifact = artifactRepository.save(artifact);
        return enrichWithUserName(savedArtifact);
    }

    @Override
    @Transactional
    public ArtifactDto uploadFile(MultipartFile file, Long taskId, Long uploadedBy, String customName) {
        // Загружаем файл в MinIO
        String fileName = minioStorageService.storeFile(file);

        ArtifactDto artifactDto = new ArtifactDto();
        artifactDto.setName(customName != null && !customName.trim().isEmpty() 
                ? customName 
                : file.getOriginalFilename());
        artifactDto.setUrl(fileName);  // Сохраняем только имя файла в MinIO
        artifactDto.setTaskId(taskId);
        artifactDto.setUploadedBy(uploadedBy);
        artifactDto.setFileType(file.getContentType());
        artifactDto.setFileSize(file.getSize());

        ArtifactDto created = createArtifact(artifactDto);
        
        // Генерируем временную ссылку для ответа
        String presignedUrl = minioStorageService.getPresignedUrl(fileName, 60);
        created.setDownloadUrl(presignedUrl);
        
        return created;
    }

    @Override
    @Transactional(readOnly = true)
    public String getDownloadUrl(Long id, int expiryMinutes) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artifact not found with id: " + id));
        
        if (artifact.getUrl() == null || artifact.getUrl().startsWith("http")) {
            // Внешняя ссылка
            return artifact.getUrl() != null ? artifact.getUrl() : "";
        }
        
        // Генерируем presigned URL для файла в MinIO
        return minioStorageService.getPresignedUrl(artifact.getUrl(), expiryMinutes);
    }

    @Override
    @Transactional(readOnly = true)
    public Resource downloadFile(String fileName) {
        return minioStorageService.loadFileAsResource(fileName);
    }

    @Override
    @Transactional
    public void deleteArtifact(Long id) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artifact not found with id: " + id));
        
        // Delete physical file if it's a local file
        if (artifact.getUrl() != null && artifact.getUrl().contains("/api/artifacts/files/")) {
            String fileName = artifact.getUrl().substring(artifact.getUrl().lastIndexOf("/") + 1);
            fileStorageService.deleteFile(fileName);
        }
        
        artifactRepository.deleteById(id);
    }

    private ArtifactDto enrichWithUserName(Artifact artifact) {
        ArtifactDto dto = ArtifactMapper.toDto(artifact);

        if (artifact.getUploadedBy() != null) {
            try {
                UserResponse user = userServiceClient.getUserById(artifact.getUploadedBy());
                dto.setUploadedByName(user.getName());
            } catch (Exception e) {
                log.warn("Could not fetch uploader name for user id {}: {}", artifact.getUploadedBy(), e.getMessage());
            }
        }

        return dto;
    }
}
