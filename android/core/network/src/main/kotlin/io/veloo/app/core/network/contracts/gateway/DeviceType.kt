package io.signallq.app.core.network.contracts.gateway

/**
 * Classificacao do tipo de equipamento local (gateway) detectado na rede do
 * usuario — GH#545, epic GH#547.
 *
 * Base para a UI decidir quais capabilities oferecer (ex.: fibra/PON so faz
 * sentido para [ONT_GPON]) e para o motor de diagnostico decidir que contexto
 * de equipamento local esta disponivel.
 */
enum class DeviceType {
    /** ONT/GPON — equipamento de fibra optica (ex.: Nokia G-1425G-B). */
    ONT_GPON,

    /** Roteador padrao, sem funcao de fibra (ex.: TP-Link Archer C20/C6). */
    ROUTER,

    /** Ponto de acesso mesh ou extensor de sinal. */
    MESH_OR_EXTENDER,

    /** Equipamento nao reconhecido pelo catalogo, mas com superficie administrativa detectada (ex.: HTTP/rota conhecida respondendo). */
    UNKNOWN_SUPPORTED,

    /** Equipamento nao reconhecido e sem qualquer evidencia de superficie administrativa local. */
    UNKNOWN_UNSUPPORTED,
}
