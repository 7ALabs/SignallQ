package io.signallq.app.ads

/**
 * Sinal de contexto enviado ao AdMob via [com.google.android.gms.ads.AdRequest.Builder]
 * (`setContentUrl` / `setNeighboringContentUrls`) -- issue #555, passo 3 do plano.
 *
 * Deliberadamente NAO usa a API de `keywords`/`addKeyword()`: foi removida das versoes
 * atuais do Google Mobile Ads SDK para Android. `setContentUrl`/`setNeighboringContentUrls`
 * e o mecanismo real e vigente de contextual targeting -- e o unico usado aqui.
 *
 * As URLs sao sinteticas (nao apontam para paginas reais, so classificam o topico) e
 * construidas so a partir de: (a) o [AdSlot] atual -- fixo por tela, sem dado de usuario;
 * (b) ids de [io.signallq.app.core.recommendation.DiagnosticTag] -- vocabulario fechado
 * de tags de diagnostico (ex.: "wifi_fraco", "bufferbloat_alto"), nunca SSID/BSSID/IP/
 * device id. [sanitizarTag] reforca isso descartando qualquer valor fora do padrao
 * alfanumerico+hifen esperado desse vocabulario, blindando contra qualquer regressao
 * futura que tente enfiar dado bruto aqui.
 */
data class NativeAdContentSignal(
    val contentUrl: String,
    val neighboringContentUrls: List<String> = emptyList(),
)

object NativeAdContentSignals {
    private const val BASE = "https://signallq.app/contexto-anuncio"
    private const val MAX_TAGS = 3

    private val topicoPorSlot =
        mapOf(
            AdSlot.VELOCIDADE to "velocidade",
            AdSlot.RESULTADO to "resultado-teste",
            AdSlot.DISPOSITIVOS to "dispositivos-rede",
            AdSlot.HISTORICO to "historico-conectividade",
            AdSlot.JOGOS to "jogos-resultado",
        )

    /**
     * @param diagnosticTagIds ids de [io.signallq.app.core.recommendation.DiagnosticTag]
     *   (ex.: `decision.matchedTags.map { it.id }.toSet()`). Vazio nas telas sem diagnostico
     *   ativo (Dispositivos/Historico/Velocidade idle) -- o slot sozinho ja da contexto.
     */
    fun forSlot(
        slot: AdSlot,
        diagnosticTagIds: Set<String> = emptySet(),
    ): NativeAdContentSignal {
        val topico = topicoPorSlot.getValue(slot)
        val vizinhos =
            diagnosticTagIds
                .mapNotNull(::sanitizarTag)
                .take(MAX_TAGS)
                .map { "$BASE/$topico/$it" }
        return NativeAdContentSignal(
            contentUrl = "$BASE/$topico",
            neighboringContentUrls = vizinhos,
        )
    }

    /** Normaliza `_` para `-` (vocabulario real de [io.signallq.app.core.recommendation.DiagnosticTag]
     *  usa underscore, ex. "wifi_fraco") e so aceita `[a-z0-9-]` depois disso -- descarta
     *  qualquer coisa fora desse formato (defesa em profundidade: mesmo que um chamador
     *  futuro passe dado que nao devia, como SSID/IP/MAC com `:`/`.`/espaco). */
    private fun sanitizarTag(tag: String): String? {
        val normalizado = tag.trim().lowercase().replace('_', '-')
        if (normalizado.isEmpty()) return null
        if (!normalizado.matches(Regex("^[a-z0-9-]+$"))) return null
        return normalizado
    }
}
