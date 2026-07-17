package io.signallq.app.ui.screen

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1076 (HOME-001) — caracteriza o card "Caminho da sua internet" (NetworkPath) no estado de
 * carregamento. `SignallQCard` empilha o conteudo num `Box` (sem `Column`), entao qualquer
 * filho direto nasce ancorado em `Alignment.TopStart` — o titulo e o texto de status final
 * ficavam sobrepostos. Trava a regressao comparando as posicoes verticais dos dois textos.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class HomeScreenNetworkPathTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `titulo e texto de status do card de rede nao se sobrepoem durante o carregamento`() {
        composeRule.setContent {
            SignallQTheme {
                HomeScreen(
                    snapshotRede = SnapshotRede.desconectado(0L).copy(estadoConexao = EstadoConexao.wifi, conectado = true),
                    snapshotSpeedtest =
                        SnapshotExecucaoSpeedtest(
                            estado = EstadoExecucaoSpeedtest.idle,
                            progressoPercentual = 0,
                            resultado = null,
                            erroMensagem = null,
                        ),
                    history = emptyList(),
                    ultimaMedicao = null,
                    // localIp nulo + estadoConexao.wifi/conectado=true => loadingLocal=true,
                    // o mesmo estado que expunha a sobreposicao no emulador (#1076).
                    localIp = null,
                    publicIp = null,
                    ispInfo = null,
                    gateways = emptyList(),
                    deviceName = "Pixel de Teste",
                    nomeUsuario = "Luiz",
                    fotoUriUsuario = null,
                    connectedNetwork = null,
                    movelSnapshot = null,
                    simsAtivos = emptyList(),
                    anatelBannerDismissed = true,
                    onDismissAnatelBanner = {},
                    onIniciarTeste = {},
                    onAbrirHistorico = {},
                    onAbrirPerfil = {},
                    onAbrirRedes = {},
                )
            }
        }

        val titulo = composeRule.onNodeWithText("CAMINHO DA SUA INTERNET").fetchSemanticsNode()
        val status = composeRule.onNodeWithText("Estamos confirmando cada etapa da sua conexão.").fetchSemanticsNode()

        // Sem sobreposicao vertical: o texto de status precisa comecar abaixo de onde o
        // titulo termina (a Row com os nos da trilha fica entre os dois).
        assertTrue(
            "titulo (bottom=${titulo.boundsInRoot.bottom}) e status (top=${status.boundsInRoot.top}) " +
                "estao sobrepostos verticalmente",
            titulo.boundsInRoot.bottom <= status.boundsInRoot.top,
        )
    }
}
