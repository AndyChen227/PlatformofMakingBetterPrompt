package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

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
            changes.add("[duplicateSentenceRemover] 输入为空，无需处理");
            return buildStep(inputText, inputText, tokensBefore, changes);
        }

        boolean caseInsensitive = getBooleanParam(config, "caseInsensitive", true);
        boolean keepFirst = getBooleanParam(config, "keepFirst", true);

        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(inputText);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }

        if (sentences.size() <= 1) {
            changes.add("[duplicateSentenceRemover] 未检测到重复句");
            return buildStep(inputText, inputText, tokensBefore, changes);
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
                changes.add("[duplicateSentenceRemover] 删除重复句: \"" + sentence + "\"");
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
                changes.add("[duplicateSentenceRemover] 删除重复句: \"" + sentence + "\"");
            }
        }

        String result = removedCount > 0 ? String.join(" ", keptSentences) : inputText;

        if (removedCount > 0) {
            changes.add("[duplicateSentenceRemover] 共删除 " + removedCount + " 个重复句");
        } else {
            changes.add("[duplicateSentenceRemover] 未检测到重复句");
        }

        return buildStep(inputText, result, tokensBefore, changes);
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
}
