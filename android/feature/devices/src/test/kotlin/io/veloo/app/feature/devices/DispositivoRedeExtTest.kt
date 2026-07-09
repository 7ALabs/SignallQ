package io.signallq.app.feature.devices

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** GH#531 — cobre a regra de "dispositivo-cliente real" usada nas contagens
 *  de subtítulo do gateway em Ajustes e Dispositivos. */
class DispositivoRedeExtTest {

    private fun dispositivo(
        fonteNome: String,
        tipo: TipoDispositivo,
    ) = DispositivoRede(
        id = "test",
        ip = "192.168.1.100",
        mac = "AA:BB:CC:DD:EE:FF",
        nomeExibicao = "Dispositivo",
        fonteNome = fonteNome,
        tipoDispositivo = tipo,
    )

    @Test
    fun `gateway nunca conta como cliente`() {
        val gw = dispositivo(fonteNome = "gateway", tipo = TipoDispositivo.roteador)
        assertFalse(gw.ehClienteFinal())
    }

    @Test
    fun `ponto de acesso mesh nunca conta como cliente`() {
        val ap = dispositivo(fonteNome = "mdns", tipo = TipoDispositivo.pontoAcesso)
        assertFalse(ap.ehClienteFinal())
    }

    @Test
    fun `smartphone comum conta como cliente`() {
        val d = dispositivo(fonteNome = "arp", tipo = TipoDispositivo.smartphone)
        assertTrue(d.ehClienteFinal())
    }

    @Test
    fun `dispositivo desconhecido ainda conta como cliente`() {
        val d = dispositivo(fonteNome = "arp", tipo = TipoDispositivo.desconhecido)
        assertTrue(d.ehClienteFinal())
    }
}
