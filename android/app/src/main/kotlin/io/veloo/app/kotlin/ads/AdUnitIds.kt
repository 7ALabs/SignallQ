package io.signallq.app.ads

/**
 * Ad Unit IDs de anuncio nativo (AdMob) -- issue #555.
 *
 * BLOQUEIO EXTERNO (nao e tarefa de codigo): o Luiz ainda precisa criar o app real no
 * console AdMob e gerar os Ad Unit IDs de producao. Ate la, usamos os IDs de teste
 * publicos do Google (https://developers.google.com/admob/android/test-ads) -- servem
 * anuncio de amostra real (nao clique fantasma, nao view em branco) sem gerar
 * impressao/clique faturavel nem risco de suspensao de conta por trafego invalido.
 *
 * NUNCA usar estes IDs de teste em build de release publicado na Play Store, e NUNCA
 * clicar repetidamente em anuncio de producao durante teste manual (risco de banimento
 * da conta AdMob por trafego invalido).
 */
object AdUnitIds {
    /** ID de app AdMob de teste (usado no AndroidManifest ate existir conta real). */
    const val APPLICATION_ID_TESTE = "ca-app-pub-3940256099942544~3347511713"

    /** Ad Unit ID de teste para anuncio nativo avancado (Native Advanced). */
    const val NATIVE_TESTE = "ca-app-pub-3940256099942544/2247696110"

    /**
     * Resolve o Ad Unit ID de anuncio nativo por slot. Por ora todas as 4 telas usam
     * o mesmo Ad Unit de teste -- quando o Luiz criar a conta real, cada tela pode
     * ganhar seu proprio Ad Unit ID de producao (granularidade de relatorio por tela),
     * substituindo aqui sem tocar nas telas que consomem este objeto.
     */
    fun para(slot: AdSlot): String =
        when (slot) {
            AdSlot.VELOCIDADE, AdSlot.RESULTADO, AdSlot.DISPOSITIVOS, AdSlot.HISTORICO -> NATIVE_TESTE
        }
}
