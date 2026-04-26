package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;
import java.util.ArrayList;
import java.util.List;

/**
 * PunctuationNormalizerRule — 压缩重复标点，规范省略号
 *
 * 当前实现：
 *   1. 连续感叹号（2个以上）→ 单个 !
 *   2. 连续问号（2个以上）→ 单个 ?
 *   3. 连续英文句点（4个以上）→ 标准省略号 ...
 *
 * Scope boundary:
 *   This rule handles PUNCTUATION characters only (! ? .).
 *   It does NOT handle:
 *     - Whitespace, blank lines, or trailing spaces  → StructureMinimizer
 *
 * 未来升级方向：
 *   1. 支持中文标点规范化（！！！→！）
 *   2. 处理混合标点（!? 或 ?!）→ 统一为 ?
 *   3. 保护代码块内的标点不被修改
 */
public class PunctuationNormalizerRule implements Rule {

    @Override public String getRuleId()      { return "punctuationNormalizer"; }
    @Override public String getRuleName()    { return "Punctuation Normalizer"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Removes repeated punctuation and normalises ellipses"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        String result = inputText;
        List<String> changes = new ArrayList<>();

        String afterExcl = result.replaceAll("!{2,}", "!");
        if (!afterExcl.equals(result)) {
            changes.add("[punctuationNormalizer] 压缩重复感叹号: '!!' → '!'");
            result = afterExcl;
        }

        String afterQuestion = result.replaceAll("\\?{2,}", "?");
        if (!afterQuestion.equals(result)) {
            changes.add("[punctuationNormalizer] 压缩重复问号: '??' → '?'");
            result = afterQuestion;
        }

        String afterEllipsis = result.replaceAll("\\.{4,}", "...");
        if (!afterEllipsis.equals(result)) {
            changes.add("[punctuationNormalizer] 规范省略号: '....' → '...'");
            result = afterEllipsis;
        }

        if (changes.isEmpty()) {
            changes.add("[punctuationNormalizer] 未检测到需要规范化的标点");
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
