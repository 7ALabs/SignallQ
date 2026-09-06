package io.signallq.app.feature.diagnostico.recommendation

import io.signallq.app.core.recommendation.DiagnosticTag
import io.signallq.app.core.recommendation.NetworkContextType
import io.signallq.app.core.recommendation.Recommendation
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationEngine
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.core.recommendation.RecommendationFlags
import io.signallq.app.core.recommendation.RecommendationRequest
import io.signallq.app.core.recommendation.RecommendationType
import io.signallq.app.core.recommendation.catalog.RecommendationCatalog
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Cobre a integracao completa historico persistido (Room via [FakeRecommendationHistoryDao])
 * + [RecommendationEngine] -- criterio de aceite da issue #812: cooldown, limite diario e
 * limite semanal batendo com o comportamento esperado do engine, alimentado pelo
 * historico real vindo da camada de persistencia.
 */
class RecommendationHistoryEngineIntegrationTest {
    private val hourMs = 3_600_000L
    private val dayMs = 24 * hourMs
    private val weekMs = 7 * dayMs

    private fun catalogWith(vararg items: Recommendation) =
        object : RecommendationCatalog {
            override fun all(): List<Recommendation> = items.toList()
        }

    private fun request() =
        RecommendationRequest(
            tags = setOf(DiagnosticTag.WIFI_FRACO),
            network = NetworkContextType.WIFI,
            flags = RecommendationFlags(nativeAdFallbackEnabled = false),
        )

    /** Constroi uma decisao "de fachada" so para persistir uma exibicao historica com controle total do instante. */
    private fun decisaoDe(
        recommendation: Recommendation,
        trackingId: String,
    ) =
        RecommendationDecision(
            recommendation = recommendation,
            matchedTags = recommendation.tags,
            score = 0.0,
            priorityTier = 0,
            reason = "seed de teste",
            trackingId = trackingId,
        )

    private suspend fun escolherEPersistir(
        repository: RecommendationHistoryRepository,
        engine: RecommendationEngine,
        nowMs: Long,
    ): RecommendationDecision? {
        val historico = repository.historicoRecente(nowMs)
        val decisao = engine.choose(request().copy(history = historico))
        decisao?.let { repository.registrarExibicao(it, nowMs) }
        return decisao
    }

    @Test
    fun `cooldown bloqueia repeticao antes do prazo e libera depois`() =
        runTest {
            val candidato =
                Recommendation(
                    id = "free_tip_teste",
                    type = RecommendationType.FREE_TIP,
                    title = "Recomendacao com cooldown",
                    tags = setOf(DiagnosticTag.WIFI_FRACO),
                    cooldownHours = 24,
                )
            val decoy =
                Recommendation(
                    id = "decoy",
                    type = RecommendationType.FREE_TIP,
                    title = "Decoy -- so para nao ser a mesma id do turno anterior",
                    tags = setOf(DiagnosticTag.WIFI_FRACO),
                )
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())
            val engine = RecommendationEngine(catalog = catalogWith(candidato), now = { 0L })

            // Seed: candidato exibido em t=0, decoy exibido logo depois em t=1000 -- assim o
            // "mais recente geral" nao e o candidato, e o teste isola exatamente a regra de cooldown
            // (sem cair na regra separada de "nao repetir a mesma recomendacao em sequencia").
            repository.registrarExibicao(decisaoDe(candidato, "candidato-0"), shownAtEpochMs = 0L)
            repository.registrarExibicao(decisaoDe(decoy, "decoy-1000"), shownAtEpochMs = 1_000L)

            val dentroDoCooldown =
                escolherEPersistir(
                    repository,
                    RecommendationEngine(catalog = catalogWith(candidato), now = { 1_000L + hourMs }),
                    nowMs = 1_000L + hourMs,
                )
            assertNull(dentroDoCooldown)

            val aposCooldown =
                escolherEPersistir(
                    repository,
                    RecommendationEngine(catalog = catalogWith(candidato), now = { 24 * hourMs + 1L }),
                    nowMs = 24 * hourMs + 1L,
                )
            assertNotNull(aposCooldown)
        }

    @Test
    fun `limite diario bloqueia apos maxPerDay exibicoes no mesmo dia e libera no dia seguinte`() =
        runTest {
            val candidato =
                Recommendation(
                    id = "free_tip_teste",
                    type = RecommendationType.FREE_TIP,
                    title = "Recomendacao com limite diario",
                    tags = setOf(DiagnosticTag.WIFI_FRACO),
                    maxPerDay = 2,
                )
            val decoy =
                Recommendation(
                    id = "decoy",
                    type = RecommendationType.FREE_TIP,
                    title = "Decoy",
                    tags = setOf(DiagnosticTag.WIFI_FRACO),
                )
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())

            repository.registrarExibicao(decisaoDe(candidato, "c-0"), shownAtEpochMs = 0L)
            repository.registrarExibicao(decisaoDe(candidato, "c-1000"), shownAtEpochMs = 1_000L)
            repository.registrarExibicao(decisaoDe(decoy, "decoy-2000"), shownAtEpochMs = 2_000L)

            val noMesmoDia =
                escolherEPersistir(
                    repository,
                    RecommendationEngine(catalog = catalogWith(candidato), now = { 3_000L }),
                    nowMs = 3_000L,
                )
            assertNull(noMesmoDia)

            // Mais de 24h depois das duas exibicoes que contavam para o limite diario.
            val noDiaSeguinte =
                escolherEPersistir(
                    repository,
                    RecommendationEngine(catalog = catalogWith(candidato), now = { dayMs + 5_000L }),
                    nowMs = dayMs + 5_000L,
                )
            assertNotNull(noDiaSeguinte)
        }

    @Test
    fun `limite semanal bloqueia apos maxPerWeek exibicoes na semana e libera apos a janela`() =
        runTest {
            val candidato =
                Recommendation(
                    id = "free_tip_teste",
                    type = RecommendationType.FREE_TIP,
                    title = "Recomendacao com limite semanal",
                    tags = setOf(DiagnosticTag.WIFI_FRACO),
                    maxPerWeek = 2,
                )
            val decoy =
                Recommendation(
                    id = "decoy",
                    type = RecommendationType.FREE_TIP,
                    title = "Decoy",
                    tags = setOf(DiagnosticTag.WIFI_FRACO),
                )
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())

            // Duas exibicoes em dias diferentes (fora do cooldown, que e 0 aqui), ambas dentro da semana.
            repository.registrarExibicao(decisaoDe(candidato, "c-0"), shownAtEpochMs = 0L)
            repository.registrarExibicao(decisaoDe(candidato, "c-2dias"), shownAtEpochMs = 2 * dayMs)
            repository.registrarExibicao(decisaoDe(decoy, "decoy"), shownAtEpochMs = 2 * dayMs + 1_000L)

            val dentroDaSemana =
                escolherEPersistir(
                    repository,
                    RecommendationEngine(catalog = catalogWith(candidato), now = { 3 * dayMs }),
                    nowMs = 3 * dayMs,
                )
            assertNull(dentroDaSemana)

            // nowMs = 8 dias: a exibicao de t=0 sai da janela de 7 dias, sobra so 1 na semana (< maxPerWeek).
            val aposJanela =
                escolherEPersistir(
                    repository,
                    RecommendationEngine(catalog = catalogWith(candidato), now = { 8 * dayMs }),
                    nowMs = 8 * dayMs,
                )
            assertNotNull(aposJanela)
        }

    @Test
    fun `feedback HIDE bloqueia a recomendacao permanentemente`() =
        runTest {
            val candidato =
                Recommendation(
                    id = "free_tip_teste",
                    type = RecommendationType.FREE_TIP,
                    title = "Recomendacao ocultavel",
                    tags = setOf(DiagnosticTag.WIFI_FRACO),
                )
            val repository = RecommendationHistoryRepository(FakeRecommendationHistoryDao())
            val engine = RecommendationEngine(catalog = catalogWith(candidato), now = { 0L })

            val primeira = escolherEPersistir(repository, engine, nowMs = 0L)
            assertNotNull(primeira)

            repository.registrarFeedback(primeira!!.trackingId, RecommendationFeedbackType.HIDE, feedbackAtEpochMs = 500L)

            // 10 dias depois -- ainda dentro da janela de leitura do historico (14 dias),
            // confirma que o bloqueio por HIDE nao "expira" como cooldown/frequencia.
            val depoisDoHide =
                escolherEPersistir(
                    repository,
                    RecommendationEngine(catalog = catalogWith(candidato), now = { 10 * dayMs }),
                    nowMs = 10 * dayMs,
                )
            assertNull(depoisDoHide)
        }
}
