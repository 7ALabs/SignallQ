# Feature Flags — SignallQ Android + Admin Panel

**Última atualização:** 2026-06-23 (SIG-133, SIG-125)
**Fonte:** especificação de design e código
**Responsável:** Felipe (painel), Camilo (Android), Gema (review)

---

## 1. O que são Feature Flags?

Feature flag é um interruptor remoto e em tempo real que ativa/desativa funcionalidades no app sem precisar de nova versão.

**Casos de uso:**
- Rollout gradual de feature (ex: liberar fibra module para 10% dos users)
- Desabilitar funcionalidade com problema (ex: worker IA caiu → desabilitar diagnóstico IA)
- A/B testing (ex: ligar novo UI diagnóstico para um slice de devices)
- Manutenção (ex: maintenance_mode = true → app mostra "em manutenção")

**Fluxo:**
1. Dev seta flag no painel admin (ex: `ai_diagnosis_enabled = OFF`)
2. App faz polling do endpoint `/admin/feature-flags?device_id=X`
3. FeatureFlagManager em Android ativa/desativa componente/fluxo
4. Sem deploy, sem rollout Play Store, instantâneo

---

## 2. Arquitetura

### 2.1 — Worker + D1

Flags armazenadas em `d1_feature_flags` (D1 Cloudflare):

```sql
CREATE TABLE IF NOT EXISTS d1_feature_flags (
  flag_name         TEXT    PRIMARY KEY,
  enabled           INTEGER NOT NULL DEFAULT 0,
  description       TEXT,
  last_modified     INTEGER NOT NULL,
  last_modified_by  TEXT
);
```

**Endpoint público (Android):**
```
GET /admin/feature-flags?device_id=abc123
```

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

**Proteção:** rate-limit por device_id (ex: máx 1 req/5min por device).

---

### 2.2 — Admin Panel (Painel)

Tela `/feature-flags` (SIG-133):

- **Input:** tabela com flags existentes
- **Ação:** toggle (ON/OFF), edit description
- **Audit:** log de quem mudou e quando
- **HTTP:** POST `/admin/feature-flags/:name/toggle` (requer sessão + role=admin)

**Screenshot (conceitual):**
```
┌─────────────────────────────────────────────┐
│ Feature Flags                               │
├─────────────────────────────────────────────┤
│ Flag Name                  Status   Modified │
├─────────────────────────────────────────────┤
│ ai_diagnosis_enabled       ✓ ON   2026-06-23│
│ speedtest_enabled          ✓ ON   2026-06-23│
│ fibra_module_enabled       ✗ OFF  2026-06-22│
│ new_ui_diagnostics         ✓ ON   2026-06-23│
└─────────────────────────────────────────────┘
```

---

### 2.3 — Android FeatureFlagManager (SIG-125)

**Módulo:** `:featureSettings` ou `:app` (a decidir por Gema)

**Responsabilidade:**
- Fazer fetch periódico de flags (60s debounced)
- Cachear em DataStore com TTL 1h
- Expor StateFlow<Map<String, Boolean>>
- Integrar com Hilt (singleton)

**Pseudocódigo:**

```kotlin
// AppModule.kt ou FeatureSettingsModule.kt
@Singleton
class FeatureFlagManager @Inject constructor(
    private val apiClient: OkHttpClient,
    private val deviceId: String,  // injected from BuildConfig ou context
    private val dataStore: DataStore<PreferenciasApp>,
    private val dispatcher: DispatcherProvider
) {
    
    private val _flags = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val flags: StateFlow<Map<String, Boolean>> = _flags.asStateFlow()
    
    suspend fun refreshFlags() {
        try {
            val response = apiClient.newCall(
                Request.Builder()
                    .url("${BuildConfig.ADMIN_WORKER_URL}/admin/feature-flags?device_id=$deviceId")
                    .get()
                    .build()
            ).execute()
            
            if (response.isSuccessful) {
                val json = JSONObject(response.body?.string() ?: "{}")
                val flagMap = json.optJSONObject("flags")?.let {
                    mutableMapOf<String, Boolean>().apply {
                        it.keys().forEach { key ->
                            put(key, it.optBoolean(key, false))
                        }
                    }
                } ?: emptyMap()
                
                _flags.value = flagMap
                // cache em DataStore com timestamp
                dataStore.updateData { prefs ->
                    prefs.copy(featureFlagsJson = flagMap.toString())
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Falha ao carregar feature flags")
            // fallback: usar cache se disponível
        }
    }
    
    fun isEnabled(flagName: String, default: Boolean = false): Boolean {
        return _flags.value[flagName] ?: default
    }
}
```

**Injeção nas telas:**

```kotlin
// DiagnosticoScreen.kt
@Composable
fun DiagnosticoScreen(
    viewModel: DiagnosticoViewModel
) {
    val flags by viewModel.featureFlagManager.flags.collectAsState()
    val aiEnabled = flags["ai_diagnosis_enabled"] ?: true
    
    if (aiEnabled) {
        // renderiza diagnóstico IA normal
    } else {
        // fallback: diagnóstico local ou placeholder
    }
}
```

---

## 3. Flags Definidas

| Flag Name | Descrição | Default | Entrega | Uso |
|---|---|---|---|---|
| `ai_diagnosis_enabled` | Ativa diagnóstico IA (worker) | true | SIG-125 | Fallback: diagnóstico local |
| `speedtest_enabled` | Ativa módulo speedtest | true | SIG-125 | Fallback: ocultar abá |
| `fibra_module_enabled` | Ativa tela de Fibra | false | SIG-134 | M3 (launch) ou depois |
| `new_ui_diagnostics` | Novo design diagnóstico | false | SIG-125 | A/B test ou rollout gradual |

---

## 4. Como Adicionar uma Nova Flag

### Passo 1: Definir no Painel (Felipe)

**Arquivo:** `integrations/cloudflare/signallq-admin-worker/schema.sql`

```sql
-- Adicionar linha com INSERT no seed/migration:
INSERT INTO d1_feature_flags (flag_name, enabled, description, last_modified, last_modified_by)
VALUES ('nova_feature', 0, 'Descrição da nova feature', 1719086400, 'setup');
```

---

### Passo 2: Integrar no Android (Camilo)

**Arquivo:** `app/src/main/kotlin/.../DiagnosticoScreen.kt` (ou tela afetada)

```kotlin
@Composable
fun DiagnosticoScreen(
    viewModel: DiagnosticoViewModel
) {
    val flags by viewModel.featureFlagManager.flags.collectAsState()
    val novaFeatureEnabled = flags["nova_feature"] ?: false
    
    if (novaFeatureEnabled) {
        NovaFeatureUI()
    } else {
        FeatureAntiga()
    }
}
```

---

### Passo 3: Deploy + Teste

1. **Painel:** merge SIG-132 (Felipe), flag aparece no `/feature-flags`
2. **Android:** merge código acima, build + upload Firebase (sem feature ativada no painel ainda)
3. **Validar:**
   - App faz fetch de flags → vê `nova_feature = false`
   - Dev ativa no painel: `/admin/feature-flags/nova_feature/toggle`
   - App faz refresh (próximo polling) → vê `nova_feature = true` → UI muda
   - Log: `Timber.d("Feature flag nova_feature: true")`

---

## 5. Ciclo de Vida — Rollout Gradual (Exemplo)

**Cenário:** lançar `fibra_module_enabled` para 10% dos users, depois 100%.

### Semana 1: Canary (10%)

1. Dev ativa flag no painel
2. Android já tem código (desativado por default)
3. 10% dos devices verão "Fibra" na navegação (ou conforme segmentação)
4. Monitor erros/crashes em `/admin/errors`

### Semana 2: Ramp-up (50%)

- Sem mudança de código
- Admin muda apenas no painel
- `fibra_module_enabled` ligada para mais devices

### Semana 3: GA (100%)

- Feature completamente ligada
- Considerar remover o flag depois (cleanup — usar SIG-134)

---

## 6. Rate-Limiting e Cache

**Estratégia:**

```
App Request
  ├─ Último fetch < 1h?
  │   └─ SIM → usa DataStore cache
  │   └─ NÃO → faz HTTP GET /admin/feature-flags?device_id=X
  │
  └─ Worker
      ├─ Rate limit por device: 1 req / 5 min (armazena em KV)
      ├─ Retorna JSON com 200
      └─ TTL sugerido: 3600s (app cache)
```

**Por quê:**
- Reduz latência (cache local)
- Reduz carga no worker (batching)
- Offline-aware (funciona sem rede se houver cache)

---

## 7. Auditoria e Segurança

### Audit Log (SIG-132)

Toda mudança em flag é registrada em `d1_feature_flags_audit`:

```sql
CREATE TABLE IF NOT EXISTS d1_feature_flags_audit (
  audit_id         TEXT    PRIMARY KEY,
  flag_name        TEXT    NOT NULL,
  old_value        INTEGER,
  new_value        INTEGER,
  modified_by      TEXT,
  modified_at      INTEGER NOT NULL
);
```

**Painel exibe:** histórico de mudanças (quem, quando, de → para).

### Segurança

- **Endpoint de leitura (`GET /admin/feature-flags`):** público, rate-limited por device_id
- **Endpoint de escrita (`POST /admin/feature-flags/:name/toggle`):** requer sessão + role=admin
- **Sem tokens em QS:** usar POST + body ou cookie sessão

---

## 8. Troubleshooting

### App não vê flag atualizada

1. **Checar cache:** DataStore pode estar cachando com TTL vencido
   ```kotlin
   // Force refresh:
   viewModel.featureFlagManager.refreshFlags()
   ```

2. **Checar device_id:** certificar que device_id no request é único/consistente
   ```kotlin
   Timber.d("Device ID: ${BuildConfig.DEVICE_ID}")
   ```

3. **Verificar painel:** abrir `/admin/feature-flags` e confirmar que flag está lá e com valor esperado

### Painel não consegue ativar flag

1. **Sessão expirou:** re-logar
2. **Role errada:** certificar que usuário tem `role='admin'` em `admin_users` (não 'viewer')
3. **Worker error:** checar logs do worker em Cloudflare Dashboard → Workers → linka-ai-diagnosis-worker → Logs

---

## 9. Referências

- `docs_ai/operations/ADMIN_PANEL.md` — visão geral do painel admin, schema D1
- `docs_ai/operations/ADMIN_AUTH.md` — autenticação e segurança
- `docs_ai/functional/SETTINGS.md` — preferências do usuário (diferente de feature flags globais)
- SIG-125 (Android FeatureFlagManager), SIG-133 (painel), SIG-132 (endpoints ingest) no Linear
