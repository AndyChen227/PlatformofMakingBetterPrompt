package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class SemanticCompressorRuleTest {

    private final SemanticCompressorRule rule = new SemanticCompressorRule();

    @Test
    void lowTierPhraseOutsideProtectedRegionCompresses() {
        String output = rule.apply(
                "Explain in order to make it shorter.",
                configWithCompressionLevel(20)
        ).getOutputText();

        assertThat(output).isEqualTo("Explain to make it shorter.");
    }

    @Test
    void fencedCodeBlockIsPreserved() {
        String input = """
                ```text
                in order to
                ```

                Explain in order to make it shorter.
                """;
        String expected = """
                ```text
                in order to
                ```

                Explain to make it shorter.
                """;

        String output = rule.apply(input, configWithCompressionLevel(20)).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void inlineCodeIsPreserved() {
        String input = "Use `in order to` as the exact phrase, then explain in order to simplify it.";
        String expected = "Use `in order to` as the exact phrase, then explain to simplify it.";

        String output = rule.apply(input, configWithCompressionLevel(20)).getOutputText();

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void protectedOnlyPhraseDoesNotChange() {
        String input = """
                ```text
                due to the fact that
                ```
                """;

        String output = rule.apply(input, configWithCompressionLevel(20)).getOutputText();

        assertThat(output).isEqualTo(input);
    }

    @Test
    void mediumTierPhraseStillWorksOutsideProtectedRegion() {
        String output = rule.apply(
                "We need to make a decision today.",
                configWithCompressionLevel(50)
        ).getOutputText();

        assertThat(output).isEqualTo("We need to decide today.");
    }

    @Test
    void highTierPhraseStillWorksOutsideProtectedRegion() {
        String output = rule.apply(
                "The tool has the ability to reduce tokens.",
                configWithCompressionLevel(85)
        ).getOutputText();

        assertThat(output).isEqualTo("The tool can reduce tokens.");
    }

    private RuleConfig configWithCompressionLevel(int compressionLevel) {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of("compressionLevel", compressionLevel));
        return config;
    }
}
