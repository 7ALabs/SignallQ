package io.signallq.app.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CellTower
import androidx.compose.material.icons.outlined.DevicesOther
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.Print
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Smartphone
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
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
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.core.network.contracts.topologia.PapelTopologia
import io.signallq.app.feature.devices.DispositivoRede
import io.signallq.app.feature.devices.NamingPrioridade
import io.signallq.app.feature.devices.ResultadoCorrelacaoTopologia
import io.signallq.app.feature.devices.TipoDispositivo
import io.signallq.app.feature.devices.chaveApelido
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LkTokens
import io.signallq.app.ui.component.LkInfoCallout
import io.signallq.app.ui.component.LkPillBadge
import io.signallq.app.ui.component.LkSectionOverline
import io.signallq.app.ui.component.LkStatusDot
import io.signallq.app.ui.component.SheetDragHandle

// ---------------------------------------------------------------------------
// Sheets de detalhe de dispositivo — extraído de DispositivosScreen.kt na
// issue #1663 (épico #1647, Task 2.0.15). Sem mudança de comportamento.
//
// IP/MAC/SSID exibidos aqui são somente leitura de tela (nunca enviados a
// analytics — ver DevicesViewModel/MainViewModel, que só emitem contagens).
// ---------------------------------------------------------------------------

// ---------------------------------------------------------------------------
// DeviceDetailSheet — somente leitura (sem personalização por ora)
// ---------------------------------------------------------------------------

@Composable
internal fun DeviceDetailSheet(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelidoAtual: String,
    onSalvarApelido: (String) -> Unit,
    /** #983 (Fase 4) — correlacao best-effort com a topologia Wi-Fi/gateway pra este
     *  dispositivo especifico. Null quando nao ha correlacao (comportamento pre-Fase 4). */
    correlacao: ResultadoCorrelacaoTopologia? = null,
) {
    val iconBg = iconBgColor(dispositivo.tipoDispositivo, c)
    val iconFg = iconFgColor(dispositivo.tipoDispositivo, c)
    val icon = iconForTipo(dispositivo.tipoDispositivo)
    val mac = dispositivo.mac
    // #853 — a secao APELIDO usa a chave com fallback ip+nome (chaveApelido), nao so o MAC
    // cru, senao ela some sempre que o Android nao consegue resolver o MAC via ARP.
    val chaveApelido = dispositivo.chaveApelido()
    val fabricante = dispositivo.fabricante
    var apelidoInput by remember { mutableStateOf(apelidoAtual) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.primary,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

    LazyColumn(
        contentPadding = PaddingValues(bottom = 32.dp),
    ) {
        item {
            SheetDragHandle()
        }

        // Cabeçalho
        item {
            Row(
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(CircleShape)
                            .background(c.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = icon,
                        contentDescription = null,
                        tint = c.primary,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = dispositivo.nomeExibicao,
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = listOfNotNull(fabricante, "identificada nesta rede").joinToString(" · "),
                        style = MaterialTheme.typography.bodyMedium,
                        color = c.textSecondary,
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LkStatusDot(color = c.success)
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(
                            text = "Online",
                            style = MaterialTheme.typography.labelMedium,
                            color = c.success,
                        )
                    }
                }
            }
            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border)
        }

        // Seção APELIDO (#853 — chave com fallback ip+nome quando não há MAC resolvível)
        if (chaveApelido != null) {
            item {
                SheetSectionHeader(title = "APELIDO")
            }
            item {
                Column(
                    modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm),
                ) {
                    OutlinedTextField(
                        value = apelidoInput,
                        onValueChange = { apelidoInput = it },
                        label = { Text("Apelido (opcional)") },
                        placeholder = { Text(dispositivo.nomeExibicao, color = c.textTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(LkRadius.input),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Button(
                        onClick = { onSalvarApelido(apelidoInput.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                    ) {
                        Text("Salvar apelido")
                    }
                }
            }
            item { HorizontalDivider(color = c.border) }
        }

        // Seção REDE
        item {
            SheetSectionHeader(title = "REDE")
        }
        item {
            LkListRow(c = c, title = "Endereço IP", trailing = {
                Text(dispositivo.ip ?: "—", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
            })
        }
        if (mac != null) {
            item {
                LkListRow(c = c, title = "Endereço físico", trailing = {
                    Text(mascaraMac(mac), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                })
            }
        }
        if (fabricante != null) {
            item {
                LkListRow(c = c, title = "Fabricante", trailing = {
                    Text(fabricante, style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
                })
            }
        }
        item {
            LkListRow(c = c, title = "Tipo", trailing = {
                Text(
                    tipoLabel(dispositivo.tipoDispositivo),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.textSecondary,
                )
            })
        }
        // #983 (Fase 4) — so aparece quando ha correlacao confirmada (ClientSnapshot exato ou
        // MAC==BSSID exato); correlacao fraca (so OUI) nunca chega aqui como papel/conexao,
        // so como evidencia auxiliar (nao exibida, ver correlacionarDispositivoComTopologia).
        correlacao?.tipoConexaoFisicaConfirmada?.let { tipoConexao ->
            item {
                LkListRow(c = c, title = "Conexão física", trailing = {
                    Text(
                        tipoConexaoFisicaLabel(tipoConexao),
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textSecondary,
                    )
                })
            }
        }
        correlacao?.papelTopologiaHerdado?.let { papel ->
            item {
                LkListRow(c = c, title = "Papel na rede", trailing = {
                    Text(
                        papelTopologiaLabel(papel),
                        style = MaterialTheme.typography.titleSmall,
                        color = c.textSecondary,
                    )
                })
            }
        }
        item {
            LkListRow(c = c, title = "Descoberto via", showDivider = false, trailing = {
                Text(
                    fonteNomeLabel(dispositivo.fonteNome),
                    style = MaterialTheme.typography.titleSmall,
                    color = c.primary,
                )
            })
        }
    }
}

// ---------------------------------------------------------------------------
// MeshApSheet — ponto de acesso / nó mesh
// ---------------------------------------------------------------------------

// GH#1025 — exposta (sem `private`) pra ser reaproveitada por SinalScreen.kt/SinalWifiSection.kt
// (mesmo pacote ui.screen), que abre esta sheet quando um nó da árvore de topologia é
// correlacionado a um DispositivoRede do scan LAN.
@Composable
fun MeshApSheet(
    dispositivo: DispositivoRede,
    c: LkTokens,
    apelidoAtual: String,
    onSalvarApelido: (String) -> Unit,
) {
    val mac = dispositivo.mac
    // #853 — mesma logica de fallback do DeviceDetailSheet: chave com fallback ip+nome.
    val chaveApelido = dispositivo.chaveApelido()
    val fabricante = dispositivo.fabricante
    var apelidoInput by remember { mutableStateOf(apelidoAtual) }

    val fieldColors =
        OutlinedTextFieldDefaults.colors(
            focusedBorderColor = c.primary,
            unfocusedBorderColor = c.border,
            focusedLabelColor = c.primary,
            unfocusedLabelColor = c.textSecondary,
            cursorColor = c.primary,
            focusedTextColor = c.textPrimary,
            unfocusedTextColor = c.textPrimary,
        )

    LazyColumn(contentPadding = PaddingValues(bottom = 32.dp)) {
        item {
            SheetDragHandle()
        }

        // Cabeçalho
        item {
            Row(
                modifier = Modifier.padding(horizontal = LkSpacing.lg),
                verticalAlignment = Alignment.Top,
            ) {
                Box(
                    modifier =
                        Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(LkRadius.input))
                            .background(c.success.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CellTower,
                        contentDescription = null,
                        tint = c.success,
                        modifier = Modifier.size(24.dp),
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text = dispositivo.nomeExibicao,
                        style = MaterialTheme.typography.headlineSmall,
                        color = c.textPrimary,
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        LkStatusDot(color = c.success)
                        Spacer(Modifier.width(LkSpacing.xs))
                        Text(text = "Online", style = MaterialTheme.typography.labelSmall, color = c.success)
                        Spacer(Modifier.width(LkSpacing.sm))
                        BadgePill(label = "AP Mesh", bg = c.success.copy(0.12f), fg = c.success)
                    }
                }
            }
            Spacer(Modifier.height(LkSpacing.lg))
            HorizontalDivider(color = c.border)
        }

        // Aviso sobre dados disponíveis
        item {
            Spacer(Modifier.height(LkSpacing.md))
            Row(modifier = Modifier.padding(horizontal = LkSpacing.lg)) {
                LkInfoCallout(
                    icon = Icons.Outlined.Info,
                    text =
                        "Sinal, banda e clientes conectados não estão disponíveis via varredura passiva. " +
                            "Para métricas detalhadas, acesse o painel do seu roteador mesh.",
                    iconTint = c.textSecondary,
                )
            }
            Spacer(Modifier.height(LkSpacing.sm))
        }

        // Seção APELIDO (#853 — chave com fallback ip+nome quando não há MAC resolvível)
        if (chaveApelido != null) {
            item { SheetSectionHeader(title = "APELIDO") }
            item {
                Column(modifier = Modifier.padding(horizontal = LkSpacing.lg, vertical = LkSpacing.sm)) {
                    OutlinedTextField(
                        value = apelidoInput,
                        onValueChange = { apelidoInput = it },
                        label = { Text("Apelido (opcional)") },
                        placeholder = { Text(dispositivo.nomeExibicao, color = c.textTertiary) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        colors = fieldColors,
                        shape = RoundedCornerShape(LkRadius.input),
                    )
                    Spacer(Modifier.height(LkSpacing.sm))
                    Button(
                        onClick = { onSalvarApelido(apelidoInput.trim()) },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = c.primary),
                    ) { Text("Salvar apelido") }
                }
            }
            item { HorizontalDivider(color = c.border) }
        }

        // Seção REDE
        item { SheetSectionHeader(title = "REDE") }
        item {
            LkListRow(c = c, title = "Endereço IP", trailing = {
                Text(dispositivo.ip ?: "—", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
            })
        }
        if (mac != null) {
            item {
                LkListRow(c = c, title = "MAC", trailing = {
                    Text(mascaraMac(mac), style = MaterialTheme.typography.labelSmall, color = c.textSecondary)
                })
            }
        }
        if (fabricante != null) {
            item {
                LkListRow(c = c, title = "Fabricante", trailing = {
                    Text(fabricante, style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
                })
            }
        }
        item {
            LkListRow(c = c, title = "Tipo", showDivider = false, trailing = {
                Text("Ponto de Acesso / Mesh", style = MaterialTheme.typography.titleSmall, color = c.textSecondary)
            })
        }
    }
}

// ---------------------------------------------------------------------------
// Sheet section header
// ---------------------------------------------------------------------------

@Composable
private fun SheetSectionHeader(title: String) {
    LkSectionOverline(
        text = title,
        modifier = Modifier.padding(start = LkSpacing.lg, top = LkSpacing.lg, bottom = 4.dp),
    )
}

// ---------------------------------------------------------------------------
// LkListRow — linha reutilizável com divisor (compartilhada entre lista e sheets)
// ---------------------------------------------------------------------------

@Composable
internal fun LkListRow(
    c: LkTokens,
    title: String = "",
    subtitle: String? = null,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    showDivider: Boolean = true,
    onTap: (() -> Unit)? = null,
) {
    Column {
        Row(
            modifier =
                Modifier
                    .fillMaxWidth()
                    .then(
                        if (onTap != null) {
                            Modifier
                                .minimumInteractiveComponentSize()
                                .semantics { role = Role.Button }
                                .clickable(onClick = onTap)
                        } else {
                            Modifier
                        },
                    ).padding(horizontal = LkSpacing.lg, vertical = LkSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (leading != null) {
                leading()
                Spacer(Modifier.width(LkSpacing.md))
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    color = c.textPrimary,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                if (subtitle != null) {
                    Spacer(Modifier.height(LkSpacing.xs))
                    Text(
                        text = subtitle,
                        color = c.textSecondary,
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
            }
            if (trailing != null) trailing()
        }
        if (showDivider) {
            HorizontalDivider(color = c.border.copy(alpha = 0.5f), thickness = 0.5.dp)
        }
    }
}

// ---------------------------------------------------------------------------
// Badge pill
// ---------------------------------------------------------------------------

@Composable
internal fun BadgePill(
    label: String,
    bg: Color,
    fg: Color,
) {
    LkPillBadge(
        text = label,
        containerColor = bg,
        contentColor = fg,
    )
}

// ---------------------------------------------------------------------------
// Helpers de mapeamento (compartilhados entre lista e sheets)
// ---------------------------------------------------------------------------

internal fun iconForTipo(tipo: TipoDispositivo): ImageVector =
    when (tipo) {
        TipoDispositivo.roteador -> Icons.Outlined.Router
        TipoDispositivo.pontoAcesso -> Icons.Outlined.CellTower
        TipoDispositivo.computador -> Icons.Outlined.Laptop
        TipoDispositivo.smartphone -> Icons.Outlined.Smartphone
        TipoDispositivo.smarthome -> Icons.Outlined.Lightbulb
        TipoDispositivo.impressora -> Icons.Outlined.Print
        TipoDispositivo.console -> Icons.Outlined.SportsEsports
        // Regra de produto (issue #1663): dispositivo não confirmado usa ícone genérico,
        // nunca um ícone que sugira marca/tipo não comprovado.
        TipoDispositivo.desconhecido -> Icons.Outlined.DevicesOther
    }

@Composable
internal fun iconBgColor(
    tipo: TipoDispositivo,
    c: LkTokens,
): Color =
    when (tipo) {
        TipoDispositivo.smartphone -> c.primary.copy(alpha = 0.12f)
        TipoDispositivo.computador -> c.success.copy(alpha = 0.12f)
        TipoDispositivo.roteador -> c.primary.copy(alpha = 0.12f)
        TipoDispositivo.pontoAcesso -> c.success.copy(alpha = 0.12f)
        TipoDispositivo.smarthome -> c.warning.copy(alpha = 0.12f)
        else -> c.bgSecondary
    }

@Composable
internal fun iconFgColor(
    tipo: TipoDispositivo,
    c: LkTokens,
): Color =
    when (tipo) {
        TipoDispositivo.smartphone -> c.primary
        TipoDispositivo.computador -> c.success
        TipoDispositivo.roteador -> c.primary
        TipoDispositivo.pontoAcesso -> c.success
        TipoDispositivo.smarthome -> c.warning
        else -> c.textSecondary
    }

internal fun tipoLabel(tipo: TipoDispositivo): String =
    when (tipo) {
        TipoDispositivo.roteador -> "Roteador / Gateway"
        TipoDispositivo.pontoAcesso -> "Ponto de Acesso / Mesh"
        TipoDispositivo.computador -> "Computador"
        TipoDispositivo.smartphone -> "Celular / Tablet"
        TipoDispositivo.smarthome -> "Dispositivo inteligente"
        TipoDispositivo.impressora -> "Impressora"
        TipoDispositivo.console -> "Console de jogos"
        TipoDispositivo.desconhecido -> "Desconhecido"
    }

/** #983 (Fase 4) — traduz [TipoConexaoFisica] (confirmado por leitura direta do gateway,
 *  [ResultadoCorrelacaoTopologia.tipoConexaoFisicaConfirmada]) pra rotulo exibido no detalhe
 *  do dispositivo. `internal` pra ser testavel isoladamente (padrao ja usado em
 *  `PapelParaTipoTopologiaLegadoTest`/`PapelParaConnectionNodeTypeTest`). */
internal fun tipoConexaoFisicaLabel(tipo: TipoConexaoFisica): String =
    when (tipo) {
        TipoConexaoFisica.ETHERNET -> "Cabo (Ethernet)"
        TipoConexaoFisica.WIFI -> "Wi-Fi"
        TipoConexaoFisica.DESCONHECIDO -> "Desconhecida"
    }

/** #983 (Fase 4) — traduz [PapelTopologia] herdado por correlacao forte (MAC/ClientSnapshot
 *  exato, [ResultadoCorrelacaoTopologia.papelTopologiaHerdado]) pra rotulo exibido no detalhe
 *  do dispositivo. Nunca chamado com papel vindo de correlacao fraca (so OUI) — ver
 *  [correlacionarDispositivoComTopologia]. */
internal fun papelTopologiaLabel(papel: PapelTopologia): String =
    when (papel) {
        PapelTopologia.ROTEADOR -> "Roteador"
        PapelTopologia.NO_MESH -> "Nó mesh"
        PapelTopologia.REPETIDOR -> "Repetidor"
        PapelTopologia.PONTO_DE_ACESSO -> "Ponto de acesso"
        PapelTopologia.SISTEMA_MESH_PROVAVEL -> "Sistema mesh (provável)"
        PapelTopologia.DESCONHECIDO -> "Desconhecido"
    }

// #854: nunca expor o valor cru de fonteNome na UI (viola "métrica crua sempre
// acompanhada de veredito humano" do design system) — todo valor produzido pelo
// scanner (ver prioridade de fonte em ScannerDispositivosAndroid) precisa de
// tradução aqui. O fallback (`fonte tratada`) so existe pra nao quebrar em caso
// de fonte nova ainda nao mapeada, nunca deve aparecer em uso normal.
internal fun fonteNomeLabel(fonte: String) =
    when (fonte) {
        NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE -> "Confirmado pelo roteador"
        // GH#1217 item 2 — associação só por IP (sem MAC batendo) é provável, não
        // confirmada: o IP pode ter sido reatribuído por DHCP entre os dois snapshots.
        NamingPrioridade.FONTE_NOME_ROUTER_ACTIVE_IP -> "Provável (roteador · por IP)"
        "gateway" -> "Roteador (gateway)"
        "mdns" -> "mDNS · Bonjour"
        "mdnsJmDns" -> "mDNS · Bonjour"
        "subnetMdns" -> "mDNS · Bonjour"
        "ssdp" -> "UPnP · SSDP"
        "ssdpXml" -> "UPnP · SSDP"
        "nbns" -> "NetBIOS"
        "arp" -> "ARP (varredura)"
        "subnet" -> "Varredura de rede"
        "tcpProbe" -> "TCP probe"
        else -> "Varredura de rede"
    }

/** Mascara os octetos 3-4 do MAC: ex. "c4:8e:de:ad:1a:2b" → "c4:8e:••:••:1a:2b" */
internal fun mascaraMac(mac: String): String {
    val partes = mac.trim().split(":")
    return if (partes.size == 6) {
        "${partes[0]}:${partes[1]}:••:••:${partes[4]}:${partes[5]}"
    } else {
        mac
    }
}
