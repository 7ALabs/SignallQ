package io.signallq.app.core.network.contracts.oui

/**
 * O quão específico um prefixo OUI é para identificar equipamento de rede (roteador/mesh/AP),
 * em oposição a um bloco genérico do fabricante usado em qualquer categoria de produto.
 *
 * Contrato da Fase 1 do plano de unificação de topologia (issue #975/#978). Uma [OuiEntry] com
 * algum [OuiEntry.papeisPossiveis] conhecido (ex.: bloco dedicado de uma linha mesh/roteador) é
 * [LINHA_PRODUTO_ESPECIFICA]; sem nenhum papel de topologia conhecido (bloco genérico do
 * fabricante, que também vende smartphone/notebook/eletrônico sem relação com rede) é
 * [FABRICANTE_GENERICO].
 */
enum class EspecificidadeOui {
    LINHA_PRODUTO_ESPECIFICA,
    FABRICANTE_GENERICO,
}
