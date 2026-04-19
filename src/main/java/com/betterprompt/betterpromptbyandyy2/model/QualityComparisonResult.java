package com.betterprompt.betterpromptbyandyy2.model;

public class QualityComparisonResult {

    private String originalPrompt;
    private String optimizedPrompt;
    private String originalAnswer;
    private String optimizedAnswer;

    /** Average of the three dimension scores (rounded). */
    private int    originalScore;
    private int    optimizedScore;

    // ── Per-dimension scores ─────────────────────────────────────
    private int relevanceScoreBefore;
    private int relevanceScoreAfter;
    private int densityScoreBefore;
    private int densityScoreAfter;
    private int clarityScoreBefore;
    private int clarityScoreAfter;

    /** 2-3 sentence Chinese natural-language summary from the model. */
    private String naturalSummary;

    private int    tokensBefore;
    private int    tokensAfter;
    // token 效率比 = (优化后质量/优化后token - 优化前质量/优化前token) / 优化前效率 × 100
    // 单位：百分比，正数表示每个token产出的质量提升了多少
    private double optimizationScore;
    private String verdict;

    // ── Getters / Setters ────────────────────────────────────────

    public String getOriginalPrompt()  { return originalPrompt; }
    public void setOriginalPrompt(String v)  { this.originalPrompt = v; }

    public String getOptimizedPrompt() { return optimizedPrompt; }
    public void setOptimizedPrompt(String v) { this.optimizedPrompt = v; }

    public String getOriginalAnswer()  { return originalAnswer; }
    public void setOriginalAnswer(String v)  { this.originalAnswer = v; }

    public String getOptimizedAnswer() { return optimizedAnswer; }
    public void setOptimizedAnswer(String v) { this.optimizedAnswer = v; }

    public int getOriginalScore()  { return originalScore; }
    public void setOriginalScore(int v)  { this.originalScore = v; }

    public int getOptimizedScore() { return optimizedScore; }
    public void setOptimizedScore(int v) { this.optimizedScore = v; }

    public int getRelevanceScoreBefore() { return relevanceScoreBefore; }
    public void setRelevanceScoreBefore(int v) { this.relevanceScoreBefore = v; }

    public int getRelevanceScoreAfter()  { return relevanceScoreAfter; }
    public void setRelevanceScoreAfter(int v)  { this.relevanceScoreAfter = v; }

    public int getDensityScoreBefore()   { return densityScoreBefore; }
    public void setDensityScoreBefore(int v)   { this.densityScoreBefore = v; }

    public int getDensityScoreAfter()    { return densityScoreAfter; }
    public void setDensityScoreAfter(int v)    { this.densityScoreAfter = v; }

    public int getClarityScoreBefore()   { return clarityScoreBefore; }
    public void setClarityScoreBefore(int v)   { this.clarityScoreBefore = v; }

    public int getClarityScoreAfter()    { return clarityScoreAfter; }
    public void setClarityScoreAfter(int v)    { this.clarityScoreAfter = v; }

    public String getNaturalSummary()    { return naturalSummary; }
    public void setNaturalSummary(String v)    { this.naturalSummary = v; }

    public int getTokensBefore() { return tokensBefore; }
    public void setTokensBefore(int v) { this.tokensBefore = v; }

    public int getTokensAfter()  { return tokensAfter; }
    public void setTokensAfter(int v)  { this.tokensAfter = v; }

    public double getOptimizationScore() { return optimizationScore; }
    public void setOptimizationScore(double v) { this.optimizationScore = v; }

    public String getVerdict() { return verdict; }
    public void setVerdict(String v) { this.verdict = v; }
}
