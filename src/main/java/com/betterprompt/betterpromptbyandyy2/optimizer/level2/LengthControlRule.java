package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.List;

/**
 * ============================================================
 *  MOCK Implementation — Length Control Rule
 * ============================================================
 * Current behaviour:
 *   If the word count exceeds the "maxWords" parameter (default 50),
 *   the text is hard-truncated at the word boundary and "..." is appended.
 *   Words are split on whitespace.
 *
 * Future real algorithm should:
 *   - Summarise rather than truncate: use an extractive or abstractive
 *     summariser to preserve the most important content within the budget
 *   - Respect sentence boundaries — never cut mid-sentence
 *   - Prioritise the task description over background/context when trimming
 *   - Support token budget (not just word count) once real tokeniser is integrated
 *   - Provide a "preserve_last_N_words" option to keep conclusions/instructions
 * ============================================================
 */
public class LengthControlRule implements Rule {

    private static final int DEFAULT_MAX_WORDS = 50;

    @Override public String getRuleId()      { return "lengthControl"; }
    @Override public String getRuleName()    { return "Length Control"; }
    @Override public String getRuleLevel()   { return "Level 2"; }
    @Override public String getDescription() { return "Truncates text that exceeds the max-words budget"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        int maxWords = config.getIntParam("maxWords", DEFAULT_MAX_WORDS);
        if (maxWords <= 0) maxWords = DEFAULT_MAX_WORDS;

        String result = inputText;
        String changeMsg;

        if (tokensBefore > maxWords) {
            String[] words = inputText.trim().split("\\s+");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < maxWords && i < words.length; i++) {
                if (i > 0) sb.append(" ");
                sb.append(words[i]);
            }
            sb.append("...");
            result = sb.toString();
            changeMsg = "[MOCK] 截断至 " + maxWords + " 词 (原 " + tokensBefore + " 词)";
        } else {
            changeMsg = "[MOCK] 词数 " + tokensBefore + " ≤ maxWords " + maxWords + "，无需截断";
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
}
