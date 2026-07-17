package io.signallq.app.ui.screen

import android.content.Context
import android.content.Intent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.feature.history.ExportadorHistoricoCSV
import io.signallq.app.feature.history.ExportadorHistoricoPDF
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSheetFrame
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// ─── Enums de estado ──────────────────────────────────────────────────────────

enum class PeriodoExport(
    val label: String,
    val diasAtras: Int?,
) {
    SETE_DIAS("7 dias", 7),
    TRINTA_DIAS("30 dias", 30),
    TUDO("Tudo", null),
}

enum class FormatoExport(
    val label: String,
    val extensao: String,
    val descricao: String,
) {
    CSV("CSV", "csv", "Planilha compatível com Excel e Google Sheets"),
    PDF("PDF", "pdf", "Relatório formatado para impressão"),
}

// ─── ExportHistoricoBottomSheet ────────────────────────────────────────────────

/**
 * Bottom sheet de exportação de histórico.
 *
 * Gerencia internamente o estado de seleção de período e formato.
 * A lógica de export roda em background via coroutines.
 *
 * [onDismiss] deve fechar o sheet no pai.
 * [onExportSuccess] chamado após export bem-sucedido — pai pode abrir Intent.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ExportHistoricoBottomSheet(
    historico: List<MedicaoEntity>,
    snackbarHostState: SnackbarHostState,
    onDismiss: () -> Unit,
    onRetry: (() -> Unit)? = null,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var periodoSelecionado by remember { mutableStateOf(PeriodoExport.SETE_DIAS) }
    var formatoSelecionado by remember { mutableStateOf(FormatoExport.CSV) }
    var exportando by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        LkSheetFrame {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.pill))
                        .background(c.surfaceContainer)
                        .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                listOf("Seleção" to !exportando, "Exportando" to exportando).forEach { (label, ativo) ->
                    Box(
                        modifier =
                            Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(LkRadius.pill))
                                .background(if (ativo) c.secondaryContainer else androidx.compose.ui.graphics.Color.Transparent)
                                .padding(vertical = 10.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            label,
                            style = MaterialTheme.typography.labelSmall,
                            color = if (ativo) c.onSecondaryContainer else c.textSecondary,
                        )
                    }
                }
            }

            Spacer(Modifier.height(LkSpacing.lg))
            Text(
                text = "Exportar histórico",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )

            Spacer(Modifier.height(LkSpacing.lg))

            Text(
                text = "Período",
                style = MaterialTheme.typography.labelLarge,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                PeriodoExport.entries.forEach { periodo ->
                    FilterChip(
                        selected = periodoSelecionado == periodo,
                        onClick = { if (!exportando) periodoSelecionado = periodo },
                        label = { Text(periodo.label) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.secondaryContainer,
                                selectedLabelColor = c.onSecondaryContainer,
                            ),
                    )
                }
            }

            Spacer(Modifier.height(LkSpacing.lg))

            Text(
                text = "Formato",
                style = MaterialTheme.typography.labelLarge,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.xs))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                FormatoExport.entries.forEach { formato ->
                    FilterChip(
                        selected = formatoSelecionado == formato,
                        onClick = { if (!exportando) formatoSelecionado = formato },
                        label = { Text(formato.label) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                selectedContainerColor = c.secondaryContainer,
                                selectedLabelColor = c.onSecondaryContainer,
                            ),
                    )
                }
            }
            Spacer(Modifier.height(LkSpacing.xs))
            Text(
                text = formatoSelecionado.descricao,
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )

            Spacer(Modifier.height(LkSpacing.xl))

            Button(
                onClick = {
                    scope.launch {
                        exportando = true
                        val resultado =
                            executarExport(
                                context = context,
                                historico = historico,
                                periodo = periodoSelecionado,
                                formato = formatoSelecionado,
                            )
                        exportando = false

                        if (resultado != null) {
                            abrirIntentCompartilhamento(context, resultado, formatoSelecionado)
                            onDismiss()
                        } else {
                            snackbarHostState
                                .showSnackbar(
                                    message = "Não foi possível exportar",
                                    actionLabel = "Tentar novamente",
                                ).let { result ->
                                    if (result == androidx.compose.material3.SnackbarResult.ActionPerformed) {
                                        onRetry?.invoke()
                                    }
                                }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                enabled = historico.isNotEmpty() && !exportando,
                shape = RoundedCornerShape(LkRadius.button),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
            ) {
                Text(
                    text = if (exportando) "Exportando..." else "Exportar",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.W600,
                )
            }

            if (exportando) {
                Spacer(Modifier.height(LkSpacing.sm))
                LinearProgressIndicator(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .height(4.dp)
                            .clip(RoundedCornerShape(2.dp)),
                    color = c.primary,
                    trackColor = c.surfaceContainerHighest,
                )
            }
        }
    }
}

// ─── Lógica de export ─────────────────────────────────────────────────────────

private suspend fun executarExport(
    context: Context,
    historico: List<MedicaoEntity>,
    periodo: PeriodoExport,
    formato: FormatoExport,
): File? {
    val medicoesParaExportar = filtrarPorPeriodo(historico, periodo)
    if (medicoesParaExportar.isEmpty()) return null

    val timestamp = SimpleDateFormat("yyyyMMdd_HHmm", Locale.US).format(Date())
    val nomeArquivo = "signallq_historico_$timestamp.${formato.extensao}"
    val arquivo = File(context.cacheDir, nomeArquivo)

    val sucesso =
        when (formato) {
            FormatoExport.CSV -> ExportadorHistoricoCSV().exportar(medicoesParaExportar, arquivo)
            FormatoExport.PDF -> ExportadorHistoricoPDF().exportarComWebView(medicoesParaExportar, arquivo, context)
        }

    return if (sucesso) arquivo else null
}

private fun filtrarPorPeriodo(
    historico: List<MedicaoEntity>,
    periodo: PeriodoExport,
): List<MedicaoEntity> {
    val diasAtras = periodo.diasAtras ?: return historico
    val cutoff = System.currentTimeMillis() - (diasAtras * 24 * 3600 * 1000L)
    return historico.filter { it.timestampEpochMs >= cutoff }
}

private fun abrirIntentCompartilhamento(
    context: Context,
    arquivo: File,
    formato: FormatoExport,
) {
    val mimeType =
        when (formato) {
            FormatoExport.CSV -> "text/csv"
            FormatoExport.PDF -> "application/pdf"
        }
    val uri =
        FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            arquivo,
        )
    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(intent, "Compartilhar histórico"))
}
