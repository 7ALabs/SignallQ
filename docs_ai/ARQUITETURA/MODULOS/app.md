---
title: "Módulo :app"
description: "Aplicação Android do SignallQ Consumer — composição de features, navegação, DI raiz e telas Compose."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-19"
---

# `:app`

- **Caminho físico:** `android/app/`
- **Namespace:** `io.signallq.app` (mesmo valor em `applicationId`)
- **Tipo:** aplicação (`com.android.application`)

## Responsabilidade

Módulo de composição do app Consumer: hospeda `SignallQApplication`, `MainActivity`, o
`MainViewModel` raiz, o grafo Hilt de nível de aplicação (`di/AppModule.kt`), a navegação
(`ui/screen/AppShell.kt`) e praticamente todas as telas Compose do produto. Também concentra o
que só existe quando várias features se juntam: monetização AdMob + consentimento UMP,
analytics/telemetria para o `signallq-admin-worker`, monitoramento em background via WorkManager,
notificações e o exportador de relatório em PDF do consumidor.

Não é dele: coleta de dados de rede (`:coreNetwork`, `:coreTelephony`), persistência
(`:coreDatabase`, `:coreDatastore`), regras de classificação/causa-raiz (`:core:diagnostico`),
paginação HTML→PDF (`:core:relatorio`) nem o contrato de feature flags remoto
(`:core:featureflags`). Na prática essa fronteira ainda vaza — ver Riscos.

## Dependências

### Módulos do projeto

| Módulo | Para quê |
|---|---|
| `project(":coreNetwork")` | Monitor de conectividade, contratos de gateway/dispositivo local, scan Wi-Fi, `AnalyticsTracker`, `FeatureFlagProvider` legado (SIG-13) |
| `project(":corePermissions")` | Fluxos de permissão (localização, telefonia, notificações) |
| `project(":coreDatabase")` | Room — histórico de medições, outbox de analytics |
| `project(":coreDatastore")` | `PreferenciasAppRepository` (tema, consentimento, onboarding) |
| `project(":coreTelephony")` | RSRP/RSRQ/SINR/banda quando a conexão é móvel |
| `project(":coreRecommendation")` | Motor de recomendações práticas do consumidor |
| `project(":featureHome")` | Feature Home |
| `project(":featureWifi")` | Feature Wi-Fi |
| `project(":featureDevices")` | Feature Dispositivos |
| `project(":featureDns")` | Feature DNS |
| `project(":featureSpeedtest")` | Feature SpeedTest |
| `project(":featureDiagnostico")` | Feature Diagnóstico (orquestração local + remota) |
| `project(":featureFibra")` | Feature Fibra/GPON |
| `project(":featureHistory")` | Feature Histórico |
| `project(":featureSettings")` | Feature Ajustes |
| `project(":core:diagnostico")` | `DiagnosticReport`/`DiagnosticInput`/`DiagnosticStatus` consumidos direto por telas e ViewModels (issue #1157 Fase 1a) |
| `project(":core:relatorio")` | `exportarHtmlComoPdf` — renderer único de PDF do consumidor (GH#1219) |
| `project(":core:featureflags")` | `FeatureFlagProvider` + catálogo tipado sobre Firebase Remote Config (issue #1477) |

### Bibliotecas externas (do catálogo `libs.versions.toml`)

| Biblioteca | Para quê |
|---|---|
| Compose BOM, `compose.ui`, `material3`, `material.icons.extended`, `activity.compose`, `navigation.compose` | Toda a camada de UI |
| `androidx.core.ktx`, `activity.ktx`, `lifecycle.runtime.ktx`, `lifecycle.runtime.compose` | Ciclo de vida e extensões Android |
| Hilt (`hilt.android` + `hilt.compiler` via kapt) e `hilt.work` (+ `hilt.work.compiler` via KSP) | Injeção de dependência, inclusive nos Workers |
| `androidx.work.runtime.ktx` | `MonitoramentoWorker`, `AdminSyncWorker` |
| Firebase BOM + `crashlytics`, `analytics`, `config` | Crash reporting, analytics e Remote Config |
| `okhttp` | Chamadas HTTP diretas (ingest do admin worker, sonda de latência de jogos) |
| `coil.compose` | Logo remota de operadora de cauda longa (GH#970) |
| `play.services.ads` + `user.messaging.platform` | AdMob nativo e gate de consentimento UMP (issue #555) |
| `play.review` | Avaliação in-app (SIG-173/#664) |
| `timber`, `androidx.profileinstaller`, `desugar.jdk.libs` | Log, baseline profile, desugaring |
| Testes: `junit`, `robolectric`, `mockk`, `kotlinx.coroutines.test`, `org.json:json:20260719`, `compose.ui.test.junit4` | Suíte de unit tests JVM/Robolectric |

Plugins aplicados: AGP application, Kotlin Android, Compose compiler, kapt, KSP, Hilt,
`google-services`, Firebase App Distribution, Firebase Crashlytics, detekt, ktlint e
`gradle-play-publisher`.

## Consumidores

Nenhum. `:app` é o topo do grafo do Consumer — a busca por `project(":app")` nos
`build.gradle.kts` do repositório não retorna nenhum consumidor.

## Componentes principais

| Arquivo / classe | Responsabilidade |
|---|---|
| `app/src/main/kotlin/io/signallq/app/SignallQApplication.kt` | `@HiltAndroidApp`, `Configuration.Provider` do WorkManager; inicializa Timber/Crashlytics, feature flags legadas e do novo `FeatureFlagProvider`, coordenador de persistência de speedtest, `AdsFlagsManager` e agendamento de sync com o admin worker |
| `app/src/main/kotlin/io/signallq/app/MainActivity.kt` | Activity única (`@AndroidEntryPoint`), 640 linhas; monta `SignallQTheme { AppShell(...) }` e trata permissões contextuais |
| `app/src/main/kotlin/io/signallq/app/MainViewModel.kt` | ViewModel raiz que orquestra os serviços e expõe os `StateFlow` das telas — **2438 linhas** |
| `app/src/main/kotlin/io/signallq/app/ui/screen/AppShell.kt` | Navegação, bottom bar e composição das telas — 1635 linhas |
| `app/src/main/kotlin/io/signallq/app/ui/screen/AppShellOverlayRegistry.kt` | Ponto de extensão de overlays (issue #1695, épico #1647) — agrega os `AppShellXxxOverlay.kt` sem exigir editar `AppShell.kt` para plugar overlay novo |
| `app/src/main/kotlin/io/signallq/app/ui/screen/AppShellFeatureGating.kt` | Aplica o gate de navegação por flag remota nos 9 módulos feature (F4/#1480) |
| `app/src/main/kotlin/io/signallq/app/di/AppModule.kt` | Módulo Hilt único (393 linhas) — provê tudo, inclusive a lambda `() -> FirebaseRemoteConfig` e o `FeatureFlagProvider` de `:core:featureflags` |
| `app/src/main/kotlin/io/signallq/app/FeatureFlags.kt` | Flags de compilação (`BuildConfig.FEATURE_*`) — mecanismo por build type, distinto das flags remotas |
| `app/src/main/kotlin/io/signallq/app/featureflags/ConsumerFeatureGateCoordinator.kt` | Deriva `AppShellFeatureFlagsState` reativo a partir do `FeatureFlagProvider` remoto |
| `app/src/main/kotlin/io/signallq/app/featureflags/FeatureFlagManager.kt` / `FeatureFlagRepository.kt` | Mecanismo legado de flags via HTTP `GET /flags` (SIG-13) |
| `app/src/main/kotlin/io/signallq/app/ui/relatorio/` (4 arquivos, 349 linhas) | `RelatorioDiagnosticoSnapshot` → `RelatorioDiagnosticoHtmlBuilder` (puro) → `RelatorioDiagnosticoExporter`, que delega a paginação para `:core:relatorio` |
| `app/src/main/kotlin/io/signallq/app/ads/` (7 arquivos) | `AdSlot`, `AdUnitIds` (real vs teste conforme `-PplayTrack`), `ConsentManager` (UMP), `AdsRemoteConfigRepository` |
| `app/src/main/kotlin/io/signallq/app/monitoramento/` (7 arquivos) | `MonitoramentoWorker`/`Scheduler`, `AdminSyncWorker`/`Scheduler`, `AnalyticsOutboxProcessor`, `HisteresiHelper` |
| `app/src/main/kotlin/io/signallq/app/analytics/` (5 arquivos) | `CompositeAnalyticsTracker`, `FirebaseAnalyticsTracker`, `AnalyticsOutboxFunnelTracker`, `DistributionChannel` |
| `app/src/main/kotlin/io/signallq/app/ui/screen/` | 92 arquivos de tela/estado — inclui `HomeScreen.kt` (2967), `SinalCanalSection.kt` (1215), `SinalWifiSection.kt` (1110). `SinalScreen.kt` (476) virou scaffold — issue #1660 extraiu as três abas para `SinalWifiSection.kt`/`SinalCanalSection.kt`/`SinalMovelSection.kt` + `SinalSharedComponents.kt`. `DispositivosScreen.kt` (168) virou scaffold — issue #1663 extraiu lista/estados vazios para `DispositivosLista.kt` (622) e sheets de detalhe para `DispositivoDetalheSheet.kt` (617) |
| `app/src/main/AndroidManifest.xml` | 8 permissões, `FileProvider`, App ID do AdMob, remoção do `WorkManagerInitializer` automático |

Versão declarada em `android/gradle/libs.versions.toml`: `versionCode = 72`, `versionName = 0.31.0`
(`compileSdk = 37`, `minSdk = 24`, `targetSdk = 36`).

## Riscos e dívidas

- **Path físico alinhado ao package `io.signallq.app.*`** — os 150 arquivos `.kt` de `src/main` e
  os 73 de `src/test` vivem em `io/signallq/app/` desde 2026-08-15 (#1645); migração de 221
  arquivos legados fisicamente em `io/veloo/app/kotlin/` concluída em uma única PR (§4.1 da higiene).
- **Arquivos acima de 800 linhas em `src/main`** (contagem real, `wc -l`):
  `ui/screen/HomeScreen.kt` 2967, `MainViewModel.kt` 2438, `ui/screen/AppShell.kt` 1635,
  `ui/screen/SinalCanalSection.kt` 1215, `ui/screen/SinalWifiSection.kt` 1110,
  `ui/component/LocalDeviceSection.kt` 1248, `ui/screen/DiagnosticoGuiadoScreen.kt` 916,
  `ui/screen/SpeedTestScreen.kt` 851, `ui/screen/HistoricoScreen.kt` 815 e
  `ui/screen/DnsScreen.kt` 815. A issue #1660 (épico #1647)
  extraiu `ui/screen/SinalScreen.kt` (era 3383 linhas, dívida crítica) em scaffold (476 linhas) +
  `SinalWifiSection.kt`/`SinalCanalSection.kt`/`SinalMovelSection.kt` (539)/`SinalSharedComponents.kt`
  (79) — `SinalWifiSection.kt` e `SinalCanalSection.kt` nasceram acima de 800 linhas e são
  candidatos a nova extração incremental por componente numa fatia futura, não redesign.
  A issue #1663 (mesmo épico) extraiu `ui/screen/DispositivosScreen.kt` (era 1381 linhas, dívida
  crítica) em scaffold (168 linhas) + `DispositivosLista.kt` (622) + `DispositivoDetalheSheet.kt`
  (617) — as duas ficaram abaixo do limiar de 800 linhas, sem exigir extração adicional.
  `MainViewModel.kt` já é tratado como dívida crítica no próprio
  código (o KDoc de `ConsumerFeatureGateCoordinator` cita a regra de higiene §4.2: extrair, não
  adicionar responsabilidade). `AppShell.kt` caiu de 1703 para 1635 linhas com a issue #1695
  (épico #1647), que criou `AppShellOverlayRegistry.kt` como ponto de extensão de **overlays**
  (não rota — a navegação entre raízes segue inline) — 8 overlays (Assist, Termos, Novidades,
  Privacidade, DetalhesTecnicos, SinalWifi, Ping, Dns) migraram para arquivos próprios
  registrados ali. Só ~15% das ~226 linhas que 5 fatias do épico devolveram a `AppShell.kt`
  eram bloco de overlay — o resto foi wiring de root content e estado hoisted, que este
  registro não cobre (ver `docs_ai/technical/appshell-overlay-registry.md`, seção "O que este
  registro não resolve"). Os demais overlays (Ajustes, Perfil, Ferramentas, Dispositivos,
  Fibra/EquipamentoInternet, Laudo, SinalCanais, ResultadoVelocidade, DiagnosticoGuiado,
  ModoGamer) continuam inline — migração é trabalho das fatias futuras que tocarem cada área.
- **Tamanho geral:** 40017 linhas em `src/main` contra 8637 em `src/test` — o módulo de composição
  concentra mais código do que qualquer módulo `core`/`feature`.
- **Dois sistemas de feature flag remotos convivendo.** `featureflags/FeatureFlagManager` (HTTP,
  SIG-13, contrato `io.signallq.app.core.network.FeatureFlagProvider`) e o novo
  `io.signallq.app.core.featureflags.FeatureFlagProvider` (Firebase Remote Config). Nomes de
  interface idênticos em pacotes diferentes — risco real de import errado. A migração está
  prevista em F4/#1480 e o único consumidor real do legado
  (`DiagnosticDivergenceReporter`/shadow mode) já foi migrado na #1497. Some-se a isso o terceiro
  mecanismo, `FeatureFlags.kt` sobre `BuildConfig`.
- **Sem testes instrumentados.** Existe `src/main` e `src/test`, mas nenhum diretório
  `src/androidTest` — as dependências `androidTestImplementation` declaradas em
  `build.gradle.kts` não têm código correspondente.
- **Segredo de ingest em `BuildConfig`.** `ADMIN_INGEST_KEY` é injetada via `local.properties`/env
  e acaba como string no APK; o escopo é limitado a `POST /ingest/*`, mas continua sendo material
  extraível do binário.
- **`di/AppModule.kt` como módulo Hilt único** (393 linhas) para todo o grafo da aplicação —
  ponto de acoplamento central entre todos os módulos.
