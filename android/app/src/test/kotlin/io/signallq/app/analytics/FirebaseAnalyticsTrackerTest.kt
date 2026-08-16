package io.signallq.app.analytics

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.google.firebase.analytics.FirebaseAnalytics
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import io.signallq.app.BuildConfig
import io.signallq.app.core.network.AssistAbandonado
import io.signallq.app.core.network.AssistEtapa
import io.signallq.app.core.network.AssistObjetivoSelecionado
import io.signallq.app.core.network.AssistOrigem
import io.signallq.app.core.network.AssistPerguntaRespondida
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Testes unitarios de [FirebaseAnalyticsTracker], com foco na cobertura nova de
 * GH#1360 (user properties `environment`/`dist_channel`/`build_type`).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FirebaseAnalyticsTrackerTest {
    private lateinit var firebaseAnalytics: FirebaseAnalytics
    private lateinit var tracker: FirebaseAnalyticsTracker

    @Before
    fun setUp() {
        firebaseAnalytics = mockk(relaxed = true)
        val context = ApplicationProvider.getApplicationContext<Context>()
        tracker = FirebaseAnalyticsTracker(firebaseAnalytics, context)
    }

    @Test
    fun `registrarSessionStart define as tres user properties de ambiente`() {
        tracker.registrarSessionStart()

        verify { firebaseAnalytics.setUserProperty("environment", any()) }
        verify { firebaseAnalytics.setUserProperty("dist_channel", any()) }
        verify { firebaseAnalytics.setUserProperty("build_type", BuildConfig.BUILD_TYPE) }
    }

    @Test
    fun `registrarSessionStart usa staging como environment sem instalador Play Store`() {
        // Robolectric nao simula instalacao via Play Store — getInstallSourceInfo
        // nao retorna "com.android.vending", entao distributionChannel cai em
        // "sideload"/"unknown" e environmentFor mapeia para "staging" (nunca "production").
        tracker.registrarSessionStart()

        verify { firebaseAnalytics.setUserProperty("environment", "staging") }
    }

    @Test
    fun `registrarSessionStart continua enviando o evento app_session_start`() {
        tracker.registrarSessionStart()

        verify { firebaseAnalytics.logEvent("app_session_start", any()) }
    }

    // GH#1480 (Epico #1347, F4) — feature_blocked_remote.
    @Test
    fun `registrarFeatureBloqueadaRemota envia feature_blocked_remote com o feature_id`() {
        tracker.registrarFeatureBloqueadaRemota("wifi")

        verify { firebaseAnalytics.logEvent("feature_blocked_remote", any()) }
    }

    @Test
    fun `eventos Assist usam apenas ids fechados e propriedades especificadas`() {
        val objetivoBundle = slot<android.os.Bundle>()
        val respostaBundle = slot<android.os.Bundle>()

        tracker.registrarAssistObjetivo(AssistObjetivoSelecionado("jogos_com_lag", AssistOrigem.Inicio2, false))
        tracker.registrarAssistResposta(AssistPerguntaRespondida("jogos_com_lag", "conexao_jogo", "opcao_2", false))

        verify { firebaseAnalytics.logEvent("diagnostico_objetivo_selecionado", capture(objetivoBundle)) }
        verify { firebaseAnalytics.logEvent("diagnostico_pergunta_respondida", capture(respostaBundle)) }
        assert(objetivoBundle.captured.keySet() == setOf("objetivo", "origem", "retomada"))
        assert(respostaBundle.captured.keySet() == setOf("objetivo", "pergunta_id", "resposta_id", "retomada"))
    }

    // Review da PR #1683 — registrarAssistAbandono é o único dos 3 eventos do Assist com forma
    // de bundle variável (objetivo é opcional: nulo quando abandona ainda na lista de sintomas,
    // preenchido quando abandona na pergunta contextual). Os outros 2 eventos sempre têm o mesmo
    // conjunto de chaves, então a trava de keySet() já bastava; este precisa cobrir os dois casos.
    @Test
    fun `registrarAssistAbandono na lista de sintomas nao inclui a chave objetivo`() {
        val bundle = slot<android.os.Bundle>()

        tracker.registrarAssistAbandono(AssistAbandonado(AssistEtapa.Objetivo, objetivoId = null, retomavel = false))

        verify { firebaseAnalytics.logEvent("diagnostico_guiado_abandonado", capture(bundle)) }
        assert(bundle.captured.keySet() == setOf("etapa", "retomavel"))
    }

    @Test
    fun `registrarAssistAbandono com objetivo preenchido inclui a chave objetivo`() {
        val bundle = slot<android.os.Bundle>()

        // AssistScreen hoje só abandona de fato na lista (voltar na pergunta contextual volta
        // um passo em vez de abandonar — review da PR #1683, bloqueios 1/2), então
        // objetivoId preenchido não é produzido em produção agora; o mapeamento do tracker
        // continua correto pra essa forma do dado, e a interface não impede outro chamador
        // futuro de reintroduzir o caso.
        tracker.registrarAssistAbandono(AssistAbandonado(AssistEtapa.Contexto, objetivoId = "jogos_com_lag", retomavel = false))

        verify { firebaseAnalytics.logEvent("diagnostico_guiado_abandonado", capture(bundle)) }
        assert(bundle.captured.keySet() == setOf("etapa", "objetivo", "retomavel"))
    }
}
