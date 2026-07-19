package io.signallq.pro.feature.visita

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.database.visita.EtapaVisita
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.StatusChip
import io.signallq.pro.core.designsystem.StatusChipTone
import io.signallq.pro.core.designsystem.TopBar

private val ETAPAS_EM_ORDEM = listOf(EtapaVisita.CHECKLIST, EtapaVisita.AMBIENTES, EtapaVisita.CONCLUSAO)

private fun rotuloEtapa(etapa: EtapaVisita): String =
    when (etapa) {
        EtapaVisita.CHECKLIST -> "Checklist"
        EtapaVisita.AMBIENTES -> "Ambientes"
        EtapaVisita.CONCLUSAO -> "Conclusão"
    }

/**
 * Tela 2.5 -- hub do atendimento. TopBar (cliente/tipo), StatusChip da etapa e indicador
 * de progresso por etapa com rótulo (não barra genérica) -- handoff Fase 2, #1161.
 * "Continuar" leva pra etapa salva -- é a retomada de visita interrompida na prática.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AtendimentoScreen(
    onContinuarChecklist: () -> Unit,
    onContinuarAmbientes: () -> Unit,
    onContinuarConclusao: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AtendimentoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopBar(
                titulo = uiState.clienteNome,
                subtitulo = uiState.tipo,
                leading = null,
            )
        },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (uiState.carregando) {
                CircularProgressIndicator()
                return@Column
            }

            StatusChip(texto = "Etapa: ${rotuloEtapa(uiState.etapaAtual)}", tone = StatusChipTone.POSITIVO)

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                ETAPAS_EM_ORDEM.forEach { etapa ->
                    val ativa = etapa == uiState.etapaAtual
                    Text(
                        text = rotuloEtapa(etapa),
                        style = if (ativa) MaterialTheme.typography.labelLarge else MaterialTheme.typography.labelSmall,
                        color =
                            if (ativa) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.onSurfaceVariant
                            },
                    )
                }
            }

            ProButton(
                texto = "Continuar",
                onClick = {
                    when (uiState.etapaAtual) {
                        EtapaVisita.CHECKLIST -> onContinuarChecklist()
                        EtapaVisita.AMBIENTES -> onContinuarAmbientes()
                        EtapaVisita.CONCLUSAO -> onContinuarConclusao()
                    }
                },
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
