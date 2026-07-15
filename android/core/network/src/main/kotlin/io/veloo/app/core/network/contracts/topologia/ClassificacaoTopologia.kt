package io.signallq.app.core.network.contracts.topologia

/**
 * Resultado estruturado do motor de topologia unificado (Fase 2A em diante — este contrato é
 * criado na Fase 0 sem nenhum motor emitindo-o ainda). Substitui um enum final sozinho: carrega
 * o papel provável, o quão confiável é essa leitura, quais sinais sustentam a conclusão e se
 * algum sinal discordou de outro.
 */
data class ClassificacaoTopologia(
    val papelProvavel: PapelTopologia,
    val confianca: NivelConfianca,
    val evidencias: List<Evidencia>,
    val origemDados: OrigemDados,
    val conflitos: List<ConflitoSinal> = emptyList(),
)
