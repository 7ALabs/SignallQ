package io.veloo.app.feature.devices

import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.LinkProperties
import android.net.Network
import android.util.Log
import androidx.core.content.ContextCompat
import android.net.wifi.WifiManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.MulticastSocket
import java.net.NetworkInterface
import java.net.Socket
import java.net.SocketException
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.math.max
import kotlin.math.min

class ScannerDispositivosAndroid(
    private val context: Context,
) : ScannerDispositivos {
    private val scanEmAndamento = AtomicBoolean(false)

    private val mutableSnapshotFlow =
        MutableStateFlow(
            SnapshotScanDispositivos(
                estado = EstadoScanDispositivos.idle,
                progressoPercentual = 0,
                dispositivos = emptyList(),
                erroMensagem = null,
            ),
        )

    override val snapshotFlow: StateFlow<SnapshotScanDispositivos> = mutableSnapshotFlow.asStateFlow()

    override suspend fun iniciarScan(profundo: Boolean) {
        withContext(Dispatchers.IO) {
            if (!scanEmAndamento.compareAndSet(false, true)) {
                Log.d("LinkaDevices", "scan ja em andamento, ignorando")
                return@withContext
            }
            try {
                if (!temPermissaoParaScan()) {
                    mutableSnapshotFlow.value =
                        mutableSnapshotFlow.value.copy(
                            estado = EstadoScanDispositivos.erro,
                            progressoPercentual = 0,
                            erroMensagem = "semPermissaoLocalizacao",
                        )
                    return@withContext
                }

                atualizarEstado(EstadoScanDispositivos.varrendo, 5, null)
                val dispositivos = linkedMapOf<String, DispositivoRede>()
                val localIp = detectarIpLocal()
                Log.d("LinkaDevices", "ipLocal=$localIp")

                val gatewayIp = detectarGatewayIp()
                Log.d("LinkaDevices", "gateway=$gatewayIp")
                if (!gatewayIp.isNullOrBlank()) {
                    adicionarDispositivo(
                        dispositivos,
                        DispositivoRede(
                            id = "gateway:$gatewayIp",
                            ip = gatewayIp,
                            mac = null,
                            nomeExibicao = "Gateway",
                            fonteNome = "gateway",
                        ),
                    )
                }
                publicar(dispositivos.values.toList(), 10)

                if (profundo) {
                    // Varredura ICMP: popula a tabela ARP do kernel antes de lê-la.
                    // Apps concorrentes usam exatamente esta técnica — sem ela a tabela
                    // ARP só contém dispositivos com comunicação recente.
                    val subnet = inferirPrefixoRede(gatewayIp ?: localIp)
                    Log.d("LinkaDevices", "subnet=$subnet")
                    if (subnet != null) varrerSubrede(subnet)
                    publicar(dispositivos.values.toList(), 45)

                    val arp = coletarViaArp()
                    Log.d("LinkaDevices", "arp: ${arp.size} dispositivos")
                    arp.forEach { adicionarDispositivo(dispositivos, it) }
                    publicar(dispositivos.values.toList(), 55)

                    val mdns = coletarViaMdns()
                    Log.d("LinkaDevices", "mdns: ${mdns.size} dispositivos")
                    mdns.forEach { adicionarDispositivo(dispositivos, it) }
                    publicar(dispositivos.values.toList(), 68)

                    val ssdp = coletarViaSsdp()
                    Log.d("LinkaDevices", "ssdp: ${ssdp.size} dispositivos")
                    ssdp.forEach { adicionarDispositivo(dispositivos, it) }
                    publicar(dispositivos.values.toList(), 80)

                    val nbns = coletarViaNbns(gatewayIp)
                    Log.d("LinkaDevices", "nbns: ${nbns.size} dispositivos")
                    nbns.forEach { adicionarDispositivo(dispositivos, it) }
                    publicar(dispositivos.values.toList(), 90)

                    // TCP probe complementa ARP para dispositivos que bloqueiam ICMP
                    val tcpProbe = coletarViaTcpProbe(gatewayIp)
                    Log.d("LinkaDevices", "tcpProbe: ${tcpProbe.size} dispositivos")
                    tcpProbe.forEach { adicionarDispositivo(dispositivos, it) }
                    publicar(dispositivos.values.toList(), 96)
                } else {
                    val arp = coletarViaArp()
                    Log.d("LinkaDevices", "scan leve arp: ${arp.size} dispositivos")
                    arp.forEach { adicionarDispositivo(dispositivos, it) }
                    publicar(dispositivos.values.toList(), 65)
                }

                val genericosParaResolver = setOf(
                    "Dispositivo não identificado", "Host ativo",
                    "Serviço mDNS", "Dispositivo SSDP", "Host NetBIOS",
                )
                val dispositivosEnriquecidos = dispositivos.values.map { d ->
                    val fabricante = OuiDatabase.lookupFabricante(d.mac)
                    val tipo = ClassificadorDispositivoRede.classificar(d, fabricante)
                    val hostname = if (d.ip != null && d.fonteNome != "gateway") resolverHostname(d.ip) else null
                    val nomeResolvido = when {
                        d.fonteNome == "gateway" -> d.nomeExibicao
                        hostname != null -> hostname
                        d.nomeExibicao !in genericosParaResolver -> d.nomeExibicao
                        fabricante != null -> fabricante
                        else -> d.ip ?: d.nomeExibicao
                    }
                    d.copy(
                        fabricante = fabricante,
                        tipoDispositivo = tipo,
                        nomeExibicao = nomeResolvido,
                        esteDispositivo = localIp != null && d.ip == localIp,
                    )
                }
                Log.d("LinkaDevices", "scan concluido: ${dispositivosEnriquecidos.size} dispositivos")
                mutableSnapshotFlow.value =
                    mutableSnapshotFlow.value.copy(
                        estado = EstadoScanDispositivos.concluido,
                        progressoPercentual = 100,
                        dispositivos = dispositivosEnriquecidos,
                        erroMensagem = null,
                    )
            } catch (t: Throwable) {
                Log.e("LinkaDevices", "scan falhou", t)
                val erroSemantico = when {
                    t is SecurityException -> "semPermissaoLocalizacao"
                    t is SocketException -> "erroRede"
                    t.message?.contains("timeout", ignoreCase = true) == true -> "timeout"
                    else -> "erroDesconhecido"
                }
                mutableSnapshotFlow.value =
                    mutableSnapshotFlow.value.copy(
                        estado = EstadoScanDispositivos.erro,
                        erroMensagem = erroSemantico,
                    )
            } finally {
                scanEmAndamento.set(false)
            }
        }
    }

    private fun temPermissaoParaScan(): Boolean {
        val localizacaoOk =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.ACCESS_FINE_LOCATION,
            ) == PackageManager.PERMISSION_GRANTED
        if (!localizacaoOk) return false

        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.TIRAMISU) {
            return true
        }

        val nearbyWifiOk =
            ContextCompat.checkSelfPermission(
                context,
                android.Manifest.permission.NEARBY_WIFI_DEVICES,
            ) == PackageManager.PERMISSION_GRANTED
        return nearbyWifiOk
    }

    private fun publicar(dispositivos: List<DispositivoRede>, progresso: Int) {
        mutableSnapshotFlow.value =
            mutableSnapshotFlow.value.copy(
                estado = EstadoScanDispositivos.varrendo,
                progressoPercentual = min(99, max(0, progresso)),
                dispositivos = enriquecer(dispositivos),
            )
    }

    private fun enriquecer(dispositivos: List<DispositivoRede>): List<DispositivoRede> =
        dispositivos.map { d ->
            val fabricante = OuiDatabase.lookupFabricante(d.mac)
            d.copy(
                fabricante = fabricante,
                tipoDispositivo = ClassificadorDispositivoRede.classificar(d, fabricante),
            )
        }

    private fun atualizarEstado(estado: EstadoScanDispositivos, progresso: Int, erro: String?) {
        mutableSnapshotFlow.value =
            mutableSnapshotFlow.value.copy(
                estado = estado,
                progressoPercentual = progresso,
                erroMensagem = erro,
            )
    }

    private fun adicionarDispositivo(
        mapa: LinkedHashMap<String, DispositivoRede>,
        dispositivo: DispositivoRede,
    ) {
        val chave = dispositivo.mac?.lowercase(Locale.ROOT) ?: "ip:${dispositivo.ip}" ?: dispositivo.id
        val existente = mapa[chave]
        if (existente == null) {
            mapa[chave] = dispositivo
            return
        }

        // Prioridade de fonte: ssdp > mdns > nbns > arp > tcpProbe
        val prioFonte = mapOf("ssdp" to 4, "mdns" to 3, "nbns" to 2, "arp" to 1, "tcpProbe" to 0)
        val prioExistente = prioFonte[existente.fonteNome] ?: 0
        val prioNova = prioFonte[dispositivo.fonteNome] ?: 0
        val genericos = setOf("Dispositivo não identificado", "Host ativo", "Serviço mDNS", "Dispositivo SSDP", "Host NetBIOS")
        val nome = when {
            existente.nomeExibicao in genericos -> dispositivo.nomeExibicao
            dispositivo.nomeExibicao !in genericos && prioNova > prioExistente -> dispositivo.nomeExibicao
            else -> existente.nomeExibicao
        }
        val fonte = if (prioNova > prioExistente) dispositivo.fonteNome else existente.fonteNome
        mapa[chave] =
            existente.copy(
                ip = existente.ip ?: dispositivo.ip,
                mac = existente.mac ?: dispositivo.mac,
                nomeExibicao = nome,
                fonteNome = fonte,
                tiposServicoMdns = existente.tiposServicoMdns + dispositivo.tiposServicoMdns,
                portasAbertas = existente.portasAbertas + dispositivo.portasAbertas,
            )
    }

    @SuppressLint("MissingPermission")
    private fun detectarGatewayIp(): String? {
        try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val network: Network = connectivityManager.activeNetwork ?: error("sem rede ativa")
            val lp: LinkProperties = connectivityManager.getLinkProperties(network) ?: error("sem link properties")
            val ip = lp.routes.firstOrNull { it.isDefaultRoute }?.gateway?.hostAddress
            if (!ip.isNullOrBlank()) return ip
        } catch (_: Throwable) {}

        // Fallback: WifiManager.dhcpInfo (funciona mesmo sem permissão de localização fina)
        try {
            val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val dhcp = wm.dhcpInfo
            val gw = dhcp.gateway
            if (gw != 0) {
                return String.format(
                    Locale.ROOT,
                    "%d.%d.%d.%d",
                    gw and 0xff,
                    (gw shr 8) and 0xff,
                    (gw shr 16) and 0xff,
                    (gw shr 24) and 0xff,
                )
            }
        } catch (_: Throwable) {}

        return null
    }

    private fun coletarViaArp(): List<DispositivoRede> {
        // /proc/net/arp é restrito a apps em Android 10+ (target SDK 29+).
        // O try-catch impede que a IOException aborte o scan inteiro.
        try {
            val arquivo = java.io.File("/proc/net/arp")
            if (!arquivo.exists()) return emptyList()
            val linhas = arquivo.readLines()
            if (linhas.size <= 1) return emptyList()
            return linhas
                .drop(1)
                .mapNotNull { linha ->
                    val p = linha.trim().split(Regex("\\s+"))
                    if (p.size < 6) return@mapNotNull null
                    val ip = p[0]
                    val mac = p[3]
                    if (!validarIpv4(ip) || !validarMac(mac)) return@mapNotNull null
                    DispositivoRede(
                        id = "arp:$mac",
                        ip = ip,
                        mac = mac,
                        nomeExibicao = "Dispositivo não identificado",
                        fonteNome = "arp",
                    )
                }
        } catch (_: Throwable) {
            Log.d("LinkaDevices", "arp: acesso negado a /proc/net/arp (Android 10+)")
            return emptyList()
        }
    }

    private suspend fun coletarViaTcpProbe(gatewayIp: String?): List<DispositivoRede> = coroutineScope {
        val base = inferirPrefixoRede(gatewayIp) ?: return@coroutineScope emptyList()
        val portas = intArrayOf(80, 443, 22, 53, 139, 445, 8080, 8443)
        val alvos = (1..254).map { host -> "$base.$host" }.filter { it != gatewayIp }

        val tarefas =
            alvos.map { ip ->
                async {
                    val portasAbertas = portas.filter { porta -> testarPortaAberta(ip, porta, 500) }.toSet()
                    if (portasAbertas.isEmpty()) return@async null
                    DispositivoRede(
                        id = "tcp:$ip",
                        ip = ip,
                        mac = null,
                        nomeExibicao = "Host ativo",
                        fonteNome = "tcpProbe",
                        portasAbertas = portasAbertas,
                    )
                }
            }

        tarefas.awaitAll().filterNotNull()
    }

    // Fase 1: queries para tipos comuns (900ms) + coleta de tipos anunciados via _services.
    // Fase 2: se restarem dispositivos sem nome, consulta os tipos descobertos na fase 1 (600ms).
    // Dispositivos como Spotify Connect e HomeKit só respondem com nome quando interrogados
    // diretamente pelo tipo de serviço que suportam.
    private fun coletarViaMdns(): List<DispositivoRede> {
        val resultados = mutableMapOf<String, DispositivoRede>()
        val tiposDescobertos = mutableSetOf<String>()
        val tiposPorIp = mutableMapOf<String, MutableSet<String>>()

        val wm = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
        val multicastLock = wm.createMulticastLock("linka_mdns")
        multicastLock.setReferenceCounted(false)
        multicastLock.acquire()

        val grupo = InetAddress.getByName("224.0.0.251")
        val grupoAddr = InetSocketAddress(grupo, 5353)
        val socket = MulticastSocket(0)
        try {
            socket.timeToLive = 1
            socket.soTimeout = 300
            socket.broadcast = false
            socket.joinGroup(grupoAddr, obterInterfaceWifi())

            val consulta1 = construirConsultaMdnsMulti()
            socket.send(DatagramPacket(consulta1, consulta1.size, grupo, 5353))

            val inicio1 = System.currentTimeMillis()
            while (System.currentTimeMillis() - inicio1 < 900) {
                try {
                    val buf = ByteArray(1500)
                    val resp = DatagramPacket(buf, buf.size)
                    socket.receive(resp)
                    val ip = resp.address?.hostAddress ?: continue
                    if (!validarIpv4(ip)) continue
                    val tiposDoIp = extrairTiposServicoMdns(buf, resp.length)
                    tiposDescobertos.addAll(tiposDoIp)
                    tiposPorIp.getOrPut(ip) { mutableSetOf() }.addAll(tiposDoIp)
                    val nome = extrairNomeMdnsDoPayload(buf, resp.length)
                    if (nome != null) {
                        resultados[ip] = DispositivoRede(id = "mdns:$ip", ip = ip, mac = null, nomeExibicao = nome, fonteNome = "mdns", tiposServicoMdns = tiposPorIp[ip] ?: emptySet())
                    } else if (!resultados.containsKey(ip)) {
                        resultados[ip] = DispositivoRede(id = "mdns:$ip", ip = ip, mac = null, nomeExibicao = "Serviço mDNS", fonteNome = "mdns", tiposServicoMdns = tiposPorIp[ip] ?: emptySet())
                    }
                } catch (_: java.net.SocketTimeoutException) {}
            }

            // Fase 2: consultar tipos descobertos para obter nomes de instância
            val tiposParaConsultar = tiposDescobertos
                .filter { !it.startsWith("_services") }
                .take(8)
            val temSemNome = resultados.values.any { it.nomeExibicao == "Serviço mDNS" }
            if (temSemNome && tiposParaConsultar.isNotEmpty()) {
                val consulta2 = construirConsultaMdnsParaTipos(tiposParaConsultar)
                socket.send(DatagramPacket(consulta2, consulta2.size, grupo, 5353))

                val inicio2 = System.currentTimeMillis()
                while (System.currentTimeMillis() - inicio2 < 1200) {
                    try {
                        val buf = ByteArray(1500)
                        val resp = DatagramPacket(buf, buf.size)
                        socket.receive(resp)
                        val ip = resp.address?.hostAddress ?: continue
                        if (!validarIpv4(ip)) continue
                        val nome = extrairNomeMdnsDoPayload(buf, resp.length) ?: continue
                        val existente = resultados[ip]
                        if (existente == null || existente.nomeExibicao == "Serviço mDNS") {
                            resultados[ip] = DispositivoRede(id = "mdns:$ip", ip = ip, mac = null, nomeExibicao = nome, fonteNome = "mdns", tiposServicoMdns = (existente?.tiposServicoMdns ?: emptySet()) + (tiposPorIp[ip] ?: emptySet()))
                        }
                    } catch (_: java.net.SocketTimeoutException) {}
                }
            }
        } finally {
            try { socket.leaveGroup(grupoAddr, null) } catch (_: Throwable) {}
            socket.close()
            if (multicastLock.isHeld) multicastLock.release()
        }
        return resultados.values.toList()
    }

    // Prefere interface wlan* para evitar que usb0/rmnet0 seja selecionada quando
    // o celular está conectado via USB tethering.
    private fun obterInterfaceWifi(): NetworkInterface? =
        try {
            val candidatos = NetworkInterface.getNetworkInterfaces()?.toList()
                ?.filter { iface ->
                    iface.isUp && !iface.isLoopback && iface.supportsMulticast() &&
                        iface.inetAddresses.toList().any { it is Inet4Address && !it.isLoopbackAddress }
                } ?: emptyList()
            candidatos.firstOrNull { it.name.startsWith("wlan") } ?: candidatos.firstOrNull()
        } catch (_: Throwable) {
            null
        }

    private fun coletarViaSsdp(): List<DispositivoRede> {
        // mapa IP -> (device, qualidade): 2=FRIENDLYNAME, 1=SERVER, 0=genérico
        val resultados = mutableMapOf<String, Pair<DispositivoRede, Int>>()
        val payload =
            (
                "M-SEARCH * HTTP/1.1\r\n" +
                    "HOST: 239.255.255.250:1900\r\n" +
                    "MAN: \"ssdp:discover\"\r\n" +
                    "MX: 1\r\n" +
                    "ST: ssdp:all\r\n\r\n"
                ).toByteArray()

        val socket = DatagramSocket()
        try {
            socket.broadcast = true
            socket.soTimeout = 250
            socket.send(
                DatagramPacket(
                    payload,
                    payload.size,
                    InetAddress.getByName("239.255.255.250"),
                    1900,
                ),
            )
            val inicio = System.currentTimeMillis()
            while (System.currentTimeMillis() - inicio < 900) {
                try {
                    val buf = ByteArray(2048)
                    val resp = DatagramPacket(buf, buf.size)
                    socket.receive(resp)
                    val ip = resp.address?.hostAddress ?: continue
                    if (!validarIpv4(ip)) continue
                    val (nome, qualidade) = extrairNomeSsdpComQualidade(buf, resp.length)
                    val existente = resultados[ip]
                    if (existente == null || qualidade > existente.second) {
                        resultados[ip] = Pair(
                            DispositivoRede(id = "ssdp:$ip", ip = ip, mac = null, nomeExibicao = nome, fonteNome = "ssdp"),
                            qualidade,
                        )
                    }
                } catch (_: java.net.SocketTimeoutException) {
                    // Continua.
                }
            }
        } finally {
            socket.close()
        }
        return resultados.values.map { it.first }
    }

    private fun coletarViaNbns(gatewayIp: String?): List<DispositivoRede> {
        val alvo = inferirBroadcast(gatewayIp) ?: return emptyList()
        val transacaoId = byteArrayOf(0x13, 0x37)
        val consulta = construirConsultaNbns(transacaoId)
        val resultados = mutableListOf<DispositivoRede>()

        val socket = DatagramSocket(null)
        try {
            socket.reuseAddress = true
            socket.broadcast = true
            socket.bind(InetSocketAddress(0))
            socket.soTimeout = 250
            socket.send(DatagramPacket(consulta, consulta.size, InetAddress.getByName(alvo), 137))

            val inicio = System.currentTimeMillis()
            while (System.currentTimeMillis() - inicio < 900) {
                try {
                    val buf = ByteArray(2048)
                    val resp = DatagramPacket(buf, buf.size)
                    socket.receive(resp)
                    val ip = resp.address?.hostAddress ?: continue
                    if (!validarIpv4(ip)) continue
                    val nome = extrairNomeNbns(buf, resp.length) ?: "Host NetBIOS"
                    resultados.add(
                        DispositivoRede(
                            id = "nbns:$ip",
                            ip = ip,
                            mac = null,
                            nomeExibicao = nome,
                            fonteNome = "nbns",
                        ),
                    )
                } catch (_: java.net.SocketTimeoutException) {
                    // Continua.
                }
            }
        } finally {
            socket.close()
        }
        return resultados.distinctBy { it.ip }
    }

    // Constrói uma query mDNS com múltiplas perguntas PTR para tipos de serviço comuns.
    // Respostas PTR têm RDATA no formato "NomeInstancia._tipo._tcp.local" — o primeiro
    // label é o nome amigável do dispositivo (ex: "Minha TV", "Notebook do João").
    private fun construirConsultaMdnsMulti(): ByteArray {
        val tiposServico = listOf(
            listOf("_http", "_tcp", "local"),
            listOf("_https", "_tcp", "local"),
            listOf("_airplay", "_tcp", "local"),
            listOf("_googlecast", "_tcp", "local"),
            listOf("_ipp", "_tcp", "local"),
            listOf("_smb", "_tcp", "local"),
            listOf("_afpovertcp", "_tcp", "local"),
            listOf("_ssh", "_tcp", "local"),
            listOf("_raop", "_tcp", "local"),
            listOf("_companion-link", "_tcp", "local"),
            listOf("_services", "_dns-sd", "_udp", "local"),
        )
        return buildList<Byte> {
            add(0x00); add(0x00) // Transaction ID
            add(0x00); add(0x00) // Flags (standard query)
            add(0x00); add(tiposServico.size.toByte()) // Questions
            add(0x00); add(0x00) // Answers
            add(0x00); add(0x00) // Authority
            add(0x00); add(0x00) // Additional
            for (labels in tiposServico) {
                for (label in labels) {
                    add(label.length.toByte())
                    label.forEach { add(it.code.toByte()) }
                }
                add(0x00)            // terminador de nome
                add(0x00); add(0x0C) // Type PTR
                add(0x00); add(0x01) // Class IN
            }
        }.toByteArray()
    }

    private fun construirConsultaNbns(transactionId: ByteArray): ByteArray {
        val nome = ByteArray(32) { 'A'.code.toByte() } // * (wildcard) codificado simplificado
        return buildList {
            add(transactionId[0]); add(transactionId[1])
            add(0x00); add(0x00) // Flags
            add(0x00); add(0x01) // Questions
            add(0x00); add(0x00) // Answer RRs
            add(0x00); add(0x00) // Authority RRs
            add(0x00); add(0x00) // Additional RRs
            add(0x20) // NetBIOS name length (32)
            nome.forEach { add(it) }
            add(0x00) // Null terminator
            add(0x00); add(0x21) // NBSTAT
            add(0x00); add(0x01) // IN
        }.toByteArray()
    }

    private fun inferirBroadcast(gatewayIp: String?): String? {
        if (gatewayIp.isNullOrBlank()) return null
        if (!validarIpv4(gatewayIp)) return null
        val partes = gatewayIp.split(".")
        if (partes.size != 4) return null
        return "${partes[0]}.${partes[1]}.${partes[2]}.255"
    }

    private fun inferirPrefixoRede(gatewayIp: String?): String? {
        if (gatewayIp.isNullOrBlank()) return null
        if (!validarIpv4(gatewayIp)) return null
        val partes = gatewayIp.split(".")
        if (partes.size != 4) return null
        return "${partes[0]}.${partes[1]}.${partes[2]}"
    }

    private fun testarPortaAberta(ip: String, porta: Int, timeoutMs: Int): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, porta), timeoutMs)
                true
            }
        } catch (_: Throwable) {
            false
        }
    }

    private fun validarIpv4(ip: String): Boolean {
        val partes = ip.split(".")
        if (partes.size != 4) return false
        return partes.all { p -> p.toIntOrNull()?.let { it in 0..255 } == true }
    }

    private fun validarMac(mac: String): Boolean {
        return Regex("^[0-9a-fA-F]{2}(:[0-9a-fA-F]{2}){5}$").matches(mac)
    }

    private fun detectarIpLocal(): String? =
        try {
            NetworkInterface.getNetworkInterfaces()?.toList()
                ?.filter { it.isUp && !it.isLoopback }
                ?.flatMap { it.inetAddresses.toList() }
                ?.firstOrNull { it is Inet4Address && !it.isLoopbackAddress }
                ?.hostAddress
        } catch (_: Throwable) { null }

    private suspend fun varrerSubrede(subnet: String): Unit = coroutineScope {
        (1..254).map { host ->
            async(Dispatchers.IO) {
                try { InetAddress.getByName("$subnet.$host").isReachable(300) } catch (_: Throwable) {}
            }
        }.awaitAll()
    }

    private fun resolverHostname(ip: String): String? =
        try {
            val partes = ip.split(".").map { it.toInt().toByte() }.toByteArray()
            val addr = InetAddress.getByAddress(partes)
            val hostname = addr.canonicalHostName
            if (hostname == ip) null
            else hostname.removeSuffix(".local").removeSuffix(".").takeIf { it.isNotBlank() }
        } catch (_: Throwable) { null }

    // --- Extração de nomes dos protocolos ---

    // Retorna (nome, qualidade): 2=FRIENDLYNAME, 1=SERVER-derivado, 0=genérico
    private fun extrairNomeSsdpComQualidade(payload: ByteArray, tamanho: Int): Pair<String, Int> {
        val texto = try { String(payload, 0, tamanho, Charsets.UTF_8) } catch (_: Throwable) { return Pair("Dispositivo SSDP", 0) }
        val linhas = texto.split("\r\n", "\n")
        for (linha in linhas) {
            if (linha.lowercase().startsWith("friendlyname")) {
                val nome = linha.substringAfter(":").trim()
                if (nome.isNotBlank()) return Pair(nome, 2)
            }
        }
        for (linha in linhas) {
            if (linha.lowercase().startsWith("server:")) {
                val server = linha.substringAfter(":").trim()
                val genericos = setOf("linux", "windows", "macos", "darwin", "ios", "android", "upnp", "http")
                val tokens = server.split(" ").map { it.trimEnd(',') }.filter { it.isNotBlank() }
                val produto = tokens.lastOrNull { tok ->
                    tok.contains("/") &&
                    tok.substringBefore("/").lowercase() !in genericos
                }
                val nome = produto?.substringBefore("/")?.takeIf { it.isNotBlank() }
                if (nome != null) return Pair(nome, 1)
            }
        }
        return Pair("Dispositivo SSDP", 0)
    }

    // Parseia a resposta NBSTAT buscando o primeiro nome NetBIOS (tipo 0x00 = workstation).
    // Suporta tanto ponteiro de compressão (0xC0) quanto nome completo (34 bytes) no campo
    // de nome da seção de resposta.
    private fun extrairNomeNbns(payload: ByteArray, tamanho: Int): String? {
        return try {
            if (tamanho < 24) return null
            // Header: 12 bytes. Depois vem o nome da seção de resposta.
            var offset = 12
            offset += if ((payload[offset].toInt() and 0xFF) == 0xC0) 2 else 34
            // type(2) + class(2) + ttl(4) + rdlength(2) = 10 bytes
            if (offset + 10 >= tamanho) return null
            offset += 10
            if (offset >= tamanho) return null
            val numNomes = payload[offset].toInt() and 0xFF
            offset++
            if (numNomes == 0 || offset + 15 > tamanho) return null
            // Cada entrada: 15 bytes de nome (padded com espaços) + 1 byte tipo + 2 bytes flags
            val nomeRaw = String(payload, offset, 15, Charsets.US_ASCII).trimEnd()
            nomeRaw.takeIf { it.isNotBlank() }
        } catch (_: Throwable) { null }
    }

    // Parseia a resposta mDNS iterando todos os records para encontrar o melhor nome:
    //   TXT (fn=/name=/md=) > PTR first label > A/AAAA record hostname
    // Itera todos os records sem retorno antecipado para garantir que o TXT seja avaliado
    // mesmo que venha depois do PTR (ordem não garantida em mDNS).
    private fun extrairNomeMdnsDoPayload(payload: ByteArray, tamanho: Int): String? {
        if (tamanho < 12) return null
        val reservados = setOf("local", "tcp", "udp", "ip6", "ipv6", "arpa", "in-addr")
        return try {
            val numPerguntas  = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
            val numRespostas  = ((payload[6].toInt() and 0xFF) shl 8) or (payload[7].toInt() and 0xFF)
            val numAuthority  = ((payload[8].toInt() and 0xFF) shl 8) or (payload[9].toInt() and 0xFF)
            val numAdicionais = ((payload[10].toInt() and 0xFF) shl 8) or (payload[11].toInt() and 0xFF)

            var pos = 12
            repeat(numPerguntas) {
                val novo = pularNomeDns(payload, pos)
                pos = if (novo >= 0 && novo + 4 <= tamanho) novo + 4 else tamanho
            }

            var nomeInstancia: String? = null // de PTR ou A/AAAA
            var nomeTxt: String? = null        // de TXT — preferido

            fun candidatoValido(nome: String?) =
                !nome.isNullOrBlank() && !nome.startsWith("_") &&
                    nome.lowercase() !in reservados && nome.any { it.isLetterOrDigit() }

            repeat(numRespostas + numAuthority + numAdicionais) {
                if (pos >= tamanho) return@repeat
                val rNomeOffset = pos
                val aposNome = pularNomeDns(payload, pos)
                if (aposNome < 0 || aposNome + 10 > tamanho) { pos = tamanho; return@repeat }
                val tipo  = ((payload[aposNome].toInt() and 0xFF) shl 8) or (payload[aposNome + 1].toInt() and 0xFF)
                val rdlen = ((payload[aposNome + 8].toInt() and 0xFF) shl 8) or (payload[aposNome + 9].toInt() and 0xFF)
                val rdataOffset = aposNome + 10
                pos = rdataOffset + rdlen

                when (tipo) {
                    0x000C -> if (nomeInstancia == null) {
                        val nome = lerPrimeiroLabelDns(payload, rdataOffset)
                        if (candidatoValido(nome)) nomeInstancia = nome
                    }
                    0x0001, 0x001C -> if (nomeInstancia == null) {
                        val nome = lerPrimeiroLabelDns(payload, rNomeOffset)
                        if (candidatoValido(nome)) nomeInstancia = nome
                    }
                    0x0010 -> if (nomeTxt == null) {
                        nomeTxt = extrairNomeTxtMdns(payload, rdataOffset, rdlen)
                    }
                }
            }
            nomeTxt ?: nomeInstancia
        } catch (_: Throwable) { null }
    }

    // Parseia o RDATA de um TXT record buscando chaves de nome amigável.
    // Formato TXT: sequência de [len][string], onde string = "chave=valor".
    // Ordem de preferência: fn > name > n > md (model).
    private fun extrairNomeTxtMdns(payload: ByteArray, rdataOffset: Int, rdlen: Int): String? {
        val prioridade = listOf("fn", "name", "n", "md", "model", "integrator", "manufacturer")
        val encontrados = mutableMapOf<String, String>()
        var pos = rdataOffset
        val fim = (rdataOffset + rdlen).coerceAtMost(payload.size)
        try {
            while (pos < fim) {
                val len = payload[pos].toInt() and 0xFF
                pos++
                if (pos + len > fim) break
                val entrada = String(payload, pos, len, Charsets.UTF_8)
                pos += len
                val sep = entrada.indexOf('=')
                if (sep < 1) continue
                val chave = entrada.substring(0, sep).lowercase().trim()
                val valor = entrada.substring(sep + 1).trim()
                if (valor.isNotBlank()) encontrados[chave] = valor
            }
        } catch (_: Throwable) {}
        return prioridade.firstNotNullOfOrNull { encontrados[it] }
    }

    // Extrai os tipos de serviço anunciados via PTR em respostas a _services._dns-sd._udp.local.
    // Retorna strings como "_spotify-connect._tcp.local" para uso na Fase 2.
    private fun extrairTiposServicoMdns(payload: ByteArray, tamanho: Int): List<String> {
        val tipos = mutableListOf<String>()
        if (tamanho < 12) return tipos
        try {
            val numPerguntas  = ((payload[4].toInt() and 0xFF) shl 8) or (payload[5].toInt() and 0xFF)
            val numRespostas  = ((payload[6].toInt() and 0xFF) shl 8) or (payload[7].toInt() and 0xFF)
            val numAuthority  = ((payload[8].toInt() and 0xFF) shl 8) or (payload[9].toInt() and 0xFF)
            val numAdicionais = ((payload[10].toInt() and 0xFF) shl 8) or (payload[11].toInt() and 0xFF)
            var pos = 12
            repeat(numPerguntas) {
                pos = pularNomeDns(payload, pos).also { if (it < 0) return@repeat } + 4
            }
            repeat(numRespostas + numAuthority + numAdicionais) {
                val aposNome = pularNomeDns(payload, pos).also { if (it < 0) return@repeat }
                if (aposNome + 10 > tamanho) return@repeat
                val tipo  = ((payload[aposNome].toInt() and 0xFF) shl 8) or (payload[aposNome + 1].toInt() and 0xFF)
                val rdlen = ((payload[aposNome + 8].toInt() and 0xFF) shl 8) or (payload[aposNome + 9].toInt() and 0xFF)
                val rdataOffset = aposNome + 10
                pos = rdataOffset + rdlen
                if (tipo == 0x000C) {
                    val nomeCompleto = lerNomeDnsCompleto(payload, rdataOffset)
                    if (nomeCompleto != null && nomeCompleto.startsWith("_")) tipos.add(nomeCompleto)
                }
            }
        } catch (_: Throwable) {}
        return tipos
    }

    // Lê o nome DNS completo como string pontuada (ex: "_airplay._tcp.local"),
    // seguindo ponteiros de compressão.
    private fun lerNomeDnsCompleto(payload: ByteArray, offset: Int): String? {
        val labels = mutableListOf<String>()
        var pos = offset
        val visitados = mutableSetOf<Int>()
        while (pos < payload.size) {
            if (!visitados.add(pos)) return null
            val len = payload[pos].toInt() and 0xFF
            when {
                (len and 0xC0) == 0xC0 -> {
                    if (pos + 1 >= payload.size) return null
                    pos = ((len and 0x3F) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                }
                len == 0 -> break
                len in 1..63 -> {
                    if (pos + 1 + len > payload.size) return null
                    labels.add(String(payload, pos + 1, len, Charsets.UTF_8))
                    pos += len + 1
                }
                else -> return null
            }
        }
        return if (labels.isEmpty()) null else labels.joinToString(".")
    }

    // Constrói uma query PTR para uma lista de tipos de serviço já descobertos
    // (ex: ["_spotify-connect._tcp.local", "_homekit._tcp.local"]).
    private fun construirConsultaMdnsParaTipos(tipos: List<String>): ByteArray {
        return buildList<Byte> {
            add(0x00); add(0x00)
            add(0x00); add(0x00)
            add(0x00); add(tipos.size.toByte())
            add(0x00); add(0x00)
            add(0x00); add(0x00)
            add(0x00); add(0x00)
            for (tipo in tipos) {
                for (label in tipo.split(".")) {
                    add(label.length.toByte())
                    label.forEach { add(it.code.toByte()) }
                }
                add(0x00)
                add(0x00); add(0x0C)
                add(0x00); add(0x01)
            }
        }.toByteArray()
    }

    // Avança o offset além de um nome DNS comprimido ou plano.
    // Retorna o offset do byte imediatamente após o nome, ou -1 em erro.
    private fun pularNomeDns(payload: ByteArray, offset: Int): Int {
        var pos = offset
        while (pos < payload.size) {
            val len = payload[pos].toInt() and 0xFF
            when {
                (len and 0xC0) == 0xC0 -> return pos + 2 // ponteiro de compressão: 2 bytes
                len == 0               -> return pos + 1 // terminador nulo
                len in 1..63           -> pos += len + 1
                else                   -> return -1
            }
        }
        return -1
    }

    // Lê o primeiro label de um nome DNS, seguindo ponteiros de compressão se necessário.
    private fun lerPrimeiroLabelDns(payload: ByteArray, offset: Int): String? {
        var pos = offset
        val visitados = mutableSetOf<Int>()
        while (pos < payload.size) {
            if (!visitados.add(pos)) return null // loop de ponteiro
            val len = payload[pos].toInt() and 0xFF
            when {
                (len and 0xC0) == 0xC0 -> {
                    if (pos + 1 >= payload.size) return null
                    pos = ((len and 0x3F) shl 8) or (payload[pos + 1].toInt() and 0xFF)
                }
                len == 0 -> return null // nome vazio
                len in 1..63 -> {
                    if (pos + 1 + len > payload.size) return null
                    return String(payload, pos + 1, len, Charsets.UTF_8)
                }
                else -> return null
            }
        }
        return null
    }
}
