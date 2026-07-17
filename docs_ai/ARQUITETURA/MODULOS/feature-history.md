# Módulo :featureHistory

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/feature/history/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/history/`
- **Namespace:** `io.signallq.app.feature.history`

## Responsabilidade

Observação reativa do histórico persistido, cálculo de tendência/uptime e exportação (CSV/PDF).

## Principais packages/pastas

Base: `feature/history/src/main/kotlin/io/veloo/app/kotlin/feature/history/` (caminho físico
legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ObservadorHistorico.kt` (interface) / `ObservadorHistoricoRoom.kt` (impl) | Repository | Observa Room e emite histórico reativo |
| `UptimeChartUseCase.kt` | Use case | Prepara dados para gráfico de uptime |
| `UptimeNarrativaEngine.kt` | Engine | Gera narrativa textual sobre disponibilidade |
| `TendenciaCalculador.kt` | Engine | Calcula tendência histórica |
| `ResumoHistorico.kt` | Data class | Resumo consolidado do histórico |
| `ExportadorHistoricoCSV.kt` / `ExportadorHistoricoPDF.kt` | Exportador | Exporta histórico em CSV/PDF |
| `PdfPrintHelper.kt` | Utilitário | Suporte à geração de PDF |

## Entradas/saídas

- **Entradas:** leitura de `MedicaoDao` (`:coreDatabase`).
- **Saídas:** `ResumoHistorico`, dados de gráfico, arquivos exportados (CSV/PDF) consumidos por
  `:app` (tela Histórico, Laudo).

## Dependências declaradas (build.gradle.kts real)

`:coreDatabase`. Libs: `androidx-core-ktx`, `kotlinx-coroutines-android`.

Nota: declara `testOptions.unitTests.isReturnDefaultValues = true` (peculiaridade real deste
módulo, provavelmente por causa de `PdfPrintHelper` chamando APIs Android não mockadas).

## Consumidores

Via grep de `project(":featureHistory")`: apenas `:app`.

## Testes existentes

`src/test`: **7 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Caminho físico `io/veloo` diverge do package — dívida 4.1. Nenhum outro risco estrutural
identificado na leitura do `build.gradle.kts` e da lista de arquivos.
```

