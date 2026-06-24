package io.veloo.app.ui.screen

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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.veloo.app.feature.diagnostico.ai.PerguntaContextual
import io.veloo.app.feature.diagnostico.pulse.OpcaoResposta
import io.veloo.app.feature.diagnostico.pulse.ResponseSource
import io.veloo.app.ui.LkSpacing
import io.veloo.app.ui.LocalLkTokens
import io.veloo.app.ui.component.AiModelFooter
import io.veloo.app.ui.component.AppBorderGlowEffect
import io.veloo.app.ui.component.SignallQAiMessageBubble
import io.veloo.app.ui.component.SignallQInlineQuestion
import io.veloo.app.ui.component.SignallQInputArea
import io.veloo.app.ui.component.SignallQThinkingBubble
import io.veloo.app.ui.component.SignallQUserMessageBubble

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    uiState: SignallQUiState,
    onNavigateBack: () -> Unit,
    onIniciarSignallQ: (foco: String?) -> Unit,
    onResetSignallQ: () -> Unit,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onResponderPergunta: (OpcaoResposta) -> Unit,
    // T6.2/T6.5: mensagens digitadas têm fluxo separado (guard off-topic + turno)
    onEnviarMensagemTexto: (String) -> Unit = {},
) {
    val c = LocalLkTokens.current

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Assistente SignallQ",
                        style = MaterialTheme.typography.titleLarge,
                        color = c.textPrimary,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors =
                    TopAppBarDefaults.centerAlignedTopAppBarColors(
                        containerColor = c.bgPrimary,
                    ),
            )
        },
    ) { innerPadding ->
        ChatContent(
            uiState = uiState,
            onIniciarSignallQ = onIniciarSignallQ,
            onResetSignallQ = onResetSignallQ,
            onSelecionarChip = onSelecionarChip,
            onResponderPergunta = onResponderPergunta,
            onEnviarMensagemTexto = onEnviarMensagemTexto,
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
        )
    }
}

@Composable
private fun ChatContent(
    uiState: SignallQUiState,
    onIniciarSignallQ: (foco: String?) -> Unit,
    onResetSignallQ: () -> Unit,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onResponderPergunta: (OpcaoResposta) -> Unit,
    onEnviarMensagemTexto: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val listState = rememberLazyListState()

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

    // T6.3: input bloqueado quando sessão atingiu 5 turnos do usuário
    val isLimitReached = session?.userTurnCount?.let { it >= 5 } == true

    val analysesCount = session?.analyses?.size ?: 0
    val scrollKey =
        analysesCount * 10 + (if (isAnalyzing || isLoading) 1 else 0) +
            (if (isAwaitingAnswer) 2 else 0)
    LaunchedEffect(scrollKey) {
        val count = listState.layoutInfo.totalItemsCount
        if (count > 0) listState.animateScrollToItem(count - 1)
    }

    Box(
        modifier =
            modifier
                .fillMaxSize()
                .background(c.bgPrimary),
    ) {
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
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            // Estado inicial — Thinking/Collecting chega aqui logo após navegar
            if (uiState is SignallQUiState.Idle) {
                item(key = "idle_hint") {
                    Box(
                        modifier =
                            Modifier
                                .fillParentMaxSize()
                                .padding(horizontal = LkSpacing.lg),
                        contentAlignment = Alignment.Center,
                    ) {
                        SignallQThinkingBubble(mensagem = "Iniciando análise...")
                    }
                }
                return@LazyColumn
            }

            // Intenção inicial do usuário
            val foco = uiState.focoDiagnostico
            val intentLabel = foco?.removePrefix("Foco: ") ?: "Diagnóstico geral"
            item(key = "user_intent") {
                SignallQUserMessageBubble(
                    text = intentLabel,
                    modifier =
                        Modifier
                            .animateItem(fadeInSpec = tween(200, easing = LinearOutSlowInEasing))
                            .semantics { contentDescription = "Você: $intentLabel" },
                )
            }

            // Análises da IA intercaladas com chips do usuário
            session?.let { s ->
                val hasInsights = s.analyses.any { it.source == ResponseSource.INSIGHT }
                val firstGemmaIdx = s.analyses.indexOfFirst { it.source == ResponseSource.GEMMA }
                s.analyses.forEachIndexed { idx, analysis ->
                    // Separador fino antes da primeira resposta Gemma, quando há insights antes dela
                    if (idx == firstGemmaIdx && hasInsights && firstGemmaIdx > 0) {
                        item(key = "gemma_divider") {
                            HorizontalDivider(
                                modifier =
                                    Modifier.padding(
                                        vertical = LkSpacing.md,
                                        horizontal = LkSpacing.lg,
                                    ),
                                color = c.border.copy(alpha = 0.5f),
                                thickness = 0.5.dp,
                            )
                        }
                    }
                    item(key = "ai_${analysis.id}") {
                        SignallQAiMessageBubble(
                            analysis = analysis,
                            isLatest = !isAnalyzing && idx == s.analyses.lastIndex,
                            // Métricas inline só para a primeira entry Gemma (initial analysis)
                            session = if (idx == firstGemmaIdx) s else null,
                            modifier =
                                Modifier
                                    .animateItem(fadeInSpec = tween(200, easing = LinearOutSlowInEasing))
                                    .semantics { contentDescription = "Assistente: ${analysis.content}" },
                        )
                    }
                    s.chipHistory.getOrNull(idx)?.let { userText ->
                        item(key = "user_chip_${idx}_${s.sessionId}") {
                            SignallQUserMessageBubble(
                                text = userText,
                                modifier =
                                    Modifier
                                        .animateItem(fadeInSpec = tween(200, easing = LinearOutSlowInEasing))
                                        .semantics { contentDescription = "Você: $userText" },
                            )
                        }
                    }
                }
            }

            // Loading / thinking bubble
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

            // Pergunta inline do motor dinâmico
            if (isAwaitingAnswer && pendingQuestion != null) {
                item(key = "inline_question_${pendingQuestion.id}") {
                    SignallQInlineQuestion(
                        texto = pendingQuestion.texto,
                        opcoes = pendingQuestion.opcoes,
                        onResponder = onResponderPergunta,
                    )
                }
            }

            // Perguntas contextuais da IA
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

            // Erro
            if (isError) {
                item(key = "error") {
                    ChatErrorCard(
                        mensagem = (uiState as SignallQUiState.Erro).mensagem,
                        onTentar = { onIniciarSignallQ(uiState.focoDiagnostico) },
                    )
                }
            }

            // Botão "Novo diagnóstico" quando sessão está completa
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

        // Footer: modelo + input
        Column(
            modifier =
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth(),
        ) {
            AiModelFooter(modeloIa = modeloIa)
            // Contador de turnos: exibe quando >= 3 turnos usados
            val userTurnCount = session?.userTurnCount ?: 0
            androidx.compose.animation.AnimatedVisibility(visible = userTurnCount >= 3) {
                val c = LocalLkTokens.current
                if (isLimitReached) {
                    androidx.compose.foundation.layout.Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LkSpacing.lg, vertical = 4.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = "Limite atingido. Inicie um novo diagnóstico.",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                        )
                    }
                } else {
                    androidx.compose.foundation.layout.Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LkSpacing.lg, vertical = 4.dp),
                        contentAlignment = Alignment.CenterEnd,
                    ) {
                        Text(
                            text = "${5 - userTurnCount} perguntas restantes",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                        )
                    }
                }
            }
            SignallQInputArea(
                value = inputValue,
                onValueChange = { inputValue = it },
                onEnviarMensagem = {
                    val text = inputValue.text.trim()
                    if (text.isBlank()) return@SignallQInputArea
                    // T6.2/T6.5: mensagem digitada usa fluxo separado (guard off-topic + turno)
                    onEnviarMensagemTexto(text)
                    inputValue = TextFieldValue("")
                },
                chips = availableChips,
                onSelecionarChip = onSelecionarChip,
                hasAiResponse = session?.analyses?.isNotEmpty() == true,
                isLimitReached = isLimitReached,
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
private fun ChatErrorCard(
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
