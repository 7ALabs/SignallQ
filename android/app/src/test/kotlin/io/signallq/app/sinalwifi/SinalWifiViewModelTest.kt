package io.signallq.app.sinalwifi

import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.signallq.app.core.diagnostico.BandaWifi
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
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
        networkId: Int = 0,
    ): WifiInfo {
        val info = mockk<WifiInfo>()
        every { info.rssi } returns rssi
        every { info.linkSpeed } returns linkSpeed
        every { info.ssid } returns ssid
        every { info.wifiStandard } returns wifiStandard
        every { info.frequency } returns frequencia
        every { info.networkId } returns networkId
        return info
    }

    private fun wifiManagerMock(
        habilitado: Boolean = true,
        connectionInfo: WifiInfo? = null,
    ): WifiManager {
        val wifiManager = mockk<WifiManager>()
        every { wifiManager.isWifiEnabled } returns habilitado
        if (connectionInfo != null) {
            every { wifiManager.connectionInfo } returns connectionInfo
        }
        return wifiManager
    }

    @Test
    fun `descarta leitura com rssi sentinela 0 e nao atualiza uiState`() =
        runTest {
            val wifiManager = wifiManagerMock(connectionInfo = wifiInfoMock(rssi = 0))
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(20)
            job.cancelAndJoin()

            assertNull(viewModel.uiState.value.rssiAtual)
        }

    @Test
    fun `descarta leitura com rssi sentinela -127 e nao atualiza uiState`() =
        runTest {
            val wifiManager = wifiManagerMock(connectionInfo = wifiInfoMock(rssi = -127))
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(20)
            job.cancelAndJoin()

            assertNull(viewModel.uiState.value.rssiAtual)
        }

    @Test
    fun `padrao wifi e suportaMuMimo sao calculados na 1a leitura valida e preservados depois`() =
        runTest {
            // Wi-Fi 5 na 1a chamada, Wi-Fi 7 na 2a -- se o cálculo fosse refeito a cada
            // amostragem, o teste pegaria a regressão (padrão mudando de leitura em leitura).
            val info = wifiInfoMock(rssi = -55, linkSpeed = 400, wifiStandard = 5)
            every { info.wifiStandard } returnsMany listOf(5, 8)
            val wifiManager = wifiManagerMock(connectionInfo = info)
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
            assertEquals(true, estado.conectado)
            assertEquals(true, estado.wifiHabilitado)
        }

    // Issue #1668 — antes do 1o ciclo de amostragem completar, a tela não pode presumir
    // "sem conexão"; precisa de um sinal explícito de que nenhuma leitura aconteceu ainda.
    @Test
    fun `estado inicial nao esta amostrado`() {
        val wifiManager = mockk<WifiManager>()
        val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true })

        assertEquals(false, viewModel.uiState.value.amostrado)
    }

    @Test
    fun `iniciarAmostragem retorna sem entrar no loop quando permissao nao concedida`() =
        runTest {
            val wifiManager = mockk<WifiManager>()
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { false })

            viewModel.iniciarAmostragem()

            verify(exactly = 0) { wifiManager.connectionInfo }
            verify(exactly = 0) { wifiManager.isWifiEnabled }
            assertEquals(SinalWifiUiState(permissaoConcedida = false), viewModel.uiState.value)
        }

    // Issue #1668 — critério de aceite "Wi-Fi desligado tem ação útil": a tela primeiro
    // precisa SABER que o Wi-Fi está desligado, em vez de ficar presa em "aguardando leitura".
    @Test
    fun `wifi desligado marca wifiHabilitado false e zera leitura anterior, sem chamar connectionInfo`() =
        runTest {
            val wifiManager = wifiManagerMock(habilitado = false)
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(20)
            job.cancelAndJoin()

            verify(exactly = 0) { wifiManager.connectionInfo }
            val estado = viewModel.uiState.value
            assertEquals(false, estado.wifiHabilitado)
            assertNull(estado.rssiAtual)
            assertEquals(false, estado.conectado)
        }

    // Wi-Fi religado depois de desligado: a leitura anterior (de uma possível rede diferente)
    // não pode vazar para o novo ciclo -- este teste cobre a transição desligado -> ligado.
    @Test
    fun `wifi religado apos desligado retoma amostragem normalmente`() =
        runTest {
            val wifiManager = mockk<WifiManager>()
            every { wifiManager.isWifiEnabled } returnsMany listOf(false, true, true, true)
            every { wifiManager.connectionInfo } returns wifiInfoMock(rssi = -60)
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(20)
            job.cancelAndJoin()

            val estado = viewModel.uiState.value
            assertEquals(true, estado.wifiHabilitado)
            assertEquals(-60, estado.rssiAtual)
        }

    // Issue #1668 — antes desta mudança, `networkId` não-associado era ignorado por completo
    // (só o sentinela de RSSI era tratado); desconectar de uma rede sem desligar o Wi-Fi deixava
    // a última leitura "congelada" na tela pra sempre.
    @Test
    fun `sem rede associada marca conectado false e zera leitura anterior`() =
        runTest {
            val wifiManager = wifiManagerMock(connectionInfo = wifiInfoMock(rssi = -60, networkId = -1))
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(20)
            job.cancelAndJoin()

            val estado = viewModel.uiState.value
            assertEquals(true, estado.wifiHabilitado)
            assertEquals(false, estado.conectado)
            assertEquals(true, estado.amostrado)
            assertNull(estado.rssiAtual)
            assertNull(estado.padraoWifi)
        }

    @Test
    fun `banda 2_4GHz e derivada da frequencia abaixo de 3000 MHz`() =
        runTest {
            val wifiManager = wifiManagerMock(connectionInfo = wifiInfoMock(rssi = -55, frequencia = 2437))
            val viewModel = SinalWifiViewModel(wifiManager, permissaoConcedida = { true }, intervaloAmostragemMs = 5)

            val job = launch { viewModel.iniciarAmostragem() }
            advanceTimeBy(10)
            job.cancelAndJoin()

            assertEquals(BandaWifi.ghz24, viewModel.uiState.value.banda)
        }

    // Issue #1668 — decisão de produto (Luiz, 2026-08-19): abaixo do Android 10 o app ainda pode
    // ligar o Wi-Fi direto (setWifiEnabled funciona); a partir do 10 (API 29+, restrição de
    // plataforma) o caminho vira o painel do sistema (Settings.Panel.ACTION_WIFI), que resolve
    // sem tirar o usuário do app.
    @Test
    fun `intentAcaoLigarWifi retorna null abaixo do Android 10`() {
        assertNull(intentAcaoLigarWifi(Build.VERSION_CODES.P))
    }

    @Test
    fun `intentAcaoLigarWifi retorna intent do painel do sistema a partir do Android 10`() {
        val intent = intentAcaoLigarWifi(Build.VERSION_CODES.Q)
        assertNotNull(intent)
        assertEquals(android.provider.Settings.Panel.ACTION_WIFI, intent!!.action)
    }
}
