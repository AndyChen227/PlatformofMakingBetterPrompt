package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  MOCK Implementation — Redundancy Suppressor Rule
 * ============================================================
 * Current behaviour:
 *   Removes common closing / filler sentences that add no informational
 *   value to the prompt using fixed regex patterns:
 *     "I hope this helps"
 *     "please let me know"
 *     "feel free to ask"
 *     "don't hesitate to ask"
 *     "let me know if you have any questions"
 *     "if you have any questions"
 *     "thank you for your time"
 *     "thanks in advance"
 *
 * Future real algorithm should:
 *   - Use semantic similarity to detect paraphrases of these fillers
 *   - Identify and remove duplicate information across paragraphs
 *   - Detect over-specified constraints (e.g. "please make it short and brief and concise")
 *     and collapse to a single constraint
 *   - Remove content that is implied by the task type (e.g. "write correct code"
 *     in a code generation task)
 *   - Detect and remove topic sentences that merely re-state the previous paragraph
 * ============================================================
 */
public class RedundancySuppressorRule implements Rule {

    private static final List<String> FILLER_PATTERNS = List.of(
        "I hope this helps[.!]*",
        "please let me know[^.]*[.!]*",
        "feel free to ask[^.]*[.!]*",
        "don'?t hesitate to (ask|contact)[^.]*[.!]*",
        "let me know if you have any (questions|concerns|issues)[^.]*[.!]*",
        "if you have any (questions|concerns|issues)[^.]*[.!]*",
        "thank you for your time[.!]*",
        "thanks in advance[.!]*",
        "looking forward to your (response|reply|feedback)[^.]*[.!]*",
        "best regards[^.]*[.!]*",
        "kind regards[^.]*[.!]*",
        "hope that (makes sense|helps|clarifies)[^.]*[.!]*"
    );

    @Override public String getRuleId()      { return "redundancySuppressor"; }
    @Override public String getRuleName()    { return "Redundancy Suppressor"; }
    @Override public String getRuleLevel()   { return "Level 2"; }
    @Override public String getDescription() { return "Removes closing filler sentences that add no prompt value"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        String result = inputText;
        List<String> changes = new ArrayList<>();

        for (String patternStr : FILLER_PATTERNS) {
            Pattern p = Pattern.compile("\\b" + patternStr, Pattern.CASE_INSENSITIVE);
            String replaced = p.matcher(result).replaceAll("").strip();
            if (!replaced.equals(result.strip())) {
                // Extract the removed text for the change log
                String removed = p.matcher(result).results()
                        .map(mr -> mr.group().strip())
                        .findFirst()
                        .orElse(patternStr);
                changes.add("[MOCK] 删除结尾套话: \"" + removed + "\"");
                result = replaced;
            }
        }

        // Clean up double spaces and orphaned punctuation left after removal
        result = result.replaceAll("[ \t]{2,}", " ").strip();

        if (changes.isEmpty()) {
            changes.add("[MOCK] No closing filler phrases found");
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
}
