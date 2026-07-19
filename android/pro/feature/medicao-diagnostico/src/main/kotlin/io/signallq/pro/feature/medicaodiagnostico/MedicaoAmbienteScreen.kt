package io.signallq.pro.feature.medicaodiagnostico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudUpload
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ListRow
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProButtonVariant
import io.signallq.pro.core.designsystem.QualityGauge
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant
import io.signallq.pro.core.designsystem.TopBar

private const val DOWNLOAD_REFERENCIA_MBPS = 200.0

/**
 * Tela 2.10 -- 1 gauge em destaque (download) + [ListRow] expansível pras secundárias, NÃO
 * grid de 6-7 cards de métrica (handoff Fase 2, #1161). Reaproveita o motor de speedtest
 * real do consumidor via [MedicaoAmbienteViewModel].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MedicaoAmbienteScreen(
    onMedicaoConcluida: (ambienteId: String) -> Unit,
    onIniciarWalkTest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: MedicaoAmbienteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        if (uiState.estado == MedicaoAmbienteEstado.OCIOSO) viewModel.iniciarMedicao()
    }

    Scaffold(
        topBar = { TopBar(titulo = "Medição do ambiente", leading = null) },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            when (uiState.estado) {
                MedicaoAmbienteEstado.OCIOSO, MedicaoAmbienteEstado.MEDINDO ->
                    StateCard(
                        variant = StateCardVariant.CARREGANDO,
                        titulo = "Medindo (${uiState.progressoPercentual}%)",
                        mensagem = "Testando velocidade, latência e perda de pacotes da rede local.",
                    )
                MedicaoAmbienteEstado.ERRO ->
                    StateCard(
                        variant = StateCardVariant.ERRO,
                        titulo = "Medição inválida",
                        mensagem = uiState.mensagemErro ?: "Tente novamente.",
                        acaoTexto = "Medir novamente",
                        onAcaoClick = viewModel::iniciarMedicao,
                    )
                MedicaoAmbienteEstado.SUCESSO -> {
                    QualityGauge(
                        valorFormatado = "%.0f".format(uiState.downloadMbps),
                        unidade = "Mbps download",
                        veredito = veredictoDownload(uiState.downloadMbps),
                        progresso = (uiState.downloadMbps / DOWNLOAD_REFERENCIA_MBPS).toFloat().coerceIn(0f, 1f),
                    )
                    ListRow(
                        titulo = "Upload",
                        subtitulo = "%.1f Mbps".format(uiState.uploadMbps),
                        icone = Icons.Outlined.CloudUpload,
                    )
                    ListRow(
                        titulo = "Latência",
                        subtitulo = "%.0f ms".format(uiState.latenciaMs),
                        icone = Icons.Outlined.Timer,
                    )
                    ListRow(
                        titulo = "Jitter",
                        subtitulo = "%.1f ms".format(uiState.jitterMs),
                        icone = Icons.Outlined.SignalCellularAlt,
                    )
                    ListRow(
                        titulo = "Perda de pacotes",
                        subtitulo = "%.1f%%".format(uiState.perdaPercentual),
                        icone = Icons.Outlined.Warning,
                    )
                    ProButton(
                        texto = "Continuar para diagnóstico",
                        onClick = { onMedicaoConcluida(viewModel.ambienteId) },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProButton(
                        texto = "Fazer walk test",
                        onClick = onIniciarWalkTest,
                        variant = ProButtonVariant.SECUNDARIO,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        }
    }
}

private fun veredictoDownload(downloadMbps: Double): String =
    when {
        downloadMbps >= 100 -> "Excelente"
        downloadMbps >= 50 -> "Bom"
        downloadMbps >= 20 -> "Regular"
        else -> "Fraco"
    }
