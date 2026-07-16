package io.signallq.app.feature.diagnostico.remote

import io.signallq.app.feature.diagnostico.DiagnosticReport
import io.signallq.app.feature.diagnostico.DiagnosticResult
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.diagnostico.EvidenceScore
import io.signallq.app.feature.diagnostico.GameReadinessClassifier
import io.signallq.app.feature.diagnostico.Provenance
import io.signallq.app.feature.diagnostico.ScoreResult
import io.signallq.app.feature.diagnostico.UsageProfileClassifier
import org.json.JSONArray
import org.json.JSONObject

/**
 * Converte o `DiagnosticReportPayload` (JSON devolvido por `POST /diagnostic/evaluate`
 * do worker `signallq-diagnostic`) para o [DiagnosticReport] local (GH#962).
 *
 * ## O que mapeia 1:1
 * Os buckets de [DiagnosticResult] (`wifiResultados`..`redeResultados`, `decisao`,
 * `achadosSecundarios`, `hipotesesDescartadas`, `dadosAusentes`,
 * `limitacoesEquipamentoLocal`, `recomendacoes`) tem o MESMO shape nos dois lados —
 * o worker foi desenhado para paridade com o Android (ver PR #964) — entao o
 * mapeamento e campo-a-campo, sem fabricar nada.
 *
 * ## O que NAO mapeia 1:1 (decisao documentada, GH#962)
 * - **`scoreEngineResultado`**: o worker expoe `dimensoes: [{id, score}]` com ids
 *   simplificados para exibicao ("internet"/"wifi"/"dns"/"fibra"/"mobile"/"historico"),
 *   DIFERENTES da taxonomia interna do [io.signallq.app.feature.diagnostico.ScoreEngine]
 *   local ("estabilidade"/"wifiRedeLocal"/"velocidade"/...). Reconstruimos
 *   [EvidenceScore] com esses ids remotos e [Provenance.medida] (dado real, so que
 *   com granularidade menor) — sao informativos, nao devem ser recombinados pelo
 *   [io.signallq.app.feature.diagnostico.ScoreEngine] local (o `score` final ja vem
 *   pronto do worker, nao e recalculado no app).
 * - **`perfisUso` e `gameReadiness`**: o worker calcula versoes simplificadas
 *   (`perfisUso` deriva so do score geral; `gameReadiness` usa 4 perfis de catalogo
 *   remoto — COMPETITIVE_EXTREME/COMPETITIVE/SPORTS_COMPETITIVE/MULTIPLAYER_MODERATE —
 *   que NAO correspondem as 3 categorias locais FPS_COMPETITIVO/CLOUD_GAMING/
 *   MOBILE_COMPETITIVO do [GameReadinessClassifier]). Forcar essa correspondencia
 *   inventaria dado. Por isso este mapper NAO preenche esses dois campos — a
 *   [RemoteDiagnosticRepository] sempre os calcula localmente via
 *   [UsageProfileClassifier]/[GameReadinessClassifier] a partir do mesmo
 *   `DiagnosticInput` usado para montar o snapshot remoto, regardless da fonte do
 *   restante do relatorio. Essas duas classificacoes sao puras/deterministicas e nao
 *   se beneficiam de vir do servidor.
 */
internal object RemoteDiagnosticReportMapper {

    fun toDiagnosticReport(payload: JSONObject, geradoEmMs: Long): DiagnosticReport {
        val decisaoJson = payload.optJSONObject("decisao")
            ?: error("payload remoto sem campo 'decisao' obrigatorio")

        return DiagnosticReport(
            wifiResultados = resultsFrom(payload.optJSONArray("wifiResultados")),
            internetResultados = resultsFrom(payload.optJSONArray("internetResultados")),
            mobileResultados = resultsFrom(payload.optJSONArray("mobileResultados")),
            fibraResultados = resultsFrom(payload.optJSONArray("fibraResultados")),
            dnsResultados = resultsFrom(payload.optJSONArray("dnsResultados")),
            historicoResultados = resultsFrom(payload.optJSONArray("historicoResultados")),
            wifiCanalResultados = resultsFrom(payload.optJSONArray("wifiCanalResultados")),
            redeResultados = resultsFrom(payload.optJSONArray("redeResultados")),
            decisao = resultFrom(decisaoJson),
            achadosSecundarios = resultsFrom(payload.optJSONArray("achadosSecundarios")),
            hipotesesDescartadas = resultsFrom(payload.optJSONArray("hipotesesDescartadas")),
            dadosAusentes = stringListFrom(payload.optJSONArray("dadosAusentes")),
            limitacoesEquipamentoLocal = stringListFrom(payload.optJSONArray("limitacoesEquipamentoLocal")),
            recomendacoes = resultsFrom(payload.optJSONArray("recomendacoes")),
            scoreEngineResultado = scoreResultFrom(payload.optJSONObject("scoreEngineResultado"), payload),
            // perfisUso e gameReadiness: sempre calculados localmente pelo caller
            // (RemoteDiagnosticRepository) — ver kdoc da classe. Vazios aqui de
            // proposito, nunca preenchidos com dado remoto fabricado.
            perfisUso = emptyList(),
            gameReadiness = emptyList(),
            geradoEmMs = geradoEmMs,
        )
    }

    private fun statusFrom(raw: String?): DiagnosticStatus = when (raw) {
        "ok" -> DiagnosticStatus.ok
        "info" -> DiagnosticStatus.info
        "attention" -> DiagnosticStatus.attention
        "critical" -> DiagnosticStatus.critical
        else -> DiagnosticStatus.inconclusive
    }

    private fun resultFrom(o: JSONObject): DiagnosticResult = DiagnosticResult(
        id = o.optString("id", ""),
        titulo = o.optString("titulo", ""),
        status = statusFrom(o.optStringOrNull("status")),
        evidencia = o.optStringOrNull("evidencia"),
        mensagemUsuario = o.optString("mensagemUsuario", ""),
        recomendacao = o.optStringOrNull("recomendacao"),
        categoria = o.optString("categoria", ""),
        podeConcluir = o.optBoolean("podeConcluir", false),
        categoriaOrigem = o.optStringOrNull("categoriaOrigem"),
    )

    private fun resultsFrom(arr: JSONArray?): List<DiagnosticResult> {
        if (arr == null) return emptyList()
        val list = mutableListOf<DiagnosticResult>()
        for (i in 0 until arr.length()) {
            val item = arr.optJSONObject(i) ?: continue
            list.add(resultFrom(item))
        }
        return list
    }

    private fun stringListFrom(arr: JSONArray?): List<String> {
        if (arr == null) return emptyList()
        val list = mutableListOf<String>()
        for (i in 0 until arr.length()) {
            val s = arr.optString(i, "")
            if (s.isNotEmpty()) list.add(s)
        }
        return list
    }

    /** Ver kdoc da classe — dimensoes remotas nao usam a taxonomia local, mas o
     *  score final e o `dadosAusentes` (aproximado pelo do relatorio) sao dado
     *  real, nao fabricado. */
    private fun scoreResultFrom(scoreJson: JSONObject?, reportJson: JSONObject): ScoreResult? {
        if (scoreJson == null) return null
        val score = if (scoreJson.has("score") && !scoreJson.isNull("score")) scoreJson.optInt("score") else null
        val dimensoesJson = scoreJson.optJSONArray("dimensoes")
        val dimensoes = mutableListOf<EvidenceScore>()
        if (dimensoesJson != null) {
            for (i in 0 until dimensoesJson.length()) {
                val d = dimensoesJson.optJSONObject(i) ?: continue
                val id = d.optString("id", "")
                if (id.isEmpty()) continue
                dimensoes.add(EvidenceScore(dimensao = id, nota = d.optInt("score"), provenance = Provenance.medida))
            }
        }
        return ScoreResult(
            score = score,
            dimensoesUsadas = dimensoes,
            dadosAusentes = stringListFrom(reportJson.optJSONArray("dadosAusentes")),
        )
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    if (!has(name) || isNull(name)) return null
    val s = optString(name, "")
    return s.ifBlank { null }
}
