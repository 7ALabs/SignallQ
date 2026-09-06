package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.diagnostico.ConnectionType
import io.signallq.app.core.diagnostico.DiagnosticEvaluationSource
import io.signallq.app.core.diagnostico.DiagnosticInput
import io.signallq.app.core.diagnostico.DiagnosticRolloutStatus
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.InternetDiagnosticInput
import io.signallq.app.core.diagnostico.WifiDiagnosticInput
import io.signallq.app.core.featureflags.FeatureFlagKey
import io.signallq.app.core.featureflags.FeatureFlagProvider
import io.signallq.app.core.featureflags.FeatureFlagRawValue
import io.signallq.app.core.featureflags.FeatureFlagRefreshResult
import io.signallq.app.core.featureflags.FeatureFlagSource
import io.signallq.app.core.featureflags.FeatureFlagValue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.nio.file.Files
import java.util.concurrent.TimeUnit

/**
 * Cobre a estrategia local vs. remoto do GH#962: resposta normal (remoto vence,
 * mapeado corretamente) e as 2 formas de "worker indisponivel" (erro HTTP e
 * timeout) caindo pro motor local sem travar.
 */
class RemoteDiagnosticRepositoryTest {
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
            "id": "DECISAO-SAUDAVEL_MONITORAR",
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
    fun `worker responde 200 com JSON valido - evaluate usa o relatorio remoto`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

            val report = repo.evaluate(snapshotSaudavelInput())

            assertEquals("DECISAO-SAUDAVEL_MONITORAR", report.decisao.id)
            assertEquals(DiagnosticStatus.ok, report.decisao.status)
            assertEquals(95, report.scoreEngineResultado?.score)
            // perfisUso/gameReadiness sempre calculados localmente, mesmo com fonte remota.
            assertTrue(report.perfisUso.isNotEmpty())
            assertTrue(report.gameReadiness.isNotEmpty())

            val recorded = server.takeRequest()
            assertEquals("/api/diagnostic/evaluate", recorded.path)
            assertEquals("POST", recorded.method)
        }

    @Test
    fun `worker responde 500 - cai pro motor local sem travar`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500).setBody("""{"error":"boom"}"""))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

            val report = repo.evaluate(snapshotSaudavelInput())

            // Prova que caiu pro motor LOCAL de verdade (nao um relatorio remoto/vazio
            // fabricado): decisao valida e com id gerado pelo FindingEngine local,
            // nunca o placeholder "DECISAO-INCONCLUSIVO" de fallback de erro do worker.
            assertNotNull(report.decisao)
            assertTrue(report.decisao.id.isNotBlank())
            assertTrue(report.decisao.id != "DECISAO-INCONCLUSIVO")
            assertNotNull(report.scoreEngineResultado)
        }

    @Test
    fun `worker retorna corpo vazio - cai pro motor local`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(""))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

            val report = repo.evaluate(snapshotSaudavelInput())
            assertNotNull(report.decisao)
        }

    @Test
    fun `worker retorna JSON invalido - cai pro motor local`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody("{ nao é json valido"))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

            val report = repo.evaluate(snapshotSaudavelInput())
            assertNotNull(report.decisao)
        }

    @Test
    fun `worker nao responde (conexao cai) - nunca trava, cai pro motor local`() =
        runTest {
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.DISCONNECT_AT_START))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), client = quickTimeoutClient())

            val report = repo.evaluate(snapshotSaudavelInput())
            assertNotNull(report.decisao)
        }

    @Test
    fun `evaluateRemote isolado retorna null em qualquer falha`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(404))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())
            assertNull(repo.evaluateRemote(snapshotSaudavelInput()))
        }

    // --- Fallback local de 3 niveis (GH#1450, secao "Fallback local" de #952) ---

    private fun tempCacheDir() = Files.createTempDirectory("ruleset-cache-repo-test").toFile()

    private fun cacheStore(dir: java.io.File = tempCacheDir()): FileRulesetCacheStore = FileRulesetCacheStore(dir)

    @Test
    fun `worker responde 200 - evaluationSource fica REMOTE e persiste cache`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
            val cache = cacheStore()
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), cacheStore = cache)

            val report = repo.evaluate(snapshotSaudavelInput())

            assertEquals(DiagnosticEvaluationSource.REMOTE, report.evaluationSource)
            assertNotNull(cache.load())
        }

    @Test
    fun `remoto falha sem cache previamente persistido - cai direto pro BUNDLED_LOCAL`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(500))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), cacheStore = cacheStore())

            val report = repo.evaluate(snapshotSaudavelInput())

            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
        }

    @Test
    fun `remoto falha com cache valido - usa CACHED_LOCAL em vez do motor embarcado`() =
        runTest {
            val cache = cacheStore()
            // Primeira chamada: remoto ok, popula o cache.
            server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), cacheStore = cache)
            repo.evaluate(snapshotSaudavelInput())

            // Segunda chamada: remoto falha -> deve usar o cache da primeira.
            server.enqueue(MockResponse().setResponseCode(500))
            val report = repo.evaluate(snapshotSaudavelInput())

            assertEquals(DiagnosticEvaluationSource.CACHED_LOCAL, report.evaluationSource)
            assertEquals("DECISAO-SAUDAVEL_MONITORAR", report.decisao.id)
        }

    @Test
    fun `cache corrompido - ignora e cai pro BUNDLED_LOCAL sem travar`() =
        runTest {
            val dir = tempCacheDir()
            // Arquivo de cache corrompido diretamente em disco (sem passar por save(),
            // que ja validaria e rejeitaria) -- simula corrupcao real do arquivo persistido.
            java.io
                .File(dir, "diagnostic_ruleset_cache.json")
                .writeText("{ corrompido de verdade", Charsets.UTF_8)

            server.enqueue(MockResponse().setResponseCode(500))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), cacheStore = cacheStore(dir))

            val report = repo.evaluate(snapshotSaudavelInput())

            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
            assertNotNull(report.decisao)
        }

    @Test
    fun `download invalido apos cache valido - nao sobrescreve cache, proxima falha ainda usa CACHED_LOCAL`() =
        runTest {
            val cache = cacheStore()
            server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString(), cacheStore = cache)
            repo.evaluate(snapshotSaudavelInput())
            val cachedAposSucesso = cache.load()

            // "Download" seguinte volta corpo vazio (falha de rede/parsing) -- evaluateRemote
            // ja retorna null nesse caso, entao evaluate() nem tenta persistir nada; o cache
            // valido anterior deve permanecer intacto.
            server.enqueue(MockResponse().setResponseCode(200).setBody(""))
            repo.evaluate(snapshotSaudavelInput())

            assertEquals(cachedAposSucesso, cache.load())

            // E a proxima falha real ainda consegue usar esse mesmo cache.
            server.enqueue(MockResponse().setResponseCode(500))
            val report = repo.evaluate(snapshotSaudavelInput())
            assertEquals(DiagnosticEvaluationSource.CACHED_LOCAL, report.evaluationSource)
        }

    // --- Shadow mode (GH#1444, parte de #952): evaluateShadow ---

    private fun sempreLigadaFlagProvider(): FeatureFlagProvider =
        object : FeatureFlagProvider {
            override fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue> =
                flowOf(FeatureFlagValue(key = key, raw = FeatureFlagRawValue.BooleanValue(true), source = FeatureFlagSource.STATIC))

            override fun isEnabled(key: FeatureFlagKey): Boolean = true

            override suspend fun refresh(force: Boolean): FeatureFlagRefreshResult =
                FeatureFlagRefreshResult.Success(activated = false, fetchTimeMillis = null)
        }

    /** GH#1445 — status de rollout a 100%, sem segmentacao, pra manter os testes
     *  de shadow mode pre-existentes (comportamento de rede) participando. */
    private fun rollout100PorCento(): DiagnosticRolloutStatus =
        DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 100)

    @Test
    fun `evaluateShadow retorna sempre o relatorio LOCAL, mesmo com worker remoto saudavel`() =
        runTest {
            server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

            val report = repo.evaluateShadow(snapshotSaudavelInput())

            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
        }

    @Test
    fun `evaluateShadow sem divergenceReporter configurado nao faz nenhuma chamada de rede`() =
        runTest {
            val repo = RemoteDiagnosticRepository(baseUrl = server.url("/").toString())

            repo.evaluateShadow(snapshotSaudavelInput())

            assertEquals(0, server.requestCount)
        }

    @Test
    fun `evaluateShadow com divergenceReporter dispara avaliacao remota em paralelo e envia comparacao classificada`() =
        runTest {
            // 1a requisicao: avaliacao remota interna (evaluateRemote, chamada pelo shadow comparison).
            server.enqueue(MockResponse().setResponseCode(200).setBody(remoteReportJson()))
            // 2a requisicao: ingest da divergencia ja classificada.
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{"ok":true}"""))

            val reporter =
                DiagnosticDivergenceReporter(
                    baseUrl = server.url("/").toString(),
                    client = OkHttpClient(),
                    featureFlagProvider = sempreLigadaFlagProvider(),
                    appVersion = "0.31.0",
                    versionCode = 312,
                    consentimentoProvider = { true },
                    installationIdProvider = { "installation-de-teste" },
                    rolloutStatusProvider = { rollout100PorCento() },
                )
            // shadowScope default (Dispatchers.IO real) — evita as sutilezas de
            // StandardTestDispatcher com trabalho de rede real acontecendo em
            // background; `server.takeRequest(timeout)` sincroniza pela chegada
            // real da requisicao, nao pelo agendamento do coroutine test scheduler.
            val repo =
                RemoteDiagnosticRepository(
                    baseUrl = server.url("/").toString(),
                    divergenceReporter = reporter,
                )

            val report = repo.evaluateShadow(snapshotSaudavelInput())
            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)

            val evaluateReq = server.takeRequest(5, TimeUnit.SECONDS)
            assertNotNull(evaluateReq)
            assertEquals("/api/diagnostic/evaluate", evaluateReq!!.path)

            val ingestReq = server.takeRequest(5, TimeUnit.SECONDS)
            assertNotNull(ingestReq)
            assertEquals("/ingest/diagnostic-divergence", ingestReq!!.path)

            val body = JSONObject(ingestReq.body.readUtf8())
            assertTrue(body.has("classification"))
            assertTrue(body.has("snapshotHash"))
            assertTrue(body.has("executionId"))
            assertEquals("BUNDLED_LOCAL", body.getString("localSource"))
            assertEquals("REMOTE", body.getString("remoteSource"))
        }

    @Test
    fun `evaluateShadow com rollout em 0 por cento nao dispara avaliacao remota nem ingest (GH#1445)`() =
        runTest {
            val reporter =
                DiagnosticDivergenceReporter(
                    baseUrl = server.url("/").toString(),
                    client = OkHttpClient(),
                    featureFlagProvider = sempreLigadaFlagProvider(),
                    appVersion = "0.31.0",
                    versionCode = 312,
                    consentimentoProvider = { true },
                    installationIdProvider = { "installation-de-teste" },
                    rolloutStatusProvider = { DiagnosticRolloutStatus(rulesetVersion = 1, rolloutPercent = 0) },
                )
            val repo =
                RemoteDiagnosticRepository(
                    baseUrl = server.url("/").toString(),
                    divergenceReporter = reporter,
                )

            val report = repo.evaluateShadow(snapshotSaudavelInput())

            assertEquals(DiagnosticEvaluationSource.BUNDLED_LOCAL, report.evaluationSource)
            // shadowScope roda em Dispatchers.IO real (fire-and-forget) -- da um
            // tempo curto pra garantir que, se fosse disparar algo, ja teria chegado.
            assertNull(server.takeRequest(500, TimeUnit.MILLISECONDS))
        }

    @Test
    fun `evaluateShadow sem status de rollout disponivel (worker fora do ar) nao participa (fail closed)`() =
        runTest {
            val reporter =
                DiagnosticDivergenceReporter(
                    baseUrl = server.url("/").toString(),
                    client = OkHttpClient(),
                    featureFlagProvider = sempreLigadaFlagProvider(),
                    appVersion = "0.31.0",
                    versionCode = 312,
                    consentimentoProvider = { true },
                    installationIdProvider = { "installation-de-teste" },
                    rolloutStatusProvider = { null },
                )
            val repo =
                RemoteDiagnosticRepository(
                    baseUrl = server.url("/").toString(),
                    divergenceReporter = reporter,
                )

            repo.evaluateShadow(snapshotSaudavelInput())

            assertNull(server.takeRequest(500, TimeUnit.MILLISECONDS))
        }

    @Test
    fun `evaluateShadow com falha remota classifica REMOTE_ERROR e ainda assim reporta`() =
        runTest {
            // Avaliacao remota falha (500); o shadow comparison classifica REMOTE_ERROR.
            server.enqueue(MockResponse().setResponseCode(500))
            server.enqueue(MockResponse().setResponseCode(202).setBody("""{"ok":true}"""))

            val reporter =
                DiagnosticDivergenceReporter(
                    baseUrl = server.url("/").toString(),
                    client = OkHttpClient(),
                    featureFlagProvider = sempreLigadaFlagProvider(),
                    appVersion = "0.31.0",
                    versionCode = 312,
                    consentimentoProvider = { true },
                    installationIdProvider = { "installation-de-teste" },
                    rolloutStatusProvider = { rollout100PorCento() },
                )
            val repo =
                RemoteDiagnosticRepository(
                    baseUrl = server.url("/").toString(),
                    divergenceReporter = reporter,
                )

            repo.evaluateShadow(snapshotSaudavelInput())

            server.takeRequest(5, TimeUnit.SECONDS) // avaliacao remota (falhou)
            val ingestReq = server.takeRequest(5, TimeUnit.SECONDS)
            assertNotNull(ingestReq)
            val body = JSONObject(ingestReq!!.body.readUtf8())
            assertEquals("REMOTE_ERROR", body.getString("classification"))
        }
}
