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

- 当前版本：v1.3
- 当前阶段：UI 重构中
- 已完成模块数：7/7（Level 1 + Level 2 全部实现）
- 下一步：接入 Anthropic Claude API

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

### ✅ v1.1 — 真实算法实现（2026/3/26）

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

### ✅ v1.2 — Prompt Generator（2026/3/27）

产出：
- TemplatePromptGenerator：45+模板，覆盖5种任务x3种废话程度
- AiPromptGenerator：占位符，TODO接入Claude API
- GET /api/generator/prompt 接口
- 前端 Generator 面板（Task Type + Verbosity 下拉 + Generate按钮）
- AI Generate 按钮置灰，hover显示 "Coming Soon"

状态：模板生成可用，AI生成待接入

---

### ✅ v1.3 — UI 重构（2026/3/31）

产出：
- 经典 Google Material Design 风格
- 三页 SPA：Input & Config / Pipeline 详情 / Final Result
- Pipeline 每步展开显示：参数说明 + Before/After diff + 改动清单
- 右侧策略面板：每条规则可独立开关 + LOW/MID/HIGH 切换
- ℹ 弹窗：每条规则点击查看详细说明 + 示例 + 未来计划
- Token 统计卡片 + 贡献度柱状图（原生 canvas）

状态：UI 重构进行中

---

## 待完成功能

| 功能 | 优先级 | 说明 |
|------|--------|------|
| Claude API 接入（AI Generate） | 高 | AiPromptGenerator.java 已预留接口 |
| Claude API 接入（质量对比） | 高 | 优化前后各发一次，对比回答质量 |
| 真实 BPE Tokenizer | 中 | 替换 TokenCounter 的空格估算 |
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

---

## 风险与阻塞

| 日期 | 类型 | 描述 | 状态 |
|------|------|------|------|
| 2026/3/25 | 阻塞 | Claude Code 生成文件路径不在 IntelliJ 项目目录 | ✅ 已解决：在项目目录内启动 Claude Code |
| 2026/3/31 | 阻塞 | style.css 写入失败（文件被 IntelliJ 锁定） | ✅ 已解决：停止项目运行后重试 |
| 2026/3/31 | 风险 | Token 计数用空格分词，与真实 BPE 有偏差 | ⏳ 待解决：v2.1 替换真实 tokenizer |

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

1. 完成 UI 重构测试，确认三页流程跑通
2. 接入 Anthropic Claude API（AI Generate 功能）
3. 实现优化前后质量对比功能
4. 更新本日志

---

*最后更新：2026/3/31 · 维护人：Andy*
