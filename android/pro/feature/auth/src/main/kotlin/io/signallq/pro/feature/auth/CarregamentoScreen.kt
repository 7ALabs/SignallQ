package io.signallq.pro.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Tela 1.1 do prototipo -- carregamento inicial. Decide o destino (perfil ja existe ->
 * segue pra permissoes/painel; perfil nao existe -> apresentacao) via [CarregamentoViewModel],
 * sem logica de negocio no Composable.
 */
@Composable
fun CarregamentoScreen(
    onDestinoDecidido: (DestinoPosCarregamento) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CarregamentoViewModel = hiltViewModel(),
) {
    val destino by viewModel.destino.collectAsStateWithLifecycle()

    LaunchedEffect(destino) {
        destino?.let(onDestinoDecidido)
    }

    Scaffold { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(text = "SignallQ Pro", style = MaterialTheme.typography.headlineMedium)
            Spacer(modifier = Modifier.size(24.dp))
            CircularProgressIndicator(modifier = Modifier.size(32.dp))
        }
    }
}
