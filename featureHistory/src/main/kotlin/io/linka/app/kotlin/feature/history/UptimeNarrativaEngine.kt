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
 * v2.0: detecta padroes horarios recorrentes, sequencias longas de OFFLINE
 *       e tendencia de qualidade (melhora / piora / estavel).
 */
object UptimeNarrativaEngine {

    private val formatadorHora = DateTimeFormatter.ofPattern("HH'h'", Locale("pt", "BR"))

    // Cada bloco representa 30 minutos.
    private const val MINUTOS_POR_BLOCO = 30
    // Sequencia minima (em blocos) para considerar interrupcao "longa" — 30 min cada, entao 1 bloco = 30 min.
    // Threshold: >30 min = 2+ blocos consecutivos OFFLINE.
    private const val BLOCOS_INTERRUPCAO_LONGA = 2
    // Numero minimo de dias distintos com queda num mesmo horario para considerar padrao recorrente.
    private const val DIAS_MINIMOS_PADRAO_HORARIO = 2
    // Blocos por dia para separar janelas de 24h.
    private const val BLOCOS_POR_DIA = 48 // 24h / 30min

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
            val horasOffline = (offline * MINUTOS_POR_BLOCO) / 60
            val minutosOffline = (offline * MINUTOS_POR_BLOCO) % 60
            val duracaoOffline = when {
                horasOffline > 0 && minutosOffline > 0 -> "${horasOffline}h ${minutosOffline}min"
                horasOffline > 0 -> "${horasOffline}h"
                else -> "${minutosOffline}min"
            }

            // v2.0: sequencias longas (>30 min = mais de 1 bloco consecutivo)
            val sequenciasLongas = detectarInterrupcoesLongas(blocos)
            val maiorSequencia = sequenciasLongas.maxByOrNull { it.duracaoMinutos }

            if (maiorSequencia != null && maiorSequencia.duracaoMinutos > 30) {
                val horas = maiorSequencia.duracaoMinutos / 60
                val minutos = maiorSequencia.duracaoMinutos % 60
                val duracaoStr = when {
                    horas > 0 && minutos > 0 -> "${horas}h ${minutos}min"
                    horas > 0 -> "${horas}h"
                    else -> "${minutos}min"
                }
                val descricaoPeriodo = descreverPeriodo(maiorSequencia.inicio)
                partes.add("Sua rede ficou offline por $duracaoStr $descricaoPeriodo.")
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
                val horasLento = (lento * MINUTOS_POR_BLOCO) / 60
                val minutosLento = (lento * MINUTOS_POR_BLOCO) % 60
                val duracaoLento = when {
                    horasLento > 0 -> "${horasLento}h e ${minutosLento}min"
                    else -> "${minutosLento}min"
                }
                partes.add("Houve lentidão por $duracaoLento.")
            }
        }

        // v2.0: padroes horarios recorrentes
        val padraoHorario = detectarPadraoHorario(blocos)
        if (padraoHorario != null) {
            partes.add(padraoHorario)
        }

        // v2.0: tendencia de qualidade
        val tendencia = calcularTendencia(blocos)
        when (tendencia) {
            Tendencia.PIORANDO -> partes.add("A qualidade da sua rede está piorando nas últimas 24h.")
            Tendencia.MELHORANDO -> partes.add("A qualidade da sua rede está melhorando nas últimas 24h.")
            Tendencia.ESTAVEL -> { /* sem comentario extra */ }
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

    // ---------------------------------------------------------------------------
    // v2.0 — padroes horarios recorrentes
    // ---------------------------------------------------------------------------

    /**
     * Detecta se quedas OFFLINE ocorrem recorrentemente na mesma hora do dia,
     * em pelo menos [DIAS_MINIMOS_PADRAO_HORARIO] dias distintos.
     *
     * Retorna uma string legivel descrevendo o padrao, ou null se nenhum encontrado.
     */
    fun detectarPadraoHorario(blocos: List<BlocoUptime>): String? {
        val offlinePorHora = blocos
            .filter { it.status == StatusUptime.OFFLINE }
            .groupBy { it.dataHora.hour }

        // Para cada hora, conta em quantos dias distintos houve queda
        val horarioComPadrao = offlinePorHora
            .mapValues { (_, eventos) ->
                eventos.map { it.dataHora.toLocalDate() }.toSet().size
            }
            .filter { (_, diasDistintos) -> diasDistintos >= DIAS_MINIMOS_PADRAO_HORARIO }
            .maxByOrNull { (_, diasDistintos) -> diasDistintos }

        if (horarioComPadrao == null) return null

        val hora = horarioComPadrao.key
        val diasDistintos = horarioComPadrao.value
        val periodoStr = when (hora) {
            in 5..11 -> "de manhã"
            in 12..17 -> "à tarde"
            in 18..22 -> "à noite"
            else -> "de madrugada"
        }
        val horaStr = "%02dh".format(hora)
        return "Detectado padrão recorrente: quedas às ${horaStr} ($periodoStr) por $diasDistintos dias."
    }

    // ---------------------------------------------------------------------------
    // v2.0 — sequencias longas de OFFLINE (interrupcoes graves)
    // ---------------------------------------------------------------------------

    /**
     * Retorna todas as interrupcoes continuas de OFFLINE com duracao > 30 minutos,
     * ordenadas da mais longa para a mais curta.
     *
     * Cada [InterrupcaoOffline] contem o horario de inicio e a duracao em minutos.
     */
    fun detectarInterrupcoesLongas(blocos: List<BlocoUptime>): List<InterrupcaoOffline> {
        val interrupcoes = mutableListOf<InterrupcaoOffline>()
        var inicioAtual: LocalDateTime? = null
        var contadorAtual = 0

        for (bloco in blocos) {
            if (bloco.status == StatusUptime.OFFLINE) {
                if (contadorAtual == 0) inicioAtual = bloco.dataHora
                contadorAtual++
            } else {
                inicioAtual?.let { inicio ->
                    if (contadorAtual >= BLOCOS_INTERRUPCAO_LONGA) {
                        interrupcoes.add(
                            InterrupcaoOffline(
                                inicio = inicio,
                                duracaoMinutos = contadorAtual * MINUTOS_POR_BLOCO,
                            ),
                        )
                    }
                }
                contadorAtual = 0
                inicioAtual = null
            }
        }

        // Sequencia que termina no fim da lista
        if (contadorAtual >= BLOCOS_INTERRUPCAO_LONGA && inicioAtual != null) {
            interrupcoes.add(
                InterrupcaoOffline(
                    inicio = inicioAtual,
                    duracaoMinutos = contadorAtual * MINUTOS_POR_BLOCO,
                ),
            )
        }

        return interrupcoes.sortedByDescending { it.duracaoMinutos }
    }

    // ---------------------------------------------------------------------------
    // v2.0 — tendencia de qualidade
    // ---------------------------------------------------------------------------

    /**
     * Compara o uptime (% de blocos OK) das ultimas 24h vs as 24h anteriores.
     *
     * Retorna [Tendencia.MELHORANDO] se o uptime aumentou >5pp,
     * [Tendencia.PIORANDO] se caiu >5pp, ou [Tendencia.ESTAVEL] caso contrario.
     *
     * Se nao houver dados suficientes em qualquer janela, retorna [Tendencia.ESTAVEL].
     */
    fun calcularTendencia(blocos: List<BlocoUptime>): Tendencia {
        if (blocos.size < BLOCOS_POR_DIA * 2) return Tendencia.ESTAVEL

        // Ultimas 24h = ultimo BLOCOS_POR_DIA blocos
        val ultimas24h = blocos.takeLast(BLOCOS_POR_DIA)
        // 24h anteriores = BLOCOS_POR_DIA blocos antes das ultimas 24h
        val anteriores24h = blocos.dropLast(BLOCOS_POR_DIA).takeLast(BLOCOS_POR_DIA)

        val uptimeAtual = calcularUptimePercent(ultimas24h)
        val uptimeAnterior = calcularUptimePercent(anteriores24h)

        if (uptimeAtual == null || uptimeAnterior == null) return Tendencia.ESTAVEL

        val delta = uptimeAtual - uptimeAnterior
        return when {
            delta > 5.0 -> Tendencia.MELHORANDO
            delta < -5.0 -> Tendencia.PIORANDO
            else -> Tendencia.ESTAVEL
        }
    }

    /**
     * Calcula o percentual de blocos OK dentre os blocos medidos (exclui SEM_DADO).
     * Retorna null se nao houver blocos medidos suficientes (menos de 5).
     */
    private fun calcularUptimePercent(blocos: List<BlocoUptime>): Double? {
        val medidos = blocos.filter { it.status != StatusUptime.SEM_DADO }
        if (medidos.size < 5) return null
        val ok = medidos.count { it.status == StatusUptime.OK }
        return (ok.toDouble() / medidos.size) * 100.0
    }

    // ---------------------------------------------------------------------------
    // Helpers privados (v1.0, mantidos por compatibilidade interna)
    // ---------------------------------------------------------------------------

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

// ---------------------------------------------------------------------------
// Modelos de suporte v2.0
// ---------------------------------------------------------------------------

/** Representa uma interrupcao continua de OFFLINE com duracao acima do threshold. */
data class InterrupcaoOffline(
    val inicio: LocalDateTime,
    /** Duracao total em minutos (multiplo de 30). */
    val duracaoMinutos: Int,
)

/** Tendencia de qualidade da rede comparando as ultimas 24h vs as 24h anteriores. */
enum class Tendencia { MELHORANDO, PIORANDO, ESTAVEL }
