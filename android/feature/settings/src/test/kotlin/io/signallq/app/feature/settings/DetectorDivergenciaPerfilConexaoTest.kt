package io.signallq.app.feature.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DetectorDivergenciaPerfilConexaoTest {

    private fun perfil(
        providerFixed: String?,
        userConfirmed: Boolean,
    ) = ConnectionProfile(
        networkId = "wifi-bssid:aa:bb:cc:dd:ee:ff",
        providerFixed = providerFixed,
        contractedDownloadMbps = null,
        contractedUploadMbps = null,
        city = null,
        state = null,
        userConfirmed = userConfirmed,
    )

    @Test
    fun `sem perfil salvo nao ha base pra comparar`() {
        val resultado = DetectorDivergenciaPerfilConexao.avaliar(perfilSalvo = null, providerDetectado = "Vivo Fibra")
        assertTrue(resultado is ResultadoDivergenciaPerfilConexao.SemBaseParaComparar)
    }

    @Test
    fun `perfil salvo sem provedor fixo nao ha base pra comparar`() {
        val resultado = DetectorDivergenciaPerfilConexao.avaliar(perfilSalvo = perfil(null, false), providerDetectado = "Vivo Fibra")
        assertTrue(resultado is ResultadoDivergenciaPerfilConexao.SemBaseParaComparar)
    }

    @Test
    fun `nada detectado nao ha base pra comparar`() {
        val resultado = DetectorDivergenciaPerfilConexao.avaliar(perfilSalvo = perfil("Vivo Fibra", true), providerDetectado = null)
        assertTrue(resultado is ResultadoDivergenciaPerfilConexao.SemBaseParaComparar)
    }

    @Test
    fun `provedores iguais (case-insensitive) coincidem`() {
        val resultado = DetectorDivergenciaPerfilConexao.avaliar(perfilSalvo = perfil("vivo fibra", true), providerDetectado = "Vivo Fibra")
        assertTrue(resultado is ResultadoDivergenciaPerfilConexao.PerfilCoincide)
    }

    @Test
    fun `diverge e usuario NUNCA confirmou -- atualizavel silenciosamente`() {
        val resultado = DetectorDivergenciaPerfilConexao.avaliar(perfilSalvo = perfil("Vivo Fibra", userConfirmed = false), providerDetectado = "Claro NET")
        val esperado = ResultadoDivergenciaPerfilConexao.AtualizavelSilenciosamente("Vivo Fibra", "Claro NET")
        assertEquals(esperado, resultado)
    }

    @Test
    fun `diverge e usuario confirmou explicitamente -- exige sinalizacao, nunca sobrescreve silenciosamente`() {
        val resultado = DetectorDivergenciaPerfilConexao.avaliar(perfilSalvo = perfil("Vivo Fibra", userConfirmed = true), providerDetectado = "Claro NET")
        val esperado = ResultadoDivergenciaPerfilConexao.DivergenciaConfirmadaPeloUsuario("Vivo Fibra", "Claro NET")
        assertEquals(esperado, resultado)
    }
}
