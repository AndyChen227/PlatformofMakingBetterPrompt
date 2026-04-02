package com.betterprompt.betterpromptbyandyy2.model;

import java.util.Map;

/**
 * The incoming optimization request.
 * Contains the raw prompt and a map of rule-ID -> RuleConfig.
 *
 * Example JSON:
 * {
 *   "prompt": "Hello, can you please write a function...",
 *   "rules": {
 *     "inputCleaner":       { "enabled": true,  "params": { "aggressiveness": 50 } },
 *     "taskAnalyzer":       { "enabled": true,  "params": {} },
 *     "semanticCompressor": { "enabled": true,  "params": { "compressionLevel": 60 } },
 *     ...
 *   }
 * }
 */
public class OptimizationRequest {

    private String prompt;
    private Map<String, RuleConfig> rules;

    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }

    public Map<String, RuleConfig> getRules() { return rules; }
    public void setRules(Map<String, RuleConfig> rules) { this.rules = rules; }
}
