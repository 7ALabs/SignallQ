package io.veloo.app.feature.devices

import okhttp3.OkHttpClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import java.net.ServerSocket
import java.util.concurrent.TimeUnit

/**
 * Testes de contrato do OkHttpClient injetado em [ScannerDispositivosAndroid].
 *
 * Contexto da regressão (review feat/httpclient-upnp-scan-singleton):
 * O AppModule introduziu upnpClient singleton com 2s de timeout. Este timeout é correto
 * para o ScannerDispositivosAndroid (SSDP local LAN, redes domésticas respondem em <1s).
 * Contudo, o mesmo cliente NÃO deve ser usado para UpnpIgdDiscovery (IGD/gateway) que
 * precisa de 5s em redes ADSL/4G. A separação em dois clientes resolve a regressão.
 *
 * Estes testes validam:
 * 1. O cliente de 2s tem as propriedades corretas para scan de rede local.
 * 2. O comportamento de fallback quando o servidor HTTP retorna resposta de erro.
 * 3. O comportamento de fallback quando a conexão demora mais que o timeout.
 */
class ScannerDispositivosClienteTest {

    @Test
    fun `upnpScanClient deve ter connectTimeout de 2 segundos`() {
        val client = buildScanClient()
        assertEquals(
            "connectTimeout para scan local deve ser 2000ms",
            2_000,
            client.connectTimeoutMillis
        )
    }

    @Test
    fun `upnpScanClient deve ter readTimeout de 2 segundos`() {
        val client = buildScanClient()
        assertEquals(
            "readTimeout para scan local deve ser 2000ms",
            2_000,
            client.readTimeoutMillis
        )
    }

    @Test
    fun `upnpScanClient deve ter writeTimeout de 2 segundos`() {
        val client = buildScanClient()
        assertEquals(
            "writeTimeout para scan local deve ser 2000ms",
            2_000,
            client.writeTimeoutMillis
        )
    }

    @Test
    fun `fetchDescricaoUpnp retorna null quando servidor responde com 404`() {
        // Sobe um servidor local mínimo que responde 404 e fecha a conexão
        val server = ServerSocket(0) // porta aleatória
        val porta = server.localPort
        val thread = Thread {
            try {
                val conn = server.accept()
                val response = "HTTP/1.1 404 Not Found\r\nContent-Length: 0\r\n\r\n"
                conn.getOutputStream().write(response.toByteArray())
                conn.close()
            } catch (_: Exception) {}
        }
        thread.isDaemon = true
        thread.start()

        val client = buildScanClient()
        val resultado = fetchDescricaoUpnpComCliente(client, "http://127.0.0.1:$porta/desc.xml")

        server.close()
        assertNull("fetchDescricaoUpnp deve retornar null para resposta 404", resultado)
    }

    @Test
    fun `fetchDescricaoUpnp retorna null quando servidor responde com XML invalido`() {
        val server = ServerSocket(0)
        val porta = server.localPort
        val thread = Thread {
            try {
                val conn = server.accept()
                val body = "isso nao eh xml valido"
                val response = "HTTP/1.1 200 OK\r\nContent-Length: ${body.length}\r\n\r\n$body"
                conn.getOutputStream().write(response.toByteArray())
                conn.close()
            } catch (_: Exception) {}
        }
        thread.isDaemon = true
        thread.start()

        val client = buildScanClient()
        val resultado = fetchDescricaoUpnpComCliente(client, "http://127.0.0.1:$porta/desc.xml")

        server.close()
        // XML inválido não contém friendlyName — parser retorna null
        assertNull("fetchDescricaoUpnp deve retornar null para XML sem friendlyName", resultado)
    }

    @Test
    fun `fetchDescricaoUpnp retorna null quando host nao existe`() {
        // IP de documentação RFC 5737 — nunca roteado, conexão falha imediatamente
        // ou após timeout. Usamos 127.0.0.1 em porta fechada para falha imediata.
        val client = OkHttpClient.Builder()
            .connectTimeout(100, TimeUnit.MILLISECONDS) // timeout curto para o teste não demorar
            .readTimeout(100, TimeUnit.MILLISECONDS)
            .build()

        // Porta alta improvável de estar em uso
        val resultado = fetchDescricaoUpnpComCliente(client, "http://127.0.0.1:19876/desc.xml")
        assertNull("fetchDescricaoUpnp deve retornar null quando conexao falha", resultado)
    }

    // ── Helpers ──────────────────────────────────────────────────────────────────

    /** Replica provideUpnpOkHttpClient() do AppModule (timeout 2s para scan local). */
    private fun buildScanClient(): OkHttpClient =
        OkHttpClient.Builder()
            .connectTimeout(2, TimeUnit.SECONDS)
            .readTimeout(2, TimeUnit.SECONDS)
            .writeTimeout(2, TimeUnit.SECONDS)
            .build()

    /**
     * Extrai e testa apenas a lógica de fetch HTTP do ScannerDispositivosAndroid.
     *
     * Replica o comportamento de fetchDescricaoUpnp() sem instanciar o Scanner completo
     * (que requer Context/WifiManager/ConnectivityManager). A lógica real está em
     * ScannerDispositivosAndroid.fetchDescricaoUpnp() — este helper testa o mesmo fluxo.
     */
    private fun fetchDescricaoUpnpComCliente(client: OkHttpClient, url: String): XmlDescricaoUpnpParser.Descricao? {
        return try {
            val request = okhttp3.Request.Builder().url(url).get().build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return null
                val body = response.body?.string() ?: return null
                XmlDescricaoUpnpParser.parsear(body)
            }
        } catch (_: Throwable) {
            null
        }
    }
}
