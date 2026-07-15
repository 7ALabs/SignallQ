package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.feature.diagnostico.DiagnosticStatus
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RemoteDiagnosticReportMapperTest {

    private fun cardJson(
        id: String = "TEST-01",
        titulo: String = "Titulo",
        status: String = "attention",
        categoria: String = "wifi",
    ): JSONObject = JSONObject().apply {
        put("id", id)
        put("titulo", titulo)
        put("status", status)
        put("evidencia", "RSSI -75 dBm")
        put("mensagemUsuario", "mensagem")
        put("recomendacao", "recomendacao")
        put("categoria", categoria)
        put("podeConcluir", true)
        put("categoriaOrigem", "wifi")
    }

    private fun minimalPayload(): JSONObject = JSONObject().apply {
        put("evaluationSource", "REMOTE")
        put("wifiResultados", org.json.JSONArray().put(cardJson()))
        put("internetResultados", org.json.JSONArray())
        put("mobileResultados", org.json.JSONArray())
        put("fibraResultados", org.json.JSONArray())
        put("dnsResultados", org.json.JSONArray())
        put("historicoResultados", org.json.JSONArray())
        put("wifiCanalResultados", org.json.JSONArray())
        put("redeResultados", org.json.JSONArray())
        put("decisao", cardJson(id = "DECISAO-WIFI_LOCAL", status = "attention", categoria = "decisao"))
        put("achadosSecundarios", org.json.JSONArray())
        put("hipotesesDescartadas", org.json.JSONArray())
        put("dadosAusentes", org.json.JSONArray().put("fibra"))
        put("limitacoesEquipamentoLocal", org.json.JSONArray())
        put("recomendacoes", org.json.JSONArray())
        put(
            "scoreEngineResultado",
            JSONObject().apply {
                put("score", 62)
                put("veredictoHumano", "regular")
                put("dimensoes", org.json.JSONArray().put(JSONObject().apply { put("id", "wifi"); put("score", 62) }))
            },
        )
        put("perfisUso", org.json.JSONArray())
        put("gameReadiness", org.json.JSONArray())
        put("geradoEmMs", 1_000_000L)
    }

    @Test
    fun `mapeia buckets de resultado 1 para 1`() {
        val report = RemoteDiagnosticReportMapper.toDiagnosticReport(minimalPayload(), geradoEmMs = 123L)

        assertEquals(1, report.wifiResultados.size)
        assertEquals("TEST-01", report.wifiResultados.first().id)
        assertEquals(DiagnosticStatus.attention, report.wifiResultados.first().status)
        assertEquals("RSSI -75 dBm", report.wifiResultados.first().evidencia)

        assertEquals("DECISAO-WIFI_LOCAL", report.decisao.id)
        assertEquals(DiagnosticStatus.attention, report.decisao.status)
        assertEquals(listOf("fibra"), report.dadosAusentes)
        assertEquals(123L, report.geradoEmMs)
    }

    @Test
    fun `mapeia score com dimensoes remotas como EvidenceScore informativo`() {
        val report = RemoteDiagnosticReportMapper.toDiagnosticReport(minimalPayload(), geradoEmMs = 0L)

        val score = report.scoreEngineResultado
        assertEquals(62, score?.score)
        assertEquals(1, score?.dimensoesUsadas?.size)
        assertEquals("wifi", score?.dimensoesUsadas?.first()?.dimensao)
        assertEquals(62, score?.dimensoesUsadas?.first()?.nota)
    }

    @Test
    fun `perfisUso e gameReadiness sempre vem vazios do mapper (calculados localmente pelo caller)`() {
        val payload = minimalPayload()
        val report = RemoteDiagnosticReportMapper.toDiagnosticReport(payload, geradoEmMs = 0L)

        assertTrue(report.perfisUso.isEmpty())
        assertTrue(report.gameReadiness.isEmpty())
    }

    @Test
    fun `status desconhecido cai para inconclusive, nunca quebra`() {
        val payload = minimalPayload()
        payload.put("decisao", cardJson(status = "algo_esquisito"))
        val report = RemoteDiagnosticReportMapper.toDiagnosticReport(payload, geradoEmMs = 0L)
        assertEquals(DiagnosticStatus.inconclusive, report.decisao.status)
    }

    @Test
    fun `scoreEngineResultado ausente mapeia para null, nao quebra`() {
        val payload = minimalPayload()
        payload.remove("scoreEngineResultado")
        val report = RemoteDiagnosticReportMapper.toDiagnosticReport(payload, geradoEmMs = 0L)
        assertNull(report.scoreEngineResultado)
    }
}
