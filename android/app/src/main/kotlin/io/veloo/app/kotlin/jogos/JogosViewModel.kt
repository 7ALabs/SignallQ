package io.signallq.app.jogos

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.feature.speedtest.PingExecutor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Mensagens da Etapa 4 (progresso), na ordem exata do spec — sem jargão técnico. */
val MENSAGENS_PROGRESSO_JOGOS =
    listOf(
        "Verificando a conexão",
        "Medindo o tempo de resposta",
        "Avaliando estabilidade",
        "Preparando o resultado",
    )

private const val INTERVALO_MENSAGEM_MS = 3_000L
private const val AMOSTRAS_TESTE = 24

sealed interface JogosEtapa {
    data object SelecaoPlataforma : JogosEtapa

    data class SelecaoJogo(
        val plataforma: Plataforma,
        val busca: String = "",
    ) : JogosEtapa

    data class Confirmacao(
        val plataforma: Plataforma,
        val jogo: JogoCatalogo,
    ) : JogosEtapa

    data class Progresso(
        val plataforma: Plataforma,
        val jogo: JogoCatalogo,
        val mensagemIndex: Int,
    ) : JogosEtapa

    data class Resultado(
        val resultado: ResultadoTesteJogo,
    ) : JogosEtapa

    data class Erro(
        val plataforma: Plataforma,
        val jogo: JogoCatalogo,
        val mensagem: String,
    ) : JogosEtapa
}

/**
 * ViewModel do fluxo de 5 etapas da tela Jogos (GH#935). Nao e @HiltViewModel — criado via
 * remember{} no Composable, mesmo padrao do [io.signallq.app.ui.screen.PingScreenViewModel]
 * (escopo do overlay, sem necessidade de sobreviver a recomposicao do grafo de navegacao).
 */
class JogosViewModel(
    private val probeUrl: String,
    private val tipoConexaoAtual: () -> EstadoConexao,
    private val wifiLinkSnapshot: () -> WifiLinkSnapshot?,
) {
    private val mutableEtapa = MutableStateFlow<JogosEtapa>(JogosEtapa.SelecaoPlataforma)
    val etapa: StateFlow<JogosEtapa> = mutableEtapa.asStateFlow()

    fun selecionarPlataforma(plataforma: Plataforma) {
        mutableEtapa.value = JogosEtapa.SelecaoJogo(plataforma)
    }

    fun buscar(query: String) {
        mutableEtapa.update { atual ->
            if (atual is JogosEtapa.SelecaoJogo) atual.copy(busca = query) else atual
        }
    }

    fun selecionarJogo(jogo: JogoCatalogo) {
        mutableEtapa.update { atual ->
            val plataforma = (atual as? JogosEtapa.SelecaoJogo)?.plataforma ?: return
            JogosEtapa.Confirmacao(plataforma, jogo)
        }
    }

    fun voltarParaSelecaoPlataforma() {
        mutableEtapa.value = JogosEtapa.SelecaoPlataforma
    }

    fun voltarParaSelecaoJogo() {
        mutableEtapa.update { atual ->
            val plataforma =
                when (atual) {
                    is JogosEtapa.Confirmacao -> atual.plataforma
                    is JogosEtapa.Progresso -> atual.plataforma
                    is JogosEtapa.Erro -> atual.plataforma
                    else -> return
                }
            JogosEtapa.SelecaoJogo(plataforma)
        }
    }

    fun escolherOutroJogo() {
        mutableEtapa.update { atual ->
            val plataforma =
                when (atual) {
                    is JogosEtapa.Resultado -> atual.resultado.plataforma
                    is JogosEtapa.Erro -> atual.plataforma
                    else -> return
                }
            JogosEtapa.SelecaoJogo(plataforma)
        }
    }

    fun testarNovamente() {
        val atual = mutableEtapa.value
        val (plataforma, jogo) =
            when (atual) {
                is JogosEtapa.Resultado -> atual.resultado.plataforma to atual.resultado.jogo
                is JogosEtapa.Erro -> atual.plataforma to atual.jogo
                else -> return
            }
        mutableEtapa.value = JogosEtapa.Confirmacao(plataforma, jogo)
    }

    /** Etapa 3→4→5: nunca inicia sozinho — so chamado pelo botao "Testar conexão para {jogo}". */
    suspend fun iniciarTeste() {
        val confirmacao = mutableEtapa.value as? JogosEtapa.Confirmacao ?: return
        val (plataforma, jogo) = confirmacao

        mutableEtapa.value = JogosEtapa.Progresso(plataforma, jogo, mensagemIndex = 0)

        try {
            val medicao =
                coroutineScope {
                    val medicaoDeferred =
                        async(Dispatchers.IO) {
                            PingExecutor(targetUrl = probeUrl).executar(count = AMOSTRAS_TESTE)
                        }
                    for (indice in 1 until MENSAGENS_PROGRESSO_JOGOS.size) {
                        delay(INTERVALO_MENSAGEM_MS)
                        mutableEtapa.update { atual ->
                            if (atual is JogosEtapa.Progresso) atual.copy(mensagemIndex = indice) else atual
                        }
                    }
                    medicaoDeferred.await()
                }

            val resultado =
                JogoConexaoEngine.avaliar(
                    jogo = jogo,
                    plataforma = plataforma,
                    medicao = medicao,
                    tipoConexaoAtual = tipoConexaoAtual(),
                    wifiLinkSnapshot = wifiLinkSnapshot(),
                )
            mutableEtapa.value = JogosEtapa.Resultado(resultado)
        } catch (e: Exception) {
            mutableEtapa.value =
                JogosEtapa.Erro(
                    plataforma = plataforma,
                    jogo = jogo,
                    mensagem = "Não foi possível testar a conexão agora. Verifique sua internet e tente novamente.",
                )
        }
    }
}
