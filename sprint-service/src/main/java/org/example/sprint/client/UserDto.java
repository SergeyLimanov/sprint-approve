package org.example.sprint.client;

import lombok.Data;

@Data
public class UserDto {
    private Long id;
    private String email;
    private String name;
    private String role;
    private Long teamId;
}
