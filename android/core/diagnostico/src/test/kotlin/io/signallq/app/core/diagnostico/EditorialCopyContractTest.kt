package io.signallq.app.core.diagnostico

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Guarda de regressão do De-Para editorial aprovado na issue #1502 — não recalcula
 * métricas nem thresholds (não é golden test de classificação, ver
 * [InternetDiagnosticEngineTest]/[SpeedtestQualityClassifierTest] para isso), só
 * confirma que os textos de superfície (títulos, subtítulos, mensagens) usam a
 * linguagem aprovada e não vazam jargão técnico não traduzido nem linguagem interna
 * de debug (achado crítico da aba "Achados de copy").
 */
class EditorialCopyContractTest {

    private val jargõesProibidos = listOf(
        "bufferbloat",
        "qos",
        "sqm",
        "rssi",
        "rsrp",
        "olt",
        "nat udp",
        "casou com",
        "% de match",
    )

    @Test
    fun `titulos e subtitulos dos 7 objetivos nao vazam aspas informais nem jargao`() {
        ObjetivoDiagnostico.entries.forEach { objetivo ->
            assertFalse("subtitulo de $objetivo tem aspas informais", objetivo.subtitulo.contains("\""))
            jargõesProibidos.forEach { jargao ->
                assertFalse(
                    "titulo/subtitulo de $objetivo vaza jargao '$jargao'",
                    objetivo.titulo.contains(jargao, ignoreCase = true) || objetivo.subtitulo.contains(jargao, ignoreCase = true),
                )
            }
        }
    }

    @Test
    fun `titulos do motor de internet usam linguagem simples aprovada`() {
        val input = InternetDiagnosticInput(
            downloadMbps = 5.0,
            uploadMbps = 0.0,
            latencyMs = 250.0,
            jitterMs = 40.0,
            perdaPercentual = 5.0,
            bufferbloatMs = 250.0,
        )
        val resultados = InternetDiagnosticEngine.avaliar(input, wifiConfiavelParaTeste = true)
        assertTrue(resultados.isNotEmpty())
        resultados.forEach { resultado ->
            jargõesProibidos.forEach { jargao ->
                assertFalse(
                    "resultado ${resultado.id} vaza jargao '$jargao' na mensagemUsuario",
                    resultado.mensagemUsuario.contains(jargao, ignoreCase = true),
                )
                resultado.recomendacao?.let {
                    assertFalse(
                        "resultado ${resultado.id} vaza jargao '$jargao' na recomendacao",
                        it.contains(jargao, ignoreCase = true),
                    )
                }
            }
        }
    }

    @Test
    fun `motor de fibra nao cita OLT nem ONT nas mensagens ao usuario`() {
        val desligado = FibraDiagnosticInput(isUp = false)
        val resultados = FibraSignalQualityEngine.avaliar(desligado)
        assertTrue(resultados.isNotEmpty())
        resultados.forEach { resultado ->
            assertFalse(resultado.mensagemUsuario.contains("OLT"))
            assertFalse(resultado.mensagemUsuario.contains("ONT"))
            resultado.recomendacao?.let {
                assertFalse(it.contains("OLT"))
                assertFalse(it.contains("ONT"))
            }
        }
    }

    @Test
    fun `recomendacao de motor gamer usa priorizacao de trafego em vez de QoS`() {
        val resultado = ModoGamerEngine.avaliar(
            categoria = CategoriaJogoModoGamer.FPS_COMPETITIVO,
            device = DeviceJogo.ANDROID,
            input = DiagnosticInput(
                internet = InternetDiagnosticInput(
                    downloadMbps = 100.0,
                    uploadMbps = 20.0,
                    latencyMs = 150.0,
                    jitterMs = 30.0,
                    perdaPercentual = 0.5,
                ),
            ),
        )
        assertTrue(resultado.acoes.any { it.contains("priorização de tráfego") })
        assertFalse(resultado.acoes.any { it.contains("QoS", ignoreCase = false) })
    }
}
