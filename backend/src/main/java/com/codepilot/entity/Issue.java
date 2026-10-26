package com.codepilot.entity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "issues")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Issue {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long repositoryId;
    private String type;
    private String severity;
    private String filePath;
    private Integer lineNumber;
    @Column(columnDefinition = "TEXT")
    private String description;
}