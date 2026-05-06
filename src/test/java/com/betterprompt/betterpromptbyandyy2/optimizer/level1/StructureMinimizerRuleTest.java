package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class StructureMinimizerRuleTest {

    private final StructureMinimizerRule rule = new StructureMinimizerRule();

    @Test
    void preservesMultilineFencedCodeBlockExactly() {
        String input = """
                PLEASE   EXPLAIN   THIS   CODE!!!

                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                ```



                Thanks
                """;

        String output = rule.apply(input, new RuleConfig()).getOutputText();

        assertThat(output).contains("""
                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                ```
                """);
        assertThat(output).contains("PLEASE EXPLAIN THIS CODE!!!");
        assertThat(output).contains("Thanks");
    }

    @Test
    void protectedOnlyWhitespaceDoesNotReportCleanup() {
        String input = """
                ```java
                String  value   =  "twenty";


                ```
                """;

        String output = rule.apply(input, new RuleConfig()).getOutputText();

        assertThat(output).isEqualTo("""
                ```java
                String  value   =  "twenty";


                ```\
                """);
    }
}
