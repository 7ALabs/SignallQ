package io.signallq.pro.feature.medicaodiagnostico

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.TrendingUp
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.SwapHoriz
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ListRow
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProButtonVariant
import io.signallq.pro.core.designsystem.QualityGauge
import io.signallq.pro.core.designsystem.RecommendationBlock
import io.signallq.pro.core.designsystem.RecommendationPriority
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant
import io.signallq.pro.core.designsystem.StatusChip
import io.signallq.pro.core.designsystem.StatusChipTone
import io.signallq.pro.core.designsystem.TopBar
import io.signallq.pro.core.designsystem.TopBarLeading

private const val RSSI_PROGRESSO_MIN_DBM = -90.0
private const val RSSI_PROGRESSO_MAX_DBM = -30.0

/**
 * Tela 2.11 -- Walk Test. RSSI amostrado ao vivo por [WalkTestViewModel] (polling real via
 * `WifiManager`, não mockado, issue #1176). Segue o tema ambiente (claro/escuro pelo sistema)
 * como qualquer outra tela do Pro -- dark mode NÃO é forçado aqui.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WalkTestScreen(
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WalkTestViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) { viewModel.iniciarSessao() }

    Scaffold(
        topBar = {
            TopBar(
                titulo = "Walk Test",
                subtitulo = "Sessão: ${uiState.tempoSessaoFormatado}",
                leading = TopBarLeading.VOLTAR,
                onLeadingClick = onVoltar,
            )
        },
    ) { paddingValues ->
        if (!uiState.permissaoConcedida) {
            Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(24.dp)) {
                StateCard(
                    variant = StateCardVariant.ERRO,
                    titulo = "Permissão de localização necessária",
                    mensagem =
                        "O Walk Test precisa da permissão de localização para ler o sinal Wi-Fi em " +
                            "tempo real. Conceda em Ajustes > Permissões e volte para esta tela.",
                )
            }
            return@Scaffold
        }

        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            DestaqueRssi(uiState)
            AcoesSessao(uiState, onMarcarCandidato = viewModel::marcarPontoCandidato, onSalvarMedicao = viewModel::salvarMedicao)
            HorizontalDivider()
            MetricasSessao(uiState)
        }
    }
}

@Composable
private fun DestaqueRssi(uiState: WalkTestUiState) {
    QualityGauge(
        valorFormatado = uiState.rssiAtual?.toString() ?: "--",
        unidade = "dBm · RSSI atual",
        veredito = rotuloQualidade(uiState.qualidade),
        progresso = progressoRssi(uiState.rssiAtual),
    )
    StatusChip(
        texto = rotuloStatusChip(uiState),
        tone = toneQualidade(uiState.qualidade),
    )
    GraficoVariacaoRssi(uiState.historico)

    uiState.deltaPontoCandidatoDbm?.let { delta ->
        RecommendationBlock(
            problema = "Ponto candidato identificado",
            impacto = "Sinal $delta dB melhor que o pior ponto medido nesta sessão.",
            acao = "Considere marcar este ponto para reposicionamento do roteador ou repetidor.",
            prioridade = RecommendationPriority.INFO,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun AcoesSessao(
    uiState: WalkTestUiState,
    onMarcarCandidato: () -> Unit,
    onSalvarMedicao: () -> Unit,
) {
    ProButton(
        texto = "Marcar ponto candidato",
        onClick = onMarcarCandidato,
        habilitado = uiState.rssiAtual != null,
        modifier = Modifier.fillMaxWidth(),
    )
    ProButton(
        texto = "Salvar medição",
        onClick = onSalvarMedicao,
        habilitado = uiState.rssiAtual != null,
        variant = ProButtonVariant.SECUNDARIO,
        modifier = Modifier.fillMaxWidth(),
    )
    if (uiState.pontosSalvosNaSessao > 0) {
        Text(
            text = "${uiState.pontosSalvosNaSessao} ponto(s) salvo(s) nesta sessão",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun MetricasSessao(uiState: WalkTestUiState) {
    ListRow(
        titulo = "Velocidade do link",
        subtitulo = uiState.linkSpeedMbps?.let { "$it Mbps" } ?: "Aguardando leitura",
        icone = Icons.Outlined.Speed,
    )
    ListRow(
        titulo = "Canal e banda",
        subtitulo = uiState.canalBanda ?: "Aguardando leitura",
        icone = Icons.Outlined.Router,
    )
    ListRow(
        titulo = "Rede",
        subtitulo = uiState.ssid ?: "Aguardando leitura",
        icone = Icons.Outlined.NetworkWifi,
    )
    ListRow(
        titulo = "Faixa de RSSI da sessão",
        subtitulo =
            if (uiState.rssiMinSessao != null && uiState.rssiMaxSessao != null) {
                "Mín ${uiState.rssiMinSessao} dBm · Máx ${uiState.rssiMaxSessao} dBm"
            } else {
                "Aguardando leitura"
            },
        icone = Icons.AutoMirrored.Outlined.TrendingUp,
    )
    ListRow(
        titulo = "Roaming nesta sessão",
        subtitulo = if (uiState.roamingNaSessao) "Sim" else "Não",
        icone = Icons.Outlined.SwapHoriz,
    )
}

@Composable
private fun GraficoVariacaoRssi(historico: List<LeituraRssi>) {
    val corLinha = MaterialTheme.colorScheme.primary
    val corReferencia = MaterialTheme.colorScheme.outline

    Canvas(modifier = Modifier.fillMaxWidth().height(90.dp)) {
        if (historico.size < 2) return@Canvas

        val minRssi = historico.minOf { it.rssiDbm }
        val maxRssi = historico.maxOf { it.rssiDbm }
        val faixa = (maxRssi - minRssi).coerceAtLeast(1)
        val passoX = size.width / (historico.size - 1)

        fun yPara(rssi: Int): Float {
            val fracao = (rssi - minRssi).toFloat() / faixa
            return size.height - (fracao * size.height)
        }

        val media = historico.map { it.rssiDbm }.average().toInt()
        val yMedia = yPara(media)
        drawLine(
            color = corReferencia,
            start = Offset(0f, yMedia),
            end = Offset(size.width, yMedia),
            strokeWidth = 1.5.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f)),
        )

        val pontos =
            historico.mapIndexed { indice, leitura ->
                Offset(x = indice * passoX, y = yPara(leitura.rssiDbm))
            }
        for (indice in 0 until pontos.size - 1) {
            drawLine(
                color = corLinha,
                start = pontos[indice],
                end = pontos[indice + 1],
                strokeWidth = 2.5.dp.toPx(),
                cap = StrokeCap.Round,
            )
        }
    }
}

private fun progressoRssi(rssiDbm: Int?): Float {
    if (rssiDbm == null) return 0f
    val faixa = RSSI_PROGRESSO_MAX_DBM - RSSI_PROGRESSO_MIN_DBM
    return ((rssiDbm - RSSI_PROGRESSO_MIN_DBM) / faixa).toFloat().coerceIn(0f, 1f)
}

private fun rotuloQualidade(qualidade: QualidadeRssi?): String =
    when (qualidade) {
        QualidadeRssi.EXCELENTE -> "Excelente"
        QualidadeRssi.BOA -> "Boa"
        QualidadeRssi.REGULAR -> "Regular"
        QualidadeRssi.FRACA -> "Fraca"
        null -> "Aguardando leitura"
    }

private fun rotuloStatusChip(uiState: WalkTestUiState): String {
    val qualidadeTexto = rotuloQualidade(uiState.qualidade).lowercase()
    if (uiState.qualidade == null) return "Aguardando leitura"
    val estabilidade = if (uiState.estavel) "estável" else "variando"
    return "Sinal $qualidadeTexto e $estabilidade"
}

private fun toneQualidade(qualidade: QualidadeRssi?): StatusChipTone =
    when (qualidade) {
        QualidadeRssi.EXCELENTE, QualidadeRssi.BOA -> StatusChipTone.POSITIVO
        QualidadeRssi.REGULAR -> StatusChipTone.ATENCAO
        QualidadeRssi.FRACA -> StatusChipTone.CRITICO
        null -> StatusChipTone.NEUTRO
    }
