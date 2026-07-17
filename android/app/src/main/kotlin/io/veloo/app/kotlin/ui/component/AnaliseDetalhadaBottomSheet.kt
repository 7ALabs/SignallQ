package io.signallq.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.feature.diagnostico.ai.AiAcaoRecomendada
import io.signallq.app.feature.diagnostico.ai.ordenadasPorPrioridade
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.screen.AnalisadorState

/**
 * Bottom sheet dedicado para o fluxo "Analisar meu problema com IA" — GH#931 (Fase 2 MD3).
 *
 * Extraído de dentro do sheet "Diagnóstico detalhado" (`ResultadoVelocidadeScreen`), onde
 * antes vivia inline como `AnalisadorProblemaSection`. Estados `Inativo/Analisando/Resultado/
 * Erro` de [AnalisadorState] mapeiam 1:1 para seletor de problema / loading / card de
 * diagnóstico / erro com retry — mesma máquina de estados de antes, só isolada em sheet
 * próprio para não competir visualmente com o resto do diagnóstico detalhado.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AnaliseDetalhadaBottomSheet(
    state: AnalisadorState,
    onAnalisarProblema: (String) -> Unit,
    onResetar: () -> Unit,
    onDismiss: () -> Unit,
) {
    val c = LocalLkTokens.current
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = c.bgPrimary,
    ) {
        AnaliseDetalhadaConteudo(
            state = state,
            onAnalisarProblema = onAnalisarProblema,
            onResetar = onResetar,
            c = c,
        )
    }
}

/** Título/subtítulo do header do sheet aninhado — distingue laudo automático da tela 1a
 * (`problemaRelatado == null`) de análise pedida pelo usuário por sintoma
 * (`problemaRelatado != null`); qualquer outro estado mantém a copy original de convite
 * a descrever o problema. Extraída como função pura pra ser testável isoladamente
 * (follow-up Lia, PR #1013). */
internal fun headerAnaliseDetalhada(state: AnalisadorState): Pair<String, String> =
    when {
        state is AnalisadorState.Resultado && state.problemaRelatado == null ->
            "Diagnóstico geral da sua conexão" to
                "Baseado no teste que você acabou de rodar. Quer detalhar um problema específico?"
        state is AnalisadorState.Resultado && state.problemaRelatado != null ->
            "Análise do seu problema" to
                "Diagnóstico específico para \"${state.problemaRelatado}\"."
        else ->
            "Analisar meu problema com IA" to
                "Descreva o que está acontecendo pra receber um diagnóstico específico."
    }

private val problemasPredefinidos =
    listOf(
        "Baixa velocidade",
        "Quedas constantes",
        "Travamentos em streaming ou jogos",
    )

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun AnaliseDetalhadaConteudo(
    state: AnalisadorState,
    onAnalisarProblema: (String) -> Unit,
    onResetar: () -> Unit,
    c: LkTokens,
) {
    var selecionandoProblema by remember { mutableStateOf(state is AnalisadorState.Inativo) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.xl)
                .padding(bottom = LkSpacing.xxl)
                .navigationBarsPadding(),
    ) {
        val (headerTitulo, headerSubtitulo) = headerAnaliseDetalhada(state)
        Text(
            text = headerTitulo,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.W700,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.xs))
        Text(
            text = headerSubtitulo,
            style = MaterialTheme.typography.bodySmall,
            color = c.textTertiary,
        )
        Spacer(Modifier.height(LkSpacing.lg))

        if (state is AnalisadorState.Inativo && selecionandoProblema) {
            Overline(texto = "Qual é o seu problema?", color = c.textTertiary)
            Spacer(Modifier.height(LkSpacing.sm))
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                problemasPredefinidos.forEach { problema ->
                    FilterChip(
                        selected = false,
                        onClick = { onAnalisarProblema(problema) },
                        label = { Text(text = problema, style = MaterialTheme.typography.bodySmall) },
                        colors =
                            FilterChipDefaults.filterChipColors(
                                containerColor = c.bgSecondary,
                                labelColor = c.textPrimary,
                            ),
                    )
                }
            }
            return@Column
        }

        when (state) {
            is AnalisadorState.Analisando -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        strokeWidth = 2.dp,
                        color = c.primary,
                    )
                    Text(
                        text = "Analisando seu problema...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                }
            }

            is AnalisadorState.Resultado -> {
                val origemLabel = if (state.origem == "ia") "Análise por IA" else "Diagnóstico local"
                val origemCor = if (state.origem == "ia") c.primary else c.textTertiary
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.card),
                    colors = CardDefaults.cardColors(containerColor = c.bgSecondary),
                ) {
                    Column(modifier = Modifier.padding(LkSpacing.lg)) {
                        Overline(
                            texto = state.problemaRelatado?.let { "Diagnóstico — $it" } ?: "Diagnóstico geral",
                            color = c.textTertiary,
                        )
                        Spacer(Modifier.height(LkSpacing.sm))
                        Text(
                            text = state.texto,
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textPrimary,
                        )

                        val proximasAcoes: List<AiAcaoRecomendada> =
                            state.acoes.ordenadasPorPrioridade().take(2)
                        if (proximasAcoes.isNotEmpty()) {
                            Spacer(Modifier.height(LkSpacing.md))
                            Column(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(LkRadius.card))
                                        .background(c.primary.copy(alpha = 0.08f))
                                        .padding(LkSpacing.md),
                                verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                            ) {
                                proximasAcoes.forEach { acao ->
                                    Row(verticalAlignment = Alignment.Top) {
                                        Icon(
                                            imageVector = Icons.Outlined.Info,
                                            contentDescription = null,
                                            tint = c.primary,
                                            modifier = Modifier.size(18.dp),
                                        )
                                        Spacer(Modifier.width(LkSpacing.sm))
                                        Column {
                                            Text(
                                                text = acao.titulo.ifBlank { "Próximo passo" },
                                                style = MaterialTheme.typography.titleSmall,
                                                fontWeight = FontWeight.W600,
                                                color = c.textPrimary,
                                            )
                                            Text(
                                                text = acao.descricao,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = c.textSecondary,
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        Spacer(Modifier.height(LkSpacing.md))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = origemLabel,
                                style = MaterialTheme.typography.labelMedium,
                                color = origemCor,
                                fontWeight = FontWeight.Medium,
                            )
                            TextButton(onClick = {
                                selecionandoProblema = true
                                onResetar()
                            }) {
                                Text(text = "Nova análise", color = c.textTertiary, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                }
            }

            is AnalisadorState.Erro -> {
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(text = state.mensagem, style = MaterialTheme.typography.bodySmall, color = LkColors.error)
                    Spacer(Modifier.height(LkSpacing.xs))
                    TextButton(onClick = {
                        selecionandoProblema = true
                        onResetar()
                    }) {
                        Text(text = "Tentar novamente", color = c.textTertiary, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }

            else -> {
                OutlinedButton(
                    onClick = { selecionandoProblema = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(LkRadius.button),
                ) {
                    Text(
                        text = "Escolher o problema",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = c.textPrimary,
                    )
                }
            }
        }
    }
}
