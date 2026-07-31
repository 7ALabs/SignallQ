package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.FindingResult
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.recommendation.DiagnosticTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Fase 0 da auditoria #1228 — congela a divergencia estrutural entre os DOIS motores
 * chamados "RecommendationEngine" no repositorio:
 *
 * - `feature/diagnostico.RecommendationEngine` (este arquivo/pacote, REC-01..REC-14):
 *   decide se recomenda trocar para Wi-Fi 5GHz olhando o achado PRINCIPAL do
 *   [FindingEngine] -- se a causa raiz provavel e externa (`problemaExternoProvavel`,
 *   ex.: DNS/operadora/fibra), a recomendacao de Wi-Fi e explicitamente SUPRIMIDA
 *   (`recomendarWifi5Ghz`, ver o proprio codigo de producao).
 * - `core/recommendation.RecommendationEngine` (via [RecommendationRequestMapper]):
 *   deriva a tag `WIFI_FRACO` so olhando se existe um `DiagnosticResult` com id
 *   `WIFI-03`/`WIFI-04` em `report.wifiResultados` -- SEM NENHUMA exclusao equivalente
 *   ao `problemaExternoProvavel` do motor legado.
 *
 * Resultado: para o MESMO diagnostico (mesmo `DiagnosticInput`, mesmo achado
 * principal), o motor legado explicitamente decide NAO recomendar o ajuste de Wi-Fi,
 * enquanto o motor novo (`core/recommendation`) recebe uma tag que sinaliza
 * exatamente esse problema como relevante. Ver
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, Parte 8, P1-1/P1-2.
 *
 * Este teste nao decide qual dos dois esta "certo" -- so prova, com um cenario
 * concreto, que os dois podem divergir hoje.
 */
class RecommendationEnginesDivergenceCharacterizationTest {
    private fun wifiFracoNaBanda24Input(): DiagnosticInput = DiagnosticInput(
        connectionType = ConnectionType.wifi,
        wifi = WifiDiagnosticInput(
            rssiDbm = -68, // acima do piso de -70 que bloquearia a recomendacao de troca
            linkSpeedMbps = 72, // < 144, dispara a condicao "linkBaixo"
            frequenciaMhz = 2412, // 2.4GHz
            is5GhzCapable = true,
        ),
        internet = InternetDiagnosticInput(
            downloadMbps = 15.0,
            uploadMbps = 10.0,
            latencyMs = 20.0,
            jitterMs = 5.0,
            perdaPercentual = 0.0,
        ),
    )

    private fun wifiFracoWifiResultados(): List<DiagnosticResult> = listOf(
        DiagnosticResult(
            id = "WIFI-03",
            titulo = "Sinal Wi-Fi fraco",
            status = DiagnosticStatus.attention,
            evidencia = "rssi=-68dBm",
            mensagemUsuario = "O sinal Wi-Fi está fraco.",
            recomendacao = null,
            categoria = "wifi",
        ),
    )

    @Test
    fun `motor legado suprime REC-01 quando o achado principal aponta causa externa`() {
        val input = wifiFracoNaBanda24Input()
        val achadoExterno = DiagnosticResult(
            id = "DECISAO-DNS-01",
            titulo = "DNS lento",
            status = DiagnosticStatus.critical,
            evidencia = null,
            mensagemUsuario = "O DNS configurado está lento.",
            recomendacao = null,
            categoria = "dns",
        )
        val achados = FindingResult(principal = achadoExterno)

        val recomendacoes = RecommendationEngine.recomendar(input, achados)

        // Caracteriza o comportamento atual: nenhuma recomendacao de troca pra 5GHz
        // (REC-01) e gerada quando a causa raiz principal e externa (DNS, neste caso).
        assertFalse(recomendacoes.any { it.id == "REC-01" })
    }

    @Test
    fun `motor novo core-recommendation deriva tag WIFI_FRACO independente da causa raiz principal`() {
        val input = wifiFracoNaBanda24Input()
        val decisaoExterna = DiagnosticResult(
            id = "DECISAO-DNS-01",
            titulo = "DNS lento",
            status = DiagnosticStatus.critical,
            evidencia = null,
            mensagemUsuario = "O DNS configurado está lento.",
            recomendacao = null,
            categoria = "dns",
        )
        val report = DiagnosticReport(
            wifiResultados = wifiFracoWifiResultados(),
            internetResultados = emptyList(),
            fibraResultados = emptyList(),
            decisao = decisaoExterna,
            geradoEmMs = 0L,
        )

        val request = RecommendationRequestMapper.map(report = report, input = input)

        // Caracteriza a divergencia: RecommendationRequestMapper.mapTags() ve o
        // DiagnosticResult "WIFI-03" em report.wifiResultados e adiciona WIFI_FRACO,
        // MESMO com a mesma causa raiz externa (DNS) que fez o motor legado suprimir
        // REC-01 no teste acima -- os dois motores partem do mesmo relatorio e chegam
        // a conclusoes estruturalmente diferentes sobre se "sugerir ajuste de Wi-Fi" e
        // relevante.
        assertTrue(request.tags.contains(DiagnosticTag.WIFI_FRACO))
    }
}
