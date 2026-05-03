package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PunctuationNormalizerRuleTest {

    private final PunctuationNormalizerRule rule = new PunctuationNormalizerRule();

    @Test
    void normalizesRepeatedPunctuationOutsideMarkdownCodeOnly() {
        String input = """
                Explain this!!!

                ```java
                System.out.println("Hello!!!");
                String path = "a....b";
                ```

                Then answer???? Use `value!!!` and `a....b`.
                """;

        String output = rule.apply(input, new RuleConfig()).getOutputText();

        assertThat(output).contains("Explain this!");
        assertThat(output).contains("Then answer? Use `value!!!` and `a....b`.");
        assertThat(output).contains("System.out.println(\"Hello!!!\");");
        assertThat(output).contains("String path = \"a....b\";");
    }

    @Test
    void keepsExistingBehaviorForPlainText() {
        String output = rule.apply("Wait!!!! Really???? Fine.....", new RuleConfig()).getOutputText();

        assertThat(output).isEqualTo("Wait! Really? Fine...");
    }
}
