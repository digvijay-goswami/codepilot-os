package com.codepilot.service;

import com.codepilot.dto.FileDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@Slf4j
public class GitHubService {

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<String> allowedExt = Set.of(".java", ".py", ".js", ".ts", ".tsx", ".go", ".cpp");

    private HttpHeaders createHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "CodePilot-OS");
        headers.set("Accept", "application/vnd.github.v3+json");
        return headers;
    }

    public List<FileDto> fetchFiles(String githubUrl) throws Exception {
        String cleanUrl = githubUrl.trim().replaceAll("/+$", "");
        String[] parts = cleanUrl.replace("https://github.com/", "")
                                  .replace("http://github.com/", "")
                                  .split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub URL format: " + githubUrl);
        }
        String owner = parts[0];
        String repo = parts[1].replace(".git", "");

        HttpEntity<Void> requestEntity = new HttpEntity<>(createHeaders());

        String repoInfoUrl = "https://api.github.com/repos/" + owner + "/" + repo;
        ResponseEntity<String> repoInfoResp = restTemplate.exchange(repoInfoUrl, HttpMethod.GET, requestEntity, String.class);
        JsonNode repoInfo = mapper.readTree(repoInfoResp.getBody());
        String branch = repoInfo.has("default_branch") ? repoInfo.get("default_branch").asText("main") : "main";

        String treeUrl = "https://api.github.com/repos/" + owner + "/" + repo + "/git/trees/" + branch + "?recursive=1";
        ResponseEntity<String> treeResp = restTemplate.exchange(treeUrl, HttpMethod.GET, requestEntity, String.class);
        JsonNode tree = mapper.readTree(treeResp.getBody());

        List<FileDto> files = new ArrayList<>();
        int count = 0;
        if (tree.has("tree") && tree.get("tree").isArray()) {
            for (JsonNode node : tree.get("tree")) {
                if (!"blob".equals(node.get("type").asText())) continue;
                String path = node.get("path").asText();
                if (allowedExt.stream().noneMatch(path::endsWith)) continue;
                if (count >= 40) break;

                try {
                    String rawUrl = "https://raw.githubusercontent.com/" + owner + "/" + repo + "/" + branch + "/" + path;
                    ResponseEntity<String> fileResp = restTemplate.exchange(rawUrl, HttpMethod.GET, requestEntity, String.class);
                    String content = fileResp.getBody();
                    if (content != null && content.length() > 15000) {
                        content = content.substring(0, 15000);
                    }
                    files.add(new FileDto(path, content));
                    count++;
                } catch (Exception e) {
                    log.warn("skip file {} {}", path, e.getMessage());
                }
            }
        }
        log.info("Fetched {} files from {}", files.size(), githubUrl);
        return files;
    }
}
