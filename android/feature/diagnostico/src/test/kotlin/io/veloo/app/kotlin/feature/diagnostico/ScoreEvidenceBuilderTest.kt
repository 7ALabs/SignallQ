package io.signallq.app.feature.diagnostico

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Testes do [ScoreEvidenceBuilder] (SIG-288): traducao de [DiagnosticInput] em
 * [EvidenceScore] por dimensao, e integracao ponta-a-ponta via [DiagnosticRunner] +
 * [DiagnosticReport.scoreConexao].
 */
class ScoreEvidenceBuilderTest {

    // ── tipoConexao ──────────────────────────────────────────────────────────

    @Test
    fun `tipoConexao movel quando connectionType e mobile`() {
        val input = DiagnosticInput(connectionType = ConnectionType.mobile)
        assertEquals(ScoreEngine.TipoConexao.MOVEL, ScoreEvidenceBuilder.tipoConexao(input))
    }

    @Test
    fun `tipoConexao fibra quando fibra esta up mesmo em connectionType wifi`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            fibra = FibraDiagnosticInput(isUp = true, rxPowerDbm = -20.0, txPowerDbm = 2.0, temperatureCelsius = 40.0),
        )
        assertEquals(ScoreEngine.TipoConexao.FIBRA, ScoreEvidenceBuilder.tipoConexao(input))
    }

    @Test
    fun `tipoConexao wifi quando connectionType wifi sem fibra`() {
        val input = DiagnosticInput(connectionType = ConnectionType.wifi)
        assertEquals(ScoreEngine.TipoConexao.WIFI, ScoreEvidenceBuilder.tipoConexao(input))
    }

    @Test
    fun `tipoConexao desconhecido no caso default`() {
        val input = DiagnosticInput(connectionType = ConnectionType.desconectado)
        assertEquals(ScoreEngine.TipoConexao.DESCONHECIDO, ScoreEvidenceBuilder.tipoConexao(input))
    }

    // ── Integracao: DiagnosticRunner popula scoreEngineResultado e scoreConexao usa ele ──

    @Test
    fun `diagnostico wifi saudavel gera score alto via ScoreEngine`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 200.0, uploadMbps = 50.0, latencyMs = 15.0, jitterMs = 2.0,
                perdaPercentual = 0.0, bufferbloatMs = 5.0, packetLossSource = "medida",
            ),
            wifi = WifiDiagnosticInput(rssiDbm = -45, linkSpeedMbps = 400, frequenciaMhz = 5180),
            dns = DnsDiagnosticInput(currentDnsLatencyMs = 20),
        )
        val report = DiagnosticRunner.run(input)
        assertTrue(report.scoreEngineResultado?.score != null)
        assertTrue("score=${report.scoreConexao}", report.scoreConexao >= 85)
    }

    @Test
    fun `diagnostico com perda de pacotes real critica aplica teto mesmo com resto saudavel`() {
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 200.0, uploadMbps = 50.0, latencyMs = 15.0, jitterMs = 2.0,
                perdaPercentual = 5.0, bufferbloatMs = 5.0, packetLossSource = "modem",
            ),
            wifi = WifiDiagnosticInput(rssiDbm = -45, linkSpeedMbps = 400, frequenciaMhz = 5180),
            dns = DnsDiagnosticInput(currentDnsLatencyMs = 20),
        )
        val report = DiagnosticRunner.run(input)
        assertTrue("score=${report.scoreConexao}", report.scoreConexao <= ScoreEngine.TETO_PERDA_PACOTES_CRITICA)
    }

    @Test
    fun `diagnostico sem nenhum dado bruto cai para tabela legada baseada em decisao`() {
        val input = DiagnosticInput(connectionType = ConnectionType.desconhecido)
        val report = DiagnosticRunner.run(input)
        assertEquals(null, report.scoreEngineResultado?.score)
        // sem internet input, IN-NORMAL-00 (inconclusive) dispara DECISAO-INC ->
        // tabela legada = 50 (inconclusive).
        assertEquals(50, report.scoreConexao)
    }

    @Test
    fun `fibra critica pondera mais no score de conexao fibra do que em wifi`() {
        val inputFibra = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            internet = InternetDiagnosticInput(
                downloadMbps = 200.0, uploadMbps = 50.0, latencyMs = 15.0, jitterMs = 2.0,
                perdaPercentual = 0.0, bufferbloatMs = 5.0,
            ),
            wifi = WifiDiagnosticInput(rssiDbm = -45, linkSpeedMbps = 400, frequenciaMhz = 5180),
            dns = DnsDiagnosticInput(currentDnsLatencyMs = 20),
            fibra = FibraDiagnosticInput(isUp = true, rxPowerDbm = -30.0, txPowerDbm = 2.0, temperatureCelsius = 40.0),
        )
        val report = DiagnosticRunner.run(inputFibra)
        assertEquals(ScoreEngine.TipoConexao.FIBRA, ScoreEvidenceBuilder.tipoConexao(inputFibra))
        assertTrue("score=${report.scoreConexao}", report.scoreConexao <= ScoreEngine.TETO_FIBRA_RX_CRITICA)
    }
}
