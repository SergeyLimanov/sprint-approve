package org.example.task.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.task.dto.CommentDto;
import org.example.task.service.CommentService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/comments")
@RequiredArgsConstructor
@Tag(name = "Comments", description = "Comment management API")
public class CommentController {
    private final CommentService commentService;

    @GetMapping("/task/{taskId}")
    @Operation(summary = "Get comments by task ID")
    public ResponseEntity<List<CommentDto>> getCommentsByTaskId(@PathVariable Long taskId) {
        return ResponseEntity.ok(commentService.getCommentsByTaskId(taskId));
    }

    @GetMapping("/artifact/{artifactId}")
    @Operation(summary = "Get comments by artifact ID")
    public ResponseEntity<List<CommentDto>> getCommentsByArtifactId(@PathVariable Long artifactId) {
        return ResponseEntity.ok(commentService.getCommentsByArtifactId(artifactId));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get comment by ID")
    public ResponseEntity<CommentDto> getCommentById(@PathVariable Long id) {
        return ResponseEntity.ok(commentService.getCommentById(id));
    }

    @PostMapping
    @Operation(summary = "Create new comment")
    public ResponseEntity<CommentDto> createComment(@Valid @RequestBody CommentDto commentDto) {
        return ResponseEntity.status(HttpStatus.CREATED).body(commentService.createComment(commentDto));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update comment")
    public ResponseEntity<CommentDto> updateComment(@PathVariable Long id, @Valid @RequestBody CommentDto commentDto) {
        return ResponseEntity.ok(commentService.updateComment(id, commentDto));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete comment")
    public ResponseEntity<Void> deleteComment(@PathVariable Long id, @RequestParam Long authorId) {
        commentService.deleteComment(id, authorId);
        return ResponseEntity.noContent().build();
    }
}
