# Firebase Integration Endpoints — OpenAPI Documentation

**Created**: 27.06.2026  
**Issue**: SIG-169  
**Based on**: integrations/cloudflare/signallq-admin-worker/src/index.ts (lines 1080–1276)

---

## Overview

SignallQ Admin integrates with **Firebase** (Google Cloud) to sync app analytics and crash logs. The integration uses:

1. **GA4 (Google Analytics 4)** — User events, sessions, crash-affected users
2. **Crashlytics** — Crash summaries, error tracking, app version stats
3. **BigQuery** — Real-time exports of GA4 and Crashlytics data (via Firebase Project)

**Authentication**: Service account (FIREBASE_CLIENT_EMAIL, FIREBASE_PRIVATE_KEY) → JWT token → OAuth2 → BigQuery API + Analytics Reporting API

---

## Endpoints (6)

### 1. GET /admin/integrations/firebase/status

**Summary**: Firebase connection status and credentials check

**Security**: Requires session cookie (`cookieAuth`)

**Response Schema**:
```json
{
  "source": "worker",
  "projectId": "signallq-analytics-prod",
  "status": "connected",
  "hasCredentials": true,
  "ga4PropertyConfigured": true
}
```

**Fields**:
- `source` (string): Always "worker" — response from Worker, not external API
- `projectId` (string): Firebase Project ID from env
- `status` (string): Always "connected" if credentials present
- `hasCredentials` (boolean): Whether FIREBASE_CLIENT_EMAIL and FIREBASE_PRIVATE_KEY are configured
- `ga4PropertyConfigured` (boolean): Whether FIREBASE_GA4_PROPERTY_ID is set

**Status Codes**:
- `200 OK`: Always returns successfully (no external call)
- `401 Unauthorized`: Missing session cookie

---

### 2. GET /admin/integrations/firebase/analytics

**Summary**: Firebase GA4 analytics — active users, sessions, crash-affected users (7d)

**Security**: Requires session cookie (`cookieAuth`)

**Query Parameters**: None

**Response Schema**:
```json
{
  "source": "firebase_analytics",
  "data": {
    "rows": [
      {
        "dimensions": ["2026-06-21"],
        "metricValues": [
          { "value": "1234" },
          { "value": "5678" },
          { "value": "89" }
        ]
      }
    ]
  }
}
```

**Error Responses**:
- Missing credentials → `200` with `{ "source": "no_credentials", "activeUsersToday": 0, "sessionsToday": 0 }`
- Missing GA4 property → `200` with `{ "source": "no_ga4_property_id", "message": "Configure FIREBASE_GA4_PROPERTY_ID..." }`
- API error → `500` with `{ "source": "error", "message": "..." }`

**Raw Data Format** (Analytics Reporting API v1beta):
```json
{
  "dateRanges": [{ "startDate": "7daysAgo", "endDate": "today" }],
  "metrics": [
    { "name": "activeUsers" },
    { "name": "sessions" },
    { "name": "crashAffectedUsers" }
  ],
  "dimensions": [{ "name": "date" }]
}
```

**Backend Implementation**:
- Calls Google Analytics Reporting API: `https://analyticsdata.googleapis.com/v1beta/properties/{GA4_PROPERTY_ID}:runReport`
- Metric order: activeUsers, sessions, crashAffectedUsers (7-day window)

---

### 3. GET /admin/integrations/firebase/crashlytics

**Summary**: Crashlytics error summary — total crashes, affected users, crash-free rate (7d)

**Security**: Requires session cookie (`cookieAuth`)

**Response Schema**:
```json
{
  "source": "bigquery",
  "unresolvedCrashes": 42,
  "affectedUsers": 12,
  "crashFreeUsersPercentage": 98.5
}
```

**Fields**:
- `source` (string): "bigquery", "no_data_yet", "no_credentials", or "error"
- `unresolvedCrashes` (integer): Count of distinct crashes in last 7 days
- `affectedUsers` (integer): Count of distinct installation_uuid affected by crashes
- `crashFreeUsersPercentage` (number): Percentage calculated as (total_users - affected_users) / total_users * 100

**Error Responses**:
- No data yet (first 24h) → `200` with `{ "source": "no_data_yet", "message": "BigQuery export...", "unresolvedCrashes": 0, "crashFreeUsersPercentage": 100 }`
- BigQuery query error → `200` with `{ "source": "error", "message": "...", "unresolvedCrashes": 0, "crashFreeUsersPercentage": 100 }`
- No credentials → `200` with `{ "source": "no_credentials", "unresolvedCrashes": 0, "crashFreeUsersPercentage": 100 }`

**BigQuery Query**:
```sql
SELECT
  COUNT(*)                                   AS total_crashes,
  COUNT(DISTINCT installation_uuid)           AS affected_users,
  (SELECT COUNT(DISTINCT installation_uuid)
   FROM `{PROJECT}.analytics_{GA4_PROPERTY}.events_*`
   WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
     AND event_name = 'app_session_start'
  )                                          AS total_users
FROM `{PROJECT}.firebase_crashlytics.android_crashes_*`
WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 7 DAY))
```

---

### 4. GET /admin/integrations/firebase/versions

**Summary**: App version distribution — crashes and affected users per version (30d, top 10)

**Security**: Requires session cookie (`cookieAuth`)

**Response Schema**:
```json
{
  "source": "bigquery",
  "versions": [
    {
      "version": "0.21.0",
      "totalCrashes": 42,
      "affectedUsers": 8
    },
    {
      "version": "0.20.5",
      "totalCrashes": 12,
      "affectedUsers": 3
    }
  ]
}
```

**Fields** (array):
- `version` (string): App version string (e.g., "0.21.0")
- `totalCrashes` (integer): Crash events in version over 30 days
- `affectedUsers` (integer): Unique installation_uuid affected in version

**Error Responses**:
- No data yet → `200` with `{ "source": "no_data_yet", "versions": [] }`
- BigQuery error → `200` with `{ "source": "error", "versions": [] }`
- No credentials → `200` with `{ "source": "no_credentials", "versions": [] }`

**BigQuery Query**:
```sql
SELECT
  app_version,
  COUNT(*)                          AS total_crashes,
  COUNT(DISTINCT installation_uuid) AS affected_users
FROM `{PROJECT}.firebase_crashlytics.android_crashes_*`
WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))
GROUP BY app_version
ORDER BY total_crashes DESC
LIMIT 10
```

---

### 5. GET /admin/integrations/firebase/crash-issues

**Summary**: Crashlytics grouped issues — top crashes ranked by frequency (30d, top 20)

**Security**: Requires session cookie (`cookieAuth`)

**Response Schema**:
```json
{
  "source": "bigquery",
  "issues": [
    {
      "id": "NullPointerException",
      "title": "NullPointerException in DiagnosticViewModel",
      "totalCrashes": 156,
      "affectedUsers": 42,
      "lastSeen": 1719457200
    },
    {
      "id": "ArrayIndexOutOfBoundsException",
      "title": "ArrayIndexOutOfBoundsException in SpeedtestTask",
      "totalCrashes": 89,
      "affectedUsers": 18,
      "lastSeen": 1719453600
    }
  ]
}
```

**Fields** (array):
- `id` (string): Issue ID from Crashlytics
- `title` (string): Exception message / issue title
- `totalCrashes` (integer): Crash count for this issue over 30 days
- `affectedUsers` (integer): Unique installation_uuid affected
- `lastSeen` (integer, unix timestamp): Most recent crash in seconds

**Error Responses**:
- No data yet → `200` with `{ "source": "no_data_yet", "issues": [] }`
- BigQuery error → `200` with `{ "source": "error", "issues": [] }`
- No credentials → `200` with `{ "source": "no_credentials", "issues": [] }`

**BigQuery Query**:
```sql
SELECT
  issue_id,
  issue_title,
  COUNT(*)                              AS total_crashes,
  COUNT(DISTINCT installation_uuid)     AS affected_users,
  MAX(event_timestamp)                  AS last_seen
FROM `{PROJECT}.firebase_crashlytics.android_crashes_*`
WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 30 DAY))
GROUP BY issue_id, issue_title
ORDER BY total_crashes DESC
LIMIT 20
```

---

### 6. POST /admin/integrations/firebase/sync

**Summary**: Manually trigger Firebase data sync (BigQuery query)

**Security**: Requires session cookie (`cookieAuth`)

**Request Body**: None (empty body)

**Response Schema**:
```json
{
  "ok": true,
  "source": "bigquery",
  "sessionsYesterday": 5432,
  "syncedAt": 1719457200
}
```

**Fields**:
- `ok` (boolean): Whether sync was successful
- `source` (string): "bigquery", "no_data_yet", "no_credentials", or "error"
- `sessionsYesterday` (integer): Session count from yesterday (GA4)
- `syncedAt` (integer, unix timestamp): Sync timestamp in seconds

**Error Responses**:
- No data yet → `200` with `{ "ok": false, "source": "no_data_yet", "sessionsYesterday": 0, "syncedAt": ... }`
- BigQuery error → `200` with `{ "ok": false, "source": "error", "message": "...", "syncedAt": ... }`
- No credentials → `200` with `{ "ok": false, "source": "no_credentials", "syncedAt": ... }`

**BigQuery Query**:
```sql
SELECT COUNT(*) AS sessions
FROM `{PROJECT}.analytics_{GA4_PROPERTY}.events_*`
WHERE _TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', DATE_SUB(CURRENT_DATE(), INTERVAL 1 DAY))
  AND event_name = 'app_session_start'
```

**Note**: Fire-and-forget — returns immediately after query, does not wait for BigQuery result storage.

---

## BigQuery Tables (Firebase Project)

| Table | Dataset | Purpose |
|-------|---------|---------|
| `firebase_crashlytics.android_crashes_*` | Crashlytics | Crash events (partitioned by date, _TABLE_SUFFIX) |
| `analytics_{GA4_PROPERTY}.events_*` | GA4 | User events (partitioned by date, _TABLE_SUFFIX) |

**Partitioning**: Date-based (`_TABLE_SUFFIX >= FORMAT_DATE('%Y%m%d', ...)`)  
**Retention**: 13 months (standard Firebase export policy)

---

## Authentication Flow

### Service Account JWT → OAuth2 → BigQuery API

1. **Load Private Key**:
   - Source: `FIREBASE_PRIVATE_KEY` (from wrangler.toml secrets)
   - Format: PKCS#8 PEM with `-----BEGIN PRIVATE KEY-----` / `-----END PRIVATE KEY-----`
   - Parse: Remove header/footer, base64-decode, import into WebCrypto

2. **Create JWT Payload**:
   ```json
   {
     "iss": "service-account-email@project.iam.gserviceaccount.com",
     "sub": "service-account-email@project.iam.gserviceaccount.com",
     "aud": "https://oauth2.googleapis.com/token",
     "iat": 1719457200,
     "exp": 1719460800,
     "scope": [
       "https://www.googleapis.com/auth/firebase",
       "https://www.googleapis.com/auth/analytics.readonly",
       "https://www.googleapis.com/auth/cloud-platform"
     ]
   }
   ```

3. **Sign with RSA-SHA256**:
   - Algorithm: RS256 (RSASSA-PKCS1-v1_5 with SHA-256)
   - Output: JWS (JWT token)

4. **Exchange for Access Token**:
   - POST to `https://oauth2.googleapis.com/token`
   - Grant: `urn:ietf:params:oauth:grant-type:jwt-bearer`
   - Returns: `{ "access_token": "...", "expires_in": 3600, "token_type": "Bearer" }`

5. **Use Access Token**:
   - BigQuery API: `Authorization: Bearer {access_token}`
   - Analytics Reporting API: `Authorization: Bearer {access_token}`
   - TTL: 1 hour (iat to exp)

---

## Rate Limiting & Caching

**No explicit rate limits** on these endpoints — limited by:
- BigQuery pricing (~$6.25/TB scanned)
- OAuth2 token generation cost (1 per request, caching would improve)
- Worker request timeout (30s default)

**Recommended future improvements**:
- Cache BigQuery results for 5-10 minutes (query expensive, infrequent changes)
- Cache OAuth2 access tokens (1 hour lifetime, reuse across requests)
- Add query timeout validation (10s hardcoded in queryBigQuery)

---

## Implementation Checklist

- [x] GET /admin/integrations/firebase/status — health check
- [x] GET /admin/integrations/firebase/analytics — GA4 user metrics
- [x] GET /admin/integrations/firebase/crashlytics — crash summary
- [x] GET /admin/integrations/firebase/versions — version distribution
- [x] GET /admin/integrations/firebase/crash-issues — grouped issues
- [x] POST /admin/integrations/firebase/sync — manual sync trigger
- [x] Service account auth (JWT + OAuth2)
- [x] BigQuery queries with date-partitioned tables
- [x] Error handling (no_credentials, table_not_found, auth_failed)
- [x] Response normalization (always 200, error in `source` field)

---

## Next Steps (SIG-169)

1. Update OpenAPI v2.0.0 with complete schemas for all 6 endpoints
2. Add example responses for:
   - `/admin/integrations/firebase/analytics`
   - `/admin/integrations/firebase/crashlytics`
   - `/admin/integrations/firebase/crash-issues`
3. Document BigQuery query patterns and performance notes
4. Add rate limiting recommendations to OpenAPI
5. Validate with Admin Dashboard client (test live sync)

---

**Status**: Documentation complete, ready for OpenAPI integration (SIG-169 Phase 2)
