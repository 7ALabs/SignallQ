package io.signallq.pro.feature.medicaodiagnostico

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.RecommendationBlock
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant

/**
 * Tela 2.16 -- [RecommendationBlock] por achado (problema/impacto/acao/prioridade), mapeado
 * direto de `DiagnosticResult` (`:core:diagnostico`). Estado "adequado" tem StateCard de
 * sucesso claro, nao fica vazio por omissao (handoff Fase 2, #1161).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticoResultadoScreen(
    onConcluir: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DiagnosticoAmbienteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Resultado do diagnostico") }) },
    ) { paddingValues ->
        LazyColumn(
            modifier = modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                Text(
                    text = "Veredito: ${uiState.veredito} (${uiState.scoreConexao}/100)",
                    style = MaterialTheme.typography.titleMedium,
                )
            }
            if (uiState.achados.isEmpty()) {
                item {
                    StateCard(
                        variant = StateCardVariant.SUCESSO,
                        titulo = "Rede adequada",
                        mensagem = "Nenhum problema relevante foi encontrado neste ambiente.",
                    )
                }
            } else {
                items(uiState.achados) { achado ->
                    RecommendationBlock(
                        problema = achado.titulo,
                        impacto = achado.mensagem,
                        acao = achado.recomendacao,
                        prioridade = achado.prioridade,
                    )
                }
            }
            item {
                Button(onClick = onConcluir, modifier = Modifier.fillMaxWidth()) {
                    Text("Concluir")
                }
            }
        }
    }
}
