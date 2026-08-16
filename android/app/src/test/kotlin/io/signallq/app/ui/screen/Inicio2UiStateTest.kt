package io.signallq.app.ui.screen

import io.mockk.every
import io.mockk.mockk
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.SnapshotRede
import io.signallq.app.feature.diagnostico.EstadoDiagnostico
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.home.MetricasMedicaoHome
import io.signallq.app.feature.home.OrigemMedicaoHome
import io.signallq.app.feature.home.ResolvedHomeMeasurement
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class Inicio2UiStateTest {
    private val idle = SnapshotDiagnostico(EstadoDiagnostico.idle, null, null)

    @Test
    fun `mapeia wifi movel offline e carregando sem fonte paralela`() {
        assertEquals(Inicio2Conexao.Wifi, map(rede(EstadoConexao.wifi), idle).conexao)
        assertEquals(Inicio2Conexao.Movel, map(rede(EstadoConexao.movel), idle).conexao)
        assertEquals(Inicio2Conexao.Offline, map(rede(EstadoConexao.desconectado), idle).conexao)
        assertEquals(Inicio2Conexao.Carregando, map(rede(EstadoConexao.desconhecido), idle).conexao)
    }

    @Test
    fun `relatorio atual e valido e medicao persistida isolada e expirada`() {
        val report = mockk<DiagnosticReport>()
        every { report.veredito } returns "Bom"
        val valida = map(rede(EstadoConexao.wifi), SnapshotDiagnostico(EstadoDiagnostico.concluido, report, null))
        assertEquals("Bom", (valida.analise as Inicio2Analise.Valida).veredito)

        val expirada = map(rede(EstadoConexao.wifi), idle, medicaoAnterior())
        assertEquals(123L, (expirada.analise as Inicio2Analise.Expirada).timestampEpochMs)
    }

    @Test
    fun `execucao e erro preservam loading e interrompido`() {
        assertTrue(map(rede(EstadoConexao.wifi), idle, estado = EstadoExecucaoSpeedtest.executando).analise is Inicio2Analise.Carregando)
        assertTrue(map(rede(EstadoConexao.wifi), idle, estado = EstadoExecucaoSpeedtest.erro).analise is Inicio2Analise.Interrompida)
    }

    @Test
    fun `feature flag canonica mantem Inicio2 opt in e Legacy como fallback`() {
        assertFalse(AppShellMode.Legacy.usaInicio2())
        assertTrue(AppShellMode.Guided2.usaInicio2())
    }

    private fun map(
        rede: SnapshotRede,
        diagnostico: SnapshotDiagnostico,
        medicao: ResolvedHomeMeasurement? = null,
        estado: EstadoExecucaoSpeedtest = EstadoExecucaoSpeedtest.idle,
    ) = Inicio2UiStateMapper.map(rede, estado, diagnostico, medicao)

    private fun rede(estado: EstadoConexao) =
        SnapshotRede.desconectado(0L).copy(estadoConexao = estado, conectado = estado != EstadoConexao.desconectado)

    private fun medicaoAnterior() =
        ResolvedHomeMeasurement(
            MetricasMedicaoHome(null, null, null, null, null, 123L, "wifi", null, null, null, true),
            OrigemMedicaoHome.ANTERIOR,
        )
}
