package io.signallq.app.ui.screen

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.MenuAnchorType
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R
import io.signallq.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.signallq.app.core.network.contracts.fibra.GponSaudeStatus
import io.signallq.app.core.network.contracts.gateway.AcessoEquipamento
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.feature.diagnostico.topology.model.NatStatus
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.GponStatus
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LocalDeviceSection
import io.signallq.app.ui.component.LocalDeviceSectionUiState
import io.signallq.app.ui.component.Skeleton
import io.signallq.app.ui.component.SkeletonCard
import io.signallq.app.ui.component.deviceTypeIcon
import io.signallq.app.ui.component.deviceTypeLabel
import io.signallq.app.ui.component.formatarFrescor
import io.signallq.app.ui.component.mapLocalDeviceSectionUiState
import io.signallq.app.ui.component.tituloEquipamento

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
 * [Identification] + [DeviceSelector] + [StatusCard] + [Topology] (estrutura
 * universal da spec To-Be, GH#934 reauditoria), alerta de Double NAT e a ação
 * de reiniciar (só quando o driver declara `suportaGerenciamento`).
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
            CenterAlignedTopAppBar(
                title = {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            "Equipamento de internet",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.W600,
                            color = c.textPrimary,
                        )
                        Text(
                            acessoLabel(acesso),
                            fontSize = 12.sp,
                            // GH#937: textTertiary (#9CA3AF) sobre branco dava ~2.5:1 (fail AA).
                            // textSecondary fica ~4.8:1 (AA).
                            color = c.textSecondary,
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
                        Icon(Icons.Outlined.Refresh, contentDescription = "Atualizar", tint = c.textPrimary)
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(containerColor = c.bgPrimary),
            )
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
                    gpon = snapshotFibra.gpon,
                    acesso = acesso,
                    doubleNatSuspeito = doubleNatSuspeito,
                    onSolicitarReiniciar = { mostrarDialogoReiniciar = true },
                    c = c,
                    modifier = Modifier.padding(padding),
                )

            else ->
                EquipamentoAcessoIndisponivelContent(
                    acesso = acesso,
                    localDevice = localDevice,
                    gpon = snapshotFibra.gpon,
                    dentroDaJanelaPosReboot = dentroDaJanelaPosReboot,
                    onRetentar = onRetentar,
                    onAbrirAjustes = onAbrirAjustes,
                    c = c,
                    modifier = Modifier.padding(padding),
                )
        }
    }
}

/** Estado de carregamento (GH#934 reauditoria) — blocos [Skeleton]/[SkeletonCard]
 *  pulsantes no lugar de cada seção do corpo real (Identification, StatusCard,
 *  Topology, módulo de dados técnicos), em vez do spinner central genérico
 *  anterior. Reaproveita o mesmo componente já usado pela tela 1a — nada novo
 *  criado aqui. */
@Composable
private fun EquipamentoCarregando(
    modifier: Modifier = Modifier,
    c: LkTokens,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Spacer(Modifier.height(LkSpacing.xs))

        // Identification
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier =
                    Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(c.bgSecondary),
            )
            Spacer(Modifier.width(LkSpacing.md))
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Skeleton(height = 20.dp, modifier = Modifier.fillMaxWidth(0.6f))
                Skeleton(height = 14.dp, modifier = Modifier.fillMaxWidth(0.4f))
            }
        }

        // StatusCard
        SkeletonCard()
        // Topology
        SkeletonCard()
        // Módulo de dados técnicos (Fibra/Wi-Fi/WAN...)
        SkeletonCard()
        SkeletonCard()

        Spacer(Modifier.height(LkSpacing.lg))
    }
}

@Composable
private fun EquipamentoConectadoContent(
    localDevice: LocalNetworkDeviceSnapshot?,
    gpon: GponStatus?,
    acesso: AcessoEquipamento,
    doubleNatSuspeito: Boolean,
    onSolicitarReiniciar: () -> Unit,
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

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Spacer(Modifier.height(LkSpacing.xs))

        Identification(localDevice = localDevice, c = c)

        DeviceSelector(
            opcoes = listOf(EquipamentoOpcao(id = "atual", label = tituloEquipamento(localDevice))),
            selecionadoId = "atual",
            onSelecionar = {},
            c = c,
        )

        StatusCard(
            icone = statusCardIcone(acesso),
            titulo = statusCardTitulo(acesso, dentroDaJanelaPosReboot = false),
            descricao = statusCardDescricao(acesso, dentroDaJanelaPosReboot = false),
            cor = acessoStatusColor(acesso),
            stats = statusCardStats(acesso, localDevice, gpon),
        )

        if (acesso == AcessoEquipamento.LEITURA_PARCIAL) {
            AvisoAcessoCard(
                icone = Icons.Outlined.ErrorOutline,
                cor = LkColors.warning,
                texto = "Leitura parcial — algumas seções deste equipamento não vieram preenchidas nesta captura.",
            )
        }

        Topology(
            deviceType = localDevice.deviceType,
            deviceLabel = tituloEquipamento(localDevice),
            quantidadeClientes = if (localDevice.capabilities.suportaClientes) localDevice.clientes.size else null,
            doubleNatSuspeito = doubleNatSuspeito,
            c = c,
        )

        gpon?.let {
            SaudeOpticaBadge(
                status =
                    ClassificadorSaudeGpon.classificar(
                        isUp = it.isUp,
                        rxPowerDbm = it.rxPowerDbm,
                        txPowerDbm = it.txPowerDbm,
                        temperatureCelsius = it.temperatureCelsius,
                    ),
            )
        }

        LocalDeviceSection(state = estadoSecao, refazerDisponivel = true)

        if (acesso == AcessoEquipamento.GERENCIAMENTO_DISPONIVEL) {
            ReiniciarEquipamentoRow(onClick = onSolicitarReiniciar, c = c)
        }

        Spacer(Modifier.height(LkSpacing.lg))
    }
}

/** Bloco "Identification" da spec To-Be (5b): fabricante+modelo (`headlineSmall`),
 *  tipo de equipamento (`bodyMedium`) e frescor da leitura (`labelMedium`), com a
 *  foto real do equipamento quando reconhecido (hoje só o Nokia G-1425G-B via
 *  `ExecutorFibra` — fallback pro ícone genérico do tipo em qualquer outro caso,
 *  mesmo padrão de fallback do [io.signallq.app.ui.OperadoraLogoCatalog]). */
@Composable
private fun Identification(
    localDevice: LocalNetworkDeviceSnapshot,
    c: LkTokens,
) {
    val ehNokiaConhecido = localDevice.vendor?.trim()?.equals("Nokia", ignoreCase = true) == true

    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(c.bgSecondary),
            contentAlignment = Alignment.Center,
        ) {
            if (ehNokiaConhecido) {
                Image(
                    painter = painterResource(R.drawable.ont_nokia_g1425g),
                    contentDescription = null,
                    modifier = Modifier.size(44.dp),
                    contentScale = ContentScale.Fit,
                )
            } else {
                Icon(
                    deviceTypeIcon(localDevice.deviceType),
                    contentDescription = null,
                    tint = c.textSecondary,
                    modifier = Modifier.size(26.dp),
                )
            }
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column {
            Text(
                tituloEquipamento(localDevice),
                style = MaterialTheme.typography.headlineSmall,
                color = c.textPrimary,
            )
            Text(
                deviceTypeLabel(localDevice.deviceType),
                style = MaterialTheme.typography.bodyMedium,
                color = c.textSecondary,
            )
            Text(
                formatarFrescor(localDevice.freshness.capturadoEmEpochMs),
                style = MaterialTheme.typography.labelMedium,
                color = c.textTertiary,
            )
        }
    }
}

private data class EquipamentoOpcao(
    val id: String,
    val label: String,
)

/**
 * Bloco "DeviceSelector" da spec To-Be (5b) — dropdown que só aparece com mais
 * de 1 equipamento. Hoje a única fonte real de equipamento (`ExecutorFibra`,
 * Nokia G-1425G-B) produz no máximo 1 item, então [opcoes] nunca ultrapassa 1
 * em produção e este componente fica inerte (early return) — preparado para
 * quando existir uma segunda fonte real, sem simular um equipamento fake só
 * para exercitar o dropdown (GH#934).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DeviceSelector(
    opcoes: List<EquipamentoOpcao>,
    selecionadoId: String,
    onSelecionar: (String) -> Unit,
    c: LkTokens,
) {
    if (opcoes.size <= 1) return

    var expanded by remember { mutableStateOf(false) }
    val selecionado = opcoes.firstOrNull { it.id == selecionadoId } ?: opcoes.first()

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = it },
        modifier = Modifier.fillMaxWidth(),
    ) {
        OutlinedTextField(
            value = selecionado.label,
            onValueChange = {},
            readOnly = true,
            label = { Text("Equipamento") },
            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
            modifier =
                Modifier
                    .fillMaxWidth()
                    .menuAnchor(type = MenuAnchorType.PrimaryNotEditable),
            colors =
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = LkColors.accent,
                    focusedLabelColor = LkColors.accent,
                ),
            singleLine = true,
        )
        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.background(c.bgSecondary),
        ) {
            opcoes.forEach { opcao ->
                DropdownMenuItem(
                    text = {
                        Text(
                            opcao.label,
                            color = if (opcao.id == selecionado.id) LkColors.accent else c.textPrimary,
                            fontWeight = if (opcao.id == selecionado.id) FontWeight.W600 else FontWeight.W400,
                        )
                    },
                    onClick = {
                        onSelecionar(opcao.id)
                        expanded = false
                    },
                    modifier =
                        Modifier.background(
                            if (opcao.id == selecionado.id) LkColors.accent.copy(alpha = 0.08f) else c.bgSecondary,
                        ),
                )
            }
        }
    }
}

/**
 * "StatusCard" da spec To-Be (5b): fundo cor-de-status a 10% + borda a 30%,
 * radius 16px, ícone 26px + título `titleLarge` + descrição `bodyMedium` +
 * grade 2 colunas de estatísticas — [stats] vazio (estados sem equipamento
 * lido) simplesmente omite a grade, nunca inventa estatística.
 */
@Composable
private fun StatusCard(
    icone: ImageVector,
    titulo: String,
    descricao: String,
    cor: Color,
    stats: List<Pair<String, String>>,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(cor.copy(alpha = 0.10f))
                .border(1.dp, cor.copy(alpha = 0.30f), RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
    ) {
        Row(verticalAlignment = Alignment.Top) {
            Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(26.dp))
            Spacer(Modifier.width(LkSpacing.md))
            Column(modifier = Modifier.weight(1f)) {
                Text(titulo, style = MaterialTheme.typography.titleLarge, color = cor)
                Spacer(Modifier.height(2.dp))
                Text(descricao, style = MaterialTheme.typography.bodyMedium, color = cor.copy(alpha = 0.85f))
            }
        }

        if (stats.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.md))
            stats.chunked(2).forEach { linha ->
                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                    linha.forEach { (label, valor) ->
                        Column(modifier = Modifier.weight(1f)) {
                            Text(label, style = MaterialTheme.typography.labelMedium, color = cor.copy(alpha = 0.7f))
                            Text(valor, style = MaterialTheme.typography.titleSmall, color = cor)
                        }
                    }
                    if (linha.size == 1) Spacer(Modifier.weight(1f))
                }
                Spacer(Modifier.height(LkSpacing.sm))
            }
        }
    }
}

private fun acessoStatusColor(acesso: AcessoEquipamento): Color =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA,
        AcessoEquipamento.LEITURA_PARCIAL,
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL,
        -> LkColors.success
        // Bucket "error" da spec: desconectado/sem resposta/credenciais inválidas.
        AcessoEquipamento.SESSAO_EXPIRADA,
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS,
        -> LkColors.error
        // Bucket "warning" da spec: demais casos.
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> LkColors.warning
    }

private fun statusCardIcone(acesso: AcessoEquipamento): ImageVector =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA,
        AcessoEquipamento.LEITURA_PARCIAL,
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL,
        -> Icons.Outlined.CheckCircle
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> Icons.AutoMirrored.Outlined.HelpOutline
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS -> Icons.Outlined.Lock
        AcessoEquipamento.SESSAO_EXPIRADA -> Icons.Outlined.ErrorOutline
    }

private fun statusCardTitulo(
    acesso: AcessoEquipamento,
    dentroDaJanelaPosReboot: Boolean,
): String =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA, AcessoEquipamento.LEITURA_PARCIAL, AcessoEquipamento.GERENCIAMENTO_DISPONIVEL ->
            "Equipamento conectado"
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> "Equipamento não suportado"
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS -> "Configure o acesso ao equipamento"
        AcessoEquipamento.SESSAO_EXPIRADA ->
            if (dentroDaJanelaPosReboot) "O equipamento está reiniciando" else "Não consegui acessar o equipamento agora"
    }

private fun statusCardDescricao(
    acesso: AcessoEquipamento,
    dentroDaJanelaPosReboot: Boolean,
): String =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA -> "Leitura completa dos dados do equipamento."
        AcessoEquipamento.LEITURA_PARCIAL ->
            "Leitura parcial — algumas seções deste equipamento não vieram preenchidas nesta captura."
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL -> "Leitura completa, com gerenciamento remoto disponível."
        AcessoEquipamento.SOMENTE_IDENTIFICACAO ->
            "Identificamos um equipamento nesta rede, mas ainda não sabemos ler os dados dele — " +
                "isso costuma acontecer quando o modem não é o modelo que o SignallQ já conhece."
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS ->
            "Informe o IP, usuário e senha do seu roteador ou ONT para o SignallQ conseguir ler os dados dele."
        AcessoEquipamento.SESSAO_EXPIRADA ->
            if (dentroDaJanelaPosReboot) {
                "Isso pode levar alguns minutos. Tente atualizar novamente daqui a pouco."
            } else {
                "Verifique o IP, o usuário e a senha nas configurações do equipamento."
            }
    }

/** Estatísticas reais do [StatusCard] — nunca inclui um par quando o dado de
 *  origem não existe (ex.: sinal óptico só entra com [gpon] não nulo). */
private fun statusCardStats(
    acesso: AcessoEquipamento,
    localDevice: LocalNetworkDeviceSnapshot?,
    gpon: GponStatus?,
): List<Pair<String, String>> {
    if (localDevice == null) return emptyList()
    return buildList {
        add("Tipo" to deviceTypeLabel(localDevice.deviceType))
        add("Nível de acesso" to acessoLabel(acesso))
        gpon?.let {
            val status =
                ClassificadorSaudeGpon.classificar(
                    isUp = it.isUp,
                    rxPowerDbm = it.rxPowerDbm,
                    txPowerDbm = it.txPowerDbm,
                    temperatureCelsius = it.temperatureCelsius,
                )
            add("Sinal óptico" to gponSaudeLabel(status))
        }
        add("Atualizado" to formatarFrescor(localDevice.freshness.capturadoEmEpochMs))
    }
}

private fun gponSaudeLabel(status: GponSaudeStatus): String =
    when (status) {
        GponSaudeStatus.boa -> "Bom"
        GponSaudeStatus.regular -> "Regular"
        GponSaudeStatus.ruim -> "Ruim"
    }

/**
 * "Topology" da spec To-Be (5b): nós em círculo 34px + rótulo, coluna vertical
 * ligada por traço 1px, com o alerta de Double NAT agrupado logo abaixo dos
 * nós quando [doubleNatSuspeito] (evidência real já calculada em
 * [suspeitaDoubleNat], nunca por suposição).
 */
@Composable
private fun Topology(
    deviceType: DeviceType,
    deviceLabel: String,
    quantidadeClientes: Int?,
    doubleNatSuspeito: Boolean,
    c: LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
    ) {
        Text(
            text = "MAPA DE REDE",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = c.textTertiary,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(LkSpacing.md))

        TopologyNode(icone = Icons.Outlined.Public, rotulo = "Internet", c = c)
        TopologyConector(c)
        TopologyNode(icone = deviceTypeIcon(deviceType), rotulo = deviceLabel, c = c)
        if (quantidadeClientes != null) {
            TopologyConector(c)
            TopologyNode(
                icone = Icons.Outlined.Devices,
                rotulo = if (quantidadeClientes == 1) "1 dispositivo conectado" else "$quantidadeClientes dispositivos conectados",
                c = c,
            )
        }

        if (doubleNatSuspeito) {
            Spacer(Modifier.height(LkSpacing.md))
            AvisoAcessoCard(
                icone = Icons.Outlined.WarningAmber,
                cor = LkColors.warning,
                texto =
                    "Possível NAT duplo detectado: seu equipamento e um roteador adicional podem estar " +
                        "fazendo NAT ao mesmo tempo. Isso pode causar problemas em jogos online e chamadas de vídeo.",
            )
        }
    }
}

@Composable
private fun TopologyNode(
    icone: ImageVector,
    rotulo: String,
    c: LkTokens,
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier =
                Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(c.bgSecondary),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icone, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(16.dp))
        }
        Spacer(Modifier.width(LkSpacing.sm))
        Text(rotulo, fontSize = 12.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
    }
}

/** Traço vertical 1px conectando dois nós de 34px — recuo de metade do círculo
 *  (17dp) menos metade da própria espessura, pra ficar centralizado sob o nó. */
@Composable
private fun TopologyConector(c: LkTokens) {
    Box(
        modifier =
            Modifier
                .padding(start = 16.dp)
                .width(1.dp)
                .height(16.dp)
                .background(c.outlineVariant),
    )
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
                .clip(RoundedCornerShape(999.dp))
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
                .background(cor.copy(alpha = 0.08f))
                .padding(LkSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(icone, contentDescription = null, tint = cor, modifier = Modifier.size(18.dp))
        Spacer(Modifier.width(LkSpacing.sm))
        Text(texto, fontSize = 12.sp, color = cor, lineHeight = 17.sp, modifier = Modifier.weight(1f))
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
                .background(c.bgCard)
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
        TextButton(onClick = onClick) { Text("Reiniciar", color = LkColors.warning) }
    }
}

@Composable
private fun ReiniciarEquipamentoDialog(
    onConfirmar: () -> Unit,
    onCancelar: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onCancelar,
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
                Text("Reiniciar", color = LkColors.warning)
            }
        },
        dismissButton = {
            TextButton(onClick = onCancelar) { Text("Cancelar") }
        },
    )
}

@Composable
private fun EquipamentoAcessoIndisponivelContent(
    acesso: AcessoEquipamento,
    localDevice: LocalNetworkDeviceSnapshot?,
    gpon: GponStatus?,
    dentroDaJanelaPosReboot: Boolean,
    onRetentar: () -> Unit,
    onAbrirAjustes: () -> Unit,
    c: LkTokens,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = LkSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(LkSpacing.md),
    ) {
        Spacer(Modifier.height(LkSpacing.xs))

        // Identification/DeviceSelector só aparecem quando já existe algum
        // dado de equipamento (ex.: SOMENTE_IDENTIFICACAO com fingerprint
        // passivo) — CREDENCIAIS_NECESSARIAS/SESSAO_EXPIRADA tipicamente não
        // têm localDevice ainda, e a tela nunca inventa esse bloco vazio.
        localDevice?.let { device ->
            Identification(localDevice = device, c = c)
            DeviceSelector(
                opcoes = listOf(EquipamentoOpcao(id = "atual", label = tituloEquipamento(device))),
                selecionadoId = "atual",
                onSelecionar = {},
                c = c,
            )
        }

        StatusCard(
            icone = statusCardIcone(acesso),
            titulo = statusCardTitulo(acesso, dentroDaJanelaPosReboot),
            descricao = statusCardDescricao(acesso, dentroDaJanelaPosReboot),
            cor = acessoStatusColor(acesso),
            stats = statusCardStats(acesso, localDevice, gpon),
        )

        Spacer(Modifier.height(LkSpacing.sm))
        Button(
            onClick = onRetentar,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text("Tentar novamente", fontSize = 14.sp, fontWeight = FontWeight.W600)
        }
        OutlinedButton(
            onClick = onAbrirAjustes,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            shape = RoundedCornerShape(LkRadius.button),
        ) {
            Text("Revisar configurações", fontSize = 14.sp)
        }

        Spacer(Modifier.height(LkSpacing.lg))
    }
}

private fun acessoLabel(acesso: AcessoEquipamento): String =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA -> "Leitura completa"
        AcessoEquipamento.LEITURA_PARCIAL -> "Leitura parcial"
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> "Somente identificação"
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL -> "Gerenciamento disponível"
        AcessoEquipamento.SESSAO_EXPIRADA -> "Sessão expirada"
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS -> "Credenciais necessárias"
    }
