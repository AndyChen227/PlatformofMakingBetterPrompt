package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class DuplicateSentenceRemoverRuleTest {

    private final DuplicateSentenceRemoverRule rule = new DuplicateSentenceRemoverRule();

    @Test
    void removesDuplicateSentencesOutsideProtectedRegionsOnly() {
        String input = """
                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                ```

                Explain arrays. Explain arrays.
                """;

        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).contains("""
                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                ```
                """);
        assertThat(output).contains("Explain arrays.");
        assertThat(output).doesNotContain("Explain arrays. Explain arrays.");
    }

    @Test
    void preservesInlineCodeWhileRemovingOutsideDuplicateSentence() {
        String input = "Use `SYSTEM.OUT.PRINTLN(\"HELLO!!!\")`. Explain arrays. Explain arrays.";
        String output = rule.apply(input, testConfig()).getOutputText();

        assertThat(output).contains("`SYSTEM.OUT.PRINTLN(\"HELLO!!!\")`");
        assertThat(output).contains("Explain arrays.");
        assertThat(output).doesNotContain("Explain arrays. Explain arrays.");
    }

    private RuleConfig testConfig() {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of(
                "caseInsensitive", true,
                "keepFirst", true
        ));
        return config;
    }
}
