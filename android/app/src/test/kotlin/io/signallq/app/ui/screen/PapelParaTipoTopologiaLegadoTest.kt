package io.signallq.app.ui.screen

import io.signallq.app.core.network.contracts.topologia.NivelConfianca
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.feature.wifi.ConfiancaTopologia
import io.signallq.app.feature.wifi.TipoTopologia
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Testa isoladamente o adaptador `PapelTopologia -> TipoTopologia` / `NivelConfianca ->
 * ConfiancaTopologia` usado por `SinalScreen.classificarComMotorUnificado` — Fase 2B (#980),
 * parte da migração da aba Sinal → Redes pro motor unificado (TopologiaRedeEngine, Fase 2A/#979).
 *
 * O caso crítico é [PapelTopologia.SISTEMA_MESH_PROVAVEL]: vira [TipoTopologia.NO_MESH] (mesmo
 * ícone que já existe hoje) — a incerteza continua comunicada pelo aviso "* Gateway estimado
 * pelo sinal mais forte" em `GrupoRedeTree`, que já é calculado independente do motor.
 */
class PapelParaTipoTopologiaLegadoTest {
    @Test
    fun `ROTEADOR vira ROTEADOR`() {
        assertEquals(TipoTopologia.ROTEADOR, PapelTopologia.ROTEADOR.paraTipoTopologiaLegado())
    }

    @Test
    fun `NO_MESH vira NO_MESH`() {
        assertEquals(TipoTopologia.NO_MESH, PapelTopologia.NO_MESH.paraTipoTopologiaLegado())
    }

    @Test
    fun `SISTEMA_MESH_PROVAVEL vira NO_MESH`() {
        assertEquals(TipoTopologia.NO_MESH, PapelTopologia.SISTEMA_MESH_PROVAVEL.paraTipoTopologiaLegado())
    }

    @Test
    fun `REPETIDOR vira REPETIDOR`() {
        assertEquals(TipoTopologia.REPETIDOR, PapelTopologia.REPETIDOR.paraTipoTopologiaLegado())
    }

    @Test
    fun `PONTO_DE_ACESSO vira PONTO_DE_ACESSO`() {
        assertEquals(TipoTopologia.PONTO_DE_ACESSO, PapelTopologia.PONTO_DE_ACESSO.paraTipoTopologiaLegado())
    }

    @Test
    fun `DESCONHECIDO vira DESCONHECIDO`() {
        assertEquals(TipoTopologia.DESCONHECIDO, PapelTopologia.DESCONHECIDO.paraTipoTopologiaLegado())
    }

    @Test
    fun `ALTA vira ALTA`() {
        assertEquals(ConfiancaTopologia.ALTA, NivelConfianca.ALTA.paraConfiancaTopologiaLegado())
    }

    @Test
    fun `MEDIA vira MEDIA`() {
        assertEquals(ConfiancaTopologia.MEDIA, NivelConfianca.MEDIA.paraConfiancaTopologiaLegado())
    }

    @Test
    fun `BAIXA vira BAIXA`() {
        assertEquals(ConfiancaTopologia.BAIXA, NivelConfianca.BAIXA.paraConfiancaTopologiaLegado())
    }
}
