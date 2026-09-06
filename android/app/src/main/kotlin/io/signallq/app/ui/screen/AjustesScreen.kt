package io.signallq.app.ui.screen

import android.annotation.SuppressLint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.feature.settings.ResultadoDivergenciaPerfilConexao
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
    // Issue #1670 — estado observável (EmAndamento/Sucesso/Falha) da última ação disparada
    // pela DadosLocaisSheet abaixo; ver AcaoDadosLocaisEstado.kt.
    dadosLocaisAcaoEstado: AcaoDadosLocaisEstado = AcaoDadosLocaisEstado.Ocioso,
    onConsumirDadosLocaisAcaoEstado: () -> Unit = {},
    quantidadeHistorico: Int? = null,
    quantidadeApelidos: Int? = null,
    onAbrirHistorico: () -> Unit,
    onAbrirLaudo: () -> Unit,
    onAbrirPrivacidade: () -> Unit = {},
    onAbrirTermos: () -> Unit = {},
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
    // aliases locais para não explodir o código interno com prefixos
    val deviceName = perfil.deviceName
    val appVersion = perfil.appVersion
    val nomeUsuario = perfil.nomeUsuario
    val fotoUriUsuario = perfil.fotoUriUsuario
    val planoInternet = provedor.planoInternet
    val regiao = provedor.regiao
    // GH#1249 -- "Minha conexao" agora e espelho do perfil por rede, nao mais de chaves
    // globais soltas (operadora/estadoUf/cidadeNome/velocidadeContratada*/ispDetectado/
    // ispConfirmado saíram daqui).
    val minhaConexao = provedor.minhaConexao
    // GH#1099 — modemHost/modemUsername/modemPassword/modemPermanecerConectado/
    // gatewayIpDetectado/conectarGateway/onGatewayConectado removidos daqui: só existiam
    // pra alimentar o showGatewayConnectionSheet órfão abaixo (nunca setado como true em
    // lugar nenhum, dead code de uma versão anterior). O fluxo real de credenciais do
    // roteador é via Home (GatewayConnectionSheet) ou, desde #1099, via
    // EquipamentoInternetScreen — nenhum dos dois passa por AjustesScreen.
    // gatewaySessaoValida/bandasWifi/dispositivosNaRede também são lidos de `modem` mas já
    // estavam sem nenhum consumidor antes desta mudança — dívida pré-existente, não
    // resolvida aqui (ver seção "Dívidas" da entrega).

    // aliases de lambdas — mantém corpo interno sem alteração
    val onSalvarPerfil = perfil.onSalvarPerfil
    val onSalvarConnectionProfile = provedor.onSalvarConnectionProfile
    val speedtestPermiteHeavyMovel = dadosMoveis.speedtestPermiteHeavyMovel
    val speedtestMbConsumidosMes = dadosMoveis.speedtestMbConsumidosMes
    val onSetSpeedtestPermiteHeavyMovel = dadosMoveis.onSetSpeedtestPermiteHeavyMovel

    var showPerfilSheet by remember { mutableStateOf(false) }
    var showSobreSheet by remember { mutableStateOf(false) }
    var showDadosLocaisSheet by remember { mutableStateOf(false) }
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
                navigationIcon = {
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
            // Os quatro destinos do protótipo aparecem primeiro, para que a tela responda
            // à intenção de cuidar da conexão antes das preferências secundárias.
            item { Spacer(Modifier.height(LkSpacing.md)) }
            item { SectionHeader("Sua conexão", c) }

            // GH#1249 item C -- divergência entre provedor salvo e detectado nesta rede. Só
            // aparece quando o usuário já confirmou explicitamente o valor salvo (senão o
            // LaunchedEffect abaixo já sobrescreve silenciosamente, sem perguntar nada).
            val divergenciaConfirmada =
                minhaConexao.divergencia as? ResultadoDivergenciaPerfilConexao.DivergenciaConfirmadaPeloUsuario
            if (divergenciaConfirmada != null) {
                item {
                    MinhaConexaoDivergenciaBanner(
                        c = c,
                        provedorDetectado = divergenciaConfirmada.detectado,
                        onUsarDetectado = {
                            onSalvarConnectionProfile(
                                divergenciaConfirmada.detectado,
                                minhaConexao.contractedDownloadMbps,
                                minhaConexao.contractedUploadMbps,
                                minhaConexao.city,
                                minhaConexao.state,
                                true,
                            )
                        },
                    )
                }
                item { Spacer(Modifier.height(8.dp)) }
            }
            // GH#1249 item C -- divergência sem confirmação prévia do usuário: aplica o valor
            // detectado silenciosamente, sem sheet/banner nenhum.
            val atualizavelSilenciosamente =
                minhaConexao.divergencia as? ResultadoDivergenciaPerfilConexao.AtualizavelSilenciosamente
            if (atualizavelSilenciosamente != null) {
                item {
                    LaunchedEffect(atualizavelSilenciosamente) {
                        onSalvarConnectionProfile(
                            atualizavelSilenciosamente.detectado,
                            minhaConexao.contractedDownloadMbps,
                            minhaConexao.contractedUploadMbps,
                            minhaConexao.city,
                            minhaConexao.state,
                            false,
                        )
                    }
                }
            }

            item {
                SettingsSectionCard(c = c) {
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Business,
                        label = "Provedor",
                        subtitle = "Operadora, plano e cidade",
                        value = null,
                        onClick = { showMinhaConexaoSheet = true },
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Router,
                        label = "Equipamento de internet",
                        subtitle = "Acesso e informações do roteador",
                        value = null,
                        onClick = onAbrirFibra,
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Notifications,
                        label = "Monitoramento e alertas",
                        value = null,
                        onClick = onAbrirMonitoramento,
                    )
                }
            }
            item { Spacer(Modifier.height(LkSpacing.base)) }

            item { SectionHeader("Dados móveis", c) }
            item {
                SettingsSectionCard(c = c) {
                    ToggleItem(
                        c = c,
                        icon = Icons.Outlined.Speed,
                        label = "Permitir testes pesados",
                        subtitle =
                            "Autoriza medições de velocidade quando você estiver usando a rede móvel",
                        checked = speedtestPermiteHeavyMovel,
                        onCheckedChange = onSetSpeedtestPermiteHeavyMovel,
                    )
                    HorizontalDivider(color = c.border, thickness = 1.dp)
                    Text(
                        text = "Uso neste mês: ${formatarMegabytes(speedtestMbConsumidosMes)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = c.textSecondary,
                        modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
                    )
                }
            }
            item { Spacer(Modifier.height(LkSpacing.base)) }

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
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Delete,
                        label = "Dados locais",
                        subtitle = "Histórico, preferências e credenciais",
                        value = null,
                        onClick = { showDadosLocaisSheet = true },
                    )
                }
            }
            item { Spacer(Modifier.height(LkSpacing.base)) }

            // ── PREFERÊNCIAS ADICIONAIS ──────────────────────────────────────────────
            // GH#1358 — hero card com avatar/foto removido: qualquer imagem/foto de perfil
            // fica desabilitada em todo o app. Edição de nome preservada, agora como linha
            // no mesmo padrão visual das demais seções (ValueSettingRow).
            item { Spacer(Modifier.height(LkSpacing.md)) }
            item { SectionHeader("Perfil", c) }
            item {
                val nomeDisplay = if (nomeUsuario.isNotBlank()) nomeUsuario else "Seu perfil"
                SettingsSectionCard(c = c) {
                    ValueSettingRow(
                        c = c,
                        icon = Icons.Outlined.Person,
                        label = "Nome",
                        value = nomeDisplay,
                        onClick = { showPerfilSheet = true },
                    )
                }
            }

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
            item { Spacer(Modifier.height(LkSpacing.base)) }

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
            item { Spacer(Modifier.height(LkSpacing.base)) }

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
            onAbrirTermos = onAbrirTermos,
            onAbrirPrivacidade = onAbrirPrivacidade,
        )
    }

    if (showDadosLocaisSheet) {
        DadosLocaisSheet(
            c = c,
            onDismiss = { showDadosLocaisSheet = false },
            onLimparHistorico = onLimparHistorico,
            onApagarDadosLocais = onApagarDadosLocais,
            onResetarApp = onResetarApp,
            estado = dadosLocaisAcaoEstado,
            onConsumirEstado = onConsumirDadosLocaisAcaoEstado,
            quantidadeHistorico = quantidadeHistorico,
            quantidadeApelidos = quantidadeApelidos,
            onAbrirHistorico = onAbrirHistorico,
        )
    }

    if (showMinhaConexaoSheet) {
        MinhaConexaoSheet(
            operadora = minhaConexao.providerFixed,
            estadoUf = minhaConexao.state,
            cidadeNome = minhaConexao.city,
            velocidadeContratadaDownMbps = minhaConexao.contractedDownloadMbps,
            velocidadeContratadaUpMbps = minhaConexao.contractedUploadMbps,
            // GH#1249 -- a sugestão de operadora detectada agora vem do mesmo detector de
            // divergência usado no banner (nunca duplica outra fonte de detecção).
            operadoraAutodetectada =
                (minhaConexao.divergencia as? ResultadoDivergenciaPerfilConexao.AtualizavelSilenciosamente)?.detectado
                    ?: (minhaConexao.divergencia as? ResultadoDivergenciaPerfilConexao.DivergenciaConfirmadaPeloUsuario)?.detectado,
            onSalvar = { op, uf, cidade, down, up ->
                // Salvamento explícito pela sheet = confirmação explícita do usuário
                // (userConfirmed = true) -- futuras divergências passam a exigir confirmação
                // em vez de sobrescrever silenciosamente (ver DetectorDivergenciaPerfilConexao).
                onSalvarConnectionProfile(op, down, up, cidade, uf, true)
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
    subtitle: String? = null,
    value: String?,
    onClick: () -> Unit,
    isPlaceholder: Boolean = false,
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
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyLarge,
                color = c.textPrimary,
            )
            if (!subtitle.isNullOrBlank()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = c.textSecondary,
                )
            }
        }
        if (!value.isNullOrBlank()) {
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isPlaceholder) FontWeight.Medium else null,
                color = if (isPlaceholder) c.primary else c.textSecondary,
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

private fun formatarMegabytes(valor: Long): String =
    when {
        valor < 1024L -> "$valor MB"
        valor < 1024L * 1024L -> "%.1f GB".format(valor / 1024.0)
        else -> "%.2f TB".format(valor / (1024.0 * 1024.0))
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
