package io.signallq.app.core.network.contracts.localdevice

/**
 * Classificação do equipamento local (ONT/roteador/mesh) detectado pelo
 * SignallQ, independente do driver/parser que produziu os dados.
 *
 * A UI nunca deve inferir tipo por string de vendor (`vendor == "TP-Link"`) —
 * sempre a partir deste enum, populado pela camada de classificação (GH#545).
 */
enum class DeviceType {
    /** ONT GPON com dados ópticos (ex: Nokia G-1425G-B). */
    ONT_GPON,

    /** Roteador doméstico sem fibra (ex: TP-Link Archer C20/C6). */
    ROUTER,

    /** Nó mesh ou extensor de Wi-Fi. */
    MESH_OR_EXTENDER,

    /** Equipamento não identificado, mas com driver/parser capaz de ler algo. */
    UNKNOWN_SUPPORTED,

    /** Equipamento não identificado e sem suporte de leitura. */
    UNKNOWN_UNSUPPORTED,
}
