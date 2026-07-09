package io.signallq.app.core.network.contracts.gateway

/**
 * Contrato minimo de conexao ativa ao gateway (roteador/ONT) — usado pela sheet
 * de conexao da Home (GatewayConnectionSheet, GH#526, epic #525).
 *
 * A implementacao real (autenticacao HTTP/TR-064 no roteador ou ONT, por
 * fabricante/firmware) segue fora de escopo — item futuro do epic #525. Esta
 * interface existe so para a UI funcionar hoje (sheet manual e autoconexao por
 * BSSID do GH#527) com estados mockaveis, sem acoplar a nenhuma implementacao
 * concreta ainda inexistente.
 */
fun interface GatewayConnectionService {
    suspend fun conectar(ip: String, usuario: String, senha: String): GatewayConnectionResultado
}

sealed interface GatewayConnectionResultado {
    data object Sucesso : GatewayConnectionResultado

    /**
     * [mensagemUsuario] deve ser amigavel e sem jargao de protocolo (nunca
     * algo como "timeout TR-064" ou "HTTP 401") — a implementacao real e
     * responsavel por traduzir o erro tecnico antes de retornar aqui.
     */
    data class Falha(val mensagemUsuario: String) : GatewayConnectionResultado
}
