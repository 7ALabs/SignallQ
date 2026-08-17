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
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.feature.speedtest.ResultadoSpeedtest

// Overlay do diagnóstico guiado — issue #1704 (2.0.09b), épico #1647.
//
// Migra o bloco `AnimatedVisibility` que vivia inline em `AppShell.kt` (linhas 999-1041, 24
// parâmetros de tela). É o PRIMEIRO overlay migrado depois da #1698, e por isso estreia no
// registro o padrão que a ressalva 3 de Caio (PR #1697) tornou obrigatório: **um `@Stable data
// class` de entrada, não N campos soltos**.
//
// Sem isso, `AppShellOverlayRegistry` sairia de 17 para ~35 parâmetros só com esta migração — o
// registro viraria o próximo monolito, que é exatamente o que a decisão da #1698 evita. Ver
// `docs_ai/technical/appshell-root-content-registry.md`, seção "um parâmetro por raiz".

/**
 * Tudo que o overlay do diagnóstico guiado consome do shell, em três grupos coesos.
 *
 * A divisão não é estética: cada grupo tem um dono diferente e muda por motivos diferentes —
 * [dados] vem dos snapshots, [operadora] é a cadeia de resolução injetada da `MainActivity`
 * (GH#970), e [acoes] é navegação/estado do shell. Uma fatia futura que mexa só na resolução de
 * operadora toca um campo, não a assinatura inteira.
 */
@Stable
internal data class AppShellDiagnosticoGuiadoEntry(
    val dados: AppShellDiagnosticoGuiadoDados,
    val operadora: AppShellOperadoraResolvers,
    val acoes: AppShellDiagnosticoGuiadoAcoes,
)

/** Dados de diagnóstico e contexto de rede que a tela exibe. */
@Stable
internal data class AppShellDiagnosticoGuiadoDados(
    val input: DiagnosticInput?,
    val resultado: ResultadoSpeedtest?,
    val analisadorState: AnalisadorState,
    val objetivoPreSelecionado: ObjetivoDiagnostico?,
    val respostaPreSelecionadaPasso0: Int?,
    val categoria: String?,
    val ispNome: String?,
    val operadoraMovel: String?,
    val recommendationDecision: RecommendationDecision?,
    val recommendationFeedback: RecommendationFeedbackType?,
)

/** Navegação e efeitos colaterais que só o shell sabe executar. */
@Stable
internal data class AppShellDiagnosticoGuiadoAcoes(
    val onAnalisarProblema: (String?) -> Unit,
    val onResetarAnalisador: () -> Unit,
    val onVoltar: () -> Unit,
    val onIrParaHome: () -> Unit,
    val onIniciarModoGamer: () -> Unit,
    val onAbrirFerramentaSugerida: (TipoFerramenta) -> Unit,
    val onRecommendationShown: () -> Unit,
    val onRecommendationClicked: () -> Unit,
    val onRecommendationFeedback: (RecommendationFeedbackType) -> Unit,
)

/**
 * O overlay em si. Comportamento idêntico ao bloco inline que substitui, incluindo a guarda dupla:
 * o container só compõe quando o overlay está na pilha **e** há resultado de speedtest.
 *
 * O `testTag` no container existe pelo mesmo motivo do `AppShellDetalhesTecnicosOverlay`: sem ele,
 * um teste não distingue "container não compôs" (guarda presente) de "container compôs vazio"
 * (guarda removida) — o `?.let` interno omite o conteúdo sozinho nos dois casos. Achado da
 * revisão de Caio na PR #1697, aplicado aqui de saída em vez de depois.
 */
@Composable
internal fun AppShellDiagnosticoGuiadoOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    entry: AppShellDiagnosticoGuiadoEntry,
) {
    val dados = entry.dados
    AnimatedVisibility(
        visible = AppShellOverlay.DiagnosticoGuiado in overlayStack,
        modifier =
            Modifier
                .zIndex(rememberOverlayZIndex(AppShellOverlay.DiagnosticoGuiado, overlayStack))
                .testTag(TAG_OVERLAY_DIAGNOSTICO_GUIADO),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        // GH#1714 — ver ResultadoIndisponivelScreen. Sem resultado o overlay não compunha nada,
        // e o back virava um toque sem efeito visível.
        val resultado = dados.resultado
        if (resultado == null) {
            ResultadoIndisponivelScreen(
                titulo = "Diagnóstico",
                onVoltar = entry.acoes.onVoltar,
                onMedirNovamente = entry.acoes.onIrParaHome,
            )
        } else {
            DiagnosticoGuiadoScreen(
                input = dados.input,
                resultadoValidoParaConclusao = resultado.status.liberaConclusaoCompleta,
                objetivoPreSelecionado = dados.objetivoPreSelecionado,
                respostaPreSelecionadaPasso0 = dados.respostaPreSelecionadaPasso0,
                analisadorState = dados.analisadorState,
                onAnalisarProblema = entry.acoes.onAnalisarProblema,
                onResetarAnalisador = entry.acoes.onResetarAnalisador,
                onVoltar = entry.acoes.onVoltar,
                onIrParaHome = entry.acoes.onIrParaHome,
                categoria = dados.categoria,
                ispNome = dados.ispNome,
                connectionType = resultado.connectionType,
                operadoraMovel = dados.operadoraMovel,
                recommendationDecision = dados.recommendationDecision,
                recommendationFeedback = dados.recommendationFeedback,
                onRecommendationShown = entry.acoes.onRecommendationShown,
                onRecommendationClicked = entry.acoes.onRecommendationClicked,
                onRecommendationFeedback = entry.acoes.onRecommendationFeedback,
                resolveOperadoraIdentidadeLocal = entry.operadora.identidadeLocal,
                resolveOperadoraContatoLocal = entry.operadora.contatoLocal,
                resolveOperadoraIdentidadeRemota = entry.operadora.identidadeRemota,
                resolveOperadoraContatoRemoto = entry.operadora.contatoRemoto,
                onIniciarModoGamer = entry.acoes.onIniciarModoGamer,
                onAbrirFerramentaSugerida = entry.acoes.onAbrirFerramentaSugerida,
            )
        }
    }
}

internal const val TAG_OVERLAY_DIAGNOSTICO_GUIADO = "appshell_overlay_diagnostico_guiado"
