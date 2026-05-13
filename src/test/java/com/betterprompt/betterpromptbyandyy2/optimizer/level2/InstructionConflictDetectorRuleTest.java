package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class InstructionConflictDetectorRuleTest {

    private final InstructionConflictDetectorRule rule = new InstructionConflictDetectorRule();

    @Test
    void detectsConciseVsDetailedConflict() {
        String input = "Explain recursion. Be concise. Give a detailed explanation.";

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).anySatisfy(change ->
                assertThat(change).contains("[instructionConflictDetector] CONCISE 命中", "Be concise."));
        assertThat(result.getChanges()).anySatisfy(change ->
                assertThat(change).contains("[instructionConflictDetector] DETAILED 命中", "Give a detailed explanation."));
        assertThat(result.getChanges()).contains(
                "[instructionConflictDetector] 检测到潜在冲突: CONCISE 与 DETAILED");
    }

    @Test
    void detectsOneSentenceVsStepByStepConflict() {
        String input = "Answer in 1 sentence. Explain step by step.";

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).anySatisfy(change ->
                assertThat(change).contains("[instructionConflictDetector] ONE_SENTENCE 命中", "Answer in 1 sentence."));
        assertThat(result.getChanges()).anySatisfy(change ->
                assertThat(change).contains("[instructionConflictDetector] STEP_BY_STEP 命中", "Explain step by step."));
        assertThat(result.getChanges()).contains(
                "[instructionConflictDetector] 检测到潜在冲突: ONE_SENTENCE 与 STEP_BY_STEP");
    }

    @Test
    void detectsJsonVsMarkdownConflict() {
        String input = "Respond in JSON. Use Markdown format.";

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).anySatisfy(change ->
                assertThat(change).contains("[instructionConflictDetector] JSON 命中", "Respond in JSON."));
        assertThat(result.getChanges()).anySatisfy(change ->
                assertThat(change).contains("[instructionConflictDetector] MARKDOWN 命中", "Use Markdown format."));
        assertThat(result.getChanges()).contains(
                "[instructionConflictDetector] 检测到潜在冲突: JSON 与 MARKDOWN");
    }

    @Test
    void detectsMultipleConflictPairsInOnePrompt() {
        String input = "Be concise. Give a detailed explanation. Answer in 1 sentence. "
                + "Explain step by step. Respond in JSON. Use Markdown format.";

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).contains(
                "[instructionConflictDetector] 检测到潜在冲突: CONCISE 与 DETAILED",
                "[instructionConflictDetector] 检测到潜在冲突: ONE_SENTENCE 与 STEP_BY_STEP",
                "[instructionConflictDetector] 检测到潜在冲突: JSON 与 MARKDOWN"
        );
    }

    @Test
    void doesNotModifyOutputTextWhenConflictExists() {
        String input = "Keep your response short. Provide a detailed explanation. Return JSON. Return Markdown.";

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).contains(
                "[instructionConflictDetector] 检测到潜在冲突: CONCISE 与 DETAILED",
                "[instructionConflictDetector] 检测到潜在冲突: JSON 与 MARKDOWN"
        );
    }

    @Test
    void returnsNoConflictForCompatibleInstructions() {
        String input = "Explain arrays. Be concise. Give examples.";

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).contains("[instructionConflictDetector] 未检测到明显指令冲突");
    }

    @Test
    void ignoresConflictsInsideFencedCodeBlock() {
        String input = """
                ```text
                Be concise. Give a detailed answer.
                Answer in 1 sentence. Explain step by step.
                Respond in JSON. Use Markdown format.
                ```

                Explain arrays.
                """;

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).contains("[instructionConflictDetector] 未检测到明显指令冲突");
    }

    @Test
    void ignoresConflictsInsideInlineCode() {
        String input = "Use `Respond in JSON. Use Markdown format.` as an exact phrase. Explain arrays.";

        StepResult result = rule.apply(input, testConfig());

        assertUnchanged(input, result);
        assertThat(result.getChanges()).contains("[instructionConflictDetector] 未检测到明显指令冲突");
    }

    private void assertUnchanged(String input, StepResult result) {
        assertThat(result.getOutputText()).isEqualTo(input);
        assertThat(result.getTokensSaved()).isZero();
        assertThat(result.getTokensBefore()).isEqualTo(result.getTokensAfter());
    }

    private RuleConfig testConfig() {
        RuleConfig config = new RuleConfig();
        config.setEnabled(true);
        config.setParams(Map.of());
        return config;
    }
}
