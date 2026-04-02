package com.betterprompt.betterpromptbyandyy2.generator;

import org.springframework.stereotype.Component;

/**
 * AI-backed prompt generator — currently a stub.
 *
 * TODO: Integrate Anthropic Claude API to generate prompts dynamically.
 *
 * Implementation plan:
 *   1. Inject anthropic.api.key from application.properties
 *   2. Build a system prompt: "Generate a realistic user prompt for task type X
 *      at verbosity level Y. Verbosity HIGH = full of greetings and filler words."
 *   3. Call POST https://api.anthropic.com/v1/messages with model claude-opus-4-6
 *   4. Parse the response content block and return the text
 *   5. Add error handling / retry with exponential back-off
 */
@Component
public class AiPromptGenerator {

    /**
     * Generate a prompt using Claude AI.
     *
     * @param taskType  one of CODING / EXPLAIN / DEBUG / WRITING / COMPARE
     * @param verbosity one of LOW / MEDIUM / HIGH
     * @return generated prompt text (currently a placeholder)
     */
    public String generate(String taskType, String verbosity) {
        // TODO: replace with real Anthropic Claude API call
        return "AI generation coming soon.";
    }
}
