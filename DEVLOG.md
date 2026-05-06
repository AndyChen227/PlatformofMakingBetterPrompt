# BetterPrompt 开发日志

> 持续记录项目从 0 到 1 的搭建过程。
>
> 使用规则：
> - 每完成一个真实能力，补一条记录
> - 每遇到关键设计选择，记录在"决策日志"
> - 每发现风险或阻塞，记录在"风险与阻塞"
> - 所有新增或修改代码必须补充清晰中文注释

---

## 当前状态快照

（每次更新都要更新这一栏）

- 当前版本：v1.5.5
- 当前阶段：Protected Text Safety Layer 已扩展到高风险 Level 1 路径；Markdown fenced code blocks 和 inline code 会被 byte-for-byte 保留
- 已完成模块数：14/14（Level 1 + Level 2 + Quality Check + AI Generate + Sentence Budget + Duplicate Sentence Remover + Case Normalizer + Duplicate Phrase Reducer + Protected Text Safety Layer）
- 下一步：继续评估 quoted text、未 fenced JSON-like blocks、Markdown tables 等后续保护范围

---

## 项目里程碑

### ✅ v1.0.0 — 框架搭建（2026/3/25）

产出：
- 完整 Spring Boot 项目结构
- Rule 接口 + RuleEngine 流水线框架
- 7个规则类（全部 Mock 占位符实现）
- 基础前端页面可访问
- POST /api/optimize 接口跑通

关键设计：
- Strategy 模式：每个 Rule 是独立策略
- Chain of Responsibility：RuleEngine 串联所有规则
- RuleRegistryConfig：新增规则只改一个文件

状态：框架跑通，算法全是占位符

---

### ✅ v1.0.0 — 真实算法实现（2026/3/26）

产出：
- InputCleanerRule：aggressiveness 参数真实生效
  LOW=只删强寒暄 / MID=加删please类 / HIGH=加删软开头+filler词
- TaskAnalyzerRule：真实关键词分类
  CODING/EXPLAIN/DEBUG/WRITING/COMPARE
  复杂度按词数判断：<15=LOW / 15-40=MEDIUM / >40=HIGH
- SemanticCompressorRule：compressionLevel 参数真实生效
  LOW=8组 / MID=19组 / HIGH=29组替换规则
- StructureMinimizerRule：四步清理逻辑（已是最完整实现）
- LengthControlRule：maxWords 参数真实截断
- FormatControlRule：8组格式符替换
- RedundancySuppressorRule：12种结尾废话正则匹配

状态：所有参数真实生效，算法有真实逻辑

---

### ✅ v1.1.0 — Prompt Generator（2026/3/27）

产出：
- TemplatePromptGenerator：45+模板，覆盖5种任务x3种废话程度
- AiPromptGenerator：占位符，TODO接入Claude API
- GET /api/generator/prompt 接口
- 前端 Generator 面板（Task Type + Verbosity 下拉 + Generate按钮）
- AI Generate 按钮置灰，hover显示 "Coming Soon"

状态：模板生成可用，AI生成待接入

---

### ✅ v1.2.0（tag v1.0.4） — UI 重构（2026/3/31）

产出：
- 经典 Google Material Design 风格
- 三页 SPA：Input & Config / Pipeline 详情 / Final Result
- Pipeline 每步展开显示：参数说明 + Before/After diff + 改动清单
- 右侧策略面板：每条规则可独立开关 + LOW/MID/HIGH 切换
- ℹ 弹窗：每条规则点击查看详细说明 + 示例 + 未来计划
- Token 统计卡片 + 贡献度柱状图（原生 canvas）

状态：UI 重构进行中

---

### ✅ v1.3.0 — Quality Check 功能（2026/4/4）

产出：
- `POST /api/compare` 接口
- `QualityComparisonController`：OpenAI gpt-4o-mini 并行回答生成 + 三维度打分（切题性/信息密度/表达清晰度）+ Token 效率比计算
- `QualityComparisonResult`：完整结果模型（6个维度分、综合分、naturalSummary、optimizationScore、verdict）
- 前端第四页 Quality Check：两列回答对比 + 维度进度条 + AI 自然语言分析卡片 + Token Efficiency Gain 展示
- 第三页由 "Final Result" 改名为 "Token Analysis"

状态：Quality Check 全链路上线，含后端 OpenAI 调用和前端可视化

---

### ✅ v1.4.0 — AI Generate 功能上线（2026/4/7）

产出：
- `AiPromptGenerator`：接入 OpenAI gpt-4o-mini，实现 `generate(taskType, verbosity)` 方法
  - 英文 system prompt，明确要求输出英文 prompt，按 HIGH/MEDIUM/LOW 控制冗余风格
  - 英文 user message：`"Generate a {taskType} prompt with verbosity level {verbosity}."`
  - 用 `java.net.http.HttpClient` 调用，复用与 `QualityComparisonController` 完全一致的 JSON 构造和解析逻辑
  - 异常时返回 `"AI generation failed: " + 错误信息`
- `AI Generate` 按钮激活：移除 `disabled`，加入 confirm 确认弹窗（`"Generate a prompt using AI? This may take a few seconds."`）
- 生成结果为英文 prompt，覆盖5种任务类型 × 3种 verbosity

状态：AI Generate 全链路上线，与 Quality Check 共享 OpenAI API key

---

### ✅ v1.4.1–v1.4.2 — Punctuation Normalizer + Number Normalizer（2026/4/15）

产出：
- `PunctuationNormalizerRule`：三步正则实现
  - `!{2,}` → `!` 压缩重复感叹号
  - `\?{2,}` → `?` 压缩重复问号
  - `\.{4,}` → `...` 规范省略号
- `NumberNormalizerRule`：完整位权解析器
  - 支持 ones / tens / hundred / thousand / million 任意组合
  - 两阶段处理：百分比优先（`fifty percent` → `50%`），再处理普通数字（`two hundred and fifty three` → `253`）
- 两条新 Rule 注册在 `RuleRegistryConfig.java`，位置：StructureMinimizerRule 之后、LengthControlRule 之前（执行顺序 5、6）
- 同步更新：PRODUCT.md / FEATURES.md（新增 2.5、2.6 章节）/ README.md / DEVLOG.md

状态：两条新 Rule 全链路上线

---

### ✅ v1.4.3 — 规则合并：FillerRemovalRule（2026/4/20）

产出：
- 新建 FillerRemovalRule（Level 1），合并原 InputCleanerRule（Level 1）和
  RedundancySuppressorRule（Level 2）
- 单参数 aggressiveness (0-100)，三档 LOW/MEDIUM/HIGH 累积式控制
  开头清理 + 中间 filler 清理 + 结尾客套清理
- 删除 InputCleanerRule.java 和 RedundancySuppressorRule.java 两个旧文件
- 在 SemanticCompressor / FormatControl / StructureMinimizer / PunctuationNormalizer
  四个类注释中新增 "Scope boundary" 段，明确各规则的职责边界
- 同步更新前端 index.html 和 app.js（卡片合并、state.rules、RULE_ORDER、RULE_INFO）
- 同步更新 PRODUCT.md / FEATURES.md / README.md

状态：规则总数 9 → 8，职责边界清晰化

---

### ✅ v1.5.0 — 真实 BPE Tokenizer 接入 + MOCK 标注清理（2026/4/25）

产出：
- `TokenCounter` 重写：接入 jtokkit 1.1.0（OpenAI tiktoken 的 Java 移植版），
  采用 `o200k_base` 编码器，与项目实际调用的 gpt-4o-mini 完全对齐
- 静态初始化块加载 `Encoding` 单例，应用启动时一次性建好 BPE 词表，
  后续 `count()` 调用零延迟、线程安全
- 新增 `TokenCounter.wordCount(String)`：保留原"按词数计数"语义，
  专供"逻辑判断"场景使用（任务复杂度判断、长度截断 budget），
  与 BPE token 数解耦
- 新增 `TokenCounter.getEncodingName()`：返回当前编码器名，便于调试
- `TaskAnalyzerRule`：复杂度判断（阈值 15/40）改用 `wordCount(inputText)`，
  避免 BPE token 数虚高导致复杂度标签偏移
- `LengthControlRule`：截断判断（maxWords budget）改用 `wordCount(inputText)`，
  避免 BPE token 数虚高误触发截断；`tokensBefore`/`tokensAfter` 仍用真实
  BPE 数值供前端显示
- 新增 Maven 依赖：`com.knuddels:jtokkit:1.1.0`（纯 Java、无 native 依赖）
- 验证：`"Hello world!"` 旧版报 2 词 → 新版报 3 token（`!` 单独切分），
  与 o200k_base 行为一致
- 清理全部残留 `[MOCK]` 标注：`LengthControlRule`、`FormatControlRule` 类头注释
  改为 "Real Implementation"，change 消息统一用规则 ID 前缀（`[lengthControl]`、
  `[formatControl]`）；FEATURES.md 同步清理 13 处 MOCK 标记
- 文档版本号体系统一至标准三段式 major.minor.patch（git tag 历史 v1.0.4 / v3.0 保留）

状态：BPE token 计数全链路上线，"逻辑用词数 + 显示用 BPE"语义干净分离

---

### ✅ v1.5.1 — SentenceBudgetRule 上线（2026/4/27）

产出：
- 新增 `SentenceBudgetRule`（Level 2），通过 `maxSentences` 参数控制最多保留句子数
- 默认 `maxSentences=3`，传入 ≤ 0 时自动回退为默认值
- 使用简单英文句子切分规则，句末标点包括 `.`, `?`, `!`
- 当句子数超过限制时，保留前 N 个完整句子并追加 `...`
- `RuleRegistryConfig` 中 Level 2 执行顺序更新为：`SentenceBudgetRule → LengthControlRule → FormatControlRule`
- `LengthControlRule` 保留为最终 maxWords 硬兜底规则，只更新注释和 description，不改变原有截断逻辑
- 前端同步新增 `sentenceBudget` 配置、`RULE_ORDER`、`RULE_LEVEL`、`RULE_INFO`
- `index.html` 新增 Sentence Budget 规则卡片和 `maxSentencesInput`
- Level 2 UI 规则数量从 2 rules 更新为 3 rules

状态：SentenceBudgetRule 全链路上线，后端 `/api/rules` 可返回 `sentenceBudget`，前端可配置并执行

---

### ✅ v1.5.2 — DuplicateSentenceRemoverRule 上线（2026/4/30）

产出：
- 新增 `DuplicateSentenceRemoverRule`（Level 1），用于删除 prompt 中完全重复的完整句子
- 默认忽略大小写进行重复判断（`caseInsensitive=true`）
- 默认保留第一次出现的句子（`keepFirst=true`）
- 使用简单英文句子切分规则，句末标点包括 `.`, `?`, `!`
- normalize 过程包括 trim、折叠多余空白、去除句尾标点、按配置转小写
- 使用 `LinkedHashSet` 保持原始句子顺序并检测重复
- `RuleRegistryConfig` 中 Level 1 执行顺序更新为：`StructureMinimizerRule → DuplicateSentenceRemoverRule → PunctuationNormalizerRule`
- 前端同步新增 `duplicateSentenceRemover` 配置、`RULE_ORDER`、`RULE_LEVEL`、`RULE_INFO`
- `index.html` 新增 Duplicate Sentence Remover 规则卡片
- Level 1 UI 规则数量从 6 rules 更新为 7 rules

验证：
- 输入：`Explain arrays. Explain arrays. Give one example.`
- 输出：`Explain arrays. Give one example.`
- `/api/rules` 可返回 `duplicateSentenceRemover`
- Page 1 / Page 2 / Page 3 均可正常显示该 rule

状态：DuplicateSentenceRemoverRule 全链路上线

---

### ✅ v1.5.3 — CaseNormalizerRule 上线（2026/4/30）

产出：
- 新增 `CaseNormalizerRule`（Level 1），用于保守修复明显全大写 prompt
- 默认 `uppercaseRatioThreshold=0.9`，只有大写英文字母比例足够高时才触发
- 默认 `minLetters=8`，避免短缩写如 SQL / API 被误处理
- 只统计英文字母，忽略数字、空格和标点
- 转换逻辑为 sentence case：整体转小写，再将文本开头和 `.`, `?`, `!` 后的第一个英文字母转为大写
- `RuleRegistryConfig` 中 Level 1 执行顺序更新为：`FillerRemovalRule → CaseNormalizerRule → TaskAnalyzerRule`
- 前端同步新增 `caseNormalizer` 配置、`RULE_ORDER`、`RULE_LEVEL`、`RULE_INFO`
- `index.html` 新增 Case Normalizer 规则卡片
- Level 1 UI 规则数量从 7 rules 更新为 8 rules

验证：
- 输入：`PLEASE EXPLAIN HOW ARRAYS WORK. GIVE ONE EXAMPLE.`
- 输出：`Please explain how arrays work. Give one example.`
- 普通混合大小写输入如 `Explain REST API and JSON parsing.` 不会被处理
- `/api/rules` 可返回 `caseNormalizer`
- Page 1 / Page 2 / Page 3 均可正常显示该 rule

状态：CaseNormalizerRule 全链路上线

---

### ✅ v1.5.4 — DuplicatePhraseReducerRule 上线（2026/4/30）

产出：
- 新增 `DuplicatePhraseReducerRule`（Level 1），用于删除同一句内部连续重复出现的词或短语
- 默认 `maxPhraseLength=3`，第一版仅检测 unigram、bigram、trigram
- 默认 `caseInsensitive=true`，比较时忽略大小写，输出时保留第一次出现片段的原始文本
- 只处理连续重复片段，不处理非连续重复或语义相似短语
- 支持例子：`simple simple` → `simple`
- 支持例子：`step by step step by step` → `step by step`
- `RuleRegistryConfig` 中 Level 1 执行顺序更新为：`DuplicateSentenceRemoverRule → DuplicatePhraseReducerRule → PunctuationNormalizerRule`
- 前端同步新增 `duplicatePhraseReducer` 配置、`RULE_ORDER`、`RULE_LEVEL`、`RULE_INFO`
- `index.html` 新增 Duplicate Phrase Reducer 规则卡片
- Level 1 UI 规则数量从 8 rules 更新为 9 rules

验证：
- 输入：`Explain this step by step step by step.`
- 输出：`Explain this step by step.`
- `/api/rules` 可返回 `duplicatePhraseReducer`
- Page 1 / Page 2 / Page 3 均可正常显示该 rule

已知显示问题：
- 当前前端 Before/After diff 对重复短语删除的高亮可能不够完整，例如可能只高亮部分重复词
- 该问题属于前端 diff visualization 问题，不影响后端 Rule 输出正确性

状态：DuplicatePhraseReducerRule 全链路上线

---

### ✅ v1.5.5 — Protected Text Safety Layer（2026/5/3）

产出：
- 新增 `ProtectedTextProcessor` 工具类，位置：`src/main/java/com/betterprompt/betterpromptbyandyy2/optimizer/util/ProtectedTextProcessor.java`
- 当前保护范围：
  - Markdown fenced code blocks（triple backticks）
  - inline code（single backticks）
- 已接入以下高风险文本转换规则：
  - `CaseNormalizerRule`
  - `StructureMinimizerRule`
  - `DuplicateSentenceRemoverRule`
  - `DuplicatePhraseReducerRule`
  - `PunctuationNormalizerRule`
  - `NumberNormalizerRule`
  - `SemanticCompressorRule`
- 这些规则现在只转换 protected regions 之外的普通自然语言文本；protected regions 之外的文本仍可正常优化
- protected code regions 保持 byte-for-byte unchanged
- `ProtectedTextProcessor` 是共享 safety utility，不是前端可见规则卡片，也不是独立 pipeline rule
- 当前不保护 quoted text、未 fenced 的 JSON-like blocks、Markdown tables 或 custom delimiters
- 新增单元测试：
  - `ProtectedTextProcessorTest`
  - `ProtectedMarkdownPipelineTest`
  - `StructureMinimizerRuleTest`
  - `DuplicateSentenceRemoverRuleTest`
  - `PunctuationNormalizerRuleTest`
  - `CaseNormalizerRuleTest`
  - `NumberNormalizerRuleTest`
  - `SemanticCompressorRuleTest`
  - `DuplicatePhraseReducerRuleTest`

本地测试说明：
- Focused protected Markdown regression suite passes
- `mvn -DskipTests compile` passes
- Full `mvn test` 当前被既有 Spring context test 阻塞，因为环境缺少 `OPENAI_API_KEY`；该问题与 protected text change 无关

设计决策：
- v1.5.5 采用共享 utility layer，而不是新增一个普通 pipeline rule
- 原因：当前 `Rule` interface 只传递 `String input/output`，不携带 shared pipeline context，普通 Protector/Restorer rule 无法安全地跨规则保存 protected state
- 未来如果引入 `PipelineContext`，可以考虑升级为 Protector/Restorer architecture

状态：Protected Text Safety Layer 已接入七个高风险 Level 1 文本转换规则

---

## 待完成功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| OutputFormatDeduplicatorRule | 中 | 去除重复输出格式要求，和 FormatControlRule 形成互补 |
| CodeBlockProtectorRule 升级 | 中 | 当前 `ProtectedTextProcessor` 已覆盖 fenced code blocks 和 inline code；后续可扩展 quoted text、JSON-like blocks、Markdown tables |
| Level 3 上下文优化 | 中 | 历史裁剪、摘要记忆、相关性过滤 |
| Level 4 系统级优化 | 低 | 缓存、模型分流、任务拆分 |
| Level 5 高级优化 | 低 | 长期研究方向 |

---

## 决策日志

| 日期 | 决策 | 原因 |
|------|------|------|
| 2026/3/25 | 用 Strategy + Chain of Responsibility | 每条规则独立，新增不影响其他文件 |
| 2026/3/25 | RuleRegistryConfig 集中注册 | 新增规则只改一个文件，零侵入 |
| 2026/3/25 | Token 计数用空格分词 | 快速实现，TokenCounter 隔离，未来替换只改一处 |
| 2026/3/26 | 参数用 LOW/MID/HIGH 而非0-100 | UI 直观，代码自文档化，内部仍映射数字 |
| 2026/3/26 | TaskAnalyzer 用关键词分类而非 ML | 第一版快速实现，架构支持后续替换为 ML 分类器 |
| 2026/3/27 | AI Generate 先占位不实现 | 框架优先，API 接入作为独立迭代 |
| 2026/3/31 | 改为三页 SPA 而非单页 | 内容太多，分页后每页更专注，演示效果更好 |
| 2026/4/4 | 用 Token 效率比替代线性加权公式 | 效率比 = 每个 token 产出的质量，和项目核心目标"提高 token 使用效率"直接对应，有明确经济学含义；加权公式参数主观，无法解释 |
| 2026/4/7 | AI Generate 用 OpenAI 而非 Anthropic | 与 Quality Check 复用同一 API key 和调用方式，零新增依赖；Anthropic API 格式不同，切换需单独适配 |
| 2026/4/15 | NumberNormalizer 实现完整位权解析器而非简单正则 | 简单正则无法处理组合数字（two hundred and fifty），位权算法可正确解析任意合法英文数字词组 |
| 2026/4/20 | 合并 InputCleaner 和 RedundancySuppressor 为 FillerRemoval | 两者本质都是删除社交性填充语，区别仅在位置（开头/结尾），不是强划分维度。合并后用 aggressiveness 参数统一控制，减少职责重合，未来加中间位置的 filler 也无需新规则 |
| 2026/4/25 | 在 FEATURES.md 中以 Scope boundary 段落明确 FillerRemoval / SemanticCompressor / FormatControl 三者的职责边界 | 防止未来新增词条时归属不清。统一判定原则:能直接删除的归 Filler,只能压缩的归 Compressor,改变输出形式的归 FormatControl |
| 2026/4/25 | TokenCounter 选 jtokkit 而非自实现 BPE | jtokkit 是 OpenAI tiktoken 官方词表的 Java 移植，o200k_base 与项目实际调用的 gpt-4o-mini 完全对齐；纯 Java 无 native 依赖，零集成成本；自实现 BPE 需复刻整个词表与合并规则，无价值 |
| 2026/4/27 | 保留 LengthControlRule，并新增 SentenceBudgetRule | SentenceBudgetRule 按句子数量做较自然的结构裁剪；LengthControlRule 继续作为最终 maxWords 硬兜底，两者控制单位不同，不构成功能重叠 |
| 2026/4/30 | 先实现 DuplicateSentenceRemoverRule，而不是 CaseNormalizerRule | 完整重复句删除更直接服务 token optimization，独立性强、风险低、演示效果明显；CaseNormalizerRule 更偏输入规范化，可放到后续版本 |
| 2026/4/30 | CaseNormalizerRule 采用 0.9 大写比例阈值 | 大小写规范化容易误伤专有名词和缩写，因此第一版只处理明显全大写输入，使用更保守的 0.9 阈值降低误改风险 |
| 2026/4/30 | DuplicatePhraseReducerRule 第一版只处理连续重复短语 | 为避免误删用户真实意图，第一版仅处理 exact adjacent duplicate unigram/bigram/trigram，不做语义相似判断；前端 diff 高亮问题后续作为独立 UI 优化处理 |
| 2026/5/1 | 文档版本号体系统一至标准三段式 major.minor.patch | major 留给架构级新维度（Level 3 = v2.0.0，Level 4/5 = v3.0.0）；minor 留给独立新功能模块（Generator / UI 重构 / Quality Check / AI Generate / BPE Tokenizer）；patch 留给单条规则上线。git tag 历史（v1.0.4 / v3.0）保留，文档归文档、tag 归 tag。 |
| 2026/5/3 | Protected Text Safety Layer 采用共享工具类而不是普通 pipeline rule | 当前 Rule interface 只传递 String input/output，不携带 shared pipeline context；共享 `ProtectedTextProcessor` 可以让高风险规则在本地跳过 fenced code blocks 和 inline code。未来若引入 PipelineContext，可考虑 Protector/Restorer architecture。 |

---

## 风险与阻塞

| 日期 | 类型 | 描述 | 状态 |
|------|------|------|------|
| 2026/3/25 | 阻塞 | Claude Code 生成文件路径不在 IntelliJ 项目目录 | ✅ 已解决：在项目目录内启动 Claude Code |
| 2026/3/31 | 阻塞 | style.css 写入失败（文件被 IntelliJ 锁定） | ✅ 已解决：停止项目运行后重试 |
| 2026/3/31 | 风险 | Token 计数用空格分词，与真实 BPE 有偏差 | ✅ 已解决：v1.5.0 接入 jtokkit (o200k_base) |

---

## 注释规范

从 v1.0.0 起，所有 Java 文件必须包含以下注释：

### 类级注释模板

```java
/**
 * [类名] — [一句话说明这个类做什么]
 *
 * 当前实现：[现在的算法逻辑]
 * 未来升级方向：[后续可以怎么改进]
 */
```

### 方法级注释模板

```java
/**
 * [方法作用]
 *
 * @param [参数名] [参数说明]
 * @return [返回值说明]
 */
```

### Mock 标注规范

所有 Mock 实现必须在方法内注明：

```java
// TODO: 当前为 Mock 实现，真实算法应该：
// 1. [升级方向1]
// 2. [升级方向2]
```

---

## 下一步行动

1. 评估 OutputFormatDeduplicatorRule 与 CodeBlockProtectorRule 的优先级
2. 启动下一条 Level 1 规则设计

---

*最后更新：2026/5/1 · 维护人：Andy*
