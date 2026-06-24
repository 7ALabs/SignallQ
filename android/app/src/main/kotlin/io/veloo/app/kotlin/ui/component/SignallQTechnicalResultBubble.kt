package io.veloo.app.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import io.veloo.app.feature.diagnostico.DiagnosticStatus
import io.veloo.app.feature.diagnostico.pulse.IntelligentDiagnosticSession
import io.veloo.app.ui.LkColors
import io.veloo.app.ui.LocalLkTokens

private val BUBBLE_SHAPE = RoundedCornerShape(topStart = 4.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 16.dp)

fun IntelligentDiagnosticSession.hasAnyData(): Boolean =
    speedtestDownloadMbps != null ||
        speedtestUploadMbps != null ||
        speedtestLatencyMs != null ||
        wifiRssiDbm != null ||
        diagnosticReport != null

@Composable
fun SignallQTechnicalResultBubble(
    session: IntelligentDiagnosticSession,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    val report = session.diagnosticReport

    val statusColor =
        when (report?.decisao?.status) {
            DiagnosticStatus.ok, DiagnosticStatus.info -> LkColors.success
            DiagnosticStatus.attention -> LkColors.warning
            DiagnosticStatus.critical -> LkColors.error
            else -> c.textTertiary
        }
    val statusLabel =
        when (report?.decisao?.status) {
            DiagnosticStatus.ok, DiagnosticStatus.info -> "Rede OK"
            DiagnosticStatus.attention -> "Atenção"
            DiagnosticStatus.critical -> "Problema crítico"
            else -> "Inconclusivo"
        }

    Row(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth(0.85f)
                    .clip(BUBBLE_SHAPE)
                    .background(c.bgCard)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Icon(
                    imageVector = Icons.Default.AutoAwesome,
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier = Modifier.size(12.dp),
                )
                Text(
                    text = "Coletei os dados da sua rede.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }

            // Status pill + inline metrics
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                if (report != null) {
                    Row(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(100.dp))
                                .background(statusColor.copy(alpha = 0.15f))
                                .padding(horizontal = 8.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(4.dp)
                                    .clip(CircleShape)
                                    .background(statusColor),
                        )
                        Text(
                            text = statusLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = statusColor,
                        )
                    }
                }
                session.speedtestDownloadMbps?.let {
                    Text(
                        "↓ ${"%.0f".format(it)} Mbps",
                        style = MaterialTheme.typography.bodySmall,
                        color = LkColors.phaseDownload,
                    )
                }
                session.speedtestUploadMbps?.let {
                    Text(
                        "↑ ${"%.0f".format(it)} Mbps",
                        style = MaterialTheme.typography.bodySmall,
                        color = LkColors.phaseUpload,
                    )
                }
                session.speedtestLatencyMs?.let {
                    Text(
                        "${"%.0f".format(it)} ms",
                        style = MaterialTheme.typography.bodySmall,
                        color = LkColors.phaseLatencia,
                    )
                }
            }

            // Wi-Fi info (compact, secondary)
            session.wifiSsid?.let { ssid ->
                val rssi = session.wifiRssiDbm?.let { " · $it dBm" } ?: ""
                Text(
                    text = "Wi-Fi: $ssid$rssi",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
        }
    }
}
