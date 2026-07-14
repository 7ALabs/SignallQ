package io.signallq.app.ads

/**
 * As telas com slot de anuncio nativo AdMob -- issue #555 (Velocidade, Resultado do
 * diagnostico, Dispositivos, Historico) + JOGOS (spec To-Be 5g, `design-tobe-alinhamento`):
 * unica superfície de anuncio no fluxo de Jogos, so no passo 5 (Resultado), logo abaixo
 * das recomendacoes. Cada slot tem sua propria chave de Remote Config, alem da chave
 * mestra que liga/desliga tudo de uma vez.
 */
enum class AdSlot {
    VELOCIDADE,
    RESULTADO,
    DISPOSITIVOS,
    HISTORICO,
    JOGOS,
}
