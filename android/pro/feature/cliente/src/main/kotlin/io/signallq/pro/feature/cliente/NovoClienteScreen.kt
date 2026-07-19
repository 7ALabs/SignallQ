package io.signallq.pro.feature.cliente

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
        topBar = { TopAppBar(title = { Text("Novo cliente") }) },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            OutlinedTextField(
                value = uiState.nome,
                onValueChange = viewModel::atualizarNome,
                label = { Text("Nome do cliente") },
                isError = uiState.erroNomeVazio,
                supportingText = { if (uiState.erroNomeVazio) Text("Informe o nome do cliente") },
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.telefone,
                onValueChange = viewModel::atualizarTelefone,
                label = { Text("Telefone (opcional)") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = uiState.endereco,
                onValueChange = viewModel::atualizarEndereco,
                label = { Text("Endereço do local (opcional)") },
                supportingText = { Text("Pode ser preenchido depois, na visita") },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(onClick = viewModel::salvar, modifier = Modifier.fillMaxWidth()) {
                Text("Salvar")
            }
        }
    }
}
