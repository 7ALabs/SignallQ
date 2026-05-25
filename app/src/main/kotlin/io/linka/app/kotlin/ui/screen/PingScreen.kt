package io.linka.app.kotlin.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.linka.app.kotlin.feature.speedtest.PingExecutor
import io.linka.app.kotlin.feature.speedtest.PingResultado
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkRadius
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens
import io.linka.app.kotlin.ui.state.UiState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Dado de UI do PingScreen.
 *
 * [Executando] carrega o progresso incremental durante a coleta de amostras.
 * [Concluido] carrega o resultado final do ping.
 *
 * Nota: PingScreenViewModel nao e @HiltViewModel — e criado via remember{} no Composable.
 * Isso e intencional para manter o escopo do bottom sheet. Nao refatorar aqui (Sub-task C).
 */
sealed interface PingUiData {
    data class Executando(
        val progresso: Int,
    ) : PingUiData

    data class Concluido(
        val resultado: PingResultado,
    ) : PingUiData
}

class PingScreenViewModel {
    private val mutableStateFlow = MutableStateFlow<UiState<PingUiData>>(UiState.Empty)
    val stateFlow: StateFlow<UiState<PingUiData>> = mutableStateFlow.asStateFlow()

    suspend fun executarPing() {
        try {
            mutableStateFlow.value = UiState.Success(PingUiData.Executando(0))
            val executor = PingExecutor()
            val resultado =
                executor.executar(count = 20) { progresso ->
                    mutableStateFlow.value = UiState.Success(PingUiData.Executando(progresso))
                }
            mutableStateFlow.value = UiState.Success(PingUiData.Concluido(resultado))
        } catch (e: Exception) {
            mutableStateFlow.value = UiState.Error(e.message ?: "Erro ao executar ping")
        }
    }

    fun resetar() {
        mutableStateFlow.value = UiState.Empty
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PingScreen(onDismiss: () -> Unit) {
    val c = LocalLkTokens.current
    val sheetState = rememberModalBottomSheetState()
    val scope = rememberCoroutineScope()
    val viewModel = remember { PingScreenViewModel() }
    val state by viewModel.stateFlow.collectAsState()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {},
        containerColor = c.bgCard,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp, start = LkSpacing.lg, end = LkSpacing.lg),
        ) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(top = LkSpacing.md, bottom = LkSpacing.lg),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier =
                        Modifier
                            .width(40.dp)
                            .height(4.dp)
                            .clip(RoundedCornerShape(999.dp))
                            .background(c.border),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(bottom = LkSpacing.md),
            ) {
                Icon(
                    imageVector = Icons.Outlined.NetworkCheck,
                    contentDescription = null,
                    tint = c.textPrimary,
                    modifier = Modifier.size(24.dp),
                )
                Spacer(Modifier.padding(start = LkSpacing.sm))
                Text(
                    text = "Teste de Latência",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = c.textPrimary,
                )
            }

            when (val currentState = state) {
                UiState.Empty -> {
                    Text(
                        text = "Mede o tempo de resposta (latência) dos servidores de teste.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(bottom = LkSpacing.md),
                    )
                    Button(
                        onClick = {
                            scope.launch {
                                viewModel.executarPing()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Iniciar teste de latência")
                    }
                }

                UiState.Loading -> {
                    // Loading generico nao usado neste fluxo — estado intermediario e Success<Executando>
                }

                is UiState.Success -> {
                    when (val data = currentState.data) {
                        is PingUiData.Executando -> {
                            Text(
                                text = "Coletando amostras... ${data.progresso}/20",
                                style = MaterialTheme.typography.bodyMedium,
                                color = c.textPrimary,
                                modifier = Modifier.padding(bottom = LkSpacing.md),
                            )
                            LinearProgressIndicator(
                                progress = { (data.progresso / 20f).coerceIn(0f, 1f) },
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .height(4.dp)
                                        .clip(RoundedCornerShape(2.dp))
                                        .semantics {
                                            contentDescription =
                                                "Medindo latência — ${data.progresso} de 20 amostras coletadas"
                                        },
                            )
                        }

                        is PingUiData.Concluido -> {
                            val resultado = data.resultado
                            Text(
                                text = "Resultados do teste",
                                style = MaterialTheme.typography.labelMedium,
                                color = c.textSecondary,
                                modifier = Modifier.padding(bottom = LkSpacing.md),
                            )

                            Row(
                                modifier =
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = LkSpacing.md),
                                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                            ) {
                                PingMetricCard(
                                    c = c,
                                    label = "Latência",
                                    valor = "%.1f ms".format(resultado.latenciaMs),
                                    modifier = Modifier.weight(1f),
                                )
                                PingMetricCard(
                                    c = c,
                                    label = "Jitter",
                                    valor = "%.1f ms".format(resultado.jitterMs),
                                    modifier = Modifier.weight(1f),
                                )
                                PingMetricCard(
                                    c = c,
                                    label = "Perda",
                                    valor = "%.0f%%".format(resultado.perdaPercentual),
                                    modifier = Modifier.weight(1f),
                                )
                            }

                            Button(
                                onClick = {
                                    viewModel.resetar()
                                    scope.launch {
                                        viewModel.executarPing()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("Testar novamente")
                            }
                        }
                    }
                }

                is UiState.Error -> {
                    Text(
                        text = currentState.message,
                        style = MaterialTheme.typography.bodySmall,
                        color = LkColors.error,
                        modifier = Modifier.padding(bottom = LkSpacing.md),
                    )
                    Button(
                        onClick = {
                            viewModel.resetar()
                            scope.launch {
                                viewModel.executarPing()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("Tentar novamente")
                    }
                }
            }
        }
    }
}

@Composable
private fun PingMetricCard(
    c: LkTokens,
    label: String,
    valor: String,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier =
            modifier
                .clip(RoundedCornerShape(LkRadius.card))
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary)
                .padding(LkSpacing.md),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = c.textSecondary,
                modifier = Modifier.padding(bottom = 4.dp),
            )
            Text(
                text = valor,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                textAlign = TextAlign.Center,
            )
        }
    }
}
