# API Path Consolidation Plan — SIG-168

**Date**: 27.06.2026  
**Issue**: SIG-168  
**Recommendation**: Option A — Standardize on `/admin/metrics/*` for all analytics/reporting

---

## Summary

Consolidate fragmented `/admin/analytics/*` and `/admin/diagnostics/intelligence` endpoints under a unified `/admin/metrics/*` namespace for cleaner API organization.

---

## Current State (Implementation Mismatch)

### OpenAPI v2.0.0 Documents
- `/admin/metrics/*` — 14 endpoints (overview, diagnostics, ai-usage, timeline, network, etc.)
- `/admin/analytics/*` — 2 endpoints (product, battery)
- `/admin/diagnostics/intelligence` — 1 endpoint

### Worker Implementation (index.ts)
**Lines 1799–1834**:
- `/admin/metrics/*` routes — 12+ endpoints
- `/admin/analytics/product` — handler: handleProductAnalytics
- `/admin/analytics/battery` — handler: handleBatteryAnalytics  
- `/admin/diagnostics/intelligence` — handler: handleDiagnosticsIntelligence
- `/admin/alerts` — 2 endpoints (GET, POST resolve) — kept separate (business logic)

### Admin Dashboard Client
**SignallQ Admin/src/api.ts** — Likely calls:
- `GET /admin/analytics/product`
- `GET /admin/analytics/battery`
- `GET /admin/diagnostics/intelligence`

---

## Solution: Option A (Consolidate to `/admin/metrics/*`)

### Changes Required

#### Phase 1: Worker Routes (index.ts)

**Add new routes** (lines 1814–1815):
```typescript
// Analytics endpoints consolidated under /admin/metrics/
{ method: "GET", pattern: /^\/admin\/metrics\/analytics\/product$/, handler: withErrorLogging('analytics', handleProductAnalytics) },
{ method: "GET", pattern: /^\/admin\/metrics\/analytics\/battery$/, handler: withErrorLogging('analytics', handleBatteryAnalytics) },
// Diagnostics consolidated under /admin/metrics/
{ method: "GET", pattern: /^\/admin\/metrics\/intelligence$/, handler: withErrorLogging('metrics', handleDiagnosticsIntelligence) },
```

**Keep legacy routes for backward compatibility** (deprecated):
```typescript
// Deprecated: maintained for 2 versions, then remove
{ method: "GET", pattern: /^\/admin\/analytics\/product$/, handler: withErrorLogging('analytics', handleProductAnalytics) },
{ method: "GET", pattern: /^\/admin\/analytics\/battery$/, handler: withErrorLogging('analytics', handleBatteryAnalytics) },
{ method: "GET", pattern: /^\/admin\/diagnostics\/intelligence$/, handler: withErrorLogging('metrics', handleDiagnosticsIntelligence) },
```

#### Phase 2: OpenAPI v2.0.0

**Update paths**:
```yaml
# OLD → NEW
/admin/analytics/product → /admin/metrics/analytics/product
/admin/analytics/battery → /admin/metrics/analytics/battery
/admin/diagnostics/intelligence → /admin/metrics/intelligence

# Keep in x-changelog:
Deprecated (v2.0.0): /admin/analytics/* and /admin/diagnostics/intelligence
  → Use /admin/metrics/analytics/* and /admin/metrics/intelligence
  → Legacy paths will be removed in v3.0.0
```

**x-path-structure** (update):
```yaml
x-path-structure: |
  ## API Structure (v2.0.0)

  ### Authentication Routes
  - /auth/* — User authentication (login, logout, me, create-user, change-password)

  ### Data Ingestion Routes (App → Backend)
  - /ingest/* — App measurements (diagnostic, ai-usage, analytics)

  ### Admin Dashboard Routes (Dashboard → Backend)
  - /admin/metrics/* — Analytics and reporting
    - /admin/metrics/overview, diagnostics, ai-usage, timeline, network, alerts, etc.
    - /admin/metrics/analytics/* — Product engagement, battery (consolidated v2.0.0)
    - /admin/metrics/intelligence — AI insights (consolidated v2.0.0)
  - /admin/alerts/* — Alert management (separate: business logic)
  - /admin/integrations/* — External service status (separate: business logic)
  - /admin/settings — Configuration (separate: business logic)
  - /admin/feature-flags/* — Feature flags (separate: business logic)
  - /admin/diagnostics/* — DEPRECATED, use /admin/metrics/intelligence

  ### Health Check
  - /health — Service health check
```

#### Phase 3: Admin Dashboard Client

**Update SignallQ Admin/src/api.ts** (example):
```typescript
// OLD
const productAnalytics = await fetch('/admin/analytics/product')
const batteryAnalytics = await fetch('/admin/analytics/battery')
const diagnosticsIntelligence = await fetch('/admin/diagnostics/intelligence')

// NEW
const productAnalytics = await fetch('/admin/metrics/analytics/product')
const batteryAnalytics = await fetch('/admin/metrics/analytics/battery')
const diagnosticsIntelligence = await fetch('/admin/metrics/intelligence')
```

---

## Implementation Checklist

### Worker Routes
- [ ] Add 3 new routes to ROUTES array (lines ~1814–1815)
  - `/admin/metrics/analytics/product`
  - `/admin/metrics/analytics/battery`
  - `/admin/metrics/intelligence`
- [ ] Keep legacy routes (backward compatibility, 2 versions)
  - `/admin/analytics/product` (deprecated)
  - `/admin/analytics/battery` (deprecated)
  - `/admin/diagnostics/intelligence` (deprecated)
- [ ] Add deprecation warning in response headers (optional):
  - `Deprecation: true`
  - `Sunset: 2026-12-31T00:00:00Z` (2 minor versions = ~6 months)

### OpenAPI v2.0.0
- [ ] Add 3 new paths in `/paths` section
- [ ] Add deprecation note in old paths (if kept)
- [ ] Update x-path-structure documentation
- [ ] Update x-changelog with v2.0.0 note

### Admin Dashboard Client
- [ ] Update api.ts: 3 endpoint calls
- [ ] Test in staging environment
- [ ] Verify dashboard metrics load correctly

---

## Testing Plan

### Manual Testing
1. **GET /admin/metrics/analytics/product** — product engagement metrics
2. **GET /admin/metrics/analytics/battery** — battery drain analytics
3. **GET /admin/metrics/intelligence** — AI-powered insights

### Backward Compatibility Testing
1. **GET /admin/analytics/product** (legacy) — should still work
2. **GET /admin/analytics/battery** (legacy) — should still work
3. **GET /admin/diagnostics/intelligence** (legacy) — should still work

### Admin Dashboard
1. Analytics page loads without 404 errors
2. Product engagement data displays
3. Battery analytics displays
4. AI intelligence section displays

---

## Impact Analysis

### Breaking Changes
- None (backward compatibility maintained)

### Non-Breaking Changes
- 3 new preferred paths (v2.0.0+)
- 3 old paths deprecated but functional

### Client Compatibility
- **Required**: Admin Dashboard client must update api.ts calls (or support both old/new)
- **Optional**: Mobile/Web apps consume /ingest/* (unaffected)

---

## Rollback Plan

If issues arise:
1. Revert ROUTES array to remove new paths
2. Revert OpenAPI changes
3. Keep legacy paths in Worker indefinitely
4. No database or data migration needed (pure routing change)

---

## Estimate

- Phase 1 (Worker routes): 15 min
- Phase 2 (OpenAPI): 20 min
- Phase 3 (Admin client): 20 min
- Testing: 30 min
- **Total**: ~1.5 hours (SIG-168 is 6 pts, conservative estimate)

---

**Status**: Plan ready for implementation  
**Blocker**: None  
**Next**: Execute Phase 1-3 in sequence
