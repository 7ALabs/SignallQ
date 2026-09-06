package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.core.diagnostico.DiagnosticRolloutStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber

/**
 * Busca a configuracao de rollout (percentual + segmentacao) do ruleset
 * PUBLISHED atual no worker `signallq-diagnostic` — GH#1445, parte de #952.
 *
 * Endpoint publico (`GET /diagnostic/rollout-status`), sem PII e sem sessao
 * admin — o mesmo motivo de [RemoteDiagnosticRepository.evaluateRemote] ser
 * publico: chamado antes de qualquer conta/sessao existir no app.
 *
 * ## Cache em memoria com TTL curto
 * [DiagnosticDivergenceReporter] consulta isto antes de CADA diagnostico
 * (potencialmente varias vezes por sessao), e o valor muda raramente (so
 * quando alguem publica ruleset novo ou atualiza rollout no admin). Sem
 * cache, cada diagnostico pagaria uma chamada de rede extra so pra saber se
 * vai ou nao disparar OUTRA chamada de rede (a comparacao remota em si).
 * [cacheTtlMs] tambem e o limite pratico de quanto tempo o criterio de
 * aceite "reduzir rollout_percent pra 0 remove instalacoes sem redeploy"
 * (#1445) leva pra propagar num app ja aberto — reduzir o TTL deixa isso
 * mais rapido, ao custo de mais chamadas de rede.
 *
 * Falha de rede/parse: devolve o ultimo valor cacheado (mesmo expirado) em
 * vez de `null`, quando existir — evita que uma falha transitoria do worker
 * derrube o shadow mode inteiro. Sem NENHUM valor cacheado ainda, devolve
 * `null` — o chamador ([DiagnosticDivergenceReporter]) trata isso como "nao
 * participa" (fail closed).
 */
class DiagnosticRolloutStatusRepository(
    private val baseUrl: String,
    private val client: OkHttpClient,
    private val cacheTtlMs: Long = DEFAULT_CACHE_TTL_MS,
) {
    @Volatile
    private var cached: Pair<Long, DiagnosticRolloutStatus>? = null

    suspend fun current(): DiagnosticRolloutStatus? {
        val snapshot = cached
        val now = System.currentTimeMillis()
        if (snapshot != null && now - snapshot.first < cacheTtlMs) {
            return snapshot.second
        }

        val fetched = fetch()
        if (fetched != null) {
            cached = now to fetched
            return fetched
        }
        return snapshot?.second
    }

    private suspend fun fetch(): DiagnosticRolloutStatus? =
        withContext(Dispatchers.IO) {
            withTimeoutOrNull(TIMEOUT_MS) {
                try {
                    val url = baseUrl.trimEnd('/') + "/diagnostic/rollout-status"
                    val request =
                        Request
                            .Builder()
                            .url(url)
                            .get()
                            .build()
                    client.newCall(request).execute().use { response ->
                        if (!response.isSuccessful) {
                            Timber.w("DiagnosticRolloutStatusRepository: worker HTTP ${response.code}")
                            return@use null
                        }
                        val text = response.body?.string()
                        if (text.isNullOrBlank()) {
                            Timber.w("DiagnosticRolloutStatusRepository: worker retornou corpo vazio")
                            return@use null
                        }
                        parseStatus(text)
                    }
                } catch (t: Throwable) {
                    Timber.w(t, "DiagnosticRolloutStatusRepository: falha ao buscar rollout status")
                    null
                }
            }
        }

    private fun parseStatus(text: String): DiagnosticRolloutStatus? =
        try {
            val json = JSONObject(text)
            DiagnosticRolloutStatus(
                rulesetVersion = if (json.has("rulesetVersion") && !json.isNull("rulesetVersion")) json.optInt("rulesetVersion") else null,
                rolloutPercent = json.optInt("rolloutPercent", 0),
                rolloutMinVersionCode = if (json.has("rolloutMinVersionCode") && !json.isNull("rolloutMinVersionCode")) json.optInt("rolloutMinVersionCode") else null,
                rolloutChannels =
                    json.optJSONArray("rolloutChannels")?.let { array ->
                        (0 until array.length()).map { index -> array.getString(index) }
                    },
            )
        } catch (t: Throwable) {
            Timber.w(t, "DiagnosticRolloutStatusRepository: JSON invalido do worker")
            null
        }

    private companion object {
        const val TIMEOUT_MS = 5_000L
        const val DEFAULT_CACHE_TTL_MS = 15 * 60 * 1000L
    }
}
