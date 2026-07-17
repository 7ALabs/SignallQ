# Módulo :featureSpeedtest

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/feature/speedtest/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/speedtest/`
- **Namespace:** `io.signallq.app.feature.speedtest`

## Responsabilidade

Execução de teste de velocidade (download/upload/latência/jitter/perda) via Cloudflare Speed CDN.

## Principais packages/pastas

Base: `feature/speedtest/src/main/kotlin/io/veloo/app/kotlin/feature/speedtest/` (caminho físico
legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ExecutorSpeedtest.kt` (interface) / `ExecutorSpeedtestCloudflare.kt` (impl) | Serviço | Speedtest via Cloudflare Speed CDN |
| `SnapshotExecucaoSpeedtest.kt` / `ResultadoSpeedtest.kt` | Data class | Estado durante execução / resultado final |
| `EstadoExecucaoSpeedtest.kt` | Enum | idle, executando, concluido, erro, cancelado |
| `ModoSpeedtest.kt` | Enum | complete, ping_only |
| `PingExecutor.kt` | Class | 20 amostras ICMP-over-HTTP/2 contra `speed.cloudflare.com/__down?bytes=0` (ou URL customizada como game-latency-probe), descarta 1ª amostra, filtra outliers — usa `AnalisadorAmostragemPing` para análise (GH#1019 consolidação) |
| `AnalisadorAmostragemPing.kt` | Object stateless | Algoritmo puro consolidado: mediana, jitter, % perda. Extraído de `ExecutorSpeedtestCloudflare` e `PingExecutor` (duplicação literal removida em GH#1019). Reusado também por `ExecutorSpeedtestCloudflare` |
| `PingResultado.kt` | Data class | latenciaMs, jitterMs, perdaPercentual, amostras |

## Entradas/saídas

- **Entradas:** disparo do teste a partir da UI (`:app`) ou do fluxo silencioso do
  `SignallQOrchestrator` (`:featureDiagnostico`).
- **Saídas:** `SnapshotExecucaoSpeedtest`/`ResultadoSpeedtest` consumido por `:app` e por
  `:featureDiagnostico`.

## Dependências declaradas (build.gradle.kts real)

`:coreNetwork`, `:coreDatastore`, `:coreTelephony`. Hilt próprio (`alias(libs.plugins.hilt)` + kapt).
Libs: `okhttp`, `timber`.

## Consumidores

Via grep de `project(":featureSpeedtest")`: `:app` **e `:featureDiagnostico`**.

## Testes existentes

`src/test`: **2 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

**É o alvo da violação de dependência feature→feature**: `:featureDiagnostico` depende diretamente
deste módulo (`implementation(project(":featureSpeedtest"))`), contrariando a regra 4.5 da regra de
higiene ("Features não podem depender diretamente de outras features"). Ver README da arquitetura,
seção 4/8, e a lista de violações ao final da entrega desta tarefa. Cobertura de teste baixa (2
arquivos) para uma feature crítica de mensuração.
```

