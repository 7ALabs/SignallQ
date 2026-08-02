package io.signallq.app.core.network.contracts.connectivity

/**
 * Conclusão do diagnóstico local de conectividade (GH#1512). Estar conectado ao Wi-Fi
 * não significa ter internet — este enum separa explicitamente em qual camada o
 * problema foi observado, em vez de um "sem internet" genérico.
 */
enum class ConnectivityStatus {
    /** Enlace, gateway, DNS e rota externa confirmados — internet funcional. */
    INTERNET_AVAILABLE,

    /** Conectado ao Wi-Fi, gateway/DNS ok, mas a rota externa não confirma internet —
     *  conclusão honesta quando não dá para atribuir a causa a uma camada específica. */
    WIFI_WITHOUT_INTERNET,

    /** Wi-Fi conectado, mas o gateway local não responde. */
    GATEWAY_UNREACHABLE,

    /** Gateway alcançável, mas a resolução DNS falhou ou não está configurada. */
    DNS_FAILURE,

    /** Gateway e DNS ok, mas nenhuma sondagem de alcance externo (IP ou hostname) confirmou rota. */
    EXTERNAL_ROUTE_FAILURE,

    /** Rede exige portal cativo (login/aceite) antes de liberar internet. */
    CAPTIVE_PORTAL,

    /** Sinal misto: parte das sondagens externas confirma alcance, parte não — conexão instável. */
    PARTIAL_CONNECTIVITY,

    /** Sem transporte Wi-Fi ativo no momento do diagnóstico. */
    WIFI_DISCONNECTED,

    /** Evidência insuficiente para concluir com segurança qual camada falhou. */
    INCONCLUSIVE,
}
