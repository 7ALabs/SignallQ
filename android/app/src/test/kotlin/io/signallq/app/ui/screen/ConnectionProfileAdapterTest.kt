package io.signallq.app.ui.screen

import io.signallq.app.core.datastore.ConnectionProfilePersistido
import io.signallq.app.ui.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConnectionProfileAdapterTest {
    private val persistido =
        ConnectionProfilePersistido(
            networkId = "wifi-bssid:aa:bb:cc:dd:ee:ff",
            providerFixed = "Vivo Fibra",
            contractedDownloadMbps = 500,
            contractedUploadMbps = 250,
            city = "São Paulo",
            state = "SP",
            userConfirmed = true,
        )

    @Test
    fun `paraConnectionProfile preserva todos os campos`() {
        val dominio = persistido.paraConnectionProfile()
        assertEquals(persistido.networkId, dominio.networkId)
        assertEquals(persistido.providerFixed, dominio.providerFixed)
        assertEquals(persistido.contractedDownloadMbps, dominio.contractedDownloadMbps)
        assertEquals(persistido.contractedUploadMbps, dominio.contractedUploadMbps)
        assertEquals(persistido.city, dominio.city)
        assertEquals(persistido.state, dominio.state)
        assertEquals(persistido.userConfirmed, dominio.userConfirmed)
    }

    @Test
    fun `round-trip persistido menos-dominio-menos-persistido preserva os dados`() {
        val restaurado = persistido.paraConnectionProfile().paraPersistido()
        assertEquals(persistido, restaurado)
    }

    @Test
    fun `wifi resolve por bssid quando disponivel`() {
        val id = resolverNetworkIdAtual(ConnectionType.WIFI, ssid = "MinhaRede", bssid = "AA:BB:CC:DD:EE:FF", operadoraMovelAtiva = null)
        assertEquals("wifi-bssid:aa:bb:cc:dd:ee:ff", id)
    }

    @Test
    fun `wifi sem bssid cai pro ssid`() {
        val id = resolverNetworkIdAtual(ConnectionType.WIFI, ssid = "MinhaRede", bssid = null, operadoraMovelAtiva = null)
        assertEquals("wifi-ssid:MinhaRede", id)
    }

    @Test
    fun `movel resolve pela operadora ativa`() {
        val id = resolverNetworkIdAtual(ConnectionType.MOBILE, ssid = null, bssid = null, operadoraMovelAtiva = "Claro")
        assertEquals("movel:Claro", id)
    }

    @Test
    fun `ethernet nao tem sinal estavel -- retorna null`() {
        assertNull(resolverNetworkIdAtual(ConnectionType.ETHERNET, ssid = null, bssid = null, operadoraMovelAtiva = null))
    }

    @Test
    fun `desconhecido retorna null`() {
        assertNull(resolverNetworkIdAtual(ConnectionType.UNKNOWN, ssid = "qualquer", bssid = "qualquer", operadoraMovelAtiva = "qualquer"))
    }
}
