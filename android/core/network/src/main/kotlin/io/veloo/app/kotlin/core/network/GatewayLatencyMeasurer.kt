package io.signallq.app.core.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

/**
 * Mede a latência entre o dispositivo e o gateway local via TCP connect.
 *
 * Usa TCP connect (sem ICMP/ping) nas portas 80, 443 e 53 — funciona em
 * dispositivos Android sem permissão de raw socket e sem root.
 *
 * Validação Otávio: TCP connect em portas fechadas pode levar até 1s de
 * timeout por porta. Isso é aceitável no fluxo de diagnóstico.
 *
 * Nota: Em emulador, o gateway normalmente não aceita TCP — retorna null.
 * Testar em device físico antes de release.
 *
 * Persistência: o RTT do gateway é medido apenas no momento do diagnóstico.
 * Não é persistido em histórico — é dado contextual, não série temporal.
 * Se no futuro for necessário histórico de RTT, adicionar coluna rttGatewayMs
 * em MedicaoEntity e persistir em MonitoramentoWorker.persistirMedicaoMonitor().
 *
 * @param timeoutMs timeout por tentativa TCP em ms. Padrão 1000ms em produção;
 *   use valor menor (ex: 100ms) em testes para evitar espera desnecessária em CI.
 */
class GatewayLatencyMeasurer(private val timeoutMs: Int = TIMEOUT_MS_DEFAULT) {

    companion object {
        private val PORTAS_TENTATIVA = listOf(80, 443, 53)
        private const val TIMEOUT_MS_DEFAULT = 1000
        private const val AMOSTRAS = 3
    }

    /**
     * Mede o RTT TCP para o [gatewayIp] nas portas 80, 443 e 53 (nessa ordem).
     *
     * Para cada porta, tenta [AMOSTRAS] conexões TCP e retorna a mediana dos
     * tempos de conexão bem-sucedidos.
     *
     * @return Mediana em ms das medições bem-sucedidas, ou null se todas falharem.
     */
    suspend fun measureRttGateway(gatewayIp: String): Int? = withContext(Dispatchers.IO) {
        val amostras = mutableListOf<Long>()

        for (porta in PORTAS_TENTATIVA) {
            val amostrasPorta = medirPorta(gatewayIp, porta)
            amostras.addAll(amostrasPorta)
            // Se conseguiu pelo menos 2 amostras nessa porta, já temos material
            if (amostrasPorta.size >= 2) break
        }

        if (amostras.isEmpty()) return@withContext null

        amostras.sort()
        amostras[amostras.size / 2].toInt()
    }

    private fun medirPorta(gatewayIp: String, porta: Int): List<Long> {
        val resultados = mutableListOf<Long>()
        repeat(AMOSTRAS) {
            val ms = medirConexaoTcp(gatewayIp, porta)
            if (ms != null) resultados.add(ms)
        }
        return resultados
    }

    private fun medirConexaoTcp(gatewayIp: String, porta: Int): Long? {
        return try {
            // .use {} garante que o socket é fechado mesmo se connect() lançar exceção
            Socket().use { socket ->
                val inicio = System.currentTimeMillis()
                socket.connect(InetSocketAddress(gatewayIp, porta), timeoutMs)
                val fim = System.currentTimeMillis()
                fim - inicio
            }
        } catch (_: Exception) {
            // Porta fechada, timeout ou gateway não responde nessa porta — normal
            null
        }
    }
}
