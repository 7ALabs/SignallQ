package io.veloo.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Send
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.veloo.app.feature.diagnostico.chat.ChatMensagem
import io.veloo.app.feature.diagnostico.chat.PapelChatMensagem
import io.veloo.app.feature.diagnostico.chat.StatusChatMensagem
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LkSpacing
import io.veloo.app.ui.LocalLkTokens
import io.veloo.app.ui.SignallQTheme
import io.veloo.app.ui.component.LLMAssistantMessage
import io.veloo.app.ui.component.SignallQUserMessageBubble
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LLMChatScreen(
    mensagens: List<ChatMensagem>,
    draft: String,
    isStreaming: Boolean,
    chips: List<String>,
    onEnviarMensagem: (String) -> Unit,
    onAtualizarDraft: (String) -> Unit,
    onSelecionarChip: (String) -> Unit,
    onNovaSessao: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = c.bgPrimary,
        contentWindowInsets = WindowInsets.systemBars,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "SignallQ",
                            style = MaterialTheme.typography.titleLarge,
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                        ) {
                            Box(
                                modifier =
                                    Modifier
                                        .size(6.dp)
                                        .background(
                                            color = LkColors.success,
                                            shape = RoundedCornerShape(999.dp),
                                        ),
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Assistente de conexão",
                                style =
                                    MaterialTheme.typography.labelSmall.copy(
                                        fontSize = 11.sp,
                                        color = c.textSecondary,
                                    ),
                            )
                        }
                    }
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
                    IconButton(onClick = onNovaSessao) {
                        Icon(
                            imageVector = Icons.Outlined.Edit,
                            contentDescription = "Nova sessão",
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
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .windowInsetsPadding(WindowInsets.ime),
        ) {
            LLMChatMensagens(
                mensagens = mensagens,
                isStreaming = isStreaming,
                modifier = Modifier.weight(1f),
            )

            if (chips.isNotEmpty() && !isStreaming) {
                LLMChatChips(
                    chips = chips,
                    onSelecionarChip = onSelecionarChip,
                    borderColor = c.border,
                )
            }

            LLMChatInput(
                draft = draft,
                isStreaming = isStreaming,
                onAtualizarDraft = onAtualizarDraft,
                onEnviarMensagem = onEnviarMensagem,
                borderColor = c.border,
            )
        }
    }
}

@Composable
private fun LLMChatMensagens(
    mensagens: List<ChatMensagem>,
    isStreaming: Boolean,
    modifier: Modifier = Modifier,
) {
    val lazyListState = rememberLazyListState()

    val isScrolledToEnd by remember {
        derivedStateOf {
            val layoutInfo = lazyListState.layoutInfo
            val totalItems = layoutInfo.totalItemsCount
            if (totalItems == 0) return@derivedStateOf true
            val lastVisible = layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0
            lastVisible >= totalItems - 1
        }
    }

    LaunchedEffect(mensagens.size, isStreaming) {
        if (isScrolledToEnd && mensagens.isNotEmpty()) {
            lazyListState.animateScrollToItem(mensagens.size - 1)
        }
    }

    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxWidth(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        items(
            items = mensagens,
            key = { it.id },
        ) { mensagem ->
            val isUltima = mensagem == mensagens.lastOrNull()
            when (mensagem.papel) {
                PapelChatMensagem.usuario -> {
                    SignallQUserMessageBubble(text = mensagem.conteudo)
                }
                PapelChatMensagem.assistente, PapelChatMensagem.sistema -> {
                    LLMAssistantMessage(
                        content = mensagem.conteudo,
                        isStreaming = isUltima && isStreaming,
                    )
                }
            }
        }
    }
}

@Composable
private fun LLMChatChips(
    chips: List<String>,
    onSelecionarChip: (String) -> Unit,
    borderColor: Color,
) {
    val c = LocalLkTokens.current

    HorizontalDivider(color = borderColor)

    LazyRow(
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        items(chips) { chip ->
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(999.dp))
                        .background(c.bgSecondary)
                        .border(1.dp, borderColor, RoundedCornerShape(999.dp))
                        .clickable { onSelecionarChip(chip) }
                        .padding(horizontal = 12.dp, vertical = 7.dp),
            ) {
                Text(
                    text = chip,
                    style =
                        MaterialTheme.typography.labelMedium.copy(
                            fontSize = 12.sp,
                            color = c.textSecondary,
                        ),
                )
            }
        }
    }
}

@Composable
private fun LLMChatInput(
    draft: String,
    isStreaming: Boolean,
    onAtualizarDraft: (String) -> Unit,
    onEnviarMensagem: (String) -> Unit,
    borderColor: Color,
) {
    val c = LocalLkTokens.current
    val sendEnabled = draft.isNotBlank() && !isStreaming

    Column {
        HorizontalDivider(color = borderColor)

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(24.dp))
                        .background(c.bgSecondary)
                        .padding(start = 16.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(modifier = Modifier.weight(1f)) {
                    if (draft.isEmpty()) {
                        Text(
                            text = "Pergunte qualquer coisa…",
                            style =
                                TextStyle(
                                    fontSize = 14.sp,
                                    color = c.textTertiary,
                                ),
                        )
                    }
                    BasicTextField(
                        value = draft,
                        onValueChange = onAtualizarDraft,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle =
                            TextStyle(
                                fontSize = 14.sp,
                                color = c.textPrimary,
                            ),
                        cursorBrush = SolidColor(LkColors.accent),
                        maxLines = 6,
                    )
                }

                Spacer(modifier = Modifier.width(LkSpacing.sm))

                Box(
                    modifier =
                        Modifier
                            .size(38.dp)
                            .clip(CircleShape)
                            .background(
                                if (sendEnabled) LkColors.accent else LkColors.accent.copy(alpha = 0.4f),
                            ).clickable(enabled = sendEnabled) {
                                val text = draft.trim()
                                if (text.isNotBlank()) {
                                    onEnviarMensagem(text)
                                }
                            },
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Enviar",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Text(
                text = "O SignallQ roda no aparelho e pode errar. Confira dados importantes.",
                style =
                    MaterialTheme.typography.labelSmall.copy(
                        fontSize = 10.5.sp,
                        color = c.textTertiary,
                    ),
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "LLMChatScreen - mensagens")
@Composable
private fun LLMChatScreenPreview() {
    SignallQTheme {
        val mensagens =
            listOf(
                ChatMensagem(
                    id = UUID.randomUUID().toString(),
                    sessionId = "s1",
                    papel = PapelChatMensagem.assistente,
                    conteudo = "Olá! Como posso ajudar com sua conexão?",
                    criadoEmEpochMs = System.currentTimeMillis(),
                    status = StatusChatMensagem.concluido,
                ),
                ChatMensagem(
                    id = UUID.randomUUID().toString(),
                    sessionId = "s1",
                    papel = PapelChatMensagem.usuario,
                    conteudo = "Minha internet fica lenta toda noite. O que pode ser?",
                    criadoEmEpochMs = System.currentTimeMillis(),
                    status = StatusChatMensagem.concluido,
                ),
                ChatMensagem(
                    id = UUID.randomUUID().toString(),
                    sessionId = "s1",
                    papel = PapelChatMensagem.assistente,
                    conteudo =
                        "Sua internet pode ficar lenta à noite por alguns motivos comuns:\n\n" +
                            "1. Horário de pico — muitos vizinhos usando a rede ao mesmo tempo.\n" +
                            "2. Wi-Fi 2.4 GHz cheio — banda congestionada no período noturno.\n" +
                            "3. Atualizações em segundo plano — apps baixando enquanto você usa.\n\n" +
                            "Quer rodar um teste rápido para confirmar?",
                    criadoEmEpochMs = System.currentTimeMillis(),
                    status = StatusChatMensagem.concluido,
                ),
            )

        LLMChatScreen(
            mensagens = mensagens,
            draft = "",
            isStreaming = false,
            chips = listOf("Como troco o canal do Wi-Fi?", "Vale a pena 5 GHz?"),
            onEnviarMensagem = {},
            onAtualizarDraft = {},
            onSelecionarChip = {},
            onNovaSessao = {},
            onVoltar = {},
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF, name = "LLMChatScreen - streaming")
@Composable
private fun LLMChatScreenStreamingPreview() {
    SignallQTheme {
        val mensagens =
            listOf(
                ChatMensagem(
                    id = UUID.randomUUID().toString(),
                    sessionId = "s1",
                    papel = PapelChatMensagem.usuario,
                    conteudo = "Por que minha internet fica lenta?",
                    criadoEmEpochMs = System.currentTimeMillis(),
                    status = StatusChatMensagem.concluido,
                ),
                ChatMensagem(
                    id = UUID.randomUUID().toString(),
                    sessionId = "s1",
                    papel = PapelChatMensagem.assistente,
                    conteudo = "Analisando sua pergunta",
                    criadoEmEpochMs = System.currentTimeMillis(),
                    status = StatusChatMensagem.streaming,
                ),
            )

        LLMChatScreen(
            mensagens = mensagens,
            draft = "",
            isStreaming = true,
            chips = emptyList(),
            onEnviarMensagem = {},
            onAtualizarDraft = {},
            onSelecionarChip = {},
            onNovaSessao = {},
            onVoltar = {},
        )
    }
}
