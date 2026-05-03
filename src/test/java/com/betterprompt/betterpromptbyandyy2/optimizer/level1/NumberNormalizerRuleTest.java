package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class NumberNormalizerRuleTest {

    private final NumberNormalizerRule rule = new NumberNormalizerRule();

    @Test
    void convertsWrittenNumber() {
        String output = rule.apply("Give me twenty examples.", testConfig()).getOutputText();

        assertThat(output).isEqualTo("Give me 20 examples.");
    }

    @Test
    void convertsPercentage() {
        String output = rule.apply("Use fifty percent of the budget.", testConfig()).getOutputText();

        assertThat(output).isEqualTo("Use 50% of the budget.");
    }

    @Test
    void preservesFencedCodeBlock() {
        String input = """
                ```java
                String value = "twenty";
                ```

                Give me twenty examples.
                """;
        String expected = """
                ```java
                String value = "twenty";
                ```

                Give me 20 examples.
                """;

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void preservesInlineCode() {
        String input = "Use `String value = \"twenty\";` and give me twenty examples.";
        String expected = "Use `String value = \"twenty\";` and give me 20 examples.";

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void numberWordsOnlyInsideProtectedBlockDoNotChange() {
        String input = """
                ```text
                twenty percent
                ```
                """;

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(input);
    }

    private RuleConfig testConfig() {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of());
        return config;
    }
}
