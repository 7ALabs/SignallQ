package io.signallq.app.core.database.connectivity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Persistencia local do diagnostico de conectividade (GH#1512) -- só campos já
 * avaliados/sanitizados. Nunca guarda SSID, BSSID, IP local, IP do gateway ou
 * endereco de servidor DNS, seguindo o mesmo padrao de allowlist de
 * `LocalDeviceSafeFilter` (core:network, GH#541): status agregado, nunca o dado bruto
 * que o originou.
 *
 * Tabela: connectivity_diagnosis_history
 *
 * @param gatewayReachableOutcome nome do tipo selado `ProbeResult` (Success/Failure/
 *   Timeout/NotExecuted/Unavailable) -- nunca o IP sondado.
 * @param incompleteReason motivo textual (sem IP/hostname) de etapas nao executadas,
 *   quando o diagnostico terminou parcial.
 */
@Entity(
    tableName = "connectivity_diagnosis_history",
    indices = [Index(value = ["startedAtEpochMs"])],
)
data class ConnectivityDiagnosisHistoryEntity(
    @PrimaryKey
    val id: String,
    val startedAtEpochMs: Long,
    val finishedAtEpochMs: Long,
    val transport: String,
    val status: String,
    val confidence: String,
    val wifiConnected: Boolean,
    val localAddressAvailable: Boolean,
    val gatewayConfigured: Boolean,
    val gatewayReachableOutcome: String,
    val dnsConfigured: Boolean,
    val dnsReachableOutcome: String,
    val externalIpReachableOutcome: String,
    val hostnameReachableOutcome: String,
    val androidInternetCapability: Boolean,
    val androidValidated: Boolean,
    val captivePortalDetected: Boolean,
    val mobileFallbackAvailable: Boolean,
    val incompleteReason: String? = null,
)
