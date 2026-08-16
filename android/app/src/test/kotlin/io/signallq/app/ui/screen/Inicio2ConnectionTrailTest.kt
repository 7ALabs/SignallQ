package io.signallq.app.ui.screen

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.core.network.contracts.wifi.RedeVizinha
import io.signallq.app.core.network.contracts.wifi.SegurancaWifi
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.ui.SignallQTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Inicio2ConnectionTrailTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun `mesh provavel ou mesmo SSID sem confirmacao nao vira no da trilha`() {
        val state = Inicio2ConnectionTrailMapper.map(redeConectada(), scanMesh(), true)

        assertFalse(state.nodes.any { it.title == "Mesh" })
    }

    @Test
    fun `mesh aparece somente quando motor canonico devolve no com alta confianca`() {
        val state =
            Inicio2ConnectionTrailMapper.map(
                snapshotRede = redeConectada(),
                snapshotWifi = scanMesh(),
                temPermissaoLocalizacao = true,
                temConfirmacaoRoteadorCentral = true,
            )

        assertTrue(state.nodes.any { it.title == "Mesh" })
    }

    @Test
    fun `permissao ausente mantem trilha parcial sem afirmar mesh`() {
        val state = Inicio2ConnectionTrailMapper.map(redeConectada(), scanMesh(), false)

        assertFalse(state.nodes.any { it.title == "Mesh" })
        assertEquals("Permita redes próximas para completar a trilha.", state.supportingMessage)
    }

    @Test
    fun `font scale 2 mantem alvo acessivel e rota valida`() {
        var route: Inicio2TrailRoute? = null
        composeRule.setContent {
            SignallQTheme {
                CompositionLocalProvider(LocalDensity provides Density(1f, 2f)) {
                    Inicio2ConnectionTrail(
                        state =
                            Inicio2ConnectionTrailState(
                                nodes = listOf(Inicio2TrailNode("Equipamento", "Roteador", Inicio2TrailRoute.Equipamento)),
                                supportingMessage = null,
                            ),
                        onOpenRoute = { route = it },
                    )
                }
            }
        }

        composeRule.onNodeWithText("Trilha da conexão").assertIsDisplayed()
        composeRule
            .onNodeWithText("Equipamento")
            .assertHasClickAction()
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        assertEquals(Inicio2TrailRoute.Equipamento, route)
    }

    private fun redeConectada() =
        SnapshotRede.desconectado(0L).copy(estadoConexao = EstadoConexao.wifi, conectado = true)

    private fun scanMesh() =
        SnapshotScanWifi(
            estado = EstadoScanWifi.concluido,
            redes =
                listOf(
                    rede("50:C7:BF:00:00:01", -50),
                    rede("50:C7:BF:00:00:02", -65),
                ),
            erroMensagem = null,
        )

    private fun rede(
        bssid: String,
        rssi: Int,
    ) =
        RedeVizinha("Casa", bssid, rssi, 2412, SegurancaWifi.wpa2, null, "50C7BF")
}
