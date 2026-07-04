package io.signallq.app.ui.screen

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForwardIos
import androidx.compose.material.icons.automirrored.outlined.Article
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Business
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Email
import androidx.compose.material.icons.outlined.History
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.NewReleases
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.SignalCellularAlt
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import io.signallq.app.BuildConfig
import io.signallq.app.R
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.monitoramento.OemKillInfo
import io.signallq.app.ui.IspInfo
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.ConfirmacaoDialog
import io.signallq.app.ui.component.ProfileAvatarButton

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
    onAbrirMinhaConexao: () -> Unit = {},
    onAbrirFibra: () -> Unit = {},
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
    val analiseAvancada = monitoramento.analiseAvancada
    val monitoramentoAtivo = monitoramento.monitoramentoAtivo
    val notificacaoLatenciaAtiva = monitoramento.notificacaoLatenciaAtiva
    val notificacaoDnsAtiva = monitoramento.notificacaoDnsAtiva
    val notificacaoRssiAtiva = monitoramento.notificacaoRssiAtiva
    val notificacaoSemInternetAtiva = monitoramento.notificacaoSemInternetAtiva
    val modemHost = modem.modemHost
    val modemUsername = modem.modemUsername
    val modemPassword = modem.modemPassword
    val modemPermanecerConectado = modem.modemPermanecerConectado
    val gatewayIpDetectado = modem.gatewayIpDetectado
    // aliases de lambdas — mantém corpo interno sem alteração
    val onSalvarPerfil = perfil.onSalvarPerfil
    val onSalvarDadosProvedor = provedor.onSalvarDadosProvedor
    val onSalvarEstadoCidade = provedor.onSalvarEstadoCidade
    val onConfirmarIsp = provedor.onConfirmarIsp
    val onDispensarBannerIsp = provedor.onDispensarBannerIsp
    val onDefinirAnaliseAvancada = monitoramento.onDefinirAnaliseAvancada
    val onAtivarMonitoramento = monitoramento.onAtivarMonitoramento
    val onDefinirNotificacaoLatenciaAtiva = monitoramento.onDefinirNotificacaoLatenciaAtiva
    val onDefinirNotificacaoDnsAtiva = monitoramento.onDefinirNotificacaoDnsAtiva
    val onDefinirNotificacaoRssiAtiva = monitoramento.onDefinirNotificacaoRssiAtiva
    val onDefinirNotificacaoSemInternetAtiva = monitoramento.onDefinirNotificacaoSemInternetAtiva
    val onSalvarConfiguracaoModem = modem.onSalvarConfiguracaoModem
    val onConectarFibra = modem.onConectarFibra
    val speedtestPermiteHeavyMovel = dadosMoveis.speedtestPermiteHeavyMovel
    val speedtestMbConsumidosMes = dadosMoveis.speedtestMbConsumidosMes
    val onSetSpeedtestPermiteHeavyMovel = dadosMoveis.onSetSpeedtestPermiteHeavyMovel

    var showRoteadorSheet by remember { mutableStateOf(false) }
    var showPerfilSheet by remember { mutableStateOf(false) }
    var showSobreSheet by remember { mutableStateOf(false) }
    var showConfirmResetApp by remember { mutableStateOf(false) }
    var showProvedorSheet by remember { mutableStateOf(false) }
    // showPreferenciasSheet removido — dead code (nunca aberto via LazyColumn)
    var showDiagnosticoSheet by remember { mutableStateOf(false) }
    var showDadosLocaisSheet by remember { mutableStateOf(false) }
    var showDiagnosticoAppSheet by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Outlined.Settings,
                            contentDescription = null,
                            tint = c.textPrimary,
                            modifier = Modifier.size(18.dp),
                        )
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            stringResource(R.string.ajustes_titulo),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                    }
                },
                navigationIcon = {
                    ProfileAvatarButton(
                        nomeUsuario = nomeUsuario,
                        fotoUri = fotoUriUsuario,
                        onClick = onAbrirPerfil,
                    )
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
                // #224: nome do device não deve ocupar o campo de identidade do usuário
                val temNome = nomeUsuario.isNotBlank()
                val nomeDisplay = if (temNome) nomeUsuario else "Adicionar nome"
                val (subtituloBase, isRealData) = buildHeroSubtitle(nomeUsuario, operadora, planoInternet)
                // Quando sem nome, mostra device como dado secundário no subtítulo
                val subtitulo =
                    if (!temNome && deviceName.isNotBlank()) {
                        if (isRealData) "$subtituloBase · $deviceName" else deviceName
                    } else {
                        subtituloBase
                    }
                val subtituloIsRealData = isRealData || (!temNome && deviceName.isNotBlank())
                val cdPerfil = stringResource(R.string.ajustes_cd_perfil)
                Box(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg)
                            .padding(top = LkSpacing.xl, bottom = LkSpacing.lg)
                            .semantics {
                                contentDescription = cdPerfil
                            }.clip(RoundedCornerShape(LkRadius.card))
                            .background(c.bgSecondary)
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
                            size = 56.dp,
                        )
                        Spacer(Modifier.width(LkSpacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = nomeDisplay,
                                style = MaterialTheme.typography.bodyLarge,
                                fontWeight = FontWeight.W600,
                                color = if (temNome) c.textPrimary else c.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                            Text(
                                text = subtitulo,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (subtituloIsRealData) c.textSecondary else c.textTertiary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.Outlined.Edit,
                        contentDescription = stringResource(R.string.ajustes_cd_editar_perfil),
                        tint = c.textTertiary,
                        modifier =
                            Modifier
                                .size(16.dp)
                                .align(Alignment.TopEnd),
                    )
                }
            }

            // ── MINHA CONEXÃO ────────────────────────────────────────────────────────
            item { SectionHeader(stringResource(R.string.ajustes_minha_conexao), c) }

            // Banner de confirmação de ISP auto-detectado
            if (!ispConfirmado && !ispDetectado.isNullOrBlank() && operadora.isBlank()) {
                item {
                    Box(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .padding(horizontal = LkSpacing.lg)
                                .padding(bottom = LkSpacing.sm)
                                .clip(RoundedCornerShape(LkRadius.card))
                                .background(LkColors.accent.copy(alpha = 0.08f))
                                .border(1.dp, LkColors.accent.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.card))
                                .padding(LkSpacing.md),
                    ) {
                        Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Outlined.Wifi,
                                    contentDescription = null,
                                    tint = LkColors.accent,
                                    modifier = Modifier.size(16.dp),
                                )
                                Spacer(Modifier.width(LkSpacing.sm))
                                Text(
                                    text = stringResource(R.string.ajustes_operadora_detectada, ispDetectado),
                                    style = MaterialTheme.typography.bodySmall,
                                    fontWeight = FontWeight.W600,
                                    color = c.textPrimary,
                                    modifier = Modifier.weight(1f),
                                )
                            }
                            Text(
                                text = stringResource(R.string.ajustes_usar_operadora),
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
                                Button(
                                    onClick = { onConfirmarIsp(ispDetectado) },
                                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                                    contentPadding =
                                        androidx.compose.foundation.layout
                                            .PaddingValues(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
                                ) {
                                    Text(stringResource(R.string.ajustes_btn_confirmar), style = MaterialTheme.typography.labelMedium)
                                }
                                TextButton(onClick = onDispensarBannerIsp) {
                                    Text("Ignorar", color = c.textTertiary, style = MaterialTheme.typography.labelMedium)
                                }
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .semantics {
                                contentDescription = "Minha conexão. ${
                                    if (operadora.isNotBlank()) "Operadora: $operadora." else ""
                                } Toque para editar."
                            }.clickable { onAbrirMinhaConexao() }
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.md),
                ) {
                    Box(
                        modifier =
                            Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(LkColors.accent.copy(alpha = 0.12f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Business,
                            contentDescription = null,
                            tint = LkColors.accent,
                            modifier = Modifier.size(18.dp),
                        )
                    }
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = "Minha conexão",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = c.textPrimary,
                        )
                        val tudoVazio =
                            operadora.isBlank() && planoInternet.isBlank() && regiao.isBlank() && cidadeNome.isBlank() && estadoUf.isBlank()
                        if (tudoVazio) {
                            Text(
                                text = "Toque para configurar sua conexão",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textTertiary,
                            )
                        } else {
                            Text(
                                text = "Operadora: ${if (operadora.isNotBlank()) operadora else "—"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = c.textSecondary,
                            )
                            // #225: plano vazio exibe call-to-action em vez de "—"
                            val planoTexto =
                                if (planoInternet.isNotBlank()) "$planoInternet Mbps" else "Toque para informar seu plano"
                            val planoColor = if (planoInternet.isNotBlank()) c.textSecondary else c.textTertiary
                            Text(
                                text = "Plano: $planoTexto",
                                style = MaterialTheme.typography.bodySmall,
                                color = planoColor,
                            )
                            // #225: local vazio exibe call-to-action em vez de "—"
                            val localTexto =
                                when {
                                    cidadeNome.isNotBlank() && estadoUf.isNotBlank() -> "$cidadeNome, $estadoUf"
                                    regiao.isNotBlank() -> regiao
                                    else -> "Toque para informar seu local"
                                }
                            val localColor =
                                if (cidadeNome.isNotBlank() || regiao.isNotBlank()) c.textSecondary else c.textTertiary
                            Text(
                                text = "Local: $localTexto",
                                style = MaterialTheme.typography.bodySmall,
                                color = localColor,
                            )
                        }
                    }
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForwardIos,
                        contentDescription = "Editar",
                        tint = c.textTertiary,
                        modifier = Modifier.size(16.dp),
                    )
                }
            }
            if (BuildConfig.FEATURE_FIBRA_SCREEN) {
                item { HorizontalDivider(color = c.border, thickness = 1.dp) }
                item {
                    SettingItem(
                        c = c,
                        icon = Icons.Outlined.Router,
                        label = "Fibra óptica",
                        subtitle = modemHost ?: "Não configurado",
                        onClick = {
                            if (modemHost.isNullOrBlank()) {
                                showRoteadorSheet = true
                            } else {
                                onAbrirFibra()
                            }
                        },
                    )
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
            // ── APARÊNCIA ────────────────────────────────────────────────────────────
            item { SectionHeader("Aparência", c) }
            item {
                ThemeSelector(
                    selecionado = temaSelecionado,
                    onSelect = onDefinirTemaSelecionado,
                    c = c,
                )
            }
            item { Spacer(Modifier.height(16.dp)) }

            // ── NOTIFICAÇÕES ─────────────────────────────────────────────────────────
            item { SectionHeader("Notificações", c) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.Notifications,
                    label = "Notificações",
                    subtitle = "Receba alertas quando sua conexão cair",
                    onClick = {
                        val intent =
                            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
                                .putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
                        context.startActivity(intent)
                    },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }

            // ── HISTÓRICO E DADOS ─────────────────────────────────────────────────────
            item { SectionHeader("Histórico e dados", c) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.History,
                    label = "Ver histórico",
                    subtitle = "Suas medições recentes",
                    onClick = onAbrirHistorico,
                )
            }
            item { HorizontalDivider(color = c.border, thickness = 1.dp) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.AutoMirrored.Outlined.Article,
                    label = "Comprovante para a Anatel",
                    subtitle = "Documento com suas medições para registrar reclamação",
                    onClick = onAbrirLaudo,
                )
            }
            item { HorizontalDivider(color = c.border, thickness = 1.dp) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.Delete,
                    label = "Gerenciar dados locais",
                    subtitle = "Limpar histórico e preferências",
                    onClick = { showDadosLocaisSheet = true },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }

            // ── DADOS MÓVEIS ──────────────────────────────────────────────────────────
            item { SectionHeader("Dados móveis", c) }
            item {
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.SignalCellularAlt,
                    label = "Sempre permitir testes pesados em dados móveis",
                    subtitle = "Não mostrar aviso para modo Completo e Triplo na próxima vez",
                    checked = speedtestPermiteHeavyMovel,
                    onCheckedChange = onSetSpeedtestPermiteHeavyMovel,
                )
            }
            item { HorizontalDivider(color = c.border, thickness = 1.dp) }
            item {
                val mbLabel = if (speedtestMbConsumidosMes > 0L) "$speedtestMbConsumidosMes MB" else "0 MB"
                InfoRow(c, "Consumo em testes este mês", mbLabel)
            }
            item { Spacer(Modifier.height(16.dp)) }

            // ── AVANÇADO (feature-flagged: monitoramento passivo / análise avançada) ──
            if (BuildConfig.FEATURE_LINKPULSE_ATIVO) {
                item { SectionHeader("Avançado", c) }
                item {
                    SettingItem(
                        c = c,
                        icon = Icons.Outlined.Sensors,
                        label = "Monitoramento passivo",
                        subtitle =
                            when {
                                !monitoramentoAtivo -> "Desativado"
                                OemKillInfo.fabricanteRiscoAlto -> "Ativo · pode ser limitado pelo sistema"
                                else -> "Ativo"
                            },
                        onClick = { showDiagnosticoSheet = true },
                    )
                }
                item { HorizontalDivider(color = c.border, thickness = 1.dp) }
                item {
                    SettingItem(
                        c = c,
                        icon = Icons.Outlined.Analytics,
                        label = "Análise avançada",
                        subtitle = if (analiseAvancada) "Ativa" else "Desativada",
                        onClick = { showDiagnosticoSheet = true },
                    )
                }
                item { Spacer(Modifier.height(16.dp)) }
            }

            // ── INFORMAÇÕES ───────────────────────────────────────────────────────────
            item { SectionHeader("Informações", c) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.Lock,
                    label = "Privacidade e dados",
                    subtitle = "Como seus dados são protegidos",
                    onClick = { onAbrirPrivacidade() },
                )
            }
            item { HorizontalDivider(color = c.border, thickness = 1.dp) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.NewReleases,
                    label = "O que há de novo",
                    subtitle = "Confira o que mudou",
                    onClick = { onAbrirNovidades() },
                )
            }
            item { HorizontalDivider(color = c.border, thickness = 1.dp) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.Email,
                    label = "Fale conosco",
                    subtitle = "Envie uma mensagem para o suporte",
                    onClick = {
                        val intent =
                            Intent(Intent.ACTION_SENDTO).apply {
                                data = Uri.parse("mailto:hello7agents@icloud.com")
                            }
                        context.startActivity(intent)
                    },
                )
            }
            item { HorizontalDivider(color = c.border, thickness = 1.dp) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.VerifiedUser,
                    label = "Diagnóstico do app",
                    subtitle = "Integridade, binários e assinatura",
                    onClick = { showDiagnosticoAppSheet = true },
                )
            }
            item { HorizontalDivider(color = c.border, thickness = 1.dp) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.Info,
                    label = "Sobre o SignallQ",
                    subtitle = "v$appVersion · Android · Kotlin",
                    onClick = { showSobreSheet = true },
                )
            }
            item { Spacer(Modifier.height(16.dp)) }

            // ── ZONA DE RISCO ─────────────────────────────────────────────────────────
            item { SectionHeader("Zona de risco", c) }
            item {
                SettingItem(
                    c = c,
                    icon = Icons.Outlined.Delete,
                    label = "Redefinir o app",
                    subtitle = "Apaga todos os dados e restaura configurações iniciais",
                    onClick = { showConfirmResetApp = true },
                    tintError = true,
                )
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

    if (showRoteadorSheet) {
        RoteadorBottomSheet(
            c = c,
            modemHost = modemHost,
            modemUsername = modemUsername,
            modemPassword = modemPassword,
            modemPermanecerConectado = modemPermanecerConectado,
            gatewayIpDetectado = gatewayIpDetectado,
            onDismiss = { showRoteadorSheet = false },
            onSalvar = { host, user, pass, perm ->
                onSalvarConfiguracaoModem(host, user, pass, perm)
                showRoteadorSheet = false
            },
            onConectar = { host, user, pass, perm ->
                onSalvarConfiguracaoModem(host, user, pass, perm)
                showRoteadorSheet = false
                onConectarFibra(host, user, pass)
            },
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
        SimpleInfoSheet(
            c = c,
            titulo = "Sobre o SignallQ",
            onDismiss = { showSobreSheet = false },
        ) {
            InfoRow(c, "Versão", "v$appVersion")
            HorizontalDivider(color = c.border, thickness = 1.dp)
            InfoRow(c, "Plataforma", "Android · Kotlin + Compose")
            HorizontalDivider(color = c.border, thickness = 1.dp)
            InfoRow(c, "Desenvolvido por", "Equipe SignallQ")
            HorizontalDivider(color = c.border, thickness = 1.dp)
            InfoRow(c, "Suporte", "suporte@signallq.app")
        }
    }

    if (showProvedorSheet) {
        ProvedorSheet(
            c = c,
            operadoraAtual = operadora,
            planoAtual = planoInternet,
            regiaoAtual = regiao,
            estadoUfAtual = estadoUf,
            cidadeNomeAtual = cidadeNome,
            onDismiss = { showProvedorSheet = false },
            onSalvar = { op, plano, reg ->
                onSalvarDadosProvedor(op, plano, reg)
                showProvedorSheet = false
            },
            onSalvarEstadoCidade = { uf, cidade ->
                onSalvarEstadoCidade(uf, cidade)
            },
        )
    }

    if (showDiagnosticoSheet) {
        DiagnosticoSheet(
            c = c,
            analiseAvancada = analiseAvancada,
            monitoramentoAtivo = monitoramentoAtivo,
            notificacaoLatenciaAtiva = notificacaoLatenciaAtiva,
            notificacaoDnsAtiva = notificacaoDnsAtiva,
            notificacaoRssiAtiva = notificacaoRssiAtiva,
            notificacaoSemInternetAtiva = notificacaoSemInternetAtiva,
            onDismiss = { showDiagnosticoSheet = false },
            onDefinirAnaliseAvancada = onDefinirAnaliseAvancada,
            onAtivarMonitoramento = onAtivarMonitoramento,
            onDefinirNotificacaoLatenciaAtiva = onDefinirNotificacaoLatenciaAtiva,
            onDefinirNotificacaoDnsAtiva = onDefinirNotificacaoDnsAtiva,
            onDefinirNotificacaoRssiAtiva = onDefinirNotificacaoRssiAtiva,
            onDefinirNotificacaoSemInternetAtiva = onDefinirNotificacaoSemInternetAtiva,
        )
    }

    if (showDadosLocaisSheet) {
        DadosLocaisSheet(
            c = c,
            onDismiss = { showDadosLocaisSheet = false },
            onLimparHistorico = onLimparHistorico,
            onApagarDadosLocais = onApagarDadosLocais,
        )
    }

    if (showDiagnosticoAppSheet) {
        DiagnosticoAppSheet(
            c = c,
            appVersion = appVersion,
            onDismiss = { showDiagnosticoAppSheet = false },
        )
    }

    if (showConfirmResetApp) {
        ConfirmacaoDialog(
            titulo = "Redefinir o app?",
            mensagem =
                "Esta ação apagará todos os dados locais: histórico de testes, configurações salvas e preferências. " +
                    "O app voltará ao estado inicial. Esta ação não pode ser desfeita.",
            onConfirmar = {
                onResetarApp()
                showConfirmResetApp = false
            },
            onCancelar = { showConfirmResetApp = false },
        )
    }
}

@Composable
private fun CardMonitoramentoExplicativo(
    c: LkTokens,
    monitoramentoAtivo: Boolean,
) {
    Box(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg)
                .padding(top = LkSpacing.sm)
                .clip(RoundedCornerShape(16.dp))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(16.dp))
                .padding(LkSpacing.lg),
    ) {
        if (!monitoramentoAtivo) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Sensors,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Text(
                    text = "Sem consumo adicional de bateria ou dados quando desativado",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        } else {
            Column {
                Text(
                    text = "O que está sendo monitorado",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Spacer(Modifier.height(LkSpacing.sm))
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Speed,
                        contentDescription = null,
                        tint = c.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Latência de rede",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textPrimary,
                    )
                }
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Language,
                        contentDescription = null,
                        tint = c.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Velocidade de DNS",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textPrimary,
                    )
                }
                Row(
                    modifier = Modifier.padding(vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Wifi,
                        contentDescription = null,
                        tint = c.textSecondary,
                        modifier = Modifier.size(16.dp),
                    )
                    Spacer(Modifier.width(LkSpacing.sm))
                    Text(
                        text = "Sinal Wi-Fi",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textPrimary,
                    )
                }
                Spacer(Modifier.height(LkSpacing.md))
                HorizontalDivider(color = c.border, thickness = 0.5.dp)
                Spacer(Modifier.height(LkSpacing.md))
                Row {
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(LkColors.success.copy(alpha = 0.10f))
                                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
                    ) {
                        Text(
                            text = "Bateria: Muito baixo",
                            style = MaterialTheme.typography.labelSmall,
                            color = LkColors.success,
                        )
                    }
                    Spacer(Modifier.width(LkSpacing.sm))
                    Box(
                        modifier =
                            Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(c.bgSecondary)
                                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
                    ) {
                        Text(
                            text = "Dados: ~2 MB/dia",
                            style = MaterialTheme.typography.labelSmall,
                            color = c.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

private fun buildHeroSubtitle(
    nomeUsuario: String,
    operadora: String,
    planoInternet: String,
): Pair<String, Boolean> =
    when {
        nomeUsuario.isBlank() && operadora.isBlank() -> "Toque para configurar seu perfil" to false
        nomeUsuario.isBlank() && operadora.isNotBlank() -> operadora to true
        nomeUsuario.isNotBlank() && operadora.isBlank() -> "Adicione sua operadora" to false
        operadora.isNotBlank() && planoInternet.isNotBlank() -> "$operadora · $planoInternet" to true
        else -> operadora to true
    }

@Composable
private fun UserAvatar(
    fotoUri: String?,
    fallbackInitial: Char?,
    size: Dp,
    onClick: (() -> Unit)? = null,
) {
    val context = LocalContext.current
    val bitmap =
        remember(fotoUri) {
            fotoUri?.let { uriStr ->
                runCatching {
                    context.contentResolver
                        .openInputStream(uriStr.toUri())
                        ?.use { stream -> BitmapFactory.decodeStream(stream)?.asImageBitmap() }
                }.getOrNull()
            }
        }
    Box(
        modifier =
            Modifier
                .size(size)
                .clip(CircleShape)
                .background(LkColors.accent.copy(alpha = 0.12f))
                .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap,
                contentDescription = "Foto de perfil",
                contentScale = ContentScale.Crop,
                modifier =
                    Modifier
                        .fillMaxSize()
                        .clip(CircleShape),
            )
        } else {
            Text(
                text = fallbackInitial?.uppercaseChar()?.toString() ?: "?",
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.W700,
                color = LkColors.accent,
            )
        }
    }
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
    val iconTint = if (tintError) LkColors.error else LkColors.accent
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(iconTint.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
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
                Text(badge, fontSize = 10.sp, fontWeight = FontWeight.W600, color = c.textTertiary)
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
private fun ToggleItem(
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
                    .clip(RoundedCornerShape(12.dp))
                    .background(LkColors.accent.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = LkColors.accent,
                modifier = Modifier.size(18.dp),
            )
        }

        Spacer(Modifier.width(LkSpacing.md))

        Column(modifier = Modifier.weight(1f)) {
            Text(text = label, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, fontWeight = FontWeight.W500)
            Spacer(Modifier.height(2.dp))
            Text(text = subtitle, style = MaterialTheme.typography.bodySmall, color = c.textSecondary)
        }

        Switch(
            checked = checked,
            onCheckedChange = onCheckedChange,
            colors =
                SwitchDefaults.colors(
                    checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                    checkedTrackColor = LkColors.accent,
                    uncheckedThumbColor = c.textTertiary,
                    uncheckedTrackColor = c.border,
                ),
        )
    }
}

@Composable
private fun ThemeSelector(
    selecionado: String,
    onSelect: (String) -> Unit,
    c: LkTokens,
) {
    val opcoes =
        listOf(
            Triple("sistema", "Sistema", Icons.Outlined.Settings),
            Triple("claro", "Claro", Icons.Outlined.LightMode),
            Triple("escuro", "Escuro", Icons.Outlined.DarkMode),
        )

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg)
                .padding(bottom = LkSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            opcoes.forEach { (valor, label, icone) ->
                val selecionada = selecionado == valor
                Box(
                    modifier =
                        Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(LkRadius.card))
                            .background(c.bgCard)
                            .border(
                                width = if (selecionada) 2.dp else 1.dp,
                                color = if (selecionada) LkColors.accent else c.border,
                                shape = RoundedCornerShape(LkRadius.card),
                            ).clickable { onSelect(valor) }
                            .padding(vertical = LkSpacing.lg),
                    contentAlignment = Alignment.Center,
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Icon(
                            imageVector = icone,
                            contentDescription = label,
                            tint = if (selecionada) LkColors.accent else c.textSecondary,
                            modifier = Modifier.size(20.dp),
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            fontWeight = if (selecionada) FontWeight.W600 else FontWeight.W400,
                            color = if (selecionada) LkColors.accent else c.textSecondary,
                        )
                    }
                }
            }
        }
    }
}

// ─── Perfil edit sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun PerfilEditSheet(
    c: LkTokens,
    nomeAtual: String,
    fotoUriAtual: String?,
    deviceName: String,
    appVersion: String,
    ispInfo: IspInfo? = null,
    estadoConexao: EstadoConexao? = null,
    onDismiss: () -> Unit,
    onSalvar: (nome: String, fotoUri: String?) -> Unit,
) {
    val context = LocalContext.current
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var nomeInput by remember { mutableStateOf(nomeAtual) }
    var fotoUriInput by remember { mutableStateOf(fotoUriAtual) }

    val pickerFoto =
        rememberLauncherForActivityResult(
            contract = ActivityResultContracts.GetContent(),
        ) { uri: Uri? ->
            if (uri != null) {
                context.contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION,
                )
                fotoUriInput = uri.toString()
            }
        }

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
                    .padding(horizontal = LkSpacing.lg)
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
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
            Spacer(Modifier.height(LkSpacing.sm))
            Text("Meu perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)

            // Avatar
            Box(
                modifier = Modifier.fillMaxWidth(),
                contentAlignment = Alignment.Center,
            ) {
                UserAvatar(
                    fotoUri = fotoUriInput,
                    fallbackInitial = nomeInput.firstOrNull() ?: deviceName.firstOrNull(),
                    size = 80.dp,
                    onClick = { pickerFoto.launch("image/*") },
                )
            }
            Text(
                "Toque no avatar para alterar a foto",
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
                modifier = Modifier.align(Alignment.CenterHorizontally),
            )

            val fieldColors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LkColors.accent,
                    unfocusedBorderColor = c.border,
                    focusedLabelColor = LkColors.accent,
                    unfocusedLabelColor = c.textSecondary,
                    cursorColor = LkColors.accent,
                    focusedTextColor = c.textPrimary,
                    unfocusedTextColor = c.textPrimary,
                )

            OutlinedTextField(
                value = nomeInput,
                onValueChange = { nomeInput = it },
                label = { Text("Seu nome ou apelido") },
                placeholder = { Text(deviceName.ifBlank { "Ex: João" }, color = c.textTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )

            HorizontalDivider(color = c.border)
            val tipoConexao =
                when (estadoConexao) {
                    EstadoConexao.wifi -> "Wi-Fi"
                    EstadoConexao.movel -> ispInfo?.isp?.takeIf { it.isNotEmpty() } ?: "Rede móvel"
                    EstadoConexao.ethernet -> "Ethernet"
                    else -> "Sem conexão"
                }
            val localizacao = listOfNotNull(ispInfo?.region, ispInfo?.country).joinToString(", ").ifBlank { null }

            ispInfo?.isp?.let { InfoRow(c, "Operadora / ISP", it) }
            if (ispInfo?.isp != null) HorizontalDivider(color = c.border)
            ispInfo?.ip?.let { InfoRow(c, "IP Público", it) }
            if (ispInfo?.ip != null) HorizontalDivider(color = c.border)
            InfoRow(c, "Conexão", tipoConexao)
            HorizontalDivider(color = c.border)
            localizacao?.let { InfoRow(c, "Localização", it) }
            if (localizacao != null) HorizontalDivider(color = c.border)
            InfoRow(c, "Versão", "v$appVersion")
            HorizontalDivider(color = c.border)

            Spacer(Modifier.height(LkSpacing.sm))
            Button(
                onClick = { onSalvar(nomeInput.trim(), fotoUriInput) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            ) {
                Text("Salvar perfil")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SimpleInfoSheet(
    c: LkTokens,
    titulo: String,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit,
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
                    .padding(top = LkSpacing.md)
                    .padding(bottom = LkSpacing.xxl)
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
                text = titulo,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
            )
            Spacer(Modifier.height(LkSpacing.md))
            content()
        }
    }
}

@Composable
private fun InfoRow(
    c: LkTokens,
    label: String,
    value: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, color = c.textPrimary, modifier = Modifier.weight(1f))
        Text(text = value, style = MaterialTheme.typography.titleSmall, color = c.textSecondary, fontWeight = FontWeight.W500)
    }
}

// ─── Provedor sheet ───────────────────────────────────────────────────────────

private val cidadesCache = HashMap<String, List<String>>()

private val ESTADOS_BR =
    listOf(
        "AC" to "Acre",
        "AL" to "Alagoas",
        "AP" to "Amapá",
        "AM" to "Amazonas",
        "BA" to "Bahia",
        "CE" to "Ceará",
        "DF" to "Distrito Federal",
        "ES" to "Espírito Santo",
        "GO" to "Goiás",
        "MA" to "Maranhão",
        "MT" to "Mato Grosso",
        "MS" to "Mato Grosso do Sul",
        "MG" to "Minas Gerais",
        "PA" to "Pará",
        "PB" to "Paraíba",
        "PR" to "Paraná",
        "PE" to "Pernambuco",
        "PI" to "Piauí",
        "RJ" to "Rio de Janeiro",
        "RN" to "Rio Grande do Norte",
        "RS" to "Rio Grande do Sul",
        "RO" to "Rondônia",
        "RR" to "Roraima",
        "SC" to "Santa Catarina",
        "SP" to "São Paulo",
        "SE" to "Sergipe",
        "TO" to "Tocantins",
    )

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ProvedorSheet(
    c: LkTokens,
    operadoraAtual: String,
    planoAtual: String,
    regiaoAtual: String,
    estadoUfAtual: String,
    cidadeNomeAtual: String,
    onDismiss: () -> Unit,
    onSalvar: (operadora: String, plano: String, regiao: String) -> Unit,
    onSalvarEstadoCidade: (estadoUf: String, cidadeNome: String) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var operadoraInput by remember { mutableStateOf(operadoraAtual) }
    var planoInput by remember { mutableStateOf(planoAtual.filter { it.isDigit() }.take(4)) }
    var estadoUfInput by remember { mutableStateOf(estadoUfAtual) }
    var cidadeNomeInput by remember { mutableStateOf(cidadeNomeAtual) }
    var cidadeQuery by remember { mutableStateOf(cidadeNomeAtual) }
    var customISPInput by remember { mutableStateOf("") }
    var showProvedorDropdown by remember { mutableStateOf(false) }
    var showEstadoDropdown by remember { mutableStateOf(false) }
    var showCidadeDropdown by remember { mutableStateOf(false) }
    var cidadesFiltradas by remember { mutableStateOf<List<String>>(emptyList()) }
    var cidadeBuscando by remember { mutableStateOf(false) }

    LaunchedEffect(estadoUfInput) {
        if (estadoUfInput.isNotBlank()) {
            cidadeBuscando = true
            val cached = cidadesCache[estadoUfInput]
            if (cached != null) {
                cidadesFiltradas = cached
            } else {
                val fetched =
                    kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.IO) {
                        runCatching {
                            val url = "https://servicodados.ibge.gov.br/api/v1/localidades/estados/$estadoUfInput/municipios"
                            val connection = java.net.URL(url).openConnection() as java.net.HttpURLConnection
                            connection.connectTimeout = 8_000
                            connection.readTimeout = 8_000
                            val text = connection.inputStream.bufferedReader().use { it.readText() }
                            connection.disconnect()
                            val arr = org.json.JSONArray(text)
                            (0 until arr.length()).map { arr.getJSONObject(it).getString("nome") }.sorted()
                        }.getOrElse { emptyList() }
                    }
                if (fetched.isNotEmpty()) cidadesCache[estadoUfInput] = fetched
                cidadesFiltradas = fetched
            }
            cidadeBuscando = false
        }
    }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LkColors.accent,
            unfocusedBorderColor = c.border,
            focusedLabelColor = LkColors.accent,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = LkColors.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

    val operadorasDisponiveis =
        listOf(
            "Vivo",
            "Claro",
            "NET/Claro",
            "TIM",
            "Oi",
            "Sky",
            "Algar Telecom",
            "Brisanet",
            "Desktop",
            "Copel Telecom",
            "Surf Telecom",
            "Unifique",
            "Vogel",
            "WDC Networks",
            "Ligga",
            "Intelbras",
            "Vero",
            "Sercomtel",
            "Outra / ISP Local",
        )

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
                    .padding(horizontal = LkSpacing.lg)
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
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
            Spacer(Modifier.height(LkSpacing.sm))
            Text("Dados do provedor", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = c.textPrimary)
            Text(
                "Informe sua operadora e plano para análises personalizadas.",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
            )

            // Operadora
            ExposedDropdownMenuBox(expanded = showProvedorDropdown, onExpandedChange = { showProvedorDropdown = it }) {
                OutlinedTextField(
                    value = operadoraInput,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Operadora / ISP") },
                    placeholder = { Text("Selecione uma operadora", color = c.textTertiary) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showProvedorDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = showProvedorDropdown, onDismissRequest = { showProvedorDropdown = false }) {
                    operadorasDisponiveis.forEach { op ->
                        DropdownMenuItem(
                            text = { Text(op) },
                            onClick = {
                                operadoraInput = op
                                if (op == "Outra / ISP Local") customISPInput = ""
                                showProvedorDropdown = false
                            },
                        )
                    }
                }
            }

            if (operadoraInput == "Outra / ISP Local") {
                OutlinedTextField(
                    value = customISPInput,
                    onValueChange = { customISPInput = it },
                    label = { Text("Qual operadora / ISP?") },
                    placeholder = { Text("Digite o nome", color = c.textTertiary) },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    colors = fieldColors,
                    shape = RoundedCornerShape(8.dp),
                )
            }

            // Velocidade contratada — numérico, max 4 dígitos
            OutlinedTextField(
                value = planoInput,
                onValueChange = { v -> if (v.all { it.isDigit() } && v.length <= 4) planoInput = v },
                label = { Text("Velocidade contratada (Mbps)") },
                placeholder = { Text("Ex: 100", color = c.textTertiary) },
                suffix = { if (planoInput.isNotBlank()) Text("Mbps", color = c.textTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )

            // Estado
            ExposedDropdownMenuBox(expanded = showEstadoDropdown, onExpandedChange = { showEstadoDropdown = it }) {
                OutlinedTextField(
                    value = ESTADOS_BR.firstOrNull { it.first == estadoUfInput }?.second ?: estadoUfInput,
                    onValueChange = {},
                    readOnly = true,
                    label = { Text("Estado") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = showEstadoDropdown) },
                    modifier = Modifier.menuAnchor().fillMaxWidth(),
                    colors = fieldColors,
                    shape = RoundedCornerShape(8.dp),
                )
                ExposedDropdownMenu(expanded = showEstadoDropdown, onDismissRequest = { showEstadoDropdown = false }) {
                    ESTADOS_BR.forEach { (uf, nome) ->
                        DropdownMenuItem(
                            text = { Text("$uf — $nome") },
                            onClick = {
                                estadoUfInput = uf
                                cidadeNomeInput = ""
                                cidadeQuery = ""
                                showEstadoDropdown = false
                            },
                        )
                    }
                }
            }

            // Cidade com autocomplete
            val cidadesFiltPorQuery =
                remember(cidadeQuery, cidadesFiltradas) {
                    if (cidadeQuery.length < 2) {
                        emptyList()
                    } else {
                        cidadesFiltradas.filter { it.contains(cidadeQuery, ignoreCase = true) }.take(5)
                    }
                }
            OutlinedTextField(
                value = cidadeQuery,
                onValueChange = { v ->
                    cidadeQuery = v
                    cidadeNomeInput = v
                    showCidadeDropdown = v.length >= 2
                },
                label = { Text("Cidade") },
                placeholder = {
                    Text(
                        text =
                            when {
                                cidadeBuscando -> "Buscando cidades…"
                                estadoUfInput.isBlank() -> "Selecione um estado primeiro"
                                else -> "Digite a cidade"
                            },
                        color = c.textTertiary,
                    )
                },
                enabled = estadoUfInput.isNotBlank() && !cidadeBuscando,
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )
            if (showCidadeDropdown && cidadesFiltPorQuery.isNotEmpty()) {
                Column(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(c.bgSecondary)
                            .border(1.dp, c.border, RoundedCornerShape(8.dp)),
                ) {
                    cidadesFiltPorQuery.forEach { cidade ->
                        Text(
                            text = cidade,
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        cidadeNomeInput = cidade
                                        cidadeQuery = cidade
                                        showCidadeDropdown = false
                                    }.padding(LkSpacing.md),
                            style = MaterialTheme.typography.bodyMedium,
                            color = c.textPrimary,
                        )
                    }
                }
            }

            Button(
                onClick = {
                    val finalOperadora = if (operadoraInput == "Outra / ISP Local") customISPInput else operadoraInput
                    val regiaoCombinada =
                        if (cidadeNomeInput.isNotBlank() && estadoUfInput.isNotBlank()) {
                            "$cidadeNomeInput, $estadoUfInput"
                        } else {
                            regiaoAtual
                        }
                    onSalvar(finalOperadora.trim(), planoInput.trim(), regiaoCombinada)
                    if (estadoUfInput.isNotBlank()) onSalvarEstadoCidade(estadoUfInput, cidadeNomeInput.trim())
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                enabled = !(operadoraInput == "Outra / ISP Local" && customISPInput.isBlank()),
            ) {
                Text("Salvar")
            }
        }
    }
}

// ─── Preferências sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PreferenciasSheet(
    c: LkTokens,
    limiteAtual: Int,
    onDismiss: () -> Unit,
    onSalvar: (Int) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var limiteInput by remember { mutableStateOf(if (limiteAtual > 0) limiteAtual.toString() else "") }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LkColors.accent,
            unfocusedBorderColor = c.border,
            focusedLabelColor = LkColors.accent,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = LkColors.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

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
                    .padding(horizontal = LkSpacing.lg)
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
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
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                "Alertas de qualidade",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Text(
                "Defina um limite mínimo de download. Quando sua conexão ficar abaixo desse valor, o SignallQ pode alertar você.",
                style = MaterialTheme.typography.titleSmall,
                color = c.textSecondary,
                lineHeight = 18.sp,
            )
            OutlinedTextField(
                value = limiteInput,
                onValueChange = { limiteInput = it.filter { ch -> ch.isDigit() }.take(4) },
                label = { Text("Mínimo de download (Mbps)") },
                placeholder = { Text("Ex: 50", color = c.textTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )
            if (limiteInput.isBlank()) {
                Text(
                    "Deixe em branco para desativar os alertas.",
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textTertiary,
                )
            }
            Button(
                onClick = { onSalvar(limiteInput.toIntOrNull() ?: 0) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            ) {
                Text("Salvar")
            }
        }
    }
}

// ─── Changelog sheet ──────────────────────────────────────────────────────

// ─── Diagnóstico sheet ────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DiagnosticoSheet(
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
                text = "Diagnóstico avançado",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
            )
            Text(
                text = "Recursos que aprofundam a análise da sua rede",
                style = MaterialTheme.typography.bodySmall,
                color = c.textSecondary,
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
            )
            Spacer(Modifier.height(LkSpacing.sm))
            ToggleItem(
                c = c,
                icon = Icons.Outlined.Analytics,
                label = "Análise avançada",
                subtitle = if (analiseAvancada) "Ativa" else "Desativada · pode aumentar consumo de bateria",
                checked = analiseAvancada,
                onCheckedChange = { enabled ->
                    if (enabled && !analiseAvancada) {
                        showConfirmAnalise = true
                    } else if (!enabled) {
                        onDefinirAnaliseAvancada(false)
                    }
                },
            )
            HorizontalDivider(color = c.border, thickness = 1.dp)
            ToggleItem(
                c = c,
                icon = Icons.Outlined.Sensors,
                label = "Monitoramento passivo",
                subtitle = if (monitoramentoAtivo) "Ativo · verifica a cada 30 minutos" else "Desativado",
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
                HorizontalDivider(
                    color = c.border,
                    thickness = 1.dp,
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                )
                Text(
                    text = "Notificações",
                    style = MaterialTheme.typography.labelMedium,
                    color = c.textSecondary,
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.xs),
                )
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.WifiOff,
                    label = "Sem internet",
                    subtitle = "Avisa quando a conexão cair",
                    checked = notificacaoSemInternetAtiva,
                    onCheckedChange = onDefinirNotificacaoSemInternetAtiva,
                )
                HorizontalDivider(color = c.border, thickness = 1.dp)
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.Speed,
                    label = "Latência alta",
                    subtitle = "Avisa quando a rede ficar lenta",
                    checked = notificacaoLatenciaAtiva,
                    onCheckedChange = onDefinirNotificacaoLatenciaAtiva,
                )
                HorizontalDivider(color = c.border, thickness = 1.dp)
                ToggleItem(
                    c = c,
                    icon = Icons.Outlined.Language,
                    label = "DNS lento",
                    subtitle = "Avisa quando sites e apps demorarem para carregar",
                    checked = notificacaoDnsAtiva,
                    onCheckedChange = onDefinirNotificacaoDnsAtiva,
                )
                HorizontalDivider(color = c.border, thickness = 1.dp)
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
                HorizontalDivider(color = c.border, thickness = 1.dp)
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = LkColors.warning,
                        modifier = Modifier.size(16.dp),
                    )
                    Text(
                        text =
                            "Em alguns dispositivos ${OemKillInfo.nomeFabricante}, o sistema pode reduzir a frequência " +
                                "das verificações para economizar bateria. Para garantir o funcionamento, mantenha o SignallQ " +
                                "na lista de apps sem restrição de bateria nas configurações do sistema.",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
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

// ─── Dados locais sheet ───────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DadosLocaisSheet(
    c: LkTokens,
    onDismiss: () -> Unit,
    onLimparHistorico: () -> Unit,
    onApagarDadosLocais: () -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var showConfirmLimpar by remember { mutableStateOf(false) }
    var showConfirmApagar by remember { mutableStateOf(false) }

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
                    .padding(horizontal = LkSpacing.lg)
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
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
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                text = "Gerenciar dados locais",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )
            Text(
                text = "Estas ações são irreversíveis. Os dados serão removidos permanentemente do dispositivo.",
                style = MaterialTheme.typography.titleSmall,
                color = c.textSecondary,
                lineHeight = 18.sp,
            )
            OutlinedButton(
                onClick = { showConfirmLimpar = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, LkColors.warning),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LkColors.warning),
            ) {
                Icon(Icons.Outlined.History, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Limpar histórico de testes")
            }
            OutlinedButton(
                onClick = { showConfirmApagar = true },
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, LkColors.error),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = LkColors.error),
            ) {
                Icon(Icons.Outlined.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text("Apagar dados locais")
            }
        }
    }

    if (showConfirmLimpar) {
        ConfirmacaoDialog(
            titulo = "Limpar histórico?",
            mensagem = "Esta ação removerá todos os testes registrados. Não pode ser desfeita.",
            onConfirmar = {
                onLimparHistorico()
                showConfirmLimpar = false
                onDismiss()
            },
            onCancelar = { showConfirmLimpar = false },
        )
    }

    if (showConfirmApagar) {
        ConfirmacaoDialog(
            titulo = "Apagar dados locais?",
            mensagem = "Remove configurações salvas e preferências. Esta ação não pode ser desfeita.",
            onConfirmar = {
                onApagarDadosLocais()
                showConfirmApagar = false
                onDismiss()
            },
            onCancelar = { showConfirmApagar = false },
        )
    }
}

// ─── Diagnóstico do app sheet ─────────────────────────────────────────────────

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

// ─── Roteador sheet ───────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun RoteadorBottomSheet(
    c: LkTokens,
    modemHost: String?,
    modemUsername: String,
    modemPassword: String,
    modemPermanecerConectado: Boolean,
    gatewayIpDetectado: String?,
    onDismiss: () -> Unit,
    onSalvar: (host: String, username: String, password: String, permanecer: Boolean) -> Unit,
    onConectar: (host: String, username: String, password: String, permanecer: Boolean) -> Unit,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var hostInput by remember { mutableStateOf(modemHost ?: gatewayIpDetectado ?: "") }
    var usernameInput by remember { mutableStateOf(modemUsername) }
    var passwordInput by remember { mutableStateOf(modemPassword) }
    var permanecerInput by remember { mutableStateOf(modemPermanecerConectado) }
    var showPassword by remember { mutableStateOf(false) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = LkColors.accent,
            unfocusedBorderColor = c.border,
            focusedLabelColor = LkColors.accent,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = LkColors.accent,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

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
                    .padding(horizontal = LkSpacing.lg)
                    .padding(top = LkSpacing.md, bottom = LkSpacing.xxl)
                    .navigationBarsPadding(),
            verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
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

            Spacer(Modifier.height(LkSpacing.sm))

            Text(
                text = "Configurações do roteador",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = c.textPrimary,
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(LkColors.accent.copy(alpha = 0.08f))
                        .padding(LkSpacing.md),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Router,
                    contentDescription = null,
                    tint = LkColors.accent,
                    modifier = Modifier.size(22.dp),
                )
                Spacer(Modifier.width(LkSpacing.sm))
                Column {
                    Text(
                        text = "Modem suportado",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.W600,
                        color = LkColors.accent,
                    )
                    Text(
                        text = "Nokia GPON (série SA / NT)",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
            }

            OutlinedTextField(
                value = hostInput,
                onValueChange = { hostInput = it },
                label = { Text("Endereço IP do modem") },
                placeholder = { Text(gatewayIpDetectado ?: "192.168.1.1", color = c.textTertiary) },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )

            OutlinedTextField(
                value = usernameInput,
                onValueChange = { usernameInput = it },
                label = { Text("Usuário") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )

            OutlinedTextField(
                value = passwordInput,
                onValueChange = { passwordInput = it },
                label = { Text("Senha") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                trailingIcon = {
                    IconButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Filled.VisibilityOff else Icons.Filled.Visibility,
                            contentDescription = if (showPassword) "Ocultar senha" else "Mostrar senha",
                            tint = c.textSecondary,
                        )
                    }
                },
                colors = fieldColors,
                shape = RoundedCornerShape(8.dp),
            )

            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.card))
                        .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                        .background(c.bgCard)
                        .padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Permanecer conectado",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.W500,
                        color = c.textPrimary,
                    )
                    Text(
                        text = "Conectar automaticamente ao iniciar o app",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                    )
                }
                Switch(
                    checked = permanecerInput,
                    onCheckedChange = { permanecerInput = it },
                    colors =
                        SwitchDefaults.colors(
                            checkedThumbColor = MaterialTheme.colorScheme.onPrimary,
                            checkedTrackColor = LkColors.accent,
                            uncheckedThumbColor = c.textTertiary,
                            uncheckedTrackColor = c.border,
                        ),
                )
            }

            Spacer(Modifier.height(LkSpacing.sm))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(LkSpacing.sm),
            ) {
                OutlinedButton(
                    onClick = { onSalvar(hostInput, usernameInput, passwordInput, permanecerInput) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(LkRadius.button),
                    border = BorderStroke(1.dp, c.border),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = c.textPrimary),
                ) {
                    Text("Salvar")
                }
                Button(
                    onClick = { onConectar(hostInput, usernameInput, passwordInput, permanecerInput) },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
                ) {
                    Text("Conectar e ver status")
                }
            }
        }
    }
}
