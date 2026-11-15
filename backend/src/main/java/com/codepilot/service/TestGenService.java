package com.codepilot.service;

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
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestGenService {

    private final RepositoryRepo repositoryRepo;
    private final GitHubService gitHubService;
    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public List<String> listFiles(Long repoId) throws Exception {
        return gitHubService.listSourceFiles(githubUrlOf(repoId));
    }

    public JsonNode generate(Long repoId, String filePath) throws Exception {
        String githubUrl = githubUrlOf(repoId);
        String content = gitHubService.fetchFileContent(githubUrl, filePath);
        if (content.isBlank()) {
            throw new IllegalArgumentException("That file is empty");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        Map<String, Object> body = Map.of("path", filePath, "content", content);

        log.info("Generating tests for {} ({} chars) in repo {}", filePath, content.length(), repoId);
        try {
            String response = restTemplate.postForObject(
                    aiServiceUrl + "/api/ai/generate-tests",
                    new HttpEntity<>(body, headers), String.class);
            return mapper.readTree(response);
        } catch (HttpStatusCodeException e) {
            throw new IllegalStateException(aiError(e));
        }
    }

    private String githubUrlOf(Long repoId) {
        return repositoryRepo.findById(repoId)
                .orElseThrow(() -> new IllegalArgumentException("Repository not found. Analyze it first."))
                .getGithubUrl();
    }

    /** Pulls the readable message out of the AI service's error reply. */
    private String aiError(HttpStatusCodeException e) {
        try {
            JsonNode detail = mapper.readTree(e.getResponseBodyAsString()).path("detail");
            if (detail.isTextual()) return detail.asText();
        } catch (Exception ignored) {
            // fall through to the generic message
        }
        return "AI service error (" + e.getStatusCode().value() + ")";
    }
}