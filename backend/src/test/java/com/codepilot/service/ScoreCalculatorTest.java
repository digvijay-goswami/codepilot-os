package com.codepilot.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ScoreCalculatorTest {

    @Test
    void testingScoreIsZeroWhenThereAreNoTests() {
        assertEquals(0.0, ScoreCalculator.testingScore(20, 0));
    }

    @Test
    void testingScoreIsZeroWhenThereAreNoSourceFiles() {
        assertEquals(0.0, ScoreCalculator.testingScore(0, 5));
    }

    @Test
    void oneTestFilePerTwoSourceFilesScores100() {
        assertEquals(100.0, ScoreCalculator.testingScore(10, 5));
    }

    @Test
    void testingScoreIsCappedAt100() {
        assertEquals(100.0, ScoreCalculator.testingScore(2, 10));
    }

    @Test
    void testingScoreScalesWithTheRatio() {
        // 1 test per 10 source files: 0.1 * 200 = 20
        assertEquals(20.0, ScoreCalculator.testingScore(10, 1));
    }

    @Test
    void healthScoreIsTheWeightedSum() {
        // 80*0.30 + 60*0.25 + 40*0.20 + 70*0.15 + 50*0.10 = 62.5
        assertEquals(62.5, ScoreCalculator.healthScore(80, 60, 40, 70, 50), 0.001);
    }

    @Test
    void perfectScoresGiveAPerfectHealthScore() {
        assertEquals(100.0, ScoreCalculator.healthScore(100, 100, 100, 100, 100), 0.001);
    }

    @Test
    void clampKeepsValuesBetween0And100() {
        assertEquals(0.0, ScoreCalculator.clamp(-15));
        assertEquals(100.0, ScoreCalculator.clamp(140));
        assertEquals(63.0, ScoreCalculator.clamp(63));
    }
}