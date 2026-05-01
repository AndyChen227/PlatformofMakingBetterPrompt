package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.ArrayList;
import java.util.List;

public class DuplicatePhraseReducerRule implements Rule {

    private static final int DEFAULT_MAX_PHRASE_LENGTH = 3;
    private static final int MAX_SUPPORTED_PHRASE_LENGTH = 3;
    private static final int MAX_PASSES = 5;

    @Override public String getRuleId()      { return "duplicatePhraseReducer"; }
    @Override public String getRuleName()    { return "Duplicate Phrase Reducer"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Removes consecutive duplicated words or short phrases"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        List<String> changes = new ArrayList<>();

        if (inputText.isEmpty()) {
            changes.add("[duplicatePhraseReducer] 输入为空，无需处理");
            return buildStep(inputText, inputText, tokensBefore, changes);
        }

        int maxPhraseLength = config == null
                ? DEFAULT_MAX_PHRASE_LENGTH
                : config.getIntParam("maxPhraseLength", DEFAULT_MAX_PHRASE_LENGTH);
        maxPhraseLength = Math.max(1, Math.min(MAX_SUPPORTED_PHRASE_LENGTH, maxPhraseLength));
        boolean caseInsensitive = getBooleanParam(config, "caseInsensitive", true);

        ReductionResult reduction = reduceText(inputText, maxPhraseLength, caseInsensitive, changes);
        String result = reduction.changed ? reduction.text : inputText;

        if (reduction.removedCount > 0) {
            changes.add("[duplicatePhraseReducer] 共删除 " + reduction.removedCount + " 个重复短语");
        } else {
            changes.add("[duplicatePhraseReducer] 未检测到连续重复短语");
        }

        return buildStep(inputText, result, tokensBefore, changes);
    }

    private ReductionResult reduceText(
            String text,
            int maxPhraseLength,
            boolean caseInsensitive,
            List<String> changes
    ) {
        String[] tokens = text.trim().split("\\s+");
        List<String> outputTokens = new ArrayList<>();
        int removedCount = 0;
        boolean changed = false;

        List<String> sentenceTokens = new ArrayList<>();
        for (String token : tokens) {
            sentenceTokens.add(token);
            if (endsSentence(token)) {
                ReductionResult sentenceResult = reduceSentence(sentenceTokens, maxPhraseLength, caseInsensitive, changes);
                outputTokens.addAll(List.of(sentenceResult.text.split("\\s+")));
                removedCount += sentenceResult.removedCount;
                changed = changed || sentenceResult.changed;
                sentenceTokens.clear();
            }
        }

        if (!sentenceTokens.isEmpty()) {
            ReductionResult sentenceResult = reduceSentence(sentenceTokens, maxPhraseLength, caseInsensitive, changes);
            outputTokens.addAll(List.of(sentenceResult.text.split("\\s+")));
            removedCount += sentenceResult.removedCount;
            changed = changed || sentenceResult.changed;
        }

        return new ReductionResult(String.join(" ", outputTokens), changed, removedCount);
    }

    private ReductionResult reduceSentence(
            List<String> sentenceTokens,
            int maxPhraseLength,
            boolean caseInsensitive,
            List<String> changes
    ) {
        List<String> current = new ArrayList<>(sentenceTokens);
        int removedCount = 0;
        boolean changed = false;

        for (int pass = 0; pass < MAX_PASSES; pass++) {
            PassResult passResult = reduceOnePass(current, maxPhraseLength, caseInsensitive, changes);
            current = passResult.tokens;
            removedCount += passResult.removedCount;
            changed = changed || passResult.changed;

            if (!passResult.changed) {
                break;
            }
        }

        return new ReductionResult(String.join(" ", current), changed, removedCount);
    }

    private PassResult reduceOnePass(
            List<String> tokens,
            int maxPhraseLength,
            boolean caseInsensitive,
            List<String> changes
    ) {
        List<String> output = new ArrayList<>();
        int removedCount = 0;
        int i = 0;

        while (i < tokens.size()) {
            boolean matched = false;

            for (int n = maxPhraseLength; n >= 1; n--) {
                if (i + 2 * n > tokens.size()) {
                    continue;
                }

                if (ngramsMatch(tokens, i, n, caseInsensitive)) {
                    output.addAll(tokens.subList(i, i + n));
                    changes.add("[duplicatePhraseReducer] 删除重复短语: \"" + phraseForChange(tokens, i, n, caseInsensitive) + "\"");
                    removedCount++;
                    i += 2 * n;
                    matched = true;
                    break;
                }
            }

            if (!matched) {
                output.add(tokens.get(i));
                i++;
            }
        }

        return new PassResult(output, removedCount > 0, removedCount);
    }

    private boolean ngramsMatch(List<String> tokens, int start, int n, boolean caseInsensitive) {
        boolean hasComparableToken = false;

        for (int offset = 0; offset < n; offset++) {
            String left = normalizeToken(tokens.get(start + offset), caseInsensitive);
            String right = normalizeToken(tokens.get(start + n + offset), caseInsensitive);

            if (!left.isEmpty()) {
                hasComparableToken = true;
            }
            if (!left.equals(right)) {
                return false;
            }
        }

        return hasComparableToken;
    }

    private String phraseForChange(List<String> tokens, int start, int n, boolean caseInsensitive) {
        List<String> normalizedTokens = new ArrayList<>();
        for (int i = start; i < start + n; i++) {
            String normalized = normalizeToken(tokens.get(i), caseInsensitive);
            if (!normalized.isEmpty()) {
                normalizedTokens.add(normalized);
            }
        }
        return String.join(" ", normalizedTokens);
    }

    private String normalizeToken(String token, boolean caseInsensitive) {
        int start = 0;
        int end = token.length();

        while (start < end && isCommonBoundaryPunctuation(token.charAt(start))) {
            start++;
        }
        while (end > start && isCommonBoundaryPunctuation(token.charAt(end - 1))) {
            end--;
        }

        String normalized = token.substring(start, end);
        return caseInsensitive ? normalized.toLowerCase() : normalized;
    }

    private boolean isCommonBoundaryPunctuation(char c) {
        return c == ',' || c == '.' || c == '?' || c == '!'
                || c == ':' || c == ';' || c == '"' || c == '\''
                || c == '(' || c == ')';
    }

    private boolean endsSentence(String token) {
        int i = token.length() - 1;
        while (i >= 0 && isCommonBoundaryPunctuation(token.charAt(i)) && token.charAt(i) != '.'
                && token.charAt(i) != '?' && token.charAt(i) != '!') {
            i--;
        }
        return i >= 0 && (token.charAt(i) == '.' || token.charAt(i) == '?' || token.charAt(i) == '!');
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

    private static class PassResult {
        private final List<String> tokens;
        private final boolean changed;
        private final int removedCount;

        private PassResult(List<String> tokens, boolean changed, int removedCount) {
            this.tokens = tokens;
            this.changed = changed;
            this.removedCount = removedCount;
        }
    }
}
