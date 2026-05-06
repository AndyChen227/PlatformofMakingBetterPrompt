package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ProtectedMarkdownPipelineTest {

    @Test
    void highRiskLevelOneRulesPreserveFencedAndInlineCodeExactly() {
        String input = """
                PLEASE EXPLAIN THIS CODE!!!

                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                String phrase = "in order to";
                String repeated = "step by step step by step";
                ```

                Use `SYSTEM.OUT.PRINTLN("HELLO!!!")` and `String value = "twenty";` as exact examples.

                I NEED TWENTY EXAMPLES?? Explain this step by step step by step because I want to understand in order to reduce tokens.
                """;

        String output = input;
        for (RuleInvocation invocation : pipeline()) {
            output = invocation.rule.apply(output, invocation.config).getOutputText();
        }

        assertThat(output).contains("""
                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                String phrase = "in order to";
                String repeated = "step by step step by step";
                ```
                """);
        assertThat(output).contains("`SYSTEM.OUT.PRINTLN(\"HELLO!!!\")`");
        assertThat(output).contains("`String value = \"twenty\";`");
    }

    private List<RuleInvocation> pipeline() {
        return List.of(
                new RuleInvocation(new CaseNormalizerRule(), config(Map.of(
                        "uppercaseRatioThreshold", 0.9,
                        "minLetters", 8
                ))),
                new RuleInvocation(new StructureMinimizerRule(), config(Map.of())),
                new RuleInvocation(new DuplicateSentenceRemoverRule(), config(Map.of(
                        "caseInsensitive", true,
                        "keepFirst", true
                ))),
                new RuleInvocation(new DuplicatePhraseReducerRule(), config(Map.of(
                        "maxPhraseLength", 3,
                        "caseInsensitive", true
                ))),
                new RuleInvocation(new PunctuationNormalizerRule(), config(Map.of())),
                new RuleInvocation(new NumberNormalizerRule(), config(Map.of())),
                new RuleInvocation(new SemanticCompressorRule(), config(Map.of(
                        "compressionLevel", 50
                )))
        );
    }

    private RuleConfig config(Map<String, Object> params) {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(params);
        return config;
    }

    private record RuleInvocation(Rule rule, RuleConfig config) {
    }
}
