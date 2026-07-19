package io.signallq.pro.feature.medicaodiagnostico

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant
import io.signallq.pro.core.designsystem.TopBar

/**
 * Tela 2.15 -- explica o que será analisado antes de começar (handoff Fase 2, #1161),
 * depois delega ao [DiagnosticoAmbienteViewModel] (motor real, `:core:diagnostico`).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticoMedindoScreen(
    onDiagnosticoConcluido: (ambienteId: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosticoAmbienteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.estado) {
        if (uiState.estado == DiagnosticoAmbienteEstado.SUCESSO) {
            onDiagnosticoConcluido(viewModel.ambienteId)
        }
    }

    Scaffold(
        topBar = { TopBar(titulo = "Diagnóstico", leading = null) },
    ) { paddingValues ->
        when (uiState.estado) {
            DiagnosticoAmbienteEstado.MEDINDO ->
                StateCard(
                    variant = StateCardVariant.CARREGANDO,
                    titulo = "Analisando a medição",
                    mensagem = "Avaliando velocidade, latência e estabilidade da rede deste ambiente.",
                    modifier = modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                )
            DiagnosticoAmbienteEstado.ERRO ->
                StateCard(
                    variant = StateCardVariant.ERRO,
                    titulo = "Não foi possível diagnosticar",
                    mensagem = uiState.mensagemErro ?: "Tente medir novamente.",
                    acaoTexto = "Tentar de novo",
                    onAcaoClick = viewModel::executarDiagnostico,
                    modifier = modifier.fillMaxSize().padding(paddingValues).padding(24.dp),
                )
            DiagnosticoAmbienteEstado.SUCESSO -> Unit
        }
    }
}
