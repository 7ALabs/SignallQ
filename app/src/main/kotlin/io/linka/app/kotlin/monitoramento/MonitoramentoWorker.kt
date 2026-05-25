package io.linka.app.kotlin.monitoramento

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import io.linka.app.kotlin.core.database.CoreDatabaseModulo
import io.linka.app.kotlin.core.database.MedicaoEntity
import io.linka.app.kotlin.core.datastore.CoreDatastoreModulo
import io.linka.app.kotlin.notificacao.LinkaNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.UUID

internal class MonitoramentoWorker(
    appContext: Context,
    params: WorkerParameters,
) : CoroutineWorker(appContext, params) {
    private val preferenciasAppRepository by lazy {
        CoreDatastoreModulo.criarPreferenciasAppRepository(appContext)
    }

    private enum class RssiMotivo { SemWifi, SemPermissao, Invalido }

    private data class RssiInfo(
        val rssi: Int?,
        val motivo: RssiMotivo?,
    )

    private data class EstadosAlertasAnteriores(
        val latencia: Boolean,
        val dns: Boolean,
        val rssi: Boolean,
        val semInternet: Boolean,
    )

    private data class ControlesNotificacao(
        val latencia: Boolean,
        val dns: Boolean,
        val rssi: Boolean,
        val semInternet: Boolean,
    )

    override suspend fun doWork(): Result {
        val latencia = medirLatenciaHttp()
        val dns = medirDnsResolveTime()
        val rssiInfo = medirRssiWifi()

        preferenciasAppRepository.definirUltimaVerificacaoMonitoramento(System.currentTimeMillis())

        Timber.d("Metricas: latencia=${latencia}ms, dns=${dns}ms, rssi=${rssiInfo.rssi} (motivo=${rssiInfo.motivo})")

        aplicarHisterese(latencia, dns, rssiInfo)
        persistirMedicaoMonitor(latencia, rssiInfo.rssi)

        return Result.success()
    }

    /**
     * Persiste uma medicao sintetica no Room para compor o grafico de uptime.
     * Fonte = "monitor" diferencia de medicoes de speedtest completo.
     * downloadMbps/uploadMbps ficam null — o monitor nao mede throughput.
     */
    private suspend fun persistirMedicaoMonitor(
        latenciaMs: Long?,
        rssiDbm: Int?,
    ) {
        try {
            val medicao =
                MedicaoEntity(
                    id = UUID.randomUUID().toString(),
                    timestampEpochMs = System.currentTimeMillis(),
                    connectionType = "monitor",
                    connectionTypeStart = null,
                    connectionTypeEnd = null,
                    contaminado = false,
                    speedtestMode = null,
                    specVersion = null,
                    downloadMbps = null,
                    uploadMbps = null,
                    latencyMs = latenciaMs?.toDouble(),
                    jitterMs = null,
                    perdaPercentual = null,
                    bufferbloatMs = null,
                    packetLossSource = null,
                    vereditoStreaming = null,
                    vereditoGamer = null,
                    vereditoVideoChamada = null,
                    gargaloPrimario = null,
                    fonte = "monitor",
                )
            withContext(Dispatchers.IO) {
                CoreDatabaseModulo.criarBanco(applicationContext).medicaoDao().salvar(medicao)
            }
            Timber.d("Medicao monitor persistida id=${medicao.id} latencia=${latenciaMs}ms")
        } catch (e: Exception) {
            Timber.w("Falha ao persistir medicao monitor: ${e.message}")
        }
    }

    /**
     * Aplica histerese nos alertas: notifica APENAS quando há transição de estado
     * (ok → alerta ou alerta → ok). Evita spam de notificação em oscilações
     * dentro dos thresholds.
     *
     * Thresholds:
     *  - Latência: entra em alerta > 400ms, sai < 300ms
     *  - DNS:      entra em alerta > 2500ms, sai < 1800ms
     *  - RSSI:     entra em alerta < -75dBm, sai > -68dBm
     *  - Sem internet: sem latência E sem DNS → alerta; qualquer um voltando → ok
     *
     * Se a métrica for null (ex: Doze Mode), mantém estado anterior (sem transição).
     */
    private suspend fun aplicarHisterese(
        latencia: Long?,
        dns: Long?,
        rssiInfo: RssiInfo,
    ) {
        // Ler estados anteriores em paralelo
        val (alertaLatenciaAnterior, alertaDnsAnterior, alertaRssiAnterior, alertaSemInternetAnterior) =
            combine(
                preferenciasAppRepository.alertaLatenciaAtivoFlow,
                preferenciasAppRepository.alertaDnsAtivoFlow,
                preferenciasAppRepository.alertaRssiAtivoFlow,
                preferenciasAppRepository.alertaSemInternetAtivoFlow,
            ) { lat, dns, rssi, sem -> EstadosAlertasAnteriores(lat, dns, rssi, sem) }.first()

        // Calcular novos estados com histerese — lógica pura em HisteresiHelper
        val alertaLatenciaNovo = HisteresiHelper.calcularAlertaLatencia(latencia, alertaLatenciaAnterior)
        val alertaDnsNovo = HisteresiHelper.calcularAlertaDns(dns, alertaDnsAnterior)
        val alertaRssiNovo = HisteresiHelper.calcularAlertaRssi(rssiInfo.rssi, alertaRssiAnterior)

        // rssiConfirmaSemWifi: true APENAS quando o motivo é explicitamente SemWifi.
        // Motivos Invalido e SemPermissao não confirmam ausência de Wi-Fi — são ambíguos.
        // rssi != null (RSSI válido) significa conectado → definitivamente não é "sem Wi-Fi".
        val rssiConfirmaSemWifi = rssiInfo.motivo == RssiMotivo.SemWifi
        val alertaSemInternetNovo =
            HisteresiHelper.calcularAlertaSemInternet(
                latencia,
                dns,
                rssiConfirmaSemWifi,
                alertaSemInternetAnterior,
            )

        // Ler controles granulares do usuário em paralelo
        val (notifLatenciaAtiva, notifDnsAtiva, notifRssiAtiva, notifSemInternetAtiva) =
            combine(
                preferenciasAppRepository.notificacaoLatenciaAtivaFlow,
                preferenciasAppRepository.notificacaoDnsAtivaFlow,
                preferenciasAppRepository.notificacaoRssiAtivaFlow,
                preferenciasAppRepository.notificacaoSemInternetAtivaFlow,
            ) { lat, dns, rssi, sem -> ControlesNotificacao(lat, dns, rssi, sem) }.first()

        // Detectar transições e notificar somente em transição ok→alerta
        if (alertaSemInternetNovo && !alertaSemInternetAnterior && notifSemInternetAtiva) {
            LinkaNotificationHelper.notificarSemInternet(applicationContext)
        } else if (!alertaSemInternetNovo) {
            // Só checa os outros alertas se não estiver em "sem internet"
            if (alertaLatenciaNovo && !alertaLatenciaAnterior && notifLatenciaAtiva) {
                LinkaNotificationHelper.notificarLatenciaAlta(applicationContext, latencia ?: 0L)
            }
            if (alertaDnsNovo && !alertaDnsAnterior && notifDnsAtiva) {
                LinkaNotificationHelper.notificarDnsLento(applicationContext, dns ?: 0L)
            }
            if (alertaRssiNovo && !alertaRssiAnterior && notifRssiAtiva) {
                LinkaNotificationHelper.notificarWifiFraco(applicationContext, rssiInfo.rssi ?: 0)
            }
        }

        // Salvar novos estados
        preferenciasAppRepository.setAlertaLatenciaAtivo(alertaLatenciaNovo)
        preferenciasAppRepository.setAlertaDnsAtivo(alertaDnsNovo)
        preferenciasAppRepository.setAlertaRssiAtivo(alertaRssiNovo)
        preferenciasAppRepository.setAlertaSemInternetAtivo(alertaSemInternetNovo)
    }

    private fun medirLatenciaHttp(): Long? {
        val amostras = mutableListOf<Long>()
        repeat(3) {
            try {
                val url = URL("https://speed.cloudflare.com/__down?bytes=0")
                val conexao = url.openConnection() as HttpURLConnection
                conexao.connectTimeout = 5000
                conexao.readTimeout = 5000
                conexao.requestMethod = "GET"
                val inicio = System.currentTimeMillis()
                conexao.connect()
                conexao.responseCode // garante que a conexao foi estabelecida
                val fim = System.currentTimeMillis()
                conexao.disconnect()
                amostras.add(fim - inicio)
            } catch (e: Exception) {
                Timber.d("Erro ao medir latencia HTTP: ${e.message}")
            }
        }
        if (amostras.isEmpty()) return null
        amostras.sort()
        return amostras[amostras.size / 2]
    }

    private fun medirDnsResolveTime(): Long? =
        try {
            val inicio = System.nanoTime()
            InetAddress.getByName("cloudflare.com")
            val fim = System.nanoTime()
            (fim - inicio) / 1_000_000
        } catch (e: Exception) {
            Timber.d("Erro ao medir DNS: ${e.message}")
            null
        }

    @Suppress("DEPRECATION")
    private fun medirRssiWifi(): RssiInfo {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val cm = applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
                val rede = cm.activeNetwork ?: return RssiInfo(null, RssiMotivo.SemWifi)
                val caps = cm.getNetworkCapabilities(rede) ?: return RssiInfo(null, RssiMotivo.SemWifi)
                if (!caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) {
                    return RssiInfo(null, RssiMotivo.SemWifi)
                }
                val wifiInfo =
                    caps.transportInfo as? WifiInfo
                        ?: return RssiInfo(null, RssiMotivo.SemPermissao)
                val rssi = wifiInfo.rssi
                if (rssi == Integer.MAX_VALUE) RssiInfo(null, RssiMotivo.Invalido) else RssiInfo(rssi, null)
            } else {
                val wm = applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
                val info = wm.connectionInfo ?: return RssiInfo(null, RssiMotivo.SemWifi)
                val rssi = info.rssi
                if (rssi == Integer.MAX_VALUE) RssiInfo(null, RssiMotivo.Invalido) else RssiInfo(rssi, null)
            }
        } catch (e: Exception) {
            Timber.d("Erro ao medir RSSI: ${e.message}")
            RssiInfo(null, RssiMotivo.Invalido)
        }
    }
}
