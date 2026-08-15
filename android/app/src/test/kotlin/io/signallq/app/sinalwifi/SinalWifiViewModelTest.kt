package io.signallq.app.sinalwifi

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes da amostragem de RSSI/PHY/padrão Wi-Fi da tela "Sinal WiFi" (GH#1201). SDK 34 (via
 * Robolectric) é necessário porque `calcularPadraoWifi` só lê `WifiInfo.wifiStandard` a partir
 * da API 30 (Build.VERSION_CODES.R) -- sem Robolectric, `Build.VERSION.SDK_INT` fica 0 em teste
 * JVM puro e o padrão Wi-Fi nunca seria calculado.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SinalWifiViewModelTest {
    private fun wifiInfoMock(
        rssi: Int,
        linkSpeed: Int = 100,
        ssid: String = "\"CasaWifi\"",
        wifiStandard: Int = 5,
        frequencia: Int = 5180,
    ): WifiInfo {
        val info = mockk<WifiInfo>()
        every { info.rssi } returns rssi
        every { info.linkSpeed } returns linkSpeed
        every { info.ssid } returns ssid
        every { info.wifiStandard } returns wifiStandard
        every { info.frequency } returns frequencia
        return info
    }

    @Test
    fun `descarta leitura com rssi sentinela 0 e nao atualiza uiState`() =
        runTest {
            val wifiManager = mockk<WifiManager>()
            every { wifiManager.connectionInfo } returns wifiInfoMock(rssi = 0)
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(20)
            job.cancelAndJoin()

            assertNull(viewModel.uiState.value.rssiAtual)
        }

    @Test
    fun `descarta leitura com rssi sentinela -127 e nao atualiza uiState`() =
        runTest {
            val wifiManager = mockk<WifiManager>()
            every { wifiManager.connectionInfo } returns wifiInfoMock(rssi = -127)
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(20)
            job.cancelAndJoin()

            assertNull(viewModel.uiState.value.rssiAtual)
        }

    @Test
    fun `padrao wifi e suportaMuMimo sao calculados na 1a leitura valida e preservados depois`() =
        runTest {
            val wifiManager = mockk<WifiManager>()
            // Wi-Fi 5 na 1a chamada, Wi-Fi 7 na 2a -- se o cálculo fosse refeito a cada
            // amostragem, o teste pegaria a regressão (padrão mudando de leitura em leitura).
            val info = wifiInfoMock(rssi = -55, linkSpeed = 400, wifiStandard = 5)
            every { info.wifiStandard } returnsMany listOf(5, 8)
            every { wifiManager.connectionInfo } returns info
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(17) // >= 3 amostragens de 5ms
            job.cancelAndJoin()

            val estado = viewModel.uiState.value
            assertEquals(-55, estado.rssiAtual)
            assertEquals(400, estado.linkSpeedMbps)
            assertEquals("CasaWifi", estado.ssid)
            assertEquals("Wi-Fi 5 (ac)", estado.padraoWifi)
            assertEquals(true, estado.suportaMuMimo)
        }

    @Test
    fun `iniciarAmostragem retorna sem entrar no loop quando permissao nao concedida`() =
        runTest {
            val wifiManager = mockk<WifiManager>()
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { false })

            viewModel.iniciarAmostragem()

            verify(exactly = 0) { wifiManager.connectionInfo }
            assertEquals(SinalWifiUiState(permissaoConcedida = false), viewModel.uiState.value)
        }
}
