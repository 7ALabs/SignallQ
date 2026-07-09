package io.signallq.app.core.network.contracts.localdevice

/**
 * Payload seguro de equipamento local (ONT/roteador) — único formato
 * permitido para IA, analytics e logs técnicos (GH#541).
 *
 * Allowlist estrita: qualquer campo novo em [LocalNetworkDeviceSnapshot]
 * fica de fora daqui até ser explicitamente avaliado quanto a sensibilidade
 * e adicionado por [LocalDeviceSafeFilter]. Nunca adicionar campos de
 * credencial/cookie/token, MAC completo, IP público completo, identificador
 * persistente sem anonimização, payload bruto do equipamento (HTML/JSON de
 * admin) ou lista crua de clientes.
 */
data class SafeLocalDeviceContext(
    val vendor: String?,
    val modelo: String?,
    val firmwareVersion: String?,
    val deviceType: DeviceType,
    val supportLevel: SupportLevel,
    val capabilities: DeviceCapabilities,
    val connectionStatus: LocalDeviceSectionStatus,
    val statusFibra: LocalDeviceSectionStatus,
    val statusWan: LocalDeviceSectionStatus,
    val statusWifi: LocalDeviceSectionStatus,
    val statusLan: LocalDeviceSectionStatus,
    val quantidadeClientes: Int,
    val warnings: List<DeviceWarning>,
    val coletadoEmEpochMs: Long,
)

/** Status agregado de uma seção do equipamento — nunca a métrica bruta que o originou. */
enum class LocalDeviceSectionStatus {
    /** Seção suportada, leitura presente e sem sinal de problema. */
    OK,

    /** Seção suportada e lida, mas com sinal de degradação (ex.: link de fibra caído). */
    ATENCAO,

    /** Seção suportada pelo equipamento, mas sem leitura disponível nesta captura. */
    INDISPONIVEL,

    /** Equipamento não suporta esta seção (ex.: fibra em roteador sem ONT). */
    NAO_SUPORTADO,
}

/**
 * Filtro centralizado de dados de equipamento local — GH#541.
 *
 * Único ponto de conversão entre [LocalNetworkDeviceSnapshot] (contrato
 * completo, uso interno/UI) e [SafeLocalDeviceContext] (allowlist, único
 * formato aceito para IA/analytics/logs técnicos). IA, analytics e logging
 * NUNCA devem receber [LocalNetworkDeviceSnapshot] bruto — sempre o
 * resultado de [filtrar].
 */
object LocalDeviceSafeFilter {

    fun filtrar(snapshot: LocalNetworkDeviceSnapshot): SafeLocalDeviceContext =
        SafeLocalDeviceContext(
            vendor = snapshot.vendor,
            modelo = snapshot.modelo,
            firmwareVersion = snapshot.firmwareVersion,
            deviceType = snapshot.deviceType,
            supportLevel = snapshot.supportLevel,
            capabilities = snapshot.capabilities,
            connectionStatus = connectionStatus(snapshot),
            statusFibra = fibraStatus(snapshot),
            statusWan = wanStatus(snapshot),
            statusWifi = wifiStatus(snapshot),
            statusLan = lanStatus(snapshot),
            quantidadeClientes = snapshot.clientes.size,
            warnings = snapshot.warnings,
            coletadoEmEpochMs = snapshot.freshness.capturadoEmEpochMs,
        )

    /** Status geral de comunicação com o equipamento — falha de login/comunicação prevalece. */
    private fun connectionStatus(snapshot: LocalNetworkDeviceSnapshot): LocalDeviceSectionStatus {
        val temErroComunicacao = snapshot.warnings.any {
            it.type == DeviceWarningType.LOGIN_FALHOU || it.type == DeviceWarningType.ERRO_COMUNICACAO
        }
        if (temErroComunicacao) return LocalDeviceSectionStatus.INDISPONIVEL
        val temDadosParciais = snapshot.warnings.any { it.type == DeviceWarningType.DADOS_PARCIAIS }
        return if (temDadosParciais) LocalDeviceSectionStatus.ATENCAO else LocalDeviceSectionStatus.OK
    }

    private fun fibraStatus(snapshot: LocalNetworkDeviceSnapshot): LocalDeviceSectionStatus {
        if (!snapshot.capabilities.suportaFibra) return LocalDeviceSectionStatus.NAO_SUPORTADO
        val fiber = snapshot.fiber ?: return LocalDeviceSectionStatus.INDISPONIVEL
        return when (fiber.linkAtivo) {
            true -> LocalDeviceSectionStatus.OK
            false -> LocalDeviceSectionStatus.ATENCAO
            null -> LocalDeviceSectionStatus.INDISPONIVEL
        }
    }

    private fun wanStatus(snapshot: LocalNetworkDeviceSnapshot): LocalDeviceSectionStatus {
        if (!snapshot.capabilities.suportaWan) return LocalDeviceSectionStatus.NAO_SUPORTADO
        val wan = snapshot.wan ?: return LocalDeviceSectionStatus.INDISPONIVEL
        return if (wan.ipExterno != null || wan.gateway != null) {
            LocalDeviceSectionStatus.OK
        } else {
            LocalDeviceSectionStatus.ATENCAO
        }
    }

    private fun wifiStatus(snapshot: LocalNetworkDeviceSnapshot): LocalDeviceSectionStatus {
        if (!snapshot.capabilities.suportaWifi) return LocalDeviceSectionStatus.NAO_SUPORTADO
        val wifi = snapshot.wifi ?: return LocalDeviceSectionStatus.INDISPONIVEL
        if (wifi.radios.isEmpty()) return LocalDeviceSectionStatus.INDISPONIVEL
        return if (wifi.radios.any { it.habilitado == true }) {
            LocalDeviceSectionStatus.OK
        } else {
            LocalDeviceSectionStatus.ATENCAO
        }
    }

    private fun lanStatus(snapshot: LocalNetworkDeviceSnapshot): LocalDeviceSectionStatus {
        if (!snapshot.capabilities.suportaLan) return LocalDeviceSectionStatus.NAO_SUPORTADO
        val lan = snapshot.lan ?: return LocalDeviceSectionStatus.INDISPONIVEL
        return if (lan.ipRoteador != null) LocalDeviceSectionStatus.OK else LocalDeviceSectionStatus.ATENCAO
    }
}
