package io.signallq.app.ui.screen

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.ClienteConectadoUi
import io.signallq.app.ui.component.EquipamentoItemTecnico
import io.signallq.app.ui.component.EquipamentoSecaoTecnica
import io.signallq.app.ui.component.LkSurfaceCard

/**
 * Módulos técnicos (passos 9, 10, 11 e 12 da narrativa, bug #6 spec Lia) —
 * cada card representa uma seção de dado cru do equipamento (Fibra óptica,
 * Internet/WAN, LAN, Wi-Fi, Dispositivos conectados). Extraído de
 * `EquipamentoInternetScreen.kt` (dívida crítica, ver
 * `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.6).
 *
 * ## Wi-Fi — full-width (revisão Lia 2026-07-18)
 * A primeira versão desta tela particionava Wi-Fi em 2 colunas por banda
 * (2,4GHz | 5/6GHz). Reavaliado: com só 1 item de dado por banda (canal +
 * configuração — o contrato
 * [io.signallq.app.core.network.contracts.localdevice.WifiRadioSnapshot] não
 * modela sinal/clientes por rádio), os dois cards lado a lado ficavam ~90%
 * vazios e recriavam a poluição visual que o redesign eliminou. Wi-Fi passou
 * a ser um card full-width único, com as duas bandas como linhas dentro do
 * mesmo card (2,4GHz antes de 5/6GHz — ordem já preservada pelo mapper, 1
 * item por rádio), no mesmo formato dos demais itens técnicos.
 */
@Composable
internal fun ModuloTecnicoCard(
    secao: EquipamentoSecaoTecnica,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val tituloExibido = if (secao.titulo == "Fibra óptica") "Fibra" else secao.titulo
    val toggleLabel = if (secao.titulo == "Fibra óptica") "Ver detalhes técnicos" else "Ver detalhes"
    var expandido by remember(secao.titulo) { mutableStateOf(secao.titulo != "Fibra óptica") }

    LkSurfaceCard(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(secao.icone, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                text = tituloExibido,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            if (secao.itens.size > 2) {
                Text(
                    text = if (expandido) "Ocultar" else toggleLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = c.primary,
                    modifier = Modifier.clickable { expandido = !expandido },
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.sm))
        val itensVisiveis = if (expandido || secao.itens.size <= 2) secao.itens else secao.itens.take(2)
        secao.overline?.let {
            Text(it, style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
            Spacer(Modifier.height(4.dp))
        }
        itensVisiveis.forEachIndexed { index, item ->
            DataRowCard(item = secao.normalizarItem(item), c = c)
            if (index < itensVisiveis.lastIndex) {
                Spacer(Modifier.height(6.dp))
            }
        }
        if (secao.clientes.isNotEmpty()) {
            secao.clientes.forEach { cliente ->
                Spacer(Modifier.height(8.dp))
                ClienteResumoRow(cliente = cliente, c = c)
            }
        }
        secao.trailing?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }
    }
}

internal fun EquipamentoSecaoTecnica.normalizarItem(item: EquipamentoItemTecnico): EquipamentoItemTecnico =
    if (titulo == "Fibra óptica" && item.label == "Link óptico") {
        item.copy(label = "Conexão PON")
    } else {
        item
    }

@Composable
internal fun DataRowCard(
    item: EquipamentoItemTecnico,
    c: LkTokens,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = item.label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Spacer(Modifier.width(8.dp))
        Text(
            text = item.valor,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.labelLarge,
            color = item.statusValor?.let { statusColor(it, c) } ?: c.textPrimary,
            textAlign = TextAlign.End,
            maxLines = 3,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
internal fun ClienteResumoRow(
    cliente: ClienteConectadoUi,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(cliente.tipoIcone, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
        Spacer(Modifier.width(6.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(cliente.titulo, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
                cliente.tipoLabel?.let {
                    Spacer(Modifier.width(6.dp))
                    Text(it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary)
                }
            }
            cliente.ip?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textTertiary) }
        }
    }
}

@Composable
internal fun DevicesSummaryCard(
    summary: DevicesSummaryUi,
    c: LkTokens,
) {
    LkSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Devices, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text("Dispositivos conectados", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.W600, color = c.textPrimary)
        }
        Spacer(Modifier.height(LkSpacing.sm))
        Text("${summary.total} dispositivos", style = MaterialTheme.typography.headlineSmall, color = c.textPrimary)
        Spacer(Modifier.height(2.dp))
        Text("${summary.wifi} pelo Wi-Fi · ${summary.cabo} por cabo", style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        if (summary.flags.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.sm))
            summary.flags.forEachIndexed { index, flag ->
                Text(flag, style = MaterialTheme.typography.labelMedium, color = LkColors.warning)
                if (index < summary.flags.lastIndex) Spacer(Modifier.height(4.dp))
            }
        }
    }
}

private fun statusColor(
    status: DiagnosticStatus,
    c: LkTokens,
): Color =
    when (status) {
        DiagnosticStatus.ok -> LkColors.success
        DiagnosticStatus.info -> c.primary
        DiagnosticStatus.attention -> LkColors.warning
        DiagnosticStatus.critical -> LkColors.error
        DiagnosticStatus.inconclusive -> LkColors.warning
    }
