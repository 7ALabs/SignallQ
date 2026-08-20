package io.signallq.app.feature.history

import io.signallq.app.core.database.MedicaoEntity

/**
 * GH#1707 (Task 2.0.09e, parte 2/2, épico #1647) — veredito de comparação de um reteste vinculado
 * contra a análise original (spec §8.8/§14.6). Decisão de Luiz (2026-08-20): só o veredito simples
 * ("Melhorou"/"Não mudou"/"Piorou"), reusando o vocabulário de [TendenciaEstado]
 * (`MELHOROU`/`PIOROU`/`ESTAVEL`) — nunca números lado a lado, nunca gráfico.
 *
 * Retorna `null` ("inconclusiva") quando NÃO há par comparável de verdade: [original] ou [novo]
 * nulos, ou alguma das duas medições sem `downloadMbps`. A elegibilidade de rede (mesma rede) é
 * responsabilidade de quem chama, via `MedicaoDao.buscarUltimaComparavelNaRede` — esta função
 * nunca recebe uma medição de rede diferente por engano, mas também não valida isso sozinha (não
 * duplica a regra que o DAO já aplica). O app declara o limite em vez de comparar com aviso —
 * mesmo critério já usado no Histórico (#1669).
 *
 * Mesmos limiares de [calcularTendencia] (±10%), para não introduzir um segundo critério
 * concorrente de "melhora" no mesmo domínio (ver `.claude/rules/higiene-e-padronizacao-
 * repositorio.md`, §4.9).
 */
fun calcularVereditoReteste(
    original: MedicaoEntity?,
    novo: MedicaoEntity?,
): TendenciaEstado? {
    val downloadOriginal = original?.downloadMbps ?: return null
    val downloadNovo = novo?.downloadMbps ?: return null
    if (downloadOriginal == 0.0) return null
    val delta = (downloadNovo - downloadOriginal) / downloadOriginal * 100.0
    return when {
        delta > 10.0 -> TendenciaEstado.MELHOROU
        delta < -10.0 -> TendenciaEstado.PIOROU
        else -> TendenciaEstado.ESTAVEL
    }
}

/** Rótulo de UI (§14.6) — texto simples, `null` vira "Comparação inconclusiva". */
fun TendenciaEstado?.rotuloComparacaoReteste(): String =
    when (this) {
        TendenciaEstado.MELHOROU -> "Melhorou"
        TendenciaEstado.PIOROU -> "Piorou"
        TendenciaEstado.ESTAVEL -> "Não mudou"
        null -> "Comparação inconclusiva"
    }

/**
 * Vocabulário de telemetria do evento `diagnostico_comparacao_concluida` (spec aprovada em
 * #1657): `melhorou`/`nao_mudou`/`piorou`/`inconclusiva`.
 */
fun TendenciaEstado?.paraTelemetriaReteste(): String =
    when (this) {
        TendenciaEstado.MELHOROU -> "melhorou"
        TendenciaEstado.PIOROU -> "piorou"
        TendenciaEstado.ESTAVEL -> "nao_mudou"
        null -> "inconclusiva"
    }
