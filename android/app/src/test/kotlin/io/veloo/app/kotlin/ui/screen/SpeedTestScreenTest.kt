package io.signallq.app.ui.screen

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * #1071 (SPD-002) e #1074 (SPD-019) — caracteriza o seletor de modo (Rápido/Completo/3 testes)
 * e a guarda contra duplo clique no CTA "Iniciar teste" da tela Velocidade (estado idle).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SpeedTestScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val snapshotConectado =
        SnapshotRede.desconectado(0L).copy(estadoConexao = EstadoConexao.wifi, conectado = true)

    private val snapshotIdle =
        SnapshotExecucaoSpeedtest(
            estado = EstadoExecucaoSpeedtest.idle,
            progressoPercentual = 0,
            resultado = null,
            erroMensagem = null,
        )

    @Test
    fun `seletor de modo mostra Rapido Completo e 3 testes`() {
        composeRule.setContent {
            SignallQTheme {
                SpeedTestScreen(
                    snapshotSpeedtest = snapshotIdle,
                    snapshotRede = snapshotConectado,
                    ispInfo = null,
                    localizacaoServidor = null,
                    modoSelecionado = ModoSpeedtest.fast,
                    onModoSelecionado = {},
                    onIniciarTeste = {},
                    onCancelarTeste = {},
                    onAbrirDnsBenchmark = {},
                )
            }
        }

        composeRule.onNodeWithText("Rápido").assertIsDisplayed()
        composeRule.onNodeWithText("Completo").assertIsDisplayed()
        composeRule.onNodeWithText("3 testes").assertIsDisplayed()
    }

    @Test
    fun `duplo clique no CTA Iniciar teste dispara callback uma unica vez`() {
        val contador = AtomicInteger(0)

        composeRule.setContent {
            SignallQTheme {
                SpeedTestScreen(
                    snapshotSpeedtest = snapshotIdle,
                    snapshotRede = snapshotConectado,
                    ispInfo = null,
                    localizacaoServidor = null,
                    modoSelecionado = ModoSpeedtest.fast,
                    onModoSelecionado = {},
                    onIniciarTeste = { contador.incrementAndGet() },
                    onCancelarTeste = {},
                    onAbrirDnsBenchmark = {},
                )
            }
        }

        val cta: SemanticsNodeInteraction =
            composeRule.onNodeWithContentDescription("Iniciar teste de velocidade")
        cta.performClick()
        cta.performClick()

        assertEquals("esperava exatamente 1 chamada, teve ${contador.get()}", 1, contador.get())
    }
}
