package io.linka.app.kotlin.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.linka.app.kotlin.feature.history.BlocoUptime
import io.linka.app.kotlin.feature.history.StatusUptime
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LocalLkTokens
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.TextStyle
import java.util.Locale

// ─── Constants ────────────────────────────────────────────────────────────────

private const val LINHAS_DIAS = 7 // dias exibidos
private const val COLUNAS_TEMPO = 48 // blocos de 30min por dia

// ─── UptimeGridChart ──────────────────────────────────────────────────────────

/**
 * Lista de eventos de uptime dos últimos 7 dias.
 *
 * [blocos] deve ter exatamente 336 itens, do mais antigo (índice 0) ao mais
 * recente (índice 335), conforme produzido pelo UptimeChartUseCase.
 *
 * Assinatura mantida para compatibilidade com HistoricoScreen.
 * [diaSelecionado] e [onDiaSelecionado] são mantidos mas ignorados — o grid Canvas
 * foi substituído por lista de eventos textual.
 */
@Composable
fun UptimeGridChart(
    blocos: List<BlocoUptime>,
    narrativa: String,
    diaSelecionado: Int? = null,
    onDiaSelecionado: (Int) -> Unit = {},
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val diasLabels = remember(blocos) { calcularLabelsDiasCompletos(blocos) }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        // ── Narrativa ──
        if (narrativa.isNotBlank()) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(LkRadius.card),
                colors = CardDefaults.cardColors(containerColor = c.bgCard),
            ) {
                Text(
                    text = narrativa,
                    modifier = Modifier.padding(LkSpacing.lg),
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
            Spacer(Modifier.height(LkSpacing.xs))
        }

        // ── Lista de eventos por dia (mais recente primeiro) ──
        // Os dados chegam do mais antigo (0) ao mais recente (335).
        // Dia 0 = mais antigo, Dia 6 = hoje → exibir em ordem reversa.
        (LINHAS_DIAS - 1 downTo 0).forEach { diaIdx ->
            val inicio = diaIdx * COLUNAS_TEMPO
            val fim = minOf(inicio + COLUNAS_TEMPO, blocos.size)
            val diaBlocos = if (inicio < blocos.size) blocos.subList(inicio, fim) else emptyList()

            val totalMedidos = diaBlocos.count { it.status != StatusUptime.SEM_DADO }
            val okCount = diaBlocos.count { it.status == StatusUptime.OK }
            val uptimePct = if (totalMedidos > 0) (okCount * 100) / totalMedidos else 100

            val labelDia = diasLabels.getOrElse(diaIdx) { "Dia ${diaIdx + 1}" }

            val barCor =
                when {
                    uptimePct == 100 -> LkColors.success
                    uptimePct >= 95 -> LkColors.success.copy(alpha = 0.7f)
                    uptimePct >= 80 -> LkColors.warning
                    else -> LkColors.error
                }
            val pctCor = barCor

            // Calcular períodos offline
            val resumoOffline = calcularResumoOffline(diaBlocos)

            Column(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    // Coluna de data
                    Text(
                        text = labelDia,
                        modifier = Modifier.width(52.dp),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                        maxLines = 1,
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    // Barra de uptime
                    Box(modifier = Modifier.weight(1f)) {
                        LinearProgressIndicator(
                            progress = { uptimePct / 100f },
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                            color = barCor,
                            trackColor = c.border.copy(alpha = 0.3f),
                            strokeCap = StrokeCap.Round,
                        )
                    }
                    Spacer(Modifier.width(LkSpacing.sm))
                    // Percentual
                    Text(
                        text = "$uptimePct%",
                        modifier = Modifier.width(36.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = pctCor,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                    )
                }
                // Sub-item offline
                if (resumoOffline != null) {
                    Text(
                        text = "· $resumoOffline",
                        modifier = Modifier.padding(start = 60.dp, top = 2.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textTertiary,
                    )
                }
            }
        }
    }
}

// ─── Helpers ─────────────────────────────────────────────────────────────────

/**
 * Retorna labels legíveis para cada um dos 7 dias.
 * Dia 0 = mais antigo, Dia 6 = hoje.
 */
private fun calcularLabelsDiasCompletos(blocos: List<BlocoUptime>): List<String> {
    val hoje = LocalDate.now()
    return (0 until LINHAS_DIAS).map { diaIdx ->
        val idx = diaIdx * COLUNAS_TEMPO
        if (idx < blocos.size) {
            val dt: LocalDateTime = blocos[idx].dataHora
            val data = dt.toLocalDate()
            when {
                data == hoje -> "Hoje"
                data == hoje.minusDays(1) -> "Ontem"
                else ->
                    dt.dayOfWeek
                        .getDisplayName(TextStyle.SHORT, Locale.forLanguageTag("pt-BR"))
                        .replaceFirstChar { it.uppercaseChar() }
                        .take(3)
            }
        } else {
            ""
        }
    }
}

/**
 * Calcula resumo textual dos períodos offline de um dia.
 * Cada bloco = 30 min.
 * Retorna null se não houver blocos offline.
 */
private fun calcularResumoOffline(diaBlocos: List<BlocoUptime>): String? {
    if (diaBlocos.isEmpty()) return null

    // Encontrar sequências consecutivas de OFFLINE
    data class Periodo(
        val inicio: Int,
        val tamanho: Int,
        val hora: LocalDateTime?,
    )

    val periodos = mutableListOf<Periodo>()
    var emPeriodo = false
    var inicioIdx = 0

    diaBlocos.forEachIndexed { i, bloco ->
        if (bloco.status == StatusUptime.OFFLINE) {
            if (!emPeriodo) {
                emPeriodo = true
                inicioIdx = i
            }
        } else {
            if (emPeriodo) {
                emPeriodo = false
                val tamanho = i - inicioIdx
                periodos.add(Periodo(inicioIdx, tamanho, diaBlocos[inicioIdx].dataHora))
            }
        }
    }
    // Fechar período se chegou ao fim ainda aberto
    if (emPeriodo) {
        val tamanho = diaBlocos.size - inicioIdx
        periodos.add(Periodo(inicioIdx, tamanho, diaBlocos[inicioIdx].dataHora))
    }

    if (periodos.isEmpty()) return null

    val totalMinutos = periodos.sumOf { it.tamanho * 30 }

    return if (periodos.size == 1) {
        val hora = periodos[0].hora
        if (hora != null) {
            val hh = hora.hour.toString().padStart(2, '0')
            val mm = hora.minute.toString().padStart(2, '0')
            "$totalMinutos min offline às ${hh}h$mm"
        } else {
            "$totalMinutos min offline"
        }
    } else {
        "$totalMinutos min offline em ${periodos.size} períodos"
    }
}
