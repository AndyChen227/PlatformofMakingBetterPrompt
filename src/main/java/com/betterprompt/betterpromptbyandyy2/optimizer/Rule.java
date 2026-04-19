package com.betterprompt.betterpromptbyandyy2.optimizer;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;

/**
 * Strategy interface — every optimization rule must implement this.
 *
 * Design principles:
 *  - Each Rule is self-contained. All algorithm logic lives inside the Rule class only.
 *  - RuleEngine calls apply() without knowing implementation details.
 *  - Adding a new rule = create a new class implementing Rule + register it in RuleRegistryConfig.
 *    No other files need to change.
 *
 * Execution contract:
 *  - apply() must NEVER throw a checked exception. Wrap errors in the StepResult with status="error".
 *  - apply() must always return a non-null StepResult.
 *  - If inputText is null, treat it as an empty string.
 */
public interface Rule {

    /** Unique camelCase ID used as the key in the request JSON (e.g. "inputCleaner"). */
    String getRuleId();

    /** Human-readable display name (e.g. "Input Cleaner"). */
    String getRuleName();

    /** Grouping label returned to the UI (e.g. "Level 1" or "Level 2"). */
    String getRuleLevel();

    /** Short description shown in the UI below the rule name. */
    String getDescription();

    /**
     * Apply this rule's transformation to {@code inputText}.
     *
     * @param inputText  the text as it enters this step of the pipeline
     * @param config     the rule config from the request (enabled=true guaranteed by RuleEngine)
     * @return           a fully-populated StepResult (status will be set by RuleEngine)
     */
    StepResult apply(String inputText, RuleConfig config);
}
