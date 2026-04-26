package com.betterprompt.betterpromptbyandyy2.optimizer;

import com.knuddels.jtokkit.Encodings;
import com.knuddels.jtokkit.api.Encoding;
import com.knuddels.jtokkit.api.EncodingRegistry;
import com.knuddels.jtokkit.api.EncodingType;

/**
 * Token counter — backed by jtokkit (Java port of OpenAI's tiktoken).
 *
 * 当前实现:使用 o200k_base 编码器进行真实 BPE 分词。
 *   o200k_base 是 GPT-4o / GPT-4o-mini 实际使用的 BPE 词表,与本项目
 *   QualityComparisonController 和 AiPromptGenerator 调用的模型完全一致。
 *
 * 设计要点:
 *   - Encoding 实例线程安全,但创建昂贵,因此通过静态初始化块加载一次,
 *     全局单例复用,避免每次 count() 重复初始化。
 *   - count() 方法签名与旧实现完全一致 (static int count(String)),
 *     所有调用方零修改。
 *   - 新增 wordCount() 用于"按词数判断"的场景(任务复杂度、长度截断
 *     budget),与 token 数解耦。
 *
 * 未来升级方向:
 *   - 若项目后续切换到 Claude (Anthropic),将 ENCODING_TYPE 改为对应
 *     编码即可(目前 jtokkit 暂未提供 Claude 官方 BPE,届时需评估替代方案)
 *   - 增加 batch 接口,一次性 encode 多段文本,减少调用开销
 *   - 支持估算多模型 token 数对比(如同时返回 GPT-4o 和 GPT-3.5 的 token 数)
 */
public class TokenCounter {

    /** 当前使用的 BPE 编码类型,与 OpenAI gpt-4o / gpt-4o-mini 对齐 */
    private static final EncodingType ENCODING_TYPE = EncodingType.O200K_BASE;

    /** 全局单例 Encoding 实例。jtokkit 文档明确说明 Encoding 是线程安全的。 */
    private static final Encoding ENCODING;

    static {
        // 应用启动时一次性加载 BPE 词表(约 100-200ms 开销)
        EncodingRegistry registry = Encodings.newDefaultEncodingRegistry();
        ENCODING = registry.getEncoding(ENCODING_TYPE);
    }

    private TokenCounter() {}

    /**
     * 用真实 BPE 算法计算文本的 token 数。
     *
     * 行为兼容性:与旧版完全一致地处理 null / blank → 返回 0。
     * 调用方无需任何修改。
     *
     * @param text 输入文本,允许 null 或全空白
     * @return token 数,始终 >= 0
     */
    public static int count(String text) {
        if (text == null || text.isBlank()) return 0;
        return ENCODING.countTokens(text);
    }

    /**
     * 按空白分词的"词数",用于不需要 BPE 精度的场景:
     *   - 任务复杂度判断 (TaskAnalyzerRule):"句子有多复杂"用词数更直观
     *   - 长度截断 budget (LengthControlRule):用户传入的 maxWords 概念是词
     *
     * 行为完全等同于旧版 TokenCounter.count() 的实现,迁移到此方法以
     * 解耦 "BPE token 数" 与 "词数" 两种语义。
     *
     * @param text 输入文本,允许 null 或全空白
     * @return 词数,始终 >= 0
     */
    public static int wordCount(String text) {
        if (text == null || text.isBlank()) return 0;
        return text.trim().split("\\s+").length;
    }

    /**
     * 返回当前编码器名称,便于日志、调试和未来切换模型时排查问题。
     *
     * @return 编码器名,例如 "O200K_BASE"
     */
    public static String getEncodingName() {
        return ENCODING_TYPE.name();
    }
}
