package io.veloo.app.feature.diagnostico.topology.correlation

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object TopologyTracer {

    // Best-effort: retorna lista de IPs dos primeiros hops ou null se exec bloqueado
    suspend fun trace(maxHops: Int = 5): List<String>? = withContext(Dispatchers.IO) {
        try {
            val hops = mutableListOf<String>()
            for (ttl in 1..maxHops) {
                val ip = pingWithTtl(ttl) ?: break
                hops.add(ip)
            }
            hops.takeIf { it.isNotEmpty() }
        } catch (_: Exception) { null }
    }

    private fun pingWithTtl(ttl: Int): String? = try {
        val proc = Runtime.getRuntime().exec(
            arrayOf("/system/bin/ping", "-c", "1", "-t", ttl.toString(), "-W", "1", "8.8.8.8")
        )
        val output = proc.inputStream.bufferedReader().readText()
        proc.waitFor()
        // Parse "From <ip>: ..." para hop que rejeitou o TTL, ou "64 bytes from <ip>" para destino
        Regex("""(?:From|from) ([\d.]+)""").find(output)?.groupValues?.get(1)
    } catch (_: Exception) { null }
}
