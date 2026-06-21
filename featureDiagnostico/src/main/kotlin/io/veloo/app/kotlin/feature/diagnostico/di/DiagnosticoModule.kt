package io.veloo.app.feature.diagnostico.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.veloo.app.feature.diagnostico.BuildConfig
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.veloo.app.feature.diagnostico.topology.TopologyDiagnostic
import okhttp3.OkHttpClient
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DiagnosticoModule {
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
}
