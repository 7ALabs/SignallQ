package io.linka.app.kotlin.ui.screen

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.linka.app.kotlin.feature.diagnostico.DiagnosticResult
import io.linka.app.kotlin.feature.diagnostico.DiagnosticStatus
import io.linka.app.kotlin.feature.diagnostico.FibraDiagnosticInput
import io.linka.app.kotlin.feature.diagnostico.FibraSignalQualityEngine
import io.linka.app.kotlin.feature.fibra.DeviceInfoFibra
import io.linka.app.kotlin.feature.fibra.GponStatus
import io.linka.app.kotlin.ui.LkColors
import io.linka.app.kotlin.ui.LkSpacing
import io.linka.app.kotlin.ui.LkTokens
import io.linka.app.kotlin.ui.LocalLkTokens

// ─── Entry point ──────────────────────────────────────────────────────────────

@Composable
fun FibraModemScreen(
    uiState: FibraModemUiState,
    onConectar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .background(c.bgPrimary)
                .navigationBarsPadding(),
    ) {
        // Loading indicator no topo — visível durante Conectando (refresh)
        if (uiState is FibraModemUiState.Conectando) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth(),
                color = LkColors.accent,
                trackColor = c.bgSecondary,
            )
        }

        when (uiState) {
            is FibraModemUiState.SemWifi -> FibraEstadoVazio(
                icone = Icons.Outlined.WifiOff,
                titulo = "Análise indisponível",
                descricao = "A análise do modem só funciona quando você está na rede local (Wi-Fi).",
                c = c,
            )

            is FibraModemUiState.SemCredenciais -> FibraEstadoVazio(
                icone = Icons.Outlined.Settings,
                titulo = "Configure o acesso ao modem",
                descricao = "Informe o IP, usuário e senha do modem nos ajustes para consultar os dados da fibra.",
                ctaLabel = "Configurar modem",
                onCta = onAbrirAjustes,
                c = c,
            )

            is FibraModemUiState.Conectando -> FibraEstadoSkeleton(c = c)

            is FibraModemUiState.Erro -> FibraEstadoErro(
                onConectar = onConectar,
                onAbrirAjustes = onAbrirAjustes,
                c = c,
            )

            is FibraModemUiState.Concluido -> FibraEstadoConcluido(
                estado = uiState,
                c = c,
            )
        }
    }
}

// ─── Estado: sem Wi-Fi / sem credenciais ──────────────────────────────────────

@Composable
private fun FibraEstadoVazio(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    ctaLabel: String? = null,
    onCta: (() -> Unit)? = null,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icone,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = c.textTertiary,
        )
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            text = titulo,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            text = descricao,
            fontSize = 14.sp,
            color = c.textSecondary,
        )
        if (ctaLabel != null && onCta != null) {
            Spacer(Modifier.height(LkSpacing.lg))
            Button(onClick = onCta) {
                Text(ctaLabel)
            }
        }
    }
}

// ─── Estado: erro de conexão ───────────────────────────────────────────────────

@Composable
private fun FibraEstadoErro(
    onConectar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxSize()
                .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = LkColors.warning,
        )
        Spacer(Modifier.height(LkSpacing.md))
        Text(
            text = "Não consegui acessar o modem",
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            text = "Verifique o IP, o usuário e a senha nas configurações do modem.",
            fontSize = 14.sp,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.lg))
        Button(onClick = onConectar, modifier = Modifier.fillMaxWidth()) {
            Text("Tentar novamente")
        }
        Spacer(Modifier.height(LkSpacing.sm))
        OutlinedButton(onClick = onAbrirAjustes, modifier = Modifier.fillMaxWidth()) {
            Text("Revisar configurações")
        }
    }
}

// ─── Estado: skeleton de carregamento inicial ──────────────────────────────────

@Composable
private fun FibraEstadoSkeleton(c: LkTokens) {
    val shimmer by rememberInfiniteTransition(label = "fibra_shimmer").animateFloat(
        initialValue = 0f,
        targetValue = 1000f,
        animationSpec =
            infiniteRepeatable(
                animation = tween(durationMillis = 1200, easing = LinearEasing),
                repeatMode = RepeatMode.Restart,
            ),
        label = "shimmer_offset",
    )
    val shimmerBrush =
        Brush.linearGradient(
            colors =
                listOf(
                    c.bgSecondary,
                    c.bgPrimary,
                    c.bgSecondary,
                ),
            start = Offset(shimmer - 300f, 0f),
            end = Offset(shimmer + 300f, 0f),
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(top = LkSpacing.lg),
    ) {
        repeat(3) {
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(72.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(shimmerBrush),
            )
            Spacer(Modifier.height(LkSpacing.sm))
        }
    }
}

// ─── Estado: concluído ─────────────────────────────────────────────────────────

@Composable
private fun FibraEstadoConcluido(
    estado: FibraModemUiState.Concluido,
    c: LkTokens,
) {
    // Derivar interpretações localmente a partir do GponStatus
    val fibraInput =
        remember(estado.gpon) {
            FibraDiagnosticInput(
                rxPowerDbm = estado.gpon.rxPowerDbm,
                txPowerDbm = estado.gpon.txPowerDbm,
                temperatureCelsius = estado.gpon.temperatureCelsius,
                isUp = estado.gpon.isUp,
            )
        }
    val interpretacoes = remember(fibraInput) { FibraSignalQualityEngine.avaliar(fibraInput) }

    // Calcular status geral
    val statusGeral =
        when {
            interpretacoes.any { it.status == DiagnosticStatus.critical } -> StatusGeral.Ruim
            interpretacoes.any { it.status == DiagnosticStatus.attention } -> StatusGeral.Regular
            else -> StatusGeral.Boa
        }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
                .padding(top = LkSpacing.lg, bottom = LkSpacing.xl),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        // Chip de status geral
        FibraChipStatus(statusGeral = statusGeral, c = c)

        Spacer(Modifier.height(LkSpacing.xs))

        // Bloco valores técnicos
        FibraBlocoValores(gpon = estado.gpon, deviceInfo = estado.deviceInfo, c = c)

        Spacer(Modifier.height(LkSpacing.xs))

        // Bloco interpretação
        if (interpretacoes.isNotEmpty()) {
            FibraBlocoInterpretacao(interpretacoes = interpretacoes, c = c)
        }
    }
}

// ─── Chip de status geral ─────────────────────────────────────────────────────

private enum class StatusGeral { Boa, Regular, Ruim }

@Composable
private fun FibraChipStatus(
    statusGeral: StatusGeral,
    c: LkTokens,
) {
    val (label, cor) =
        when (statusGeral) {
            StatusGeral.Boa -> "Sinal ótimo" to LkColors.success
            StatusGeral.Regular -> "Sinal regular" to LkColors.warning
            StatusGeral.Ruim -> "Sinal fraco" to LkColors.error
        }
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(20.dp))
                .background(cor.copy(alpha = 0.12f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = cor,
        )
    }
}

// ─── Bloco valores ────────────────────────────────────────────────────────────

@Composable
private fun FibraBlocoValores(
    gpon: GponStatus,
    deviceInfo: DeviceInfoFibra?,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bgSecondary)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.xs),
    ) {
        Text(
            text = "SINAIS DA FIBRA",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textTertiary,
        )
        Spacer(Modifier.height(4.dp))

        FibraValorRow(
            label = "RX Power",
            valor = if (gpon.rxPowerDbm != 0.0) "${"%.2f".format(gpon.rxPowerDbm)} dBm" else "--",
            c = c,
        )
        HorizontalDivider(color = c.divider, thickness = 0.5.dp)
        FibraValorRow(
            label = "TX Power",
            valor = if (gpon.txPowerDbm != 0.0) "${"%.2f".format(gpon.txPowerDbm)} dBm" else "--",
            c = c,
        )
        HorizontalDivider(color = c.divider, thickness = 0.5.dp)
        FibraValorRow(
            label = "Temperatura",
            valor =
                if (gpon.temperatureCelsius != 0.0) {
                    "${"%.1f".format(gpon.temperatureCelsius)} °C"
                } else {
                    "--"
                },
            c = c,
        )
        HorizontalDivider(color = c.divider, thickness = 0.5.dp)
        FibraValorRow(
            label = "Status óptico",
            valor = if (gpon.status.isNotBlank()) gpon.status.replaceFirstChar { it.uppercase() } else "--",
            c = c,
        )
        if (deviceInfo != null) {
            HorizontalDivider(color = c.border, thickness = 0.5.dp)
            FibraValorRow(
                label = "Modelo",
                valor = "${deviceInfo.manufacturer} ${deviceInfo.model}".trim().ifBlank { "--" },
                c = c,
            )
        }
    }
}

@Composable
private fun FibraValorRow(
    label: String,
    valor: String,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, fontSize = 14.sp, color = c.textSecondary)
        Text(
            text = valor,
            fontSize = 14.sp,
            fontWeight = FontWeight.Medium,
            color = c.textPrimary,
        )
    }
}

// ─── Bloco interpretação ──────────────────────────────────────────────────────

@Composable
private fun FibraBlocoInterpretacao(
    interpretacoes: List<DiagnosticResult>,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .background(c.bgSecondary)
                .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Text(
            text = "INTERPRETAÇÃO",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = c.textTertiary,
        )
        Spacer(Modifier.height(4.dp))
        interpretacoes.forEach { resultado ->
            val mensagem = resultado.mensagemUsuario
            val recomendacao = resultado.recomendacao
            val corStatus =
                when (resultado.status) {
                    DiagnosticStatus.critical -> LkColors.error
                    DiagnosticStatus.attention -> LkColors.warning
                    else -> LkColors.success
                }
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(corStatus.copy(alpha = 0.08f))
                        .padding(12.dp),
            ) {
                Text(
                    text = resultado.titulo,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = corStatus,
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = mensagem,
                    fontSize = 13.sp,
                    color = c.textSecondary,
                )
                if (recomendacao.isNotBlank()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = recomendacao,
                        fontSize = 12.sp,
                        color = c.textTertiary,
                    )
                }
            }
        }
    }
}

// ─── Previews ─────────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "FibraModem — Concluído")
@Composable
private fun PreviewConcluido() {
    val c = LocalLkTokens.current
    FibraModemScreen(
        uiState =
            FibraModemUiState.Concluido(
                gpon =
                    GponStatus(
                        status = "up",
                        mode = "GPON",
                        rxPowerDbm = -20.5,
                        txPowerDbm = 2.1,
                        temperatureCelsius = 52.3,
                        serial = "HWTC1A2B3C4D",
                        voltageV = 3.3,
                        laserCurrentMa = 18.4,
                    ),
                deviceInfo =
                    DeviceInfoFibra(
                        model = "EG8145X6-10",
                        manufacturer = "Huawei",
                        serialNumber = "HC9012345",
                        firmwareVersion = "V6R020C00S280",
                        hardwareVersion = "VER.B",
                        uptimeSeconds = 86401,
                    ),
                wan = null,
                ppp = null,
                interpretacoes = emptyList(),
            ),
        onConectar = {},
        onAbrirAjustes = {},
        c = c,
    )
}

@Preview(showBackground = true, name = "FibraModem — Erro")
@Composable
private fun PreviewErro() {
    val c = LocalLkTokens.current
    FibraModemScreen(
        uiState = FibraModemUiState.Erro(chave = "fibra.timeout"),
        onConectar = {},
        onAbrirAjustes = {},
        c = c,
    )
}

@Preview(showBackground = true, name = "FibraModem — Sem Wi-Fi")
@Composable
private fun PreviewSemWifi() {
    val c = LocalLkTokens.current
    FibraModemScreen(
        uiState = FibraModemUiState.SemWifi,
        onConectar = {},
        onAbrirAjustes = {},
        c = c,
    )
}
