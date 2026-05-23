package io.linka.app.kotlin.feature.history

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Engine de narrativa para o grafico de uptime.
 *
 * Analisa [List<BlocoUptime>] e produz uma string legivel descrevendo
 * o comportamento da rede nos ultimos 7 dias.
 *
 * Esta e a versao 1.0 — heuristica basica por contagem e padroes simples.
 * TODO v2.0: detectar padroes horarios recorrentes (ex: "toda manha entre 8h e 9h")
 * TODO v2.0: detectar sequencias longas de OFFLINE (interrupcoes)
 * TODO v2.0: identificar tendencia de piora/melhora
 */
object UptimeNarrativaEngine {

    private val formatadorHora = DateTimeFormatter.ofPattern("HH'h'", Locale("pt", "BR"))

    fun gerarNarrativa(blocos: List<BlocoUptime>): String {
        if (blocos.isEmpty()) return "Sem dados de monitoramento disponíveis."

        val total = blocos.size
        val offline = blocos.count { it.status == StatusUptime.OFFLINE }
        val lento = blocos.count { it.status == StatusUptime.LENTO }
        val ok = blocos.count { it.status == StatusUptime.OK }
        val semDado = blocos.count { it.status == StatusUptime.SEM_DADO }

        // Sem dados suficientes para narrativa
        val blocosMedidos = total - semDado
        if (blocosMedidos < 10) {
            return "Monitoramento iniciado recentemente. Continue usando o app para ver o histórico da sua rede."
        }

        // Tudo OK
        if (offline == 0 && lento == 0) {
            return "Sua rede esteve estável nos últimos 7 dias. Nenhuma lentidão ou queda detectada."
        }

        val partes = mutableListOf<String>()

        // Instabilidades graves (OFFLINE)
        if (offline > 0) {
            val horasOffline = (offline * 30) / 60
            val minutosOffline = (offline * 30) % 60
            val duracaoOffline = when {
                horasOffline > 0 && minutosOffline > 0 -> "${horasOffline}h ${minutosOffline}min"
                horasOffline > 0 -> "${horasOffline}h"
                else -> "${minutosOffline}min"
            }

            val sequenciaLonga = encontrarMaiorSequencia(blocos, StatusUptime.OFFLINE)
            if (sequenciaLonga >= 4) {
                // Sequencia >= 2h: menciona interrupcao longa
                val blocoInicio = encontrarInicioDaMaiorSequencia(blocos, StatusUptime.OFFLINE)
                val descricaoPeriodo = descreverPeriodo(blocoInicio)
                partes.add("Sua rede ficou offline por cerca de ${(sequenciaLonga * 30 / 60)}h $descricaoPeriodo.")
            } else {
                partes.add("Sua rede ficou indisponível por $duracaoOffline nos últimos 7 dias.")
            }
        }

        // Lentidao
        if (lento > 0) {
            val percentualLento = (lento * 100) / blocosMedidos
            if (percentualLento >= 20) {
                partes.add("A rede ficou lenta em $percentualLento% do tempo monitorado.")
            } else if (lento >= 2) {
                val horasLento = (lento * 30) / 60
                val minutosLento = (lento * 30) % 60
                val duracaoLento = when {
                    horasLento > 0 -> "${horasLento}h e ${minutosLento}min"
                    else -> "${minutosLento}min"
                }
                partes.add("Houve lentidão por $duracaoLento.")
            }
        }

        // Resumo geral de estabilidade
        val percentualOk = if (blocosMedidos > 0) (ok * 100) / blocosMedidos else 0
        if (percentualOk >= 90 && partes.isNotEmpty()) {
            partes.add("No geral, a rede ficou estável em $percentualOk% do tempo.")
        }

        return if (partes.isEmpty()) {
            "Sua rede apresentou algumas instabilidades menores nos últimos 7 dias."
        } else {
            partes.joinToString(" ")
        }
    }

    /** Retorna o tamanho da maior sequencia continua do status informado. */
    private fun encontrarMaiorSequencia(blocos: List<BlocoUptime>, status: StatusUptime): Int {
        var maxima = 0
        var atual = 0
        blocos.forEach { bloco ->
            if (bloco.status == status) {
                atual++
                if (atual > maxima) maxima = atual
            } else {
                atual = 0
            }
        }
        return maxima
    }

    /** Retorna o dataHora do primeiro bloco da maior sequencia continua do status. */
    private fun encontrarInicioDaMaiorSequencia(blocos: List<BlocoUptime>, status: StatusUptime): LocalDateTime? {
        var maxima = 0
        var atual = 0
        var inicioAtual: LocalDateTime? = null
        var melhorInicio: LocalDateTime? = null

        blocos.forEach { bloco ->
            if (bloco.status == status) {
                if (atual == 0) inicioAtual = bloco.dataHora
                atual++
                if (atual > maxima) {
                    maxima = atual
                    melhorInicio = inicioAtual
                }
            } else {
                atual = 0
                inicioAtual = null
            }
        }
        return melhorInicio
    }

    private fun descreverPeriodo(dataHora: LocalDateTime?): String {
        if (dataHora == null) return ""
        val diaSemana = dataHora.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("pt", "BR"))
        val hora = dataHora.format(formatadorHora)
        return "na $diaSemana às $hora"
    }
}
