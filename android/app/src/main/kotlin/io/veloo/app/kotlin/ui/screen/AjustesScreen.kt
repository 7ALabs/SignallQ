package io.signallq.app.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens

@SuppressLint("InlinedApi")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AjustesScreen(
    perfil: AjustesPerfilState,
    provedor: AjustesProvedorState,
    monitoramento: AjustesMonitoramentoState,
    modem: AjustesModemState,
    temaSelecionado: String,
    onDefinirTemaSelecionado: (String) -> Unit,
    limiteAlertaMbps: Int,
    onSalvarLimiteAlerta: (Int) -> Unit,
    onLimparHistorico: () -> Unit,
    onApagarDadosLocais: () -> Unit,
    onResetarApp: () -> Unit,
    onAbrirHistorico: () -> Unit,
    onAbrirLaudo: () -> Unit,
    onAbrirPerfil: () -> Unit = {},
    onAbrirPrivacidade: () -> Unit = {},
    onAbrirNovidades: () -> Unit = {},
    onAbrirFibra: () -> Unit = {},
    // GH#936 — Fase 7 MD3 (5f): "Monitoramento passivo" + "Análise avançada" saíram de
    // dois toggles inline pra um sheet único (MonitoramentoSheet.kt), aberto tanto por
    // aqui quanto pelo atalho "Monitoramento" no hub Ferramentas — single source, sem
    // reimplementar os toggles neste arquivo.
    onAbrirMonitoramento: () -> Unit = {},
    // GH#930 — Fase 1 MD3: Ajustes deixou de ser tab (agora "Perfil" via overlay, acessado
    // pelo avatar no TopBar das outras telas). Quando não-nulo, mostra botão de fechar.
    onVoltar: (() -> Unit)? = null,
    dadosMoveis: AjustesDadosMoveisState =
        AjustesDadosMoveisState(
            speedtestPermiteHeavyMovel = false,
            speedtestMbConsumidosMes = 0L,
            onSetSpeedtestPermiteHeavyMovel = {},
        ),
) {
    val c = LocalLkTokens.current
    val context = LocalContext.current
    // aliases locais para não explodir o código interno com prefixos
    val deviceName = perfil.deviceName
    val appVersion = perfil.appVersion
    val nomeUsuario = perfil.nomeUsuario
    val fotoUriUsuario = perfil.fotoUriUsuario
    val operadora = provedor.operadora
    val planoInternet = provedor.planoInternet
    val regiao = provedor.regiao
    val estadoUf = provedor.estadoUf
    val cidadeNome = provedor.cidadeNome
    val ispDetectado = provedor.ispDetectado
    val ispConfirmado = provedor.ispConfirmado
    val velocidadeContratadaDownMbps = provedor.velocidadeContratadaDownMbps
    val velocidadeContratadaUpMbps = provedor.velocidadeContratadaUpMbps
    val operadoraAutodetectada = provedor.operadoraAutodetectada
    val monitoramentoAtivo = monitoramento.monitoramentoAtivo
    val modemHost = modem.modemHost
    val modemUsername = modem.modemUsername
    val modemPassword = modem.modemPassword
    val modemPermanecerConectado = modem.modemPermanecerConectado
    val gatewayIpDetectado = modem.gatewayIpDetectado
    val gatewaySessaoValida = modem.gatewaySessaoValida
    val conectarGateway = modem.conectarGateway
    val onGatewayConectado = modem.onGatewayConectado
    val bandasWifiGateway = modem.bandasWifi
    val dispositivosNaRedeGateway = modem.dispositivosNaRede
    // aliases de lambdas — mantém corpo interno sem alteração
    val onSalvarPerfil = perfil.onSalvarPerfil
    val onSalvarDadosProvedor = provedor.onSalvarDadosProvedor
    val onSalvarEstadoCidade = provedor.onSalvarEstadoCidade
    val onConfirmarIsp = provedor.onConfirmarIsp
    val onDispensarBannerIsp = provedor.onDispensarBannerIsp
    val onSalvarVelocidadeContratada = provedor.onSalvarVelocidadeContratada
    val speedtestPermiteHeavyMovel = dadosMoveis.speedtestPermiteHeavyMovel
    val speedtestMbConsumidosMes = dadosMoveis.speedtestMbConsumidosMes
    val onSetSpeedtestPermiteHeavyMovel = dadosMoveis.onSetSpeedtestPermiteHeavyMovel

    // GH#530 — GatewayConnectionSheet (mesmo componente da Home) na linha do roteador.
    var showGatewayConnectionSheet by remember { mutableStateOf(false) }
    var showPerfilSheet by remember { mutableStateOf(false) }
    var showSobreSheet by remember { mutableStateOf(false) }
    var showDadosLocaisSheet by remember { mutableStateOf(false) }
    var showDiagnosticoAppSheet by remember { mutableStateOf(false) }
    var showMinhaConexaoSheet by remember { mutableStateOf(false) }
    // GH#936 — sheet de "Alertas de qualidade", entrada na seção Notificações.
    var showPreferenciasSheet by remember { mutableStateOf(false) }
    // Row "Tema" abria showPreferenciasSheet por engano (sheet de "Alertas de
    // qualidade", sem relação com tema) — ThemeSelector existia pronto mas nunca
    // tinha ponto de entrada. Ver TemaSheet.kt.
    var showTemaSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        stringResource(R.string.ajustes_titulo),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.W600,
                        color = c.textPrimary,
                    )
                },
                actions = {
                    if (onVoltar != null) {
                        IconButton(onClick = onVoltar) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = "Fechar",
                                tint = c.textPrimary,
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
        },
    ) { padding ->
        LazyColumn(
            modifier =
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .background(c.bgPrimary),
        ) {
            // ── HERO CARD ────────────────────────────────────────────────────────────
            item {
                val nomeDisplay = if (nomeUsuario.isNotBlank()) nomeUsuario else "Seu perfil"
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg)
                            .padding(top = LkSpacing.xl, bottom = LkSpacing.md)
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(c.surfaceContainer)
                            .clickable { showPerfilSheet = true }
                            .padding(LkSpacing.lg),
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        UserAvatar(
                            fotoUri = fotoUriUsuario,
                            fallbackInitial = nomeUsuario.firstOrNull(),
                            size = 52.dp,
                        )
                        Spacer(Modifier.width(LkSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nomeDisplay,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = "Ver perfil",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                            contentDescription = null,
                            tint = c.textTertiary,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
            }

            item { SectionHeader(stringResource(R.string.ajustes_minha_conexao), c) }
            item {
                SettingsSectionCard(c = c) {
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Business,
                        label = "Operadora",
                        value = operadora.ifBlank { "—" },
                        onClick = { showMinhaConexaoSheet = true },
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Speed,
                        label = "Plano contratado",
                        value =
                            when {
                                velocidadeContratadaDownMbps > 0 && velocidadeContratadaUpMbps > 0 ->
                                    "$velocidadeContratadaDownMbps Mbps"
                                planoInternet.isNotBlank() -> planoInternet
                                else -> "—"
                            },
                        onClick = { showMinhaConexaoSheet = true },
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Router,
                        label = "Cidade",
                        value =
                            when {
                                cidadeNome.isNotBlank() && estadoUf.isNotBlank() -> "$cidadeNome, $estadoUf"
                                regiao.isNotBlank() -> regiao
                                else -> "—"
                            },
                        onClick = { showMinhaConexaoSheet = true },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }

            item { SectionHeader("Aparência", c) }
            item {
                SettingsSectionCard(c = c) {
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.DarkMode,
                        label = "Tema",
                        value = temaLabel(temaSelecionado),
                        onClick = { showTemaSheet = true },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }

            item { SectionHeader("Notificações", c) }
            item {
                SettingsSectionCard(c = c) {
                    // GH#936 — PreferenciasSheet ficou órfã depois que "Tema" passou a abrir
                    // showTemaSheet (PR #1032). Entrada correta é aqui, não em Aparência.
                    SimpleSettingRow(
                        c = c,
                        icon = Icons.Outlined.Notifications,
                        label = "Alertas de qualidade",
                        onClick = { showPreferenciasSheet = true },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }

            item { SectionHeader("Dados e privacidade", c) }
            item {
                SettingsSectionCard(c = c) {
                    SimpleSettingRow(
                        c = c,
                        icon = Icons.Outlined.Lock,
                        label = "Privacidade",
                        onClick = onAbrirPrivacidade,
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    SimpleSettingRow(
                        c = c,
                        icon = Icons.Outlined.Delete,
                        label = "Gerenciar dados",
                        onClick = { showDadosLocaisSheet = true },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }

            item { SectionHeader("Sobre", c) }
            item {
                SettingsSectionCard(c = c) {
                    SimpleSettingRow(
                        c = c,
                        icon = Icons.Outlined.NewReleases,
                        label = "Novidades",
                        onClick = onAbrirNovidades,
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Info,
                        label = "Sobre o SignallQ",
                        value = "v$appVersion",
                        onClick = { showSobreSheet = true },
                    )
                }
            }
            item {
                Spacer(
                    Modifier
                        .navigationBarsPadding()
                        .height(24.dp),
                )
            }
        }
    }

    // ── Bottom sheets & dialogs ───────────────────────────────────────────────

    if (showGatewayConnectionSheet) {
        GatewayConnectionSheet(
            ipInicial = modemHost ?: gatewayIpDetectado,
            usuarioInicial = modemUsername,
            senhaInicial = modemPassword,
            lembrarSenhaInicial = modemUsername.isNotBlank() || modemPassword.isNotBlank(),
            manterConectadoInicial = modemPermanecerConectado,
            onDismissRequest = { showGatewayConnectionSheet = false },
            conectar = conectarGateway,
            onConectado = onGatewayConectado,
        )
    }

    if (showPerfilSheet) {
        PerfilEditSheet(
            c = c,
            nomeAtual = nomeUsuario,
            fotoUriAtual = fotoUriUsuario,
            deviceName = deviceName,
            appVersion = appVersion,
            onDismiss = { showPerfilSheet = false },
            onSalvar = { nome, fotoUri ->
                onSalvarPerfil(nome, fotoUri)
                showPerfilSheet = false
            },
        )
    }

    if (showSobreSheet) {
        SobreSheet(
            c = c,
            appVersion = appVersion,
            onDismiss = { showSobreSheet = false },
        )
    }

    if (showDadosLocaisSheet) {
        DadosLocaisSheet(
            c = c,
            onDismiss = { showDadosLocaisSheet = false },
            onLimparHistorico = onLimparHistorico,
            onApagarDadosLocais = onApagarDadosLocais,
            onResetarApp = onResetarApp,
        )
    }

    if (showDiagnosticoAppSheet) {
        DiagnosticoAppSheet(
            c = c,
            appVersion = appVersion,
            onDismiss = { showDiagnosticoAppSheet = false },
        )
    }

    if (showMinhaConexaoSheet) {
        MinhaConexaoSheet(
            operadora = operadora,
            estadoUf = estadoUf,
            cidadeNome = cidadeNome,
            velocidadeContratadaDownMbps = velocidadeContratadaDownMbps,
            velocidadeContratadaUpMbps = velocidadeContratadaUpMbps,
            operadoraAutodetectada = operadoraAutodetectada,
            onSalvar = { op, uf, cidade, down, up ->
                onSalvarDadosProvedor(op, planoInternet, regiao)
                onSalvarEstadoCidade(uf, cidade)
                onSalvarVelocidadeContratada(down, up)
            },
            onDismiss = { showMinhaConexaoSheet = false },
        )
    }

    if (showPreferenciasSheet) {
        PreferenciasSheet(
            c = c,
            limiteAtual = limiteAlertaMbps,
            onDismiss = { showPreferenciasSheet = false },
            onSalvar = { limite ->
                onSalvarLimiteAlerta(limite)
                showPreferenciasSheet = false
            },
        )
    }

    if (showTemaSheet) {
        TemaSheet(
            c = c,
            temaSelecionado = temaSelecionado,
            onSelecionarTema = onDefinirTemaSelecionado,
            onDismiss = { showTemaSheet = false },
        )
    }
}

@Composable
private fun SettingsSectionCard(
    c: LkTokens,
    content: @Composable ColumnScope.() -> Unit,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg)
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer),
        content = content,
    )
}

@Composable
private fun SimpleSettingRow(
    c: LkTokens,
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
) {
    ValueSettingRow(c = c, icon = icon, label = label, value = null, onClick = onClick)
}

@Composable
private fun ValueSettingRow(
    c: LkTokens,
    icon: ImageVector,
    label: String,
    value: String?,
    onClick: () -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(horizontal = LkSpacing.lg, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(c.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(19.dp),
            )
        }
        Spacer(Modifier.width(LkSpacing.lg))
        Text(
            text = label,
            style = MaterialTheme.typography.bodyLarge,
            color = c.textPrimary,
            modifier = Modifier.weight(1f),
        )
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.width(LkSpacing.sm))
        }
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
            contentDescription = null,
            tint = c.textTertiary,
            modifier = Modifier.size(14.dp),
        )
    }
}

private fun temaLabel(valor: String): String =
    when (valor.lowercase()) {
        "claro" -> "Claro"
        "escuro" -> "Escuro"
        else -> "Sistema"
    }

@Composable
private fun SectionHeader(
    titulo: String,
    c: LkTokens,
) {
    Text(
        text = titulo.uppercase(),
        style = MaterialTheme.typography.labelMedium,
        fontWeight = FontWeight.W600,
        color = c.textTertiary,
        letterSpacing = 0.8.sp,
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp).padding(top = 8.dp),
    )
}

@Composable
private fun SettingItem(
    c: LkTokens,
    icon: ImageVector,
    label: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    badge: String? = null,
    tintError: Boolean = false,
) {
    val iconTint = if (tintError) LkColors.error else c.primary
    val labelColor = if (tintError) LkColors.error else c.textPrimary

    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(LkRadius.input))
                    .background(c.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (tintError) LkColors.error else c.primary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(LkSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = labelColor, fontWeight = FontWeight.W500)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }

        if (badge != null) {
            Box(
                modifier =
                    Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(c.border)
                        .padding(horizontal = LkSpacing.sm, vertical = 4.dp),
            ) {
                // Contraste WCAG AA (GH#937): textTertiary sobre chip com fundo c.border
                // dava ~2:1 (fail) e textSecondary ~3.9:1 (ainda abaixo do AA p/ 10sp);
                // textPrimary garante AA/AAA. Path hoje sem call site com badge != null.
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
            }
        } else {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                contentDescription = null,
                tint = if (onClick != null) c.textTertiary else c.border,
                modifier = Modifier.size(14.dp),
            )
        }
    }
}

@Composable
internal fun ToggleItem(
    c: LkTokens,
    icon: ImageVector,
    label: String,
    subtitle: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(LkSpacing.lg),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(LkRadius.input))
                    .background(c.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = c.primary,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(LkSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.titleSmall, color = c.textPrimary, fontWeight = FontWeight.W500)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = c.textSecondary)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = c.primary,
                    uncheckedThumbColor = c.textTertiary,
                    uncheckedTrackColor = c.border,
                ),
        )
    }
}

// ─── Diagnóstico do app sheet ─────────────────────────────────────────────────
// Fora do escopo 6a-6f (item "Diagnóstico do app" não faz parte da reorganização
// Perfil/Ajustes da Fase 7) — mantido aqui por enquanto.

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticoAppSheet(
    c: LkTokens,
    appVersion: String,
    onDismiss: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = {},
        containerColor = c.bgSecondary,
    ) {
        Column(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
        ) {
            Box(
                modifier =
                    Modifier
                        .width(40.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(c.border)
                        .align(Alignment.CenterHorizontally)
                        .semantics { contentDescription = "Arrastar para fechar" },
            )
            Spacer(Modifier.height(LkSpacing.md))
            Text(
                text = "Diagnóstico do app",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
            )
            Spacer(Modifier.height(LkSpacing.md))
            InfoRow(c, "Versão", "v$appVersion")
            HorizontalDivider(color = c.border, thickness = 1.dp)
            InfoRow(c, "Plataforma", "Android · Kotlin + Compose")
            HorizontalDivider(color = c.border, thickness = 1.dp)
            InfoRow(c, "Integridade", "OK")
            HorizontalDivider(color = c.border, thickness = 1.dp)
            InfoRow(c, "Assinatura", "Verificada")
            HorizontalDivider(color = c.border, thickness = 1.dp)
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .padding(horizontal = LkSpacing.lg)
                        .padding(top = LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.xs),
            ) {
                Icon(
                    imageVector = Icons.Outlined.VerifiedUser,
                    contentDescription = null,
                    tint = LkColors.success,
                    modifier = Modifier.size(16.dp),
                )
                Text(
                    text = "Binários íntegros · Nenhuma anomalia detectada",
                    style = MaterialTheme.typography.bodySmall,
                    color = LkColors.success,
                )
            }
            Spacer(Modifier.height(LkSpacing.md))
        }
    }
}

// ─── Roteador sheet (GH#526/#530): ver GatewayConnectionSheet.kt (componente único
// reaproveitado aqui e no nó do gateway em HomeScreen.kt) ──────────────────────
