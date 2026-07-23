# Módulo :featureHistory

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/history/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureHistory` (alias legado; pasta física `android/feature/history/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Observação reativa do histórico persistido, cálculo de tendência/uptime e exportação (CSV/PDF).
Namespace declarado: `io.signallq.app.feature.history`. **Correção factual desde a última auditoria**:
`PdfPrintHelper.kt` (antes descrito como pertencente a este módulo) **foi extraído para o módulo
compartilhado `:core:relatorio`** (issue #1164, motor de paginação HTML→PDF reaproveitado pela linha
SignallQ Pro — o módulo Pro `:pro:feature:laudo` reaproveita o mesmo motor).

## Diagrama de componentes

```
:featureHistory (io/veloo/ — caminho legado)
ObservadorHistorico (interface) → ObservadorHistoricoRoom (impl, observa MedicaoDao)
    ├── UptimeChartUseCase — prepara dados para gráfico de uptime
    ├── UptimeNarrativaEngine — narrativa textual sobre disponibilidade
    ├── TendenciaCalculador — calcula tendência histórica
    ├── ResumoHistorico — resumo consolidado
    └── ExportadorHistoricoCSV / ExportadorHistoricoPDF — exportação (PDF usa WebViewHtmlPdfExporter
        de :core:relatorio)
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `ObservadorHistorico.kt` (interface) / `ObservadorHistoricoRoom.kt` (impl) | Repository | Observa Room e emite histórico reativo |
| `UptimeChartUseCase.kt` | Use case | Prepara dados para gráfico de uptime |
| `UptimeNarrativaEngine.kt` | Engine | Gera narrativa textual sobre disponibilidade |
| `TendenciaCalculador.kt` | Engine | Calcula tendência histórica |
| `ResumoHistorico.kt` | Data class | Resumo consolidado do histórico |
| `ExportadorHistoricoCSV.kt` | Exportador | Exporta histórico em CSV |
| `ExportadorHistoricoPDF.kt` | Exportador | Exporta histórico em PDF — delega paginação HTML→PDF para `WebViewHtmlPdfExporter` (`:core:relatorio`) |

## Fluxo de dados principal

- **Entradas:** leitura de `MedicaoDao` (`:coreDatabase`).
- **Saídas:** `ResumoHistorico`, dados de gráfico, arquivos exportados (CSV/PDF) consumidos por
  `:app` (tela Histórico, Laudo).

## Decisões arquiteturais (ADR)

- **Dependências de módulo:** `:coreDatabase` e, desde a issue #1164, `:core:relatorio` (módulo
  Pro-shared — ver `docs_ai/ARQUITETURA/README.md` seção 4.1). Libs: `androidx-core-ktx`,
  `kotlinx-coroutines-android`.
- Declara `testOptions.unitTests.isReturnDefaultValues = true` (peculiaridade real deste módulo,
  provavelmente por causa de dependências que chamam APIs Android não mockadas).
- **Extração do motor de PDF para `:core:relatorio` (GH#1164)** — mesma lógica de reaproveitamento
  aplicada em `:featureDiagnostico`/`:core:diagnostico`: motor genérico de paginação HTML→PDF
  compartilhado entre o Laudo do consumer e o Laudo técnico do SignallQ Pro.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Dependência nova em módulo Pro-shared (`:core:relatorio`) | Amplia o blast radius de mudanças nesse módulo para o produto consumer | Tratar como infraestrutura compartilhada real em code review |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |

`src/test`: **7 arquivos** (estável desde a última auditoria). `src/androidTest`: 0.
