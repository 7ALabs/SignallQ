package io.signallq.app.feature.dns

/**
 * GH#1212 item 8/12/13 — antes a UI escolhia o menor `tempoMs` como "mais rápido" mesmo
 * quando a diferença era irrelevante (23ms vs 24ms) ou a amostra pouco confiável (poucos
 * sucessos). Esta função central decide se existe um vencedor técnico real, um empate
 * técnico (vários candidatos dentro da margem), ou dados insuficientes pra recomendar
 * qualquer troca — sem inventar nível de "confiança" formal (variância/estabilidade),
 * mas com os dois filtros que os critérios de aceite pedem: taxa de sucesso mínima e
 * margem mínima de ganho.
 */
sealed interface RecomendacaoDns {
    data class Vencedor(val resultado: ResultadoBenchmarkDns) : RecomendacaoDns
    data class EmpateTecnico(val candidatos: List<ResultadoBenchmarkDns>) : RecomendacaoDns
    data object SemDadosSuficientes : RecomendacaoDns
}

object AvaliadorRecomendacaoDns {
    // Mesmo valor citado no guia de produto ("< 10 ms de diferença" já era o texto da tela)
    // — agora vira regra de fato, não só explicação.
    private const val MARGEM_EMPATE_TECNICO_MS = 10.0

    // GH#1212 item 12 — não recomendar troca com baixa confiança: taxa de sucesso abaixo
    // disso não entra na disputa por "mais rápido", mesmo que o tempo medido seja bom.
    private const val TAXA_SUCESSO_MINIMA_PERCENTUAL = 80.0

    fun avaliar(resultados: List<ResultadoBenchmarkDns>): RecomendacaoDns {
        val candidatos =
            resultados.filter {
                it.tempoMs != null &&
                    !it.isGatewayLocal &&
                    it.taxaSucessoPercentual >= TAXA_SUCESSO_MINIMA_PERCENTUAL
            }
        if (candidatos.isEmpty()) return RecomendacaoDns.SemDadosSuficientes

        val ordenados = candidatos.sortedBy { it.tempoMs }
        val melhorTempo = ordenados.first().tempoMs!!
        val dentroDaMargem = ordenados.filter { (it.tempoMs!! - melhorTempo) <= MARGEM_EMPATE_TECNICO_MS }

        return if (dentroDaMargem.size > 1) {
            RecomendacaoDns.EmpateTecnico(dentroDaMargem)
        } else {
            RecomendacaoDns.Vencedor(ordenados.first())
        }
    }
}
