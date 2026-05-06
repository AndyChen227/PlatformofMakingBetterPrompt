package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;
import com.betterprompt.betterpromptbyandyy2.optimizer.util.ProtectedTextProcessor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DuplicateSentenceRemoverRule implements Rule {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?");

    @Override public String getRuleId()      { return "duplicateSentenceRemover"; }
    @Override public String getRuleName()    { return "Duplicate Sentence Remover"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Removes fully duplicated sentences from the prompt"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        List<String> changes = new ArrayList<>();

        if (inputText.isEmpty()) {
            changes.add("[duplicateSentenceRemover] input is empty, no processing needed");
            return buildStep(inputText, inputText, tokensBefore, changes);
        }

        final boolean caseInsensitive = getBooleanParam(config, "caseInsensitive", true);
        final boolean keepFirst = getBooleanParam(config, "keepFirst", true);
        final int[] removedCount = {0};

        String result = ProtectedTextProcessor.transformOutsideMarkdownCode(
                inputText,
                normalText -> {
                    ReductionResult reduction = reduceNormalText(normalText, caseInsensitive, keepFirst, changes);
                    removedCount[0] += reduction.removedCount;
                    return reduction.changed ? preserveBoundaryWhitespace(normalText, reduction.text) : normalText;
                }
        );

        if (removedCount[0] > 0) {
            changes.add("[duplicateSentenceRemover] removed " + removedCount[0] + " duplicate sentence(s)");
        } else {
            changes.add("[duplicateSentenceRemover] no duplicate sentences detected");
        }

        return buildStep(inputText, result, tokensBefore, changes);
    }

    private ReductionResult reduceNormalText(
            String text,
            boolean caseInsensitive,
            boolean keepFirst,
            List<String> changes
    ) {
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(text);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        if (sentences.size() <= 1) {
            return new ReductionResult(text, false, 0);
        }

        List<String> keptSentences = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        int removedCount = 0;

        if (keepFirst) {
            for (String sentence : sentences) {
                String normalized = normalizeSentence(sentence, caseInsensitive);
                if (!seen.contains(normalized)) {
                    seen.add(normalized);
                    keptSentences.add(sentence);
                    continue;
                }

                removedCount++;
                changes.add("[duplicateSentenceRemover] removed duplicate sentence: \"" + sentence + "\"");
            }
        } else {
            for (int i = sentences.size() - 1; i >= 0; i--) {
                String sentence = sentences.get(i);
                String normalized = normalizeSentence(sentence, caseInsensitive);
                if (!seen.contains(normalized)) {
                    seen.add(normalized);
                    keptSentences.add(0, sentence);
                    continue;
                }

                removedCount++;
                changes.add("[duplicateSentenceRemover] removed duplicate sentence: \"" + sentence + "\"");
            }
        }

        return removedCount > 0
                ? new ReductionResult(String.join(" ", keptSentences), true, removedCount)
                : new ReductionResult(text, false, 0);
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

    private boolean getBooleanParam(RuleConfig config, String key, boolean defaultValue) {
        if (config == null || config.getParams() == null || !config.getParams().containsKey(key)) {
            return defaultValue;
        }

        Object val = config.getParams().get(key);
        if (val instanceof Boolean bool) {
            return bool;
        }
        return Boolean.parseBoolean(val.toString());
    }

    private String normalizeSentence(String sentence, boolean caseInsensitive) {
        String normalized = sentence.trim()
                .replaceAll("\\s+", " ")
                .replaceAll("[.!?]+$", "")
                .trim();

        if (caseInsensitive) {
            normalized = normalized.toLowerCase();
        }

        return normalized;
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

    private static class ReductionResult {
        private final String text;
        private final boolean changed;
        private final int removedCount;

        private ReductionResult(String text, boolean changed, int removedCount) {
            this.text = text;
            this.changed = changed;
            this.removedCount = removedCount;
        }
    }
}
