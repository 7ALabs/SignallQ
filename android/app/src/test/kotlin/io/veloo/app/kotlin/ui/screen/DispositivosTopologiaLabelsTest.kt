package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testa isoladamente os rótulos exibidos em `DeviceDetailSheet` (seção REDE) pras novas linhas
 * "Conexão física" / "Papel na rede" — wiring de #983 (Fase 4) na tela Dispositivos.
 */
class DispositivosTopologiaLabelsTest {
    @Test
    fun `tipoConexaoFisicaLabel traduz os 3 valores`() {
        assertEquals("Cabo (Ethernet)", tipoConexaoFisicaLabel(TipoConexaoFisica.ETHERNET))
        assertEquals("Wi-Fi", tipoConexaoFisicaLabel(TipoConexaoFisica.WIFI))
        assertEquals("Desconhecida", tipoConexaoFisicaLabel(TipoConexaoFisica.DESCONHECIDO))
    }

    @Test
    fun `papelTopologiaLabel traduz os 6 papeis`() {
        assertEquals("Roteador", papelTopologiaLabel(PapelTopologia.ROTEADOR))
        assertEquals("Nó mesh", papelTopologiaLabel(PapelTopologia.NO_MESH))
        assertEquals("Repetidor", papelTopologiaLabel(PapelTopologia.REPETIDOR))
        assertEquals("Ponto de acesso", papelTopologiaLabel(PapelTopologia.PONTO_DE_ACESSO))
        assertEquals("Desconhecido", papelTopologiaLabel(PapelTopologia.DESCONHECIDO))
    }

    @Test
    fun `SISTEMA_MESH_PROVAVEL nunca vira rotulo afirmativo de roteador central`() {
        // Caso crítico do épico #975 — nenhum nó "Roteador central" pode ser afirmado sem
        // confirmação real; um sistema mesh sem 2ª rota IP precisa deixar isso explícito no rótulo.
        val rotulo = papelTopologiaLabel(PapelTopologia.SISTEMA_MESH_PROVAVEL)
        assertEquals("Sistema mesh (provável)", rotulo)
        assertEquals(false, rotulo.contains("Roteador", ignoreCase = true))
    }
}
