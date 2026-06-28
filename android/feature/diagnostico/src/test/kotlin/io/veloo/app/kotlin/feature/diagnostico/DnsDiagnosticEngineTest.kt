package io.signallq.app.feature.diagnostico

import org.junit.Assert.assertTrue
import org.junit.Test

class DnsDiagnosticEngineTest {

    @Test
    fun `dns over 150ms generates attention`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "1.1.1.1",
                currentDnsName = "Cloudflare",
                currentDnsLatencyMs = 151,
                dnsComparisonAvailable = false,
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertTrue(r.any { it.id == "DNS-02" && it.status == DiagnosticStatus.attention })
    }

    @Test
    fun `dns over 300ms generates critical`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "1.1.1.1",
                currentDnsName = "Cloudflare",
                currentDnsLatencyMs = 301,
                dnsComparisonAvailable = false,
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertTrue(r.any { it.id == "DNS-01" && it.status == DiagnosticStatus.critical })
    }
}

