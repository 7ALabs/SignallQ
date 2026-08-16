package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.zIndex
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.network.AssistAbandonado
import io.signallq.app.core.network.AssistEtapa
import io.signallq.app.core.network.AssistObjetivoSelecionado
import io.signallq.app.core.network.AssistOrigem
import io.signallq.app.core.network.AssistPerguntaRespondida

/**
 * Overlay do SignallQ Assist (issue #1656, épico #1647) — seleção de sintoma e a pergunta
 * contextual de uma pergunta só, antes do teste de velocidade. Extraído do corpo de [AppShell]
 * para não inflar ainda mais um arquivo já classificado como dívida crítica (higiene
 * §4.3/§7): aqui só fica a orquestração de `AnimatedVisibility` + [AssistScreen] e a conversão
 * dos callbacks locais para os eventos tipados de `AssistAnalytics.kt` — nenhuma regra de
 * domínio nova, reusa [ObjetivoDiagnostico]/`PerguntasDiagnosticoGuiado` como o resto do app.
 *
 * [onPreSelecaoParaDiagnosticoGuiado] leva o objetivo/resposta escolhidos aqui para o
 * "plano existente" (`DiagnosticoGuiadoScreen`, aberto depois do resultado da velocidade) —
 * sem isso a pergunta do Assist não mudaria nada além de analytics, o que contraria o
 * critério de aceite da issue ("pergunta só existe se muda plano, recomendação ou
 * confiança"): [io.signallq.app.core.diagnostico.DiagnosticoGuiadoEngine] usa exatamente o
 * índice dessa primeira resposta para JOGOS_COM_LAG/WIFI_VS_OPERADORA.
 */
@Composable
internal fun AppShellAssistOverlay(
    overlayStack: MutableList<AppShellOverlay>,
    onAssistObjetivo: (AssistObjetivoSelecionado) -> Unit,
    onAssistResposta: (AssistPerguntaRespondida) -> Unit,
    onAssistAbandono: (AssistAbandonado) -> Unit,
    onPreSelecaoParaDiagnosticoGuiado: (objetivo: ObjetivoDiagnostico?, respostaPasso0: Int?) -> Unit,
    onSolicitarDiagnostico: () -> Long?,
) {
    AnimatedVisibility(
        visible = AppShellOverlay.Assist in overlayStack,
        modifier = Modifier.zIndex(rememberOverlayZIndex(AppShellOverlay.Assist, overlayStack)),
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
    ) {
        AssistScreen(
            onObjetivoConfirmado = { objetivo, retomada ->
                onAssistObjetivo(
                    AssistObjetivoSelecionado(
                        objetivoId = objetivo?.analyticsId() ?: "neutro",
                        origem = AssistOrigem.Inicio2,
                        retomada = retomada,
                    ),
                )
            },
            onRespostaConfirmada = { objetivo, contexto, resposta, retomada ->
                onAssistResposta(
                    AssistPerguntaRespondida(
                        objetivoId = objetivo.analyticsId(),
                        perguntaId = contexto.perguntaId,
                        respostaId = "opcao_${resposta + 1}",
                        retomada = retomada,
                    ),
                )
            },
            onConcluir = { objetivo, respostas ->
                onPreSelecaoParaDiagnosticoGuiado(objetivo, respostas.firstOrNull())
                overlayStack.remove(AppShellOverlay.Assist)
                onSolicitarDiagnostico()
            },
            onAbandonar = { objetivo, retomavel ->
                onAssistAbandono(
                    AssistAbandonado(
                        etapa = if (objetivo == null) AssistEtapa.Objetivo else AssistEtapa.Contexto,
                        objetivoId = objetivo?.analyticsId(),
                        retomavel = retomavel,
                    ),
                )
                // Review da PR #1683, bloqueio 3 — abandonar também precisa zerar uma
                // pré-seleção de uma sessão ANTERIOR do Assist ainda guardada aqui (AppShell
                // sobrevive à recriação do overlay; AssistScreenState não). Sem isso, uma escolha
                // já abandonada continuava alimentando o DiagnosticoGuiadoScreen na próxima vez
                // que o usuário completasse o teste, mesmo sem retomar o Assist.
                onPreSelecaoParaDiagnosticoGuiado(null, null)
                overlayStack.remove(AppShellOverlay.Assist)
            },
        )
    }
}
