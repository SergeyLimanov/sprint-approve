package org.example.task.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentDto {
    private Long id;

    @NotBlank(message = "Comment content is required")
    private String content;

    private Long taskId;

    private Long artifactId;

    @NotNull(message = "Author ID is required")
    private Long authorId;

    private String authorName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
