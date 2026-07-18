package io.signallq.app.ui.screen

import android.content.Context
import android.content.Intent
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.Share
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.FileProvider
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkStatusDot
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
    conectado: Boolean = true,
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    var gerando by remember { mutableStateOf(false) }
    var erro by remember { mutableStateOf<String?>(null) }

    val relatorio = snapshotDiagnostico.relatorio
    val decisao = relatorio?.decisao

    val compartilharLaudo: () -> Unit = {
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
                    conectado = conectado,
                )
            } catch (e: Exception) {
                erro = "Não foi possível gerar o PDF: ${e.message}"
            } finally {
                gerando = false
            }
        }
    }

    // #375: offline reaproveita a ultima medicao salva — exibir o timestamp da
    // medicao original, nunca o momento da consulta, para nao sugerir uma analise nova.
    val dataHoraEpochMs =
        if (!conectado) ultimaMedicao?.timestampEpochMs else null
    val dataHora =
        remember(dataHoraEpochMs) {
            val data = dataHoraEpochMs?.let { Date(it) } ?: Date()
            SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.of("pt", "BR")).format(data)
        }
    val headerTitulo =
        buildString {
            if (nomeUsuario.isNotBlank()) append("$nomeUsuario · ")
            if (operadora.isNotBlank()) append(operadora)
            if (velocidadeContratadaMbps != null && velocidadeContratadaMbps > 0) append(" $velocidadeContratadaMbps Mbps")
        }.ifBlank { "Diagnóstico de rede" }
    val headerSub =
        buildString {
            ssid?.let { append("SSID $it") }
            ipLocal?.let {
                if (isNotEmpty()) append(" · ")
                append(mascaraIpLocal(it))
            }
        }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        "Laudo de diagnóstico",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(
                        onClick = compartilharLaudo,
                        enabled = !gerando,
                    ) {
                        if (gerando) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = c.textPrimary,
                                strokeWidth = 2.dp,
                            )
                        } else {
                            Icon(
                                Icons.Outlined.Share,
                                contentDescription = "Compartilhar",
                            )
                        }
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = c.bgPrimary,
                        titleContentColor = c.textPrimary,
                        navigationIconContentColor = c.textPrimary,
                        actionIconContentColor = c.textPrimary,
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
            verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
        ) {
            // Banner de status — colorido por severidade da decisão
            if (decisao != null) {
                item {
                    val containerColor =
                        when (decisao.status) {
                            DiagnosticStatus.ok -> c.successContainer
                            DiagnosticStatus.attention -> c.warningContainer
                            DiagnosticStatus.critical -> c.errorContainer
                            DiagnosticStatus.inconclusive -> c.warningContainer
                            DiagnosticStatus.info -> c.primaryContainer
                        }
                    val textColor =
                        when (decisao.status) {
                            DiagnosticStatus.ok -> c.onSuccessContainer
                            DiagnosticStatus.attention -> c.onWarningContainer
                            DiagnosticStatus.critical -> c.onErrorContainer
                            DiagnosticStatus.inconclusive -> c.onWarningContainer
                            DiagnosticStatus.info -> c.onPrimaryContainer
                        }
                    val labelStatus =
                        when (decisao.status) {
                            DiagnosticStatus.ok -> "Conexão saudável"
                            DiagnosticStatus.attention -> "Atenção"
                            DiagnosticStatus.critical -> "Problema detectado"
                            DiagnosticStatus.inconclusive -> "Inconclusivo"
                            DiagnosticStatus.info -> "Informação"
                        }
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(containerColor)
                                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                            modifier = Modifier.weight(1f),
                        ) {
                            LkStatusDot(color = textColor)
                            Column {
                                Text(
                                    labelStatus,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.W600,
                                    color = textColor,
                                )
                                Spacer(Modifier.height(1.dp))
                                Text(
                                    decisao.titulo,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.W600,
                                    color = textColor,
                                )
                            }
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                "${relatorio.scoreConexao}",
                                style = MaterialTheme.typography.headlineLarge,
                                fontWeight = FontWeight.W700,
                                color = textColor,
                            )
                            Text(
                                relatorio.veredito,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.W600,
                                color = textColor,
                            )
                        }
                    }
                }
            }

            // Header
            item {
                Column {
                    Text(
                        "LAUDO TÉCNICO · $dataHora",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textTertiary,
                    )
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        headerTitulo,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W700,
                        color = c.textPrimary,
                    )
                    if (headerSub.isNotBlank()) {
                        Spacer(Modifier.height(2.dp))
                        Text(
                            headerSub,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textTertiary,
                        )
                    }
                    // #375: sem conexao no momento da consulta — deixa explicito que os
                    // dados exibidos sao de uma medicao anterior, nao uma analise nova.
                    if (!conectado) {
                        Spacer(Modifier.height(LkSpacing.xs))
                        Text(
                            "Sem conexão no momento · exibindo última medição salva",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.W600,
                            color = c.error,
                        )
                    }
                }
            }

            // RESUMO
            if (decisao != null) {
                item {
                    LaudoSection(titulo = "RESUMO", c = c) {
                        Text(
                            decisao.mensagemUsuario,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                        )
                    }
                }
            }

            // MÉTRICAS — grid 3×2
            if (ultimaMedicao != null) {
                item {
                    LaudoSection(titulo = "MÉTRICAS", c = c) {
                        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                            Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                                LaudoMetrica(
                                    label = "Download",
                                    valor = ultimaMedicao.downloadMbps?.let { "%.1f".format(it) } ?: "—",
                                    unidade = "Mbps",
                                    c = c,
                                    modifier = Modifier.weight(1f),
                                )
                                LaudoMetrica(
                                    label = "Upload",
                                    valor = ultimaMedicao.uploadMbps?.let { "%.1f".format(it) } ?: "—",
                                    unidade = "Mbps",
                                    c = c,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            HorizontalDivider(color = c.border, thickness = 0.5.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                                LaudoMetrica(
                                    label = "Latência",
                                    valor = ultimaMedicao.latencyMs?.let { "%.0f".format(it) } ?: "—",
                                    unidade = "ms",
                                    c = c,
                                    modifier = Modifier.weight(1f),
                                )
                                LaudoMetrica(
                                    label = "Jitter",
                                    valor = ultimaMedicao.jitterMs?.let { "%.0f".format(it) } ?: "—",
                                    unidade = "ms",
                                    c = c,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            HorizontalDivider(color = c.border, thickness = 0.5.dp)
                            Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                                LaudoMetrica(
                                    label = "Perda",
                                    valor = ultimaMedicao.perdaPercentual?.let { "%.1f".format(it) } ?: "—",
                                    unidade = "%",
                                    c = c,
                                    modifier = Modifier.weight(1f),
                                    nota = "estimado".takeIf { ultimaMedicao.packetLossSource == "estimated" },
                                )
                                LaudoMetrica(
                                    label = "Bufferbloat",
                                    valor = ultimaMedicao.bufferbloatMs?.let { "%.0f".format(it) } ?: "—",
                                    unidade = "ms",
                                    c = c,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // RECOMENDAÇÃO
            val recomendacao = decisao?.recomendacao
            if (!recomendacao.isNullOrBlank()) {
                item {
                    LaudoSection(titulo = "RECOMENDAÇÃO", c = c) {
                        Text(
                            recomendacao,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textSecondary,
                        )
                    }
                }
            }

            // Error message if PDF generation failed
            if (erro != null) {
                item {
                    Text(
                        erro!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = c.error,
                    )
                }
            }

            item {
                Button(
                    onClick = compartilharLaudo,
                    enabled = !gerando,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                    contentPadding = PaddingValues(vertical = 14.dp),
                ) {
                    if (gerando) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                        )
                        Spacer(Modifier.width(LkSpacing.sm))
                    } else {
                        Icon(
                            imageVector = Icons.Outlined.Share,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.sm))
                    }
                    Text(
                        "Compartilhar laudo em PDF",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.W600,
                    )
                }
            }
        }
    }
}

@Composable
private fun LaudoSection(
    titulo: String,
    c: LkTokens,
    content: @Composable () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .padding(LkSpacing.lg),
    ) {
        LkSectionOverline(titulo)
        Spacer(Modifier.height(LkSpacing.sm))
        content()
    }
}

@Composable
private fun LaudoMetrica(
    label: String,
    valor: String,
    unidade: String,
    c: LkTokens,
    modifier: Modifier = Modifier,
    nota: String? = null,
) {
    Column(modifier = modifier) {
        Text(
            label,
            style = MaterialTheme.typography.labelMedium,
            color = c.textTertiary,
        )
        Spacer(Modifier.height(2.dp))
        Row(verticalAlignment = Alignment.Bottom) {
            Text(
                valor,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
            Spacer(Modifier.width(3.dp))
            Text(
                unidade,
                style = MaterialTheme.typography.labelMedium,
                color = c.textSecondary,
                modifier = Modifier.padding(bottom = 2.dp),
            )
        }
        if (nota != null) {
            Text(
                nota,
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
        }
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
    conectado: Boolean = true,
) {
    val uri: Uri =
        withContext(Dispatchers.IO) {
            val document = PdfDocument()
            val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
            val page = document.startPage(pageInfo)
            val canvas = page.canvas

            // #375: offline reaproveita a ultima medicao salva — o comprovante (usado para
            // reclamacao formal na Anatel) precisa exibir o timestamp real da medicao.
            val dataHoraBase = if (!conectado) ultimaMedicao?.timestampEpochMs?.let { Date(it) } ?: Date() else Date()
            val dataHora = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.of("pt", "BR")).format(dataHoraBase)
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
            canvas.drawText("SignallQ — Laudo Técnico", margem, y, paintTitulo)
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
            if (!conectado) {
                canvas.drawText(
                    "Sem conexão no momento da geração — dados da última medição salva.",
                    margem,
                    y,
                    paintFooter,
                )
                y += 14f
            }
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
                    ipLocal?.let { add("IP local" to mascaraIpLocal(it)) }
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
            canvas.drawText("Gerado pelo app SignallQ", margem, 818f, paintFooter)

            document.finishPage(page)

            val dir =
                context.getExternalFilesDir(Environment.DIRECTORY_DOCUMENTS)
                    ?: context.cacheDir.resolve("laudos").also { it.mkdirs() }
            val arquivo = File(dir, "laudo_signallq_${System.currentTimeMillis()}.pdf")
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

/** Mascara o último octeto de um IPv4 para proteger dados sensíveis no laudo.
 * Ex: "192.168.1.100" → "192.168.1.*"
 * IPv6 e formatos não-IPv4 são retornados sem alteração.
 * Input é trimado para lidar com espaços acidentais. */
private fun mascaraIpLocal(ip: String): String {
    val partes = ip.trim().split(".")
    return if (partes.size == 4) "${partes[0]}.${partes[1]}.${partes[2]}.*" else ip.trim()
}
