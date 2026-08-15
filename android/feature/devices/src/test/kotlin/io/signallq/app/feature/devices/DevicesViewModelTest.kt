package io.signallq.app.feature.devices

import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Testes unitarios de logica pura do DevicesViewModel.
 *
 * Nota: testes de integracao com Room (snapshotDispositivos, apelidos) ficam
 * no androidTest via Robolectric, pois SignallQDatabase e uma classe abstrata Room
 * que requer o runtime Android para ser instanciada em testes JVM.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DevicesViewModelTest {

    /** Helper: cria um DispositivoRede minimo para teste. */
    private fun dispositivo(
        ip: String? = "192.168.1.1",
        mac: String? = null,
        nome: String = "Teste",
    ) = DispositivoRede(
        id = ip ?: nome,
        ip = ip,
        mac = mac,
        nomeExibicao = nome,
        fonteNome = "test",
    )

    // Instancia auxiliar para testar o metodo internal sem Hilt/Room
    private val helper = DispositivosIdentidadeHelper

    @Test
    fun `identidade retorna null para dispositivo com MAC`() {
        val d = dispositivo(mac = "AA:BB:CC:DD:EE:FF")
        assertNull(helper.identidadeEstavelDispositivo(d))
    }

    @Test
    fun `identidade retorna ipnome para dispositivo sem MAC com IP`() {
        val d = dispositivo(ip = "192.168.1.50", mac = null, nome = "Smart TV")
        assertEquals("ipnome:192.168.1.50:smart tv", helper.identidadeEstavelDispositivo(d))
    }

    @Test
    fun `identidade normaliza nome em lowercase e trim`() {
        val d = dispositivo(ip = "10.0.0.1", mac = null, nome = "  CHROMECAST  ")
        assertEquals("ipnome:10.0.0.1:chromecast", helper.identidadeEstavelDispositivo(d))
    }

    @Test
    fun `identidade retorna null para dispositivo sem MAC e sem IP`() {
        val d = dispositivo(ip = null, mac = null, nome = "Fantasma")
        assertNull(helper.identidadeEstavelDispositivo(d))
    }

    @Test
    fun `identidade com mesmo IP e nome gera mesma string`() {
        val d1 = dispositivo(ip = "192.168.1.99", mac = null, nome = "Impressora")
        val d2 = dispositivo(ip = "192.168.1.99", mac = null, nome = "Impressora")
        assertEquals(helper.identidadeEstavelDispositivo(d1), helper.identidadeEstavelDispositivo(d2))
    }

    @Test
    fun `identidade com IPs diferentes gera strings diferentes`() {
        val d1 = dispositivo(ip = "192.168.1.10", mac = null, nome = "TV")
        val d2 = dispositivo(ip = "192.168.1.11", mac = null, nome = "TV")
        val id1 = helper.identidadeEstavelDispositivo(d1)
        val id2 = helper.identidadeEstavelDispositivo(d2)
        assert(id1 != id2) { "IDs distintos esperados para IPs diferentes" }
    }
}
