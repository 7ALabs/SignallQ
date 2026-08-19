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
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(20, TimeUnit.SECONDS)
            .writeTimeout(10, TimeUnit.SECONDS)
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
     * 401 e 429 tem shape confirmado no ADR-017: `{"error","message"}`.
     * Formato de erro generico (5xx, timeout de rede) NAO tem shape
     * confirmado — nunca assumimos JSON parseavel; corpo que nao bate com o
     * shape conhecido vira [NdsDiagnosticsOutcome.UnknownError] em vez de
     * lancar excecao de parse.
     */
    private fun parseErrorOutcome(statusCode: Int, bodyText: String?): NdsDiagnosticsOutcome {
        if (bodyText.isNullOrBlank()) return NdsDiagnosticsOutcome.UnknownError(statusCode, bodyText)
        return try {
            val parsed = JSONObject(bodyText)
            val error = parsed.optStringOrNull("error")
            val message = parsed.optStringOrNull("message")
            if (error != null && message != null) {
                NdsDiagnosticsOutcome.KnownError(statusCode, error, message)
            } else {
                NdsDiagnosticsOutcome.UnknownError(statusCode, bodyText)
            }
        } catch (t: Throwable) {
            NdsDiagnosticsOutcome.UnknownError(statusCode, bodyText, t)
        }
    }
}
