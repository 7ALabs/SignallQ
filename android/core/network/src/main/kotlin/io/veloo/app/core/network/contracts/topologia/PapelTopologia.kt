package io.signallq.app.core.network.contracts.topologia

/**
 * Papel provável de um nó de rede dentro da topologia Wi-Fi.
 *
 * Contrato da Fase 0 do plano de unificação de topologia (issue #975/#977). Não existe
 * `ROTEADOR_CENTRAL_INFERIDO`: a plataforma Android não permite confirmar com certeza qual nó
 * de um sistema mesh é o central sem uma segunda rota IP visível — quando só há evidência de
 * múltiplos nós (sem essa confirmação), o resultado correto é [SISTEMA_MESH_PROVAVEL], nunca a
 * afirmação de qual nó específico é o roteador.
 */
enum class PapelTopologia {
    ROTEADOR,
    NO_MESH,
    REPETIDOR,
    PONTO_DE_ACESSO,
    SISTEMA_MESH_PROVAVEL,
    DESCONHECIDO,
}
