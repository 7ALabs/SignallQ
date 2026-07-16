# Módulo :app

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/app/build.gradle.kts`, código real)
- **Caminho físico:** `android/app/`
- **Namespace/applicationId:** `io.signallq.app`

## Responsabilidade

Composição entre features, navegação, DI de nível de aplicação (Hilt `AppModule`), único
`ViewModel` raiz do app (`MainViewModel`), telas de UI, integrações que dependem de múltiplas
features (feature flags, notificações, monitoramento em background, chamadas aos Workers Cloudflare
de ingest/probe).

## Principais packages/pastas

Base: `app/src/main/kotlin/io/veloo/app/kotlin/` (caminho físico legado — ver dívida 4.1 da regra
de higiene; package declarado é `io.signallq.app`).

- `ui/screen/` — 15+ telas Composable (Home, SpeedTest, Sinal, Historico, Dispositivos, Ajustes,
  Velocidade, ResultadoVelocidade, Diagnostico, LLMChat, Fibra, Laudo, Privacidade, Novidades,
  Onboarding)
- `ui/viewmodel/` — `ChatDiagnosticoIaViewModel`
- `di/` — `AppModule.kt` (Hilt, provê todas as dependências injetadas)
- `pulse/` — `SignallQOrchestrator.kt` (fluxo Chat/Pulse)
- `monitoramento/` — `MonitoramentoWorker.kt`, `MonitoramentoScheduler.kt`
- `notificacao/` — `SignallQNotificationHelper.kt`
- `speedtest/` — `SpeedtestPersistenceCoordinator.kt`

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SignallQApplication.kt` | `Application` | Inicialização, Hilt, canais de notificação |
| `MainActivity.kt` | `Activity` | Entry point único — `setContent { SignallQTheme { AppShell(...) } }` |
| `MainViewModel.kt` | `@HiltViewModel` | Orquestra todos os serviços e expõe `StateFlow`s |
| `FeatureFlags.kt` | Object | Controle de 32 `buildConfigField` booleanos por build type |
| `AppShell.kt` | Composable | Shell — `NavigationBar` 5 abas + fluxos sobrepostos |
| `AppNavGraph.kt` | `NavHost` | Rotas das 5 abas principais |
| `SignallQTheme.kt` | Composable | Tema MD3 |

## Entradas/saídas

- **Entradas:** eventos de UI, callbacks do SO (`ConnectivityManager`, `TelephonyManager`,
  `WifiManager`), WorkManager (execução periódica do `MonitoramentoWorker`).
- **Saídas:** persistência em Room/DataStore, notificações via `NotificationManager`, POST HTTP aos
  Workers Cloudflare (`ADMIN_INGEST_URL`, `GAME_LATENCY_PROBE_URL`), eventos Firebase Analytics.

## Dependências declaradas (build.gradle.kts real)

Todos os 15 outros módulos do monorepo:

```
:coreNetwork, :corePermissions, :coreDatabase, :coreDatastore, :coreTelephony,
:coreRecommendation, :featureHome, :featureWifi, :featureDevices, :featureDns,
:featureSpeedtest, :featureDiagnostico, :featureFibra, :featureHistory, :featureSettings
```

Mais: Hilt, Compose (BOM), Navigation Compose, WorkManager, Firebase BOM (Crashlytics, Analytics,
Config), Coil, `play.review`, `play.services.ads` + `user.messaging.platform` (AdMob/UMP), OkHttp,
Timber.

## Consumidores

Nenhum — `:app` é o módulo de aplicação, não é consumido por nenhum outro módulo.

## Testes existentes

`src/test`: **42 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

- `MainViewModel.kt` — **2186 linhas** (caminho real:
  `app/src/main/kotlin/io/veloo/app/kotlin/MainViewModel.kt`), acima do limiar de "dívida crítica"
  (seção 7 da regra de higiene). Ver seção 4.2 da regra para diretrizes de extração.
- `AppShell.kt` — **1140 linhas**, acima do limiar de extração obrigatória. Ver seção 4.3 da regra.
- `AjustesScreen.kt` — **809 linhas**, no limiar de extração obrigatória. Ver seção 4.4 da regra.
- Caminho físico `io/veloo/app/kotlin/` diverge do package declarado `io.signallq.app` — dívida
  4.1 da regra de higiene, não exclusiva deste módulo.
```

