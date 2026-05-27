package io.linka.app.kotlin.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.StartOffset
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NetworkWifi
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.R
import io.linka.app.kotlin.feature.diagnostico.ConnectionType
import io.linka.app.kotlin.feature.diagnostico.DiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.EstadoDiagnostico
import io.linka.app.kotlin.feature.diagnostico.SnapshotDiagnostico
import io.linka.app.kotlin.feature.diagnostico.ai.AiAcaoRecomendada
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisRepository
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisResult
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisState
import io.linka.app.kotlin.feature.diagnostico.ai.AiFallbackFactory
import io.linka.app.kotlin.feature.diagnostico.ai.DiagChatAutor
import io.linka.app.kotlin.feature.diagnostico.ai.DiagChatEntry
import io.linka.app.kotlin.feature.diagnostico.ai.DiagnosisAiContextFactory
import io.linka.app.kotlin.feature.diagnostico.ai.normalizeClassificacaoLabel
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.state.UiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AI_BASE_URL = "https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev"

/**
 * Fase de carregamento do DiagnosticoScreen.
 * Distingue loading das engines de rede do loading da analise de IA.
 */
enum class DiagnosticoFase { Engines, Ia }

/**
 * Dado de UI do DiagnosticoScreen.
 *
 * [Carregando] representa qualquer fase intermediaria — engines ou IA.
 * [Resultado] representa o estado final com o laudo da IA.
 *
 * Mapeamento para UiState<DiagnosticoUiData>:
 *   - UiState.Empty         → tela inicial (Idle), nenhuma analise solicitada
 *   - UiState.Success(Carregando(Engines)) → engines de rede em execucao
 *   - UiState.Success(Carregando(Ia))      → IA consultando resultado
 *   - UiState.Success(Resultado(...))      → laudo disponivel
 *   - UiState.Error(message)               → falha no diagnostico
 */
sealed interface DiagnosticoUiData {
    data class Carregando(
        val fase: DiagnosticoFase,
    ) : DiagnosticoUiData

    data class Resultado(
        val result: AiDiagnosisResult,
        val isFallback: Boolean,
    ) : DiagnosticoUiData
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DiagnosticoScreen(
    snapshotDiagnostico: SnapshotDiagnostico,
    onAbrirRedes: () -> Unit,
    onIniciarDiagnostico: () -> Unit,
    analiseSolicitada: Boolean,
    onAnaliseSolicitadaChange: (Boolean) -> Unit,
    aiState: AiDiagnosisState,
    onAiStateChange: (AiDiagnosisState) -> Unit,
    onVoltar: () -> Unit,
    chatHistorico: List<DiagChatEntry> = emptyList(),
    chatCarregando: Boolean = false,
    onEnviarChat: (String) -> Unit = {},
) {
    val c = LocalLkTokens.current
    val scope = rememberCoroutineScope()

    val aiRepository =
        remember {
            AiDiagnosisRepository(baseUrl = AI_BASE_URL, isAuthorized = { true })
        }

    LaunchedEffect(snapshotDiagnostico.estado, analiseSolicitada) {
        if (!analiseSolicitada) return@LaunchedEffect
        if (aiState is AiDiagnosisState.loading || aiState is AiDiagnosisState.success) return@LaunchedEffect
        val relatorio = snapshotDiagnostico.relatorio ?: return@LaunchedEffect
        if (snapshotDiagnostico.estado != EstadoDiagnostico.concluido) return@LaunchedEffect

        onAiStateChange(AiDiagnosisState.loading)
        val connectionType = snapshotDiagnostico.input?.connectionType ?: ConnectionType.desconhecido
        val ctx = DiagnosisAiContextFactory.from(relatorio, snapshotDiagnostico.input, connectionType)
        onAiStateChange(aiRepository.explainDiagnosis(ctx) { AiFallbackFactory.fromLocal(relatorio) })
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Speed,
                            contentDescription = null,
                            tint = c.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            stringResource(R.string.diagnostico_titulo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        val uiState = resolveUiState(snapshotDiagnostico, aiState, analiseSolicitada)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when (uiState) {
                UiState.Empty -> {
                    // Estado 1 — sem dados: nunca houve relatorio gerado (relatorio null + estado idle)
                    val semDados = snapshotDiagnostico.relatorio == null &&
                        snapshotDiagnostico.estado == EstadoDiagnostico.idle

                    // TODO #141: aguardando campo `telephonyPermissionGranted: Boolean` no SnapshotDiagnostico
                    // val semPermissao = !snapshotDiagnostico.telephonyPermissionGranted

                    // TODO #141: aguardando campo `timestampUltimoTeste: Long?` no SnapshotDiagnostico
                    // val dadosAntigos = snapshotDiagnostico.timestampUltimoTeste?.let {
                    //     (System.currentTimeMillis() - it) > 24 * 60 * 60 * 1000L
                    // } ?: false

                    DiagnosticoIdleContent(
                        c = c,
                        semDados = semDados,
                        // semPermissao = semPermissao,  // TODO #141
                        // dadosAntigos = dadosAntigos,  // TODO #141
                        onAnalisar = {
                            onAnaliseSolicitadaChange(true)
                            onAiStateChange(AiDiagnosisState.idle)
                            onIniciarDiagnostico()
                        },
                        onPermitirAnalise = {
                            // TODO #141: abrir sheet de permissão de telefonia quando campo disponível
                        },
                        onAtualizarDiagnostico = {
                            onAnaliseSolicitadaChange(true)
                            onAiStateChange(AiDiagnosisState.idle)
                            onIniciarDiagnostico()
                        },
                    )
                }

                UiState.Loading -> {
                    // Nao utilizado — loading com fase usa Success<Carregando>
                }

                is UiState.Success -> {
                    when (val data = uiState.data) {
                        is DiagnosticoUiData.Carregando ->
                            DiagnosticoLoadingContent(
                                c = c,
                                isAiPhase = data.fase == DiagnosticoFase.Ia,
                            )

                        is DiagnosticoUiData.Resultado ->
                            DiagnosticoResultadoContent(
                                c = c,
                                result = data.result,
                                isFallback = data.isFallback,
                                input = snapshotDiagnostico.input,
                                onReanalisar = {
                                    scope.launch {
                                        val relatorio = snapshotDiagnostico.relatorio ?: return@launch
                                        onAiStateChange(AiDiagnosisState.loading)
                                        val connectionType =
                                            snapshotDiagnostico.input?.connectionType ?: ConnectionType.desconhecido
                                        val ctx =
                                            DiagnosisAiContextFactory.from(
                                                relatorio,
                                                snapshotDiagnostico.input,
                                                connectionType,
                                            )
                                        onAiStateChange(
                                            aiRepository.explainDiagnosis(ctx) { AiFallbackFactory.fromLocal(relatorio) },
                                        )
                                    }
                                },
                                onAbrirRedes = onAbrirRedes,
                                chatHistorico = chatHistorico,
                                chatCarregando = chatCarregando,
                                onEnviarChat = onEnviarChat,
                            )
                    }
                }

                is UiState.Error -> {
                    val isAiError = uiState.message != "Não foi possível diagnosticar a conexão."
                    if (isAiError) {
                        val codigoAmigavel =
                            when {
                                uiState.message.contains("timeout", ignoreCase = true) -> "ERR_TIMEOUT"
                                uiState.message.contains("503") || uiState.message.contains("504") -> "ERR_SERVIDOR_INDISPONIVEL"
                                uiState.message.contains("sem_relatorio") -> "ERR_SEM_DADOS"
                                else -> uiState.message.take(40)
                            }
                        var mostrarDetalhes by remember { mutableStateOf(false) }
                        AlertDialog(
                            onDismissRequest = { onAiStateChange(AiDiagnosisState.idle) },
                            title = { Text("Falha na conexão com a IA") },
                            text = {
                                Column {
                                    Text("Não foi possível conectar ao servidor de análise. Verifique sua conexão e tente novamente.")
                                    if (mostrarDetalhes) {
                                        Spacer(Modifier.height(LkSpacing.sm))
                                        Text(
                                            "Código: $codigoAmigavel",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = c.textTertiary,
                                        )
                                    }
                                }
                            },
                            confirmButton = {
                                Button(
                                    onClick = {
                                        scope.launch {
                                            val relatorio = snapshotDiagnostico.relatorio ?: return@launch
                                            onAiStateChange(AiDiagnosisState.loading)
                                            val connectionType = snapshotDiagnostico.input?.connectionType ?: ConnectionType.desconhecido
                                            val ctx = DiagnosisAiContextFactory.from(relatorio, snapshotDiagnostico.input, connectionType)
                                            onAiStateChange(aiRepository.explainDiagnosis(ctx) { AiFallbackFactory.fromLocal(relatorio) })
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                                ) { Text("Tentar novamente") }
                            },
                            dismissButton = {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    TextButton(onClick = {
                                        onAnaliseSolicitadaChange(false)
                                        onAiStateChange(AiDiagnosisState.idle)
                                    }) { Text("Sair") }
                                    TextButton(onClick = { mostrarDetalhes = !mostrarDetalhes }) {
                                        Text(
                                            if (mostrarDetalhes) "Ocultar detalhes" else "Ver detalhes",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = c.textTertiary,
                                        )
                                    }
                                }
                            },
                        )
                    } else {
                        DiagnosticoErroContent(
                            c = c,
                            onTentar = {
                                onAnaliseSolicitadaChange(false)
                                onAiStateChange(AiDiagnosisState.idle)
                            },
                        )
                    }
                }
            }
        }
    }
}

// ─── State resolution ─────────────────────────────────────────────────────────

private fun resolveUiState(
    snap: SnapshotDiagnostico,
    ai: AiDiagnosisState,
    solicitada: Boolean,
): UiState<DiagnosticoUiData> {
    if (!solicitada) return UiState.Empty
    if (snap.estado == EstadoDiagnostico.erro) return UiState.Error("Não foi possível diagnosticar a conexão.")
    if (ai is AiDiagnosisState.success) {
        return UiState.Success(DiagnosticoUiData.Resultado(ai.result, isFallback = false))
    }
    if (ai is AiDiagnosisState.fallback || ai is AiDiagnosisState.error) {
        val code = if (ai is AiDiagnosisState.error) ai.code else "ERR_SERVIDOR_INDISPONIVEL"
        return UiState.Error(code)
    }
    if (ai is AiDiagnosisState.loading) {
        return UiState.Success(DiagnosticoUiData.Carregando(DiagnosticoFase.Ia))
    }
    return UiState.Success(DiagnosticoUiData.Carregando(DiagnosticoFase.Engines))
}

// ─── Estados da tela ──────────────────────────────────────────────────────────

@Composable
private fun DiagnosticoIdleContent(
    c: LkTokens,
    onAnalisar: () -> Unit,
    semDados: Boolean = false,
    semPermissao: Boolean = false, // TODO #141: aguardando campo telephonyPermissionGranted no SnapshotDiagnostico
    dadosAntigos: Boolean = false, // TODO #141: aguardando campo timestampUltimoTeste no SnapshotDiagnostico
    onPermitirAnalise: () -> Unit = {},
    onAtualizarDiagnostico: () -> Unit = {},
) {
    val gradient =
        remember {
            Brush.linearGradient(colors = listOf(LkColors.accent, LkColors.accentBlue))
        }
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
    ) {
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(gradient)
                    .padding(LkSpacing.xxl),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(LkColors.linkaTextOnDark.copy(alpha = 0.18f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Analytics,
                        contentDescription = null,
                        tint = LkColors.linkaTextOnDark,
                        modifier = Modifier.size(40.dp),
                    )
                }
                Spacer(Modifier.height(LkSpacing.xs))
                Text(
                    stringResource(R.string.diagnostico_idle_titulo),
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.W800,
                    color = LkColors.linkaTextOnDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                )
                Text(
                    stringResource(R.string.diagnostico_idle_descricao),
                    style = MaterialTheme.typography.bodyMedium,
                    color = LkColors.linkaTextOnDark.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
        }

        // Estado 1 — sem dados suficientes
        if (semDados) {
            DiagnosticoEstadoEspecialBanner(
                c = c,
                icone = Icons.Outlined.Info,
                iconeTint = LkColors.accent,
                mensagem = stringResource(R.string.diagnostico_sem_dados_msg),
                textoBotao = stringResource(R.string.diagnostico_sem_dados_btn),
                onBotao = onAnalisar,
            )
        }

        // Estado 2 — sem permissão de telefonia
        // TODO #141: habilitar quando campo telephonyPermissionGranted estiver em SnapshotDiagnostico
        if (semPermissao) {
            DiagnosticoEstadoEspecialBanner(
                c = c,
                icone = Icons.Outlined.Security,
                iconeTint = LkColors.warning,
                mensagem = stringResource(R.string.diagnostico_sem_permissao_msg),
                textoBotao = stringResource(R.string.diagnostico_sem_permissao_btn),
                onBotao = onPermitirAnalise,
            )
        }

        // Estado 3 — dados antigos (último teste > 24h)
        // TODO #141: habilitar quando campo timestampUltimoTeste estiver em SnapshotDiagnostico
        if (dadosAntigos) {
            DiagnosticoEstadoEspecialBanner(
                c = c,
                icone = Icons.Outlined.Refresh,
                iconeTint = c.textSecondary,
                mensagem = stringResource(R.string.diagnostico_dados_antigos_msg),
                textoBotao = stringResource(R.string.diagnostico_dados_antigos_btn),
                onBotao = onAtualizarDiagnostico,
            )
        }

        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.bgCard)
                    .padding(LkSpacing.lg),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Text(
                stringResource(R.string.diagnostico_idle_secao_analisa),
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W700,
                color = c.textTertiary,
                letterSpacing = 0.8.sp,
            )
            HorizontalDivider(color = c.border)
            DiagnosticoFeatureItem(stringResource(R.string.diagnostico_feature_1), c)
            DiagnosticoFeatureItem(stringResource(R.string.diagnostico_feature_2), c)
            DiagnosticoFeatureItem(stringResource(R.string.diagnostico_feature_3), c)
            DiagnosticoFeatureItem(stringResource(R.string.diagnostico_feature_4), c)
        }

        Spacer(Modifier.height(LkSpacing.sm))

        Button(
            onClick = onAnalisar,
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(52.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Icon(
                imageVector = Icons.Outlined.Analytics,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Text(
                stringResource(R.string.diagnostico_btn_iniciar),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
            )
        }

        Spacer(Modifier.height(LkSpacing.lg))
    }
}

/**
 * Banner de estado especial exibido no DiagnosticoIdleContent.
 * Usado para "sem dados", "sem permissão" e "dados antigos" (#141).
 */
@Composable
private fun DiagnosticoEstadoEspecialBanner(
    c: LkTokens,
    icone: ImageVector,
    iconeTint: Color,
    mensagem: String,
    textoBotao: String,
    onBotao: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            tint = iconeTint,
            modifier = Modifier
                .size(20.dp)
                .padding(top = 2.dp),
        )
        Column(
            verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = mensagem,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                lineHeight = 20.sp,
            )
            TextButton(
                onClick = onBotao,
                contentPadding = PaddingValues(0.dp),
            ) {
                Text(
                    text = textoBotao,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.W600,
                    color = LkColors.accent,
                )
            }
        }
    }
}

@Composable
private fun DiagnosticoFeatureItem(
    text: String,
    c: LkTokens,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Icon(
            imageVector = Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = LkColors.accent,
            modifier = Modifier.size(18.dp),
        )
        Text(
            text,
            style = MaterialTheme.typography.titleSmall,
            color = c.textPrimary,
            lineHeight = 19.sp,
            modifier = Modifier.weight(1f),
        )
    }
}

// ENGINE_STEPS agora são resource IDs — resolvidos via stringResource() em DiagnosticoLoadingContent
private val ENGINE_STEP_RES_IDS =
    listOf(
        R.string.diagnostico_step_1,
        R.string.diagnostico_step_2,
        R.string.diagnostico_step_3,
    )

private enum class StepEstado { Pendente, Ativo, Concluido }

@Composable
private fun DiagnosticoLoadingContent(
    c: LkTokens,
    isAiPhase: Boolean,
) {
    val engineSteps = ENGINE_STEP_RES_IDS.map { stringResource(it) }

    var stepsVisiveis by remember(isAiPhase) {
        mutableIntStateOf(if (isAiPhase) engineSteps.size else 0)
    }

    LaunchedEffect(isAiPhase) {
        if (!isAiPhase) {
            engineSteps.indices.forEach { i ->
                delay(if (i == 0) 500L else 750L)
                stepsVisiveis = i + 1
            }
        }
    }

    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = LkSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.Start,
            verticalArrangement = Arrangement.spacedBy(0.dp),
        ) {
            Text(
                if (isAiPhase) stringResource(R.string.diagnostico_loading_ia) else stringResource(R.string.diagnostico_loading_engines),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.xl))

            engineSteps.forEachIndexed { index, texto ->
                AnimatedVisibility(
                    visible = index < stepsVisiveis,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                ) {
                    StepRow(c = c, texto = texto, estado = StepEstado.Concluido)
                }
            }

            AnimatedVisibility(
                visible = stepsVisiveis == engineSteps.size,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            ) {
                StepRow(
                    c = c,
                    texto = stringResource(R.string.diagnostico_step_ia),
                    estado = if (isAiPhase) StepEstado.Ativo else StepEstado.Pendente,
                )
            }
        }
    }
}

@Composable
private fun StepRow(
    c: LkTokens,
    texto: String,
    estado: StepEstado,
) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(900), repeatMode = RepeatMode.Reverse),
        label = "pulse-alpha",
    )

    val estadoDescricao =
        when (estado) {
            StepEstado.Concluido -> "concluído"
            StepEstado.Ativo -> "em andamento"
            StepEstado.Pendente -> "pendente"
        }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = LkSpacing.sm)
                .semantics(mergeDescendants = true) {
                    contentDescription = "$texto — $estadoDescricao"
                    stateDescription = estadoDescricao
                },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        when (estado) {
            StepEstado.Concluido ->
                Icon(
                    imageVector = Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = LkColors.success,
                    modifier = Modifier.size(20.dp),
                )
            StepEstado.Ativo ->
                CircularProgressIndicator(
                    color = LkColors.accent,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(20.dp),
                )
            StepEstado.Pendente ->
                Icon(
                    imageVector = Icons.Outlined.RadioButtonUnchecked,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(20.dp),
                )
        }
        Text(
            texto,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = if (estado == StepEstado.Ativo) FontWeight.W600 else FontWeight.W400,
            color =
                when (estado) {
                    StepEstado.Concluido -> c.textSecondary
                    StepEstado.Ativo -> c.textPrimary.copy(alpha = pulse)
                    StepEstado.Pendente -> c.textTertiary
                },
        )
    }
}

@Composable
private fun DiagnosticoErroContent(
    c: LkTokens,
    onTentar: () -> Unit,
) {
    Box(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = LkSpacing.xl),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = LkColors.error,
                modifier = Modifier.size(48.dp),
            )
            Text(
                stringResource(R.string.diagnostico_erro_titulo),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                stringResource(R.string.diagnostico_erro_descricao),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            OutlinedButton(onClick = onTentar) {
                Text(stringResource(R.string.global_btn_tentar_novamente))
            }
        }
    }
}

// ─── Tela de resultado — redesign ────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticoResultadoContent(
    c: LkTokens,
    result: AiDiagnosisResult,
    isFallback: Boolean,
    input: DiagnosticInput?,
    onReanalisar: () -> Unit,
    onAbrirRedes: () -> Unit,
    chatHistorico: List<DiagChatEntry> = emptyList(),
    chatCarregando: Boolean = false,
    onEnviarChat: (String) -> Unit = {},
) {
    var chatInput by remember { mutableStateOf("") }
    val chipsSomidos = chatHistorico.isNotEmpty()
    val limiteAtingido = chatHistorico.count { it.autor == DiagChatAutor.Usuario } >= 5
    val listState = rememberLazyListState()

    LaunchedEffect(chatHistorico.size, chatCarregando) {
        val totalItems = listState.layoutInfo.totalItemsCount
        if (totalItems > 0) listState.animateScrollToItem(totalItems - 1)
    }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .windowInsetsPadding(WindowInsets.ime),
    ) {
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding =
                PaddingValues(
                    start = LkSpacing.lg,
                    end = LkSpacing.lg,
                    top = LkSpacing.md,
                    bottom = LkSpacing.lg,
                ),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
        ) {
            // Card 1 — StatusDiagnosticoCard
            item {
                StaggeredCard(index = 0) {
                    StatusDiagnosticoCard(c = c, result = result)
                }
            }

            // Card 2 — PrincipalPontoCard (só se houver problema principal com descrição)
            if (result.problemaPrincipal.descricao.isNotBlank()) {
                item {
                    StaggeredCard(index = 1) {
                        PrincipalPontoCard(
                            c = c,
                            result = result,
                            onAbrirRedes = onAbrirRedes,
                        )
                    }
                }
            }

            // Card 3 — OQueFazerCard (só se houver ações)
            if (result.acoesRecomendadas.isNotEmpty()) {
                item {
                    StaggeredCard(index = 2) {
                        OQueFazerCard(
                            c = c,
                            actions = result.acoesRecomendadas,
                            onAbrirRedes = onAbrirRedes,
                            onReanalisar = onReanalisar,
                        )
                    }
                }
            }

            // Seção duas colunas — Evidências + Análise por categoria
            val temEvidencias = result.evidencias.isNotEmpty()
            val classificacao = result.classificacaoTecnica
            val temClassificacao =
                listOfNotNull(
                    classificacao.velocidade,
                    classificacao.estabilidade,
                    classificacao.wifi,
                    classificacao.dns,
                    classificacao.fibra,
                ).any { it.avaliacao?.isNotBlank() == true }

            if (temEvidencias || temClassificacao) {
                item {
                    StaggeredCard(index = 3) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                        ) {
                            if (temEvidencias) {
                                EvidenciasColuna(
                                    c = c,
                                    evidencias = result.evidencias,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            if (temClassificacao) {
                                AnaliseCategoriasColuna(
                                    c = c,
                                    classificacao = classificacao,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                        }
                    }
                }
            }

            // Separador visual antes do chat — distingue diagnóstico local de respostas IA
            if (chatHistorico.isNotEmpty() || chatCarregando) {
                item {
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = LkSpacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = c.border)
                        Icon(
                            imageVector = Icons.Outlined.AutoAwesome,
                            contentDescription = null,
                            tint = LkColors.accent,
                            modifier = Modifier.size(14.dp),
                        )
                        Text(
                            text = "Assistente IA",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                        )
                        HorizontalDivider(modifier = Modifier.weight(1f), color = c.border)
                    }
                }
            }

            // Mensagens do chat inline
            items(chatHistorico, key = { it.timestamp }) { entry ->
                DiagChatMensagem(
                    entry = entry,
                    c = c,
                    onRetry = {
                        val ultimaPergunta =
                            chatHistorico
                                .lastOrNull {
                                    it.autor == DiagChatAutor.Usuario
                                }?.texto ?: ""
                        if (ultimaPergunta.isNotBlank()) onEnviarChat(ultimaPergunta)
                    },
                )
            }

            if (chatCarregando) {
                item { DiagChatLoadingItem(c = c) }
            }

            // Card Final — ChatCard
            item {
                ChatCard(
                    c = c,
                    perguntasContextuais = result.perguntasContextuais.map { it.pergunta }.take(3),
                    chatInput = chatInput,
                    onChatInputChange = { chatInput = it },
                    onEnviar = { pergunta ->
                        onEnviarChat(pergunta)
                        chatInput = ""
                    },
                    chipsSomidos = chipsSomidos,
                    limiteAtingido = limiteAtingido,
                )
            }

            // Rodapé: fonte + botão de reanálise
            item {
                val sdf = remember { SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")) }
                val horario = sdf.format(Date(result.generatedAt))
                val fonteLabel = if (isFallback) "Diagnóstico do dispositivo" else result.modeloIa.nomeExibicao.ifBlank { "Linka IA" }
                Column(
                    verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    Text(
                        "$fonteLabel · $horario",
                        style = MaterialTheme.typography.labelMedium,
                        color = c.textTertiary,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                    )
                    OutlinedButton(
                        onClick = onReanalisar,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(LkRadius.button),
                    ) {
                        Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(stringResource(R.string.diagnostico_btn_reanalisar), fontSize = 13.sp)
                    }
                }
            }
        }
    }
}

// ─── Animação escalonada de entrada ──────────────────────────────────────────

@Composable
private fun StaggeredCard(
    index: Int,
    content: @Composable () -> Unit,
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index * 120L)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        enter = fadeIn(tween(300)) + slideInVertically(tween(300)) { it / 5 },
    ) {
        content()
    }
}

// ─── Card 1 — StatusDiagnosticoCard ──────────────────────────────────────────

@Composable
private fun StatusDiagnosticoCard(
    c: LkTokens,
    result: AiDiagnosisResult,
) {
    val statusColor = statusToColor(result.status, c)
    val isAtencao = result.status.lowercase() in setOf("regular", "ruim", "critico")

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Ícone escudo com fundo circular
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(CircleShape)
                        .background(LkColors.accentBlue.copy(alpha = 0.12f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = LkColors.accentBlue,
                    modifier = Modifier.size(28.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Text(
                    result.titulo.ifBlank { "Diagnóstico concluído" },
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                    fontSize = 18.sp,
                )
                Text(
                    result.textoLaudo.ifBlank { result.resumo },
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    lineHeight = 20.sp,
                )
            }
        }

        // Chip de status / atenção
        if (isAtencao) {
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(100.dp))
                        .border(1.dp, c.onWarningContainer.copy(alpha = 0.5f), RoundedCornerShape(100.dp))
                        .background(c.warningContainer)
                        .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(6.dp)
                            .clip(CircleShape)
                            .background(statusColor),
                )
                Text(
                    when (result.status.lowercase()) {
                        "critico" -> "Problema crítico"
                        "ruim" -> "Conexão com problemas"
                        else -> "Atenção no Wi-Fi"
                    },
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.W600,
                    color = c.onWarningContainer,
                )
            }
        }

        MetricsRow(c = c, result = result, statusColor = statusColor)
    }
}

// ─── Card 2 — PrincipalPontoCard ──────────────────────────────────────────────

@Composable
private fun PrincipalPontoCard(
    c: LkTokens,
    result: AiDiagnosisResult,
    onAbrirRedes: () -> Unit,
) {
    val isWifiOuRede =
        result.problemaPrincipal.tipo.lowercase() in
            setOf("wifi", "roteador", "canal", "rede", "isp")
    val tipCard = result.problemaPrincipal.descricao.isNotBlank()

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Text(
            "Principal ponto encontrado",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            fontSize = 16.sp,
        )

        Row(
            verticalAlignment = Alignment.Top,
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(LkColors.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = iconForProblemaTipo(result.problemaPrincipal.tipo),
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier = Modifier.size(18.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Text(
                    result.problemaPrincipal.tipo.replaceFirstChar { it.uppercaseChar() },
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textPrimary,
                    fontSize = 15.sp,
                )
                Text(
                    result.problemaPrincipal.descricao,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                    lineHeight = 18.sp,
                )
            }
        }

        // Tip card âmbar
        if (tipCard) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = c.amberSurface,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier.padding(LkSpacing.md),
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                    verticalAlignment = Alignment.Top,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = c.onWarningContainer,
                        modifier =
                            Modifier
                                .padding(top = 2.dp)
                                .size(14.dp),
                    )
                    Text(
                        result.resumo.ifBlank { result.problemaPrincipal.descricao },
                        style = MaterialTheme.typography.bodySmall,
                        color = c.onWarningContainer,
                        lineHeight = 17.sp,
                    )
                }
            }
        }

        // Link — "Ver dispositivo" para ações de rede/Wi-Fi
        if (isWifiOuRede) {
            Row(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(LkRadius.button))
                        .clickable(onClick = onAbrirRedes)
                        .padding(vertical = LkSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Text(
                    "Ver redes Wi-Fi",
                    style = MaterialTheme.typography.bodySmall,
                    color = LkColors.accent,
                    fontWeight = FontWeight.W600,
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier = Modifier.size(12.dp),
                )
            }
        }
    }
}

// ─── Card 3 — OQueFazerCard ────────────────────────────────────────────────────

@Composable
private fun OQueFazerCard(
    c: LkTokens,
    actions: List<AiAcaoRecomendada>,
    onAbrirRedes: () -> Unit,
    onReanalisar: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Text(
            "O que fazer agora",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            fontSize = 16.sp,
        )

        // Lista de ações com ícone
        actions.take(4).forEachIndexed { index, acao ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .size(24.dp)
                            .clip(CircleShape)
                            .background(LkColors.accent.copy(alpha = 0.10f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = iconForAcaoIndex(index),
                        contentDescription = null,
                        tint = LkColors.accent,
                        modifier = Modifier.size(12.dp),
                    )
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        acao.titulo,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                    if (acao.descricao.isNotBlank()) {
                        Text(
                            acao.descricao,
                            style = MaterialTheme.typography.bodySmall,
                            color = c.textSecondary,
                            lineHeight = 17.sp,
                        )
                    }
                }
            }
        }

        // Botões de ação rápida
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        ) {
            OutlinedButton(
                onClick = onReanalisar,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Reanalisar", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            OutlinedButton(
                onClick = onAbrirRedes,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(Icons.Outlined.NetworkWifi, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Wi-Fi", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
            OutlinedButton(
                onClick = onAbrirRedes,
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(horizontal = LkSpacing.sm, vertical = LkSpacing.xs),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Icon(Icons.Outlined.Router, contentDescription = null, modifier = Modifier.size(12.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Redes", style = MaterialTheme.typography.labelSmall, maxLines = 1)
            }
        }
    }
}

// ─── Seção de duas colunas ─────────────────────────────────────────────────────

@Composable
private fun EvidenciasColuna(
    c: LkTokens,
    evidencias: List<io.linka.app.kotlin.feature.diagnostico.ai.AiEvidenceOut>,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            "Evidências usadas",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            fontSize = 15.sp,
        )
        evidencias.take(5).forEach { ev ->
            Row(
                verticalAlignment = Alignment.Top,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                modifier = Modifier.fillMaxWidth(),
            ) {
                Icon(
                    Icons.Outlined.CheckCircle,
                    contentDescription = null,
                    tint = LkColors.accent.copy(alpha = 0.7f),
                    modifier =
                        Modifier
                            .padding(top = 2.dp)
                            .size(12.dp),
                )
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(
                        ev.label.substringAfter(":").ifBlank { ev.label },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                        lineHeight = 15.sp,
                    )
                    if (ev.valor.isNotBlank()) {
                        Text(
                            ev.valor,
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textSecondary,
                            lineHeight = 14.sp,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun AnaliseCategoriasColuna(
    c: LkTokens,
    classificacao: io.linka.app.kotlin.feature.diagnostico.ai.ClassificacaoTecnica,
    modifier: Modifier = Modifier,
) {
    val categorias =
        buildList {
            classificacao.velocidade?.let { add("Velocidade" to it) }
            classificacao.estabilidade?.let { add("Estabilidade" to it) }
            classificacao.wifi?.let { add("Wi-Fi" to it) }
            classificacao.dns?.let { add("DNS" to it) }
            classificacao.fibra?.let { add("Fibra" to it) }
        }.filter { (_, item) -> item.avaliacao?.isNotBlank() == true }

    Column(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            "Análise por categoria",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            fontSize = 15.sp,
        )
        categorias.forEach { (nome, item) ->
            val label = normalizeClassificacaoLabel(item.avaliacao)
            val isBom = item.avaliacao?.lowercase() in setOf("boa", "bom")
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    nome,
                    style = MaterialTheme.typography.labelSmall,
                    color = c.textSecondary,
                    modifier = Modifier.weight(1f),
                )
                Box(
                    modifier =
                        Modifier
                            .clip(RoundedCornerShape(100.dp))
                            .background(if (isBom) c.successContainer else c.warningContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp),
                ) {
                    Text(
                        label,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isBom) c.onSuccessContainer else c.onWarningContainer,
                        fontWeight = FontWeight.W600,
                    )
                }
            }
        }
    }
}

// ─── Card Final — ChatCard ─────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ChatCard(
    c: LkTokens,
    perguntasContextuais: List<String>,
    chatInput: String,
    onChatInputChange: (String) -> Unit,
    onEnviar: (String) -> Unit = {},
    chipsSomidos: Boolean = false,
    limiteAtingido: Boolean = false,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Text(
            "Perguntar sobre este diagnóstico",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = c.textPrimary,
            fontSize = 16.sp,
        )

        // Chips de sugestão — somem quando há histórico de chat
        AnimatedVisibility(
            visible = !chipsSomidos && perguntasContextuais.isNotEmpty(),
            exit = fadeOut() + shrinkVertically(),
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                modifier = Modifier.fillMaxWidth(),
            ) {
                perguntasContextuais.take(3).forEach { pergunta ->
                    SuggestionChip(
                        onClick = { onChatInputChange(pergunta) },
                        label = {
                            Text(
                                pergunta.take(30).let { if (pergunta.length > 30) "$it..." else it },
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                            )
                        },
                        colors =
                            SuggestionChipDefaults.suggestionChipColors(
                                containerColor = c.bgSecondary,
                                labelColor = c.textSecondary,
                            ),
                        border =
                            SuggestionChipDefaults.suggestionChipBorder(
                                enabled = true,
                                borderColor = c.border,
                            ),
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }

        // Campo de texto pill
        OutlinedTextField(
            value = chatInput,
            onValueChange = onChatInputChange,
            modifier = Modifier.fillMaxWidth(),
            enabled = !limiteAtingido,
            placeholder = {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                ) {
                    Icon(
                        Icons.Outlined.Search,
                        contentDescription = null,
                        tint = c.textTertiary,
                        modifier = Modifier.size(14.dp),
                    )
                    Text(
                        "Pergunte sobre sua rede...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textTertiary,
                    )
                }
            },
            trailingIcon = {
                val filled = chatInput.isNotBlank() && !limiteAtingido
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = if (filled) "Enviar" else null,
                    tint = if (filled) LkColors.accent else c.textTertiary,
                    modifier =
                        Modifier
                            .clip(CircleShape)
                            .background(
                                if (filled) {
                                    LkColors.accent.copy(alpha = 0.10f)
                                } else {
                                    Color.Transparent
                                },
                            ).size(32.dp)
                            .then(
                                if (filled) {
                                    Modifier.clickable { onEnviar(chatInput) }
                                } else {
                                    Modifier
                                },
                            ).padding(LkSpacing.sm),
                )
            },
            singleLine = true,
            shape = RoundedCornerShape(24.dp),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LkColors.accent,
                    unfocusedBorderColor = c.border,
                    focusedContainerColor = c.bgCard,
                    unfocusedContainerColor = c.bgCard,
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary,
                    cursorColor = LkColors.accent,
                ),
        )

        if (limiteAtingido) {
            Text(
                "Limite da sessão atingido. Reinicie o diagnóstico para continuar.",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
        }
    }
}

// ─── Helpers internos reutilizados ────────────────────────────────────────────

@Composable
private fun MetricsRow(
    c: LkTokens,
    result: AiDiagnosisResult,
    statusColor: Color,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(100.dp))
                    .background(statusColor.copy(alpha = 0.10f))
                    .padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(6.dp)
                        .clip(CircleShape)
                        .background(statusColor),
            )
            Text(
                result.status.replaceFirstChar { it.uppercaseChar() },
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W600,
                color = statusColor,
            )
        }
        if (result.problemaPrincipal.confianca > 0.0) {
            Text(
                "${(result.problemaPrincipal.confianca * 100).toInt()}% confiança",
                style = MaterialTheme.typography.bodySmall,
                color = c.textTertiary,
            )
        }
    }
}

// ─── Helpers de ícone ─────────────────────────────────────────────────────────

private fun iconForProblemaTipo(tipo: String): ImageVector =
    when (tipo.lowercase()) {
        "wifi", "canal", "sinal" -> Icons.Outlined.NetworkWifi
        "roteador", "gateway" -> Icons.Outlined.Router
        "dispositivo" -> Icons.Outlined.DeviceHub
        "isp", "operadora" -> Icons.Outlined.Speed
        else -> Icons.Outlined.Info
    }

private fun iconForAcaoIndex(index: Int): ImageVector =
    when (index) {
        0 -> Icons.Outlined.Router
        1 -> Icons.Outlined.NetworkWifi
        2 -> Icons.Outlined.DeviceHub
        3 -> Icons.Outlined.Refresh
        else -> Icons.Outlined.CheckCircle
    }

// ─── Chat inline composables ──────────────────────────────────────────────────

@Composable
private fun DiagChatMensagem(
    entry: DiagChatEntry,
    c: LkTokens,
    onRetry: () -> Unit,
) {
    when (entry.autor) {
        DiagChatAutor.Usuario -> {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.CenterEnd) {
                Box(
                    modifier =
                        Modifier
                            .widthIn(max = LocalConfiguration.current.screenWidthDp.dp * 0.8f)
                            .background(
                                color = LkColors.accent.copy(alpha = 0.12f),
                                shape =
                                    RoundedCornerShape(
                                        topStart = 16.dp,
                                        topEnd = 16.dp,
                                        bottomStart = 16.dp,
                                        bottomEnd = 4.dp,
                                    ),
                            ).padding(horizontal = 12.dp, vertical = 8.dp),
                ) {
                    Text(
                        entry.texto,
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textPrimary,
                    )
                }
            }
        }
        DiagChatAutor.Ia -> DiagChatIaBubble(entry = entry, c = c, onRetry = onRetry)
    }
}

@Composable
private fun DiagChatIaBubble(
    entry: DiagChatEntry,
    c: LkTokens,
    onRetry: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = LkColors.accent,
            modifier =
                Modifier
                    .size(16.dp)
                    .padding(top = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                text = entry.nomeModelo ?: "Linka IA",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
            if (entry.isErro) {
                DiagChatErroContent(onRetry = onRetry, c = c)
            } else if (entry.isParcial) {
                DiagChatTextoComCursor(texto = entry.texto, c = c)
            } else {
                Text(
                    text = entry.texto,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textPrimary,
                )
            }
        }
    }
}

@Composable
private fun DiagChatLoadingItem(c: LkTokens) {
    val infiniteTransition = rememberInfiniteTransition(label = "diagChatLoading")

    @Composable
    fun PulseDot(delayMs: Int): Float {
        val alpha by infiniteTransition.animateFloat(
            initialValue = 0.3f,
            targetValue = 1f,
            animationSpec =
                infiniteRepeatable(
                    animation =
                        keyframes {
                            durationMillis = 900
                            0.3f at 0
                            1f at 300
                            0.3f at 600
                        },
                    initialStartOffset = StartOffset(delayMs),
                ),
            label = "dot$delayMs",
        )
        return alpha
    }

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.AutoAwesome,
            contentDescription = null,
            tint = LkColors.accent,
            modifier =
                Modifier
                    .size(16.dp)
                    .padding(top = 2.dp),
        )
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(
                "Linka IA",
                style = MaterialTheme.typography.labelSmall,
                color = c.textTertiary,
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                repeat(3) { i ->
                    Box(
                        modifier =
                            Modifier
                                .size(6.dp)
                                .background(
                                    color = LkColors.accent.copy(alpha = PulseDot(i * 150)),
                                    shape = CircleShape,
                                ),
                    )
                }
            }
        }
    }
}

@Composable
private fun DiagChatTextoComCursor(
    texto: String,
    c: LkTokens,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(500, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse,
            ),
        label = "cursorAlpha",
    )
    Text(
        text =
            buildAnnotatedString {
                append(texto)
                withStyle(SpanStyle(color = LkColors.accent.copy(alpha = cursorAlpha))) {
                    append("|")
                }
            },
        style = MaterialTheme.typography.bodyMedium,
        color = c.textPrimary,
    )
}

@Composable
private fun DiagChatErroContent(
    onRetry: () -> Unit,
    c: LkTokens,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(14.dp),
            )
            Text(
                "Não consegui responder agora.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )
        }
        TextButton(
            onClick = onRetry,
            contentPadding = PaddingValues(0.dp),
        ) {
            Text(
                "Tentar de novo",
                style = MaterialTheme.typography.labelSmall,
                color = LkColors.accent,
            )
        }
    }
}

// ─── Helpers ──────────────────────────────────────────────────────────────────

private fun statusToColor(
    status: String,
    c: LkTokens,
): Color =
    when (status.lowercase()) {
        "excelente", "bom" -> LkColors.success
        "regular" -> LkColors.warning
        "ruim", "critico" -> LkColors.error
        else -> c.textTertiary
    }
