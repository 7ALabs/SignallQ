package io.signallq.app.ui.screen

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import io.signallq.app.core.diagnostico.ObjetivoDiagnostico
import io.signallq.app.core.diagnostico.PerguntaFechada
import io.signallq.app.core.diagnostico.PerguntasDiagnosticoGuiado
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.SignallQButton
import io.signallq.app.ui.component.SignallQTopAppBar

internal data class AssistContexto(
    val perguntaId: String,
    val pergunta: PerguntaFechada,
)

internal fun ObjetivoDiagnostico.analyticsId(): String = name.lowercase()

internal fun ObjetivoDiagnostico.contextoQueAlteraDiagnostico(): AssistContexto? =
    when (this) {
        ObjetivoDiagnostico.JOGOS_COM_LAG ->
            AssistContexto("conexao_jogo", PerguntasDiagnosticoGuiado.perguntas(this).first())
        ObjetivoDiagnostico.WIFI_VS_OPERADORA ->
            AssistContexto("melhora_sem_wifi", PerguntasDiagnosticoGuiado.perguntas(this).first())
        ObjetivoDiagnostico.INTERNET_CAI_OSCILA,
        ObjetivoDiagnostico.VIDEOS_TRAVAM,
        ObjetivoDiagnostico.CHAMADAS_CONGELAM,
        ObjetivoDiagnostico.SITES_DEMORAM,
        ObjetivoDiagnostico.VELOCIDADE_NAO_CHEGA,
        -> null
    }

@Stable
internal class AssistScreenState(
    objectiveName: String? = null,
    selectedAnswer: Int = -1,
    val retomada: Boolean = false,
) {
    var objectiveName by mutableStateOf(objectiveName)
    var selectedAnswer by mutableIntStateOf(selectedAnswer)
    var terminal by mutableStateOf(false)

    val objetivo: ObjetivoDiagnostico?
        get() = objectiveName?.let(ObjetivoDiagnostico::valueOf)

    companion object {
        internal fun snapshot(state: AssistScreenState): List<Any?> =
            listOf(state.objectiveName, state.selectedAnswer)

        internal fun restoreSnapshot(value: List<Any?>): AssistScreenState =
            AssistScreenState(value[0] as String?, value[1] as Int, retomada = true)

        val Saver =
            Saver<AssistScreenState, List<Any?>>(
                save = { state -> snapshot(state) },
                restore = ::restoreSnapshot,
            )
    }
}

@Composable
internal fun AssistScreen(
    onObjetivoConfirmado: (ObjetivoDiagnostico?, retomada: Boolean) -> Unit,
    onRespostaConfirmada: (ObjetivoDiagnostico, AssistContexto, respostaIndex: Int, retomada: Boolean) -> Unit,
    onConcluir: (ObjetivoDiagnostico?, List<Int>) -> Unit,
    onAbandonar: (objetivo: ObjetivoDiagnostico?, retomavel: Boolean) -> Unit,
) {
    val c = LocalLkTokens.current
    val state = rememberSaveable(saver = AssistScreenState.Saver) { AssistScreenState() }
    val objetivo = state.objetivo
    val contexto = objetivo?.contextoQueAlteraDiagnostico()
    // Mesmo padrão de DiagnosticoGuiadoScreen.voltarUmPasso() (review da PR #1683, bloqueio 2):
    // Voltar na pergunta contextual corrige o toque errado voltando pra lista de sintomas, não
    // descarta a sessão. Só abandona de fato (fecha o Assist) a partir da lista — nesse ponto
    // nenhum objetivo foi confirmado ainda, então não há nada "retomável" (bloqueio 1: o campo
    // `retomavel` do evento de abandono deixa de poder mentir, porque a única saída de
    // `contexto != null` agora é "voltar um passo", não abandonar).
    val voltarUmPasso = {
        if (!state.terminal) {
            if (contexto != null) {
                state.objectiveName = null
                state.selectedAnswer = -1
            } else {
                state.terminal = true
                onAbandonar(objetivo, objetivo != null)
            }
        }
    }
    BackHandler(onBack = voltarUmPasso)

    androidx.compose.material3.Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            SignallQTopAppBar(
                // Mesma distinção de título do protótipo (index.html, data-title):
                // "O que está acontecendo?" na lista de sintomas, "Assist" só na pergunta
                // contextual de uma pergunta só.
                title = if (contexto != null) "Assist" else "O que está acontecendo?",
                navigationIcon = {
                    IconButton(onClick = voltarUmPasso) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
            )
        },
    ) { padding ->
        if (objetivo == null) {
            AssistObjetivos(
                modifier = Modifier.padding(padding),
                c = c,
                onSelect = { selecionado ->
                    if (!state.terminal) {
                        onObjetivoConfirmado(selecionado, state.retomada)
                        if (selecionado == null || selecionado.contextoQueAlteraDiagnostico() == null) {
                            state.terminal = true
                            onConcluir(selecionado, emptyList())
                        } else {
                            state.objectiveName = selecionado.name
                        }
                    }
                },
            )
        } else if (contexto != null) {
            AssistPergunta(
                modifier = Modifier.padding(padding),
                c = c,
                contexto = contexto,
                selectedAnswer = state.selectedAnswer,
                onSelect = { state.selectedAnswer = it },
                onContinue = {
                    if (!state.terminal) {
                        state.terminal = true
                        onRespostaConfirmada(objetivo, contexto, state.selectedAnswer, state.retomada)
                        onConcluir(objetivo, listOf(state.selectedAnswer))
                    }
                },
            )
        }
    }
}

@Composable
private fun AssistObjetivos(
    onSelect: (ObjetivoDiagnostico?) -> Unit,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            "O que está acontecendo com sua internet?",
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = c.textPrimary,
        )
        Text(
            "Escolha o que mais se parece com o problema.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        AssistObjectiveRow(
            title = "Quero verificar minha conexão",
            subtitle = "Análise geral, sem presumir um problema",
            c = c,
            onClick = { onSelect(null) },
        )
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            AssistObjectiveRow(title = objetivo.titulo, subtitle = objetivo.subtitulo, c = c, onClick = { onSelect(objetivo) })
        }
    }
}

@Composable
private fun AssistObjectiveRow(
    title: String,
    subtitle: String,
    c: LkTokens,
    onClick: () -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(
                    role = Role.Button,
                    onClickLabel = "Selecionar $title",
                    onClick = onClick,
                ).padding(vertical = LkSpacing.base),
    ) {
        Text(title, style = MaterialTheme.typography.titleMedium, color = c.textPrimary)
        Text(subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
    }
}

@Composable
private fun AssistPergunta(
    contexto: AssistContexto,
    selectedAnswer: Int,
    onSelect: (Int) -> Unit,
    onContinue: () -> Unit,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.lg),
    ) {
        LkSectionOverline(text = "SignallQ Assist · uma pergunta")
        Text(
            contexto.pergunta.texto,
            modifier = Modifier.semantics { heading() },
            style = MaterialTheme.typography.headlineSmall,
            color = c.textPrimary,
        )
        Text(
            "Responda para ajustarmos a análise ao seu caso.",
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        Column(Modifier.selectableGroup()) {
            contexto.pergunta.opcoes.forEachIndexed { index, answer ->
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selectedAnswer == index,
                                role = Role.RadioButton,
                                onClick = { onSelect(index) },
                            ).padding(vertical = LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    RadioButton(selected = selectedAnswer == index, onClick = null)
                    Text(answer, style = MaterialTheme.typography.bodyLarge, color = c.textPrimary)
                }
            }
        }
        SignallQButton(
            label = "Continuar",
            enabled = selectedAnswer >= 0,
            onClick = onContinue,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}
