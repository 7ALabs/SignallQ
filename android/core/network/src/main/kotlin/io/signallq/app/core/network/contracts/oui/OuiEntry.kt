package io.signallq.app.core.network.contracts.oui

import io.signallq.app.core.network.contracts.topologia.PapelTopologia

/**
 * Entrada única do catálogo OUI unificado (Fase 1 do plano de unificação de topologia —
 * issue #975/#978). Substitui as duas listas paralelas que existiam antes (`OuiDatabase` em
 * feature/devices, com fabricante mas sem papel de topologia; `MeshOuiDatabase` em coreNetwork,
 * com papel de topologia mas sem fabricante).
 *
 * @param prefixo os 6 primeiros hex do MAC (sem separador, uppercase).
 * @param fabricante nome comercial do fabricante desse bloco OUI.
 * @param papeisPossiveis papéis de topologia que esse OUI **pode** assumir — nunca um único
 * veredito. Um OUI pode aparecer legitimamente em mais de um papel (ex.: o conflito Intelbras
 * `C46E1F`/`6C5AB0`, hoje cadastrado como mesh E gateway ISP em listas separadas, vira aqui um
 * único registro com `papeisPossiveis = {ROTEADOR, NO_MESH}`). Conjunto vazio significa que o
 * fabricante é conhecido, mas o bloco não é de equipamento de rede (ex.: smartphone, notebook) —
 * a decisão de qual papel vale em cada contexto pertence ao motor de topologia (Fase 2A), nunca
 * a este catálogo.
 * @param nivelValidacao ver [NivelValidacaoOui].
 * @param especificidade ver [EspecificidadeOui].
 */
data class OuiEntry(
    val prefixo: String,
    val fabricante: String,
    val papeisPossiveis: Set<PapelTopologia>,
    val nivelValidacao: NivelValidacaoOui,
    val especificidade: EspecificidadeOui,
)
