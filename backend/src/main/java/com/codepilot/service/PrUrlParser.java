package com.codepilot.service;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Turns a pull request link into owner, repo and number. */
public final class PrUrlParser {

    public record PrRef(String owner, String repo, int number) {}

    private static final Pattern PR_URL = Pattern.compile(
            "^https?://github\\.com/([\\w.-]+)/([\\w.-]+)/pull/(\\d+)(?:[/?#].*)?$");

    private PrUrlParser() {}

    public static PrRef parse(String url) {
        if (url == null) {
            throw new IllegalArgumentException("Pull request link is missing");
        }
        Matcher m = PR_URL.matcher(url.trim());
        if (!m.matches()) {
            throw new IllegalArgumentException(
                    "Enter a pull request link like https://github.com/owner/repo/pull/123");
        }
        return new PrRef(m.group(1), m.group(2), Integer.parseInt(m.group(3)));
    }
}