package io.signallq.app.feature.devices

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testes unitários para [ClassificadorDispositivoRede].
 * Verifica a classificação de tipo de dispositivo com base em nome, serviços mDNS,
 * portas abertas e fabricante.
 */
class ClassificadorDispositivoTest {

    private fun dispositivo(
        nome: String = "Dispositivo não identificado",
        fonteNome: String = "arp",
        tiposServico: Set<String> = emptySet(),
        portas: Set<Int> = emptySet(),
        mac: String? = null,
    ) = DispositivoRede(
        id = "test:$nome",
        ip = "192.168.1.100",
        mac = mac,
        nomeExibicao = nome,
        fonteNome = fonteNome,
        tiposServicoMdns = tiposServico,
        portasAbertas = portas,
    )

    @Test
    fun `gateway sempre classifica como roteador`() {
        val d = dispositivo(nome = "Gateway", fonteNome = "gateway")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.roteador, tipo)
    }

    @Test
    fun `nome contendo iphone classifica como smartphone`() {
        val d = dispositivo(nome = "iPhone de João")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smartphone, tipo)
    }

    @Test
    fun `nome contendo galaxy classifica como smartphone`() {
        val d = dispositivo(nome = "Galaxy S23")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smartphone, tipo)
    }

    @Test
    fun `nome contendo chromecast classifica como smarthome`() {
        val d = dispositivo(nome = "Chromecast")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smarthome, tipo)
    }

    @Test
    fun `nome contendo router classifica como roteador`() {
        val d = dispositivo(nome = "TP-Link Router")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.roteador, tipo)
    }

    @Test
    fun `servico _airplay classifica como smarthome`() {
        val d = dispositivo(tiposServico = setOf("_airplay._tcp.local"))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smarthome, tipo)
    }

    @Test
    fun `servico _ipp classifica como impressora`() {
        val d = dispositivo(tiposServico = setOf("_ipp._tcp.local"))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.impressora, tipo)
    }

    @Test
    fun `servico _smb classifica como computador`() {
        val d = dispositivo(tiposServico = setOf("_smb._tcp.local"))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.computador, tipo)
    }

    @Test
    fun `porta 53 classifica como roteador`() {
        val d = dispositivo(portas = setOf(53))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.roteador, tipo)
    }

    @Test
    fun `porta 445 (SMB) classifica como computador`() {
        val d = dispositivo(portas = setOf(445))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.computador, tipo)
    }

    @Test
    fun `fabricante Apple classifica como smartphone`() {
        val d = dispositivo()
        val tipo = ClassificadorDispositivoRede.classificar(d, "Apple")
        assertEquals(TipoDispositivo.smartphone, tipo)
    }

    @Test
    fun `fabricante Ubiquiti classifica como pontoAcesso`() {
        val d = dispositivo()
        val tipo = ClassificadorDispositivoRede.classificar(d, "Ubiquiti")
        assertEquals(TipoDispositivo.pontoAcesso, tipo)
    }

    @Test
    fun `fabricante desconhecido sem outros hints retorna desconhecido`() {
        val d = dispositivo()
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.desconhecido, tipo)
    }

    @Test
    fun `nome contendo printer classifica como impressora`() {
        val d = dispositivo(nome = "HP LaserJet Printer")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.impressora, tipo)
    }

    @Test
    fun `nome eero (mesh) classifica como pontoAcesso`() {
        val d = dispositivo(nome = "eero-livingroom")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.pontoAcesso, tipo)
    }

    @Test
    fun `servico _googlecast classifica como smarthome`() {
        val d = dispositivo(tiposServico = setOf("_googlecast._tcp.local"))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smarthome, tipo)
    }

    // ─── #982 (Fase 3): sinais novos como evidencia, nunca veredito isolado ─────

    @Test
    fun `servico _matter classifica como smarthome generico, sem subtipo especifico`() {
        val d = dispositivo(tiposServico = setOf("_matter._tcp.local"))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smarthome, tipo)
    }

    @Test
    fun `servico _androidtvremote classifica como smarthome (smart tv)`() {
        val d = dispositivo(tiposServico = setOf("_androidtvremote._tcp.local"))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smarthome, tipo)
    }

    @Test
    fun `servico _rtsp via mDNS classifica como smarthome (camera)`() {
        val d = dispositivo(tiposServico = setOf("_rtsp._tcp.local"))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smarthome, tipo)
    }

    @Test
    fun `CASO NEGATIVO - porta 554 sozinha nao confirma camera`() {
        // Varios dispositivos abrem a porta RTSP sem ser camera — sem corroboracao de nome
        // ou mDNS especifico (_rtsp), a porta isolada nao deve virar veredito de smarthome.
        val d = dispositivo(portas = setOf(554))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.desconhecido, tipo)
    }

    @Test
    fun `porta 554 com nome sugestivo de camera classifica como smarthome`() {
        val d = dispositivo(nome = "IPCam Quintal", portas = setOf(554))
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.smarthome, tipo)
    }

    @Test
    fun `fabricante Nintendo classifica como console (correcao do bug conhecido)`() {
        // Antes da Fase 3, Nintendo caia em fabricantesSmarthome e virava "smarthome" — hoje
        // tem categoria propria.
        val d = dispositivo()
        val tipo = ClassificadorDispositivoRede.classificar(d, "Nintendo")
        assertEquals(TipoDispositivo.console, tipo)
    }

    @Test
    fun `nome nintendo switch classifica como console mesmo sem fabricante`() {
        val d = dispositivo(nome = "Nintendo Switch de Ana")
        val tipo = ClassificadorDispositivoRede.classificar(d, null)
        assertEquals(TipoDispositivo.console, tipo)
    }
}
