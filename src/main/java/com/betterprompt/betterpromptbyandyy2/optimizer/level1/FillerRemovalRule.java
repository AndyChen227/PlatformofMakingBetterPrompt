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
 *  Filler Removal Rule — Unified social-filler cleaner (real algorithm)
 * ============================================================
 * Current implementation:
 *   Merges the former InputCleanerRule (opening fillers) and
 *   RedundancySuppressorRule (closing fillers) into one rule.
 *   Three tiers driven by the aggressiveness parameter (0–100):
 *
 *   LOW (0–30):
 *     • Opening: explicit greetings (hello / hi / hey / good morning …)
 *     • Closing: 3 most common closings
 *         "I hope this helps", "thanks in advance",
 *         "thank you for your time"
 *
 *   MEDIUM (31–70) — LOW +:
 *     • Opening: polite request openers
 *         (please / could you / can you / would you / I need you to …)
 *     • Closing: 5 more closings
 *         (please let me know, feel free to ask, don't hesitate to ask,
 *          let me know if you have any questions,
 *          if you have any questions)
 *
 *   HIGH (71–100) — MEDIUM +:
 *     • Opening: soft openers
 *         (I was wondering if / I'd like you to / I am reaching out …)
 *     • Middle: standalone filler words anywhere in the text
 *         (basically / essentially / literally / actually)
 *     • Closing: 4 more closings
 *         (looking forward to your response, best regards,
 *          kind regards, hope that makes sense)
 *
 *   All tiers capitalise the first letter after cleaning.
 *   Tiers are cumulative (MEDIUM includes LOW, HIGH includes MEDIUM).
 *
 * Scope boundary:
 *   This rule handles SOCIAL FILLER ONLY — greetings, polite openers,
 *   mid-text fillers, and closing remarks. It does NOT handle:
 *     - Verbose phrase compression (e.g. "in order to" → "to")        → SemanticCompressor
 *     - Output-format instruction symbols (e.g. "bullet points:" → "•") → FormatControl
 *   Decision rule: if the text can be deleted outright without loss of
 *   information, it belongs here; if it must be compressed to a shorter
 *   equivalent, it belongs to SemanticCompressor.
 *
 * Future upgrades:
 *   - NLP classifier to detect filler phrases anywhere, not just predefined patterns
 *   - Multilingual support: Chinese (你好/麻烦你), French (Bonjour/Pourriez-vous)
 *   - Context-aware mode: preserve filler in customer-service or formal-writing prompts
 *   - Semantic similarity to catch paraphrases of closing remarks not in the fixed list
 *   - Detect polite openers that span multiple sentences
 *   - Detect and remove duplicate information across paragraphs
 *   - Detect over-specified constraints (e.g. "please make it short and brief and concise")
 *     and collapse to a single constraint
 * ============================================================
 */
public class FillerRemovalRule implements Rule {

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

    // ── LOW tier closing patterns ─────────────────────────────────────
    private static final String[] CLOSING_LOW = {
        "\\bI hope this helps[.!]*",
        "\\bthanks in advance[.!]*",
        "\\bthank you for your time[.!]*"
    };

    // ── MEDIUM tier closing patterns (additions) ──────────────────────
    private static final String[] CLOSING_MEDIUM = {
        "\\bplease let me know[^.]*[.!]*",
        "\\bfeel free to ask[^.]*[.!]*",
        "\\bdon'?t hesitate to (ask|contact)[^.]*[.!]*",
        "\\blet me know if you have any (questions|concerns|issues)[^.]*[.!]*",
        "\\bif you have any (questions|concerns|issues)[^.]*[.!]*"
    };

    // ── HIGH tier closing patterns (additions) ────────────────────────
    private static final String[] CLOSING_HIGH = {
        "\\blooking forward to your (response|reply|feedback)[^.]*[.!]*",
        "\\bbest regards[^.]*[.!]*",
        "\\bkind regards[^.]*[.!]*",
        "\\bhope that (makes sense|helps|clarifies)[^.]*[.!]*"
    };

    @Override public String getRuleId()      { return "fillerRemoval"; }
    @Override public String getRuleName()    { return "Filler Removal"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Removes greetings, polite openers, mid-text fillers, and closing remarks"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        int aggressiveness = config.getIntParam("aggressiveness", 50);
        String tier = aggressiveness <= 30 ? "LOW" : aggressiveness <= 70 ? "MEDIUM" : "HIGH";

        String result = inputText;
        List<String> changes = new ArrayList<>();

        // ── 1. Opening cleaner: loop until no new matches ─────────────
        // Handles stacked greetings like "Hello! Hi there! Please..."
        boolean progress = true;
        while (progress) {
            progress = false;

            // LOW patterns — always applied
            for (String pat : PATTERNS_LOW) {
                String before = result;
                result = tryRemoveOpener(result, pat, tier, "删除强寒暄", changes);
                if (!result.equals(before)) progress = true;
            }

            // MEDIUM patterns — applied when tier is MEDIUM or HIGH
            if (!tier.equals("LOW")) {
                for (String pat : PATTERNS_MEDIUM) {
                    String before = result;
                    result = tryRemoveOpener(result, pat, tier, "删除软开头", changes);
                    if (!result.equals(before)) progress = true;
                }
            }

            // HIGH opener patterns — applied only when tier is HIGH
            if (tier.equals("HIGH")) {
                for (String pat : PATTERNS_HIGH_OPENERS) {
                    String before = result;
                    result = tryRemoveOpener(result, pat, tier, "删除软开头", changes);
                    if (!result.equals(before)) progress = true;
                }
            }
        }

        // ── 2. HIGH: remove mid-text filler words ─────────────────────
        // Match filler only as a whole word flanked by whitespace.
        if (tier.equals("HIGH")) {
            for (String filler : FILLER_WORDS) {
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

        // ── 3. Closing filler removal ─────────────────────────────────
        // Build cumulative closing pattern list based on tier.
        List<String> activeClosing = new ArrayList<>();
        for (String p : CLOSING_LOW) activeClosing.add(p);
        if (!tier.equals("LOW")) {
            for (String p : CLOSING_MEDIUM) activeClosing.add(p);
        }
        if (tier.equals("HIGH")) {
            for (String p : CLOSING_HIGH) activeClosing.add(p);
        }

        for (String patternStr : activeClosing) {
            Pattern p = Pattern.compile(patternStr, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(result);
            String replaced = p.matcher(result).replaceAll("").strip();
            if (!replaced.equals(result.strip())) {
                String removed = m.reset().results()
                        .map(mr -> mr.group().strip())
                        .findFirst()
                        .orElse(patternStr);
                changes.add("[aggressiveness=" + tier + "] 删除结尾套话: '" + removed + "'");
                result = replaced;
            }
        }

        // Clean up double spaces left after closing removal
        result = result.replaceAll("[ \\t]{2,}", " ").strip();

        // ── 4. Capitalise first letter after all cleaning ─────────────
        if (!result.isEmpty()) {
            result = Character.toUpperCase(result.charAt(0)) + result.substring(1);
        }

        if (changes.isEmpty()) {
            changes.add("[aggressiveness=" + tier + "] 未检测到可删除的填充语");
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
    private String tryRemoveOpener(String text, String patStr, String tier,
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
