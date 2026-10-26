package com.codepilot.service;
import com.codepilot.dto.FileDto;
import com.codepilot.entity.*;
import com.codepilot.repository.*;
import com.fasterxml.jackson.databind.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.*;

@Service @RequiredArgsConstructor @Slf4j
public class AnalysisService {

    private final RepositoryRepo repositoryRepo;
    private final AnalysisReportRepo reportRepo;
    private final IssueRepo issueRepo;
    private final GitHubService gitHubService;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public AnalysisReport analyze(String githubUrl) throws Exception {
        String[] parts = githubUrl.replace("https://github.com/", "").split("/");
        Repository repo = repositoryRepo.save(Repository.builder()
                .name(parts[1]).owner(parts[0]).githubUrl(githubUrl).branch("main").build());

        List<FileDto> files = gitHubService.fetchFiles(githubUrl);
        if (files.isEmpty()) throw new RuntimeException("No code files found or repo is private");

        Map<String, Object> aiRequest = Map.of("repo_id", repo.getId(), "files", files);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String,Object>> entity = new HttpEntity<>(aiRequest, headers);

        log.info("Calling AI Service at {}", aiServiceUrl + "/api/ai/analyze");
        String aiResponseStr = restTemplate.postForObject(aiServiceUrl + "/api/ai/analyze", entity, String.class);
        
        JsonNode aiJson = mapper.readTree(aiResponseStr);
        String analysisText = aiJson.get("analysis").asText();
       analysisText = analysisText.replace("```json", "").replace("```", "").trim();
    JsonNode analysis = mapper.readTree(analysisText);

        double quality = analysis.path("quality_score").asDouble(70);
        double security = analysis.path("security_score").asDouble(70);
        double maintain = analysis.path("maintainability_score").asDouble(70);
        double perf = 75;
        double testing = 60;
        double health = (quality*0.30) + (security*0.25) + (testing*0.20) + (perf*0.15) + (maintain*0.10);

        AnalysisReport report = reportRepo.save(AnalysisReport.builder()
                .repositoryId(repo.getId())
                .healthScore(health).qualityScore(quality).securityScore(security)
                .maintainabilityScore(maintain).performanceScore(perf).testingScore(testing)
                .rawAiResponse(analysisText)
                .build());

        saveIssues(repo.getId(), analysis.path("bugs"), "BUG");
        saveIssues(repo.getId(), analysis.path("security"), "SECURITY");
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