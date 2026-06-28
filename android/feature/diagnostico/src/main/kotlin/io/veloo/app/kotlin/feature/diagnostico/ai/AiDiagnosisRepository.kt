package io.signallq.app.feature.diagnostico.ai

import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

// =============================================================================
// AiDiagnosisRepository
// =============================================================================
// Estrategia de retrocompat:
//  - O cliente Kotlin sempre envia payload v2 (schemaVersion = "2") com campos
//    extras opcionais. O Worker antigo ignora os extras silenciosamente.
//  - O parser aceita schemaVersion "1" e "2" e tolera campos ausentes em ambos.
//  - Quando schemaVersion ausente ou "1" e nao houver `modeloIa` na resposta,
//    preenchemos defaults que nao mentem ("SignallQ IA").
//  - Em qualquer falha (sem auth, timeout, !2xx, JSON invalido) -> fallback
//    local com modeloIa = "Diagnostico local do SignallQ".
// =============================================================================

sealed class AiDiagnosisState {
    data object idle : AiDiagnosisState()
    data object loading : AiDiagnosisState()
    data class success(val result: AiDiagnosisResult) : AiDiagnosisState()
    data class fallback(val result: AiDiagnosisResult) : AiDiagnosisState()
    data class error(val code: String) : AiDiagnosisState()
    data object timeout : AiDiagnosisState()
}

class AiDiagnosisRepository(
    private val baseUrl: String,
    private val isAuthorized: () -> Boolean,
    private val client: OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            // Gemma 4 26B gera reasoning (500-2500 tokens) + JSON (700-1100 tokens)
            // antes de devolver resposta. Inferencia tipica: 40-60 s no free tier.
            // 90 s garante margem sem travar a UX por tempo indeterminado.
            .readTimeout(90, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build(),
    /** Fonte de tempo injetavel — permite controle deterministico em testes. */
    internal val clock: () -> Long = System::currentTimeMillis,
) {
    companion object {
        private const val CACHE_TTL_MS = 5 * 60 * 1000L // 5 minutos
    }

    init {
        // LOG TEMPORARIO — validar que existe apenas UMA instancia (Hilt @Singleton).
        // Se aparecer mais de um hashCode diferente no logcat, a injecao nao esta funcionando.
        // Remover apos validacao em device.
        // runCatching: Timber nao esta plantado em testes JVM — nao queremos
        // que este log de validacao quebre os testes unitarios do repositorio.
        runCatching { Timber.d("AiDiagnosisRepository criado: hashCode=${hashCode()}") }
    }

    // Pair: (resultado, timestamp de insercao em ms).
    // internal para permitir injecao direta em testes unitarios do mesmo modulo.
    internal val cache = ConcurrentHashMap<String, Pair<AiDiagnosisResult, Long>>()

    /**
     * Tokens do último streaming concluído. Atualizado ao processar evento SSE de usage.
     * Lido pelo ViewModel após o collect do Flow para persistir em ChatSessionEntity.
     * Thread-safe via @Volatile — escrita única por stream, leitura após collect.
     */
    @Volatile
    var lastStreamUsage: AiStreamUsage? = null
        private set

    /**
     * Verifica se o worker de IA está acessível.
     * Faz um HEAD request com timeout de 5s. Retorna false em qualquer falha.
     */
    suspend fun checkAvailability(): Boolean {
        return try {
            withContext(Dispatchers.IO) {
                val url = baseUrl.trimEnd('/') + "/api/ai/diagnostico-conexao"
                val req = Request.Builder().url(url).head().build()
                val availabilityClient = OkHttpClient.Builder()
                    .connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(5, TimeUnit.SECONDS)
                    .build()
                availabilityClient.newCall(req).execute().use { resp ->
                    // 405 Method Not Allowed também indica que o servidor está vivo
                    resp.isSuccessful || resp.code == 405
                }
            }
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Envia o contexto bruto para o Worker e devolve o estado parseado.
     *
     * @param context payload v3 (apenas dados brutos).
     * @param localFallback factory chamada em qualquer falha (sem auth, !2xx, parse, timeout).
     * @param decisaoLocalStatus status do `DiagnosticReport.decisao.status.name`
     *   (ok|info|attention|critical|inconclusive). Usado APENAS na normalizacao
     *   de status quando a IA devolve algo invalido — NAO faz parte do payload
     *   enviado a IA. Default "" deixa a IA decidir sozinha.
     */
    suspend fun explainDiagnosis(
        context: DiagnosisAiContext,
        decisaoLocalStatus: String = "",
        localFallback: () -> AiDiagnosisResult,
    ): AiDiagnosisState {
        if (!isAuthorized()) return AiDiagnosisState.fallback(localFallback())

        val key = cacheKey(context)
        cache[key]?.let { (result, timestamp) ->
            if (clock() - timestamp > CACHE_TTL_MS) {
                cache.remove(key)
            } else {
                return AiDiagnosisState.success(result.copy(source = "cache"))
            }
        }

        return withContext(Dispatchers.IO) {
            val result = withTimeoutOrNull(40_000L) {
                try {
                    val url = baseUrl.trimEnd('/') + "/api/ai/diagnostico-conexao"
                    val json = contextToJson(context).toString()
                    val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
                    val req =
                        Request.Builder()
                            .url(url)
                            .post(body)
                            .build()

                    client.newCall(req).execute().use { resp ->
                        if (!resp.isSuccessful) {
                            val errorBody = resp.body?.string()?.take(300) ?: "(vazio)"
                            Timber.w("Worker HTTP ${resp.code} — body: $errorBody — ativando fallback local")
                            return@use AiDiagnosisState.fallback(localFallback())
                        }
                        val txt = resp.body?.string()
                        if (txt.isNullOrBlank()) {
                            Timber.w("Worker retornou body vazio — ativando fallback local")
                            return@use AiDiagnosisState.fallback(localFallback())
                        }
                        val parsed = parseResult(txt)
                        if (parsed == null) {
                            Timber.w("Falha ao parsear JSON do Worker — ativando fallback local. Body: ${txt.take(200)}")
                            return@use AiDiagnosisState.fallback(localFallback())
                        }
                        val normalized = parsed.copy(
                            status = normalizeStatus(
                                aiStatus = parsed.status,
                                decisaoStatus = decisaoLocalStatus,
                                problemaPrincipalTipo = parsed.problemaPrincipal.tipo,
                                hasSpeedtestData = context.metricasAtuais?.downloadMbps != null,
                            ),
                        )
                        Timber.d("IA respondeu com sucesso: status=${normalized.status} modelo=${normalized.modeloIa.nomeExibicao}")
                        cache[key] = Pair(normalized, clock())
                        AiDiagnosisState.success(normalized)
                    }
                } catch (t: Throwable) {
                    Timber.e("explainDiagnosis falhou: ${t::class.simpleName} — ${t.message}")
                    AiDiagnosisState.fallback(localFallback())
                }
            }
            result ?: AiDiagnosisState.timeout
        }
    }

    // --------------------------------------------------------------------------
    // Stream SSE — emite tokens conforme chegam do Worker.
    // Fallback silencioso se Content-Type nao for text/event-stream.
    // call.cancel() no finally garante cancelamento ao sair da tela.
    // --------------------------------------------------------------------------
    fun explainDiagnosisStream(context: DiagnosisAiContext): Flow<String> = flow {
        if (!isAuthorized()) return@flow

        lastStreamUsage = null

        val url = baseUrl.trimEnd('/') + "/api/ai/diagnostico-conexao?stream=true"
        val json = contextToJson(context).toString()
        val body = json.toRequestBody("application/json; charset=utf-8".toMediaType())
        val req = Request.Builder().url(url).post(body).build()

        val call = client.newCall(req)
        try {
            val resp = call.execute()
            if (!resp.isSuccessful) return@flow
            val contentType = resp.header("Content-Type") ?: ""
            if (!contentType.contains("text/event-stream")) return@flow // fallback silencioso

            val source = resp.body?.source() ?: return@flow
            while (!source.exhausted()) {
                val line = source.readUtf8Line() ?: break
                if (line.startsWith("data: ")) {
                    val data = line.removePrefix("data: ").trim()
                    if (data == "[DONE]") break
                    try {
                        val obj = JSONObject(data)
                        val token = obj.optString("response", "")
                        if (token.isNotEmpty()) emit(token)
                        // Captura uso de tokens quando o worker envia o bloco de usage
                        // (tipicamente no último evento SSE antes de [DONE]).
                        val usageObj = obj.optJSONObject("usage")
                        if (usageObj != null) {
                            val prompt = usageObj.optInt("prompt_tokens", 0)
                            val completion = usageObj.optInt("completion_tokens", 0)
                            val total = usageObj.optInt("total_tokens", prompt + completion)
                            if (total > 0) {
                                lastStreamUsage = AiStreamUsage(prompt, completion, total)
                            }
                        }
                    } catch (_: Exception) { /* evento malformado — ignorar */ }
                }
            }
        } finally {
            call.cancel()
        }
    }.flowOn(Dispatchers.IO)

    // --------------------------------------------------------------------------
    // Parser tolerante: aceita schema v1 e v2.
    // --------------------------------------------------------------------------
    internal fun parseResult(txt: String): AiDiagnosisResult? {
        return try {
            val o = JSONObject(txt)

            val schemaVersion = o.optString("schemaVersion", "1")

            // Bloco modeloIa — pode nao existir em respostas v1.
            val modeloIa = parseModeloIa(o.optJSONObject("modeloIa"))

            // problemaPrincipal pode vir nulo em respostas malformadas.
            val ppObj = o.optJSONObject("problemaPrincipal")
            val problemaPrincipal = AiProblemaPrincipal(
                tipo = ppObj?.optString("tipo", "desconhecido") ?: "desconhecido",
                descricao = ppObj?.optString("descricao", "") ?: "",
                confianca = ppObj?.optDouble("confianca", 0.5) ?: 0.5,
            )

            val impactoObj = o.optJSONObject("impacto")
            val impacto = AiImpacto(
                navegacao = impactoObj?.optString("navegacao", "") ?: "",
                streaming = impactoObj?.optString("streaming", "") ?: "",
                videochamada = impactoObj?.optString("videochamada", "") ?: "",
                jogos = impactoObj?.optString("jogos", "") ?: "",
                trabalho = impactoObj?.optString("trabalho", "") ?: "",
            )

            val acoes = parseAcoes(o.optJSONArray("acoesRecomendadas"))
            val evidencias = parseEvidencias(o.optJSONArray("evidencias"))
            val limites = parseStringArray(o.optJSONArray("limitesDaAnalise"))

            val classificacao = parseClassificacaoTecnica(o.optJSONObject("classificacaoTecnica"))
            val hipoteses = parseHipoteses(o.optJSONArray("hipotesesDescartadas"))
            val perguntas = parsePerguntas(o.optJSONArray("perguntasContextuais"))

            // Bloco "usage" do Cloudflare Workers AI — ausente em fallback local ou worker antigo.
            val usageObj = o.optJSONObject("usage")
            val promptTokens = usageObj?.optInt("prompt_tokens", 0) ?: 0
            val completionTokens = usageObj?.optInt("completion_tokens", 0) ?: 0
            val totalTokens = usageObj?.optInt("total_tokens", promptTokens + completionTokens) ?: 0

            AiDiagnosisResult(
                schemaVersion = schemaVersion,
                source = o.optString("source", "cloudflare_ai"),
                generatedAt = o.optLong("generatedAt", System.currentTimeMillis()),
                status = o.optString("status", "inconclusivo"),
                titulo = o.optString("titulo", "Diagnostico"),
                resumo = o.optString("resumo", ""),
                problemaPrincipal = problemaPrincipal,
                impacto = impacto,
                acoesRecomendadas = acoes,
                evidencias = evidencias,
                textoLaudo = o.optString("textoLaudo", ""),
                limitesDaAnalise = limites,
                modeloIa = modeloIa,
                classificacaoTecnica = classificacao,
                hipotesesDescartadas = hipoteses,
                perguntasContextuais = perguntas,
                promptTokens = promptTokens,
                completionTokens = completionTokens,
                totalTokens = totalTokens,
            )
        } catch (_: Throwable) {
            null
        }
    }

    private fun parseModeloIa(obj: JSONObject?): ModeloIa {
        if (obj == null) return ModeloIa.unknown()
        return ModeloIa(
            idInterno = obj.optString("idInterno", ""),
            provedor = obj.optString("provedor", ""),
            familia = obj.optString("familia", ""),
            versao = obj.optStringOrNull("versao"),
            tamanho = obj.optStringOrNull("tamanho"),
            variante = obj.optStringOrNull("variante"),
            nomeExibicao = obj.optString("nomeExibicao", "SignallQ IA").ifBlank { "SignallQ IA" },
            nomeCompletoComercial = obj.optString("nomeCompletoComercial", "SignallQ IA").ifBlank { "SignallQ IA" },
            descricaoComercial = obj.optString("descricaoComercial", ""),
            textoRodape = obj.optString("textoRodape", "Motor de análise: SignallQ IA")
                .ifBlank { "Motor de análise: SignallQ IA" },
        )
    }

    private fun parseAcoes(arr: JSONArray?): List<AiAcaoRecomendada> {
        if (arr == null) return emptyList()
        val list = mutableListOf<AiAcaoRecomendada>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            list.add(
                AiAcaoRecomendada(
                    titulo = item.optString("titulo", ""),
                    descricao = item.optString("descricao", ""),
                    prioridade = item.optString("prioridade", "media"),
                    tipo = item.optString("tipo", ""),
                    executavelNoApp = item.optBoolean("executavelNoApp", false),
                ),
            )
        }
        return list
    }

    private fun parseEvidencias(arr: JSONArray?): List<AiEvidenceOut> {
        if (arr == null) return emptyList()
        val list = mutableListOf<AiEvidenceOut>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            list.add(
                AiEvidenceOut(
                    label = item.optString("label", ""),
                    valor = item.optString("valor", ""),
                    interpretacao = item.optString("interpretacao", ""),
                ),
            )
        }
        return list
    }

    private fun parseStringArray(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i)
            if (s.isNotEmpty()) list.add(s)
        }
        return list
    }

    private fun parseClassificacaoTecnica(obj: JSONObject?): ClassificacaoTecnica {
        if (obj == null) return ClassificacaoTecnica()
        return ClassificacaoTecnica(
            velocidade = parseClassificacaoItem(obj.optJSONObject("velocidade")),
            estabilidade = parseClassificacaoItem(obj.optJSONObject("estabilidade")),
            wifi = parseClassificacaoItem(obj.optJSONObject("wifi")),
            dns = parseClassificacaoItem(obj.optJSONObject("dns")),
            fibra = parseClassificacaoItem(obj.optJSONObject("fibra")),
        )
    }

    // Le exatamente `avaliacao` e `justificativa`. Campos extras enviados pela IA
    // sao ignorados silenciosamente pelo JSONObject. Se faltar `avaliacao`,
    // devolve null para a dimensao inteira (mantendo o ClassificacaoTecnica
    // com campos nulos onde a IA nao falou nada).
    private fun parseClassificacaoItem(obj: JSONObject?): ClassificacaoItem? {
        if (obj == null) return null
        val avaliacaoRaw = obj.optStringOrNull("avaliacao")
        val justificativaRaw = obj.optStringOrNull("justificativa")
        if (avaliacaoRaw.isNullOrBlank() && justificativaRaw.isNullOrBlank()) return null
        return ClassificacaoItem(
            avaliacao = avaliacaoRaw,
            justificativa = justificativaRaw,
        )
    }

    private fun parseHipoteses(arr: JSONArray?): List<HipoteseDescartada> {
        if (arr == null) return emptyList()
        val list = mutableListOf<HipoteseDescartada>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val hip = item.optString("hipotese", "")
            if (hip.isBlank()) continue
            list.add(
                HipoteseDescartada(
                    hipotese = hip,
                    motivo = item.optString("motivo", ""),
                ),
            )
        }
        return list
    }

    private fun parsePerguntas(arr: JSONArray?): List<PerguntaContextual> {
        if (arr == null) return emptyList()
        val list = mutableListOf<PerguntaContextual>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            val id = item.optString("id", "")
            val pergunta = item.optString("pergunta", "")
            if (pergunta.isBlank()) continue
            val opcoesArr = item.optJSONArray("opcoes")
            val opcoes = mutableListOf<OpcaoPerguntaContextual>()
            if (opcoesArr != null) {
                for (j in 0 until opcoesArr.length()) {
                    val op = opcoesArr.optJSONObject(j) ?: continue
                    val rotulo = op.optString("rotulo", "")
                    if (rotulo.isBlank()) continue
                    opcoes.add(
                        OpcaoPerguntaContextual(
                            id = op.optString("id", ""),
                            rotulo = rotulo,
                        ),
                    )
                }
            }
            val tema = item.optStringOrNull("tema")
            list.add(PerguntaContextual(id = id, pergunta = pergunta, tema = tema, opcoes = opcoes))
        }
        return list
    }

    // --------------------------------------------------------------------------
    // Serializacao do payload enviado ao Worker (schema v3 — APENAS DADOS BRUTOS).
    // Nao envia: classificacao local, decisao local, recomendacoes locais,
    // limites da analise, perfis pre-computados, ou interpretacao em evidencias.
    // A IA recebe so numeros e contexto bruto e faz toda a analise.
    // --------------------------------------------------------------------------
    internal fun contextToJson(ctx: DiagnosisAiContext): JSONObject {
        val o = JSONObject()
        o.put("schemaVersion", ctx.schemaVersion)
        o.put("generatedAtEpochMs", ctx.generatedAtEpochMs)
        o.put("connectionType", ctx.connectionType.name)

        ctx.metricasAtuais?.let { m ->
            val mo = JSONObject()
            mo.putOrNull("downloadMbps", m.downloadMbps)
            mo.putOrNull("uploadMbps", m.uploadMbps)
            mo.putOrNull("latenciaMs", m.latenciaMs)
            mo.putOrNull("jitterMs", m.jitterMs)
            mo.putOrNull("perdaPacotesPercentual", m.perdaPacotesPercentual)
            mo.putOrNull("bufferbloatMs", m.bufferbloatMs)
            m.severidadeBufferbloat?.let { mo.put("severidadeBufferbloat", it) }
            mo.putOrNull("stabilityScore", m.stabilityScore)
            mo.putOrNull("peakDownloadMbps", m.peakDownloadMbps)
            mo.putOrNull("peakUploadMbps", m.peakUploadMbps)
            mo.putOrNull("latencyDownloadMs", m.latencyDownloadMs)
            mo.putOrNull("latencyUploadMs", m.latencyUploadMs)
            m.packetLossSource?.let { mo.put("packetLossSource", it) }
            o.put("metricasAtuais", mo)
        }

        ctx.contextoRede?.let { cr ->
            val co = JSONObject()
            cr.tipoConexao?.let { co.put("tipoConexao", it) }
            cr.ssid?.let { co.put("ssid", it) }
            cr.bssid?.let { co.put("bssid", it) }
            cr.rssi?.let { co.put("rssi", it) }
            cr.bandaWifi?.let { co.put("bandaWifi", it) }
            cr.canal?.let { co.put("canal", it) }
            cr.larguraCanalMhz?.let { co.put("larguraCanalMhz", it) }
            cr.frequenciaMhz?.let { co.put("frequenciaMhz", it) }
            cr.linkSpeedMbps?.let { co.put("linkSpeedMbps", it) }
            cr.padraoWifi?.let { co.put("padraoWifi", it) }
            cr.seguranca?.let { co.put("seguranca", it) }
            cr.privateDnsAtivo?.let { co.put("privateDnsAtivo", it) }
            cr.privateDnsHostname?.let { co.put("privateDnsHostname", it) }
            if (cr.redesProximas.isNotEmpty()) {
                val arr = JSONArray()
                cr.redesProximas.forEach { rv ->
                    val rvObj = JSONObject()
                    rv.ssid?.let { rvObj.put("ssid", it) }
                    rv.bssid?.let { rvObj.put("bssid", it) }
                    rv.rssiDbm?.let { rvObj.put("rssiDbm", it) }
                    rv.frequenciaMhz?.let { rvObj.put("frequenciaMhz", it) }
                    rv.canal?.let { rvObj.put("canal", it) }
                    rv.seguranca?.let { rvObj.put("seguranca", it) }
                    arr.put(rvObj)
                }
                co.put("redesProximas", arr)
            }
            o.put("contextoRede", co)
        }

        ctx.rede?.let { r ->
            val ro = JSONObject()
            r.operadora?.let { ro.put("operadora", it) }
            r.asn?.let { ro.put("asn", it) }
            r.ipPublico?.let { ro.put("ipPublico", it) }
            r.ipLocal?.let { ro.put("ipLocal", it) }
            r.pais?.let { ro.put("pais", it) }
            r.regiao?.let { ro.put("regiao", it) }
            r.dnsResolverIp?.let { ro.put("dnsResolverIp", it) }
            r.dnsResolverProvider?.let { ro.put("dnsResolverProvider", it) }
            r.dnsLatenciaMs?.let { ro.put("dnsLatenciaMs", it) }
            r.gatewayIp?.let { ro.put("gatewayIp", it) }
            r.servidorTesteCidade?.let { ro.put("servidorTesteCidade", it) }
            o.put("rede", ro)
        }

        ctx.movel?.let { m ->
            val mo = JSONObject()
            m.operadora?.let { mo.put("operadora", it) }
            m.tecnologia?.let { mo.put("tecnologia", it) }
            m.rsrpDbm?.let { mo.put("rsrpDbm", it) }
            m.rsrqDb?.let { mo.put("rsrqDb", it) }
            m.sinrDb?.let { mo.put("sinrDb", it) }
            m.ecnoDb?.let { mo.put("ecnoDb", it) }
            m.bandaMovel?.let { mo.put("bandaMovel", it) }
            m.cellId?.let { mo.put("cellId", it) }
            m.mcc?.let { mo.put("mcc", it) }
            m.mnc?.let { mo.put("mnc", it) }
            m.tac?.let { mo.put("tac", it) }
            m.roaming?.let { mo.put("roaming", it) }
            o.put("movel", mo)
        }

        ctx.dispositivos?.let { d ->
            val dout = JSONObject()
            d.fabricante?.let { dout.put("fabricante", it) }
            d.modelo?.let { dout.put("modelo", it) }
            d.sistema?.let { dout.put("sistema", it) }
            d.versaoSO?.let { dout.put("versaoSO", it) }
            d.quantidadeNaRede?.let { dout.put("quantidadeNaRede", it) }
            o.put("dispositivos", dout)
        }

        ctx.historico?.let { h ->
            val ho = JSONObject()
            h.media7d?.let { ho.put("media7d", historicoMediaToJson(it)) }
            h.media30d?.let { ho.put("media30d", historicoMediaToJson(it)) }
            if (h.ultimosTestes.isNotEmpty()) {
                val arr = JSONArray()
                h.ultimosTestes.forEach { t ->
                    val obj = JSONObject()
                    obj.put("timestampEpochMs", t.timestampEpochMs)
                    t.downloadMbps?.let { obj.put("downloadMbps", it) }
                    t.uploadMbps?.let { obj.put("uploadMbps", it) }
                    t.latenciaMs?.let { obj.put("latenciaMs", it) }
                    t.jitterMs?.let { obj.put("jitterMs", it) }
                    t.perdaPercentual?.let { obj.put("perdaPercentual", it) }
                    t.connectionType?.let { obj.put("connectionType", it) }
                    arr.put(obj)
                }
                ho.put("ultimosTestes", arr)
            }
            o.put("historico", ho)
        }

        val evArr = JSONArray()
        ctx.evidencias.forEach { e ->
            val eo = JSONObject()
            eo.put("label", e.label)
            eo.put("valor", e.valor)
            // Sem `interpretacao` — payload v3 e bruto.
            evArr.put(eo)
        }
        o.put("evidencias", evArr)

        ctx.feedbackUsuario?.let { o.put("feedbackUsuario", it.take(500)) }

        ctx.instrucaoTom?.let { o.put("instrucaoTom", it) }

        return o
    }

    private fun historicoMediaToJson(m: AiHistoricoMedia): JSONObject {
        val o = JSONObject()
        o.putOrNull("downloadMbps", m.downloadMbps)
        o.putOrNull("uploadMbps", m.uploadMbps)
        o.putOrNull("pingMs", m.pingMs)
        m.testes?.let { o.put("testes", it) }
        return o
    }

    // --------------------------------------------------------------------------
    // normalizeStatus: cruza o status da IA com o status local SEM colapsar
    // problemas distintos.
    //
    // Regras:
    //  - Se o status da IA esta no conjunto valido, mantemos o que a IA disse.
    //  - Se o problema principal indica claramente "estabilidade" ou "wifi",
    //    nunca rebaixamos para "ruim" generico via decisao local — a
    //    granularidade da IA prevalece.
    //  - O fallback so e usado quando o status da IA e invalido.
    // --------------------------------------------------------------------------
    internal fun normalizeStatus(
        aiStatus: String,
        decisaoStatus: String,
        problemaPrincipalTipo: String = "",
        hasSpeedtestData: Boolean = false,
    ): String {
        val valid = setOf("excelente", "bom", "regular", "ruim", "critico", "inconclusivo")
        val normalizedAi = aiStatus.lowercase()
        if (normalizedAi in valid) {
            // Enforcement da rule 13: se a IA retornou "inconclusivo" mas há dados
            // de speedtest (downloadMbps presente), a IA violou a regra — sobrescrever
            // para "regular" como diagnóstico mínimo honesto. "inconclusivo" com dados
            // reais é mentira para o usuário.
            if (normalizedAi == "inconclusivo" && hasSpeedtestData) return "regular"
            return normalizedAi
        }

        // Se a IA falhou em status mas conseguiu identificar problema especifico,
        // mapeamos para um status razoavel sem colapsar a categoria.
        if (problemaPrincipalTipo.lowercase() in
            setOf("estabilidade", "velocidade", "wifi", "dns", "isp", "fibra", "roteador", "dispositivo", "historico")
        ) {
            return when (decisaoStatus.lowercase()) {
                "ok", "info" -> "regular"
                "attention" -> "regular"
                "critical" -> "ruim"
                else -> "regular"
            }
        }

        return when (decisaoStatus.lowercase()) {
            "ok", "info" -> "bom"
            "attention" -> "regular"
            "critical" -> "critico"
            else -> "inconclusivo"
        }
    }

    // --------------------------------------------------------------------------
    // Cache key: SHA-256(prompt_version + context.toString()).
    // O AI_PROMPT_VERSION garante que respostas geradas com o prompt antigo
    // (schema v1) nao sejam servidas para um cliente esperando schema v2.
    // --------------------------------------------------------------------------
    internal fun cacheKey(ctx: DiagnosisAiContext): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(AI_PROMPT_VERSION.toByteArray(Charsets.UTF_8))
        md.update(ctx.toString().toByteArray(Charsets.UTF_8))
        return md.digest().joinToString("") { "%02x".format(it) }
    }
}

// --- helpers JSON ---------------------------------------------------------

private fun JSONObject.putOrNull(name: String, value: Double?) {
    if (value == null) put(name, JSONObject.NULL) else put(name, value)
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    val s = optString(name, "")
    return if (s.isBlank()) null else s
}
