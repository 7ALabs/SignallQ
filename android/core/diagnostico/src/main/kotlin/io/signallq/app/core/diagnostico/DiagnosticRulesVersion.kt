package io.signallq.app.core.diagnostico

/**
 * Versão canônica única do conjunto de regras de classificação/diagnóstico local
 * (GH#1228, Fase 3 — `executionId`/`rulesVersion`).
 *
 * Antes desta fatia, nenhuma linha de [io.signallq.app.core.database.MedicaoEntity] carregava
 * uma versão de regra — se um threshold mudasse no futuro, um resultado antigo no Histórico
 * era reclassificado silenciosamente com a regra ATUAL, sem forma de saber se o rótulo
 * exibido reflete a regra vigente quando o teste rodou ou uma regra posterior (achado P0-9
 * de `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`).
 *
 * [CURRENT] é a fonte canônica única desse identificador — nunca deriva de data, nunca muda a
 * cada build, nunca é um hash instável, e não pode ser recalculado por tela/mapper/presenter
 * (sempre este valor, propagado). NÃO é o mesmo conceito de `BuildConfig.VERSION_NAME`
 * (versão do app) nem de `DiagnosticEvaluation.rulesetVersion` (versão do ruleset do worker
 * remoto, `Int`, ainda não plugado em produção — ver ADR-011 seção 3.2): este é o versionamento
 * do motor LOCAL (`DiagnosticRunner`/`MetricClassifier`/`InternetDiagnosticEngine`/
 * `SpeedtestQualityClassifier` e demais classificadores de `core/diagnostico`/`feature/speedtest`
 * hoje sem versão nenhuma).
 *
 * ## Quando incrementar (`diagnostic-rules-v2`, e assim por diante)
 *
 * Incrementar SEMPRE que uma mudança de PRODUÇÃO alterar, de forma observável ao usuário:
 * - qualquer threshold numérico usado por um motor de classificação/diagnóstico local
 *   (`MetricClassifier`, `InternetDiagnosticEngine`, `SpeedtestQualityClassifier`,
 *   `UsageProfileClassifier`, `GameReadinessClassifier`, `WifiChannelDiagnosticEngine`,
 *   `ClassificadorSaudeGpon`, `DnsDiagnosticEngine`, `ScoreEvidenceBuilder` etc.);
 * - o vocabulário ou o mapeamento de severidade de um desses motores (`DiagnosticStatus`,
 *   `MetricStatus`, `VereditoUso`, `UsageProfileStatus`, `ReadinessStatus` etc.);
 * - a lógica do `FindingEngine` que decide o achado principal/secundário;
 * - o catálogo de recomendações (REC-01..14) de forma que mude a conclusão apresentada ao
 *   usuário para o mesmo conjunto de métricas.
 *
 * NÃO precisa incrementar para: mudança de copy/texto sem alterar a conclusão/severidade,
 * refactor interno sem mudança de comportamento observável (ex.: mover threshold de arquivo
 * sem mudar o valor), ou adição de motor novo ainda não consumido em produção.
 *
 * Nunca usar sufixo de versão no NOME do tipo (`V2`/`Final`, proibido pela regra de higiene,
 * seção 6) — a versão é sempre o VALOR desta constante, nunca o nome de uma classe nova.
 */
object DiagnosticRulesVersion {
    const val CURRENT: String = "diagnostic-rules-v1"

    /** Valor persistido em linhas gravadas antes desta coluna existir (migração Room 15→16)
     *  — nunca inventamos qual conjunto de regras classificou esses dados antigos. */
    const val LEGACY_UNVERSIONED: String = "legacy-unversioned"
}
