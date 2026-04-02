package com.betterprompt.betterpromptbyandyy2.optimizer;

/**
 * Simple whitespace-based token counter.
 *
 * MOCK implementation — splits on whitespace and counts words.
 *
 * Future real implementation should:
 * - Use a proper BPE tokenizer (e.g. tiktoken or Anthropic's tokenizer)
 * - Count tokens as Claude actually counts them (subword units, not words)
 * - Handle special tokens, punctuation, and Unicode correctly
 * - Cache tokenizer initialization for performance
 */
public class TokenCounter {

    private TokenCounter() {}

    /**
     * Count the approximate number of tokens in {@code text}.
     * Current: word count (split on whitespace).
     *
     * @param text input string, may be null or blank
     * @return token count, always >= 0
     */
    public static int count(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }
}
