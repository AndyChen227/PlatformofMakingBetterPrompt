package com.betterprompt.betterpromptbyandyy2.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

/**
 * The full optimization response returned to the frontend.
 * Includes per-step details, the final prompt text, and aggregate token statistics.
 */
public class OptimizationResult {

    private List<StepResult> steps;
    private String finalPrompt;
    private TokenStats tokenStats;

    public List<StepResult> getSteps() { return steps; }
    public void setSteps(List<StepResult> steps) { this.steps = steps; }

    public String getFinalPrompt() { return finalPrompt; }
    public void setFinalPrompt(String finalPrompt) { this.finalPrompt = finalPrompt; }

    public TokenStats getTokenStats() { return tokenStats; }
    public void setTokenStats(TokenStats tokenStats) { this.tokenStats = tokenStats; }

    /**
     * Aggregate token statistics for the whole pipeline run.
     */
    public static class TokenStats {

        private int original;

        /** JSON field name is "final" — mapped via @JsonProperty because "final" is a Java keyword. */
        @JsonProperty("final")
        private int finalTokens;

        private double compressionRate;

        /** Maps each rule's display name to the number of tokens it saved. */
        private Map<String, Integer> byRule;

        public int getOriginal() { return original; }
        public void setOriginal(int original) { this.original = original; }

        public int getFinalTokens() { return finalTokens; }
        public void setFinalTokens(int finalTokens) { this.finalTokens = finalTokens; }

        public double getCompressionRate() { return compressionRate; }
        public void setCompressionRate(double compressionRate) { this.compressionRate = compressionRate; }

        public Map<String, Integer> getByRule() { return byRule; }
        public void setByRule(Map<String, Integer> byRule) { this.byRule = byRule; }
    }
}
