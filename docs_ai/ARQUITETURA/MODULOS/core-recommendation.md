# Módulo :coreRecommendation

- **Status:** ativo (motor pronto, ainda não integrado a UI/monetização)
- **Última validação:** 2026-07-16 (fonte: `android/core/recommendation/build.gradle.kts`, código real)
- **Caminho físico:** `android/core/recommendation/`
- **Namespace:** `io.signallq.app.core.recommendation`

## Responsabilidade

Recommendation Engine desacoplado do motor de diagnóstico (issue #790) — engine determinística que
ranqueia recomendações (`free_tip`/`tutorial`/`configuration`/`affiliate_product`/`partner_offer`/
`operator_offer`/`native_ad_fallback`) por tags de diagnóstico, com cooldown/frequência e contrato
de analytics. Único módulo `core` que já nasceu fisicamente em `io/signallq/` — não sofre da dívida
4.1.

**Não confundir** com o `RecommendationEngine` de `:featureDiagnostico`, que gera as 12 dicas
práticas do diagnóstico local, sem monetização/catálogo.

## Principais packages/pastas

Base: `core/recommendation/src/main/kotlin/io/signallq/app/core/recommendation/` (caminho físico já
correto):
- `analytics/RecommendationAnalytics.kt`
- `catalog/LocalRecommendationCatalog.kt`, `catalog/RecommendationCatalog.kt`

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
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

## Entradas/saídas

- **Entradas:** `RecommendationRequest` (tags de diagnóstico) vindo de `:featureDiagnostico`.
- **Saídas:** `RecommendationDecision`/`Recommendation` — ainda sem consumidor de UI real (fora do
  escopo da #790).

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Sem libs de produção declaradas além do padrão Android library plugin —
módulo Kotlin puro de domínio.

## Consumidores

Via grep de `project(":coreRecommendation")`: `:app`, `:featureDiagnostico`.

## Testes existentes

`src/test`: **1 arquivo**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Cobertura de teste (1 arquivo) baixa para uma engine de decisão com múltiplos tipos e regras de
cooldown/frequência — risco de regressão silenciosa se a engine crescer sem characterization tests
adicionais. Ainda não integrado a nenhuma feature/UI real nem a AdMob/afiliados.
```

