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
import io.signallq.app.feature.diagnostico.remote.DiagnosticDivergenceReporter
import io.signallq.app.feature.diagnostico.remote.DiagnosticRolloutStatusRepository
import io.signallq.app.feature.diagnostico.remote.FileRulesetCacheStore
import io.signallq.app.feature.diagnostico.remote.ProviderDirectoryCache
import io.signallq.app.feature.diagnostico.remote.ProviderDirectoryRepository
import io.signallq.app.feature.diagnostico.remote.RemoteDiagnosticRepository
import io.signallq.app.feature.diagnostico.remote.RoomProviderDirectoryCache
import io.signallq.app.core.database.SignallQDatabase
import io.signallq.app.core.database.provider.ProviderDirectoryCacheDao
import io.signallq.app.core.database.recommendation.RecommendationHistoryDao
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.featureflags.FeatureFlagProvider
import io.signallq.app.core.network.AnalyticsHelper
import io.signallq.app.core.nds.NdsClient
import io.signallq.app.core.nds.NdsClientFactory
import io.signallq.app.core.recommendation.RecommendationEngine
import io.signallq.app.core.recommendation.catalog.LocalRecommendationCatalog
import io.signallq.app.core.recommendation.catalog.RecommendationCatalog
import io.signallq.app.feature.diagnostico.ingest.AdminIngestRepository
import io.signallq.app.feature.diagnostico.nds.NdsDiagnosticRepository
import io.signallq.app.feature.diagnostico.topology.TopologyDiagnostic
import okhttp3.OkHttpClient
import java.io.File
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
     *
     * NDS-02k (issue #1759): tambem injeta [NdsDiagnosticRepository] e o mesmo
     * [FeatureFlagProvider] do grafo (`RemoteConfigFeatureFlagProvider`, ver `AppModule`) —
     * `DiagnosticOrchestrator.executarProtegido` decide entre os dois motores remotos
     * conforme `FeatureFlagKeys.CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED`.
     */
    @Provides
    @Singleton
    fun provideDiagnosticOrchestrator(
        analyticsHelper: AnalyticsHelper,
        remoteDiagnosticRepository: RemoteDiagnosticRepository,
        ndsDiagnosticRepository: NdsDiagnosticRepository,
        featureFlagProvider: FeatureFlagProvider,
    ): DiagnosticOrchestrator =
        DiagnosticOrchestrator(analyticsHelper, remoteDiagnosticRepository, ndsDiagnosticRepository, featureFlagProvider)

    /**
     * Provê NdsClient no grafo Hilt (NDS-02k, issue #1759) — via [NdsClientFactory],
     * mesma convencao ja documentada la (`BuildConfig.NDS_BASE_URL`/`NDS_API_TOKEN` de
     * `:core:nds`, timeout de 12s — ver kdoc do construtor de [NdsClient]).
     */
    @Provides
    @Singleton
    fun provideNdsClient(): NdsClient = NdsClientFactory.create()

    /**
     * Provê NdsDiagnosticRepository no grafo Hilt (NDS-02k, issue #1759) — chamada viva
     * remoto-primeiro/fallback-total ao NDS, atras da flag
     * `consumer_diagnostico_nds_live_enabled` (decidida em [DiagnosticOrchestrator], nao aqui).
     */
    @Provides
    @Singleton
    fun provideNdsDiagnosticRepository(
        ndsClient: NdsClient,
        analyticsHelper: AnalyticsHelper,
    ): NdsDiagnosticRepository = NdsDiagnosticRepository(ndsClient = ndsClient, analyticsHelper = analyticsHelper)

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
     *
     * GH#1450: [FileRulesetCacheStore] persiste o ultimo ruleset remoto valido em
     * `filesDir/diagnostic_ruleset` (nunca `cacheDir` — dado usado como fallback de
     * seguranca, o SO nao pode limpar sob pressao de storage), habilitando o nivel
     * intermediario `CACHED_LOCAL` do fallback de 3 niveis (#952).
     */
    @Provides
    @Singleton
    fun provideRemoteDiagnosticRepository(
        @ApplicationContext ctx: Context,
        divergenceReporter: DiagnosticDivergenceReporter,
    ): RemoteDiagnosticRepository =
        RemoteDiagnosticRepository(
            baseUrl = BuildConfig.DIAGNOSTIC_WORKER_URL,
            cacheStore = FileRulesetCacheStore(File(ctx.filesDir, "diagnostic_ruleset")),
            divergenceReporter = divergenceReporter,
        )

    /**
     * Provê DiagnosticRolloutStatusRepository (GH#1445, parte de #952) — busca
     * (com cache curto) o percentual/segmentacao de rollout publicados pelo
     * worker `signallq-diagnostic` (`GET /diagnostic/rollout-status`).
     */
    @Provides
    @Singleton
    fun provideDiagnosticRolloutStatusRepository(): DiagnosticRolloutStatusRepository =
        DiagnosticRolloutStatusRepository(
            baseUrl = BuildConfig.DIAGNOSTIC_WORKER_URL,
            client = OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(5, TimeUnit.SECONDS)
                .writeTimeout(5, TimeUnit.SECONDS)
                .build(),
        )

    /**
     * Provê DiagnosticDivergenceReporter (GH#1444, parte de #952) — shadow mode,
     * envia o registro ja classificado (nao o relatorio completo) para
     * `POST /ingest/diagnostic-divergence` no mesmo worker `signallq-diagnostic`.
     *
     * Reusa o mesmo consentimento LGPD ja aplicado em [provideAdminIngestRepository]
     * (nenhum dado sai do aparelho sem o usuario ter consentido).
     *
     * GH#1445: [installationIdProvider] reusa o `anon_device_id` que ja existe
     * em [PreferenciasAppRepository] (nao cria identificador novo). [appChannel]
     * vem de `@Named("appDistributionChannel")`, provido em `AppModule` (`:app`)
     * — `distributionChannel()` vive em `io.signallq.app.analytics` (`:app`),
     * que `:featureDiagnostico` nao pode importar diretamente (feature nao
     * depende de `:app`); o binding `@Named` evita mover esse arquivo so por
     * causa disto.
     */
    @Provides
    @Singleton
    fun provideDiagnosticDivergenceReporter(
        featureFlagProvider: FeatureFlagProvider,
        prefs: PreferenciasAppRepository,
        rolloutStatusRepository: DiagnosticRolloutStatusRepository,
        @Named("appDistributionChannel") appChannel: String,
    ): DiagnosticDivergenceReporter =
        DiagnosticDivergenceReporter(
            baseUrl = BuildConfig.DIAGNOSTIC_WORKER_URL,
            client = OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .build(),
            featureFlagProvider = featureFlagProvider,
            appVersion = BuildConfig.APP_VERSION,
            versionCode = BuildConfig.VERSION_CODE,
            consentimentoProvider = { prefs.buscarConsentimentoLgpd() == true },
            installationIdProvider = { prefs.buscarOuGerarAnonDeviceId() },
            rolloutStatusProvider = { rolloutStatusRepository.current() },
            appChannel = appChannel,
        )

    /** Provê o DAO do cache local do diretorio remoto de provedores (GH#1462, parte de #951). */
    @Provides
    @Singleton
    fun provideProviderDirectoryCacheDao(bancoDados: SignallQDatabase): ProviderDirectoryCacheDao =
        bancoDados.providerDirectoryCacheDao()

    /** Provê ProviderDirectoryCache no grafo Hilt (GH#1462, parte de #951) — implementacao
     *  Room, ultimo resultado valido por consulta (findById/searchByName). */
    @Provides
    @Singleton
    fun provideProviderDirectoryCache(dao: ProviderDirectoryCacheDao): ProviderDirectoryCache =
        RoomProviderDirectoryCache(dao)

    /**
     * Provê ProviderDirectoryRepository no grafo Hilt (GH#965) — diretorio remoto
     * de provedores de cauda longa (logo + contato). Consumido pelo resolver de
     * identidade de operadora em `:app` (catalogo local -> este repository ->
     * fallback generico), nunca direto por Composable.
     *
     * GH#1462: [cache] permite responder com o ultimo dado valido conhecido quando a
     * rede falha, antes do resolver cair no fallback generico final (ver kdoc de
     * [ProviderDirectoryRepository]).
     */
    @Provides
    @Singleton
    fun provideProviderDirectoryRepository(cache: ProviderDirectoryCache): ProviderDirectoryRepository =
        ProviderDirectoryRepository(baseUrl = BuildConfig.DIAGNOSTIC_WORKER_URL, cache = cache)

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
