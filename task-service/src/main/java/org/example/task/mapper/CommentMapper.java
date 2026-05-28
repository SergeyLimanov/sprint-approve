package org.example.task.mapper;

import org.example.task.dto.CommentDto;
import org.example.task.entity.Comment;

public final class CommentMapper {
    
    private CommentMapper() {
        throw new UnsupportedOperationException("Utility class");
    }
    
    public static CommentDto toDto(Comment comment) {
        if (comment == null) {
            return null;
        }
        
        CommentDto dto = new CommentDto();
        dto.setId(comment.getId());
        dto.setContent(comment.getContent());
        
        if (comment.getTask() != null) {
            dto.setTaskId(comment.getTask().getId());
        }
        if (comment.getArtifact() != null) {
            dto.setArtifactId(comment.getArtifact().getId());
        }
        
        dto.setAuthorId(comment.getAuthorId());
        dto.setCreatedAt(comment.getCreatedAt());
        dto.setUpdatedAt(comment.getUpdatedAt());
        return dto;
    }
}
