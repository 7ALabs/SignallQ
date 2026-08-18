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
 * O que decidiu foi a medicao do ganho. `setContentUrl` e `setNeighboringContentUrls` valem
 * pelo conteudo que o Google encontra na URL, e estas URLs sao sinteticas -- alem disso,
 * `signallq.app` **nao e um dominio registrado** (NXDOMAIN em dois resolvedores publicos,
 * verificado em 2026-08-17; o dominio do projeto e `signallq.com`). Nao ha pagina para
 * rastrear, entao o mecanismo esta inerte hoje. Remover os marcadores nao custou receita
 * mensuravel, e tirou do texto publico a parte mais dificil de defender.
 *
 * O `BASE` apontar para dominio inexistente e problema proprio, registrado em issue: e o que
 * precisa ser corrigido ANTES de qualquer discussao sobre valor de sinal contextual.
 */
data class NativeAdContentSignal(
    val contentUrl: String,
)

object NativeAdContentSignals {
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
     * Nao ha parametro de diagnostico, e a ausencia e o ponto -- ver o KDoc do arquivo. Quem
     * precisar reintroduzir contexto por diagnostico tem que passar pela politica de privacidade
     * primeiro, porque a versao publicada declara que apenas o topico da tela e enviado.
     */
    fun forSlot(slot: AdSlot): NativeAdContentSignal =
        NativeAdContentSignal(contentUrl = "$BASE/${topicoPorSlot.getValue(slot)}")
}
