package io.signallq.app.core.network.contracts.dispositivo

/**
 * Superset canônico do `TipoDispositivo` de `feature/devices`, para o motor unificado de
 * classificação (Fase 3+ do plano de unificação de topologia, issue #975).
 *
 * `console` é reservado nesta fase sem lógica associada ainda — hoje um Nintendo Switch cai
 * incorretamente em `smarthome` no classificador atual; a correção de heurística é escopo da
 * Fase 3, não desta fase (#977), que só existe pra travar o valor do enum com antecedência.
 */
enum class TipoDispositivoRede {
    roteador,
    pontoAcesso,
    computador,
    smartphone,
    smarthome,
    impressora,
    console,
    desconhecido,
}
