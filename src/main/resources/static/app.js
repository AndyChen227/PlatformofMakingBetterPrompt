/**
 * BetterPrompt Token Optimizer
 * app.js — Pure vanilla JS, Google Material Design, 3-page SPA
 */
'use strict';

// ═══════════════════════════════════════════════════════════════
// GLOBAL STATE
// ═══════════════════════════════════════════════════════════════

const state = {
  prompt: '',
  rules: {
    inputCleaner:         { enabled: true,  params: { aggressiveness: 50 } },
    taskAnalyzer:         { enabled: true,  params: {} },
    semanticCompressor:   { enabled: true,  params: { compressionLevel: 50 } },
    structureMinimizer:   { enabled: true,  params: {} },
    lengthControl:        { enabled: true,  params: { maxWords: 50 } },
    formatControl:        { enabled: true,  params: {} },
    redundancySuppressor: { enabled: true,  params: {} },
  },
  result: null,
  currentPage: 1,
  completedPages: new Set(),
};

// Execution order — must match RuleRegistryConfig.java
const RULE_ORDER = [
  'inputCleaner',
  'taskAnalyzer',
  'semanticCompressor',
  'structureMinimizer',
  'lengthControl',
  'formatControl',
  'redundancySuppressor',
];

const RULE_LEVEL = {
  inputCleaner: 'l1', taskAnalyzer: 'l1',
  semanticCompressor: 'l1', structureMinimizer: 'l1',
  lengthControl: 'l2', formatControl: 'l2', redundancySuppressor: 'l2',
};

// ═══════════════════════════════════════════════════════════════
// RULE INFO (modal content)
// ═══════════════════════════════════════════════════════════════

const RULE_INFO = {
  inputCleaner: {
    name: 'Input Cleaner',
    level: 'Level 1',
    levelClass: 'badge-blue',
    what: 'Removes greetings, filler words, and unnecessary openers from the beginning of prompts. These phrases carry no information value for the model and only consume tokens.',
    hasParams: true,
    params: [
      { tier: 'LOW',  cls: 'badge-green',  text: 'Only removes explicit greetings: hi, hello, hey, good morning/afternoon/evening' },
      { tier: 'MID',  cls: 'badge-blue',   text: 'LOW + polite request openers: please, could you, can you, would you, I need you to' },
      { tier: 'HIGH', cls: 'badge-orange', text: 'MID + soft openers (I was wondering if…, I\'d like you to…) + mid-text filler words: basically, essentially, literally, actually' },
    ],
    exBefore: 'Hello! I was wondering if you could please help me understand recursion. Thanks in advance!',
    exAfter:  'Explain recursion.',
    future: [
      'NLP classifier to detect filler phrases anywhere in text, not just at the start',
      'Multilingual support: Chinese (你好/麻烦你), French (Bonjour/Pourriez-vous)',
      'Context-aware mode: preserve openers in customer-service or formal-writing prompts',
    ],
  },
  taskAnalyzer: {
    name: 'Task Analyzer',
    level: 'Level 1',
    levelClass: 'badge-blue',
    what: 'Analyzes the prompt to classify its task type (CODING, EXPLAIN, DEBUG, WRITING, COMPARE) and estimate complexity (LOW / MEDIUM / HIGH) based on word count. Results are appended as metadata tags to guide downstream rules.',
    hasParams: false,
    exBefore: 'Write a Python function that reverses a string.',
    exAfter:  'Write a Python function that reverses a string. [Task: CODING | Complexity: LOW]',
    future: [
      'ML-based intent classifier trained on prompt datasets',
      'Complexity scoring model beyond word count (syntax depth, entity density)',
      'Use task type to dynamically enable/disable other rules (e.g. skip Format Control for CODING)',
    ],
  },
  semanticCompressor: {
    name: 'Semantic Compressor',
    level: 'Level 1',
    levelClass: 'badge-blue',
    what: 'Replaces verbose multi-word phrases with shorter equivalents that carry identical meaning. Operates on a tiered substitution dictionary — higher tiers add more aggressive replacements on top of lower ones.',
    hasParams: true,
    params: [
      { tier: 'LOW',  cls: 'badge-green',  text: '8 safe substitutions: "in order to" → "to", "due to the fact that" → "because", and 6 others' },
      { tier: 'MID',  cls: 'badge-blue',   text: '19 substitutions: LOW + "make a decision" → "decide", "take into consideration" → "consider", and 9 others' },
      { tier: 'HIGH', cls: 'badge-orange', text: '29 substitutions: MID + "has the ability to" → "can", "the fact that" → "that", and 8 others' },
    ],
    exBefore: 'I need you to write a function in order to sort a list, due to the fact that the current implementation is slow.',
    exAfter:  'I need you to write a function to sort a list because the current implementation is slow.',
    future: [
      'Semantic similarity model to detect arbitrary paraphrase pairs beyond the fixed list',
      'LLM-based paraphrase detection for zero-shot languages',
      'Handle coreference: replace repeated noun phrases with pronouns',
    ],
  },
  structureMinimizer: {
    name: 'Structure Minimizer',
    level: 'Level 1',
    levelClass: 'badge-blue',
    what: 'Cleans up structural noise in the prompt: strips trailing whitespace per line, collapses 3+ consecutive blank lines into one, and normalizes multiple spaces into single spaces. Ensures the text is compact before further processing.',
    hasParams: false,
    exBefore: '"Write a   function\n\n\n\nto sort  a list   ."',
    exAfter:  '"Write a function\n\nto sort a list."',
    future: [
      'Detect and remove redundant markdown headers (## Intro / ## Overview)',
      'Remove duplicate paragraphs or near-duplicate sentences',
      'Normalize Unicode whitespace (non-breaking spaces, zero-width characters)',
    ],
  },
  lengthControl: {
    name: 'Length Control',
    level: 'Level 2',
    levelClass: 'badge-red',
    what: 'Enforces a maximum word budget on the prompt. If the prompt exceeds the configured limit, it is hard-truncated at the nearest word boundary and "..." is appended. Prevents overly long prompts from consuming excessive tokens.',
    hasParams: true,
    params: [
      { tier: 'Max Words', cls: 'badge-blue', text: 'Number input (default: 50). Prompt is truncated to this many words if it exceeds the limit.' },
    ],
    exBefore: 'Write a Python function that takes a list of integers as input and returns a new list containing only the even numbers, sorted in ascending order, with duplicates removed. [60 words total]',
    exAfter:  'Write a Python function that takes a list of integers as input and returns a new list containing only the even numbers, sorted in ascending order... [truncated to 50 words]',
    future: [
      'Summarize instead of truncate: use extractive summarization to preserve key intent',
      'Sentence-boundary-aware trimming (never cut mid-sentence)',
      'Adaptive budget based on detected task complexity',
    ],
  },
  formatControl: {
    name: 'Format Control',
    level: 'Level 2',
    levelClass: 'badge-red',
    what: 'Converts verbose formatting instructions into compact symbols. Reduces token count for prompts that contain explicit formatting directives without changing the intended output format.',
    hasParams: false,
    exBefore: '"Please list the following items using bullet points: item one, item two, item three."',
    exAfter:  '"Please list the following items using •: item one, item two, item three."',
    future: [
      'Detect implicit format intent ("give me a list" → infer bullet points)',
      'Normalize mixed list styles within a single prompt',
      'Support more format patterns: tables, headers, code blocks',
    ],
  },
  redundancySuppressor: {
    name: 'Redundancy Suppressor',
    level: 'Level 2',
    levelClass: 'badge-red',
    what: 'Removes closing filler phrases at the end of prompts. These phrases — such as "I hope this helps" or "please let me know if you have questions" — add no information value and are ignored by the model anyway.',
    hasParams: false,
    exBefore: 'Explain recursion. I hope this helps! Please let me know if you have any questions. Thanks in advance!',
    exAfter:  'Explain recursion.',
    future: [
      'Semantic similarity detection for paraphrase fillers not in the fixed list',
      'Detect over-specified constraints that repeat earlier instructions',
      'Handle multi-sentence closing blocks spanning several lines',
    ],
  },
};

// ═══════════════════════════════════════════════════════════════
// DOM REFS
// ═══════════════════════════════════════════════════════════════

const $ = id => document.getElementById(id);

// ═══════════════════════════════════════════════════════════════
// INIT
// ═══════════════════════════════════════════════════════════════

function initUI() {
  // Live word/char count
  $('promptTextarea').addEventListener('input', updateCounts);

  // Rule toggles
  document.querySelectorAll('.rule-toggle').forEach(cb => {
    cb.addEventListener('change', () => toggleRule(cb.dataset.rule, cb.checked));
  });

  // Tier buttons
  document.querySelectorAll('.tier-group').forEach(group => {
    group.querySelectorAll('.tier-btn').forEach(btn => {
      btn.addEventListener('click', () => {
        group.querySelectorAll('.tier-btn').forEach(b => b.classList.remove('active'));
        btn.classList.add('active');
        setTierParam(group.dataset.rule, group.dataset.param, parseInt(btn.dataset.value, 10));
      });
    });
  });

  // Max words input
  $('maxWordsInput').addEventListener('input', () => {
    const v = parseInt($('maxWordsInput').value, 10);
    if (!isNaN(v) && v > 0) state.rules.lengthControl.params.maxWords = v;
  });

  // Page navigation
  updateCrumbs();
}

function updateCounts() {
  const text = $('promptTextarea').value;
  const words = text.trim() ? text.trim().split(/\s+/).length : 0;
  $('wordCount').textContent = `${words} word${words !== 1 ? 's' : ''}`;
  $('charCount').textContent = `${text.length} character${text.length !== 1 ? 's' : ''}`;
  state.prompt = text;
}

function toggleRule(ruleId, enabled) {
  state.rules[ruleId].enabled = enabled;
  const card = $(`rcard-${ruleId}`);
  if (card) card.classList.toggle('disabled', !enabled);
  // Update strategy count display in middle column
  const total   = Object.keys(state.rules).length;
  const active  = Object.values(state.rules).filter(r => r.enabled).length;
  const el = $('strategyCount');
  if (el) el.textContent = `${active} of ${total} strategies enabled`;
}

function setTierParam(ruleId, paramKey, value) {
  if (state.rules[ruleId]) state.rules[ruleId].params[paramKey] = value;
}

// ═══════════════════════════════════════════════════════════════
// PAGE NAVIGATION
// ═══════════════════════════════════════════════════════════════

function goToPage(n) {
  // Can only go to completed pages or current page
  if (n > 1 && !state.completedPages.has(n - 1) && n !== state.currentPage) return;

  document.querySelectorAll('.page').forEach(p => p.style.display = 'none');
  const target = $(`page-${n}`);
  if (target) {
    target.style.display = n === 1 ? 'flex' : 'block';
    window.scrollTo(0, 0);
  }
  state.currentPage = n;
  updateCrumbs();
}

function crumbClick(n) {
  if (n === state.currentPage) return;
  if (n < state.currentPage || state.completedPages.has(n)) {
    goToPage(n);
  }
}

function updateCrumbs() {
  [1, 2, 3].forEach(n => {
    const el = $(`crumb${n}`);
    if (!el) return;
    el.className = 'crumb';
    const numEl = el.querySelector('.crumb-num');
    const lblEl = el.querySelector('.crumb-label');

    if (n === state.currentPage) {
      el.classList.add('active');
      if (numEl) numEl.textContent = `⬤`;
    } else if (state.completedPages.has(n)) {
      el.classList.add('done');
      if (numEl) numEl.textContent = `✓`;
    } else {
      if (numEl) numEl.textContent = `${['①','②','③'][n-1]}`;
    }
  });
}

// ═══════════════════════════════════════════════════════════════
// API: OPTIMIZE
// ═══════════════════════════════════════════════════════════════

function collectConfig() {
  return {
    prompt: state.prompt,
    rules: Object.fromEntries(
      Object.entries(state.rules).map(([id, cfg]) => [
        id, { enabled: cfg.enabled, params: { ...cfg.params } }
      ])
    ),
  };
}

async function callOptimize() {
  const prompt = $('promptTextarea').value.trim();
  if (!prompt) {
    $('promptTextarea').focus();
    $('promptTextarea').style.borderColor = '#d93025';
    setTimeout(() => { $('promptTextarea').style.borderColor = ''; }, 1500);
    $('optimizeError').textContent = 'Please enter a prompt first.';
    return;
  }

  state.prompt = $('promptTextarea').value;
  $('optimizeError').textContent = '';

  const btn = $('btnOptimize');
  btn.disabled = true;
  btn.innerHTML = '<span class="spinner"></span>Optimizing...';

  try {
    const res = await fetch('/api/optimize', {
      method:  'POST',
      headers: { 'Content-Type': 'application/json' },
      body:    JSON.stringify(collectConfig()),
    });

    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: `HTTP ${res.status}` }));
      throw new Error(err.error || `HTTP ${res.status}`);
    }

    state.result = await res.json();
    state.completedPages.add(1);
    state.completedPages.add(2);
    state.completedPages.add(3);

    // Update nav status
    const ts = state.result.tokenStats || {};
    const saved = Math.max(0, (ts.original || 0) - (ts['final'] || 0));
    $('navStatus').textContent = `✓ Optimization complete · ${saved} tokens saved`;

    // Render page 2 and 3
    renderPipelinePage(state.result);
    renderResultPage(state.result);

    // Navigate to page 2
    goToPage(2);

  } catch (err) {
    $('optimizeError').textContent = `Error: ${err.message}`;
  } finally {
    btn.disabled = false;
    btn.innerHTML = 'Optimize →';
  }
}

// ═══════════════════════════════════════════════════════════════
// API: GENERATOR
// ═══════════════════════════════════════════════════════════════

async function callGenerator() {
  const type      = $('genTaskType').value;
  const verbosity = $('genVerbosity').value;
  const btn       = $('btnGenTemplate');

  btn.disabled    = true;
  btn.textContent = '...';
  $('genMeta').textContent = '';

  try {
    const url = `/api/generator/prompt?type=${encodeURIComponent(type)}&verbosity=${encodeURIComponent(verbosity)}&source=template`;
    const res = await fetch(url);

    if (!res.ok) {
      const err = await res.json().catch(() => ({ error: `HTTP ${res.status}` }));
      throw new Error(err.error || `HTTP ${res.status}`);
    }

    const data = await res.json();
    const ta = $('promptTextarea');
    ta.value = data.prompt;
    updateCounts();

    // Blue border flash
    ta.style.transition  = 'border-color 0.1s ease';
    ta.style.borderColor = '#1a73e8';
    setTimeout(() => {
      ta.style.transition  = '';
      ta.style.borderColor = '';
    }, 800);

    $('genMeta').textContent = `✓ Generated: ${data.taskType} · ${data.verbosity} · ${data.source}`;

  } catch (err) {
    $('genMeta').textContent = `Error: ${err.message}`;
  } finally {
    btn.disabled    = false;
    btn.innerHTML   = '⚡ Generate Template';
  }
}

// ═══════════════════════════════════════════════════════════════
// RENDER: PAGE 2 — PIPELINE
// ═══════════════════════════════════════════════════════════════

function renderPipelinePage(result) {
  const ts = result.tokenStats || {};

  // Stats bar
  $('statsBar2').innerHTML = buildStatsBarHTML(ts);

  // Steps
  const steps = result.steps || [];
  let activeIdx = 0;
  const html = [];

  steps.forEach((step, i) => {
    if (step.status === 'skipped') return;
    activeIdx++;
    const ruleId   = RULE_ORDER[i] || '';
    const levelCls = RULE_LEVEL[ruleId] || 'l1';
    const saved    = step.tokensSaved || 0;

    // Left bar color
    let barColor, numClass, numIcon;
    if (saved > 0)      { barColor = 'bar-green'; numClass = 'done';    numIcon = '✓'; }
    else if (saved < 0) { barColor = 'bar-gray';  numClass = 'warn';    numIcon = '+'; }
    else                { barColor = 'bar-gray';  numClass = 'neutral'; numIcon = '—'; }

    // Token pill
    let pillCls, pillText;
    if (saved > 0)       { pillCls = 'saved';   pillText = `−${saved} tokens`; }
    else if (saved < 0)  { pillCls = 'added';   pillText = `+${Math.abs(saved)} tokens`; }
    else                 { pillCls = 'neutral';  pillText = '0 tokens'; }

    // Level badge
    const lvlBadge = levelCls === 'l1'
      ? `<span class="badge badge-blue">Level 1</span>`
      : `<span class="badge badge-red">Level 2</span>`;

    // Param summary
    const paramText = buildParamSummaryText(ruleId);

    // Detail blocks
    const detailHTML = buildStepDetailHTML(step, ruleId);

    html.push(`
      <div class="step-card">
        <div class="step-left-bar ${barColor}"></div>
        <div class="step-header">
          <div class="step-num ${numClass}">${numIcon}</div>
          <div class="step-info">
            <div class="step-name-row">
              <span class="step-name">${escHtml(step.ruleName)}</span>
              ${lvlBadge}
            </div>
            ${paramText ? `<div class="step-param-text">${escHtml(paramText)}</div>` : ''}
          </div>
          <div class="token-pill ${pillCls}">${pillText}</div>
        </div>
        ${detailHTML ? `<div class="step-detail">${detailHTML}</div>` : ''}
      </div>
    `);
  });

  $('pipelineSteps').innerHTML = html.join('') || '<p style="color:var(--text-muted);text-align:center;padding:32px 0;">No active rules.</p>';
}

function buildStepDetailHTML(step, ruleId) {
  const parts = [];

  // Param block (blue)
  const paramDesc = buildParamDescription(ruleId);
  if (paramDesc) {
    parts.push(`
      <div class="detail-block blue">
        <div class="detail-block-label">Parameter used</div>
        <div class="detail-param-text">${escHtml(paramDesc)}</div>
      </div>
    `);
  }

  // Before / After block (green)
  if (step.inputText != null && step.outputText != null) {
    const beforeHtml = generateDiffBefore(step.inputText, step.outputText);
    const afterHtml  = generateDiffAfter(step.inputText, step.outputText);
    parts.push(`
      <div class="detail-block green">
        <div class="detail-block-label">Before / After</div>
        <div class="ba-box ba-box-before">
          <div class="ba-label">Before</div>
          <div>${beforeHtml}</div>
        </div>
        <div class="ba-arrow">↓</div>
        <div class="ba-box ba-box-after">
          <div class="ba-label">After</div>
          <div>${afterHtml}</div>
        </div>
      </div>
    `);
  }

  // Changes block (yellow)
  if (step.changes && step.changes.length > 0) {
    const items = step.changes.map(c =>
      `<div class="change-item"><span class="change-arrow">→</span><span>${escHtml(c)}</span></div>`
    ).join('');
    parts.push(`
      <div class="detail-block yellow">
        <div class="detail-block-label">Changes made</div>
        <div class="changes-list">${items}</div>
      </div>
    `);
  }

  return parts.join('');
}

function buildParamSummaryText(ruleId) {
  if (ruleId === 'inputCleaner') {
    const v = state.rules.inputCleaner.params.aggressiveness;
    return `Aggressiveness: ${v <= 30 ? 'LOW' : v <= 70 ? 'MID' : 'HIGH'}`;
  }
  if (ruleId === 'semanticCompressor') {
    const v = state.rules.semanticCompressor.params.compressionLevel;
    return `Compression Level: ${v <= 30 ? 'LOW' : v <= 70 ? 'MID' : 'HIGH'}`;
  }
  if (ruleId === 'lengthControl') {
    return `Max words: ${state.rules.lengthControl.params.maxWords}`;
  }
  return '';
}

function buildParamDescription(ruleId) {
  if (ruleId === 'inputCleaner') {
    const v = state.rules.inputCleaner.params.aggressiveness;
    if (v <= 30) return 'LOW — removes explicit greetings only (hello, hi, hey, good morning/afternoon/evening)';
    if (v <= 70) return 'MID — greetings + polite openers (please, could you, can you, would you, I need you to)';
    return 'HIGH — all openers + mid-text filler words (basically, essentially, literally, actually)';
  }
  if (ruleId === 'semanticCompressor') {
    const v = state.rules.semanticCompressor.params.compressionLevel;
    if (v <= 30) return 'LOW — 8 safe substitutions (e.g. "in order to" → "to", "due to the fact that" → "because")';
    if (v <= 70) return 'MID — 19 substitutions including "make a decision" → "decide", "take into consideration" → "consider"';
    return 'HIGH — 29 substitutions including "has the ability to" → "can", "the fact that" → "that"';
  }
  if (ruleId === 'lengthControl') {
    return `Max words: ${state.rules.lengthControl.params.maxWords} — truncates prompt exceeding this word budget`;
  }
  return '';
}

// ═══════════════════════════════════════════════════════════════
// RENDER: PAGE 3 — FINAL RESULT
// ═══════════════════════════════════════════════════════════════

function renderResultPage(result) {
  const ts       = result.tokenStats || {};
  const original = ts.original || 0;
  const final_   = ts['final'] || 0;

  // Stats bar
  $('statsBar3').innerHTML = buildStatsBarHTML(ts);

  // Side-by-side comparison
  const origText  = state.prompt || '';
  const optText   = result.finalPrompt || '';
  const origDiff  = generateDiffBefore(origText, optText);
  const optDiff   = generateDiffAfter(origText, optText);

  $('compareRow').innerHTML = `
    <div class="compare-col">
      <div class="compare-head">
        <span class="compare-title">Original Prompt</span>
        <span class="badge badge-red">Before</span>
      </div>
      <div class="compare-body original">${origDiff}</div>
      <div class="compare-footer">${original} tokens</div>
    </div>
    <div class="compare-col">
      <div class="compare-head">
        <span class="compare-title">Optimized Prompt</span>
        <span class="badge badge-green">After</span>
        <button class="btn-copy" id="btnCopy" onclick="copyPrompt()">Copy</button>
      </div>
      <div class="compare-body optimized">${optDiff}</div>
      <div class="compare-footer green">${final_} tokens</div>
    </div>
  `;

  // Applied rule chips
  const steps = result.steps || [];
  const chipsHTML = RULE_ORDER.map((ruleId, i) => {
    const step = steps[i];
    if (!step || step.status === 'skipped') return '';
    const saved = step.tokensSaved || 0;
    let cls, label;
    if (saved > 0)       { cls = 'saved';   label = `${step.ruleName} −${saved}`; }
    else if (saved < 0)  { cls = 'added';   label = `${step.ruleName} +${Math.abs(saved)}`; }
    else                 { cls = 'neutral'; label = `${step.ruleName} 0`; }
    return `<span class="rule-chip ${cls}">${escHtml(label)}</span>`;
  }).join('');
  $('chipsRow').innerHTML = chipsHTML || '<span style="color:var(--text-muted);font-size:13px;">No rules applied.</span>';

  // Chart
  if (ts.byRule && Object.keys(ts.byRule).length > 0) {
    $('chartEmpty').style.display = 'none';
    drawChart('chartCanvas', ts.byRule);
  } else {
    $('chartEmpty').style.display = 'block';
  }
}

function buildStatsBarHTML(ts) {
  const original = ts.original || 0;
  const final_   = ts['final'] || 0;
  const saved    = Math.max(0, original - final_);
  const rate     = ts.compressionRate || 0;

  return `
    <div class="stat-card">
      <div class="stat-num">${original}</div>
      <div class="stat-lbl">Original Tokens</div>
    </div>
    <div class="stat-card">
      <div class="stat-num blue">${final_}</div>
      <div class="stat-lbl">Optimized Tokens</div>
    </div>
    <div class="stat-card">
      <div class="stat-num green">${saved}</div>
      <div class="stat-lbl">Tokens Saved</div>
    </div>
    <div class="stat-card">
      <div class="stat-num green">${rate.toFixed(1)}%</div>
      <div class="stat-lbl">Compression Rate</div>
    </div>
  `;
}

// ═══════════════════════════════════════════════════════════════
// WORD-LEVEL DIFF
// ═══════════════════════════════════════════════════════════════

function tokenize(text) {
  if (!text || !text.trim()) return [];
  return text.trim().split(/\s+/);
}

/**
 * Render the "before" text: words that were deleted shown with red strikethrough.
 */
function generateDiffBefore(before, after) {
  if (!before) return '';
  const bWords = tokenize(before);
  const aSet   = new Set(tokenize(after).map(w => w.toLowerCase()));
  return bWords.map(w => {
    if (!aSet.has(w.toLowerCase())) {
      return `<span class="diff-del">${escHtml(w)}</span>`;
    }
    return escHtml(w);
  }).join(' ');
}

/**
 * Render the "after" text: words that are genuinely new shown with green highlight.
 */
function generateDiffAfter(before, after) {
  if (!after) return '';
  const aWords = tokenize(after);
  const bSet   = new Set(tokenize(before).map(w => w.toLowerCase()));
  return aWords.map(w => {
    if (!bSet.has(w.toLowerCase())) {
      return `<span class="diff-add">${escHtml(w)}</span>`;
    }
    return escHtml(w);
  }).join(' ');
}

// ═══════════════════════════════════════════════════════════════
// CANVAS CHART
// ═══════════════════════════════════════════════════════════════

function drawChart(canvasId, byRule) {
  const canvas = $(canvasId);
  if (!canvas) return;

  const entries = Object.entries(byRule);
  if (entries.length === 0) return;

  const dpr     = window.devicePixelRatio || 1;
  const LABEL_W = 140;
  const VAL_W   = 48;
  const ROW_H   = 32;
  const GAP     = 8;
  const PAD_T   = 8;
  const PAD_B   = 8;

  const containerW = canvas.parentElement.clientWidth || 340;
  const BAR_AREA   = Math.max(80, containerW - LABEL_W - VAL_W - 8);
  const W          = containerW;
  const H          = PAD_T + entries.length * (ROW_H + GAP) - GAP + PAD_B;

  canvas.width        = W * dpr;
  canvas.height       = H * dpr;
  canvas.style.width  = `${W}px`;
  canvas.style.height = `${H}px`;

  const ctx = canvas.getContext('2d');
  ctx.scale(dpr, dpr);
  ctx.clearRect(0, 0, W, H);

  const maxAbs = Math.max(...entries.map(([, v]) => Math.abs(v)), 1);

  entries.forEach(([name, value], idx) => {
    const y    = PAD_T + idx * (ROW_H + GAP);
    const midY = y + ROW_H / 2;

    // Label
    ctx.font         = `13px 'Google Sans', -apple-system, sans-serif`;
    ctx.fillStyle    = '#5f6368';
    ctx.textAlign    = 'right';
    ctx.textBaseline = 'middle';
    const label      = name.length > 18 ? name.slice(0, 17) + '…' : name;
    ctx.fillText(label, LABEL_W - 8, midY);

    // Bar background
    ctx.fillStyle = '#f1f3f4';
    ctx.beginPath();
    if (ctx.roundRect) ctx.roundRect(LABEL_W, y + 6, BAR_AREA, ROW_H - 12, 4);
    else               ctx.rect(LABEL_W, y + 6, BAR_AREA, ROW_H - 12);
    ctx.fill();

    // Bar fill
    const barW    = Math.max(4, (Math.abs(value) / maxAbs) * BAR_AREA);
    ctx.fillStyle = value >= 0 ? '#1a73e8' : '#d93025';
    ctx.beginPath();
    if (ctx.roundRect) ctx.roundRect(LABEL_W, y + 6, barW, ROW_H - 12, 4);
    else               ctx.rect(LABEL_W, y + 6, barW, ROW_H - 12);
    ctx.fill();

    // Value label
    ctx.font      = `bold 12px 'Google Sans', -apple-system, sans-serif`;
    ctx.fillStyle = '#202124';
    ctx.textAlign = 'left';
    const sign    = value > 0 ? '−' : value < 0 ? '+' : '';
    ctx.fillText(`${sign}${Math.abs(value)}`, LABEL_W + barW + 6, midY);
  });
}

// ═══════════════════════════════════════════════════════════════
// COPY
// ═══════════════════════════════════════════════════════════════

function copyPrompt() {
  const text = state.result?.finalPrompt || '';
  if (!text) return;
  const btn = $('btnCopy');

  navigator.clipboard.writeText(text).then(() => {
    btn.textContent = '✓ Copied!';
    btn.classList.add('copied');
    setTimeout(() => { btn.textContent = 'Copy'; btn.classList.remove('copied'); }, 2000);
  }).catch(() => {
    const ta = document.createElement('textarea');
    ta.value = text; ta.style.cssText = 'position:fixed;opacity:0;';
    document.body.appendChild(ta); ta.select(); document.execCommand('copy');
    document.body.removeChild(ta);
    btn.textContent = '✓ Copied!';
    btn.classList.add('copied');
    setTimeout(() => { btn.textContent = 'Copy'; btn.classList.remove('copied'); }, 2000);
  });
}

// ═══════════════════════════════════════════════════════════════
// PAGE ACTIONS
// ═══════════════════════════════════════════════════════════════

function startOver() {
  state.completedPages.clear();
  state.result = null;
  goToPage(1);
}

function optimizeAgain() {
  goToPage(1);
}

function newPrompt() {
  state.prompt = '';
  state.result = null;
  state.completedPages.clear();
  $('promptTextarea').value = '';
  updateCounts();
  $('navStatus').textContent = '';
  goToPage(1);
}

// ═══════════════════════════════════════════════════════════════
// MODAL
// ═══════════════════════════════════════════════════════════════

function showModal(ruleId) {
  const info = RULE_INFO[ruleId];
  if (!info) return;

  $('modalTitle').textContent = info.name;
  $('modalLevelBadge').className = `badge ${info.levelClass}`;
  $('modalLevelBadge').textContent = info.level;

  const parts = [];

  // What it does
  parts.push(`
    <div class="modal-section">
      <div class="modal-section-title blue">What it does</div>
      <div class="modal-section-text">${escHtml(info.what)}</div>
    </div>
  `);

  // Parameters
  if (info.hasParams && info.params && info.params.length > 0) {
    const paramRows = info.params.map(p => `
      <div class="modal-param-row">
        <span class="modal-param-badge ${p.cls}">${escHtml(p.tier)}</span>
        <span class="modal-param-text">${escHtml(p.text)}</span>
      </div>
    `).join('');
    parts.push(`
      <div class="modal-section">
        <div class="modal-section-title blue">Parameters</div>
        ${paramRows}
      </div>
    `);
  }

  // Example
  parts.push(`
    <div class="modal-section">
      <div class="modal-section-title green">Example</div>
      <div class="modal-example-box before">
        <div class="modal-example-lbl">Before</div>
        <div>${escHtml(info.exBefore)}</div>
      </div>
      <div class="modal-example-arrow">↓</div>
      <div class="modal-example-box after">
        <div class="modal-example-lbl">After</div>
        <div>${escHtml(info.exAfter)}</div>
      </div>
    </div>
  `);

  // Future
  if (info.future && info.future.length > 0) {
    const items = info.future.map(f =>
      `<div class="modal-future-item">${escHtml(f)}</div>`
    ).join('');
    parts.push(`
      <div class="modal-section">
        <div class="modal-section-title gray">Planned improvements</div>
        <div class="modal-future-box">${items}</div>
      </div>
    `);
  }

  $('modalBody').innerHTML = parts.join('');
  $('modalOverlay').classList.add('open');
  document.body.style.overflow = 'hidden';
}

function closeModal() {
  $('modalOverlay').classList.remove('open');
  document.body.style.overflow = '';
}

// Close modal with Escape key
document.addEventListener('keydown', e => {
  if (e.key === 'Escape') closeModal();
});

// ═══════════════════════════════════════════════════════════════
// UTILITIES
// ═══════════════════════════════════════════════════════════════

function escHtml(str) {
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;');
}

// Resize: redraw chart
let resizeTimer;
window.addEventListener('resize', () => {
  clearTimeout(resizeTimer);
  resizeTimer = setTimeout(() => {
    if (state.result?.tokenStats?.byRule) {
      drawChart('chartCanvas', state.result.tokenStats.byRule);
    }
  }, 150);
});

// ═══════════════════════════════════════════════════════════════
// BOOT
// ═══════════════════════════════════════════════════════════════
initUI();
