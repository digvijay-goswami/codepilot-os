package com.codepilot.entity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnalysisReport {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long repositoryId;
    private Double healthScore;
    private Double qualityScore;
    private Double securityScore;
    private Double maintainabilityScore;
    private Double performanceScore;
    private Double testingScore;
    @Column(columnDefinition = "TEXT")
    private String rawAiResponse;
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}