package io.signallq.app.ui.component

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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.HelpOutline
import androidx.compose.material.icons.outlined.Cable
import androidx.compose.material.icons.outlined.DeviceUnknown
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Dns
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Lan
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.Science
import androidx.compose.material.icons.outlined.SettingsInputAntenna
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.core.network.contracts.gateway.EquipmentClassification
import io.signallq.app.core.network.contracts.localdevice.DataFreshness
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.DeviceWarningType
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.diagnostico.FibraDiagnosticInput
import io.signallq.app.feature.diagnostico.FibraSignalQualityEngine
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens
import io.signallq.app.ui.SignallQTheme
import io.signallq.app.core.network.contracts.gateway.DeviceType as GatewayDeviceType
import io.signallq.app.core.network.contracts.gateway.SupportLevel as GatewaySupportLevel

// ─── Estado de UI (GH#544, epic #547) ──────────────────────────────────────
//
// Deriva os 7 estados obrigatorios da secao "Equipamento local" a partir do
// contrato normalizado (GH#546) e da classificacao (GH#545) — sempre por
// capabilities declaradas pelo driver, nunca checando vendor/modelo direto
// (ex.: "TP-Link" nao aparece em nenhum `if` deste arquivo).
//
// A logica de interpretacao mora aqui, fora do Composable, para poder ser
// testada com JUnit puro (ver LocalDeviceSectionUiStateTest) sem depender de
// Robolectric/Compose.

sealed interface LocalDeviceSectionUiState {
    /** Nenhum equipamento local foi detectado na rede atual. */
    data object NenhumEncontrado : LocalDeviceSectionUiState

    /** Equipamento identificado por fingerprint passivo, sem tentativa de login ainda. */
    data class EncontradoNaoConectado(
        val deviceType: DeviceType,
        val supportLevel: SupportLevel,
    ) : LocalDeviceSectionUiState

    /** Login foi tentado e falhou — sem dados de leitura para exibir. */
    data class LoginFalhou(
        val deviceType: DeviceType,
        val mensagem: String,
    ) : LocalDeviceSectionUiState

    /** Nenhum driver/parser reconhece este equipamento. */
    data class SemSuporte(
        val deviceType: DeviceType,
    ) : LocalDeviceSectionUiState

    /** Conectado com dados — completos, parciais e/ou de driver experimental. */
    data class Conectado(
        val tituloEquipamento: String,
        val deviceType: DeviceType,
        val supportLevel: SupportLevel,
        val experimental: Boolean,
        val completo: Boolean,
        val resumoTitulo: String,
        val resumoDescricao: String,
        val resumoStatus: DiagnosticStatus,
        val secoes: List<EquipamentoSecaoTecnica>,
        val freshness: DataFreshness,
    ) : LocalDeviceSectionUiState
}

data class EquipamentoSecaoTecnica(
    val titulo: String,
    val icone: ImageVector,
    val itens: List<EquipamentoItemTecnico>,
)

data class EquipamentoItemTecnico(
    val label: String,
    val valor: String,
)

/**
 * Mapeia o snapshot bruto (uso interno/UI — nunca IA/analytics, ver
 * [io.signallq.app.core.network.contracts.localdevice.LocalDeviceSafeFilter])
 * para o estado de UI da secao "Equipamento local".
 *
 * [descoberta] e opcional: quando so ha fingerprint passivo (fase 1, GH#545)
 * sem tentativa de leitura completa ainda, ela produz o estado
 * "encontrado nao conectado". Quando ambos sao null, o estado e "nenhum
 * encontrado" — nunca um card vazio fingindo ter dado.
 */
fun mapLocalDeviceSectionUiState(
    snapshot: LocalNetworkDeviceSnapshot?,
    descoberta: EquipmentClassification? = null,
): LocalDeviceSectionUiState {
    if (snapshot == null) {
        return descoberta?.let {
            LocalDeviceSectionUiState.EncontradoNaoConectado(
                deviceType = it.deviceType.paraContratoLocal(),
                supportLevel = it.supportLevel.paraContratoLocal(),
            )
        } ?: LocalDeviceSectionUiState.NenhumEncontrado
    }

    val loginFalhou = snapshot.warnings.firstOrNull { it.type == DeviceWarningType.LOGIN_FALHOU }
    if (loginFalhou != null) {
        return LocalDeviceSectionUiState.LoginFalhou(snapshot.deviceType, loginFalhou.mensagem)
    }

    val semDriverConhecido =
        snapshot.deviceType == DeviceType.UNKNOWN_SUPPORTED ||
            snapshot.deviceType == DeviceType.UNKNOWN_UNSUPPORTED ||
            snapshot.supportLevel == SupportLevel.UNKNOWN
    if (semDriverConhecido) {
        return LocalDeviceSectionUiState.SemSuporte(snapshot.deviceType)
    }

    val (resumoTitulo, resumoDescricao, resumoStatus) = resumoInterpretado(snapshot)

    return LocalDeviceSectionUiState.Conectado(
        tituloEquipamento = tituloEquipamento(snapshot),
        deviceType = snapshot.deviceType,
        supportLevel = snapshot.supportLevel,
        experimental =
            snapshot.supportLevel == SupportLevel.PARSER_IMPORTED ||
                snapshot.supportLevel == SupportLevel.INFERRED_FAMILY,
        completo = !dadosParciais(snapshot),
        resumoTitulo = resumoTitulo,
        resumoDescricao = resumoDescricao,
        resumoStatus = resumoStatus,
        secoes = secoesTecnicas(snapshot),
        freshness = snapshot.freshness,
    )
}

/** [io.signallq.app.core.network.contracts.gateway.DeviceType] (fingerprint, GH#545) e
 *  [DeviceType] (contrato normalizado, GH#546) sao enums espelhados intencionalmente
 *  em pacotes diferentes — nao ha upcast direto, so conversao explicita por nome. */
private fun GatewayDeviceType.paraContratoLocal(): DeviceType =
    when (this) {
        GatewayDeviceType.ONT_GPON -> DeviceType.ONT_GPON
        GatewayDeviceType.ROUTER -> DeviceType.ROUTER
        GatewayDeviceType.MESH_OR_EXTENDER -> DeviceType.MESH_OR_EXTENDER
        GatewayDeviceType.UNKNOWN_SUPPORTED -> DeviceType.UNKNOWN_SUPPORTED
        GatewayDeviceType.UNKNOWN_UNSUPPORTED -> DeviceType.UNKNOWN_UNSUPPORTED
    }

private fun GatewaySupportLevel.paraContratoLocal(): SupportLevel =
    when (this) {
        GatewaySupportLevel.LAB_VALIDATED -> SupportLevel.LAB_VALIDATED
        GatewaySupportLevel.PARSER_IMPORTED -> SupportLevel.PARSER_IMPORTED
        GatewaySupportLevel.INFERRED_FAMILY -> SupportLevel.INFERRED_FAMILY
        GatewaySupportLevel.UNKNOWN -> SupportLevel.UNKNOWN
    }

private fun tituloEquipamento(snapshot: LocalNetworkDeviceSnapshot): String {
    val vendor = snapshot.vendor?.trim().orEmpty()
    val modelo = snapshot.modelo?.trim().orEmpty()
    val vendorModelo = listOf(vendor, modelo).filter { it.isNotBlank() }.joinToString(" ")
    return vendorModelo.ifBlank {
        when (snapshot.deviceType) {
            DeviceType.ONT_GPON -> "ONT de fibra"
            DeviceType.ROUTER -> "Roteador"
            DeviceType.MESH_OR_EXTENDER -> "Nó mesh"
            DeviceType.UNKNOWN_SUPPORTED, DeviceType.UNKNOWN_UNSUPPORTED -> "Equipamento local"
        }
    }
}

/** Dado ausente numa secao suportada pelo driver conta como leitura parcial —
 *  lista de clientes vazia NAO conta sozinha (pode ser legitimo ninguem conectado). */
private fun dadosParciais(snapshot: LocalNetworkDeviceSnapshot): Boolean {
    if (snapshot.warnings.any { it.type == DeviceWarningType.DADOS_PARCIAIS }) return true
    val cap = snapshot.capabilities
    if (cap.suportaFibra && snapshot.fiber == null) return true
    if (cap.suportaWan && snapshot.wan == null) return true
    if (cap.suportaWifi && snapshot.wifi?.radios.isNullOrEmpty()) return true
    if (cap.suportaLan && snapshot.lan == null) return true
    return false
}

private fun DiagnosticStatus.severidade(): Int =
    when (this) {
        DiagnosticStatus.critical -> 3
        DiagnosticStatus.attention -> 2
        DiagnosticStatus.inconclusive -> 1
        DiagnosticStatus.info -> 1
        DiagnosticStatus.ok -> 0
    }

/** Traduz o snapshot em veredito humano — fibra usa o mesmo motor de limiares
 *  de [FibraSignalQualityEngine] que a tela de fibra Nokia ja usa (nao
 *  duplica regra de negocio). Equipamentos sem fibra (ex.: roteador) usam
 *  WAN/Wi-Fi como base do resumo, nunca fibra/PON/OLT. */
private fun resumoInterpretado(snapshot: LocalNetworkDeviceSnapshot): Triple<String, String, DiagnosticStatus> {
    val cap = snapshot.capabilities

    if (cap.suportaFibra) {
        val fiber =
            snapshot.fiber
                ?: return Triple(
                    "Sem leitura da fibra",
                    "O equipamento suporta dados ópticos, mas esta leitura não trouxe valores.",
                    DiagnosticStatus.inconclusive,
                )
        if (fiber.linkAtivo == false) {
            return Triple("Fibra desconectada", "O link óptico está inativo — sem sinal da operadora.", DiagnosticStatus.critical)
        }
        val avaliacoes =
            FibraSignalQualityEngine.avaliar(
                FibraDiagnosticInput(
                    rxPowerDbm = fiber.rxPowerDbm,
                    txPowerDbm = fiber.txPowerDbm,
                    temperatureCelsius = fiber.temperaturaCelsius,
                    isUp = fiber.linkAtivo ?: true,
                ),
            )
        val pior = avaliacoes.maxByOrNull { it.status.severidade() }
        return if (pior != null) {
            Triple(pior.titulo, pior.mensagemUsuario, pior.status)
        } else {
            Triple("Fibra ativa", "Link óptico ativo, sem métricas adicionais nesta leitura.", DiagnosticStatus.ok)
        }
    }

    if (cap.suportaWan) {
        val wan = snapshot.wan
        val wifiHabilitado = snapshot.wifi?.radios?.any { it.habilitado == true } == true
        return if (wan != null && (wan.ipExterno != null || wan.gateway != null)) {
            Triple(
                "Internet chegando normalmente",
                "O roteador confirma conexão ativa com a operadora." +
                    if (wifiHabilitado) " Wi-Fi ligado e transmitindo." else "",
                DiagnosticStatus.ok,
            )
        } else {
            Triple(
                "Sem confirmação da internet",
                "O roteador não reportou IP da operadora nesta leitura.",
                DiagnosticStatus.attention,
            )
        }
    }

    return Triple(
        "Equipamento respondendo",
        "Conectado ao equipamento, mas sem métricas de internet para avaliar automaticamente.",
        DiagnosticStatus.info,
    )
}

/** Monta as secoes de dado tecnico exibidas ABAIXO do resumo interpretado —
 *  uma por capability declarada, nunca uma secao de fibra para quem nao tem
 *  [io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities.suportaFibra]. */
private fun secoesTecnicas(snapshot: LocalNetworkDeviceSnapshot): List<EquipamentoSecaoTecnica> {
    val cap = snapshot.capabilities
    return buildList {
        if (cap.suportaFibra) {
            val fiber = snapshot.fiber
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Fibra óptica",
                    icone = Icons.Outlined.Cable,
                    itens =
                        listOfNotNull(
                            EquipamentoItemTecnico("Link óptico", fiber?.linkAtivo?.let { if (it) "Ativo" else "Inativo" } ?: "—"),
                            fiber?.rxPowerDbm?.let { EquipamentoItemTecnico("Potência RX", "%.2f dBm".format(it)) },
                            fiber?.txPowerDbm?.let { EquipamentoItemTecnico("Potência TX", "%.2f dBm".format(it)) },
                            fiber?.temperaturaCelsius?.let { EquipamentoItemTecnico("Temperatura", "%.1f °C".format(it)) },
                        ).ifEmpty { listOf(EquipamentoItemTecnico("Fibra óptica", "Sem leitura nesta captura")) },
                ),
            )
        }
        if (cap.suportaWan) {
            val wan = snapshot.wan
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Internet (WAN)",
                    icone = Icons.Outlined.Dns,
                    itens =
                        listOfNotNull(
                            wan?.ipExterno?.let { EquipamentoItemTecnico("IP externo", it) },
                            wan?.tipoConexao?.let { EquipamentoItemTecnico("Tipo de conexão", it) },
                            wan?.dnsPrimario?.let { EquipamentoItemTecnico("DNS primário", it) },
                            wan?.uptimeSegundos?.let { EquipamentoItemTecnico("Uptime", formatarUptime(it)) },
                        ).ifEmpty { listOf(EquipamentoItemTecnico("Internet (WAN)", "Sem leitura nesta captura")) },
                ),
            )
        }
        if (cap.suportaWifi) {
            val radios = snapshot.wifi?.radios.orEmpty()
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Wi-Fi",
                    icone = Icons.Outlined.Wifi,
                    itens =
                        if (radios.isEmpty()) {
                            listOf(EquipamentoItemTecnico("Wi-Fi", "Sem leitura nesta captura"))
                        } else {
                            radios.map { radio ->
                                EquipamentoItemTecnico(
                                    label = radio.ssid ?: radio.banda,
                                    valor =
                                        buildString {
                                            append(radio.banda)
                                            radio.canal?.let { append(" · canal $it") }
                                            if (radio.habilitado == false) append(" · desligado")
                                        },
                                )
                            }
                        },
                ),
            )
        }
        if (cap.suportaClientes) {
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Dispositivos conectados",
                    icone = Icons.Outlined.Devices,
                    itens = listOf(EquipamentoItemTecnico("Clientes na rede", "${snapshot.clientes.size}")),
                ),
            )
        }
        if (cap.suportaLan) {
            val lan = snapshot.lan
            add(
                EquipamentoSecaoTecnica(
                    titulo = "Rede local (LAN)",
                    icone = Icons.Outlined.Lan,
                    itens =
                        listOfNotNull(
                            lan?.ipRoteador?.let { EquipamentoItemTecnico("IP do roteador", mascaraIpEquipamento(it)) },
                            lan?.dhcpHabilitado?.let { EquipamentoItemTecnico("DHCP", if (it) "Ativo" else "Desligado") },
                        ).ifEmpty { listOf(EquipamentoItemTecnico("Rede local (LAN)", "Sem leitura nesta captura")) },
                ),
            )
        }
    }
}

private fun formatarUptime(segundos: Int): String {
    if (segundos <= 0) return "—"
    val d = segundos / 86400
    val h = (segundos % 86400) / 3600
    val m = (segundos % 3600) / 60
    return when {
        d > 0 -> "${d}d ${h}h ${m}min"
        h > 0 -> "${h}h ${m}min"
        else -> "${m}min"
    }
}

/** Formata o tempo decorrido desde a captura do snapshot em texto humano — a
 *  regra de expiracao ([DataFreshness.expirado]) e do motor, esta funcao so
 *  cobre o texto de "ha quanto tempo" quando a leitura ainda e considerada
 *  valida. */
private fun formatarFrescor(
    capturadoEmEpochMs: Long,
    agora: Long = System.currentTimeMillis(),
): String {
    val minutos = ((agora - capturadoEmEpochMs).coerceAtLeast(0)) / 60_000
    return when {
        minutos < 2 -> "Atualizado agora"
        minutos < 60 -> "Atualizado há $minutos min"
        else -> "Atualizado há ${minutos / 60}h"
    }
}

/** Mascara o ultimo octeto de um IPv4 para nao expor a rede local inteira na tela. */
private fun mascaraIpEquipamento(ip: String): String {
    val partes = ip.trim().split(".")
    return if (partes.size == 4) "${partes[0]}.${partes[1]}.${partes[2]}.*" else ip.trim()
}

// ─── Composable ─────────────────────────────────────────────────────────────

/**
 * Secao "Equipamento local" do resultado/diagnostico (GH#544, epic #547).
 * Renderiza estritamente a partir de [state] — nenhuma regra de negocio
 * aqui, so apresentacao. Ver [mapLocalDeviceSectionUiState] para a decisao
 * de qual estado exibir.
 */
@Composable
fun LocalDeviceSection(
    state: LocalDeviceSectionUiState,
    modifier: Modifier = Modifier,
    /** Indica se a tela onde esta secao esta inserida tem uma acao de refazer
     *  diagnostico disponivel agora (ex.: [io.signallq.app.ui.component.DiagActionFooter]
     *  ja presente na tela) — controla se a nota de leitura desatualizada convida o
     *  usuario a usa-la. Nao cria um botao novo aqui, so reaproveita a acao existente. */
    refazerDisponivel: Boolean = false,
) {
    val c = LocalLkTokens.current

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "EQUIPAMENTO LOCAL",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = c.textTertiary,
            letterSpacing = 0.5.sp,
        )
        Spacer(Modifier.height(LkSpacing.sm))

        when (state) {
            is LocalDeviceSectionUiState.NenhumEncontrado ->
                LocalDeviceEmptyCard(
                    icon = Icons.Outlined.Router,
                    titulo = "Nenhum equipamento encontrado",
                    descricao = "Não identificamos um roteador ou ONT compatível nesta rede.",
                    accent = c.textTertiary,
                )

            is LocalDeviceSectionUiState.EncontradoNaoConectado ->
                LocalDeviceEmptyCard(
                    icon = Icons.Outlined.Router,
                    titulo = "Equipamento identificado",
                    descricao =
                        "Encontramos um " + deviceTypeLabel(state.deviceType).lowercase() +
                            " na rede. Conecte para ver dados detalhados.",
                    accent = LkColors.accent,
                )

            is LocalDeviceSectionUiState.LoginFalhou ->
                LocalDeviceEmptyCard(
                    icon = Icons.Outlined.ErrorOutline,
                    titulo = "Não foi possível conectar ao equipamento",
                    descricao = state.mensagem,
                    accent = LkColors.warning,
                )

            is LocalDeviceSectionUiState.SemSuporte ->
                LocalDeviceEmptyCard(
                    icon = Icons.AutoMirrored.Outlined.HelpOutline,
                    titulo = "Equipamento sem suporte",
                    descricao = semSuporteDescricao(state.deviceType),
                    accent = c.textTertiary,
                )

            is LocalDeviceSectionUiState.Conectado ->
                LocalDeviceConectadoContent(state = state, c = c, refazerDisponivel = refazerDisponivel)
        }
    }
}

@Composable
private fun LocalDeviceEmptyCard(
    icon: ImageVector,
    titulo: String,
    descricao: String,
    accent: Color,
) {
    val c = LocalLkTokens.current
    Row(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg)
                .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier =
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.10f)),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = accent, modifier = Modifier.size(18.dp))
        }
        Spacer(Modifier.width(LkSpacing.md))
        Column(modifier = Modifier.weight(1f)) {
            Text(titulo, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = c.textPrimary)
            Spacer(Modifier.height(2.dp))
            Text(descricao, fontSize = 11.5.sp, color = c.textSecondary, lineHeight = 16.sp)
        }
    }
}

@Composable
private fun LocalDeviceConectadoContent(
    state: LocalDeviceSectionUiState.Conectado,
    c: io.signallq.app.ui.LkTokens,
    refazerDisponivel: Boolean,
) {
    var detalhesExpandidos by remember { mutableStateOf(false) }
    val statusColor = statusParaCor(state.resumoStatus)

    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(LkRadius.card))
                .background(c.bgCard)
                .border(1.dp, c.border, RoundedCornerShape(LkRadius.card))
                .padding(LkSpacing.lg),
    ) {
        // Cabecalho: nome do equipamento + tipo (GH#538, deixa claro se e ONT ou
        // roteador antes de qualquer dado tecnico) + nivel de suporte.
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                deviceTypeIcon(state.deviceType),
                contentDescription = null,
                tint = c.textSecondary,
                modifier = Modifier.size(16.dp),
            )
            Spacer(Modifier.width(LkSpacing.xs))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    state.tituloEquipamento,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = c.textSecondary,
                )
                Text(
                    deviceTypeLabel(state.deviceType),
                    fontSize = 10.sp,
                    color = c.textTertiary,
                )
            }
            FrescorIndicator(freshness = state.freshness, c = c)
            if (!state.completo) {
                Spacer(Modifier.width(LkSpacing.xs))
                SuporteBadge(texto = "Parcial", cor = LkColors.warning)
            }
        }

        // Nota de capability (GH#538) — roteador/mesh nunca leem dado optico de
        // fibra; deixa isso explicito para o usuario nao esperar essa informacao
        // aqui e nao confundir com falha de leitura.
        notaSemLeituraDeFibra(state.deviceType)?.let { nota ->
            Spacer(Modifier.height(4.dp))
            Text(
                nota,
                fontSize = 10.5.sp,
                color = c.textTertiary,
                lineHeight = 14.sp,
            )
        }

        // Aviso de suporte experimental (GH#539) — microcopy exata definida na
        // regra de produto do catalogo, nao deve ser reformulada aqui sem
        // atualizar a issue/regra.
        if (state.experimental) {
            Spacer(Modifier.height(LkSpacing.sm))
            Row(
                modifier =
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .background(LkColors.warning.copy(alpha = 0.08f))
                        .padding(horizontal = LkSpacing.sm, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(Icons.Outlined.Science, contentDescription = null, tint = LkColors.warning, modifier = Modifier.size(14.dp))
                Spacer(Modifier.width(LkSpacing.xs))
                Text(
                    "Suporte experimental. Alguns dados podem não aparecer ou estar incompletos.",
                    fontSize = 10.5.sp,
                    color = c.textSecondary,
                    lineHeight = 14.sp,
                )
            }
        }

        Spacer(Modifier.height(LkSpacing.md))

        // Resumo interpretado — sempre acima dos dados tecnicos crus.
        Row(verticalAlignment = Alignment.Top) {
            Box(
                modifier =
                    Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(statusColor),
            )
            Spacer(Modifier.width(LkSpacing.sm))
            Column {
                Text(state.resumoTitulo, fontSize = 14.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(state.resumoDescricao, fontSize = 12.sp, color = c.textSecondary, lineHeight = 17.sp)
            }
        }

        // Aviso de leitura desatualizada (frescor) — so convida a usar a acao de
        // refazer quando ela ja existe na tela (footer/botao existente, nunca um
        // botao novo criado dentro deste card).
        if (state.freshness.expirado && refazerDisponivel) {
            Spacer(Modifier.height(4.dp))
            Text(
                "Estes dados podem não refletir o estado atual do equipamento — " +
                    "toque em Atualizar para uma nova leitura.",
                fontSize = 10.5.sp,
                color = LkColors.warning,
                lineHeight = 14.sp,
            )
        }

        if (state.secoes.isNotEmpty()) {
            Spacer(Modifier.height(LkSpacing.md))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = "DADOS TÉCNICOS",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = c.textTertiary,
                    letterSpacing = 0.5.sp,
                    modifier = Modifier.weight(1f),
                )
                Text(
                    text = if (detalhesExpandidos) "Ocultar" else "Ver detalhes",
                    fontSize = 11.sp,
                    color = c.textTertiary,
                    modifier =
                        Modifier
                            .semantics { role = Role.Button }
                            .clickable { detalhesExpandidos = !detalhesExpandidos },
                )
            }

            Spacer(Modifier.height(LkSpacing.xs))

            AnimatedVisibility(visible = detalhesExpandidos, enter = expandVertically(), exit = shrinkVertically()) {
                Column(verticalArrangement = Arrangement.spacedBy(LkSpacing.md)) {
                    state.secoes.forEach { secao -> EquipamentoSecaoRow(secao = secao, c = c) }
                }
            }
        }
    }
}

@Composable
private fun EquipamentoSecaoRow(
    secao: EquipamentoSecaoTecnica,
    c: io.signallq.app.ui.LkTokens,
) {
    Column(
        modifier =
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .background(c.bgSecondary)
                .padding(horizontal = 11.dp, vertical = 9.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(secao.icone, contentDescription = null, tint = c.textSecondary, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(6.dp))
            Text(secao.titulo, fontSize = 11.sp, fontWeight = FontWeight.W600, color = c.textSecondary)
        }
        Spacer(Modifier.height(6.dp))
        secao.itens.forEach { item ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 2.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(item.label, fontSize = 11.sp, color = c.textTertiary, modifier = Modifier.weight(1f))
                Text(item.valor, fontSize = 11.sp, fontWeight = FontWeight.W600, color = c.textPrimary)
            }
        }
    }
}

/** Indicador de frescor da leitura no cabecalho do card "Conectado" — traduz
 *  [DataFreshness] em confianca visual: dado recente e neutro (cinza,
 *  discreto), dado marcado como expirado pelo motor vira alerta ambar com
 *  icone, nunca a mesma aparencia de um dado atual. */
@Composable
private fun FrescorIndicator(
    freshness: DataFreshness,
    c: io.signallq.app.ui.LkTokens,
) {
    if (freshness.expirado) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Outlined.ErrorOutline,
                contentDescription = null,
                tint = LkColors.warning,
                modifier = Modifier.size(12.dp),
            )
            Spacer(Modifier.width(2.dp))
            Text(
                "Leitura desatualizada",
                fontSize = 10.sp,
                fontWeight = FontWeight.W600,
                color = LkColors.warning,
            )
        }
    } else {
        Text(
            formatarFrescor(freshness.capturadoEmEpochMs),
            fontSize = 10.sp,
            color = c.textTertiary,
        )
    }
}

@Composable
private fun SuporteBadge(
    texto: String,
    cor: Color,
) {
    Box(
        modifier =
            Modifier
                .clip(RoundedCornerShape(999.dp))
                .background(cor.copy(alpha = 0.10f))
                .padding(horizontal = LkSpacing.sm, vertical = 2.dp),
    ) {
        Text(texto, fontSize = 10.sp, fontWeight = FontWeight.W700, color = cor)
    }
}

private fun deviceTypeIcon(deviceType: DeviceType): ImageVector =
    when (deviceType) {
        DeviceType.ONT_GPON -> Icons.Outlined.Cable
        DeviceType.ROUTER -> Icons.Outlined.Router
        DeviceType.MESH_OR_EXTENDER -> Icons.Outlined.SettingsInputAntenna
        DeviceType.UNKNOWN_SUPPORTED, DeviceType.UNKNOWN_UNSUPPORTED -> Icons.Outlined.DeviceUnknown
    }

private fun deviceTypeLabel(deviceType: DeviceType): String =
    when (deviceType) {
        DeviceType.ONT_GPON -> "ONT de fibra"
        DeviceType.ROUTER -> "Roteador"
        DeviceType.MESH_OR_EXTENDER -> "Nó mesh"
        DeviceType.UNKNOWN_SUPPORTED, DeviceType.UNKNOWN_UNSUPPORTED -> "Equipamento"
    }

/** Descricao do estado "sem suporte" (GH#538) — personaliza pelo tipo quando o
 *  fingerprint ja reconheceu se e ONT ou roteador, mesmo sem driver de leitura. */
private fun semSuporteDescricao(deviceType: DeviceType): String =
    when (deviceType) {
        DeviceType.UNKNOWN_SUPPORTED, DeviceType.UNKNOWN_UNSUPPORTED ->
            "Identificamos um equipamento na rede, mas ainda não sabemos ler dados dele."
        else ->
            "Identificamos um " + deviceTypeLabel(deviceType).lowercase() +
                " na rede, mas ainda não sabemos ler os dados dele."
    }

/** Nota exibida no cabecalho de equipamentos que nao leem dados opticos de fibra
 *  (GH#538) — deixa explicito que a leitura de fibra so aparece na ONT/modem,
 *  sem citar marca/modelo (a regra e por capability, nao por vendor). */
private fun notaSemLeituraDeFibra(deviceType: DeviceType): String? =
    when (deviceType) {
        DeviceType.ROUTER -> "Este roteador não faz leitura de fibra — esse dado aparece apenas na ONT/modem da operadora."
        DeviceType.MESH_OR_EXTENDER -> "Nós mesh não fazem leitura de fibra — esse dado aparece apenas na ONT/modem da operadora."
        else -> null
    }

private fun statusParaCor(status: DiagnosticStatus): Color =
    when (status) {
        DiagnosticStatus.ok -> LkColors.success
        DiagnosticStatus.attention -> LkColors.warning
        DiagnosticStatus.critical -> LkColors.error
        DiagnosticStatus.inconclusive, DiagnosticStatus.info -> LkColors.accent
    }

// ─── Previews — cobrem os 7 estados obrigatorios do GH#544 ─────────────────

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocalDeviceSectionNenhumEncontradoPreview() {
    SignallQTheme { LocalDeviceSection(LocalDeviceSectionUiState.NenhumEncontrado) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocalDeviceSectionEncontradoNaoConectadoPreview() {
    SignallQTheme {
        LocalDeviceSection(
            LocalDeviceSectionUiState.EncontradoNaoConectado(DeviceType.ROUTER, SupportLevel.LAB_VALIDATED),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocalDeviceSectionLoginFalhouPreview() {
    SignallQTheme {
        LocalDeviceSection(
            LocalDeviceSectionUiState.LoginFalhou(
                DeviceType.ROUTER,
                "Usuário ou senha incorretos. Verifique as credenciais do roteador.",
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocalDeviceSectionSemSuportePreview() {
    SignallQTheme { LocalDeviceSection(LocalDeviceSectionUiState.SemSuporte(DeviceType.UNKNOWN_UNSUPPORTED)) }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocalDeviceSectionNokiaCompletoPreview() {
    SignallQTheme {
        LocalDeviceSection(
            LocalDeviceSectionUiState.Conectado(
                tituloEquipamento = "Nokia G-1425G-B",
                deviceType = DeviceType.ONT_GPON,
                supportLevel = SupportLevel.LAB_VALIDATED,
                experimental = false,
                completo = true,
                resumoTitulo = "Sinal de Recepção Bom",
                resumoDescricao = "O sinal de recepção da fibra está dentro da faixa ideal (-19.80 dBm).",
                resumoStatus = DiagnosticStatus.ok,
                secoes =
                    listOf(
                        EquipamentoSecaoTecnica(
                            "Fibra óptica",
                            Icons.Outlined.Cable,
                            listOf(
                                EquipamentoItemTecnico("Link óptico", "Ativo"),
                                EquipamentoItemTecnico("Potência RX", "-19.80 dBm"),
                                EquipamentoItemTecnico("Potência TX", "2.10 dBm"),
                            ),
                        ),
                    ),
                freshness = DataFreshness(capturadoEmEpochMs = System.currentTimeMillis()),
            ),
        )
    }
}

@Preview(showBackground = true, backgroundColor = 0xFFFFFFFF)
@Composable
private fun LocalDeviceSectionRoteadorParcialExperimentalPreview() {
    SignallQTheme {
        LocalDeviceSection(
            LocalDeviceSectionUiState.Conectado(
                tituloEquipamento = "TP-Link Archer C6",
                deviceType = DeviceType.ROUTER,
                supportLevel = SupportLevel.PARSER_IMPORTED,
                experimental = true,
                completo = false,
                resumoTitulo = "Internet chegando normalmente",
                resumoDescricao = "O roteador confirma conexão ativa com a operadora. Wi-Fi ligado e transmitindo.",
                resumoStatus = DiagnosticStatus.ok,
                secoes =
                    listOf(
                        EquipamentoSecaoTecnica(
                            "Internet (WAN)",
                            Icons.Outlined.Dns,
                            listOf(EquipamentoItemTecnico("IP externo", "203.0.113.44"), EquipamentoItemTecnico("Uptime", "2d 4h 10min")),
                        ),
                        EquipamentoSecaoTecnica(
                            "Wi-Fi",
                            Icons.Outlined.Wifi,
                            listOf(EquipamentoItemTecnico("Casa_5G", "5 GHz · canal 44")),
                        ),
                    ),
                freshness = DataFreshness(capturadoEmEpochMs = System.currentTimeMillis() - 3_600_000L, expirado = true),
            ),
            refazerDisponivel = true,
        )
    }
}
