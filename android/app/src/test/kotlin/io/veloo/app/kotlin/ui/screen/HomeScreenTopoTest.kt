package io.signallq.app.ui.screen

import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * #1069 (HOME-002) — caracteriza o avatar do TopAppBar na tela Início.
 * #1086 — os pills DNS/Ping/Diagnóstico foram removidos da Home de vez (viraram overlays/
 * Ferramentas, não fazem mais parte da tela Início); trava a regressão do pill DNS não voltar.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = "w411dp-h891dp")
class HomeScreenTopoTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `avatar do perfil aparece e pill DNS nao aparece mais na tela Inicio`() {
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

        composeRule.onNodeWithContentDescription("Foto de perfil de Luiz").assertIsDisplayed().assertHasClickAction()
        assertThrows(AssertionError::class.java) {
            composeRule.onNodeWithText("DNS").assertIsDisplayed()
        }
    }
}
