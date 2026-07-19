package io.signallq.pro.feature.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProTextField
import io.signallq.pro.core.designsystem.TopBar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NovoClienteScreen(
    onClienteCriado: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: NovoClienteViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.clienteIdCriado) {
        uiState.clienteIdCriado?.let(onClienteCriado)
    }

    Scaffold(
        topBar = { TopBar(titulo = "Novo cliente", leading = null) },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            ProTextField(
                valor = uiState.nome,
                onValorChange = viewModel::atualizarNome,
                rotulo = "Nome do cliente",
                erro = uiState.erroNomeVazio,
                textoAjuda = if (uiState.erroNomeVazio) "Informe o nome do cliente" else null,
                modifier = Modifier.fillMaxWidth(),
            )
            ProTextField(
                valor = uiState.telefone,
                onValorChange = viewModel::atualizarTelefone,
                rotulo = "Telefone (opcional)",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            ProTextField(
                valor = uiState.endereco,
                onValorChange = viewModel::atualizarEndereco,
                rotulo = "Endereço do local (opcional)",
                textoAjuda = "Pode ser preenchido depois, na visita",
                modifier = Modifier.fillMaxWidth(),
            )
            ProButton(texto = "Salvar", onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth())
        }
    }
}
