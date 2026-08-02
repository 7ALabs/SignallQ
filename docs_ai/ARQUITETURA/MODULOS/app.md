# Módulo :app

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/app/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:app` (pasta física `android/app/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Composição entre features, navegação, DI de nível de aplicação (Hilt `AppModule`), único
`ViewModel` raiz do app (`MainViewModel`), telas de UI, integrações que dependem de múltiplas
features (feature flags, notificações, monitoramento em background, chamadas aos Workers Cloudflare
de ingest/probe). Namespace/applicationId: `io.signallq.app`.

## Diagrama de componentes

```
:app
├── ui/screen/      — 15+ telas Composable (Home, SpeedTest, Sinal, Historico, Dispositivos,
│                      Ajustes, Velocidade, ResultadoVelocidade, Diagnostico, LLMChat, Fibra,
│                      Laudo, Privacidade, Novidades, Onboarding)
├── ui/viewmodel/   — ChatDiagnosticoIaViewModel
├── di/             — AppModule.kt (Hilt, provê todas as dependências injetadas)
├── pulse/          — SignallQOrchestrator.kt (fluxo Chat/Pulse)
├── monitoramento/  — MonitoramentoWorker.kt, MonitoramentoScheduler.kt
├── notificacao/    — SignallQNotificationHelper.kt
└── speedtest/      — SpeedtestPersistenceCoordinator.kt
```

Base física: `app/src/main/kotlin/io/veloo/app/kotlin/` (caminho legado — ver dívida 4.1 da regra
de higiene; package declarado é `io.signallq.app`).

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `SignallQApplication.kt` | `Application` | Inicialização, Hilt, canais de notificação |
| `MainActivity.kt` | `Activity` | Entry point único — `setContent { SignallQTheme { AppShell(...) } }` |
| `MainViewModel.kt` | `@HiltViewModel` | Orquestra todos os serviços e expõe `StateFlow`s — **2285 linhas**, acima do limiar de "dívida crítica" (seção 7 da regra de higiene) |
| `FeatureFlags.kt` | Object | Controle de `buildConfigField` booleanos por build type |
| `AppShell.kt` | Composable | Shell — `NavigationBar` 5 abas + fluxos sobrepostos — **1241 linhas**, acima do limiar de extração obrigatória |
| `AppNavGraph.kt` | `NavHost` | Rotas das 5 abas principais |
| `SignallQTheme.kt` | Composable | Tema MD3 |
| `AjustesScreen.kt` | Composable | Overlay Perfil/Ajustes — **803 linhas** |

## Fluxo de dados principal

- **Entradas:** eventos de UI, callbacks do SO (`ConnectivityManager`, `TelephonyManager`,
  `WifiManager`), WorkManager (execução periódica do `MonitoramentoWorker`).
- **Saídas:** persistência em Room/DataStore, notificações via `NotificationManager`, POST HTTP aos
  Workers Cloudflare (`ADMIN_INGEST_URL`, `GAME_LATENCY_PROBE_URL`), eventos Firebase Analytics.
- Fluxo geral: evento de UI → função no `MainViewModel` → atualiza `StateFlow` → recomposição da UI
  (ver `docs_ai/ARQUITETURA/README.md` seção 4).

## Decisões arquiteturais (ADR)

- **Dependência de todos os 15 outros módulos consumer**, mais dois módulos cross-linha
  (`:core:diagnostico`, `:core:relatorio` — compartilhados com SignallQ Pro, issues #1157/#1164):

```
:coreNetwork, :corePermissions, :coreDatabase, :coreDatastore, :coreTelephony,
:coreRecommendation, :featureHome, :featureWifi, :featureDevices, :featureDns,
:featureSpeedtest, :featureDiagnostico, :featureFibra, :featureHistory, :featureSettings,
:core:diagnostico, :core:relatorio
```

  Mais: Hilt, Compose (BOM), Navigation Compose, WorkManager, Firebase BOM (Crashlytics, Analytics,
  Config), Coil, `play.review`, `play.services.ads` + `user.messaging.platform` (AdMob/UMP), OkHttp,
  Timber.
- **`:app` é o único consumidor de nenhum outro módulo** — nenhum módulo do monorepo depende de
  `:app` (é a folha de composição, conforme a regra 5 da higiene).
- **Único `ViewModel` raiz do app** — decisão de arquitetura preexistente que concentra composição de
  estado em `MainViewModel`, mesmo com `feature/*` expondo estado próprio internamente.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| `MainViewModel.kt` — 2285 linhas | Dívida crítica (seção 7 da regra de higiene) | Extrair orquestração/persistência/analytics/mapeamentos por responsabilidade ao tocar — seção 4.2 da regra |
| `AppShell.kt` — 1241 linhas | Acima do limiar de extração obrigatória | Separar estado de navegação, overlays, wiring entre features — seção 4.3 da regra |
| `AjustesScreen.kt` — 803 linhas | No limiar de extração obrigatória | Extrair sheets/fluxos independentes por responsabilidade — seção 4.4 da regra |
| Caminho físico `io/veloo/app/kotlin/` diverge do package declarado `io.signallq.app` | Dívida 4.1 da regra de higiene, não exclusiva deste módulo | Não migrar oportunisticamente |
| Dependência nova em módulos Pro-shared (`:core:diagnostico`, `:core:relatorio`) | Amplia o blast radius de mudanças nesses módulos para o produto consumer | Tratar como infraestrutura compartilhada real em code review |

`src/test`: **62 arquivos** (cresceu frente aos 42 da última auditoria). `src/androidTest`: 0.
