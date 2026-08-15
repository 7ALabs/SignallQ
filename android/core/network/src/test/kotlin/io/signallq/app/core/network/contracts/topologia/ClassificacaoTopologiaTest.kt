package io.signallq.app.core.network.contracts.topologia

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 0 do plano de unificação de topologia (issue #977) — só garante que os contratos novos
 * compilam e representam bem os casos previstos no plano. Nenhum motor real gera este resultado
 * ainda; não há lógica de negócio pra testar aqui.
 */
class ClassificacaoTopologiaTest {

    @Test
    fun `sistema mesh provavel nao afirma qual no e central`() {
        val evidenciaOui = Evidencia(
            tipo = TipoEvidencia.OUI,
            valorBruto = "C46E1F",
            peso = PesoEvidencia.FORTE,
        )
        val evidenciaSsid = Evidencia(
            tipo = TipoEvidencia.SSID,
            valorBruto = "Casa_Mesh_5G",
            peso = PesoEvidencia.MEDIO,
        )

        val resultado = ClassificacaoTopologia(
            papelProvavel = PapelTopologia.SISTEMA_MESH_PROVAVEL,
            confianca = NivelConfianca.MEDIA,
            evidencias = listOf(evidenciaOui, evidenciaSsid),
            origemDados = OrigemDados.SCAN_WIFI_PASSIVO,
        )

        assertEquals(PapelTopologia.SISTEMA_MESH_PROVAVEL, resultado.papelProvavel)
        assertEquals(2, resultado.evidencias.size)
        assertTrue(resultado.conflitos.isEmpty())
    }

    @Test
    fun `conflito Intelbras registra as duas evidencias que discordam`() {
        val evidenciaMesh = Evidencia(
            tipo = TipoEvidencia.OUI,
            valorBruto = "C46E1F",
            peso = PesoEvidencia.MEDIO,
        )
        val evidenciaGatewayIsp = Evidencia(
            tipo = TipoEvidencia.OUI,
            valorBruto = "C46E1F",
            peso = PesoEvidencia.MEDIO,
        )
        val conflito = ConflitoSinal(
            evidenciaA = evidenciaMesh,
            evidenciaB = evidenciaGatewayIsp,
            descricao = "OUI C46E1F consta como nó mesh e como gateway ISP",
        )

        val resultado = ClassificacaoTopologia(
            papelProvavel = PapelTopologia.DESCONHECIDO,
            confianca = NivelConfianca.BAIXA,
            evidencias = listOf(evidenciaMesh, evidenciaGatewayIsp),
            origemDados = OrigemDados.SCAN_WIFI_PASSIVO,
            conflitos = listOf(conflito),
        )

        assertEquals(1, resultado.conflitos.size)
        assertEquals(conflito, resultado.conflitos.first())
    }

    @Test
    fun `papeis nao incluem roteador central inferido`() {
        val nomes = PapelTopologia.entries.map { it.name }

        assertTrue(nomes.contains("SISTEMA_MESH_PROVAVEL"))
        assertTrue(!nomes.contains("ROTEADOR_CENTRAL_INFERIDO"))
    }

    @Test
    fun `todos os niveis de confianca existem`() {
        assertEquals(
            setOf("ALTA", "MEDIA", "BAIXA"),
            NivelConfianca.entries.map { it.name }.toSet(),
        )
    }

    @Test
    fun `todas as origens de dados previstas no plano existem`() {
        assertEquals(
            setOf("SCAN_WIFI_PASSIVO", "SCAN_LAN_ATIVO", "GATEWAY_DIRETO", "CORRELACAO"),
            OrigemDados.entries.map { it.name }.toSet(),
        )
    }
}
