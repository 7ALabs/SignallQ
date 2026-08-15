package io.signallq.app.feature.devices

import org.junit.Assert.assertEquals
import org.junit.Test

/** GH#1217 item 1 — níveis de confiança de identidade entre scans. */
class AvaliadorConfiancaIdentidadeTest {

    private fun dispositivo(
        mac: String? = null,
        ip: String? = "192.168.1.10",
        nome: String = "Dispositivo não identificado",
        fabricante: String? = null,
        esteDispositivo: Boolean = false,
    ) = DispositivoRede(
        id = "x",
        ip = ip,
        mac = mac,
        nomeExibicao = nome,
        fonteNome = "arp",
        fabricante = fabricante,
        esteDispositivo = esteDispositivo,
    )

    @Test
    fun `mac disponivel e sempre CONFIRMADA`() {
        val d = dispositivo(mac = "aa:bb:cc:dd:ee:ff")
        assertEquals(NivelConfiancaIdentidade.CONFIRMADA, AvaliadorConfiancaIdentidade.avaliar(d))
    }

    @Test
    fun `este aparelho e sempre CONFIRMADA mesmo sem mac`() {
        val d = dispositivo(mac = null, esteDispositivo = true)
        assertEquals(NivelConfiancaIdentidade.CONFIRMADA, AvaliadorConfiancaIdentidade.avaliar(d))
    }

    @Test
    fun `sem mac, nome corroborado e fabricante conhecido e PROVAVEL`() {
        val d = dispositivo(mac = null, nome = "Samsung Galaxy S23", fabricante = "Samsung")
        assertEquals(NivelConfiancaIdentidade.PROVAVEL, AvaliadorConfiancaIdentidade.avaliar(d))
    }

    @Test
    fun `sem mac e nome generico e TEMPORARIA`() {
        val d = dispositivo(mac = null, nome = "Dispositivo não identificado", fabricante = null)
        assertEquals(NivelConfiancaIdentidade.TEMPORARIA, AvaliadorConfiancaIdentidade.avaliar(d))
    }

    @Test
    fun `sem mac, nome real mas sem fabricante e TEMPORARIA -- falta uma das duas corroboracoes`() {
        val d = dispositivo(mac = null, nome = "MinhaImpressora.local", fabricante = null)
        assertEquals(NivelConfiancaIdentidade.TEMPORARIA, AvaliadorConfiancaIdentidade.avaliar(d))
    }

    @Test
    fun `sem mac e sem ip e DESCONHECIDA`() {
        val d = dispositivo(mac = null, ip = null)
        assertEquals(NivelConfiancaIdentidade.DESCONHECIDA, AvaliadorConfiancaIdentidade.avaliar(d))
    }

    @Test
    fun `rotulo fallback Dispositivo Fabricante nao conta como nome corroborado`() {
        val d = dispositivo(mac = null, nome = "Dispositivo Samsung", fabricante = "Samsung")
        assertEquals(NivelConfiancaIdentidade.TEMPORARIA, AvaliadorConfiancaIdentidade.avaliar(d))
    }
}
