package org.example.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task.client.UserDto;
import org.example.task.client.UserServiceClient;
import org.example.task.dto.ArtifactDto;
import org.example.task.entity.Artifact;
import org.example.task.entity.Task;
import org.example.task.repository.ArtifactRepository;
import org.example.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ArtifactService {
    private final ArtifactRepository artifactRepository;
    private final TaskRepository taskRepository;
    private final UserServiceClient userServiceClient;
    private final FileStorageService fileStorageService;

    @Transactional(readOnly = true)
    public List<ArtifactDto> getArtifactsByTaskId(Long taskId) {
        return artifactRepository.findByTaskId(taskId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ArtifactDto getArtifactById(Long id) {
        Artifact artifact = artifactRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Artifact not found with id: " + id));
        return convertToDto(artifact);
    }

    @Transactional
    public ArtifactDto createArtifact(ArtifactDto artifactDto) {
        Task task = taskRepository.findById(artifactDto.getTaskId())
                .orElseThrow(() -> new RuntimeException("Task not found with id: " + artifactDto.getTaskId()));

        Artifact artifact = new Artifact();
        artifact.setName(artifactDto.getName());
        artifact.setUrl(artifactDto.getUrl());
        artifact.setFileType(artifactDto.getFileType());
        artifact.setFileSize(artifactDto.getFileSize());
        artifact.setTask(task);
        artifact.setUploadedBy(artifactDto.getUploadedBy());

        Artifact savedArtifact = artifactRepository.save(artifact);
        return convertToDto(savedArtifact);
    }

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

    private ArtifactDto convertToDto(Artifact artifact) {
        ArtifactDto dto = new ArtifactDto();
        dto.setId(artifact.getId());
        dto.setName(artifact.getName());
        dto.setUrl(artifact.getUrl());
        dto.setFileType(artifact.getFileType());
        dto.setFileSize(artifact.getFileSize());
        dto.setTaskId(artifact.getTask().getId());
        dto.setUploadedBy(artifact.getUploadedBy());
        dto.setCreatedAt(artifact.getCreatedAt());

        if (artifact.getUploadedBy() != null) {
            try {
                UserDto user = userServiceClient.getUserById(artifact.getUploadedBy());
                dto.setUploadedByName(user.getName());
            } catch (Exception e) {
                log.warn("Could not fetch uploader name for user id {}: {}", artifact.getUploadedBy(), e.getMessage());
            }
        }

        return dto;
    }
}
