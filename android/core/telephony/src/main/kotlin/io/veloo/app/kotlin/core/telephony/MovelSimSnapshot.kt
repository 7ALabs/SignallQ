package io.signallq.app.core.telephony

/**
 * Snapshot de um SIM fisico ativo capturado via SubscriptionManager.
 *
 * Contrato:
 *  - Todos os campos opcionais. Quando indisponivel (sem permissao, OEM nao reporta), campo fica null.
 *  - Nao contem PII: sem IMEI, IMSI, numero de telefone.
 *
 * @property subId          SubscriptionInfo.subscriptionId — identificador do SIM.
 * @property simIndex       Slot 1-based (1, 2...) derivado de simSlotIndex+1.
 * @property operadora      Nome comercial da operadora (SubscriptionInfo.carrierName).
 * @property tecnologiaRede "5G SA" | "5G NSA" | "4G" | "3G" | "2G" | null.
 * @property rsrpDbm        RSRP do SIM. Classificacao real fica a cargo de
 *                          `MetricClassifier.classificarRsrp` (core/diagnostico) por
 *                          tecnologia (GH#1206) -- nao documentar limiar fixo aqui.
 * @property rsrqDb         Reference Signal Received Quality deste SIM (GH#1206) — mesma
 *                          extracao ja usada pelo snapshot geral ([MovelSnapshot.rsrqDb]),
 *                          agora tambem por assinatura/SIM.
 * @property sinrDb         Signal-to-Interference-plus-Noise Ratio deste SIM (GH#1206).
 * @property emRoaming      true se o SIM esta em roaming internacional.
 * @property radioDesligado true quando o radio celular esta desligado (modo aviao) —
 *                          rsrpDbm/rsrqDb/sinrDb nao representam medicao real quando true.
 */
data class MovelSimSnapshot(
    val subId: Int,
    val simIndex: Int,
    val operadora: String?,
    val tecnologiaRede: String?,
    val rsrpDbm: Int?,
    val emRoaming: Boolean,
    val isDefaultData: Boolean = false,
    val radioDesligado: Boolean = false,
    val rsrqDb: Int? = null,
    val sinrDb: Int? = null,
)
