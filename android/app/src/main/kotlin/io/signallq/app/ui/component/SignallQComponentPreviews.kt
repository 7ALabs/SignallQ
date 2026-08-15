package io.signallq.app.ui.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.SignallQTheme

@Preview(name = "Componentes 2.0 — claro", showBackground = true, widthDp = 360)
@Composable
private fun SignallQComponentsLightPreview() {
    SignallQComponentsPreview(darkTheme = false)
}

@Preview(name = "Componentes 2.0 — escuro", showBackground = true, widthDp = 360)
@Composable
private fun SignallQComponentsDarkPreview() {
    SignallQComponentsPreview(darkTheme = true)
}

@Composable
private fun SignallQComponentsPreview(darkTheme: Boolean) {
    SignallQTheme(darkTheme = darkTheme) {
        Surface {
            Column(
                modifier = Modifier.fillMaxWidth().padding(LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                SignallQButton("Analisar minha conexão", onClick = {})
                SignallQButton("Ver detalhes", onClick = {}, style = SignallQButtonStyle.Secondary)
                SignallQButton("Verificando…", onClick = {}, loading = true)
                SignallQTextField("Casa", {}, "Nome da rede")
                SignallQTextField("", {}, "Servidor", isError = true, supportingText = "Confira o endereço informado")
                SignallQChoiceChip("Wi-Fi", selected = true, onClick = {}, leadingIcon = Icons.Outlined.Wifi)
                SignallQChoiceChip("Rede móvel", selected = false, onClick = {}, enabled = false)
                SignallQBanner("Sinal instável", message = "A conexão pode oscilar neste cômodo.", tone = SignallQFeedbackTone.Warning)
                SignallQResultBlock(
                    conclusion = "O Wi-Fi está fraco neste cômodo",
                    explanation = "A distância do roteador está afetando sua conexão.",
                    tone = SignallQFeedbackTone.Warning,
                    nextStep = "Aproxime-se do roteador e teste novamente.",
                )
                SignallQTranslatedMetric("Tempo de resposta", "28 ms", "Adequado para chamadas e jogos")
                SignallQProgress("Verificando estabilidade", progress = 0.62f)
            }
        }
    }
}

@Preview(name = "Estados 2.0 — claro", showBackground = true, widthDp = 360, heightDp = 480)
@Composable
private fun SignallQStatesLightPreview() {
    SignallQStatePreview(darkTheme = false)
}

@Preview(name = "Estados 2.0 — escuro", showBackground = true, widthDp = 360, heightDp = 480)
@Composable
private fun SignallQStatesDarkPreview() {
    SignallQStatePreview(darkTheme = true)
}

@Composable
private fun SignallQStatePreview(darkTheme: Boolean) {
    SignallQTheme(darkTheme = darkTheme) {
        Surface {
            SignallQStatefulScreen<Unit>(
                state =
                    SignallQScreenState.RecoverableError(
                        title = "A conexão foi perdida",
                        message = "Reconecte-se ao Wi-Fi. Seu contexto foi preservado.",
                    ),
                actionLabel = "Tentar novamente",
                onAction = {},
                content = {},
            )
        }
    }
}
