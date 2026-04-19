package com.betterprompt.betterpromptbyandyy2.optimizer;

import com.betterprompt.betterpromptbyandyy2.model.OptimizationRequest;
import com.betterprompt.betterpromptbyandyy2.model.OptimizationResult;
import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Executes all registered Rules in order against the input prompt.
 *
 * Design: RuleEngine only knows about the Rule interface — never about any concrete Rule class.
 * The ordered list of Rules is injected by Spring from RuleRegistryConfig.
 *
 * To add a new rule: implement Rule, create a @Bean in RuleRegistryConfig. Done.
 * To remove a rule: remove its @Bean from RuleRegistryConfig. Done.
 * No changes to RuleEngine are ever needed.
 */
@Service
public class RuleEngine {

    private final List<Rule> rules;

    /** Spring injects the ordered List<Rule> bean defined in RuleRegistryConfig. */
    public RuleEngine(List<Rule> rules) {
        this.rules = rules;
    }

    /**
     * Run the full optimization pipeline.
     *
     * For each Rule:
     *  - If disabled in the request → add a "skipped" StepResult, pass text through unchanged.
     *  - If enabled → call rule.apply(), accumulate results.
     *
     * @param request the optimization request from the API controller
     * @return the full result with per-step breakdown and aggregate stats
     */
    public OptimizationResult optimize(OptimizationRequest request) {
        String currentText = request.getPrompt() != null ? request.getPrompt() : "";
        int originalTokens = TokenCounter.count(currentText);

        List<StepResult> steps = new ArrayList<>();
        Map<String, Integer> byRule = new LinkedHashMap<>();

        for (Rule rule : rules) {
            RuleConfig config = request.getRules() != null
                    ? request.getRules().get(rule.getRuleId())
                    : null;

            if (config == null || !config.isEnabled()) {
                StepResult skipped = buildSkippedStep(rule, currentText);
                steps.add(skipped);
                byRule.put(rule.getRuleName(), 0);
                continue;
            }

            StepResult step = rule.apply(currentText, config);
            step.setStatus("done");
            steps.add(step);
            byRule.put(rule.getRuleName(), Math.max(0, step.getTokensSaved()));
            currentText = step.getOutputText() != null ? step.getOutputText() : currentText;
        }

        int finalTokens = TokenCounter.count(currentText);
        double compressionRate = originalTokens > 0
                ? Math.round((1.0 - (double) finalTokens / originalTokens) * 1000.0) / 10.0
                : 0.0;

        OptimizationResult.TokenStats stats = new OptimizationResult.TokenStats();
        stats.setOriginal(originalTokens);
        stats.setFinalTokens(finalTokens);
        stats.setCompressionRate(compressionRate);
        stats.setByRule(byRule);

        OptimizationResult result = new OptimizationResult();
        result.setSteps(steps);
        result.setFinalPrompt(currentText);
        result.setTokenStats(stats);
        return result;
    }

    private StepResult buildSkippedStep(Rule rule, String currentText) {
        int tokens = TokenCounter.count(currentText);
        StepResult step = new StepResult();
        step.setRuleName(rule.getRuleName());
        step.setRuleLevel(rule.getRuleLevel());
        step.setInputText(currentText);
        step.setOutputText(currentText);
        step.setTokensBefore(tokens);
        step.setTokensAfter(tokens);
        step.setTokensSaved(0);
        step.setChanges(List.of("[SKIPPED] Rule is disabled"));
        step.setStatus("skipped");
        return step;
    }
}
