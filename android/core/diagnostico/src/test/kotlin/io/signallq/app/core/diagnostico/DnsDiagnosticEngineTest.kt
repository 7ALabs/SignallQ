package io.signallq.app.core.diagnostico

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cobre os findings do [DnsDiagnosticEngine] que a issue #1840 passou a alimentar de
 * verdade pela primeira vez em producao: [MainViewModel.montarDnsInput] (android/app) agora
 * preenche `currentDnsLatencyMs`/`bestDnsNameFromComparison`/`bestDnsLatencyMsFromComparison`/
 * `dnsGrade`, que antes sempre chegavam `null` e nunca disparavam DNS-00/DNS-01/DNS-02/DNS-03/
 * DNS-REC-01 (ver PR #1851, achado do Caio).
 */
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

    @Test
    fun `dns between 50ms and 150ms generates info threshold finding`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "1.1.1.1",
                currentDnsName = "Cloudflare",
                currentDnsLatencyMs = 80,
                dnsComparisonAvailable = false,
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertTrue(r.any { it.id == "DNS-03" && it.status == DiagnosticStatus.info })
    }

    @Test
    fun `dns under 50ms generates no latency finding`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "1.1.1.1",
                currentDnsName = "Cloudflare",
                currentDnsLatencyMs = 30,
                dnsComparisonAvailable = false,
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertFalse(r.any { it.id in setOf("DNS-00", "DNS-01", "DNS-02", "DNS-03") })
    }

    @Test
    fun `missing current dns latency with comparison available generates inconclusive finding`() {
        val input =
            DnsDiagnosticInput(
                currentDnsLatencyMs = null,
                dnsComparisonAvailable = true,
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertTrue(r.any { it.id == "DNS-00" && it.status == DiagnosticStatus.inconclusive })
    }

    @Test
    fun `missing current dns latency without comparison generates no finding`() {
        val input =
            DnsDiagnosticInput(
                currentDnsLatencyMs = null,
                dnsComparisonAvailable = false,
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertTrue(r.isEmpty())
    }

    @Test
    fun `better dns found in comparison generates recommendation with grade in evidence`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "200.1.2.3",
                currentDnsName = "Operadora",
                currentDnsLatencyMs = 120,
                dnsComparisonAvailable = true,
                bestDnsNameFromComparison = "Cloudflare",
                bestDnsLatencyMsFromComparison = 20,
                dnsGrade = "B",
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        val rec = r.firstOrNull { it.id == "DNS-REC-01" }
        assertTrue(rec != null)
        assertTrue(rec!!.evidencia!!.contains("grade=B"))
        assertTrue(rec.mensagemUsuario.contains("Cloudflare"))
    }

    @Test
    fun `best dns same as current does not generate recommendation`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "1.1.1.1",
                currentDnsName = "Cloudflare",
                currentDnsLatencyMs = 20,
                dnsComparisonAvailable = true,
                bestDnsNameFromComparison = "Cloudflare",
                bestDnsLatencyMsFromComparison = 18,
                dnsGrade = "A",
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertFalse(r.any { it.id == "DNS-REC-01" })
    }

    @Test
    fun `best dns not meaningfully better than current does not generate recommendation`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "200.1.2.3",
                currentDnsName = "Operadora",
                currentDnsLatencyMs = 50,
                dnsComparisonAvailable = true,
                bestDnsNameFromComparison = "Cloudflare",
                bestDnsLatencyMsFromComparison = 48,
                dnsGrade = "B",
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertFalse(r.any { it.id == "DNS-REC-01" })
    }

    @Test
    fun `comparison unavailable does not generate recommendation even with best fields present`() {
        val input =
            DnsDiagnosticInput(
                currentDnsIp = "200.1.2.3",
                currentDnsName = "Operadora",
                currentDnsLatencyMs = 120,
                dnsComparisonAvailable = false,
                bestDnsNameFromComparison = "Cloudflare",
                bestDnsLatencyMsFromComparison = 20,
                dnsGrade = "B",
            )
        val r = DnsDiagnosticEngine.avaliar(input)
        assertFalse(r.any { it.id == "DNS-REC-01" })
    }
}
