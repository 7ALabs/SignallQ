package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens

@Composable
internal fun DiagnosticoGuiadoPerguntaBinariaSection(
    modifier: Modifier = Modifier,
    onSelecionarVideo: () -> Unit,
    onSelecionarChamada: () -> Unit,
    c: LkTokens,
) {
    Column(
        modifier = modifier.fillMaxSize().background(c.bgPrimary).padding(LkSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Text("O problema acontece mais em vídeos ou chamadas?", color = c.textPrimary)
        Text("Escolha o que mais se aproxima do que está acontecendo agora.", color = c.textSecondary)
        Button(onClick = onSelecionarVideo, modifier = Modifier.fillMaxWidth()) {
            Text("Vídeos travam")
        }
        Button(onClick = onSelecionarChamada, modifier = Modifier.fillMaxWidth()) {
            Text("Chamadas congelam")
        }
    }
}
