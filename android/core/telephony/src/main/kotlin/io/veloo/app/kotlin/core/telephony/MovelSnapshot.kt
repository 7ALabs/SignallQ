package io.signallq.app.core.telephony

/**
 * Snapshot bruto da rede movel coletado via TelephonyManager.
 *
 * Contrato:
 *  - Todos os campos sao opcionais. Quando o dado nao esta disponivel
 *    (sem SIM, sem permissao, OEM nao reporta), o campo vai null.
 *  - Sem PII direto (sem IMEI, IMSI, telefone, GPS). cellId/mcc/mnc/tac
 *    sao metadados de celula — destinados ao diagnostico e descartados
 *    apos envio a IA.
 *
 * @property operadora      Nome comercial reportado pelo SO (TelephonyManager.networkOperatorName).
 *                          Pode diferir de mcc/mnc em casos de MVNO.
 * @property tecnologia     "5G SA" | "5G NSA" | "4G" | "3G" | "2G" | null.
 *                          Derivada de dataNetworkType + serviceState.nrState.
 * @property rsrpDbm        Reference Signal Received Power (4G/5G). Faixa tipica -140..-44 dBm.
 *                          Bom: > -85, medio: -85..-100, ruim: -100..-110, pessimo: < -110.
 * @property rsrqDb         Reference Signal Received Quality. Faixa tipica -20..-3 dB.
 * @property sinrDb         Signal-to-Interference-plus-Noise Ratio. Bom: > 10, medio: 0..10, ruim: < 0.
 * @property ecnoDb         Energy per chip / noise (CDMA/UMTS legado). null em LTE/NR.
 * @property bandaMovel     Identificacao de banda. Ex: "B3 (1800 MHz)" / "n78". Pode ser null
 *                          em OEMs que nao expoem EARFCN/NRARFCN.
 * @property cellId         CellIdentityLte.ci ou CellIdentityNr.nci (Long). Uniquely identifica
 *                          a celula servidora. Combinado com mcc/mnc/tac permite localizar
 *                          aproximadamente — tratar como sensivel.
 * @property mcc            Mobile Country Code (ex: "724" Brasil).
 * @property mnc            Mobile Network Code (ex: "06" Vivo).
 * @property tac            Tracking Area Code (LTE) ou TAC (NR).
 * @property roaming        true se em roaming.
 * @property timestampMs    Quando o snapshot foi capturado.
 */
data class MovelSnapshot(
    val operadora: String?,
    val tecnologia: String?,
    val rsrpDbm: Int?,
    val rsrqDb: Int?,
    val sinrDb: Int?,
    val ecnoDb: Int?,
    val bandaMovel: String?,
    val cellId: Long?,
    val mcc: String?,
    val mnc: String?,
    val tac: Int?,
    val roaming: Boolean?,
    val timestampMs: Long,
)
