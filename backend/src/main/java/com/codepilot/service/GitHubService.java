package com.codepilot.service;

import com.codepilot.dto.FileDto;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

@Service
@Slf4j
public class GitHubService {

    /** What we learn about a repo: the files to analyze, plus how many source and test files it has. */
    public record RepoSnapshot(List<FileDto> files, int sourceFiles, int testFiles) {}

    private static final int MAX_FILES = 40;
    private static final int MAX_CHARS_PER_FILE = 15000;

    /**
     * Optional GitHub token (env var GITHUB_TOKEN). Without it GitHub allows only 60 API calls
     * per hour per IP, which shared hosts like Render exhaust immediately.
     */
    @Value("${github.token:}")
    private String githubToken;

    private final RestTemplate restTemplate = buildRestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();
    private final Set<String> allowedExt = Set.of(".java", ".py", ".js", ".ts", ".tsx", ".go", ".cpp");

    private RestTemplate buildRestTemplate() {
        RestTemplate rt = new RestTemplate();
        // Add the token to every api.github.com request (the token is read per request).
        rt.getInterceptors().add((request, body, execution) -> {
            if (githubToken != null && !githubToken.isBlank()
                    && "api.github.com".equals(request.getURI().getHost())) {
                request.getHeaders().setBearerAuth(githubToken.trim());
            }
            return execution.execute(request, body);
        });
        return rt;
    }

    /** Kept so the /test-github endpoint still works. */
    public List<FileDto> fetchFiles(String githubUrl) throws Exception {
        return fetchRepo(githubUrl).files();
    }

    public RepoSnapshot fetchRepo(String githubUrl) throws Exception {
        String[] parts = githubUrl.replace("https://github.com/", "").split("/");
        String owner = parts[0];
        String repo = parts[1].replace(".git", "");

        String repoInfoUrl = "https://api.github.com/repos/" + owner + "/" + repo;
        JsonNode repoInfo = mapper.readTree(restTemplate.getForObject(repoInfoUrl, String.class));
        String branch = repoInfo.get("default_branch").asText("main");

        String treeUrl = "https://api.github.com/repos/" + owner + "/" + repo
                + "/git/trees/" + branch + "?recursive=1";
        JsonNode tree = mapper.readTree(restTemplate.getForObject(treeUrl, String.class));
        if (tree.path("truncated").asBoolean(false)) {
            log.warn("GitHub returned a truncated file list for {}, counts are approximate", githubUrl);
        }

        List<FileDto> files = new ArrayList<>();
        int sourceFiles = 0;
        int testFiles = 0;

        for (JsonNode node : tree.get("tree")) {
            if (!"blob".equals(node.get("type").asText())) continue;
            String path = node.get("path").asText();
            if (allowedExt.stream().noneMatch(path::endsWith)) continue;
            if (isNoise(path)) continue;

            if (isTestFile(path)) {
                testFiles++;
                continue;
            }
            sourceFiles++;

            // Keep counting every file, but only download the first MAX_FILES
            if (files.size() >= MAX_FILES) continue;

            try {
                String rawUrl = "https://raw.githubusercontent.com/" + owner + "/" + repo
                        + "/" + branch + "/" + path;
                String content = restTemplate.getForObject(rawUrl, String.class);
                if (content != null && content.length() > MAX_CHARS_PER_FILE) {
                    content = content.substring(0, MAX_CHARS_PER_FILE);
                }
                files.add(new FileDto(path, content));
            } catch (Exception e) {
                log.warn("skip file {} {}", path, e.getMessage());
            }
        }

        log.info("Repo stats for {}: {} source files, {} test files, {} files downloaded",
                githubUrl, sourceFiles, testFiles, files.size());
        return new RepoSnapshot(files, sourceFiles, testFiles);
    }

    /** Source files (no tests, no generated code) that tests could be written for. */
    public List<String> listSourceFiles(String githubUrl) throws Exception {
        String[] ref = ownerAndRepo(githubUrl);
        String base = "https://api.github.com/repos/" + ref[0] + "/" + ref[1];
        JsonNode repoInfo = mapper.readTree(restTemplate.getForObject(base, String.class));
        String branch = repoInfo.get("default_branch").asText("main");
        JsonNode tree = mapper.readTree(restTemplate.getForObject(
                base + "/git/trees/" + branch + "?recursive=1", String.class));

        List<String> paths = new ArrayList<>();
        for (JsonNode node : tree.get("tree")) {
            if (!"blob".equals(node.get("type").asText())) continue;
            String path = node.get("path").asText();
            if (allowedExt.stream().noneMatch(path::endsWith)) continue;
            if (isNoise(path) || isTestFile(path)) continue;
            paths.add(path);
        }
        return paths.stream().sorted().limit(500).toList();
    }

    /** Downloads one file from the repo's default branch. The path is validated first. */
    public String fetchFileContent(String githubUrl, String path) {
        if (!isSafeSourcePath(path)) {
            throw new IllegalArgumentException("Invalid file path");
        }
        String[] ref = ownerAndRepo(githubUrl);
        String url = "https://api.github.com/repos/" + ref[0] + "/" + ref[1] + "/contents/" + path;
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.ACCEPT, "application/vnd.github.raw+json");
        headers.set(HttpHeaders.USER_AGENT, "codepilot-os");
        String content = restTemplate
                .exchange(url, HttpMethod.GET, new HttpEntity<>(headers), String.class)
                .getBody();
        return content == null ? "" : content;
    }

    /** Only plain relative source-file paths are allowed: no "..", no query strings, no odd characters. */
    boolean isSafeSourcePath(String path) {
        return path != null
                && path.matches("[\\w./@+-]+")
                && !path.contains("..")
                && !path.startsWith("/")
                && allowedExt.stream().anyMatch(path::endsWith);
    }

    private String[] ownerAndRepo(String githubUrl) {
        String[] parts = githubUrl.replace("https://github.com/", "").split("/");
        if (parts.length < 2) {
            throw new IllegalArgumentException("Invalid GitHub URL");
        }
        return new String[]{parts[0], parts[1].replace(".git", "")};
    }

    /** Generated or third-party code that should not be analyzed or counted. */
    boolean isNoise(String path) {
        String p = "/" + path.toLowerCase();
        return p.contains("/node_modules/") || p.contains("/.storybook/")
                || p.contains("/dist/") || p.contains("/build/")
                || p.contains("/generated/") || p.contains("/vendor/")
                || p.endsWith(".d.ts") || p.endsWith(".min.js");
    }

    boolean isTestFile(String path) {
        String p = "/" + path.toLowerCase();
        String name = p.substring(p.lastIndexOf('/') + 1);
        String originalName = path.substring(path.lastIndexOf('/') + 1);
        return p.contains("/test/") || p.contains("/tests/") || p.contains("/__tests__/")
                || name.contains(".test.") || name.contains(".spec.")
                || name.startsWith("test_") || name.contains("_test.")
                || originalName.endsWith("Test.java") || originalName.endsWith("Tests.java");
    }
}