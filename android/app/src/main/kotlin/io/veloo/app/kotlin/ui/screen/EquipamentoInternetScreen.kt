package io.signallq.app.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.AccountTree
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.signallq.app.core.network.contracts.fibra.GponSaudeStatus
import io.signallq.app.core.network.contracts.gateway.AcessoEquipamento
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.diagnostico.topology.model.NatStatus
import io.signallq.app.feature.fibra.DeviceInfoFibra
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.GponStatus
import io.signallq.app.feature.fibra.LanStatus
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.feature.fibra.WanStatus
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.EquipamentoItemTecnico
import io.signallq.app.ui.component.EquipamentoSecaoTecnica
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkSurfaceCard
import io.signallq.app.ui.component.LocalDeviceSectionUiState
import io.signallq.app.ui.component.mapLocalDeviceSectionUiState

/** Janela de tolerância pós-reboot em que um erro de comunicação é explicado
 *  como "o equipamento está voltando" em vez do texto genérico de sessão
 *  caída — o reboot real leva 1-3 minutos num GPON típico (GH#934). */
private const val JANELA_POS_REBOOT_MS = 3 * 60 * 1000L

/**
 * Tela "Equipamento de internet" (GH#934, Fase 5 MD3 To-Be) — substitui o
 * antigo `FibraModemScreen.kt` (Nokia-only, sem composição por capacidade).
 *
 * Composição por capacidade: o corpo "conectado" é inteiramente delegado a
 * [LocalDeviceSection] (já existente, já cobre fibra/WAN/Wi-Fi/LAN/clientes
 * por [io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities] e já
 * reaproveita [ClassificadorSaudeGpon] via `FibraSignalQualityEngine` — nada
 * duplicado aqui). Esta tela adiciona por cima: chrome (topo/voltar/refresh),
 * o nível de [AcessoEquipamento] (novo nesta fase), alerta de Double NAT e a
 * ação de reiniciar (só quando o driver declara `suportaGerenciamento`).
 *
 * Fabricante não-Nokia / equipamento sem driver: cai em
 * [AcessoEquipamento.SOMENTE_IDENTIFICACAO] — nunca inventa dado, nunca
 * trava a tela (item 6 da issue #934, ver limitação documentada em
 * [mapAcessoEquipamento]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EquipamentoInternetScreen(
    snapshotFibra: SnapshotFibra,
    localDevice: LocalNetworkDeviceSnapshot?,
    natStatus: NatStatus?,
    modemHost: String?,
    modemUsername: String,
    modemPassword: String,
    onVoltar: () -> Unit,
    onRetentar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    onReiniciarEquipamento: () -> Unit,
    // GH#1031 — ações antes fantasmas (enabled=false): agora navegam para fluxos
    // reais já existentes no app, sem duplicar telas/lógica.
    onVerDispositivos: () -> Unit = {},
    onExecutarDiagnostico: () -> Unit = {},
    onVerDetalhesWifi: () -> Unit = {},
) {
    val c = LocalLkTokens.current
    var reiniciadoEmEpochMs by remember { mutableStateOf<Long?>(null) }
    var mostrarDialogoReiniciar by remember { mutableStateOf(false) }

    val acesso =
        remember(snapshotFibra, localDevice, modemHost, modemUsername, modemPassword) {
            mapAcessoEquipamento(snapshotFibra, localDevice, modemHost, modemUsername, modemPassword)
        }
    val doubleNatSuspeito =
        remember(natStatus, snapshotFibra.gpon?.mode) {
            suspeitaDoubleNat(natStatus, snapshotFibra.gpon?.mode)
        }
    val dentroDaJanelaPosReboot =
        reiniciadoEmEpochMs?.let { System.currentTimeMillis() - it < JANELA_POS_REBOOT_MS } ?: false

    if (mostrarDialogoReiniciar) {
        ReiniciarEquipamentoDialog(
            onConfirmar = {
                mostrarDialogoReiniciar = false
                reiniciadoEmEpochMs = System.currentTimeMillis()
                onReiniciarEquipamento()
            },
            onCancelar = { mostrarDialogoReiniciar = false },
        )
    }

    Scaffold(
        containerColor = c.bgPrimary,
        topBar = {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(c.bgPrimary),
            ) {
                Row(
                    modifier =
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = LkSpacing.sm, vertical = LkSpacing.sm),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        IconButton(onClick = onVoltar) {
                            Icon(
                                Icons.AutoMirrored.Outlined.ArrowBack,
                                contentDescription = "Voltar",
                                tint = c.textPrimary,
                            )
                        }
                        Spacer(Modifier.width(LkSpacing.xs))
                        Column {
                            Text(
                                "Equipamento de internet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                            Text(
                                acessoLabel(acesso),
                                fontSize = 12.sp,
                                color = c.textSecondary,
                            )
                        }
                    }
                    IconButton(onClick = onRetentar) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar", tint = c.textPrimary)
                    }
                }
                HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
            }
        },
    ) { padding ->
        when {
            snapshotFibra.estado == EstadoFibra.idle || snapshotFibra.estado == EstadoFibra.conectando ->
                EquipamentoCarregando(modifier = Modifier.padding(padding), c = c)

            acesso == AcessoEquipamento.LEITURA_COMPLETA ||
                acesso == AcessoEquipamento.LEITURA_PARCIAL ||
                acesso == AcessoEquipamento.GERENCIAMENTO_DISPONIVEL ->
                EquipamentoConectadoContent(
                    localDevice = localDevice,
                    snapshotFibra = snapshotFibra,
                    acesso = acesso,
                    doubleNatSuspeito = doubleNatSuspeito,
                    onSolicitarReiniciar = { mostrarDialogoReiniciar = true },
                    onVerDispositivos = onVerDispositivos,
                    onExecutarDiagnostico = onExecutarDiagnostico,
                    onVerDetalhesWifi = onVerDetalhesWifi,
                    c = c,
                    modifier = Modifier.padding(padding),
                )

            else ->
                EquipamentoAcessoIndisponivelContent(
                    acesso = acesso,
                    dentroDaJanelaPosReboot = dentroDaJanelaPosReboot,
                    onRetentar = onRetentar,
                    onAbrirAjustes = onAbrirAjustes,
                    c = c,
                    modifier = Modifier.padding(padding),
                )
        }
    }
}

@Composable
private fun EquipamentoCarregando(
    modifier: Modifier = Modifier,
    c: LkTokens,
) {
    val pulsar =
        rememberInfiniteTransition(label = "equipamento_skeleton").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Reverse),
            label = "equipamento_skeleton_alpha",
        )
    Column(
        modifier = modifier.fillMaxSize().padding(LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        repeat(5) { index ->
            Box(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .height(if (index == 0) 88.dp else 112.dp)
                        .clip(RoundedCornerShape(LkRadius.card))
                        .background(c.surfaceContainerHigh.copy(alpha = pulsar.value)),
            )
        }
    }
}

@Composable
private fun EquipamentoConectadoContent(
    localDevice: LocalNetworkDeviceSnapshot?,
    snapshotFibra: SnapshotFibra,
    acesso: AcessoEquipamento,
    doubleNatSuspeito: Boolean,
    onSolicitarReiniciar: () -> Unit,
    onVerDispositivos: () -> Unit,
    onExecutarDiagnostico: () -> Unit,
    onVerDetalhesWifi: () -> Unit,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val estadoSecao = localDevice?.let { mapLocalDeviceSectionUiState(it) }
    if (localDevice == null || estadoSecao !is LocalDeviceSectionUiState.Conectado) {
        // Defensivo: mapAcessoEquipamento so cai numa das 3 variantes "conectado" quando
        // localDevice nao e nulo e passa nos mesmos criterios de mapLocalDeviceSectionUiState
        // — chegar aqui indica inconsistencia entre os dois mapeamentos, nunca deveria
        // acontecer com dado real, mas nunca deve quebrar a tela.
        Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text("Sem dados do equipamento nesta captura.", fontSize = 14.sp, color = c.textSecondary)
        }
        return
    }

    val paineis =
        remember(localDevice, estadoSecao, snapshotFibra, acesso, doubleNatSuspeito) {
            buildEquipmentPanels(
                localDevice = localDevice,
                estadoSecao = estadoSecao,
                snapshotFibra = snapshotFibra,
                acesso = acesso,
                doubleNatSuspeito = doubleNatSuspeito,
            )
        }
    var painelSelecionadoId by remember(paineis) { mutableStateOf(paineis.firstOrNull()?.id.orEmpty()) }
    val painelSelecionado = paineis.firstOrNull { it.id == painelSelecionadoId } ?: paineis.first()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Spacer(Modifier.height(LkSpacing.xs))

        IdentificacaoEquipamentoCard(
            vendor = painelSelecionado.vendor,
            modelo = painelSelecionado.modelo,
            deviceType = painelSelecionado.deviceTypeLabel,
            atualizadoEm = painelSelecionado.atualizacaoLabel,
            c = c,
        )

        if (paineis.size > 1) {
            DeviceSelectorCard(
                paineis = paineis,
                selecionadoId = painelSelecionadoId,
                onSelecionar = { painelSelecionadoId = it },
                c = c,
            )
        }

        StatusEquipamentoCard(
            titulo = painelSelecionado.statusTitulo,
            descricao = painelSelecionado.statusDescricao,
            cor = painelSelecionado.statusColor,
            suportaFibra = painelSelecionado.suportaFibra,
            suportaWifi = painelSelecionado.suportaWifi,
            totalClientes = painelSelecionado.totalClientes,
            acessoLabel = painelSelecionado.acessoLabel,
            c = c,
        )

        TopologiaRedeCard(
            paineis = paineis,
            selecionadoId = painelSelecionado.id,
            warning = painelSelecionado.topologyWarning,
            c = c,
        )

        painelSelecionado.alerta?.let { alerta ->
            AlertaCard(alerta = alerta, onAcionar = onExecutarDiagnostico)
        }

        if (painelSelecionado.mostrarAvisoLeituraParcial) {
            AvisoAcessoCard(
                icone = Icons.Outlined.ErrorOutline,
                cor = LkColors.warning,
                texto = "Leitura parcial — algumas seções deste equipamento não vieram preenchidas nesta captura.",
            )
        }

        painelSelecionado.gponSaude?.let { status ->
            SaudeOpticaBadge(
                status = status,
            )
        }

        painelSelecionado.secoesTecnicas
            .filterNot { it.titulo == "Dispositivos conectados" }
            .forEach { secao ->
                ModuloTecnicoCard(secao = secao, c = c)
            }

        painelSelecionado.devicesSummary?.let { summary ->
            DevicesSummaryCard(summary = summary, c = c)
        }

        DeviceInfoSectionCard(
            linhas = painelSelecionado.infoRows,
            acesso = painelSelecionado.acessoLabel,
            acessoColor = painelSelecionado.statusColor,
            c = c,
        )

        if (painelSelecionado.actions.isNotEmpty()) {
            ActionsSectionCard(
                actions = painelSelecionado.actions,
                onSolicitarReiniciar = onSolicitarReiniciar,
                onVerDispositivos = onVerDispositivos,
                onExecutarDiagnostico = onExecutarDiagnostico,
                onVerDetalhesWifi = onVerDetalhesWifi,
                c = c,
            )
        } else if (painelSelecionado.podeReiniciar) {
            ReiniciarEquipamentoRow(onClick = onSolicitarReiniciar, c = c)
        }

        Spacer(Modifier.height(LkSpacing.lg))
    }
}

@Composable
private fun SaudeOpticaBadge(status: GponSaudeStatus) {
    val (texto, cor) =
        when (status) {
            GponSaudeStatus.boa -> "Sinal óptico bom" to LkColors.success
            GponSaudeStatus.regular -> "Sinal óptico regular" to LkColors.warning
            GponSaudeStatus.ruim -> "Sinal óptico ruim" to LkColors.error
        }
    Row(
        modifier =
            Modifier
                .clip(RoundedCornerShape(LkRadius.pill))
                .background(cor.copy(alpha = 0.10f))
                .padding(horizontal = LkSpacing.md, vertical = LkSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(cor))
        Spacer(Modifier.width(LkSpacing.xs))
        Text(texto, fontSize = 12.sp, fontWeight = FontWeight.W600, color = cor)
    }
}

@Composable
private fun IdentificacaoEquipamentoCard(
    vendor: String?,
    modelo: String?,
    deviceType: String,
    atualizadoEm: String,
    c: LkTokens,
) {
    val titulo = listOfNotNull(vendor?.takeIf { it.isNotBlank() }, modelo?.takeIf { it.isNotBlank() }).joinToString(" ").ifBlank { "Equipamento local" }
    LkSurfaceCard {
        Text(
            text = titulo,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.W600,
            color = c.textPrimary,
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = deviceType,
            style = MaterialTheme.typography.bodyMedium,
            color = c.textSecondary,
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Text(
            text = atualizadoEm,
            style = MaterialTheme.typography.labelMedium,
            color = c.textTertiary,
        )
    }
}

@Composable
private fun StatusEquipamentoCard(
    titulo: String,
    descricao: String,
    cor: Color,
    suportaFibra: Boolean,
    suportaWifi: Boolean,
    totalClientes: Int,
    acessoLabel: String,
    c: LkTokens,
) {
    LkSurfaceCard(
        modifier =
            Modifier
                .background(cor.copy(alpha = 0.10f), RoundedCornerShape(LkRadius.card))
                .border(1.dp, cor.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.card)),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                imageVector =
                    when (cor) {
                        LkColors.success -> Icons.Outlined.CheckCircle
                        LkColors.error -> Icons.Outlined.ErrorOutline
                        else -> Icons.Outlined.WarningAmber
                    },
                contentDescription = null,
                tint = cor,
                modifier = Modifier.size(26.dp),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = titulo,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                )
                Text(
                    text = descricao,
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                )
            }
        }
        Spacer(Modifier.height(LkSpacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
            StatusMiniStat(label = "Fibra", value = if (suportaFibra) "Disponível" else "Não se aplica", modifier = Modifier.weight(1f), c = c)
            StatusMiniStat(label = "Wi-Fi", value = if (suportaWifi) "Disponível" else "Não se aplica", modifier = Modifier.weight(1f), c = c)
        }
        Spacer(Modifier.height(LkSpacing.sm))
        Row(horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
            StatusMiniStat(label = "Clientes", value = totalClientes.toString(), modifier = Modifier.weight(1f), c = c)
            StatusMiniStat(label = "Acesso", value = acessoLabel, modifier = Modifier.weight(1f), c = c)
        }
    }
}

@Composable
private fun StatusMiniStat(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    c: LkTokens,
) {
    Column(modifier = modifier) {
        Text(label, style = MaterialTheme.typography.labelMedium, color = c.textTertiary)
        Spacer(Modifier.height(2.dp))
        Text(value, style = MaterialTheme.typography.titleSmall, color = c.textPrimary)
    }
}

@Composable
private fun AvisoAcessoCard(
    icone: ImageVector,
    cor: Color,
    texto: String,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(cor.copy(alpha = 0.10f))
                .border(1.dp, cor.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.Top,
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text("Atenção", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = cor)
            Spacer(Modifier.height(2.dp))
            Text(texto, fontSize = 12.sp, color = LocalLkTokens.current.textSecondary, lineHeight = 17.sp)
        }
    }
}

/** Alerta acionável (fundo warning 10% / borda warning 30% / botão tonal) — ver protótipo
 *  TO-BE `tobe/screens/EquipamentoInternet.jsx`, função `AlertCard` (linhas ~145-154). Botão sem
 *  `onClick` real de propósito: nenhuma das ações candidatas ("Executar diagnóstico") está
 *  ligada a um fluxo de navegação real ainda (ver GH#1031). */
@Composable
private fun AlertaCard(
    alerta: EquipmentAlertUi,
    onAcionar: () -> Unit,
) {
    val cor = LkColors.warning
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(cor.copy(alpha = 0.10f))
                .border(1.dp, cor.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.base),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.sm),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = cor, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(LkSpacing.sm))
            Column(modifier = Modifier.weight(1f)) {
                Text(alerta.titulo, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.W600, color = LocalLkTokens.current.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(alerta.descricao, fontSize = 12.sp, color = LocalLkTokens.current.textSecondary, lineHeight = 17.sp)
            }
        }
        FilledTonalButton(onClick = onAcionar) {
            Text(alerta.botaoLabel)
        }
    }
}

@Composable
private fun ReiniciarEquipamentoRow(
    onClick: () -> Unit,
    c: LkTokens,
) {
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.surfaceContainer)
                .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(Icons.Outlined.RestartAlt, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Column(modifier = Modifier.weight(1f)) {
            Text("Reiniciar equipamento", fontSize = 13.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
            Text(
                "O equipamento fica indisponível por alguns minutos após reiniciar.",
                fontSize = 11.sp,
                // GH#937: mesma correção de contraste (ver acessoLabel acima).
                color = c.textSecondary,
            )
        }
        OutlinedButton(onClick = onClick, shape = RoundedCornerShape(14.dp)) { Text("Reiniciar", color = LkColors.error) }
    }
}

@Composable
private fun ReiniciarEquipamentoDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
        containerColor = LocalLkTokens.current.surfaceContainerHigh,
        shape = RoundedCornerShape(LkRadius.dialog),
        title = { Text("Reiniciar equipamento?", fontWeight = FontWeight.W600) },
        text = {
            Text(
                "O equipamento vai desligar e ligar novamente. Durante esse tempo — geralmente " +
                    "de 1 a 3 minutos — você fica sem internet e sem acesso a esta tela, até ele " +
                    "voltar a responder.",
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirmar) {
                Text("Reiniciar", color = LkColors.error)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}

@Composable
private fun DeviceSelectorCard(
    paineis: List<EquipmentPanelUi>,
    selecionadoId: String,
    onSelecionar: (String) -> Unit,
    c: LkTokens,
) {
    var expandido by remember { mutableStateOf(false) }
    val selecionado = paineis.first { it.id == selecionadoId }

    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        LkSectionOverline(text = "Equipamento")
        OutlinedButton(
            onClick = { expandido = !expandido },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(LkRadius.input),
        ) {
            Text(
                text = "${selecionado.vendor} ${selecionado.modelo} — ${selecionado.papel}",
                modifier = Modifier.weight(1f),
                textAlign = TextAlign.Start,
                color = c.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = if (expandido) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = c.textSecondary,
            )
        }

        if (expandido) {
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(LkRadius.input))
                        .border(1.dp, c.outlineVariant, RoundedCornerShape(LkRadius.input)),
            ) {
                paineis.forEachIndexed { index, painel ->
                    val ativo = painel.id == selecionadoId
                    Row(
                        modifier =
                            Modifier
                                .fillMaxWidth()
                                .background(if (ativo) c.secondaryContainer else c.surfaceContainerLow)
                                .clickable {
                                    onSelecionar(painel.id)
                                    expandido = false
                                }.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${painel.vendor} ${painel.modelo} — ${painel.papel}",
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (ativo) c.onSecondaryContainer else c.textPrimary,
                        )
                    }
                    if (index < paineis.lastIndex) {
                        Box(
                            modifier =
                                Modifier
                                    .fillMaxWidth()
                                    .height(1.dp)
                                    .background(c.outlineVariant),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TopologiaRedeCard(
    paineis: List<EquipmentPanelUi>,
    selecionadoId: String,
    warning: String?,
    c: LkTokens,
) {
    val compacto = paineis.size <= 1
    LkSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.AccountTree, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                text = "Como sua rede está conectada",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
            )
        }
        Spacer(Modifier.height(LkSpacing.md))
        if (compacto) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                CompactTopologyNode(
                    label = "Internet",
                    icon = Icons.Outlined.AccountTree,
                    highlighted = false,
                    c = c,
                )
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(15.dp))
                CompactTopologyNode(
                    label = paineis.first().topologyLabel,
                    icon = Icons.Outlined.Router,
                    highlighted = true,
                    c = c,
                )
                Icon(Icons.AutoMirrored.Outlined.ArrowForward, contentDescription = null, tint = c.textTertiary, modifier = Modifier.size(15.dp))
                CompactTopologyNode(
                    label = "Este celular",
                    icon = Icons.Outlined.Smartphone,
                    highlighted = false,
                    c = c,
                )
            }
        } else {
            val nos = buildTopologyNodes(paineis, selecionadoId)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                nos.forEachIndexed { index, no ->
                    if (index > 0) {
                        Box(
                            modifier =
                                Modifier
                                    .padding(start = 16.dp)
                                    .width(1.dp)
                                    .height(14.dp)
                                    .background(c.outlineVariant),
                        )
                    }
                    TopologyNodeRow(node = no, c = c)
                }
            }
        }
        warning?.let {
            Spacer(Modifier.height(LkSpacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = LkColors.warning, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(6.dp))
                Text(it, style = MaterialTheme.typography.bodySmall, color = LkColors.warning)
            }
        }
    }
}

@Composable
private fun CompactTopologyNode(
    label: String,
    icon: ImageVector,
    highlighted: Boolean,
    c: LkTokens,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (highlighted) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (highlighted) c.primary else c.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = if (highlighted) c.primary else c.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TopologyNodeRow(
    node: TopologyNodeUi,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(if (node.highlighted) c.primary.copy(alpha = 0.16f) else c.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = node.icon,
                contentDescription = null,
                tint = if (node.highlighted) c.primary else c.textSecondary,
                modifier = Modifier.size(17.dp),
            )
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Column {
            Text(node.label, style = MaterialTheme.typography.labelLarge, color = c.textPrimary)
            node.sub?.let { Text(it, style = MaterialTheme.typography.bodySmall, color = c.textSecondary) }
        }
    }
}

@Composable
private fun ModuloTecnicoCard(
    secao: EquipamentoSecaoTecnica,
    c: LkTokens,
) {
    val tituloExibido = if (secao.titulo == "Fibra óptica") "Fibra" else secao.titulo
    val toggleLabel = if (secao.titulo == "Fibra óptica") "Ver detalhes técnicos" else "Ver detalhes"
    var expandido by remember(secao.titulo) { mutableStateOf(secao.titulo != "Fibra óptica") }

    LkSurfaceCard {
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

@Composable
private fun DataRowCard(
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
private fun ClienteResumoRow(
    cliente: io.signallq.app.ui.component.ClienteConectadoUi,
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
private fun DevicesSummaryCard(
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

@Composable
private fun DeviceInfoSectionCard(
    linhas: List<Pair<String, String>>,
    acesso: String,
    acessoColor: Color,
    c: LkTokens,
) {
    var expandido by remember { mutableStateOf(false) }
    LkSurfaceCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Router, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(LkSpacing.xs))
            Text(
                text = "Equipamento",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.W600,
                color = c.textPrimary,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = if (expandido) "Ocultar informações" else "Ver informações",
                style = MaterialTheme.typography.labelLarge,
                color = c.primary,
                modifier = Modifier.clickable { expandido = !expandido },
            )
        }
        if (expandido) {
            Spacer(Modifier.height(LkSpacing.sm))
            linhas.forEachIndexed { index, linha ->
                DataRowCard(item = EquipamentoItemTecnico(linha.first, linha.second), c = c)
                if (index < linhas.lastIndex) Spacer(Modifier.height(6.dp))
            }
        }
        Spacer(Modifier.height(LkSpacing.sm))
        Box(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(c.outlineVariant),
        )
        Spacer(Modifier.height(LkSpacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Text(
                text = "Acesso ao equipamento",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = acesso,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelLarge,
                color = acessoColor,
                textAlign = TextAlign.End,
            )
        }
    }
}

@Composable
private fun ActionsSectionCard(
    actions: List<EquipmentActionUi>,
    onSolicitarReiniciar: () -> Unit,
    onVerDispositivos: () -> Unit,
    onExecutarDiagnostico: () -> Unit,
    onVerDetalhesWifi: () -> Unit,
    c: LkTokens,
) {
    Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.sm)) {
        LkSectionOverline(text = "Ações disponíveis")
        actions.forEach { action ->
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.surfaceContainer)
                        .border(1.dp, c.outlineVariant, RoundedCornerShape(14.dp))
                        .clickable(enabled = action.enabled) {
                            when (action.id) {
                                "restart" -> onSolicitarReiniciar()
                                "devices" -> onVerDispositivos()
                                "diagnosis" -> onExecutarDiagnostico()
                                "wifi" -> onVerDetalhesWifi()
                            }
                        }.padding(horizontal = 14.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(action.icon, contentDescription = null, tint = if (action.danger) LkColors.error else c.textSecondary, modifier = Modifier.size(19.dp))
                Spacer(Modifier.width(LkSpacing.md))
                Text(
                    text = action.label,
                    style = MaterialTheme.typography.titleSmall,
                    color =
                        when {
                            !action.enabled -> c.textTertiary
                            action.danger -> LkColors.error
                            else -> c.textPrimary
                        },
                    modifier = Modifier.weight(1f),
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = if (action.enabled) c.textTertiary else c.outlineVariant,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}

private fun EquipamentoSecaoTecnica.normalizarItem(item: EquipamentoItemTecnico): EquipamentoItemTecnico =
    if (titulo == "Fibra óptica" && item.label == "Link óptico") {
        item.copy(label = "Conexão PON")
    } else {
        item
    }

private data class EquipmentPanelUi(
    val id: String,
    val vendor: String,
    val modelo: String,
    val papel: String,
    val topologyLabel: String,
    val deviceTypeLabel: String,
    val atualizacaoLabel: String,
    val statusTitulo: String,
    val statusDescricao: String,
    val statusColor: Color,
    val suportaFibra: Boolean,
    val suportaWifi: Boolean,
    val totalClientes: Int,
    val acessoLabel: String,
    val mostrarAvisoLeituraParcial: Boolean,
    val gponSaude: GponSaudeStatus?,
    val alerta: EquipmentAlertUi?,
    val topologyWarning: String?,
    val secoesTecnicas: List<EquipamentoSecaoTecnica>,
    val devicesSummary: DevicesSummaryUi?,
    val infoRows: List<Pair<String, String>>,
    val actions: List<EquipmentActionUi>,
    val podeReiniciar: Boolean,
)

/** Ver protótipo TO-BE `tobe/screens/EquipamentoInternet.jsx`, cenário `gpon-bad`
 *  (alert.title/desc/button, linha ~297). */
private data class EquipmentAlertUi(
    val titulo: String,
    val descricao: String,
    val botaoLabel: String,
)

/** Sinal óptico abaixo do esperado (GponSaudeStatus.ruim) é o único gatilho real disponível
 *  hoje nesta tela — canal 2,4GHz congestionado (segundo cenário do protótipo) exigiria dado de
 *  varredura de redes vizinhas que só existe em featureDiagnostico/SinalScreen, não plumbado
 *  aqui (ver GH#1031). */
private fun alertaSinalOptico(gponSaude: GponSaudeStatus?): EquipmentAlertUi? =
    if (gponSaude == GponSaudeStatus.ruim) {
        EquipmentAlertUi(
            titulo = "Sinal óptico abaixo do esperado",
            descricao = "O problema pode estar na fibra ou na instalação da operadora, e não no Wi-Fi.",
            botaoLabel = "Executar diagnóstico",
        )
    } else {
        null
    }

private data class DevicesSummaryUi(
    val total: Int,
    val wifi: Int,
    val cabo: Int,
    val flags: List<String> = emptyList(),
)

private data class EquipmentActionUi(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val danger: Boolean = false,
)

private data class TopologyNodeUi(
    val label: String,
    val sub: String? = null,
    val icon: ImageVector,
    val highlighted: Boolean = false,
)

private fun buildEquipmentPanels(
    localDevice: LocalNetworkDeviceSnapshot,
    estadoSecao: LocalDeviceSectionUiState.Conectado,
    snapshotFibra: SnapshotFibra,
    acesso: AcessoEquipamento,
    doubleNatSuspeito: Boolean,
): List<EquipmentPanelUi> {
    val paineis = mutableListOf<EquipmentPanelUi>()
    val gponSaudeAtual =
        localDevice.fiber?.let { fiber ->
            val rx = fiber.rxPowerDbm
            val tx = fiber.txPowerDbm
            val temperatura = fiber.temperaturaCelsius
            if (rx != null && tx != null && temperatura != null) {
                ClassificadorSaudeGpon.classificar(
                    isUp = fiber.linkAtivo ?: true,
                    rxPowerDbm = rx,
                    txPowerDbm = tx,
                    temperatureCelsius = temperatura,
                )
            } else {
                null
            }
        }
    paineis +=
        EquipmentPanelUi(
            id = "current",
            vendor = localDevice.vendor?.ifBlank { null } ?: "Equipamento",
            modelo = localDevice.modelo?.ifBlank { null } ?: "local",
            papel = deviceRole(localDevice.deviceType),
            topologyLabel = localDevice.modelo?.ifBlank { null } ?: localDevice.deviceType.label(),
            deviceTypeLabel = localDevice.deviceType.label(),
            atualizacaoLabel = freshnessLabel(localDevice.freshness.capturadoEmEpochMs),
            statusTitulo = accessTitle(acesso),
            statusDescricao = accessDescription(acesso),
            statusColor = accessColor(acesso),
            suportaFibra = localDevice.capabilities.suportaFibra,
            suportaWifi = localDevice.capabilities.suportaWifi,
            totalClientes = localDevice.clientes.size,
            acessoLabel = acessoLabel(acesso),
            mostrarAvisoLeituraParcial = acesso == AcessoEquipamento.LEITURA_PARCIAL,
            gponSaude = gponSaudeAtual,
            alerta = alertaSinalOptico(gponSaudeAtual),
            topologyWarning =
                if (doubleNatSuspeito) {
                    "Possível NAT duplo detectado: seu equipamento e um roteador adicional podem estar fazendo NAT ao mesmo tempo. Isso pode causar problemas em jogos online e chamadas de vídeo."
                } else {
                    null
                },
            secoesTecnicas = estadoSecao.secoes,
            devicesSummary = localDevice.toDevicesSummary(),
            infoRows = buildInfoRows(localDevice, snapshotFibra.deviceInfo, snapshotFibra.gpon?.mode, snapshotFibra.gatewayIpDetectado),
            actions = buildActions(localDevice, estadoSecao, acesso),
            podeReiniciar = acesso == AcessoEquipamento.GERENCIAMENTO_DISPONIVEL,
        )

    val deveCriarPainelOnt =
        localDevice.deviceType != io.signallq.app.core.network.contracts.localdevice.DeviceType.ONT_GPON &&
            snapshotFibra.deviceInfo != null &&
            snapshotFibra.gpon != null
    if (deveCriarPainelOnt) {
        paineis += buildOntPanel(snapshotFibra)
    }
    return paineis
}

private fun buildOntPanel(snapshotFibra: SnapshotFibra): EquipmentPanelUi {
    val info = requireNotNull(snapshotFibra.deviceInfo)
    val gpon = requireNotNull(snapshotFibra.gpon)
    val secoes = buildSectionsFromFibra(snapshotFibra)
    val gponSaudeOnt =
        ClassificadorSaudeGpon.classificar(
            isUp = gpon.isUp,
            rxPowerDbm = gpon.rxPowerDbm,
            txPowerDbm = gpon.txPowerDbm,
            temperatureCelsius = gpon.temperatureCelsius,
        )
    return EquipmentPanelUi(
        id = "ont",
        vendor = info.manufacturer,
        modelo = info.model,
        papel = "equipamento da operadora",
        topologyLabel = info.model,
        deviceTypeLabel = "Equipamento de fibra",
        atualizacaoLabel = "Atualizado nesta leitura",
        statusTitulo = if (gpon.isUp) "Conectado ao equipamento" else "Leitura parcial",
        statusDescricao = if (gpon.isUp) "A ONT está respondendo e a fibra foi lida com sucesso." else "A ONT respondeu, mas o link óptico ou parte da leitura exige atenção.",
        statusColor = if (gpon.isUp) LkColors.success else LkColors.warning,
        suportaFibra = true,
        suportaWifi = snapshotFibra.wifi?.radios?.isNotEmpty() == true,
        totalClientes = snapshotFibra.clientes.size,
        acessoLabel = "Leitura completa",
        mostrarAvisoLeituraParcial = false,
        gponSaude = gponSaudeOnt,
        alerta = alertaSinalOptico(gponSaudeOnt),
        topologyWarning = null,
        secoesTecnicas = secoes,
        devicesSummary = snapshotFibra.toDevicesSummary(),
        infoRows = buildInfoRows(snapshotFibra.deviceInfo, snapshotFibra.gpon, snapshotFibra.wan, snapshotFibra.lan, snapshotFibra.gatewayIpDetectado),
        actions = listOf(EquipmentActionUi(id = "diagnosis", label = "Executar diagnóstico", icon = Icons.Outlined.WarningAmber)),
        podeReiniciar = false,
    )
}

private fun buildSectionsFromFibra(snapshotFibra: SnapshotFibra): List<EquipamentoSecaoTecnica> =
    buildList {
        snapshotFibra.gpon?.let { gpon ->
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Fibra óptica",
                    icone = Icons.Outlined.SettingsEthernet,
                    itens =
                        listOf(
                            EquipamentoItemTecnico("Link óptico", if (gpon.isUp) "Ativo" else "Inativo"),
                            EquipamentoItemTecnico("Potência RX", "%.2f dBm".format(gpon.rxPowerDbm)),
                            EquipamentoItemTecnico("Potência TX", "%.2f dBm".format(gpon.txPowerDbm)),
                            EquipamentoItemTecnico("Temperatura", "%.1f °C".format(gpon.temperatureCelsius)),
                            EquipamentoItemTecnico("Modo de operação", gpon.mode),
                            EquipamentoItemTecnico("Número de série da ONT", gpon.serial),
                        ),
                ),
            )
        }
        snapshotFibra.wan?.let { wan ->
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Internet (WAN)",
                    icone = Icons.Outlined.AccountTree,
                    itens =
                        listOf(
                            EquipamentoItemTecnico("IP externo", wan.externalIp),
                            EquipamentoItemTecnico("Gateway", wan.gateway),
                            EquipamentoItemTecnico("DNS primário", wan.primaryDns),
                            EquipamentoItemTecnico("DNS secundário", wan.secondaryDns),
                            EquipamentoItemTecnico("Tipo de conexão", wan.connectionType),
                            EquipamentoItemTecnico("Interface", wan.interfaceName),
                        ),
                ),
            )
        }
        snapshotFibra.wifi?.let { wifi ->
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Wi-Fi",
                    icone = Icons.Outlined.Wifi,
                    itens =
                        wifi.radios.map { radio ->
                            EquipamentoItemTecnico(
                                label = radio.ssid.ifBlank { radio.banda },
                                valor =
                                    buildString {
                                        append(radio.banda)
                                        radio.canal?.let { append(" · canal $it") }
                                        append(if (radio.habilitado) " · ativo" else " · desligado")
                                    },
                            )
                        },
                ),
            )
        }
        snapshotFibra.lan?.let { lan ->
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Rede local (LAN)",
                    icone = Icons.Outlined.Router,
                    itens =
                        listOf(
                            EquipamentoItemTecnico("IP do roteador", lan.routerIp),
                            EquipamentoItemTecnico("Máscara de sub-rede", lan.subnetMask),
                            EquipamentoItemTecnico("DHCP", if (lan.dhcpHabilitado) "Ativo" else "Desligado"),
                            EquipamentoItemTecnico("Faixa DHCP", "${lan.dhcpFaixaInicio} – ${lan.dhcpFaixaFim}"),
                        ),
                ),
            )
        }
    }

private fun buildInfoRows(
    localDevice: LocalNetworkDeviceSnapshot,
    deviceInfo: DeviceInfoFibra?,
    modoOperacao: String?,
    gatewayDetectado: String?,
): List<Pair<String, String>> =
    buildList {
        localDevice.vendor?.takeIf { it.isNotBlank() }?.let { add("Fabricante" to it) }
        localDevice.modelo?.takeIf { it.isNotBlank() }?.let { add("Modelo" to it) }
        localDevice.firmwareVersion?.takeIf { it.isNotBlank() }?.let { add("Firmware" to it) }
        deviceInfo?.hardwareVersion?.takeIf { it.isNotBlank() }?.let { add("Versão de hardware" to it) }
        modoOperacao?.takeIf { it.isNotBlank() }?.let { add("Modo de operação" to it) }
        (localDevice.lan?.ipRoteador ?: gatewayDetectado)?.takeIf { it.isNotBlank() }?.let { add("Endereço local" to it) }
        localDevice.wan
            ?.tipoConexao
            ?.takeIf { it.isNotBlank() }
            ?.let { add("Tipo de conexão" to it) }
        deviceInfo?.serialNumber?.takeIf { it.isNotBlank() }?.let { add("Número de série" to it) }
    }

private fun buildInfoRows(
    deviceInfo: DeviceInfoFibra?,
    gpon: GponStatus?,
    wan: WanStatus?,
    lan: LanStatus?,
    gatewayDetectado: String?,
): List<Pair<String, String>> =
    buildList {
        deviceInfo?.manufacturer?.takeIf { it.isNotBlank() }?.let { add("Fabricante" to it) }
        deviceInfo?.model?.takeIf { it.isNotBlank() }?.let { add("Modelo" to it) }
        deviceInfo?.hardwareVersion?.takeIf { it.isNotBlank() }?.let { add("Versão de hardware" to it) }
        deviceInfo?.firmwareVersion?.takeIf { it.isNotBlank() }?.let { add("Firmware" to it) }
        gpon?.mode?.takeIf { it.isNotBlank() }?.let { add("Modo de operação" to it) }
        (lan?.routerIp ?: gatewayDetectado)?.takeIf { it.isNotBlank() }?.let { add("Endereço local" to it) }
        wan?.connectionType?.takeIf { it.isNotBlank() }?.let { add("Tipo de conexão" to it) }
        deviceInfo?.serialNumber?.takeIf { it.isNotBlank() }?.let { add("Número de série" to it) }
    }

private fun buildActions(
    localDevice: LocalNetworkDeviceSnapshot,
    estadoSecao: LocalDeviceSectionUiState.Conectado,
    acesso: AcessoEquipamento,
): List<EquipmentActionUi> =
    buildList {
        if (localDevice.clientes.isNotEmpty()) {
            add(EquipmentActionUi(id = "devices", label = "Ver dispositivos", icon = Icons.Outlined.Devices))
        }
        if (estadoSecao.suportaDiagnosticoNativo) {
            add(EquipmentActionUi(id = "diagnosis", label = "Executar diagnóstico", icon = Icons.Outlined.Troubleshoot))
        }
        if (localDevice.capabilities.suportaWifi) {
            add(EquipmentActionUi(id = "wifi", label = "Ver detalhes do Wi-Fi", icon = Icons.Outlined.Wifi))
        }
        if (acesso == AcessoEquipamento.GERENCIAMENTO_DISPONIVEL) {
            add(EquipmentActionUi(id = "restart", label = "Reiniciar equipamento", icon = Icons.Outlined.RestartAlt, danger = true))
        }
    }

private fun buildTopologyNodes(
    paineis: List<EquipmentPanelUi>,
    selecionadoId: String,
): List<TopologyNodeUi> =
    buildList {
        add(TopologyNodeUi(label = "Internet", icon = Icons.Outlined.AccountTree))
        paineis.forEach { painel ->
            add(
                TopologyNodeUi(
                    label = "${painel.vendor} ${painel.modelo}",
                    sub = painel.papel.replaceFirstChar { it.titlecase() },
                    icon =
                        when (painel.deviceTypeLabel) {
                            "Equipamento de fibra" -> Icons.Outlined.Router
                            "Ponto de acesso / mesh" -> Icons.Outlined.Devices
                            else -> Icons.Outlined.Router
                        },
                    highlighted = painel.id == selecionadoId,
                ),
            )
        }
        add(TopologyNodeUi(label = "Este celular", icon = Icons.Outlined.Smartphone))
    }

// GH#983 Fase 4 — usa tipoConexaoFisica (normalizado por NokiaModemParser) em vez de
// tipoConexao (string crua) com .contains ad-hoc — nunca consome a string crua fora do parser.
private fun LocalNetworkDeviceSnapshot.toDevicesSummary(): DevicesSummaryUi? {
    if (clientes.isEmpty()) return null
    val wifi = clientes.count { it.tipoConexaoFisica == TipoConexaoFisica.WIFI }
    val cabo = clientes.count { it.tipoConexaoFisica == TipoConexaoFisica.ETHERNET }
    return DevicesSummaryUi(total = clientes.size, wifi = wifi, cabo = cabo)
}

private fun SnapshotFibra.toDevicesSummary(): DevicesSummaryUi? {
    if (clientes.isEmpty()) return null
    val wifi = clientes.count { it.tipoConexaoFisica == TipoConexaoFisica.WIFI }
    val cabo = clientes.count { it.tipoConexaoFisica == TipoConexaoFisica.ETHERNET }
    return DevicesSummaryUi(total = clientes.size, wifi = wifi, cabo = cabo)
}

private fun accessTitle(acesso: AcessoEquipamento): String =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA -> "Conectado ao equipamento"
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL -> "Gerenciamento disponível"
        AcessoEquipamento.LEITURA_PARCIAL -> "Leitura parcial"
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> "Somente identificação"
        AcessoEquipamento.SESSAO_EXPIRADA -> "Sessão expirada"
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS -> "Credenciais necessárias"
    }

private fun accessDescription(acesso: AcessoEquipamento): String =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA -> "Todos os módulos disponíveis nesta leitura foram carregados."
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL -> "Além da leitura, já é possível acionar funções compatíveis do equipamento."
        AcessoEquipamento.LEITURA_PARCIAL -> "Algumas seções vieram incompletas, mas já há dados úteis para análise."
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> "O equipamento foi identificado, mas esta leitura não está completa."
        AcessoEquipamento.SESSAO_EXPIRADA -> "A sessão com o equipamento expirou e precisa ser refeita."
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS -> "As credenciais do equipamento ainda precisam ser configuradas."
    }

private fun accessColor(acesso: AcessoEquipamento): Color =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA,
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL,
        -> LkColors.success
        AcessoEquipamento.LEITURA_PARCIAL,
        AcessoEquipamento.SOMENTE_IDENTIFICACAO,
        -> LkColors.warning
        AcessoEquipamento.SESSAO_EXPIRADA,
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS,
        -> LkColors.error
    }

private fun deviceRole(deviceType: io.signallq.app.core.network.contracts.localdevice.DeviceType): String =
    when (deviceType) {
        io.signallq.app.core.network.contracts.localdevice.DeviceType.ONT_GPON -> "equipamento da operadora"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.ROUTER -> "roteador principal"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.MESH_OR_EXTENDER -> "ponto de acesso"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.UNKNOWN_SUPPORTED,
        io.signallq.app.core.network.contracts.localdevice.DeviceType.UNKNOWN_UNSUPPORTED,
        -> "equipamento local"
    }

private fun freshnessLabel(capturadoEmEpochMs: Long): String {
    val diffMin = ((System.currentTimeMillis() - capturadoEmEpochMs).coerceAtLeast(0L)) / 60_000L
    return when {
        diffMin <= 0L -> "Atualizado agora"
        diffMin == 1L -> "Atualizado há 1 minuto"
        diffMin < 60L -> "Atualizado há $diffMin minutos"
        else -> "Atualizado nesta leitura"
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

private fun io.signallq.app.core.network.contracts.localdevice.DeviceType.label(): String =
    when (this) {
        io.signallq.app.core.network.contracts.localdevice.DeviceType.ONT_GPON -> "Equipamento de fibra"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.ROUTER -> "Roteador Wi-Fi"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.MESH_OR_EXTENDER -> "Ponto de acesso / mesh"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.UNKNOWN_SUPPORTED,
        io.signallq.app.core.network.contracts.localdevice.DeviceType.UNKNOWN_UNSUPPORTED,
        -> "Equipamento local"
    }

@Composable
private fun EquipamentoAcessoIndisponivelContent(
    acesso: AcessoEquipamento,
    dentroDaJanelaPosReboot: Boolean,
    onRetentar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    val (icone, titulo, descricao, mostrarRevisarConfig) =
        when (acesso) {
            AcessoEquipamento.CREDENCIAIS_NECESSARIAS ->
                AcessoIndisponivelCopy(
                    Icons.Outlined.Lock,
                    "Configure o acesso ao equipamento",
                    "Informe o IP, usuário e senha do seu roteador ou ONT para o SignallQ conseguir ler os dados dele.",
                    true,
                )

            AcessoEquipamento.SOMENTE_IDENTIFICACAO ->
                AcessoIndisponivelCopy(
                    Icons.AutoMirrored.Outlined.HelpOutline,
                    "Equipamento não suportado",
                    "Identificamos um equipamento nesta rede, mas ainda não sabemos ler os dados dele — " +
                        "isso costuma acontecer quando o modem não é o modelo que o SignallQ já conhece.",
                    true,
                )

            else -> // SESSAO_EXPIRADA e fallback defensivo
                AcessoIndisponivelCopy(
                    Icons.Outlined.ErrorOutline,
                    if (dentroDaJanelaPosReboot) "O equipamento está reiniciando" else "Não consegui acessar o equipamento agora",
                    if (dentroDaJanelaPosReboot) {
                        "Isso pode levar alguns minutos. Tente atualizar novamente daqui a pouco."
                    } else {
                        "Verifique o IP, o usuário e a senha nas configurações do equipamento."
                    },
                    true,
                )
        }

    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
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
                Icon(icone, contentDescription = null, tint = LkColors.warning, modifier = Modifier.size(36.dp))
            }
            Spacer(Modifier.height(LkSpacing.lg))
            Text(titulo, fontSize = 17.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
            Spacer(Modifier.height(LkSpacing.sm))
            Text(descricao, fontSize = 13.sp, color = c.textSecondary, lineHeight = 19.sp)
            Spacer(Modifier.height(LkSpacing.xl))
            Button(
                onClick = onRetentar,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                shape = RoundedCornerShape(LkRadius.button),
            ) {
                Text("Tentar novamente", fontSize = 14.sp, fontWeight = FontWeight.W600)
            }
            if (mostrarRevisarConfig) {
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
}

private data class AcessoIndisponivelCopy(
    val icone: ImageVector,
    val titulo: String,
    val descricao: String,
    val mostrarRevisarConfig: Boolean,
)

private fun acessoLabel(acesso: AcessoEquipamento): String =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA -> "Leitura completa"
        AcessoEquipamento.LEITURA_PARCIAL -> "Leitura parcial"
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> "Somente identificação"
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL -> "Gerenciamento disponível"
        AcessoEquipamento.SESSAO_EXPIRADA -> "Sessão expirada"
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS -> "Credenciais necessárias"
    }
