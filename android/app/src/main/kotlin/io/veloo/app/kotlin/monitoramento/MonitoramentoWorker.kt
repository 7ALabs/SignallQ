package io.signallq.app.monitoramento

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.signallq.app.core.database.MedicaoDao
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.datastore.PreferenciasAppRepository
import io.signallq.app.notificacao.SignallQNotificationHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import java.net.HttpURLConnection
import java.net.InetAddress
import java.net.URL
import java.util.UUID

@HiltWorker
internal class MonitoramentoWorker
    @AssistedInject
    constructor(
        @Assisted appContext: Context,
        @Assisted params: WorkerParameters,
        private val preferenciasAppRepository: PreferenciasAppRepository,
        private val medicaoDao: MedicaoDao,
    ) : CoroutineWorker(appContext, params) {
        private companion object {
            /** Timeout total por amostra HTTP (cobre connect + read + overhead de rede). */
            const val CALL_TIMEOUT_MS = 10_000L

            /** connectTimeout do HttpURLConnection. */
            const val CONNECT_TIMEOUT_MS = 5_000L

            /** readTimeout do HttpURLConnection. */
            const val READ_TIMEOUT_MS = 5_000L

            /** Timeout para resolução DNS via InetAddress. */
            const val DNS_TIMEOUT_MS = 5_000L
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
                    medicaoDao.salvar(medicao)
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
                SignallQNotificationHelper.notificarSemInternet(applicationContext)
            } else if (!alertaSemInternetNovo) {
                // Só checa os outros alertas se não estiver em "sem internet"
                if (alertaLatenciaNovo && !alertaLatenciaAnterior && notifLatenciaAtiva) {
                    SignallQNotificationHelper.notificarLatenciaAlta(applicationContext, latencia ?: 0L)
                }
                if (alertaDnsNovo && !alertaDnsAnterior && notifDnsAtiva) {
                    SignallQNotificationHelper.notificarDnsLento(applicationContext, dns ?: 0L)
                }
                if (alertaRssiNovo && !alertaRssiAnterior && notifRssiAtiva) {
                    SignallQNotificationHelper.notificarWifiFraco(applicationContext, rssiInfo.rssi ?: 0)
                }
            }

            // Salvar novos estados
            preferenciasAppRepository.setAlertaLatenciaAtivo(alertaLatenciaNovo)
            preferenciasAppRepository.setAlertaDnsAtivo(alertaDnsNovo)
            preferenciasAppRepository.setAlertaRssiAtivo(alertaRssiNovo)
            preferenciasAppRepository.setAlertaSemInternetAtivo(alertaSemInternetNovo)
        }

        /**
         * Mede latência HTTP com 3 amostras paralelas e retorna a mediana.
         *
         * Amostras rodam em paralelo via async/awaitAll para reduzir o tempo total de
         * wakelock: em vez de ate 30s sequenciais, o worker fica acordado no maximo 10s
         * (timeout da amostra mais lenta). A mediana continua valida estatisticamente.
         *
         * Cada amostra tem callTimeout total de 10s (connectTimeout + readTimeout = 5s + 5s,
         * com withTimeout cobrindo travamentos silenciosos alem do readTimeout).
         */
        private suspend fun medirLatenciaHttp(): Long? =
            coroutineScope {
                val jobs =
                    (1..3).map {
                        async(Dispatchers.IO) {
                            try {
                                withTimeout(CALL_TIMEOUT_MS) {
                                    val url = URL("https://speed.cloudflare.com/__down?bytes=0")
                                    val conexao = url.openConnection() as HttpURLConnection
                                    conexao.connectTimeout = CONNECT_TIMEOUT_MS.toInt()
                                    conexao.readTimeout = READ_TIMEOUT_MS.toInt()
                                    conexao.requestMethod = "GET"
                                    val inicio = System.currentTimeMillis()
                                    try {
                                        conexao.connect()
                                        conexao.responseCode
                                        System.currentTimeMillis() - inicio
                                    } finally {
                                        conexao.disconnect()
                                    }
                                }
                            } catch (e: Exception) {
                                Timber.d("Erro ao medir latencia HTTP: ${e.message}")
                                null
                            }
                        }
                    }
                val amostras = jobs.awaitAll().filterNotNull()
                if (amostras.isEmpty()) return@coroutineScope null
                val sorted = amostras.sorted()
                sorted[sorted.size / 2]
            }

        /**
         * Mede tempo de resolução DNS com timeout de 5s via withTimeout.
         * InetAddress.getByName() pode travar indefinidamente em alguns carriers/dispositivos
         * sem o timeout de coroutine cobrindo o bloqueio de thread.
         * Roda em Dispatchers.IO pois a chamada é bloqueante.
         */
        private suspend fun medirDnsResolveTime(): Long? =
            try {
                withContext(Dispatchers.IO) {
                    withTimeout(DNS_TIMEOUT_MS) {
                        val inicio = System.nanoTime()
                        InetAddress.getByName("cloudflare.com")
                        val fim = System.nanoTime()
                        (fim - inicio) / 1_000_000
                    }
                }
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
