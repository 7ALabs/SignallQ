package io.signallq.app.ui.screen

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import io.signallq.app.core.network.connectivity.ConnectivityDiagnosisEngine
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
// O que a extração cobre e o que não cobre — três camadas, e só duas estão fechadas:
//
// 1. **transições de estado** — testadas (`AppShellMedicaoGuiadaTest`, snapshot dirigido no tempo);
// 2. **as regras das leituras do shell** — `deveMostrarOverlayVelocidade` e
//    `deveTratarBackDeErroDoSpeedtest`, testadas como função pura (ressalva RS7 de Caio: eu tinha
//    descartado essa cobertura cedo demais dizendo "exige compor o AppShell", o que só vale para o
//    wiring, não para a regra);
// 3. **os call sites** — descobertos. Apagar a chamada de `deveMostrarOverlayVelocidade` dentro de
//    `AppShell.kt` continua passando na suíte inteira. Isso sim exige compor o `AppShell`, que tem
//    30+ parâmetros e nenhum harness, e segue declarado como resíduo — não como resolvido.
//
// ## Contrato de ciclo de vida (ressalva RS11)
//
// `rememberMedicaoGuiada` mora no `AppShell`, que nunca sai de composição — então o holder
// **sobrevive** ao fechamento do overlay guiado. Hoje isso está fechado porque toda saída da rota
// passa por `voltarUmPasso` → `onCancelar`, mas por convenção de call site, não por construção.
//
// Um caminho de saída novo que não cancele (deep link, `onIrParaHome` alcançável da rota Analise)
// deixa `suprimeReacoesDoShell = true` para sempre, e o app perde a tela de resultado do speedtest
// até o processo morrer. Um `DisposableEffect` na rota **não** resolve: ele dispararia também na
// transição normal para a conclusão, cancelando o executor logo depois de a medição terminar.
// Quem acrescentar saída nova é responsável por cancelar.

/**
 * Folga sobre o teto do pré-voo antes de declarar que a medição não começou.
 *
 * A primeira versão desta constante era `8_000L` literal — e **empatava** com o orçamento do
 * pré-voo que ela deveria estar esperando (bloqueio B6 de Caio na PR #1719). Antes de publicar
 * `executando`, `MainViewModel.reiniciarSuite` roda `interromperSpeedtestPorWifiSemInternet()`, que
 * chama o `ConnectivityDiagnosisEngine` com [ConnectivityDiagnosisEngine.GLOBAL_TIMEOUT_MS_DEFAULT]
 * — 8000 ms — e ainda soma `existeRedeWifiAtiva()` (IPC de binder), uma escrita no Room e
 * `garantirPerfilRede()`. O teto real é estritamente maior que 8000 ms.
 *
 * O efeito, executado por Caio: em Wi-Fi (o caso majoritário, e justamente a população que abre
 * "descobrir o que está acontecendo"), a pessoa via "Não consegui começar a análise" e a tela então
 * voltava sozinha para "Estou medindo sua conexão". Uma espera eterna tinha virado falsa falha.
 *
 * Por isso o limite é **derivado**, não escolhido: o acoplamento entre os dois números fica visível
 * no código em vez de existir por acidente entre dois literais iguais em módulos diferentes.
 */
internal const val FOLGA_APOS_PRE_VOO_MS = 7_000L

/** Quanto esperar o executor sair do lugar antes de declarar que a medição não começou. */
internal const val LIMITE_PARA_INICIAR_MS =
    ConnectivityDiagnosisEngine.GLOBAL_TIMEOUT_MS_DEFAULT + FOLGA_APOS_PRE_VOO_MS

internal const val MENSAGEM_NAO_INICIOU =
    "Não consegui começar a análise. Verifique se você continua conectado."

/**
 * O overlay de execução do speedtest em tela cheia deve aparecer?
 *
 * Existe como função pura por causa da ressalva RS7 de Caio na PR #1719: apagar a supressão do
 * `AnimatedVisibility` dentro de `AppShell.kt` sobrevivia à suíte inteira do `:app`, e a resposta
 * "exige compor o `AppShell`" só vale para testar o **wiring** — a **regra** sai barato daqui.
 *
 * O que continua descoberto por teste é alguém parar de *chamar* esta função. Isso é o call site,
 * não a regra, e segue declarado como resíduo.
 */
internal fun deveMostrarOverlayVelocidade(
    suprimeReacoesDoShell: Boolean,
    estado: EstadoExecucaoSpeedtest,
): Boolean =
    !suprimeReacoesDoShell &&
        (estado == EstadoExecucaoSpeedtest.executando || estado == EstadoExecucaoSpeedtest.erro)

/**
 * O `BackHandler` que descarta a tela de erro do speedtest (GH#374) deve estar ativo?
 *
 * Durante a medição guiada, não: o `VelocidadeScreen` nem compõe, então este handler descartaria o
 * erro por baixo de uma tela que mostra o próprio estado de falha, e o back do usuário não voltaria
 * ao roteiro de perguntas como ele espera. Mesma justificativa de [deveMostrarOverlayVelocidade]
 * para existir como função pura.
 */
internal fun deveTratarBackDeErroDoSpeedtest(
    suprimeReacoesDoShell: Boolean,
    estado: EstadoExecucaoSpeedtest,
): Boolean = !suprimeReacoesDoShell && estado == EstadoExecucaoSpeedtest.erro

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

    /**
     * Cancela a medição guiada.
     *
     * Corrida conhecida e aceita (ressalva RS14 de Caio na PR #1719): `emCurso` zera aqui, mas o
     * executor só desenrola na fronteira de fase. Se ele alcançar `concluido` antes de ver o
     * `cancelFlag`, `consumirConclusao()` devolve `false` e o shell empilha
     * `Overlay.ResultadoVelocidade` por cima do fluxo guiado, depois de a pessoa ter cancelado.
     *
     * A janela é estreita e o resultado não é perdido nem inventado — a pessoa vê a tela de
     * resultado de uma medição que de fato aconteceu. Fechá-la exigiria o executor distinguir
     * "cancelado" de "concluído" no snapshot, que é mudança de contrato dele, não desta fatia.
     */
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

    /**
     * Consome a conclusão. Limpa os **três** campos — `naoIniciou` inclusive.
     *
     * A primeira versão deixava `naoIniciou` de pé (bloqueio B8 de Caio na PR #1719), quebrando o
     * invariante `naoIniciou ⇒ emCurso`. O resultado, executado por ele: com a falsa falha ativa, a
     * medição concluía de verdade, `consumirConclusao()` devolvia `true` (então o shell **engolia**
     * o `Overlay.ResultadoVelocidade`), e a rota ficava travada em "Não consegui começar a análise"
     * — `medicaoObservadaEmCurso` nunca via `Concluida`, então §8.6 nunca chegava. Resultado
     * completo em mãos e nenhuma tela para mostrá-lo, com saída só por back ou medindo tudo de novo.
     */
    fun consumirConclusao(): Boolean {
        if (!emCurso) return false
        emCurso = false
        aguardandoInicio = false
        naoIniciou = false
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

    // UM efeito, com chave nos dois valores — não dois efeitos separados.
    //
    // A primeira versão usava `LaunchedEffect(snapshot.estado)` para observar o início, o que é um
    // detector de **transição** fazendo o papel de detector de **estado** (bloqueio B7 de Caio na
    // PR #1719). Se o executor JÁ estivesse `executando` no momento do pedido, a chave não mudava,
    // o efeito não reexecutava, `registrarInicioObservado()` nunca era chamado — e o limite
    // disparava em cima de uma medição correndo. Caio executou o cenário: medição a 50% de
    // progresso com a tela dizendo "não consegui começar".
    //
    // Esse caminho é alcançável justamente pelo cancelar-e-refazer rápido que o KDoc abaixo
    // documenta: `cancelar()` marca uma flag, o loop só desenrola na fronteira de fase, e
    // `execucaoSpeedtestEmAndamento` só é liberado no `finally` — então `reiniciarSuite` retorna em
    // `:960` E o snapshot já está `executando`.
    //
    // Nivelado por valor e rearmado a cada mudança de estado: a checagem depois do `delay` fecha a
    // corrida de o executor começar durante a espera.
    LaunchedEffect(medicao.aguardandoInicio, snapshot.estado) {
        if (!medicao.aguardandoInicio) return@LaunchedEffect
        if (snapshot.estado == EstadoExecucaoSpeedtest.executando) {
            medicao.registrarInicioObservado()
            return@LaunchedEffect
        }
        delay(limiteParaIniciarMs)
        if (snapshot.estado != EstadoExecucaoSpeedtest.executando) medicao.registrarFalhaDeInicio()
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
