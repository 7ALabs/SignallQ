package io.signallq.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import kotlinx.coroutines.delay

// Medição pedida pelo fluxo guiado — GH#1704, extraído de `AppShell.kt` na rodada 2 de revisão.
//
// Motivo da extração (bloqueio B3 de Caio na PR #1719): o flag vivia dentro de um arquivo de 1669
// linhas sem harness de teste, e por isso dois mutantes sobreviviam à suíte inteira do `:app` —
// remover o reset em `onCancelar`, e remover a supressão do `VelocidadeScreen`. "Não tem teste"
// era consequência de onde o código morava, não de o teste ser difícil.
//
// O que a extração cobre e o que não cobre: as **transições de estado** ficaram testáveis
// (`AppShellMedicaoGuiadaTest`, 9 casos com snapshot dirigido no tempo). As **três leituras** em
// `AppShell.kt` continuam sem teste — apagar `!medicaoGuiada.suprimeReacoesDoShell` de lá ainda
// passa na suíte inteira, verificado por mutação. Cobrir isso exige compor o `AppShell`, que tem
// 30+ parâmetros e nenhum harness; não é escopo desta fatia.

/** Quanto esperar o executor sair do lugar antes de declarar que a medição não começou. */
internal const val LIMITE_PARA_INICIAR_MS = 8_000L

internal const val MENSAGEM_NAO_INICIOU =
    "Não consegui começar a análise. Verifique se você continua conectado."

/**
 * O que o `AppShell` consome. Reconstruído a cada recomposição a partir do snapshot; o estado que
 * precisa persistir mora em [MedicaoGuiada].
 */
@Stable
internal data class MedicaoGuiadaUi(
    val contrato: AnaliseGuiadaContrato,
    /**
     * O `ExecutorSpeedtest` é `@Singleton`, então uma medição pedida pelo fluxo guiado é
     * indistinguível, no snapshot, de uma pedida na tela Velocidade. Enquanto isto for `true` o
     * shell não deve reagir como reage a um teste comum.
     */
    val suprimeReacoesDoShell: Boolean,
    /** `true` se a conclusão que acabou de chegar pertencia ao fluxo guiado. Consome o estado. */
    val consumirConclusao: () -> Boolean,
)

/**
 * Estado vivo da medição guiada. Classe própria, e não três `var` soltos no shell, porque as
 * transições têm invariantes entre si — `naoIniciou` e `aguandoInicio` nunca são verdadeiros ao
 * mesmo tempo, e `pedirMedicao` tem que limpar os dois.
 */
@Stable
internal class MedicaoGuiada {
    /** Há uma medição deste fluxo em aberto (pedida e ainda não concluída nem cancelada). */
    var emCurso by mutableStateOf(false)
        private set

    /** Pedimos a medição e ainda não vimos o executor publicar `executando`. */
    var aguardandoInicio by mutableStateOf(false)
        private set

    /** O executor recusou o disparo em silêncio — ver o KDoc de [rememberMedicaoGuiada]. */
    var naoIniciou by mutableStateOf(false)
        private set

    fun pedirMedicao(disparar: () -> Unit) {
        emCurso = true
        aguardandoInicio = true
        naoIniciou = false
        disparar()
    }

    fun cancelar(cancelarExecutor: () -> Unit) {
        emCurso = false
        aguardandoInicio = false
        naoIniciou = false
        cancelarExecutor()
    }

    fun registrarInicioObservado() {
        aguardandoInicio = false
        naoIniciou = false
    }

    fun registrarFalhaDeInicio() {
        aguardandoInicio = false
        naoIniciou = true
    }

    fun consumirConclusao(): Boolean {
        if (!emCurso) return false
        emCurso = false
        aguardandoInicio = false
        return true
    }
}

/**
 * Liga a medição guiada ao executor.
 *
 * ## O limite de início (bloqueio B2 de Caio na PR #1719)
 *
 * `onNovoTeste` não garante que uma medição vá acontecer. `MainViewModel.reiniciarSuite` tem dois
 * `return` silenciosos alcançáveis a partir daqui:
 *
 * - `MainViewModel.kt:960` — já há execução em andamento (`compareAndSet` falha); sai só um
 *   `Timber.w`. Alcançável em cancelar-e-refazer rápido, porque `cancelar()` marca uma flag e o
 *   loop do executor só desenrola nas fronteiras de fase.
 * - `MainViewModel.kt:1003` — `interromperSpeedtestPorWifiSemInternet()`. **Wi-Fi conectado sem
 *   internet**, que é justamente o motivo mais provável de alguém abrir "descobrir o que está
 *   acontecendo".
 *
 * Nos dois casos o snapshot não muda. Sem este limite a rota `Analise` ficava exibindo "Estou
 * medindo sua conexão" para sempre — mentindo — e a única saída era a pessoa desistir e cancelar.
 * Passado [limiteParaIniciarMs] sem ver `executando`, o estado vira [EstadoAnaliseGuiada.Falhou],
 * que já tem "Tentar de novo" desenhado.
 *
 * O terceiro `return` de `reiniciarSuite` (`:994`, confirmação de rede móvel) **não** é alcançável
 * daqui: `deveSolicitarConfirmacaoRedeMovel` exige `modo != ModoSpeedtest.fast`
 * (`MainViewModel.kt:2381`) e a análise guiada usa `fast`.
 *
 * ## Por que `aguardandoInicio` também mascara o `Concluida` velho
 *
 * Entrar na rota com um resultado anterior que o motor recusou faz o snapshot ainda reportar
 * `Concluida` nos frames entre o pedido e o executor publicar `executando`. Enquanto
 * [MedicaoGuiada.aguardandoInicio] estiver ativo, o estado devolvido é `EmAndamento` — assim a
 * tela não conclui em cima da medição velha, sem precisar de um segundo guarda lá dentro.
 */
@Composable
internal fun rememberMedicaoGuiada(
    snapshot: SnapshotExecucaoSpeedtest,
    onNovoTeste: (ModoSpeedtest) -> Unit,
    onCancelarTeste: () -> Unit,
    limiteParaIniciarMs: Long = LIMITE_PARA_INICIAR_MS,
): MedicaoGuiadaUi {
    val medicao = remember { MedicaoGuiada() }
    val dispararTeste by rememberUpdatedState(onNovoTeste)
    val cancelarTeste by rememberUpdatedState(onCancelarTeste)

    LaunchedEffect(snapshot.estado) {
        if (snapshot.estado == EstadoExecucaoSpeedtest.executando) medicao.registrarInicioObservado()
    }

    LaunchedEffect(medicao.aguardandoInicio) {
        if (!medicao.aguardandoInicio) return@LaunchedEffect
        delay(limiteParaIniciarMs)
        medicao.registrarFalhaDeInicio()
    }

    val estado =
        when {
            medicao.naoIniciou -> EstadoAnaliseGuiada.Falhou(MENSAGEM_NAO_INICIOU)
            medicao.aguardandoInicio ->
                EstadoAnaliseGuiada.EmAndamento(0f, etapaEmLinguagemHumana(FaseSpeedtest.idle))
            else -> estadoAnaliseGuiada(snapshot)
        }

    return MedicaoGuiadaUi(
        contrato =
            AnaliseGuiadaContrato(
                estado = estado,
                onIniciar = {
                    // `fast` e não o modo escolhido na tela Velocidade: são jornadas diferentes, e
                    // §8.5 pede progresso curto e compreensível, não a bateria completa.
                    medicao.pedirMedicao { dispararTeste(ModoSpeedtest.fast) }
                },
                onCancelar = { medicao.cancelar { cancelarTeste() } },
            ),
        suprimeReacoesDoShell = medicao.emCurso,
        consumirConclusao = medicao::consumirConclusao,
    )
}
