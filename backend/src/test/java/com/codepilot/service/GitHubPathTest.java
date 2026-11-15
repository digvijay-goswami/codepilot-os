package com.codepilot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GitHubPathTest {

    private final GitHubService service = new GitHubService();

    @Test
    void acceptsNormalSourcePaths() {
        assertTrue(service.isSafeSourcePath("src/main/java/com/x/Owner.java"));
        assertTrue(service.isSafeSourcePath("app/utils.py"));
    }

    @Test
    void rejectsTraversalAbsoluteAndOddPaths() {
        assertFalse(service.isSafeSourcePath("../secrets.py"));
        assertFalse(service.isSafeSourcePath("/etc/passwd.py"));
        assertFalse(service.isSafeSourcePath("a/b.py?ref=other"));
        assertFalse(service.isSafeSourcePath(null));
    }

    @Test
    void rejectsUnsupportedFileTypes() {
        assertFalse(service.isSafeSourcePath("README.md"));
        assertFalse(service.isSafeSourcePath("pom.xml"));
    }
}