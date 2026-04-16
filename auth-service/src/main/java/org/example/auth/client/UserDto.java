package org.example.auth.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String password;
    private Long teamId;
    private String teamName;
    private String role;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
