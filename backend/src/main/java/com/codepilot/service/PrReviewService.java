package com.codepilot.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class PrReviewService {

    private static final int MAX_DIFF_CHARS = 40000;

    private final RestTemplate restTemplate;
    private final ObjectMapper mapper;

    @Value("${ai-service.url:http://localhost:8000}")
    private String aiServiceUrl;

    public Map<String, Object> review(String prUrl) throws Exception {
        PrUrlParser.PrRef ref = PrUrlParser.parse(prUrl);
        String api = "https://api.github.com/repos/" + ref.owner() + "/" + ref.repo()
                + "/pulls/" + ref.number();

        // 1. Pull request details (title, description, author, size)
        JsonNode pr = mapper.readTree(restTemplate.getForObject(api, String.class));
        String title = pr.path("title").asText("");
        String description = cut(pr.path("body").asText(""), 2000);

        // 2. The diff itself: same URL, but ask GitHub for the diff format
        HttpHeaders githubHeaders = new HttpHeaders();
        githubHeaders.set(HttpHeaders.ACCEPT, "application/vnd.github.v3.diff");
        githubHeaders.set(HttpHeaders.USER_AGENT, "codepilot-os");
        String diff = restTemplate
                .exchange(api, HttpMethod.GET, new HttpEntity<>(githubHeaders), String.class)
                .getBody();
        if (diff == null || diff.isBlank()) {
            throw new IllegalArgumentException("This pull request has no changes to review");
        }
        boolean truncated = diff.length() > MAX_DIFF_CHARS;
        diff = cut(diff, MAX_DIFF_CHARS);

        // 3. Ask the AI service to review it
        Map<String, Object> aiRequest = Map.of(
                "title", title, "description", description, "diff", diff);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("Reviewing PR {}/{}#{} ({} diff chars, truncated={})",
                ref.owner(), ref.repo(), ref.number(), diff.length(), truncated);
        String aiResponse = restTemplate.postForObject(
                aiServiceUrl + "/api/ai/review-pr",
                new HttpEntity<>(aiRequest, headers), String.class);

        JsonNode aiJson = mapper.readTree(aiResponse);
        String fence = "`".repeat(3); // markdown code fence, in case the AI adds one
        String reviewText = aiJson.get("review").asText()
                .replace(fence + "json", "").replace(fence, "").trim();
        JsonNode review = mapper.readTree(reviewText);

        // 4. Build the answer for the frontend
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("prUrl", prUrl.trim());
        result.put("title", title);
        result.put("author", pr.path("user").path("login").asText(""));
        result.put("changedFiles", pr.path("changed_files").asInt(0));
        result.put("additions", pr.path("additions").asInt(0));
        result.put("deletions", pr.path("deletions").asInt(0));
        result.put("diffTruncated", truncated);
        result.put("review", review);
        return result;
    }

    private String cut(String text, int max) {
        return text.length() > max ? text.substring(0, max) : text;
    }
}