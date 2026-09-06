package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.featureflags.FeatureFlagKey
import io.signallq.app.core.featureflags.FeatureFlagKeys
import io.signallq.app.core.featureflags.FeatureFlagProvider
import io.signallq.app.core.featureflags.FeatureFlagRawValue
import io.signallq.app.core.featureflags.FeatureFlagRefreshResult
import io.signallq.app.core.featureflags.FeatureFlagSource
import io.signallq.app.core.featureflags.FeatureFlagValue
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.feature.diagnostico.nds.NdsDiagnosticRepository
import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.util.concurrent.TimeUnit

/**
 * Fake mínimo para os testes de NDS-02k — devolve [enabled] pra qualquer chave por default,
 * nunca toca rede. [overrides] (feat/nds-v2-fluxo-principal) permite testes que precisam de
 * decisões independentes por flag (ex.: `nds_live` ligada mas `nds_v2` desligada) sem precisar
 * de um fake novo por combinação.
 */
private class FakeFeatureFlagProvider(
    private val enabled: Boolean,
    private val overrides: Map<FeatureFlagKey, Boolean> = emptyMap(),
) : FeatureFlagProvider {
    override fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue> {
        val valor = overrides[key] ?: enabled
        return flowOf(FeatureFlagValue(key = key, raw = FeatureFlagRawValue.BooleanValue(valor), source = FeatureFlagSource.DEFAULT))
    }

    override fun isEnabled(key: FeatureFlagKey): Boolean = overrides[key] ?: enabled

    override suspend fun refresh(force: Boolean): FeatureFlagRefreshResult =
        FeatureFlagRefreshResult.Success(activated = false, fetchTimeMillis = null)
}

/**
 * Cobre a ligacao do GH#1444 (shadow mode, parte de #952): desde essa issue,
 * [DiagnosticOrchestrator.executar] delega pro
 * [RemoteDiagnosticRepository.evaluateShadow], NAO mais [RemoteDiagnosticRepository.evaluate]
 * (GH#969, remoto-primeiro) — motor LOCAL sempre autoritativo, worker remoto
 * saudavel ou fora do ar NUNCA muda o que a UI mostra. Ver kdoc de
 * [RemoteDiagnosticRepository] para a distincao completa entre os dois metodos.
 */
class DiagnosticOrchestratorTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    private fun quickTimeoutClient(): OkHttpClient =
        OkHttpClient
            .Builder()
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .writeTimeout(300, TimeUnit.MILLISECONDS)
            .build()

    private fun snapshotSaudavelInput() =
        DiagnosticInput(
            connectionType = ConnectionType.wifi,
            wifi = WifiDiagnosticInput(rssiDbm = -55, linkSpeedMbps = 400, frequenciaMhz = 5180),
            internet =
                InternetDiagnosticInput(
                    downloadMbps = 200.0,
                    uploadMbps = 50.0,
                    latencyMs = 12.0,
                    jitterMs = 2.0,
                    perdaPercentual = 0.0,
                ),
        )

    private fun remoteReportJson(): String =
        """
        {
          "evaluationSource": "REMOTE",
          "wifiResultados": [],
          "internetResultados": [],
          "mobileResultados": [],
          "fibraResultados": [],
          "dnsResultados": [],
          "historicoResultados": [],
          "wifiCanalResultados": [],
          "redeResultados": [],
          "decisao": {
            "id": "DECISAO-REMOTA-TESTE",
            "titulo": "Conexao saudavel no momento",
            "status": "ok",
            "evidencia": null,
            "mensagemUsuario": "Tudo certo por aqui.",
            "recomendacao": null,
            "categoria": "decisao",
            "podeConcluir": true,
            "categoriaOrigem": null
          },
          "achadosSecundarios": [],
          "hipotesesDescartadas": [],
          "dadosAusentes": [],
          "limitacoesEquipamentoLocal": [],
          "recomendacoes": [],
          "scoreEngineResultado": { "score": 95, "veredictoHumano": "excelente", "dimensoes": [{"id":"wifi","score":95}] },
          "perfisUso": [],
          "gameReadiness": [],
          "geradoEmMs": 1700000000000
        }
        """.trimIndent()

    @Test
    fun `worker remoto saudavel respondendo - orquestrador ainda assim usa decisao do motor LOCAL`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
            val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

            orchestrator.executar(snapshotSaudavelInput())

            val snapshot = orchestrator.snapshotFlow.value
            assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
            assertNotNull(snapshot.relatorio)
            // GH#1444: shadow mode -- mesmo com o worker saudavel e respondendo rapido,
            // o resultado exibido e sempre o do motor local, nunca o id fabricado do
            // fixture remoto usado neste teste.
            assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, snapshot.relatorio?.evaluationSource)
        }

    @Test
    fun `worker indisponivel (timeout) - orquestrador cai pro motor local sem travar`() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), client = quickTimeoutClient())
            val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

            orchestrator.executar(snapshotSaudavelInput())

            val snapshot = orchestrator.snapshotFlow.value
            assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
            assertNotNull(snapshot.relatorio)
            // Prova que caiu pro motor LOCAL de verdade, nunca o id fabricado do teste remoto.
            assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
            assertTrue(
                snapshot.relatorio!!
                    .decisao.id
                    .isNotBlank(),
            )
        }

    @Test
    fun `worker respondendo 500 - orquestrador cai pro motor local sem excecao`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
            val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

            orchestrator.executar(snapshotSaudavelInput())

            val snapshot = orchestrator.snapshotFlow.value
            assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
            assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
        }

    @Test
    fun `geracao avanca mesmo quando o veredito final se repete`() =
        runTest {
            val orchestrator = DiagnosticOrchestrator()

            orchestrator.executar(snapshotSaudavelInput())
            val primeiro = orchestrator.snapshotFlow.value
            orchestrator.executar(snapshotSaudavelInput())
            val segundo = orchestrator.snapshotFlow.value

            assertEquals(primeiro.relatorio?.veredito, segundo.relatorio?.veredito)
            assertEquals(EstadoDiagnostico.concluido, segundo.estado)
            assertEquals(primeiro.geracao + 1L, segundo.geracao)
        }

    @Test
    fun `falha precoce e cancelamento finalizam a geracao canonica`() =
        runTest {
            val orchestrator = DiagnosticOrchestrator()

            orchestrator.executarSolicitacao { error("falha antes do input") }
            assertEquals(EstadoDiagnostico.erro, orchestrator.snapshotFlow.value.estado)
            assertEquals(1L, orchestrator.snapshotFlow.value.geracao)

            var cancelamentoPropagado = false
            try {
                orchestrator.executarSolicitacao { throw CancellationException("cancelado") }
            } catch (_: CancellationException) {
                cancelamentoPropagado = true
            }
            assertTrue(cancelamentoPropagado)
            assertEquals(EstadoDiagnostico.cancelado, orchestrator.snapshotFlow.value.estado)
            assertEquals(2L, orchestrator.snapshotFlow.value.geracao)
            assertNotNull(orchestrator.tentarReservar())
        }

    @Test
    fun `solicitacao concorrente e rejeitada sem nova geracao`() =
        runTest {
            val liberar = CompletableDeferred<Unit>()
            var diagnosticosIniciados = 0
            val analytics =
                object : AnalyticsHelper by NoOpAnalyticsHelper {
                    override fun registrarDiagIniciado(
                        tipoConexao: String,
                        areasHabilitadas: String?,
                        temSpeedtest: Boolean,
                    ) {
                        diagnosticosIniciados++
                    }
                }
            val orchestrator = DiagnosticOrchestrator(analyticsHelper = analytics)
            val primeira =
                async {
                    orchestrator.executarSolicitacao {
                        liberar.await()
                        snapshotSaudavelInput()
                    }
                }

            yield()
            assertEquals(EstadoDiagnostico.executando, orchestrator.snapshotFlow.value.estado)
            assertTrue(
                orchestrator.executarSolicitacao { snapshotSaudavelInput() } is
                    DiagnosticOrchestrator.ResultadoSolicitacao.Rejeitada,
            )
            assertEquals(1L, orchestrator.snapshotFlow.value.geracao)
            liberar.complete(Unit)
            primeira.await()
            assertEquals(1, diagnosticosIniciados)
        }

    @Test
    fun `terminal de sucesso so e publicado depois de analytics e com gate liberado`() =
        runTest {
            var analyticsFechado = false
            val analytics =
                object : AnalyticsHelper by NoOpAnalyticsHelper {
                    override fun registrarDiagConcluido(
                        tipoConexao: String,
                        statusGeral: String,
                        decisaoId: String,
                        scoreConexao: Long,
                        confianca: Double,
                        nResultadosCriticos: Long?,
                        nResultadosAttention: Long?,
                    ) {
                        analyticsFechado = true
                    }
                }
            val orchestrator = DiagnosticOrchestrator(analyticsHelper = analytics)

            orchestrator.executar(snapshotSaudavelInput())

            assertTrue(analyticsFechado)
            assertEquals(EstadoDiagnostico.concluido, orchestrator.snapshotFlow.value.estado)
            assertNotNull(orchestrator.tentarReservar())
        }

    @Test
    fun `overloads legados preservam retorno Unit`() =
        runTest {
            val orchestrator = DiagnosticOrchestrator()

            val retornoInput: Unit = orchestrator.executar(snapshotSaudavelInput())
            val retornoLegado: Unit =
                orchestrator.executar(
                    snapshotSaudavelInput().internet,
                    snapshotSaudavelInput().wifi,
                )

            assertEquals(Unit, retornoInput)
            assertEquals(Unit, retornoLegado)
        }

    // -------------------------------------------------------------------
    // NDS-02k (issue #1759) — flag `consumer_diagnostico_nds_live_enabled`.
    // -------------------------------------------------------------------

    private fun ndsSuccessBody(): String =
        """
        {
          "recommendation": null,
          "results": [
            { "module": "scoring", "module_version": "1.1.0", "request_id": "req-1", "warnings": [], "missing_inputs": [], "result": { "score": 95, "veredicto": "excelente", "tipo_conexao": "WIFI", "observed_dimensions": 1, "dimensoes": [] } },
            { "module": "ai", "module_version": "1.5.0", "request_id": "req-1", "warnings": [], "missing_inputs": [], "result": { "tokens_used": 0, "ai_model_used": "copy-catalog", "fallback_used": false, "explanation_source": "copy_catalog", "explanation_status": "catalog_hit", "explanation": { "titulo_amigavel": "Tudo certo", "resumo_tecnico_traduzido": "Conexao excelente." }, "source_finding_ids": [] } }
          ],
          "traces": []
        }
        """.trimIndent()

    @Test
    fun `flag nds_live desligada (default) - NdsDiagnosticRepository nunca recebe trafego, comportamento identico ao atual`() =
        runTest {
            // Nenhuma resposta enfileirada de propósito: se o orquestrador chamasse o NDS
            // com a flag desligada, este teste travaria/falharia. server.requestCount
            // prova que nenhuma requisição saiu.
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator = DiagnosticOrchestrator(ndsDiagnosticRepository = ndsRepo)

            orchestrator.executar(snapshotSaudavelInput())

            assertEquals(0, server.requestCount)
            val snapshot = orchestrator.snapshotFlow.value
            assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, snapshot.relatorio?.evaluationSource)
        }

    @Test
    fun `Assist usa NDS remoto mesmo com flag global desligada`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(ndsSuccessBody()))
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator =
                DiagnosticOrchestrator(
                    ndsDiagnosticRepository = ndsRepo,
                    featureFlagProvider = FakeFeatureFlagProvider(enabled = false),
                )

            val report = orchestrator.avaliarAssist(snapshotSaudavelInput())

            assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
            assertEquals("nds:excelente", report.decisao.id)
            assertEquals(1, server.requestCount)
        }

    @Test
    fun `flag nds_live ligada - usa NdsDiagnosticRepository em vez do shadow mode do worker antigo`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(ndsSuccessBody()))
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator =
                DiagnosticOrchestrator(
                    ndsDiagnosticRepository = ndsRepo,
                    // USAR_NDS_V2_NO_FLUXO_PRINCIPAL desligada explicitamente: este teste cobre
                    // o v1 (comportamento default), o v2 do fluxo principal tem cobertura
                    // dedicada na secao feat/nds-v2-fluxo-principal abaixo.
                    featureFlagProvider =
                        FakeFeatureFlagProvider(
                            enabled = true,
                            overrides = mapOf(FeatureFlagKeys.USAR_NDS_V2_NO_FLUXO_PRINCIPAL to false),
                        ),
                )

            orchestrator.executar(snapshotSaudavelInput())

            val snapshot = orchestrator.snapshotFlow.value
            assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
            assertEquals(DiagnosticEvaluationSource.REMOTE, snapshot.relatorio?.evaluationSource)
            assertEquals("nds:excelente", snapshot.relatorio?.decisao?.id)
            // Prova que o path do worker antigo (signallq-diagnostic, shadow mode) nao
            // rodou: unica requisicao recebida pelo server mock foi a do NDS.
            assertEquals(1, server.requestCount)
            assertEquals("/v1/diagnostics/evaluate", server.takeRequest().path)
        }

    @Test
    fun `flag nds_live ligada e NDS fora do ar - cai pro motor local, sem excecao, mesmo com a flag ligada`() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            val quickClient = quickTimeoutClient()
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = quickClient)
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator =
                DiagnosticOrchestrator(
                    ndsDiagnosticRepository = ndsRepo,
                    featureFlagProvider = FakeFeatureFlagProvider(enabled = true),
                )

            orchestrator.executar(snapshotSaudavelInput())

            val snapshot = orchestrator.snapshotFlow.value
            assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, snapshot.relatorio?.evaluationSource)
        }

    @Test
    fun `cancel before start libera somente a reserva proprietaria e cleanup e idempotente`() {
        val orchestrator = DiagnosticOrchestrator()
        val primeira = requireNotNull(orchestrator.tentarReservar())

        assertTrue(orchestrator.cancelarReserva(primeira))
        assertEquals(EstadoDiagnostico.cancelado, orchestrator.snapshotFlow.value.estado)
        assertEquals(primeira.geracao, orchestrator.snapshotFlow.value.geracao)

        val segunda = requireNotNull(orchestrator.tentarReservar())
        assertTrue(segunda.geracao > primeira.geracao)
        assertFalse(orchestrator.cancelarReserva(primeira))
        assertFalse(orchestrator.cancelarReserva(primeira))
        assertEquals(EstadoDiagnostico.executando, orchestrator.snapshotFlow.value.estado)
        assertEquals(segunda.geracao, orchestrator.snapshotFlow.value.geracao)
        assertTrue(orchestrator.cancelarReserva(segunda))
    }

    // -------------------------------------------------------------------
    // feat/nds-client-v2 — avaliarAssist le USAR_NDS_V2_NO_ASSIST e repassa pro
    // NdsDiagnosticRepository. O contrato v2 aceita contexto parcial, então a flag
    // ligada seleciona v2 mesmo enquanto a tela não tem subcategoria canônica.
    // -------------------------------------------------------------------

    @Test
    fun `avaliarAssist com USAR_NDS_V2_NO_ASSIST desligada (default) - chama v1`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(ndsSuccessBody()))
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator = DiagnosticOrchestrator(ndsDiagnosticRepository = ndsRepo)

            val report = orchestrator.avaliarAssist(snapshotSaudavelInput())

            val recorded = server.takeRequest()
            assertEquals("/v1/diagnostics/evaluate", recorded.path)
            assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
        }

    @Test
    fun `avaliarAssist com USAR_NDS_V2_NO_ASSIST ligada sem subcategory chama v2`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"raw":{},"explanation":{"titulo":"t","descricao":"d","dados":[]}}"""))
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator =
                DiagnosticOrchestrator(
                    ndsDiagnosticRepository = ndsRepo,
                    featureFlagProvider = FakeFeatureFlagProvider(enabled = true),
                )

            val report = orchestrator.avaliarAssist(snapshotSaudavelInput())

            val recorded = server.takeRequest()
            assertEquals("/v2/diagnostics/evaluate", recorded.path)
            assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
        }

    // -------------------------------------------------------------------
    // feat/nds-v2-fluxo-principal — executarProtegido le
    // USAR_NDS_V2_NO_FLUXO_PRINCIPAL e repassa pro NdsDiagnosticRepository, mesmo padrao
    // ja coberto acima pra USAR_NDS_V2_NO_ASSIST. So importa quando
    // CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED tambem esta ligada (senao o fluxo nem chama
    // o NDS).
    // -------------------------------------------------------------------

    @Test
    fun `fluxo principal com nds_live ligada e USAR_NDS_V2_NO_FLUXO_PRINCIPAL desligada (default) - chama v1`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(ndsSuccessBody()))
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator =
                DiagnosticOrchestrator(
                    ndsDiagnosticRepository = ndsRepo,
                    featureFlagProvider =
                        FakeFeatureFlagProvider(
                            enabled = true,
                            overrides = mapOf(FeatureFlagKeys.USAR_NDS_V2_NO_FLUXO_PRINCIPAL to false),
                        ),
                )

            orchestrator.executar(snapshotSaudavelInput())

            val snapshot = orchestrator.snapshotFlow.value
            assertEquals("/v1/diagnostics/evaluate", server.takeRequest().path)
            assertEquals(DiagnosticEvaluationSource.REMOTE, snapshot.relatorio?.evaluationSource)
        }

    @Test
    fun `fluxo principal com nds_live ligada e USAR_NDS_V2_NO_FLUXO_PRINCIPAL ligada - chama v2`() =
        runTest {
            server.enqueue(
                MockResponse().setResponseCode(200).setBody(
                    """{"raw":{},"explanation":{"titulo":"t","descricao":"d","dados":[]}}""",
                ),
            )
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator =
                DiagnosticOrchestrator(
                    ndsDiagnosticRepository = ndsRepo,
                    featureFlagProvider =
                        FakeFeatureFlagProvider(
                            enabled = true,
                            overrides = mapOf(FeatureFlagKeys.USAR_NDS_V2_NO_FLUXO_PRINCIPAL to true),
                        ),
                )

            orchestrator.executar(snapshotSaudavelInput())

            val snapshot = orchestrator.snapshotFlow.value
            assertEquals("/v2/diagnostics/evaluate", server.takeRequest().path)
            assertEquals(DiagnosticEvaluationSource.REMOTE, snapshot.relatorio?.evaluationSource)
        }

    @Test
    fun `fluxo principal com nds_live desligada - USAR_NDS_V2_NO_FLUXO_PRINCIPAL ligada nao tem efeito`() =
        runTest {
            // nds_live desligada: o orquestrador nem chama o NdsDiagnosticRepository, entao a
            // flag v2 (mesmo ligada) nao pode gerar trafego. server.requestCount prova isso.
            val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
            val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
            val orchestrator =
                DiagnosticOrchestrator(
                    ndsDiagnosticRepository = ndsRepo,
                    featureFlagProvider =
                        FakeFeatureFlagProvider(
                            enabled = false,
                            overrides = mapOf(FeatureFlagKeys.USAR_NDS_V2_NO_FLUXO_PRINCIPAL to true),
                        ),
                )

            orchestrator.executar(snapshotSaudavelInput())

            assertEquals(0, server.requestCount)
            val snapshot = orchestrator.snapshotFlow.value
            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, snapshot.relatorio?.evaluationSource)
        }
}
