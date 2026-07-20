package io.signallq.app.feature.diagnostico.topology.lan

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.SocketTimeoutException
import kotlin.random.Random

data class StunServerConfig(val host: String, val porta: Int)

enum class NatUdpTipo { ABERTO, MODERADO, RESTRITO, BLOQUEADO, NAO_VERIFICADO }

data class NatUdpResultado(val tipo: NatUdpTipo)

private val SERVIDORES_STUN_PADRAO =
    listOf(
        StunServerConfig("stun.l.google.com", 19302),
        StunServerConfig("stun.cloudflare.com", 3478),
    )

/**
 * Sonda simplificada de NAT Type via STUN (RFC 5389), pra aba Jogos (#1200).
 *
 * ## Escopo — versão SIMPLIFICADA, não RFC 5780 completo
 * Esta classificação distingue apenas 5 estados (ABERTO/MODERADO/RESTRITO/BLOQUEADO/
 * NAO_VERIFICADO) comparando a porta mapeada vista por dois servidores STUN distintos
 * contra a porta local de origem. Não implementa RFC 5780 (NAT Behavior Discovery), que
 * exigiria o atributo CHANGE-REQUEST para distinguir Full Cone de Restricted/Port-
 * Restricted Cone dentro do bucket "MODERADO" — a maioria dos servidores STUN públicos
 * modernos (inclusive os dois usados aqui) não responde a CHANGE-REQUEST de forma
 * confiável, então essa distinção fina não vale a complexidade adicional aqui. Rota B
 * (infra própria/Cloudflare Spectrum, com RFC 5780 completo) fica registrada como plano
 * B na issue caso a rota A (STUN público) se mostre não confiável no uso real.
 *
 * ## Falha nunca propaga
 * Qualquer exceção (DNS não resolveu, `SecurityException`, socket não abriu, timeout)
 * resulta em [NatUdpTipo.NAO_VERIFICADO] ou [NatUdpTipo.BLOQUEADO] — nunca lança, mesmo
 * padrão defensivo do resto do módulo (`UpnpIgdDiscovery`, `GatewayResolver`).
 */
class StunNatProbe(
    private val servidores: List<StunServerConfig> = SERVIDORES_STUN_PADRAO,
    private val timeoutMsPorTentativa: Long = 2500L,
) {
    suspend fun sondar(): NatUdpResultado =
        withContext(Dispatchers.IO) {
            try {
                DatagramSocket().use { socket ->
                    socket.soTimeout = timeoutMsPorTentativa.toInt()
                    val portaLocal = socket.localPort
                    val resultados = consultarServidores(socket)
                    NatUdpResultado(classificar(portaLocal, resultados.getOrNull(0), resultados.getOrNull(1)))
                }
            } catch (_: Exception) {
                NatUdpResultado(NatUdpTipo.NAO_VERIFICADO)
            }
        }

    /**
     * Envia o Binding Request pros dois servidores a partir da MESMA porta local (mesmo
     * socket) e coleta as respostas numa única thread de leitura, casando cada pacote
     * recebido pelo transaction ID — não pelo servidor que originou o `send`, já que UDP
     * não garante ordem nem exclusividade de leitura por remetente. Duas chamadas
     * concorrentes de `receive()` no mesmo [DatagramSocket] são uma corrida real (o
     * pacote de um servidor pode ser entregue à leitura que esperava o outro); por isso
     * o envio é disparado pros dois de uma vez ("em paralelo" no sentido de estarem os
     * dois em trânsito simultaneamente), mas a leitura é sequencial e casada por ID.
     * 1 retry por servidor pendente em caso de [SocketTimeoutException] — perda de um
     * datagrama UDP isolado não significa que a rede está bloqueando o tráfego.
     */
    private fun consultarServidores(socket: DatagramSocket): List<StunBindingResult?> {
        data class Pendencia(val servidor: StunServerConfig, val transactionId: ByteArray)

        val pendencias = servidores.map { Pendencia(it, ByteArray(12).also(Random::nextBytes)) }
        val porTransactionId = pendencias.associateBy { it.transactionId.paraChave() }
        val resultados = mutableMapOf<StunServerConfig, StunBindingResult?>()
        val restantes = pendencias.toMutableList()

        fun enviar(lista: List<Pendencia>) {
            lista.forEach { pendencia ->
                try {
                    val request = StunMessageCodec.buildBindingRequest(pendencia.transactionId)
                    val endereco = InetAddress.getByName(pendencia.servidor.host)
                    socket.send(DatagramPacket(request, request.size, endereco, pendencia.servidor.porta))
                } catch (_: Exception) {
                    resultados[pendencia.servidor] = null
                    restantes.remove(pendencia)
                }
            }
        }

        enviar(restantes)
        var retriesDisponiveis = pendencias.size
        val buffer = ByteArray(512)

        while (restantes.isNotEmpty()) {
            val pacote = DatagramPacket(buffer, buffer.size)
            try {
                socket.receive(pacote)
            } catch (_: SocketTimeoutException) {
                if (retriesDisponiveis <= 0) break
                val paraReenviar = restantes.toList()
                retriesDisponiveis -= paraReenviar.size
                enviar(paraReenviar)
                continue
            } catch (_: Exception) {
                break
            }

            if (pacote.length < 20) continue
            val chave = pacote.data.copyOfRange(8, 20).paraChave()
            val pendencia = porTransactionId[chave] ?: continue
            if (pendencia !in restantes) continue // resposta duplicada/tardia de um retry já resolvido

            resultados[pendencia.servidor] =
                StunMessageCodec.parseBindingResponse(pacote.data, pacote.length, pendencia.transactionId)
            restantes.remove(pendencia)
        }

        restantes.forEach { resultados.putIfAbsent(it.servidor, null) }
        return pendencias.map { resultados[it.servidor] }
    }

    private fun ByteArray.paraChave(): String = joinToString(",")
}

/**
 * Classificação pura (sem I/O) do NAT Type, extraída pra ser testável sem rede real.
 *
 * `portaLocal` é a porta de origem do socket UDP usado nas duas tentativas; `r1`/`r2`
 * são os resultados dos bindings vistos por cada um dos dois servidores STUN.
 */
fun classificar(
    portaLocal: Int,
    r1: StunBindingResult?,
    r2: StunBindingResult?,
): NatUdpTipo =
    when {
        r1 == null && r2 == null -> NatUdpTipo.BLOQUEADO
        r1 == null || r2 == null -> NatUdpTipo.NAO_VERIFICADO
        r1.portaMapeada == portaLocal && r2.portaMapeada == portaLocal -> NatUdpTipo.ABERTO
        r1.portaMapeada == r2.portaMapeada -> NatUdpTipo.MODERADO
        else -> NatUdpTipo.RESTRITO
    }
