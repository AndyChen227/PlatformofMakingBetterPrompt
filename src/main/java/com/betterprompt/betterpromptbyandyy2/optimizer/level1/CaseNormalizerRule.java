package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class CaseNormalizerRule implements Rule {

    private static final double DEFAULT_UPPERCASE_RATIO_THRESHOLD = 0.9;
    private static final int DEFAULT_MIN_LETTERS = 8;

    @Override public String getRuleId()      { return "caseNormalizer"; }
    @Override public String getRuleName()    { return "Case Normalizer"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Normalizes clearly all-uppercase prompts into sentence case"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        List<String> changes = new ArrayList<>();

        if (inputText.isEmpty()) {
            changes.add("[caseNormalizer] 输入为空，无需处理");
            return buildStep(inputText, inputText, tokensBefore, changes);
        }

        double uppercaseRatioThreshold = getDoubleParam(
                config,
                "uppercaseRatioThreshold",
                DEFAULT_UPPERCASE_RATIO_THRESHOLD
        );
        int minLetters = config == null
                ? DEFAULT_MIN_LETTERS
                : config.getIntParam("minLetters", DEFAULT_MIN_LETTERS);

        String result = inputText;
        if (shouldNormalize(inputText, uppercaseRatioThreshold, minLetters)) {
            result = toSentenceCase(inputText);
            changes.add("[caseNormalizer] 检测到明显全大写输入，已转换为 sentence case");
            changes.add("[caseNormalizer] uppercaseRatioThreshold=" + uppercaseRatioThreshold
                    + ", minLetters=" + minLetters);
        } else {
            changes.add("[caseNormalizer] 未检测到明显全大写输入，无需处理");
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

    private double getDoubleParam(RuleConfig config, String key, double defaultValue) {
        if (config == null || config.getParams() == null || !config.getParams().containsKey(key)) {
            return defaultValue;
        }

        Object val = config.getParams().get(key);
        if (val instanceof Number number) {
            return number.doubleValue();
        }

        try {
            return Double.parseDouble(val.toString());
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private boolean shouldNormalize(String text, double threshold, int minLetters) {
        int letterCount = 0;
        int uppercaseCount = 0;

        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c >= 'A' && c <= 'Z') {
                letterCount++;
                uppercaseCount++;
            } else if (c >= 'a' && c <= 'z') {
                letterCount++;
            }
        }

        if (letterCount < minLetters) {
            return false;
        }

        double uppercaseRatio = (double) uppercaseCount / letterCount;
        return uppercaseRatio >= threshold;
    }

    private String toSentenceCase(String text) {
        char[] chars = text.toLowerCase(Locale.ENGLISH).toCharArray();
        boolean capitalizeNextLetter = true;

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];
            if ((c >= 'a' && c <= 'z') && capitalizeNextLetter) {
                chars[i] = Character.toUpperCase(c);
                capitalizeNextLetter = false;
            } else if (c >= 'a' && c <= 'z') {
                capitalizeNextLetter = false;
            } else if (c == '.' || c == '?' || c == '!') {
                capitalizeNextLetter = true;
            }
        }

        return new String(chars);
    }
}
