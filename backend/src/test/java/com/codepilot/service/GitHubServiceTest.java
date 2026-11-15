package com.codepilot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubServiceTest {

    private final GitHubService service = new GitHubService();

    @Test
    void recognizesTestFilesAcrossLanguages() {
        assertTrue(service.isTestFile("src/test/java/com/x/OwnerTest.java"));
        assertTrue(service.isTestFile("src/components/Button.test.tsx"));
        assertTrue(service.isTestFile("tests/test_api.py"));
        assertTrue(service.isTestFile("pkg/handler_test.go"));
    }

    @Test
    void doesNotTreatNormalSourceFilesAsTests() {
        assertFalse(service.isTestFile("src/main/java/com/x/Owner.java"));
        assertFalse(service.isTestFile("src/app/latest.ts"));
    }

    @Test
    void skipsGeneratedAndThirdPartyCode() {
        assertTrue(service.isNoise("node_modules/react/index.js"));
        assertTrue(service.isNoise("src/types/index.d.ts"));
        assertTrue(service.isNoise("web/dist/app.min.js"));
        assertFalse(service.isNoise("src/main/java/com/x/App.java"));
    }
}