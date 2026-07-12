package io.signallq.app.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.CheckCircleOutline
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.diagnostico.FibraDiagnosticInput
import io.signallq.app.feature.diagnostico.FibraSignalQualityEngine
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FibraModemScreen(
    snapshotFibra: SnapshotFibra,
    onVoltar: () -> Unit,
    onRetentar: () -> Unit,
    onAbrirAjustes: () -> Unit,
) {
    val c = LocalLkTokens.current

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Sua internet por fibra",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                        Text(
                            "Modem conectado pela operadora",
                            fontSize = 12.sp,
                            color = c.textTertiary,
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onVoltar) {
                        Icon(
                            Icons.AutoMirrored.Outlined.ArrowBack,
                            contentDescription = "Voltar",
                            tint = c.textPrimary,
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onRetentar) {
                        Icon(
                            Icons.Outlined.Refresh,
                            contentDescription = "Atualizar",
                            tint = c.textPrimary,
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        when (snapshotFibra.estado) {
            EstadoFibra.idle,
            EstadoFibra.conectando,
            -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(40.dp),
                            color = LkColors.accent,
                        )
                        Spacer(Modifier.height(LkSpacing.md))
                        Text("Conectando ao modem...", fontSize = 14.sp, color = c.textSecondary)
                    }
                }
            }

            EstadoFibra.erro -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(padding),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                    ) {
                        Box(
                            modifier =
                                Modifier
                                    .size(80.dp)
                                    .clip(CircleShape)
                                    .background(LkColors.warning.copy(alpha = 0.10f)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                Icons.Outlined.ErrorOutline,
                                contentDescription = stringResource(R.string.cd_fibra_erro_icone),
                                tint = LkColors.warning,
                                modifier = Modifier.size(36.dp),
                            )
                        }
                        Spacer(Modifier.height(LkSpacing.lg))
                        Text(
                            "Não consegui acessar o modem",
                            fontSize = 17.sp,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                        Spacer(Modifier.height(LkSpacing.sm))
                        Text(
                            "Verifique o IP, o usuário e a senha nas configurações do modem.",
                            fontSize = 13.sp,
                            color = c.textSecondary,
                            lineHeight = 19.sp,
                        )
                        Spacer(Modifier.height(LkSpacing.xl))
                        Button(
                            onClick = onRetentar,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                            shape = RoundedCornerShape(LkRadius.button),
                        ) {
                            Text("Tentar novamente", fontSize = 14.sp, fontWeight = FontWeight.W600)
                        }
                        Spacer(Modifier.height(LkSpacing.sm))
                        OutlinedButton(
                            onClick = onAbrirAjustes,
                            modifier = Modifier.fillMaxWidth().height(44.dp),
                            shape = RoundedCornerShape(LkRadius.button),
                        ) {
                            Text("Revisar configurações", fontSize = 14.sp)
                        }
                    }
                }
            }

            EstadoFibra.concluido -> {
                val gpon = snapshotFibra.gpon
                if (gpon == null) {
                    Box(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("Sem dados GPON disponíveis.", fontSize = 14.sp, color = c.textSecondary)
                    }
                } else {
                    FibraConcluidoContent(
                        gpon = gpon,
                        deviceInfo = snapshotFibra.deviceInfo,
                        wifi = snapshotFibra.wifi,
                        wan = snapshotFibra.wan,
                        c = c,
                        modifier = Modifier.padding(padding),
                    )
                }
            }
        }
    }
}

@Composable
private fun FibraConcluidoContent(
    gpon: io.signallq.app.feature.fibra.GponStatus,
    deviceInfo: io.signallq.app.feature.fibra.DeviceInfoFibra?,
    wifi: io.signallq.app.feature.fibra.WifiStatus?,
    wan: io.signallq.app.feature.fibra.WanStatus?,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val fibraInput =
        remember(gpon) {
            FibraDiagnosticInput(
                rxPowerDbm = gpon.rxPowerDbm,
                txPowerDbm = gpon.txPowerDbm,
                temperatureCelsius = gpon.temperatureCelsius,
                isUp = gpon.isUp,
            )
        }
    val interpretacoes = remember(fibraInput) { FibraSignalQualityEngine.avaliar(fibraInput) }

    val temCritico = interpretacoes.any { it.status == DiagnosticStatus.critical }
    val temAtencao = interpretacoes.any { it.status == DiagnosticStatus.attention }

    val (tituloSaude, descSaude, corSaude, badgeLabel) =
        when {
            temCritico ->
                StatusSaude(
                    "Atenção necessária",
                    "Foram identificados problemas na sua conexão de fibra que podem afetar a estabilidade.",
                    LkColors.error,
                    "Problema detectado",
                )
            temAtencao ->
                StatusSaude(
                    "Conexão regular",
                    "Sua internet funciona, mas há pontos que merecem atenção.",
                    LkColors.warning,
                    "Atenção",
                )
            else ->
                StatusSaude(
                    "Conexão saudável",
                    "Sua internet está estável e funcionando bem.",
                    LkColors.success,
                    "Tudo certo",
                )
        }

    // Map diagnostics to friendly cards
    val potenciaResult = interpretacoes.find { it.id.startsWith("FIB-02") }
    val ruidoResult = interpretacoes.find { it.id.startsWith("FIB-03") }
    val conexaoResult =
        interpretacoes.find { it.id.startsWith("FIB-01") }
            ?: interpretacoes.find { it.id.startsWith("FIB-04") }

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Spacer(Modifier.height(LkSpacing.xs))

        // Health header
        Text(
            tituloSaude,
            fontSize = 20.sp,
            fontWeight = FontWeight.W700,
            color = c.textPrimary,
        )
        Text(
            descSaude,
            fontSize = 13.sp,
            color = c.textSecondary,
            lineHeight = 19.sp,
        )
        // Badge
        Row(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(corSaude.copy(alpha = 0.10f))
                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.CheckCircleOutline,
                contentDescription = null,
                tint = corSaude,
                modifier = Modifier.size(14.dp),
            )
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                badgeLabel,
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                color = corSaude,
            )
        }

        Spacer(Modifier.height(LkSpacing.sm))

        // Friendly cards
        if (potenciaResult != null) {
            FibraFriendlyCard(
                icone = Icons.Outlined.SignalCellularAlt,
                titulo = "Potência do sinal",
                descricao = friendlyDesc(potenciaResult),
                badge = friendlyBadge(potenciaResult),
                badgeCor = statusColor(potenciaResult.status),
                iconeCor = LkColors.success,
                c = c,
            )
        }
        if (ruidoResult != null) {
            FibraFriendlyCard(
                icone = Icons.Outlined.SignalCellularAlt,
                titulo = "Ruído na linha",
                descricao = friendlyDescRuido(ruidoResult),
                badge = friendlyBadgeRuido(ruidoResult),
                badgeCor = statusColor(ruidoResult.status),
                iconeCor = LkColors.accent,
                c = c,
            )
        }
        FibraFriendlyCard(
            icone = Icons.Outlined.CheckCircleOutline,
            titulo = "Conexão",
            descricao = if (gpon.isUp) "Ativa e sem quedas" else "Link óptico inativo",
            badge = if (gpon.isUp) "Estável" else "Inativa",
            badgeCor = if (gpon.isUp) LkColors.success else LkColors.error,
            iconeCor = if (gpon.isUp) LkColors.success else LkColors.error,
            c = c,
        )

        // GH#893 — 3 cards novos, mesmo padrao FibraFriendlyCard
        if (deviceInfo != null && deviceInfo.uptimeSeconds > 0) {
            FibraFriendlyCard(
                icone = Icons.Outlined.CheckCircleOutline,
                titulo = "Tempo ligado",
                descricao = "Direto, sem quedas ou reinícios",
                badge = deviceInfo.formatarUptime(),
                badgeCor = LkColors.success,
                iconeCor = LkColors.success,
                c = c,
            )
        }

        wifi?.radios?.forEach { radio ->
            FibraWifiRadioCard(radio = radio, c = c)
        }

        if (wan != null) {
            FibraDnsCard(wan = wan, c = c)
        }

        FibraDetalhesTecnicosDisclosure(gpon = gpon, c = c)

        Spacer(Modifier.height(LkSpacing.lg))
    }
}

/**
 * Bloco colapsado de identificação técnica (tensão/corrente do laser, serial
 * da ONT, modo de conexão) — dado de suporte/diagnóstico, não veredito de
 * saúde. Nenhum destes itens tem threshold calibrado no
 * [FibraSignalQualityEngine], então não recebem badge/cor de status (spec
 * Lia, gap "tensão/corrente/serial nunca aparecem na UI"). Some por
 * completo (nem o header aparece) quando nenhum dos 4 campos tem leitura.
 */
@Composable
private fun FibraDetalhesTecnicosDisclosure(
    gpon: io.signallq.app.feature.fibra.GponStatus,
    c: LkTokens,
) {
    val itens =
        remember(gpon) {
            listOfNotNull(
                gpon.voltageV.takeIf { it > 0.0 }?.let { "Tensão do laser" to "%.2f V".format(it) },
                gpon.laserCurrentMa.takeIf { it > 0.0 }?.let { "Corrente do laser" to "%.1f mA".format(it) },
                gpon.serial
                    .trim()
                    .takeIf { it.isNotBlank() && it != "—" }
                    ?.let { "Número de série" to it },
                gpon.mode
                    .trim()
                    .takeIf { it.isNotBlank() && it != "—" }
                    ?.let { "Modo de conexão" to it },
            )
        }
    if (itens.isEmpty()) return

    var expandido by remember { mutableStateOf(false) }

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgSecondary),
    ) {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
                    .clickable { expandido = !expandido }
                    .padding(horizontal = LkSpacing.md, vertical = LkSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                Icons.Outlined.Info,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                "Detalhes técnicos",
                fontSize = 12.sp,
                fontWeight = FontWeight.W600,
                color = c.textSecondary,
                modifier = Modifier.weight(1f),
            )
            Icon(
                if (expandido) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(18.dp),
            )
        }

        AnimatedVisibility(visible = expandido, enter = expandVertically(), exit = shrinkVertically()) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.md)
                        .padding(bottom = LkSpacing.sm),
            ) {
                itens.forEach { (label, valor) ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(label, fontSize = 12.sp, color = c.textSecondary, modifier = Modifier.weight(1f))
                        Text(
                            valor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                            fontFamily = if (label == "Número de série") FontFamily.Monospace else FontFamily.Default,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun FibraFriendlyCard(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    badge: String,
    badgeCor: Color,
    iconeCor: Color,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(iconeCor.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = iconeCor, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
            Text(descricao, fontSize = 11.sp, color = c.textSecondary, lineHeight = 15.sp)
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Box(
            modifier =
                Modifier
                    .clip(RoundedCornerShape(999.dp))
                    .background(badgeCor.copy(alpha = 0.10f))
                    .padding(horizontal = LkSpacing.sm, vertical = 2.dp),
        ) {
            Text(badge, fontSize = 11.sp, fontWeight = FontWeight.W700, color = badgeCor)
        }
    }
}

/**
 * Card de um radio Wi-Fi (2.4GHz/5GHz) do roteador — GH#893.
 *
 * Rede aberta (`criptografia == "None"`) usa o mesmo tratamento visual de
 * atenção do resto da tela (badge cor `LkColors.warning`, ver `StatusSaude`/
 * `friendlyBadge*`) em vez do "Ativa" neutro — sem senha é uma situação que
 * merece chamar atenção do usuário, não uma confirmação tranquila.
 */
@Composable
private fun FibraWifiRadioCard(
    radio: io.signallq.app.feature.fibra.WifiRadioStatus,
    c: LkTokens,
) {
    val redeAberta = radio.criptografia.equals("None", ignoreCase = true)
    val descricaoCripto = if (redeAberta) "Rede aberta, sem senha" else "Wi-Fi protegida"
    val (badge, badgeCor) =
        when {
            redeAberta -> "Sem senha" to LkColors.warning
            radio.habilitado -> "Ativa" to LkColors.success
            else -> "Inativa" to c.textTertiary
        }
    FibraFriendlyCard(
        icone = Icons.Outlined.Wifi,
        titulo = radio.ssid,
        descricao = "${radio.banda} · Canal ${radio.canal ?: "—"} · $descricaoCripto",
        badge = badge,
        badgeCor = badgeCor,
        iconeCor = if (redeAberta) LkColors.warning else LkColors.accent,
        c = c,
    )
}

/**
 * Card de DNS (Servidor de nomes) — GH#893. IPs primário/secundário aparecem
 * numa legenda discreta abaixo, mesma fonte monoespaçada do disclosure
 * "Detalhes técnicos".
 */
@Composable
private fun FibraDnsCard(
    wan: io.signallq.app.feature.fibra.WanStatus,
    c: LkTokens,
) {
    val configurado = wan.primaryDns.isNotBlank() || wan.secondaryDns.isNotBlank()
    Column(Modifier.fillMaxWidth()) {
        FibraFriendlyCard(
            icone = Icons.Outlined.Dns,
            titulo = "Servidor de nomes",
            descricao = "Ajuda seu Wi-Fi a encontrar os sites mais rápido",
            badge = if (configurado) "Configurado" else "Não configurado",
            badgeCor = if (configurado) LkColors.success else c.textTertiary,
            iconeCor = LkColors.accent,
            c = c,
        )
        if (configurado) {
            Text(
                listOfNotNull(
                    wan.primaryDns.takeIf { it.isNotBlank() },
                    wan.secondaryDns.takeIf { it.isNotBlank() },
                ).joinToString(" · "),
                fontSize = 11.sp,
                color = c.textTertiary,
                fontFamily = FontFamily.Monospace,
                modifier = Modifier.padding(start = 48.dp, top = 2.dp),
            )
        }
    }
}

private data class StatusSaude(
    val titulo: String,
    val descricao: String,
    val cor: Color,
    val badge: String,
)

private fun statusColor(status: DiagnosticStatus): Color =
    when (status) {
        DiagnosticStatus.critical -> LkColors.error
        DiagnosticStatus.attention -> LkColors.warning
        else -> LkColors.success
    }

private fun friendlyDesc(result: io.signallq.app.feature.diagnostico.DiagnosticResult): String =
    when (result.status) {
        DiagnosticStatus.ok -> "Sinal forte chegando até o modem"
        DiagnosticStatus.attention -> "Sinal abaixo do ideal"
        else -> "Sinal muito fraco — pode causar instabilidade"
    }

private fun friendlyBadge(result: io.signallq.app.feature.diagnostico.DiagnosticResult): String =
    when (result.status) {
        DiagnosticStatus.ok -> "Excelente"
        DiagnosticStatus.attention -> "Regular"
        else -> "Fraco"
    }

private fun friendlyDescRuido(result: io.signallq.app.feature.diagnostico.DiagnosticResult): String =
    when (result.status) {
        DiagnosticStatus.ok -> "Pouca interferência detectada"
        DiagnosticStatus.attention -> "Interferência moderada"
        else -> "Interferência alta detectada"
    }

private fun friendlyBadgeRuido(result: io.signallq.app.feature.diagnostico.DiagnosticResult): String =
    when (result.status) {
        DiagnosticStatus.ok -> "Baixo"
        DiagnosticStatus.attention -> "Moderado"
        else -> "Alto"
    }
