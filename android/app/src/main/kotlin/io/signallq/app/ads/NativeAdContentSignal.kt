package io.signallq.app.ads

/**
 * Sinal de contexto enviado ao AdMob via [com.google.android.gms.ads.AdRequest.Builder]
 * (`setContentUrl`) -- issue #555, passo 3 do plano; reduzido pela #1703/#1717.
 *
 * Deliberadamente NAO usa a API de `keywords`/`addKeyword()`: foi removida das versoes
 * atuais do Google Mobile Ads SDK para Android. `setContentUrl` e o mecanismo real e vigente
 * de contextual targeting -- e o unico usado aqui.
 *
 * ## O que saiu, e por que (GH#1717, decisao de Luiz em 2026-08-17)
 *
 * Ate esta issue o sinal tambem carregava ate 3 ids de `DiagnosticTag` em
 * `setNeighboringContentUrls` -- isto e, a CONCLUSAO do diagnostico da pessoa
 * (ex.: `velocidade_abaixo_do_contratado`) saia do aparelho para o Google. A politica de
 * privacidade publicada precisava declarar isso, e declarar era o problema: o ganho nao
 * justificava a frase.
 *
 * O que decidiu foi a medicao do ganho. `setContentUrl` vale pelo conteudo que o Google
 * encontra na URL, e estas URLs sao sinteticas -- alem disso, `signallq.app` **nao e um
 * dominio registrado** (NXDOMAIN em dois resolvedores publicos, verificado em 2026-08-17; o
 * dominio do projeto e `signallq.com`). Nao ha pagina para rastrear, entao o mecanismo esta
 * inerte hoje. Ver a issue #1726 -- e o que precisa ser corrigido ANTES de qualquer discussao
 * sobre valor de sinal contextual.
 *
 * ## Construtor privado, e o motivo e o texto publicado
 *
 * A primeira versao desta reducao trazia um KDoc afirmando que "nao existe caminho por onde
 * dado de usuario entre -- nao porque e filtrado, mas porque nao existe parametro para ele".
 * Era falso: o `data class` tinha construtor publico, e Caio provou construindo o sinal a mao
 * num call site, com SSID e IP na URL -- suite inteira verde (bloqueio B8 da PR #1717).
 *
 * O construtor agora e privado e [forSlot] e a unica entrada. A garantia deixou de ser
 * convencao de call site e virou erro de compilacao, que e o que uma politica publicada exige:
 * ela declara que apenas o topico da tela e enviado, e agora o codigo nao permite outra coisa.
 *
 * Nao e `data class` de proposito -- `copy()` reabriria o construtor. Quem precisar comparar
 * dois sinais compara [contentUrl].
 */
class NativeAdContentSignal private constructor(
    val contentUrl: String,
) {
    /**
     * O sinal é uma chave de carregamento, não identidade de composição. As telas
     * recriam [NativeAdContentSignal] durante recomposições; comparar pelo URL
     * impede que cada frame cancele e reinicie a mesma solicitação de anúncio.
     */
    override fun equals(other: Any?): Boolean =
        other is NativeAdContentSignal && contentUrl == other.contentUrl

    override fun hashCode(): Int = contentUrl.hashCode()

    companion object {
        private const val BASE = "https://signallq.app/contexto-anuncio"

        private val topicoPorSlot =
            mapOf(
                AdSlot.VELOCIDADE to "velocidade",
                AdSlot.RESULTADO to "resultado-teste",
                AdSlot.DISPOSITIVOS to "dispositivos-rede",
                AdSlot.HISTORICO to "historico-conectividade",
                AdSlot.JOGOS to "jogos-resultado",
            )

        /**
         * O unico sinal enviado: o topico do [slot], fixo por tela e sem nenhum dado do usuario.
         *
         * Nao ha parametro de diagnostico, e a ausencia e o ponto -- ver o KDoc da classe.
         */
        fun forSlot(slot: AdSlot): NativeAdContentSignal =
            NativeAdContentSignal(contentUrl = "$BASE/${topicoPorSlot.getValue(slot)}")
    }
}
