# SignallQ API Endpoints — Complete Mapping

**Date**: 27.06.2026  
**Issue**: SIG-166  
**Source**: integrations/cloudflare/signallq-admin-worker/src/index.ts

---

## Summary

- **Documented** (OpenAPI): 25 endpoints
- **Implemented** (Workers): 42+ endpoints  
- **Gap**: 17+ endpoints missing from OpenAPI

---

## Complete Endpoint List (42+)

### Authentication (`/auth/*`) — 5 endpoints

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| POST | /auth/login | Login with email + password | None (rate-limited) | ✅ SIG-167 |
| POST | /auth/logout | Logout and revoke session | session | ✅ SIG-167 |
| GET | /auth/me | Get current user | session | ✅ SIG-167 |
| POST | /auth/create-user | Create new admin user (admin-only) | session | ✅ SIG-167 |
| POST | /auth/change-password | Change password for current user | session | ✅ SIG-167 |

### Ingest (`/ingest/*`) — 3 endpoints

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| POST | /ingest/diagnostic | Ingest diagnostic measurements | Bearer INGEST_KEY | ✅ SIG-167 |
| POST | /ingest/ai-usage | Ingest AI usage metrics | Bearer INGEST_KEY | ✅ SIG-167 |
| POST | /ingest/analytics | Ingest analytics events | Bearer INGEST_KEY | ❌ **Missing** |

### Admin Metrics (`/admin/metrics/*`) — 12 endpoints

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /admin/metrics/overview | Dashboard overview stats | session | ✅ Partial |
| GET | /admin/metrics/diagnostics | Diagnostics history + summaries | session | ✅ Partial |
| GET | /admin/metrics/diagnostics/summary | Diagnostic aggregations | session | ❌ **Missing** |
| GET | /admin/metrics/network | Network/WiFi analytics | session | ✅ Partial |
| GET | /admin/metrics/operators | Telecom operator performance | session | ✅ Partial |
| GET | /admin/metrics/ai-usage | AI token usage + costs | session | ✅ Partial |
| GET | /admin/metrics/ai-costs | AI costs by model + reliability | session | ❌ **Missing** |
| GET | /admin/metrics/ai-providers | AI providers breakdown | session | ❌ **Missing** |
| GET | /admin/metrics/ai-usage/timeline | AI usage timeline (per day) | session | ❌ **Missing** |
| GET | /admin/metrics/timeline | Test timeline by day | session | ❌ **Missing** |
| GET | /admin/metrics/alerts | Recent alerts (critical issues) | session | ❌ **Missing** |
| GET | /admin/metrics/top-issues | Top 10 most common issues | session | ❌ **Missing** |
| GET | /admin/metrics/errors | Error logs from Worker + SDK | session | ✅ Partial |
| GET | /admin/metrics/app-versions | App versions distribution | session | ✅ Partial |

### Admin Diagnostics (`/admin/diagnostics/*`) — 1 endpoint

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /admin/diagnostics/intelligence | AI-powered diagnostic insights | session | ❌ **Missing** |

### Admin Analytics (`/admin/analytics/*`) — 2 endpoints

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /admin/analytics/product | Product engagement analytics | session | ❌ **Missing** |
| GET | /admin/analytics/battery | Battery drain + device analytics | session | ❌ **Missing** |

### Admin Alerts (`/admin/alerts/*`) — 2 endpoints

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /admin/alerts | Get all alerts (paginated) | session | ❌ **Missing** |
| POST | /admin/alerts/:id/resolve | Mark alert as resolved | session | ❌ **Missing** |

### Admin Integrations (`/admin/integrations/*`) — 6 endpoints

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /admin/integrations/firebase | Firebase connection status | session | ✅ Partial |
| GET | /admin/integrations/firebase/analytics | Firebase GA4 analytics | session | ❌ **Missing** |
| GET | /admin/integrations/firebase/status | Firebase health check | session | ❌ **Missing** |
| GET | /admin/integrations/firebase/crashlytics | Crashlytics error summary | session | ❌ **Missing** |
| GET | /admin/integrations/firebase/versions | App version stats from GA4 | session | ❌ **Missing** |
| GET | /admin/integrations/firebase/crash-issues | Crashlytics grouped issues | session | ❌ **Missing** |
| POST | /admin/integrations/firebase/sync | Manual Firebase sync (trigger) | session | ❌ **Missing** |
| GET | /admin/integrations/google-play | Google Play auth status | session | ✅ Partial |
| GET | /admin/integrations/app-store | App Store Connect (future) | session | ✅ Partial |

### Admin Settings (`/admin/settings*`) — 1 endpoint

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /admin/settings | Get configuration | session | ✅ Partial |
| PUT | /admin/settings | Update configuration | session | ❌ **Missing** |

### Admin Feature Flags (`/admin/feature-flags/*`) — 2 endpoints

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /admin/feature-flags | List all feature flags | session | ❌ **Missing** |
| PUT | /admin/feature-flags/:flag | Update feature flag state | session | ❌ **Missing** |

### Health (`/health`) — 1 endpoint

| Method | Path | Summary | Auth | Documented |
|--------|------|---------|------|------------|
| GET | /health | Worker heartbeat | Bearer ADMIN_SECRET (legacy) | ❌ **Missing** |

---

## Path Naming Inconsistencies

### Current Implementation Mix
- `/admin/metrics/*` — Some documented as `/admin/metrics/`
- `/admin/alerts` vs `/admin/metrics/alerts` — Inconsistent nesting
- `/admin/analytics/*` — Not in OpenAPI at all
- `/admin/integrations/firebase/*` — Nested too deep (documented as `/admin/integrations/firebase` only)

### Recommendation: Consolidate to `/admin/metrics/*`

**Rationale**:
- All analytics/reporting go under `/admin/metrics/`
- Business logic under `/admin/` (alerts, settings, feature-flags, integrations)
- Cleaner API surface

**New Structure**:
```
/admin/metrics/*           — all analytics/reporting
/admin/alerts/*            — alert management  
/admin/analytics/*         — product/device analytics (keep separate from metrics)
/admin/settings            — configuration
/admin/integrations/*      — external service status
/admin/feature-flags/*     — feature toggles
/admin/diagnostics/*       — intelligence/insights
/auth/*                    — authentication
/ingest/*                  — app data ingestion
/health                    — health check
```

---

## Additions to OpenAPI (Next Steps — SIG-166)

### Missing Endpoints (High Priority)

1. **POST /ingest/analytics** — App analytics events
2. **GET /admin/metrics/diagnostics/summary** — Agg summaries
3. **GET /admin/metrics/ai-costs** — AI cost breakdown
4. **GET /admin/metrics/ai-providers** — Provider stats
5. **GET /admin/metrics/ai-usage/timeline** — AI usage per day
6. **GET /admin/metrics/timeline** — Test timeline
7. **GET /admin/metrics/alerts** — Alert list
8. **GET /admin/metrics/top-issues** — Top issues
9. **GET /admin/diagnostics/intelligence** — AI insights
10. **GET /admin/analytics/product** — Product engagement
11. **GET /admin/analytics/battery** — Battery analytics
12. **GET /admin/alerts** — All alerts
13. **POST /admin/alerts/:id/resolve** — Resolve alert
14. **GET /admin/integrations/firebase/analytics** — GA4 data
15. **GET /admin/integrations/firebase/crashlytics** — Crash logs
16. **GET /admin/integrations/firebase/versions** — Version stats
17. **GET /admin/integrations/firebase/crash-issues** — Grouped issues
18. **POST /admin/integrations/firebase/sync** — Trigger sync
19. **PUT /admin/settings** — Update settings
20. **GET /admin/feature-flags** — List flags
21. **PUT /admin/feature-flags/:flag** — Update flag
22. **GET /health** — Health check

### Missing from Documentation

Also missing: Request/response schemas, query parameters, examples for all existing endpoints.

---

## Implementation Plan (SIG-166)

### Phase 1: Add Missing Endpoints to OpenAPI
- [ ] Add 22 missing endpoints (list above)
- [ ] Add request/response schemas
- [ ] Add query parameters (period, environment, limit, etc.)
- [ ] Add operationIds and tags

### Phase 2: Consolidate Path Naming
- [ ] Decide on final path structure (recommend: everything under /admin/metrics/*)
- [ ] Update Worker routes if consolidating
- [ ] Update OpenAPI to match

### Phase 3: Complete Schemas
- [ ] Fill in all response schemas
- [ ] Add examples for each endpoint
- [ ] Add error cases (400, 401, 403, 429)

### Phase 4: Validation & Testing
- [ ] Run OpenAPI validator
- [ ] Test with swagger-ui
- [ ] Test with Admin Dashboard client

---

## Files to Update

- `SignallQ Admin/docs/openapi/signallq-admin-api.yaml` — Add endpoints, schemas, examples
- `integrations/cloudflare/signallq-admin-worker/src/index.ts` — Consolidate paths (optional, if standardizing)

---

## Estimate (SIG-166)

- Phase 1: 4-6 hours (endpoint scaffolding)
- Phase 2: 2-3 hours (path consolidation)
- Phase 3: 3-4 hours (schemas + examples)
- Phase 4: 1-2 hours (validation + testing)
- **Total**: 10-15 hours (~2 sprints, 8 pts estimate is conservative)

---

**Status**: Ready for implementation  
**Blocker**: None  
**Next**: Execute Phase 1 (add endpoints)
