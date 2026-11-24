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
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnalysisService {

    // Wait times (ms) before retry 1 and retry 2, so there are 3 attempts in total
    private static final long[] RETRY_WAITS_MS = {3000, 10000};

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

        String aiResponseStr = callAiServiceWithRetry(aiServiceUrl + "/api/ai/analyze", entity);

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

    /**
     * Calls the AI service. Render's free tier sits behind Cloudflare, which sometimes answers
     * with 429 (or 502/503/504 while the service wakes up). Those are temporary, so wait and retry.
     */
    private String callAiServiceWithRetry(String url, HttpEntity<Map<String, Object>> entity) {
        int attempts = RETRY_WAITS_MS.length + 1;
        for (int attempt = 1; ; attempt++) {
            try {
                log.info("Calling AI Service at {} (attempt {}/{})", url, attempt, attempts);
                return restTemplate.postForObject(url, entity, String.class);
            } catch (HttpClientErrorException | HttpServerErrorException e) {
                int status = e.getStatusCode().value();
                boolean temporary = status == 429 || status == 502 || status == 503 || status == 504;
                if (!temporary) throw e;

                if (attempt >= attempts) {
                    log.error("AI service still returning {} after {} attempts", status, attempts);
                    throw new RuntimeException(
                            "The AI service is busy or waking up (HTTP " + status + "). "
                                    + "Please wait a minute and try again.");
                }
                long wait = RETRY_WAITS_MS[attempt - 1];
                log.warn("AI service returned {} on attempt {}, retrying in {} ms", status, attempt, wait);
                try {
                    Thread.sleep(wait);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException("Analysis was interrupted");
                }
            }
        }
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