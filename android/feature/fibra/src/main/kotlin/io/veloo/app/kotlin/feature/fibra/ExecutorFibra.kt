package io.signallq.app.feature.fibra

import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.SocketTimeoutException

class ExecutorFibra {
    private val mutableSnapshotFlow = MutableStateFlow(
        SnapshotFibra(
            estado = EstadoFibra.idle,
            gpon = null,
            wan = null,
            ppp = null,
            deviceInfo = null,
            erroMensagem = null,
        ),
    )

    val snapshotFlow: StateFlow<SnapshotFibra> = mutableSnapshotFlow.asStateFlow()

    suspend fun executar(host: String, username: String, password: String) = withContext(Dispatchers.IO) {
        mutableSnapshotFlow.value = SnapshotFibra(EstadoFibra.conectando, null, null, null, null, null)
        var ultimoErro: Throwable? = null
        repeat(3) { tentativa ->
            if (tentativa > 0) delay(1_000L * tentativa)
            try {
                val client = NokiaModemClient(host)
                client.login(username, password)

                val gponHtml = client.fetchPage("/wan_status.cgi?gpon")
                val wanHtml = client.fetchPage("/show_wan_status.cgi?ipv4")
                val pppJson = client.fetchPage("/index.cgi?getppp")
                val deviceHtml = client.fetchPage("/device_status.cgi")

                val gpon = NokiaModemParser.parseGpon(gponHtml)
                val wan = NokiaModemParser.parseWan(wanHtml)
                val ppp = NokiaModemParser.parsePpp(pppJson)
                val devInfo = NokiaModemParser.parseDeviceInfo(deviceHtml)

                Timber.i("executar[${tentativa + 1}]: gpon=${gpon?.status} rx=${gpon?.rxPowerDbm}")
                mutableSnapshotFlow.value = SnapshotFibra(
                    estado = EstadoFibra.concluido,
                    gpon = gpon,
                    wan = wan,
                    ppp = ppp,
                    deviceInfo = devInfo,
                    erroMensagem = null,
                )
                return@withContext
            } catch (t: Throwable) {
                ultimoErro = t
                Timber.w("executar[${tentativa + 1}]: falhou — ${t.message}")
            }
        }
        val t = ultimoErro ?: return@withContext
        val chave = when {
            t is ConnectException -> "erroModemInacessivel"
            t is SocketTimeoutException -> "erroTimeout"
            t.message?.contains("timed out", ignoreCase = true) == true -> "erroTimeout"
            t.message?.contains("refused", ignoreCase = true) == true -> "erroModemInacessivel"
            t.message?.contains("pubkey") == true
                || t.message?.contains("nonce") == true
                || t.message?.contains("csrf") == true -> "erroRespostaModemInvalida"
            else -> "erroComunicacaoModem"
        }
        Timber.e(t, "executar: $chave após 3 tentativas — ${t.message}")
        mutableSnapshotFlow.value = SnapshotFibra(
            estado = EstadoFibra.erro,
            gpon = null,
            wan = null,
            ppp = null,
            deviceInfo = null,
            erroMensagem = chave,
        )
    }

    fun marcarSemRede() {
        mutableSnapshotFlow.value = SnapshotFibra(
            estado = EstadoFibra.erro,
            gpon = null,
            wan = null,
            ppp = null,
            deviceInfo = null,
            erroMensagem = "semRede",
        )
    }
}
