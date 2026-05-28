package org.example.task.service;

import org.example.task.dto.CommentDto;

import java.util.List;

public interface ICommentService {
    List<CommentDto> getCommentsByTaskId(Long taskId);
    
    List<CommentDto> getCommentsByArtifactId(Long artifactId);
    
    CommentDto getCommentById(Long id);
    
    CommentDto createComment(CommentDto commentDto);
    
    CommentDto updateComment(Long id, CommentDto commentDto);
    
    void deleteComment(Long id, Long authorId);
}
