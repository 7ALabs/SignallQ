package io.signallq.pro.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
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

/**
 * Substitui a tela 1.5 (Criar conta) -- cadastro local do profissional, SEM backend
 * (decisao registrada na issue #1158). So o nome e obrigatorio; logo e opcional e usado
 * depois no laudo (Fase 3).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CadastroProfissionalScreen(
    onConcluido: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CadastroProfissionalViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.salvo) {
        if (uiState.salvo) onConcluido()
    }

    Scaffold(
        topBar = { TopAppBar(title = { Text("Seu perfil") }) },
    ) { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            Text(
                text = "Como devemos te chamar nos laudos e atendimentos?",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            OutlinedTextField(
                value = uiState.nome,
                onValueChange = viewModel::atualizarNome,
                label = { Text("Nome") },
                isError = uiState.erroNomeVazio,
                supportingText = {
                    if (uiState.erroNomeVazio) Text("Informe seu nome")
                },
                modifier = Modifier.fillMaxWidth(),
            )
            Button(
                onClick = viewModel::salvar,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Continuar")
            }
        }
    }
}
