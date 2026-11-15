package com.codepilot.service;

/** Pure scoring math, kept separate from I/O so it is easy to test. */
public final class ScoreCalculator {

    private ScoreCalculator() {}

    public static double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    /** Heuristic: 1 test file for every 2 source files earns 100. No tests earns 0. */
    public static double testingScore(int sourceFiles, int testFiles) {
        if (sourceFiles == 0 || testFiles == 0) return 0;
        double ratio = (double) testFiles / sourceFiles;
        return Math.min(100.0, Math.round(ratio * 200.0));
    }

    public static double healthScore(double quality, double security, double testing,
                                     double performance, double maintainability) {
        double health = (quality * 0.30) + (security * 0.25) + (testing * 0.20)
                + (performance * 0.15) + (maintainability * 0.10);
        return Math.round(health * 10) / 10.0;
    }
}