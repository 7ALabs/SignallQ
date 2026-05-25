package io.linka.app.kotlin.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.linka.app.kotlin.core.database.MedicaoEntity
import io.linka.app.kotlin.feature.diagnostico.SnapshotDiagnostico
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.component.rememberTopBarAlpha
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaudoScreen(
    snapshotDiagnostico: SnapshotDiagnostico,
    ultimaMedicao: MedicaoEntity?,
    nomeUsuario: String,
    operadora: String,
    ssid: String?,
    ipLocal: String?,
    ipPublico: String?,
    onVoltar: () -> Unit,
    velocidadeContratadaMbps: Int? = null,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val uriHandler = LocalUriHandler.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val topBarAlpha = listState.rememberTopBarAlpha()
    var gerando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val relatorio = snapshotDiagnostico.relatorio
    val decisao = relatorio?.decisao

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                modifier = Modifier.graphicsLayer { alpha = topBarAlpha },
                title = {
                    Text(
                        "Comprovante para a Anatel",
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.W600,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = c.bgPrimary,
                        titleContentColor = c.textPrimary,
                        navigationIconContentColor = c.textPrimary,
                    ),
            )
        },
    ) { padding ->
        LazyColumn(
            state = listState,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
            contentPadding =
                PaddingValues(
                    start = LkSpacing.lg,
                    end = LkSpacing.lg,
                    top = 0.dp,
                    bottom = LkSpacing.xxl,
                ),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            item {
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    "Resumo do diagnóstico de rede",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }

            if (ultimaMedicao != null) {
                item {
                    LaudoCard(c = c, titulo = "Teste de velocidade") {
                        LaudoRow(c, "Download", ultimaMedicao.downloadMbps?.let { "%.1f Mbps".format(it) } ?: "—")
                        LaudoRow(c, "Upload", ultimaMedicao.uploadMbps?.let { "%.1f Mbps".format(it) } ?: "—")
                        LaudoRow(c, "Latência", ultimaMedicao.latencyMs?.let { "%.0f ms".format(it) } ?: "—")
                        LaudoRow(c, "Oscilação", ultimaMedicao.jitterMs?.let { "%.0f ms".format(it) } ?: "—")
                        LaudoRow(c, "Perda de pacotes", ultimaMedicao.perdaPercentual?.let { "%.1f%%".format(it) } ?: "—")
                    }
                }
            }

            if (decisao != null) {
                item {
                    LaudoCard(c = c, titulo = "Diagnóstico") {
                        LaudoRow(c, "Veredito", decisao.titulo)
                        LaudoRow(c, "Resumo", decisao.mensagemUsuario)
                        decisao.recomendacao?.let { LaudoRow(c, "Recomendação", it) }
                        ultimaMedicao?.gargaloPrimario?.let { LaudoRow(c, "Gargalo primário", it) }
                    }
                }
            }

            val linhasRede =
                listOfNotNull(
                    ssid?.let { "Wi-Fi (SSID)" to it },
                    ipLocal?.let { "IP local" to it },
                    ipPublico?.let { "IP público" to it },
                    operadora.takeIf { it.isNotBlank() }?.let { "Operadora" to it },
                )
            if (linhasRede.isNotEmpty()) {
                item {
                    LaudoCard(c = c, titulo = "Rede") {
                        linhasRede.forEach { (label, valor) ->
                            LaudoRow(c, label, valor)
                        }
                    }
                }
            }

            item {
                Spacer(Modifier.height(LkSpacing.sm))
                Text(
                    text = "Comprovante técnico de qualidade",
                    style = MaterialTheme.typography.titleMedium,
                    color = c.textPrimary,
                    fontWeight = FontWeight.W600,
                )
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    text =
                        "Documento com seus dados de conexão medidos pelo Linka. " +
                            "Aceito como prova em reclamações na Anatel, no Procon e junto à sua operadora.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
                Spacer(Modifier.height(LkSpacing.md))
                erro?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = LkColors.error)
                    Spacer(Modifier.height(LkSpacing.sm))
                }
                Button(
                    onClick = {
                        scope.launch {
                            gerando = true
                            erro = null
                            try {
                                gerarECompartilharLaudo(
                                    context = context,
                                    snapshotDiagnostico = snapshotDiagnostico,
                                    ultimaMedicao = ultimaMedicao,
                                    nomeUsuario = nomeUsuario,
                                    operadora = operadora,
                                    ssid = ssid,
                                    ipLocal = ipLocal,
                                    ipPublico = ipPublico,
                                    velocidadeContratadaMbps = velocidadeContratadaMbps,
                                )
                            } catch (e: Exception) {
                                erro = "Não foi possível gerar o PDF: ${e.message}"
                            } finally {
                                gerando = false
                            }
                        }
                    },
                    enabled = !gerando,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                    shape = RoundedCornerShape(LkRadius.button),
                ) {
                    if (gerando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = c.bgPrimary,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Icon(
                            Icons.AutoMirrored.Outlined.Article,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(if (gerando) "Gerando PDF…" else "Gerar e compartilhar comprovante")
                }
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    text = "Registrar reclamação na Anatel →",
                    style = MaterialTheme.typography.bodySmall,
                    color = LkColors.accent,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                role = Role.Button
                                contentDescription = "Registrar reclamação na Anatel"
                            }
                            .clickable { uriHandler.openUri("https://www.anatel.gov.br/consumidor") },
                )
            }
        }
    }
}

@Composable
private fun LaudoCard(
    c: LkTokens,
    titulo: String,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        Text(
            titulo,
            style = MaterialTheme.typography.titleSmall,
            color = c.textPrimary,
            fontWeight = FontWeight.W600,
        )
        HorizontalDivider(color = c.border, thickness = 1.dp)
        content()
    }
}

@Composable
private fun LaudoRow(
    c: LkTokens,
    label: String,
    valor: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            label,
            style = MaterialTheme.typography.bodySmall,
            color = c.textSecondary,
            modifier = Modifier.weight(0.45f),
        )
        Text(
            valor,
            style = MaterialTheme.typography.bodySmall,
            color = c.textPrimary,
            fontWeight = FontWeight.W500,
            modifier = Modifier.weight(0.55f),
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

private suspend fun gerarECompartilharLaudo(
    context: Context,
    snapshotDiagnostico: SnapshotDiagnostico,
    ultimaMedicao: MedicaoEntity?,
    nomeUsuario: String,
    operadora: String,
    ssid: String?,
    ipLocal: String?,
    ipPublico: String?,
    velocidadeContratadaMbps: Int? = null,
) {
    val uri: Uri =
        withContext(Dispatchers.IO) {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            val dataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale("pt", "BR")).format(Date())
            val margem = 40f
            val colValor = 180f

            val paintTitulo =
                Paint().apply {
                    textSize = 20f
                    isFakeBoldText = true
                    color = LkColors.Light.textPrimary.toArgb()
                }
            val paintSubtitulo =
                Paint().apply {
                    textSize = 10f
                    color = LkColors.Light.textSecondary.toArgb()
                }
            val paintSecao =
                Paint().apply {
                    textSize = 12f
                    isFakeBoldText = true
                    color = LkColors.Light.textPrimary.toArgb()
                }
            val paintLabel =
                Paint().apply {
                    textSize = 10f
                    color = LkColors.Light.textSecondary.toArgb()
                }
            val paintValor =
                Paint().apply {
                    textSize = 10f
                    color = LkColors.Light.textPrimary.toArgb()
                }
            val paintLinha =
                Paint().apply {
                    color = LkColors.Light.border.toArgb()
                    strokeWidth = 0.5f
                    style = Paint.Style.STROKE
                }
            val paintFooter =
                Paint().apply {
                    textSize = 8f
                    color = LkColors.Light.textTertiary.toArgb()
                }

            fun truncar(
                texto: String,
                max: Int = 65,
            ) = if (texto.length > max) texto.take(max - 1) + "…" else texto

            var y = 55f

            // Cabeçalho
            canvas.drawText("LINKA — Laudo Técnico", margem, y, paintTitulo)
            y += 18f
            canvas.drawText("Diagnóstico de rede doméstica", margem, y, paintSubtitulo)
            y += 14f
            val metaLinha =
                buildString {
                    append("Data: $dataHora")
                    if (nomeUsuario.isNotBlank()) append("   |   Usuário: $nomeUsuario")
                }
            canvas.drawText(metaLinha, margem, y, paintSubtitulo)
            y += 14f
            canvas.drawLine(margem, y, 595f - margem, y, paintLinha)
            y += 18f

            // Seção: Teste de Velocidade
            if (ultimaMedicao != null) {
                canvas.drawText("TESTE DE VELOCIDADE", margem, y, paintSecao)
                y += 16f
                listOf(
                    "Download" to (ultimaMedicao.downloadMbps?.let { "%.1f Mbps".format(it) } ?: "—"),
                    "Upload" to (ultimaMedicao.uploadMbps?.let { "%.1f Mbps".format(it) } ?: "—"),
                    "Latência" to (ultimaMedicao.latencyMs?.let { "%.0f ms".format(it) } ?: "—"),
                    "Oscilação" to (ultimaMedicao.jitterMs?.let { "%.0f ms".format(it) } ?: "—"),
                    "Perda de pacotes" to (ultimaMedicao.perdaPercentual?.let { "%.1f%%".format(it) } ?: "—"),
                ).forEach { (label, valor) ->
                    canvas.drawText(label, margem, y, paintLabel)
                    canvas.drawText(valor, colValor, y, paintValor)
                    y += 16f
                }
                y += 6f
                canvas.drawLine(margem, y, 595f - margem, y, paintLinha)
                y += 16f
            }

            // Seção: Diagnóstico
            val decisao = snapshotDiagnostico.relatorio?.decisao
            if (decisao != null) {
                canvas.drawText("DIAGNÓSTICO", margem, y, paintSecao)
                y += 16f
                val linhasDiagnostico =
                    buildList {
                        add("Veredito" to truncar(decisao.titulo))
                        add("Resumo" to truncar(decisao.mensagemUsuario))
                        decisao.recomendacao?.let { add("Recomendação" to truncar(it)) }
                        ultimaMedicao?.gargaloPrimario?.let { add("Gargalo primário" to it) }
                    }
                linhasDiagnostico.forEach { (label, valor) ->
                    canvas.drawText(label, margem, y, paintLabel)
                    canvas.drawText(valor, colValor, y, paintValor)
                    y += 16f
                }
                y += 6f
                canvas.drawLine(margem, y, 595f - margem, y, paintLinha)
                y += 16f
            }

            // Seção: Rede
            val linhasRede =
                buildList {
                    ssid?.let { add("Wi-Fi (SSID)" to it) }
                    ipLocal?.let { add("IP local" to it) }
                    ipPublico?.let { add("IP público" to it) }
                    if (operadora.isNotBlank()) add("Operadora" to operadora)
                }
            if (linhasRede.isNotEmpty()) {
                canvas.drawText("REDE", margem, y, paintSecao)
                y += 16f
                linhasRede.forEach { (label, valor) ->
                    canvas.drawText(label, margem, y, paintLabel)
                    canvas.drawText(valor, colValor, y, paintValor)
                    y += 16f
                }
            }

            // Seção: Conformidade ANATEL
            if (velocidadeContratadaMbps != null && velocidadeContratadaMbps > 0) {
                val downloadMedido = ultimaMedicao?.downloadMbps
                val minimoAnatel = velocidadeContratadaMbps * 0.40
                val metaIdeal = velocidadeContratadaMbps * 0.80
                val percentualEntregue = downloadMedido?.let { (it / velocidadeContratadaMbps.toDouble()) * 100 }

                if (y + 140f < 790f) {
                    y += 6f
                    canvas.drawLine(margem, y, 595f - margem, y, paintLinha)
                    y += 16f
                    canvas.drawText("CONFORMIDADE ANATEL", margem, y, paintSecao)
                    y += 16f
                    listOf(
                        "Velocidade contratada" to "$velocidadeContratadaMbps Mbps",
                        "Mínimo garantido ANATEL (40%)" to "%.1f Mbps".format(minimoAnatel),
                        "Meta ideal (80%)" to "%.1f Mbps".format(metaIdeal),
                        "Velocidade medida (download)" to (downloadMedido?.let { "%.1f Mbps".format(it) } ?: "—"),
                        "Entrega" to (percentualEntregue?.let { "%.0f%% do plano contratado".format(it) } ?: "—"),
                    ).forEach { (label, valor) ->
                        canvas.drawText(label, margem, y, paintLabel)
                        canvas.drawText(valor, colValor, y, paintValor)
                        y += 16f
                    }
                }

                // Rodapé ANATEL (texto longo — desenhar manualmente com quebra)
                val rodapeAnatel = "De acordo com as normas da ANATEL, sua operadora é obrigada a entregar pelo menos 80% da velocidade contratada."
                val rodapeAnatel2 =
                    "Se ficar abaixo do mínimo por período prolongado, você tem direito a desconto ou cancelamento " +
                        "sem multa (Res. nº 574/2011, RQUAL)."
                if (y + 40f < 795f) {
                    y += 8f
                    canvas.drawText(rodapeAnatel, margem, y, paintFooter)
                    y += 12f
                    canvas.drawText(rodapeAnatel2, margem, y, paintFooter)
                }
            }

            // Rodapé
            canvas.drawLine(margem, 800f, 595f - margem, 800f, paintLinha)
            canvas.drawText("Gerado pelo app LINKA", margem, 818f, paintFooter)

            document.finishPage(page)

            val dir =
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.cacheDir.resolve("laudos").also { it.mkdirs() }
            val arquivo = File(dir, "laudo_linka_${System.currentTimeMillis()}.pdf")
            FileOutputStream(arquivo).use { document.writeTo(it) }
            document.close()

            FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", arquivo)
        }

    val intent =
        Intent(Intent.ACTION_SEND).apply {
            type = "application/pdf"
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
    context.startActivity(Intent.createChooser(intent, "Compartilhar laudo"))
}
