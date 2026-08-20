package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSurfaceCard

private enum class OnboardingOverlay { TERMOS, PRIVACIDADE }

/**
 * Onboarding de 1 tela (Task 2.0.23 / issue #1671, épico #1647): boas-vindas + aceite
 * obrigatório de Termos/Privacidade. Comunica diagnóstico (não speed test), sem listar o
 * catálogo de ferramentas.
 *
 * Permissões opcionais (localização, telefonia, dispositivos na rede, notificações) NÃO são
 * mais pedidas aqui em lote — decisão de produto (Luiz, 2026-08-19, issue #1671): cada uma é
 * solicitada de forma contextual, só quando o usuário entra na funcionalidade que precisa
 * dela (ex.: aba Sinal pede telefonia, aba Wi-Fi pede localização, ativar monitoramento pede
 * notificações). Ver MainActivity.kt (solicitarPermissao*Contextual) e AppShell.kt.
 */
@Composable
fun OnboardingScreen(
    onConcluir: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val alturaTelaDP = LocalConfiguration.current.screenHeightDp

    var termosAceitos by remember { mutableStateOf(false) }
    var overlay by remember { mutableStateOf<OnboardingOverlay?>(null) }

    // Back: overlay aberto -> fecha overlay; tela unica -> consome sem navegar.
    BackHandler {
        if (overlay != null) overlay = null
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .safeDrawingPadding(),
    ) {
        OnboardingTelaBemVindo(
            c = c,
            termosAceitos = termosAceitos,
            onTermosAceitosChange = { termosAceitos = it },
            onAbrirTermos = { overlay = OnboardingOverlay.TERMOS },
            onAbrirPrivacidade = { overlay = OnboardingOverlay.PRIVACIDADE },
            alturaTelaDP = alturaTelaDP,
            onContinuar = onConcluir,
        )

        overlay?.let { tela ->
            Box(Modifier.fillMaxSize().background(c.bgPrimary)) {
                when (tela) {
                    OnboardingOverlay.TERMOS -> TermosDeUsoScreen(onVoltar = { overlay = null })
                    OnboardingOverlay.PRIVACIDADE -> PrivacidadeScreen(onVoltar = { overlay = null })
                }
            }
        }
    }
}

// ─── Tela unica — Bem-vindo ───────────────────────────────────────────────────

@Composable
private fun OnboardingTelaBemVindo(
    c: LkTokens,
    termosAceitos: Boolean,
    onTermosAceitosChange: (Boolean) -> Unit,
    onAbrirTermos: () -> Unit,
    onAbrirPrivacidade: () -> Unit,
    alturaTelaDP: Int,
    onContinuar: () -> Unit,
) {
    val logoSizeDp: Dp = if (alturaTelaDP < 540) 88.dp else 120.dp

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier.fillMaxWidth().weight(0.32f),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(logoSizeDp)
                        .clip(CircleShape)
                        .background(c.surfaceContainerHigh),
                contentAlignment = Alignment.Center,
            ) {
                Image(
                    painter = painterResource(id = R.drawable.ic_signallq_logo),
                    contentDescription = "Logo SignallQ",
                    modifier = Modifier.size(logoSizeDp * 0.72f),
                )
            }
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(0.5f)
                    .padding(horizontal = LkSpacing.xl),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = stringResource(R.string.onboarding_tela1_titulo),
                style = MaterialTheme.typography.headlineSmall,
                color = c.onSurface,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                text = stringResource(R.string.onboarding_tela1_subtitulo),
                style = MaterialTheme.typography.bodyLarge,
                color = c.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )

            Spacer(Modifier.height(LkSpacing.xl))

            val textoTermos =
                buildAnnotatedString {
                    append("Li e aceito os ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "termos_de_uso",
                            styles = TextLinkStyles(style = SpanStyle(color = c.primary, fontWeight = FontWeight.SemiBold)),
                        ) { onAbrirTermos() },
                    ) {
                        append("Termos de Uso")
                    }
                    append(" e a ")
                    withLink(
                        LinkAnnotation.Clickable(
                            tag = "politica_privacidade",
                            styles = TextLinkStyles(style = SpanStyle(color = c.primary, fontWeight = FontWeight.SemiBold)),
                        ) { onAbrirPrivacidade() },
                    ) {
                        append("Política de Privacidade")
                    }
                }

            LkSurfaceCard(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clickable { onTermosAceitosChange(!termosAceitos) },
                outlined = false,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Checkbox(
                        checked = termosAceitos,
                        onCheckedChange = onTermosAceitosChange,
                        modifier =
                            Modifier.semantics {
                                contentDescription = "Aceitar termos de uso e política de privacidade"
                            },
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = textoTermos,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.onSurface,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .weight(0.18f)
                    .padding(horizontal = LkSpacing.xl)
                    .navigationBarsPadding(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Button(
                onClick = onContinuar,
                enabled = termosAceitos,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = if (termosAceitos) "Começar" else "Aceite os termos para continuar"
                        },
                colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
            ) {
                Text(
                    text = stringResource(R.string.onboarding_tela1_btn_comecar),
                    color = c.onPrimary,
                )
            }
        }
    }
}
