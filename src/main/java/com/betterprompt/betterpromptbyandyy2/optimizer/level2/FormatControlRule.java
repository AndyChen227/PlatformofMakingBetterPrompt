package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  MOCK Implementation — Format Control Rule
 * ============================================================
 * Current behaviour:
 *   Performs simple string replacements to convert verbose formatting
 *   instructions into compact symbols:
 *     "bullet points:"   → "•"
 *     "- bullet point:"  → "•"
 *     "numbered list:"   → "1."
 *     "bold text:"       → "**"
 *
 * Future real algorithm should:
 *   - Detect implicit formatting intent ("list the following items")
 *     and convert to explicit compact notation
 *   - Normalise mixed list marker styles (-, *, 1., a.) to a single style
 *   - Remove redundant markdown that the target model will ignore
 *   - Convert verbose inline instructions ("write in bold") to markdown
 *   - Optionally strip all formatting if the target API doesn't support markdown
 * ============================================================
 */
public class FormatControlRule implements Rule {

    private static final List<Map.Entry<String, String>> FORMAT_REPLACEMENTS = List.of(
        new AbstractMap.SimpleImmutableEntry<>("bullet points:", "•"),
        new AbstractMap.SimpleImmutableEntry<>("- bullet point:", "•"),
        new AbstractMap.SimpleImmutableEntry<>("numbered list:",  "1."),
        new AbstractMap.SimpleImmutableEntry<>("bold text:",      "**"),
        new AbstractMap.SimpleImmutableEntry<>("italic text:",    "*"),
        new AbstractMap.SimpleImmutableEntry<>("code block:",     "```"),
        new AbstractMap.SimpleImmutableEntry<>("as follows:",     ":"),
        new AbstractMap.SimpleImmutableEntry<>("the following:",  ":")
    );

    @Override public String getRuleId()      { return "formatControl"; }
    @Override public String getRuleName()    { return "Format Control"; }
    @Override public String getRuleLevel()   { return "Level 2"; }
    @Override public String getDescription() { return "Converts verbose formatting instructions to compact symbols"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        String result = inputText;
        List<String> changes = new ArrayList<>();

        for (Map.Entry<String, String> entry : FORMAT_REPLACEMENTS) {
            String verbose    = entry.getKey();
            String compact    = entry.getValue();
            String replaced   = result.replaceAll("(?i)" + java.util.regex.Pattern.quote(verbose), compact);
            if (!replaced.equals(result)) {
                changes.add("[MOCK] \"" + verbose + "\" → \"" + compact + "\"");
                result = replaced;
            }
        }

        if (changes.isEmpty()) {
            changes.add("[MOCK] No verbose formatting instructions found");
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
