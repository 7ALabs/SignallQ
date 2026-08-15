package io.signallq.app.ui.screen

import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.telephony.MovelSimSnapshot
import io.signallq.app.core.telephony.MovelSnapshot
import io.signallq.app.ui.lightTokens
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * GH#1206 — cobre os limites exatos de classificacao de sinal movel (4G/5G), o bug de
 * mistura de dados entre chips (item 1), e RSRP ausente nunca virando avaliacao positiva
 * (item 3/4).
 */
class SinalMovelClassificacaoTest {
    private fun sim(
        rsrpDbm: Int? = null,
        tecnologiaRede: String? = "4G",
        radioDesligado: Boolean = false,
        isDefaultData: Boolean = true,
        rsrqDb: Int? = null,
        sinrDb: Int? = null,
    ) = MovelSimSnapshot(
        subId = 1,
        simIndex = 1,
        operadora = "Operadora Teste",
        tecnologiaRede = tecnologiaRede,
        rsrpDbm = rsrpDbm,
        emRoaming = false,
        isDefaultData = isDefaultData,
        radioDesligado = radioDesligado,
        rsrqDb = rsrqDb,
        sinrDb = sinrDb,
    )

    private fun snapshot(
        rsrpDbm: Int? = -70,
        tecnologia: String? = "5G NSA",
        rsrqDb: Int? = null,
        sinrDb: Int? = null,
    ) = MovelSnapshot(
        operadora = "Operadora Snapshot",
        tecnologia = tecnologia,
        rsrpDbm = rsrpDbm,
        rsrqDb = rsrqDb,
        sinrDb = sinrDb,
        ecnoDb = null,
        bandaMovel = null,
        cellId = null,
        mcc = null,
        mnc = null,
        tac = null,
        roaming = false,
        timestampMs = 0L,
    )

    // ── RadioTech ────────────────────────────────────────────────────────────

    @Test
    fun `tecnologia 5G mapeia para NR_5G`() {
        assertEquals(MetricClassifier.RadioTech.NR_5G, radioTechDeTecnologia("5G NSA"))
        assertEquals(MetricClassifier.RadioTech.NR_5G, radioTechDeTecnologia("5G SA"))
    }

    @Test
    fun `qualquer outra tecnologia mapeia para LTE_4G`() {
        assertEquals(MetricClassifier.RadioTech.LTE_4G, radioTechDeTecnologia("4G"))
        assertEquals(MetricClassifier.RadioTech.LTE_4G, radioTechDeTecnologia("3G"))
        assertEquals(MetricClassifier.RadioTech.LTE_4G, radioTechDeTecnologia(null))
    }

    // ── Limites exatos 4G/5G (criterios de aceite da issue) ────────────────────

    @Test
    fun `4G em -78dBm e excelente`() {
        val dados = sim(rsrpDbm = -78, tecnologiaRede = "4G").paraDadosSinalMovel(null)
        assertEquals("Excelente", classificarQualidadeSinalMovel(dados, tokensFake()).label)
    }

    @Test
    fun `4G em -103dBm e ruim`() {
        val dados = sim(rsrpDbm = -103, tecnologiaRede = "4G").paraDadosSinalMovel(null)
        assertEquals("Ruim", classificarQualidadeSinalMovel(dados, tokensFake()).label)
    }

    @Test
    fun `5G em -93dBm e bom`() {
        val dados = sim(rsrpDbm = -93, tecnologiaRede = "5G NSA").paraDadosSinalMovel(null)
        assertEquals("Bom", classificarQualidadeSinalMovel(dados, tokensFake()).label)
    }

    @Test
    fun `5G em -108dBm e regular`() {
        val dados = sim(rsrpDbm = -108, tecnologiaRede = "5G NSA").paraDadosSinalMovel(null)
        assertEquals("Regular", classificarQualidadeSinalMovel(dados, tokensFake()).label)
    }

    // ── RSRP ausente nunca vira avaliacao positiva ─────────────────────────────

    @Test
    fun `rsrp nulo classifica qualidade como indisponivel`() {
        val dados = sim(rsrpDbm = null).paraDadosSinalMovel(null)
        assertEquals("Indisponível", classificarQualidadeSinalMovel(dados, tokensFake()).label)
    }

    @Test
    fun `rsrp nulo em 5G nunca classifica experiencia como boa ou otima`() {
        val dados = sim(rsrpDbm = null, tecnologiaRede = "5G NSA").paraDadosSinalMovel(null)
        val experiencia = classificarExperienciaMovel(dados, tokensFake())
        assertEquals("Inconclusivo", experiencia.label)
    }

    @Test
    fun `rsrp nulo em 4G nunca classifica experiencia como boa`() {
        val dados = sim(rsrpDbm = null, tecnologiaRede = "4G").paraDadosSinalMovel(null)
        val experiencia = classificarExperienciaMovel(dados, tokensFake())
        assertEquals("Inconclusivo", experiencia.label)
    }

    @Test
    fun `radio desligado exibe sem sinal, nao classifica piso do hardware`() {
        val dados = sim(rsrpDbm = -140, radioDesligado = true).paraDadosSinalMovel(null)
        assertEquals("Sem sinal", classificarQualidadeSinalMovel(dados, tokensFake()).label)
    }

    // ── RSRQ/SINR pior-caso (item 6) ────────────────────────────────────────────

    @Test
    fun `rsrq ruim rebaixa qualidade mesmo com rsrp bom`() {
        // RSRP -80 (excelente 4G) mas RSRQ -25 (pior que -20, ruim) -- pior das 2 vence.
        val dados = sim(rsrpDbm = -78, tecnologiaRede = "4G", rsrqDb = -25).paraDadosSinalMovel(null)
        assertEquals("Ruim", classificarQualidadeSinalMovel(dados, tokensFake()).label)
    }

    // ── Mistura de dados entre chips (item 1 — bug principal da issue) ────────

    @Test
    fun `sim secundario nao herda rsrp do snapshot geral`() {
        val simSecundario = sim(rsrpDbm = null, tecnologiaRede = null, isDefaultData = false)
        val snapshotGeral = snapshot(rsrpDbm = -70, tecnologia = "5G NSA")
        val dados = simSecundario.paraDadosSinalMovel(snapshotGeral)
        assertNull("SIM secundario sem RSRP proprio nao pode herdar do snapshot geral", dados.rsrpDbm)
        assertNull("SIM secundario sem tecnologia propria nao pode herdar do snapshot geral", dados.tecnologia)
    }

    @Test
    fun `sim padrao de dados pode complementar com snapshot geral`() {
        val simPadrao = sim(rsrpDbm = null, tecnologiaRede = null, isDefaultData = true)
        val snapshotGeral = snapshot(rsrpDbm = -70, tecnologia = "5G NSA")
        val dados = simPadrao.paraDadosSinalMovel(snapshotGeral)
        assertEquals(-70, dados.rsrpDbm)
        assertEquals("5G NSA", dados.tecnologia)
    }

    @Test
    fun `sim com dado proprio nunca e sobrescrito pelo snapshot geral`() {
        val simComDadoProprio = sim(rsrpDbm = -50, tecnologiaRede = "4G", isDefaultData = true)
        val snapshotGeral = snapshot(rsrpDbm = -110, tecnologia = "5G NSA")
        val dados = simComDadoProprio.paraDadosSinalMovel(snapshotGeral)
        assertEquals(-50, dados.rsrpDbm)
        assertEquals("4G", dados.tecnologia)
    }

    // ── GH#1258 — Home e Sinal devem concordar pro mesmo chip (print real do Luiz,
    // 2026-07-21: RSRP -79 dBm 5G NSA, RSRQ/SINR ruins o bastante pra puxar o veredito
    // geral pra Regular. Antes, Home usava mobileSignalQuality/simStatusLabel (so RSRP,
    // sem olhar RSRQ/SINR) e mostrava "Bom"; Sinal ja usava o caminho abaixo e mostrava
    // "Regular" -- corretamente rebaixado pelo pior status entre as 3 metricas). ────────

    @Test
    fun `rsrp -79 5G isolado e excelente mas rsrq e sinr ruins rebaixam para regular`() {
        val dadosViaSnapshot =
            snapshot(rsrpDbm = -79, tecnologia = "5G NSA", rsrqDb = -18, sinrDb = 5).paraDadosSinalMovel()
        assertEquals("Regular", classificarQualidadeSinalMovel(dadosViaSnapshot, tokensFake()).label)
    }

    @Test
    fun `Home (card CHIP MOVEL via sim) e Sinal (aba Movel via snapshot) concordam no mesmo veredito`() {
        // MobileChipsCard (Home, "CHIP MÓVEL") consome MovelSimSnapshot.paraDadosSinalMovel();
        // MobileSnapshotCard (SinalScreen, aba Móvel) consome MovelSnapshot.paraDadosSinalMovel().
        // Pro mesmo chip/instante, os dois caminhos tem que bater no mesmo veredito.
        val snapshotGeral = snapshot(rsrpDbm = -79, tecnologia = "5G NSA", rsrqDb = -18, sinrDb = 5)
        val simDoMesmoChip =
            sim(rsrpDbm = -79, tecnologiaRede = "5G NSA", rsrqDb = -18, sinrDb = 5, isDefaultData = true)

        val insightHome = classificarQualidadeSinalMovel(simDoMesmoChip.paraDadosSinalMovel(snapshotGeral), tokensFake())
        val insightSinal = classificarQualidadeSinalMovel(snapshotGeral.paraDadosSinalMovel(), tokensFake())

        assertEquals("Regular", insightHome.label)
        assertEquals(insightSinal.label, insightHome.label)
        assertEquals(insightSinal.color, insightHome.color)
    }

    private fun tokensFake() = lightTokens()
}
