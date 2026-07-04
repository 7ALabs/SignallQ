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
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.core.network.AnalyticsHelper
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
     */
    @Provides
    @Singleton
    fun provideDiagnosticOrchestrator(analyticsHelper: AnalyticsHelper): DiagnosticOrchestrator =
        DiagnosticOrchestrator(analyticsHelper)

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
}
