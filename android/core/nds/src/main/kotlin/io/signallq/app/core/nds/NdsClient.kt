package io.signallq.app.core.nds

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import timber.log.Timber
import java.util.concurrent.TimeUnit

private const val EVALUATE_PATH = "/v1/diagnostics/evaluate"

/**
 * Cliente HTTP para o Network Diagnostics Service (NDS) — fatia NDS-01
 * (#1744, ADR-017). Isolado: nenhuma tela do app depende deste cliente ainda,
 * a religacao dos consumidores reais (Home, Wifi, Devices, Diagnostico) e
 * fatia seguinte (NDS-02+).
 *
 * Autenticacao via Bearer token estatico ([apiToken]). O valor real NUNCA
 * fica hardcoded neste modulo — chega via `BuildConfig.NDS_API_TOKEN`
 * (`core/nds/build.gradle.kts`), lido de `local.properties`/variavel de
 * ambiente, nunca commitado (ver ADR-017, secao "Autenticacao").
 */
class NdsClient(
    private val baseUrl: String,
    private val apiToken: String,
    /**
     * Timeout reduzido de 20s (fatia NDS-01) para 12s de leitura — decisao da
     * NDS-02k (issue #1759, item 8). Os dois gatilhos de producao que passaram a
     * chamar [evaluate] (`DiagnosticOrchestrator.executarProtegido`, atras da
     * flag `consumer_diagnostico_nds_live_enabled`) disparam em BACKGROUND, sem
     * "aguarde" explicito do usuario — bem diferente do padrao de UI que
     * justificava um teto mais largo em outros pontos do app (ex.: o teto de
     * 42s de `RemoteDiagnosticRepository`, atras de uma tela que ja mostra
     * estado de carregamento). 12s fica ACIMA do `EVALUATE_TIMEOUT_MS` default
     * do proprio NDS (10000ms, ADR-017) — da margem para o servidor responder
     * com o envelope de erro estruturado (`504 NDS_TIMEOUT`) antes do cliente
     * abortar por conta propria, e ainda assim bem abaixo dos 20s antigos. O
     * fallback total para `DiagnosticRunner` local (NdsDiagnosticRepository,
     * `:featureDiagnostico`) cobre qualquer estouro deste teto sem travar a UI.
     */
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(5, TimeUnit.SECONDS)
            .readTimeout(12, TimeUnit.SECONDS)
            .writeTimeout(5, TimeUnit.SECONDS)
            .build(),
) {
    /**
     * Avalia o snapshot de rede enviado e devolve o resultado modular do NDS.
     * Nunca lanca excecao para o chamador — qualquer falha (auth, rate limit,
     * timeout, corpo inesperado) volta como [NdsDiagnosticsOutcome.KnownError]
     * ou [NdsDiagnosticsOutcome.UnknownError].
     */
    suspend fun evaluate(request: NdsDiagnosticsRequest): NdsDiagnosticsOutcome =
        withContext(Dispatchers.IO) {
            try {
                val body =
                    request.toJson().toString()
                        .toRequestBody("application/json; charset=utf-8".toMediaType())
                val httpRequest =
                    Request.Builder()
                        .url(baseUrl.trimEnd('/') + EVALUATE_PATH)
                        .addHeader("Authorization", "Bearer $apiToken")
                        .post(body)
                        .build()

                client.newCall(httpRequest).execute().use { response ->
                    val bodyText = response.body.string()
                    when {
                        !response.isSuccessful -> parseErrorOutcome(response.code, bodyText)
                        bodyText.isBlank() ->
                            NdsDiagnosticsOutcome.UnknownError(response.code, bodyText)
                        else -> parseSuccessOutcome(response.code, bodyText)
                    }
                }
            } catch (t: Throwable) {
                Timber.w("NdsClient.evaluate falhou: ${t::class.simpleName} — ${t.message}")
                NdsDiagnosticsOutcome.UnknownError(statusCode = null, rawBody = null, cause = t)
            }
        }

    private fun parseSuccessOutcome(statusCode: Int, bodyText: String): NdsDiagnosticsOutcome =
        try {
            NdsDiagnosticsOutcome.Success(NdsResponseParser.parse(bodyText))
        } catch (t: Throwable) {
            Timber.w("NdsClient: resposta ${statusCode} nao parseavel — ${t.message}")
            NdsDiagnosticsOutcome.UnknownError(statusCode, bodyText, t)
        }

    /**
     * Corpo de erro defensivo para os DOIS formatos possiveis (NDS-02k, issue
     * #1759, item 9) — tenta o envelope canonico do PR #12 do NDS primeiro
     * (`{"error":{"code","message","retryable"},"request_id"}`, ADR-017, ainda
     * em DRAFT/nao confirmado em producao), cai para o shape antigo flat
     * confirmado (`{"error","message"}`, 401/429) se o corpo nao bater com o
     * novo. Nenhum dos dois formatos lanca excecao — corpo que nao bate com
     * shape nenhum vira [NdsDiagnosticsOutcome.UnknownError], o fallback final.
     */
    private fun parseErrorOutcome(statusCode: Int, bodyText: String?): NdsDiagnosticsOutcome {
        if (bodyText.isNullOrBlank()) return NdsDiagnosticsOutcome.UnknownError(statusCode, bodyText)
        return try {
            val parsed = JSONObject(bodyText)
            parseCanonicalEnvelope(statusCode, parsed)
                ?: parseLegacyFlatError(statusCode, parsed)
                ?: NdsDiagnosticsOutcome.UnknownError(statusCode, bodyText)
        } catch (t: Throwable) {
            NdsDiagnosticsOutcome.UnknownError(statusCode, bodyText, t)
        }
    }

    /** `null` quando `error` nao e um objeto JSON ou falta `code`/`message` —
     *  nesse caso [parseErrorOutcome] tenta o shape antigo em seguida. */
    private fun parseCanonicalEnvelope(statusCode: Int, parsed: JSONObject): NdsDiagnosticsOutcome.KnownError? {
        val errorObj = parsed.optJSONObject("error") ?: return null
        val code = errorObj.optStringOrNull("code") ?: return null
        val message = errorObj.optStringOrNull("message") ?: return null
        val retryable = if (errorObj.has("retryable") && !errorObj.isNull("retryable")) {
            errorObj.optBoolean("retryable")
        } else {
            null
        }
        return NdsDiagnosticsOutcome.KnownError(
            statusCode = statusCode,
            error = code,
            message = message,
            code = code,
            retryable = retryable,
            requestId = parsed.optStringOrNull("request_id"),
        )
    }

    /** `null` quando `error`/`message` (ambos String, shape antigo) nao estao
     *  presentes — nesse caso [parseErrorOutcome] cai para [NdsDiagnosticsOutcome.UnknownError]. */
    private fun parseLegacyFlatError(statusCode: Int, parsed: JSONObject): NdsDiagnosticsOutcome.KnownError? {
        val error = parsed.optStringOrNull("error") ?: return null
        val message = parsed.optStringOrNull("message") ?: return null
        return NdsDiagnosticsOutcome.KnownError(statusCode, error, message)
    }
}
