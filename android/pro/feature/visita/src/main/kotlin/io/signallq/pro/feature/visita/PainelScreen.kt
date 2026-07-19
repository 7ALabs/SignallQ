package io.signallq.pro.feature.visita

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Build
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Receipt
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant

private data class AcaoRapida(
    val titulo: String,
    val icone: ImageVector,
    val onClick: () -> Unit,
)

/**
 * Tela 2.1 -- painel inicial. Lidera com "Proximos atendimentos" (lista, nao card) + 4
 * acoes rapidas em grid -- NAO replica os 3 cards de metrica de vaidade do prototipo
 * (handoff Fase 2, #1161).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PainelScreen(
    onRetomarAtendimento: (String) -> Unit,
    onNovoAtendimento: () -> Unit,
    onNovoCliente: () -> Unit,
    onFerramentas: () -> Unit,
    onCobrar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PainelViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("SignallQ Pro") }) },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {
            AcoesRapidasGrid(
                acoes =
                    listOf(
                        AcaoRapida("Novo atendimento", Icons.Outlined.Add, onNovoAtendimento),
                        AcaoRapida("Novo cliente", Icons.Outlined.PersonAdd, onNovoCliente),
                        AcaoRapida("Ferramentas", Icons.Outlined.Build, onFerramentas),
                        AcaoRapida("Cobrar", Icons.Outlined.Receipt, onCobrar),
                    ),
            )

            Text(text = "Proximos atendimentos", style = MaterialTheme.typography.titleMedium)

            when {
                uiState.carregando ->
                    StateCard(
                        variant = StateCardVariant.CARREGANDO,
                        titulo = "Carregando",
                        mensagem = "Buscando seus atendimentos...",
                    )
                uiState.vazio ->
                    StateCard(
                        variant = StateCardVariant.VAZIO,
                        titulo = "Nenhum atendimento ainda",
                        mensagem = "Comece um novo atendimento para um cliente.",
                        acaoTexto = "Nova visita",
                        onAcaoClick = onNovoAtendimento,
                    )
                else -> {
                    Column {
                        uiState.visitaEmAndamento?.let { resumo ->
                            AtendimentoResumoRow(
                                resumo = resumo,
                                destaque = true,
                                onClick = { onRetomarAtendimento(resumo.visitaId) },
                            )
                            HorizontalDivider()
                        }
                        uiState.proximosAtendimentos.forEach { resumo ->
                            AtendimentoResumoRow(
                                resumo = resumo,
                                destaque = false,
                                onClick = { onRetomarAtendimento(resumo.visitaId) },
                            )
                            HorizontalDivider()
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AcoesRapidasGrid(acoes: List<AcaoRapida>) {
    LazyVerticalGrid(
        columns = GridCells.Fixed(4),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        items(acoes) { acao ->
            Surface(
                shape =
                    androidx.compose.foundation.shape
                        .RoundedCornerShape(12.dp),
                border =
                    androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                modifier = Modifier.aspectRatio(1f).clickable(onClick = acao.onClick),
            ) {
                Column(
                    modifier = Modifier.fillMaxSize().padding(8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Icon(
                        imageVector = acao.icone,
                        contentDescription = null,
                        modifier = Modifier.padding(bottom = 4.dp),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = acao.titulo,
                        style = MaterialTheme.typography.labelSmall,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                    )
                }
            }
        }
    }
}

@Composable
private fun AtendimentoResumoRow(
    resumo: AtendimentoResumoUiState,
    destaque: Boolean,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column {
            Text(text = resumo.clienteNome, style = MaterialTheme.typography.bodyLarge)
            Text(
                text = resumo.tipo,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (destaque) {
            Button(onClick = onClick) { Text("Retomar") }
        }
    }
}
