package org.example.task.client.dto;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String email;
    private String role;
    private String name;
}
