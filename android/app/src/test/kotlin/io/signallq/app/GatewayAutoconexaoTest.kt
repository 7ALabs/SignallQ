package io.signallq.app

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GatewayAutoconexaoTest {
    @Test
    fun `toggle desligado nunca e elegivel mesmo com bssid batendo`() {
        assertFalse(
            bssidElegivelParaAutoconexao(
                permanecerConectado = false,
                bssidVinculado = "AA:BB:CC:DD:EE:01",
                bssidAtual = "AA:BB:CC:DD:EE:01",
            ),
        )
    }

    @Test
    fun `sem bssid vinculado nao e elegivel`() {
        assertFalse(
            bssidElegivelParaAutoconexao(
                permanecerConectado = true,
                bssidVinculado = null,
                bssidAtual = "AA:BB:CC:DD:EE:01",
            ),
        )
    }

    @Test
    fun `sem bssid atual (sem permissao ou localizacao desativada) nao e elegivel`() {
        assertFalse(
            bssidElegivelParaAutoconexao(
                permanecerConectado = true,
                bssidVinculado = "AA:BB:CC:DD:EE:01",
                bssidAtual = null,
            ),
        )
    }

    @Test
    fun `bssid atual diferente do vinculado nao e elegivel mesmo com toggle ligado`() {
        // Mesmo SSID, rede fisicamente diferente (BSSID diferente) — nunca autoconecta.
        assertFalse(
            bssidElegivelParaAutoconexao(
                permanecerConectado = true,
                bssidVinculado = "AA:BB:CC:DD:EE:01",
                bssidAtual = "AA:BB:CC:DD:EE:02",
            ),
        )
    }

    @Test
    fun `toggle ligado e bssid batendo exatamente e elegivel`() {
        assertTrue(
            bssidElegivelParaAutoconexao(
                permanecerConectado = true,
                bssidVinculado = "AA:BB:CC:DD:EE:01",
                bssidAtual = "AA:BB:CC:DD:EE:01",
            ),
        )
    }
}
