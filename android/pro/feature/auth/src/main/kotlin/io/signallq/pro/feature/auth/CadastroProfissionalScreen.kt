package io.signallq.pro.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.signallq.pro.core.designsystem.ProButton
import io.signallq.pro.core.designsystem.ProTextField
import io.signallq.pro.core.designsystem.TopBar

/**
 * Substitui a tela 1.5 (Criar conta) -- cadastro local do profissional, SEM backend
 * (decisão registrada na issue #1158). Só o nome é obrigatório; logo é opcional e usado
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
        topBar = { TopBar(titulo = "Seu perfil", leading = null) },
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
            ProTextField(
                valor = uiState.nome,
                onValorChange = viewModel::atualizarNome,
                rotulo = "Nome",
                erro = uiState.erroNomeVazio,
                textoAjuda = if (uiState.erroNomeVazio) "Informe seu nome" else null,
                modifier = Modifier.fillMaxWidth(),
            )
            ProButton(
                texto = "Continuar",
                onClick = viewModel::salvar,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}
