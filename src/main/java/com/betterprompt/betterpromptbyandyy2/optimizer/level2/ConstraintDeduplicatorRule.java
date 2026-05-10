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

public class ConstraintDeduplicatorRule implements Rule {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?");

    private static final List<ConstraintPattern> CONSTRAINT_PATTERNS = List.of(
            new ConstraintPattern(ConstraintType.CONCISE, Pattern.compile(
                    "\\bbe\\s+concise\\b"
                            + "|\\bkeep\\s+it\\s+short\\b"
                            + "|\\bmake\\s+it\\s+brief\\b"
                            + "|\\bmake\\s+(?:the\\s+answer|your\\s+answer|the\\s+response)\\s+brief\\b"
                            + "|\\banswer\\s+briefly\\b"
                            + "|\\bkeep\\s+(?:the\\s+answer|your\\s+response|the\\s+response)\\s+(?:concise|short)\\b",
                    Pattern.CASE_INSENSITIVE)),
            new ConstraintPattern(ConstraintType.DETAILED, Pattern.compile(
                    "\\bbe\\s+detailed\\b"
                            + "|\\bgive\\s+a\\s+detailed\\s+answer\\b"
                            + "|\\bexplain\\s+in\\s+detail\\b"
                            + "|\\binclude\\s+enough\\s+detail\\b"
                            + "|\\bprovide\\s+a\\s+thorough\\s+explanation\\b",
                    Pattern.CASE_INSENSITIVE)),
            new ConstraintPattern(ConstraintType.STEP_BY_STEP, Pattern.compile(
                    "\\bexplain\\s+step\\s+by\\s+step\\b"
                            + "|\\bshow\\s+each\\s+step\\b"
                            + "|\\bwalk\\s+me\\s+through\\s+the\\s+process\\b"
                            + "|\\bprovide\\s+a\\s+step-by-step\\s+explanation\\b"
                            + "|\\bbreak\\s+it\\s+down\\s+step\\s+by\\s+step\\b",
                    Pattern.CASE_INSENSITIVE)),
            new ConstraintPattern(ConstraintType.SIMPLE, Pattern.compile(
                    "\\bkeep\\s+it\\s+simple\\b"
                            + "|\\bexplain\\s+simply\\b"
                            + "|\\buse\\s+simple\\s+language\\b"
                            + "|\\bmake\\s+it\\s+easy\\s+to\\s+understand\\b"
                            + "|\\bexplain\\s+like\\s+I\\s+am\\s+a\\s+beginner\\b",
                    Pattern.CASE_INSENSITIVE)),
            new ConstraintPattern(ConstraintType.EXAMPLES, Pattern.compile(
                    "\\bgive\\s+examples\\b"
                            + "|\\binclude\\s+examples\\b"
                            + "|\\bprovide\\s+examples\\b"
                            + "|\\bshow\\s+examples\\b"
                            + "|\\bgive\\s+me\\s+an\\s+example\\b",
                    Pattern.CASE_INSENSITIVE))
    );

    @Override public String getRuleId()      { return "constraintDeduplicator"; }
    @Override public String getRuleName()    { return "Constraint Deduplicator"; }
    @Override public String getRuleLevel()   { return "Level 2"; }
    @Override public String getDescription() { return "Removes repeated output constraints while keeping the first one"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        List<String> changes = new ArrayList<>();

        if (inputText.isEmpty()) {
            changes.add("[constraintDeduplicator] 输入为空，无需处理");
            return buildStep(inputText, inputText, tokensBefore, changes);
        }

        Set<ConstraintType> seenTypes = EnumSet.noneOf(ConstraintType.class);
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
            changes.add("[constraintDeduplicator] 共删除 " + removedCount[0] + " 个重复输出约束");
        } else {
            changes.add("[constraintDeduplicator] 未检测到重复输出约束");
        }

        return buildStep(inputText, result, tokensBefore, changes);
    }

    private ReductionResult deduplicateNormalText(
            String text,
            Set<ConstraintType> seenTypes,
            List<String> changes
    ) {
        List<String> sentences = splitSentences(text);
        if (sentences.isEmpty()) {
            return new ReductionResult(text, false, 0);
        }

        List<String> keptSentences = new ArrayList<>();
        int removedCount = 0;

        for (String sentence : sentences) {
            ConstraintType constraintType = detectConstraintType(sentence);
            if (constraintType == null) {
                keptSentences.add(sentence);
                continue;
            }

            if (seenTypes.add(constraintType)) {
                keptSentences.add(sentence);
                changes.add("[constraintDeduplicator] 保留约束 " + constraintType + ": \"" + sentence + "\"");
            } else {
                removedCount++;
                changes.add("[constraintDeduplicator] 删除重复约束 " + constraintType + ": \"" + sentence + "\"");
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

    private ConstraintType detectConstraintType(String sentence) {
        for (ConstraintPattern constraintPattern : CONSTRAINT_PATTERNS) {
            if (constraintPattern.pattern.matcher(sentence).find()) {
                return constraintPattern.type;
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

    private enum ConstraintType {
        CONCISE,
        DETAILED,
        STEP_BY_STEP,
        SIMPLE,
        EXAMPLES
    }

    private record ConstraintPattern(ConstraintType type, Pattern pattern) {
    }

    private record ReductionResult(String text, boolean changed, int removedCount) {
    }
}
