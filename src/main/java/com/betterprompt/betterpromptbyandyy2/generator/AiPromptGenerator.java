package com.betterprompt.betterpromptbyandyy2.generator;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * AiPromptGenerator — 使用 OpenAI gpt-4o-mini 动态生成测试用 prompt
 *
 * 当前实现：调用 OpenAI Chat Completions API，根据 taskType 和 verbosity
 *           生成模拟真实用户写法的 prompt 文本
 * 未来升级方向：
 *   1. 接入 Claude API（claude-opus-4-6），响应格式不同，需要适配
 *   2. 增加 retry + 指数退避，应对 429 限流
 *   3. 支持更多任务类型和 verbosity 维度
 */
@Component
public class AiPromptGenerator {

    /** OpenAI API 端点 */
    private static final String OPENAI_URL = "https://api.openai.com/v1/chat/completions";

    /** 从 application.properties 注入 openai.api.key，与 QualityComparisonController 保持一致 */
    @Value("${openai.api.key}")
    private String openaiApiKey;

    /**
     * 调用 OpenAI gpt-4o-mini 生成一条测试用 prompt。
     *
     * @param taskType  任务类型：CODING / EXPLAIN / DEBUG / WRITING / COMPARE
     * @param verbosity 冗余程度：LOW / MEDIUM / HIGH
     * @return 生成的 prompt 文本；异常时返回 "AI generation failed: " + 错误信息
     */
    public String generate(String taskType, String verbosity) {
        try {
            HttpClient client = HttpClient.newHttpClient();

            // system prompt：全英文，要求生成英文 prompt，并按 verbosity 级别控制措辞风格
            String system =
                "You are an assistant that generates sample prompts for testing purposes. " +
                "Always write the generated prompt in English. " +
                "Match the verbosity level: " +
                "HIGH = include excessive greetings and filler words (e.g. 'Hi there!', 'I was wondering if you could possibly...'); " +
                "MEDIUM = include moderate politeness (e.g. 'Please', 'Could you'); " +
                "LOW = concise and direct, no pleasantries. " +
                "Output only the prompt itself, no explanations or extra text.";

            // user message：全英文，指定任务类型和冗余程度
            String user = "Generate a " + taskType + " prompt with verbosity level " + verbosity + ".";

            // 构造请求体（手动拼接 JSON，无需引入第三方库）
            String requestBody = buildRequestBody(system, user);

            HttpRequest req = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + openaiApiKey)
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> resp = client.send(req, HttpResponse.BodyHandlers.ofString());

            // 非 200 时抛出异常，包含状态码和响应体
            if (resp.statusCode() != 200) {
                throw new RuntimeException(
                        "OpenAI returned " + resp.statusCode() + ": " + resp.body());
            }

            // 从响应中提取 choices[0].message.content
            return extractContent(resp.body());

        } catch (Exception e) {
            // 异常时返回错误信息，不抛出，保证调用方可以正常处理
            return "AI generation failed: " + e.getMessage();
        }
    }

    // ── 请求体构造 ────────────────────────────────────────────────────────────

    /**
     * 手动拼接 OpenAI Chat Completions 请求体 JSON。
     * 格式：{"model":"gpt-4o-mini","messages":[{"role":"system","content":"..."},{"role":"user","content":"..."}]}
     */
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

    // ── 响应解析 ─────────────────────────────────────────────────────────────

    /**
     * 从 OpenAI 响应 JSON 中提取 choices[0].message.content 的字符串值。
     * 实现方式与 QualityComparisonController.extractContent() 一致，避免引入 JSON 库。
     *
     * @param body OpenAI 返回的完整 JSON 字符串
     * @return content 字段的文本内容（已反转义）
     */
    private String extractContent(String body) {
        // 定位 "message" 字段
        int msgIdx = body.indexOf("\"message\"");
        if (msgIdx < 0) throw new RuntimeException("No message field in response: " + body);

        // 定位 "content" 字段
        int contentIdx = body.indexOf("\"content\":", msgIdx);
        if (contentIdx < 0) throw new RuntimeException("No content field in response: " + body);

        // 找到 content 值的起始引号之后的位置
        int start = body.indexOf('"', contentIdx + 10) + 1;
        int end   = jsonStringEnd(body, start);
        return unescJson(body.substring(start, end));
    }

    /**
     * 从字符串 s 的 start 位置（opening quote 之后）扫描到对应的 closing quote，
     * 正确处理 \\ 转义序列。
     */
    private int jsonStringEnd(String s, int start) {
        int i = start;
        while (i < s.length()) {
            char c = s.charAt(i);
            if (c == '\\') { i += 2; continue; } // 跳过转义字符
            if (c == '"')  return i;              // 找到结束引号
            i++;
        }
        return i;
    }

    // ── JSON 字符串转义 / 反转义 ──────────────────────────────────────────────

    /** 将普通字符串转义为 JSON 字符串内容（用于构造请求体）。 */
    private String escJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    /** 将 JSON 字符串内容反转义为普通字符串（用于解析响应体）。 */
    private String unescJson(String s) {
        return s.replace("\\n",  "\n")
                .replace("\\r",  "\r")
                .replace("\\t",  "\t")
                .replace("\\\"", "\"")
                .replace("\\\\", "\\");
    }
}
