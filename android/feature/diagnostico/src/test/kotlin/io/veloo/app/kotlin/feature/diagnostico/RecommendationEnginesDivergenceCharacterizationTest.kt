package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticResult
import io.signallq.app.core.diagnostico.DiagnosticRunner
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.DnsDiagnosticInput
import io.signallq.app.core.diagnostico.FindingResult
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.recommendation.DiagnosticTag
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Issue #1528 (P1-2 da auditoria #1228) -- RESOLVIDO. Ate aqui, os dois motores de
 * recomendacao do repositorio partiam do mesmo relatorio e podiam chegar a conclusoes
 * diferentes sobre a mesma causa raiz:
 *
 * - `feature/diagnostico.RecomendacaoPraticaEngine` (REC-01..REC-14): decide se recomenda
 *   trocar para Wi-Fi 5GHz olhando o achado PRINCIPAL do [FindingEngine] -- se a causa raiz
 *   provavel e externa (`problemaExternoProvavel`, ex.: DNS/operadora/fibra), a recomendacao
 *   de Wi-Fi e explicitamente SUPRIMIDA (`recomendarWifi5Ghz`, ver o proprio codigo de
 *   producao).
 * - `core/recommendation.RecommendationEngine` (via [RecommendationRequestMapper]): ate a
 *   #1528, derivava a tag `WIFI_FRACO` so olhando se existia um `DiagnosticResult` com id
 *   `WIFI-03`/`WIFI-04` em `report.wifiResultados` -- SEM NENHUMA exclusao equivalente ao
 *   `problemaExternoProvavel` do motor legado.
 *
 * A partir da #1528, [RecommendationRequestMapper.mapTags] passou a ler
 * `report.recomendacoes` (saida real do [RecomendacaoPraticaEngine]) para as tags com regra
 * REC-01..14 equivalente direta -- a tag so aparece quando a REC correspondente de fato
 * disparou, herdando as mesmas exclusoes/gatilhos do motor legado. Este arquivo cobre os 3
 * cenarios que provam a correcao. Ver
 * `docs_ai/ARQUITETURA/AUDITORIA_1228_FASE0_INVENTARIO_COMPLETO.md`, Parte 8, P1-1/P1-2.
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

        val recomendacoes = RecomendacaoPraticaEngine.recomendar(input, achados)

        // Caracteriza o comportamento atual: nenhuma recomendacao de troca pra 5GHz
        // (REC-01) e gerada quando a causa raiz principal e externa (DNS, neste caso).
        assertFalse(recomendacoes.any { it.id == "REC-01" })
    }

    @Test
    fun `mapTags nao gera WIFI_FRACO quando REC-01 e suprimido por causa externa`() {
        // Mesmo input base do teste acima, mas com DNS realmente critico (latencia > 300ms)
        // para que o achado principal SAIA do proprio pipeline (FindingEngine), nao seja
        // montado a mao -- currentDnsLatencyMs=320 gera DNS-01 critico, que faz
        // DECISAO-DNS-01 vencer o desempate por score contra o achado de Wi-Fi (rssi=-68 so
        // gera WIFI-03/attention, confianca menor que a correlacao DNS critica).
        val input = wifiFracoNaBanda24Input().copy(
            dns = DnsDiagnosticInput(
                currentDnsName = "operadora",
                currentDnsLatencyMs = 320,
            ),
        )

        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

        // Confirma a premissa: o achado principal real desse input e mesmo a causa externa
        // (DNS), e por isso REC-01 nao esta nas recomendacoes.
        assertTrue("esperava achado principal DECISAO-DNS-01", report.decisao.id == "DECISAO-DNS-01")
        assertFalse(report.recomendacoes.any { it.id == "REC-01" })
        // WIFI-03 (sinal fraco) continua presente no achado bruto -- e exatamente esse
        // achado bruto que o mapper antigo usava sozinho para gerar WIFI_FRACO.
        assertTrue(report.wifiResultados.any { it.id == "WIFI-03" })

        val request = RecommendationRequestMapper.map(report = report, input = input)

        // Correcao da #1528: como nem REC-01 nem REC-02 dispararam para este achado
        // principal, a tag comercial tambem nao aparece -- os dois motores concordam agora.
        assertFalse(request.tags.contains(DiagnosticTag.WIFI_FRACO))
    }

    @Test
    fun `mapTags nao gera DNS_LENTO quando REC-06 nao dispara mesmo com DNS-02 bruto presente`() {
        // DNS atual lento (200ms, dispara DNS-02 bruto) mas SEM alternativa de DNS melhor no
        // comparativo -- REC-06 (recomendarDnsLento) exige bestDnsLatencyMsFromComparison/
        // bestDnsNameFromComparison com margem segura de 5ms para disparar; sem isso, retorna
        // null mesmo com o achado bruto DNS-02 presente.
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            wifi = WifiDiagnosticInput(
                rssiDbm = -50,
                linkSpeedMbps = 300,
                frequenciaMhz = 5200,
            ),
            internet = InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 20.0,
                latencyMs = 15.0,
                jitterMs = 3.0,
                perdaPercentual = 0.0,
            ),
            dns = DnsDiagnosticInput(
                currentDnsName = "operadora",
                currentDnsLatencyMs = 200,
            ),
        )

        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

        assertTrue("esperava DNS-02 no achado bruto", report.dnsResultados.any { it.id == "DNS-02" })
        assertFalse("REC-06 nao deveria disparar sem alternativa de DNS no comparativo", report.recomendacoes.any { it.id == "REC-06" })

        val request = RecommendationRequestMapper.map(report = report, input = input)

        // Prova que o motor comercial nao cria recomendacao que o diagnostico descartou --
        // mesmo com evidencia bruta (DNS-02) sugerindo que sim.
        assertFalse(request.tags.contains(DiagnosticTag.DNS_LENTO))
    }

    @Test
    fun `mapTags gera BUFFERBLOAT_ALTO quando REC-05 dispara normalmente`() {
        // Cenario sem nenhuma causa externa concorrendo -- REC-05 (bufferbloat) nao tem
        // gating por achado principal, dispara direto quando bufferbloatMs > 30ms.
        val input = DiagnosticInput(
            connectionType = ConnectionType.wifi,
            wifi = WifiDiagnosticInput(
                rssiDbm = -50,
                linkSpeedMbps = 300,
                frequenciaMhz = 5200,
            ),
            internet = InternetDiagnosticInput(
                downloadMbps = 100.0,
                uploadMbps = 20.0,
                latencyMs = 15.0,
                jitterMs = 3.0,
                perdaPercentual = 0.0,
                bufferbloatMs = 150.0,
            ),
        )

        val report = DiagnosticRunner.run(input, gerarRecomendacoes = RecomendacaoPraticaEngine::recomendar)

        assertTrue("esperava REC-05 nas recomendacoes", report.recomendacoes.any { it.id == "REC-05" })

        val request = RecommendationRequestMapper.map(report = report, input = input)

        assertTrue(request.tags.contains(DiagnosticTag.BUFFERBLOAT_ALTO))
    }
}
