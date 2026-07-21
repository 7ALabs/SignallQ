package io.signallq.app.core.datastore

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** GH#1227 item 3/RF-A — round-trip de serialização, sem precisar de Context/DataStore. */
class ConnectionProfilePersistidoTest {

    @Test
    fun `round-trip preserva todos os campos preenchidos`() {
        val original = ConnectionProfilePersistido(
            networkId = "wifi-bssid:aa:bb:cc:dd:ee:ff",
            providerFixed = "Vivo Fibra",
            contractedDownloadMbps = 500,
            contractedUploadMbps = 250,
            city = "São Paulo",
            state = "SP",
            userConfirmed = true,
        )
        val registro = original.serializar()
        val restaurado = desserializarConnectionProfile(registro)
        assertEquals(original, restaurado)
    }

    @Test
    fun `round-trip preserva campos nulos como null, nao como string vazia`() {
        val original = ConnectionProfilePersistido(
            networkId = "wifi-ssid:MinhaRede",
            providerFixed = null,
            contractedDownloadMbps = null,
            contractedUploadMbps = null,
            city = null,
            state = null,
            userConfirmed = false,
        )
        val restaurado = desserializarConnectionProfile(original.serializar())
        assertEquals(original, restaurado)
        assertNull(restaurado?.providerFixed)
    }

    @Test
    fun `registro malformado (numero errado de campos) retorna null, nao lanca excecao`() {
        assertNull(desserializarConnectionProfile("campo1${SEP_CAMPO_CONNECTION_PROFILE}campo2"))
        assertNull(desserializarConnectionProfile(""))
    }

    @Test
    fun `multiplos registros nao colidem entre si ao serializar uma lista`() {
        val perfilA = ConnectionProfilePersistido("rede-a", "ProvedorA", 100, 50, "Cidade A", "SP", true)
        val perfilB = ConnectionProfilePersistido("rede-b", "ProvedorB", 300, 150, "Cidade B", "RJ", false)
        val serializado = listOf(perfilA, perfilB).joinToString(SEP_REGISTRO_CONNECTION_PROFILE) { it.serializar() }
        val restaurados = serializado.split(SEP_REGISTRO_CONNECTION_PROFILE).mapNotNull { desserializarConnectionProfile(it) }
        assertEquals(listOf(perfilA, perfilB), restaurados)
    }
}
