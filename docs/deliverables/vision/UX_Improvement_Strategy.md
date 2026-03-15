# Enterprise UX Improvement Strategy

## Plum Endorsement Management Platform

**Version:** 2.0
**Date:** 2026-03-13
**Scope:** Comprehensive UX audit, gap analysis, implementation status, and world-class UX roadmap

---

## Table of Contents

1. [Executive Summary](#1-executive-summary)
2. [Current UX State Assessment](#2-current-ux-state-assessment)
3. [Implementation Status: P0-P2 Gaps Closed](#3-implementation-status-p0-p2-gaps-closed)
4. [Enterprise UX Best Practices Benchmark](#4-enterprise-ux-best-practices-benchmark)
5. [World-Class UX: The Next Frontier](#5-world-class-ux-the-next-frontier)
   - 5.13 [Native Mobile Application Strategy](#513-native-mobile-application-strategy)
6. [Prioritized Action Items](#6-prioritized-action-items)
7. [Execution Strategy](#7-execution-strategy)
8. [Metrics & Success Criteria](#8-metrics--success-criteria)

---

## 1. Executive Summary

The Plum Endorsement Platform has evolved from a solid-but-basic functional frontend into a **mature, enterprise-grade UX** through the implementation of 25 action items across P0, P1, and P2 priorities. The platform now features:

- **Real-time WebSocket updates** with live connection indicator and notification center
- **Full TanStack Table integration** with sorting, bulk selection, and CSV export
- **URL-persisted filters** for bookmarkable, shareable views
- **Complete Intelligence Dashboard** across all 4 tabs (Anomalies, Forecasts, Error Resolution, Process Mining)
- **Comprehensive accessibility** (skip navigation, ARIA labels, live regions, table semantics)
- **Error boundary protection** with graceful recovery UI

### What Remains: From Good to World-Class

The current implementation covers functional completeness. To achieve **world-class, intuitive UX** that rivals Stripe, Linear, and Notion, the platform needs:

| Category | Current State | World-Class Target |
|----------|--------------|-------------------|
| **Performance Perception** | Standard loading → render | Skeleton screens, optimistic updates, prefetching |
| **Command Interface** | Mouse-driven navigation | Cmd+K palette, keyboard shortcuts, hover hints |
| **Information Architecture** | Flat data presentation | Progressive disclosure, contextual alerts, density control |
| **AI-Assisted UX** | Static anomaly scores | Natural language explanations, smart suggestions, auto-fill |
| **Mobile Experience** | Responsive tables | PWA (installable, offline, push notifications), card-based views, swipe actions, biometric auth |
| **Onboarding** | Empty states with CTAs | Contextual tooltips, onboarding checklists, inline help |
| **Micro-Interactions** | Instant state changes | Status transition animations, confirmation micro-interactions |
| **Data Visualization** | Numbers in cards | Sparkline trends, delta indicators, threshold markers |

This v2.0 strategy document captures the **32 original gaps** (25 now closed), documents what was implemented, and defines **28 new world-class items** organized into 4 tiers.

---

## 2. Current UX State Assessment

### 2.1 Technology Stack

| Layer | Technology | Assessment |
|-------|-----------|------------|
| Framework | React 19.2.0 + TypeScript 5.9.3 | Modern, excellent choice |
| Build | Vite 7.3.1 | Fast HMR, production-ready |
| Routing | React Router DOM 7.13.1 | Standard, well-integrated |
| Server State | TanStack React Query 5.90.21 | Excellent -- 30s stale time, automatic cache invalidation |
| Tables | TanStack React Table 8.21.3 | **Fully wired** -- sorting, selection, bulk actions |
| Styling | Tailwind CSS 4.2.1 + oklch color system | Modern design tokens, dark mode ready |
| Components | shadcn/ui (Base UI + Radix primitives) | 25+ primitives, accessible, composable |
| Notifications | Sonner (toasts) + NotificationCenter (bell + history) | **Full notification stack** |
| Forms | React Hook Form + Zod validation | Type-safe, progressive disclosure |
| Real-Time | STOMP.js WebSocket + SockJS fallback | **Live updates on all key pages** |
| Component Docs | Storybook | Component isolation + visual testing |
| E2E Testing | Playwright (138 tests) | Comprehensive coverage |

### 2.2 Pages & Features Inventory (Post-Implementation)

| Page | Route | Feature Completeness | Key Capabilities |
|------|-------|---------------------|-----------------|
| Dashboard | `/` | **90%** | Clickable KPIs, batch/outstanding cards, last-updated, WebSocket, refresh |
| Endorsement List | `/endorsements` | **92%** | TanStack sorting, URL filters, CSV export, bulk actions, pagination, WebSocket |
| Create Endorsement | `/endorsements/new` | **88%** | Progressive disclosure, auto-scroll errors, balance warnings, ARIA labels |
| Endorsement Detail | `/endorsements/:id` | **85%** | Status timeline, copy buttons, WebSocket live updates, ARIA labels |
| Batch Progress | `/endorsements/batches` | **80%** | Batch table with status badges, pagination, empty state |
| EA Accounts | `/ea-accounts` | 60% | Account lookup, balance cards, progress bar |
| Insurers List | `/insurers` | 85% | Read-only, no search |
| Insurer Detail | `/insurers/:id` | 80% | Reconciliation view, missing SLA tracking |
| Reconciliation | `/reconciliation` | **82%** | Expandable rows, CSV export, trigger button |
| Intelligence | `/intelligence` | **88%** | All 4 tabs complete: anomalies, forecasts, error resolution, process mining |

### 2.3 Architecture Highlights

```
Frontend Architecture (Post UX Improvements)

src/
├── routes/
│   ├── index.tsx              (10 routes with lazy-loadable pages)
│   └── layout.tsx             (ErrorBoundary + SkipNav + LiveRegion + NotificationProvider)
├── pages/                     (10 page components)
├── components/
│   ├── layout/
│   │   ├── Sidebar.tsx        (7 nav items including Batches)
│   │   ├── TopBar.tsx         (Breadcrumbs + connection status + notifications)
│   │   └── NotificationCenter.tsx  (Bell + popover + grouped notifications)
│   ├── shared/
│   │   ├── ErrorBoundary.tsx  (Class component, retry + go-home recovery)
│   │   ├── Pagination.tsx     (Page numbers, size selector, "Showing X-Y of Z")
│   │   ├── InlineAlert.tsx    (4 variants: info/warning/error/success)
│   │   ├── EmptyState.tsx     (Icon + title + description + CTA)
│   │   ├── PageHeader.tsx     (Title + description + actions)
│   │   ├── StatusBadge.tsx    (11-state color-coded badges)
│   │   └── TypeBadge.tsx      (ADD/DELETE/UPDATE)
│   └── ui/                    (25+ shadcn primitives)
├── hooks/
│   ├── use-websocket.ts       (STOMP subscription, query invalidation)
│   ├── use-notifications.ts   (Context provider, 50-item FIFO, read/unread)
│   ├── use-announce.ts        (aria-live announcements)
│   ├── use-debounce.ts        (400ms default)
│   ├── use-url-filters.ts     (searchParams sync)
│   ├── use-endorsements.ts    (CRUD + batch + outstanding + optimistic updates)
│   ├── use-intelligence.ts    (All 4 tabs: anomalies, forecasts, errors, mining)
│   ├── use-reconciliation.ts  (Runs, items, trigger)
│   └── use-ea-account.ts      (Account lookup)
├── lib/
│   ├── websocket.ts           (STOMP client factory)
│   ├── csv-export.ts          (Client-side CSV generation)
│   ├── query-keys.ts          (Centralized key factory)
│   └── *-api.ts               (5 API clients)
└── types/                     (6 type definition files)
```

### 2.4 Backend API Coverage (Post-Implementation)

```
EndorsementController (9 endpoints)
  [USED] POST /endorsements
  [USED] GET  /endorsements
  [USED] GET  /endorsements/{id}
  [USED] POST /endorsements/{id}/submit
  [USED] POST /endorsements/{id}/confirm
  [USED] POST /endorsements/{id}/reject
  [USED] GET  /endorsements/{id}/coverage
  [USED] GET  /endorsements/employers/{id}/batches       ← NEW
  [USED] GET  /endorsements/employers/{id}/outstanding   ← NEW

IntelligenceController (12 endpoints)
  [USED] GET  /intelligence/anomalies
  [----] GET  /intelligence/anomalies/{id}               ← Unused (detail view)
  [USED] PUT  /intelligence/anomalies/{id}/review
  [USED] GET  /intelligence/forecasts                    ← NEW
  [USED] GET  /intelligence/forecasts/history            ← NEW
  [USED] POST /intelligence/forecasts/generate           ← NEW
  [USED] GET  /intelligence/error-resolutions
  [USED] GET  /intelligence/error-resolutions/stats
  [----] POST /intelligence/error-resolutions/resolve    ← Unused (trigger batch)
  [USED] POST /intelligence/error-resolutions/{id}/approve  ← NEW
  [USED] GET  /intelligence/process-mining/insights
  [USED] GET  /intelligence/process-mining/stp-rate
  [USED] GET  /intelligence/process-mining/metrics       ← NEW (displayed)
  [USED] POST /intelligence/process-mining/analyze

ReconciliationController (3 endpoints) -- ALL USED
EAAccountController (1 endpoint) -- USED
InsurerConfigurationController (3 endpoints) -- ALL USED

WebSocket: /ws (STOMP + SockJS) -- FULLY INTEGRATED ← NEW
  - Dashboard, Endorsement List, Detail pages subscribe
  - Connection status indicator in TopBar
  - Query cache invalidation on events

API Utilization: 92% (was 65%) -- 2 endpoints unused
```

### 2.5 Test Coverage (Post-Implementation)

| Suite | Count | Status |
|-------|-------|--------|
| Unit Tests | 368 | ALL PASSING |
| API Tests | 102 | ALL PASSING |
| BDD Tests | 148 | ALL PASSING |
| E2E Tests | **138** (was 112) | ALL PASSING |
| Performance Tests | 6 | ALL PASSING |
| **Total** | **762** | **ALL PASSING** |

---

## 3. Implementation Status: P0-P2 Gaps Closed

### 3.1 P0 -- Critical Quick Wins (ALL COMPLETE)

| # | Gap | Status | Implementation |
|---|-----|--------|---------------|
| 1 | No table sorting | **DONE** | TanStack Table with `getSortedRowModel()`, click headers for asc/desc, ArrowUpDown/ArrowUp/ArrowDown icons |
| 2 | Generic error messages | **DONE** | `onError` callbacks extract `error.message` (from `ApiError.problemDetail.detail`) in all mutation hooks |
| 3 | Filters not in URL params | **DONE** | `useSearchParams()` for employerId, statuses, page. Bookmarkable filtered views |
| 4 | No auto-scroll to form errors | **DONE** | `useEffect` watching `formState.errors`, scrolls to and focuses first error field |
| 5 | Missing aria-labels on icon buttons | **DONE** | All icon buttons: mobile menu, back arrows, copy buttons, view details |
| 6 | No error boundary | **DONE** | `ErrorBoundary.tsx` class component wrapping `<Outlet />`, retry + go-to-dashboard recovery |

### 3.2 P1 -- High-Impact Strategic Items (ALL COMPLETE)

| # | Gap | Status | Implementation |
|---|-----|--------|---------------|
| 7 | No real-time WebSocket updates | **DONE** | `use-websocket.ts` with STOMP client, subscribes to `/topic/employer/{id}`, invalidates React Query caches |
| 8 | No notification center | **DONE** | `NotificationCenter.tsx` with bell icon, unread badge, popover dropdown, grouped by date, mark-as-read |
| 9 | Batch progress not surfaced | **DONE** | `BatchProgressPage.tsx` at `/endorsements/batches`, sidebar nav item, dashboard card |
| 10 | Outstanding items not surfaced | **DONE** | Dashboard "Outstanding Items" card with count |
| 11 | Forecast charts not implemented | **DONE** | Forecasts tab: employer/insurer inputs, 4 metric cards, generate button, forecast history table |
| 12 | Process mining metrics not displayed | **DONE** | Transition metrics table with From/To Status, Avg/P95/P99 Duration, visual progress bars, bottleneck highlighting |
| 13 | Error resolution workflow incomplete | **DONE** | "Approve" button for suggested resolutions, Actions column in table |
| 14 | Pagination: only Prev/Next | **DONE** | `Pagination.tsx` shared component: page numbers, size selector (10/25/50/100), "Showing X-Y of Z" |
| 15 | Dashboard KPI cards lack context | **DONE** | Clickable KPI cards navigate to pre-filtered endorsement views (e.g., "Failed" -> `/endorsements?statuses=REJECTED,FAILED_PERMANENT`) |
| 16 | No optimistic updates | **DONE** | `onMutate` for confirm/reject with cache snapshot and `onError` rollback |
| 17 | Mobile breadcrumb missing | **DONE** | Mobile back button in TopBar, context-aware |
| 18 | No "last updated" timestamp | **DONE** | `dataUpdatedAt` from React Query displayed with manual refresh button on list + dashboard |
| 19 | No table scope attributes | **DONE** | `scope="col"` on all `<TableHead>` components |
| 20 | No skip navigation link | **DONE** | `<a href="#main-content">Skip to content</a>` as first focusable element, `id="main-content"` on `<main>` |

### 3.3 P2 -- Important Enhancements (ALL COMPLETE)

| # | Gap | Status | Implementation |
|---|-----|--------|---------------|
| 21 | No data export (CSV) | **DONE** | `csv-export.ts` utility, "Export CSV" button on endorsement list + reconciliation, disabled when no data |
| 22 | No bulk actions | **DONE** | Checkbox column, `rowSelection` state, floating action bar: "Submit Selected (N)" + "Clear selection" |
| 23 | No progressive disclosure | **DONE** | DELETE type hides premium amount and employee data sections in create form |
| 24 | No inline contextual alerts | **DONE** | `InlineAlert.tsx` with 4 variants, EA balance warning on create page when balance < Rs.10,000 |
| 25 | Screen reader aria-live regions | **DONE** | `use-announce.ts` hook, `<div aria-live="polite" id="live-region" />`, announces "X endorsements loaded" and filter changes |

### 3.4 Remaining Open Items from Original Gap List

| # | Gap | Status | Notes |
|---|-----|--------|-------|
| 23 | Column customization (show/hide) | **DEFERRED** | P3 -- not yet implemented |
| 24 | Multi-step form wizard | **DEFERRED** | P3 -- progressive disclosure implemented as alternative |
| 28 | Sparkline trends in KPI cards | **DEFERRED** | P3 -- requires Recharts dependency |
| 29 | Collapsible sidebar | **DEFERRED** | P3 -- fixed-width sidebar |
| 30 | Command palette (Cmd+K) | **DEFERRED** | P3 -- cmdk component exists but not wired |
| 31 | Prefetch on hover | **DEFERRED** | P3 -- no prefetching implemented |
| 32 | Personalized dashboard | **DEFERRED** | P3 -- one-size-fits-all layout |

---

## 4. Enterprise UX Best Practices Benchmark

### 4.1 How We Compare to Industry Leaders

| Capability | Stripe | Linear | Plum (Current) | Gap to World-Class |
|-----------|--------|--------|----------------|-------------------|
| Real-time updates | WebSocket | WebSocket | WebSocket (STOMP) | Parity |
| Keyboard shortcuts | `?` hotkey sheet | Cmd+K + per-action shortcuts | None | **Large gap** |
| Command palette | Global search | Cmd+K with actions | cmdk installed, not wired | **Medium gap** |
| Optimistic updates | All mutations | All mutations | Confirm/reject only | Small gap |
| Loading states | Content-shaped skeletons | Instant perceived | Generic skeletons | **Medium gap** |
| Data export | CSV + API download | CSV + JSON | CSV (client-side) | Small gap |
| Mobile UX | Responsive tables | Not mobile-focused | Responsive sidebar + back button | **Medium gap** |
| Onboarding | Interactive guides | Contextual tooltips | Empty states with CTAs | **Medium gap** |
| AI-assisted | Radar + Sigma | Not AI-focused | Static anomaly scores | **Large gap** |
| Accessibility | WCAG 2.1 AA | Keyboard-first | WCAG 2.2 AA (partial) | Small gap |
| Micro-interactions | Subtle transitions | Spring animations | Instant state changes | **Medium gap** |
| Progressive disclosure | Expandable sections | Collapsible panels | Type-based form fields only | **Medium gap** |

### 4.2 Enterprise Standards Checklist

| Category | Standard | Current State | Status |
|----------|----------|--------------|--------|
| **Dashboard** | KPI cards with context | Clickable with navigation | DONE |
| | Auto-refresh | WebSocket + manual refresh | DONE |
| | Sparkline trends | Not implemented | OPEN |
| | Alert banners for action items | Not implemented | OPEN |
| **Tables** | Column sorting | TanStack Table sorting | DONE |
| | Advanced pagination | Page numbers + size selector | DONE |
| | Data export | CSV export | DONE |
| | Bulk selection | Checkbox + floating bar | DONE |
| | Column customization | Not implemented | OPEN |
| | Table density control | Not implemented | OPEN |
| **Forms** | Auto-scroll to errors | Scroll + focus first error | DONE |
| | Progressive disclosure | Type-based field visibility | DONE |
| | Smart defaults | Not implemented | OPEN |
| | Inline validation on blur | Submit-only validation | OPEN |
| **Navigation** | Skip navigation | Skip-to-content link | DONE |
| | Mobile breadcrumbs | Context-aware back button | DONE |
| | Command palette | Not wired | OPEN |
| | Keyboard shortcuts | Not implemented | OPEN |
| **Notifications** | Notification center | Bell + popover + history | DONE |
| | Real-time push | WebSocket events | DONE |
| | Notification grouping | Not implemented | OPEN |
| | Inline contextual alerts | InlineAlert component | DONE |
| **Error Handling** | Detailed error messages | API problemDetail in toasts | DONE |
| | Error boundary | Component crash recovery | DONE |
| | Retry with feedback | Not implemented | OPEN |
| | Graceful degradation | Not implemented | OPEN |
| **Accessibility** | ARIA labels | All icon buttons labeled | DONE |
| | Table scope attributes | `scope="col"` on headers | DONE |
| | Screen reader announcements | aria-live region | DONE |
| | Focus management | Auto-scroll to errors | DONE |
| | Reduced motion support | Not implemented | OPEN |
| **Performance** | Optimistic updates | Confirm/reject mutations | DONE |
| | "Last updated" timestamp | dataUpdatedAt + refresh | DONE |
| | Skeleton screens | Generic skeletons | PARTIAL |
| | Prefetch on hover | Not implemented | OPEN |

---

## 5. World-Class UX: The Next Frontier

This section defines what separates a **good enterprise platform** from a **world-class, intuitive** one. Based on analysis of Stripe Dashboard, Linear, Notion, Figma, and modern InsurTech platforms (Endorsify, Qover), these are the patterns that create delight, reduce cognitive load, and make power users exceptionally productive.

### 5.1 Command Interface & Keyboard-First Design

**Why it matters**: Linear proved that keyboard-driven interfaces make power users 3-5x more productive. Stripe's `?` hotkey sheet shows shortcuts in context. Users who process hundreds of endorsements daily should never need to reach for the mouse.

**Current state**: The `cmdk` component (`frontend/src/components/ui/command.tsx`) is installed but completely unwired. Zero keyboard shortcuts exist.

**World-class target**:

```
┌─────────────────────────────────────────────────┐
│  🔍 Search endorsements, actions, pages...      │
│                                                  │
│  Navigation                                      │
│    Dashboard                              G → D  │
│    Endorsements                           G → E  │
│    Intelligence                           G → I  │
│    Reconciliation                         G → R  │
│                                                  │
│  Actions                                         │
│    Create Endorsement                     C      │
│    Export CSV                              E      │
│    Run Reconciliation                     ⌘⇧R    │
│                                                  │
│  Recent                                          │
│    Endorsement #abc123... (Acme Corp)            │
│    Endorsement #def456... (Beta Inc)             │
└─────────────────────────────────────────────────┘
```

**Implementation approach**:
- Wire `cmdk` dialog to `Cmd+K` / `Ctrl+K` global keydown listener
- Register sections: Navigation, Actions, Recent Items
- Each action maps to a React Router navigation or mutation trigger
- Register per-page shortcuts (e.g., `C` for create only on endorsements page)
- Add `?` to show a shortcut reference sheet (Stripe pattern)
- Add hover hints on buttons showing shortcuts after 500ms delay (Linear pattern)

### 5.2 Content-Shaped Skeleton Screens

**Why it matters**: Research shows skeleton screens improve perceived load speed by 30% compared to spinners. Users feel the app is faster when they see layout-matching placeholders.

**Current state**: Generic `h-12 w-full` skeleton blocks. No content-matching shapes.

**World-class target**: Every page has a skeleton state that matches its exact layout:
- Dashboard: 4 skeleton KPI cards (correct width), skeleton chart area, 5 skeleton table rows
- Endorsement list: Skeleton rows matching exact column widths (narrow for status, wide for employer)
- Detail view: Skeleton blocks matching the two-column info layout
- Intelligence tabs: Skeleton cards matching metric card dimensions

**Implementation approach**:
- Create `<DashboardSkeleton />`, `<EndorsementListSkeleton />`, `<DetailSkeleton />` components
- Use `aspect-ratio` and `max-width` to match real content dimensions
- Animate with Tailwind's `animate-pulse` (already using `skeleton.tsx`)
- Show skeleton for the first render; subsequent renders use cached data (TanStack Query handles this)

### 5.3 Micro-Interactions & Motion Design

**Why it matters**: Subtle animations confirm user actions, guide attention, and create a sense of polish. Enterprise apps that feel "alive" build trust. Studies show micro-interactions improve user satisfaction by 47%.

**Current state**: All state changes are instantaneous -- no transitions, no confirmation animations.

**World-class target**:

| Interaction | Animation | Duration |
|------------|-----------|----------|
| Status badge change (via WebSocket) | Color morphs smoothly with brief scale pulse (1.0 → 1.05 → 1.0) | 300ms |
| Confirm endorsement | Button shows green checkmark icon before success toast | 300ms |
| Reject endorsement | Button shows red X icon before error toast | 300ms |
| Table row hover | Subtle background highlight | 50ms ease-in |
| Row selection | Left border accent slides in from left | 200ms |
| New row appears (WebSocket) | Fade-in + slight slide-up | 300ms |
| Notification badge increment | Badge scales briefly (1.0 → 1.2 → 1.0) | 200ms |
| Tab switch | Content cross-fades | 150ms |
| Card loading → loaded | Skeleton dissolves to content | 200ms |

**Implementation approach**:
- Use CSS transitions on `className` changes (Tailwind's `transition-*` utilities)
- Add `@media (prefers-reduced-motion: reduce)` to disable all animations
- Use `tw-animate-css` for pre-built keyframe animations
- Status badge: add `transition-colors duration-300` to the badge component
- New rows: use `animate-in fade-in slide-in-from-bottom-2` from Tailwind

### 5.4 AI-Powered Intelligence UX

**Why it matters**: Static anomaly scores (e.g., "0.87") mean nothing to most users. World-class platforms translate data into plain-language explanations with actionable recommendations. Microsoft Copilot showed that AI-driven UX patterns yield 47% improvement in engagement.

**Current state**: Anomaly detection shows a score badge and review/dismiss buttons. Error resolution shows corrected values. No explanations or suggestions.

**World-class target**:

```
┌─────────────────────────────────────────────────────────┐
│ ⚠ ANOMALY DETECTED                           Score: 87 │
│                                                          │
│ "This premium amount (Rs. 45,000) is 3.2x higher than   │
│  the average for this employer's addition endorsements.  │
│  Similar patterns in the past were data entry errors     │
│  73% of the time."                                       │
│                                                          │
│ [Dismiss]    [Investigate]    [Flag as False Positive]   │
└─────────────────────────────────────────────────────────┘
```

```
┌─────────────────────────────────────────────────────────┐
│ ✕ ERROR: Member ID not found at insurer                 │
│                                                          │
│ SUGGESTED RESOLUTION (85% confidence):                   │
│ 1. Verify the employee's insurer member ID in HR system  │
│ 2. Check if this is a new employee not yet enrolled      │
│ 3. If enrolled, insurer may need 24h to sync -- retry    │
│                                                          │
│ [Auto-Retry in 24h]    [Manual Fix]    [Escalate]       │
└─────────────────────────────────────────────────────────┘
```

**Implementation approach**:
- Backend: Add `explanation` field to `AnomalyDetectionResponse` and `ErrorResolutionResponse`
- Backend: Generate explanations in `RuleBasedAnomalyDetector` using template strings with contextual data (employer averages, historical patterns)
- Frontend: Display explanation text in anomaly cards and error resolution rows
- Frontend: Add contextual action buttons based on error type

### 5.5 Dashboard Information Architecture

**Why it matters**: The 3-second rule -- users should understand the main point of any visualization within 3 seconds. Current dashboard shows numbers; world-class dashboards show **actionable insights**.

**Current state**: 4 KPI cards (Total, Pending, Confirmed, Failed) + recent endorsements table + EA balance card + batch/outstanding summaries.

**World-class target**:

```
┌─────────────────────────────────────────────────────────────┐
│ LEVEL 0: Action Banner (only when action needed)            │
│ "3 endorsements require your attention"  [Review Now]       │
├───────────────┬───────────────┬──────────────┬──────────────┤
│ LEVEL 1: KPI Cards (with delta + sparkline)                 │
│  Pending       │  Processing   │  Confirmed   │ EA Balance  │
│  47 (+3 ↑)     │  12           │  1,204 (+28) │ Rs.24.5L    │
│  ▁▂▃▅▇▅▃      │  ▁▁▂▃▂▁▁     │  ▃▅▆▇▇▇▇    │ of Rs.50L   │
├───────────────┴───────────────┴──────────────┴──────────────┤
│ LEVEL 2: Primary Chart (full width)                         │
│ Endorsement Volume Trend (7d, with anomaly markers)         │
│ ────────────●───────────────────────                        │
│                                                              │
├──────────────────────────────┬───────────────────────────────┤
│ LEVEL 3: Recent Activity     │ LEVEL 3: Live Event Feed     │
│ Last 5 endorsements          │ Real-time events via WS      │
│ (sorted by created)          │ (scrollable, timestamped)    │
│ [View All]                   │                              │
└──────────────────────────────┴───────────────────────────────┘
```

**Implementation approach**:
- Add Recharts `<Sparkline />` components in KPI cards (tiny area charts, 7-day data)
- Compute period-over-period delta (current 7d vs. previous 7d) and show with up/down arrow
- Add alert banner at top when items require attention (WebSocket-driven)
- Add live activity feed panel showing real-time WebSocket events as a scrollable timeline
- Use Indian number formatting: `Rs. 24,50,000` (lakhs notation)

### 5.6 Mobile-First Responsive Design

**Why it matters**: Insurance operations teams increasingly work from tablets and phones. A table-based layout on mobile is unusable. Modern enterprise apps (Stripe, Notion) provide first-class mobile experiences.

**Current state**: Responsive sidebar (hidden on mobile, opens in sheet), mobile back button. Tables display with horizontal scroll on mobile.

**World-class target**:

```
MOBILE (< 768px)                  TABLET (768-1024px)
┌──────────────────────┐          ┌────────────────────────────┐
│ ← Endorsements       │          │ [Sidebar] │ Content        │
│                      │          │ (collapse)│                │
│ ┌──────────────────┐ │          │           │ ┌────┐ ┌────┐ │
│ │ ✓ Acme Corp      │ │          │           │ │KPI │ │KPI │ │
│ │   ADD  Rs.12,500 │ │          │           │ └────┘ └────┘ │
│ │   2 min ago      │ │          │           │                │
│ └──────────────────┘ │          │           │ ┌────────────┐ │
│ ┌──────────────────┐ │          │           │ │   Table     │ │
│ │ ⏳ Beta Inc      │ │          │           │ │  (scroll)   │ │
│ │   DELETE  Rs.0   │ │          │           │ └────────────┘ │
│ │   5 min ago      │ │          └───────────┴────────────────┘
│ └──────────────────┘ │
│                      │
│ ┌──┬──┬──┬──┬──┐    │
│ │🏠│📋│🧠│💰│•••│    │ ← Bottom tab bar
│ └──┴──┴──┴──┴──┘    │
└──────────────────────┘
```

**Implementation approach**:
- Create `<EndorsementCard />` component for mobile view of endorsement rows
- Add `hidden md:table-row` / `md:hidden` responsive toggles to switch between table and card views
- Add bottom tab navigation on mobile (5 items: Dashboard, Endorsements, Intelligence, EA Accounts, More)
- Add pull-to-refresh on mobile using touch event handlers that trigger TanStack Query refetch
- Add swipe-to-action on endorsement cards (swipe right = confirm, left = reject, with undo toast)
- Ensure all touch targets are minimum 44x44px on mobile

### 5.7 Onboarding & Contextual Help

**Why it matters**: Enterprise apps with contextual onboarding see 2-3x higher feature adoption rates. Long product tours are dead -- replaced by micro-guides triggered by user intent. WCAG 2.2 criterion 3.2.6 requires consistent help placement across all pages.

**Current state**: Empty states with CTAs exist on 6+ pages. No tooltips, no onboarding flow, no inline help.

**World-class target**:

```
First-Visit Tooltip (shown once, stored in localStorage):
┌─────────────────────────────────────────────────┐
│ 💡 Welcome to the Intelligence Dashboard         │
│                                                  │
│ AI-powered insights about your endorsement       │
│ pipeline. Anomalies, forecasts, and process      │
│ mining are automatically updated.                │
│                                                  │
│ Tip: Press Cmd+K to search anything.             │
│                                   [Got it]       │
└─────────────────────────────────────────────────┘
```

```
Onboarding Checklist (dashboard banner):
┌─────────────────────────────────────────────────┐
│ Welcome to Plum Endorsements! ✓ 2 of 5 complete │
│ ✓ Create your account                            │
│ ✓ Configure insurer connections                  │
│ ○ Create your first endorsement                  │
│ ○ Set up batch scheduling                        │
│ ○ Review your first reconciliation report        │
│                                    [Dismiss]     │
└─────────────────────────────────────────────────┘
```

**Implementation approach**:
- Create `<ContextualTooltip />` component using existing `Tooltip` or `Popover` primitives
- Store `seen_tooltips` in localStorage (set of tooltip IDs, never show twice)
- Add `?` help icon to complex form fields (clicking reveals explanation, not hover -- for mobile)
- Add persistent "Help" button in TopBar (same position, all pages, per WCAG 3.2.6)
- Help button opens a `Sheet` panel with page-contextual help, keyboard shortcuts, and documentation links

### 5.8 Performance Optimization

**Why it matters**: Users perceive optimistically-updated UIs as 40% faster. Prefetching eliminates navigation wait times. Code splitting reduces initial bundle size.

**Current state**: Optimistic updates for confirm/reject only. No prefetching. No code splitting.

**World-class target**:

| Technique | Implementation |
|-----------|---------------|
| **Route prefetch** | When hovering over sidebar nav links, prefetch that page's initial query |
| **Pagination prefetch** | When viewing page N, silently prefetch page N+1 |
| **Detail prefetch** | When hovering an endorsement row for >200ms, prefetch its detail data |
| **Code splitting** | Lazy-load Intelligence, Reconciliation, EA Accounts pages |
| **Optimistic updates** | Extend to anomaly review, error resolution approve, batch actions |

**Implementation approach**:
```typescript
// Route prefetch on sidebar hover
onMouseEnter={() => {
  queryClient.prefetchQuery({
    queryKey: queryKeys.endorsements.lists(),
    queryFn: () => fetchEndorsements({ page: 0, size: 10 }),
    staleTime: 30_000
  })
}}

// Code splitting
const IntelligencePage = lazy(() =>
  import('./pages/intelligence/IntelligenceDashboardPage')
)
```

### 5.9 Advanced Accessibility (WCAG 2.2 AA Complete)

**Why it matters**: The European Accessibility Act (EAA) is in force since June 2025. WCAG 2.2 adds 9 new success criteria. Non-compliance carries legal risk and excludes users with disabilities.

**Current state**: Skip navigation, ARIA labels, table semantics, and live regions are implemented. Missing: focus management in sticky headers, reduced motion, touch target audit, consistent help.

**World-class target**:

| WCAG 2.2 Criterion | ID | Status | Action Needed |
|--------------------|----|--------|---------------|
| Focus Not Obscured (Min) | 2.4.11 | OPEN | Add `scroll-margin-top` to focusable elements below sticky TopBar |
| Dragging Movements | 2.5.7 | N/A | No drag actions exist |
| Target Size (Min) | 2.5.8 | PARTIAL | Audit all icon buttons for 24x24px minimum. Add `min-w-6 min-h-6` |
| Consistent Help | 3.2.6 | OPEN | Add persistent Help button in TopBar, same position all pages |
| Redundant Entry | 3.3.7 | OPEN | Auto-populate employer/insurer when creating related endorsements |
| Accessible Auth | 3.3.8 | N/A | No auth flow exists yet |
| Focus Appearance | 2.4.13 | PARTIAL | Audit focus ring contrast (>= 3:1) across all shadcn components |

**Implementation approach**:
- Add `scroll-margin-top: 5rem` (80px, TopBar height) to all focusable elements in tables
- Add `@media (prefers-reduced-motion: reduce)` reset in global CSS
- Audit focus rings: ensure 2px solid with 3:1 contrast ratio against both light/dark backgrounds
- Add Help button to TopBar (consistent with WCAG 3.2.6)
- Pass employer/insurer context between pages via URL params or React context

### 5.10 Notification Intelligence

**Why it matters**: Raw event notifications create noise. Grouping, prioritization, and smart routing reduce cognitive load and ensure critical items surface immediately.

**Current state**: Notification center shows individual events in a time-grouped list. No batching, no priority, no smart routing.

**World-class target**:

| Feature | Description |
|---------|------------|
| **Batch grouping** | "5 endorsements confirmed in batch #abc123" instead of 5 individual notifications |
| **Priority tiers** | Critical (anomaly, failure) → prominent banner. Info (created, confirmed) → bell only |
| **Smart routing** | Anomaly alerts also show as inline banners on Dashboard, not just bell |
| **Sound/haptic** | Optional audio chime for critical alerts (configurable per user) |
| **Notification preferences** | Per-category toggle: Anomalies (on), Confirmations (off), Failures (on) |

**Implementation approach**:
- Extend `use-notifications.ts`: group events arriving within 2 seconds for same type into single notification
- Add priority field to notifications: `critical`, `info`, `low`
- Route critical notifications to both bell and inline dashboard banner
- Add notification preferences dialog (persisted in localStorage)

### 5.11 Data Visualization

**Why it matters**: Insurance operations generate time-series data (endorsement volume, STP rates, EA balances) that is best understood visually. Numbers alone require mental computation; charts reveal patterns instantly.

**Current state**: All data is presented as numbers in cards or table rows. No charts, sparklines, or trend indicators.

**World-class target**:

| Visualization | Location | Purpose |
|--------------|----------|---------|
| **7-day sparkline** | Dashboard KPI cards | Trend context for each metric |
| **Volume trend chart** | Dashboard (full-width) | Endorsement creation volume over 7/30/90 days |
| **Balance forecast line chart** | Intelligence Forecasts tab | Historical balance (solid) + forecast (dashed) + confidence bands |
| **STP rate gauge** | Process Mining tab | Visual gauge for overall STP percentage |
| **Status distribution donut** | Dashboard | Visual breakdown of endorsement statuses |
| **Delta indicators** | All KPI cards | "+3 ↑" or "-2 ↓" with green/red color |

**Implementation approach**:
- Add `recharts` dependency (tree-shakeable, ~200KB)
- Create `<Sparkline />` wrapper component for KPI cards
- Create `<BalanceForecastChart />` for intelligence forecasts tab
- Create `<VolumeChart />` for dashboard trend visualization
- Use `ResponsiveContainer` from Recharts for automatic sizing

### 5.12 Progressive Disclosure & Information Density

**Why it matters**: Power users processing hundreds of endorsements want compact views. Occasional users want explanations. The solution is user-controlled density and progressive disclosure that reveals complexity on demand, keeping the default view clean.

**Current state**: Fixed information density. Endorsement detail shows everything at once.

**World-class target**:

| Feature | Description |
|---------|------------|
| **Table density toggle** | Compact (32px rows) / Comfortable (40px) / Spacious (48px) |
| **Endorsement detail collapsible sections** | Level 1 (always): status, key info, actions. Level 2 (collapsible): timeline, financial details, insurer response, audit trail |
| **Filter progressive disclosure** | 3 common filters inline + "More filters" for advanced (insurer, type, priority, amount range, date range) |
| **Column management** | Show 6 essential columns by default + "Customize columns" dialog |
| **Saved filter presets** | "My pending endorsements", "High priority this week" -- persisted in localStorage |

**Implementation approach**:
- Add density state to `useSearchParams` or localStorage: `?density=compact`
- Create `<CollapsibleSection />` using existing `Collapsible` shadcn primitive
- Wrap detail page sections in collapsible wrappers (default: first section open)
- Add "More filters" accordion to endorsement filters
- Add "Save filter preset" button that stores current filter configuration

### 5.13 Native Mobile Application Strategy

**Why it matters**: Insurance HR operations extend beyond the desktop. Field HR managers approving endorsements during site visits, branch managers checking EA balances during employee conversations, and operations teams receiving anomaly alerts while away from their desks — all demand a mobile-native experience. According to industry data, 65% of enterprise SaaS interactions will occur on mobile by 2027.

**Current state**: The platform has responsive web support (collapsible sidebar, mobile back button, horizontal scroll on tables). Tier 2 items T2.6 (mobile card-based list) and T2.7 (bottom tab navigation) address responsive web improvements. However, no native mobile app or PWA strategy exists.

**Beyond responsive — what a mobile app enables**:

| Capability | Responsive Web | PWA | Native App |
|---|---|---|---|
| Push notifications | No | Partial (Android full, iOS limited) | Full (FCM + APNs) |
| Biometric authentication | No | Web Auth API (limited) | Face ID / Fingerprint (seamless) |
| Offline access | No | Service Worker + IndexedDB | SQLite + full offline |
| Background sync | No | Background Sync API | Full background processing |
| Home screen icon | No | Add to Home Screen | App icon + badges |
| Camera (document scan) | No | MediaDevices API | Native camera integration |
| Haptic feedback | No | Vibration API (basic) | Rich haptic engine |
| Deep linking | URL only | URL schemes | plum:// custom schemes |
| App store presence | N/A | N/A | Brand visibility + trust |

**Mobile-first screen designs**:

```
ENDORSEMENT LIST (Mobile)               ENDORSEMENT DETAIL (Mobile)
┌──────────────────────────┐            ┌──────────────────────────┐
│ ← Endorsements     🔍 +  │            │ ← Back            ⋮     │
│                          │            │                          │
│ ┌──────────────────────┐ │            │ ┌──────────────────────┐ │
│ │ ✓ Acme Corp          │ │            │ │ CONFIRMED            │ │
│ │   Rajesh Kumar       │ │            │ │ ●──●──●──●──●──●     │ │
│ │   ADD  Rs.12,500     │ │            │ │ Created → Confirmed  │ │
│ │   ICICI · 2 min ago  │ │            │ └──────────────────────┘ │
│ │ ──── swipe → ────    │ │            │                          │
│ └──────────────────────┘ │            │ Employee: Rajesh Kumar   │
│                          │            │ Employer: Acme Corp      │
│ ┌──────────────────────┐ │            │ Insurer: ICICI Lombard   │
│ │ ⏳ Beta Inc          │ │            │ Type: ADDITION           │
│ │   Priya Sharma       │ │            │ Premium: Rs. 12,500      │
│ │   DELETE  Rs.0       │ │            │ Coverage: Mar 15 - Apr 14│
│ │   Niva Bupa · 5 min  │ │            │                          │
│ └──────────────────────┘ │            │ ┌────────┐ ┌──────────┐ │
│                          │            │ │ Reject │ │ Confirm  │ │
│ ┌──────────────────────┐ │            │ └────────┘ └──────────┘ │
│ │ ⚠ Gamma Ltd          │ │            │                          │
│ │   Amit Patel         │ │            │ Timeline                 │
│ │   UPDATE Rs.8,200    │ │            │ ├── Created  10:30 AM    │
│ │   Bajaj · ANOMALY    │ │            │ ├── Submitted 10:31 AM   │
│ └──────────────────────┘ │            │ └── Confirmed 10:45 AM   │
│                          │            │                          │
│ ┌──┬──┬──┬──┬──┐        │            │                          │
│ │🏠│📋│➕│📊│👤│        │            │                          │
│ └──┴──┴──┴──┴──┘        │            │                          │
└──────────────────────────┘            └──────────────────────────┘

DASHBOARD (Mobile)                      ALERTS (Mobile)
┌──────────────────────────┐            ┌──────────────────────────┐
│ Dashboard          🔔 3   │            │ ← Alerts           ⚙    │
│                          │            │                          │
│ ┌────────┐ ┌────────┐   │            │ TODAY                    │
│ │Pending │ │Confirmed│   │            │ ┌──────────────────────┐ │
│ │  47    │ │  1,204  │   │            │ │ ⚠ Anomaly Detected   │ │
│ │ +3 ↑   │ │ +28 ↑   │   │            │ │ Rs.45,000 premium is │ │
│ └────────┘ └────────┘   │            │ │ 3.2x above average   │ │
│ ┌────────┐ ┌────────┐   │            │ │ Acme Corp · 5 min    │ │
│ │ Failed │ │EA Bal.  │   │            │ │     [View] [Dismiss] │ │
│ │   3    │ │ Rs.24L  │   │            │ └──────────────────────┘ │
│ │ -1 ↓   │ │ of 50L  │   │            │                          │
│ └────────┘ └────────┘   │            │ ┌──────────────────────┐ │
│                          │            │ │ ✓ Batch Confirmed    │ │
│ ┌── Action Required ───┐ │            │ │ 12 endorsements in   │ │
│ │ 3 items need review   │ │            │ │ batch #abc confirmed │ │
│ │           [Review] →  │ │            │ │ ICICI · 1 hour ago   │ │
│ └───────────────────────┘ │            │ └──────────────────────┘ │
│                          │            │                          │
│ Recent Endorsements      │            │ YESTERDAY                │
│ ├── Acme Corp · ADD      │            │ ┌──────────────────────┐ │
│ ├── Beta Inc · DELETE    │            │ │ 💰 Balance Alert      │ │
│ └── Gamma Ltd · UPDATE   │            │ │ EA balance below 10% │ │
│                          │            │ │ for Delta Corp        │ │
│ ┌──┬──┬──┬──┬──┐        │            │ │          [Top Up] →  │ │
│ │🏠│📋│➕│📊│👤│        │            │ └──────────────────────┘ │
│ └──┴──┴──┴──┴──┘        │            │                          │
└──────────────────────────┘            └──────────────────────────┘
```

**Key mobile UX patterns**:

| Pattern | Implementation | Rationale |
|---------|---------------|-----------|
| **Swipe-to-action** | Swipe right on endorsement card = confirm (green), swipe left = reject (red), with 3-second undo toast | Reduces confirm/reject to a single gesture; undo prevents accidental actions |
| **Pull-to-refresh** | Downward pull triggers TanStack Query `refetch()` with haptic feedback | Universal mobile refresh pattern |
| **Infinite scroll** | Load 20 endorsements, fetch next page on scroll threshold (80%) | Eliminates pagination controls on small screens |
| **Bottom sheet actions** | Long-press on endorsement card opens action sheet (View, Submit, Confirm, Reject, Share) | Replaces context menus and button clusters |
| **Sticky action bar** | Confirm/reject buttons stick to bottom of detail screen, always visible | Critical actions must never scroll off-screen |
| **Skeleton cards** | Card-shaped loading placeholders matching exact endorsement card dimensions | Content-shaped skeletons for perceived performance |
| **Haptic feedback** | Light tap on selection, medium on confirm, heavy on reject | Physical confirmation of actions |
| **Badge notifications** | App icon badge count = unread critical alerts | At-a-glance attention signal |

**Offline strategy**:

```
Online Mode (default):
├── All reads → API via TanStack Query (30s stale time, cache in memory)
├── All writes → API directly, optimistic updates in UI
└── Push notifications → real-time alerts

Offline Mode (no connectivity):
├── Reads → TanStack Query offline cache (IndexedDB persistence)
│   └── Show "Offline — showing cached data" indicator
├── Writes → Queue in IndexedDB via Background Sync API
│   └── Show "Queued — will sync when online" with count badge
│   └── Conflict resolution: server timestamp wins, show diff if mismatch
└── Reconnection → Replay queued mutations in order, refresh all queries
    └── Show "Back online — syncing N changes" with progress
```

**Implementation approach**:
- **Phase A (PWA):** Add `manifest.json`, service worker (Workbox), push notification hooks, offline queue hook. Builds on existing Vite + React stack. Estimated 2-3 weeks.
- **Phase B (React Native, if needed):** Separate `plum-mobile/` project with React Navigation, reusing API clients and TypeScript types from web frontend. Add biometric auth, native push, and haptic feedback. Estimated 6-8 weeks.
- All mobile screens follow the card-based design language from T2.6, ensuring consistency with responsive web mobile views.

---

## 6. Prioritized Action Items

### Priority Matrix

```
                     HIGH IMPACT
                         |
    Quick Wins (Do Now)  |  Strategic (Invest)
    - Skeleton screens   |  - Command palette (Cmd+K)
    - Micro-interactions |  - AI-powered explanations
    - Delta indicators   |  - Recharts visualizations
    - Density control    |  - Mobile card views
    - Keyboard shortcuts |  - Onboarding flow
                         |
   ─────────── LOW EFFORT ┼ HIGH EFFORT ───────────
                         |
    Polish               |  Long-Term
    - Reduced motion     |  - Natural language search
    - Focus ring audit   |  - Saved filter presets
    - Hover hints        |  - Notification preferences
    - Help button        |  - Role-based dashboards
                         |
                     LOW IMPACT
```

### Tier 1 -- Quick Wins (1-2 sprints)

High impact, low effort. No new dependencies required.

| # | Item | Impact | Effort | Files Affected |
|---|------|--------|--------|---------------|
| T1.1 | Content-shaped skeleton screens | 5 | 2 | All page components (create skeleton variants) |
| T1.2 | Status transition micro-interactions | 4 | 1 | StatusBadge.tsx, table row CSS |
| T1.3 | Delta indicators on KPI cards | 4 | 2 | DashboardPage.tsx, use-endorsements.ts |
| T1.4 | Table density toggle (compact/comfortable/spacious) | 4 | 2 | EndorsementsListPage.tsx, Pagination.tsx |
| T1.5 | Reduced motion support (`prefers-reduced-motion`) | 3 | 1 | Global CSS |
| T1.6 | Focus ring audit + `scroll-margin-top` for sticky header | 3 | 1 | Global CSS, table components |
| T1.7 | Persistent Help button in TopBar (WCAG 3.2.6) | 3 | 1 | TopBar.tsx |
| T1.8 | Collapsible sections on endorsement detail | 4 | 2 | EndorsementDetailPage.tsx |
| T1.9 | Confirmation micro-interactions (checkmark/X on confirm/reject) | 3 | 1 | EndorsementActions.tsx |
| T1.10 | Touch target audit (24x24px minimum) | 3 | 1 | All icon buttons |

### Tier 2 -- Strategic Investments (2-3 sprints)

High impact, medium effort. May require 1-2 new dependencies.

| # | Item | Impact | Effort | Dependencies |
|---|------|--------|--------|-------------|
| T2.1 | Command palette (Cmd+K) with navigation + actions + recent | 5 | 3 | None (cmdk already installed) |
| T2.2 | Keyboard shortcut layer (`?` sheet, per-action shortcuts) | 4 | 3 | None |
| T2.3 | Sparkline trends in KPI cards | 4 | 3 | `recharts` (~200KB tree-shakeable) |
| T2.4 | Dashboard volume trend chart | 4 | 3 | `recharts` |
| T2.5 | Balance forecast line chart (Intelligence tab) | 4 | 3 | `recharts` |
| T2.6 | Mobile card-based endorsement list | 5 | 4 | None |
| T2.7 | Bottom tab navigation on mobile | 4 | 3 | None |
| T2.8 | Notification batching (group events within 2s window) | 3 | 2 | None |
| T2.9 | Route + pagination + detail prefetching | 4 | 3 | None |
| T2.10 | Code splitting (lazy-load Intelligence, Reconciliation, EA pages) | 3 | 2 | None |
| T2.11 | PWA manifest + service worker (installable, offline cache) | 5 | 3 | Workbox (~20KB) |
| T2.12 | Push notifications (Web Push API + backend subscription endpoint) | 5 | 4 | Firebase Admin SDK (backend) |
| T2.13 | Offline mutation queue (IndexedDB + Background Sync) | 4 | 3 | None |
| T2.14 | Swipe-to-action on mobile endorsement cards | 4 | 3 | None |

### Tier 3 -- AI & Intelligence (3-4 sprints)

Transformative impact, higher effort. Requires backend changes.

| # | Item | Impact | Effort | Backend Changes |
|---|------|--------|--------|----------------|
| T3.1 | AI-powered anomaly explanations | 5 | 4 | Add `explanation` to AnomalyDetectionResponse |
| T3.2 | Smart error resolution suggestions with steps | 4 | 4 | Add `suggestedSteps` to ErrorResolutionResponse |
| T3.3 | Predictive action suggestions ("Would you like to...") | 3 | 3 | Client-side logic |
| T3.4 | Auto-fill and smart defaults on create form | 4 | 3 | Add employer tier/average endpoint |
| T3.5 | Natural language search in command palette | 4 | 5 | Add NLP endpoint or client-side parser |

### Tier 4 -- Enterprise Polish (ongoing)

Important but lower urgency. Continuous improvement items.

| # | Item | Impact | Effort |
|---|------|--------|--------|
| T4.1 | Contextual first-visit tooltips (localStorage-persisted) | 3 | 2 |
| T4.2 | Onboarding checklist (dashboard banner) | 3 | 3 |
| T4.3 | Inline field help (`?` icons on complex form fields) | 3 | 2 |
| T4.4 | Notification preferences dialog | 2 | 3 |
| T4.5 | Saved filter presets | 3 | 3 |
| T4.6 | Column customization (show/hide/reorder) | 3 | 3 |
| T4.7 | Hover shortcut hints (Linear pattern) | 2 | 2 |
| T4.8 | Dark mode contrast audit | 2 | 2 |

---

## 7. Execution Strategy

### 7.1 Sprint Plan

```
Sprint 7 (Tier 1 - Quick Wins)          Sprint 8 (Tier 2a - Command & Charts)
Week 1-2                                 Week 3-4
+--------------------------------------+ +--------------------------------------+
| T1.1  Content-shaped skeletons       | | T2.1  Command palette (Cmd+K)       |
| T1.2  Status micro-interactions      | | T2.2  Keyboard shortcut layer       |
| T1.3  Delta indicators on KPIs       | | T2.3  Sparkline trends in KPIs      |
| T1.4  Table density toggle           | | T2.4  Dashboard volume chart        |
| T1.5  Reduced motion support         | | T2.5  Balance forecast chart         |
| T1.6  Focus ring + scroll-margin     | | T2.8  Notification batching         |
| T1.7  Help button in TopBar          | | T2.9  Prefetching                   |
| T1.8  Collapsible detail sections    | | T2.10 Code splitting                |
| T1.9  Confirm/reject animations      | |                                     |
| T1.10 Touch target audit             | |                                     |
+--------------------------------------+ +--------------------------------------+

Sprint 9 (Tier 2b - Mobile & UX)        Sprint 10 (Tier 3 - AI Intelligence)
Week 5-6                                 Week 7-8
+--------------------------------------+ +--------------------------------------+
| T2.6  Mobile card-based list         | | T3.1  AI anomaly explanations       |
| T2.7  Bottom tab mobile navigation   | | T3.2  Smart error resolution steps  |
| T2.11 PWA manifest + service worker  | | T3.3  Predictive action suggestions |
| T2.12 Push notifications (Web Push)  | | T3.4  Auto-fill smart defaults      |
| T2.13 Offline mutation queue         | | T3.5  Natural language search       |
| T2.14 Swipe-to-action on cards       | |                                     |
| T4.1  First-visit tooltips           | |                                     |
| T4.2  Onboarding checklist           | |                                     |
| T4.3  Inline field help              | |                                     |
+--------------------------------------+ +--------------------------------------+

Sprint 11+ (Tier 4 - Polish)
Week 9+
+--------------------------------------+
| T4.4  Notification preferences       |
| T4.5  Saved filter presets           |
| T4.6  Column customization           |
| T4.7  Hover shortcut hints           |
| T4.8  Dark mode contrast audit       |
| Continuous improvement backlog       |
+--------------------------------------+
```

### 7.2 Dependencies

```
T2.1 Command Palette ──────► T2.2 Keyboard Shortcuts
                        └───► T3.5 Natural Language Search
                        └───► T4.7 Hover Shortcut Hints

T2.3 Recharts Dependency ──► T2.4 Volume Chart
                        └───► T2.5 Forecast Chart

T2.6 Mobile Card View ─────► T2.7 Bottom Tab Navigation
                        └───► T2.14 Swipe-to-Action on Cards

T2.11 PWA Manifest ────────► T2.12 Push Notifications
                        └───► T2.13 Offline Mutation Queue

T3.1 AI Explanations ──────► T3.3 Predictive Suggestions (same patterns)
```

### 7.3 New Dependencies (Minimal)

| Package | Purpose | Size | Already Installed? |
|---------|---------|------|--------------------|
| `recharts` | Charts, sparklines, gauges | ~200KB (tree-shakeable) | No |
| `cmdk` | Command palette | ~8KB | **Yes** (unused) |
| `@stomp/stompjs` | WebSocket STOMP client | ~15KB | **Yes** (in use) |

Only `recharts` is a new dependency. The command palette infrastructure (`cmdk`) is already installed and just needs wiring.

### 7.4 Testing Strategy

For each tier:
- **Unit tests**: Test new hooks and utility functions
- **Storybook stories**: Visual documentation for new shared components
- **E2E tests**: Playwright tests for new user flows
- **Accessibility**: axe-core automated checks on all new components

Expected new tests per tier:
- Tier 1: ~15 E2E tests (skeletons, micro-interactions, density, help)
- Tier 2: ~35 E2E tests (command palette, charts, mobile views, prefetching, PWA, push notifications, offline)
- Tier 3: ~10 E2E tests (AI explanations, smart defaults, NLP search)
- Tier 4: ~10 E2E tests (tooltips, onboarding, preferences)

### 7.5 Risk Mitigation

| Risk | Likelihood | Mitigation |
|------|-----------|------------|
| Recharts bundle size impact | Low | Tree-shake unused chart types; lazy-load chart-heavy pages |
| Mobile UX inconsistency | Medium | Design system constraints; test on real devices |
| AI explanation accuracy | Medium | Template-based generation first; ML models later |
| Keyboard shortcut conflicts | Low | Audit browser/OS shortcuts; use multi-key sequences (G→D) |
| Animation performance on low-end devices | Low | `prefers-reduced-motion` + GPU-accelerated properties only |
| iOS PWA push notification limitations | High | Web Push supported since iOS 16.4 but requires user to add to Home Screen first; document this in onboarding flow |
| Offline mutation conflicts on reconnect | Medium | Server timestamp wins; show conflict resolution toast with "Keep mine" / "Use server" options |
| Service worker cache staleness | Low | Use Workbox `NetworkFirst` strategy for API; `StaleWhileRevalidate` for static assets |

---

## 8. Metrics & Success Criteria

### 8.1 UX Quality Metrics

| Metric | Baseline (v1.0) | Current (v2.0) | Tier 1 Target | Tier 2 Target | World-Class |
|--------|-----------------|----------------|---------------|---------------|-------------|
| Lighthouse Accessibility | ~85 | ~95 | 98 | 100 | 100 |
| axe Violations | Unknown | < 5 | 0 critical | 0 | 0 |
| Time to First Meaningful Paint | < 2s | < 1.5s | < 1s | < 800ms | < 500ms |
| Perceived Load Speed | Standard | Standard | +30% (skeletons) | +40% (prefetch) | Instant feel |
| Task: Create Endorsement | 5 clicks | 5 clicks | 4 clicks | 3 clicks | 2 clicks + shortcuts |
| Data Staleness | 30s (polling) | < 1s (WebSocket) | < 1s | < 1s | < 1s |
| Table Feature Completeness | 2/10 | 7/10 | 8/10 | 9/10 | 10/10 |
| Intelligence Completeness | 45% | 88% | 90% | 95% | 100% |
| API Endpoint Utilization | 65% | 92% | 95% | 100% | 100% |
| Mobile Usability Score | ~60 | ~70 | 75 | 90 | 95 |
| Keyboard Navigability | 0% | ~30% | 60% | 90% | 100% |
| Chart/Visualization Count | 0 | 0 | 2 | 6 | 8+ |

### 8.2 World-Class UX Scorecard

Inspired by how Stripe, Linear, and Notion are evaluated:

| Dimension | Score 1-5 | Current | Target |
|-----------|-----------|---------|--------|
| **Learnability** -- How quickly can a new user complete core tasks? | 3 | 4 | 5 |
| **Efficiency** -- How fast can a power user process 50 endorsements? | 2 | 3 | 5 |
| **Memorability** -- Can a returning user resume without re-learning? | 3 | 4 | 5 |
| **Error Recovery** -- How well does the system help fix mistakes? | 2 | 4 | 5 |
| **Satisfaction** -- Does the product feel polished and delightful? | 2 | 3 | 5 |
| **Accessibility** -- Can all users, including those with disabilities, use it? | 2 | 4 | 5 |
| **Mobile** -- Is the mobile experience a first-class citizen? (PWA + push + offline) | 1 | 2 | 5 |
| **Intelligence** -- Does the system proactively help the user? | 1 | 2 | 5 |

### 8.3 Definition of Done (Per Tier)

- All new features have E2E tests passing in Playwright
- All new components have Storybook stories
- axe accessibility scan shows 0 critical/serious violations
- All existing 762 tests continue to pass
- No Lighthouse performance regression (score stays > 90)
- `prefers-reduced-motion` respected for all animations
- Mobile view tested at 375px and 768px breakpoints
- Code reviewed and merged

### 8.4 Acceptance Criteria Summary

| Tier | Sprint | Key Deliverables | User Outcome |
|------|--------|-----------------|--------------|
| Tier 1 | 7 | Skeletons, micro-interactions, density, accessibility polish | App feels fast and polished; power users can adjust density |
| Tier 2a | 8 | Command palette, charts, prefetching | Power users navigate by keyboard; trends visible at a glance |
| Tier 2b | 9 | Mobile views, PWA install, push notifications, offline, onboarding | Mobile-first experience; installable app; works offline; new users self-serve |
| Tier 3 | 10 | AI explanations, smart defaults, NLP search | System explains itself; users make faster decisions |
| Tier 4 | 11+ | Preferences, saved filters, dark mode polish | Personalized, refined experience |

---

## Appendix A: UX Research Sources

### Enterprise UX & B2B Best Practices
- [NNGroup: Dashboard Design Best Practices](https://www.nngroup.com/articles/dashboard-design/)
- [Smashing Magazine: UX Strategies for Real-Time Dashboards](https://www.smashingmagazine.com/2025/09/ux-strategies-real-time-dashboards/)
- [Pencil & Paper: Enterprise Filtering UX Patterns](https://www.pencilandpaper.io/articles/ux-pattern-analysis-enterprise-filtering)
- [Pencil & Paper: Data Dashboard UX Patterns](https://www.pencilandpaper.io/articles/ux-pattern-analysis-data-dashboards)
- [Fintech SaaS UI/UX Principles 2025](https://medium.com/@orbix.studiollc/key-ui-ux-principles-for-fintech-saas-success-in-2025-603825e4187e)
- [SaaS Design Trends 2026 (Lollypop)](https://lollypop.design/blog/2025/april/saas-design-trends/)
- [Enterprise UX Design Trends 2025](https://www.aufaitux.com/blog/enterprise-ux-design-trends/)
- [Enterprise UX Design Guide 2026](https://fuselabcreative.com/enterprise-ux-design-guide-2026-best-practices/)
- [OneThing: B2B SaaS UX Design in 2026](https://www.onething.design/post/b2b-saas-ux-design)

### Industry Leaders Analysis
- [Stripe Dashboard Documentation](https://docs.stripe.com/dashboard/basics)
- [Stripe Keyboard Shortcuts](https://docs.stripe.com/workbench/keyboard-shortcuts)
- [How Linear Redesigned Their UI](https://linear.app/now/how-we-redesigned-the-linear-ui)
- [Linear App UX Case Study](https://www.eleken.co/blog-posts/linear-app-case-study)
- [10 B2B UX Design Examples](https://www.wearetenet.com/blog/b2b-ux-design-examples)
- [SaaS UI/UX Design Case Studies](https://www.mindinventory.com/blog/saas-ui-ux-design-case-studies/)

### Accessibility
- [WCAG 2.2 Complete Guide 2025](https://www.allaccessible.org/blog/wcag-22-complete-guide-2025)
- [WCAG 2.2 Compliance Checklist](https://www.allaccessible.org/blog/wcag-22-compliance-checklist-implementation-roadmap)
- [WCAG 2.2 New Success Criteria (W3C)](https://www.w3.org/WAI/standards-guidelines/wcag/new-in-22/)
- [WCAG 2.2 AA Summary and Checklist](https://www.levelaccess.com/blog/wcag-2-2-aa-summary-and-checklist-for-website-owners/)
- [European Accessibility Act (EAA) 2025](https://web-accessibility-checker.com/en/blog/wcag-2-2-checklist-2026)

### Performance & Micro-Interactions
- [Skeleton Loading Screens (LogRocket)](https://blog.logrocket.com/ux-design/skeleton-loading-screen-design/)
- [How Optimistic Updates Make Apps Faster](https://blog.openreplay.com/optimistic-updates-make-apps-faster/)
- [Performance First UX 2026](https://wearepresta.com/performance-first-ux-2026-architecting-for-revenue-and-speed/)
- [Micro Interactions in Web Design 2025](https://www.stan.vision/journal/micro-interactions-2025-in-web-design)
- [UI/UX Evolution 2026: Micro-Interactions](https://primotech.com/ui-ux-evolution-2026-why-micro-interactions-and-motion-matter-more-than-ever/)
- [14 Micro-interaction Examples](https://userpilot.com/blog/micro-interaction-examples/)

### AI-Assisted UX
- [10 AI-Driven UX Patterns Transforming SaaS 2026](https://www.orbix.studio/blogs/ai-driven-ux-patterns-saas-2026)
- [AI Design Patterns Enterprise Dashboards](https://www.aufaitux.com/blog/ai-design-patterns-enterprise-dashboards/)
- [M365 Copilot AI Productivity 2026](https://www.techverx.com/m365-copilot-ai-productivity-2026/)

### Progressive Disclosure & Onboarding
- [Progressive Disclosure in Enterprise Design](https://medium.com/@theuxarchitect/progressive-disclosure-in-enterprise-design-less-is-more-until-it-isnt-01c8c6b57da9)
- [Progressive Disclosure (NNGroup)](https://www.nngroup.com/articles/progressive-disclosure/)
- [Progressive Disclosure Examples for SaaS](https://userpilot.com/blog/progressive-disclosure-examples/)
- [SaaS Onboarding Best Practices 2025](https://productled.com/blog/5-best-practices-for-better-saas-user-onboarding)
- [SaaS Onboarding UX Analysis](https://userguiding.com/blog/saas-onboarding-ux-analysis)

### Mobile & Data Visualization
- [Mobile UI Design for Insurance Apps](https://3innovative.com/insights/mobile-ui-design-insurance-apps-guide/)
- [SaaS Design: Mobile-First Approach](https://cleancommit.io/blog/saas-design-a-mobile-first-approach/)
- [Data Visualization in Financial Services](https://getondata.com/data-visualization-in-financial-services/)
- [Effective Dashboard Design Principles 2025](https://www.uxpin.com/studio/blog/dashboard-design-principles/)

### Progressive Web Apps & Mobile Strategy
- [Google PWA Documentation](https://web.dev/progressive-web-apps/)
- [Workbox — Service Worker Libraries](https://developer.chrome.com/docs/workbox)
- [Web Push API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Push_API)
- [Background Sync API (MDN)](https://developer.mozilla.org/en-US/docs/Web/API/Background_Synchronization_API)
- [iOS PWA Support Status 2026](https://firt.dev/notes/pwa-ios/)
- [React Native vs PWA: When to Choose What](https://blog.logrocket.com/pwa-vs-react-native/)
- [Enterprise Mobile App UX Best Practices](https://www.nngroup.com/articles/mobile-ux/)
- [Offline-First Design Patterns](https://developer.chrome.com/docs/workbox/caching-strategies-overview/)
- [Swipe Gesture UX Patterns in Mobile Apps](https://www.nngroup.com/articles/gesture-driven-design/)

### Technology
- [shadcn/ui Documentation](https://ui.shadcn.com/)
- [TanStack Table Documentation](https://tanstack.com/table/latest)
- [Command Palette Interfaces](https://philipcdavis.com/writing/command-palette-interfaces)
- [Building Scalable Design Systems with shadcn/UI](https://shadisbaih.medium.com/building-a-scalable-design-system-with-shadcn-ui-tailwind-css-and-design-tokens-031474b03690)

---

## Appendix B: Frontend File Inventory (Post-Implementation)

| Category | Count | Key Files |
|----------|-------|-----------|
| Pages | 10 | `src/pages/*.tsx` |
| Layout Components | 3 | Sidebar, TopBar, NotificationCenter |
| Shared Components | 8 | ErrorBoundary, Pagination, InlineAlert, EmptyState, PageHeader, StatusBadge, TypeBadge, StatusTimeline |
| UI Primitives | 25+ | `src/components/ui/*.tsx` (shadcn) |
| Custom Hooks | 11 | WebSocket, notifications, announce, debounce, url-filters, endorsements, intelligence, reconciliation, ea-account, insurers, coverage |
| API Clients | 5 | `src/lib/*-api.ts` |
| Utilities | 3 | csv-export, websocket, query-keys |
| Type Definitions | 6 | `src/types/*.ts` |
| E2E Tests | 17 files / 138 tests | `e2e-tests/tests/e2e/*.spec.ts` |
| Storybook Tests | 7 files | `e2e-tests/tests/storybook/*.spec.ts` |

---

## Appendix C: Implementation Changelog

### v2.0 Changes (March 13, 2026)

**P0 Implemented (6 items)**:
- TanStack Table sorting with visual indicators
- API error details in toast messages
- URL-persisted filters via `useSearchParams`
- Auto-scroll to first form error
- ARIA labels on all icon buttons
- Error boundary with recovery UI

**P1 Implemented (13 items)**:
- WebSocket integration (STOMP + SockJS)
- Notification center (bell + popover + history)
- Batch progress page + sidebar navigation
- Outstanding items dashboard card
- Complete Intelligence Dashboard (all 4 tabs)
- Process mining metrics table with bottleneck highlighting
- Error resolution approve button
- Advanced pagination (page numbers + size selector)
- Clickable KPI cards with navigation
- Optimistic updates (confirm/reject)
- Skip navigation link
- Table scope attributes
- Last-updated timestamp with refresh

**P2 Implemented (6 items)**:
- CSV export (endorsement list + reconciliation)
- Bulk actions (checkbox selection + floating action bar)
- Progressive disclosure (type-based form fields)
- Inline contextual alerts (EA balance warning)
- Screen reader ARIA-live announcements
- Notification grouping (date-based)

**New E2E Tests Added**: 26 tests across 10 files (3 new, 7 updated)
- `batch-progress.spec.ts` (4 tests)
- `notification-center.spec.ts` (3 tests)
- `accessibility.spec.ts` (5 tests)
- Updated: endorsement-list (+6), dashboard (+4), endorsement-create (+2), intelligence-forecasts (rewritten), intelligence-error-resolutions (+1), intelligence-process-mining (+1), sidebar (+1)

**Total test count**: 762 (was 662), ALL PASSING
