package io.signallq.pro.feature.auth

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Engineering
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.signallq.pro.core.designsystem.ProButton

/**
 * Tela 1.2 do protótipo -- boas-vindas simples (reduzida do carrossel de vendas original,
 * que pertence ao 1.9/1.10 cortados do MVP0 -- handoff Fase 2, #1161).
 */
@Composable
fun ApresentacaoScreen(
    onContinuar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold { paddingValues ->
        Column(
            modifier =
                modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        ) {
            Icon(
                imageVector = Icons.Outlined.Engineering,
                contentDescription = null,
                modifier = Modifier.padding(bottom = 8.dp),
            )
            Text(text = "Bem-vindo ao SignallQ Pro", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = "Diagnóstico técnico de rede para instaladores e técnicos de campo.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            ProButton(texto = "Começar", onClick = onContinuar, modifier = Modifier.padding(top = 16.dp))
        }
    }
}
