# Admin API — Schema de Contratos

**Última atualização:** 2026-07-16 (v0.23.0, versionCode 56) — GH#1043: cobre `play_track`,
`system_health_snapshots`, `uf` e corrige vocabulário de `status`
**Versão do worker:** 1.x (Cloudflare Worker — name `signallq-admin`, diretório `signallq-admin-worker`)
**Base URL (produção):** `https://signallq-admin.giammattey-luiz.workers.dev`
**Configurada no frontend via:** `VITE_ADMIN_API_BASE_URL`

---

## Visão geral

A Admin API é um Cloudflare Worker que expõe três superfícies distintas:

| Superfície | Prefixo | Autenticação |
|---|---|---|
| Painel admin | `/admin/*` | Sessão httpOnly (cookie `session`) |
| Ingest do app | `/ingest/*` | Bearer `INGEST_KEY` |
| Feature flags públicos | `/feature-flags` | Pública (sem auth) |
| Health check | `/health` | Bearer `ADMIN_SECRET` (legado) |

**Banco de dados:** Cloudflare D1 (`signallq-admin-db`). Toda telemetria de diagnóstico e uso de IA vem do D1. Firebase é consultado via API para dados de analytics/Crashlytics (quando credenciais configuradas).

**Convenção de campos:** `camelCase` nas respostas do worker (fonte de verdade). Os campos do D1 internamente usam `snake_case`, mas o worker os mapeia para `camelCase` antes de devolver ao frontend. **Exceção:** `/admin/metrics/diagnostics` devolve campos do D1 em `snake_case` diretamente (ex.: `device_id`, `created_at`) — o frontend faz o mapeamento em `diagnosticsService.ts`.

---

## Autenticação

### Sessão httpOnly (SIG-136)

Rotas `/admin/*` exigem cookie `session` válido. O cookie é `HttpOnly; Secure; SameSite=None` e expira em 7 dias.

**Fluxo:**

```
POST /admin/auth/login   →  Set-Cookie: session=<token>; HttpOnly
GET  /admin/metrics/...  →  Cookie: session=<token>  (enviado automaticamente)
POST /admin/auth/logout  →  Set-Cookie: session=; Max-Age=0
```

O frontend usa `credentials: "include"` em todos os requests (configurado em `apiClient.ts`).

**Rate limiting:** > 5 tentativas de login em 15 min por IP resulta em HTTP 429.

**Roles:** `admin` (único role ativo). Criação de usuários exige role `admin`.

### INGEST_KEY (app Android)

Rotas `/ingest/*` aceitam `Authorization: Bearer <INGEST_KEY>`. A chave vai no APK via `BuildConfig`. Scope limitado: só pode escrever em `/ingest/*`. Não dá acesso aos dados do painel.

---

## Endpoints de autenticação

### POST /admin/auth/login

Cria sessão. Retorna cookie httpOnly.

**Request:**
```json
{
  "email": "admin@signallq.io",
  "password": "senha-segura"
}
```

**Response 200:**
```json
{
  "ok": true,
  "role": "admin"
}
```

**Headers de resposta:**
```
Set-Cookie: session=<token>; HttpOnly; Secure; SameSite=None; Path=/; Max-Age=604800
```

**Erros:**
- `400` — body inválido ou campos ausentes
- `401` — credenciais incorretas
- `429` — rate limit excedido (>5 tentativas/15 min por IP)

---

### POST /admin/auth/logout

Revoga a sessão ativa.

**Response 200:**
```json
{ "ok": true }
```

---

### GET /admin/auth/me

Retorna dados do usuário autenticado.

**Response 200:**
```json
{
  "email": "admin@signallq.io",
  "role": "admin"
}
```

---

### POST /admin/auth/users

Cria novo usuário admin. Exige role `admin`.

**Request:**
```json
{
  "email": "novo@signallq.io",
  "password": "senha-segura"
}
```

**Response 201:**
```json
{
  "ok": true,
  "id": "uuid-v4"
}
```

**Erro 409:** e-mail já cadastrado.

---

### POST /admin/auth/password

Altera senha do usuário autenticado.

**Request:**
```json
{
  "currentPassword": "senha-atual",
  "newPassword": "nova-senha"
}
```

**Response 200:**
```json
{ "ok": true }
```

---

## Endpoints de métricas (`/admin/metrics/*`)

Todos os endpoints de métricas exigem sessão válida.

### Parâmetros comuns de query

| Parâmetro | Tipo | Valores | Padrão | Descrição |
|---|---|---|---|---|
| `period` | string | `1d`, `7d`, `30d`, `90d` | `7d` | Janela temporal |
| `environment` | string | `production`, `staging`, `all` | `all` | Filtra por ambiente |

**Nota sobre `environment`:** quando ausente ou `all`, o worker não aplica filtro (retorna todos os registros). O frontend converte `today` → `1d` antes de enviar.

---

### GET /admin/metrics/overview

Contadores consolidados do período: diagnósticos, score de rede, uso de IA.

**Parâmetros:** `period`, `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "totalDiagnostics": 1247,
  "activeSessions": 83,
  "avgNetworkScore": 71,
  "aiCallsToday": 312,
  "aiCostToday": 0.0,
  "aiTokensToday": 487200
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `source` | string | Sempre `"d1"` |
| `totalDiagnostics` | integer | Total de sessões no período |
| `activeSessions` | integer | Sessões com `resolved=0` (proxy para "ativas") |
| `avgNetworkScore` | integer | Média do score 0–100 das sessões do período |
| `aiCallsToday` | integer | Chamadas de IA nas últimas 24h |
| `aiCostToday` | number | Custo USD acumulado nas últimas 24h |
| `aiTokensToday` | integer | Total de tokens consumidos nas últimas 24h |

**Nota:** `aiCallsToday`, `aiCostToday` e `aiTokensToday` são sempre das últimas 24h, independente do `period`.

---

### GET /admin/metrics/diagnostics

Lista de sessões de diagnóstico com payload completo.

**Parâmetros:** `period`, `environment`, `limit` (máx 200, padrão 50)

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "sessions": [
    {
      "id": "diag_abc123",
      "created_at": 1750700400,
      "network_type": "wifi",
      "status": "bom",
      "score": 82,
      "download_mbps": 245.3,
      "upload_mbps": 78.1,
      "latency_ms": 18,
      "jitter_ms": 2.4,
      "packet_loss": 0.1,
      "issues": ["sinal_fraco"],
      "resolved": 0,
      "operator": "Vivo",
      "device_model": "Pixel 8 Pro",
      "os_version": "Android 15",
      "app_version": "0.21.0",
      "ai_summary_report": "Conexão estável com leve instabilidade no sinal Wi-Fi...",
      "environment": "production",
      "dist_channel": "play_store",
      "build_type": "release",
      "version_code": 52,
      "device_id": "a3f8b2c1d4e5"
    }
  ]
}
```

**Schema de cada sessão:**

| Campo | Tipo | Descrição |
|---|---|---|
| `id` | string | ID único da sessão |
| `created_at` | integer | Unix timestamp em segundos |
| `network_type` | string | `wifi` \| `fibra` \| `celular` \| `ethernet` \| `unknown` |
| `status` | string | `excelente` \| `bom` \| `regular` \| `critico` \| `inconclusivo` \| `failed` \| `unknown` |
| `score` | integer\|null | Score 0–100 |
| `download_mbps` | number\|null | Velocidade de download |
| `upload_mbps` | number\|null | Velocidade de upload |
| `latency_ms` | integer\|null | Latência em ms |
| `jitter_ms` | number\|null | Jitter em ms |
| `packet_loss` | number\|null | Perda de pacotes em % |
| `issues` | string[] | Array de issue keys, vocabulario canonico GH#881 (ex.: `["sinal_fraco", "falha_dns"]`) — ver ADR-009 |
| `resolved` | integer | `0` = aberto, `1` = resolvido |
| `operator` | string | Operadora detectada (ex.: `"Claro"`, `"Vivo"`) |
| `device_model` | string | Modelo do dispositivo |
| `os_version` | string | Ex.: `"Android 15"` |
| `app_version` | string | Ex.: `"0.21.0"` |
| `ai_summary_report` | string | Texto do laudo gerado pela IA (pode ser vazio) |
| `environment` | string | `production` \| `staging` \| `development` |
| `dist_channel` | string | `play_store` \| `firebase_app_distribution` \| `sideload` |
| `build_type` | string | `release` \| `debug` |
| `version_code` | integer | Build number do app |
| `device_id` | string | Hash anônimo do dispositivo (sem PII) |

**Mapeamento no frontend (`diagnosticsService.ts`):** o service converte `snake_case` → `camelCase` e `created_at` (Unix) → ISO 8601 string.

---

### GET /admin/metrics/timeline

Série temporal de diagnósticos agrupados por dia.

**Parâmetros:** `period`, `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "timeline": [
    {
      "date": "2026-06-18",
      "completedDiagnostics": 178,
      "activeUsers": 178,
      "criticalAlerts": 12
    }
  ]
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `date` | string | Formato `YYYY-MM-DD` (UTC) |
| `completedDiagnostics` | integer | Sessões no dia |
| `activeUsers` | integer | Proxy: igual a `completedDiagnostics` (sem user_id no D1 — limitação documentada em SIG-110) |
| `criticalAlerts` | integer | Sessões com `status='failed'` ou `score < 40` |

---

### GET /admin/metrics/network

Distribuição de sessões por tipo de rede com métricas médias.

**Parâmetros:** `period`, `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "items": [
    {
      "name": "wifi",
      "count": 724,
      "avg_score": 74,
      "avg_download_mbps": 187.4,
      "avg_latency_ms": 22,
      "percentage": 58.1
    },
    {
      "name": "celular",
      "count": 312,
      "avg_score": 61,
      "avg_download_mbps": 34.2,
      "avg_latency_ms": 54,
      "percentage": 25.0
    }
  ]
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `name` | string | Valor de `network_type` do D1 |
| `count` | integer | Total de sessões do tipo no período |
| `avg_score` | integer\|null | Média de score |
| `avg_download_mbps` | number\|null | Média de download |
| `avg_latency_ms` | integer\|null | Média de latência |
| `percentage` | number | % do total de sessões no período |

---

### GET /admin/metrics/top-issues

Top 5 problemas mais frequentes no período.

**Parâmetros:** `period`, `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "items": [
    { "id": "issue_1", "problem": "sinal_fraco", "count": 284, "percentage": 38 },
    { "id": "issue_2", "problem": "falha_dns", "count": 201, "percentage": 27 },
    { "id": "issue_3", "problem": "bufferbloat", "count": 143, "percentage": 19 }
  ]
}
```

**Limitação:** o worker explode o array `issues` de cada sessão em runtime (D1 não suporta `json_each` nativo). Performance aceitável até ~10k sessões no período; acima disso considerar pré-agregação.

---

### GET /admin/metrics/alerts

Alertas em tempo real baseados em threshold checking contra o D1.

**Parâmetros:** `environment` (ignora `period` — sempre últimas 24h/1h)

**Response 200:**
```json
{
  "source": "d1",
  "items": [
    {
      "id": "ai_budget_exceeded",
      "type": "AI_BUDGET",
      "severity": "critical",
      "title": "Orçamento diário de IA excedido",
      "message": "Custo nas últimas 24h: $1.2400 USD (limite: $1.0)",
      "created_at": 1750784400,
      "resolved": false
    }
  ]
}
```

**Tipos de alerta:**

| `type` | `severity` | Condição |
|---|---|---|
| `AI_BUDGET` | `critical` | Custo de IA últimas 24h > `aiDailyBudgetUsd` (default: $1.00) |
| `ERROR_SPIKE` | `warning` | Erros na última hora > `errorSpikeThreshold` (default: 10) |
| `LOW_SCORE` | `warning` | Score médio últimas 24h < `criticalScoreThreshold` (default: 50) |

Thresholds configuráveis via `PATCH /admin/settings`.

---

### GET /admin/metrics/ai-usage

Uso de IA agrupado por modelo, com totais.

**Parâmetros:** `period`, `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "byModel": [
    {
      "model": "@cf/qwen/qwen3-30b-a3b-fp8-fast",
      "calls": 1247,
      "tokens": 2187400,
      "cost_usd": 0.0
    }
  ],
  "totals": {
    "calls": 1247,
    "tokens": 2187400,
    "cost": 0.0
  }
}
```

**Nota:** custo é 0 para modelos free tier (Gemini, Qwen/Workers AI). Ver `AI_MODEL_RATE_USD` em `index.ts` para adicionar modelos pagos.

---

### GET /admin/metrics/ai-costs

Métricas de custo e confiabilidade de IA para o período.

**Parâmetros:** `period`, `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "totalCostUsd": 0.0,
  "totalRequests": 1247,
  "avgCostPerRequest": 0.0,
  "totalTokens": 2187400,
  "promptTokens": 1574688,
  "completionTokens": 612712,
  "reliabilityPercentage": 98
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `reliabilityPercentage` | integer | % de chamadas com `completion_tokens > 0` (resposta real gerada) |
| `promptTokens` | integer | Total de tokens de entrada no período |
| `completionTokens` | integer | Total de tokens de saída no período |

---

### GET /admin/metrics/ai-providers

Distribuição de uso de tokens por provedor (agrupamento de modelos).

**Parâmetros:** `period`, `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "items": [
    { "name": "Qwen / Workers AI", "tokensProcessed": 1820000, "percentage": 83 },
    { "name": "Gemini", "tokensProcessed": 367400, "percentage": 17 }
  ]
}
```

**Mapeamento modelo → provedor (no worker):**

| Padrão no nome do modelo | Provedor exibido |
|---|---|
| `gemini` | `Gemini` |
| `qwen` ou `@cf/` | `Qwen / Workers AI` |
| `gpt` | `OpenAI GPT` |
| `claude` | `Anthropic Claude` |
| outro | nome técnico do modelo |

**Cores não vêm do worker** — são mapeadas no frontend (`adminMetricsService.ts`).

---

### GET /admin/metrics/ai-usage/timeline

Série temporal de tokens por provedor por dia.

**Parâmetros:** `days` (1–90, padrão 30), `environment`

**Response 200:**
```json
{
  "source": "d1",
  "days": 7,
  "environment": "production",
  "series": [
    {
      "date": "2026-06-18",
      "byProvider": {
        "Qwen / Workers AI": 182400,
        "Gemini": 34200
      }
    }
  ]
}
```

---

### GET /admin/metrics/ai-usage/records

Histórico de execuções individuais de IA (GH#421) — cada item é uma linha real
de `ai_usage`, correlacionada com `diagnostic_sessions` via `session_id` quando
existir. Substitui a tabela mockada/vazia da aba "IA & Custo".

**Parâmetros:** `period` (padrão `7d`), `environment`, `limit` (1–500, padrão 100)

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "records": [
    {
      "id": "a1b2c3d4-...",
      "timestamp": "2026-07-04T13:20:05.000Z",
      "model": "@cf/qwen/qwen3-30b-a3b-fp8",
      "provider": "Qwen / Workers AI",
      "promptTokens": 812,
      "completionTokens": 305,
      "costUsd": 0,
      "status": "success",
      "errorMessage": null,
      "diagnosisId": "diag_8f3d1e90",
      "environment": "production"
    }
  ]
}
```

Sem campo de latência: o schema de `ai_usage` não registra tempo de resposta —
não é inventado no worker nem no frontend. `status`/`errorMessage` dependem da
migration `009_gh421.sql`; registros anteriores a ela assumem `status: "success"`
(default da coluna), já que o app hoje só grava `ai_usage` ao final de uma
chamada concluída.

---

### GET /admin/analytics/product

Métricas de uso de produto (Produto & Uso, GH#418). Também aceita o prefixo
`/admin/metrics/analytics/product` (compatibilidade). Fonte: `analytics_events`
(alimentada por `POST /ingest/analytics` — ver seção de ingest).

**Parâmetros:** `period` (padrão `7d`), `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "environment": "production",
  "no_data_yet": false,
  "feature_usage": [
    { "feature": "speedtest", "label": "speedtest", "usageCount": 1240, "uniqueUsers": 380,
      "completionRate": 0, "failureRate": 0, "avgDurationMs": 0, "trendPercent": 0 }
  ],
  "screen_navigation": [
    { "screen": "home", "label": "home", "views": 4200, "uniqueUsers": 900,
      "avgTimeOnScreenSec": 0, "exitRate": 0, "nextMostCommonScreen": null }
  ],
  "feature_crashes": [
    { "feature": "devices_scan", "label": "devices_scan", "crashes": 3, "nonFatalErrors": 0,
      "anrs": 0, "crashRate": 1.2, "affectedVersions": ["0.21.0"], "severity": "attention" }
  ],
  "avg_session_duration_ms": 187000,
  "session_count": 640,
  "retention": [
    { "cohort": "Cohort geral (7d)", "cohortSize": 320,
      "day1": 42.5, "day7": 18.0, "day30": null,
      "avgInstalledDays": 6.2, "uninstallRate": 11.4 }
  ]
}
```

| Campo | Descrição |
|---|---|
| `feature_usage[].completionRate`, `failureRate`, `avgDurationMs`, `trendPercent` | Ainda **não computados** — sempre `0`. Exigiriam eventos `feature_started`/`feature_completed`/`feature_failed` distintos de `feature_used`, que o app não emite hoje |
| `screen_navigation[].avgTimeOnScreenSec`, `exitRate`, `nextMostCommonScreen` | Idem — não computados, sempre `0`/`null` |
| `avg_session_duration_ms` | Média real de `duration_ms` dos eventos `session_end` no período. `null` se não houver nenhum evento `session_end` com duração no período |
| `session_count` | Total de eventos `session_end` com `duration_ms` no período |
| `retention[].day1`/`day7`/`day30` | % de dispositivos (`device_id`) que retornaram na respectiva janela após o primeiro evento visto. `null` quando nenhum dispositivo do cohort ainda tem dias suficientes decorridos para aquela janela |
| `retention[].uninstallRate` | Proxy de inatividade: % de dispositivos sem nenhum evento nos últimos 14 dias. **Não é confirmação de desinstalação** (exigiria Play Console, não integrado) |
| `retention[].avgInstalledDays` | Média de dias entre o primeiro e o último evento observado por dispositivo (span de atividade observado, não "tempo até desinstalar") |

**Gap conhecido (não resolvido nesta correção):** associação de uso de IA por
feature (`GET /admin/analytics/product` não inclui `feature_ai_usage`). Exigiria
um identificador de sessão compartilhado entre `analytics_events.session_id`
(sessão de app) e `ai_usage.session_id` (hoje referencia `diagnostic_sessions.id`)
— são espaços de ID diferentes hoje, cruzá-los produziria associação incorreta.
Ver `SignallQ Admin/docs/architecture/data-architecture.md` (seção Gaps) para o
contrato necessário antes de implementar.

---

### GET /admin/analytics/battery

Snapshot agregado de nível de bateria. Também aceita
`/admin/metrics/analytics/battery`.

**Parâmetros:** `period` (padrão `7d`)

**Response 200:**
```json
{
  "source": "d1",
  "period": "7d",
  "no_data_yet": false,
  "summary": {
    "avg_battery_level": 62,
    "charging_sessions_pct": 18,
    "total_snapshots": 940
  },
  "items": []
}
```

---

### GET /admin/metrics/operators

Métricas de diagnóstico agrupadas por operadora de telecomunicações.

**Parâmetros:** `period` (padrão `30d`), `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "30d",
  "environment": "production",
  "operators": [
    {
      "operator": "Vivo",
      "total_diagnostics": 412,
      "avg_score": 68,
      "avg_download": 87.3,
      "avg_upload": 31.2,
      "avg_latency": 48,
      "completed": 398,
      "resolved": 127
    }
  ]
}
```

**Nota:** sessões sem operadora (`operator = ''`) são excluídas desta query.

---

### GET /admin/metrics/app-versions

Sessões de diagnóstico agrupadas por versão do app, build e canal de distribuição —
fonte real para a aba "Versões" do painel (GH#423). Não depende de Firebase/BigQuery:
usa apenas colunas já ingeridas em `diagnostic_sessions`.

**Parâmetros:** `period` (padrão `30d`), `environment`

**Response 200:**
```json
{
  "source": "d1",
  "period": "30d",
  "environment": "production",
  "versions": [
    {
      "appVersion": "0.21.0",
      "versionCode": 52,
      "distChannel": "play_store",
      "buildType": "release",
      "sessions": 842,
      "avgScore": 74,
      "firstSeen": 1751500800,
      "lastSeen": 1751932800
    }
  ],
  "productionVersion": { "appVersion": "0.21.0", "versionCode": 52, "distChannel": "play_store", "...": "..." }
}
```

| Campo | Tipo | Descrição |
|---|---|---|
| `versions[].appVersion` | string | `app_version` do D1 |
| `versions[].versionCode` | integer\|null | `version_code` do D1 |
| `versions[].distChannel` | string | `play_store` \| `firebase_app_distribution` \| `sideload` \| `unknown` |
| `versions[].buildType` | string | `release` \| `debug` \| `unknown` |
| `versions[].sessions` | integer | Total de sessões da combinação versão+canal+build no período |
| `versions[].avgScore` | integer\|null | Score médio das sessões |
| `versions[].firstSeen` / `lastSeen` | integer | Unix timestamp em segundos |
| `productionVersion` | object\|null | Entrada de `versions[]` com `distChannel: "play_store"` mais recente (fallback: qualquer canal) — usada como "versão em produção" |

**Crash rate por versão:** não vem desta rota. O frontend cruza esta lista com `GET /admin/integrations/firebase/versions` (crashes por `appVersion`, exige credenciais Firebase); quando o Firebase não está configurado, o painel exibe explicitamente "Não configurado" em vez de omitir ou zerar o dado.

---

### GET /admin/metrics/errors

Erros de sistema dedupliciados, ordenados por frequência.

**Parâmetros:** `period` (padrão `30d`), `environment` (não filtrado nesta query — erros são globais)

**Response 200:**
```json
{
  "source": "d1",
  "period": "30d",
  "errors": [
    {
      "id": "aW5nZXN0OnRhYmxlIG5v",
      "source": "ingest",
      "message": "table not found: diagnostic_sessions",
      "stackTrace": "...",
      "count": 3,
      "first_seen": 1750700000000,
      "last_seen": 1750784400000,
      "timestamp": "2026-06-24T10:00:00.000Z",
      "affectedUserCount": 0
    }
  ]
}
```

**Nota:** `first_seen` e `last_seen` são Unix em **milissegundos** (Date.now()). O campo `timestamp` é derivado de `last_seen` convertido para ISO 8601.

---

### GET /admin/system-health

Saúde do sistema com verificação real de cada dependência — GH#425. Substitui os placeholders
que existiam na aba "Saúde do Sistema" (workers mockados, D1 sempre "connected", sem checagem
de Firebase/BigQuery/ingest).

**Parâmetros:** nenhum.

**Response 200:**
```json
{
  "source": "worker",
  "timestamp": "2026-07-04T12:00:00.000Z",
  "checks": {
    "worker": { "status": "ok" },
    "d1": { "status": "ok", "latencyMs": 12 },
    "firebaseCredentials": { "status": "ok", "latencyMs": 180 },
    "bigQuery": { "status": "not_configured", "message": "Requer credenciais Firebase válidas para autenticar no BigQuery." },
    "ingest": { "status": "ok", "keyConfigured": true, "lastSuccessAt": "2026-07-04T11:20:00.000Z" }
  },
  "lastFailure": { "source": "bigquery-crashlytics", "message": "table_not_found", "timestamp": "2026-07-04T06:00:00.000Z" },
  "lastSuccess": { "source": "ingest", "timestamp": "2026-07-04T11:20:00.000Z" }
}
```

| Campo | Descrição |
|---|---|
| `checks.worker` | Sempre `ok` se o worker respondeu — o próprio fato de gerar esta resposta prova que o worker está de pé |
| `checks.d1` | Executa `SELECT 1` real no D1. `latencyMs` medido no worker |
| `checks.firebaseCredentials` | Gera um JWT real e troca por access token OAuth2 (`getFirebaseAccessToken`). `not_configured` se `FIREBASE_CLIENT_EMAIL`/`FIREBASE_PRIVATE_KEY` ausentes |
| `checks.bigQuery` | Roda `SELECT 1 AS ok` no BigQuery via API real. `not_configured` se as credenciais Firebase não passaram no check anterior |
| `checks.ingest` | `keyConfigured` reflete se `INGEST_KEY` está definida. `lastSuccessAt` é o `MAX(created_at)` de `diagnostic_sessions` — status `idle` se não houver ingest nas últimas 48h |
| `lastFailure` | Última linha de `system_errors` por `last_seen DESC` (pode ser `null`) |
| `lastSuccess` | Baseado no `ingest.lastSuccessAt` (pode ser `null` se nunca houve ingest) |

**Status possíveis:** `ok`, `error`, `not_configured`, `idle`. Nenhum é tratado como "sempre verde" no frontend — `not_configured` e `idle` são estados legítimos e exibidos como tal.

---

## Endpoints de integração Firebase (`/admin/integrations/firebase/*`)

### GET /admin/integrations/firebase/status

```json
{
  "source": "worker",
  "projectId": "signallq-app",
  "status": "connected",
  "hasCredentials": true,
  "ga4PropertyConfigured": true
}
```

### GET /admin/integrations/firebase/analytics

Faz chamada real à GA4 Data API quando credenciais estão configuradas. Retorna dados brutos da API do Google.

```json
{
  "source": "firebase_analytics",
  "data": { ... }
}
```

### GET /admin/integrations/firebase/crashlytics

Consulta real ao BigQuery (export do Crashlytics) quando há credenciais. `source` indica o estado:

| `source` | Significado |
|---|---|
| `no_credentials` | `FIREBASE_CLIENT_EMAIL`/`FIREBASE_PRIVATE_KEY` ausentes no worker |
| `no_data_yet` | Export do BigQuery ativo mas sem linhas (tabela ainda não existe ou sem crash no período) |
| `bigquery` | Dados reais |
| `error` | Erro na query — logado em `system_errors` |

```json
{
  "source": "bigquery",
  "unresolvedCrashes": 4,
  "affectedUsers": 3,
  "crashFreeUsersPercentage": 99.6
}
```

**Nota:** o frontend (`firebaseAdapter.ts`) trata `no_credentials`, `no_data_yet` e `error` como "dado indisponível" (`null`) — nunca exibe zero como se fosse dado real.

### GET /admin/integrations/firebase/versions

Total de crashes por `app_version` via BigQuery, mesmos estados de `source` da rota acima.

```json
{
  "source": "bigquery",
  "versions": [
    { "app_version": "0.21.0", "total_crashes": 12, "affected_users": 8 }
  ]
}
```

**Limitação:** não cruza com `version_code`/`dist_channel` do D1 — junte com `GET /admin/metrics/app-versions` no frontend para exibir versão + canal + crashes juntos (feito em `VersionsTab.tsx`).

### POST /admin/integrations/firebase/sync

Roda a query de conectividade no BigQuery (conta `session_start` do dia anterior) e persiste `admin_settings/'firebase_sync'`. Também disparado pelo cron diário (`[triggers]` em `wrangler.toml`, 06:00 UTC) e pelo botão "Sincronizar telemetria" no Admin.

```json
{
  "ok": false,
  "source": "disabled",
  "message": "Firebase Analytics não configurado (export BigQuery ausente)."
}
```

| `source` | Significado |
|---|---|
| `disabled` | `FIREBASE_SYNC_ENABLED` != `"true"` em `wrangler.toml` — job pulado sem chamar BigQuery (ver decisão abaixo) |
| `no_credentials` | `FIREBASE_CLIENT_EMAIL`/`FIREBASE_PRIVATE_KEY` ausentes |
| `no_data_yet` | Query rodou mas a tabela `events_*` do BigQuery ainda não existe, ou não achou sessão no período — **não é falha** |
| `bigquery` (`ok: true`) | Sync real, conta de sessões do dia anterior |
| `error` | Falha real na query (auth, `bq_error_*`) — logado em `system_errors` (só quando `FIREBASE_SYNC_ENABLED=true`) |

**Decisão 2026-07-11 (GH#877/#878) — sync de Firebase desligado por flag:** o job do cron
começou (PR #878) a registrar corretamente os erros reais do BigQuery em vez de escondê-los
como `no_data_yet`. Só que o export GA4→BigQuery (`analytics_542463828`) **nunca foi criado**
— o projeto Firebase `signallq-app` está em modo Sandbox e o Luiz decidiu **não habilitar
billing** (sem custo novo). Resultado: todo dia às 06:00 UTC o cron ia gravar um
`bq_error_403` real em `system_errors`, ruído permanente sem solução no horizonte.

Em vez de apagar `handleFirebaseSync`, o worker ganhou a env var `FIREBASE_SYNC_ENABLED`
(`wrangler.toml`, default `"false"`). Com a flag desligada, o handler curto-circuita antes
de chamar o BigQuery e devolve `source: "disabled"` — nem o cron nem o botão manual geram
erro cru; os outros dois jobs agendados (`google-play`, `google-play-tracks`) continuam
rodando normalmente.

**Para reativar:** (1) habilitar billing no projeto `signallq-app`; (2) criar o vínculo
GA4 → BigQuery no Firebase Console; (3) virar `FIREBASE_SYNC_ENABLED = "true"` em
`wrangler.toml`; (4) conceder a role `dataViewer` do dataset recém-criado à service account
`signallq-admin-bq`.

---

## Endpoints de integração Google Play (`/admin/integrations/google-play/*`)

**GH#1042/#1043 — já integrado**, ao contrário do que a versão anterior deste documento (05/07)
listava em "Endpoints planejados". Exige sessão.

| Endpoint | Método | Descrição |
|---|---|---|
| `/admin/integrations/google-play/status` | GET | Status do último sync de reviews/rating |
| `/admin/integrations/google-play/sync` | POST | Sincroniza rating médio via Play Developer Reviews API. Também roda no cron diário (`google-play`) |
| `/admin/integrations/google-play/tracks/status` | GET | Status do último sync de `play_console_tracks` |
| `/admin/integrations/google-play/tracks/sync` | POST | Sincroniza `play_console_tracks` (version_code → trilha) via Android Publisher API. Também roda no cron diário (`google-play-tracks`) |
| `/admin/integrations/google-play/tracks/backfill` | POST | Preenche `play_track` retroativamente em `diagnostic_sessions`/`ai_usage`/`analytics_events` a partir de `play_console_tracks` — **manual**, não automático. Idempotente (só atualiza linhas com `play_track IS NULL`) |

**Limitação conhecida (GH#1042):** os handlers de métrica (`/admin/metrics/*`) ainda **não filtram
por `play_track`** — só por `environment`. Até o app passar pelo M3 (lançamento Play Store), nenhuma
trilha `production` existe em `play_console_tracks`, então o efeito prático do filtro seria nulo;
o mecanismo (tabela, backfill, sync) já está pronto para quando isso mudar.

---

## Endpoints de configuração

### GET /admin/settings

Retorna as configurações salvas no D1 (tabela `admin_settings`, chave `'admin'`). Retorna `{}` se nunca configurado.

**GH#426:** o contrato de settings foi reduzido aos únicos três campos com consumidor real
no worker (lidos em `GET /admin/metrics/alerts`). Os demais campos que existiam antes
(`selectedDefaultAiModel`, `aiFallbackEnabled`, `maxTokensPerDiagnostic`, `speedtestIntervalSeconds`,
`androidLogsCollectionEnabled`, `stagingAlertWebhookUrl`, `productionAlertWebhookUrl`,
`cloudflareWorkerEndpoint`, `monthlyBudgetUsd`, `budgetAction`, `anonymizeIp`, `retentionDays`,
`firebaseAnalyticsEnabled`, `maxAiTokensUserDaily`, `maxSpeedTestDataDailyMb`,
`contextualAdsEnabled`, `contextualAdsCategories`) eram persistidos no D1 mas nunca lidos por
nenhum código do worker ou do app — foram removidos da UI e do contrato do frontend. Reintroduzir
qualquer um deles exige, no mesmo PR, o código que efetivamente os consome.

**Response 200:**
```json
{
  "source": "d1",
  "settings": {
    "aiDailyBudgetUsd": 1.0,
    "errorSpikeThreshold": 10,
    "criticalScoreThreshold": 50
  }
}
```

| Campo | Tipo | Consumidor real | Descrição |
|---|---|---|---|
| `aiDailyBudgetUsd` | number | `GET /admin/metrics/alerts` (`AI_DAILY_BUDGET`) | Custo de IA (USD) nas últimas 24h acima do qual dispara alerta crítico `AI_BUDGET` |
| `errorSpikeThreshold` | integer | `GET /admin/metrics/alerts` (`ERROR_THRESHOLD`) | Erros na última hora acima do qual dispara alerta `ERROR_SPIKE` |
| `criticalScoreThreshold` | integer (0-100) | `GET /admin/metrics/alerts` (`MIN_SCORE`) | Score médio nas últimas 24h abaixo do qual dispara alerta `LOW_SCORE` |

### POST /admin/settings

Persiste o objeto completo de settings no D1. Substitui o registro anterior (`INSERT OR REPLACE`).
Valida `aiDailyBudgetUsd` (número ≥ 0), `errorSpikeThreshold` (inteiro ≥ 1) e `criticalScoreThreshold`
(inteiro entre 0 e 100) quando presentes no body — `400` caso contrário.

**Request:** objeto JSON com qualquer subconjunto dos três campos do schema acima.

**Response 200:**
```json
{
  "ok": true,
  "settings": { "aiDailyBudgetUsd": 1.0, "errorSpikeThreshold": 10, "criticalScoreThreshold": 50 }
}
```

---

## Endpoints de feature flags

### GET /admin/feature-flags

Retorna todas as flags (internas e públicas). Exige sessão.

**Response 200:**
```json
{
  "source": "d1",
  "flags": [
    { "key": "ai_diagnosis_enabled",  "enabled": true,  "scope": "public",   "description": "Habilita diagnóstico por IA" },
    { "key": "speedtest_enabled",     "enabled": true,  "scope": "public",   "description": "Habilita speedtest" },
    { "key": "fibra_module_enabled",  "enabled": true,  "scope": "public",   "description": "Habilita módulo fibra" },
    { "key": "new_ui_diagnostics",    "enabled": false, "scope": "internal", "description": "Nova UI de diagnósticos (internal)" }
  ]
}
```

### PUT /admin/feature-flags/:key

Atualiza o campo `enabled` de uma única flag identificada por `:key` e grava um registro de auditoria. Exige sessão.

**Request:**
```json
{ "enabled": false }
```

**Response 200:**
```json
{ "ok": true }
```

**Erros:** `400` (body inválido), `404` (flag inexistente).

### GET /flags (público)

Sem prefixo `/admin/`. Sem auth. Retorna `key` + `enabled` da tabela `feature_flags` (SIG-13) para consumo do app Android.

```json
{
  "flags": [
    { "key": "feature_diagnostico_ia", "enabled": true }
  ]
}
```

### GET /feature-flags (público)

Sem prefixo `/admin/`. Sem auth. Retorna apenas flags com `scope: "public"`. Consumido pelo app Android para verificar flags sem credenciais admin.

```json
{
  "flags": [
    { "key": "ai_diagnosis_enabled", "enabled": true, "scope": "public", "description": "..." }
  ]
}
```

---

## Endpoints de ingest (app Android)

### POST /ingest/diagnostic

Persiste uma sessão de diagnóstico. Autenticação: `Authorization: Bearer <INGEST_KEY>`.

**Request:**
```json
{
  "id": "diag_uuid",
  "created_at": 1750784400,
  "network_type": "wifi",
  "status": "bom",
  "score": 82,
  "download_mbps": 245.3,
  "upload_mbps": 78.1,
  "latency_ms": 18,
  "jitter_ms": 2.4,
  "packet_loss": 0.1,
  "issues": ["sinal_fraco"],
  "operator": "Vivo",
  "device_model": "Pixel 8 Pro",
  "os_version": "Android 15",
  "app_version": "0.21.0",
  "ai_summary_report": "Texto do laudo...",
  "environment": "production",
  "dist_channel": "play_store",
  "build_type": "release",
  "version_code": 52,
  "device_id": "a3f8b2c1d4e5"
}
```

**Campos obrigatórios:** `id`. Todos os demais têm defaults no D1.

**Response 201:**
```json
{ "ok": true, "id": "diag_uuid" }
```

---

### POST /ingest/ai-usage

Persiste um registro de uso de IA. Autenticação: `Authorization: Bearer <INGEST_KEY>`.

**Request:**
```json
{
  "id": "ai_uuid",
  "session_id": "diag_uuid",
  "created_at": 1750784400,
  "model": "@cf/qwen/qwen3-30b-a3b-fp8-fast",
  "prompt_tokens": 1280,
  "completion_tokens": 420,
  "total_tokens": 1700,
  "cost_usd": 0.0,
  "environment": "production",
  "version_code": 52,
  "status": "success",
  "error_message": ""
}
```

**Campos obrigatórios:** `id`, `model`. `cost_usd` é calculado pelo worker se ausente (via `costForModel()`).
**GH#421:** `status` (`"success"` | `"error"`, default `"success"`) e `error_message`/`error`
(opcional) — permite ao painel auditar falhas de inferência por execução, não só
custo agregado. Requer migration `009_gh421.sql` para as colunas existirem no D1.

**Response 201:**
```json
{ "ok": true, "id": "ai_uuid" }
```

---

## Endpoint de health check

### GET /health

**Auth:** `Authorization: Bearer <ADMIN_SECRET>` (legado — mantido para monitoramento externo).

**Response 200:**
```json
{ "status": "ok", "worker": "signallq-admin-worker" }
```

---

## Respostas de erro

Todos os erros seguem o schema:

```json
{ "error": "mensagem descritiva em PT-BR" }
```

| Status | Significado |
|---|---|
| `400` | Body inválido ou parâmetro obrigatório ausente |
| `401` | Sessão ausente, expirada ou credenciais inválidas |
| `403` | Autenticado, mas sem permissão (ex.: role insuficiente) |
| `404` | Rota não encontrada |
| `405` | Método HTTP não suportado na rota |
| `409` | Conflito (ex.: e-mail já cadastrado) |
| `429` | Rate limit excedido |
| `500` | Erro interno do worker |

---

## Schema do Cloudflare D1

### Tabela `diagnostic_sessions`

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | TEXT PK | ID único da sessão |
| `created_at` | INTEGER | Unix timestamp (segundos) |
| `network_type` | TEXT | Tipo de rede |
| `status` | TEXT | Veredito: `excelente \| bom \| regular \| critico \| inconclusivo \| failed \| unknown` (GH#764 — mapeia a decisao real do motor local, nao o ciclo de vida da sessao) |
| `score` | INTEGER | 0–100 |
| `download_mbps` | REAL | — |
| `upload_mbps` | REAL | — |
| `latency_ms` | INTEGER | — |
| `jitter_ms` | REAL | — |
| `packet_loss` | REAL | — |
| `issues` | TEXT | JSON array serializado |
| `resolved` | INTEGER | 0 = aberto, 1 = resolvido |
| `operator` | TEXT | Operadora |
| `environment` | TEXT | `production \| staging \| development` |
| `dist_channel` | TEXT | Canal de distribuição |
| `build_type` | TEXT | `release \| debug` |
| `version_code` | INTEGER | Build number |
| `device_id` | TEXT | Hash anônimo |
| `device_model` | TEXT | Modelo do dispositivo |
| `os_version` | TEXT | Versão do Android |
| `app_version` | TEXT | Ex.: `"0.21.0"` |
| `ai_summary_report` | TEXT | Laudo gerado pela IA |
| `platform` | TEXT | `android` \| `web` (GH#442 — `web` é dado histórico do extinto PWA; default `android`) |
| `play_track` | TEXT\|NULL | Trilha do Play Console (`internal`\|`alpha`\|`beta`\|`production`\|outra trilha custom) resolvida via `play_console_tracks.version_code`. `NULL` = ainda não mapeada — **nunca assumir `production`** quando ausente (migration `012_play_track.sql`, GH#1042: hoje 100% do dado `environment=production` está com `play_track=null` ou é trilha fechada, porque nenhuma trilha `production` existe até o app passar pelo M3) |
| `uf` | TEXT | UF brasileira aproximada (2 letras), derivada de `request.cf.regionCode` no momento do ingest — nunca o IP em si. `''` se fora da whitelist de 27 UFs (`UF_WHITELIST`, migration `014_gh786.sql`) |

### Tabela `ai_usage`

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | TEXT PK | ID único do registro |
| `session_id` | TEXT FK | Referencia `diagnostic_sessions.id` |
| `created_at` | INTEGER | Unix timestamp (segundos) |
| `model` | TEXT | ID técnico do modelo (ex.: `@cf/qwen/qwen3-30b-a3b-fp8-fast`) |
| `prompt_tokens` | INTEGER | — |
| `completion_tokens` | INTEGER | — |
| `total_tokens` | INTEGER | — |
| `cost_usd` | REAL | Custo calculado pelo worker |
| `environment` | TEXT | — |
| `version_code` | INTEGER | — |
| `status` | TEXT | `success` \| `error`. Default `success` (GH#421, migration `009_gh421.sql`) |
| `error_message` | TEXT | Mensagem de erro quando `status = 'error'`; vazio caso contrário |
| `platform` | TEXT | `android` \| `web` (GH#442, default `android`) |
| `play_track` | TEXT\|NULL | Idem `diagnostic_sessions.play_track` (migration `012_play_track.sql`) |

### Tabela `play_console_tracks`

Mapeamento `version_code` → trilha do Play Console, sincronizado via Android Publisher API
(migration `012_play_track.sql`, GH#1042). Alimenta o backfill de `play_track` nas três tabelas de
telemetria (`diagnostic_sessions`, `ai_usage`, `analytics_events`) — o backfill é **manual**
(`POST /admin/integrations/google-play/tracks/backfill`), não roda automaticamente a cada sync.

| Coluna | Tipo | Descrição |
|---|---|---|
| `version_code` | INTEGER PK | versionCode do app (Android) |
| `track` | TEXT | Trilha real do Play Console — `internal`\|`alpha`\|`beta`\|`production`\| trilha custom de teste fechado/aberto (não restrito às 4 padrão) |
| `synced_at` | INTEGER | Unix timestamp (segundos) da última sincronização via `POST /admin/integrations/google-play/tracks/sync` |

### Tabela `system_health_snapshots`

Série histórica de latência/status por dependência (GH#788), gravada a cada execução do Cron
Trigger do worker (ver `scheduled` em `src/index.ts`) — alimenta o histórico da aba "Saúde do
Sistema" (o endpoint `GET /admin/system-health` acima só reflete o instante atual).

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | TEXT PK | ID único do snapshot |
| `service` | TEXT | `d1` \| `firebase` \| `bigquery` |
| `status` | TEXT | `ok` \| `error` \| `not_configured` |
| `latency_ms` | INTEGER\|NULL | Latência medida no snapshot |
| `created_at` | INTEGER | Unix timestamp (segundos) |

### Tabela `admin_settings`

| Coluna | Tipo | Descrição |
|---|---|---|
| `key` | TEXT PK | `'admin'` (settings) ou `'feature_flags'` |
| `value` | TEXT | JSON serializado do payload |
| `updated_at` | INTEGER | Unix timestamp (segundos) |

### Tabela `system_errors`

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | TEXT PK | Hash base64 de `source:message` (deduplicação determinística) |
| `source` | TEXT | Ex.: `ingest`, `ai-usage`, `worker` |
| `message` | TEXT | Mensagem do erro |
| `stack_trace` | TEXT | Stack trace (pode ser vazio) |
| `count` | INTEGER | Número de ocorrências |
| `first_seen` | INTEGER | Unix ms (Date.now()) |
| `last_seen` | INTEGER | Unix ms (Date.now()) |

---

## Endpoints planejados (não implementados)

Os endpoints abaixo são necessários para completar o painel mas **ainda não existem no worker**. O frontend os trata com mock ou retorna `null`.

| Endpoint | Motivo | Bloqueio |
|---|---|---|
| `GET /admin/metrics/diagnostics/:id` | Detalhe de uma sessão específica | Worker só tem listagem, sem endpoint de detalhe individual |
| `POST /admin/errors/:id/resolve` | Marcar erro como resolvido | Retorna `{ success: false, message: "Em implementação" }` |
| `POST /diagnosis/explain` | Rediagnóstico remoto | Retorna `{ success: false }` com mensagem informativa |

**Nota (GH#423):** `GET /admin/metrics/app-versions`, `GET /admin/integrations/firebase/crashlytics` e `GET /admin/integrations/firebase/versions` já existem no worker (ver seções acima) — retiradas desta tabela. A aba "Versões" do painel usa o primeiro para versão/canal/sessões reais (D1) e o segundo para crash rate real quando o Firebase está configurado.

---

## Convenções do frontend

### Modo mock vs. produção

Controlado por `VITE_ENABLE_MOCKS` (padrão: `true`). Quando `true`, todos os services usam dados de `src/mocks/`. Quando `false` e `VITE_ADMIN_API_BASE_URL` estiver configurada, os services fazem chamadas HTTP reais.

O `apiClient.ts` lança erro ao tentar fazer request em modo mock — não silencia: `"ApiClient está em modo mock ou sem VITE_ADMIN_API_BASE_URL"`.

### Header customizado

Todos os requests do frontend enviam `X-Environment: <production|staging>` para contexto de logging no worker. O filtro de ambiente nas queries é feito via query param `?environment=`, não via header.

### Timeout

Padrão: 15 segundos (`VITE_API_TIMEOUT_MS`). Configura via `.env`.

---

## Como manter este documento

Qualquer mudança no worker (`integrations/cloudflare/signallq-admin-worker/src/index.ts`) que adicione, remova ou altere endpoint, campo ou comportamento de autenticação **deve** ser acompanhada de atualização neste arquivo no mesmo PR.

Checklist ao adicionar endpoint:

- [ ] Método HTTP, path e parâmetros documentados
- [ ] Schema de request e response com exemplo real
- [ ] Autenticação necessária indicada
- [ ] Campos com valores especiais ou limitações explicados
- [ ] Se vier do D1: coluna correspondente na tabela documentada
- [ ] Se for stub: indicado na seção "Endpoints planejados"
