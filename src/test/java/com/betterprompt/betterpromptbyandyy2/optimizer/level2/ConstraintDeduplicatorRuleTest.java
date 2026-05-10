package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ConstraintDeduplicatorRuleTest {

    private final ConstraintDeduplicatorRule rule = new ConstraintDeduplicatorRule();

    @Test
    void removesConciseDuplicates() {
        String input = "Explain arrays. Be concise. Keep it short. Make the answer brief.";
        String expected = "Explain arrays. Be concise.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void removesDetailedDuplicates() {
        String input = "Explain arrays. Give a detailed answer. Explain in detail.";
        String expected = "Explain arrays. Give a detailed answer.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void removesStepByStepDuplicates() {
        String input = "Solve this. Explain step by step. Show each step.";
        String expected = "Solve this. Explain step by step.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void keepsDifferentConstraintTypes() {
        String input = "Explain arrays. Be concise. Give examples. Explain step by step.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(input);
    }

    @Test
    void preservesFencedCodeBlock() {
        String input = """
                ```text
                Be concise. Keep it short.
                ```

                Explain arrays. Be concise. Keep it short.
                """;
        String expected = """
                ```text
                Be concise. Keep it short.
                ```

                Explain arrays. Be concise.
                """;

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void preservesInlineCode() {
        String input = "Use `Keep it short.` as an exact phrase. Be concise. Keep it short.";
        String expected = "Use `Keep it short.` as an exact phrase. Be concise.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    private RuleConfig testConfig() {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of());
        return config;
    }
}
