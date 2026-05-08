package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class OutputFormatDeduplicatorRuleTest {

    private final OutputFormatDeduplicatorRule rule = new OutputFormatDeduplicatorRule();

    @Test
    void removesBulletListDuplicates() {
        String input = "Explain recursion. Please use bullet points. Answer as a list. Give me bullet points.";
        String expected = "Explain recursion. Please use bullet points.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void removesNumberedListDuplicates() {
        String input = "Compare TCP and UDP. Use a numbered list. Number each point. Organize with numbers.";
        String expected = "Compare TCP and UDP. Use a numbered list.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void removesTableDuplicates() {
        String input = "Summarize the data. Use a table. Format as a table. Put it in a table.";
        String expected = "Summarize the data. Use a table.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void removesJsonDuplicates() {
        String input = "Return the result as JSON. Use JSON format. The output should be valid JSON.";
        String expected = "Return the result as JSON.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void doesNotTreatTaskContentAsFormatInstruction() {
        String input = "Explain what a JSON object is. List three examples.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(input);
    }

    @Test
    void differentFormatTypesAreNotDeduplicatedAgainstEachOther() {
        String input = "Answer in bullet points. Return as JSON.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(input);
    }

    @Test
    void preservesFencedCodeBlock() {
        String input = """
                ```text
                Use bullet points. Answer as a list.
                ```

                Explain recursion. Use bullet points. Answer as a list.
                """;
        String expected = """
                ```text
                Use bullet points. Answer as a list.
                ```

                Explain recursion. Use bullet points.
                """;

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    @Test
    void preservesInlineCode() {
        String input = "Use `Answer as a list.` as an exact phrase. Use bullet points. Answer as a list.";
        String expected = "Use `Answer as a list.` as an exact phrase. Use bullet points.";

        assertThat(rule.apply(input, testConfig()).getOutputText()).isEqualTo(expected);
    }

    private RuleConfig testConfig() {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of());
        return config;
    }
}
