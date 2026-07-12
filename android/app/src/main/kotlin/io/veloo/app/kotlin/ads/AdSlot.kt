package io.signallq.app.ads

/**
 * As 4 telas com slot de anuncio nativo AdMob -- issue #555.
 *
 * Ordem de prioridade definida na spec: Velocidade, Resultado do diagnostico,
 * Dispositivos, Historico. Cada slot tem sua propria chave de Remote Config,
 * alem da chave mestra que liga/desliga tudo de uma vez.
 */
enum class AdSlot {
    VELOCIDADE,
    RESULTADO,
    DISPOSITIVOS,
    HISTORICO,
}
