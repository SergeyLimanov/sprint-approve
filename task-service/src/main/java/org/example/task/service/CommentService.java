package org.example.task.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.task.client.dto.UserResponse;
import org.example.task.client.UserServiceClient;
import org.example.task.dto.CommentDto;
import org.example.task.entity.Artifact;
import org.example.task.entity.Comment;
import org.example.task.entity.Task;
import org.example.task.mapper.CommentMapper;
import org.example.task.repository.CommentRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CommentService implements ICommentService {
    private final CommentRepository commentRepository;
    private final ITaskService taskService;
    private final IArtifactService artifactService;
    private final UserServiceClient userServiceClient;

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByTaskId(Long taskId) {
        return commentRepository.findByTaskId(taskId).stream()
                .map(this::enrichWithAuthorName)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CommentDto> getCommentsByArtifactId(Long artifactId) {
        return commentRepository.findByArtifactId(artifactId).stream()
                .map(this::enrichWithAuthorName)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CommentDto getCommentById(Long id) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));
        return enrichWithAuthorName(comment);
    }

    @Override
    @Transactional
    public CommentDto createComment(CommentDto commentDto) {
        Comment comment = new Comment();
        comment.setContent(commentDto.getContent());
        comment.setAuthorId(commentDto.getAuthorId());

        // Comment can be attached to either a task or an artifact
        if (commentDto.getTaskId() != null) {
            Task task = taskService.getTaskEntityById(commentDto.getTaskId());
            comment.setTask(task);
        } else if (commentDto.getArtifactId() != null) {
            Artifact artifact = artifactService.getArtifactEntityById(commentDto.getArtifactId());
            comment.setArtifact(artifact);
        } else {
            throw new RuntimeException("Comment must be attached to either a task or an artifact");
        }

        Comment savedComment = commentRepository.save(comment);
        return enrichWithAuthorName(savedComment);
    }

    @Override
    @Transactional
    public CommentDto updateComment(Long id, CommentDto commentDto) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        if (!comment.getAuthorId().equals(commentDto.getAuthorId())) {
            throw new RuntimeException("Only the author can update this comment");
        }

        comment.setContent(commentDto.getContent());
        Comment updatedComment = commentRepository.save(comment);
        return enrichWithAuthorName(updatedComment);
    }

    @Override
    @Transactional
    public void deleteComment(Long id, Long authorId) {
        Comment comment = commentRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Comment not found with id: " + id));

        if (!comment.getAuthorId().equals(authorId)) {
            throw new RuntimeException("Only the author can delete this comment");
        }

        commentRepository.deleteById(id);
    }

    private CommentDto enrichWithAuthorName(Comment comment) {
        CommentDto dto = CommentMapper.toDto(comment);

        try {
            UserResponse user = userServiceClient.getUserById(comment.getAuthorId());
            dto.setAuthorName(user.getName());
        } catch (Exception e) {
            log.warn("Could not fetch author name for user id {}: {}", comment.getAuthorId(), e.getMessage());
        }

        return dto;
    }
}
