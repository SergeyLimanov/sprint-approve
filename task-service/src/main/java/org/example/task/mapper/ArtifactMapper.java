package org.example.task.mapper;

import org.example.task.dto.ArtifactDto;
import org.example.task.entity.Artifact;

public final class ArtifactMapper {
    
    private ArtifactMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static ArtifactDto toDto(Artifact artifact) {
        if (artifact == null) {
            return null;
        }
        
        ArtifactDto dto = new ArtifactDto();
        dto.setId(artifact.getId());
        dto.setName(artifact.getName());
        dto.setUrl(artifact.getUrl());
        dto.setFileType(artifact.getFileType());
        dto.setFileSize(artifact.getFileSize());
        dto.setTaskId(artifact.getTask().getId());
        dto.setUploadedBy(artifact.getUploadedBy());
        dto.setCreatedAt(artifact.getCreatedAt());
        return dto;
    }
}
