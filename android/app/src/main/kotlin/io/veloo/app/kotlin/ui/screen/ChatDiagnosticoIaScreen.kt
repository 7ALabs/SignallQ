package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.EaseInOut
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import io.signallq.app.feature.diagnostico.chat.ChatMensagem
import io.signallq.app.feature.diagnostico.chat.PapelChatMensagem
import io.signallq.app.feature.diagnostico.chat.SessaoChatDiagnostico
import io.signallq.app.feature.diagnostico.chat.StatusChatMensagem
import io.signallq.app.feature.diagnostico.chat.TipoDiagnostico
import io.signallq.app.feature.diagnostico.pulse.OpcaoResposta
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.SignallQInputArea
import io.signallq.app.ui.component.SignallQThinkingBubble
import io.signallq.app.ui.component.SignallQUserMessageBubble
import io.signallq.app.ui.component.TypewriterText
import io.signallq.app.ui.viewmodel.ChatDiagUiState
import io.signallq.app.ui.viewmodel.EstadoChatDiagnostico
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ─────────────────────────────────────────────────────────────────────────────
// Screen principal (Stateless)
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDiagnosticoIaScreen(
    uiState: ChatDiagUiState,
    onVoltar: () -> Unit,
    onEnviarMensagem: (String) -> Unit,
    onAtualizarDraft: (String) -> Unit,
    onEscolherOpcao: (TipoDiagnostico) -> Unit,
    onAbrirSessao: (String) -> Unit,
    onApagarSessao: (String) -> Unit,
    onRenomearSessao: (String, String) -> Unit,
    onNovaSessao: () -> Unit,
    onToggleDrawer: () -> Unit,
    onCancelarAcaoAtual: () -> Unit,
) {
    val tokens = LocalLkTokens.current
    val drawerState =
        rememberDrawerState(
            initialValue =
                if (uiState.drawerAberto) {
                    androidx.compose.material3.DrawerValue.Open
                } else {
                    androidx.compose.material3.DrawerValue.Closed
                },
        )
    val scope = rememberCoroutineScope()

    // Sincroniza drawerState com uiState.drawerAberto
    LaunchedEffect(uiState.drawerAberto) {
        if (uiState.drawerAberto) {
            drawerState.open()
        } else {
            drawerState.close()
        }
    }

    // Quando o drawer abre/fecha fora do ViewModel (swipe), sincroniza
    LaunchedEffect(drawerState.isOpen) {
        if (drawerState.isOpen != uiState.drawerAberto) {
            onToggleDrawer()
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            DrawerConteudo(
                sessoes = uiState.sessoesAnteriores,
                sessaoAtualId = uiState.sessaoAtual?.id,
                onNovaSessao = {
                    onNovaSessao()
                    scope.launch { drawerState.close() }
                },
                onAbrirSessao = { id ->
                    onAbrirSessao(id)
                    scope.launch { drawerState.close() }
                },
                onApagarSessao = onApagarSessao,
                onRenomearSessao = onRenomearSessao,
            )
        },
    ) {
        Scaffold(
            containerColor = tokens.bgPrimary,
            contentWindowInsets = WindowInsets.systemBars,
            topBar = {
                CenterAlignedTopAppBar(
                    title = {
                        Text(
                            text = "Diagnóstico IA",
                            style = MaterialTheme.typography.titleLarge,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onVoltar) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Voltar",
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                onToggleDrawer()
                                scope.launch { drawerState.open() }
                            },
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.History,
                                contentDescription = "Histórico de conversas",
                            )
                        }
                    },
                    colors =
                        TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = tokens.bgPrimary,
                        ),
                )
            },
        ) { innerPadding ->
            Column(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .windowInsetsPadding(WindowInsets.ime),
            ) {
                // Lista de mensagens
                ListaMensagens(
                    modifier = Modifier.weight(1f),
                    uiState = uiState,
                    onEscolherOpcao = onEscolherOpcao,
                )

                // Footer: input ou banner de cota
                if (uiState.estado == EstadoChatDiagnostico.CotaExcedida) {
                    CotaExcedidaBanner(
                        renovacaoEpochMs = uiState.cota?.renovacaoEpochMs,
                    )
                } else {
                    // Draft local para TextFieldValue — OnAtualizarDraft chamado no onChange
                    ChatInputArea(
                        draft = uiState.mensagemEmDigitacao,
                        onDraftChange = onAtualizarDraft,
                        onEnviar = { texto -> onEnviarMensagem(texto) },
                        enabled =
                            uiState.estado == EstadoChatDiagnostico.Idle ||
                                uiState.estado == EstadoChatDiagnostico.ErroModelo ||
                                uiState.estado == EstadoChatDiagnostico.ErroRede,
                        mostrarPlaceholderAguarde =
                            uiState.estado == EstadoChatDiagnostico.ExecutandoTeste ||
                                uiState.estado == EstadoChatDiagnostico.AguardandoIa ||
                                uiState.estado == EstadoChatDiagnostico.Streaming,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Lista de mensagens com auto-scroll inteligente
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ListaMensagens(
    uiState: ChatDiagUiState,
    onEscolherOpcao: (TipoDiagnostico) -> Unit,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()
    val tokens = LocalLkTokens.current

    // Auto-scroll inteligente: só rola pro fim se o usuário já estava lá
    val isScrolledToEnd by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisibleIndex = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisibleIndex >= totalItems - 1
        }
    }

    LaunchedEffect(uiState.mensagens.size, uiState.estado) {
        if (isScrolledToEnd && uiState.mensagens.isNotEmpty()) {
            lazyListState.animateScrollToItem(uiState.mensagens.size - 1)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxWidth(),
        contentPadding =
            androidx.compose.foundation.layout.PaddingValues(
                horizontal = LkSpacing.lg,
                vertical = LkSpacing.sm,
            ),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        items(
            items = uiState.mensagens,
            key = { it.id },
        ) { mensagem ->
            val isUltima = mensagem == uiState.mensagens.lastOrNull()
            when (mensagem.papel) {
                PapelChatMensagem.usuario -> {
                    SignallQUserMessageBubble(text = mensagem.conteudo)
                }
                PapelChatMensagem.assistente, PapelChatMensagem.sistema -> {
                    if (mensagem.status == StatusChatMensagem.streaming && mensagem.conteudo.isBlank()) {
                        SignallQThinkingBubble(
                            mensagem = "",
                            modifier =
                                Modifier.semantics {
                                    contentDescription = "Diagnóstico IA está processando"
                                },
                        )
                    } else {
                        BubbleAssistente(
                            mensagem = mensagem,
                            isLatest = isUltima,
                            tokens = tokens,
                            modeloDisplayName = uiState.modeloDisplayName,
                        )
                    }
                }
            }
        }

        // Chips inline — aparece após a última mensagem da IA
        if (uiState.opcoesIniciaisVisiveis) {
            item(key = "opcoes_iniciais_chips") {
                OpcoesIniciaisChips(
                    enabled = uiState.estado == EstadoChatDiagnostico.Idle,
                    onEscolherOpcao = onEscolherOpcao,
                )
            }
        }

        // Indicador de "pensando" enquanto aguarda/streaming com conteúdo
        if (uiState.estado == EstadoChatDiagnostico.AguardandoIa) {
            item(key = "thinking_indicator") {
                SignallQThinkingBubble(
                    mensagem = "",
                    modifier =
                        Modifier.semantics {
                            contentDescription = "Diagnóstico IA está processando"
                        },
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helper: parseia <think>...</think> do conteúdo da mensagem
// ─────────────────────────────────────────────────────────────────────────────

/**
 * Separa o bloco <think>...</think> do texto de resposta.
 *
 * Retorna Pair(thinkingText, responseText):
 *  - thinkingText: conteúdo dentro do bloco <think>, ou null se não houver.
 *  - responseText: texto da resposta sem o bloco de thinking.
 *
 * Casos especiais:
 *  - Se <think> existe mas </think> não (thinking ainda em andamento durante
 *    streaming): thinkingText = conteúdo parcial, responseText = "".
 */
private fun parseThinkingContent(text: String): Pair<String?, String> {
    val thinkStart = text.indexOf("<think>")
    if (thinkStart == -1) return Pair(null, text)

    val thinkEnd = text.indexOf("</think>", thinkStart)
    return if (thinkEnd == -1) {
        // Thinking ainda em andamento (streaming)
        val partial = text.substring(thinkStart + 7).trim()
        Pair(partial.ifBlank { null }, "")
    } else {
        val thinkContent = text.substring(thinkStart + 7, thinkEnd).trim()
        val responseContent = (text.substring(0, thinkStart) + text.substring(thinkEnd + 8)).trim()
        Pair(thinkContent.ifBlank { null }, responseContent)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Bubble da IA — versão simples para ChatMensagem (não AiAnalysisEntry)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun BubbleAssistente(
    mensagem: ChatMensagem,
    isLatest: Boolean,
    tokens: io.signallq.app.ui.LkTokens,
    modeloDisplayName: String = "Qwen3 30B",
) {
    val timeStr =
        remember(mensagem.criadoEmEpochMs) {
            val cal = Calendar.getInstance().apply { timeInMillis = mensagem.criadoEmEpochMs }
            "%02d:%02d".format(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE))
        }

    val isStreaming = mensagem.status == StatusChatMensagem.streaming
    val (thinkingText, responseText) =
        remember(mensagem.conteudo) {
            parseThinkingContent(mensagem.conteudo)
        }
    val isThinkingInProgress =
        isStreaming &&
            mensagem.conteudo.contains("<think>") &&
            !mensagem.conteudo.contains("</think>")

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .animateContentSize()
                .padding(vertical = LkSpacing.sm),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Box(
            modifier =
                Modifier
                    .padding(top = 2.dp)
                    .size(20.dp)
                    .background(
                        color = LkColors.accent.copy(alpha = 0.15f),
                        shape = RoundedCornerShape(999.dp),
                    ),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .background(
                            color = LkColors.accent,
                            shape = RoundedCornerShape(999.dp),
                        ),
            )
        }

        Box(
            modifier =
                Modifier
                    .weight(1f)
                    .background(
                        color = tokens.bgSecondary,
                        shape =
                            RoundedCornerShape(
                                topStart = 4.dp,
                                topEnd = 16.dp,
                                bottomStart = 16.dp,
                                bottomEnd = 16.dp,
                            ),
                    ).padding(LkSpacing.md),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                // ── Thinking section ──
                if (isThinkingInProgress) {
                    ThinkingInProgressRow(tokens)
                } else if (thinkingText != null) {
                    ThinkingCompletedSection(thinkingText, tokens)
                }

                val textStyle =
                    MaterialTheme.typography.bodyMedium.copy(
                        color =
                            if (mensagem.status == StatusChatMensagem.falhou) {
                                MaterialTheme.colorScheme.error
                            } else {
                                tokens.textPrimary
                            },
                    )

                val displayText = if (thinkingText != null) responseText else mensagem.conteudo
                if (displayText.isNotBlank()) {
                    if (isLatest && isStreaming) {
                        TypewriterText(text = displayText, style = textStyle)
                    } else {
                        Text(text = displayText, style = textStyle)
                    }
                }

                // ── Footer: fonte + modelo ──
                Spacer(Modifier.height(2.dp))
                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        text = "SignallQ IA · $timeStr",
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.textTertiary,
                    )
                    if (mensagem.status == StatusChatMensagem.concluido && !mensagem.isLocal) {
                        Text(
                            text = modeloDisplayName,
                            style = MaterialTheme.typography.labelSmall,
                            color = tokens.textTertiary.copy(alpha = 0.6f),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThinkingInProgressRow(tokens: io.signallq.app.ui.LkTokens) {
    val infiniteTransition = rememberInfiniteTransition(label = "thinking-pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(800, easing = EaseInOut),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "pulse-alpha",
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Box(
            modifier =
                Modifier
                    .size(8.dp)
                    .background(
                        color = LkColors.accent.copy(alpha = alpha),
                        shape = RoundedCornerShape(999.dp),
                    ),
        )
        Text(
            text = "Pensando...",
            style = MaterialTheme.typography.labelSmall,
            color = tokens.textTertiary.copy(alpha = alpha),
        )
    }
}

@Composable
private fun ThinkingCompletedSection(
    thinkingText: String,
    tokens: io.signallq.app.ui.LkTokens,
) {
    var expanded by remember { mutableStateOf(false) }
    val estimatedSeconds = (thinkingText.length / 100).coerceAtLeast(1)
    val cdRaciocinio = if (expanded) stringResource(R.string.cd_recolher_raciocinio) else stringResource(R.string.cd_expandir_raciocinio)

    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .semantics { contentDescription = cdRaciocinio }
                    .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tokens.textTertiary,
            )
            Text(
                text = "Pensou por ${estimatedSeconds}s",
                style = MaterialTheme.typography.labelSmall,
                color = tokens.textTertiary,
            )
        }

        AnimatedVisibility(
            visible = expanded,
            enter = expandVertically(animationSpec = tween(200)),
            exit = shrinkVertically(animationSpec = tween(200)),
        ) {
            Text(
                text = thinkingText,
                style = MaterialTheme.typography.bodySmall,
                color = tokens.textTertiary,
                modifier = Modifier.padding(start = 20.dp, top = LkSpacing.xs),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// OpcoesIniciaisChips — 3 botões em coluna vertical
// ─────────────────────────────────────────────────────────────────────────────

private data class ChipOpcao(
    val label: String,
    val tipo: TipoDiagnostico,
)

private val CHIPS_OPCOES =
    listOf(
        ChipOpcao("Analisar meu último teste", TipoDiagnostico.ultimoTeste),
        ChipOpcao("Executar novo teste agora", TipoDiagnostico.novoTeste),
        ChipOpcao("Analisar meu histórico recente", TipoDiagnostico.historico),
    )

@Composable
private fun OpcoesIniciaisChips(
    enabled: Boolean,
    onEscolherOpcao: (TipoDiagnostico) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier.padding(
                start = 36.dp,
                end = LkSpacing.lg,
                top = LkSpacing.sm,
            ),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        CHIPS_OPCOES.forEach { chip ->
            androidx.compose.material3.FilledTonalButton(
                onClick = { onEscolherOpcao(chip.tipo) },
                enabled = enabled,
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .heightIn(min = 48.dp)
                        .semantics { contentDescription = chip.label },
                shape = RoundedCornerShape(LkRadius.button),
                colors =
                    androidx.compose.material3.ButtonDefaults.filledTonalButtonColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer,
                        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                    ),
                contentPadding =
                    androidx.compose.foundation.layout.PaddingValues(
                        horizontal = LkSpacing.lg,
                        vertical = LkSpacing.sm,
                    ),
            ) {
                Text(
                    text = chip.label,
                    style = MaterialTheme.typography.labelLarge,
                    textAlign = TextAlign.Start,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// CotaExcedidaBanner — substitui o input quando cota excedida
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun CotaExcedidaBanner(
    renovacaoEpochMs: Long?,
    modifier: Modifier = Modifier,
) {
    val tokens = LocalLkTokens.current
    val renovacaoTexto =
        remember(renovacaoEpochMs) {
            formatarRenovacaoDateTime(renovacaoEpochMs)
        }

    Surface(
        modifier =
            modifier
                .fillMaxWidth()
                .heightIn(min = 56.dp)
                .wrapContentHeight(),
        color = tokens.warningContainer,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
        ) {
            Text(
                text = "Limite diário atingido",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = tokens.onWarningContainer,
            )
            Text(
                text = "Você poderá fazer uma nova análise $renovacaoTexto.",
                style = MaterialTheme.typography.bodySmall,
                color = tokens.onWarningContainer,
            )
        }
    }
}

private fun formatarRenovacaoDateTime(renovacaoEpochMs: Long?): String {
    if (renovacaoEpochMs == null) return "em 24 horas"
    return try {
        val renovacao =
            LocalDateTime.ofInstant(
                Instant.ofEpochMilli(renovacaoEpochMs),
                ZoneId.systemDefault(),
            )
        val agora = LocalDateTime.now(ZoneId.systemDefault())
        val horaFormatada = DateTimeFormatter.ofPattern("HH'h'mm", Locale.forLanguageTag("pt-BR")).format(renovacao)

        when {
            renovacao.toLocalDate() == agora.toLocalDate() -> "hoje às $horaFormatada"
            renovacao.toLocalDate() == agora.toLocalDate().plusDays(1) -> "amanhã às $horaFormatada"
            else -> "em 24 horas"
        }
    } catch (_: Exception) {
        "em 24 horas"
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ChatInputArea — wrapper local do SignallQInputArea
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ChatInputArea(
    draft: String,
    onDraftChange: (String) -> Unit,
    onEnviar: (String) -> Unit,
    enabled: Boolean,
    mostrarPlaceholderAguarde: Boolean,
    modifier: Modifier = Modifier,
) {
    // TextFieldValue gerenciado localmente — só sincroniza o texto com o ViewModel
    var textFieldValue by remember { mutableStateOf(TextFieldValue(draft)) }

    // Sincroniza se o draft externo mudar (ex: ao limpar após envio)
    LaunchedEffect(draft) {
        if (draft != textFieldValue.text) {
            textFieldValue = TextFieldValue(draft)
        }
    }

    // Placeholder contextual: muda conforme o estado atual do chat
    val placeholderAtual =
        when {
            mostrarPlaceholderAguarde -> "Aguarde o resultado do teste..."
            !enabled -> "Aguarde a resposta da IA..."
            else -> "Pergunte sobre sua conexão, Wi-Fi ou diagnóstico..."
        }

    SignallQInputArea(
        value = textFieldValue,
        onValueChange = { new ->
            textFieldValue = new
            onDraftChange(new.text)
        },
        onEnviarMensagem = {
            val texto = textFieldValue.text.trim()
            if (texto.isNotBlank() && enabled) {
                onEnviar(texto)
                textFieldValue = TextFieldValue("")
                onDraftChange("")
            }
        },
        chips = emptyList<OpcaoResposta>(),
        onSelecionarChip = {},
        modifier = modifier,
        // isLimitReached não se aplica aqui — cota excedida troca o componente inteiro
        // pelo CotaExcedidaBanner acima (ver ChatDiagnosticoIaScreen). O input fica
        // disabled via `enabled` mas sem bloquear a UI com a mensagem de cota.
        isLimitReached = false,
        placeholder = placeholderAtual,
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Drawer — conteúdo lateral
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerConteudo(
    sessoes: List<SessaoChatDiagnostico>,
    sessaoAtualId: String?,
    onNovaSessao: () -> Unit,
    onAbrirSessao: (String) -> Unit,
    onApagarSessao: (String) -> Unit,
    onRenomearSessao: (String, String) -> Unit,
) {
    val tokens = LocalLkTokens.current

    ModalDrawerSheet(
        modifier = Modifier.semantics { paneTitle = "Histórico de conversas" },
    ) {
        DrawerCabecalho(onNovaSessao = onNovaSessao)
        HorizontalDivider(color = tokens.border)

        val sessoesOrdenadas =
            remember(sessoes) {
                sessoes.sortedByDescending { it.atualizadoEmEpochMs }
            }

        if (sessoesOrdenadas.isEmpty()) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(LkSpacing.xl),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Nenhuma conversa anterior.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = tokens.textTertiary,
                    textAlign = TextAlign.Center,
                )
            }
        } else {
            LazyColumn {
                items(
                    items = sessoesOrdenadas,
                    key = { it.id },
                ) { sessao ->
                    SessaoListItem(
                        sessao = sessao,
                        isSelected = sessao.id == sessaoAtualId,
                        onAbrir = { onAbrirSessao(sessao.id) },
                        onApagar = { onApagarSessao(sessao.id) },
                        onRenomear = { novoTitulo -> onRenomearSessao(sessao.id, novoTitulo) },
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DrawerCabecalho
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DrawerCabecalho(onNovaSessao: () -> Unit) {
    val tokens = LocalLkTokens.current
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.xl),
    ) {
        Text(
            text = "Conversas",
            style = MaterialTheme.typography.headlineSmall,
            color = tokens.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.md))
        OutlinedButton(
            onClick = onNovaSessao,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(48.dp),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Icon(
                imageVector = Icons.Outlined.Add,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Text(
                text = "Nova conversa",
                style = MaterialTheme.typography.labelLarge,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SessaoListItem — item do drawer com long-press → DropdownMenu
// ─────────────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun SessaoListItem(
    sessao: SessaoChatDiagnostico,
    isSelected: Boolean,
    onAbrir: () -> Unit,
    onApagar: () -> Unit,
    onRenomear: (String) -> Unit,
) {
    val tokens = LocalLkTokens.current
    var showDropdown by remember { mutableStateOf(false) }
    var showDialogApagar by remember { mutableStateOf(false) }
    var showDialogRenomear by remember { mutableStateOf(false) }

    val dataFormatada =
        remember(sessao.atualizadoEmEpochMs) {
            formatarDataRelativa(sessao.atualizadoEmEpochMs)
        }

    Box(modifier = Modifier.padding(horizontal = LkSpacing.sm)) {
        NavigationDrawerItem(
            selected = isSelected,
            onClick = onAbrir,
            label = {
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        text = sessao.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = dataFormatada,
                        style = MaterialTheme.typography.labelMedium,
                        color = tokens.textTertiary,
                    )
                }
            },
            modifier =
                Modifier
                    .semantics {
                        contentDescription = "${sessao.titulo}, $dataFormatada"
                    }.combinedClickable(
                        onClick = onAbrir,
                        onLongClick = { showDropdown = true },
                    ),
        )

        DropdownMenu(
            expanded = showDropdown,
            onDismissRequest = { showDropdown = false },
        ) {
            DropdownMenuItem(
                text = { Text("Renomear") },
                onClick = {
                    showDropdown = false
                    showDialogRenomear = true
                },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Renomear conversa ${sessao.titulo}"
                    },
            )
            DropdownMenuItem(
                text = { Text("Apagar") },
                onClick = {
                    showDropdown = false
                    showDialogApagar = true
                },
                modifier =
                    Modifier.semantics {
                        contentDescription = "Apagar conversa ${sessao.titulo}"
                    },
            )
        }
    }

    if (showDialogApagar) {
        DialogConfirmarApagar(
            onConfirmar = {
                showDialogApagar = false
                onApagar()
            },
            onCancelar = { showDialogApagar = false },
        )
    }

    if (showDialogRenomear) {
        DialogRenomearSessao(
            tituloAtual = sessao.titulo,
            onSalvar = { novoTitulo ->
                showDialogRenomear = false
                onRenomear(novoTitulo)
            },
            onCancelar = { showDialogRenomear = false },
        )
    }
}

private fun formatarDataRelativa(epochMs: Long): String {
    val agora = Calendar.getInstance()
    val data = Calendar.getInstance().apply { timeInMillis = epochMs }
    val localePtBr = Locale.forLanguageTag("pt-BR")

    return when {
        agora.get(Calendar.DAY_OF_YEAR) == data.get(Calendar.DAY_OF_YEAR) &&
            agora.get(Calendar.YEAR) == data.get(Calendar.YEAR) -> {
            val horaFmt = SimpleDateFormat("HH:mm", localePtBr)
            "Hoje, ${horaFmt.format(Date(epochMs))}"
        }
        agora.get(Calendar.DAY_OF_YEAR) - data.get(Calendar.DAY_OF_YEAR) == 1 &&
            agora.get(Calendar.YEAR) == data.get(Calendar.YEAR) -> "Ontem"
        agora.get(Calendar.YEAR) == data.get(Calendar.YEAR) -> {
            SimpleDateFormat("dd/MM", localePtBr).format(Date(epochMs))
        }
        else -> SimpleDateFormat("dd/MM/yy", localePtBr).format(Date(epochMs))
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// DialogConfirmarApagar
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DialogConfirmarApagar(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Apagar conversa?") },
        text = { Text("Esta ação não pode ser desfeita.") },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text(
                    text = "Apagar",
                    color = MaterialTheme.colorScheme.error,
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        },
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// DialogRenomearSessao
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DialogRenomearSessao(
    tituloAtual: String,
    onSalvar: (String) -> Unit,
    onCancelar: () -> Unit,
) {
    var novoTitulo by rememberSaveable { mutableStateOf(tituloAtual) }

    AlertDialog(
        onDismissRequest = onCancelar,
        title = { Text("Renomear conversa") },
        text = {
            OutlinedTextField(
                value = novoTitulo,
                onValueChange = { if (it.length <= 60) novoTitulo = it },
                label = { Text("Nome da conversa") },
                singleLine = true,
                supportingText = { Text("${novoTitulo.length}/60") },
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onSalvar(novoTitulo.trim()) },
                enabled = novoTitulo.isNotBlank(),
            ) {
                Text("Salvar")
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) {
                Text("Cancelar")
            }
        },
    )
}
