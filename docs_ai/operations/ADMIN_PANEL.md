# SignallQ Admin Panel — Documentação Operacional

**Última atualização:** 2026-06-23 (fase SIG-143/136/132/125)
**Fonte:** estado real do painel, worker, D1 e decisões do Luiz
**Responsável:** Felipe (React/Vite), Gema + Camilo (Android), Luiz (decisões)

---

## 1. Estado Atual — Telas e Fontes de Dados

O painel admin (Cloudflare Pages + React/Vite) expõe 5 telas principais. Todas validam sessão via `admin_sessions` D1.

### 1.1 — Login

**Rota:** `/login`

**Fluxo:**
1. Usuário digita email + senha
2. POST `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev/admin/auth/login`
3. Worker valida contra `admin_users` D1 (hash PBKDF2-HMAC-SHA256)
4. Se válido: cria sessão em `admin_sessions`, seta cookie `httpOnly; Secure; SameSite=None`
5. Redireciona para `/dashboard`

**Fonte de dados:** `admin_users` (email, password_hash, role, active, last_login)

**Status:** SIG-136 entrega (Felipe). Manuais do Luiz em seção 7.

---

### 1.2 — Dashboard

**Rota:** `/dashboard`

**Componentes:**
- **Estatísticas gerais:** usuários únicos, mensurações ingeridas, diagnósticos IA, taxa de erro
- **Gráficos:** ingestão por dia (últimos 7), diagnósticos por modelo, erros por tipo
- **Alerts:** regressões detectadas (ex: taxa de erro > 5%)

**Fonte de dados:** `d1_ingest_events` (agregações via SQL) + `d1_ai_diagnostics` (contadores)

**Query padrão:** 
```sql
SELECT 
  DATE(created_at) as data,
  COUNT(*) as total_ingestoes,
  COUNT(CASE WHEN tipo='mensuracao' THEN 1 END) as medicoes,
  COUNT(CASE WHEN tipo='diagnostico' THEN 1 END) as diagnosticos,
  COUNT(CASE WHEN erro IS NOT NULL THEN 1 END) as erros
FROM d1_ingest_events
WHERE created_at >= datetime('now', '-7 days')
GROUP BY DATE(created_at)
ORDER BY data DESC;
```

**Status:** SIG-143 entrega (Felipe). Schema tabelas em seção 2.

---

### 1.3 — Device Ingestion

**Rota:** `/devices`

**Componentes:**
- **Tabela:** device_id, app_version, sdk_version, brand, model, storage_free, last_ingest (últimas 50)
- **Filtros:** app_version, sdk_version, brand
- **Ações:** copy device_id, visualizar diagrama de ações do dispositivo

**Fonte de dados:** `d1_device_info` (ingestão via `/ingest/device-info`)

**Campos:**
- device_id (TEXT PRIMARY KEY)
- app_version (TEXT)
- sdk_version (INTEGER)
- brand (TEXT)
- model (TEXT)
- storage_free (INTEGER, bytes)
- battery_pct (INTEGER)
- last_ingest (INTEGER, unix timestamp)
- ingest_count (INTEGER)

**Status:** SIG-143 entrega (Felipe). Endpoint `/ingest/device-info` em SIG-132 (Camilo).

---

### 1.4 — Feature Flags

**Rota:** `/feature-flags`

**Componentes:**
- **Tabela:** flag_name, status (ON/OFF), description, last_modified, last_modified_by
- **Ações:** toggle flag, histórico de mudanças, audit log

**Fonte de dados:** `d1_feature_flags` (leitura/escrita), `d1_feature_flags_audit` (changelog)

**Campos `d1_feature_flags`:**
- flag_name (TEXT PRIMARY KEY) — ex: `ai_diagnosis_enabled`, `speedtest_enabled`
- enabled (INTEGER, 0 ou 1)
- description (TEXT)
- last_modified (INTEGER, unix timestamp)
- last_modified_by (TEXT, email do admin)

**Flags definidas:**
- `ai_diagnosis_enabled` — ativa/desativa o worker de IA (fallback: diagnóstico local)
- `speedtest_enabled` — ativa/desativa o módulo de speedtest
- `fibra_module_enabled` — ativa/desativa a tela de Fibra
- `new_ui_diagnostics` — toggle de novo design na tela de Diagnóstico
- (mais flags podem ser adicionadas conforme feature)

**Fluxo Android:**
1. App inicia → FeatureFlagManager carrega flags via `GET /admin/feature-flags?device_id=X`
2. Retorna JSON com status de cada flag
3. App cache em DataStore com TTL 1h
4. Mudança no painel → próximo app que chamar o endpoint vê a flag atualizada

**Status:** SIG-133 entrega (Felipe painel), SIG-125 entrega (Camilo Android FeatureFlagManager).

---

### 1.5 — Operators (Operadoras)

**Rota:** `/operators`

**Componentes:**
- **Tabela:** operator_name, country_code, region, mcc/mnc, active, last_updated
- **Ações:** marcar como ativa/inativa, editar dados de região/suporte

**Fonte de dados:** `d1_operators` (ingestão em SIG-139)

**Campos:**
- operator_id (TEXT PRIMARY KEY)
- name (TEXT)
- country_code (TEXT, ex: 'BR')
- mcc (INTEGER) — Mobile Country Code
- mnc (INTEGER) — Mobile Network Code
- region (TEXT, ex: 'north', 'northeast')
- active (INTEGER)
- last_updated (INTEGER)

**Uso Android:** app identifica operadora via TelephonyManager.getNetworkOperator() → match com MCC/MNC → diagnostico sensível à operadora (latência esperada, tipo de rede).

**Status:** SIG-139 entrega (Camilo ingest), painel em SIG-132 (Felipe).

---

### 1.6 — Error Pipeline (Diagnóstico de Erros)

**Rota:** `/errors`

**Componentes:**
- **Tabela:** error_id, type, message, device_id, app_version, timestamp, resolved
- **Filtros:** type, app_version, date range
- **Ações:** marcar como resolvido, agrupar erros similares, exportar log

**Fonte de dados:** `d1_ai_errors` (ingestão em SIG-135 Fase A)

**Campos:**
- error_id (TEXT PRIMARY KEY, uuid)
- error_type (TEXT, ex: 'worker_timeout', 'network_unreachable', 'parse_error')
- message (TEXT)
- device_id (TEXT)
- app_version (TEXT)
- timestamp (INTEGER)
- resolved (INTEGER, 0 ou 1)
- error_context (JSON) — traceback/contexto

**Pipeline (SIG-135 Fase A):**
1. App chama worker (IA) e falha
2. App ingest erros via POST `/ingest/ai-error`
3. Worker armazena em `d1_ai_errors`
4. Painel exibe erros, dev marca como resolvido (mitiga falso positivo)

**Status:** SIG-135 Fase A entrega (Camilo). Painel em SIG-132 (Felipe).

---

### 1.7 — Settings (Ajustes Gerais)

**Rota:** `/settings`

**Componentes:**
- **Chaves globais:** maintenance_mode (bool), rate_limit (req/s por device), log_level (debug/info/warn)
- **E-mail de suporte:** para enviar notificação de degradação
- **Teste de conexão ao worker:** botão `Test AI Worker`

**Fonte de dados:** `d1_settings` (key-value simples)

**Campos:**
- setting_key (TEXT PRIMARY KEY)
- value (TEXT) — JSON-encoded se complexo
- last_updated (INTEGER)
- updated_by (TEXT, email admin)

**Status:** SIG-132 entrega (Felipe).

---

## 2. Schema D1 Completo (Estado Real)

Database: **`signallq-admin-db`** (Cloudflare D1, grátis na zona gmmattey-luiz.workers.dev)

### 2.1 — Autenticação (SIG-136)

```sql
CREATE TABLE IF NOT EXISTS admin_users (
  id            TEXT    PRIMARY KEY,
  email         TEXT    NOT NULL UNIQUE,
  password_hash TEXT    NOT NULL,
  role          TEXT    NOT NULL DEFAULT 'admin',
  active        INTEGER NOT NULL DEFAULT 1,
  created_at    INTEGER NOT NULL,
  last_login    INTEGER
);

CREATE TABLE IF NOT EXISTS admin_sessions (
  token_hash  TEXT    PRIMARY KEY,
  user_id     TEXT    NOT NULL,
  created_at  INTEGER NOT NULL,
  expires_at  INTEGER NOT NULL,
  last_seen   INTEGER NOT NULL,
  FOREIGN KEY (user_id) REFERENCES admin_users(id)
);

CREATE INDEX IF NOT EXISTS idx_admin_sessions_expires 
  ON admin_sessions(expires_at);
```

Vide `docs_ai/operations/ADMIN_AUTH.md` para detalhe de hashing (PBKDF2) e pepper.

---

### 2.2 — Ingestão de Dados (SIG-143, SIG-139, SIG-138, SIG-135 Fase A)

```sql
-- Eventos de ingestão (app Android, endpoint /ingest/*)
CREATE TABLE IF NOT EXISTS d1_ingest_events (
  event_id      TEXT    PRIMARY KEY,
  device_id     TEXT    NOT NULL,
  tipo          TEXT    NOT NULL,
  payload       TEXT    NOT NULL,
  erro          TEXT,
  created_at    INTEGER NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_ingest_device 
  ON d1_ingest_events(device_id);
CREATE INDEX IF NOT EXISTS idx_ingest_created 
  ON d1_ingest_events(created_at);

-- Informações de dispositivo (ingestão SIG-132)
CREATE TABLE IF NOT EXISTS d1_device_info (
  device_id     TEXT    PRIMARY KEY,
  app_version   TEXT,
  sdk_version   INTEGER,
  brand         TEXT,
  model         TEXT,
  storage_free  INTEGER,
  battery_pct   INTEGER,
  last_ingest   INTEGER NOT NULL,
  ingest_count  INTEGER DEFAULT 0
);

-- Diagnósticos IA (saída do worker, armazenamento em D1)
CREATE TABLE IF NOT EXISTS d1_ai_diagnostics (
  diagnosis_id   TEXT    PRIMARY KEY,
  device_id      TEXT    NOT NULL,
  app_version    TEXT,
  timestamp      INTEGER NOT NULL,
  model_used     TEXT,
  inference_ms   INTEGER,
  tokens_input   INTEGER,
  tokens_output  INTEGER,
  result_json    TEXT
);

CREATE INDEX IF NOT EXISTS idx_ai_diag_device 
  ON d1_ai_diagnostics(device_id);
CREATE INDEX IF NOT EXISTS idx_ai_diag_timestamp 
  ON d1_ai_diagnostics(timestamp);

-- Erros de IA (pipeline SIG-135 Fase A)
CREATE TABLE IF NOT EXISTS d1_ai_errors (
  error_id      TEXT    PRIMARY KEY,
  error_type    TEXT    NOT NULL,
  message       TEXT,
  device_id     TEXT,
  app_version   TEXT,
  timestamp     INTEGER NOT NULL,
  resolved      INTEGER DEFAULT 0,
  error_context TEXT
);

-- Operadoras (ingestão SIG-139)
CREATE TABLE IF NOT EXISTS d1_operators (
  operator_id   TEXT    PRIMARY KEY,
  name          TEXT    NOT NULL,
  country_code  TEXT,
  mcc           INTEGER,
  mnc           INTEGER,
  region        TEXT,
  active        INTEGER DEFAULT 1,
  last_updated  INTEGER
);
```

---

### 2.3 — Feature Flags (SIG-133)

```sql
CREATE TABLE IF NOT EXISTS d1_feature_flags (
  flag_name         TEXT    PRIMARY KEY,
  enabled           INTEGER NOT NULL DEFAULT 0,
  description       TEXT,
  last_modified     INTEGER NOT NULL,
  last_modified_by  TEXT
);

CREATE TABLE IF NOT EXISTS d1_feature_flags_audit (
  audit_id         TEXT    PRIMARY KEY,
  flag_name        TEXT    NOT NULL,
  old_value        INTEGER,
  new_value        INTEGER,
  modified_by      TEXT,
  modified_at      INTEGER NOT NULL
);
```

---

### 2.4 — Settings (SIG-132)

```sql
CREATE TABLE IF NOT EXISTS d1_settings (
  setting_key   TEXT    PRIMARY KEY,
  value         TEXT,
  last_updated  INTEGER,
  updated_by    TEXT
);
```

---

## 3. Endpoints do Worker

**Base URL:** `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev`

### 3.1 — Autenticação (SIG-136)

| Rota | Método | Auth | Função |
|---|---|---|---|
| `/admin/auth/login` | POST | — | Login (email + senha) → cookie sessão |
| `/admin/auth/logout` | POST | sessão | Logout (apaga sessão) |
| `/admin/auth/me` | GET | sessão | Dados do usuário logado |
| `/admin/auth/users` | POST | sessão + role=admin | Criar novo usuário |
| `/admin/auth/password` | POST | sessão | Trocar própria senha |
| `/admin/auth/users/:id/reset` | POST | sessão + role=admin | Resetar senha de outro usuário |

---

### 3.2 — Ingestão (SIG-143, SIG-139, SIG-138, SIG-135 Fase A)

Todos exigem `INGEST_KEY` (secret no worker, **não muda**).

| Rota | Método | Payload | Função |
|---|---|---|---|
| `/ingest/mensuracao` | POST | `{device_id, timestamp, latency_ms, ...}` | Ingest de medição |
| `/ingest/device-info` | POST | `{device_id, brand, model, app_version, ...}` | Ingest de info de dispositivo (SIG-132) |
| `/ingest/operadora` | POST | `{device_id, mcc, mnc, ...}` | Ingest de dados de operadora (SIG-139) |
| `/ingest/ai-error` | POST | `{device_id, error_type, message, ...}` | Ingest de erro IA (SIG-135 Fase A) |

---

### 3.3 — Admin (Painel)

Todos exigem sessão válida (cookie `httpOnly`).

| Rota | Método | Auth | Função |
|---|---|---|---|
| `/admin/metrics/dashboard` | GET | sessão | Estatísticas gerais |
| `/admin/devices` | GET | sessão | Lista de dispositivos |
| `/admin/feature-flags` | GET | sessão | Lista flags |
| `/admin/feature-flags/:name/toggle` | POST | sessão + role=admin | Ativa/desativa flag |
| `/admin/operators` | GET | sessão | Lista operadoras |
| `/admin/errors` | GET | sessão | Lista erros |
| `/admin/settings` | GET\|POST | sessão + role=admin | Lê/escreve settings |

---

### 3.4 — Feature Flags (Android)

Público (sem autenticação), rate-limitado por device_id.

| Rota | Método | Query | Função |
|---|---|---|---|
| `/admin/feature-flags` | GET | `?device_id=X` | Retorna flags para o device |

**Response:**
```json
{
  "flags": {
    "ai_diagnosis_enabled": true,
    "speedtest_enabled": true,
    "fibra_module_enabled": false,
    "new_ui_diagnostics": true
  },
  "version": 3,
  "cached_seconds": 3600
}
```

---

## 4. Etapas Manuais do Luiz

**Responsabilidade:** executar scripts / comandos únicos para bootstrap e setup.

### 4.1 — Setup Autenticação (SIG-136)

**Quando:** após merge de SIG-136 (painel + worker)

**Pré-requisito:** ter `wrangler` CLI instalado e credenciais Cloudflare configuradas

**Passos:**

1. **Gerar pepper (secret):**
   ```bash
   openssl rand -hex 32
   # Output: ex: a1b2c3d4e5f6g7h8i9j0k1l2m3n4o5p6
   ```

2. **Configurar pepper no worker:**
   ```bash
   cd C:\Projetos\Linka Android\integrations\cloudflare\signallq-admin-worker
   npx wrangler secret put ADMIN_AUTH_PEPPER
   # Colar o valor gerado acima e confirmar
   ```

3. **Gerar hash de senha do admin inicial:**
   - Felipe entrega script `scripts/generate-admin-hash.mjs` no worker
   - Rodar localmente:
     ```bash
     cd integrations/cloudflare/signallq-admin-worker
     node scripts/generate-admin-hash.mjs
     # Prompt: email (ex: giammattey.luiz@gmail.com), senha, pepper
     # Output: hash (ex: pbkdf2$150000$...base64...)
     ```

4. **Inserir primeiro usuário no D1:**
   ```bash
   npx wrangler d1 execute signallq-admin-db --remote << 'EOF'
   INSERT INTO admin_users (id, email, password_hash, role, active, created_at)
   VALUES (
     'uuid-aqui',
     'giammattey.luiz@gmail.com',
     'pbkdf2$150000$...hash...',
     'admin',
     1,
     strftime('%s', 'now')
   );
   EOF
   ```

5. **Confirmar login:** abrir `https://signallq-admin-panel.pages.dev/login`, digitar email e senha.

---

### 4.2 — Deploy Painel (SIG-142)

**Quando:** após merge de SIG-142 (painel React/Vite pronto, feature flags visíveis)

**Pré-requisito:** GitHub conectado ao Cloudflare Pages

**Passos:**

1. **Conectar repositório GitHub ao Cloudflare Pages:**
   - Ir para Cloudflare Dashboard → Pages
   - Clique "Create a project" → Connect to Git
   - Authorize GitHub (Luiz como owner)
   - Selecionar repo `7ALabs/linka-android`
   - Build command: `npm run build`
   - Build output dir: `dist`
   - Variável de ambiente: `VITE_ENABLE_MOCKS=false` (desativa mocks em produção)
   - Deploy

2. **Confirmar deploy:** abrir URL pública do painel (produção real: `signallq-admin-panel.pages.dev` —
   confirmado em 2026-07-17 via `SignallQ Admin/tests/README.md`; não confundir com
   `signallq.pages.dev/console/`, legada e desativada desde 2026-07-16), logar, ver dashboard.

---

### 4.3 — BigQuery Sync (SIG-128) — **BLOQUEADO**

**Status:** AGUARDA DECISÃO DO LUIZ

**O quê:** enviar diagnósticos IA do D1 para BigQuery em tempo real (para análise de ML, retrainamento).

**Por quê:** melhorar diagnóstico com feedback de usuário real.

**Bloqueio:** requer Google Cloud credentials, habilitação de APIs, eventual custo (beyond free tier).

**Decisão necessária:** 
- Habilitar BigQuery e credenciais?
- Quando?
- Custo esperado?

Vide SIG-128 no Linear para contexto completo.

---

### 4.4 — Firebase Sync (SIG-130) — **BLOQUEADO**

**Status:** AGUARDA DECISÃO DO LUIZ

**O quê:** enviar ingestão de dados (device_info, diagnósticos) também para Firestore em tempo real (redundância, backup, análise em Firebase console).

**Por quê:** visibilidade no Firebase console sem montar painel separado; backup automático.

**Bloqueio:** requer habilitação de Firestore, eventual custo beyond free tier.

**Decisão necessária:**
- Habilitar Firebase Firestore?
- Quando?
- Custo esperado?

Vide SIG-130 no Linear para contexto completo.

---

## 5. Dependency Graph — O que depende de quê

```
SIG-136 (auth própria)
  ↓
SIG-143 (dashboard + stats)
  ↓
SIG-132 (device_info, operators, errors, settings, feature flags)
  ← SIG-139 (ingest operadoras)
  ← SIG-138 (ingest device_info)
  ← SIG-135 Fase A (ingest ai_errors)
  
SIG-133 (feature flags UI painel)
  ↓
SIG-125 (FeatureFlagManager Android)

SIG-142 (deploy painel em Cloudflare Pages)
  ← SIG-143 + SIG-132 + SIG-133 prontos

[BLOQUEADO] SIG-128 (BigQuery)
[BLOQUEADO] SIG-130 (Firebase Firestore)
[BLOQUEADO] SIG-134 (Mobile UX de Feature Flags)
```

---

## 6. Decisões Registradas

- **Auth:** usar D1 próprio em vez de Cloudflare Access (controle total, custo zero). Vide `ADMIN_AUTH.md`.
- **Armazenamento:** D1 SQLite (sem custo na free zone).
- **Pepper:** secret armazenado em `wrangler secrets` (não commitado).
- **Sessão:** httpOnly cookie com Secure + SameSite=None (mitigação XSS/CSRF, suporta cross-origin Pages ↔ Workers).
- **Painel:** Cloudflare Pages + React/Vite (deploy automático via GitHub, no mesmo tenant).
- **Feature flags:** endpoint público com cache 1h no Android (reduz latência, suporta offline).

---

## 7. Referências

- `docs_ai/operations/ADMIN_AUTH.md` — detalhe de autenticação, hashing, sessão
- `docs_ai/functional/FEATURE_FLAGS.md` — guia de uso de feature flags (Android + painel)
- `ADMIN_PANEL.md` — este documento
- SIG-136, SIG-143, SIG-132, SIG-125, SIG-133, SIG-139, SIG-138, SIG-135 Fase A no Linear
- SIG-128, SIG-130, SIG-134, SIG-142 — bloqueadas ou em discussão
