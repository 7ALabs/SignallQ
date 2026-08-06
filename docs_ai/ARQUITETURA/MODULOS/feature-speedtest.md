---
title: "Módulo :featureSpeedtest"
description: "Motor de medição de velocidade (Cloudflare), amostragem de ping, classificação de qualidade e diagnóstico local de conectividade."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-06"
---

# `:featureSpeedtest`

- **Caminho físico:** `android/feature/speedtest/` (alias flat legado)
- **Namespace:** `io.signallq.app.feature.speedtest`

## Responsabilidade

É o motor de medição do produto: executa o speedtest contra endpoints Cloudflare (fases de latência-base, download, upload e latência sob carga), calcula jitter, perda de pacote, bufferbloat, estabilidade e picos, e produz um `ResultadoSpeedtest` já classificado (`MeasurementStatus`, `DiagnosticoQualidadeSpeedtest`, `DiagnosticoFasesSpeedtest`). Expõe também o `SpeedtestViewModel` (Hilt) com a guarda de rede medida, a acumulação mensal de MB em rede móvel e a interrupção por "Wi-Fi sem internet" (`connectivity/`).

Não é dele: renderizar as telas de execução e resultado (`ResultadoVelocidadeScreen.kt`, 770 linhas, e o card de velocidade em `HomeScreen.kt`, ambos no `:app`), persistir histórico de medições (`:coreDatabase`), gerar recomendação/laudo, nem decidir navegação. Também não define os thresholds de bufferbloat — delega a `:core:diagnostico` (`MetricClassifier`).

## Dependências

Extraídas de `android/feature/speedtest/build.gradle.kts`.

| Tipo | Dependência | Observação |
|---|---|---|
| Plugin | `com.android.library`, `org.jetbrains.kotlin.android`, `org.jetbrains.kotlin.kapt`, `libs.plugins.hilt` | único dos cinco, junto de `:featureDevices`, com DI própria |
| `implementation` | `libs.hilt.android` + `kapt(libs.hilt.compiler)` | `@HiltViewModel` |
| `implementation` | `libs.androidx.core.ktx` | — |
| `implementation` | `libs.kotlinx.coroutines.android` | — |
| `implementation` | `libs.androidx.lifecycle.runtime.ktx` | `ViewModel`/`viewModelScope` |
| `implementation` | `libs.okhttp` | transporte das fases de medição |
| `implementation` | `project(":coreNetwork")` | `MonitorRede`, `AnalyticsHelper/Tracker`, contratos de conectividade |
| `implementation` | `project(":coreDatabase")` | `ConnectivityDiagnosisHistoryDao/Entity` |
| `implementation` | `project(":coreDatastore")` | `PreferenciasAppRepository` (MB acumulados, preferências) |
| `implementation` | `project(":coreTelephony")` | `MonitorTelephony` (tecnologia da rede móvel) |
| `implementation` | `project(":core:diagnostico")` | GH#1228 fatia 6 — fonte única dos cortes de bufferbloat |
| `implementation` | `libs.timber` | — |
| `testImplementation` | `libs.junit`, `libs.kotlinx.coroutines.test`, `libs.okhttp.mockwebserver` | — |
| `androidTestImplementation` | `libs.androidx.junit`, `libs.androidx.espresso.core` | não há `src/androidTest` |

## Consumidores

`grep` por `project(":featureSpeedtest")` em `android/**/build.gradle.kts` — é o módulo mais consumido dos cinco:

| Consumidor | Arquivo | Situação |
|---|---|---|
| `:app` | `android/app/build.gradle.kts:315` | legítimo (composição no app) |
| `:featureDiagnostico` | `android/feature/diagnostico/build.gradle.kts:62` | **violação da regra feature → feature** |
| `:pro:app` | `android/pro/app/build.gradle.kts:110` | legítimo (outro app) |
| `:pro:feature:medicao-diagnostico` | `android/pro/feature/medicao-diagnostico/build.gradle.kts:69` | **violação da regra feature → feature** |

## Componentes principais

| Arquivo / classe | Linhas | Responsabilidade |
|---|---|---|
| `.../feature/speedtest/ExecutorSpeedtestCloudflare.kt` | 1495 | Implementação real do motor: pool HTTP adaptativo (móvel × Wi-Fi), fases ping/download/upload, latência sob carga, cálculo de bufferbloat/estabilidade/picos, `construirResultado` (onde `MeasurementStatus` é computado uma única vez). |
| `.../feature/speedtest/SpeedtestViewModel.kt` | 312 | `@HiltViewModel`. Orquestra execução (fast/complete/triplo), guarda de rede medida, acúmulo de MB, analytics (Firebase + admin-worker), interrupção por Wi-Fi sem internet, callback `onSpeedtestConcluido` para o orquestrador. |
| `.../feature/speedtest/PingExecutor.kt` | 174 | Ping via HTTP (Android não permite ICMP bruto sem `CAP_NET_RAW`); classifica motivo de falha por amostra. |
| `.../feature/speedtest/connectivity/ConnectivityDiagnosisPresenter.kt` | 142 | Converte `ConnectivityDiagnosis` em título/mensagem + `ConnectivityAction` ordenadas. |
| `.../feature/speedtest/connectivity/ConnectivityDiagnosisRepository.kt` | 109 | Interface + implementação: executa o motor de `:coreNetwork`, persiste sanitizado em `:coreDatabase`, expõe `ultimoDiagnostico` e `existeRedeWifiAtiva`. |
| `.../feature/speedtest/AnalisadorAmostragemPing.kt` | 97 | Algoritmo puro: mediana, jitter, perda, filtro de outlier (`> 3x` mediana), `maxMs`/`p95Ms`/`picos`. |
| `.../feature/speedtest/MeasurementStatus.kt` | 77 | Fonte única de integridade da execução: `COMPLETE`/`PARTIAL`/`INCONCLUSIVE`/`CONTAMINATED`/`CANCELLED`, com as regras de consumo documentadas. |
| `.../feature/speedtest/SpeedtestQualityClassifier.kt` | 72 | Traduz `MetricStatus` de `:core:diagnostico` para `SeveridadeBufferbloat`. |
| `.../feature/speedtest/connectivity/ConnectivityBlockingPolicy.kt` | 51 | Decide se um diagnóstico é evidência forte o bastante para interromper o teste (extraída da duplicação entre `MainViewModel` e `SpeedtestViewModel`). |
| `.../feature/speedtest/ResultadoSpeedtest.kt` | 41 | Contrato de saída (28+ campos, incluindo métricas DNS e diagnósticos). |
| `.../feature/speedtest/ValidadorBaselineLatencia.kt` | 28 | Guardas puras: probe indisponível e baseline fisicamente implausível. |
| `.../feature/speedtest/ExecutorSpeedtest.kt` | 22 | Interface do motor (`snapshotFlow`, `executar`, `cancelar`). |
| `.../feature/speedtest/FeatureSpeedtestModulo.kt` | 20 | Factory que injeta a URL do worker dedicado de latência (`GAME_LATENCY_PROBE_URL`). |
| Modelos de apoio | 3–23 cada | `SnapshotExecucaoSpeedtest`, `DiagnosticoFasesSpeedtest`, `DiagnosticoQualidadeSpeedtest`, `EstadoExecucaoSpeedtest`, `FaseSpeedtest`, `ModoSpeedtest`, `PontoAoVivo`, `SeveridadeBufferbloat`. |

Total: 2740 linhas em `src/main` e 962 em `src/test` (8 arquivos de teste).

## Riscos e dívidas

- **Violação da regra "feature nunca depende de feature" — duas ocorrências confirmadas:**
  - `android/feature/diagnostico/build.gradle.kts:62` → `implementation(project(":featureSpeedtest"))`. O uso é real e profundo: `feature/diagnostico/src/main/kotlin/io/veloo/app/kotlin/feature/diagnostico/pulse/SignallQOrchestrator.kt` importa `ExecutorSpeedtest`, `ResultadoSpeedtest`, `ModoSpeedtest`, `EstadoExecucaoSpeedtest`, `DiagnosticoFasesSpeedtest` e `SpeedtestQualityClassifier`.
  - `android/pro/feature/medicao-diagnostico/build.gradle.kts:69` → mesma dependência, cruzando ainda a fronteira Consumer/Pro.
  - O caminho correto seria extrair o contrato do motor (`ExecutorSpeedtest` + tipos de resultado) para um módulo `core` — exatamente o movimento já feito em GH#1228 para os thresholds (`:core:diagnostico`) e em `:featureHome` (struct genérica adaptada no `:app`).
- **`ExecutorSpeedtestCloudflare.kt` com 1495 linhas** — muito acima do limite de 800. É o maior arquivo dos cinco módulos, concentra rede, concorrência, estatística e construção do resultado, e **não tem teste direto**: os testes cobrem as peças puras extraídas dele (`AnalisadorAmostragemPing`, `ValidadorBaselineLatencia`, `SpeedtestQualityClassifier`, `PingExecutor`) e o pacote `connectivity`.
- **Caminho legado `io/veloo`:** todos os fontes vivem em `src/main/kotlin/io/veloo/app/kotlin/feature/speedtest/` com pacote `io.signallq.app.feature.speedtest`.
- **Pacote de teste divergente:** `src/test/kotlin/io/veloo/app/kotlin/feature/speedtest/SpeedtestMbEstimativaTest.kt` declara pacote `io.signallq.app.kotlin.feature.speedtest` (com `.kotlin.`), diferente dos demais — visível no nome do relatório JUnit gerado.
- **Telas fora do módulo:** `ResultadoVelocidadeScreen.kt` (770 linhas) e o fluxo de execução em `HomeScreen.kt` (2967 linhas) estão em `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/`. O módulo não contém nenhum Composable — a separação UI/motor é real aqui, mas assimétrica: a UI inteira ficou no `:app`.
- **`MainViewModel.kt` do `:app` também consome `ConnectivityDiagnosisRepository`** (linha 190), o que mantém acoplamento do app ao pacote `connectivity` de uma feature de medição — candidato natural a `:coreNetwork`.
