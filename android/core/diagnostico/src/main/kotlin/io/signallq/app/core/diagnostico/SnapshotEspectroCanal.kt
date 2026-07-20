package io.signallq.app.core.diagnostico

import io.signallq.app.core.network.contracts.topologia.NivelConfianca

/**
 * @property confianca GH#1207 (critérios de robustez) — confiança na recomendação atual,
 * derivada do tamanho da amostra (poucas redes = menos confiável) e de quanto da análise foi
 * estimada (largura de canal assumida). Nunca "ALTA" com amostra pequena ou dado estimado.
 */
data class SnapshotEspectroCanal(
    val dadosPorCanal: List<DadoCanal>,
    val canalAtual: Int?,
    val canalRecomendado: Int?,
    val motivoRecomendacao: String?,
    val banda: String,
    val confianca: NivelConfianca = NivelConfianca.BAIXA,
)
