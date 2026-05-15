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

    private static final UnaryOperator<String> TRIMMING_SEMANTIC_TRANSFORMER =
            ProtectedTextProcessorTest::trimmingSemanticTransform;

    private static String trimmingSemanticTransform(String text) {
        String result = text
                .replaceFirst("^Please\\s+", "")
                .replace("in order to", "to")
                .replace("due to the fact that", "because")
                .replaceAll("\\s+", " ")
                .trim();
        return result.replaceFirst("^explain\\b", "Explain");
    }

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
    void doubleQuotedTextIsPreservedWhileOutsideTextIsTransformed() {
        String input = "Rewrite \"in order to get twenty examples??\" in order to get TWENTY examples??";
        String expected = "Rewrite \"in order to get twenty examples??\" to get 20 examples?";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void singleQuotedTextIsPreservedWithoutTreatingApostrophesAsQuotes() {
        String input = "Don't change 'step by step step by step' but change step by step step by step.";
        String expected = "Don't change 'step by step step by step' but change step by step.";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void curlyQuotedTextIsPreserved() {
        String input = "Keep \u201cin order to get twenty examples??\u201d and change TWENTY??";
        String expected = "Keep \u201cin order to get twenty examples??\u201d and change 20?";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void unclosedQuotedTextIsNotProtected() {
        String input = "Rewrite \"in order to get TWENTY examples??";
        String expected = "Rewrite \"to get 20 examples?";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, DANGEROUS_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void splitIntoSegmentsMarksQuotedTextAsProtected() {
        String input = "Before TWENTY \"do not change twenty\" after TWENTY";

        assertThat(ProtectedTextProcessor.splitIntoSegments(input))
                .extracting(ProtectedTextProcessor.Segment::getText,
                        ProtectedTextProcessor.Segment::isProtectedSegment)
                .containsExactly(
                        org.assertj.core.groups.Tuple.tuple("Before TWENTY ", false),
                        org.assertj.core.groups.Tuple.tuple("\"do not change twenty\"", true),
                        org.assertj.core.groups.Tuple.tuple(" after TWENTY", false)
                );
    }

    @Test
    void preservesSpaceBeforeDoubleQuotedTextAfterTransform() {
        String input = "Please explain this phrase: \"in order to make a decision\". Also explain in order to help.";
        String expected = "Explain this phrase: \"in order to make a decision\". Also explain to help.";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, TRIMMING_SEMANTIC_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void preservesSpaceBeforeSingleQuotedTextAfterTransform() {
        String input = "Please explain this phrase: 'due to the fact that'. Also explain due to the fact that this matters.";
        String expected = "Explain this phrase: 'due to the fact that'. Also explain because this matters.";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, TRIMMING_SEMANTIC_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void doesNotAddSpaceWhenOriginalHadNoSpace() {
        String input = "Look at(\"in order to\")";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, TRIMMING_SEMANTIC_TRANSFORMER);

        assertThat(output).isEqualTo(input);
    }

    @Test
    void preservesSpaceAfterQuotedTextWhenOriginalHadSpace() {
        String input = "\"due to the fact that\" should stay, due to the fact that outside should change.";
        String expected = "\"due to the fact that\" should stay, because outside should change.";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(input, TRIMMING_SEMANTIC_TRANSFORMER);

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void inlineCodeStillProtectedAndSpacingStable() {
        String input = "Please keep `in order to` unchanged and replace in order to outside.";
        String expected = "Please keep `in order to` unchanged and replace to outside.";

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(
                input,
                text -> text.replace("in order to", "to")
        );

        assertThat(output).isEqualTo(expected);
    }

    @Test
    void fencedCodeBlockStillProtectedAndSpacingStable() {
        String input = """
                Please keep this code:

                ```text
                in order to
                ```

                Please replace in order to outside.
                """;
        String expected = """
                Please keep this code:

                ```text
                in order to
                ```

                Please replace to outside.
                """;

        String output = ProtectedTextProcessor.transformOutsideMarkdownCode(
                input,
                text -> text.replace("in order to", "to")
        );

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
