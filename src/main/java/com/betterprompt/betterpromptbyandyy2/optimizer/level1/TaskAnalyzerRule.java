package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ============================================================
 *  Task Analyzer Rule — Keyword-based classification
 * ============================================================
 * Current implementation:
 *   Classifies task type via ordered keyword matching:
 *     Priority: DEBUG > CODING > EXPLAIN > WRITING > COMPARE > GENERAL
 *   Estimates complexity from word count:
 *     < 15 words → LOW,  15–40 → MEDIUM,  > 40 → HIGH
 *   Does NOT modify the input text — outputText == inputText.
 *   Classification result is recorded in the changes list only,
 *   pending downstream Rule integration for context passing.
 *
 * Future upgrades:
 *   - Replace keyword matching with a fine-tuned intent classifier
 *     (BERT/DistilBERT) for higher accuracy on ambiguous prompts
 *   - Complexity: factor in vocabulary diversity (type-token ratio),
 *     sub-task count, and domain-specific jargon in addition to length
 *   - Expose classification result to downstream rules via a shared
 *     context object so SemanticCompressor / LengthControl can adapt
 *   - Support multi-label classification (CODING + EXPLAIN simultaneously)
 * ============================================================
 */
public class TaskAnalyzerRule implements Rule {

    // ── Task-type keyword sets (priority order) ───────────────────────
    // More specific / actionable types checked first to avoid misclassification.
    private static final String[][] TASK_DEFINITIONS = {
        { "DEBUG",   "error", "bug", "fix", "not working", "issue", "exception",
                     "crash", "wrong", "broken", "fail", "doesn't work", "won't work" },
        { "CODING",  "write", "code", "function", "implement", "program", "class",
                     "algorithm", "method", "loop", "variable", "array", "syntax" },
        { "EXPLAIN", "explain", "what is", "how does", "describe", "understand",
                     "definition", "what are", "how do", "why does", "what does" },
        { "WRITING", "write an email", "write a letter", "write an essay",
                     "write a paragraph", "write an article", "draft", "blog post" },
        { "COMPARE", "compare", "difference", "vs", "versus", "better",
                     "pros and cons", "similarities", "which is" }
    };

    @Override public String getRuleId()      { return "taskAnalyzer"; }
    @Override public String getRuleName()    { return "Task Analyzer"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Classifies task type and complexity, appends metadata tag"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        String lower = inputText.toLowerCase();

        // ── Classify task type ────────────────────────────────────────
        String taskType       = "GENERAL";
        String matchedKeyword = null;

        outer:
        for (String[] def : TASK_DEFINITIONS) {
            String type = def[0];
            for (int i = 1; i < def.length; i++) {
                if (lower.contains(def[i])) {
                    taskType       = type;
                    matchedKeyword = def[i];
                    break outer;
                }
            }
        }

        // ── Classify complexity ───────────────────────────────────────
        // 使用真实词数而非 BPE token 数:复杂度判断的语义是"句子有多复杂",
        // 词数比 token 数更直观;阈值 15/40 历史上就是按词数定的。
        // tokensBefore 仍保留供前端显示真实 BPE token 数。
        int wordCount = TokenCounter.wordCount(inputText);
        String complexity;
        if (wordCount < 15)       complexity = "LOW";
        else if (wordCount <= 40) complexity = "MEDIUM";
        else                      complexity = "HIGH";

        // ── Changes list ──────────────────────────────────────────────
        List<String> changes = new ArrayList<>();
        if (matchedKeyword != null) {
            changes.add("检测到任务类型: " + taskType + "（匹配关键词: '" + matchedKeyword + "'）");
        } else {
            changes.add("检测到任务类型: GENERAL（无匹配关键词，使用默认值）");
        }
        changes.add("复杂度评估: " + complexity + "（词数: " + wordCount + "）");
        changes.add("任务分类完成，元数据仅用于内部记录");

        StepResult step = new StepResult();
        step.setRuleName(getRuleName());
        step.setRuleLevel(getRuleLevel());
        step.setInputText(inputText);
        step.setOutputText(inputText); // text unchanged — classification is metadata only
        step.setTokensBefore(tokensBefore);
        step.setTokensAfter(tokensBefore);
        step.setTokensSaved(0);
        step.setChanges(changes);
        return step;
    }
}
