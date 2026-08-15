package io.signallq.app.feature.devices

/**
 * GH#1217 RF (resultado parcial) — antes só existiam 4 estados, e semWifi/timeout/cancelado
 * caíam todos genericamente em [erro] + `erroMensagem` string (ex.: "naoWifi", "timeout"),
 * misturando causas bem diferentes sob o mesmo rótulo visual. Agora cada situação tem seu
 * próprio estado tipado — a UI decide o texto certo sem precisar interpretar string solta.
 */
enum class EstadoScanDispositivos {
    idle,
    varrendo,
    concluido,
    /** Alguns dispositivos foram encontrados, mas pelo menos uma fase de descoberta (Subnet/
     *  ARP/mDNS/SSDP/TCP probe) lançou exceção em vez de simplesmente não achar nada — o
     *  resultado é válido, mas incompleto. */
    concluidoParcial,
    /** Sem Wi-Fi ativo — o scan nunca chega a rodar (antes: erro + erroMensagem="naoWifi"). */
    semWifi,
    /** Nenhuma fase respondeu dentro do timeout global — antes: erro + erroMensagem="timeout". */
    timeout,
    /** Usuário/chamador cancelou o scan cooperativamente antes de concluir. */
    cancelado,
    /** Falha real e inesperada (permissão, exceção de rede não classificada). */
    erro,
}

