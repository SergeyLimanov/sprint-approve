package org.example.sprint.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.sprint.entity.SprintStatus;
import org.example.sprint.entity.SprintType;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SprintDto {
    private Long id;

    @NotBlank(message = "Sprint name is required")
    private String name;

    private String description;

    @NotNull(message = "Team ID is required")
    private Long teamId;

    private String teamName;

    @NotNull(message = "Sprint type is required")
    private SprintType type;

    private SprintStatus status;

    private LocalDateTime startDate;
    private LocalDateTime endDate;

    private Long createdBy;
    private String createdByName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
