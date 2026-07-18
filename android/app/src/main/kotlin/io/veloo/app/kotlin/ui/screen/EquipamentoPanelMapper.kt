package io.signallq.app.ui.screen

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Router
import androidx.compose.material.icons.outlined.SettingsEthernet
import androidx.compose.material.icons.outlined.Troubleshoot
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material.icons.outlined.Wifi
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import io.signallq.app.core.network.contracts.fibra.ClassificadorSaudeGpon
import io.signallq.app.core.network.contracts.fibra.GponSaudeStatus
import io.signallq.app.core.network.contracts.gateway.AcessoEquipamento
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.TipoConexaoFisica
import io.signallq.app.feature.fibra.DeviceInfoFibra
import io.signallq.app.feature.fibra.GponStatus
import io.signallq.app.feature.fibra.LanStatus
import io.signallq.app.feature.fibra.SnapshotFibra
import io.signallq.app.feature.fibra.WanStatus
import io.signallq.app.ui.LkColors
import io.signallq.app.ui.component.EquipamentoItemTecnico
import io.signallq.app.ui.component.EquipamentoSecaoTecnica
import io.signallq.app.ui.component.LocalDeviceSectionUiState

/**
 * Mapeamento puro (sem Compose) do dado bruto do equipamento para os modelos
 * de UI consumidos pelos cards da tela "Equipamento de internet"
 * (`EquipamentoInternetScreen.kt` + `Equipamento*Card.kt`, mesmo pacote).
 * Extraído da tela em si (bug #6, 2026-07-18) para que o arquivo da tela
 * fique só com composição/ordem e os estados carregando/indisponível, ver
 * `.claude/rules/higiene-e-padronizacao-repositorio.md` seção 4.6.
 */
internal data class EquipmentPanelUi(
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
internal data class EquipmentAlertUi(
    val titulo: String,
    val descricao: String,
    val botaoLabel: String,
)

internal data class DevicesSummaryUi(
    val total: Int,
    val wifi: Int,
    val cabo: Int,
    val flags: List<String> = emptyList(),
)

internal data class EquipmentActionUi(
    val id: String,
    val label: String,
    val icon: ImageVector,
    val enabled: Boolean = true,
    val danger: Boolean = false,
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

internal fun buildEquipmentPanels(
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
                    icone = Icons.Outlined.Router,
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

private fun io.signallq.app.core.network.contracts.localdevice.DeviceType.label(): String =
    when (this) {
        io.signallq.app.core.network.contracts.localdevice.DeviceType.ONT_GPON -> "Equipamento de fibra"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.ROUTER -> "Roteador Wi-Fi"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.MESH_OR_EXTENDER -> "Ponto de acesso / mesh"
        io.signallq.app.core.network.contracts.localdevice.DeviceType.UNKNOWN_SUPPORTED,
        io.signallq.app.core.network.contracts.localdevice.DeviceType.UNKNOWN_UNSUPPORTED,
        -> "Equipamento local"
    }

internal fun acessoLabel(acesso: AcessoEquipamento): String =
    when (acesso) {
        AcessoEquipamento.LEITURA_COMPLETA -> "Leitura completa"
        AcessoEquipamento.LEITURA_PARCIAL -> "Leitura parcial"
        AcessoEquipamento.SOMENTE_IDENTIFICACAO -> "Somente identificação"
        AcessoEquipamento.GERENCIAMENTO_DISPONIVEL -> "Gerenciamento disponível"
        AcessoEquipamento.SESSAO_EXPIRADA -> "Sessão expirada"
        AcessoEquipamento.CREDENCIAIS_NECESSARIAS -> "Credenciais necessárias"
    }
