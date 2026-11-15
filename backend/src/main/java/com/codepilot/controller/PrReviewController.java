package com.codepilot.controller;

import com.codepilot.dto.PrReviewRequest;
import com.codepilot.service.PrReviewService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpClientErrorException;

import java.util.Map;

@RestController
@RequestMapping("/api/pr-review")
public class PrReviewController {

    private final PrReviewService prReviewService;

    public PrReviewController(PrReviewService prReviewService) {
        this.prReviewService = prReviewService;
    }

    @PostMapping
    public ResponseEntity<?> review(@RequestBody PrReviewRequest req) {
        try {
            return ResponseEntity.ok(prReviewService.review(req.getPrUrl()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (HttpClientErrorException.NotFound e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(Map.of(
                    "error", "Pull request not found. Check the link, and note that private repos are not supported."));
        } catch (HttpClientErrorException.Forbidden e) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of(
                    "error", "GitHub rate limit reached. Please try again in a few minutes."));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of(
                    "error", e.getMessage() != null ? e.getMessage() : e.toString()));
        }
    }
}