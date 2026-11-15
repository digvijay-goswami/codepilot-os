package com.codepilot.controller;

import com.codepilot.dto.TestGenRequest;
import com.codepilot.service.TestGenService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@RestController
@RequestMapping("/api/repositories/{id}")
public class TestGenController {

    private final TestGenService testGenService;

    public TestGenController(TestGenService testGenService) {
        this.testGenService = testGenService;
    }

    @GetMapping("/files")
    public ResponseEntity<?> files(@PathVariable Long id) {
        try {
            return ResponseEntity.ok(Map.of("files", testGenService.listFiles(id)));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (HttpClientErrorException.Forbidden e) {
            return rateLimited();
        } catch (Exception e) {
            e.printStackTrace();
            return serverError(e);
        }
    }

    @PostMapping("/generate-tests")
    public ResponseEntity<?> generate(@PathVariable Long id, @RequestBody TestGenRequest req) {
        try {
            return ResponseEntity.ok(testGenService.generate(id, req.getFilePath()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "File not found on GitHub. It may have been moved or deleted."));
        } catch (HttpClientErrorException.Forbidden e) {
            return rateLimited();
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            e.printStackTrace();
            return serverError(e);
        }
    }

    private ResponseEntity<?> rateLimited() {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                .body(Map.of("error", "GitHub rate limit reached. Please try again in a few minutes."));
    }

    private ResponseEntity<?> serverError(Exception e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Map.of("error", e.getMessage() != null ? e.getMessage() : e.toString()));
    }
}