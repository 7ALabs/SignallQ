package io.signallq.app.feature.devices

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GH#1025 — cobre [encontrarDispositivoPorBssid], a correlação unidirecional (nó Wi-Fi →
 * dispositivo) usada pela tela Sinal pra abrir MeshApSheet com dado real do scan LAN.
 */
class EncontrarDispositivoPorBssidTest {
    private fun dispositivo(id: String, mac: String?) = DispositivoRede(
        id = id,
        ip = "192.168.1.50",
        mac = mac,
        nomeExibicao = "Dispositivo de teste",
        fonteNome = "arp",
    )

    @Test
    fun `mac igual ao bssid normalizado encontra o dispositivo`() {
        val alvo = dispositivo(id = "ap-1", mac = "AA:BB:CC:11:22:33")
        val outros = listOf(dispositivo(id = "outro", mac = "11:22:33:44:55:66"), alvo)

        val resultado = encontrarDispositivoPorBssid(outros, bssid = "aa:bb:cc:11:22:33")

        assertEquals(alvo, resultado)
    }

    @Test
    fun `comparacao ignora maiusculas e separadores`() {
        val alvo = dispositivo(id = "ap-1", mac = "aa-bb-cc-dd-ee-ff")

        val resultado = encontrarDispositivoPorBssid(listOf(alvo), bssid = "AA:BB:CC:DD:EE:FF")

        assertEquals(alvo, resultado)
    }

    @Test
    fun `nenhum dispositivo com mac correspondente retorna null`() {
        val dispositivos = listOf(dispositivo(id = "a", mac = "11:22:33:44:55:66"))

        val resultado = encontrarDispositivoPorBssid(dispositivos, bssid = "AA:BB:CC:DD:EE:FF")

        assertNull(resultado)
    }

    @Test
    fun `lista vazia retorna null sem lancar excecao`() {
        assertNull(encontrarDispositivoPorBssid(emptyList(), bssid = "AA:BB:CC:DD:EE:FF"))
    }

    @Test
    fun `dispositivo sem mac nunca casa por acidente`() {
        val semMac = dispositivo(id = "sem-mac", mac = null)

        val resultado = encontrarDispositivoPorBssid(listOf(semMac), bssid = "AA:BB:CC:DD:EE:FF")

        assertNull(resultado)
    }
}
