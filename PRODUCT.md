# BetterPrompt 功能文档

**第一阶段（Level 1）** 处理输入本身的内容，删除和替换无用的词句；**第二阶段（Level 2）** 控制输出的结构和长度，约束 prompt 的格式和体积。

## 输入优化 — 删掉 prompt 里没用的词和句子

| 功能名称 | 英文名称 | 功能说明 | 类型 | 阶段 | 完成情况 |
|---------|---------|---------|------|------|---------|
| 填充语删除 | Filler Removal | 删除 prompt 中的社交性填充语：开头问候语、礼貌开头、中间 filler 词、结尾客套话，可按力度档位（LOW/MID/HIGH）控制清理范围 | 输入优化 | 第一阶段 | 待升级 |
| 任务识别 | Task Analyzer | 自动判断这个 prompt 是在问什么类型的问题 | 输入优化 | 第一阶段 | 待升级 |
| 啰嗦文字替换 | Semantic Compressor | 把常见的冗长表述转换成更简短的表达 | 输入优化 | 第一阶段 | 待升级 |
| 空白清理 | Structure Minimizer | 删除多余的空行和空格 | 输入优化 | 第一阶段 | 已完成 |
| 标点规范化 | Punctuation Normalizer | 删除重复标点符号，例如 "!!!" → "!"，规范省略号用法 | 输入优化 | 第一阶段 | 已完成 |
| 数字标准化 | Number Normalizer | 把英文数字和百分比表达转换成阿拉伯数字，例如 "two hundred" → "200" | 输入优化 | 第一阶段 | 已完成 |
| 大小写规范化 | Case Normalizer | 保守修复明显全大写 prompt，使文本更稳定、更易读 | 输入优化 | 第一阶段 | 已完成 |
| 重复句删除 | Duplicate Sentence Remover | 删除 prompt 中重复出现的完整句子，减少重复表达造成的 token 浪费 | 输入优化 | 第一阶段 | 已完成 |
| 重复短语压缩 | Duplicate Phrase Reducer | 删除同一句内部重复出现的词组或短语 | 输入优化 | 第一阶段 | 已完成 |
| 代码块保护 | Code Block Protector | 通过共享 `ProtectedTextProcessor` 保护 Markdown fenced code blocks 和 inline code，避免高风险 Level 1 文本转换规则和相关 Level 2 规则误改或误判代码内容 | 输入优化 | 第一阶段 | 部分完成 |
| 引用文本保护 | Quoted Text Protector | 保护引号内文本，避免用户明确引用的内容被误改 | 输入优化 | 第一阶段 | 未完成 |

> 当前实现说明：代码块保护不是单独的前端规则卡片，也不是独立 pipeline rule；它由 `ProtectedTextProcessor` 作为共享 utility layer 提供。目前已覆盖 Markdown fenced code blocks 和 inline code，并接入 Case / Structure / Duplicate Sentence / Duplicate Phrase / Punctuation / Number / Semantic 七个高风险 Level 1 文本转换规则，以及 Level 2 的 `FormatControlRule`、`ConstraintDeduplicatorRule` 和 `InstructionConflictDetectorRule`。protected regions 保持 byte-for-byte unchanged，普通文本仍可优化。JSON-like blocks outside fenced code、Markdown tables、quoted text 和自定义 delimiter 仍属于后续升级范围。

## 结构控制 — 控制 prompt 整体的长度和格式

| 功能名称 | 英文名称 | 功能说明 | 类型 | 阶段 | 完成情况 |
|---------|---------|---------|------|------|---------|
| 长度限制 | Length Control | 超过设定字数上限时作为最终兜底规则自动截断 | 结构控制 | 第二阶段 | 待升级 |
| 格式压缩 | Format Control | 把格式指令换成更短的符号表示，并通过 `ProtectedTextProcessor` 避免误改 fenced code blocks 和 inline code | 结构控制 | 第二阶段 | 部分完成 |
| 句子数量限制 | Sentence Budget | 按最大句子数限制 prompt 长度，超过设定句数时自动截断 | 结构控制 | 第二阶段 | 已完成 |
| 输出格式去重 | Output Format Deduplicator | 去除重复的输出格式要求，例如重复的列表、表格、JSON、Markdown 或代码块格式要求，保留每种格式第一次出现的要求 | 结构控制 | 第二阶段 | 已完成 |
| 输出约束去重 | Constraint Deduplicator | 删除语义重复的输出约束，例如简洁、详细、一步步、简单易懂、例子要求；每类约束保留第一次出现的句子，删除后续重复项 | 结构控制 | 第二阶段 | 已完成 |
| 指令冲突检测 | Instruction Conflict Detector | 检测互相冲突的输出要求，例如简洁 vs 详细、单句回答 vs 分步骤解释、JSON vs Markdown；当前版本只检测并提示，不自动修改 prompt | 结构控制 | 第二阶段 | 部分完成 |
| 模板骨架压缩 | Prompt Skeleton Compressor | 压缩结构化 prompt 的模板标题，减少模板本身的 token 消耗 | 结构控制 | 第二阶段 | 未完成 |

当前 detector 已经能识别三类 conflict pairs：`CONCISE` vs `DETAILED`、`ONE_SENTENCE` vs `STEP_BY_STEP`、`JSON` vs `MARKDOWN`。它和 Constraint Deduplicator 的关系是：ConstraintDeduplicatorRule 先清理重复约束，InstructionConflictDetectorRule 再检测剩余约束之间的潜在冲突。当前没有自动 resolver，避免误改用户意图。

### 本地规则语料库优先策略

BetterPrompt 的核心价值是减少 token 使用，因此核心优化规则应尽量避免为了优化而额外调用 LLM API。项目未来优先通过本地语料库、pattern library、phrase-pair library 来扩大覆盖面，而不是把 filler、compression、constraint 或 conflict 判断交给外部模型。

这个方向可以降低额外 API 成本，增强规则可解释性，提高单元测试稳定性，也方便在 GitHub 中展示算法和工程能力。未来可逐步扩充 filler phrases、polite openers、closing remarks、verbose-to-concise phrase pairs、constraint expressions、conflict patterns、format instruction patterns，并评估将部分规则表外部化为 JSON / YAML 配置文件。

## 后续升级路线图

| 模块 | 当前状态 | 下一步升级方向 |
|------|----------|----------------|
| Filler Removal | 固定规则 | 大规模本地 filler corpus |
| Case Normalizer | 基础全大写修复 | 专有名词 / 缩写保护 |
| Task Analyzer | 关键词分类 | 更大本地关键词库 + 多标签分类 |
| Semantic Compressor | 固定 phrase table | 大规模 verbose-to-concise phrase pairs |
| Structure Minimizer | 空白清理 | Markdown 结构清理 |
| Duplicate Sentence Remover | 完全重复 | 近似重复句检测 |
| Duplicate Phrase Reducer | 短连续重复 | 更长短语 + 强调表达保护 |
| Punctuation Normalizer | 英文标点 | 中文标点 + 混合标点 |
| Number Normalizer | 整数 / 百分比 | 序数词 / 小数 / 范围表达 |
| Output Format Deduplicator | 固定格式 pattern | 更多格式类型和 paraphrase |
| Constraint Deduplicator | 固定 patterns | 本地 constraint expression corpus |
| Instruction Conflict Detector | detect-only | severity scoring + suggestion-only resolver + conflict pattern corpus |
| Sentence Budget | 保留前 N 句 | importance-aware sentence selection |
| Length Control | hard truncate | importance-aware trimming |
| Format Control | fixed replacements | implicit format intent + no-markdown/no-code detection |
| Protected Text Safety Layer | code block / inline code | quoted text / JSON-like blocks / Markdown tables |
| Prompt Skeleton Compressor | 未实现 | 未来 Level 2 rule |
