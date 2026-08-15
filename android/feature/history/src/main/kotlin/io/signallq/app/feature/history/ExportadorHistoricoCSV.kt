package io.signallq.app.feature.history

import io.signallq.app.core.database.MedicaoEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Exporta historico de medicoes para CSV.
 *
 * Campos exportados: Data, Hora, Download, Upload, Latencia, Jitter, Perda, Bufferbloat, Fonte.
 * Medicoes do monitor (fonte="monitor") aparecem sem download/upload (campos vazios).
 */
class ExportadorHistoricoCSV {

    private val formatadorData = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    private val formatadorHora = SimpleDateFormat("HH:mm:ss", Locale.US)

    suspend fun exportar(
        medicoes: List<MedicaoEntity>,
        arquivo: File,
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            val csv = buildString {
                appendLine("Data,Hora,Download (Mbps),Upload (Mbps),Latência (ms),Jitter (ms),Perda (%),Bufferbloat (ms),Fonte")
                medicoes.forEach { medicao ->
                    val date = Date(medicao.timestampEpochMs)
                    val data = formatadorData.format(date)
                    val hora = formatadorHora.format(date)
                    appendLine(
                        "$data,$hora," +
                            "${medicao.downloadMbps.csvValor()}," +
                            "${medicao.uploadMbps.csvValor()}," +
                            "${medicao.latencyMs.csvValor()}," +
                            "${medicao.jitterMs.csvValor()}," +
                            "${medicao.perdaPercentual.csvValor()}," +
                            "${medicao.bufferbloatMs.csvValor()}," +
                            "${medicao.fonte ?: ""}",
                    )
                }
            }
            arquivo.writeText(csv, Charsets.UTF_8)
            true
        } catch (e: Exception) {
            false
        }
    }

    /** Formata Double? para CSV: null vira string vazia, valor vira 2 casas decimais. */
    private fun Double?.csvValor(): String =
        if (this == null) "" else String.format(Locale.US, "%.2f", this)
}
