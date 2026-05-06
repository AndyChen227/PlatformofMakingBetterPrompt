package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;
import com.betterprompt.betterpromptbyandyy2.optimizer.util.ProtectedTextProcessor;

import java.util.ArrayList;
import java.util.List;

/**
 * ============================================================
 *  Structure Minimizer Rule - Real Implementation
 * ============================================================
 * Current behaviour:
 *   1. Collapses multiple consecutive blank lines into a single newline
 *   2. Trims trailing whitespace from each line
 *   3. Collapses multiple consecutive spaces into a single space
 *   4. Strips leading/trailing whitespace from the whole text
 *
 * Scope boundary:
 *   This rule handles WHITESPACE characters only (spaces, tabs, newlines).
 *   It does NOT handle:
 *     - Repeated punctuation marks (!!! / ??? / ....) -> PunctuationNormalizer
 *
 * Future real algorithm should:
 *   - Detect and remove redundant structural markers:
 *       excessive markdown headers, duplicate section dividers (----)
 *   - Collapse deeply nested bullet lists into flatter structures
 *       when nesting adds no semantic value
 *   - Remove empty list items and empty markdown table cells
 *   - Detect copy-paste artefacts (duplicate paragraphs, repeated headers)
 *   - Optionally normalise all list markers to a single style (-, *, etc.)
 * ============================================================
 */
public class StructureMinimizerRule implements Rule {

    @Override public String getRuleId()      { return "structureMinimizer"; }
    @Override public String getRuleName()    { return "Structure Minimizer"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Removes redundant whitespace and normalises text structure"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        List<String> changes = new ArrayList<>();
        boolean[] trimmedTrailingWhitespace = {false};
        boolean[] collapsedBlankLines = {false};
        boolean[] collapsedRepeatedSpaces = {false};
        boolean[] strippedOuterWhitespace = {false};

        String result = ProtectedTextProcessor.transformOutsideMarkdownCode(
                inputText,
                normalText -> cleanNormalText(
                        normalText,
                        trimmedTrailingWhitespace,
                        collapsedBlankLines,
                        collapsedRepeatedSpaces
                )
        );

        String stripped = result.strip();
        if (!stripped.equals(result)) {
            strippedOuterWhitespace[0] = true;
            result = stripped;
        }

        if (trimmedTrailingWhitespace[0]) {
            changes.add("清理每行尾部空格");
        }
        if (collapsedBlankLines[0]) {
            changes.add("折叠多余空行");
        }
        if (collapsedRepeatedSpaces[0]) {
            changes.add("折叠多余空格");
        }
        if (strippedOuterWhitespace[0]) {
            changes.add("清理首尾空白");
        }

        if (changes.isEmpty()) {
            changes.add("Structure is already clean, no changes needed");
        }

        int tokensAfter = TokenCounter.count(result);

        StepResult step = new StepResult();
        step.setRuleName(getRuleName());
        step.setRuleLevel(getRuleLevel());
        step.setInputText(inputText);
        step.setOutputText(result);
        step.setTokensBefore(tokensBefore);
        step.setTokensAfter(tokensAfter);
        step.setTokensSaved(tokensBefore - tokensAfter);
        step.setChanges(changes);
        return step;
    }

    private String cleanNormalText(
            String text,
            boolean[] trimmedTrailingWhitespace,
            boolean[] collapsedBlankLines,
            boolean[] collapsedRepeatedSpaces
    ) {
        String result = text;

        String trimmedLines = result.lines()
                .map(String::stripTrailing)
                .reduce((a, b) -> a + "\n" + b)
                .orElse("");
        if (!trimmedLines.equals(result)) {
            trimmedTrailingWhitespace[0] = true;
            result = trimmedLines;
        }

        String collapsedNewlines = result.replaceAll("(\\r?\\n){3,}", "\n\n");
        if (!collapsedNewlines.equals(result)) {
            collapsedBlankLines[0] = true;
            result = collapsedNewlines;
        }

        String collapsedSpaces = result.replaceAll("[ \t]{2,}", " ");
        if (!collapsedSpaces.equals(result)) {
            collapsedRepeatedSpaces[0] = true;
            result = collapsedSpaces;
        }

        return result;
    }
}
