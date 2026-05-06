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
| 代码块保护 | Code Block Protector | 通过共享 `ProtectedTextProcessor` 保护 Markdown fenced code blocks 和 inline code，避免高风险 Level 1 文本转换规则误改代码内容 | 输入优化 | 第一阶段 | 部分完成 |
| 引用文本保护 | Quoted Text Protector | 保护引号内文本，避免用户明确引用的内容被误改 | 输入优化 | 第一阶段 | 未完成 |

> 当前实现说明：代码块保护不是单独的前端规则卡片，也不是独立 pipeline rule；它由 `ProtectedTextProcessor` 作为共享 utility layer 提供。目前已覆盖 Markdown fenced code blocks 和 inline code，并接入 Case / Structure / Duplicate Sentence / Duplicate Phrase / Punctuation / Number / Semantic 七个高风险 Level 1 文本转换规则。protected regions 保持 byte-for-byte unchanged，普通文本仍可优化。JSON-like blocks outside fenced code、Markdown tables、quoted text 和自定义 delimiter 仍属于后续升级范围。

## 结构控制 — 控制 prompt 整体的长度和格式

| 功能名称 | 英文名称 | 功能说明 | 类型 | 阶段 | 完成情况 |
|---------|---------|---------|------|------|---------|
| 长度限制 | Length Control | 超过设定字数上限时作为最终兜底规则自动截断 | 结构控制 | 第二阶段 | 待升级 |
| 格式压缩 | Format Control | 把格式指令换成更短的符号表示 | 结构控制 | 第二阶段 | 待升级 |
| 句子数量限制 | Sentence Budget | 按最大句子数限制 prompt 长度，超过设定句数时自动截断 | 结构控制 | 第二阶段 | 已完成 |
| 输出格式去重 | Output Format Deduplicator | 去除重复的输出格式要求，例如重复的列表、表格或 JSON 格式要求 | 结构控制 | 第二阶段 | 未完成 |
| 输出约束去重 | Constraint Deduplicator | 去除语义重复的输出约束，例如简洁、详细、一步步等重复要求 | 结构控制 | 第二阶段 | 未完成 |
| 指令冲突检测 | Instruction Conflict Resolver | 检测或处理互相冲突的输出要求 | 结构控制 | 第二阶段 | 未完成 |
| 模板骨架压缩 | Prompt Skeleton Compressor | 压缩结构化 prompt 的模板标题，减少模板本身的 token 消耗 | 结构控制 | 第二阶段 | 未完成 |
