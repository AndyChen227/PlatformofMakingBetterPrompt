# BetterPrompt

![Java](https://img.shields.io/badge/Java-21-orange?style=flat-square&logo=java)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.5-brightgreen?style=flat-square&logo=springboot)
![Maven](https://img.shields.io/badge/Build-Maven-red?style=flat-square&logo=apachemaven)
![License](https://img.shields.io/badge/License-MIT-blue?style=flat-square)

> A systematic LLM prompt optimization engine with real-time pipeline visualization.

BetterPrompt is a portfolio project built to demonstrate algorithm design and software engineering architecture. It takes a raw, verbose prompt and runs it through a configurable multi-rule optimization pipeline — reducing token usage while preserving the semantic intent of the original input.

---

## Table of Contents

- [Background](#background)
- [Features](#features)
- [Architecture](#architecture)
- [Project Structure](#project-structure)
- [Optimization Pipeline](#optimization-pipeline)
- [API Reference](#api-reference)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Roadmap](#roadmap)
- [Engineering Decisions](#engineering-decisions)

---

## Background

When people talk about "prompt optimization," they usually mean one of two things: make the answer shorter, or reduce API costs. BetterPrompt approaches it differently.

The core insight is that most prompts sent to LLMs carry significant **structural noise**: social greetings that the model ignores (`Hello! I hope you're doing well!`), verbose phrases that add words without adding meaning (`in order to` vs `to`), redundant closing remarks (`please let me know if you have any questions`), and formatting instructions that can be expressed in one character instead of twelve (`bullet points:` → `•`).

None of these affect the quality of the model's response — but they all consume tokens. At scale, that matters. More importantly, recognizing and removing them requires a systematic, rule-based approach rather than ad-hoc edits.

This project models the optimization process as a **configurable pipeline of discrete, composable rules**. Each rule has a single responsibility, produces an auditable diff of what it changed, and can be independently enabled, disabled, or tuned. The architecture is designed so that adding a new optimization rule requires changing exactly one file.

---

## Features

### Four-Page UI

**Page 1 — Input & Configuration**
- Paste or generate a prompt in the left column
- Configure which rules to run and at what aggressiveness level in the right column
- Level 1 rules (input processing) are visually distinguished from Level 2 rules (output control)
- Real-time word count feedback on the prompt textarea

**Page 2 — Optimization Pipeline**
- Every rule in the pipeline produces a visible step card
- Each card shows: rule name, level, status (`done` / `skipped`), tokens before/after, and a list of specific changes made
- Side-by-side before/after text diff per step
- Token savings accumulate across steps

**Page 3 — Token Analysis**
- Original prompt vs. optimized prompt comparison
- Aggregate compression statistics: original tokens, final tokens, compression rate
- Bar chart showing token savings broken down by rule
- Applied rules displayed as chips
- Actions to re-optimize, reset, or proceed to Quality Check

**Page 4 — Quality Check**
- Side-by-side display of ChatGPT's responses to the original and optimized prompts
- Three-dimension scoring: Relevance, Information Density, Clarity (each 1–10, strict rubric)
- Per-dimension progress bars with colour coding (green ≥7 / yellow ≥5 / red <5)
- Token Efficiency Gain: quantifies optimization as `(quality/token after − quality/token before) / efficiency before × 100%`
- Four-level verdict: 显著提升 (≥20%) / 轻微提升 (≥5%) / 无明显变化 (≥−5%) / 优化后变差 (<−5%)
- AI natural-language analysis: 3–5 sentence Chinese summary of what specifically changed across all three dimensions

### Prompt Generator

The built-in generator produces "dirty" prompts — intentionally verbose, polite, and filler-heavy — designed to give the optimizer meaningful input to work with. It covers:

- **5 task types**: `CODING`, `EXPLAIN`, `DEBUG`, `WRITING`, `COMPARE`
- **3 verbosity levels**: `LOW` (minimal filler), `MEDIUM` (greeting + closing + some verbose phrases), `HIGH` (stacked salutations, maximal noise)
- **3 variants per combination** = 45 total templates, selected randomly per call

### Optimization Rules

| # | Rule | Level | Configurable Parameter |
|---|------|-------|----------------------|
| 1 | Filler Removal | Level 1 | `aggressiveness` (LOW / MID / HIGH) |
| 2 | Case Normalizer | Level 1 | `uppercaseRatioThreshold`, `minLetters` |
| 3 | Task Analyzer | Level 1 | — |
| 4 | Semantic Compressor | Level 1 | `compressionLevel` (LOW / MID / HIGH) |
| 5 | Structure Minimizer | Level 1 | — |
| 6 | Duplicate Sentence Remover | Level 1 | — |
| 7 | Duplicate Phrase Reducer | Level 1 | `maxPhraseLength`, `caseInsensitive` |
| 8 | Punctuation Normalizer | Level 1 | — |
| 9 | Number Normalizer | Level 1 | — |
| 10 | Sentence Budget | Level 2 | `maxSentences` (integer) |
| 11 | Length Control | Level 2 | `maxWords` (integer) |
| 12 | Format Control | Level 2 | — |

### Pipeline Visualization

Every optimization run produces a full audit trail. The frontend renders each rule's execution as a step card showing exactly what text went in, what came out, which specific phrases were removed or replaced, and how many tokens were saved. This makes the pipeline transparent and debuggable.

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Browser (SPA)                            │
│  ┌──────────┐   ┌─────────────────┐   ┌──────────────────────┐ │
│  │  Page 1  │   │     Page 2      │   │       Page 3         │ │
│  │  Input & │   │    Pipeline     │   │   Final Result &     │ │
│  │  Config  │   │ Visualization   │   │   Token Stats        │ │
│  └────┬─────┘   └────────┬────────┘   └──────────────────────┘ │
│       │                  │                                       │
│       │   app.js (state management, DOM, API calls)             │
└───────┼──────────────────┼───────────────────────────────────── ┘
        │                  │
        │ POST /api/optimize│ GET /api/rules
        │ GET /api/generator/prompt
        ▼                  ▼
┌───────────────────────────────────────────────────────────────── ┐
│                     Spring Boot (port 8080)                      │
│                                                                   │
│  ┌──────────────────────┐   ┌─────────────────────────────────┐  │
│  │  OptimizerController │   │  PromptGeneratorController      │  │
│  │  POST /api/optimize  │   │  GET /api/generator/prompt      │  │
│  │  GET  /api/rules     │   │                                 │  │
│  └──────────┬───────────┘   └─────────────┬───────────────────┘  │
│             │                             │                       │
│             ▼                             ▼                       │
│  ┌──────────────────────┐   ┌─────────────────────────────────┐  │
│  │      RuleEngine      │   │     TemplatePromptGenerator     │  │
│  │  (Chain of Resp.)    │   │     45 templates, 5 types       │  │
│  └──────────┬───────────┘   └─────────────────────────────────┘  │
│             │                                                      │
│             ▼  iterates List<Rule> in registered order            │
│  ┌──────────────────────────────────────────────────────────────┐ │
│  │                    RuleRegistryConfig                        │ │
│  │              (IoC — single source of rule order)             │ │
│  └──────────────────────────────────────────────────────────────┘ │
│             │                                                      │
│     ┌───────┴──────────────────────────────────┐                  │
│     │              Rule (interface)             │                  │
│     │  getRuleId / getRuleName / getRuleLevel   │                  │
│     │  apply(inputText, config) → StepResult   │                  │
│     └───────┬──────────────────────────────────┘                  │
│             │                                                      │
│   ┌─────────┴──────────────────────────────────────────────────┐  │
│   │  Level 1                          Level 2                  │  │
│   │  ├── FillerRemovalRule            ├── SentenceBudgetRule   │  │
│   │  ├── CaseNormalizerRule           ├── LengthControlRule    │  │
│   │  ├── TaskAnalyzerRule             └── FormatControlRule    │  │
│   │  ├── SemanticCompressorRule                                │  │
│   │  ├── StructureMinimizerRule                                │  │
│   │  ├── DuplicateSentenceRemoverRule                          │  │
│   │  ├── DuplicatePhraseReducerRule                            │  │
│   │  ├── PunctuationNormalizerRule                             │  │
│   │  └── NumberNormalizerRule                                  │  │
│   └────────────────────────────────────────────────────────────┘  │
└────────────────────────────────────────────────────────────────── ┘
```

### Design Patterns

**Strategy Pattern — `Rule` interface**

Every optimization rule implements the same `Rule` interface:

```java
public interface Rule {
    String getRuleId();
    String getRuleName();
    String getRuleLevel();
    String getDescription();
    StepResult apply(String inputText, RuleConfig config);
}
```

Each rule is a fully encapsulated strategy. The engine does not need to know how any individual rule works — it just calls `apply()` and collects the result. New rules can be added without touching any existing class.

**Chain of Responsibility — `RuleEngine`**

`RuleEngine` iterates the registered `List<Rule>` in order. The output of each rule becomes the input of the next. If a rule is disabled, the engine passes the text through unchanged and marks the step as `skipped`. The chain produces a complete audit trail of `StepResult` objects, one per rule.

**Registry / IoC — `RuleRegistryConfig`**

Rule registration and execution order live in a single `@Configuration` class that returns a `List<Rule>` Spring bean. This is the only file that needs to be edited to add, remove, or reorder a rule. No controller, engine, or test code changes are required.

---

## Project Structure

```
BetterPromptByAndyy2.0/
├── pom.xml                                         # Maven build config (Java 21, Spring Boot 3.5)
├── mvnw / mvnw.cmd                                 # Maven wrapper scripts
│
└── src/
    ├── main/
    │   ├── java/com/betterprompt/betterpromptbyandyy2/
    │   │   │
    │   │   ├── Application.java                    # Spring Boot entry point (@SpringBootApplication)
    │   │   │
    │   │   ├── api/
    │   │   │   └── OptimizerController.java        # POST /api/optimize, GET /api/rules
    │   │   │
    │   │   ├── config/
    │   │   │   └── RuleRegistryConfig.java         # Rule registration & execution order (single source of truth)
    │   │   │
    │   │   ├── generator/
    │   │   │   ├── AiPromptGenerator.java          # OpenAI gpt-4o-mini prompt generator
    │   │   │   ├── PromptGeneratorController.java  # GET /api/generator/prompt
    │   │   │   ├── PromptTemplate.java             # Value object for template metadata
    │   │   │   └── TemplatePromptGenerator.java    # 45 templates (5 types × 3 verbosity × 3 variants)
    │   │   │
    │   │   ├── model/
    │   │   │   ├── OptimizationRequest.java        # API request DTO { prompt, rules }
    │   │   │   ├── OptimizationResult.java         # API response DTO { steps, finalPrompt, tokenStats }
    │   │   │   ├── RuleConfig.java                 # Per-rule config { enabled, params }
    │   │   │   └── StepResult.java                 # Per-rule result { before, after, changes, status }
    │   │   │
    │   │   └── optimizer/
    │   │       ├── Rule.java                       # Rule interface (Strategy pattern)
    │   │       ├── RuleEngine.java                 # Pipeline executor (Chain of Responsibility)
    │   │       ├── TokenCounter.java               # jtokkit o200k_base BPE token counter
    │   │       ├── util/
    │   │       │   └── ProtectedTextProcessor.java # Protect fenced code blocks and inline code
    │   │       │
    │   │       ├── level1/                         # Input Processing Rules
    │   │       │   ├── FillerRemovalRule.java      # Remove greetings, openers, mid-text fillers, and closing remarks
    │   │       │   ├── CaseNormalizerRule.java     # Conservative all-uppercase prompt normalization
    │   │       │   ├── TaskAnalyzerRule.java       # Classify task type and complexity
    │   │       │   ├── SemanticCompressorRule.java # Verbose-phrase → concise substitutions
    │   │       │   ├── StructureMinimizerRule.java # Whitespace and blank-line normalization
    │   │       │   ├── DuplicateSentenceRemoverRule.java # Remove fully duplicated complete sentences
    │   │       │   ├── DuplicatePhraseReducerRule.java # Remove consecutive duplicated short phrases
    │   │       │   ├── PunctuationNormalizerRule.java # Repeated punctuation normalization
    │   │       │   └── NumberNormalizerRule.java   # Written numbers → Arabic numerals
    │   │       │
    │   │       └── level2/                         # Output Control Rules
    │   │           ├── SentenceBudgetRule.java     # Sentence-count limit
    │   │           ├── LengthControlRule.java      # Hard word-count truncation
    │   │           └── FormatControlRule.java      # Verbose format instructions → symbols
    │   │
    │   └── resources/
    │       ├── application.properties              # Port, app name, OpenAI API config
    │       └── static/
    │           ├── index.html                      # 3-page SPA shell
    │           ├── style.css                       # Material Design styles (pure CSS)
    │           └── app.js                          # UI state management, DOM, API calls
    │
    └── test/
        └── java/.../ApplicationTests.java          # Spring context load test
```

---

## Optimization Pipeline

### Level 1 — Input Processing

These rules run first and operate on the raw input prompt. Their job is to strip noise before any output-control rules see the text.

---

#### 1. Filler Removal

Removes social filler from prompts across all positions: greetings at the start, polite openers, mid-text filler words, and closing remarks at the end. Controlled by the `aggressiveness` parameter, mapped to three tiers:

| Tier | Range | Opening removed | Closing remarks removed |
|------|-------|-----------------|------------------------|
| LOW  | 0–30  | `hello`, `hi`, `hey`, `good morning/afternoon/evening` | "I hope this helps", "thanks in advance", "thank you for your time" |
| MID  | 31–70 | LOW + `please`, `could you`, `can you`, `would you`, `I need you to`, `I was hoping you could` | LOW + "please let me know", "feel free to ask", "don't hesitate", "let me know if you have any questions", "if you have any questions" |
| HIGH | 71–100 | MID + `I was wondering if`, `I'd like you to`, `I am reaching out because` + mid-text fillers: `basically`, `essentially`, `literally`, `actually` | MID + "looking forward to your response", "best regards", "kind regards", "hope that makes sense" |

The change log records every specific removal, e.g.:
```
[aggressiveness=HIGH] 删除强寒暄: 'Hello!'
[aggressiveness=HIGH] 删除 filler 词: 'basically'
[aggressiveness=HIGH] 删除结尾套话: 'Best regards!'
```

---

#### 2. Case Normalizer

Conservatively normalizes clearly all-uppercase prompts into sentence case before task analysis. It only triggers when the uppercase-letter ratio is high enough, with a default threshold of `0.9`, to avoid rewriting normal mixed-case prompts.

Skips Markdown fenced code blocks and inline code through `ProtectedTextProcessor`.

Current execution order: `FillerRemovalRule → CaseNormalizerRule → TaskAnalyzerRule`.

---

#### 3. Task Analyzer

Classifies the prompt by task type and complexity. The analysis is recorded in the step changes for visibility/debugging; the rule does not modify the prompt text and does not append metadata tags.

**Task classification** — keyword matching in priority order:

| Priority | Task Type | Example keywords |
|----------|-----------|-----------------|
| 1 (highest) | DEBUG | `bug`, `error`, `fix`, `debug`, `issue`, `crash`, `exception` |
| 2 | CODING | `write`, `code`, `implement`, `function`, `class`, `algorithm` |
| 3 | EXPLAIN | `explain`, `what is`, `how does`, `describe`, `summarize` |
| 4 | WRITING | `write`, `draft`, `essay`, `email`, `article`, `blog` |
| 5 (lowest) | COMPARE | `compare`, `difference`, `vs`, `versus`, `pros and cons` |

**Complexity classification** — based on word count:

| Complexity | Word count |
|-----------|-----------|
| LOW | < 15 words |
| MEDIUM | 15–40 words |
| HIGH | > 40 words |

Output text remains unchanged by this rule.

---

#### 4. Semantic Compressor

Replaces verbose multi-word phrases with shorter semantic equivalents. Uses a tiered substitution table controlled by the `compressionLevel` parameter.

Skips Markdown fenced code blocks and inline code through `ProtectedTextProcessor`.

| Tier | Range | Substitution pairs |
|------|-------|--------------------|
| LOW  | 0–30  | 8 safe substitutions |
| MID  | 31–70 | 19 total substitutions |
| HIGH | 71–100 | 29 total substitutions |

**Sample substitutions (cumulative by tier):**

| Verbose phrase | Concise replacement | Tier |
|----------------|--------------------|----|
| `in order to` | `to` | LOW |
| `due to the fact that` | `because` | LOW |
| `at this point in time` | `now` | LOW |
| `in the event that` | `if` | LOW |
| `make a decision` | `decide` | MID |
| `take into consideration` | `consider` | MID |
| `come to the conclusion` | `conclude` | MID |
| `has the ability to` | `can` | HIGH |
| `is able to` | `can` | HIGH |
| `in spite of the fact that` | `although` | HIGH |

---

#### 5. Structure Minimizer

Normalizes whitespace and blank lines — purely structural, no semantic content is changed.

Four operations applied in sequence:

1. Trim trailing whitespace from every line
2. Collapse runs of 3 or more consecutive blank lines into a single blank line
3. Collapse multiple consecutive spaces into a single space
4. Strip leading and trailing whitespace from the full text

---

#### 6. Duplicate Sentence Remover

Removes fully duplicated sentences while keeping the first occurrence. This reduces repeated token usage without changing the user's intended meaning. The first implementation handles exact duplicates after simple normalization.

Current execution order: `StructureMinimizerRule → DuplicateSentenceRemoverRule → PunctuationNormalizerRule`.

---

#### 7. Duplicate Phrase Reducer

Removes consecutive duplicated words or short phrases inside a sentence. The first implementation handles exact adjacent duplicates up to trigrams, such as `simple simple` or `step by step step by step`, without attempting semantic similarity.

Skips Markdown fenced code blocks and inline code through `ProtectedTextProcessor`.

Current execution order: `DuplicateSentenceRemoverRule → DuplicatePhraseReducerRule → PunctuationNormalizerRule`.

---

#### 8. Punctuation Normalizer

Compresses repeated punctuation and normalises ellipses. Three operations applied in sequence:

1. Collapse two or more consecutive exclamation marks → single `!` (e.g. `!!!` → `!`)
2. Collapse two or more consecutive question marks → single `?` (e.g. `??` → `?`)
3. Normalise four or more consecutive periods → standard ellipsis `...` (e.g. `....` → `...`)

Example: `"Is this right?? Sure!! Let me think...."` → `"Is this right? Sure! Let me think..."`

Skips Markdown fenced code blocks and inline code through `ProtectedTextProcessor`.

---

#### 9. Number Normalizer

Converts written English numbers and percentages to their Arabic numeral equivalents using a full place-value parser.

Two-phase processing (percent phrases first, then plain numbers):

| Written form | Converted form |
|---|---|
| `two hundred and fifty three` | `253` |
| `one million two hundred thousand` | `1200000` |
| `fifty percent` | `50%` |
| `seventy five percentage` | `75%` |

The parser supports ones (zero–nineteen), tens (twenty–ninety), and scale words (hundred, thousand, million) in arbitrary combination.

Skips Markdown fenced code blocks and inline code through `ProtectedTextProcessor`.

#### Protected Text Safety Layer

`ProtectedTextProcessor` is a shared utility layer used by the high-risk text transformation rules above. It preserves Markdown fenced code blocks and inline code byte-for-byte while allowing normal natural-language text outside those regions to be optimized.

Current scope: fenced code blocks using triple backticks and inline code wrapped in single backticks. Future work: quoted text, Markdown tables, JSON-like blocks outside fenced code, and custom delimiters.

---

### Level 2 — Output Control

These rules apply after the input has been cleaned and compressed. They enforce hard limits and format constraints.

---

#### 10. Sentence Budget

Limits the prompt by maximum sentence count before word-budget truncation. If the prompt exceeds `maxSentences` (default: 3), it keeps the first N complete sentences and appends an ellipsis. This rule runs before Length Control.

---

#### 11. Length Control

Acts as the final hard word-budget guard. If the prompt still exceeds `maxWords` (default: 50) after earlier optimization rules, it truncates the text at a word boundary and appends `...`.

---

#### 12. Format Control

Replaces verbose, human-readable formatting instructions with their compact symbol equivalents.

| Verbose instruction | Compact replacement |
|---------------------|---------------------|
| `bullet points:` | `•` |
| `numbered list:` | `1.` |
| `in bold:` | `**` |
| `underline:` | `_` |

---

---

## API Reference

### POST /api/optimize

Run the full optimization pipeline on a prompt.

**Request body:**

```json
{
  "prompt": "Hello! I was hoping you could please help me write a Python function to sort a list. Thanks in advance!",
  "rules": {
    "fillerRemoval": {
      "enabled": true,
      "params": { "aggressiveness": 85 }
    },
    "taskAnalyzer": {
      "enabled": true,
      "params": {}
    },
    "semanticCompressor": {
      "enabled": true,
      "params": { "compressionLevel": 50 }
    },
    "structureMinimizer": {
      "enabled": true,
      "params": {}
    },
    "lengthControl": {
      "enabled": true,
      "params": { "maxWords": 50 }
    },
    "formatControl": {
      "enabled": true,
      "params": {}
    }
  }
}
```

**Response:**

```json
{
  "steps": [
    {
      "ruleName": "Input Cleaner",
      "ruleLevel": "Level 1",
      "inputText": "Hello! I was hoping you could please help me write a Python function to sort a list. Thanks in advance!",
      "outputText": "write a Python function to sort a list. Thanks in advance!",
      "tokensBefore": 20,
      "tokensAfter": 12,
      "tokensSaved": 8,
      "changes": [
        "[aggressiveness=HIGH] Removed filler opener: 'Hello!'",
        "[aggressiveness=HIGH] Removed filler opener: 'I was hoping you could please help me'"
      ],
      "status": "done"
    },
    {
      "ruleName": "Task Analyzer",
      "ruleLevel": "Level 1",
      "inputText": "write a Python function to sort a list. Thanks in advance!",
      "outputText": "write a Python function to sort a list. Thanks in advance!",
      "tokensBefore": 12,
      "tokensAfter": 12,
      "tokensSaved": 0,
      "changes": ["Detected task type: CODING", "Detected complexity: LOW"],
      "status": "done"
    }
  ],
  "finalPrompt": "Write a Python function to sort a list.",
  "tokenStats": {
    "original": 20,
    "final": 12,
    "compressionRate": 40.0,
    "byRule": {
      "Input Cleaner": 8,
        "Task Analyzer": 0,
      "Semantic Compressor": 2,
      "Structure Minimizer": 0,
      "Length Control": 0,
      "Format Control": 0,
      "Redundancy Suppressor": 3
    }
  }
}
```

---

### POST /api/compare

Run a quality comparison between the original and optimized prompt by querying GPT-4o-mini for both answers, scoring them across three dimensions, and computing the token efficiency gain.

**Request body:**

```json
{
  "originalPrompt":  "Hello! Could you please help me understand what recursion is?",
  "optimizedPrompt": "Explain recursion.",
  "tokensBefore":    13,
  "tokensAfter":     3
}
```

**Response:**

```json
{
  "originalPrompt":       "Hello! Could you please help me understand what recursion is?",
  "optimizedPrompt":      "Explain recursion.",
  "originalAnswer":       "Recursion is when a function calls itself...",
  "optimizedAnswer":      "Recursion is a technique where a function calls itself to solve a smaller version of the same problem...",
  "originalScore":        7,
  "optimizedScore":       8,
  "relevanceScoreBefore": 7,
  "relevanceScoreAfter":  8,
  "densityScoreBefore":   6,
  "densityScoreAfter":    8,
  "clarityScoreBefore":   7,
  "clarityScoreAfter":    7,
  "naturalSummary":       "优化后的 prompt 更加简洁直接，切题性从 7 分提升到 8 分，因为去掉了礼貌用语后模型直接回答核心概念。信息密度提升明显，冗余铺垫减少后每句话都在传递有效信息。表达清晰度保持不变，两个回答的逻辑结构相近。综合来看这次优化是值得的，用更少的 token 换来了更聚焦的回答。",
  "tokensBefore":         13,
  "tokensAfter":          3,
  "optimizationScore":    136.75,
  "verdict":              "显著提升"
}
```

**Field reference:**

| Field | Type | Description |
|-------|------|-------------|
| `originalScore` / `optimizedScore` | `int` | Average of the three dimension scores, rounded |
| `relevanceScoreBefore/After` | `int` | Relevance dimension (1–10) |
| `densityScoreBefore/After` | `int` | Information density dimension (1–10) |
| `clarityScoreBefore/After` | `int` | Clarity dimension (1–10) |
| `naturalSummary` | `String` | 3–5 sentence Chinese analysis from the model |
| `optimizationScore` | `double` | Token efficiency gain (%) — positive = more quality per token |
| `verdict` | `String` | 显著提升 / 轻微提升 / 无明显变化 / 优化后变差 |

---

### GET /api/rules

Returns metadata for all registered rules in pipeline order.

**Response:**

```json
[
  {
    "id": "fillerRemoval",
    "name": "Filler Removal",
    "level": "Level 1",
    "description": "Removes greetings, polite openers, mid-text fillers, and closing remarks"
  },
  {
    "id": "caseNormalizer",
    "name": "Case Normalizer",
    "level": "Level 1",
    "description": "Normalizes clearly all-uppercase prompts into sentence case"
  },
  {
    "id": "taskAnalyzer",
    "name": "Task Analyzer",
    "level": "Level 1",
    "description": "Classifies task type and complexity"
  },
  {
    "id": "semanticCompressor",
    "name": "Semantic Compressor",
    "level": "Level 1",
    "description": "Replaces verbose phrases with concise equivalents"
  },
  {
    "id": "structureMinimizer",
    "name": "Structure Minimizer",
    "level": "Level 1",
    "description": "Normalizes whitespace and blank lines"
  },
  {
    "id": "duplicateSentenceRemover",
    "name": "Duplicate Sentence Remover",
    "level": "Level 1",
    "description": "Removes fully duplicated sentences from the prompt"
  },
  {
    "id": "duplicatePhraseReducer",
    "name": "Duplicate Phrase Reducer",
    "level": "Level 1",
    "description": "Removes consecutive duplicated words or short phrases"
  },
  {
    "id": "punctuationNormalizer",
    "name": "Punctuation Normalizer",
    "level": "Level 1",
    "description": "Removes repeated punctuation and normalises ellipses"
  },
  {
    "id": "numberNormalizer",
    "name": "Number Normalizer",
    "level": "Level 1",
    "description": "Converts written English numbers to Arabic numerals"
  },
  {
    "id": "sentenceBudget",
    "name": "Sentence Budget",
    "level": "Level 2",
    "description": "Limits prompt length by sentence count before word truncation"
  },
  {
    "id": "lengthControl",
    "name": "Length Control",
    "level": "Level 2",
    "description": "Truncates prompt to a maximum word count"
  },
  {
    "id": "formatControl",
    "name": "Format Control",
    "level": "Level 2",
    "description": "Replaces verbose formatting instructions with symbols"
  }
]
```

---

### GET /api/generator/prompt

Generate a sample prompt for testing the optimizer.

**Query parameters:**

| Parameter | Values | Default |
|-----------|--------|---------|
| `type` | `CODING`, `EXPLAIN`, `DEBUG`, `WRITING`, `COMPARE` | `CODING` |
| `verbosity` | `LOW`, `MEDIUM`, `HIGH` | `HIGH` |
| `source` | `template`, `ai` | `template` |

**Example:** `GET /api/generator/prompt?type=DEBUG&verbosity=HIGH&source=template`

**Response:**

```json
{
  "prompt": "Hello there! I hope you're doing absolutely wonderfully today! I was truly hoping that you might be able to help me with something that has been giving me quite a bit of trouble...",
  "taskType": "DEBUG",
  "verbosity": "HIGH",
  "source": "template"
}
```

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Build | Maven |
| Frontend | HTML5, CSS3, JavaScript (no external libraries) |
| AI API | OpenAI gpt-4o-mini |
| Token counting | jtokkit 1.1.0 (OpenAI tiktoken Java port, `o200k_base` encoder) |

The frontend deliberately uses no third-party libraries. All UI components — cards, toggles, tier buttons, modals, the token bar chart — are implemented in vanilla JavaScript and CSS.

---

## Getting Started

### Prerequisites

- Java 21+
- Maven 3.8+ (or use the included `mvnw` wrapper)

### Clone

```bash
git clone <repository-url>
cd BetterPromptByAndyy2.0
```

### Configure (optional)

To enable AI prompt generation and Quality Check, set your OpenAI API key in `src/main/resources/application.properties`:

```properties
openai.api.key=sk-...
openai.model=gpt-4o-mini
```

This is optional. The optimizer pipeline and template generator work fully without an API key.

### Run

```bash
# Using Maven wrapper (recommended)
./mvnw spring-boot:run

# Or build and run the jar
./mvnw clean package
java -jar target/BetterPromptByAndyy2.0-*.jar
```

### Access

Open `http://localhost:8080` in your browser.

---

## Roadmap

- [x] v1.0.0–v1.2.0 — Spring Boot project scaffold, REST API skeleton, Level 1 & 2 rules, Prompt Generator, three-page SPA (tag: v1.0.4)
- [x] v1.3.0 — Quality Check feature (OpenAI-powered side-by-side comparison)
- [x] v1.4.0 — AI Generate (OpenAI-powered prompt generation)
- [x] v1.4.1–v1.4.2 — Punctuation Normalizer + Number Normalizer (tag: v3.0)
- [x] v1.4.3 — Merged FillerRemovalRule (Input Cleaner + Redundancy Suppressor)
- [x] v1.5.0 — Real BPE tokenizer via jtokkit (o200k_base, aligned with gpt-4o-mini); MOCK label cleanup; version-numbering alignment
- [x] v1.5.1 — SentenceBudgetRule (sentence-count limit before word-budget truncation)
- [x] v1.5.2 — DuplicateSentenceRemoverRule (remove fully duplicated sentences)
- [x] v1.5.3 — CaseNormalizerRule (conservative all-uppercase prompt normalization)
- [x] v1.5.4 — DuplicatePhraseReducerRule (remove consecutive duplicated short phrases)
- [x] v1.5.5 — Protected Text Safety Layer (partial Code Block Protector via `ProtectedTextProcessor`; high-risk rules skip fenced code blocks and inline code)
- [ ] v2.0.0 — Level 3: context optimization (deduplication, reference compression)
- [ ] v3.0.0 — Level 4 & 5: system-level optimization (system prompt factoring, conversation compression)

---

## Engineering Decisions

### Why Strategy + Chain of Responsibility instead of a monolithic optimizer?

A single class that applies all transformations in one pass would be simpler to write initially — but it makes testing, debugging, and extension significantly harder. With the Strategy pattern, each rule is independently testable and has a single reason to change. With the Chain of Responsibility, the pipeline is auditable: you can see exactly which rule made which change and how many tokens it saved. When a user reports unexpected output, the step-by-step log makes the cause immediately visible.

The practical payoff: when adding a new rule, only one file changes (`RuleRegistryConfig`). No existing code is touched, so no existing behavior can break.

### Why keyword matching for task classification instead of ML?

A machine learning classifier for five task categories would require a labeled training set, model hosting, inference latency, and retraining infrastructure. For this project, the classification is recorded in the rule's step changes for visibility/debugging; it does not need to be perfect, it needs to be fast and transparent.

Keyword matching is deterministic, zero-latency, fully debuggable, and requires no external dependencies. If the classification is wrong, the cause is immediately visible in the keyword list. An ML model would require explainability tooling to achieve the same level of transparency.

### Why jtokkit BPE for token counting?

The optimizer's core value proposition is "reduce token usage" — which
demands that token counts match what the LLM actually sees. Whitespace-split
word count was the v1.0.0 placeholder, and it was systematically wrong:
punctuation was undercounted (`Hello world!` reports 2 words but 3 BPE tokens),
sub-word splits were invisible, and the resulting compression ratios drifted
from reality the longer the prompt got. v1.5.0 replaces this with
[jtokkit](https://github.com/knuddels/jtokkit), a Java port of OpenAI's
tiktoken, using the `o200k_base` encoder — the same vocabulary used by
`gpt-4o-mini`, which this project already calls for AI Generate and Quality Check.

The architecture made this swap clean: every token count in the codebase goes
through `TokenCounter.count(String)`, so the BPE upgrade was a single-file
change with zero modifications to the 11 call sites. Rules that needed
"word count" semantics (TaskAnalyzer's complexity thresholds, LengthControl's
`maxWords` budget) were migrated to a new `TokenCounter.wordCount()` method —
keeping logical decisions on word count while showing real BPE numbers
in the UI.

### Why a ProtectedTextProcessor utility instead of a visible rule?

v1.5.5 protects Markdown fenced code blocks and inline code inside the high-risk transformation rules themselves. This is implemented as `ProtectedTextProcessor`, not as a separate frontend rule card or normal pipeline rule, because the current `Rule` interface only passes String input/output and does not carry shared pipeline context.

The current scope is deliberately partial: fenced code blocks and inline code are protected, while quoted text, Markdown tables, JSON-like blocks outside fenced code, and custom delimiters remain future work. A future Protector/Restorer architecture could be considered if `PipelineContext` is introduced.

### Why LOW / MID / HIGH instead of exposing raw numeric parameters?

Exposing raw numbers to a UI creates a usability problem: a user setting `aggressiveness=47` has no intuition for what that means. Mapping slider values to named tiers (LOW / MID / HIGH) gives each level a clear, documented meaning — users can predict the effect before running the pipeline. The underlying parameter is still a numeric range (0–100) passed through the API, so programmatic callers retain full control. The tier names are a presentation layer, not a constraint on the data model.

---

*Built by Andy · 2026 · Last updated 2026/05/03*
