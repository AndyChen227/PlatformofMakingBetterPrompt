package com.betterprompt.betterpromptbyandyy2.optimizer.level1;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * NumberNormalizerRule — 将英文书写的数字转换为阿拉伯数字
 *
 * 当前实现：完整位权解析器
 *   1. 先处理百分比：fifty percent → 50%
 *   2. 识别连续英文数字词组，按位权计算数值：
 *      ones(1-19), tens(20-90), hundred(×100), thousand(×1000), million(×1000000)
 *   示例：two hundred and fifty three → 253
 *
 * 未来升级方向：
 *   1. 支持序数词（first, second → 1st, 2nd）
 *   2. 支持小数（one point five → 1.5）
 *   3. 保护代码块、引号内内容不被替换
 */
public class NumberNormalizerRule implements Rule {

    private static final Map<String, Long> ONES   = new LinkedHashMap<>();
    private static final Map<String, Long> TENS   = new LinkedHashMap<>();
    private static final Map<String, Long> SCALES = new LinkedHashMap<>();
    private static final Set<String> ALL_NUMBER_WORDS = new LinkedHashSet<>();

    static {
        ONES.put("zero",0L); ONES.put("one",1L); ONES.put("two",2L);
        ONES.put("three",3L); ONES.put("four",4L); ONES.put("five",5L);
        ONES.put("six",6L); ONES.put("seven",7L); ONES.put("eight",8L);
        ONES.put("nine",9L); ONES.put("ten",10L); ONES.put("eleven",11L);
        ONES.put("twelve",12L); ONES.put("thirteen",13L); ONES.put("fourteen",14L);
        ONES.put("fifteen",15L); ONES.put("sixteen",16L); ONES.put("seventeen",17L);
        ONES.put("eighteen",18L); ONES.put("nineteen",19L);
        TENS.put("twenty",20L); TENS.put("thirty",30L); TENS.put("forty",40L);
        TENS.put("fifty",50L); TENS.put("sixty",60L); TENS.put("seventy",70L);
        TENS.put("eighty",80L); TENS.put("ninety",90L);
        SCALES.put("hundred",100L); SCALES.put("thousand",1000L); SCALES.put("million",1000000L);
        ALL_NUMBER_WORDS.addAll(ONES.keySet());
        ALL_NUMBER_WORDS.addAll(TENS.keySet());
        ALL_NUMBER_WORDS.addAll(SCALES.keySet());
    }

    private static final Pattern NUMBER_PHRASE_PATTERN;
    private static final Pattern PERCENT_PATTERN;

    static {
        List<String> words = new ArrayList<>(ALL_NUMBER_WORDS);
        words.sort((a, b) -> b.length() - a.length());
        String wordGroup  = String.join("|", words);
        String connector  = "(?:\\s+(?:and\\s+)?|,\\s*)";
        String numPattern = "\\b(?:" + wordGroup + ")(?:" + connector + "(?:" + wordGroup + "))*\\b";
        NUMBER_PHRASE_PATTERN = Pattern.compile(numPattern, Pattern.CASE_INSENSITIVE);
        PERCENT_PATTERN = Pattern.compile(numPattern + "\\s+percent(?:age)?\\b", Pattern.CASE_INSENSITIVE);
    }

    @Override public String getRuleId()      { return "numberNormalizer"; }
    @Override public String getRuleName()    { return "Number Normalizer"; }
    @Override public String getRuleLevel()   { return "Level 1"; }
    @Override public String getDescription() { return "Converts written numbers and percentages to numeric form"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        String result = inputText;
        List<String> changes = new ArrayList<>();
        result = replaceMatches(result, PERCENT_PATTERN, changes, true);
        result = replaceMatches(result, NUMBER_PHRASE_PATTERN, changes, false);
        if (changes.isEmpty()) {
            changes.add("[numberNormalizer] 未检测到可转换的英文数字");
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

    private String replaceMatches(String text, Pattern pattern, List<String> changes, boolean isPercent) {
        Matcher m = pattern.matcher(text);
        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String matched = m.group();
            String numberPart = isPercent ? matched.replaceAll("(?i)\\s+percent(?:age)?$", "").trim() : matched;
            Long value = parseNumberPhrase(numberPart);
            if (value == null) { m.appendReplacement(sb, Matcher.quoteReplacement(matched)); continue; }
            String replacement = isPercent ? value + "%" : String.valueOf(value);
            changes.add("[numberNormalizer] '" + matched + "' → '" + replacement + "'");
            m.appendReplacement(sb, Matcher.quoteReplacement(replacement));
        }
        m.appendTail(sb);
        return sb.toString();
    }

    private Long parseNumberPhrase(String phrase) {
        String cleaned = phrase.toLowerCase()
            .replaceAll("\\band\\b", " ").replaceAll("[,]+", " ").replaceAll("\\s+", " ").trim();
        String[] tokens = cleaned.split(" ");
        long result = 0L, current = 0L;
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            if (ONES.containsKey(token))        { current += ONES.get(token); }
            else if (TENS.containsKey(token))   { current += TENS.get(token); }
            else if (token.equals("hundred"))   { current = (current == 0 ? 1 : current) * 100L; }
            else if (token.equals("thousand"))  { result += (current == 0 ? 1 : current) * 1000L; current = 0; }
            else if (token.equals("million"))   { result += (current == 0 ? 1 : current) * 1000000L; current = 0; }
            else return null;
        }
        return result + current;
    }
}
