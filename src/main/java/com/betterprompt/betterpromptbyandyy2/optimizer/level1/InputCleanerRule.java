package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  Input Cleaner Rule — Tiered aggressiveness (real algorithm)
 * ============================================================
 * Current implementation:
 *   Three tiers driven by the aggressiveness parameter (0–100):
 *
 *   LOW (0–30) — removes only strong explicit greetings:
 *     • hello / hi / hey at the start
 *     • "Good morning / afternoon / evening" at the start
 *
 *   MEDIUM (31–70) — LOW + polite request openers:
 *     • please / could you / can you / would you at the start
 *     • "I was hoping you could" at the start
 *     • "I need you to" at the start
 *
 *   HIGH (71–100) — MEDIUM + soft openers + mid-text filler words:
 *     • "I was wondering if" at the start
 *     • "I'd like to / I would like to ask/you" at the start
 *     • "I am reaching out because" at the start
 *     • "I hope you don't mind" at the start
 *     • Filler words anywhere: basically / essentially / literally / actually
 *     • Loops back to re-check after each removal until nothing left
 *
 *   All tiers capitalise the first letter after cleaning.
 *   Changes list shows tier level and exactly what was removed.
 *
 * Future upgrades:
 *   - Train an NLP sequence tagger on filler-detection corpora for
 *     language-agnostic removal (handles zero-shot languages)
 *   - Detect polite openers that span multiple sentences
 *   - Preserve openers in customer-service / formal-writing contexts
 *     (classify domain before applying the rule)
 *   - Support multilingual patterns (Chinese: 你好 / 麻烦你;
 *     French: Bonjour / Pourriez-vous)
 * ============================================================
 */
public class InputCleanerRule implements Rule {

    // ── LOW tier: explicit greetings ─────────────────────────────────
    private static final String[] PATTERNS_LOW = {
        "^\\s*good\\s+(morning|afternoon|evening)[,!.\\s]+",
        "^\\s*hello[,!.\\s]+",
        "^\\s*hi[,!.\\s]+",
        "^\\s*hey[,!.\\s]+"
    };

    // ── MEDIUM tier: polite request openers ──────────────────────────
    // More specific patterns first to prevent partial matches.
    private static final String[] PATTERNS_MEDIUM = {
        "^\\s*i was hoping you could\\s+",
        "^\\s*i need you to\\s+",
        "^\\s*would you please\\s+",
        "^\\s*could you please\\s+",
        "^\\s*can you please\\s+",
        "^\\s*could you\\s+",
        "^\\s*can you\\s+",
        "^\\s*would you\\s+",
        "^\\s*please\\s+"
    };

    // ── HIGH tier: soft openers ───────────────────────────────────────
    private static final String[] PATTERNS_HIGH_OPENERS = {
        "^\\s*i am reaching out because\\s+",
        "^\\s*i hope you don'?t mind\\s+",
        "^\\s*i'?d like to ask\\s+",
        "^\\s*i would like to ask\\s+",
        "^\\s*i'?d like you to\\s+",
        "^\\s*i would like you to\\s+",
        "^\\s*i was wondering if\\s+"
    };

    // ── HIGH tier: mid-text standalone filler words ───────────────────
    private static final String[] FILLER_WORDS = {
        "basically", "essentially", "literally", "actually"
    };

    @Override public String getRuleId()      { return "inputCleaner"; }
    @Override public String getRuleName()    { return "Input Cleaner"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Removes greetings and filler openers from prompts"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        int aggressiveness = config.getIntParam("aggressiveness", 50);
        String tier = aggressiveness <= 30 ? "LOW" : aggressiveness <= 70 ? "MEDIUM" : "HIGH";

        String result = inputText;
        List<String> changes = new ArrayList<>();

        // ── Loop: keep removing openers until no more match ───────────
        // This handles stacked greetings like "Hello! Hi there! Please..."
        boolean progress = true;
        while (progress) {
            progress = false;

            // LOW patterns — always applied
            for (String pat : PATTERNS_LOW) {
                String before = result;
                result = tryRemove(result, pat, tier, "删除强寒暄", changes);
                if (!result.equals(before)) progress = true;
            }

            // MEDIUM patterns — applied when tier is MEDIUM or HIGH
            if (!tier.equals("LOW")) {
                for (String pat : PATTERNS_MEDIUM) {
                    String before = result;
                    result = tryRemove(result, pat, tier, "删除软开头", changes);
                    if (!result.equals(before)) progress = true;
                }
            }

            // HIGH opener patterns — applied only when tier is HIGH
            if (tier.equals("HIGH")) {
                for (String pat : PATTERNS_HIGH_OPENERS) {
                    String before = result;
                    result = tryRemove(result, pat, tier, "删除软开头", changes);
                    if (!result.equals(before)) progress = true;
                }
            }
        }

        // ── HIGH: remove mid-text filler words ────────────────────────
        // Match filler only as a whole word flanked by whitespace.
        if (tier.equals("HIGH")) {
            for (String filler : FILLER_WORDS) {
                // (?<=\s) and (?=\s) ensure the word is surrounded by spaces
                Pattern p = Pattern.compile(
                    "(?<=\\s)" + Pattern.quote(filler) + "(?=\\s)",
                    Pattern.CASE_INSENSITIVE
                );
                Matcher m = p.matcher(result);
                if (m.find()) {
                    result = m.replaceAll("").replaceAll("[ ]{2,}", " ").strip();
                    changes.add("[aggressiveness=" + tier + "] 删除 filler 词: '" + filler + "'");
                }
            }
        }

        // ── Capitalise first letter after all cleaning ────────────────
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }

        if (changes.isEmpty()) {
            changes.add("[aggressiveness=" + tier + "] 未检测到可删除的寒暄词或 filler 词");
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

    /**
     * Try to remove a single leading pattern match.
     * Returns the updated string (unchanged if pattern doesn't match).
     * Logs the removal to {@code changes} if something was removed.
     */
    private String tryRemove(String text, String patStr, String tier,
                             String label, List<String> changes) {
        Pattern p = Pattern.compile(patStr, Pattern.CASE_INSENSITIVE);
        Matcher m = p.matcher(text);
        if (m.find()) {
            String removed = m.group().strip();
            String updated = m.replaceFirst("").stripLeading();
            if (!removed.isEmpty()) {
                changes.add("[aggressiveness=" + tier + "] " + label + ": '" + removed + "'");
            }
            return updated;
        }
        return text;
    }
}
