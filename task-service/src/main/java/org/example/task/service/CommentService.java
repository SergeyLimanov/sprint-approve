package org.example.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task.client.UserDto;
import org.example.task.client.UserServiceClient;
import org.example.task.dto.CommentDto;
import org.example.task.entity.Artifact;
import org.example.task.entity.Comment;
import org.example.task.entity.Task;
import org.example.task.repository.ArtifactRepository;
import org.example.task.repository.CommentRepository;
import org.example.task.repository.TaskRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService {
    private final CommentRepository commentRepository;
    private final TaskRepository taskRepository;
    private final ArtifactRepository artifactRepository;
    private final UserServiceClient userServiceClient;

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskId(taskId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByArtifactId(Long artifactId) {
        return commentRepository.findByArtifactId(artifactId).stream()
                .map(this::convertToDto)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        return convertToDto(comment);
    }

    @Transactional
    public CommentDto createComment(CommentDto commentDto) {
        Comment comment = new Comment();
        comment.setContent(commentDto.getContent());
        comment.setAuthorId(commentDto.getAuthorId());

        // Comment can be attached to either a task or an artifact
        if (commentDto.getTaskId() != null) {
            Task task = taskRepository.findById(commentDto.getTaskId())
                    .orElseThrow(() -> new RuntimeException("Task not found with id: " + commentDto.getTaskId()));
            comment.setTask(task);
        } else if (commentDto.getArtifactId() != null) {
            Artifact artifact = artifactRepository.findById(commentDto.getArtifactId())
                    .orElseThrow(() -> new RuntimeException("Artifact not found with id: " + commentDto.getArtifactId()));
            comment.setArtifact(artifact);
        } else {
            throw new RuntimeException("Comment must be attached to either a task or an artifact");
        }

        Comment savedComment = commentRepository.save(comment);
        return convertToDto(savedComment);
    }

    @Transactional
    public CommentDto updateComment(Long id, CommentDto commentDto) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        if (!comment.getAuthorId().equals(commentDto.getAuthorId())) {
            throw new RuntimeException("Only the author can update this comment");
        }

        comment.setContent(commentDto.getContent());
        Comment updatedComment = commentRepository.save(comment);
        return convertToDto(updatedComment);
    }

    @Transactional
    public void deleteComment(Long id, Long authorId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Only the author can delete this comment");
        }

        commentRepository.deleteById(id);
    }

    private CommentDto convertToDto(Comment comment) {
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

        try {
            UserDto user = userServiceClient.getUserById(comment.getAuthorId());
            dto.setAuthorName(user.getName());
        } catch (Exception e) {
            log.warn("Could not fetch author name for user id {}: {}", comment.getAuthorId(), e.getMessage());
        }

        return dto;
    }
}
