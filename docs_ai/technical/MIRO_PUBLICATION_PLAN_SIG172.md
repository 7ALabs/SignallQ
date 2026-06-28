# Miro Publication Plan — SIG-172

**Date**: 27.06.2026  
**Issue**: SIG-172  
**Status**: Planning (Blocked — awaiting Miro creation)

---

## Overview

Publish architecture diagrams to Miro for team reference and onboarding. Source: ARCHITECTURE_AUDIT.md + code exploration.

---

## Diagrams to Create (7 total)

### Diagram 1: System Architecture (5 Layers)

**Source**: ARCHITECTURE_AUDIT.md, section "System Design"

**Content**:
- **Layer 1: Client Apps**
  - Android (v0.21.0, versionCode 52)
  - PWA (React/TS/Vite/Tailwind)
  - iOS (future)

- **Layer 2: Local Persistence**
  - Android: Room (linkaKotlin.db), DataStore (linkaPreferencias)
  - PWA: LocalStorage, IndexedDB

- **Layer 3: HTTP Layer**
  - Android: OkHttp + Hilt DI
  - PWA: Fetch API

- **Layer 4: Backend**
  - Cloudflare Workers (3 deployments)
    1. signallq-admin-worker (OpenAPI v2.0.0, 42+ endpoints)
    2. linka-ai-diagnosis-worker (Qwen3 30B MoE, Gemini Flash fallback)
    3. feature-flags-worker (public /flags endpoint)
  - Cloudflare D1 (SQLite) — admin sessions, users, feature flags

- **Layer 5: External Services**
  - Firebase (Analytics, Crashlytics) → BigQuery
  - Google Generative AI (fallback, free tier)
  - Google Play Console (APK distribution)

**Miro Layout**: Vertical 5-layer stack with connecting arrows, color-coded by owner

---

### Diagram 2: Data Flow — Speedtest (Measurement → Sync → Ingest)

**Source**: ARCHITECTURE_AUDIT.md + android/featureSpeedtest/, integrations/cloudflare/signallq-admin-worker

**Flow**:
1. **User triggers speedtest** → Android UI (featureSpeedtest)
2. **OkHttp measurement** → Network metrics (download, upload, latency, jitter, packet loss)
3. **Local storage** → Room DB (medicao table)
4. **Background sync** → WorkManager MonitoramentoWorker (30 min interval)
5. **Ingest API call** → POST /ingest/diagnostic (Bearer INGEST_KEY)
6. **Worker stores** → D1 diagnostic_sessions table
7. **Admin dashboard** → GET /admin/metrics/diagnostics (displays test history)

**Key Metrics**:
- Download/Upload: Mbps
- Latency: ms
- Jitter: ms
- Packet Loss: %
- Status: Excelente / Bom / Regular / Ruim / Crítico

**Miro Layout**: Left-to-right flow, swimlanes (Android | Workers | D1)

---

### Diagram 3: Data Flow — AI Diagnosis (Request → Qwen3 → Response + Caching)

**Source**: ARCHITECTURE_AUDIT.md, integrations/cloudflare/ai-diagnosis-worker

**Flow**:
1. **Client sends diagnostic request** → /api/ai/diagnostico-conexao (POST)
2. **Worker receives** → linka-ai-diagnosis-worker
3. **Prompt engineering** → SignallQ persona + diagnostic session data
4. **Model call** → Qwen3 30B MoE FP8
   - Latency: ~2-5s
   - Cost: $0.X per request (free tier Gemini fallback if Qwen fails)
5. **Parse response** → Schema v2 JSON (insights, recommendations)
6. **Cache response** → Cloudflare KV (5 min TTL, reduce API hits)
7. **Return to client** → PWA displays insights in Diagnosis UI

**Decision Tree**:
- If Qwen3 available → use Qwen3
- If Qwen3 rate-limited → fallback to Gemini Flash
- If both fail → return cached response or "unavailable" message

**Miro Layout**: Request → Processing → Decision Diamond → Response, with cost annotations

---

### Diagram 4: Data Flow — Admin Sync (Checkpoint-Based, 6h Interval, Batch 50)

**Source**: ARCHITECTURE_AUDIT.md + integrations/cloudflare/signallq-admin-worker

**Flow**:
1. **Scheduler triggers** → Every 6 hours (configurable)
2. **Checkpoint lookup** → Last synced timestamp from admin_sync_state
3. **BigQuery query** → Last 50 diagnostic_sessions + metadata
4. **Aggregate stats** → 
   - Success rate
   - Top issues
   - Network distribution
   - AI cost + reliability
5. **Update admin dashboard** → Metrics endpoints return aggregated data
6. **Update checkpoint** → Store new `last_synced` timestamp
7. **Cache results** → 5-10 min TTL (reduce BigQuery cost)

**Batch Size**: 50 records per sync (configurable)
**Frequency**: 6 hours
**Cost Impact**: ~$0.X per query (BigQuery pay-per-GB)

**Miro Layout**: Cyclical diagram (Scheduler → Checkpoint → Query → Aggregate → Update → Cache → back to Scheduler)

---

### Diagram 5: Data Flow — Background Monitoring (30 min, 3 Tasks, Hysteresis Notifications)

**Source**: ARCHITECTURE_AUDIT.md, android/app/src/.../MonitoramentoScheduler.kt

**Flow**:
1. **WorkManager schedules** → MonitoramentoWorker (30 min interval)
2. **Task 1: Check connectivity** → isConnectedToWifi(), isConnectedToCellular()
3. **Task 2: Measure network** → Quick latency + bandwidth check
4. **Task 3: Detect issues** → Heuristics (latency > 100ms, packet loss > 5%, etc.)
5. **Hysteresis logic** → Only notify if state changes (not duplicate alerts)
6. **Trigger notification** → If issues detected + significant change
7. **Log to analytics** → Firebase Analytics event
8. **Store local** → Room DB for history

**Notifications**:
- "Conectividade degradada"
- "Interferência WiFi detectada"
- "Operadora congestionada"

**Miro Layout**: Flowchart with decision diamonds (for hysteresis), icons for each task

---

### Diagram 6: Module Dependency Graph (15 modules, Android)

**Source**: ARCHITECTURE_AUDIT.md + android/{app,core,feature}/build.gradle.kts

**Modules**:
- **App** (depends on: all features + core)
- **Core (5)**:
  - coreNetwork (no deps)
  - coreDatabase (no deps, Room v12)
  - coreDatastore (no deps, DataStore)
  - coreTelephony (no deps, platform APIs)
  - corePermissions (no deps, permission logic)
- **Features (9)**:
  - featureHome, featureSpeedtest, featureWifi, featureDevices, featureDns, featureFibra, featureDiagnostico, featureHistory, featureSettings
  - Dependencies: all → multiple cores
  - Exception: featureDiagnostico → featureSpeedtest (only cross-feature)

**DAG Validation**: No cycles, unidirectional (features → cores)

**Miro Layout**: Hierarchical tree (App at top, features below, cores at bottom), color-coded (green = no deps, blue = leaf, red = any cycle)

---

### Diagram 7: API Endpoints Map (40+ endpoints, Auth + Rate Limits)

**Source**: OpenAPI v2.0.0 (signallq-admin-api.yaml), ENDPOINTS_MAPPING.md

**Grouping**:
- **/auth/** (5 endpoints, session auth, 5 attempts/15 min rate limit)
  - POST /auth/login
  - POST /auth/logout
  - GET /auth/me
  - POST /auth/create-user
  - POST /auth/change-password

- **/ingest/** (3 endpoints, Bearer INGEST_KEY, write-only scope)
  - POST /ingest/diagnostic
  - POST /ingest/ai-usage
  - POST /ingest/analytics

- **/admin/metrics/** (16+ endpoints, session auth, analytics/reporting)
  - Overview, diagnostics, ai-usage, timeline, network, etc.
  - analytics/product, analytics/battery, intelligence (consolidated v2.0.0)

- **/admin/alerts/** (2 endpoints, session auth)
  - GET /admin/alerts
  - POST /admin/alerts/{id}/resolve

- **/admin/integrations/** (9 endpoints, session auth)
  - Firebase (6): status, analytics, crashlytics, versions, crash-issues, sync
  - Google Play (1)
  - App Store (1)
  - (Future: more integrations)

- **/admin/settings** (2 endpoints, session auth)
  - GET, PUT

- **/admin/feature-flags/** (3 endpoints, session auth)
  - GET, POST, PUT

- **/health** (1 endpoint, Bearer ADMIN_SECRET, legacy)

**Auth Schemes**:
- cookieAuth (httpOnly, Secure, 7d TTL)
- bearerAuth (INGEST_KEY, write-only)
- adminSecretAuth (legacy, /health only)

**Miro Layout**: Organized by endpoint group, with security badge per scheme, example request/response snippets

---

## Miro Board Setup

**Board Name**: SignallQ Architecture v2.0.0 (27.06.2026)

**Pages/Sections**:
1. System Architecture (Diagram 1)
2. Data Flows (Diagrams 2-5)
3. Module Graph (Diagram 6)
4. API Map (Diagram 7)
5. Notes & Links
   - Link to ARCHITECTURE_AUDIT.md
   - Link to OpenAPI v2.0.0 YAML
   - Link to Notion Architecture page
   - Git commit hashes for reference

---

## Publishing Checklist

- [ ] Create Miro board "SignallQ Architecture v2.0.0"
- [ ] Create 7 diagrams (System, 4 Data Flows, Modules, APIs)
- [ ] Add color-coding and legend
- [ ] Add example annotations (latency, cost, TTL)
- [ ] Link to Notion Architecture page
- [ ] Share with team (view-only for non-architects)
- [ ] Update Notion with link to Miro board
- [ ] Archive old/outdated diagrams (if any)

---

## Effort Estimate

- Create 7 diagrams: 1-2 hours (per diagram: ~15-20 min)
- Add annotations + polish: 30 min
- Share + link setup: 15 min
- **Total**: ~2-2.5 hours (5 pts estimate reasonable)

---

## Dependencies

- Miro board access (requires MCP integration or manual creation)
- Notion Architecture page ready for link
- Team access for review

---

## Timeline

- **Priority**: Low (Post-M1, not blocking launch)
- **Due**: After M1 stabilization
- **Owner**: Design/Architecture team or Claude Code agent (with Miro access)

---

**Status**: Ready for implementation (awaiting Miro access)
