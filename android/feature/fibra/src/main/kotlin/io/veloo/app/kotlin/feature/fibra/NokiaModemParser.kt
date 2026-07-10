package io.signallq.app.feature.fibra

import org.json.JSONObject
import kotlin.math.floor
import kotlin.math.log10

internal object NokiaModemParser {

    fun parseGpon(html: String): GponStatus? {
        return try {
            val stat = extractJsIntAny(html, listOf("GponConnectionStat"))
            val status = if ((stat ?: 0) == 1) "up" else "down"
            val mode = extractJsStringAny(html, listOf("GponMode", "ConnectionMode")) ?: "—"

            val rxRaw = extractJsNumberAny(html, listOf("RXPower", "RxPower", "ReceivePower"))
            val txRaw = extractJsNumberAny(html, listOf("TXPower", "TxPower", "TransmitPower"))
            val tempRaw = extractJsIntAny(html, listOf("TransceiverTemperature"))
            val serial = extractJsStringAny(html, listOf("SerialNumber", "GPONSerial")) ?: "—"
            // Nokia firmware tem typo: SupplyVottage (sic). Aceitar as duas grafias.
            val voltRaw = extractJsIntAny(html, listOf("SupplyVoltage", "SupplyVottage"))
            // LaserCurrent raw está em 0.5 µA (não µA), então /500 = mA.
            val laserRaw = extractJsIntAny(html, listOf("LaserCurrent", "TxBiasCurrent", "BiasCurrent"))

            val rxDbm = normalizeRx(convertJsRxPowerToDbm(rxRaw?.toString()))
            val txDbm = convertJsTxPowerToDbm(txRaw?.toString())
            // TransceiverTemperature é Q8.8 fixed-point: raw / 256.0 = °C.
            val tempC = (tempRaw ?: 0) / 256.0
            val voltV = (voltRaw ?: 0) / 10000.0
            val laserMa = (laserRaw ?: 0) / 500.0

            GponStatus(
                status = status,
                mode = mode,
                rxPowerDbm = rxDbm,
                txPowerDbm = txDbm,
                temperatureCelsius = tempC,
                serial = serial,
                voltageV = voltV,
                laserCurrentMa = laserMa,
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun parseWan(html: String): WanStatus? {
        return try {
            // Extrai dados do objeto JS wan_conns (path primário no Nokia G-1425G-B).
            val connPattern = Regex(
                """ConnectionStatus:'Connected'[^}]*?ExternalIPAddress:'([^']*)'[^}]*?RemoteIPAddress:'([^']*)'[^}]*?DNSServers:'([^']*)'""",
                setOf(RegexOption.DOT_MATCHES_ALL),
            )
            val match = connPattern.find(html)

            val externalIp = match?.groupValues?.get(1).orEmpty()
            val gateway = match?.groupValues?.get(2).orEmpty()
            val dnsServers = match?.groupValues?.get(3).orEmpty()
            val dnsList = dnsServers.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            val primaryDns = dnsList.getOrElse(0) { "" }
            val secondaryDns = dnsList.getOrElse(1) { "" }

            val vlanId = Regex("""VLANIDMark:\s*(\d+)""").find(html)?.groupValues?.get(1) ?: ""
            val interfaceName = Regex("""X_ASB_COM_IfName:'([^']*)'""").find(html)?.groupValues?.get(1) ?: ""
            val pppoeConc = Regex("""PPPoEACName:'([^']*)'""").find(html)?.groupValues?.get(1) ?: ""
            val uptime = Regex("""Uptime:(\d+),""").find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0
            val connType = Regex("""ConnectionType:'([^']*)'""").find(html)?.groupValues?.get(1) ?: ""

            if (externalIp.isEmpty() && gateway.isEmpty()) return null

            WanStatus(
                externalIp = externalIp.ifEmpty { "—" },
                gateway = gateway.ifEmpty { "—" },
                primaryDns = primaryDns.ifEmpty { "—" },
                secondaryDns = secondaryDns.ifEmpty { "—" },
                vlanId = vlanId.ifEmpty { "—" },
                interfaceName = interfaceName.ifEmpty { "—" },
                pppoeConcentrator = pppoeConc.ifEmpty { "—" },
                connectionType = connType.ifEmpty { "—" },
                connectionUptimeSeconds = uptime,
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun parsePpp(json: String): PppStatus? {
        return try {
            val obj = JSONObject(json)
            val list = obj.optJSONArray("ppp_status") ?: return null
            if (list.length() == 0) return null
            val entry = list.getJSONObject(0)
            val status = entry.optString("ConnectionStatus", "Unknown")
            PppStatus(
                isConnected = status == "Connected",
                connectionStatus = status,
                connectionType = entry.optString("ConnectionType", ""),
                name = entry.optString("Name", ""),
                lastError = entry.optString("LastConnectionError", ""),
            )
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Extrai os radios Wi-Fi (2.4GHz/5GHz) da tela "Home Networking"
     * (`lan_status.cgi?wlan`, objeto JS `wlan_status`) — GH#865 Fase 1.
     * Ver `docs_ai/technical/NOKIA_GPON_FIELD_MAP.md`.
     *
     * `larguraCanal` fica sempre null nesta fase: o unico campo documentado
     * para largura de banda (`X_ASB_COM_OperatingChannelBandwidth`) vive na
     * tela de configuracao (`wlan_config.cgi`), a mesma que carrega PSK/senha
     * em texto plano — nao buscamos essa pagina para evitar qualquer risco de
     * trafegar segredo, mesmo que so leiamos um campo nao-secreto dela.
     */
    fun parseWifi(html: String): WifiStatus? {
        return try {
            val blocks = extractJsObjectBlocks(html, "wlan_status")
            if (blocks.isEmpty()) return null

            val radios = blocks.mapNotNull { block ->
                val ssid = extractJsStringAny(block, listOf("SSID")) ?: return@mapNotNull null
                val standard = extractJsStringAny(block, listOf("Standard")).orEmpty()
                val banda = if (standard.contains("ac", ignoreCase = true) ||
                    standard.contains("ax", ignoreCase = true)
                ) {
                    "5GHz"
                } else {
                    "2.4GHz"
                }
                val habilitado = extractJsBoolAny(block, listOf("RadioEnabled", "Enable")) ?: true
                val canal = extractJsIntAny(block, listOf("Channel"))
                val criptografia = extractJsStringAny(block, listOf("BeaconType")) ?: "—"
                val potenciaRaw = extractJsStringAny(block, listOf("TransmitPower"))
                    ?: extractJsIntAny(block, listOf("TransmitPower"))?.toString()

                WifiRadioStatus(
                    banda = banda,
                    ssid = ssid,
                    canal = canal,
                    habilitado = habilitado,
                    criptografia = criptografia,
                    potenciaTx = potenciaRaw?.let { "$it%" } ?: "—",
                )
            }

            if (radios.isEmpty()) null else WifiStatus(radios)
        } catch (_: Throwable) {
            null
        }
    }

    /**
     * Combina o IP/mascara da propria interface LAN do ONT (`lan_ifip`, tela
     * `lan_status.cgi?lan`) com a configuracao do servidor DHCP (`ipv4_config`,
     * tela `lan_ipv4.cgi`) — GH#865 Fase 1. Ver
     * `docs_ai/technical/NOKIA_GPON_FIELD_MAP.md`.
     */
    fun parseLan(lanStatusHtml: String, lanConfigHtml: String): LanStatus? {
        return try {
            val routerIp = extractJsStringAny(
                lanStatusHtml,
                listOf("IPAddress", "IPInterfaceIPAddress", "LanIPAddress"),
            )
            val maskFromStatus = extractJsStringAny(
                lanStatusHtml,
                listOf("SubnetMask", "IPInterfaceSubnetMask", "NetMask"),
            )
            val maskFromConfig = extractJsStringAny(lanConfigHtml, listOf("SubnetMask"))
            val dhcpMin = extractJsStringAny(lanConfigHtml, listOf("MinAddress"))
            val dhcpMax = extractJsStringAny(lanConfigHtml, listOf("MaxAddress"))
            val dhcpEnabled = extractJsBoolAny(lanConfigHtml, listOf("DHCPServerEnable"))

            if (routerIp.isNullOrEmpty() && dhcpMin.isNullOrEmpty()) return null

            LanStatus(
                routerIp = routerIp?.ifEmpty { "—" } ?: "—",
                subnetMask = (maskFromStatus ?: maskFromConfig)?.ifEmpty { "—" } ?: "—",
                dhcpHabilitado = dhcpEnabled ?: false,
                dhcpFaixaInicio = dhcpMin?.ifEmpty { "—" } ?: "—",
                dhcpFaixaFim = dhcpMax?.ifEmpty { "—" } ?: "—",
            )
        } catch (_: Throwable) {
            null
        }
    }

    fun parseDeviceInfo(html: String): DeviceInfoFibra? {
        return try {
            val model = Regex(""""ModelName":"([^"]*)"""").find(html)?.groupValues?.get(1) ?: ""
            val manufacturer = Regex(""""Manufacturer":"([^"]*)"""").find(html)?.groupValues?.get(1) ?: ""
            val serial = Regex(""""SerialNumber":"([^"]*)"""").find(html)?.groupValues?.get(1) ?: ""
            val firmware = Regex(""""SoftwareVersion":"([^"]*)"""").find(html)?.groupValues?.get(1) ?: ""
            val hardware = Regex(""""HardwareVersion":"([^"]*)"""").find(html)?.groupValues?.get(1) ?: ""
            val uptime = Regex(""""UpTime":(\d+)""").find(html)?.groupValues?.get(1)?.toIntOrNull() ?: 0

            if (model.isEmpty() && serial.isEmpty()) return null

            DeviceInfoFibra(
                model = model.ifEmpty { "—" },
                manufacturer = manufacturer.ifEmpty { "—" },
                serialNumber = serial.ifEmpty { "—" },
                firmwareVersion = firmware.ifEmpty { "—" },
                hardwareVersion = hardware.ifEmpty { "—" },
                uptimeSeconds = uptime,
            )
        } catch (_: Throwable) {
            null
        }
    }

    // --- Helpers de extração JS ---

    internal fun extractJsStringAny(source: String, keys: List<String>): String? {
        for (key in keys) {
            val escaped = Regex.escape(key)
            val patterns = listOf(
                Regex(""""$escaped"\s*:\s*"([^"]*)""""),
                Regex(""""$escaped"\s*:\s*'([^']*)'"""),
                Regex("""\b$escaped\b\s*:\s*"([^"]*)""""),
                Regex("""\b$escaped\b\s*:\s*'([^']*)'"""),
                Regex("""\b$escaped\b\s*=\s*"([^"]*)""""),
                Regex("""\b$escaped\b\s*=\s*'([^']*)'"""),
            )
            for (p in patterns) {
                val m = p.find(source) ?: continue
                return m.groupValues[1]
            }
        }
        return null
    }

    internal fun extractJsIntAny(source: String, keys: List<String>): Int? {
        for (key in keys) {
            val escaped = Regex.escape(key)
            val patterns = listOf(
                Regex(""""$escaped"\s*:\s*(-?\d+)"""),
                Regex("""\b$escaped\b\s*:\s*(-?\d+)"""),
                Regex("""\b$escaped\b\s*=\s*(-?\d+)"""),
            )
            for (p in patterns) {
                val m = p.find(source) ?: continue
                return m.groupValues[1].toIntOrNull()
            }
        }
        return null
    }

    internal fun extractJsBoolAny(source: String, keys: List<String>): Boolean? {
        for (key in keys) {
            val escaped = Regex.escape(key)
            val patterns = listOf(
                Regex(""""$escaped"\s*:\s*(true|false)""", RegexOption.IGNORE_CASE),
                Regex("""\b$escaped\b\s*:\s*(true|false)""", RegexOption.IGNORE_CASE),
                Regex("""\b$escaped\b\s*=\s*(true|false)""", RegexOption.IGNORE_CASE),
            )
            for (p in patterns) {
                val m = p.find(source) ?: continue
                return m.groupValues[1].equals("true", ignoreCase = true)
            }
        }
        // Alguns firmwares reportam booleano como 0/1 em vez de true/false.
        val intVal = extractJsIntAny(source, keys) ?: return null
        return intVal == 1
    }

    /**
     * Localiza um array JS pelo nome da variavel (ex: `wlan_status:[...]` ou
     * `var lan_ether = [...]`) e retorna o conteudo bruto de cada objeto
     * `{...}` do array, respeitando aninhamento de chaves (ex: um `stat:{...}`
     * dentro do item). Necessario porque os campos novos deste parser vivem
     * em listas (varios radios Wi-Fi, varias portas LAN), diferente dos
     * objetos unicos ja tratados por [extractJsStringAny]/[extractJsIntAny].
     */
    internal fun extractJsObjectBlocks(source: String, arrayVarName: String): List<String> {
        val escaped = Regex.escape(arrayVarName)
        val varMatch = Regex("""\b$escaped\b\s*[:=]\s*\[""").find(source) ?: return emptyList()
        val arrayStart = varMatch.range.last // índice do '['

        var depthBracket = 0
        var arrayEnd = -1
        var i = arrayStart
        while (i < source.length) {
            when (source[i]) {
                '[' -> depthBracket++
                ']' -> {
                    depthBracket--
                    if (depthBracket == 0) {
                        arrayEnd = i
                        break
                    }
                }
            }
            i++
        }
        if (arrayEnd == -1) return emptyList()

        val arrayContent = source.substring(arrayStart + 1, arrayEnd)
        val blocks = mutableListOf<String>()
        var depthBrace = 0
        var blockStart = -1
        var j = 0
        while (j < arrayContent.length) {
            when (arrayContent[j]) {
                '{' -> {
                    if (depthBrace == 0) blockStart = j
                    depthBrace++
                }
                '}' -> {
                    depthBrace--
                    if (depthBrace == 0 && blockStart != -1) {
                        blocks.add(arrayContent.substring(blockStart, j + 1))
                        blockStart = -1
                    }
                }
            }
            j++
        }
        return blocks
    }

    private fun extractJsNumberAny(source: String, keys: List<String>): String? {
        val quoted = extractJsStringAny(source, keys)
        if (quoted != null) return quoted
        for (key in keys) {
            val escaped = Regex.escape(key)
            val patterns = listOf(
                Regex(""""$escaped"\s*:\s*(-?\d+(?:[,.]\d+)?)"""),
                Regex("""\b$escaped\b\s*:\s*(-?\d+(?:[,.]\d+)?)"""),
                Regex("""\b$escaped\b\s*=\s*(-?\d+(?:[,.]\d+)?)"""),
            )
            for (p in patterns) {
                val m = p.find(source) ?: continue
                return m.groupValues[1]
            }
        }
        return null
    }

    // Alguns firmwares exibem RX sem o sinal negativo (ex: "22.7" em vez de "-22.7").
    private fun normalizeRx(rxDbm: Double): Double {
        if (rxDbm > 0 && rxDbm >= 1 && rxDbm <= 60) return -rxDbm
        return rxDbm
    }

    // Converte valor bruto JS (milliwatts) para dBm.
    // Fórmula do modem: floor(log10(mW * 0.00001) * 1000) / 100
    private fun convertJsRxPowerToDbm(raw: String?): Double {
        if (raw.isNullOrEmpty()) return 0.0
        val mw = raw.toDoubleOrNull()?.toInt() ?: return 0.0
        if (mw <= 0) return 0.0
        val converted = floor(log10(mw * 0.00001) * 1000) / 100.0
        if (converted > 0 || converted < -80) return 0.0
        return converted
    }

    private fun convertJsTxPowerToDbm(raw: String?): Double {
        if (raw.isNullOrEmpty()) return 0.0
        val mw = raw.toDoubleOrNull()?.toInt() ?: return 0.0
        if (mw <= 0) return 0.0
        val converted = floor(log10(mw * 0.00001) * 1000) / 100.0
        if (converted > 10 || converted < -40) return 0.0
        return converted
    }
}
