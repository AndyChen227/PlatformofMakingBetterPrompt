package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;
import com.betterprompt.betterpromptbyandyy2.optimizer.util.ProtectedTextProcessor;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * ============================================================
 *  Semantic Compressor Rule — Tiered compression levels (real algorithm)
 * ============================================================
 * Current implementation:
 *   Three tiers driven by the compressionLevel parameter (0–100):
 *
 *   LOW (0–30) — 8 safe, fully-equivalent substitutions:
 *     "in order to"           → "to"
 *     "due to the fact that"  → "because"
 *     "at this point in time" → "now"
 *     "in the event that"     → "if"
 *     "a large number of"     → "many"
 *     "on a daily basis"      → "daily"
 *     "in the near future"    → "soon"
 *     "with regard to"        → "regarding"
 *
 *   MEDIUM (31–70) — LOW + 11 moderate substitutions:
 *     "as a result of"               → "because of"
 *     "in spite of the fact that"    → "although"
 *     "for the purpose of"           → "to"
 *     "with the exception of"        → "except"
 *     "in close proximity to"        → "near"
 *     "in terms of"                  → "regarding"
 *     "make a decision"              → "decide"
 *     "take into consideration"      → "consider"
 *     "come to the conclusion"       → "conclude"
 *     "it is important to note that" → "note that"
 *     "it is important to note"      → "note"
 *
 *   HIGH (71–100) — MEDIUM + 10 aggressive substitutions:
 *     "whether or not"      → "whether"
 *     "each and every"      → "every"
 *     "first and foremost"  → "first"
 *     "the fact that"       → "that"
 *     "in the process of"   → "while"
 *     "at the present time" → "currently"
 *     "has the ability to"  → "can"
 *     "is able to"          → "can"
 *     "very unique"         → "unique"
 *     "completely eliminate"→ "eliminate"
 *
 *   Tiers are cumulative (MEDIUM includes LOW, HIGH includes MEDIUM).
 *   Changes list shows the active tier and each substitution performed.
 *
 * Scope boundary:
 *   This rule handles VERBOSE-PHRASE → CONCISE-PHRASE substitution where both
 *   sides are semantically equivalent natural language. It does NOT handle:
 *     - Social fillers that can be deleted outright (greetings, polite
 *       openers, closing remarks)                      → FillerRemoval
 *     - Output-format instruction symbols              → FormatControl
 *   Decision rule: input and output must be fully semantically equivalent and
 *   only shorter — if the phrase can simply be deleted, it belongs to FillerRemoval.
 *
 * Future upgrades:
 *   - Use an LLM or sentence-transformers to detect arbitrary paraphrase
 *     pairs that carry identical meaning (not limited to a fixed list)
 *   - Handle coreference: replace repeated noun phrases with pronouns
 *   - Collapse redundant clauses: "X and also Y" → "X and Y"
 *   - Protect quoted strings, code blocks, and proper nouns from substitution
 *   - Apply compressionLevel continuously (not just 3 tiers) by scoring each
 *     substitution by token-saving vs. semantic-risk ratio
 * ============================================================
 */
public class SemanticCompressorRule implements Rule {

    // ── LOW tier: 8 safe, unambiguous substitutions ───────────────────
    private static final List<Map.Entry<String, String>> REPLACEMENTS_LOW = List.of(
        entry("in order to",               "to"),
        entry("due to the fact that",      "because"),
        entry("at this point in time",     "now"),
        entry("in the event that",         "if"),
        entry("a large number of",         "many"),
        entry("on a daily basis",          "daily"),
        entry("in the near future",        "soon"),
        entry("with regard to",            "regarding")
    );

    // ── MEDIUM tier: 11 moderate substitutions ────────────────────────
    // Longer/more specific patterns listed before shorter ones to avoid
    // partial matches (e.g. check "it is important to note that" before
    // "it is important to note").
    private static final List<Map.Entry<String, String>> REPLACEMENTS_MEDIUM = List.of(
        entry("as a result of",                      "because of"),
        entry("in spite of the fact that",           "although"),
        entry("for the purpose of",                  "to"),
        entry("with the exception of",               "except"),
        entry("in close proximity to",               "near"),
        entry("in terms of",                         "regarding"),
        entry("make a decision",                     "decide"),
        entry("take into consideration",             "consider"),
        entry("come to the conclusion",              "conclude"),
        entry("it is important to note that",        "note that"),
        entry("it is important to note",             "note")
    );

    // ── HIGH tier: 10 aggressive substitutions ────────────────────────
    private static final List<Map.Entry<String, String>> REPLACEMENTS_HIGH = List.of(
        entry("whether or not",            "whether"),
        entry("each and every",            "every"),
        entry("first and foremost",        "first"),
        entry("the fact that",             "that"),
        entry("in the process of",         "while"),
        entry("at the present time",       "currently"),
        entry("has the ability to",        "can"),
        entry("is able to",                "can"),
        entry("very unique",               "unique"),
        entry("completely eliminate",      "eliminate")
    );

    @Override public String getRuleId()      { return "semanticCompressor"; }
    @Override public String getRuleName()    { return "Semantic Compressor"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Replaces verbose phrases with concise equivalents"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);

        int compressionLevel = config.getIntParam("compressionLevel", 50);
        String tier = compressionLevel <= 30 ? "LOW" : compressionLevel <= 70 ? "MEDIUM" : "HIGH";

        // Build the cumulative active replacement list based on tier
        List<Map.Entry<String, String>> active = new ArrayList<>(REPLACEMENTS_LOW);
        if (!tier.equals("LOW"))  active.addAll(REPLACEMENTS_MEDIUM);
        if (tier.equals("HIGH"))  active.addAll(REPLACEMENTS_HIGH);

        List<String> changes = new ArrayList<>();
        String result = ProtectedTextProcessor.transformOutsideMarkdownCode(
                inputText,
                normalText -> applyReplacements(normalText, active, changes, tier)
        );

        if (changes.isEmpty()) {
            changes.add("[compressionLevel=" + tier + "] 未找到可替换的冗余词组");
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

    private static Map.Entry<String, String> entry(String k, String v) {
        return new AbstractMap.SimpleImmutableEntry<>(k, v);
    }

    private String applyReplacements(
            String text,
            List<Map.Entry<String, String>> replacements,
            List<String> changes,
            String tier
    ) {
        String result = text;
        for (Map.Entry<String, String> e : replacements) {
            String verbose    = e.getKey();
            String compressed = e.getValue();
            // Case-insensitive global replace; preserve surrounding context exactly
            String replaced   = result.replaceAll("(?i)" + Pattern.quote(verbose), compressed);
            if (!replaced.equals(result)) {
                changes.add("[compressionLevel=" + tier + "] '" + verbose + "' → '" + compressed + "'");
                result = replaced;
            }
        }
        return result;
    }
}
