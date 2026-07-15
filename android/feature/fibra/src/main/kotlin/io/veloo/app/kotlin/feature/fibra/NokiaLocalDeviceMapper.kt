package io.signallq.app.feature.fibra

import io.signallq.app.core.network.contracts.localdevice.ClientSnapshot
import io.signallq.app.core.network.contracts.localdevice.DataFreshness
import io.signallq.app.core.network.contracts.localdevice.DeviceCapabilities
import io.signallq.app.core.network.contracts.localdevice.DeviceType
import io.signallq.app.core.network.contracts.localdevice.FiberSnapshot
import io.signallq.app.core.network.contracts.localdevice.LanSnapshot
import io.signallq.app.core.network.contracts.localdevice.LocalNetworkDeviceSnapshot
import io.signallq.app.core.network.contracts.localdevice.SupportLevel
import io.signallq.app.core.network.contracts.localdevice.WanSnapshot
import io.signallq.app.core.network.contracts.localdevice.WifiRadioSnapshot
import io.signallq.app.core.network.contracts.localdevice.WifiSnapshot

/**
 * Converte o [SnapshotFibra] real do roteador Nokia GPON (leitura ja
 * autenticada via [NokiaModemClient]/[ExecutorFibra]) para o contrato
 * normalizado [LocalNetworkDeviceSnapshot] (GH#546, epic #547) — GH#865
 * Fase 1.
 *
 * So produz snapshot para leituras concluidas com sucesso ([EstadoFibra.concluido]
 * e dado optico presente); leitura em andamento, idle ou com erro de login
 * fica fora de escopo desta fase (a UI trata `null` como "sem leitura").
 */
object NokiaLocalDeviceMapper {

    fun map(snapshot: SnapshotFibra, capturadoEmEpochMs: Long): LocalNetworkDeviceSnapshot? {
        if (snapshot.estado != EstadoFibra.concluido) return null
        val gpon = snapshot.gpon ?: return null

        return LocalNetworkDeviceSnapshot(
            deviceType = DeviceType.ONT_GPON,
            supportLevel = SupportLevel.LAB_VALIDATED,
            capabilities = DeviceCapabilities(
                suportaFibra = true,
                suportaWan = true,
                suportaWifi = true,
                suportaLan = true,
                // GH#839/#865 Fase 2 — lista real de clientes (device_cfg +
                // alias_cfg), ligada em 2026-07-10.
                suportaClientes = true,
                suportaDiagnosticoNativo = false,
                // Nokia G-1425G-B é o único provider real com ação de gerência
                // implementada hoje (reboot.cgi, GH#934) — ver NokiaModemClient.reboot.
                suportaGerenciamento = true,
            ),
            vendor = "Nokia",
            modelo = snapshot.deviceInfo?.model,
            firmwareVersion = snapshot.deviceInfo?.firmwareVersion,
            fiber = FiberSnapshot(
                linkAtivo = gpon.isUp,
                rxPowerDbm = gpon.rxPowerDbm,
                txPowerDbm = gpon.txPowerDbm,
                temperaturaCelsius = gpon.temperatureCelsius,
                tensaoV = gpon.voltageV,
                correnteLaserMa = gpon.laserCurrentMa,
                serialOnt = gpon.serial,
            ),
            wan = snapshot.wan?.let { wan ->
                WanSnapshot(
                    ipExterno = wan.externalIp,
                    gateway = wan.gateway,
                    dnsPrimario = wan.primaryDns,
                    dnsSecundario = wan.secondaryDns,
                    tipoConexao = wan.connectionType,
                    nomeInterface = wan.interfaceName,
                    uptimeSegundos = wan.connectionUptimeSeconds,
                )
            },
            wifi = snapshot.wifi?.let { wifi ->
                WifiSnapshot(
                    radios = wifi.radios.map { radio ->
                        WifiRadioSnapshot(
                            banda = radio.banda,
                            ssid = radio.ssid,
                            canal = radio.canal,
                            // Nao capturado nesta fase — ver comentario em
                            // NokiaModemParser.parseWifi.
                            larguraCanal = null,
                            potenciaTx = radio.potenciaTx,
                            criptografia = radio.criptografia,
                            habilitado = radio.habilitado,
                        )
                    },
                )
            },
            lan = snapshot.lan?.let { lan ->
                LanSnapshot(
                    ipRoteador = lan.routerIp,
                    mascara = lan.subnetMask,
                    dhcpHabilitado = lan.dhcpHabilitado,
                    faixaDhcpInicio = lan.dhcpFaixaInicio,
                    faixaDhcpFim = lan.dhcpFaixaFim,
                )
            },
            clientes = snapshot.clientes.map { cliente ->
                ClientSnapshot(
                    mac = cliente.mac,
                    ip = cliente.ip,
                    hostname = cliente.hostname,
                    tipoConexao = cliente.tipoConexao,
                    tipoConexaoFisica = cliente.tipoConexaoFisica,
                )
            },
            warnings = emptyList(),
            freshness = DataFreshness(capturadoEmEpochMs = capturadoEmEpochMs, expirado = false),
        )
    }
}
