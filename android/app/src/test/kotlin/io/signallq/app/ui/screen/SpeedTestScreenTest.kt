package io.signallq.app.ui.screen

import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.signallq.app.ads.AdSlot
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.ui.ads.NativeAdIneligibleReason
import io.signallq.app.ui.ads.NativeAdLoadState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.concurrent.atomic.AtomicInteger

/**
 * #1071 (SPD-002) e #1074 (SPD-019) — caracterizava o seletor de modo (Rápido/Completo/3 testes)
 * e a guarda contra duplo clique no CTA "Iniciar teste" da tela Velocidade (estado idle).
 *
 * GH#1737 (Task 2.0.10a, épico #1647) — o seletor manual de modo (`ModeSelector`, com as opções
 * "Rápido"/"Completo"/"3 testes") foi removido: o modo agora é decidido automaticamente por tipo
 * de rede em AppShell, antes de chamar `onIniciarTeste`. `SpeedTestScreen` não recebe mais
 * `modoSelecionado`/`onModoSelecionado`. O primeiro teste abaixo caracteriza a ausência do
 * seletor (trava contra reintrodução acidental); a guarda de duplo clique continua válida e
 * intacta.
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
    fun `GH1737 nao existe mais seletor manual de modo Rapido Completo ou 3 testes`() {
        composeRule.setContent {
            SignallQTheme {
                SpeedTestScreen(
                    snapshotSpeedtest = snapshotIdle,
                    snapshotRede = snapshotConectado,
                    ispInfo = null,
                    localizacaoServidor = null,
                    onIniciarTeste = {},
                    onCancelarTeste = {},
                    onAbrirDnsBenchmark = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Modo do teste").assertDoesNotExist()
        composeRule.onNodeWithText("3 testes").assertDoesNotExist()
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

    // =========================================================================
    // GH#1785 — migração do último call site restante de rememberNativeAd() (legado) pra
    // rememberNativeAdState() (contrato tipado). eligibilidadeAnuncioVelocidade é o mapeamento
    // puro entre o único sinal que a tela recebe de fora (adsEnabled) e NativeAdEligibility --
    // mesmo padrão de eligibilidadeAnuncioResultado (ResultadoVelocidadeScreenTest).
    // =========================================================================

    @Test
    fun `eligibilidadeAnuncioVelocidade com adsEnabled true habilita flag e consentimento e assume online`() {
        val eligibility = eligibilidadeAnuncioVelocidade(adsEnabled = true)

        assertEquals(AdSlot.VELOCIDADE, eligibility.slot)
        assertTrue(eligibility.flagEnabled)
        assertTrue(eligibility.canRequestAds)
        assertTrue(eligibility.online)
        assertTrue(eligibility.canLoad)
        assertEquals(NativeAdLoadState.Loading, eligibility.initialState())
    }

    @Test
    fun `eligibilidadeAnuncioVelocidade com adsEnabled false desabilita flag e consentimento`() {
        val eligibility = eligibilidadeAnuncioVelocidade(adsEnabled = false)

        assertFalse(eligibility.flagEnabled)
        assertFalse(eligibility.canRequestAds)
        assertFalse(eligibility.canLoad)
        assertEquals(
            NativeAdLoadState.Ineligible(NativeAdIneligibleReason.FlagDisabled),
            eligibility.initialState(),
        )
    }

    @Test
    fun `eligibilidadeAnuncioVelocidade sempre usa o slot VELOCIDADE`() {
        assertEquals(AdSlot.VELOCIDADE, eligibilidadeAnuncioVelocidade(adsEnabled = true).slot)
        assertEquals(AdSlot.VELOCIDADE, eligibilidadeAnuncioVelocidade(adsEnabled = false).slot)
    }
}
