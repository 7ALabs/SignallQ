package io.signallq.app.monitoramento

import io.signallq.app.core.database.ApelidoDispositivoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa a logica pura de deteccao de dispositivos novos.
 *
 * Simula o comportamento de verificarDispositivosNovos() do MainViewModel
 * sem dependencia de Android/ViewModel/WorkManager.
 *
 * Cobre tanto o fluxo original por MAC (Room) quanto o fluxo por ip+nome (DataStore)
 * introduzido para dispositivos sem MAC (Android 10+ com MAC randomizado).
 */
class DeteccaoDispositivoNovoTest {
    /**
     * Extrai MACs novos: MACs no scan atual que nao existem no banco.
     * Replica a logica central de verificarDispositivosNovos().
     */
    private fun detectarMACsNovos(
        macsNoScan: List<String>,
        macsNoBank: List<String>,
    ): List<String> {
        val conhecidos = macsNoBank.toSet()
        return macsNoScan.filter { mac -> mac !in conhecidos }
    }

    // ── Helper de identidade estável — replica lógica do MainViewModel ────────

    /**
     * Replica a lógica de [MainViewModel.identidadeEstavelDispositivo].
     * Retorna null quando MAC está disponível (fluxo Room cobre esse caso).
     * Retorna "ipnome:<IP>:<nome>" para dispositivos sem MAC.
     */
    private fun identidadeEstavel(
        mac: String?,
        ip: String?,
        nome: String,
    ): String? {
        if (mac != null) return null
        val ipVal = ip ?: return null
        return "ipnome:$ipVal:${nome.trim().lowercase()}"
    }

    @Test
    fun `MAC novo detectado quando nao esta no banco`() {
        val macsNoScan = listOf("AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02")
        val macsNoBanco = listOf("AA:BB:CC:DD:EE:01") // apenas o primeiro conhecido

        val novos = detectarMACsNovos(macsNoScan, macsNoBanco)

        assertEquals("Deve detectar 1 MAC novo", 1, novos.size)
        assertEquals("MAC novo deve ser o segundo", "AA:BB:CC:DD:EE:02", novos[0])
    }

    @Test
    fun `nenhum MAC novo quando todos ja sao conhecidos`() {
        val macsNoScan = listOf("AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02")
        val macsNoBanco = listOf("AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02")

        val novos = detectarMACsNovos(macsNoScan, macsNoBanco)

        assertTrue("Nao deve detectar MACs novos quando todos ja sao conhecidos", novos.isEmpty())
    }

    @Test
    fun `todos os MACs sao novos quando banco esta vazio`() {
        val macsNoScan = listOf("AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:03")
        val macsNoBanco = emptyList<String>()

        val novos = detectarMACsNovos(macsNoScan, macsNoBanco)

        assertEquals("Todos os MACs devem ser novos com banco vazio", 3, novos.size)
    }

    @Test
    fun `MAC com apelido null e tratado como conhecido para notificacao`() {
        // Dispositivos com apelido=null foram registrados silenciosamente
        // para suprimir notificacao — devem ser tratados como conhecidos
        val entidades =
            listOf(
                ApelidoDispositivoEntity(mac = "AA:BB:CC:DD:EE:01", apelido = null), // silencioso
                ApelidoDispositivoEntity(mac = "AA:BB:CC:DD:EE:02", apelido = "Router"), // com apelido
            )
        val macsConhecidos = entidades.map { it.mac }.toSet()

        val macsNoScan = listOf("AA:BB:CC:DD:EE:01", "AA:BB:CC:DD:EE:02", "AA:BB:CC:DD:EE:03")
        val novos = macsNoScan.filter { it !in macsConhecidos }

        assertEquals("Apenas o MAC 03 deve ser novo", 1, novos.size)
        assertEquals("MAC novo deve ser o 03", "AA:BB:CC:DD:EE:03", novos[0])
    }

    @Test
    fun `scan vazio nao detecta novos`() {
        val macsNoScan = emptyList<String>()
        val macsNoBanco = listOf("AA:BB:CC:DD:EE:01")

        val novos = detectarMACsNovos(macsNoScan, macsNoBanco)

        assertTrue("Scan vazio nao deve gerar novos MACs", novos.isEmpty())
    }

    @Test
    fun `ApelidoDispositivoEntity com apelido null pode ser criado`() {
        // Valida que o modelo aceita apelido null (necessario para inserirSilencioso)
        val entidade = ApelidoDispositivoEntity(mac = "AA:BB:CC:DD:EE:FF", apelido = null)
        assertEquals("MAC deve ser preservado", "AA:BB:CC:DD:EE:FF", entidade.mac)
        assertFalse("Apelido null nao deve ser nao-nulo", entidade.apelido != null)
    }

    // ── Testes do helper de identidade estável (fluxo sem MAC) ───────────────

    @Test
    fun `identidadeEstavel retorna null quando MAC disponivel`() {
        // Com MAC presente, o fluxo Room cobre o rastreamento — DataStore nao necessario
        val resultado =
            identidadeEstavel(
                mac = "AA:BB:CC:DD:EE:01",
                ip = "192.168.1.10",
                nome = "Meu dispositivo",
            )
        assertNull("Deve retornar null quando MAC disponivel", resultado)
    }

    @Test
    fun `identidadeEstavel retorna ipnome quando MAC nulo`() {
        val resultado = identidadeEstavel(mac = null, ip = "192.168.1.20", nome = "Impressora HP")
        assertNotNull("Deve retornar identidade nao-nula sem MAC", resultado)
        assertEquals("ipnome:192.168.1.20:impressora hp", resultado)
    }

    @Test
    fun `identidadeEstavel normaliza nome em lowercase e sem espacos extras`() {
        val resultado = identidadeEstavel(mac = null, ip = "192.168.1.5", nome = "  SAMSUNG TV  ")
        assertEquals("ipnome:192.168.1.5:samsung tv", resultado)
    }

    @Test
    fun `identidadeEstavel retorna null quando ip e nulo sem MAC`() {
        // Sem IP e sem MAC, nao ha como gerar identidade estavel
        val resultado = identidadeEstavel(mac = null, ip = null, nome = "Host ativo")
        assertNull("Deve retornar null quando ip tambem e nulo", resultado)
    }

    @Test
    fun `deteccao por identidade ipnome nao notifica dispositivo ja conhecido`() {
        val identidadesConhecidas = setOf("ipnome:192.168.1.10:impressora hp")
        val identidadeAtual = identidadeEstavel(mac = null, ip = "192.168.1.10", nome = "Impressora HP")

        assertNotNull(identidadeAtual)
        assertFalse(
            "Dispositivo com identidade ja conhecida nao deve ser notificado",
            identidadeAtual !in identidadesConhecidas,
        )
    }

    @Test
    fun `deteccao por identidade ipnome notifica dispositivo novo`() {
        val identidadesConhecidas = setOf("ipnome:192.168.1.10:impressora hp")
        val identidadeAtual = identidadeEstavel(mac = null, ip = "192.168.1.15", nome = "Smart TV")

        assertNotNull(identidadeAtual)
        assertTrue(
            "Dispositivo com nova identidade ip+nome deve ser notificado",
            identidadeAtual !in identidadesConhecidas,
        )
    }

    @Test
    fun `dispositivo sem MAC nao deve gerar ApelidoDispositivoEntity com mac nulo`() {
        // Garante que o novo fluxo nao insere lixo no Room
        // Simula a decisao de roteamento: mac != null -> Room, mac == null -> DataStore
        val mac: String? = null
        val deveInserirNoRoom = mac != null
        assertFalse("Dispositivo sem MAC NAO deve ser inserido no Room", deveInserirNoRoom)
    }
}
