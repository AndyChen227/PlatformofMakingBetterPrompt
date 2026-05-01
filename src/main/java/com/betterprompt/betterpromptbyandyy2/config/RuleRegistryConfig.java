package com.betterprompt.betterpromptbyandyy2.config;

import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.CaseNormalizerRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.DuplicatePhraseReducerRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.DuplicateSentenceRemoverRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.FillerRemovalRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.SemanticCompressorRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.NumberNormalizerRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.PunctuationNormalizerRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.StructureMinimizerRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level1.TaskAnalyzerRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level2.FormatControlRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level2.LengthControlRule;
import com.betterprompt.betterpromptbyandyy2.optimizer.level2.SentenceBudgetRule;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * ============================================================
 *  Rule Registry — the ONLY file you need to edit when adding a new Rule.
 * ============================================================
 *
 * The list order defines the execution order in RuleEngine.
 * Spring injects this List<Rule> bean into RuleEngine automatically.
 *
 * To add a new Rule:
 *   1. Create YourNewRule.java implementing Rule (anywhere under optimizer/)
 *   2. Add "new YourNewRule()" to the list below in the desired position
 *   Done — no other file changes required.
 *
 * To remove a Rule:
 *   1. Delete its entry from the list below
 *   Done.
 *
 * To reorder Rules:
 *   1. Move its entry in the list below
 *   Done.
 */
@Configuration
public class RuleRegistryConfig {

    @Bean
    public List<Rule> rules() {
        return List.of(
            // ── Level 1: Input Processing ──────────────────────────────
            new FillerRemovalRule(),      // Strip greetings, polite openers, mid-text fillers, and closing remarks
            new CaseNormalizerRule(),     // Normalize clearly all-uppercase prompts into sentence case
            new TaskAnalyzerRule(),       // Classify task type & complexity
            new SemanticCompressorRule(), // Replace verbose phrases
            new StructureMinimizerRule(),       // Collapse whitespace & blank lines
            new DuplicateSentenceRemoverRule(), // Remove fully duplicated complete sentences
            new DuplicatePhraseReducerRule(),   // Remove consecutive duplicated words or short phrases
            new PunctuationNormalizerRule(),    // 压缩重复标点，规范省略号
            new NumberNormalizerRule(),         // 英文数字词 → 阿拉伯数字

            // ── Level 2: Output Control ────────────────────────────────
            new SentenceBudgetRule(),     // Limit prompt by sentence count before hard word truncation
            new LengthControlRule(),      // Final hard word-budget guard
            new FormatControlRule()       // Compact formatting instructions

            // ── Future: Level 3 (add here without touching other files) ─
            // new ContextCompressorRule(),
            // new ConstraintNormalizerRule(),

            // ── Future: Level 4 ─────────────────────────────────────────
            // new LLMRewriteRule(),
        );
    }
}
