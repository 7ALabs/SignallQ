package io.signallq.app

import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.ui.ConnectionNodeType
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testa isoladamente o mapeamento de [PapelTopologia] (motor de topologia unificado,
 * TopologiaRedeEngine/#979) para [ConnectionNodeType] (Home) usado por
 * `MainViewModel.coletarInfoLocalRede` — Fase 2B, issue #980.
 *
 * O caso crítico é [PapelTopologia.SISTEMA_MESH_PROVAVEL]: precisa virar [ConnectionNodeType.WifiMesh],
 * nunca [ConnectionNodeType.WifiRouter] — `HomeScreen.onGatewayTap` aciona um fluxo de login real
 * do modem (`GatewayConnectionSheet`) só para `WifiRouter`; um nó "provavelmente" mesh não pode
 * cair nesse fluxo como se fosse um roteador confirmado.
 */
class PapelParaConnectionNodeTypeTest {
    @Test
    fun `ROTEADOR vira WifiRouter`() {
        assertEquals(ConnectionNodeType.WifiRouter, papelParaConnectionNodeType(PapelTopologia.ROTEADOR))
    }

    @Test
    fun `NO_MESH vira WifiMesh`() {
        assertEquals(ConnectionNodeType.WifiMesh, papelParaConnectionNodeType(PapelTopologia.NO_MESH))
    }

    @Test
    fun `SISTEMA_MESH_PROVAVEL vira WifiMesh, nunca WifiRouter`() {
        assertEquals(ConnectionNodeType.WifiMesh, papelParaConnectionNodeType(PapelTopologia.SISTEMA_MESH_PROVAVEL))
    }

    @Test
    fun `REPETIDOR vira WifiExtender`() {
        assertEquals(ConnectionNodeType.WifiExtender, papelParaConnectionNodeType(PapelTopologia.REPETIDOR))
    }

    @Test
    fun `PONTO_DE_ACESSO vira Unknown`() {
        assertEquals(ConnectionNodeType.Unknown, papelParaConnectionNodeType(PapelTopologia.PONTO_DE_ACESSO))
    }

    @Test
    fun `DESCONHECIDO vira Unknown`() {
        assertEquals(ConnectionNodeType.Unknown, papelParaConnectionNodeType(PapelTopologia.DESCONHECIDO))
    }
}
