package com.codepilot.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Repository {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String githubUrl;
    private String owner;
    @Builder.Default
    private String branch = "main";
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}