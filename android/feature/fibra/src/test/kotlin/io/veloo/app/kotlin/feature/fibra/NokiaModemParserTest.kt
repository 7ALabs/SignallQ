package io.signallq.app.feature.fibra

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * GH#865 Fase 1 — cobre os campos novos de Wi-Fi/LAN extraidos a partir de
 * fixtures representativas do padrao JS ja usado pelo firmware Nokia
 * (`docs_ai/technical/NOKIA_GPON_FIELD_MAP.md`). O documento so descreve os
 * campos por nome/tipo (nao reproduz o dump JS bruto por causa de segredo
 * adjacente na mesma tela) — a fixture abaixo segue o mesmo estilo de
 * objeto JS com aspas simples ja usado nas telas de GPON/WAN, mas nao foi
 * validada contra o equipamento real (ver observacao no PR).
 */
class NokiaModemParserTest {

    // ─── parseWifi ──────────────────────────────────────────────────────

    private val wlanStatusFixture = """
        var wlan_status = [
        {SSID:'CasaWifi',RadioEnabled:true,Channel:6,Standard:'b,g,n',BeaconType:'WPAand11i',TransmitPower:100,TotalAssociations:5},
        {SSID:'CasaWifi_5G',RadioEnabled:false,Channel:44,Standard:'a,n,ac',BeaconType:'11i',TransmitPower:80,TotalAssociations:0},
        {RadioEnabled:true,Channel:1,Standard:'b,g,n',BeaconType:'None',TransmitPower:100,TotalAssociations:0}
        ];
    """.trimIndent()

    @Test
    fun `parseWifi extrai radios 2_4GHz e 5GHz e ignora bloco sem SSID`() {
        val resultado = NokiaModemParser.parseWifi(wlanStatusFixture)

        requireNotNull(resultado)
        assertEquals(2, resultado.radios.size)

        val radio24 = resultado.radios[0]
        assertEquals("2.4GHz", radio24.banda)
        assertEquals("CasaWifi", radio24.ssid)
        assertEquals(6, radio24.canal)
        assertTrue(radio24.habilitado)
        assertEquals("WPAand11i", radio24.criptografia)
        assertEquals("100%", radio24.potenciaTx)

        val radio5 = resultado.radios[1]
        assertEquals("5GHz", radio5.banda)
        assertEquals("CasaWifi_5G", radio5.ssid)
        assertEquals(44, radio5.canal)
        assertFalse(radio5.habilitado)
        assertEquals("11i", radio5.criptografia)
        assertEquals("80%", radio5.potenciaTx)
    }

    @Test
    fun `parseWifi retorna null quando nao ha wlan_status na resposta`() {
        val resultado = NokiaModemParser.parseWifi("<html><body>pagina sem dado</body></html>")
        assertNull(resultado)
    }

    @Test
    fun `parseWifi nunca extrai PreSharedKey mesmo se presente no HTML`() {
        // Regressao de seguranca: mesmo que a pagina real venha com PSK em algum
        // lugar do documento completo, o parser so le as chaves conhecidas do
        // objeto wlan_status — nunca deve produzir um campo de senha.
        val fixtureComPsk = wlanStatusFixture + "\nvar psks = {PreSharedKey:'segredo123'};"
        val resultado = NokiaModemParser.parseWifi(fixtureComPsk)

        requireNotNull(resultado)
        resultado.radios.forEach { radio ->
            assertFalse(radio.toString().contains("segredo123"))
        }
    }

    // ─── parseLan ───────────────────────────────────────────────────────

    private val lanStatusFixture = """
        var lan_ifip = {IPAddress:'192.168.1.254',SubnetMask:'255.255.255.0'};
        var lan_ether = [
        {Enable:true,Status:'Up',MACAddress:'AA:BB:CC:DD:EE:01',MaxBitRate:'1000',stat:{BytesSent:100,BytesReceived:200}},
        {Enable:true,Status:'NoLink',MACAddress:'AA:BB:CC:DD:EE:02',MaxBitRate:'Auto',stat:{BytesSent:0,BytesReceived:0}}
        ];
    """.trimIndent()

    private val lanConfigFixture = """
        var ipv4_config = {DHCPServerEnable:true,MinAddress:'192.168.1.100',MaxAddress:'192.168.1.200',SubnetMask:'255.255.255.0'};
    """.trimIndent()

    @Test
    fun `parseLan combina IP do roteador com faixa de DHCP`() {
        val resultado = NokiaModemParser.parseLan(lanStatusFixture, lanConfigFixture)

        requireNotNull(resultado)
        assertEquals("192.168.1.254", resultado.routerIp)
        assertEquals("255.255.255.0", resultado.subnetMask)
        assertTrue(resultado.dhcpHabilitado)
        assertEquals("192.168.1.100", resultado.dhcpFaixaInicio)
        assertEquals("192.168.1.200", resultado.dhcpFaixaFim)
    }

    @Test
    fun `parseLan retorna null quando nenhuma das duas paginas traz dado util`() {
        val resultado = NokiaModemParser.parseLan("<html></html>", "<html></html>")
        assertNull(resultado)
    }

    @Test
    fun `parseLan usa apenas a pagina de config quando status nao tem lan_ifip`() {
        val resultado = NokiaModemParser.parseLan("<html>sem lan_ifip</html>", lanConfigFixture)

        requireNotNull(resultado)
        assertEquals("—", resultado.routerIp)
        assertEquals("255.255.255.0", resultado.subnetMask)
        assertTrue(resultado.dhcpHabilitado)
    }

    // ─── extractJsObjectBlocks (helper interno reaproveitado por parseWifi/parseLan) ──

    @Test
    fun `extractJsObjectBlocks respeita chaves aninhadas ao separar blocos`() {
        val blocks = NokiaModemParser.extractJsObjectBlocks(lanStatusFixture, "lan_ether")
        assertEquals(2, blocks.size)
        assertTrue(blocks[0].contains("stat:{BytesSent:100,BytesReceived:200}"))
    }

    @Test
    fun `extractJsObjectBlocks retorna lista vazia quando variavel nao existe`() {
        val blocks = NokiaModemParser.extractJsObjectBlocks("var outra_coisa = [];", "lan_ether")
        assertTrue(blocks.isEmpty())
    }
}
