package com.codepilot.repository;
import com.codepilot.entity.AnalysisReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface AnalysisReportRepo extends JpaRepository<AnalysisReport, Long> {
    Optional<AnalysisReport> findTopByRepositoryIdOrderByCreatedAtDesc(Long repoId);
}