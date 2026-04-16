package org.example.team.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.example.team.entity.UserRole;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;

    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;

    @NotBlank(message = "Name is required")
    private String name;

    private String password; // Only for creation/update, not returned in responses

    private Long teamId;
    private String teamName;

    @NotNull(message = "Role is required")
    private UserRole role;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
