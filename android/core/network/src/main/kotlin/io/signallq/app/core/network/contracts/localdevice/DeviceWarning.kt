package io.signallq.app.core.network.contracts.localdevice

/** Motivo de um [DeviceWarning] — nunca um erro genérico de exceção crua. */
enum class DeviceWarningType {
    /** Tentativa de login no equipamento falhou; snapshot segue sem enriquecimento. */
    LOGIN_FALHOU,

    /** Leitura funcionou, mas parte dos campos esperados não veio preenchida. */
    DADOS_PARCIAIS,

    /** Seção pedida não é suportada por este [DeviceType]/driver (ex: fibra em roteador). */
    CAMPO_NAO_SUPORTADO,

    /** Falha de comunicação com o equipamento (timeout, rede, resposta inválida). */
    ERRO_COMUNICACAO,
}

/**
 * Representa ausência ou limitação de dado de forma explícita, para a UI
 * traduzir em mensagem amigável — nunca um erro genérico/stacktrace exposto.
 *
 * [mensagem] deve ser amigável ao usuário final (mesma regra de
 * [io.signallq.app.core.network.contracts.gateway.GatewayConnectionResultado.Falha]).
 */
data class DeviceWarning(
    val type: DeviceWarningType,
    val mensagem: String,
)
