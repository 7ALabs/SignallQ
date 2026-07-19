package io.signallq.pro.feature.laudo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MeetingRoom
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material.icons.outlined.TipsAndUpdates
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
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
import io.signallq.pro.core.designsystem.ListRow
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProButtonVariant
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant
import io.signallq.pro.core.designsystem.StatusChip
import io.signallq.pro.core.designsystem.StatusChipTone
import io.signallq.pro.core.designsystem.TopBar

/**
 * Tela 3.2 -- lista única com [ListRow] + divisores finos por seção do laudo (resumo,
 * ambientes, evidências, recomendações), não 4 cards separados (correção de design,
 * issue #1164 -- o prototipo original usava 1 card por secao, densidade desnecessaria).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LaudoScreen(
    modifier: Modifier = Modifier,
    viewModel: LaudoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopBar(titulo = "Laudo técnico", leading = null) },
        bottomBar = {
            if (uiState.estado == LaudoEstado.PRONTO) {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    ProButton(
                        texto = if (uiState.gerandoPdf) "Gerando PDF..." else "Gerar PDF",
                        onClick = viewModel::gerarPdf,
                        habilitado = !uiState.gerandoPdf,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    ProButton(
                        texto = "Compartilhar",
                        onClick = viewModel::compartilhar,
                        habilitado = uiState.pdfUri != null,
                        variant = ProButtonVariant.SECUNDARIO,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
            }
        },
    ) { paddingValues ->
        when (uiState.estado) {
            LaudoEstado.CARREGANDO ->
                Box(
                    modifier = modifier.fillMaxSize().padding(paddingValues),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            LaudoEstado.ERRO ->
                Box(modifier = modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
                    StateCard(
                        variant = StateCardVariant.ERRO,
                        titulo = "Não foi possível montar o laudo",
                        mensagem = uiState.mensagemErro ?: "Tente novamente.",
                        acaoTexto = "Tentar novamente",
                        onAcaoClick = viewModel::carregarLaudo,
                    )
                }
            LaudoEstado.PRONTO -> {
                val dados = uiState.dados
                if (dados != null) {
                    ConteudoLaudo(
                        dados = dados,
                        pdfPronto = uiState.pdfUri != null,
                        modifier =
                            modifier
                                .fillMaxSize()
                                .padding(paddingValues)
                                .verticalScroll(rememberScrollState())
                                .padding(16.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun ConteudoLaudo(
    dados: LaudoDados,
    pdfPronto: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(text = "Laudo -- ${dados.clienteNome}", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Gerado por ${dados.profissionalNome}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                StatusChip(
                    texto = if (pdfPronto) "Pronto" else "Não gerado",
                    tone = if (pdfPronto) StatusChipTone.POSITIVO else StatusChipTone.NEUTRO,
                )
            }
        }

        Column {
            ListRow(
                titulo = "Resumo executivo",
                subtitulo = "Veredito por ambiente avaliado",
                icone = Icons.Outlined.CheckCircle,
                trailing = { IndicadorPresenca(dados.ambientes.isNotEmpty()) },
            )
            HorizontalDivider()
            ListRow(
                titulo = "Ambientes avaliados",
                subtitulo = "${dados.ambientes.size} ambiente(s)",
                icone = Icons.Outlined.MeetingRoom,
                trailing = { IndicadorPresenca(dados.ambientes.isNotEmpty()) },
            )
            HorizontalDivider()
            ListRow(
                titulo = "Evidências",
                subtitulo = "${dados.totalEvidencias} registrada(s)",
                icone = Icons.Outlined.PhotoCamera,
                trailing = { IndicadorPresenca(dados.totalEvidencias > 0) },
            )
            HorizontalDivider()
            ListRow(
                titulo = "Recomendações",
                subtitulo = "${dados.totalAchados} achado(s)",
                icone = Icons.Outlined.TipsAndUpdates,
                trailing = { IndicadorPresenca(dados.totalAchados > 0) },
            )
        }
    }
}

@Composable
private fun IndicadorPresenca(presente: Boolean) {
    if (presente) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
        )
    }
}
