package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.core.diagnostico.NivelCongestionamento
import io.signallq.app.core.diagnostico.RedeWifiVizinha
import io.signallq.app.core.diagnostico.WifiChannelDiagnosticEngine
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.core.network.wifi.EstadoScanWifi
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

// ─── Tab Canal ────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun CanalTab(
    redes: List<RedeVizinha>,
    connectedNetwork: RedeVizinha?,
    estado: EstadoScanWifi = EstadoScanWifi.concluido,
    erroMensagem: String? = null,
    onRefresh: () -> Unit = {},
    wifiLinkSnapshot: WifiLinkSnapshot? = null,
) {
    val c = LocalLkTokens.current

    val bandaConectada = connectedNetwork?.banda
    val redesBanda = redes.filter { it.banda == bandaConectada }
    val canalConectado = connectedNetwork?.canal

    val espectro =
        remember(redesBanda, canalConectado) {
            if (canalConectado == null || bandaConectada == null) return@remember null
            WifiChannelDiagnosticEngine.computarEspectro(
                redes =
                    redesBanda.map {
                        RedeWifiVizinha(
                            canal = it.canal,
                            rssiDbm = it.rssiDbm,
                            frequenciaMhz = it.frequenciaMhz,
                            ssid = it.ssid,
                            bssid = it.bssid,
                            larguraCanalMhz = it.larguraCanalMhz,
                        )
                    },
                canalAtual = canalConectado,
                banda = bandaConectada,
                seuSSID = connectedNetwork.ssid,
            )
        }

    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .padding(LkSpacing.xl)
                .padding(top = 16.dp),
    ) {
        if (espectro == null || canalConectado == null) {
            Text("Conecte-se a uma rede para analisar os canais.", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
            return@Column
        }

        val canalAtualDado = espectro.dadosPorCanal.firstOrNull { it.ehCanalAtual }
        val estaLivre = canalAtualDado?.nivel != NivelCongestionamento.congestionado
        val titleText = if (estaLivre) "Seu canal está livre." else "Seu canal está congestionado."
        val supportText = "A rede usa o canal $canalConectado e tem ${if (estaLivre) "pouca" else "muita"} competição por perto."

        Text(titleText, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)
        Spacer(Modifier.height(LkSpacing.sm))
        Text(supportText, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)

        Spacer(Modifier.height(LkSpacing.xl))

        // Card section
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.bgSecondary)
                    .padding(LkSpacing.lg),
        ) {
            Text("Ocupação dos canais", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
            Spacer(Modifier.height(LkSpacing.md))

            // metric-list
            val canaisOrdenados =
                espectro.dadosPorCanal
                    .sortedByDescending { it.countTerceiros }
                    .take(5)
                    .sortedBy { it.canal }
            canaisOrdenados.forEachIndexed { index, dado ->
                if (index > 0) {
                    androidx.compose.material3.HorizontalDivider(color = c.outlineVariant)
                }
                Row(modifier = Modifier.fillMaxWidth().padding(vertical = LkSpacing.md), verticalAlignment = Alignment.CenterVertically) {
                    val label = if (dado.ehCanalAtual) "Canal ${dado.canal} · sua rede" else "Canal ${dado.canal}"
                    Text(label, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary, modifier = Modifier.weight(1f))
                    val occupLevel =
                        when (dado.nivel) {
                            NivelCongestionamento.livre -> "Baixa"
                            NivelCongestionamento.moderado -> "Moderada"
                            NivelCongestionamento.congestionado -> "Alta"
                        }
                    Text(occupLevel, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold, color = c.textPrimary)
                }
            }
        }

        Spacer(Modifier.height(LkSpacing.xl))

        // notice box
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(LkRadius.card))
                    .background(c.primary.copy(alpha = 0.08f))
                    .padding(LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                if (estaLivre) Icons.Outlined.CheckCircle else Icons.Outlined.Warning,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(24.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            val noticeMsg = if (estaLivre) "Não há motivo para trocar o canal agora." else "Mudar o canal no roteador pode melhorar o sinal."
            Text(noticeMsg, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}
