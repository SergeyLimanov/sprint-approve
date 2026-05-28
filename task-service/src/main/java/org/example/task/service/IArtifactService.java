package org.example.task.service;

import org.example.task.dto.ArtifactDto;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface IArtifactService {
    List<ArtifactDto> getArtifactsByTaskId(Long taskId, int urlExpiryMinutes);
    
    ArtifactDto getArtifactById(Long id, int urlExpiryMinutes);
    
    org.example.task.entity.Artifact getArtifactEntityById(Long id);
    
    ArtifactDto createArtifact(ArtifactDto artifactDto);
    
    ArtifactDto uploadFile(MultipartFile file, Long taskId, Long uploadedBy, String customName);
    
    String getDownloadUrl(Long id, int expiryMinutes);
    
    Resource downloadFile(String fileName);
    
    void deleteArtifact(Long id);
}
