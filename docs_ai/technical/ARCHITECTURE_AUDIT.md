# Auditoria de Arquitetura SignallQ — 27.06.2026

## Resumo Executivo

Auditoria completa da arquitetura SignallQ (v0.21.0) baseada em:
- Exploração de código fonte (Android, PWA, Workers)
- Análise de fluxos de dados
- Revisão de especificações OpenAPI
- Comparação com documentação existente

**Resultado**: 8/10 claims documentadas estão corretas. 2 divergências encontradas e corrigidas.

---

## Achados por Categoria

### ✅ CORRETO (8 itens)

| # | Claim | Onde | Status |
|---|-------|------|--------|
| 1 | 15 módulos Gradle (app + 5 core + 9 feature) | android/{app,core,feature}/build.gradle.kts | Exato |
| 2 | 5 abas (Início, Velocidade, Sinal, Histórico, Ajustes) | android/app/src/main/kotlin/.../AppShell.kt:741-745 | Exato |
| 3 | Qwen3 30B MoE FP8 como AI | integrations/cloudflare/ai-diagnosis-worker/src/providers.ts:189-190 | Exato |
| 4 | linkaKotlin.db (nome Room DB) | android/core/database/src/.../CoreDatabaseModulo.kt:162 | Exato |
| 5 | StateFlow + Hilt DI | android/**/*.kt (imports, modules) | Exato |
| 6 | featureDiagnostico/Dispositivos como overlays | android/app/src/.../AppShell.kt:587-659 (AnimatedVisibility) | Exato |
| 7 | WorkManager MonitoramentoWorker 30 min | android/app/src/.../MonitoramentoScheduler.kt:24 | Exato |
| 9 | Cloudflare Pages host | pwa/package.json, wrangler.toml, .wrangler/ | Exato |

### ❌ DIVERGÊNCIAS (2 itens)

#### #8: PWA Stack

**Documentado**: "PWA: React + TypeScript + Material 3"

**Realidade**:
- ✅ Vite 6.2.3 (pwa/package.json:21)
- ✅ React 19.0.1 (pwa/package.json:19)
- ✅ TypeScript 5.8.2 (pwa/package.json:29)
- ❌ **Tailwind CSS 4.1.14** (pwa/package.json:16, 28), não Material 3
- Design system customizado em `pwa/src/design-system/` com tokens CSS próprios
- Inspirado em Material 3 (cores, principios), mas implementação independente

**Correção**: README.md e CLAUDE.md atualizados para mencionar Tailwind CSS.

#### #10: Firebase Stack

**Documentado**: "Firebase Analytics, Crashlytics, Realtime DB"

**Realidade**:
- ✅ Firebase Analytics (android/gradle/libs.versions.toml:71, app build.gradle.kts:289)
- ✅ Firebase Crashlytics (android/gradle/libs.versions.toml:70, app build.gradle.kts:288)
- ❌ **Realtime DB NÃO é usado**
  - Nenhuma dependency em libs.versions.toml
  - Nenhum import em kotlin files
  - Persistência local: Room (SQLite) + DataStore
  - Back-end IA: Cloudflare Workers + Google Generative AI (fallback)

**Correção**: Removido "Realtime DB" de README.md e CLAUDE.md.

---

## Análise de Código Estrutura

### Android (15 módulos, confirmado)

**Core (5)**: Fundacional, sem dependências cruzadas entre cores
- coreNetwork: OkHttp, coroutines
- coreDatabase: Room v12 (4 entities: medicao, apelido_dispositivo, chat_sessions, chat_messages)
- coreDatastore: DataStore preferences (linkaPreferencias)
- coreTelephony: RSRP, RSRQ, SINR metrics
- corePermissions: validation logic

**Features (9)**: Library modules, dependências unidirecionais (features → core)
- Exceção: featureDiagnostico → featureSpeedtest (única dependência cross-feature)
- Sem ciclos: DAG enforced por Gradle

### PWA (React + TypeScript + Vite + Tailwind)

**Pages/Features**:
- diagnosis/ — AI diagnosis display
- speedtest/ — Speed test runner + quality classification
- history/ — Past test results
- report/ — Report generation/viewing

**State Management**:
- React local state per feature
- No centralized store (Redux/Zustand)
- Custom hook: useConnectionSnapshot.ts
- API client: api.ts (calls both AI Worker and backends)

**Design System**: Material 3 tokens em CSS (colors_and_type.css), components em design-system/

### Cloudflare Workers (3 deployments)

1. **linka-ai-diagnosis-worker**
   - POST /api/ai/diagnostico-conexao
   - Qwen3 30B MoE FP8 (fallback Gemini Flash)
   - Schema v2 output

2. **signallq-admin-worker**
   - POST /ingest/* (diagnostic + ai-usage)
   - GET /admin/* (login, analytics, metrics)
   - D1 database (7 tables)
   - Session auth + rate limiting

3. **signallq-privacy-worker**
   - Static privacy policy HTML

---

## Fluxos de Dados

### 1. Speedtest
- FeatureSpeedtest → ExecutorSpeedtest → MedicaoEntity (Room)
- Sync via AdminSyncWorker (6h interval) → /ingest/diagnostic
- Sem envio direto remoto (fire-and-forget design)

### 2. AI Diagnosis
- FeatureDiagnostico → POST /api/ai/diagnostico-conexao
- Payload: 20+ campos (métricas, contexto rede, móvel, histórico)
- Response: laudo_ia (texto), status, impactos, recomendações, classificações técnicas
- Caching: 5 min TTL, chave SHA-256(schema + context)
- Timeout: 90s read (modelo inference), 40s request

### 3. Admin Sync
- AdminSyncWorker: 6h interval + exponential backoff
- Auth: Bearer INGEST_KEY (write-only scope)
- Batch: 50 records per request
- Filtering: non-contaminated measurements only

### 4. Background Monitoring
- MonitoramentoWorker: 30 min interval
- Tasks: HTTP latency (3 samples), DNS resolution, WiFi RSSI
- Notifications: hysteresis-based (latency >400ms, DNS >2500ms, RSSI <-75dBm)

---

## Análise de APIs

### OpenAPI vs Implementação

**Documentado**: 25 endpoints em OpenAPI YAML

**Implementado**: 40+ endpoints em Workers

**Discrepâncias principais**:

| Documentado | Implementado | Status |
|---|---|---|
| /admin/metrics/overview | /admin/overview | Path mismatch |
| /admin/metrics/diagnostics | /admin/diagnostics | Path mismatch |
| /analytics/event | /ingest/diagnostic (proxy) | Gateway pattern |
| /diagnosis/result | /ingest/diagnostic | Gateway pattern |
| *não documentado* | /ingest/ai-usage | Missing in OpenAPI |
| *não documentado* | /admin/firebase/* | Missing in OpenAPI |
| *não documentado* | /auth/login | Missing in OpenAPI |
| *não documentado* | /admin/alerts | Missing in OpenAPI |

**Autenticação** (não documentada em OpenAPI):
- Public: /api/speedtest/*, /api/ai/diagnostico-conexao
- Ingest: Bearer INGEST_KEY (write-only to /ingest/*)
- Admin: httpOnly session cookies + PBKDF2 password, rate-limited login
- No rate limits on public speedtest endpoints

### Recomendações de API

1. ✅ Consolidar OpenAPI com implementação atual (paths, auth, rate limits)
2. ✅ Documentar /ingest/* endpoints com INGEST_KEY scope
3. ✅ Documentar /auth/* endpoints (login, logout, change-password)
4. ✅ Documentar Firebase integration endpoints
5. ✅ Documentar analytics batch endpoint
6. ✅ Versionar a API (atualmente sem /v1 em paths)
7. ✅ Adicionar rate limiting info ao OpenAPI

---

## Databases

### Android Room (v12, 4 entities)

- **medicao**: id PK, timestamps, connection type, metrics, verdicts, diagnostics
- **apelido_dispositivo**: mac PK, user nicknames
- **chat_sessions**: id PK, diagnosis type, model name, token counts, status
- **chat_messages**: id PK, sessionId FK (CASCADE), role, content, status

### Cloudflare D1 (7 tables)

- **diagnostic_sessions**: test results, context (device, environment, channel)
- **ai_usage**: model, tokens, cost, environment
- **admin_users**: email UNIQUE, password (PBKDF2), role
- **admin_sessions**: token_hash PK, expires, last_seen
- **auth_rate_limit**: IP-based login throttle
- **admin_settings**: config k-v JSON
- **system_errors**: worker crash logs

### Firebase (Google Analytics)

- Events: feature_used, screen_view, app_session_start, test_completed
- Crashlytics: stack traces + device context
- **NOT using**: Realtime DB, Firestore, Cloud Storage

---

## Métricas de Qualidade

| Métrica | Valor | Status |
|---------|-------|--------|
| Documentação vs Código | 80% match (8/10) | ✅ Bom |
| OpenAPI vs Implementação | 60% match (25/40 endpoints) | ⚠️ Requer update |
| Modularidade Android | 15 módulos, zero ciclos | ✅ Excelente |
| Separação de camadas | app → features → core → frameworks | ✅ Excelente |
| Data flow clarity | Bem definidos, 4 fluxos principais | ✅ Excelente |
| API authentication | Scheme múltiplo (Bearer + session) | ✅ Robusto |

---

## Recomendações Imediatas

### 🔴 Crítico
1. Atualizar OpenAPI YAML para refletir 40+ endpoints implementados
2. Documentar autenticação (Bearer INGEST_KEY, session cookies, rate limiting)

### 🟠 Alto
1. Consolidar OpenAPI path naming (/admin/* vs /admin/metrics/*)
2. Documentar Firebase integration endpoints
3. Documentar /auth/* endpoints (não publico, mas crítico para admin)

### 🟡 Médio
1. Versionar API (adicionar /v1 a todos os paths)
2. Documentar rate limits por endpoint
3. Documentar analytics batch endpoint (/ingest/analytics)

### 🟢 Baixo
1. Atualizar README.md/CLAUDE.md com stack correto (✅ já feito)
2. Adicionar diagramas de fluxo em Miro (arquitetura já mapeada)

---

## Próximas Etapas

- [ ] Atualizar SignallQ Admin/docs/openapi/*.yaml com endpoints reais
- [ ] Criar OpenAPI spec para /ingest/* endpoints (INGEST_KEY auth)
- [ ] Criar OpenAPI spec para /auth/* endpoints
- [ ] Consolidar path naming conventions
- [ ] Adicionar versioning /v1 a todas as rotas
- [ ] Documentar rate limits por endpoint
- [ ] Publicar diagrama de arquitetura (já gerado em draw.io)

---

**Auditoria conduzida por**: Claude Code Architecture Audit Agent  
**Data**: 27.06.2026  
**Escopo**: Android (v0.21.0, build 52), PWA, 3 Cloudflare Workers  
**Commits não necessários**: Documentação apenas (README.md, CLAUDE.md, esta auditoria)
