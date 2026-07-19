package io.signallq.pro.feature.ambiente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.EnvironmentCard
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProButtonVariant
import io.signallq.pro.core.designsystem.StateCard
import io.signallq.pro.core.designsystem.StateCardVariant
import io.signallq.pro.core.designsystem.TopBar
import io.signallq.pro.core.designsystem.corSurfaceOverlay

private sealed interface DialogoAmbiente {
    data object Criar : DialogoAmbiente

    data class Opcoes(
        val id: String,
        val nome: String,
    ) : DialogoAmbiente

    data class Renomear(
        val id: String,
        val nome: String,
    ) : DialogoAmbiente

    data class Excluir(
        val id: String,
        val nome: String,
    ) : DialogoAmbiente
}

/**
 * Telas 2.6-2.9 -- lista de ambientes com criar/renomear/excluir. [EnvironmentCard] é uso
 * legítimo de card (1 ambiente = 1 unidade de dado real, handoff Fase 2, #1161).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AmbientesScreen(
    onAbrirAmbiente: (String) -> Unit,
    onConcluirVisita: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: AmbientesViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var dialogo by remember { mutableStateOf<DialogoAmbiente?>(null) }

    Scaffold(
        topBar = {
            TopBar(
                titulo = "Ambientes",
                leading = null,
                acao = "Concluir",
                onAcao = {
                    viewModel.concluirVisita()
                    onConcluirVisita()
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = { dialogo = DialogoAmbiente.Criar }) {
                Icon(Icons.Outlined.Add, contentDescription = "Novo ambiente")
            }
        },
    ) { paddingValues ->
        Box(modifier = modifier.fillMaxSize().padding(paddingValues)) {
            if (uiState.itens.isEmpty()) {
                StateCard(
                    variant = StateCardVariant.VAZIO,
                    titulo = "Nenhum ambiente ainda",
                    mensagem = "Adicione um ambiente para começar a medir.",
                    acaoTexto = "Novo ambiente",
                    onAcaoClick = { dialogo = DialogoAmbiente.Criar },
                    modifier = Modifier.padding(24.dp).align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    contentPadding =
                        PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(uiState.itens, key = { it.id }) { ambiente ->
                        EnvironmentCard(
                            nome = ambiente.nome,
                            resumoMedicao = "Toque para medir e diagnosticar",
                            onClick = { onAbrirAmbiente(ambiente.id) },
                            onMaisOpcoes = { dialogo = DialogoAmbiente.Opcoes(ambiente.id, ambiente.nome) },
                        )
                    }
                }
            }

            if (uiState.erroExclusaoBloqueada != null) {
                StateCard(
                    variant = StateCardVariant.ERRO,
                    titulo = "Não foi possível excluir",
                    mensagem = uiState.erroExclusaoBloqueada.orEmpty(),
                    acaoTexto = "Entendi",
                    onAcaoClick = viewModel::limparErroExclusao,
                    modifier = Modifier.padding(24.dp).align(Alignment.Center),
                )
            }
        }
    }

    DialogoAmbienteHost(
        dialogo = dialogo,
        onDialogoChange = { dialogo = it },
        viewModel = viewModel,
    )
}

@Composable
private fun DialogoAmbienteHost(
    dialogo: DialogoAmbiente?,
    onDialogoChange: (DialogoAmbiente?) -> Unit,
    viewModel: AmbientesViewModel,
) {
    when (dialogo) {
        DialogoAmbiente.Criar ->
            CriarAmbienteSheet(
                onConfirmar = { nome ->
                    viewModel.criarAmbiente(nome)
                    onDialogoChange(null)
                },
                onDismiss = { onDialogoChange(null) },
            )
        is DialogoAmbiente.Opcoes ->
            AlertDialog(
                onDismissRequest = { onDialogoChange(null) },
                containerColor = corSurfaceOverlay(),
                title = { Text(dialogo.nome) },
                text = {},
                confirmButton = {
                    ProButton(
                        texto = "Renomear",
                        onClick = { onDialogoChange(DialogoAmbiente.Renomear(dialogo.id, dialogo.nome)) },
                        variant = ProButtonVariant.TEXTO,
                    )
                },
                dismissButton = {
                    ProButton(
                        texto = "Excluir",
                        onClick = { onDialogoChange(DialogoAmbiente.Excluir(dialogo.id, dialogo.nome)) },
                        variant = ProButtonVariant.DESTRUTIVO,
                    )
                },
            )
        is DialogoAmbiente.Renomear ->
            RenomearAmbienteDialog(
                nomeAtual = dialogo.nome,
                onConfirmar = { novoNome ->
                    viewModel.renomearAmbiente(dialogo.id, novoNome)
                    onDialogoChange(null)
                },
                onDismiss = { onDialogoChange(null) },
            )
        is DialogoAmbiente.Excluir ->
            ExcluirAmbienteDialog(
                nomeAmbiente = dialogo.nome,
                onConfirmar = {
                    viewModel.excluirAmbiente(dialogo.id)
                    onDialogoChange(null)
                },
                onDismiss = { onDialogoChange(null) },
            )
        null -> Unit
    }
}
