package io.signallq.app.feature.diagnostico.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.signallq.app.feature.diagnostico.BuildConfig
import io.signallq.app.feature.diagnostico.DiagnosticOrchestrator
import io.signallq.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.signallq.app.feature.diagnostico.remote.ProviderDirectoryRepository
import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import io.signallq.app.core.database.SignallQDatabase
import io.signallq.app.core.database.recommendation.RecommendationHistoryDao
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.recommendation.RecommendationEngine
import io.signallq.app.core.recommendation.catalog.LocalRecommendationCatalog
import io.signallq.app.core.recommendation.catalog.RecommendationCatalog
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.topology.TopologyDiagnostic
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticoModule {
    /**
     * Provê DiagnosticOrchestrator como @Singleton no grafo Hilt.
     *
     * Antes era instanciado via `by lazy { DiagnosticOrchestrator() }` no MainViewModel.
     * Agora e singleton compartilhado entre DiagnosticoViewModel e MainViewModel (legado).
     *
     * GH#969: reusa a mesma instancia de [RemoteDiagnosticRepository] ja provida abaixo
     * (nao cria um OkHttpClient/cache novo so pra este orquestrador).
     */
    @Provides
    @Singleton
    fun provideDiagnosticOrchestrator(
        analyticsHelper: AnalyticsHelper,
        remoteDiagnosticRepository: RemoteDiagnosticRepository,
    ): DiagnosticOrchestrator = DiagnosticOrchestrator(analyticsHelper, remoteDiagnosticRepository)

    /**
     * Provê a instância única de AiDiagnosisRepository no grafo Hilt.
     *
     * Antes desta mudança, AiDiagnosisRepository era instanciada manualmente em dois locais:
     *  - MainViewModel (diagAiRepository by lazy { AiDiagnosisRepository(...) })
     *  - SignallQOrchestrator (private val aiRepository = AiDiagnosisRepository(...))
     *
     * Resultado anterior: dois caches ConcurrentHashMap independentes e a URL duplicada
     * em dois arquivos. Agora há uma única instância com um único cache.
     */
    @Provides
    @Singleton
    fun provideAiDiagnosisRepository(): AiDiagnosisRepository =
        AiDiagnosisRepository(
            baseUrl = BuildConfig.AI_WORKER_URL,
            isAuthorized = { true },
        )

    /**
     * Provê RemoteDiagnosticRepository no grafo Hilt (GH#962).
     *
     * GH#969: wireada no [io.signallq.app.feature.diagnostico.DiagnosticOrchestrator] —
     * fluxo remoto-primeiro com fallback automatico pro motor local, sem mudanca
     * perceptivel de UI (o orquestrador so troca a fonte do relatorio).
     */
    @Provides
    @Singleton
    fun provideRemoteDiagnosticRepository(): RemoteDiagnosticRepository =
        RemoteDiagnosticRepository(baseUrl = BuildConfig.DIAGNOSTIC_WORKER_URL)

    /**
     * Provê ProviderDirectoryRepository no grafo Hilt (GH#965) — diretorio remoto
     * de provedores de cauda longa (logo + contato). Consumido pelo resolver de
     * identidade de operadora em `:app` (catalogo local -> este repository ->
     * fallback generico), nunca direto por Composable.
     */
    @Provides
    @Singleton
    fun provideProviderDirectoryRepository(): ProviderDirectoryRepository =
        ProviderDirectoryRepository(baseUrl = BuildConfig.DIAGNOSTIC_WORKER_URL)

    /**
     * Provê TopologyDiagnostic no grafo Hilt.
     *
     * Usa upnpIgdClient (5s) — necessário para IGD discovery em redes ADSL/4G lentas.
     * NÃO usar upnpClient (2s) aqui: o timeout reduzido causa regressão em discovery
     * quando o roteador demora a responder ao fetch do XML de descrição UPnP.
     */
    @Provides
    @Singleton
    fun provideTopologyDiagnostic(
        @ApplicationContext ctx: Context,
        @Named("upnpIgdClient") httpClient: OkHttpClient,
    ): TopologyDiagnostic = TopologyDiagnostic(context = ctx, httpClient = httpClient)

    /**
     * Cliente HTTP dedicado para ingest de telemetria (best-effort, sem retry).
     *
     * Timeout curto intencional: ingest e fire-and-forget. Nao queremos bloquear
     * o fluxo do usuario esperando confirmacao do servidor. Se o worker nao
     * responder em 10s, desistimos silenciosamente.
     */
    @Provides
    @Singleton
    @Named("adminIngestClient")
    fun provideAdminIngestOkHttpClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(10, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
            .build()

    /**
     * Provê AdminIngestRepository — envia telemetria ao signallq-admin-worker.
     *
     * Usa ADMIN_INGEST_URL e ADMIN_INGEST_KEY do BuildConfig do modulo app
     * (passados como parametro pelo grafo Hilt via @Named).
     *
     * Chave separada do ADMIN_SECRET: vazar INGEST_KEY nao da acesso de leitura
     * ao painel. Scope limitado: POST /ingest/ apenas.
     */
    @Provides
    @Singleton
    fun provideAdminIngestRepository(
        @Named("adminIngestClient") httpClient: OkHttpClient,
        @Named("adminIngestUrl") baseUrl: String,
        @Named("adminIngestKey") ingestKey: String,
        prefs: PreferenciasAppRepository,
    ): AdminIngestRepository = AdminIngestRepository(
        baseUrl = baseUrl,
        ingestKey = ingestKey,
        client = httpClient,
        consentimentoProvider = { prefs.buscarConsentimentoLgpd() == true },
    )

    @Provides
    @Singleton
    fun provideRecommendationHistoryDao(bancoDados: SignallQDatabase): RecommendationHistoryDao =
        bancoDados.recommendationHistoryDao()

    /** Catalogo local embarcado -- ainda nao ha catalogo remoto (fora de escopo da #790). */
    @Provides
    @Singleton
    fun provideRecommendationCatalog(): RecommendationCatalog = LocalRecommendationCatalog()

    @Provides
    @Singleton
    fun provideRecommendationEngine(catalog: RecommendationCatalog): RecommendationEngine =
        RecommendationEngine(catalog = catalog)
}
