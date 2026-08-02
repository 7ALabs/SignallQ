# Módulo :featureSpeedtest

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/speedtest/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureSpeedtest` (alias legado; pasta física `android/feature/speedtest/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Execução de teste de velocidade (download/upload/latência/jitter/perda) via Cloudflare Speed CDN, com
classificação de qualidade e diagnóstico de bufferbloat. Cresceu desde a última auditoria: hoje
também classifica qualidade do resultado e expõe `SpeedtestViewModel` como state holder próprio.
Namespace declarado: `io.signallq.app.feature.speedtest`.

## Diagrama de componentes

```
:featureSpeedtest (io/veloo/ — caminho legado)
ExecutorSpeedtest (interface) → ExecutorSpeedtestCloudflare (impl, via Cloudflare Speed CDN)
    ├── PingExecutor — 20 amostras ICMP-over-HTTP/2, usa AnalisadorAmostragemPing
    ├── AnalisadorAmostragemPing — algoritmo puro consolidado (mediana, jitter, % perda)
    ├── DiagnosticoFasesSpeedtest / FaseSpeedtest — progresso por fase do teste
    ├── DiagnosticoQualidadeSpeedtest / SpeedtestQualityClassifier / SeveridadeBufferbloat
    ├── ValidadorBaselineLatencia — baseline de latência para detectar bufferbloat
    └── SpeedtestViewModel — state holder da tela de Velocidade
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `ExecutorSpeedtest.kt` (interface) / `ExecutorSpeedtestCloudflare.kt` (impl) | Serviço | Speedtest via Cloudflare Speed CDN |
| `SnapshotExecucaoSpeedtest.kt` / `ResultadoSpeedtest.kt` | Data class | Estado durante execução / resultado final |
| `EstadoExecucaoSpeedtest.kt` | Enum | idle, executando, concluido, erro, cancelado |
| `ModoSpeedtest.kt` | Enum | complete, ping_only |
| `PingExecutor.kt` | Class | 20 amostras ICMP-over-HTTP/2 contra `speed.cloudflare.com/__down?bytes=0` (ou URL customizada, ex. game-latency-probe), descarta 1ª amostra, filtra outliers — usa `AnalisadorAmostragemPing` |
| `AnalisadorAmostragemPing.kt` | Object stateless | Algoritmo puro consolidado: mediana, jitter, % perda (GH#1019, duplicação removida) |
| `PingResultado.kt` | Data class | latenciaMs, jitterMs, perdaPercentual, amostras |
| `DiagnosticoFasesSpeedtest.kt` / `FaseSpeedtest.kt` | Data class/Enum | Acompanhamento de progresso por fase do teste |
| `DiagnosticoQualidadeSpeedtest.kt` / `SpeedtestQualityClassifier.kt` | Engine | Classifica qualidade do resultado |
| `SeveridadeBufferbloat.kt` | Enum | Severidade de bufferbloat detectada |
| `ValidadorBaselineLatencia.kt` | Validador | Compara latência sob carga contra baseline |
| `PontoAoVivo.kt` / `MeasurementStatus.kt` | Data class/Enum | Ponto de medição ao vivo durante o teste |
| `SpeedtestViewModel.kt` | State holder | Estado da tela de Velocidade |

## Fluxo de dados principal

- **Entradas:** disparo do teste a partir da UI (`:app`) ou do fluxo silencioso do
  `SignallQOrchestrator` (`:featureDiagnostico`).
- **Saídas:** `SnapshotExecucaoSpeedtest`/`ResultadoSpeedtest` consumido por `:app` e por
  `:featureDiagnostico`.

## Decisões arquiteturais (ADR)

- **Dependências de módulo:** `:coreNetwork`, `:coreDatastore`, `:coreTelephony`. Hilt próprio
  (`alias(libs.plugins.hilt)` + kapt). Libs: `okhttp`, `timber`.
- **Único consolidador de amostragem de ping** (`AnalisadorAmostragemPing`) reaproveitado tanto por
  `ExecutorSpeedtestCloudflare` quanto por `PingExecutor` — decisão GH#1019 para eliminar
  duplicação literal do algoritmo.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| **É o alvo da violação de dependência feature→feature**: `:featureDiagnostico` depende diretamente deste módulo | Contraria a regra 4.5 da regra de higiene | Ver `feature-diagnostico.md` e README seção 6 |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |

`src/test`: **5 arquivos** (cresceu frente aos 2 da última auditoria). `src/androidTest`: 0.
