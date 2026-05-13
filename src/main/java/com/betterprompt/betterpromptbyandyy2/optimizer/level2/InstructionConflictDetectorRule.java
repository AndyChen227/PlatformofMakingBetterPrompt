package com.betterprompt.betterpromptbyandyy2.optimizer.level2;

import com.betterprompt.betterpromptbyandyy2.model.RuleConfig;
import com.betterprompt.betterpromptbyandyy2.model.StepResult;
import com.betterprompt.betterpromptbyandyy2.optimizer.Rule;
import com.betterprompt.betterpromptbyandyy2.optimizer.TokenCounter;
import com.betterprompt.betterpromptbyandyy2.optimizer.util.ProtectedTextProcessor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class InstructionConflictDetectorRule implements Rule {

    private static final Pattern SENTENCE_PATTERN = Pattern.compile("[^.!?]+[.!?]?");

    private static final List<InstructionPattern> INSTRUCTION_PATTERNS = List.of(
            new InstructionPattern(InstructionType.CONCISE, Pattern.compile(
                    "\\bbe\\s+concise\\b"
                            + "|\\bkeep\\s+it\\s+short\\b"
                            + "|\\bmake\\s+the\\s+answer\\s+brief\\b"
                            + "|\\banswer\\s+briefly\\b"
                            + "|\\bkeep\\s+the\\s+answer\\s+concise\\b"
                            + "|\\bkeep\\s+your\\s+response\\s+short\\b",
                    Pattern.CASE_INSENSITIVE)),
            new InstructionPattern(InstructionType.DETAILED, Pattern.compile(
                    "\\bbe\\s+detailed\\b"
                            + "|\\bgive\\s+a\\s+detailed\\s+answer\\b"
                            + "|\\bgive\\s+a\\s+detailed\\s+explanation\\b"
                            + "|\\bprovide\\s+a\\s+detailed\\s+explanation\\b"
                            + "|\\bexplain\\s+in\\s+detail\\b"
                            + "|\\bprovide\\s+a\\s+thorough\\s+explanation\\b"
                            + "|\\binclude\\s+enough\\s+detail\\b",
                    Pattern.CASE_INSENSITIVE)),
            new InstructionPattern(InstructionType.ONE_SENTENCE, Pattern.compile(
                    "\\banswer\\s+in\\s+(?:one|1)\\s+sentence\\b"
                            + "|\\buse\\s+(?:one|1)\\s+sentence\\b"
                            + "|\\brespond\\s+in\\s+(?:one|1)\\s+sentence\\b"
                            + "|\\bkeep\\s+it\\s+to\\s+(?:one|1)\\s+sentence\\b"
                            + "|\\blimit\\s+the\\s+answer\\s+to\\s+(?:one|1)\\s+sentence\\b"
                            + "|\\bin\\s+(?:one|1)\\s+sentence\\b",
                    Pattern.CASE_INSENSITIVE)),
            new InstructionPattern(InstructionType.STEP_BY_STEP, Pattern.compile(
                    "\\bexplain\\s+step\\s+by\\s+step\\b"
                            + "|\\bshow\\s+each\\s+step\\b"
                            + "|\\bwalk\\s+me\\s+through\\s+the\\s+process\\b"
                            + "|\\bprovide\\s+a\\s+step-by-step\\s+explanation\\b"
                            + "|\\bbreak\\s+it\\s+down\\s+step\\s+by\\s+step\\b"
                            + "|\\bstep-by-step\\b",
                    Pattern.CASE_INSENSITIVE)),
            new InstructionPattern(InstructionType.JSON, Pattern.compile(
                    "\\brespond\\s+in\\s+json\\b"
                            + "|\\breturn\\s+valid\\s+json\\b"
                            + "|\\boutput\\s+as\\s+json\\b"
                            + "|\\buse\\s+json\\s+format\\b"
                            + "|\\bjson\\s+only\\b"
                            + "|\\breturn\\s+json\\b",
                    Pattern.CASE_INSENSITIVE)),
            new InstructionPattern(InstructionType.MARKDOWN, Pattern.compile(
                    "\\buse\\s+markdown\\b"
                            + "|\\bformat\\s+in\\s+markdown\\b"
                            + "|\\brespond\\s+with\\s+markdown\\b"
                            + "|\\bmarkdown\\s+format\\b"
                            + "|\\boutput\\s+in\\s+markdown\\b"
                            + "|\\breturn\\s+markdown\\b",
                    Pattern.CASE_INSENSITIVE))
    );

    private static final List<ConflictPair> CONFLICT_PAIRS = List.of(
            new ConflictPair(InstructionType.CONCISE, InstructionType.DETAILED),
            new ConflictPair(InstructionType.ONE_SENTENCE, InstructionType.STEP_BY_STEP),
            new ConflictPair(InstructionType.JSON, InstructionType.MARKDOWN)
    );

    @Override public String getRuleId()      { return "instructionConflictDetector"; }
    @Override public String getRuleName()    { return "Instruction Conflict Detector"; }
    @Override public String getRuleLevel()   { return "Level 2"; }
    @Override public String getDescription() { return "Detects potentially conflicting output instructions without modifying the prompt"; }

    @Override
    public StepResult apply(String inputText, RuleConfig config) {
        if (inputText == null) inputText = "";
        int tokensBefore = TokenCounter.count(inputText);
        List<String> changes = new ArrayList<>();

        if (inputText.isEmpty()) {
            changes.add("[instructionConflictDetector] 输入为空，无需处理");
            return buildStep(inputText, tokensBefore, changes);
        }

        Map<InstructionType, String> hits = new EnumMap<>(InstructionType.class);
        ProtectedTextProcessor.transformOutsideMarkdownCode(
                inputText,
                normalText -> {
                    detectInstructions(normalText, hits);
                    return normalText;
                }
        );

        boolean hasConflict = false;
        for (ConflictPair conflictPair : CONFLICT_PAIRS) {
            if (hits.containsKey(conflictPair.left) && hits.containsKey(conflictPair.right)) {
                hasConflict = true;
                changes.add("[instructionConflictDetector] " + conflictPair.left
                        + " 命中: \"" + hits.get(conflictPair.left) + "\"");
                changes.add("[instructionConflictDetector] " + conflictPair.right
                        + " 命中: \"" + hits.get(conflictPair.right) + "\"");
                changes.add("[instructionConflictDetector] 检测到潜在冲突: "
                        + conflictPair.left + " 与 " + conflictPair.right);
            }
        }

        if (!hasConflict) {
            changes.add("[instructionConflictDetector] 未检测到明显指令冲突");
        }

        return buildStep(inputText, tokensBefore, changes);
    }

    private void detectInstructions(String text, Map<InstructionType, String> hits) {
        for (String sentence : splitSentences(text)) {
            for (InstructionPattern instructionPattern : INSTRUCTION_PATTERNS) {
                if (!hits.containsKey(instructionPattern.type)
                        && instructionPattern.pattern.matcher(sentence).find()) {
                    hits.put(instructionPattern.type, sentence);
                }
            }
        }
    }

    private List<String> splitSentences(String text) {
        String normalized = text.trim().replaceAll("\\s+", " ");
        List<String> sentences = new ArrayList<>();
        Matcher matcher = SENTENCE_PATTERN.matcher(normalized);
        while (matcher.find()) {
            String sentence = matcher.group().trim();
            if (!sentence.isEmpty()) {
                sentences.add(sentence);
            }
        }
        if (sentences.isEmpty() && !normalized.isEmpty()) {
            sentences.add(normalized);
        }
        return sentences;
    }

    private StepResult buildStep(String inputText, int tokensBefore, List<String> changes) {
        StepResult step = new StepResult();
        step.setRuleName(getRuleName());
        step.setRuleLevel(getRuleLevel());
        step.setInputText(inputText);
        step.setOutputText(inputText);
        step.setTokensBefore(tokensBefore);
        step.setTokensAfter(tokensBefore);
        step.setTokensSaved(0);
        step.setChanges(changes);
        return step;
    }

    private enum InstructionType {
        CONCISE,
        DETAILED,
        ONE_SENTENCE,
        STEP_BY_STEP,
        JSON,
        MARKDOWN
    }

    private record InstructionPattern(InstructionType type, Pattern pattern) {
    }

    private record ConflictPair(InstructionType left, InstructionType right) {
    }
}
