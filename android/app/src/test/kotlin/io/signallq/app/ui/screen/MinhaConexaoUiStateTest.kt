package io.signallq.app.ui.screen

import io.signallq.app.core.datastore.ConnectionProfilePersistido
import io.signallq.app.feature.settings.ResultadoDivergenciaPerfilConexao
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class MinhaConexaoUiStateTest {
    private fun perfil(
        providerFixed: String?,
        userConfirmed: Boolean,
    ) = ConnectionProfilePersistido(
        networkId = "wifi-bssid:aa:bb:cc:dd:ee:ff",
        providerFixed = providerFixed,
        contractedDownloadMbps = 500,
        contractedUploadMbps = 250,
        city = "São Paulo",
        state = "SP",
        userConfirmed = userConfirmed,
    )

    @Test
    fun `sem perfil salvo expõe campos nulos e sem base pra comparar`() {
        val estado = mapMinhaConexaoUiState(perfil = null, providerDetectado = "Vivo Fibra")
        assertNull(estado.providerFixed)
        assertNull(estado.contractedDownloadMbps)
        assertTrue(estado.divergencia is ResultadoDivergenciaPerfilConexao.SemBaseParaComparar)
    }

    @Test
    fun `perfil salvo sem divergencia expõe PerfilCoincide`() {
        val estado = mapMinhaConexaoUiState(perfil = perfil("Vivo Fibra", true), providerDetectado = "Vivo Fibra")
        assertEquals("Vivo Fibra", estado.providerFixed)
        assertEquals(500, estado.contractedDownloadMbps)
        assertEquals(250, estado.contractedUploadMbps)
        assertEquals("São Paulo", estado.city)
        assertEquals("SP", estado.state)
        assertTrue(estado.divergencia is ResultadoDivergenciaPerfilConexao.PerfilCoincide)
    }

    @Test
    fun `divergencia nao confirmada expõe AtualizavelSilenciosamente`() {
        val estado = mapMinhaConexaoUiState(perfil = perfil("Vivo Fibra", userConfirmed = false), providerDetectado = "Claro NET")
        assertTrue(estado.divergencia is ResultadoDivergenciaPerfilConexao.AtualizavelSilenciosamente)
    }

    @Test
    fun `divergencia confirmada expõe DivergenciaConfirmadaPeloUsuario`() {
        val estado = mapMinhaConexaoUiState(perfil = perfil("Vivo Fibra", userConfirmed = true), providerDetectado = "Claro NET")
        assertTrue(estado.divergencia is ResultadoDivergenciaPerfilConexao.DivergenciaConfirmadaPeloUsuario)
    }
}
