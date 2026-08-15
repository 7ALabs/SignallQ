package io.signallq.app.ui.relatorio

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RelatorioDiagnosticoHtmlBuilderTest {
    private fun snapshotBase(
        downloadMbps: Double? = 87.3,
        uploadMbps: Double? = 21.4,
        uploadNaoDetectado: Boolean = false,
        latenciaMs: Double? = 18.0,
        jitterMs: Double? = 4.0,
        perdaPercentual: Double? = 0.2,
        perdaEstimada: Boolean = true,
        bufferbloatMs: Double? = 6.0,
        statusIntegridade: String? = "Completo",
        ssidMascarado: String? = "Ca***",
        ipLocalMascarado: String? = "192.168.1.*",
        ipPublicoMascarado: String? = "200.150.*.*",
        offline: Boolean = false,
    ) = RelatorioDiagnosticoSnapshot(
        executionId = "exec-123",
        medidoEmEpochMs = 1_700_000_000_000L,
        geradoEmEpochMs = 1_700_000_500_000L,
        tipoRede = "Wi-Fi",
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        uploadNaoDetectado = uploadNaoDetectado,
        latenciaMs = latenciaMs,
        jitterMs = jitterMs,
        perdaPercentual = perdaPercentual,
        perdaEstimada = perdaEstimada,
        bufferbloatMs = bufferbloatMs,
        statusIntegridade = statusIntegridade,
        ssidMascarado = ssidMascarado,
        ipLocalMascarado = ipLocalMascarado,
        ipPublicoMascarado = ipPublicoMascarado,
        operadora = "Operadora Exemplo",
        versaoApp = "0.26.0",
        versaoMotor = "1.0.0",
        offline = offline,
    )

    @Test
    fun `nao contem nenhuma afirmacao regulatoria desatualizada`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase())

        assertFalse(html.contains("574/2011"))
        assertFalse(html.contains("mínimo garantido", ignoreCase = true))
        assertFalse(html.contains("obrigada a entregar", ignoreCase = true))
        assertFalse(html.contains("direito a desconto", ignoreCase = true))
        assertFalse(html.contains("Laudo Técnico", ignoreCase = true))
    }

    @Test
    fun `disclaimer informa carater informativo e nao oficial`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase())

        assertTrue(html.contains("informativa"))
        assertTrue(html.contains("Não é uma medição oficial da Anatel"))
    }

    @Test
    fun `metrica ausente aparece como nao medido, nunca zero`() {
        val html =
            RelatorioDiagnosticoHtmlBuilder.gerarHtml(
                snapshotBase(downloadMbps = null, latenciaMs = null, bufferbloatMs = null),
            )

        assertTrue(html.contains("Não medido"))
        assertFalse(html.contains(">0.0 Mbps<"))
        assertFalse(html.contains(">0 ms<"))
    }

    @Test
    fun `upload nao detectado nao aparece como zero`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase(uploadNaoDetectado = true, uploadMbps = 0.0))

        assertTrue(html.contains("Não detectado"))
    }

    @Test
    fun `perda estimada e identificada como estimada`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase(perdaEstimada = true, perdaPercentual = 1.5))

        assertTrue(html.contains("estimada"))
    }

    @Test
    fun `medicao e geracao aparecem em campos separados`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase())

        assertTrue(html.contains("Medição realizada em"))
        assertTrue(html.contains("Relatório gerado em"))
    }

    @Test
    fun `status de integridade nao completo mostra aviso`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase(statusIntegridade = "Contaminado"))

        assertTrue(html.contains("Integridade da medição"))
        assertTrue(html.contains("Contaminado"))
    }

    @Test
    fun `status completo nao mostra aviso de integridade`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase(statusIntegridade = "Completo"))

        assertFalse(html.contains("Integridade da medição"))
    }

    @Test
    fun `offline mostra aviso de ultima medicao salva`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase(offline = true))

        assertTrue(html.contains("sem conexão", ignoreCase = true))
    }

    @Test
    fun `rodape contem id de execucao e versoes`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase())

        assertTrue(html.contains("exec-123"))
        assertTrue(html.contains("0.26.0"))
        assertTrue(html.contains("1.0.0"))
    }

    @Test
    fun `ssid e ips aparecem mascarados, nunca o valor bruto`() {
        val html = RelatorioDiagnosticoHtmlBuilder.gerarHtml(snapshotBase())

        assertTrue(html.contains("192.168.1.*"))
        assertTrue(html.contains("200.150.*.*"))
        assertFalse(html.contains("192.168.1.100"))
    }
}
