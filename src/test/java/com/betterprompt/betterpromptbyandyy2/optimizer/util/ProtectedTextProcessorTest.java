package com.betterprompt.betterpromptbyandyy2.optimizer.util;

import org.junit.jupiter.api.Test;

import java.util.function.UnaryOperator;

import static org.assertj.core.api.Assertions.assertThat;

class ProtectedTextProcessorTest {

    private static final UnaryOperator<String> DANGEROUS_TRANSFORMER = text -> text
            .replaceAll("!{2,}", "!")
            .replaceAll("\\?{2,}", "?")
            .replaceAll("(?i)twenty", "20")
            .replace("in order to", "to")
            .replace("step by step step by step", "step by step");

    @Test
    void fencedCodeBlockIsPreservedWhileOutsideTextIsTransformed() {
        String input = """
                PLEASE!!!

                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                ```

                TWENTY??
                """;
        String expected = """
                PLEASE!

                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                ```

                20?
                """;

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void inlineCodeIsPreservedWhileOutsideTextIsTransformed() {
        String input = "Use `SYSTEM.OUT.PRINTLN(\"HELLO!!!\")` and give me TWENTY examples??";
        String expected = "Use `SYSTEM.OUT.PRINTLN(\"HELLO!!!\")` and give me 20 examples?";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void fullFrontendManualTestCodeBlockIsPreserved() {
        String input = """
                PLEASE EXPLAIN THIS CODE!!!

                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                String phrase = "in order to";
                String repeated = "step by step step by step";
                ```

                I NEED TWENTY EXAMPLES??
                """;
        String expected = """
                PLEASE EXPLAIN THIS CODE!

                ```java
                SYSTEM.OUT.PRINTLN("HELLO!!!");
                String value = "twenty";
                String phrase = "in order to";
                String repeated = "step by step step by step";
                ```

                I NEED 20 EXAMPLES?
                """;

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void fencedCodeBlockIsPreservedWhenTextPrecedesOpeningFenceOnSameLine() {
        String input = "Before TWENTY ```java\nString value = \"twenty\";\n``` after TWENTY??";
        String expected = "Before 20 ```java\nString value = \"twenty\";\n``` after 20?";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void oneLineFencedCodeBlockIsPreserved() {
        String input = "Before TWENTY ```java SYSTEM.OUT.PRINTLN(\"HELLO!!!\"); String value = \"twenty\"; ``` after TWENTY??";
        String expected = "Before 20 ```java SYSTEM.OUT.PRINTLN(\"HELLO!!!\"); String value = \"twenty\"; ``` after 20?";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void nullEmptyAndNullTransformerInputsAreReturnedUnchanged() {
        assertThat(ProtectedTextProcessor.transformOutsideMarkdownCode(null, DANGEROUS_TRANSFORMER)).isNull();
        assertThat(ProtectedTextProcessor.transformOutsideMarkdownCode("", DANGEROUS_TRANSFORMER)).isEmpty();

        String input = "TWENTY!!!";
        assertThat(ProtectedTextProcessor.transformOutsideMarkdownCode(input, null)).isSameAs(input);
    }
}
