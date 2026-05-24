package io.linka.app.kotlin.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Speed
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
import androidx.compose.material3.Scaffold
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.feature.diagnostico.ConnectionType
import io.linka.app.kotlin.feature.diagnostico.DiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.EstadoDiagnostico
import io.linka.app.kotlin.feature.diagnostico.SnapshotDiagnostico
import io.linka.app.kotlin.feature.diagnostico.ai.AiAcaoRecomendada
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisRepository
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisResult
import io.linka.app.kotlin.feature.diagnostico.ai.AiDiagnosisState
import io.linka.app.kotlin.feature.diagnostico.ai.AiFallbackFactory
import io.linka.app.kotlin.feature.diagnostico.ai.DiagnosisAiContextFactory
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val AI_BASE_URL = "https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev"

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
                            "Diagnóstico",
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
        val state = resolveScreenState(snapshotDiagnostico, aiState, analiseSolicitada)
        Box(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding),
        ) {
            when (state) {
                DiagnosticoScreenState.Idle ->
                    DiagnosticoIdleContent(
                        c = c,
                        onAnalisar = {
                            onAnaliseSolicitadaChange(true)
                            onAiStateChange(AiDiagnosisState.idle)
                            onIniciarDiagnostico()
                        },
                    )

                DiagnosticoScreenState.EnginesRodando ->
                    DiagnosticoLoadingContent(
                        c = c,
                        isAiPhase = false,
                    )

                DiagnosticoScreenState.AiRodando ->
                    DiagnosticoLoadingContent(
                        c = c,
                        isAiPhase = true,
                    )

                is DiagnosticoScreenState.Resultado ->
                    DiagnosticoResultadoContent(
                        c = c,
                        result = state.result,
                        isFallback = state.isFallback,
                        input = snapshotDiagnostico.input,
                        onReanalisar = {
                            scope.launch {
                                val relatorio = snapshotDiagnostico.relatorio ?: return@launch
                                onAiStateChange(AiDiagnosisState.loading)
                                val connectionType = snapshotDiagnostico.input?.connectionType ?: ConnectionType.desconhecido
                                val ctx = DiagnosisAiContextFactory.from(relatorio, snapshotDiagnostico.input, connectionType)
                                onAiStateChange(aiRepository.explainDiagnosis(ctx) { AiFallbackFactory.fromLocal(relatorio) })
                            }
                        },
                        onAbrirRedes = onAbrirRedes,
                    )

                DiagnosticoScreenState.Erro ->
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

// ─── State resolution ─────────────────────────────────────────────────────────

private sealed interface DiagnosticoScreenState {
    data object Idle : DiagnosticoScreenState

    data object EnginesRodando : DiagnosticoScreenState

    data object AiRodando : DiagnosticoScreenState

    data class Resultado(
        val result: AiDiagnosisResult,
        val isFallback: Boolean,
    ) : DiagnosticoScreenState

    data object Erro : DiagnosticoScreenState
}

private fun resolveScreenState(
    snap: SnapshotDiagnostico,
    ai: AiDiagnosisState,
    solicitada: Boolean,
): DiagnosticoScreenState {
    if (!solicitada) return DiagnosticoScreenState.Idle
    if (snap.estado == EstadoDiagnostico.erro) return DiagnosticoScreenState.Erro
    if (ai is AiDiagnosisState.success) return DiagnosticoScreenState.Resultado(ai.result, false)
    if (ai is AiDiagnosisState.fallback) return DiagnosticoScreenState.Resultado(ai.result, true)
    if (ai is AiDiagnosisState.loading) return DiagnosticoScreenState.AiRodando
    if (snap.estado == EstadoDiagnostico.idle) return DiagnosticoScreenState.EnginesRodando
    return DiagnosticoScreenState.EnginesRodando
}

// ─── Estados da tela ──────────────────────────────────────────────────────────

@Composable
private fun DiagnosticoIdleContent(
    c: LkTokens,
    onAnalisar: () -> Unit,
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
                    "Diagnóstico Inteligente\nde Rede com IA",
                    style = MaterialTheme.typography.headlineLarge,
                    fontWeight = FontWeight.W800,
                    color = LkColors.linkaTextOnDark,
                    textAlign = TextAlign.Center,
                    lineHeight = 30.sp,
                )
                Text(
                    "Descubra em segundos por que sua internet está lenta, instável ou com queda de conexão — a IA faz a análise completa por você.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = LkColors.linkaTextOnDark.copy(alpha = 0.85f),
                    textAlign = TextAlign.Center,
                    lineHeight = 22.sp,
                )
            }
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
                "O QUE A IA ANALISA",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W700,
                color = c.textTertiary,
                letterSpacing = 0.8.sp,
            )
            HorizontalDivider(color = c.border)
            DiagnosticoFeatureItem("Velocidade de download, upload e latência da rede", c)
            DiagnosticoFeatureItem("Sinal Wi-Fi, canal e qualidade do roteador", c)
            DiagnosticoFeatureItem("DNS, fibra óptica e histórico de conexão", c)
            DiagnosticoFeatureItem("Identifica a causa raiz com relatório personalizado", c)
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
                "Iniciar Diagnóstico",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
            )
        }

        Spacer(Modifier.height(LkSpacing.lg))
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

private val ENGINE_STEPS =
    listOf(
        "Coletando velocidade e latência",
        "Verificando sinal Wi-Fi",
        "Checando DNS e histórico",
    )

private enum class StepEstado { Pendente, Ativo, Concluido }

@Composable
private fun DiagnosticoLoadingContent(
    c: LkTokens,
    isAiPhase: Boolean,
) {
    var stepsVisiveis by remember(isAiPhase) {
        mutableIntStateOf(if (isAiPhase) ENGINE_STEPS.size else 0)
    }

    LaunchedEffect(isAiPhase) {
        if (!isAiPhase) {
            ENGINE_STEPS.indices.forEach { i ->
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
                if (isAiPhase) "Consultando IA…" else "Analisando sua conexão…",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.W700,
                color = c.textPrimary,
            )
            Spacer(Modifier.height(LkSpacing.xl))

            ENGINE_STEPS.forEachIndexed { index, texto ->
                AnimatedVisibility(
                    visible = index < stepsVisiveis,
                    enter = fadeIn(tween(300)) + expandVertically(tween(300)),
                ) {
                    StepRow(c = c, texto = texto, estado = StepEstado.Concluido)
                }
            }

            AnimatedVisibility(
                visible = stepsVisiveis == ENGINE_STEPS.size,
                enter = fadeIn(tween(300)) + expandVertically(tween(300)),
            ) {
                StepRow(
                    c = c,
                    texto = "Gerando diagnóstico com IA…",
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

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = LkSpacing.sm),
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
                "Não foi possível diagnosticar",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
            Text(
                "Verifique se há conexão com a internet e tente novamente.",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                lineHeight = 20.sp,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            OutlinedButton(onClick = onTentar) {
                Text("Tentar novamente")
            }
        }
    }
}

// ─── Tela de resultado ────────────────────────────────────────────────────────

@Composable
private fun DiagnosticoResultadoContent(
    c: LkTokens,
    result: AiDiagnosisResult,
    isFallback: Boolean,
    input: DiagnosticInput?,
    onReanalisar: () -> Unit,
    onAbrirRedes: () -> Unit,
) {
    var analiseExpandida by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bgPrimary),
    ) {
        LazyColumn(
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
            // Mensagem do usuário (direita)
            item {
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                    Text(
                        "Analisar minha conexão",
                        modifier =
                            Modifier
                                .clip(
                                    RoundedCornerShape(
                                        topStart = 18.dp,
                                        topEnd = 4.dp,
                                        bottomEnd = 18.dp,
                                        bottomStart = 18.dp,
                                    ),
                                ).background(LkColors.accent)
                                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                        color = LkColors.linkaTextOnDark,
                    )
                }
            }

            // Bubble de resposta da IA
            item {
                AiResultBubble(
                    c = c,
                    result = result,
                    isFallback = isFallback,
                    input = input,
                    analiseExpandida = analiseExpandida,
                    onToggleAnalise = { analiseExpandida = !analiseExpandida },
                    onReanalisar = onReanalisar,
                    onAbrirRedes = onAbrirRedes,
                )
            }
        }
    }
}

@Composable
private fun AiResultBubble(
    c: LkTokens,
    result: AiDiagnosisResult,
    isFallback: Boolean,
    input: DiagnosticInput?,
    analiseExpandida: Boolean,
    onToggleAnalise: () -> Unit,
    onReanalisar: () -> Unit,
    onAbrirRedes: () -> Unit,
) {
    val statusColor = statusToColor(result.status, c)

    val bubbleShape =
        RoundedCornerShape(
            topStart = 4.dp,
            topEnd = LkRadius.card,
            bottomEnd = LkRadius.card,
            bottomStart = LkRadius.card,
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(bubbleShape)
                .background(c.bgCard)
                .border(1.dp, c.border, bubbleShape)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        // Ícone de IA + badge de fallback (quando aplicável)
        Row(
            horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
            verticalAlignment = Alignment.Top,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier =
                    Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(LkColors.accent.copy(alpha = 0.10f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.AutoAwesome,
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier = Modifier.size(14.dp),
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                if (isFallback) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(c.textTertiary.copy(alpha = 0.10f))
                                .padding(horizontal = 6.dp, vertical = 2.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Info,
                            contentDescription = null,
                            tint = c.textTertiary,
                            modifier = Modifier.size(10.dp),
                        )
                        Text(
                            "Análise local — sem IA",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textTertiary,
                        )
                    }
                }
                Text(
                    result.textoLaudo.ifBlank { result.resumo },
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textPrimary,
                    lineHeight = 22.sp,
                )
            }
        }

        MetricsRow(c = c, result = result, input = input, statusColor = statusColor)

        // Lista de ações — sem badges, sem números
        if (result.acoesRecomendadas.isNotEmpty()) {
            HorizontalDivider(color = c.border, thickness = 0.5.dp)
            ActionsSimpleList(c = c, actions = result.acoesRecomendadas, onAbrirRedes = onAbrirRedes)
        }

        // Header da seção expandível
        HorizontalDivider(color = c.border, thickness = 0.5.dp)
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { onToggleAnalise() }
                    .padding(vertical = LkSpacing.md),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                "Análise completa",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.W500,
                color = c.textSecondary,
            )
            Icon(
                imageVector = Icons.Outlined.ExpandMore,
                contentDescription = if (analiseExpandida) "Recolher" else "Expandir",
                tint = c.textTertiary,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(
            visible = analiseExpandida,
            enter = fadeIn(tween(200)) + expandVertically(tween(200)),
        ) {
            AnaliseCompletaContent(
                c = c,
                result = result,
                isFallback = isFallback,
                statusColor = statusColor,
                onReanalisar = onReanalisar,
            )
        }

        // Rodapé: modelo de IA
        val rodape =
            result.modeloIa.textoRodape
                .removePrefix("Motor de análise: ")
                .ifBlank { "Linka IA" }
        Text(
            rodape,
            style = MaterialTheme.typography.labelMedium,
            color = c.textTertiary,
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.End,
        )
    }
}

@Composable
private fun MetricsRow(
    c: LkTokens,
    result: AiDiagnosisResult,
    input: DiagnosticInput?,
    statusColor: Color,
) {
    val download = input?.internet?.downloadMbps
    val upload = input?.internet?.uploadMbps
    val latency = input?.internet?.latencyMs

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
        if (download != null) {
            Text(
                "↓ ${"%.0f".format(download)} Mbps",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = LkColors.phaseDownload,
            )
        }
        if (upload != null) {
            Text(
                "↑ ${"%.0f".format(upload)} Mbps",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = LkColors.phaseUpload,
            )
        }
        if (latency != null) {
            Text(
                "${"%.0f".format(latency)} ms",
                style = MaterialTheme.typography.bodySmall,
                fontWeight = FontWeight.W600,
                color = LkColors.phaseLatencia,
            )
        }
    }
}

@Composable
private fun ActionsSimpleList(
    c: LkTokens,
    actions: List<AiAcaoRecomendada>,
    onAbrirRedes: () -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        Text(
            "O QUE FAZER",
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.W700,
            color = c.textTertiary,
            letterSpacing = 0.5.sp,
        )
        actions.forEach { acao ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                verticalAlignment = Alignment.Top,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Box(
                    modifier =
                        Modifier
                            .padding(top = 4.dp)
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(LkColors.accent),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(acao.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
                    if (acao.descricao.isNotBlank()) {
                        Text(acao.descricao, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, lineHeight = 17.sp)
                    }
                    val isRedesAction =
                        acao.titulo.contains("rede", ignoreCase = true) ||
                            acao.titulo.contains("canal", ignoreCase = true) ||
                            acao.titulo.contains("Wi-Fi", ignoreCase = true)
                    if (isRedesAction) {
                        TextButton(
                            onClick = onAbrirRedes,
                        ) {
                            Text("Ver redes Wi-Fi →", style = MaterialTheme.typography.bodySmall, color = LkColors.accent)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AnaliseCompletaContent(
    c: LkTokens,
    result: AiDiagnosisResult,
    isFallback: Boolean,
    statusColor: Color,
    onReanalisar: () -> Unit,
) {
    Column(
        modifier = Modifier.padding(top = LkSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        if (result.evidencias.isNotEmpty()) {
            Text(
                "POR QUE CONCLUÍMOS",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.W700,
                color = c.textTertiary,
                letterSpacing = 0.5.sp,
            )
            result.evidencias.forEachIndexed { index, ev ->
                if (index > 0) HorizontalDivider(color = c.border, thickness = 0.5.dp)
                EvidenciaItem(c = c, label = ev.label, valor = ev.valor, interpretacao = ev.interpretacao)
            }
        }

        if (result.limitesDaAnalise.isNotEmpty()) {
            if (result.evidencias.isNotEmpty()) HorizontalDivider(color = c.border, thickness = 0.5.dp)
            result.limitesDaAnalise.forEach { limite ->
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
                ) {
                    Icon(
                        Icons.Outlined.Info,
                        contentDescription = null,
                        tint = c.textTertiary,
                        modifier =
                            Modifier
                                .padding(top = 2.dp)
                                .size(12.dp),
                    )
                    Text(limite, style = MaterialTheme.typography.labelMedium, color = c.textTertiary, lineHeight = 17.sp)
                }
            }
        }

        if (result.problemaPrincipal.confianca > 0.0) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                Text("Confiança:", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
                ConfiancaBarra(confianca = result.problemaPrincipal.confianca, color = statusColor)
                Text(
                    "${(result.problemaPrincipal.confianca * 100).toInt()}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textTertiary,
                )
            }
        }

        val horario = SimpleDateFormat("dd/MM HH:mm", Locale.forLanguageTag("pt-BR")).format(Date(result.generatedAt))
        val fonteLabel = if (isFallback) "Análise local" else "Análise IA Cloudflare"
        Text("$fonteLabel · $horario", style = MaterialTheme.typography.labelMedium, color = c.textTertiary)

        OutlinedButton(
            onClick = onReanalisar,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Icon(Icons.Outlined.Refresh, contentDescription = null, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text("Reanalisar", fontSize = 13.sp)
        }
    }
}

// ─── Componentes internos ─────────────────────────────────────────────────────

@Composable
private fun ConfiancaBarra(
    confianca: Double,
    color: Color,
) {
    val totalBlocos = 10
    val blocosCheios = (confianca * totalBlocos).toInt().coerceIn(0, totalBlocos)
    Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
        repeat(totalBlocos) { i ->
            Box(
                modifier =
                    Modifier
                        .width(14.dp)
                        .height(6.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(if (i < blocosCheios) color else color.copy(alpha = 0.15f)),
            )
        }
    }
}

@Composable
private fun EvidenciaItem(
    c: LkTokens,
    label: String,
    valor: String,
    interpretacao: String,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(
            Icons.Outlined.CheckCircle,
            contentDescription = null,
            tint = LkColors.accent.copy(alpha = 0.6f),
            modifier =
                Modifier
                    .padding(top = 2.dp)
                    .size(16.dp),
        )
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = c.textPrimary)
                if (valor.isNotBlank()) {
                    Text(valor, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.W500, color = LkColors.accent)
                }
            }
            if (interpretacao.isNotBlank()) {
                Text(interpretacao, style = MaterialTheme.typography.bodySmall, color = c.textSecondary, lineHeight = 17.sp)
            }
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
