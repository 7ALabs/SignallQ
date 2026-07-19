package io.signallq.pro.feature.visita

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ListRow
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VisitaRapidaScreen(
    onVisitaCriada: (String) -> Unit,
    onNovoCliente: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: VisitaRapidaViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.visitaIdCriada) {
        uiState.visitaIdCriada?.let(onVisitaCriada)
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Atendimento rapido") }) },
    ) { paddingValues ->
        Column(
            modifier = modifier.fillMaxSize().padding(paddingValues),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "Selecione o cliente para pular direto pros ambientes",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(16.dp),
            )
            if (uiState.clientes.isEmpty()) {
                StateCard(
                    variant = StateCardVariant.VAZIO,
                    titulo = "Nenhum cliente cadastrado",
                    mensagem = "Cadastre um cliente antes de iniciar.",
                    acaoTexto = "Novo cliente",
                    onAcaoClick = onNovoCliente,
                    modifier = Modifier.padding(16.dp),
                )
            } else {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    items(uiState.clientes, key = { it.id }) { cliente ->
                        ListRow(
                            titulo = cliente.nome,
                            onClick = { viewModel.selecionarCliente(cliente.id) },
                            trailing = {
                                RadioButton(
                                    selected = uiState.clienteSelecionadoId == cliente.id,
                                    onClick = { viewModel.selecionarCliente(cliente.id) },
                                )
                            },
                        )
                    }
                }
            }
            Button(
                onClick = viewModel::iniciarVisitaRapida,
                enabled = uiState.clienteSelecionadoId != null,
                modifier = Modifier.fillMaxWidth().padding(16.dp),
            ) {
                Text("Iniciar")
            }
        }
    }
}
