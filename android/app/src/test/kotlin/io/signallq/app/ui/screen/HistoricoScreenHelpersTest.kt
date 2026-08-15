package io.signallq.app.ui.screen

import io.signallq.app.core.database.MedicaoEntity
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * GH#1027 — banda Wi-Fi (2.4/5GHz) exibida no detalhe do histórico quando capturada
 * no momento da medição. Medições antigas (bandaWifi == null) continuam mostrando
 * apenas "Wi-Fi", sem sufixo.
 */
class HistoricoScreenHelpersTest {
    private fun medicao(
        connectionType: String,
        bandaWifi: String? = null,
    ) =
        MedicaoEntity(
            id = "id-1",
            timestampEpochMs = 1_700_000_000_000L,
            connectionType = connectionType,
            connectionTypeStart = null,
            connectionTypeEnd = null,
            contaminado = false,
            speedtestMode = null,
            specVersion = null,
            downloadMbps = null,
            uploadMbps = null,
            latencyMs = null,
            jitterMs = null,
            perdaPercentual = null,
            bufferbloatMs = null,
            packetLossSource = null,
            vereditoStreaming = null,
            vereditoGamer = null,
            vereditoVideoChamada = null,
            gargaloPrimario = null,
            bandaWifi = bandaWifi,
        )

    @Test
    fun `tipoLabel wifi com banda ghz5 mostra sufixo 5GHz`() {
        assertEquals("Wi-Fi · 5GHz", tipoLabel(medicao("wifi", bandaWifi = "ghz5")))
    }

    @Test
    fun `tipoLabel wifi com banda ghz24 mostra sufixo 2 4GHz`() {
        assertEquals("Wi-Fi · 2.4GHz", tipoLabel(medicao("wifi", bandaWifi = "ghz24")))
    }

    @Test
    fun `tipoLabel wifi sem bandaWifi (medicao antiga) mostra so Wi-Fi`() {
        assertEquals("Wi-Fi", tipoLabel(medicao("wifi", bandaWifi = null)))
    }

    @Test
    fun `tipoLabel celular ignora bandaWifi mesmo se presente por engano`() {
        assertEquals("Celular", tipoLabel(medicao("movel", bandaWifi = "ghz5")))
    }

    @Test
    fun `tipoLabel cabo nao mostra banda`() {
        assertEquals("Cabo", tipoLabel(medicao("ethernet", bandaWifi = "ghz5")))
    }

    @Test
    fun `bandaWifiSufixo mapeia ghz24 e ghz5 e retorna vazio para o resto`() {
        assertEquals(" · 2.4GHz", bandaWifiSufixo("ghz24"))
        assertEquals(" · 5GHz", bandaWifiSufixo("ghz5"))
        assertEquals("", bandaWifiSufixo(null))
        assertEquals("", bandaWifiSufixo("desconhecida"))
    }
}
