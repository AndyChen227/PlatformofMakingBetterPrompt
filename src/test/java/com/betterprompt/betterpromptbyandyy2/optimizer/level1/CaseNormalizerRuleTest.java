package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class CaseNormalizerRuleTest {

    private final CaseNormalizerRule rule = new CaseNormalizerRule();

    @Test
    void normalizesAllUppercaseText() {
        String output = rule.apply(
                "PLEASE EXPLAIN HOW ARRAYS WORK. GIVE ONE EXAMPLE.",
                testConfig()
        ).getOutputText();

        assertThat(output).isEqualTo("Please explain how arrays work. Give one example.");
    }

    @Test
    void preservesFencedCodeBlockExactly() {
        String input = """
                PLEASE EXPLAIN THIS CODE:

                ```java
                SYSTEM.OUT.PRINTLN("HELLO");
                ```
                """;
        String expected = """
                Please explain this code:

                ```java
                SYSTEM.OUT.PRINTLN("HELLO");
                ```
                """;

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void preservesInlineCodeExactly() {
        String input = "PLEASE EXPLAIN `SYSTEM.OUT.PRINTLN(\"HELLO\")` IN SIMPLE TERMS.";
        String expected = "Please explain `SYSTEM.OUT.PRINTLN(\"HELLO\")` in simple terms.";

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void uppercaseOnlyInsideCodeBlockDoesNotTriggerNormalization() {
        String input = """
                Explain this:

                ```java
                SYSTEM.OUT.PRINTLN("HELLO");
                ```
                """;

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(input);
    }

    @Test
    void mixedCaseTextDoesNotTriggerNormalization() {
        String input = "Explain REST API and JSON parsing.";

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).isEqualTo(input);
    }

    private RuleConfig testConfig() {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of(
                "uppercaseRatioThreshold", 0.9,
                "minLetters", 8
        ));
        return config;
    }
}
