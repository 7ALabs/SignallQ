package io.linka.app.kotlin.monitoramento

import io.linka.app.kotlin.core.database.ApelidoDispositivoEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testa a logica pura de deteccao de dispositivos novos.
 *
 * Simula o comportamento de verificarDispositivosNovos() do MainViewModel
 * sem dependencia de Android/ViewModel/WorkManager.
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
        val entidades = listOf(
            ApelidoDispositivoEntity(mac = "AA:BB:CC:DD:EE:01", apelido = null),  // silencioso
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
}
