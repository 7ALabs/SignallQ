package io.signallq.app.feature.devices

enum class TipoDispositivo {
    roteador,
    pontoAcesso,
    computador,
    smartphone,
    smarthome,
    impressora,
    desconhecido,
}

internal object ClassificadorDispositivoRede {

    private val fabricantesRoteador = setOf(
        "TP-Link", "D-Link", "Netgear", "ASUS", "Cisco", "Nokia",
        "Arris", "Mikrotik", "Intelbras", "ZTE", "Sagemcom", "Huawei", "Tenda",
    )
    private val fabricantesComputador = setOf("Intel", "Realtek", "Qualcomm", "Microsoft", "Dell", "HP", "Lenovo")
    private val fabricantesSmartphone = setOf("Apple", "Samsung", "Xiaomi", "Motorola", "LG", "Sony", "OnePlus", "Oppo", "Realme")
    private val fabricantesSmarthome  = setOf("Google", "Amazon", "Nintendo", "Roku")
    private val fabricantesPontoAcesso = setOf("Ubiquiti", "Aruba", "Linksys", "Ruckus", "Eero", "Extreme Networks", "EnGenius", "Cambium Networks")

    private val nomesImpressora  = listOf("printer", "impressora", "hp laserjet", "epson", "canon", "brother", "xerox", "lexmark", "ricoh", "kyocera")
    private val nomesApMesh      = listOf("eero", "deco", "orbi", "velop", "aimesh", "ai mesh", "nest wifi", "nest-wifi", "unifi", "ubiquiti", "access point", "accesspoint", "extender", "repeater", "wifi-ap", "wifi_ap", "-ap-", "_ap_")
    private val nomesApple       = listOf("iphone", "ipad", "macbook", "mac mini", "mac pro", "imac", "ipod", "airpods", "homepod", "apple tv", "apple watch")
    private val nomesSamsung     = listOf("galaxy", "samsung")
    private val nomesXiaomi      = listOf("redmi", "xiaomi", " mi ", "poco", "mipad")
    private val nomesMotorola    = listOf("motorola", "moto g", "moto e")
    private val nomesAmazon      = listOf("echo", "kindle", "fire tv", "firetv", "alexa", "amazon")
    private val nomesGoogle      = listOf("chromecast", "nest hub", "nest mini", "google home", "pixel")
    private val nomesMicrosoft   = listOf("xbox", "surface", "microsoft")
    private val nomesSony        = listOf("playstation", "ps5", "ps4", "ps3", "bravia", "sony")
    private val nomesRoteador    = listOf("router", "roteador", "gateway", "modem", "tplink", "tp-link", "dlink", "d-link", "asus rt", "mikrotik", "routeros", "ubnt", "edgerouter")
    private val nomesComputador  = listOf("desktop", "laptop", "notebook", "workstation", "pc-", "-pc", "thinkpad", "ideapad", "inspiron", "latitude", "elitebook", "probook")
    private val nomesNas         = listOf("synology", "qnap", "diskstation", "nas", "readynas", "terramaster")
    private val nomesSmartTv     = listOf("samsung tv", "lg tv", "philips tv", "sony tv", "tcl", "hisense", "vizio", "tizen", "webos")

    private data class MdnsRule(val prefixo: String, val tipo: TipoDispositivo)

    private val mdnsRules = listOf(
        MdnsRule("_airplay",          TipoDispositivo.smarthome),
        MdnsRule("_raop",             TipoDispositivo.smarthome),
        MdnsRule("_companion-link",   TipoDispositivo.smartphone),
        MdnsRule("_homekit",          TipoDispositivo.smarthome),
        MdnsRule("_googlecast",       TipoDispositivo.smarthome),
        MdnsRule("_spotify-connect",  TipoDispositivo.smarthome),
        MdnsRule("_ipp",              TipoDispositivo.impressora),
        MdnsRule("_printer",          TipoDispositivo.impressora),
        MdnsRule("_pdl-datastream",   TipoDispositivo.impressora),
        MdnsRule("_smb",              TipoDispositivo.computador),
        MdnsRule("_afpovertcp",       TipoDispositivo.computador),
        MdnsRule("_ssh",              TipoDispositivo.computador),
    )

    private fun classificarPorPortas(portas: Set<Int>): TipoDispositivo? {
        if (portas.isEmpty()) return null
        if (9100 in portas || 515 in portas) return TipoDispositivo.impressora
        if (53 in portas) return TipoDispositivo.roteador
        if (445 in portas || 139 in portas || 548 in portas) return TipoDispositivo.computador
        if (22 in portas && (80 in portas || 443 in portas || 8080 in portas || 8443 in portas)) return TipoDispositivo.roteador
        if (80 in portas || 8080 in portas || 8443 in portas) return TipoDispositivo.roteador
        return null
    }

    fun classificar(dispositivo: DispositivoRede, fabricante: String?): TipoDispositivo {
        if (dispositivo.fonteNome == "gateway") return TipoDispositivo.roteador

        val nome = dispositivo.nomeExibicao.lowercase()
        val servicos = dispositivo.tiposServicoMdns
        val portas = dispositivo.portasAbertas

        if (nomesImpressora.any { nome.contains(it) }) return TipoDispositivo.impressora
        if (servicos.any { s -> nomesImpressora.any { s.startsWith(it) } }) return TipoDispositivo.impressora

        if (nomesApMesh.any { nome.contains(it) }) return TipoDispositivo.pontoAcesso

        for (rule in mdnsRules) {
            if (servicos.any { it.startsWith(rule.prefixo) }) return rule.tipo
        }

        if (nomesApple.any    { nome.contains(it) }) return TipoDispositivo.smartphone
        if (nomesSamsung.any  { nome.contains(it) }) return TipoDispositivo.smartphone
        if (nomesXiaomi.any   { nome.contains(it) }) return TipoDispositivo.smartphone
        if (nomesMotorola.any { nome.contains(it) }) return TipoDispositivo.smartphone
        if (nomesSony.any     { nome.contains(it) }) return TipoDispositivo.smarthome
        if (nomesAmazon.any   { nome.contains(it) }) return TipoDispositivo.smarthome
        if (nomesGoogle.any   { nome.contains(it) }) return TipoDispositivo.smarthome
        if (nomesMicrosoft.any{ nome.contains(it) }) return TipoDispositivo.computador
        if (nomesRoteador.any { nome.contains(it) }) return TipoDispositivo.roteador
        if (nomesComputador.any{ nome.contains(it) }) return TipoDispositivo.computador
        if (nomesNas.any      { nome.contains(it) }) return TipoDispositivo.computador
        if (nomesSmartTv.any  { nome.contains(it) }) return TipoDispositivo.smarthome

        val porPorta = classificarPorPortas(portas)
        if (porPorta != null) return porPorta

        return when (fabricante) {
            in fabricantesPontoAcesso -> TipoDispositivo.pontoAcesso
            in fabricantesRoteador    -> TipoDispositivo.roteador
            in fabricantesSmarthome   -> TipoDispositivo.smarthome
            in fabricantesComputador  -> TipoDispositivo.computador
            in fabricantesSmartphone  -> TipoDispositivo.smartphone
            "Huawei"   -> if (dispositivo.fonteNome in setOf("arp", "tcp")) TipoDispositivo.roteador else TipoDispositivo.smartphone
            "Motorola" -> TipoDispositivo.smartphone
            else       -> TipoDispositivo.desconhecido
        }
    }
}
