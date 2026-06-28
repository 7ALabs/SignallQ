package io.signallq.app.ui.screen

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.feature.diagnostico.ai.PerguntaContextual
import io.signallq.app.feature.diagnostico.pulse.OpcaoResposta
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.AiModelFooter
import io.signallq.app.ui.component.AppBorderGlowEffect
import io.signallq.app.ui.component.SignallQAiMessageBubble
import io.signallq.app.ui.component.SignallQInlineQuestion
import io.signallq.app.ui.component.SignallQInputArea
import io.signallq.app.ui.component.SignallQThinkingBubble
import io.signallq.app.ui.component.SignallQTopBar
import io.signallq.app.ui.component.SignallQUserMessageBubble
import io.signallq.app.ui.component.SignallQWelcomeState

@Composable
fun SignallQScreen(
    uiState: SignallQUiState,
    onIniciarSignallQ: (foco: String?) -> Unit,
    onResetSignallQ: () -> Unit,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onResponderPergunta: (OpcaoResposta) -> Unit,
    modifier: Modifier = Modifier,
    currentSsid: String? = null,
) {
    val tokens = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .background(tokens.bgPrimary),
    ) {
        SignallQTopBar(uiState = uiState)
        SignallQChatContent(
            uiState = uiState,
            onIniciarSignallQ = onIniciarSignallQ,
            onResetSignallQ = onResetSignallQ,
            onSelecionarChip = onSelecionarChip,
            onResponderPergunta = onResponderPergunta,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun SignallQChatContent(
    uiState: SignallQUiState,
    onIniciarSignallQ: (foco: String?) -> Unit,
    onResetSignallQ: () -> Unit,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onResponderPergunta: (OpcaoResposta) -> Unit,
    modifier: Modifier = Modifier,
) {
    val listState = rememberLazyListState()

    val isIdle = uiState is SignallQUiState.Idle
    val isLoading = uiState is SignallQUiState.Collecting || uiState is SignallQUiState.Thinking
    val isAnalyzing = uiState is SignallQUiState.Analyzing
    val isError = uiState is SignallQUiState.Erro
    val isAwaitingAnswer = uiState is SignallQUiState.AwaitingAnswer

    val loadingMessage =
        when (uiState) {
            is SignallQUiState.Collecting -> uiState.mensagem
            is SignallQUiState.Thinking -> uiState.mensagem
            is SignallQUiState.Analyzing -> uiState.mensagem
            else -> ""
        }

    val session =
        when (uiState) {
            is SignallQUiState.Analyzing -> uiState.session
            is SignallQUiState.AwaitingChipSelection -> uiState.session
            is SignallQUiState.AwaitingAnswer -> uiState.session
            is SignallQUiState.Result -> uiState.session
            else -> null
        }

    val availableChips =
        when (uiState) {
            is SignallQUiState.AwaitingChipSelection -> uiState.chips
            is SignallQUiState.Result -> uiState.availableChips
            else -> emptyList()
        }

    val pendingQuestion = (uiState as? SignallQUiState.AwaitingAnswer)?.question
    val perguntasContextuais =
        session
            ?.analyses
            ?.lastOrNull()
            ?.fullResult
            ?.perguntasContextuais
            ?.takeIf { uiState is SignallQUiState.Result }
            .orEmpty()
    val modeloIa =
        session
            ?.analyses
            ?.lastOrNull()
            ?.fullResult
            ?.modeloIa
    val hasChipsFooter = availableChips.isNotEmpty()
    val hasAnyFooter = hasChipsFooter || modeloIa != null
    val chatBottomPadding = if (hasAnyFooter) 176.dp else 112.dp
    var inputValue by remember { mutableStateOf(TextFieldValue("")) }

    // Auto-scroll when analyses count or loading state changes
    val analysesCount = session?.analyses?.size ?: 0
    val scrollKey =
        analysesCount * 10 + (if (isAnalyzing || isLoading) 1 else 0) +
            (if (isAwaitingAnswer) 2 else 0)
    LaunchedEffect(scrollKey) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Box(modifier = modifier.fillMaxSize()) {
        AppBorderGlowEffect(active = isAnalyzing || isLoading)

        LazyColumn(
            state = listState,
            contentPadding =
                PaddingValues(
                    start = LkSpacing.lg,
                    end = LkSpacing.lg,
                    top = LkSpacing.md,
                    bottom = chatBottomPadding,
                ),
            // T2.5: espaçamento zero — cada item controla seu próprio padding vertical
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // ── Idle: welcome state fills the viewport ──────────────────────────
            if (isIdle) {
                item(key = "welcome") {
                    SignallQWelcomeState(
                        onIniciarSignallQ = onIniciarSignallQ,
                        modifier = Modifier.fillParentMaxSize(),
                    )
                }
                return@LazyColumn
            }

            // ── Initial user intent bubble ──────────────────────────────────────
            val foco = uiState.focoDiagnostico
            val intentLabel = foco?.removePrefix("Foco: ") ?: "Diagnóstico geral"
            item(key = "user_intent") {
                SignallQUserMessageBubble(
                    text = intentLabel,
                    modifier =
                        Modifier.animateItem(
                            fadeInSpec = tween(200, easing = LinearOutSlowInEasing),
                        ),
                )
            }

            // ── AI analyses interleaved with user chip selections ───────────────
            session?.let { s ->
                s.analyses.forEachIndexed { idx, analysis ->
                    item(key = "ai_${analysis.timestamp}") {
                        SignallQAiMessageBubble(
                            analysis = analysis,
                            isLatest = !isAnalyzing && idx == s.analyses.lastIndex,
                            session = if (idx == 0) s else null,
                            modifier =
                                Modifier.animateItem(
                                    fadeInSpec = tween(200, easing = LinearOutSlowInEasing),
                                ),
                        )
                    }
                    s.chipHistory.getOrNull(idx)?.let { userText ->
                        item(key = "user_chip_${idx}_${s.sessionId}") {
                            SignallQUserMessageBubble(
                                text = userText,
                                modifier =
                                    Modifier.animateItem(
                                        fadeInSpec = tween(200, easing = LinearOutSlowInEasing),
                                    ),
                            )
                        }
                    }
                }
            }

            // ── Loading / thinking bubble (Collecting, Thinking, Analyzing) ─────
            if (isLoading || isAnalyzing) {
                item(key = "thinking") {
                    SignallQThinkingBubble(
                        mensagem = loadingMessage.ifBlank { "Analisando..." },
                        modifier =
                            Modifier.animateItem(
                                fadeInSpec = tween(200, easing = LinearOutSlowInEasing),
                            ),
                    )
                }
            }

            // ── Inline question from SignallQ (AwaitingAnswer — dynamic Q engine) ─
            if (isAwaitingAnswer && pendingQuestion != null) {
                item(key = "inline_question_${pendingQuestion.id}") {
                    SignallQInlineQuestion(
                        texto = pendingQuestion.texto,
                        opcoes = pendingQuestion.opcoes,
                        onResponder = onResponderPergunta,
                    )
                }
            }

            // ── Contextual questions from AI result (shown inline in chat) ──────
            if (perguntasContextuais.isNotEmpty()) {
                val firstQuestion = perguntasContextuais.first()
                item(key = "contextual_${firstQuestion.id}") {
                    SignallQInlineQuestion(
                        texto = firstQuestion.pergunta,
                        opcoes = firstQuestion.toOpcoes(),
                        onResponder = onSelecionarChip,
                    )
                }
            }

            // ── Error state ─────────────────────────────────────────────────────
            if (isError) {
                item(key = "error") {
                    SignallQErrorCard(
                        mensagem = (uiState as SignallQUiState.Erro).mensagem,
                        onTentar = { onIniciarSignallQ(uiState.focoDiagnostico) },
                    )
                }
            }

            // ── "Novo diagnóstico" button when session is complete ──────────────
            val hasConversation = (session?.analyses?.isNotEmpty() == true)
            val isComplete = !isAnalyzing && !isLoading && hasConversation
            if (isComplete) {
                item(key = "new_diagnosis") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        OutlinedButton(onClick = onResetSignallQ) {
                            Text(
                                "Novo diagnóstico",
                                fontSize = 14.sp,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontWeight = FontWeight.W600,
                            )
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(4.dp)) }
        }

        // ── Bottom footer stack ─────────────────────────────────────────────────
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            AiModelFooter(modeloIa = modeloIa)
            SignallQInputArea(
                value = inputValue,
                onValueChange = { inputValue = it },
                onEnviarMensagem = {
                    val text = inputValue.text.trim()
                    if (text.isBlank()) return@SignallQInputArea
                    onSelecionarChip(
                        OpcaoResposta(
                            id = "typed_${System.currentTimeMillis()}",
                            label = text,
                            contextoParaIA = text,
                        ),
                    )
                    inputValue = TextFieldValue("")
                },
                chips = availableChips,
                onSelecionarChip = onSelecionarChip,
                // T2.3: chips sumem após a primeira resposta da IA chegar
                hasAiResponse = session?.analyses?.isNotEmpty() == true,
            )
        }
    }
}

private fun PerguntaContextual.toOpcoes(): List<OpcaoResposta> =
    opcoes.map { op ->
        OpcaoResposta(
            id = "${id}_${op.id}",
            label = op.rotulo,
            contextoParaIA = "$pergunta → ${op.rotulo}",
        )
    }

@Composable
private fun SignallQErrorCard(
    mensagem: String,
    onTentar: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
    ) {
        Text(
            mensagem,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.error,
            modifier = Modifier.weight(1f),
        )
        TextButton(onClick = onTentar) { Text("Tentar novamente") }
    }
}
