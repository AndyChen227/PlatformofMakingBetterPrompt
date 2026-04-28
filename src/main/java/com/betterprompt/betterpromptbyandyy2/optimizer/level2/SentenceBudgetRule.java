package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Limits prompt length by sentence count.
 *
 * Scope boundary:
 *   This rule keeps the first N complete sentences only.
 *   It does not limit word count; LengthControlRule is the final hard word-budget guard.
 */
public class SentenceBudgetRule implements Rule {

    private static final int DEFAULT_MAX_SENTENCES = 3;
    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?");

    @Override public String getRuleId()      { return "sentenceBudget"; }
    @Override public String getRuleName()    { return "Sentence Budget"; }
    @Override public String getRuleLevel()   { return "Level 2"; }
    @Override public String getDescription() { return "Limits prompt length by maximum sentence count"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        int maxSentences = config.getIntParam("maxSentences", DEFAULT_MAX_SENTENCES);
        if (maxSentences <= 0) maxSentences = DEFAULT_MAX_SENTENCES;

        String result = inputText;
        String changeMsg;

        if (inputText.trim().isEmpty()) {
            changeMsg = "[sentenceBudget] 输入为空，无需处理";
        } else {
            List<String> sentences = splitSentences(inputText);
            int sentenceCount = sentences.size();

            if (sentenceCount <= maxSentences) {
                changeMsg = "[sentenceBudget] 句子数 " + sentenceCount + " ≤ maxSentences " + maxSentences + "，无需截断";
            } else {
                result = String.join(" ", sentences.subList(0, maxSentences));
                if (result.endsWith(".") || result.endsWith("?") || result.endsWith("!")) {
                    result = result + " ...";
                } else {
                    result = result + "...";
                }
                changeMsg = "[sentenceBudget] 保留前 " + maxSentences + " 句 (原 " + sentenceCount + " 句)";
            }
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
        step.setChanges(List.of(changeMsg));
        return step;
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
}
