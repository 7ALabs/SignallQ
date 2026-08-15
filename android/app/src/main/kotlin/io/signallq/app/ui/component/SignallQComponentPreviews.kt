package io.signallq.app.ui.component

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.SignallQTheme

@Preview(name = "Controles — claro", widthDp = 360, showBackground = true)
@Preview(name = "Controles — escuro", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ControlsPreview() =
    PreviewFrame {
        SignallQButton("Analisar minha conexão", onClick = {})
        SignallQButton("Verificando", onClick = {}, loading = true)
        SignallQButton("Indisponível", onClick = {}, enabled = false)
        SignallQTextField("Casa", {}, "Nome da rede")
        SignallQTextField("", {}, "Servidor", isError = true, supportingText = "Confira o endereço informado")
        SignallQChoiceChip("Wi-Fi selecionado", selected = true, onClick = {}, leadingIcon = Icons.Outlined.Wifi)
        SignallQChoiceChip("Rede móvel desabilitada", selected = false, onClick = {}, enabled = false)
        SignallQBadge("Resultado parcial", tone = SignallQBadgeTone.Warning)
    }

@Preview(name = "Estrutura — claro", widthDp = 360, showBackground = true)
@Preview(name = "Estrutura — escuro", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun ContainersPreview() =
    PreviewFrame {
        SignallQTopAppBar(title = "Detalhes da conexão")
        SignallQListRow(
            title = "Uma opção com título longo que pode ocupar mais de uma linha",
            subtitle = "O texto de apoio também cresce sem cortar a informação essencial.",
            icon = Icons.Outlined.Info,
            onClick = {},
        )
        SignallQSurfaceCard { Text("Card justificado e opt-in") }
        SignallQExpandableDetails("Detalhes técnicos", initiallyExpanded = true) { Text("Tempo de resposta: 28 ms") }
        SignallQNavigationBar(
            items =
                listOf(
                    SignallQNavigationItem("Início", Icons.Outlined.Home),
                    SignallQNavigationItem("Histórico", Icons.Outlined.History),
                ),
            selectedIndex = 0,
            onSelected = {},
        )
    }

@Preview(name = "Feedback — claro", widthDp = 360, showBackground = true)
@Preview(name = "Feedback — escuro", widthDp = 360, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun FeedbackPreview() =
    PreviewFrame {
        SignallQBanner("Você está offline", message = "Recursos locais continuam disponíveis.", actionLabel = "Entendi", onAction = {})
        SignallQResultBlock(
            conclusion = "O Wi-Fi está fraco neste cômodo",
            explanation = "A distância do roteador está afetando sua conexão.",
            tone = SignallQFeedbackTone.Warning,
            nextStep = "Aproxime-se do roteador e teste novamente.",
        )
        SignallQTranslatedMetric("Tempo de resposta", "28 ms", "Adequado para chamadas e jogos")
        SignallQProgress("Verificando estabilidade", progress = 0.62f)
        SignallQProgress("Preparando análise")
        SignallQSkeleton(lines = 3)
    }

@Preview(name = "Estados — claro", widthDp = 360, heightDp = 640, showBackground = true)
@Preview(name = "Estados — escuro", widthDp = 360, heightDp = 640, uiMode = Configuration.UI_MODE_NIGHT_YES)
@Composable
private fun StatesPreview() =
    StatePreview(
        SignallQScreenState.PermissionRequired(
            "Permita redes próximas",
            "Sem essa permissão, o diagnóstico continua com menos detalhes.",
        ),
    )

@Preview(name = "Estado vazio", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun EmptyStatePreview() = StatePreview(SignallQScreenState.Empty("Nada analisado ainda", "Inicie uma análise para entender sua conexão."))

@Preview(name = "Estado offline", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun OfflineStatePreview() = StatePreview(SignallQScreenState.Offline("Sem internet", "Ainda é possível verificar sua rede local."))

@Preview(name = "Erro recuperável", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun ErrorStatePreview() = StatePreview(SignallQScreenState.RecoverableError("Análise interrompida", "Seu contexto foi preservado."))

@Preview(name = "Loading", widthDp = 360, heightDp = 480, showBackground = true)
@Composable
private fun LoadingStatePreview() = StatePreview(SignallQScreenState.Loading)

@Preview(name = "Texto 200%", widthDp = 360, heightDp = 640, fontScale = 2f, showBackground = true)
@Composable
private fun LargeTextPreview() =
    StatePreview(
        SignallQScreenState.RecoverableError(
            "A conexão foi perdida durante uma análise importante",
            "Reconecte-se ao Wi-Fi quando puder. As informações já coletadas continuam preservadas para você tentar novamente.",
        ),
    )

@Preview(name = "Sheet", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun SheetPreview() {
    SignallQTheme { SignallQSheet(onDismissRequest = {}, title = "Escolha uma opção") { SignallQListRow("Verificar Wi-Fi") } }
}

@Preview(name = "Dialog", widthDp = 360, heightDp = 640, showBackground = true)
@Composable
private fun DialogPreview() {
    SignallQTheme { SignallQDialog("Cancelar análise?", "O contexto será preservado.", "Cancelar", {}, {}, "Continuar") }
}

@Composable
private fun StatePreview(state: SignallQScreenState<Unit>) {
    SignallQTheme {
        Surface {
            SignallQStatefulScreen(state, actionLabel = "Tentar novamente", onAction = {}, content = {})
        }
    }
}

@Composable
private fun PreviewFrame(content: @Composable () -> Unit) {
    SignallQTheme {
        Surface {
            Column(
                modifier = Modifier.fillMaxWidth().padding(LkSpacing.lg),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) { content() }
        }
    }
}
