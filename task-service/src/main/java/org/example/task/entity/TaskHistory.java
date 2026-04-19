package org.example.task.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "task_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaskHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private String previousStatus;

    @Column(nullable = false)
    private String newStatus;

    private String comment;

    @Column(nullable = false)
    private Long changedBy;

    private String changedByName;

    @CreationTimestamp
    @Column(nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
