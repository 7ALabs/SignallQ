package io.signallq.app.ui.screen

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import io.signallq.app.monitoramento.OemKillInfo
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.ConfirmacaoDialog
import io.signallq.app.ui.component.LkInfoCallout
import io.signallq.app.ui.component.LkSheetDivider
import io.signallq.app.ui.component.LkSheetFrame

// GH#936 — Fase 7 MD3 (5f): extraido de AjustesScreen.kt (era "DiagnosticoSheet", so
// acessivel via toggles dentro de Ajustes). Agora e destino unico tanto do atalho
// "Monitoramento" no hub Ferramentas quanto da linha equivalente em Perfil/Ajustes —
// nenhum dos dois reimplementa os toggles, so abrem este sheet.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MonitoramentoSheet(
    c: LkTokens,
    analiseAvancada: Boolean,
    monitoramentoAtivo: Boolean,
    notificacaoLatenciaAtiva: Boolean,
    notificacaoDnsAtiva: Boolean,
    notificacaoRssiAtiva: Boolean,
    notificacaoSemInternetAtiva: Boolean,
    onDismiss: () -> Unit,
    onDefinirAnaliseAvancada: (Boolean) -> Unit,
    onAtivarMonitoramento: (Boolean) -> Unit,
    onDefinirNotificacaoLatenciaAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoDnsAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoRssiAtiva: (Boolean) -> Unit,
    onDefinirNotificacaoSemInternetAtiva: (Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfirmAnalise by remember { mutableStateOf(false) }
    var showConfirmMonitoramento by remember { mutableStateOf(false) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {},
        containerColor = c.surfaceContainerLow,
    ) {
        LkSheetFrame(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .navigationBarsPadding(),
        ) {
            Text(
                text = "Diagnóstico avançado",
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
                fontWeight = FontWeight.W700,
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = "Recursos que aprofundam a análise da sua rede",
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Spacer(Modifier.height(LkSpacing.sm))
            ToggleItem(
                c = c,
                icon = Icons.Outlined.Analytics,
                label = "Análise avançada",
                subtitle =
                    if (analiseAvancada) {
                        "Ativa · coleta sinais extras para aprofundar o diagnóstico"
                    } else {
                        "Desativada · pode aumentar consumo de bateria"
                    },
                checked = analiseAvancada,
                onCheckedChange = { enabled ->
                    if (enabled && !analiseAvancada) {
                        showConfirmAnalise = true
                    } else if (!enabled) {
                        onDefinirAnaliseAvancada(false)
                    }
                },
            )
            LkSheetDivider()
            ToggleItem(
                c = c,
                icon = Icons.Outlined.Sensors,
                label = "Monitoramento passivo",
                subtitle =
                    if (monitoramentoAtivo) {
                        "Ativo · verifica a conexão e pode enviar alertas"
                    } else {
                        "Desativado"
                    },
                checked = monitoramentoAtivo,
                onCheckedChange = { novoValor ->
                    if (novoValor) {
                        showConfirmMonitoramento = true
                    } else {
                        onAtivarMonitoramento(false)
                    }
                },
            )
            if (monitoramentoAtivo) {
                LkSheetDivider(modifier = Modifier.padding(horizontal = LkSpacing.lg))
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.WifiOff,
                    label = "Sem internet",
                    subtitle = "Avisa quando a conexão cair",
                    checked = notificacaoSemInternetAtiva,
                    onCheckedChange = onDefinirNotificacaoSemInternetAtiva,
                )
                LkSheetDivider()
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.Speed,
                    label = "Latência alta",
                    subtitle = "Avisa quando a rede ficar lenta",
                    checked = notificacaoLatenciaAtiva,
                    onCheckedChange = onDefinirNotificacaoLatenciaAtiva,
                )
                LkSheetDivider()
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.Language,
                    label = "DNS lento",
                    subtitle = "Avisa quando sites e apps demorarem para carregar",
                    checked = notificacaoDnsAtiva,
                    onCheckedChange = onDefinirNotificacaoDnsAtiva,
                )
                LkSheetDivider()
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.Wifi,
                    label = "Sinal Wi-Fi fraco",
                    subtitle = "Avisa quando o sinal cair abaixo do ideal",
                    checked = notificacaoRssiAtiva,
                    onCheckedChange = onDefinirNotificacaoRssiAtiva,
                )
            }
            if (monitoramentoAtivo && OemKillInfo.fabricanteRiscoAlto) {
                LkSheetDivider()
                LkInfoCallout(
                    icon = Icons.Outlined.Info,
                    iconTint = c.warning,
                    text =
                        "Em alguns dispositivos ${OemKillInfo.nomeFabricante}, o sistema pode reduzir a frequência " +
                            "das verificações para economizar bateria. Para garantir o funcionamento, mantenha o SignallQ " +
                            "na lista de apps sem restrição de bateria nas configurações do sistema.",
                    modifier =
                        Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                )
            }
        }
    }

    if (showConfirmAnalise) {
        ConfirmacaoDialog(
            titulo = "Ativar análise avançada?",
            mensagem = "Esse recurso pode aumentar o consumo de bateria e dados, especialmente nas próximas janelas de medição.",
            onConfirmar = {
                onDefinirAnaliseAvancada(true)
                showConfirmAnalise = false
            },
            onCancelar = { showConfirmAnalise = false },
        )
    }

    if (showConfirmMonitoramento) {
        ConfirmacaoDialog(
            titulo = "Ativar monitoramento em segundo plano?",
            mensagem =
                "O SignallQ verificará sua conexão periodicamente e enviará uma notificação se detectar lentidão " +
                    "ou instabilidade. Consome dados e bateria de forma mínima.",
            textoBotaoConfirmar = "Ativar",
            textoBotaoCancelar = "Agora não",
            onConfirmar = {
                onAtivarMonitoramento(true)
                showConfirmMonitoramento = false
            },
            onCancelar = { showConfirmMonitoramento = false },
        )
    }
}
