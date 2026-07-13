package io.signallq.app.ui.screen

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.signallq.app.core.network.contracts.fibra.GponSaudeStatus
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
import io.signallq.app.ui.component.LocalDeviceSection
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
                    snapshotFibra = snapshotFibra,
                    acesso = acesso,
                    doubleNatSuspeito = doubleNatSuspeito,
                    onSolicitarReiniciar = { mostrarDialogoReiniciar = true },
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
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator(modifier = Modifier.size(40.dp), color = LkColors.accent)
            Spacer(Modifier.height(LkSpacing.md))
            Text("Conectando ao equipamento...", fontSize = 14.sp, color = c.textSecondary)
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

        if (acesso == AcessoEquipamento.LEITURA_PARCIAL) {
            AvisoAcessoCard(
                icone = Icons.Outlined.ErrorOutline,
                cor = LkColors.warning,
                texto = "Leitura parcial — algumas seções deste equipamento não vieram preenchidas nesta captura.",
            )
        }

        snapshotFibra.gpon?.let { gpon ->
            SaudeOpticaBadge(
                status =
                    ClassificadorSaudeGpon.classificar(
                        isUp = gpon.isUp,
                        rxPowerDbm = gpon.rxPowerDbm,
                        txPowerDbm = gpon.txPowerDbm,
                        temperatureCelsius = gpon.temperatureCelsius,
                    ),
            )
        }

        if (doubleNatSuspeito) {
            AvisoAcessoCard(
                icone = Icons.Outlined.WarningAmber,
                cor = LkColors.warning,
                texto =
                    "Possível NAT duplo detectado: seu equipamento e um roteador adicional podem estar " +
                        "fazendo NAT ao mesmo tempo. Isso pode causar problemas em jogos online e chamadas de vídeo.",
            )
        }

        LocalDeviceSection(state = estadoSecao, refazerDisponivel = true)

        if (acesso == AcessoEquipamento.GERENCIAMENTO_DISPONIVEL) {
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
                color = c.textTertiary,
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
                colors = ButtonDefaults.buttonColors(containerColor = LkColors.accent),
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
