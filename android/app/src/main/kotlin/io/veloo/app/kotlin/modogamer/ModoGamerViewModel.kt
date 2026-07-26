package io.signallq.app.modogamer

import io.signallq.app.core.datastore.ModoGamerPadraoPersistido
import io.signallq.app.core.diagnostico.CatalogoJogosModoGamer
import io.signallq.app.core.diagnostico.CategoriaJogoModoGamer
import io.signallq.app.core.diagnostico.DeviceJogo
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.JogoCatalogoModoGamer
import io.signallq.app.core.diagnostico.ModoGamerEngine
import io.signallq.app.core.diagnostico.ResultadoModoGamer
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * Jogo escolhido na Etapa 1/3 — catalogado ([CatalogoJogosModoGamer], 9 jogos) ou fora do
 * catálogo (usuário escolheu a categoria genérica mais parecida na tela "Outro jogo", nunca
 * um erro — regra de produto da issue #1476).
 */
sealed interface SelecaoJogoModoGamer {
    val categoria: CategoriaJogoModoGamer
    val nomeExibido: String

    data class Catalogado(
        val jogo: JogoCatalogoModoGamer,
    ) : SelecaoJogoModoGamer {
        override val categoria get() = jogo.categoria
        override val nomeExibido get() = jogo.nome
    }

    data class ForaDoCatalogo(
        override val categoria: CategoriaJogoModoGamer,
    ) : SelecaoJogoModoGamer {
        override val nomeExibido get() = categoria.label
    }
}

/** As 4 telas do fluxo (Etapa 1/3 → 2/3 → 3/3 → resultado), mesma estrutura do protótipo
 *  #1474 (`ModoGamerJogo` → `ModoGamerDevice` → `ModoGamerConfig` → `ModoGamerResultado`). */
sealed interface ModoGamerEtapa {
    data class SelecaoJogo(
        val busca: String = "",
    ) : ModoGamerEtapa

    data class SelecaoDevice(
        val selecaoJogo: SelecaoJogoModoGamer,
    ) : ModoGamerEtapa

    data class Config(
        val selecaoJogo: SelecaoJogoModoGamer,
        val device: DeviceJogo,
    ) : ModoGamerEtapa

    data class Resultado(
        val selecaoJogo: SelecaoJogoModoGamer,
        val device: DeviceJogo,
        val resultado: ResultadoModoGamer,
        val salvoComoPadrao: Boolean,
    ) : ModoGamerEtapa
}

/**
 * ViewModel do fluxo de 3 etapas do Modo gamer (Feature #550, issue #1476): jogo → device →
 * salvar como padrão/usar uma vez → resultado. Não é `@HiltViewModel` — criado via
 * `remember{}` no Composable, mesmo padrão do [io.signallq.app.jogos.JogosViewModel] (escopo
 * do overlay, sem necessidade de sobreviver à recomposição do grafo de navegação).
 *
 * [padraoInicial] (combinação jogo+device já salva, resolvida a partir do
 * `PreferenciasAppRepository.modoGamerPadraoFlow`) pula direto pra etapa
 * [ModoGamerEtapa.Resultado] — cumpre a promessa do protótipo #1474 ("Próxima vez, o Modo
 * gamer já abre direto com {jogo}+{device}"). `null` (nunca salvou) começa em
 * [ModoGamerEtapa.SelecaoJogo].
 *
 * Reaproveita [ModoGamerEngine] (issue #1476) — nenhuma lógica de status/evidência aqui,
 * só orquestração de navegação entre etapas.
 */
class ModoGamerViewModel(
    padraoInicial: Pair<SelecaoJogoModoGamer, DeviceJogo>?,
    private val inputAtual: () -> DiagnosticInput?,
    private val onSalvarPadrao: suspend (jogoId: String?, categoriaFallback: String?, deviceId: String) -> Unit,
) {
    private val mutableEtapa =
        MutableStateFlow<ModoGamerEtapa>(
            if (padraoInicial != null) {
                val (selecaoJogo, device) = padraoInicial
                ModoGamerEtapa.Resultado(
                    selecaoJogo = selecaoJogo,
                    device = device,
                    resultado = ModoGamerEngine.avaliar(selecaoJogo.categoria, device, inputAtual()),
                    salvoComoPadrao = true,
                )
            } else {
                ModoGamerEtapa.SelecaoJogo()
            },
        )
    val etapa: StateFlow<ModoGamerEtapa> = mutableEtapa.asStateFlow()

    fun buscar(query: String) {
        mutableEtapa.update { atual -> if (atual is ModoGamerEtapa.SelecaoJogo) atual.copy(busca = query) else atual }
    }

    fun selecionarJogo(jogo: JogoCatalogoModoGamer) {
        mutableEtapa.value = ModoGamerEtapa.SelecaoDevice(SelecaoJogoModoGamer.Catalogado(jogo))
    }

    fun selecionarCategoriaFallback(categoria: CategoriaJogoModoGamer) {
        mutableEtapa.value = ModoGamerEtapa.SelecaoDevice(SelecaoJogoModoGamer.ForaDoCatalogo(categoria))
    }

    fun selecionarDevice(device: DeviceJogo) {
        mutableEtapa.update { atual ->
            val selecaoJogo = (atual as? ModoGamerEtapa.SelecaoDevice)?.selecaoJogo ?: return
            ModoGamerEtapa.Config(selecaoJogo, device)
        }
    }

    fun voltarParaSelecaoJogo() {
        mutableEtapa.value = ModoGamerEtapa.SelecaoJogo()
    }

    fun voltarParaSelecaoDevice() {
        mutableEtapa.update { atual ->
            val selecaoJogo =
                when (atual) {
                    is ModoGamerEtapa.Config -> atual.selecaoJogo
                    is ModoGamerEtapa.Resultado -> atual.selecaoJogo
                    else -> return
                }
            ModoGamerEtapa.SelecaoDevice(selecaoJogo)
        }
    }

    /** Etapa 3/3 → resultado. Persiste a combinação como padrão só quando [salvarComoPadrao]
     *  for `true` (opção "Salvar como padrão" da Etapa 3/3) — "Usar só desta vez" nunca
     *  escreve no DataStore. */
    suspend fun confirmar(salvarComoPadrao: Boolean) {
        val config = mutableEtapa.value as? ModoGamerEtapa.Config ?: return
        val resultado = ModoGamerEngine.avaliar(config.selecaoJogo.categoria, config.device, inputAtual())
        if (salvarComoPadrao) {
            val jogoId = (config.selecaoJogo as? SelecaoJogoModoGamer.Catalogado)?.jogo?.gameId
            val categoriaFallback = (config.selecaoJogo as? SelecaoJogoModoGamer.ForaDoCatalogo)?.categoria?.name
            onSalvarPadrao(jogoId, categoriaFallback, config.device.name)
        }
        mutableEtapa.value =
            ModoGamerEtapa.Resultado(
                selecaoJogo = config.selecaoJogo,
                device = config.device,
                resultado = resultado,
                salvoComoPadrao = salvarComoPadrao,
            )
    }

    /** Do resultado, volta pra Etapa 1/3 — mesmo botão "Trocar jogo ou aparelho" do
     *  protótipo, disponível tanto vindo de resultado normal quanto de padrão salvo. */
    fun trocarJogoOuDevice() {
        mutableEtapa.value = ModoGamerEtapa.SelecaoJogo()
    }
}

/**
 * Resolve o [ModoGamerPadraoPersistido] cru (strings salvas no DataStore, ver
 * `PreferenciasAppRepository.modoGamerPadraoFlow`) para o par tipado que
 * [ModoGamerViewModel] espera. Devolve `null` quando não há padrão salvo, ou quando o
 * `deviceId`/`jogoId`/`categoriaFallback` persistidos não correspondem mais a nenhum valor
 * válido (ex.: enum removido numa versão futura) — nunca lança exceção, mesmo princípio de
 * "fallback, nunca erro" da issue #1476.
 */
fun resolverPadraoModoGamer(persistido: ModoGamerPadraoPersistido?): Pair<SelecaoJogoModoGamer, DeviceJogo>? {
    if (persistido == null) return null
    val device = runCatching { DeviceJogo.valueOf(persistido.deviceId) }.getOrNull() ?: return null
    val selecaoJogo =
        persistido.jogoId?.let { CatalogoJogosModoGamer.porId(it) }?.let { SelecaoJogoModoGamer.Catalogado(it) }
            ?: persistido.categoriaFallback
                ?.let { runCatching { CategoriaJogoModoGamer.valueOf(it) }.getOrNull() }
                ?.let { SelecaoJogoModoGamer.ForaDoCatalogo(it) }
            ?: return null
    return selecaoJogo to device
}
