package io.veloo.app.feature.history

import io.veloo.app.core.database.MedicaoDao
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

// ---------------------------------------------------------------------------
// Modelos de dominio
// ---------------------------------------------------------------------------

enum class StatusUptime { OK, LENTO, OFFLINE, SEM_DADO }

data class BlocoUptime(
    val dataHora: LocalDateTime,
    val status: StatusUptime,
    /** Latencia representativa do bloco (primeira medicao valida), para tooltip. */
    val latencyMs: Int?,
    /** Media de latencia de todas as medicoes do bloco. */
    val latencyMediaMs: Int?,
)

// ---------------------------------------------------------------------------
// Thresholds de classificacao
// ---------------------------------------------------------------------------

private const val LATENCY_OK_MAX_MS = 300
private const val LATENCY_LENTO_MAX_MS = 800
private const val JANELA_MINUTOS = 30L
private const val DIAS_HISTORICO = 7
private const val BLOCOS_POR_DIA = 48 // 24h / 30min
private const val TOTAL_BLOCOS = DIAS_HISTORICO * BLOCOS_POR_DIA // 336

// ---------------------------------------------------------------------------
// Use case
// ---------------------------------------------------------------------------

/**
 * Agrupa medicoes Room dos ultimos 7 dias em janelas de 30 minutos
 * e classifica cada bloco com [StatusUptime].
 *
 * Medicoes com fonte = "monitor" sao preferidas pois refletem o uptime
 * continuo. Medicoes de speedtest completo tambem contribuem quando
 * presentes na janela.
 *
 * Retorna exatamente [TOTAL_BLOCOS] blocos ordenados do mais antigo para
 * o mais recente.
 */
class UptimeChartUseCase(
    private val medicaoDao: MedicaoDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
) {

    suspend fun gerar7dias(): List<BlocoUptime> {
        val agora = System.currentTimeMillis()
        val seteDiasAtras = agora - (DIAS_HISTORICO * 24 * 3600 * 1000L)

        val medicoes = withContext(ioDispatcher) { medicaoDao.buscarDesde(seteDiasAtras) }

        // Gera os TOTAL_BLOCOS slots de 30min, do mais antigo para o mais recente.
        // O slot mais recente e o que inclui "agora".
        return (TOTAL_BLOCOS - 1 downTo 0).map { blocoAtras ->
            val slotFimMs = agora - (blocoAtras * JANELA_MINUTOS * 60 * 1000L)
            val slotInicioMs = slotFimMs - (JANELA_MINUTOS * 60 * 1000L)

            val medicoesNoBloco = medicoes.filter { m ->
                m.timestampEpochMs in slotInicioMs until slotFimMs
            }

            val dataHora = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(slotInicioMs),
                ZoneId.systemDefault(),
            )

            classificarBloco(dataHora, medicoesNoBloco)
        }
    }

    private fun classificarBloco(
        dataHora: LocalDateTime,
        medicoes: List<io.veloo.app.core.database.MedicaoEntity>,
    ): BlocoUptime {
        if (medicoes.isEmpty()) {
            return BlocoUptime(
                dataHora = dataHora,
                status = StatusUptime.SEM_DADO,
                latencyMs = null,
                latencyMediaMs = null,
            )
        }

        val latencias = medicoes.mapNotNull { it.latencyMs?.toInt() }.filter { it > 0 }

        if (latencias.isEmpty()) {
            // Medicoes existem mas sem latencia mensuravel — pode ser medicao de monitor
            // onde a conexao estava indisponivel (latencia null = OFFLINE)
            return BlocoUptime(
                dataHora = dataHora,
                status = StatusUptime.OFFLINE,
                latencyMs = null,
                latencyMediaMs = null,
            )
        }

        val mediaLatencia = latencias.average().toInt()
        val primeiraLatencia = latencias.first()

        val status = when {
            mediaLatencia <= LATENCY_OK_MAX_MS -> StatusUptime.OK
            mediaLatencia <= LATENCY_LENTO_MAX_MS -> StatusUptime.LENTO
            else -> StatusUptime.OFFLINE
        }

        return BlocoUptime(
            dataHora = dataHora,
            status = status,
            latencyMs = primeiraLatencia,
            latencyMediaMs = mediaLatencia,
        )
    }
}
