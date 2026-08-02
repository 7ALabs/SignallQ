# Módulo :coreRecommendation

- **Status:** ativo (motor pronto, ainda não integrado a monetização real)
- **Última validação:** 2026-07-23 (fonte: `android/core/recommendation/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:coreRecommendation` (alias legado; pasta física `android/core/recommendation/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Recommendation Engine desacoplado do motor de diagnóstico (issue #790) — engine determinística que
ranqueia recomendações (`free_tip`/`tutorial`/`configuration`/`affiliate_product`/`partner_offer`/
`operator_offer`/`native_ad_fallback`) por tags de diagnóstico, com cooldown/frequência e contrato de
analytics. Único módulo `core` que já nasceu fisicamente em `io/signallq/` — não sofre da dívida 4.1.
Já integrado à UI via `RecommendationEngineCard` em `ResultadoVelocidadeScreen.kt` (GH#813), mas
ainda não integrado a AdMob/afiliados reais.

**Não confundir** com o `RecommendationEngine` de `:featureDiagnostico`, que gera dicas práticas do
diagnóstico local, sem monetização/catálogo.

## Diagrama de componentes

```
:coreRecommendation (io/signallq/ — caminho já correto)
RecommendationEngine (entrada: RecommendationRequest, saída: RecommendationDecision)
    ├── catalog/RecommendationCatalog (interface) → LocalRecommendationCatalog (impl)
    └── analytics/RecommendationAnalytics (contrato de eventos)
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `RecommendationEngine.kt` | Engine | Ranqueia recomendações por tags de diagnóstico |
| `RecommendationRequest.kt` / `RecommendationDecision.kt` | Data class | Contrato de entrada/saída da engine |
| `Recommendation.kt` | Data class | Modelo de uma recomendação |
| `RecommendationType.kt` | Enum | Tipos de recomendação (tip, tutorial, configuration, affiliate, etc.) |
| `DiagnosticTag.kt` / `DiagnosticMetrics.kt` | Data class/Enum | Entrada da engine — tags e métricas de diagnóstico |
| `NetworkContextType.kt` | Enum | Contexto de rede usado na decisão |
| `RecommendationFeedback.kt` | Data class | Feedback do usuário sobre a recomendação |
| `catalog/RecommendationCatalog.kt` (interface) / `LocalRecommendationCatalog.kt` (impl) | Repository | Catálogo de recomendações disponíveis |
| `analytics/RecommendationAnalytics.kt` | Contrato | Eventos de analytics da engine |

## Fluxo de dados principal

- **Entradas:** `RecommendationRequest` (tags de diagnóstico) vindo de `:featureDiagnostico`.
- **Saídas:** `RecommendationDecision`/`Recommendation`, exibida via `RecommendationEngineCard` em
  `:app`; histórico persistido em `:coreDatabase` (subpacote `recommendation/`, ver
  `core-database.md`).

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo** — módulo Kotlin puro de domínio, sem libs de
  produção além do padrão Android library plugin.
- **Único módulo `core` já nascido em `io/signallq/`** — criado depois do rebrand (issue #790),
  serve de referência de caminho-alvo para a migração dedicada da dívida 4.1.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Cobertura de teste baixa (`src/test`: **1 arquivo**) para uma engine de decisão com múltiplos tipos e regras de cooldown/frequência | Risco de regressão silenciosa se a engine crescer sem characterization tests adicionais | Ampliar teste ao adicionar novo `RecommendationType`/regra |
| Ainda não integrado a nenhuma monetização real (AdMob/afiliados) | Engine mantida sem uso de produto completo | Fora de escopo desta auditoria — acompanhar decisão de produto |
