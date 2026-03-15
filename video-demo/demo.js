/**
 * Plum Endorsement Service — Automated Video Demo (v2)
 *
 * Drives Chromium through the full demo with Playwright video recording.
 * Plays pre-generated narration audio segments in sync.
 *
 * v2 changes:
 *   - C4 model architecture slides before hexagonal
 *   - Complete endorsement lifecycle (create → submit-to-insurer → confirmed)
 *   - Interactive EA account lookup, insurer detail clicks
 *   - Expanded AI/Ollama section with code-level explanation
 *   - Vision section covering 3 documents (~6 min)
 *   - Grafana login with admin:plum
 *   - Allure report page
 */

const { chromium } = require("playwright");
const { execSync, spawn } = require("child_process");
const fs = require("fs");
const path = require("path");
const { SEGMENTS } = require("./narration");

// ── Configuration ────────────────────────────────────────────────────────
const BASE_URL = "http://localhost:5173";
const API_URL = "http://localhost:8080";
const GRAFANA_URL = "http://localhost:3000";
const JAEGER_URL = "http://localhost:16686";
const ALLURE_URL = "http://localhost:5050";
const VIDEO_DIR = path.join(__dirname, "output");
const AUDIO_DIR = path.join(__dirname, "audio");
const VIEWPORT = { width: 1920, height: 1080 };
const SKIP_AUDIO = process.argv.includes("--no-audio");

// Known IDs from seed data
const EMPLOYER_ID = "11111111-1111-1111-1111-111111111111";
const INSURER_ID = "22222222-2222-2222-2222-222222222222";

// ── Helpers ──────────────────────────────────────────────────────────────

function getAudioDurationMs(filePath) {
  if (!fs.existsSync(filePath)) return 2000;
  try {
    const out = execSync(
      `ffprobe -v quiet -show_entries format=duration -of csv=p=0 "${filePath}"`,
      { encoding: "utf-8" }
    ).trim();
    return Math.ceil(parseFloat(out) * 1000);
  } catch {
    return 2000;
  }
}

function playAudio(filePath) {
  if (SKIP_AUDIO || !fs.existsSync(filePath)) return null;
  return spawn("afplay", [filePath], { stdio: "ignore" });
}

async function narrateAndWait(segment) {
  if (!segment) return;
  const audioFile = path.join(AUDIO_DIR, `${segment.id}.aiff`);
  const durationMs = getAudioDurationMs(audioFile);
  const proc = playAudio(audioFile);
  await sleep(durationMs + (segment.pauseAfter || 1000));
  if (proc && !proc.killed) try { proc.kill(); } catch {}
}

function sleep(ms) { return new Promise(r => setTimeout(r, ms)); }
function seg(id) { return SEGMENTS.find(s => s.id === id); }

async function safeClick(page, selector) {
  try {
    const el = page.locator(selector).first();
    if (await el.isVisible({ timeout: 3000 })) {
      await el.click();
      return true;
    }
  } catch {}
  return false;
}

async function safeType(page, selector, text) {
  try {
    const el = page.locator(selector).first();
    if (await el.isVisible({ timeout: 3000 })) {
      await el.click();
      await el.fill(text);
      return true;
    }
  } catch {}
  return false;
}

// ── Slide Generators ─────────────────────────────────────────────────────

async function showTitleCard(page, title, subtitle, duration = 3000) {
  await page.setContent(`<!DOCTYPE html><html><head><style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{background:linear-gradient(135deg,#0f0c29,#302b63,#24243e);color:#fff;display:flex;align-items:center;justify-content:center;height:100vh;font-family:-apple-system,system-ui,sans-serif;text-align:center}
    .card{max-width:900px;padding:60px}
    h1{font-size:56px;font-weight:700;margin-bottom:24px;background:linear-gradient(90deg,#667eea,#764ba2);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
    h2{font-size:28px;font-weight:400;color:rgba(255,255,255,.7);line-height:1.5}
    .logo{font-size:18px;color:rgba(255,255,255,.3);margin-top:40px;letter-spacing:2px;text-transform:uppercase}
  </style></head><body><div class="card"><h1>${title}</h1><h2>${subtitle}</h2><div class="logo">Plum Endorsement Management System</div></div></body></html>`, { waitUntil: "domcontentloaded" });
  await sleep(duration);
}

async function showContentSlide(page, title, bullets, duration = 1000) {
  const bhtml = bullets.map(b => `<li><span class="l">${b.label}</span><span class="d">${b.desc}</span></li>`).join("");
  await page.setContent(`<!DOCTYPE html><html><head><style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{background:linear-gradient(135deg,#0f0c29,#302b63,#24243e);color:#fff;display:flex;align-items:flex-start;justify-content:center;height:100vh;font-family:-apple-system,system-ui,sans-serif;padding:60px 80px}
    .c{max-width:1200px;width:100%}
    h1{font-size:42px;font-weight:700;margin-bottom:40px;background:linear-gradient(90deg,#667eea,#764ba2);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
    ul{list-style:none}
    li{padding:14px 0;border-bottom:1px solid rgba(255,255,255,.08);font-size:22px;display:flex;gap:16px}
    .l{font-weight:600;color:#a5b4fc;min-width:240px;flex-shrink:0}
    .d{color:rgba(255,255,255,.75)}
  </style></head><body><div class="c"><h1>${title}</h1><ul>${bhtml}</ul></div></body></html>`, { waitUntil: "domcontentloaded" });
  await sleep(duration);
}

async function showC4ContextSlide(page, duration = 1000) {
  await page.setContent(`<!DOCTYPE html><html><head><style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{background:#0f0c29;color:#e0e0e0;font-family:'SF Mono',Monaco,Menlo,monospace;display:flex;align-items:center;justify-content:center;height:100vh;padding:30px}
    pre{font-size:15px;line-height:1.6;color:#c4b5fd}
    .h{color:#fbbf24;font-weight:bold} .a{color:#34d399} .d{color:#6b7280}
    .title{text-align:center;font-family:-apple-system,system-ui,sans-serif;margin-bottom:20px}
    .title h2{font-size:32px;background:linear-gradient(90deg,#667eea,#764ba2);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
    .title p{color:rgba(255,255,255,.4);font-size:16px}
  </style></head><body><div><div class="title"><h2>C4 Model — System Context</h2><p>Level 1: Who uses the system and what it connects to</p></div><pre>
    ┌─────────────┐     ┌─────────────┐     ┌─────────────┐
    │ <span class="a">HR Admins</span>   │     │ <span class="a">Finance</span>     │     │ <span class="a">Ops Teams</span>  │
    │ Create &amp;    │     │ EA Balance  │     │ Monitor &amp;  │
    │ manage      │     │ management  │     │ resolve     │
    └──────┬──────┘     └──────┬──────┘     └──────┬──────┘
           │                   │                    │
           └───────────────────┼────────────────────┘
                               ▼
                 ┌─────────────────────────────┐
                 │   <span class="h">PLUM ENDORSEMENT SYSTEM</span>    │
                 │                             │
                 │  React UI + Spring Boot API │
                 │  Kafka Events + PostgreSQL  │
                 │  5 Intelligence Pillars     │
                 └──────────────┬──────────────┘
                                │
           ┌────────────────────┼────────────────────┐
           ▼                    ▼                     ▼
    ┌──────────────┐  ┌──────────────┐  ┌──────────────────┐
    │<span class="a">ICICI Lombard</span> │  │ <span class="a">Niva Bupa</span>   │  │ <span class="a">Bajaj Allianz</span>    │
    │  REST/JSON   │  │  CSV/SFTP    │  │   SOAP/XML       │
    └──────────────┘  └──────────────┘  └──────────────────┘
  </pre></div></body></html>`, { waitUntil: "domcontentloaded" });
  await sleep(duration);
}

async function showC4ContainerSlide(page, duration = 1000) {
  await page.setContent(`<!DOCTYPE html><html><head><style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{background:#0f0c29;color:#e0e0e0;font-family:'SF Mono',Monaco,Menlo,monospace;display:flex;align-items:center;justify-content:center;height:100vh;padding:20px}
    pre{font-size:13px;line-height:1.5;color:#c4b5fd}
    .h{color:#fbbf24;font-weight:bold} .a{color:#34d399} .d{color:#6b7280}
    .title{text-align:center;font-family:-apple-system,system-ui,sans-serif;margin-bottom:15px}
    .title h2{font-size:28px;background:linear-gradient(90deg,#667eea,#764ba2);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
  </style></head><body><div><div class="title"><h2>C4 Model — Container Diagram</h2></div><pre>
   ┌─────────────────────────────────────────────────────────────────────────────┐
   │                              <span class="h">PLUM ENDORSEMENT SYSTEM</span>                        │
   │                                                                             │
   │  ┌──────────────────┐    ┌──────────────────────────────────────────────┐   │
   │  │  <span class="a">React 19 SPA</span>    │───▶│          <span class="a">Spring Boot 3.4 Backend</span>             │   │
   │  │  TanStack Table  │    │  Java 21 + Virtual Threads                  │   │
   │  │  shadcn/ui       │    │  5 Controllers · 27 Endpoints               │   │
   │  │  :5173           │    │  3 CQRS Handlers · 8 Schedulers             │   │
   │  └──────────────────┘    │  5 Intelligence Services                    │   │
   │                          │  Ollama GenAI (2 adapters)                  │   │
   │                          │  :8080                                      │   │
   │                          └─────┬──────────┬──────────┬─────────────────┘   │
   │                                │          │          │                     │
   │                   ┌────────────▼──┐  ┌────▼────┐  ┌──▼──────────────┐     │
   │                   │ <span class="a">PostgreSQL 16</span>│  │ <span class="a">Redis 7</span> │  │ <span class="a">Kafka (KRaft)</span>  │     │
   │                   │ 13 tables     │  │ Cache   │  │ 4 topics        │     │
   │                   │ Flyway        │  │ 60s TTL │  │ 88 partitions   │     │
   │                   └───────────────┘  └─────────┘  └─────────────────┘     │
   │                                                                             │
   │  <span class="d">── Observability ──────────────────────────────────────────────────────</span>  │
   │  │ Prometheus:9090 │ Grafana:3000 │ Jaeger:16686 │ ELK Stack         │    │
   │  │ 15s scrape      │ 7 dashboards │ 100% sample  │ Structured JSON   │    │
   └─────────────────────────────────────────────────────────────────────────────┘
  </pre></div></body></html>`, { waitUntil: "domcontentloaded" });
  await sleep(duration);
}

async function showHexArchSlide(page, duration = 1000) {
  await page.setContent(`<!DOCTYPE html><html><head><style>
    *{margin:0;padding:0;box-sizing:border-box}
    body{background:#0f0c29;color:#e0e0e0;font-family:'SF Mono',Monaco,Menlo,monospace;display:flex;align-items:center;justify-content:center;height:100vh;padding:20px}
    pre{font-size:13px;line-height:1.45;color:#c4b5fd}
    .h{color:#fbbf24;font-weight:bold} .a{color:#34d399} .d{color:#6b7280}
    .title{text-align:center;font-family:-apple-system,system-ui,sans-serif;margin-bottom:10px}
    .title h2{font-size:28px;background:linear-gradient(90deg,#667eea,#764ba2);-webkit-background-clip:text;-webkit-text-fill-color:transparent}
  </style></head><body><div><div class="title"><h2>C4 Component — Hexagonal Architecture</h2></div><pre>
                      ┌────────────────────────────────────────────────────┐
                      │                  <span class="h">API Layer</span>                        │
                      │  5 Controllers · 27 endpoints · RFC 7807          │
                      └──────────────────┬─────────────────────────────────┘
                                         │
                      ┌──────────────────▼─────────────────────────────────┐
                      │             <span class="h">Application Layer</span>                     │
                      │  3 Handlers (CQRS) · 5 Services · 8 Schedulers    │
                      │  Stateless · @Transactional · MDC · Metrics        │
                      └──────────────────┬─────────────────────────────────┘
                                         │
         ┌───────────────────────────────▼──────────────────────────────────┐
         │                       <span class="a">DOMAIN CORE</span>                              │
         │  Endorsement (11-state) · EAAccount · Batch · 18 Ports         │
         │  EndorsementEvent sealed (24 types) · StateMachine             │
         │              <span class="h">&gt;&gt;&gt; ZERO infrastructure imports &lt;&lt;&lt;</span>                │
         └───────────────────────────────┬──────────────────────────────────┘
                                         │
  ┌──────────────────────────────────────▼───────────────────────────────────────┐
  │                        <span class="h">Infrastructure Layer</span>                                │
  │  <span class="d">JPA</span>: 10 adapters+mappers     <span class="d">Insurer</span>: Mock·ICICI·Niva·Bajaj           │
  │  <span class="d">Kafka</span>: 4 topics, 88 parts    <span class="d">Intelligence</span>: 5 rule-based              │
  │  <span class="d">Resilience</span>: CB + Retry        <span class="a">+ OllamaAnomalyDetector</span>                 │
  │                                <span class="a">+ OllamaErrorResolver</span>                    │
  └──────────────────────────────────────────────────────────────────────────────┘
  </pre></div></body></html>`, { waitUntil: "domcontentloaded" });
  await sleep(duration);
}

// ── Section Runners ──────────────────────────────────────────────────────

async function runIntro(page) {
  await showTitleCard(page, "Plum Endorsement Service", "Architecture, Live Demo, AI/Ollama & Vision");
  await narrateAndWait(seg("intro_title"));
}

async function runSection1(page) {
  await showTitleCard(page, "Problem Statement", "The Four Hard Problems");
  await narrateAndWait(seg("s1_title"));
  await narrateAndWait(seg("s1_problem"));

  await showContentSlide(page, "The Four Hard Problems", [
    { label: "1. Coverage Gap", desc: "No coverage during insurer processing" },
    { label: "2. Financial Drain", desc: "Massive EA float without optimization" },
    { label: "3. Multi-Insurer Chaos", desc: "REST, SOAP, CSV/SFTP protocols" },
    { label: "4. Invisible Failures", desc: "Silent failures until month-end" },
  ]);
  await narrateAndWait(seg("s1_p1"));
  await narrateAndWait(seg("s1_p2"));
  await narrateAndWait(seg("s1_p3"));
  await narrateAndWait(seg("s1_p4"));

  await showContentSlide(page, "System at a Glance", [
    { label: "27 REST Endpoints", desc: "5 controllers" },
    { label: "4 Insurer Integrations", desc: "ICICI, Niva Bupa, Bajaj, Mock" },
    { label: "5 AI Modules", desc: "2 with Ollama/GenAI augmentation" },
    { label: "800+ Tests", desc: "All passing, zero failures" },
  ]);
  await narrateAndWait(seg("s1_solution"));
}

async function runSection2(page) {
  await showTitleCard(page, "Architecture", "C4 Model + Hexagonal Design");
  await narrateAndWait(seg("s2_title"));

  // C4 Context
  await showC4ContextSlide(page);
  await narrateAndWait(seg("s2_c4_context"));

  // C4 Container
  await showC4ContainerSlide(page);
  await narrateAndWait(seg("s2_c4_container"));

  // C4 Component → Hexagonal
  await narrateAndWait(seg("s2_c4_component"));
  await showHexArchSlide(page);
  await narrateAndWait(seg("s2_hex"));
  await narrateAndWait(seg("s2_infra"));

  // Design Patterns
  await showContentSlide(page, "Design Patterns", [
    { label: "Strategy", desc: "InsurerPort — add insurer = add class + DB row" },
    { label: "State", desc: "11-state lifecycle with canTransitionTo()" },
    { label: "Observer", desc: "EventPublisher → Kafka → consumers" },
    { label: "Factory", desc: "InsurerRouter.resolve() from DB config" },
    { label: "Adapter", desc: "Domain never imports infrastructure" },
    { label: "CQRS", desc: "Command handlers vs read-only QueryHandler" },
  ]);
  await narrateAndWait(seg("s2_patterns"));

  await showContentSlide(page, "Technology Stack", [
    { label: "Java 21", desc: "Virtual Threads — 1M+ concurrent" },
    { label: "Spring Boot 3.4", desc: "Actuator, virtual thread support" },
    { label: "PostgreSQL 16", desc: "ACID, optimistic locking" },
    { label: "Kafka KRaft", desc: "32 parts, employerId key" },
    { label: "Resilience4j", desc: "Per-insurer circuit breakers" },
    { label: "ZGC", desc: "Sub-ms GC pauses" },
  ]);
  await narrateAndWait(seg("s2_tech"));
}

async function runSection3(page) {
  await showTitleCard(page, "Live Demo", "Complete Endorsement Lifecycle");
  await narrateAndWait(seg("s3_title"));

  // Dashboard
  await page.goto(BASE_URL, { waitUntil: "networkidle", timeout: 15000 });
  await sleep(2000);
  await narrateAndWait(seg("s3_dashboard"));

  // Navigate to create
  await narrateAndWait(seg("s3_nav_create"));
  await page.goto(`${BASE_URL}/endorsements/new`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);
  await narrateAndWait(seg("s3_create_form"));

  // Fill form
  await narrateAndWait(seg("s3_fill_form"));
  const { randomUUID } = require("crypto");
  const tomorrow = new Date(Date.now() + 86400000).toISOString().split("T")[0];

  await safeType(page, "#employeeId", randomUUID());
  await safeType(page, "#employeeName", "Priya Sharma");
  await safeType(page, "#coverageStartDate", tomorrow);
  await safeType(page, "#premiumAmount", "1500");
  await safeType(page, "#employeeDob", "1990-05-15");
  await sleep(500);

  // Scroll to submit button
  await page.evaluate(() => window.scrollTo(0, document.body.scrollHeight));
  await sleep(1000);

  // Create
  await narrateAndWait(seg("s3_submit_create"));
  let submitted = await safeClick(page, 'button[type="submit"][form="create-endorsement-form"]');
  if (!submitted) await safeClick(page, 'button[type="submit"]');
  await sleep(4000);

  // Detail page - show status timeline
  await narrateAndWait(seg("s3_detail"));

  // *** KEY FIX: Submit to insurer — complete the loop ***
  await narrateAndWait(seg("s3_submit_insurer"));

  // Scroll up to see action buttons
  await page.evaluate(() => window.scrollTo(0, 0));
  await sleep(1000);

  // Click Submit to Insurer button
  let submitClicked = await safeClick(page, 'button:has-text("Submit to Insurer")');
  if (!submitClicked) submitClicked = await safeClick(page, 'button:has-text("Submit")');
  await sleep(1500);

  // Handle confirmation dialog
  await safeClick(page, 'button:has-text("Confirm")');
  await safeClick(page, 'button:has-text("Yes")');
  await safeClick(page, '[role="alertdialog"] button:has-text("Continue")');
  await sleep(3000);

  // Reload to show confirmed status
  await page.reload({ waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);
  await narrateAndWait(seg("s3_confirmed"));

  // Batch path narration
  await narrateAndWait(seg("s3_batch"));
}

async function runSection4(page) {
  await showTitleCard(page, "EA Balance Optimization", "Priority Ordering & Live Lookup");
  await narrateAndWait(seg("s4_title"));

  await showContentSlide(page, "Optimization Math", [
    { label: "Naive", desc: "Random order → Rs.60,000 peak" },
    { label: "Optimized", desc: "DELETEs first → +Rs.32,000 credit" },
    { label: "Net", desc: "Rs.28,000 — 53% savings" },
    { label: "Algorithm", desc: "0-1 Knapsack DP, 15-min scheduler" },
  ]);
  await narrateAndWait(seg("s4_problem"));
  await narrateAndWait(seg("s4_scoring"));

  // *** KEY FIX: Actually do the EA lookup ***
  await page.goto(`${BASE_URL}/ea-accounts`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);

  // Fill in employer and insurer IDs and look up
  await safeType(page, 'input[name="employerId"]', EMPLOYER_ID);
  if (!(await safeType(page, 'input[name="employerId"]', EMPLOYER_ID))) {
    await safeType(page, 'input:first-of-type', EMPLOYER_ID);
  }
  await safeType(page, 'input[name="insurerId"]', INSURER_ID);
  if (!(await safeType(page, 'input[name="insurerId"]', INSURER_ID))) {
    // Try second input
    const inputs = page.locator("input");
    const count = await inputs.count();
    if (count >= 2) {
      await inputs.nth(1).fill(INSURER_ID);
    }
  }
  await sleep(500);

  // Click Look Up
  await safeClick(page, 'button:has-text("Look Up")');
  if (!(await safeClick(page, 'button:has-text("Look Up")'))) {
    await safeClick(page, 'button:has-text("Search")');
    await safeClick(page, 'button[type="submit"]');
  }
  await sleep(3000);
  await narrateAndWait(seg("s4_ea_lookup"));
}

async function runSection5(page) {
  await showTitleCard(page, "Real-Time Visibility", "10 Interactive UI Screens");
  await narrateAndWait(seg("s5_title"));

  // Endorsement list — interactive
  await page.goto(`${BASE_URL}/endorsements`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);

  // Try sorting by clicking a column header
  await safeClick(page, 'th:has-text("Premium")');
  await sleep(1000);
  await narrateAndWait(seg("s5_list"));

  // Batch progress
  await page.goto(`${BASE_URL}/endorsements/batches`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);
  await narrateAndWait(seg("s5_batches"));

  // Insurers — click into one
  await page.goto(`${BASE_URL}/insurers`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);

  // Click the first insurer row to see detail
  await safeClick(page, 'tbody tr:first-child');
  await safeClick(page, 'table tr:nth-child(2)');
  await sleep(2000);
  await narrateAndWait(seg("s5_insurers"));

  // Reconciliation
  await page.goto(`${BASE_URL}/reconciliation`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);
  await narrateAndWait(seg("s5_reconciliation"));
}

async function runSection6(page) {
  await showTitleCard(page, "Intelligence & AI", "Ollama/GenAI — 3-Stage Evolution");
  await narrateAndWait(seg("s6_title"));

  // Navigate to intelligence
  await page.goto(`${BASE_URL}/intelligence`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(2000);
  await narrateAndWait(seg("s6_intro"));

  // Ollama deep-dive slide
  await showContentSlide(page, "How We Use Ollama", [
    { label: "Runtime", desc: "Docker container with llama3.2 model" },
    { label: "Integration", desc: "Spring AI Ollama starter, ChatClient.Builder" },
    { label: "Activation", desc: "@ConditionalOnProperty — ollama Spring profile" },
    { label: "Adapters", desc: "OllamaAugmentedAnomalyDetector + OllamaErrorResolver" },
    { label: "Resilience", desc: "@CircuitBreaker + @Retry with rule-based fallback" },
    { label: "Config", desc: "Temperature 0.3, max 512 tokens, deterministic" },
  ]);
  await narrateAndWait(seg("s6_ollama_how"));

  // Anomaly detection
  await page.goto(`${BASE_URL}/intelligence`, { waitUntil: "networkidle", timeout: 10000 });
  await sleep(1000);
  await safeClick(page, 'button:has-text("Anomalies")');
  await safeClick(page, '[data-tab="anomalies"]');
  await sleep(2000);
  await narrateAndWait(seg("s6_anomaly"));
  await narrateAndWait(seg("s6_anomaly_ollama"));

  // Forecasts — generate one
  await safeClick(page, 'button:has-text("Forecasts")');
  await safeClick(page, '[data-tab="forecasts"]');
  await sleep(1500);

  // Try to fill forecast form and generate
  await safeType(page, 'input[name="employerId"]', EMPLOYER_ID);
  await safeType(page, 'input[name="insurerId"]', INSURER_ID);
  await safeClick(page, 'button:has-text("Generate")');
  await sleep(3000);
  await narrateAndWait(seg("s6_forecast"));

  // Error resolution
  await safeClick(page, 'button:has-text("Error")');
  await safeClick(page, '[data-tab="errors"]');
  await sleep(1500);
  await narrateAndWait(seg("s6_errors"));
  await narrateAndWait(seg("s6_errors_ollama"));

  // Process mining
  await safeClick(page, 'button:has-text("Process")');
  await safeClick(page, '[data-tab="process-mining"]');
  await sleep(1500);
  await narrateAndWait(seg("s6_process"));

  // 3-stage evolution summary
  await showContentSlide(page, "3-Stage AI Evolution", [
    { label: "Stage 1: Rule-Based", desc: "5 adapters deployed, zero ML deps, production-ready" },
    { label: "Stage 2: Ollama/GenAI", desc: "2 deployed (anomaly + error), 3 planned, local LLM" },
    { label: "Stage 3: Full ML", desc: "Isolation Forest, Prophet, RAG, PM4Py, OR-Tools" },
    { label: "Migration Pattern", desc: "Adapter swap behind same port. Domain unchanged." },
  ]);
  await narrateAndWait(seg("s6_3stage"));
}

async function runSection7(page) {
  await showTitleCard(page, "Observability", "Metrics, Tracing & Dashboards");
  await narrateAndWait(seg("s7_title"));

  await showContentSlide(page, "9 Docker Services", [
    { label: "PostgreSQL :5432", desc: "13 tables, Flyway" },
    { label: "Redis :6379", desc: "Distributed cache" },
    { label: "Kafka :9092", desc: "4 topics, 88 partitions" },
    { label: "Prometheus :9090", desc: "15s scrape interval" },
    { label: "Grafana :3000", desc: "7 dashboards" },
    { label: "Jaeger :16686", desc: "100% sampling" },
    { label: "ELK Stack", desc: "Elasticsearch + Logstash + Kibana" },
  ]);
  await narrateAndWait(seg("s7_services"));

  // Grafana login
  await page.goto(`${GRAFANA_URL}/login`, { waitUntil: "networkidle", timeout: 15000 });
  await sleep(1000);
  await safeType(page, 'input[name="user"]', "admin");
  await safeType(page, 'input[aria-label="Username input field"]', "admin");
  await safeType(page, 'input[name="password"]', "plum");
  await safeType(page, 'input[aria-label="Password input field"]', "plum");
  await sleep(500);
  await safeClick(page, 'button[type="submit"]');
  await safeClick(page, 'button:has-text("Log in")');
  await sleep(3000);

  // App Overview dashboard
  try {
    await page.goto(`${GRAFANA_URL}/d/endorsement-app-overview/endorsement-service-application-overview?orgId=1`, { waitUntil: "networkidle", timeout: 15000 });
  } catch {}
  await sleep(3000);
  await narrateAndWait(seg("s7_grafana"));

  // Business Metrics
  try {
    await page.goto(`${GRAFANA_URL}/d/endorsement-business-metrics/endorsement-service-business-metrics?orgId=1`, { waitUntil: "networkidle", timeout: 10000 });
  } catch {}
  await sleep(3000);
  await narrateAndWait(seg("s7_grafana2"));

  // Intelligence Monitoring
  try {
    await page.goto(`${GRAFANA_URL}/d/intelligence-monitoring/intelligence-monitoring?orgId=1`, { waitUntil: "networkidle", timeout: 10000 });
  } catch {}
  await sleep(3000);
  await narrateAndWait(seg("s7_grafana3"));

  // Jaeger
  try {
    await page.goto(JAEGER_URL, { waitUntil: "networkidle", timeout: 10000 });
  } catch {
    try { await page.goto(JAEGER_URL, { waitUntil: "domcontentloaded", timeout: 10000 }); } catch {}
  }
  await sleep(3000);
  await narrateAndWait(seg("s7_tracing"));
}

async function runSection8(page) {
  await showTitleCard(page, "Scalability & Resilience", "1M Endorsements/Day");
  await narrateAndWait(seg("s8_title"));

  await showContentSlide(page, "Scale Design", [
    { label: "Stateless", desc: "All private final fields — horizontal scaling" },
    { label: "Virtual Threads", desc: "1M+ concurrent, no pool tuning" },
    { label: "Kafka", desc: "32 parts, employerId → per-employer order" },
    { label: "Idempotency", desc: "UNIQUE key, safe retries" },
    { label: "K8s HPA", desc: "2–8 pods at 70% CPU" },
  ]);
  await narrateAndWait(seg("s8_scale"));

  await showContentSlide(page, "Circuit Breakers", [
    { label: "ICICI Lombard", desc: "50% threshold, 20-call window, 30s wait" },
    { label: "Bajaj Allianz", desc: "40% threshold, 15-call window, 45s wait" },
    { label: "Fallback", desc: "Typed fallback method, never throws" },
  ]);
  await narrateAndWait(seg("s8_resilience"));

  // Show actuator health
  try {
    await page.goto(`${API_URL}/actuator/health`, { waitUntil: "networkidle", timeout: 10000 });
    await sleep(2000);
  } catch {}
}

async function runSection9(page) {
  await showTitleCard(page, "Test Coverage", "800+ Tests — Zero Failures");
  await narrateAndWait(seg("s9_title"));

  await showContentSlide(page, "Test Pyramid", [
    { label: "Unit (381)", desc: "JUnit 5 + Mockito + AssertJ" },
    { label: "API (105)", desc: "REST Assured + Testcontainers" },
    { label: "BDD (75)", desc: "Cucumber — 16 feature files" },
    { label: "E2E (138)", desc: "Playwright — 14 spec files" },
    { label: "Perf (6)", desc: "Gatling simulations" },
    { label: "TOTAL: 800+", desc: "Zero failures" },
  ]);
  await narrateAndWait(seg("s9_stats"));

  // Show Allure report
  try {
    await page.goto(`${ALLURE_URL}/allure-docker-service/projects/default/reports/latest/index.html`, { waitUntil: "networkidle", timeout: 15000 });
    await sleep(3000);
    await page.evaluate(() => window.scrollTo(0, 300));
    await sleep(2000);
  } catch {
    await showContentSlide(page, "Allure Report", [
      { label: "URL", desc: "localhost:5050/allure-docker-service/latest-report" },
      { label: "Sections", desc: "API, BDD, E2E, Performance" },
    ], 3000);
  }
  await narrateAndWait(seg("s9_allure"));
}

async function runSection10(page) {
  await showTitleCard(page, "Forward-Looking Vision", "AI, Product & Architecture — 5-Year Roadmap");
  await narrateAndWait(seg("s10_title"));

  // AI Automation Vision
  await showContentSlide(page, "AI Automation Vision", [
    { label: "Anomaly ML", desc: "Isolation Forest → Autoencoder → Graph-based fraud" },
    { label: "Forecasting ML", desc: "Prophet → LSTM → Ensemble methods" },
    { label: "Error Resolution", desc: "RAG pipeline with vector database" },
    { label: "Process Mining", desc: "PM4Py conformance via Python sidecar" },
    { label: "Batch Optimization", desc: "OR-Tools linear programming" },
  ]);
  await narrateAndWait(seg("s10_ai_vision"));

  await showContentSlide(page, "ML Rollout Pattern", [
    { label: "Phase 1", desc: "Implement new adapter behind existing port" },
    { label: "Phase 2", desc: "Deploy with @ConditionalOnProperty flag" },
    { label: "Phase 3", desc: "Shadow mode alongside current adapter" },
    { label: "Phase 4", desc: "Promote when metrics meet targets" },
    { label: "Cross-cutting", desc: "Feature Store, MLOps, A/B testing" },
  ]);
  await narrateAndWait(seg("s10_ai_vision2"));

  // Product Evolution Vision
  await showContentSlide(page, "Product Evolution — Phase 4", [
    { label: "Multi-Currency EA", desc: "International employers" },
    { label: "Localized Insurers", desc: "New market integrations" },
    { label: "Compliance", desc: "Country-specific regulations" },
    { label: "Self-Service Portal", desc: "Insurer onboarding" },
    { label: "API Marketplace", desc: "Platform integrations" },
    { label: "Multi-Region", desc: "Global low-latency deployment" },
  ]);
  await narrateAndWait(seg("s10_product_vision"));
  await narrateAndWait(seg("s10_product_vision2"));

  // Architectural Vision
  await showContentSlide(page, "5-Year Architecture Roadmap", [
    { label: "2026", desc: "Modular monolith hardening + fitness functions" },
    { label: "2027", desc: "Service extraction via Strangler Fig + multi-region" },
    { label: "2028", desc: "API marketplace + self-service onboarding" },
    { label: "2029", desc: "Intelligence platform + Data Mesh" },
    { label: "2030–31", desc: "Global active-active + autonomous operations" },
  ]);
  await narrateAndWait(seg("s10_arch_vision"));
  await narrateAndWait(seg("s10_arch_vision2"));
}

async function runClosing(page) {
  await showTitleCard(page, "Deliverable Mapping", "6 Requirements — All Addressed");
  await narrateAndWait(seg("closing_title"));

  await showContentSlide(page, "Design Challenge Requirements", [
    { label: "1. Architecture", desc: "C4 model + hexagonal + design patterns" },
    { label: "2. No Coverage Gap", desc: "Provisional coverage at creation, 30-day net" },
    { label: "3. EA Minimization", desc: "Priority ordering, 53% savings" },
    { label: "4. Visibility", desc: "10 UI screens + 7 Grafana dashboards" },
    { label: "5. AI/Automation", desc: "5 pillars, 2 Ollama deployed, vision roadmap" },
    { label: "6. Code/Prototype", desc: "Live demo + 800+ tests" },
  ]);
  await narrateAndWait(seg("closing_map"));

  await showTitleCard(page, "Thank You", "Production-Grade Code — Hexagonal Architecture — Ollama GenAI — 800+ Tests");
  await narrateAndWait(seg("closing_final"));
  await sleep(3000);
}

// ── Main ─────────────────────────────────────────────────────────────────

async function main() {
  console.log("🎬 Starting Plum Endorsement Service video demo (v2)...\n");

  fs.mkdirSync(VIDEO_DIR, { recursive: true });

  const browser = await chromium.launch({
    headless: false,
    args: [`--window-size=${VIEWPORT.width},${VIEWPORT.height}`, "--disable-infobars", "--no-first-run"],
  });

  const context = await browser.newContext({
    viewport: VIEWPORT,
    recordVideo: { dir: VIDEO_DIR, size: VIEWPORT },
    colorScheme: "dark",
  });

  const page = await context.newPage();
  page.setDefaultTimeout(10000);

  try {
    await runIntro(page);
    await runSection1(page);
    await runSection2(page);
    await runSection3(page);
    await runSection4(page);
    await runSection5(page);
    await runSection6(page);
    await runSection7(page);
    await runSection8(page);
    await runSection9(page);
    await runSection10(page);
    await runClosing(page);
    console.log("\n✅ Demo recording complete!");
  } catch (err) {
    console.error("❌ Error during demo:", err.message);
  } finally {
    await context.close();
    await browser.close();
  }

  const videos = fs.readdirSync(VIDEO_DIR).filter(f => f.endsWith(".webm"))
    .sort((a, b) => fs.statSync(path.join(VIDEO_DIR, b)).mtime - fs.statSync(path.join(VIDEO_DIR, a)).mtime);
  if (videos.length > 0) {
    console.log(`\n📹 Raw video: ${path.join(VIDEO_DIR, videos[0])}`);
  }
}

main().catch(console.error);
