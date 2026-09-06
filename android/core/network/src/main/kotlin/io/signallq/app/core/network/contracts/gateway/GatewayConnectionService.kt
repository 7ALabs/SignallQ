package io.signallq.app.core.network.contracts.gateway

/**
 * Contrato minimo de conexao ativa ao gateway (roteador/ONT) — usado pela sheet
 * de conexao da Home (GatewayConnectionSheet, GH#526, epic #525).
 *
 * A implementacao real (autenticacao HTTP/TR-064/NetHAL no roteador ou ONT, por
 * fabricante/firmware) segue fora de escopo — item futuro da issue #547. Esta
 * interface existe so para a UI funcionar hoje (sheet manual e autoconexao por
 * BSSID do GH#527), injetavel com qualquer implementacao futura sem reescrever
 * a tela.
 *
 * BUG#1511 (P0) — ate #547 entregar uma implementacao real, **nenhum**
 * caminho de producao pode injetar aqui um servico que retorne
 * [GatewayConnectionResultado.Sucesso] por padrao ou sem autenticacao real
 * comprovada (ver `gatewayConnectionServiceIndisponivel` em `AppShell.kt`).
 * Retornar sucesso simulado nesta interface e exatamente a classe de bug que
 * a #1511 eliminou — nao reintroduzir um mock equivalente.
 */
fun interface GatewayConnectionService {
    suspend fun conectar(
        ip: String,
        usuario: String,
        senha: String,
    ): GatewayConnectionResultado
}

sealed interface GatewayConnectionResultado {
    /** Autenticacao real comprovada no equipamento. So uma implementacao real (#547) pode retornar isto. */
    data object Sucesso : GatewayConnectionResultado

    /**
     * [mensagemUsuario] deve ser amigavel e sem jargao de protocolo (nunca
     * algo como "timeout TR-064" ou "HTTP 401") — a implementacao real e
     * responsavel por traduzir o erro tecnico antes de retornar aqui.
     */
    data class Falha(
        val mensagemUsuario: String,
    ) : GatewayConnectionResultado

    /**
     * BUG#1511 — nenhuma implementacao real de autenticacao esta conectada a
     * este canal ainda (aguardando #547). Distinto de [Falha]: nao houve
     * tentativa real de autenticar, entao nunca deve ser tratado como sessao
     * validada nem como falha de credencial/rede especifica.
     */
    data object Indisponivel : GatewayConnectionResultado
}

/**
 * BUG#1511 (P0) — default unico e seguro para parametros/valores de [GatewayConnectionService]
 * em composables e data classes de UI (ex.: `AppShell`/`Inicio2Screen`,
 * `AjustesModemState.conectarGateway`). Nunca duplicar o literal
 * `GatewayConnectionService { _, _, _ -> ... }` retornando [GatewayConnectionResultado.Sucesso]
 * como default — isso e exatamente o bug que a #1511 corrigiu. Qualquer caller de producao
 * deve injetar uma implementacao real (fora de escopo ate a #547); ate la, este e o unico
 * default aceitavel.
 */
val GatewayConnectionServiceIndisponivelPadrao: GatewayConnectionService =
    GatewayConnectionService { _, _, _ -> GatewayConnectionResultado.Indisponivel }
