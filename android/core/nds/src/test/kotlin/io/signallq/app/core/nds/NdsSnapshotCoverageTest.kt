package io.signallq.app.core.nds

import io.signallq.app.core.diagnostico.ConnectionType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobertura de [analyzeNdsSnapshotCoverage] (NDS-Snapshot-12, issue #1844). Monta
 * [NdsDiagnosticsRequest] diretamente (sem passar por [toNdsDiagnosticsRequest]) para isolar a
 * análise de cobertura do mapper — evita colisão com a fatia concorrente da issue #1842
 * (proveniência/`source`), que mexe no mapper e no request.
 */
class NdsSnapshotCoverageTest {

    private fun requestBase(
        wifi: NdsWifiInfo? = null,
        wifiScan: NdsWifiScanInfo? = null,
        speed: NdsSpeedInfo? = null,
        mobile: NdsMobileInfo? = null,
        connection: NdsConnectionInfo? = NdsConnectionInfo(type = "WIFI"),
    ) = NdsDiagnosticsRequest(
        requestId = "req-1",
        app = NdsAppInfo(id = "io.signallq.app", version = "1.0.0"),
        connection = connection,
        wifi = wifi,
        wifiScan = wifiScan,
        speed = speed,
        mobile = mobile,
    )

    @Test
    fun `snapshot saudavel wifi - speed e wifi presentes, nenhum bloco critico ausente`() {
        val request = requestBase(
            wifi = NdsWifiInfo(rssi = -55),
            speed = NdsSpeedInfo(downloadMbps = 200.0),
        )

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.wifi, mobileCapturaReduzida = false)

        assertTrue(coverage.blocksPresent.contains("connection"))
        assertTrue(coverage.blocksPresent.contains("wifi"))
        assertTrue(coverage.blocksPresent.contains("speed"))
        assertTrue("nenhum bloco critico deveria faltar", coverage.missingCriticalBlocks.isEmpty())
    }

    @Test
    fun `wifi ausente em conexao wifi - wifi entra em missingCriticalBlocks com motivo no_data`() {
        val request = requestBase(speed = NdsSpeedInfo(downloadMbps = 100.0))

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.wifi, mobileCapturaReduzida = false)

        assertTrue(coverage.missingCriticalBlocks.contains("wifi"))
        val wifiCoverage = coverage.blocks.first { it.block == NdsSnapshotBlock.WIFI }
        assertFalse(wifiCoverage.present)
        assertEquals("no_data", wifiCoverage.missingReason)
    }

    @Test
    fun `wifi ausente fora de conexao wifi - nao e critico, motivo not_wifi`() {
        val request = requestBase(speed = NdsSpeedInfo(downloadMbps = 100.0), connection = NdsConnectionInfo(type = "MOBILE"))

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.mobile, mobileCapturaReduzida = false)

        assertFalse(coverage.missingCriticalBlocks.contains("wifi"))
        val wifiCoverage = coverage.blocks.first { it.block == NdsSnapshotBlock.WIFI }
        assertEquals("not_wifi", wifiCoverage.missingReason)
    }

    @Test
    fun `mobile ausente em conexao mobile sem permissao - motivo no_permission e critico`() {
        val request = requestBase(speed = NdsSpeedInfo(downloadMbps = 50.0), connection = NdsConnectionInfo(type = "MOBILE"))

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.mobile, mobileCapturaReduzida = true)

        assertTrue(coverage.missingCriticalBlocks.contains("mobile"))
        val mobileCoverage = coverage.blocks.first { it.block == NdsSnapshotBlock.MOBILE }
        assertEquals("no_permission", mobileCoverage.missingReason)
    }

    @Test
    fun `mobile ausente fora de conexao mobile - motivo not_mobile, nao critico`() {
        val request = requestBase(wifi = NdsWifiInfo(rssi = -60), speed = NdsSpeedInfo(downloadMbps = 100.0))

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.wifi, mobileCapturaReduzida = false)

        assertFalse(coverage.missingCriticalBlocks.contains("mobile"))
        val mobileCoverage = coverage.blocks.first { it.block == NdsSnapshotBlock.MOBILE }
        assertEquals("not_mobile", mobileCoverage.missingReason)
    }

    @Test
    fun `speed ausente - sempre critico, independente do tipo de conexao`() {
        val request = requestBase(wifi = NdsWifiInfo(rssi = -60))

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.wifi, mobileCapturaReduzida = false)

        assertTrue(coverage.missingCriticalBlocks.contains("speed"))
        val speedCoverage = coverage.blocks.first { it.block == NdsSnapshotBlock.SPEED }
        assertEquals("no_measurement", speedCoverage.missingReason)
    }

    @Test
    fun `wifiScan ausente em conexao wifi - motivo no_permission, nunca critico`() {
        val request = requestBase(wifi = NdsWifiInfo(rssi = -60), speed = NdsSpeedInfo(downloadMbps = 100.0))

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.wifi, mobileCapturaReduzida = false)

        assertFalse(coverage.missingCriticalBlocks.contains("wifiScan"))
        val wifiScanCoverage = coverage.blocks.first { it.block == NdsSnapshotBlock.WIFI_SCAN }
        assertEquals("no_permission", wifiScanCoverage.missingReason)
    }

    @Test
    fun `toDebugLogLines segue exatamente o formato bloco=present ou bloco=missing-motivo`() {
        val request = requestBase(
            wifi = NdsWifiInfo(rssi = -55),
            speed = NdsSpeedInfo(downloadMbps = 200.0),
        )

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.wifi, mobileCapturaReduzida = false)
        val linhas = coverage.toDebugLogLines()

        assertTrue(linhas.contains("speed=present"))
        assertTrue(linhas.contains("wifi=present"))
        assertTrue(linhas.contains("wifiScan=missing:no_permission"))
        assertTrue(linhas.contains("mobile=missing:not_mobile"))
    }

    @Test
    fun `fieldsPresentCount cresce conforme mais campos sao preenchidos`() {
        val requestPobre = requestBase(wifi = NdsWifiInfo(rssi = -60))
        val requestRico = requestBase(
            wifi = NdsWifiInfo(rssi = -60, band = "5GHz", channel = 44, linkSpeed = 400, standard = "ac"),
            speed = NdsSpeedInfo(downloadMbps = 200.0, uploadMbps = 50.0, pingMs = 12.0, jitterMs = 2.0, packetLossPercent = 0.0),
        )

        val coveragePobre = analyzeNdsSnapshotCoverage(requestPobre, ConnectionType.wifi, mobileCapturaReduzida = false)
        val coverageRico = analyzeNdsSnapshotCoverage(requestRico, ConnectionType.wifi, mobileCapturaReduzida = false)

        assertTrue(coverageRico.fieldsPresentCount > coveragePobre.fieldsPresentCount)
    }

    @Test
    fun `bloco nulo nunca conta campos - fieldsPresentCount so soma blocos presentes`() {
        val request = requestBase()

        val coverage = analyzeNdsSnapshotCoverage(request, ConnectionType.wifi, mobileCapturaReduzida = false)

        // app.id + app.version + locale + connection.type = 4 campos sempre presentes.
        assertEquals(4, coverage.fieldsPresentCount)
    }
}
