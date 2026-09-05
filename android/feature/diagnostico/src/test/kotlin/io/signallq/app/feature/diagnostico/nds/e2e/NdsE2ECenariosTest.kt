package io.signallq.app.feature.diagnostico.nds.e2e

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticContext
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.MobileDiagnosticInput
import io.signallq.app.core.diagnostico.RedeWifiVizinha
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.diagnostico.WifiScanDiagnosticInput
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.feature.diagnostico.nds.NdsDiagnosticRepository
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * NDS-Snapshot-11 (issue #1843, épico #1832) — teste E2E cobrindo a cadeia
 * completa exigida pelo critério de aceite:
 *
 * ```
 * dado coletado -> DiagnosticInput -> snapshot (NdsDiagnosticsRequest) -> JSON
 * -> NDS -> motor/IA -> NdsDiagnosticsResponse -> DiagnosticReport
 * ```
 *
 * Cobre os dois cenários citados pelo Definition of Done de produto da
 * issue-mãe (seção 21): "Wi-Fi congestionado" e "móvel com sinal fraco".
 *
 * ## Por que MockWebServer em vez do staging real do NDS
 * Testar contra o NDS real (`network-diagnostics-service`) exigiria rede,
 * credenciais e um ambiente de staging vivo — nenhum dos três está disponível
 * no CI (`android-ci.yml` roda testes JVM isolados, sem acesso a serviços
 * externos; `AGENTS.md`, "Restrições"/"Comandos essenciais"). A mesma decisão
 * já foi tomada por `NdsClientTest` (NDS-01, #1744) e
 * `NdsDiagnosticRepositoryTest` (NDS-02k, #1759): um `MockWebServer` real
 * simula o contrato HTTP (path, headers, corpo JSON) ponta a ponta — o
 * `NdsClient` faz uma chamada HTTP de verdade, só que contra loopback, então
 * nada no caminho App -> NDS é substituído por um dublê em memória. O
 * resultado roda 100% determinístico no CI, sem depender de rede externa nem
 * de segredo (`NDS_API_TOKEN`) de produção.
 *
 * ## Fixtures
 * O repositório irmão `network-diagnostics-service` (auditado em 2026-09-04,
 * checkout local em `../network-diagnostics-service`) não possui um diretório
 * de fixtures JSON versionadas para contract tests — os testes em `tests/api`
 * cobre regras de contrato com asserts inline em TypeScript (Zod schemas),
 * não fixtures exportáveis, e não há pasta `fixtures/`. As respostas abaixo
 * foram construídas localmente a partir de três fontes confirmadas neste
 * repositório e no irmão:
 * - vocabulário fechado de veredicto em [io.signallq.app.core.nds.NdsSeverityParser]
 *   (excelente/bom/regular/ruim/critico/inconclusivo, ADR-017 §5, #1746);
 * - shape do envelope v1 confirmado por `NdsClientTest`
 *   (`results[].module/module_version/request_id/warnings/missing_inputs/result/cards`);
 * - `docs/api-contract.md` do `network-diagnostics-service` (capabilities,
 *   profiles e o envelope de erro canônico).
 * Se `network-diagnostics-service` publicar fixtures dedicadas no futuro,
 * este teste deve migrar para consumi-las em vez das constantes locais
 * abaixo — ver dívida registrada no PR desta issue.
 */
class NdsE2ECenariosTest {

    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        try {
            server.shutdown()
        } catch (_: Exception) {
            // já encerrado pelo próprio teste — nada a fazer.
        }
    }

    private fun repository() = NdsDiagnosticRepository(
        ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "test-token", client = OkHttpClient()),
    )

    // -------------------------------------------------------------------
    // Cenário 1 — Wi-Fi congestionado (issue-mãe #1832, seção 21).
    // Esperado: identificar interferência/rede vizinha, SEM culpar o ISP.
    // -------------------------------------------------------------------

    private fun inputWifiCongestionado() = DiagnosticInput(
        connectionType = ConnectionType.wifi,
        wifi = WifiDiagnosticInput(
            rssiDbm = -58,
            linkSpeedMbps = 150,
            frequenciaMhz = 2437, // canal 6, 2.4GHz
            ssid = "MinhaRede_24G",
            canal = 6,
            wifiStandard = "802.11n",
        ),
        wifiScan = WifiScanDiagnosticInput(
            conectadoCanal = 6,
            conectadoBanda = BandaWifi.ghz24,
            redes = listOf(
                RedeWifiVizinha(canal = 6, rssiDbm = -50, frequenciaMhz = 2437, ssid = "Vizinha_1"),
                RedeWifiVizinha(canal = 6, rssiDbm = -55, frequenciaMhz = 2437, ssid = "Vizinha_2"),
                RedeWifiVizinha(canal = 6, rssiDbm = -60, frequenciaMhz = 2437, ssid = "Vizinha_3"),
                RedeWifiVizinha(canal = 1, rssiDbm = -70, frequenciaMhz = 2412, ssid = "Vizinha_4"),
                RedeWifiVizinha(canal = 11, rssiDbm = -75, frequenciaMhz = 2462, ssid = "Vizinha_5"),
            ),
        ),
        internet = InternetDiagnosticInput(
            downloadMbps = 180.0,
            uploadMbps = 40.0,
            latencyMs = 28.0,
            jitterMs = 9.0,
            perdaPercentual = 0.5,
        ),
        context = DiagnosticContext(objective = "SITES_DEMORAM"),
        executionId = "exec-e2e-wifi-congestionado",
    )

    /** Corpo de resposta do NDS simulando um veredicto "ruim" causado por
     *  congestionamento de canal — card explica a rede vizinha, sem citar ISP. */
    private val respostaWifiCongestionado =
        """
        {
          "recommendation": {
            "id": "trocar_canal_wifi",
            "type": "diagnostic",
            "title": "Troque o canal do seu Wi-Fi",
            "description": "Seu roteador está competindo com várias redes vizinhas no mesmo canal.",
            "source_finding_ids": ["wifi_channel_congested"],
            "steps": ["Acesse as configurações do roteador.", "Troque para o canal 11 ou 1."]
          },
          "results": [
            {
              "module": "diagnostics.wifi",
              "module_version": "1.0.0",
              "request_id": "exec-e2e-wifi-congestionado",
              "warnings": [],
              "missing_inputs": [],
              "result": { "matched_rules": ["wifi_channel_congested"] },
              "cards": [
                {
                  "id": "wifi_channel_congested",
                  "titulo": "Canal Wi-Fi congestionado",
                  "status": "attention",
                  "mensagemUsuario": "Encontramos 3 redes vizinhas usando o mesmo canal (6) que a sua. Isso reduz a velocidade real da sua conexão Wi-Fi.",
                  "categoria": "wifi",
                  "podeConcluir": true
                }
              ]
            },
            {
              "module": "scoring",
              "module_version": "1.1.0",
              "request_id": "exec-e2e-wifi-congestionado",
              "warnings": [],
              "missing_inputs": [],
              "result": { "score": 58, "veredicto": "ruim", "tipo_conexao": "WIFI", "observed_dimensions": 2, "dimensoes": [] }
            },
            {
              "module": "ai",
              "module_version": "1.5.0",
              "request_id": "exec-e2e-wifi-congestionado",
              "warnings": [],
              "missing_inputs": [],
              "result": {
                "tokens_used": 120,
                "ai_model_used": "claude-haiku",
                "fallback_used": false,
                "explanation_source": "ai",
                "explanation_status": "ok",
                "explanation": {
                  "titulo_amigavel": "Seu Wi-Fi está disputando espaço com redes vizinhas",
                  "resumo_tecnico_traduzido": "O canal 6 do seu Wi-Fi tem 3 redes vizinhas competindo pelo mesmo espaço, o que derruba a velocidade real. Sua internet contratada está OK — o gargalo é local, no ar entre o roteador e o celular."
                },
                "source_finding_ids": ["wifi_channel_congested"]
              }
            }
          ],
          "traces": [
            { "module": "wifi", "duration_ms": 4, "status": "ok" },
            { "module": "ai", "duration_ms": 210, "status": "ok", "source": "ai" }
          ]
        }
        """.trimIndent()

    @Test
    fun `wifi congestionado - snapshot enviado ao NDS carrega wifiScan com as redes vizinhas`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(respostaWifiCongestionado))

        repository().evaluate(inputWifiCongestionado())

        val corpoEnviado = JSONObject(server.takeRequest().body.readUtf8())

        // Prova que o gap historico da issue-mae (#1832) -- `wifiScan = null` hardcoded
        // mesmo com redes vizinhas disponiveis -- esta fechado: o bloco chega ao NDS
        // com a evidencia bruta, nao so a conclusao.
        assertTrue(corpoEnviado.has("wifiScan"))
        val wifiScanEnviado = corpoEnviado.getJSONObject("wifiScan")
        assertEquals(6, wifiScanEnviado.getInt("connectedChannel"))
        assertEquals(5, wifiScanEnviado.getInt("neighborCount"))
        assertEquals(5, wifiScanEnviado.getJSONArray("neighbors").length())
        // congestionamento so existe se o motor de canal rodou de verdade sobre as
        // vizinhas -- nao e um numero fixo, mas precisa estar presente.
        assertTrue(wifiScanEnviado.has("channelCongestion"))

        // Contexto do Assist (objective) tambem precisa chegar -- issue-mae secao 18.
        assertEquals("SITES_DEMORAM", corpoEnviado.getJSONObject("context").getString("objective"))
    }

    @Test
    fun `wifi congestionado - DiagnosticReport final explica rede vizinha sem culpar o ISP`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(respostaWifiCongestionado))

        val report = repository().evaluate(inputWifiCongestionado())

        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
        assertEquals(DiagnosticStatus.attention, report.decisao.status)
        assertEquals(58, report.scoreEngineResultado?.score)
        assertEquals("exec-e2e-wifi-congestionado", report.executionId)

        val mensagem = report.decisao.mensagemUsuario
        assertTrue("mensagem deveria citar Wi-Fi/canal: $mensagem", mensagem.contains("Wi-Fi", ignoreCase = true))
        assertFalse(
            "mensagem NAO pode culpar o provedor sem evidencia (issue-mae #1832 secao 21): $mensagem",
            mensagem.contains("provedor", ignoreCase = true) || mensagem.contains("operadora", ignoreCase = true),
        )

        // O card da rede vizinha precisa estar preservado nas evidencias, nao descartado.
        val cardWifi = report.wifiResultados.firstOrNull { it.id == "wifi_channel_congested" }
        assertTrue("card wifi_channel_congested deveria estar em wifiResultados", cardWifi != null)
        assertEquals(DiagnosticStatus.attention, cardWifi?.status)

        assertEquals("trocar_canal_wifi", report.decisao.recomendacaoId)
        assertEquals(listOf("wifi_channel_congested"), report.decisao.sourceFindingIds)
    }

    // -------------------------------------------------------------------
    // Cenário 2 — móvel com sinal fraco (issue-mãe #1832, seção 21).
    // Esperado: explicar o sinal usando RSRP/RSRQ/SINR e tecnologia.
    // -------------------------------------------------------------------

    private fun inputMovelSinalFraco() = DiagnosticInput(
        connectionType = ConnectionType.mobile,
        mobile = MobileDiagnosticInput(
            carrierName = "TIM",
            mobileTechnology = "4G",
            rsrpDbm = -112,
            rsrqDb = -17,
            sinrDb = -3,
            band = "B3",
        ),
        internet = InternetDiagnosticInput(
            downloadMbps = 4.2,
            uploadMbps = 1.1,
            latencyMs = 180.0,
            jitterMs = 45.0,
            perdaPercentual = 3.5,
        ),
        context = DiagnosticContext(objective = "VIDEOS_TRAVAM"),
        executionId = "exec-e2e-movel-fraco",
    )

    /** Corpo de resposta do NDS simulando um veredicto "critico" causado por sinal
     *  móvel fraco — card explica RSRP/RSRQ/SINR, não culpa o app nem inventa causa. */
    private val respostaMovelSinalFraco =
        """
        {
          "recommendation": {
            "id": "aproximar_janela_sinal",
            "type": "diagnostic",
            "title": "Procure um sinal mais forte",
            "description": "O sinal do seu celular está muito fraco para esta conexão.",
            "source_finding_ids": ["mobile_signal_critical"],
            "steps": ["Aproxime-se de uma janela ou área aberta.", "Evite paredes de concreto entre você e a antena."]
          },
          "results": [
            {
              "module": "diagnostics.mobile",
              "module_version": "1.0.0",
              "request_id": "exec-e2e-movel-fraco",
              "warnings": [],
              "missing_inputs": [],
              "result": { "matched_rules": ["mobile_signal_critical"] },
              "cards": [
                {
                  "id": "mobile_signal_critical",
                  "titulo": "Sinal móvel muito fraco",
                  "status": "critical",
                  "mensagemUsuario": "Seu sinal 4G está muito fraco (RSRP -112 dBm, RSRQ -17 dB, SINR -3 dB), o que explica a lentidão e as travadas.",
                  "categoria": "mobile",
                  "podeConcluir": true
                }
              ]
            },
            {
              "module": "scoring",
              "module_version": "1.1.0",
              "request_id": "exec-e2e-movel-fraco",
              "warnings": [],
              "missing_inputs": [],
              "result": { "score": 22, "veredicto": "critico", "tipo_conexao": "MOBILE", "observed_dimensions": 2, "dimensoes": [] }
            },
            {
              "module": "ai",
              "module_version": "1.5.0",
              "request_id": "exec-e2e-movel-fraco",
              "warnings": [],
              "missing_inputs": [],
              "result": {
                "tokens_used": 130,
                "ai_model_used": "claude-haiku",
                "fallback_used": false,
                "explanation_source": "ai",
                "explanation_status": "ok",
                "explanation": {
                  "titulo_amigavel": "Seu sinal de celular está muito fraco onde você está",
                  "resumo_tecnico_traduzido": "RSRP de -112 dBm e SINR negativo (-3 dB) indicam sinal 4G muito fraco e ruidoso nesta localização — isso explica os vídeos travando, não um problema com a operadora em si."
                },
                "source_finding_ids": ["mobile_signal_critical"]
              }
            }
          ],
          "traces": [
            { "module": "mobile", "duration_ms": 3, "status": "ok" },
            { "module": "ai", "duration_ms": 240, "status": "ok", "source": "ai" }
          ]
        }
        """.trimIndent()

    @Test
    fun `movel com sinal fraco - snapshot enviado ao NDS carrega RSRP RSRQ SINR e tecnologia`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(respostaMovelSinalFraco))

        repository().evaluate(inputMovelSinalFraco())

        val corpoEnviado = JSONObject(server.takeRequest().body.readUtf8())

        // Prova que o bloco `mobile` (gap critico da issue-mae, secao 4) chega ao NDS
        // com as evidencias de sinal, nao so a operadora.
        assertTrue(corpoEnviado.has("mobile"))
        val mobileEnviado = corpoEnviado.getJSONObject("mobile")
        assertEquals("TIM", mobileEnviado.getString("operator"))
        assertEquals("4G", mobileEnviado.getString("technology"))
        assertEquals(-112, mobileEnviado.getInt("rsrp_dbm"))
        assertEquals(-17, mobileEnviado.getInt("rsrq_db"))
        assertEquals(-3, mobileEnviado.getInt("sinr_db"))
        assertEquals("B3", mobileEnviado.getString("band"))

        // Nunca deve vazar identificador de celula/torre (issue-mae secao 4, proibicao explicita).
        assertFalse(mobileEnviado.has("cell_id"))
        assertFalse(mobileEnviado.has("tac"))
        assertFalse(mobileEnviado.has("mcc"))
        assertFalse(mobileEnviado.has("mnc"))
    }

    @Test
    fun `movel com sinal fraco - DiagnosticReport final explica RSRP RSRQ SINR sem inventar causa`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(respostaMovelSinalFraco))

        val report = repository().evaluate(inputMovelSinalFraco())

        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
        assertEquals(DiagnosticStatus.critical, report.decisao.status)
        assertEquals(22, report.scoreEngineResultado?.score)
        assertEquals("exec-e2e-movel-fraco", report.executionId)

        val mensagem = report.decisao.mensagemUsuario
        assertTrue("mensagem deveria citar RSRP: $mensagem", mensagem.contains("RSRP", ignoreCase = true))
        assertTrue("mensagem deveria citar SINR: $mensagem", mensagem.contains("SINR", ignoreCase = true))

        // Achado da revisao do PR #1855 (Caio): mobileResultados vinha hardcoded
        // emptyList() no mapper v1, entao o card so aparecia em achadosSecundarios
        // (bucket generico, ignorado por SpeedtestPersistenceCoordinator.extrairProblemasRelatorio
        // e AiModels.findingsRelevantes). Corrigido em NdsDiagnosticsResponseMapper --
        // agora o card de categoria "mobile" chega em mobileResultados de verdade,
        // sem precisar do fallback pro bucket generico.
        val cardMovel = report.mobileResultados.firstOrNull { it.id == "mobile_signal_critical" }
        assertTrue("card mobile_signal_critical deveria estar em mobileResultados", cardMovel != null)
        assertEquals(DiagnosticStatus.critical, cardMovel?.status)

        assertEquals("aproximar_janela_sinal", report.decisao.recomendacaoId)
        assertEquals(listOf("mobile_signal_critical"), report.decisao.sourceFindingIds)
    }
}
