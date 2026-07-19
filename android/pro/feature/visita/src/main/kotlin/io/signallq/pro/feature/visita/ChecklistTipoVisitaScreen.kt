package io.signallq.pro.feature.visita

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Checklist
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ListRow
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant
import io.signallq.pro.core.designsystem.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChecklistTipoVisitaScreen(
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ChecklistTipoVisitaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopBar(titulo = "Checklist", leading = null) },
    ) { paddingValues ->
        Column(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            LinearProgressIndicator(
                progress = { uiState.progresso },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
            )
            if (uiState.itens.isEmpty()) {
                StateCard(
                    variant = StateCardVariant.VAZIO,
                    titulo = "Sem checklist",
                    mensagem = "Este tipo de visita não possui roteiro padrão.",
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.itens, key = { it.id }) { item ->
                        ListRow(
                            titulo = item.descricao,
                            icone = Icons.Outlined.Checklist,
                            onClick = { viewModel.alternarItem(item.id, !item.concluido) },
                            trailing = {
                                Checkbox(
                                    checked = item.concluido,
                                    onCheckedChange = { marcado -> viewModel.alternarItem(item.id, marcado) },
                                )
                            },
                        )
                    }
                }
            }
            ProButton(
                texto = "Continuar para ambientes",
                onClick = {
                    viewModel.avancarParaAmbientes()
                    onContinuar()
                },
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            )
        }
    }
}
