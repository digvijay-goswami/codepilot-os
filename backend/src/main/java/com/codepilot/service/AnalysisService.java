package com.codepilot.service;

import com.codepilot.dto.FileDto;
import com.codepilot.entity.AnalysisReport;
import com.codepilot.entity.Issue;
import com.codepilot.entity.Repository;
import com.codepilot.repository.AnalysisReportRepo;
import com.codepilot.repository.IssueRepo;
import com.codepilot.repository.RepositoryRepo;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    private final RepositoryRepo repositoryRepo;
    private final AnalysisReportRepo reportRepo;
    private final IssueRepo issueRepo;
    private final GitHubService gitHubService;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public AnalysisReport analyze(String githubUrl) throws Exception {
        String[] parts = githubUrl.replace("https://github.com/", "").split("/");

        // 1. Fetch code first, so a failed fetch does not leave an empty repository row
        GitHubService.RepoSnapshot snapshot = gitHubService.fetchRepo(githubUrl);
        List<FileDto> files = snapshot.files();
        if (files.isEmpty()) throw new RuntimeException("No code files found or repo is private");

        Repository repo = repositoryRepo.save(Repository.builder()
                .name(parts[1].replace(".git", "")).owner(parts[0])
                .githubUrl(githubUrl).branch("main").build());

        // 2. Ask the AI service to analyze the code
        Map<String, Object> aiRequest = Map.of("repo_id", repo.getId(), "files", files);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(aiRequest, headers);

        log.info("Calling AI Service at {}", aiServiceUrl + "/api/ai/analyze");
        String aiResponseStr = restTemplate.postForObject(aiServiceUrl + "/api/ai/analyze", entity, String.class);

        JsonNode aiJson = mapper.readTree(aiResponseStr);
        String fence = "`".repeat(3); // markdown code fence, in case the AI adds one
        String analysisText = aiJson.get("analysis").asText()
                .replace(fence + "json", "").replace(fence, "").trim();
        JsonNode analysis = mapper.readTree(analysisText);

        // 3. Calculate the scores
        double quality = ScoreCalculator.clamp(analysis.path("quality_score").asDouble(70));
        double security = ScoreCalculator.clamp(analysis.path("security_score").asDouble(70));
        double maintain = ScoreCalculator.clamp(analysis.path("maintainability_score").asDouble(70));
        if (!analysis.has("performance_score")) {
            log.warn("AI response had no performance_score, using 70");
        }
        double perf = ScoreCalculator.clamp(analysis.path("performance_score").asDouble(70));
        double testing = ScoreCalculator.testingScore(snapshot.sourceFiles(), snapshot.testFiles());
        double health = ScoreCalculator.healthScore(quality, security, testing, perf, maintain);

        log.info("Scores: quality={} security={} testing={} performance={} maintainability={} health={}",
                quality, security, testing, perf, maintain, health);

        // 4. Save the report and the issues
        AnalysisReport report = reportRepo.save(AnalysisReport.builder()
                .repositoryId(repo.getId())
                .healthScore(health).qualityScore(quality).securityScore(security)
                .maintainabilityScore(maintain).performanceScore(perf).testingScore(testing)
                .rawAiResponse(analysisText)
                .build());

        saveIssues(repo.getId(), analysis.path("bugs"), "BUG");
        saveIssues(repo.getId(), analysis.path("security"), "SECURITY");
        saveIssues(repo.getId(), analysis.path("performance"), "PERFORMANCE");
        return report;
    }

    private void saveIssues(Long repoId, JsonNode arr, String type) {
        if (arr.isArray()) {
            for (JsonNode n : arr) {
                issueRepo.save(Issue.builder()
                        .repositoryId(repoId).type(type)
                        .severity(n.path("severity").asText("MEDIUM"))
                        .filePath(n.path("file").asText("unknown"))
                        .lineNumber(n.path("line").asInt(0))
                        .description(n.path("description").asText(n.path("issue").asText("")))
                        .build());
            }
        }
    }

    public Map<String, Object> getHealth(Long repoId) {
        AnalysisReport r = reportRepo.findTopByRepositoryIdOrderByCreatedAtDesc(repoId)
                .orElseThrow(() -> new RuntimeException("No report found"));
        var issues = issueRepo.findByRepositoryId(repoId);
        return Map.of("report", r, "issues", issues, "totalIssues", issues.size());
    }
}