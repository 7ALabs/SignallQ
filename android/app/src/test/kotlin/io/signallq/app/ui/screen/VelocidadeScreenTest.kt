package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.SignallQTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1072 (SPD-004) — caracteriza a visibilidade das fases de execucao (latencia, download,
 * upload, concluido) na tela de overlay do speedtest (VelocidadeScreen/PillsFase).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class VelocidadeScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun snapshot(fase: FaseSpeedtest) =
        SnapshotExecucaoSpeedtest(
            estado = EstadoExecucaoSpeedtest.executando,
            progressoPercentual = 50,
            resultado = null,
            erroMensagem = null,
            faseAtual = fase,
        )

    @Test
    fun `pills de fase mostram as quatro etapas durante o download`() {
        // VelocidadeScreen tem um loop de animacao continuo (suavizacao do Mbps exibido via
        // withFrameMillis) que nunca fica idle sozinho — precisa de clock manual pro
        // setContent nao travar esperando uma composicao que nunca "termina".
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            SignallQTheme {
                VelocidadeScreen(
                    snapshot = snapshot(FaseSpeedtest.download),
                    localizacaoServidor = null,
                    ispInfo = null,
                    onCancelar = {},
                    onReiniciar = {},
                )
            }
        }

        composeRule.onNodeWithText("LATÊNCIA").assertIsDisplayed()
        // "DOWNLOAD" aparece 2x durante a fase de download: no rotulo central do gauge e
        // na pill de fase — ambos esperados, so garante que o rotulo da pill esta la.
        composeRule
            .onAllNodesWithText("DOWNLOAD")
            .assertCountEquals(2)
            .onFirst()
            .assertIsDisplayed()
        composeRule.onNodeWithText("UPLOAD").assertIsDisplayed()
        composeRule.onNodeWithText("CONCLUÍDO").assertIsDisplayed()
    }
}
