package io.signallq.app.core.network.contracts.oui

/**
 * Nível de confiança na curadoria de uma [OuiEntry] do catálogo.
 *
 * Contrato da Fase 1 do plano de unificação de topologia (issue #975/#978). Hoje o catálogo
 * inteiro é curado manualmente (sem confirmação de campo automatizada) — [CONFIRMADO_EM_CAMPO]
 * fica reservado para quando uma entrada for validada contra leitura direta de equipamento real
 * (ex.: [io.signallq.app.core.network.contracts.localdevice.ClientSnapshot] de um driver
 * validado em laboratório), não usado ainda nesta fase.
 */
enum class NivelValidacaoOui {
    CURADO_MANUALMENTE,
    CONFIRMADO_EM_CAMPO,
}
