package org.example.sprint.client;

import lombok.Data;

@Data
public class TaskDto {
    private Long id;
    private String title;
    private String status;
    private Long sprintId;
}
