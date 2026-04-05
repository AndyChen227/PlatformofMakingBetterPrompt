package com.betterprompt.betterpromptbyandyy2.api;

import com.betterprompt.betterpromptbyandyy2.model.QualityComparisonResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * POST /api/compare
 *
 * Accepts { originalPrompt, optimizedPrompt, tokensBefore, tokensAfter },
 * calls gpt-4o-mini to generate answers and score them, then returns
 * a QualityComparisonResult with scores, verdict, and optimizationScore.
 *
 * No new dependencies — uses Java 11 java.net.http.HttpClient throughout.
 */
@RestController
@RequestMapping("/api")
public class QualityComparisonController {

    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    @Value("${openai.api.key}")
    private String openaiApiKey;

    @PostMapping("/compare")
    public ResponseEntity<?> compare(@RequestBody Map<String, Object> body) {
        try {
            String originalPrompt  = (String) body.get("originalPrompt");
            String optimizedPrompt = (String) body.get("optimizedPrompt");
            int tokensBefore = ((Number) body.get("tokensBefore")).intValue();
            int tokensAfter  = ((Number) body.get("tokensAfter")).intValue();

            HttpClient client = HttpClient.newHttpClient();

            // ── Step 1: parallel answer generation ────────────────────────
            String answerSystem =
                "你是一个直接回答问题的助手。用自然流畅的语言回答，\n" +
                "不要使用任何 markdown 格式，不要用代码块、标题、编号、\n" +
                "加粗等符号。像和人对话一样，直接说清楚答案。";

            CompletableFuture<String> origFuture = CompletableFuture.supplyAsync(() -> {
                try { return callChat(client, answerSystem, originalPrompt); }
                catch (Exception e) { throw new RuntimeException(e); }
            });
            CompletableFuture<String> optFuture = CompletableFuture.supplyAsync(() -> {
                try { return callChat(client, answerSystem, optimizedPrompt); }
                catch (Exception e) { throw new RuntimeException(e); }
            });

            String originalAnswer  = origFuture.get();
            String optimizedAnswer = optFuture.get();

            // ── Step 2: quality scoring (six dimensions + natural summary) ──
            String system =
                "你是一个严格的 prompt 回答质量评估员，只返回 JSON，不输出任何其他内容。";

            String userMsg =
                "你是一个严格的 prompt 回答质量评估员。评分标准：\n" +
                "- 5分 = 合格，能回答问题但有明显不足\n" +
                "- 7分 = 良好，回答准确且基本完整\n" +
                "- 9分以上 = 优秀，必须有充分理由才能给\n" +
                "- 不要因为回答\"看起来很长很详细\"就给高分\n\n" +
                "请从三个维度分别评估原始回答和优化后回答：\n" +
                "1. 切题性（Relevance）：回答是否直接回应了问题核心\n" +
                "2. 信息密度（Density）：有效信息量与总字数的比值，冗余多则扣分\n" +
                "3. 表达清晰度（Clarity）：逻辑是否清晰，是否容易理解\n\n" +
                "原始Prompt: " + originalPrompt + "\n原始回答: " + originalAnswer + "\n\n" +
                "优化后Prompt: " + optimizedPrompt + "\n优化后回答: " + optimizedAnswer + "\n\n" +
                "同时写一段详细的自然语言分析（5-8句话，中文），内容必须包含：\n" +
                "第一，把原始 prompt 和优化后 prompt 的完整内容各用一句话复述一遍\n" +
                "第二，分别说明切题性、信息密度、表达清晰度三个维度，\n" +
                "      原始回答和优化后回答各自表现如何，要举出具体例子\n" +
                "第三，最后说明这次优化整体是否值得，理由是什么\n" +
                "不要用\"总的来说\"、\"综上所述\"这类开头，\n" +
                "不要用任何 markdown 格式，纯自然语言段落\n\n" +
                "严格按此 JSON 格式返回，不要有任何其他文字：\n" +
                "{\n" +
                "  \"relevanceBefore\": 数字,\n" +
                "  \"relevanceAfter\": 数字,\n" +
                "  \"densityBefore\": 数字,\n" +
                "  \"densityAfter\": 数字,\n" +
                "  \"clarityBefore\": 数字,\n" +
                "  \"clarityAfter\": 数字,\n" +
                "  \"naturalSummary\": \"中文总结\"\n" +
                "}";

            String scoreContent    = callChat(client, system, userMsg);
            String scoreJson       = extractJsonObject(scoreContent);

            int relevanceBefore = extractIntField(scoreJson, "relevanceBefore");
            int relevanceAfter  = extractIntField(scoreJson, "relevanceAfter");
            int densityBefore   = extractIntField(scoreJson, "densityBefore");
            int densityAfter    = extractIntField(scoreJson, "densityAfter");
            int clarityBefore   = extractIntField(scoreJson, "clarityBefore");
            int clarityAfter    = extractIntField(scoreJson, "clarityAfter");
            String naturalSummary = extractStringField(scoreJson, "naturalSummary");

            int originalScore  = (int) Math.round((relevanceBefore + densityBefore + clarityBefore) / 3.0);
            int optimizedScore = (int) Math.round((relevanceAfter  + densityAfter  + clarityAfter)  / 3.0);

            // ── Step 3: token efficiency gain (%) ─────────────────────────
            // efficiencyBefore = originalScore  / tokensBefore
            // efficiencyAfter  = optimizedScore / tokensAfter
            // optimizationScore = (efficiencyAfter - efficiencyBefore) / efficiencyBefore × 100
            double efficiencyBefore = tokensBefore > 0 ? (double) originalScore  / tokensBefore : 0;
            double efficiencyAfter  = tokensAfter  > 0 ? (double) optimizedScore / tokensAfter  : 0;

            double optimizationScore;
            if (efficiencyBefore > 0) {
                optimizationScore = Math.round(
                        ((efficiencyAfter - efficiencyBefore) / efficiencyBefore) * 10000.0
                ) / 100.0;
            } else {
                optimizationScore = 0.0;
            }

            String verdict;
            if      (optimizationScore >= 20) verdict = "显著提升";
            else if (optimizationScore >=  5) verdict = "轻微提升";
            else if (optimizationScore >= -5) verdict = "无明显变化";
            else                              verdict = "优化后变差";

            QualityComparisonResult result = new QualityComparisonResult();
            result.setOriginalPrompt(originalPrompt);
            result.setOptimizedPrompt(optimizedPrompt);
            result.setOriginalAnswer(originalAnswer);
            result.setOptimizedAnswer(optimizedAnswer);
            result.setOriginalScore(originalScore);
            result.setOptimizedScore(optimizedScore);
            result.setRelevanceScoreBefore(relevanceBefore);
            result.setRelevanceScoreAfter(relevanceAfter);
            result.setDensityScoreBefore(densityBefore);
            result.setDensityScoreAfter(densityAfter);
            result.setClarityScoreBefore(clarityBefore);
            result.setClarityScoreAfter(clarityAfter);
            result.setNaturalSummary(naturalSummary);
            result.setTokensBefore(tokensBefore);
            result.setTokensAfter(tokensAfter);
            result.setOptimizationScore(optimizationScore);
            result.setVerdict(verdict);

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error",
                    e.getCause() != null ? e.getCause().getMessage() : e.getMessage()));
        }
    }

    // ── HTTP helper ──────────────────────────────────────────────────────────

    private String callChat(HttpClient client, String system, String user) throws Exception {
        String requestBody = buildRequestBody(system, user);
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(OPENAI_URL))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + openaiApiKey)
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();
        HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() != 200) {
            throw new RuntimeException("OpenAI returned " + resp.statusCode() + ": " + resp.body());
        }
        return extractContent(resp.body());
    }

    private String buildRequestBody(String system, String user) {
        StringBuilder sb = new StringBuilder();
        sb.append("{\"model\":\"gpt-4o-mini\",\"messages\":[");
        if (system != null && !system.isEmpty()) {
            sb.append("{\"role\":\"system\",\"content\":\"").append(escJson(system)).append("\"},");
        }
        sb.append("{\"role\":\"user\",\"content\":\"").append(escJson(user)).append("\"}");
        sb.append("]}");
        return sb.toString();
    }

    // ── Response parsing ─────────────────────────────────────────────────────

    /** Extracts choices[0].message.content from the OpenAI JSON response body. */
    private String extractContent(String body) {
        int msgIdx     = body.indexOf("\"message\"");
        if (msgIdx < 0) throw new RuntimeException("No message field in response: " + body);
        int contentIdx = body.indexOf("\"content\":", msgIdx);
        if (contentIdx < 0) throw new RuntimeException("No content field in response: " + body);
        int start = body.indexOf('"', contentIdx + 10) + 1;
        int end   = jsonStringEnd(body, start);
        return unescJson(body.substring(start, end));
    }

    /**
     * Strips markdown code fences if present, then finds the outermost { … } object.
     * Handles responses like ```json\n{...}\n```.
     */
    private String extractJsonObject(String content) {
        // Strip markdown fences
        if (content.contains("```")) {
            int fence = content.indexOf("```");
            int nl    = content.indexOf('\n', fence);
            int end   = content.indexOf("```", nl + 1);
            if (nl >= 0 && end > nl) {
                content = content.substring(nl + 1, end).trim();
            }
        }
        int start = content.indexOf('{');
        int end   = content.lastIndexOf('}');
        return (start >= 0 && end > start) ? content.substring(start, end + 1) : content;
    }

    /** Finds the end index of a JSON string starting at {@code start} (after the opening quote). */
    private int jsonStringEnd(String s, int start) {
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') { i += 2; continue; }
            if (c == '"')  return i;
            i++;
        }
        return i;
    }

    /** Extracts the string value of a top-level JSON string field by name. */
    private String extractStringField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return "";
        int colon = json.indexOf(':', idx);
        if (colon < 0) return "";
        int start = json.indexOf('"', colon + 1) + 1;
        if (start <= 0) return "";
        int end = jsonStringEnd(json, start);
        return unescJson(json.substring(start, end));
    }

    /** Extracts the integer value of a top-level numeric JSON field by name. */
    private int extractIntField(String json, String field) {
        int idx = json.indexOf("\"" + field + "\"");
        if (idx < 0) return 5;
        int colon = json.indexOf(':', idx);
        int start = colon + 1;
        while (start < json.length() && (json.charAt(start) == ' ' || json.charAt(start) == '\t')) start++;
        int end = start;
        while (end < json.length() && Character.isDigit(json.charAt(end))) end++;
        return (end > start) ? Integer.parseInt(json.substring(start, end)) : 5;
    }

    // ── JSON string escape / unescape ────────────────────────────────────────

    private String escJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    private String unescJson(String s) {
        return s.replace("\\n",  "\n")
                .replace("\\r",  "\r")
                .replace("\\t",  "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
