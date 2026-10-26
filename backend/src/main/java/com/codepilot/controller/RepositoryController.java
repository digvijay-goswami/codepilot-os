package com.codepilot.controller;

import com.codepilot.dto.AnalyzeRequest;
import com.codepilot.dto.FileDto;
import com.codepilot.entity.AnalysisReport;
import com.codepilot.service.AnalysisService;
import com.codepilot.service.GitHubService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/repositories")
public class RepositoryController {

    private final AnalysisService analysisService;
    private final GitHubService gitHubService;

    public RepositoryController(AnalysisService analysisService, GitHubService gitHubService) {
        this.analysisService = analysisService;
        this.gitHubService = gitHubService;
    }

    @GetMapping("/test-github")
    public ResponseEntity<?> testGithub(@RequestParam String url) {
        try {
            List<FileDto> files = gitHubService.fetchFiles(url);
            return ResponseEntity.ok(Map.of(
                "status", "SUCCESS",
                "repository", url,
                "filesCount", files.size(),
                "files", files.stream().map(FileDto::getPath).toList()
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of(
                "status", "ERROR",
                "error", e.getMessage() != null ? e.getMessage() : e.toString()
            ));
        }
    }

    @PostMapping("/analyze")
    public ResponseEntity<?> analyze(@RequestBody AnalyzeRequest req) {
        try {
            AnalysisReport report = analysisService.analyze(req.getGithubUrl());
            return ResponseEntity.ok(Map.of(
                "message", "Analysis Complete",
                "repositoryId", report.getRepositoryId(),
                "healthScore", report.getHealthScore(),
                "report", report
            ));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/{id}/health-score")
    public ResponseEntity<?> health(@PathVariable Long id) {
        return ResponseEntity.ok(analysisService.getHealth(id));
    }
}