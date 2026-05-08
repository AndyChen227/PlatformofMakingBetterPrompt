package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;
import com.betterprompt.betterpromptbyandyy2.optimizer.util.ProtectedTextProcessor;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class OutputFormatDeduplicatorRule implements Rule {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?");

    private static final List<FormatPattern> FORMAT_PATTERNS = List.of(
            new FormatPattern(FormatType.BULLET_LIST, Pattern.compile(
                    "\\b(?:please\\s+)?use\\s+bullet\\s+points\\b"
                            + "|\\banswer\\s+(?:in|with)\\s+bullet\\s+points\\b"
                            + "|\\bgive\\s+me\\s+bullet\\s+points\\b"
                            + "|\\bformat\\s+as\\s+bullet\\s+points\\b"
                            + "|\\banswer\\s+as\\s+a\\s+list\\b"
                            + "|\\bgive\\s+me\\s+a\\s+list\\b",
                    Pattern.CASE_INSENSITIVE)),
            new FormatPattern(FormatType.NUMBERED_LIST, Pattern.compile(
                    "\\b(?:please\\s+)?use\\s+a\\s+numbered\\s+list\\b"
                            + "|\\banswer\\s+in\\s+a\\s+numbered\\s+list\\b"
                            + "|\\bformat\\s+as\\s+a\\s+numbered\\s+list\\b"
                            + "|\\bnumber\\s+each\\s+point\\b"
                            + "|\\borganize\\s+with\\s+numbers\\b",
                    Pattern.CASE_INSENSITIVE)),
            new FormatPattern(FormatType.TABLE, Pattern.compile(
                    "\\b(?:please\\s+)?use\\s+a\\s+table\\b"
                            + "|\\banswer\\s+in\\s+a\\s+table\\b"
                            + "|\\bformat\\s+as\\s+a\\s+table\\b"
                            + "|\\bput\\s+it\\s+in\\s+a\\s+table\\b"
                            + "|\\bmarkdown\\s+table\\b",
                    Pattern.CASE_INSENSITIVE)),
            new FormatPattern(FormatType.JSON, Pattern.compile(
                    "\\breturn\\b[^.!?]*\\bas\\s+json\\b"
                            + "|\\boutput\\b[^.!?]*\\bas\\s+json\\b"
                            + "|\\brespond\\s+in\\s+json\\b"
                            + "|\\buse\\s+json\\s+format\\b"
                            + "|\\bvalid\\s+json\\b",
                    Pattern.CASE_INSENSITIVE)),
            new FormatPattern(FormatType.MARKDOWN, Pattern.compile(
                    "\\buse\\s+markdown\\b"
                            + "|\\bformat\\s+in\\s+markdown\\b"
                            + "|\\brespond\\s+with\\s+markdown\\b"
                            + "|\\bmarkdown\\s+format\\b",
                    Pattern.CASE_INSENSITIVE)),
            new FormatPattern(FormatType.CODE_BLOCK, Pattern.compile(
                    "\\buse\\s+a\\s+code\\s+block\\b"
                            + "|\\bput\\s+code\\s+in\\s+a\\s+code\\s+block\\b"
                            + "|\\bwrap\\s+code\\s+in\\s+triple\\s+backticks\\b"
                            + "|\\bformat\\s+code\\s+as\\s+a\\s+code\\s+block\\b",
                    Pattern.CASE_INSENSITIVE))
    );

    @Override public String getRuleId()      { return "outputFormatDeduplicator"; }
    @Override public String getRuleName()    { return "Output Format Deduplicator"; }
    @Override public String getRuleLevel()   { return "Level 2"; }
    @Override public String getDescription() { return "Removes repeated output-format instructions while keeping the first one"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        List<String> changes = new ArrayList<>();
        Set<FormatType> seenTypes = EnumSet.noneOf(FormatType.class);
        int[] removedCount = {0};

        String result = ProtectedTextProcessor.transformOutsideMarkdownCode(
                inputText,
                normalText -> {
                    ReductionResult reduction = deduplicateNormalText(normalText, seenTypes, changes);
                    removedCount[0] += reduction.removedCount;
                    return reduction.changed ? preserveBoundaryWhitespace(normalText, reduction.text) : normalText;
                }
        );

        if (removedCount[0] > 0) {
            changes.add("[outputFormatDeduplicator] 共删除 " + removedCount[0] + " 个重复输出格式要求");
        } else {
            changes.add("[outputFormatDeduplicator] 未检测到重复输出格式要求");
        }

        return buildStep(inputText, result, tokensBefore, changes);
    }

    private ReductionResult deduplicateNormalText(
            String text,
            Set<FormatType> seenTypes,
            List<String> changes
    ) {
        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return new ReductionResult(text, false, 0);
        }

        List<String> keptSentences = new ArrayList<>();
        int removedCount = 0;

        for (String sentence : sentences) {
            FormatType formatType = detectFormatType(sentence);
            if (formatType == null) {
                keptSentences.add(sentence);
                continue;
            }

            if (seenTypes.add(formatType)) {
                keptSentences.add(sentence);
                changes.add("[outputFormatDeduplicator] 保留格式要求 " + formatType + ": \"" + sentence + "\"");
            } else {
                removedCount++;
                changes.add("[outputFormatDeduplicator] 删除重复格式要求 " + formatType + ": \"" + sentence + "\"");
            }
        }

        return removedCount > 0
                ? new ReductionResult(String.join(" ", keptSentences), true, removedCount)
                : new ReductionResult(text, false, 0);
    }

    private List<String> splitSentences(String text) {
        String normalized = text.trim().replaceAll("\\s+", " ");
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        if (sentences.isEmpty() && !normalized.isEmpty()) {
            sentences.add(normalized);
        }
        return sentences;
    }

    private FormatType detectFormatType(String sentence) {
        for (FormatPattern formatPattern : FORMAT_PATTERNS) {
            if (formatPattern.pattern.matcher(sentence).find()) {
                return formatPattern.type;
            }
        }
        return null;
    }

    private String preserveBoundaryWhitespace(String originalText, String reducedText) {
        int leadingEnd = 0;
        while (leadingEnd < originalText.length() && Character.isWhitespace(originalText.charAt(leadingEnd))) {
            leadingEnd++;
        }

        int trailingStart = originalText.length();
        while (trailingStart > leadingEnd && Character.isWhitespace(originalText.charAt(trailingStart - 1))) {
            trailingStart--;
        }

        return originalText.substring(0, leadingEnd) + reducedText + originalText.substring(trailingStart);
    }

    private StepResult buildStep(String inputText, String result, int tokensBefore, List<String> changes) {
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

    private enum FormatType {
        BULLET_LIST,
        NUMBERED_LIST,
        TABLE,
        JSON,
        MARKDOWN,
        CODE_BLOCK
    }

    private record FormatPattern(FormatType type, Pattern pattern) {
    }

    private record ReductionResult(String text, boolean changed, int removedCount) {
    }
}
