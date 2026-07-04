# Schema de Eventos GA4 — SignallQ Android

> SIG-134 — instrumentação GA4 para alimentar ProductAnalyticsPage no Admin Panel.

## Visão Geral

Eventos Firebase Analytics (GA4) enviados pelo app Android. Sem PII — todos os identificadores são anônimos por sessão de processo.

`session_id` é um UUID gerado uma vez por instância do processo (`FirebaseAnalyticsTracker`). Não persiste entre sessões de app.

---

## Eventos

### `feature_used`

Disparo: quando o usuário aciona uma feature principal.

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `feature_id` | String | Identificador da feature: `speedtest`, `diagnostico`, `wifi`, `historico`, `dns`, `fibra` |
| `session_id` | String | UUID da sessão atual |
| `app_version` | String | `BuildConfig.VERSION_NAME` |
| `timestamp` | Long | `System.currentTimeMillis()` |

Pontos de disparo:
- `speedtest` — `MainActivity.speedtestViewModel.onSpeedtestConcluido`
- `diagnostico` — `MainActivity.AppShellDiagnosticoState.onIniciarDiagnostico`
- `wifi` — `MainActivity.AppShellWifiState.onRefreshSinal`
- `dns` — `MainActivity.onDispararBenchmarkDns`
- `fibra` — `MainActivity.onReconectarFibra`
- `historico` — `MainActivity.onFiltroConexaoHistoricoChange` / `onFiltroOperadoraHistoricoChange`

---

### `screen_view`

Disparo: ao navegar entre as 5 abas do `AppShell`.

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `screen_name` | String | `home`, `speedtest`, `sinal_wifi`, `historico`, `ajustes` |
| `session_id` | String | UUID da sessão atual |
| `app_version` | String | `BuildConfig.VERSION_NAME` |

Ponto de disparo: `LaunchedEffect(selectedTab)` em `AppShell.kt`, via callback `onScreenView`.

---

### `app_session_start`

Disparo: `MainActivity.onCreate()`.

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `session_id` | String | UUID da sessão atual |
| `app_version` | String | `BuildConfig.VERSION_NAME` |

---

### `feature_crash`

Disparo: erros de nível ERROR+ capturados pelo `ReleaseTree` (Timber, builds release).

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `feature_id` | String | Derivado da tag Timber (ex: `speedtest`, `diagnostico`) |
| `error_type` | String | `t.javaClass.simpleName` ou `"LoggedError"` |
| `app_version` | String | `BuildConfig.VERSION_NAME` |

Nota: o mesmo evento é enviado ao Crashlytics via `FirebaseCrashlytics.recordException`.

---

### `battery_snapshot`

Disparo: `MainActivity.onCreate()`, via `ACTION_BATTERY_CHANGED` (sticky broadcast).

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `level` | Int | Percentual de bateria (0–100) |
| `charging` | Boolean | `true` se carregando ou com bateria cheia |
| `session_id` | String | UUID da sessão atual |

---

## Arquitetura

```
:coreNetwork
  AnalyticsTracker (interface)

:app
  FirebaseAnalyticsTracker (implementação @Singleton)
  AppModule.provideAnalyticsTracker() → bind interface → impl
  AppModule.provideFirebaseAnalytics() → FirebaseAnalytics.getInstance(ctx)

  Pontos de injeção:
  - SignallQApplication (@Inject) → ReleaseTree(analyticsTracker)
  - MainActivity (@Inject) → app_session_start, battery_snapshot, screen_view, feature_used
```

Os módulos `:feature*` não dependem de Firebase diretamente — se precisarem registrar eventos, receberão `AnalyticsTracker` via Hilt respeitando a lei de dependências (`:feature*` → `:core*` apenas).

---

## Funil principal (SIG-155)

Todas as 6 features do `feature_id` documentadas acima já disparam `feature_used`
ao serem acionadas pelo usuário:

```
speedtest  → MainActivity.speedtestViewModel.onSpeedtestConcluido
diagnostico → AppShellDiagnosticoState.onIniciarDiagnostico
wifi       → AppShellWifiState.onRefreshSinal
dns        → onDispararBenchmarkDns
fibra      → onReconectarFibra
historico  → onFiltroConexaoHistoricoChange / onFiltroOperadoraHistoricoChange
```

Combinado com `screen_view` (navegação entre abas) e `app_session_start`, esse
conjunto permite montar o funil de engajamento no GA4/Admin Panel: quantas
sessões tocam cada feature principal a partir da abertura do app.

Nota: `docs_ai/technical/analytics-events.md` descreve um contrato mais amplo
e granular (eventos `speedtest_iniciado`, `diag_concluido`, `ia_laudo_*` etc.)
que ainda não foi implementado — trata-se de uma proposta de evolução futura,
não do estado atual. O estado atual é o descrito neste arquivo.

---

## Feature Flags — Endpoint /flags (SIG-13)

As feature flags remotas são buscadas pelo `FeatureFlagRepository` em dois endpoints:

| Endpoint | Schema | Uso |
|---|---|---|
| `GET /flags` | `{flags:[{key,enabled}]}` | Flags de produto (SIG-13) |
| `GET /feature-flags` | `{flags:[{key,enabled,scope}]}` | Flags legadas (mantidas por compatibilidade) |

Keys do `/flags` (SIG-13): `feature_speedtest`, `feature_wifi`, `feature_diagnostico_ia`, `feature_dns`, `feature_fibra`, `feature_devices`.

Métodos de conveniência em `FeatureFlagProvider`: `isFeatureSpeedtestEnabled()`, `isFeatureWifiEnabled()`, etc.
