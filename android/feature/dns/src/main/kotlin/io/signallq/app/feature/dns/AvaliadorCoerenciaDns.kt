package io.signallq.app.feature.dns

import kotlin.math.max

enum class NivelAlertaCoerenciaDns {
    none,
    attention,
    critical,
}

data class DiagnosticoCoerenciaDns(
    val nivelAlerta: NivelAlertaCoerenciaDns,
    val divergenciasConsecutivas: Int,
    val divergenciasNaJanela: Int,
    val amostrasNaJanela: Int,
    val taxaDivergenciaPercentual: Double,
)

class AvaliadorCoerenciaDns(
    private val tamanhoJanela: Int = 5,
) {
    private val historico = ArrayDeque<Boolean>()
    private var consecutivas = 0

    fun registrarCoerencia(coerencia: String): DiagnosticoCoerenciaDns {
        when (coerencia) {
            "divergente" -> {
                registrarAmostra(divergente = true)
                consecutivas += 1
            }
            "coerente" -> {
                registrarAmostra(divergente = false)
                consecutivas = 0
            }
            else -> {
                // resultado sem referencia nao altera estado de alerta
            }
        }

        val divergencias = historico.count { it }
        val amostras = historico.size
        val taxa = if (amostras == 0) 0.0 else (divergencias.toDouble() / amostras.toDouble()) * 100.0
        val nivel =
            when {
                consecutivas >= 3 -> NivelAlertaCoerenciaDns.critical
                consecutivas >= 2 -> NivelAlertaCoerenciaDns.attention
                amostras >= 4 && taxa >= 60.0 -> NivelAlertaCoerenciaDns.attention
                else -> NivelAlertaCoerenciaDns.none
            }

        return DiagnosticoCoerenciaDns(
            nivelAlerta = nivel,
            divergenciasConsecutivas = consecutivas,
            divergenciasNaJanela = divergencias,
            amostrasNaJanela = amostras,
            taxaDivergenciaPercentual = taxa,
        )
    }

    private fun registrarAmostra(divergente: Boolean) {
        historico.addLast(divergente)
        if (historico.size > max(1, tamanhoJanela)) {
            historico.removeFirst()
        }
    }
}
