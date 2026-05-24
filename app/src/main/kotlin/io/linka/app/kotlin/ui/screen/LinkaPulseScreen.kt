package io.linka.app.kotlin.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.feature.diagnostico.pulse.OpcaoResposta
import io.linka.app.kotlin.feature.diagnostico.pulse.PulseState
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.component.AppBorderGlowEffect
import io.linka.app.kotlin.ui.component.ContextualQuestionCard
import io.linka.app.kotlin.ui.component.DiagnosisChipsRow
import io.linka.app.kotlin.ui.component.LinkaPulseSymbol
import io.linka.app.kotlin.ui.component.PulseResultCard
import io.linka.app.kotlin.ui.component.RotatingMessageText
import io.linka.app.kotlin.ui.component.SilentSpeedtestIndicator

@Composable
fun LinkaPulseScreen(
    uiState: LinkaPulseUiState,
    onIniciarPulse: () -> Unit,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onResponderPergunta: (OpcaoResposta) -> Unit,
) {
    val c = LocalLkTokens.current

    val pulseState =
        when (uiState) {
            is LinkaPulseUiState.Idle -> PulseState.Idle
            is LinkaPulseUiState.Collecting -> PulseState.Collecting
            is LinkaPulseUiState.Thinking -> PulseState.Thinking
            is LinkaPulseUiState.Analyzing -> PulseState.Analyzing
            is LinkaPulseUiState.AwaitingChipSelection -> PulseState.AwaitingInput
            is LinkaPulseUiState.AwaitingAnswer -> PulseState.AwaitingInput
            is LinkaPulseUiState.Result -> uiState.pulseState
            is LinkaPulseUiState.Erro -> PulseState.Critical
        }

    val isActive =
        pulseState == PulseState.Collecting ||
            pulseState == PulseState.Thinking ||
            pulseState == PulseState.Analyzing

    Box(modifier = Modifier.fillMaxSize().background(c.bgPrimary)) {
        Column(
            modifier =
                Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(bottom = 24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(48.dp))

            LinkaPulseSymbol(
                state = pulseState,
                size = 140.dp,
            )

            Spacer(Modifier.height(32.dp))

            AnimatedContent(
                targetState = uiState,
                transitionSpec = { fadeIn() togetherWith fadeOut() },
                label = "pulse-content",
            ) { state ->
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (state) {
                        is LinkaPulseUiState.Idle -> IdleContent(onIniciarPulse)
                        is LinkaPulseUiState.Collecting ->
                            LoadingContent(
                                mensagem = state.mensagem,
                                showSpeedtest = true,
                            )
                        is LinkaPulseUiState.Thinking -> LoadingContent(mensagem = state.mensagem)
                        is LinkaPulseUiState.Analyzing -> LoadingContent(mensagem = state.mensagem)
                        is LinkaPulseUiState.AwaitingChipSelection -> {
                            ChipSelectionContent(
                                analysis = state.lastAnalysis,
                                chips = state.chips,
                                onSelecionarChip = onSelecionarChip,
                            )
                        }
                        is LinkaPulseUiState.AwaitingAnswer -> {
                            AnswerContent(
                                state = state,
                                onResponderPergunta = onResponderPergunta,
                            )
                        }
                        is LinkaPulseUiState.Result -> {
                            ResultContent(
                                state = state,
                                onSelecionarChip = onSelecionarChip,
                                onIniciarPulse = onIniciarPulse,
                            )
                        }
                        is LinkaPulseUiState.Erro -> {
                            ErrorContent(mensagem = state.mensagem, onIniciarPulse = onIniciarPulse)
                        }
                    }
                }
            }
        }

        AppBorderGlowEffect(active = isActive)
    }
}

@Composable
private fun IdleContent(onIniciarPulse: () -> Unit) {
    val c = LocalLkTokens.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Text(
            "LINKA PULSE",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.W700,
            color = LkColors.accent,
            letterSpacing = 2.sp,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Diagnóstico inteligente guiado por IA",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(40.dp))
        Button(
            onClick = onIniciarPulse,
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text("Iniciar diagnóstico", fontWeight = FontWeight.W600)
        }
    }
}

@Composable
private fun LoadingContent(
    mensagem: String,
    showSpeedtest: Boolean = false,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        RotatingMessageText(message = mensagem)
        if (showSpeedtest) {
            Spacer(Modifier.height(16.dp))
            SilentSpeedtestIndicator()
        }
    }
}

@Composable
private fun ChipSelectionContent(
    analysis: io.linka.app.kotlin.feature.diagnostico.pulse.AiAnalysisEntry,
    chips: List<OpcaoResposta>,
    onSelecionarChip: (OpcaoResposta) -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PulseResultCard(
            analysis = analysis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )
        Spacer(Modifier.height(24.dp))
        ChipsLabel()
        Spacer(Modifier.height(12.dp))
        DiagnosisChipsRow(chips = chips, onSelect = onSelecionarChip)
    }
}

@Composable
private fun AnswerContent(
    state: LinkaPulseUiState.AwaitingAnswer,
    onResponderPergunta: (OpcaoResposta) -> Unit,
) {
    ContextualQuestionCard(
        question = state.question,
        onResponder = onResponderPergunta,
        modifier = Modifier.padding(horizontal = 24.dp),
    )
}

@Composable
private fun ResultContent(
    state: LinkaPulseUiState.Result,
    onSelecionarChip: (OpcaoResposta) -> Unit,
    onIniciarPulse: () -> Unit,
) {
    val c = LocalLkTokens.current
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        PulseResultStatusBadge(pulseState = state.pulseState)
        Spacer(Modifier.height(16.dp))

        PulseResultCard(
            analysis = state.latestAnalysis,
            modifier = Modifier.padding(horizontal = 24.dp),
        )

        AnimatedVisibility(
            visible = state.availableChips.isNotEmpty(),
            enter = fadeIn(),
            exit = fadeOut(),
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(Modifier.height(24.dp))
                ChipsLabel()
                Spacer(Modifier.height(12.dp))
                DiagnosisChipsRow(chips = state.availableChips, onSelect = onSelecionarChip)
            }
        }

        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onIniciarPulse,
            colors = ButtonDefaults.outlinedButtonColors(contentColor = c.textSecondary),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text("Novo diagnóstico", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Normal)
        }
    }
}

@Composable
private fun ErrorContent(
    mensagem: String,
    onIniciarPulse: () -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.padding(horizontal = 32.dp),
    ) {
        Text(mensagem, style = MaterialTheme.typography.bodyMedium, color = LkColors.error, textAlign = TextAlign.Center)
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onIniciarPulse,
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
        ) {
            Text("Tentar novamente")
        }
    }
}

@Composable
private fun ChipsLabel() {
    val c = LocalLkTokens.current
    Text(
        "O que está incomodando?",
        style = MaterialTheme.typography.titleSmall,
        fontWeight = FontWeight.W500,
        color = c.textSecondary,
    )
}

@Composable
private fun PulseResultStatusBadge(pulseState: PulseState) {
    val (color, label) =
        when (pulseState) {
            PulseState.Critical -> LkColors.error to "Problemas críticos"
            PulseState.Warning -> LkColors.warning to "Atenção necessária"
            else -> LkColors.success to "Tudo OK"
        }
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(color.copy(alpha = 0.12f))
                .padding(horizontal = 16.dp, vertical = 6.dp),
    ) {
        Text(label, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = color)
    }
}
