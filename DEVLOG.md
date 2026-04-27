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

- 当前版本：v3.1
- 当前阶段：TokenCounter 接入 jtokkit BPE 完成，token 计数与 OpenAI gpt-4o-mini 完全对齐
- 已完成模块数：10/10（Level 1 + Level 2 + Quality Check + AI Generate）
- 下一步：Level 3 上下文优化（历史裁剪、摘要记忆、相关性过滤）

---

## 项目里程碑

### ✅ v1.0 — 框架搭建（2026/3/25）

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

### ✅ v1.0 — 真实算法实现（2026/3/26）

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

### ✅ v1.0 — Prompt Generator（2026/3/27）

产出：
- TemplatePromptGenerator：45+模板，覆盖5种任务x3种废话程度
- AiPromptGenerator：占位符，TODO接入Claude API
- GET /api/generator/prompt 接口
- 前端 Generator 面板（Task Type + Verbosity 下拉 + Generate按钮）
- AI Generate 按钮置灰，hover显示 "Coming Soon"

状态：模板生成可用，AI生成待接入

---

### ✅ v1.0（tag v1.0.4） — UI 重构（2026/3/31）

产出：
- 经典 Google Material Design 风格
- 三页 SPA：Input & Config / Pipeline 详情 / Final Result
- Pipeline 每步展开显示：参数说明 + Before/After diff + 改动清单
- 右侧策略面板：每条规则可独立开关 + LOW/MID/HIGH 切换
- ℹ 弹窗：每条规则点击查看详细说明 + 示例 + 未来计划
- Token 统计卡片 + 贡献度柱状图（原生 canvas）

状态：UI 重构进行中

---

### ✅ v2.0 — Quality Check 功能（2026/4/4）

产出：
- `POST /api/compare` 接口
- `QualityComparisonController`：OpenAI gpt-4o-mini 并行回答生成 + 三维度打分（切题性/信息密度/表达清晰度）+ Token 效率比计算
- `QualityComparisonResult`：完整结果模型（6个维度分、综合分、naturalSummary、optimizationScore、verdict）
- 前端第四页 Quality Check：两列回答对比 + 维度进度条 + AI 自然语言分析卡片 + Token Efficiency Gain 展示
- 第三页由 "Final Result" 改名为 "Token Analysis"

状态：Quality Check 全链路上线，含后端 OpenAI 调用和前端可视化

---

### ✅ v2.1 — AI Generate 功能上线（2026/4/7）

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

### ✅ v3.0 — Punctuation Normalizer + Number Normalizer（2026/4/15）

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

### ✅ v3.0.1 — 规则合并：FillerRemovalRule（2026/4/20）

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

### ✅ v3.1 — 真实 BPE Tokenizer 接入 + MOCK 标注清理（2026/4/25）

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
- 文档版本号体系统一至 v3.x（对齐 git tag 历史 v1.0.4 / v3.0）

状态：BPE token 计数全链路上线，"逻辑用词数 + 显示用 BPE"语义干净分离

---

## 待完成功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
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
| 2026/4/25 | 文档版本号体系统一至 v3.x | 开发过程中用 v1.0–v1.8 做内部迭代标记，git tag 实际只打了 v1.0.4 和 v3.0；内部版本号与 tag 不对齐会造成混淆。统一规则：v1.0–v1.3 → v1.0（tag v1.0.4），v1.4 → v2.0，v1.5 → v2.1，v1.6 → v3.0（tag v3.0），v1.7 → v3.0.1，v1.8 → v3.1 |

---

## 风险与阻塞

| 日期 | 类型 | 描述 | 状态 |
|------|------|------|------|
| 2026/3/25 | 阻塞 | Claude Code 生成文件路径不在 IntelliJ 项目目录 | ✅ 已解决：在项目目录内启动 Claude Code |
| 2026/3/31 | 阻塞 | style.css 写入失败（文件被 IntelliJ 锁定） | ✅ 已解决：停止项目运行后重试 |
| 2026/3/31 | 风险 | Token 计数用空格分词，与真实 BPE 有偏差 | ✅ 已解决：v3.1 接入 jtokkit (o200k_base) |

---

## 注释规范

从 v1.1 起，所有 Java 文件必须包含以下注释：

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

1. 启动 Level 3 上下文优化设计（历史裁剪、摘要记忆、相关性过滤）
2. 更新本日志

---

*最后更新：2026/4/25 · 维护人：Andy*
