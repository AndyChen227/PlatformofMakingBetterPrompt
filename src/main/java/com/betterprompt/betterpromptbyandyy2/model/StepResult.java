package com.betterprompt.betterpromptbyandyy2.model;

import java.util.List;

/**
 * Result produced by a single Rule's apply() call.
 * Captures the before/after text, token counts, and a human-readable list of changes made.
 */
public class StepResult {

    private String ruleName;
    private String ruleLevel;
    private String inputText;
    private String outputText;
    private int tokensBefore;
    private int tokensAfter;
    private int tokensSaved;
    private List<String> changes;
    /** "done" | "skipped" | "error" */
    private String status;

    public String getRuleName() { return ruleName; }
    public void setRuleName(String ruleName) { this.ruleName = ruleName; }

    public String getRuleLevel() { return ruleLevel; }
    public void setRuleLevel(String ruleLevel) { this.ruleLevel = ruleLevel; }

    public String getInputText() { return inputText; }
    public void setInputText(String inputText) { this.inputText = inputText; }

    public String getOutputText() { return outputText; }
    public void setOutputText(String outputText) { this.outputText = outputText; }

    public int getTokensBefore() { return tokensBefore; }
    public void setTokensBefore(int tokensBefore) { this.tokensBefore = tokensBefore; }

    public int getTokensAfter() { return tokensAfter; }
    public void setTokensAfter(int tokensAfter) { this.tokensAfter = tokensAfter; }

    public int getTokensSaved() { return tokensSaved; }
    public void setTokensSaved(int tokensSaved) { this.tokensSaved = tokensSaved; }

    public List<String> getChanges() { return changes; }
    public void setChanges(List<String> changes) { this.changes = changes; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
}
