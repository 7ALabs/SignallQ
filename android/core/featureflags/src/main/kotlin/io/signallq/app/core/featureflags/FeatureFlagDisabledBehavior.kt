package io.signallq.app.core.featureflags

/**
 * Comportamento declarado quando a flag esta desativada (campo `disabledBehavior`
 * do schema, Epico #1347). Cobre os padroes descritos na secao "Regras de
 * comportamento Android" do Epico -- nao exaustivo por design: F4 (instrumentacao
 * dos modulos feature) pode adicionar um novo valor quando um comportamento real
 * ainda nao coberto aqui aparecer, mas sempre um valor explicito, nunca inferido
 * por convencao de nome de flag.
 */
enum class FeatureFlagDisabledBehavior {
    /** Remove/desabilita entrada de navegacao e bloqueia deep link/rota direta. */
    HIDE_ENTRY_AND_BLOCK_ROUTE,

    /** Mantem a entrada visivel mas mostra [FeatureFlagDefinition.disabledMessage] ao tentar usar. */
    SHOW_DISABLED_MESSAGE,

    /** Kill switch silencioso -- interrompe a execucao/chamada sem alterar UI nem exibir mensagem. */
    SILENT_NO_OP,

    /** Troca para um modo alternativo/degradado em vez de bloquear por completo. */
    FALLBACK_MODE,
}
