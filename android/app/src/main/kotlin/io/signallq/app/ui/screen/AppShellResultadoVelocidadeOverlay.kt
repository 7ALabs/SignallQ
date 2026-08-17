package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.zIndex
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.speedtest.ResultadoSpeedtest
import io.signallq.app.ui.IspInfo

// Overlay do resultado de velocidade — extraído de `AppShell.kt` pela issue #1714.
//
// Era o único dos três overlays de resultado que continuava inline, e o maior deles. A #1714 o
// fez crescer mais um `if/else` de 45 linhas dentro de um arquivo de 1631 — em cima de um arquivo
// que o épico #1647 inteiro tenta encolher, pela terceira PR seguida (achado de Caio, higiene §7:
// "ao tocar em arquivo acima de 800 linhas, extrair ao menos uma responsabilidade quando isso for
// pequeno e seguro").
//
// Aqui é pequeno e seguro porque o padrão-alvo já existia no mesmo diff: `AppShellDetalhesTecnicos
// Overlay.kt` e `AppShellDiagnosticoGuiadoOverlay.kt` são exatamente esta forma. Não é reforma —
// é aplicar ao terceiro o que já foi aplicado aos outros dois.

/** Dados e ações do overlay de resultado. Grupo único, no padrão que a #1698 estabeleceu. */
@Stable
internal data class AppShellResultadoVelocidadeEntry(
    val resultado: ResultadoSpeedtest?,
    val snapshotDiagnostico: SnapshotDiagnostico,
    val analisadorState: AnalisadorState,
    val localizacaoServidor: String?,
    val ispInfo: IspInfo?,
    val operadoraMovel: String?,
    val recommendationDecision: RecommendationDecision?,
    val adsEnabled: Boolean,
    val onTestarNovamente: () -> Unit,
    val onIrParaHome: () -> Unit,
    val onVoltar: () -> Unit,
    val onCompartilhar: () -> Unit,
    val onMedirNovamente: () -> Unit,
    val onIniciarDiagnosticoGuiado: () -> Unit,
    val onIniciarModoGamer: () -> Unit,
    val onVerDetalhesTecnicos: () -> Unit,
)

/**
 * Sem resultado, mostra [ResultadoIndisponivelScreen] em vez de não compor nada — ver o KDoc dela
 * para o porquê (GH#1714). O `testTag` no container distingue "não compôs" de "compôs vazio",
 * mesmo instrumento que Caio pediu no `DetalhesTecnicos` na PR #1697.
 */
@Composable
internal fun AppShellResultadoVelocidadeOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    entry: AppShellResultadoVelocidadeEntry,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.ResultadoVelocidade in overlayStack,
        modifier =
            Modifier
                .zIndex(rememberOverlayZIndex(AppShellOverlay.ResultadoVelocidade, overlayStack))
                .testTag(TAG_OVERLAY_RESULTADO_VELOCIDADE),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        val resultado = entry.resultado
        if (resultado == null) {
            ResultadoIndisponivelScreen(
                titulo = "Resultado",
                onVoltar = entry.onVoltar,
                onMedirNovamente = entry.onMedirNovamente,
            )
        } else {
            ResultadoVelocidadeScreen(
                resultado = resultado,
                snapshotDiagnostico = entry.snapshotDiagnostico,
                onTestarNovamente = entry.onTestarNovamente,
                onIrParaHome = entry.onIrParaHome,
                onVoltar = entry.onVoltar,
                onCompartilhar = entry.onCompartilhar,
                localizacaoServidor = entry.localizacaoServidor,
                ispInfo = entry.ispInfo,
                operadoraMovel = entry.operadoraMovel,
                analisadorState = entry.analisadorState,
                onIniciarDiagnosticoGuiado = entry.onIniciarDiagnosticoGuiado,
                onIniciarModoGamer = entry.onIniciarModoGamer,
                onVerDetalhesTecnicos = entry.onVerDetalhesTecnicos,
                recommendationDecision = entry.recommendationDecision,
                adsEnabled = entry.adsEnabled,
            )
        }
    }
}

internal const val TAG_OVERLAY_RESULTADO_VELOCIDADE = "appshell_overlay_resultado_velocidade"
