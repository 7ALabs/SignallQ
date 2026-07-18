package io.signallq.app.ui.screen

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.signallq.app.core.network.contracts.gateway.AcessoEquipamento
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.feature.diagnostico.topology.model.NatStatus
import io.signallq.app.feature.fibra.EstadoFibra
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.component.LocalDeviceSectionUiState
import io.signallq.app.ui.component.mapLocalDeviceSectionUiState
import kotlinx.coroutines.delay

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
 *
 * ## Distribuição dos cards (bug #6, redesign 2026-07-18, spec Lia)
 * A tela `EquipamentoConectadoContent` orquestra a narrativa "quem é o
 * equipamento → está bem ou mal → como está conectado → o que sabe fazer →
 * quem usa → detalhe técnico → o que posso fazer". Cada card visual mora
 * num arquivo próprio (`Equipamento*Card.kt`, mesmo pacote) — esta tela fica
 * só com a composição/ordem e os estados carregando/indisponível, conforme
 * `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.6 (dívida
 * crítica de tamanho de arquivo).
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
    // #1090 — mapAcessoEquipamento() nao conhece idle/conectando (ver KDoc da funcao: a tela
    // decide "Carregando" antes de usar o valor), mas o retorno dela pra esses dois estados
    // e SESSAO_EXPIRADA por eliminacao (nem erro, nem concluido). Sem esta flag, o subtitulo
    // do TopAppBar usava acessoLabel(acesso) sem checar o estado real e mostrava "Sessão
    // expirada" prematuramente durante a primeira tentativa de conexao (#1090).
    val estaCarregando = snapshotFibra.estado == EstadoFibra.idle || snapshotFibra.estado == EstadoFibra.conectando
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
            // GH#1079: migrado de Column/Row cru para TopAppBar real do M3 -- o layout
            // manual nao aplicava inset de status bar/notch (`.statusBarsPadding()`),
            // diferente das outras 14 telas do app que ja usam TopAppBar/
            // CenterAlignedTopAppBar reais.
            Column(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .background(c.bgPrimary),
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Equipamento de internet",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.W600,
                                color = c.textPrimary,
                            )
                            Text(
                                if (estaCarregando) "Conectando…" else acessoLabel(acesso),
                                fontSize = 12.sp,
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = c.bgPrimary),
                )
                HorizontalDivider(color = c.outlineVariant, thickness = 1.dp)
            }
        },
    ) { padding ->
        when {
            estaCarregando ->
                EquipamentoCarregando(
                    modifier = Modifier.padding(padding),
                    c = c,
                    onRetentar = onRetentar,
                    onAbrirAjustes = onAbrirAjustes,
                )

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

/** Tempo de espera antes de oferecer uma saida explicita da tela de carregamento (#1090)
 *  — sem isso, uma tentativa de conexao que nunca resolve (nem sucesso, nem erro) travava
 *  a tela indefinidamente em 5 skeletons vazios, sem nenhum botao de retomada. */
private const val EQUIPAMENTO_CARREGANDO_TIMEOUT_MS = 12_000L

@Composable
private fun EquipamentoCarregando(
    modifier: Modifier = Modifier,
    c: LkTokens,
    onRetentar: () -> Unit = {},
    onAbrirAjustes: () -> Unit = {},
) {
    val pulsar =
        rememberInfiniteTransition(label = "equipamento_skeleton").animateFloat(
            initialValue = 0.45f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(animation = tween(1400), repeatMode = RepeatMode.Reverse),
            label = "equipamento_skeleton_alpha",
        )
    var demorandoDemais by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(EQUIPAMENTO_CARREGANDO_TIMEOUT_MS)
        demorandoDemais = true
    }
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
        if (demorandoDemais) {
            Spacer(Modifier.height(LkSpacing.sm))
            Text(
                "Isso está demorando mais que o esperado.",
                fontSize = 13.sp,
                color = c.textSecondary,
            )
            Button(
                onClick = onRetentar,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = c.primary),
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
        }
    }
}

/**
 * Orquestra a nova distribuição narrativa dos cards (bug #6, spec Lia,
 * 2026-07-18): 1 identidade, 2 seletor, 3 status geral (absorve saúde
 * óptica), 4 disponibilidade (Fibra | Wi-Fi, 2-col), 5 uso (Clientes |
 * Acesso, 2-col), 6 alerta acionável, 7 aviso de leitura parcial,
 * 8 topologia, 9 Wi-Fi por banda (2-col quando ambas existem), 10-11 módulos
 * técnicos full (Fibra óptica/WAN/LAN), 12 dispositivos conectados,
 * 13 informações técnicas, 14 ações disponíveis.
 */
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

        // 1. Identidade do equipamento
        IdentificacaoEquipamentoCard(
            vendor = painelSelecionado.vendor,
            modelo = painelSelecionado.modelo,
            deviceType = painelSelecionado.deviceTypeLabel,
            atualizadoEm = painelSelecionado.atualizacaoLabel,
            c = c,
        )

        // 2. Seletor de equipamento (só quando há mais de um)
        if (paineis.size > 1) {
            DeviceSelectorCard(
                paineis = paineis,
                selecionadoId = painelSelecionadoId,
                onSelecionar = { painelSelecionadoId = it },
                c = c,
            )
        }

        // 3. Status geral — absorve a saúde óptica como linha, sem grid interno
        StatusEquipamentoCard(
            titulo = painelSelecionado.statusTitulo,
            descricao = painelSelecionado.statusDescricao,
            cor = painelSelecionado.statusColor,
            gponSaude = painelSelecionado.gponSaude,
            c = c,
        )

        // 4. Disponibilidade — Fibra | Wi-Fi (2-col)
        DisponibilidadeCardsRow(
            suportaFibra = painelSelecionado.suportaFibra,
            suportaWifi = painelSelecionado.suportaWifi,
            c = c,
        )

        // 5. Uso — Clientes | Acesso (2-col)
        UsoCardsRow(
            totalClientes = painelSelecionado.totalClientes,
            acessoLabel = painelSelecionado.acessoLabel,
            c = c,
        )

        // 6. Alerta acionável (condicional) — sobe pra logo após o cluster de status
        painelSelecionado.alerta?.let { alerta ->
            AlertaCard(alerta = alerta, onAcionar = onExecutarDiagnostico)
        }

        // 7. Aviso de leitura parcial (condicional)
        if (painelSelecionado.mostrarAvisoLeituraParcial) {
            AvisoAcessoCard(
                icone = Icons.Outlined.ErrorOutline,
                cor = LkColors.warning,
                texto = "Leitura parcial — algumas seções deste equipamento não vieram preenchidas nesta captura.",
            )
        }

        // 8. Topologia — peça central
        TopologiaRedeCard(
            paineis = paineis,
            selecionadoId = painelSelecionado.id,
            warning = painelSelecionado.topologyWarning,
            c = c,
        )

        // 9, 10, 11 — módulos técnicos, todos full-width (Wi-Fi deixou de ser
        // 2-col por banda em 2026-07-18, revisão Lia — ver KDoc em
        // EquipamentoModuloTecnicoCard.kt).
        painelSelecionado.secoesTecnicas
            .filterNot { it.titulo == "Dispositivos conectados" }
            .forEach { secao ->
                ModuloTecnicoCard(secao = secao, c = c)
            }

        // 12. Dispositivos conectados (resumo)
        painelSelecionado.devicesSummary?.let { summary ->
            DevicesSummaryCard(summary = summary, c = c)
        }

        // 13. Informações técnicas (colapsável, já inclui "Acesso ao equipamento")
        DeviceInfoSectionCard(
            linhas = painelSelecionado.infoRows,
            acesso = painelSelecionado.acessoLabel,
            acessoColor = painelSelecionado.statusColor,
            c = c,
        )

        // 14. Ações disponíveis
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
