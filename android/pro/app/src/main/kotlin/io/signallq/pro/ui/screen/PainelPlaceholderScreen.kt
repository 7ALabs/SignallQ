package io.signallq.pro.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.signallq.pro.R

// Tela placeholder do esqueleto (Fase 0, issue #1157). Substituida pela tela real do Painel
// (grupo 2.1 do mapa de modulos, :pro:feature:visita) na Fase 2.
@Composable
fun PainelPlaceholderScreen() {
    Scaffold { paddingValues ->
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.painelPlaceholderTitulo),
                style = MaterialTheme.typography.headlineMedium,
            )
            Text(
                text = stringResource(R.string.painelPlaceholderSubtitulo),
                style = MaterialTheme.typography.bodyMedium,
            )
        }
    }
}
