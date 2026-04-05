# BetterPrompt 功能清单 v2.0

> 本文档根据实际代码生成，记录项目每个功能的实现状态、细节和验证方式。
> 更新规则：每完成或修改一个功能，同步更新对应条目。

---

## 功能完成度总览

| 模块 | 功能数 | ✅ 已完成 | 🔧 有已知局限 | ❌ 未实现 |
|------|--------|-----------|--------------|----------|
| 后端核心框架 | 4 | 3 | 1 | 0 |
| Level 1 优化规则 | 4 | 2 | 2 | 0 |
| Level 2 优化规则 | 3 | 0 | 3 | 0 |
| Prompt Generator | 2 | 1 | 0 | 1 |
| REST API | 4 | 4 | 0 | 0 |
| 前端 UI | 5 | 5 | 0 | 0 |
| 质量对比（Quality Check） | 6 | 6 | 0 | 0 |
| **合计** | **28** | **21** | **6** | **1** |

> 注：前端 Page 1 的"AI Generate"按钮 UI 已实现但功能被禁用（`disabled` 属性），依赖 AiPromptGenerator 后端实现。

---

## 一、后端核心框架

### 1.1 Rule 接口（Strategy 模式）
✅ 已完成并测试

**接口方法列表**（`Rule.java`）：
| 方法 | 返回类型 | 说明 |
|------|----------|------|
| `getRuleId()` | `String` | 唯一 camelCase ID，与请求 JSON 中的键对应（如 `"inputCleaner"`） |
| `getRuleName()` | `String` | 人类可读的展示名（如 `"Input Cleaner"`） |
| `getRuleLevel()` | `String` | 分组标签（如 `"Level 1"` 或 `"Level 2"`） |
| `getDescription()` | `String` | 前端 UI 展示的简短描述 |
| `apply(String inputText, RuleConfig config)` | `StepResult` | 执行规则变换，inputText 为 null 时视为空字符串 |

**设计约束**（来自代码注释）：
- `apply()` 绝不能抛出 checked exception，错误须封装在 `StepResult` 中（status="error"）
- `apply()` 必须始终返回非空的 `StepResult`
- 每条 Rule 完全自包含，所有算法逻辑仅在 Rule 类内部实现
- RuleEngine 仅依赖 Rule 接口，不感知具体实现

**验证方式**：实现任意 Rule → 注册到 RuleRegistryConfig → 启动应用 → POST /api/optimize → 检查 steps 中包含对应 ruleName

---

### 1.2 RuleEngine（流水线执行器）
✅ 已完成并测试

**执行逻辑**（`RuleEngine.java`，`optimize()` 方法）：

1. 取 `request.getPrompt()`，null 时转为空字符串
2. 对原始文本调用 `TokenCounter.count()` 记录 `originalTokens`
3. 按 `RuleRegistryConfig` 注入的列表顺序遍历每条 Rule：
   - 若该 Rule 的 `RuleConfig` 为 null，或 `config.isEnabled() == false` → 构建"skipped" StepResult（inputText=outputText=currentText，tokensSaved=0，changes=`["[SKIPPED] Rule is disabled"]`），`currentText` 不变
   - 否则 → 调用 `rule.apply(currentText, config)`，将返回的 StepResult 的 status 设为 `"done"`，用 `step.getOutputText()` 更新 `currentText`（若 outputText 为 null 则 currentText 不变）
4. 对最终 `currentText` 调用 `TokenCounter.count()` 得 `finalTokens`
5. 计算压缩率：`Math.round((1.0 - finalTokens / originalTokens) * 1000.0) / 10.0`（保留一位小数），originalTokens=0 时压缩率为 0.0
6. 构建 `OptimizationResult`（steps 列表、finalPrompt、tokenStats）并返回

**byRule 贡献度 Map**：key 为 `rule.getRuleName()`，value 为 `Math.max(0, step.getTokensSaved())`（负值（TaskAnalyzer 追加 tag 时）记为 0）

**验证方式**：POST /api/optimize，传入包含多条规则的请求；检查响应中 steps 数组长度等于注册规则数（7），跳过的 rules status="skipped"，执行的 rules status="done"

---

### 1.3 RuleRegistryConfig（规则注册表）
✅ 已完成并测试

**当前注册的规则列表及执行顺序**（`RuleRegistryConfig.java`）：

| 执行顺序 | 类名 | ruleId | 所属 Level |
|---------|------|--------|-----------|
| 1 | `InputCleanerRule` | `inputCleaner` | Level 1 |
| 2 | `TaskAnalyzerRule` | `taskAnalyzer` | Level 1 |
| 3 | `SemanticCompressorRule` | `semanticCompressor` | Level 1 |
| 4 | `StructureMinimizerRule` | `structureMinimizer` | Level 1 |
| 5 | `LengthControlRule` | `lengthControl` | Level 2 |
| 6 | `FormatControlRule` | `formatControl` | Level 2 |
| 7 | `RedundancySuppressorRule` | `redundancySuppressor` | Level 2 |

**新增规则的步骤**：
1. 创建 `YourNewRule.java`，实现 `Rule` 接口（放在 `optimizer/` 下任意子包）
2. 在 `RuleRegistryConfig.rules()` 的 `List.of(...)` 中添加 `new YourNewRule()` 到期望的位置
3. 完成。不需要修改其他任何文件

**删除规则**：从 `List.of(...)` 中删除对应条目即可

---

### 1.4 TokenCounter（Token 计数）
🔧 已实现但有已知局限

**当前算法**（`TokenCounter.java`）：
- 调用 `text.trim().split("\\s+").length`，即按空白字符分割后计词数
- text 为 null 或 blank 时返回 0

**局限性**（代码注释 MOCK 标注）：
- 不是真实 BPE 分词，与 Claude 实际计数存在偏差
- 无法正确处理标点、Unicode、代码块中的特殊 token
- 未缓存 tokenizer 初始化

**未来升级方向**（代码注释）：
- 使用真实 BPE tokenizer（如 tiktoken 或 Anthropic 官方 tokenizer）
- 正确处理标点符号、Unicode 和子词单元（subword units）
- 缓存 tokenizer 初始化以提升性能

---

## 二、Level 1 优化规则

### 2.1 InputCleanerRule
✅ 已完成并测试（代码注释标注为"real algorithm"）

**参数**：`aggressiveness`（int，0–100，默认 50）

**三档行为差异**（`InputCleanerRule.java`）：

**LOW（0–30）** — 仅删除显式寒暄词（4 个正则，均匹配行首，`CASE_INSENSITIVE`）：
```
^\s*good\s+(morning|afternoon|evening)[,!.\s]+
^\s*hello[,!.\s]+
^\s*hi[,!.\s]+
^\s*hey[,!.\s]+
```

**MEDIUM（31–70）** — LOW + 礼貌性请求开头（9 个正则，更具体的在前以防部分匹配，`CASE_INSENSITIVE`）：
```
^\s*i was hoping you could\s+
^\s*i need you to\s+
^\s*would you please\s+
^\s*could you please\s+
^\s*can you please\s+
^\s*could you\s+
^\s*can you\s+
^\s*would you\s+
^\s*please\s+
```

**HIGH（71–100）** — MEDIUM + 软性开头（7 个正则）+ 中间 filler 词（4 个词）：

软性开头正则（行首，`CASE_INSENSITIVE`）：
```
^\s*i am reaching out because\s+
^\s*i hope you don'?t mind\s+
^\s*i'?d like to ask\s+
^\s*i would like to ask\s+
^\s*i'?d like you to\s+
^\s*i would like you to\s+
^\s*i was wondering if\s+
```

中间 filler 词（须被空白字符包围，`CASE_INSENSITIVE`，正则 `(?<=\s)WORD(?=\s)`）：
```
basically
essentially
literally
actually
```

**执行机制**：
- 使用 while 循环反复应用 LOW / MEDIUM / HIGH 开头正则，直到一轮扫描无任何匹配为止（处理"Hello! Hi there! Please..."之类叠加寒暄）
- Filler 词在循环外单独处理（匹配词组需两侧均有空格）
- 所有清理完成后，自动大写第一个字母
- Changes 列表格式：`[aggressiveness=TIER] 删除强寒暄: 'xxx'` 或 `删除软开头` 或 `删除 filler 词`；若无匹配则记录 `未检测到可删除的寒暄词或 filler 词`

**已知局限**（代码注释 TODO）：
- 不支持多行/跨句的礼貌开头
- 仅处理英文模式，暂不支持多语言（中文：你好/麻烦你；法文：Bonjour/Pourriez-vous）
- 无法识别需要保留开头的场景（如客服/正式写作语境）

**验证用例**：

用例 A（LOW）：
- 输入：`"Hello, write a Python function to sort a list."`
- 期望输出：`"Write a Python function to sort a list."`
- 期望改动：`["[aggressiveness=LOW] 删除强寒暄: 'Hello,'"]`

用例 B（HIGH）：
- 输入：`"Hi! I was wondering if you could explain how this basically works."`
- 期望输出：`"Explain how this works."` （Hi! 删除 → I was wondering if 删除 → you could... 无匹配 → basically 删除 → 首字母大写）
- 期望改动：`["[aggressiveness=HIGH] 删除强寒暄: 'Hi!'", "[aggressiveness=HIGH] 删除软开头: 'i was wondering if'", "[aggressiveness=HIGH] 删除 filler 词: 'basically'"]`

---

### 2.2 TaskAnalyzerRule
🔧 已实现但有已知局限（基于关键词匹配，非 ML 分类器）

**支持的任务类型及关键词**（`TaskAnalyzerRule.java`，按优先级顺序）：

| 优先级 | 类型 | 关键词（命中其一即分类，toLowerCase 匹配） |
|-------|------|------------------------------------------|
| 1 | DEBUG | error, bug, fix, not working, issue, exception, crash, wrong, broken, fail, doesn't work, won't work |
| 2 | CODING | write, code, function, implement, program, class, algorithm, method, loop, variable, array, syntax |
| 3 | EXPLAIN | explain, what is, how does, describe, understand, definition, what are, how do, why does, what does |
| 4 | WRITING | write an email, write a letter, write an essay, write a paragraph, write an article, draft, blog post |
| 5 | COMPARE | compare, difference, vs, versus, better, pros and cons, similarities, which is |
| 默认 | GENERAL | 无关键词命中时使用 |

**注意**：DEBUG 比 CODING 优先级高（"fix" 和 "write" 都存在时，因 DEBUG 先检查，会分类为 DEBUG）

**复杂度判断规则**（基于 TokenCounter.count() 词数）：
- < 15 词 → `LOW`
- 15–40 词 → `MEDIUM`
- > 40 词 → `HIGH`

**分类结果追加格式**：
```
[Task: CODING | Complexity: HIGH]
```
追加到输入文本末尾（含前导空格），`tokensSaved` 为负数（因为追加了 token）

**Changes 列表格式**：
```
检测到任务类型: CODING（匹配关键词: 'write'）
复杂度评估: LOW（词数: 7）
已追加元数据标签: [Task: CODING | Complexity: LOW]
```
若无匹配关键词：`检测到任务类型: GENERAL（无匹配关键词，使用默认值）`

**已知局限**（代码注释）：
- 关键词匹配精度有限（含 "write" 的 DEBUG prompt 可能被错误分类）
- 复杂度仅用词数评估，不考虑词汇多样性、子任务数量等
- 追加的标签目前对下游 Rule 不产生影响（下游 Rule 不读取任务类型）
- 不支持多标签分类（如 CODING + EXPLAIN 同时成立时）

**验证用例**：
- 输入：`"Write a Python function to sort a list."` （7 词）
- 期望输出：`"Write a Python function to sort a list. [Task: CODING | Complexity: LOW]"`
- 期望改动：`["检测到任务类型: CODING（匹配关键词: 'write'）", "复杂度评估: LOW（词数: 7）", "已追加元数据标签: [Task: CODING | Complexity: LOW]"]`

---

### 2.3 SemanticCompressorRule
✅ 已完成并测试（代码注释标注为"real algorithm"）

**参数**：`compressionLevel`（int，0–100，默认 50）

**匹配方式**：全局大小写不敏感（`(?i)` + `Pattern.quote()`），累积叠加（MEDIUM 包含 LOW，HIGH 包含 MEDIUM）

**LOW（0–30）— 8 组安全替换**：
| 原始短语 | 替换为 |
|---------|--------|
| in order to | to |
| due to the fact that | because |
| at this point in time | now |
| in the event that | if |
| a large number of | many |
| on a daily basis | daily |
| in the near future | soon |
| with regard to | regarding |

**MEDIUM（31–70）— 在 LOW 基础上新增 11 组（共 19 组）**：
| 原始短语 | 替换为 |
|---------|--------|
| as a result of | because of |
| in spite of the fact that | although |
| for the purpose of | to |
| with the exception of | except |
| in close proximity to | near |
| in terms of | regarding |
| make a decision | decide |
| take into consideration | consider |
| come to the conclusion | conclude |
| it is important to note that | note that |
| it is important to note | note |

> 注：`"it is important to note that"` 列在 `"it is important to note"` 之前，防止较短模式先匹配导致 `"that"` 残留

**HIGH（71–100）— 在 MEDIUM 基础上新增 10 组（共 29 组）**：
| 原始短语 | 替换为 |
|---------|--------|
| whether or not | whether |
| each and every | every |
| first and foremost | first |
| the fact that | that |
| in the process of | while |
| at the present time | currently |
| has the ability to | can |
| is able to | can |
| very unique | unique |
| completely eliminate | eliminate |

**Changes 列表格式**：`[compressionLevel=TIER] 'verbose phrase' → 'compressed'`；若无命中记录 `未找到可替换的冗余词组`

**已知局限**（代码注释）：
- 替换表固定，无法检测未列举的冗余表达
- 不保护引号内字符串、代码块和专有名词

**验证用例**：
- 输入：`"I need to write code in order to sort the array, due to the fact that it is slow."`（compressionLevel=50，MID）
- 期望输出：`"I need to write code to sort the array, because it is slow."`
- 期望改动：`["[compressionLevel=MEDIUM] 'in order to' → 'to'", "[compressionLevel=MEDIUM] 'due to the fact that' → 'because'"]`

---

### 2.4 StructureMinimizerRule
🔧 已实现但有已知局限（代码注释标注为"MOCK Implementation"）

**四步清理逻辑**（`StructureMinimizerRule.java`，按实际执行顺序）：

1. **清理每行尾部空格**：`lines().map(String::stripTrailing)` 后用 `\n` 重新拼接
   - 触发条件：任何行末尾有空格或 Tab
   - Change 消息：`[MOCK] 清理每行尾部空格`

2. **折叠多余空行**：正则 `(\r?\n){3,}` → `\n\n`（3 个或以上连续换行折叠为 2 个）
   - 触发条件：存在 3 个以上连续换行符
   - Change 消息：`[MOCK] 折叠多余空行`

3. **折叠多余空格**：正则 `[ \t]{2,}` → ` `（多个连续空格或 Tab 折叠为单空格）
   - 触发条件：存在 2 个或以上连续空格/Tab
   - Change 消息：`[MOCK] 折叠多余空格`

4. **清理首尾空白**：`result.strip()`
   - 触发条件：文本首部或尾部有空白字符
   - Change 消息：`[MOCK] 清理首尾空白`

**跳过条件**：若以上四步均无变化，Changes 列表记录 `[MOCK] Structure is already clean, no changes needed`

**已知局限**（代码注释）：
- 不检测冗余 Markdown 标题、重复分隔符（`----`）
- 不折叠深度嵌套的列表
- 不删除空列表项或空表格单元格
- 不检测复制粘贴产生的重复段落

**验证用例**：
- 输入：`"Write a   function\n\n\n\nto sort  a list."`（含多余空格和 3 个以上空行）
- 期望输出：`"Write a function\n\nto sort a list."`
- 期望改动：`["[MOCK] 折叠多余空行", "[MOCK] 折叠多余空格"]`

---

## 三、Level 2 优化规则

### 3.1 LengthControlRule
🔧 已实现但有已知局限（代码注释标注为"MOCK Implementation"）

**参数**：`maxWords`（int，默认 50；若传入 ≤ 0 则重置为 50）

**截断逻辑**（`LengthControlRule.java`）：
- 以 `\\s+` 分割单词数组
- 取前 `maxWords` 个单词用空格拼接，末尾追加 `"..."`
- 若词数 ≤ maxWords，文本不变

**Changes 消息**：
- 截断时：`[MOCK] 截断至 N 词 (原 M 词)`
- 无需截断：`[MOCK] 词数 M ≤ maxWords N，无需截断`

**已知局限**（代码注释）：
- 粗暴截断，不保证在句子边界处截断
- 不保留结论/指令（优先保留开头而非核心任务描述）
- 使用词数而非真实 token 数预算

**验证用例**：
- 输入：`"Write a Python function that returns the sum of all even integers in a list."` （16 词，maxWords=10）
- 期望输出：`"Write a Python function that returns the sum of all..."`
- 期望改动：`["[MOCK] 截断至 10 词 (原 16 词)"]`

---

### 3.2 FormatControlRule
🔧 已实现但有已知局限（代码注释标注为"MOCK Implementation"）

**完整替换列表**（`FormatControlRule.java`，全局大小写不敏感）：

| 原始字符串 | 替换为 |
|-----------|--------|
| `bullet points:` | `•` |
| `- bullet point:` | `•` |
| `numbered list:` | `1.` |
| `bold text:` | `**` |
| `italic text:` | `*` |
| `code block:` | ` ``` ` |
| `as follows:` | `:` |
| `the following:` | `:` |

**Changes 消息**：`[MOCK] "verbose" → "compact"`；若无命中：`[MOCK] No verbose formatting instructions found`

**已知局限**（代码注释）：
- 仅识别固定字符串，无法检测隐式格式意图（如"list the following items"）
- 不规范化混合列表标记样式

**验证用例**：
- 输入：`"Format using bullet points: apples, bananas."`
- 期望输出：`"Format using • apples, bananas."`
- 期望改动：`["[MOCK] \"bullet points:\" → \"•\""]`

---

### 3.3 RedundancySuppressorRule
🔧 已实现但有已知局限（代码注释标注为"MOCK Implementation"）

**完整匹配模式列表**（`RedundancySuppressorRule.java`，均添加 `\b` 前缀，`CASE_INSENSITIVE`）：

| 正则模式 |
|---------|
| `I hope this helps[.!]*` |
| `please let me know[^.]*[.!]*` |
| `feel free to ask[^.]*[.!]*` |
| `don'?t hesitate to (ask\|contact)[^.]*[.!]*` |
| `let me know if you have any (questions\|concerns\|issues)[^.]*[.!]*` |
| `if you have any (questions\|concerns\|issues)[^.]*[.!]*` |
| `thank you for your time[.!]*` |
| `thanks in advance[.!]*` |
| `looking forward to your (response\|reply\|feedback)[^.]*[.!]*` |
| `best regards[^.]*[.!]*` |
| `kind regards[^.]*[.!]*` |
| `hope that (makes sense\|helps\|clarifies)[^.]*[.!]*` |

**删除后的清理逻辑**：对删除后的文本执行 `replaceAll("[ \t]{2,}", " ").strip()`，清除双空格和首尾空白

**Changes 消息**：`[MOCK] 删除结尾套话: "matched text"`；若无命中：`[MOCK] No closing filler phrases found`

**已知局限**（代码注释）：
- 仅匹配固定模式的变体，无法检测语义等价的改写表达
- 不检测跨多句的结尾套话块
- 不识别重复限制条件（如"请简短、简洁、简单"）

**验证用例**：
- 输入：`"Explain recursion. I hope this helps! Thanks in advance."`
- 期望输出：`"Explain recursion."`
- 期望改动：`["[MOCK] 删除结尾套话: \"I hope this helps!\"", "[MOCK] 删除结尾套话: \"Thanks in advance.\""]`

---

## 四、Prompt Generator

### 4.1 模板生成（TemplatePromptGenerator）
✅ 已完成并测试

**支持的任务类型**（`VALID_TASK_TYPES`）：`CODING`、`EXPLAIN`、`DEBUG`、`WRITING`、`COMPARE`

**支持的废话程度**（`VALID_VERBOSITY_LEVELS`）：`LOW`、`MEDIUM`、`HIGH`

**模板总数量**：5 × 3 × 3 = **45 条**（每种组合固定 3 个模板变体）

**每种组合的模板数量**：

| | LOW | MEDIUM | HIGH |
|---|-----|--------|------|
| CODING | 3 | 3 | 3 |
| EXPLAIN | 3 | 3 | 3 |
| DEBUG | 3 | 3 | 3 |
| WRITING | 3 | 3 | 3 |
| COMPARE | 3 | 3 | 3 |

**各 Verbosity 级别说明**（来自类注释）：
- `LOW`：接近干净的 prompt，仅含偶尔一个 "please" / "could you"
- `MEDIUM`：包含开头问候语 + 结尾套话 + 若干冗余词组
- `HIGH`：叠加多层问候、重复礼貌表达、结尾套话，最大化"dirty"程度

**随机选取逻辑**：`candidates.get(new Random().nextInt(candidates.size()))`，每次调用随机返回 3 个变体之一

**generate() 方法**：
- 输入 taskType 和 verbosity（内部转大写拼接为键，如 `"CODING_HIGH"`）
- 若键不存在则返回 `null`（调用方需先验证参数合法性）
- `PromptGeneratorController` 在调用前已做校验，null 时返回 500

---

### 4.2 AI 生成（AiPromptGenerator）
❌ 未实现（占位符）

**当前行为**（`AiPromptGenerator.java`）：`generate()` 直接返回固定字符串 `"AI generation coming soon."`

**预留接口**：`generate(String taskType, String verbosity)` → `String`

**接入 Claude API 的实现计划**（来自代码注释 TODO）：
1. 从 `application.properties` 注入 `anthropic.api.key`（key 已预留，当前为空字符串）
2. 构建 system prompt：`"Generate a realistic user prompt for task type X at verbosity level Y. Verbosity HIGH = full of greetings and filler words."`
3. 调用 `POST https://api.anthropic.com/v1/messages`，model 为 `claude-opus-4-6`
4. 解析响应的 content block，返回文本
5. 添加错误处理 / 指数退避重试

**前端状态**：`index.html` 中"AI Generate"按钮带有 `disabled` 属性和 tooltip `"Coming Soon — Claude API integration"`

---

## 五、REST API

### 5.1 POST /api/optimize
✅ 已完成并测试

**完整请求体结构**：
```json
{
  "prompt": "Hello! I was wondering if you could write a function...",
  "rules": {
    "inputCleaner":         { "enabled": true,  "params": { "aggressiveness": 50 } },
    "taskAnalyzer":         { "enabled": true,  "params": {} },
    "semanticCompressor":   { "enabled": true,  "params": { "compressionLevel": 50 } },
    "structureMinimizer":   { "enabled": true,  "params": {} },
    "lengthControl":        { "enabled": true,  "params": { "maxWords": 50 } },
    "formatControl":        { "enabled": true,  "params": {} },
    "redundancySuppressor": { "enabled": true,  "params": {} }
  }
}
```
- `rules` 中 key 必须与各 Rule 的 `getRuleId()` 一致
- `params` 中未传入的字段使用各 Rule 内部默认值（`getIntParam(key, defaultValue)`）

**完整响应体结构**（HTTP 200）：
```json
{
  "steps": [
    {
      "ruleName": "Input Cleaner",
      "ruleLevel": "Level 1",
      "inputText": "Hello! ...",
      "outputText": "Write a function...",
      "tokensBefore": 10,
      "tokensAfter": 8,
      "tokensSaved": 2,
      "changes": ["[aggressiveness=LOW] 删除强寒暄: 'Hello!'"],
      "status": "done"
    }
  ],
  "finalPrompt": "Write a function...",
  "tokenStats": {
    "original": 10,
    "final": 8,
    "compressionRate": 20.0,
    "byRule": {
      "Input Cleaner": 2,
      "Task Analyzer": 0
    }
  }
}
```
- `status` 取值：`"done"` | `"skipped"` | `"error"`
- `tokenStats.final` 使用 `@JsonProperty("final")` 映射（因 `final` 是 Java 关键字）
- `byRule` 中 value 为 `Math.max(0, tokensSaved)`（负值记为 0）

**错误处理**：
- prompt 为 null 或空字符串 → HTTP 400，body：`{"error": "prompt must not be empty"}`

**实际测试用例**：
```
POST /api/optimize
Content-Type: application/json

{
  "prompt": "Hello! Write a Python function.",
  "rules": {
    "inputCleaner": { "enabled": true, "params": { "aggressiveness": 20 } },
    "taskAnalyzer": { "enabled": false, "params": {} },
    "semanticCompressor": { "enabled": false, "params": {} },
    "structureMinimizer": { "enabled": false, "params": {} },
    "lengthControl": { "enabled": false, "params": {} },
    "formatControl": { "enabled": false, "params": {} },
    "redundancySuppressor": { "enabled": false, "params": {} }
  }
}

期望响应（200）：
{
  "steps": [
    { "ruleName": "Input Cleaner", "status": "done",
      "inputText": "Hello! Write a Python function.",
      "outputText": "Write a Python function.",
      "tokensBefore": 5, "tokensAfter": 4, "tokensSaved": 1,
      "changes": ["[aggressiveness=LOW] 删除强寒暄: 'Hello!'"] },
    { "ruleName": "Task Analyzer",    "status": "skipped", "tokensSaved": 0 },
    { "ruleName": "Semantic Compressor", "status": "skipped", "tokensSaved": 0 },
    { "ruleName": "Structure Minimizer", "status": "skipped", "tokensSaved": 0 },
    { "ruleName": "Length Control",   "status": "skipped", "tokensSaved": 0 },
    { "ruleName": "Format Control",   "status": "skipped", "tokensSaved": 0 },
    { "ruleName": "Redundancy Suppressor", "status": "skipped", "tokensSaved": 0 }
  ],
  "finalPrompt": "Write a Python function.",
  "tokenStats": { "original": 5, "final": 4, "compressionRate": 20.0,
                  "byRule": { "Input Cleaner": 1, ... } }
}
```

---

### 5.2 GET /api/generator/prompt
✅ 已完成并测试

**所有 query 参数**（`PromptGeneratorController.java`）：

| 参数 | 必填 | 默认值 | 合法值 |
|------|------|--------|--------|
| `type` | 是 | — | CODING, EXPLAIN, DEBUG, WRITING, COMPARE（大小写不敏感） |
| `verbosity` | 是 | — | LOW, MEDIUM, HIGH（大小写不敏感） |
| `source` | 否 | `template` | `template`, `ai` |

**响应体结构**（HTTP 200）：
```json
{
  "prompt":    "...",
  "taskType":  "CODING",
  "verbosity": "HIGH",
  "source":    "template"
}
```

**错误处理（HTTP 400）**：
- `type` 不合法：`{"error": "Invalid type 'xxx'. Valid values: CODING, EXPLAIN, ..."}`
- `verbosity` 不合法：`{"error": "Invalid verbosity 'xxx'. Valid values: LOW, MEDIUM, HIGH"}`
- `source` 不合法：`{"error": "Invalid source 'xxx'. Valid values: template, ai"}`

**错误处理（HTTP 500）**：模板查找结果为 null（理论上不会发生，因参数已校验）

---

### 5.3 GET /api/rules
✅ 已完成并测试

**响应体结构**（HTTP 200，返回 List）：
```json
[
  { "id": "inputCleaner",       "name": "Input Cleaner",       "level": "Level 1", "description": "Removes greetings and filler openers from prompts" },
  { "id": "taskAnalyzer",       "name": "Task Analyzer",        "level": "Level 1", "description": "Classifies task type and complexity, appends metadata tag" },
  { "id": "semanticCompressor", "name": "Semantic Compressor",  "level": "Level 1", "description": "Replaces verbose phrases with concise equivalents" },
  { "id": "structureMinimizer", "name": "Structure Minimizer",  "level": "Level 1", "description": "Removes redundant whitespace and normalises text structure" },
  { "id": "lengthControl",      "name": "Length Control",       "level": "Level 2", "description": "Truncates text that exceeds the max-words budget" },
  { "id": "formatControl",      "name": "Format Control",       "level": "Level 2", "description": "Converts verbose formatting instructions to compact symbols" },
  { "id": "redundancySuppressor","name": "Redundancy Suppressor","level": "Level 2", "description": "Removes closing filler sentences that add no prompt value" }
]
```

**用途**：前端可用此接口动态渲染规则配置面板（当前前端硬编码了 7 条规则的 HTML，未实际调用此接口）

---

## 六、前端 UI

### 6.1 第 1 页：Input & Config
✅ 已完成并测试

**页面布局**：两栏布局（`page-two-col`），左栏为输入区，右栏为策略配置区

**Prompt Generator 面板**（左栏，`generator-card`）：
- Task Type 下拉：CODING / EXPLAIN / DEBUG / WRITING / COMPARE
- Verbosity 下拉：LOW / MEDIUM（默认选中）/ HIGH
- `⚡ Generate Template` 按钮：调用 `GET /api/generator/prompt?source=template`，成功后将 prompt 写入 textarea 并触发蓝色边框闪烁动画（800ms）；按钮 loading 状态显示 `"..."`；成功后 meta 区域显示 `"✓ Generated: TYPE · VERBOSITY · source"`
- `🤖 AI Generate` 按钮：带 `disabled` 属性和 tooltip `"Coming Soon — Claude API integration"`，完全不可点击

**输入框功能**：
- `textarea` 实时监听 `input` 事件
- 字数统计：`words word(s)`（如 `"3 words"` / `"1 word"`）
- 字符统计：`N character(s)`
- 两个计数器同步更新，同时同步 `state.prompt`

**策略选择面板**（右栏）：
- Level 1 块（蓝色标题 badge）：4 条规则
- Level 2 块（红色标题 badge）：3 条规则
- Toggle 开关：切换后调用 `toggleRule()`，禁用的规则卡片添加 `.disabled` CSS 类
- LOW/MID/HIGH 三档按钮：点击后当前档按钮添加 `.active` CSS 类，调用 `setTierParam()` 更新 `state.rules`；默认激活 MID（aggressiveness=50，compressionLevel=50）
- Length Control 的 Max Words：`<input type="number">` 默认值 50，range 10-500，实时更新 `state.rules.lengthControl.params.maxWords`
- `ℹ` 按钮：每条规则均有，调用 `showModal(ruleId)`

**Optimize 按钮**：
- 点击后校验 textarea 非空（空时边框变红 1.5s，显示错误文本）
- Loading 状态：按钮 disabled + innerHTML 变为 `<span class="spinner"></span>Optimizing...`
- 成功后：更新 nav 状态栏（`✓ Optimization complete · N tokens saved`），渲染 Page 2/3，跳转到 Page 2
- 失败后：`optimizeError` 区域显示 `Error: xxx`
- 完成后恢复按钮文本为 `Optimize →`

---

### 6.2 第 2 页：Pipeline 详情
✅ 已完成并测试

**Stats 统计栏**（上方 `stats-bar`，共 4 个 stat-card）：
- Original Tokens（黑色数字）
- Optimized Tokens（蓝色数字）
- Tokens Saved（绿色数字）
- Compression Rate（绿色数字，格式 `X.X%`）

**步骤卡片展示**（仅渲染 status ≠ "skipped" 的步骤）：
- 左侧色条：tokensSaved > 0 → 绿色（`bar-green`）；≤ 0 → 灰色（`bar-gray`）
- 步骤编号图标：tokensSaved > 0 → `✓`（绿色）；< 0 → `+`（警告色）；= 0 → `—`（中性色）
- Level badge：Level 1 → 蓝色；Level 2 → 红色
- Token pill 颜色逻辑：tokensSaved > 0 → 绿色（`−N tokens`）；< 0 → 红色（`+N tokens`）；= 0 → 灰色（`0 tokens`）
- 参数摘要文字（param text）：inputCleaner 显示 `Aggressiveness: LOW/MID/HIGH`；semanticCompressor 显示 `Compression Level: LOW/MID/HIGH`；lengthControl 显示 `Max words: N`；其他规则无此行

**步骤详情三个 detail block**：
- 蓝色 `Parameter used` 块：说明当前参数档位的含义（有参数的规则才显示）
- 绿色 `Before / After` 块：展示 inputText 和 outputText，应用词级别 diff 高亮（见 6.4）
- 黄色 `Changes made` 块：逐条显示 changes 列表，每条格式 `→ change_text`

**底部按钮**：`← Back`（返回 Page 1）、`View Final Result →`（前进 Page 3）

---

### 6.3 第 3 页：Token Analysis
✅ 已完成并测试

**Stats 统计栏**：同 Page 2

**Before/After 对比展示**（两列 compare-col）：
- 左列：Original Prompt（带 `Before` 红色 badge）+ 词级别 diff（删除词红色删除线）
- 右列：Optimized Prompt（带 `After` 绿色 badge）+ 词级别 diff（新增词绿色高亮）+ `Copy` 按钮
- 底部分别显示原始 token 数和最终 token 数（最终为绿色）

**词级别 diff 高亮逻辑**（`generateDiffBefore` / `generateDiffAfter`）：
- 将文本用 `\s+` 分割为词数组
- Before 视图：在 before 词集合中出现、但在 after 词集合（lowercase）中不存在的词 → `<span class="diff-del">` 红色删除线
- After 视图：在 after 词集合中出现、但在 before 词集合（lowercase）中不存在的词 → `<span class="diff-add">` 绿色高亮
- 匹配方式：基于 Set 的逐词比较（非序列 diff），大小写不敏感

**Applied Rules chips**（`chipsRow`）：
- 按 RULE_ORDER 顺序渲染，skipped 的规则不显示
- tokensSaved > 0 → 绿色 chip（`${ruleName} −N`）
- tokensSaved < 0 → 红色 chip（`${ruleName} +N`）
- tokensSaved = 0 → 灰色 chip（`${ruleName} 0`）

**贡献度柱状图**（`chartCanvas`，Canvas 实现）：
- 横向条形图，每条规则一行
- 标签区宽 140px，数值区宽 48px，条形宽度按 tokensSaved 占最大值的比例计算
- 条形颜色：value ≥ 0 → 蓝色（`#1a73e8`）；< 0 → 红色（`#d93025`）
- 支持 `window.devicePixelRatio` 高分屏适配
- 窗口 resize 后 150ms 防抖重绘
- 数据为空时显示占位文字 `"No data yet."`

**Copy 按钮功能**：
- 优先使用 `navigator.clipboard.writeText()`
- 降级方案：创建临时 textarea + `document.execCommand('copy')`
- 复制成功后按钮文本变为 `✓ Copied!` 并添加 `.copied` CSS 类，2000ms 后恢复

**底部按钮**：`🔄 Optimize Again`（返回 Page 1 保留文本）、`✨ New Prompt`（清空所有状态返回 Page 1）、`Run Quality Check →`（跳转 Page 4 触发质量对比）

---

### 6.4 全局
✅ 已完成并测试

**面包屑导航逻辑**（`updateCrumbs()`）：
- 当前页：圆点图标（`⬤`）+ `.active` CSS 类
- 已完成页：对号图标（`✓`）+ `.done` CSS 类（完成优化后 Page 1/2/3 均加入 `state.completedPages`）
- 未到达页：数字圆圈（`①②③`）

**页面切换规则**（`crumbClick(n)` / `goToPage(n)`）：
- 点击面包屑：只能跳转到当前页或已完成页（`state.completedPages.has(n)`）
- 前进只能通过 Optimize 按钮（跳转 Page 2）或"View Final Result →"按钮（跳转 Page 3）
- 后退可用"← Back"按钮
- 切换页面后自动滚动到顶部（`window.scrollTo(0, 0)`）

**状态管理**（`state` 对象，全局单例）：
- `state.prompt`：当前 textarea 内容
- `state.rules`：7 条规则的 enabled 和 params 配置
- `state.result`：最近一次 optimize 的完整响应
- `state.currentPage`：当前页码（1/2/3/4）
- `state.completedPages`：已解锁的页码集合（Set）；Page 4 在点击 "Run Quality Check" 时写入

**ℹ 弹窗系统**（`showModal` / `closeModal`）：
- 所有 7 条规则均有 ℹ 按钮，均有对应的 `RULE_INFO` 条目
- 弹窗内容结构：What it does → Parameters（有参数的规则）→ Example（Before/After）→ Planned improvements
- 关闭方式：点击 `×` 按钮、点击遮罩层（`onclick="closeModal()"`）、按 `Escape` 键
- 打开时 `document.body.style.overflow = 'hidden'` 禁止背景滚动；关闭时恢复

---

## 七、质量对比（Quality Check）

### 7.1 POST /api/compare 接口
✅ 已完成并测试

**接口**：`QualityComparisonController.java`，`POST /api/compare`

**请求体**：
```json
{
  "originalPrompt":  "string",
  "optimizedPrompt": "string",
  "tokensBefore":    20,
  "tokensAfter":     12
}
```

**响应**：`QualityComparisonResult`（见 7.3）

---

### 7.2 回答生成（OpenAI gpt-4o-mini）
✅ 已完成并测试

**并行调用**：用 `CompletableFuture.supplyAsync` 同时向 OpenAI 发起两个请求，分别以 `originalPrompt` 和 `optimizedPrompt` 为用户输入，互不阻塞。

**System Prompt（回答生成）**：
> 你是一个直接回答问题的助手。用自然、流畅的语言回答，不要使用 markdown 格式，不要用代码块、标题、编号列表、加粗等格式符号。像和人对话一样，直接说清楚答案就好。

**模型**：`gpt-4o-mini`，接口：`https://api.openai.com/v1/chat/completions`

**API Key 注入**：通过 `@Value("${openai.api.key}")` 从 `application.properties` 读取

**HTTP 实现**：Java 原生 `java.net.http.HttpClient`，不引入任何新依赖

---

### 7.3 三维度打分系统
✅ 已完成并测试

**打分维度**（原始和优化后各打一次，共6个分数）：

| 维度 | 说明 | 字段名（before/after） |
|------|------|-----------------------|
| 切题性（Relevance） | 回答是否直接回应了问题核心 | `relevanceScoreBefore` / `relevanceScoreAfter` |
| 信息密度（Density） | 有效信息量与总字数的比值，冗余多则扣分 | `densityScoreBefore` / `densityScoreAfter` |
| 表达清晰度（Clarity） | 逻辑是否清晰，是否容易理解 | `clarityScoreBefore` / `clarityScoreAfter` |

**评分标准（System Prompt 约束）**：
- 5分 = 合格，能回答问题但有明显不足
- 7分 = 良好，回答准确且基本完整
- 9分以上 = 优秀，必须有充分理由才能给
- 不因为"看起来很长很详细"给高分

**综合分计算**：
```java
originalScore  = Math.round((relevanceBefore + densityBefore + clarityBefore) / 3.0)
optimizedScore = Math.round((relevanceAfter  + densityAfter  + clarityAfter)  / 3.0)
```

**JSON 解析**：`extractIntField` 方法逐字符解析，无外部 JSON 库；`extractStringField` 解析 naturalSummary 字符串字段

---

### 7.4 Token 效率比公式
✅ 已完成并测试

**公式**：
```
efficiencyBefore = originalScore  / tokensBefore
efficiencyAfter  = optimizedScore / tokensAfter
optimizationScore = (efficiencyAfter - efficiencyBefore) / efficiencyBefore × 100
```

**含义**：每个 token 产出的质量变化幅度（百分比）。正数表示同等 token 预算下，优化后 prompt 能换来更好的回答。

**精度**：`Math.round(... * 10000.0) / 100.0`，保留两位小数

**防除零**：`tokensBefore`、`tokensAfter`、`efficiencyBefore` 均有零值守卫

---

### 7.5 Verdict 四级判定
✅ 已完成并测试

| 阈值 | Verdict |
|------|---------|
| `optimizationScore >= 20` | 显著提升 |
| `optimizationScore >= 5`  | 轻微提升 |
| `optimizationScore >= -5` | 无明显变化 |
| `optimizationScore < -5`  | 优化后变差 |

---

### 7.6 AI 自然语言分析（naturalSummary）
✅ 已完成并测试

**要求**（打分 user prompt 中指定）：3-5句话，中文；具体说明三个维度的变化；举出具体例子；最后一句话总结是否值得优化；不用"总的来说"开头。

**前端展示**：蓝色左边框卡片（`.qc-ai-analysis`），位于两列回答区域和综合评估卡片之间

---

### 7.7 前端第四页：Quality Check
✅ 已完成并测试

**页面结构**（`page-4`）：

1. **两列回答对比**：左列原始 prompt 回答，右列优化后 prompt 回答；每列顶部显示 `#/10` 综合分 badge（绿/黄/红根据分数段）
2. **维度分数进度条**（每列底部）：切题性 / 信息密度 / 表达清晰度，每行 label + 进度条（高度 6px，圆角）+ 分数；颜色规则：`>=7` 绿 / `>=5` 黄 / `<5` 红
3. **AI 分析卡片**：蓝色左边框，浅蓝背景，展示 naturalSummary
4. **Token Efficiency Gain 综合卡片**：大字号显示 `optimizationScore`（正数绿色 `+XX.XX%`，负数红色，零灰色）+ verdict 色块（四色对应四级）+ token 节省百分比说明

**入口**：Page 3（Token Analysis）底部的 `Run Quality Check →` 按钮；点击后直接操作 DOM 跳转至 page-4，不经过 goToPage 导航守卫

---

## 八、待开发功能清单

### 优先级高

**Claude API 接入 — AI Generate 按钮**
- 📋 待开发
- 入口：`AiPromptGenerator.java` 的 `generate()` 方法 TODO
- 前置条件：在 `application.properties` 中填入 `anthropic.api.key`
- 实现计划：POST `https://api.anthropic.com/v1/messages`，model `claude-opus-4-6`，system prompt 指定任务类型和废话程度，解析 content[0].text 返回
- 前端联动：移除 `index.html` 中 AI Generate 按钮的 `disabled` 属性，改传 `source=ai`

### 优先级中

**真实 BPE Tokenizer**
- 📋 待开发
- 替换 `TokenCounter.java` 的空格估算
- 目标：使用 Anthropic 官方 tokenizer 或 tiktoken，按子词单元（subword units）计数

**Level 3 上下文优化**
- 📋 待开发（`RuleRegistryConfig.java` 中已有注释占位）
- `ContextCompressorRule` — 历史裁剪
- `ConstraintNormalizerRule` — 约束规范化

**TaskAnalyzerRule ML 升级**
- 📋 待开发（来自 `TaskAnalyzerRule.java` 代码注释）
- 使用 BERT/DistilBERT 细粒度意图分类，替换关键词匹配
- 下游规则根据任务类型动态调整策略

**SemanticCompressorRule 语义检测升级**
- 📋 待开发（来自 `SemanticCompressorRule.java` 代码注释）
- 使用 sentence-transformers 检测任意释义对，不限于固定词表
- 支持共指消解（coreference）：将重复名词短语替换为代词

### 优先级低

- 📋 Level 4 系统级优化（`RuleRegistryConfig.java` 占位注释：`LLMRewriteRule`）
- 📋 Level 5 高级优化
- 📋 StructureMinimizerRule 升级：检测重复段落、冗余 Markdown 标题、空列表项
- 📋 RedundancySuppressorRule 升级：语义相似度检测、多句结尾套话块处理
- 📋 LengthControlRule 升级：摘要替代截断、句子边界感知

---

*最后更新：2026/04/04 · 维护人：Andy*
