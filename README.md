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

### Three-Page UI

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

**Page 3 — Final Result**
- Original prompt vs. optimized prompt comparison
- Aggregate compression statistics: original tokens, final tokens, compression rate
- Bar chart showing token savings broken down by rule
- Applied rules displayed as chips
- Actions to re-optimize or reset

### Prompt Generator

The built-in generator produces "dirty" prompts — intentionally verbose, polite, and filler-heavy — designed to give the optimizer meaningful input to work with. It covers:

- **5 task types**: `CODING`, `EXPLAIN`, `DEBUG`, `WRITING`, `COMPARE`
- **3 verbosity levels**: `LOW` (minimal filler), `MEDIUM` (greeting + closing + some verbose phrases), `HIGH` (stacked salutations, maximal noise)
- **3 variants per combination** = 45 total templates, selected randomly per call

### Optimization Rules

| # | Rule | Level | Configurable Parameter |
|---|------|-------|----------------------|
| 1 | Input Cleaner | Level 1 | `aggressiveness` (LOW / MID / HIGH) |
| 2 | Task Analyzer | Level 1 | — |
| 3 | Semantic Compressor | Level 1 | `compressionLevel` (LOW / MID / HIGH) |
| 4 | Structure Minimizer | Level 1 | — |
| 5 | Length Control | Level 2 | `maxWords` (integer) |
| 6 | Format Control | Level 2 | — |
| 7 | Redundancy Suppressor | Level 2 | — |

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
│   │  ├── InputCleanerRule             ├── LengthControlRule    │  │
│   │  ├── TaskAnalyzerRule             ├── FormatControlRule    │  │
│   │  ├── SemanticCompressorRule       └── RedundancySuppressor │  │
│   │  └── StructureMinimizerRule                                │  │
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
    │   │   │   ├── AiPromptGenerator.java          # AI generator stub (Claude API, pending integration)
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
    │   │       ├── TokenCounter.java               # Token counter (word-split approximation)
    │   │       │
    │   │       ├── level1/                         # Input Processing Rules
    │   │       │   ├── InputCleanerRule.java       # Remove greetings and polite openers
    │   │       │   ├── TaskAnalyzerRule.java       # Classify task type and complexity
    │   │       │   ├── SemanticCompressorRule.java # Verbose-phrase → concise substitutions
    │   │       │   └── StructureMinimizerRule.java # Whitespace and blank-line normalization
    │   │       │
    │   │       └── level2/                         # Output Control Rules
    │   │           ├── LengthControlRule.java      # Hard word-count truncation
    │   │           ├── FormatControlRule.java      # Verbose format instructions → symbols
    │   │           └── RedundancySuppressorRule.java # Remove closing filler sentences
    │   │
    │   └── resources/
    │       ├── application.properties              # Port, app name, Anthropic API config
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

#### 1. Input Cleaner

Removes social greetings and polite filler phrases from the beginning (and, at HIGH aggressiveness, the middle) of a prompt.

Controlled by the `aggressiveness` parameter, mapped to three tiers:

| Tier | Range | What gets removed |
|------|-------|-------------------|
| LOW  | 0–30  | `hello`, `hi`, `hey`, `good morning/afternoon/evening` |
| MID  | 31–70 | LOW + `please`, `could you`, `can you`, `would you`, `I need you to`, `I was hoping you could` |
| HIGH | 71–100 | MID + `I was wondering if`, `I'd like you to`, `I am reaching out because` + mid-text fillers: `basically`, `essentially`, `literally`, `actually` |

The change log records every specific removal, e.g.:
```
[aggressiveness=HIGH] Removed filler opener: "Hello!"
[aggressiveness=HIGH] Removed mid-text filler: "basically"
```

---

#### 2. Task Analyzer

Classifies the prompt by task type and complexity, then appends a metadata tag that downstream rules (and in future, the LLM itself) can use.

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

Output appends: `[Task: CODING | Complexity: HIGH]`

---

#### 3. Semantic Compressor

Replaces verbose multi-word phrases with shorter semantic equivalents. Uses a tiered substitution table controlled by the `compressionLevel` parameter.

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

#### 4. Structure Minimizer

Normalizes whitespace and blank lines — purely structural, no semantic content is changed.

Four operations applied in sequence:

1. Trim trailing whitespace from every line
2. Collapse runs of 3 or more consecutive blank lines into a single blank line
3. Collapse multiple consecutive spaces into a single space
4. Strip leading and trailing whitespace from the full text

---

### Level 2 — Output Control

These rules apply after the input has been cleaned and compressed. They enforce hard limits and format constraints.

---

#### 5. Length Control

Enforces a hard maximum word count via the `maxWords` parameter (default: 50).

If the prompt exceeds `maxWords`, the text is truncated at the last word boundary before the limit and `...` is appended. This is a hard cutoff — it does not summarize or paraphrase.

---

#### 6. Format Control

Replaces verbose, human-readable formatting instructions with their compact symbol equivalents.

| Verbose instruction | Compact replacement |
|---------------------|---------------------|
| `bullet points:` | `•` |
| `numbered list:` | `1.` |
| `in bold:` | `**` |
| `underline:` | `_` |

---

#### 7. Redundancy Suppressor

Removes closing filler sentences that appear frequently at the end of user-written prompts. These sentences are entirely ignored by LLMs but consume tokens.

Detects and removes patterns including:

```
"I hope this helps"
"please let me know"
"thanks in advance"
"feel free to ask"
"don't hesitate to ask"
"let me know if you need anything"
"looking forward to your response"
"any help would be appreciated"
"thank you for your time"
"I appreciate your help"
"please feel free"
"if you have any questions"
```

Matching is case-insensitive and uses regex anchored to sentence boundaries.

---

## API Reference

### POST /api/optimize

Run the full optimization pipeline on a prompt.

**Request body:**

```json
{
  "prompt": "Hello! I was hoping you could please help me write a Python function to sort a list. Thanks in advance!",
  "rules": {
    "inputCleaner": {
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
    },
    "redundancySuppressor": {
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
      "outputText": "write a Python function to sort a list. Thanks in advance! [Task: CODING | Complexity: LOW]",
      "tokensBefore": 12,
      "tokensAfter": 17,
      "tokensSaved": -5,
      "changes": ["Detected task type: CODING", "Detected complexity: LOW"],
      "status": "done"
    }
  ],
  "finalPrompt": "Write a Python function to sort a list. [Task: CODING | Complexity: LOW]",
  "tokenStats": {
    "original": 20,
    "final": 12,
    "compressionRate": 40.0,
    "byRule": {
      "Input Cleaner": 8,
      "Task Analyzer": -5,
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

### GET /api/rules

Returns metadata for all registered rules in pipeline order.

**Response:**

```json
[
  {
    "id": "inputCleaner",
    "name": "Input Cleaner",
    "level": "Level 1",
    "description": "Removes greetings and filler openers from prompts"
  },
  {
    "id": "taskAnalyzer",
    "name": "Task Analyzer",
    "level": "Level 1",
    "description": "Classifies task type and complexity, appends metadata tag"
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
  },
  {
    "id": "redundancySuppressor",
    "name": "Redundancy Suppressor",
    "level": "Level 2",
    "description": "Removes closing filler sentences"
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

> **Note:** `source=ai` is currently a stub. It returns a placeholder until the Claude API integration is complete (see Roadmap).

---

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 21 |
| Framework | Spring Boot 3.5 |
| Build | Maven |
| Frontend | HTML5, CSS3, JavaScript (no external libraries) |
| AI API | Anthropic Claude (integration in progress) |
| Token counting | Whitespace-split approximation (word count) |

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

To enable AI prompt generation, set your Anthropic API key in `src/main/resources/application.properties`:

```properties
anthropic.api.key=sk-ant-...
anthropic.model=claude-opus-4-6
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

- [x] v1.0 — Spring Boot project scaffold, REST API skeleton
- [x] v1.1 — Level 1 & Level 2 rule implementations with real algorithms
- [x] v1.2 — Prompt Generator template library (45 templates)
- [x] v1.3 — Three-page SPA + pipeline visualization UI
- [ ] v2.0 — Claude API integration (AI Generate + quality comparison)
- [ ] v2.1 — Real BPE tokenizer to replace whitespace-split approximation
- [ ] v3.0 — Level 3: context optimization (deduplication, reference compression)
- [ ] v4.0 — Level 4 & 5: system-level optimization (system prompt factoring, conversation compression)

---

## Engineering Decisions

### Why Strategy + Chain of Responsibility instead of a monolithic optimizer?

A single class that applies all transformations in one pass would be simpler to write initially — but it makes testing, debugging, and extension significantly harder. With the Strategy pattern, each rule is independently testable and has a single reason to change. With the Chain of Responsibility, the pipeline is auditable: you can see exactly which rule made which change and how many tokens it saved. When a user reports unexpected output, the step-by-step log makes the cause immediately visible.

The practical payoff: when adding a new rule, only one file changes (`RuleRegistryConfig`). No existing code is touched, so no existing behavior can break.

### Why keyword matching for task classification instead of ML?

A machine learning classifier for five task categories would require a labeled training set, model hosting, inference latency, and retraining infrastructure. For this project, the classification informs a metadata tag that helps humans (and eventually the LLM) understand what kind of prompt it is — it does not need to be perfect, it needs to be fast and transparent.

Keyword matching is deterministic, zero-latency, fully debuggable, and requires no external dependencies. If the classification is wrong, the cause is immediately visible in the keyword list. An ML model would require explainability tooling to achieve the same level of transparency.

### Why whitespace-split word count instead of a real BPE tokenizer?

Integrating a real tokenizer (Anthropic BPE, tiktoken) would require a native library or subprocess call that adds platform-specific complexity. For the purposes of demonstrating compression ratios and pipeline comparisons, a word-count approximation preserves the relative ordering: if rule A saves more words than rule B, it will also save more tokens in practice. The absolute numbers are estimates, and the UI labels them as such.

The architecture already isolates all token counting in a single `TokenCounter` class. Replacing the approximation with a real tokenizer is a one-file change.

### Why LOW / MID / HIGH instead of exposing raw numeric parameters?

Exposing raw numbers to a UI creates a usability problem: a user setting `aggressiveness=47` has no intuition for what that means. Mapping slider values to named tiers (LOW / MID / HIGH) gives each level a clear, documented meaning — users can predict the effect before running the pipeline. The underlying parameter is still a numeric range (0–100) passed through the API, so programmatic callers retain full control. The tier names are a presentation layer, not a constraint on the data model.

---

*Built by Andy · 2026*
