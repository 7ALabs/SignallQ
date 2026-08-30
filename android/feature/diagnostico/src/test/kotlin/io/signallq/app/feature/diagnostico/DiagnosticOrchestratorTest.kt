package io.signallq.app.feature.diagnostico

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.featureflags.FeatureFlagKey
import io.signallq.app.core.featureflags.FeatureFlagProvider
import io.signallq.app.core.featureflags.FeatureFlagRawValue
import io.signallq.app.core.featureflags.FeatureFlagRefreshResult
import io.signallq.app.core.featureflags.FeatureFlagSource
import io.signallq.app.core.featureflags.FeatureFlagValue
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.network.NoOpAnalyticsHelper
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.feature.diagnostico.nds.NdsDiagnosticRepository
import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
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

/** Fake mínimo para os testes de NDS-02k — sempre devolve [enabled], nunca toca rede. */
private class FakeFeatureFlagProvider(private val enabled: Boolean) : FeatureFlagProvider {
    override fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue> =
        flowOf(FeatureFlagValue(key = key, raw = FeatureFlagRawValue.BooleanValue(enabled), source = FeatureFlagSource.DEFAULT))

    override fun isEnabled(key: FeatureFlagKey): Boolean = enabled

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
        OkHttpClient.Builder()
            .connectTimeout(300, TimeUnit.MILLISECONDS)
            .readTimeout(300, TimeUnit.MILLISECONDS)
            .writeTimeout(300, TimeUnit.MILLISECONDS)
            .build()

    private fun snapshotSaudavelInput() = DiagnosticInput(
        connectionType = ConnectionType.wifi,
        wifi = WifiDiagnosticInput(rssiDbm = -55, linkSpeedMbps = 400, frequenciaMhz = 5180),
        internet = InternetDiagnosticInput(
            downloadMbps = 200.0, uploadMbps = 50.0, latencyMs = 12.0, jitterMs = 2.0, perdaPercentual = 0.0,
        ),
    )

    private fun remoteReportJson(): String = """
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
    fun `worker remoto saudavel respondendo - orquestrador ainda assim usa decisao do motor LOCAL`() = runTest {
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
    fun `worker indisponivel (timeout) - orquestrador cai pro motor local sem travar`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), client = quickTimeoutClient())
        val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

        orchestrator.executar(snapshotSaudavelInput())

        val snapshot = orchestrator.snapshotFlow.value
        assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
        assertNotNull(snapshot.relatorio)
        // Prova que caiu pro motor LOCAL de verdade, nunca o id fabricado do teste remoto.
        assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
        assertTrue(snapshot.relatorio!!.decisao.id.isNotBlank())
    }

    @Test
    fun `worker respondendo 500 - orquestrador cai pro motor local sem excecao`() = runTest {
        server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
        val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
        val orchestrator = DiagnosticOrchestrator(remoteDiagnosticRepository = repo)

        orchestrator.executar(snapshotSaudavelInput())

        val snapshot = orchestrator.snapshotFlow.value
        assertEquals(EstadoDiagnostico.concluido, snapshot.estado)
        assertNotEquals("DECISAO-REMOTA-TESTE", snapshot.relatorio?.decisao?.id)
    }

    @Test
    fun `geracao avanca mesmo quando o veredito final se repete`() = runTest {
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
    fun `falha precoce e cancelamento finalizam a geracao canonica`() = runTest {
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
    fun `solicitacao concorrente e rejeitada sem nova geracao`() = runTest {
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
    fun `terminal de sucesso so e publicado depois de analytics e com gate liberado`() = runTest {
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
    fun `overloads legados preservam retorno Unit`() = runTest {
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

    private fun ndsSuccessBody(): String = """
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
    fun `flag nds_live desligada (default) - NdsDiagnosticRepository nunca recebe trafego, comportamento identico ao atual`() = runTest {
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
    fun `Assist usa NDS remoto mesmo com flag global desligada`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ndsSuccessBody()))
        val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
        val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
        val orchestrator = DiagnosticOrchestrator(
            ndsDiagnosticRepository = ndsRepo,
            featureFlagProvider = FakeFeatureFlagProvider(enabled = false),
        )

        val report = orchestrator.avaliarAssist(snapshotSaudavelInput())

        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
        assertEquals("nds:excelente", report.decisao.id)
        assertEquals(1, server.requestCount)
    }

    @Test
    fun `flag nds_live ligada - usa NdsDiagnosticRepository em vez do shadow mode do worker antigo`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ndsSuccessBody()))
        val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
        val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
        val orchestrator = DiagnosticOrchestrator(
            ndsDiagnosticRepository = ndsRepo,
            featureFlagProvider = FakeFeatureFlagProvider(enabled = true),
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
    fun `flag nds_live ligada e NDS fora do ar - cai pro motor local, sem excecao, mesmo com a flag ligada`() = runTest {
        server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
        val quickClient = quickTimeoutClient()
        val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = quickClient)
        val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
        val orchestrator = DiagnosticOrchestrator(
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
    // NdsDiagnosticRepository. Sem subcategory disponivel em DiagnosticContext
    // ainda, mesmo com a flag ligada o request cai em v1 -- gap documentado em
    // NdsDiagnosticsRequestMapper.
    // -------------------------------------------------------------------

    @Test
    fun `avaliarAssist com USAR_NDS_V2_NO_ASSIST desligada (default) - chama v1`() = runTest {
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
    fun `avaliarAssist com USAR_NDS_V2_NO_ASSIST ligada mas sem subcategory - ainda chama v1 (gap documentado)`() = runTest {
        server.enqueue(MockResponse().setResponseCode(200).setBody(ndsSuccessBody()))
        val ndsClient = NdsClient(baseUrl = server.url("/").toString(), apiToken = "t", client = OkHttpClient())
        val ndsRepo = NdsDiagnosticRepository(ndsClient = ndsClient)
        val orchestrator = DiagnosticOrchestrator(
            ndsDiagnosticRepository = ndsRepo,
            featureFlagProvider = FakeFeatureFlagProvider(enabled = true),
        )

        val report = orchestrator.avaliarAssist(snapshotSaudavelInput())

        val recorded = server.takeRequest()
        assertEquals("/v1/diagnostics/evaluate", recorded.path)
        assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
    }
}
