package io.signallq.app.modogamer

import io.signallq.app.core.datastore.ModoGamerPadraoPersistido
import io.signallq.app.core.diagnostico.CatalogoJogosModoGamer
import io.signallq.app.core.diagnostico.CategoriaJogoModoGamer
import io.signallq.app.core.diagnostico.DeviceJogo
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.JogoCatalogoModoGamer
import io.signallq.app.core.diagnostico.ModoGamerEngine
import io.signallq.app.core.diagnostico.ResultadoModoGamer
import io.signallq.app.feature.diagnostico.topology.lan.NatUdpResultado
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

sealed interface SelecaoJogoModoGamer {
    val categoria: CategoriaJogoModoGamer
    val nomeExibido: String
    val officialServerHost: String?

    data class Catalogado(
        val jogo: JogoCatalogoModoGamer,
    ) : SelecaoJogoModoGamer {
        override val categoria get() = jogo.categoria
        override val nomeExibido get() = jogo.nome
        override val officialServerHost get() = jogo.officialServerHost
    }

    data class ForaDoCatalogo(
        override val categoria: CategoriaJogoModoGamer,
    ) : SelecaoJogoModoGamer {
        override val nomeExibido get() = categoria.label
        override val officialServerHost get() = null
    }
}

sealed interface ModoGamerEtapa {
    data class SelecaoJogo(
        val busca: String = "",
    ) : ModoGamerEtapa

    data class SelecaoDevice(
        val selecaoJogo: SelecaoJogoModoGamer,
    ) : ModoGamerEtapa

    data class Medindo(
        val selecaoJogo: SelecaoJogoModoGamer,
        val device: DeviceJogo,
    ) : ModoGamerEtapa

    data class Resultado(
        val selecaoJogo: SelecaoJogoModoGamer,
        val device: DeviceJogo,
        val resultado: ResultadoModoGamer,
        val salvoComoPadrao: Boolean,
        val natUdp: NatUdpResultado? = null,
    ) : ModoGamerEtapa
}

class ModoGamerViewModel(
    padraoInicial: Pair<SelecaoJogoModoGamer, DeviceJogo>?,
    private val inputAtual: () -> DiagnosticInput?,
    private val onSalvarPadrao: suspend (jogoId: String?, categoriaFallback: String?, deviceId: String) -> Unit,
) {
    private val mutableEtapa =
        MutableStateFlow<ModoGamerEtapa>(
            if (padraoInicial != null) {
                val (selecaoJogo, device) = padraoInicial
                ModoGamerEtapa.Medindo(selecaoJogo, device)
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
            ModoGamerEtapa.Medindo(selecaoJogo, device)
        }
    }

    fun voltarParaSelecaoJogo() {
        mutableEtapa.value = ModoGamerEtapa.SelecaoJogo()
    }

    fun voltarParaSelecaoDevice() {
        mutableEtapa.update { atual ->
            val selecaoJogo =
                when (atual) {
                    is ModoGamerEtapa.Medindo -> atual.selecaoJogo
                    is ModoGamerEtapa.Resultado -> atual.selecaoJogo
                    else -> return
                }
            ModoGamerEtapa.SelecaoDevice(selecaoJogo)
        }
    }

    suspend fun confirmarMedicao(
        pingEspecificoMs: Double?,
        jitterMs: Double?,
        perdaPercentual: Double?,
        natUdp: NatUdpResultado?,
    ) {
        val medindo = mutableEtapa.value as? ModoGamerEtapa.Medindo ?: return
        
        val salvarComoPadrao = true
        
        if (salvarComoPadrao) {
            val jogoId = (medindo.selecaoJogo as? SelecaoJogoModoGamer.Catalogado)?.jogo?.gameId
            val categoriaFallback = (medindo.selecaoJogo as? SelecaoJogoModoGamer.ForaDoCatalogo)?.categoria?.name
            onSalvarPadrao(jogoId, categoriaFallback, medindo.device.name)
        }
        
        val resultado = ModoGamerEngine.avaliar(medindo.selecaoJogo.categoria, medindo.device, inputAtual(), pingEspecificoMs, jitterMs, perdaPercentual)
        
        mutableEtapa.value =
            ModoGamerEtapa.Resultado(
                selecaoJogo = medindo.selecaoJogo,
                device = medindo.device,
                resultado = resultado,
                salvoComoPadrao = salvarComoPadrao,
                natUdp = natUdp,
            )
    }

    suspend fun alternarSalvarPadrao(salvarComoPadrao: Boolean) {
        val resultadoAtual = mutableEtapa.value as? ModoGamerEtapa.Resultado ?: return
        
        if (salvarComoPadrao) {
            val jogoId = (resultadoAtual.selecaoJogo as? SelecaoJogoModoGamer.Catalogado)?.jogo?.gameId
            val categoriaFallback = (resultadoAtual.selecaoJogo as? SelecaoJogoModoGamer.ForaDoCatalogo)?.categoria?.name
            onSalvarPadrao(jogoId, categoriaFallback, resultadoAtual.device.name)
        } else {
            onSalvarPadrao(null, null, resultadoAtual.device.name)
        }
        
        mutableEtapa.value = resultadoAtual.copy(salvoComoPadrao = salvarComoPadrao)
    }

    fun trocarJogoOuDevice() {
        mutableEtapa.value = ModoGamerEtapa.SelecaoJogo()
    }
}

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
