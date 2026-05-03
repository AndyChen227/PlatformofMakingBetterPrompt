package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicatePhraseReducerRuleTest {

    private final DuplicatePhraseReducerRule rule = new DuplicatePhraseReducerRule();

    @Test
    void duplicatePhraseOutsideProtectedRegionReduces() {
        String output = rule.apply(
                "Explain this step by step step by step.",
                testConfig()
        ).getOutputText();

        assertThat(output).isEqualTo("Explain this step by step.");
    }

    @Test
    void duplicateWordOutsideProtectedRegionReduces() {
        String output = rule.apply(
                "Please explain explain arrays.",
                testConfig()
        ).getOutputText();

        assertThat(output).isEqualTo("Please explain arrays.");
    }

    @Test
    void fencedCodeBlockIsPreserved() {
        String input = """
                ```text
                step by step step by step
                ```

                Explain this step by step step by step.
                """;
        String expected = """
                ```text
                step by step step by step
                ```

                Explain this step by step.
                """;

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void inlineCodeIsPreserved() {
        String input = "Use `step by step step by step` as the exact phrase, then explain this step by step step by step.";
        String expected = "Use `step by step step by step` as the exact phrase, then explain this step by step.";

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void protectedOnlyDuplicatePhraseDoesNotChange() {
        String input = """
                ```text
                step by step step by step
                ```
                """;

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(input);
    }

    private RuleConfig testConfig() {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of(
                "maxPhraseLength", 3,
                "caseInsensitive", true
        ));
        return config;
    }
}
