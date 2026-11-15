package com.codepilot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PrUrlParserTest {

    @Test
    void parsesANormalPullRequestLink() {
        PrUrlParser.PrRef ref = PrUrlParser.parse("https://github.com/spring-projects/spring-petclinic/pull/2090");
        assertEquals("spring-projects", ref.owner());
        assertEquals("spring-petclinic", ref.repo());
        assertEquals(2090, ref.number());
    }

    @Test
    void acceptsLinksToTheFilesTabOrWithQueryStrings() {
        assertEquals(12, PrUrlParser.parse("https://github.com/o/r/pull/12/files").number());
        assertEquals(12, PrUrlParser.parse("https://github.com/o/r/pull/12?diff=split").number());
    }

    @Test
    void ignoresSurroundingWhitespace() {
        assertEquals(7, PrUrlParser.parse("  https://github.com/o/r/pull/7  ").number());
    }

    @Test
    void rejectsLinksThatAreNotPullRequests() {
        assertThrows(IllegalArgumentException.class,
                () -> PrUrlParser.parse("https://github.com/o/r"));
        assertThrows(IllegalArgumentException.class,
                () -> PrUrlParser.parse("https://github.com/o/r/issues/5"));
        assertThrows(IllegalArgumentException.class,
                () -> PrUrlParser.parse("hello"));
    }

    @Test
    void rejectsNull() {
        assertThrows(IllegalArgumentException.class, () -> PrUrlParser.parse(null));
    }
}