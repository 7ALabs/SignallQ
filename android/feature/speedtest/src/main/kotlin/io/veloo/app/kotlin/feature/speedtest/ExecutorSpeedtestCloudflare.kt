package io.signallq.app.feature.speedtest

import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.supervisorScope
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.Collections
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.random.Random

class ExecutorSpeedtestCloudflare(isMobile: Boolean = false) : ExecutorSpeedtest {
    private companion object {
        private const val UA = "Mozilla/5.0 (Linux; Android 14; SM-A256E) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Mobile Safari/537.36"

        // Pool adaptativo: móvel usa menos conexões e keep-alive curto para poupar bateria/dados.
        // Wi-Fi/fixo usa pool maior para throughput máximo no speedtest.
        fun criarConnectionPool(isMobile: Boolean): okhttp3.ConnectionPool =
            if (isMobile) {
                okhttp3.ConnectionPool(2, 1, TimeUnit.MINUTES)
            } else {
                okhttp3.ConnectionPool(8, 5, TimeUnit.MINUTES)
            }
    }

    // Client HTTP/2 para upload e ping — múltiplos streams em uma conexão TCP.
    private val client: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_2, Protocol.HTTP_1_1))
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(criarConnectionPool(isMobile))
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", UA)
                    .header("Cache-Control", "no-store")
                    .build(),
            )
        }
        .build()

    // Client HTTP/1.1 para download — cada worker usa conexão TCP própria,
    // com headers de contexto de browser para evitar rate-limit 429/403 do Cloudflare
    // no endpoint /__down (que bloqueia clientes HTTP/2 sem contexto de origem).
    // Pool adaptado ao tipo de rede: móvel=2 conexões, Wi-Fi=8 conexões.
    private val downloadClient: OkHttpClient = OkHttpClient.Builder()
        .protocols(listOf(Protocol.HTTP_1_1))
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .connectionPool(criarConnectionPool(isMobile))
        .addInterceptor { chain ->
            chain.proceed(
                chain.request().newBuilder()
                    .header("User-Agent", UA)
                    .header("Accept", "*/*")
                    .header("Accept-Language", "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7")
                    .header("Cache-Control", "no-store")
                    .header("Origin", "https://speed.cloudflare.com")
                    .header("Referer", "https://speed.cloudflare.com/")
                    .header("Sec-Fetch-Dest", "empty")
                    .header("Sec-Fetch-Mode", "cors")
                    .header("Sec-Fetch-Site", "same-origin")
                    .build(),
            )
        }
        .build()

    // Client separado para pings — timeout menor, reusa o mesmo pool H2.
    private val pingClient: OkHttpClient = client.newBuilder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .callTimeout(4, TimeUnit.SECONDS)
        .build()

    private val emExecucao = AtomicBoolean(false)
    private val cancelFlag = AtomicBoolean(false)
    private val bytesConsumidosTotal = AtomicLong(0L)
    @Volatile private var faseAtualInterna: FaseSpeedtest = FaseSpeedtest.idle
    @Volatile private var velocidadeAtualInterna: Double = 0.0
    private val uploadPayloadCache = ConcurrentHashMap<Int, ByteArray>()
    private val pontosAoVivoInternos = Collections.synchronizedList(mutableListOf<PontoAoVivo>())
    // Estado do Teste Triplo
    @Volatile private var rodadaAtualInterna: Int = 0
    @Volatile private var aguardandoProximaRodadaInterna: Boolean = false
    private val rodadasTriploInternos = Collections.synchronizedList(mutableListOf<ResultadoRodadaTriplo>())
    private val mutableSnapshotFlow =
        MutableStateFlow(
            SnapshotExecucaoSpeedtest(
                estado = EstadoExecucaoSpeedtest.idle,
                progressoPercentual = 0,
                resultado = null,
                erroMensagem = null,
            ),
        )

    override val snapshotFlow: StateFlow<SnapshotExecucaoSpeedtest> = mutableSnapshotFlow.asStateFlow()

    override fun cancelar() { cancelFlag.set(true) }

    override suspend fun executar(
        modo: ModoSpeedtest,
        connectionType: String?,
        connectionTypeProvider: (() -> String?)?,
        tecnologiaProvider: (() -> String?)?,
    ) {
        if (!emExecucao.compareAndSet(false, true)) return
        withContext(Dispatchers.IO) {
            try {
                cancelFlag.set(false)
                bytesConsumidosTotal.set(0L)
                faseAtualInterna = FaseSpeedtest.idle
                velocidadeAtualInterna = 0.0
                rodadaAtualInterna = 0
                aguardandoProximaRodadaInterna = false
                rodadasTriploInternos.clear()

                if (modo == ModoSpeedtest.triplo) {
                    executarModoTriplo(
                        connectionType = connectionType,
                        connectionTypeProvider = connectionTypeProvider,
                        tecnologiaProvider = tecnologiaProvider,
                    )
                    return@withContext
                }

                val redeInicial = connectionType
                var faseInterrompida = "none"
                faseAtualInterna = FaseSpeedtest.ping
                pontosAoVivoInternos.clear()
                publicar(EstadoExecucaoSpeedtest.executando, 5, null, null)
                val config = SpeedtestConfig.fromModo(modo)
                val latencyPhase =
                    executarFaseLatencia(
                        config = config,
                        redeInicial = redeInicial,
                        connectionTypeProvider = connectionTypeProvider,
                        onPingProgress = { idx, total ->
                            publicar(EstadoExecucaoSpeedtest.executando, (idx.toDouble() / total * 28).toInt(), null, null)
                        },
                    )
                if (cancelFlag.get()) {
                    faseAtualInterna = FaseSpeedtest.idle
                    velocidadeAtualInterna = 0.0
                    publicar(EstadoExecucaoSpeedtest.idle, 0, null, null)
                    return@withContext
                }
                if (mudouRede(redeInicial, connectionTypeProvider)) {
                    faseInterrompida = "redeMudouAposLatencia"
                    val redeFinal = connectionTypeProvider?.invoke() ?: connectionType
                    val resultadoContaminado =
                        construirResultado(
                            modo = modo,
                            redeInicial = redeInicial,
                            redeFinal = redeFinal,
                            latencyPhase = latencyPhase,
                            downloadPhase = throughputVazio("naoExecutado"),
                            uploadPhase = throughputVazio("naoExecutado"),
                            pingDownload = emptyList(),
                            pingUpload = emptyList(),
                            dns = DnsProbeResult(null, null, null, null),
                            contaminado = true,
                            faseInterrompida = faseInterrompida,
                            tecnologia = tecnologiaProvider?.invoke(),
                        )
                    registrarDiagnostico(resultadoContaminado)
                    faseAtualInterna = FaseSpeedtest.concluido
                    publicar(EstadoExecucaoSpeedtest.concluido, 100, resultadoContaminado, null)
                    return@withContext
                }
                faseAtualInterna = FaseSpeedtest.download
                pontosAoVivoInternos.clear()
                velocidadeAtualInterna = 0.0
                publicar(EstadoExecucaoSpeedtest.executando, 28, null, null)

                val pingDownload = Collections.synchronizedList(mutableListOf<Double>())
                val downloadPhase =
                    try {
                        executarFaseTransferencia(
                            isDownload = true,
                            config = config,
                            onFaseProgress = { local -> publicar(EstadoExecucaoSpeedtest.executando, 28 + (local * 44).toInt(), null, null) },
                            pingsSobCarga = pingDownload,
                            redeInicial = redeInicial,
                            connectionTypeProvider = connectionTypeProvider,
                        )
                    } catch (t: Throwable) {
                        val mensagem = t.message.orEmpty()
                        val ehRateLimit =
                            mensagem.startsWith("download_failed:IllegalStateException:HttpStatus:429") ||
                                mensagem.startsWith("download_failed:IllegalStateException:HttpStatus:403")
                        if (!ehRateLimit) {
                            throw t
                        }
                        val configFallback429 =
                            config.copy(
                                downloadPayloadBytes = 10_000_000,
                                downloadInitialStreams = 1,
                                downloadMaxStreams = 2,
                            )
                        Timber.w(
                            "fallback429 modo=${modo.name} downloadPayload=${configFallback429.downloadPayloadBytes} streams=${configFallback429.downloadInitialStreams}..${configFallback429.downloadMaxStreams}",
                        )
                        try {
                            executarFaseTransferencia(
                                isDownload = true,
                                config = configFallback429,
                                onFaseProgress = { local -> publicar(EstadoExecucaoSpeedtest.executando, 28 + (local * 44).toInt(), null, null) },
                                pingsSobCarga = pingDownload,
                                redeInicial = redeInicial,
                                connectionTypeProvider = connectionTypeProvider,
                            )
                        } catch (t2: Throwable) {
                            val msg2 = t2.message.orEmpty()
                            val ehRateLimit2 =
                                msg2.startsWith("download_failed:IllegalStateException:HttpStatus:429") ||
                                    msg2.startsWith("download_failed:IllegalStateException:HttpStatus:403")
                            if (!ehRateLimit2) throw t2
                            Timber.w("fallback429 também bloqueado, continuando sem download modo=${modo.name}")
                            throughputVazio("download_bloqueado_429")
                        }
                    }
                if (cancelFlag.get()) {
                    faseAtualInterna = FaseSpeedtest.idle
                    velocidadeAtualInterna = 0.0
                    publicar(EstadoExecucaoSpeedtest.idle, 0, null, null)
                    return@withContext
                }
                if (mudouRede(redeInicial, connectionTypeProvider)) {
                    faseInterrompida = "redeMudouAposDownload"
                    val redeFinal = connectionTypeProvider?.invoke() ?: connectionType
                    val resultadoContaminado =
                        construirResultado(
                            modo = modo,
                            redeInicial = redeInicial,
                            redeFinal = redeFinal,
                            latencyPhase = latencyPhase,
                            downloadPhase = downloadPhase,
                            uploadPhase = throughputVazio("naoExecutado"),
                            pingDownload = pingDownload,
                            pingUpload = emptyList(),
                            dns = DnsProbeResult(null, null, null, null),
                            contaminado = true,
                            faseInterrompida = faseInterrompida,
                            tecnologia = tecnologiaProvider?.invoke(),
                        )
                    registrarDiagnostico(resultadoContaminado)
                    faseAtualInterna = FaseSpeedtest.concluido
                    publicar(EstadoExecucaoSpeedtest.concluido, 100, resultadoContaminado, null)
                    return@withContext
                }

                faseAtualInterna = FaseSpeedtest.upload
                pontosAoVivoInternos.clear()
                velocidadeAtualInterna = 0.0
                publicar(EstadoExecucaoSpeedtest.executando, 74, null, null)

                val pingUpload = Collections.synchronizedList(mutableListOf<Double>())
                val dnsProbe = async { executarDnsProbe() }
                var uploadPhase = if (connectionType == "movel") {
                    executarFaseUploadAdaptativa(
                        config = config,
                        onFaseProgress = { local -> publicar(EstadoExecucaoSpeedtest.executando, 74 + (local * 24).toInt(), null, null) },
                        pingsSobCarga = pingUpload,
                        redeInicial = redeInicial,
                        connectionTypeProvider = connectionTypeProvider,
                    )
                } else {
                    executarFaseTransferencia(
                        isDownload = false,
                        config = config,
                        onFaseProgress = { local -> publicar(EstadoExecucaoSpeedtest.executando, 74 + (local * 24).toInt(), null, null) },
                        pingsSobCarga = pingUpload,
                        redeInicial = redeInicial,
                        connectionTypeProvider = connectionTypeProvider,
                    )
                }
                var uploadNaoDetectado = false
                if (uploadPhase.throughputMbps == 0.0 && !cancelFlag.get()) {
                    val backoffMs = listOf(1_000L, 2_000L, 4_000L)
                    for ((idx, backoff) in backoffMs.withIndex()) {
                        delay(backoff)
                        if (cancelFlag.get()) break
                        Timber.w("upload=0 retry #${idx + 1} apos ${backoff}ms")
                        val mbpsRetry = executarProbeUpload()
                        if (mbpsRetry > 0.0) {
                            uploadPhase = uploadPhase.copy(
                                throughputMbps = mbpsRetry,
                                peakMbps = mbpsRetry,
                                faseEncerradaPor = "retryBemSucedido",
                                throughputOrigem = "retryProbe",
                            )
                            break
                        }
                        if (idx == backoffMs.lastIndex) uploadNaoDetectado = true
                    }
                }
                val dns = dnsProbe.await()
                val redeFinal = connectionTypeProvider?.invoke() ?: connectionType
                val contaminado = redeInicial != null && redeFinal != null && redeInicial != redeFinal

                val resultado =
                    construirResultado(
                        modo = modo,
                        redeInicial = redeInicial,
                        redeFinal = redeFinal,
                        latencyPhase = latencyPhase,
                        downloadPhase = downloadPhase,
                        uploadPhase = uploadPhase,
                        pingDownload = pingDownload,
                        pingUpload = pingUpload,
                        dns = dns,
                        contaminado = contaminado,
                        faseInterrompida = faseInterrompida,
                        uploadNaoDetectado = uploadNaoDetectado,
                        tecnologia = tecnologiaProvider?.invoke(),
                    )

                registrarDiagnostico(resultado)
                faseAtualInterna = FaseSpeedtest.concluido
                publicar(EstadoExecucaoSpeedtest.concluido, 100, resultado, null)
            } catch (t: Throwable) {
                Timber.e(t, "modo=${modo.name} erro=${t.message ?: "desconhecido"}")
                faseAtualInterna = FaseSpeedtest.idle
                velocidadeAtualInterna = 0.0
                publicar(
                    EstadoExecucaoSpeedtest.erro,
                    100,
                    null,
                    t.message ?: "erroSpeedtest",
                )
            } finally {
                emExecucao.set(false)
            }
        }
    }

    private suspend fun executarModoTriplo(
        connectionType: String?,
        connectionTypeProvider: (() -> String?)?,
        tecnologiaProvider: (() -> String?)? = null,
    ) {
        val redeInicial = connectionType
        val config = SpeedtestConfig.fromModo(ModoSpeedtest.triplo)
        val totalRodadas = 3

        for (rodada in 1..totalRodadas) {
            if (cancelFlag.get()) {
                faseAtualInterna = FaseSpeedtest.idle
                velocidadeAtualInterna = 0.0
                aguardandoProximaRodadaInterna = false
                publicar(EstadoExecucaoSpeedtest.idle, 0, null, null)
                return
            }

            rodadaAtualInterna = rodada
            aguardandoProximaRodadaInterna = false

            // Progresso global: cada rodada ocupa ~27% (ping 5 + dl 14 + ul 8 = ~27pp)
            // Rodada 1: 0..27, Rodada 2: 27..54, Rodada 3: 54..81; restam 19pp para conclusão
            val offsetProgresso = (rodada - 1) * 27

            faseAtualInterna = FaseSpeedtest.ping
            pontosAoVivoInternos.clear()
            publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 2, null, null)

            val latencyPhase = executarFaseLatencia(
                config = config,
                redeInicial = redeInicial,
                connectionTypeProvider = connectionTypeProvider,
                onPingProgress = { idx, total ->
                    publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + (idx.toDouble() / total * 5).toInt(), null, null)
                },
            )
            if (cancelFlag.get()) {
                faseAtualInterna = FaseSpeedtest.idle
                velocidadeAtualInterna = 0.0
                aguardandoProximaRodadaInterna = false
                publicar(EstadoExecucaoSpeedtest.idle, 0, null, null)
                return
            }

            faseAtualInterna = FaseSpeedtest.download
            pontosAoVivoInternos.clear()
            velocidadeAtualInterna = 0.0
            publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 5, null, null)

            val pingDownload = Collections.synchronizedList(mutableListOf<Double>())
            val downloadPhase = try {
                executarFaseTransferencia(
                    isDownload = true,
                    config = config,
                    onFaseProgress = { local ->
                        publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 5 + (local * 14).toInt(), null, null)
                    },
                    pingsSobCarga = pingDownload,
                    redeInicial = redeInicial,
                    connectionTypeProvider = connectionTypeProvider,
                )
            } catch (t: Throwable) {
                val mensagem = t.message.orEmpty()
                val ehRateLimit = mensagem.startsWith("download_failed:IllegalStateException:HttpStatus:429") ||
                    mensagem.startsWith("download_failed:IllegalStateException:HttpStatus:403")
                if (!ehRateLimit) throw t
                val configFallback = config.copy(downloadPayloadBytes = 10_000_000, downloadInitialStreams = 1, downloadMaxStreams = 2)
                try {
                    executarFaseTransferencia(
                        isDownload = true,
                        config = configFallback,
                        onFaseProgress = { local ->
                            publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 5 + (local * 14).toInt(), null, null)
                        },
                        pingsSobCarga = pingDownload,
                        redeInicial = redeInicial,
                        connectionTypeProvider = connectionTypeProvider,
                    )
                } catch (_: Throwable) {
                    throughputVazio("download_bloqueado_429")
                }
            }
            if (cancelFlag.get()) {
                faseAtualInterna = FaseSpeedtest.idle
                velocidadeAtualInterna = 0.0
                aguardandoProximaRodadaInterna = false
                publicar(EstadoExecucaoSpeedtest.idle, 0, null, null)
                return
            }

            faseAtualInterna = FaseSpeedtest.upload
            pontosAoVivoInternos.clear()
            velocidadeAtualInterna = 0.0
            publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 19, null, null)

            val pingUpload = Collections.synchronizedList(mutableListOf<Double>())
            val uploadPhase = if (connectionType == "movel") {
                executarFaseUploadAdaptativa(
                    config = config,
                    onFaseProgress = { local ->
                        publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 19 + (local * 8).toInt(), null, null)
                    },
                    pingsSobCarga = pingUpload,
                    redeInicial = redeInicial,
                    connectionTypeProvider = connectionTypeProvider,
                )
            } else {
                executarFaseTransferencia(
                    isDownload = false,
                    config = config,
                    onFaseProgress = { local ->
                        publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 19 + (local * 8).toInt(), null, null)
                    },
                    pingsSobCarga = pingUpload,
                    redeInicial = redeInicial,
                    connectionTypeProvider = connectionTypeProvider,
                )
            }

            // Acumula rodada
            rodadasTriploInternos.add(
                ResultadoRodadaTriplo(
                    downloadMbps = downloadPhase.throughputMbps,
                    uploadMbps = uploadPhase.throughputMbps,
                    latenciaMs = latencyPhase.latenciaMs,
                )
            )

            // Intervalo de 10s entre rodadas (exceto após a última)
            if (rodada < totalRodadas && !cancelFlag.get()) {
                aguardandoProximaRodadaInterna = true
                faseAtualInterna = FaseSpeedtest.idle
                velocidadeAtualInterna = 0.0
                publicar(EstadoExecucaoSpeedtest.executando, offsetProgresso + 27, null, null)
                delay(10_000L)
                aguardandoProximaRodadaInterna = false
            }
        }

        if (cancelFlag.get()) {
            faseAtualInterna = FaseSpeedtest.idle
            velocidadeAtualInterna = 0.0
            aguardandoProximaRodadaInterna = false
            publicar(EstadoExecucaoSpeedtest.idle, 0, null, null)
            return
        }

        // Calcula medianas e constrói resultado final
        fun List<Double>.mediana(): Double {
            val sorted = this.sorted()
            return if (sorted.size % 2 == 0) (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2.0 else sorted[sorted.size / 2]
        }
        val rodadas = rodadasTriploInternos.toList()
        val downloadMediana = rodadas.map { it.downloadMbps }.mediana()
        val uploadMediana = rodadas.map { it.uploadMbps }.mediana()
        val latenciaMediana = rodadas.map { it.latenciaMs }.mediana()

        // Resultado sintético com médias — usa valores neutros para campos não medidos em triplo
        // (bufferbloat, jitter, DNS não são calculados por rodada no triplo)
        val resultadoTriplo = ResultadoSpeedtest(
            timestampEpochMs = System.currentTimeMillis(),
            specVersion = "1.0.0",
            modo = ModoSpeedtest.triplo,
            connectionTypeStart = connectionType,
            connectionTypeEnd = connectionTypeProvider?.invoke() ?: connectionType,
            contaminado = false,
            connectionType = connectionType,
            tecnologia = tecnologiaProvider?.invoke(),
            latenciaMs = latenciaMediana,
            jitterMs = 0.0,
            perdaPercentual = 0.0,
            bufferbloatMs = 0.0,
            severidadeBufferbloat = SeveridadeBufferbloat.none,
            downloadMbps = downloadMediana,
            uploadMbps = uploadMediana,
            latencyDownloadMs = 0.0,
            latencyUploadMs = 0.0,
            stabilityScore = 0.0,
            peakDownloadMbps = rodadas.maxOf { it.downloadMbps },
            peakUploadMbps = rodadas.maxOf { it.uploadMbps },
            packetLossSource = "naoMedido",
            dnsLatencyMs = null,
            dnsResolverIp = null,
            dnsProvider = null,
            diagnosticoQualidade = SpeedtestQualityClassifier.classificarQualidade(
                dl = downloadMediana,
                ul = uploadMediana,
                latency = latenciaMediana,
                jitter = 0.0,
                packetLoss = 0.0,
                bufferbloatDeltaMs = 0.0,
                bufferbloat = SeveridadeBufferbloat.none,
            ),
            diagnosticoFases = DiagnosticoFasesSpeedtest(
                faseInterrompida = "none",
                latenciaAmostrasTotais = 0,
                latenciaAmostrasValidas = 0,
                latenciaTimeouts = 0,
                downloadBytesTotal = 0L,
                downloadAmostrasValidas = 0,
                downloadRequisicoesSucesso = 0,
                downloadRequisicoesErro = 0,
                downloadEncerradaPor = "triplo",
                downloadThroughputOrigem = "media3rodadas",
                downloadUltimoErro = null,
                uploadBytesTotal = 0L,
                uploadAmostrasValidas = 0,
                uploadRequisicoesSucesso = 0,
                uploadRequisicoesErro = 0,
                uploadEncerradaPor = "triplo",
                uploadThroughputOrigem = "media3rodadas",
                uploadUltimoErro = null,
                dnsErroMensagem = null,
            ),
        )

        Timber.i("triplo concluido dl=${downloadMediana} ul=${uploadMediana} lat=${latenciaMediana} rodadas=${rodadas.size}")
        faseAtualInterna = FaseSpeedtest.concluido
        aguardandoProximaRodadaInterna = false
        publicar(EstadoExecucaoSpeedtest.concluido, 100, resultadoTriplo, null)
    }

    private suspend fun executarFaseUploadAdaptativa(
        config: SpeedtestConfig,
        onFaseProgress: (Double) -> Unit,
        pingsSobCarga: MutableList<Double>,
        redeInicial: String?,
        connectionTypeProvider: (() -> String?)?,
    ): ThroughputPhase = supervisorScope {
        val inicioNs = System.nanoTime()
        val budgetMs = 25_000L
        val stopNs = inicioNs + (budgetMs * 1_000_000L)
        val amostras = mutableListOf<Sample>()
        val bytesTotal = AtomicLong(0)
        val requisicoesSucesso = AtomicInteger(0)
        val requisicoesErro = AtomicInteger(0)
        var chunkBytes = 64 * 1024
        var paralelo = 1
        var roundsLentos = 0
        var rodada = 0

        fun elapsedMs(): Long = (System.nanoTime() - inicioNs) / 1_000_000L

        // Intervalo aumentado para 1000ms (era 300ms) durante o throughput adaptativo:
        // pings frequentes competem por banda com os workers de upload no mesmo pool HTTP/2,
        // distorcendo ambas as medições. Amostras a cada 1s ainda são suficientes para
        // calcular bufferbloat (latência sob carga vs. latência em repouso).
        val pingJob =
            launch {
                while (System.nanoTime() < stopNs && !mudouRede(redeInicial, connectionTypeProvider) && !cancelFlag.get()) {
                    val t0 = System.nanoTime()
                    val rtt = medirPing()
                    if (rtt != null) pingsSobCarga.add(rtt)
                    val elapsed = (System.nanoTime() - t0) / 1_000_000L
                    delay(max(0L, 1_000L - elapsed))
                }
            }

        while (rodada < 4 && System.nanoTime() < stopNs && !mudouRede(redeInicial, connectionTypeProvider) && !cancelFlag.get()) {
            rodada++
            val tRoundNs = System.nanoTime()
            val jobs = mutableListOf<Job>()
            val bytesRodada = AtomicLong(0)

            repeat(paralelo) {
                jobs +=
                    launch {
                        try {
                            val sent = executarRequestUpload(chunkBytes)
                            bytesRodada.addAndGet(sent.toLong())
                            bytesTotal.addAndGet(sent.toLong())
                            bytesConsumidosTotal.addAndGet(sent.toLong())
                            requisicoesSucesso.incrementAndGet()
                        } catch (_: Throwable) {
                            requisicoesErro.incrementAndGet()
                        }
                    }
            }
            jobs.forEach { it.join() }
            val tRoundMs = ((System.nanoTime() - tRoundNs) / 1_000_000L).coerceAtLeast(1L)
            val mbps = (bytesRodada.get() * 8.0) / (tRoundMs.toDouble() / 1000.0) / 1_000_000.0
            amostras.add(Sample(elapsedMs().toInt(), mbps))
            velocidadeAtualInterna = mbps
            onFaseProgress(min(1.0, elapsedMs().toDouble() / config.uploadDurationMs.toDouble()))

            val roundRapido = tRoundMs < 2_000L
            val podeEscalar = paralelo < 4 && chunkBytes < (2 * 1024 * 1024)
            if (roundRapido && podeEscalar) {
                paralelo = min(4, paralelo + 1)
                chunkBytes = min(2 * 1024 * 1024, chunkBytes * 4)
                roundsLentos = 0
            } else {
                roundsLentos++
                if (roundsLentos >= 2) break
            }
        }

        pingJob.join()
        val validas = amostras.filter { it.mbps > 0.0 }
        val throughput = if (validas.isEmpty()) 0.0 else validas.map { it.mbps }.average()
        val pico = validas.maxOfOrNull { it.mbps } ?: 0.0
        val encerradaPor =
            when {
                mudouRede(redeInicial, connectionTypeProvider) -> "redeMudou"
                rodada >= 4 -> "rodadasMax"
                roundsLentos >= 2 -> "estagnou"
                else -> "tempoEsgotado"
            }
        ThroughputPhase(
            throughputMbps = throughput,
            peakMbps = pico,
            amostrasInstantaneas = validas.map { it.mbps },
            bytesTotal = bytesTotal.get(),
            requisicoesSucesso = requisicoesSucesso.get(),
            requisicoesErro = requisicoesErro.get(),
            faseEncerradaPor = encerradaPor,
            throughputOrigem = if (validas.isEmpty()) "semDados" else "validasSemCorte",
            ultimoErro = null,
        )
    }

    private suspend fun executarFaseLatencia(
        config: SpeedtestConfig,
        redeInicial: String?,
        connectionTypeProvider: (() -> String?)?,
        onPingProgress: ((Int, Int) -> Unit)? = null,
    ): LatencyPhase {
        val bruto = mutableListOf<Double?>()
        repeat(config.pingCount) { i ->
            if (mudouRede(redeInicial, connectionTypeProvider)) return@repeat
            bruto.add(medirPing())
            onPingProgress?.invoke(i + 1, config.pingCount)
        }

        val semPrimeiro = bruto.drop(1)
        val timeouts = semPrimeiro.count { it == null }
        val validos = semPrimeiro.filterNotNull()
        val mediana = median(validos)
        val filtrados = if (mediana > 0.0) validos.filter { it <= mediana * 3.0 } else validos
        val usados = if (filtrados.isNotEmpty()) filtrados else validos

        return LatencyPhase(
            latenciaMs = median(usados),
            jitterMs = jitter(usados),
            perdaPercentual = if (semPrimeiro.isNotEmpty()) (timeouts.toDouble() / semPrimeiro.size.toDouble()) * 100.0 else 0.0,
            totalAmostras = semPrimeiro.size,
            amostrasValidas = usados.size,
            timeouts = timeouts,
        )
    }

    private suspend fun executarFaseTransferencia(
        isDownload: Boolean,
        config: SpeedtestConfig,
        onFaseProgress: (Double) -> Unit,
        pingsSobCarga: MutableList<Double>,
        redeInicial: String?,
        connectionTypeProvider: (() -> String?)?,
    ): ThroughputPhase = supervisorScope {
        val duracaoMs = if (isDownload) config.downloadDurationMs else config.uploadDurationMs
        val warmupMs = if (isDownload) config.downloadWarmupMs else config.uploadWarmupMs
        val payloadBytes = if (isDownload) config.downloadPayloadBytes else config.uploadPayloadBytes
        val streamInicial = if (isDownload) config.downloadInitialStreams else config.uploadInitialStreams
        val maxStreams = if (isDownload) config.downloadMaxStreams else config.uploadMaxStreams

        val inicioNs = System.nanoTime()
        val stopNs = inicioNs + (duracaoMs * 1_000_000L)
        val stopFlag = AtomicBoolean(false)
        val bytesTick = AtomicLong(0)
        val bytesTotal = AtomicLong(0)
        val requisicoesSucesso = AtomicInteger(0)
        val requisicoesErro = AtomicInteger(0)
        val ultimoErro = AtomicReference<String?>(null)
        val targetStreams = AtomicInteger(streamInicial)
        val amostras = Collections.synchronizedList(mutableListOf<Sample>())
        val workers = mutableListOf<Job>()
        val ultimoSampleNs = AtomicLong(inicioNs)

        fun elapsedMs(): Long = (System.nanoTime() - inicioNs) / 1_000_000L

        fun spawnWorker(indice: Int) {
            workers +=
                launch {
                    if (indice > 0) delay(indice * 200L)
                    var fallbackTriedDownload = false
                    var payloadDownloadAtual = payloadBytes
                    var tentativasRateLimit = 0
                    while (!stopFlag.get() && System.nanoTime() < stopNs && !mudouRede(redeInicial, connectionTypeProvider) && !cancelFlag.get()) {
                        if (indice >= targetStreams.get()) {
                            delay(120)
                            continue
                        }
                        val restanteMs = ((stopNs - System.nanoTime()) / 1_000_000L).coerceAtLeast(0L)
                        if (restanteMs <= 0L) break
                        try {
                            if (isDownload) {
                                executarRequestDownload(payloadDownloadAtual) { lidos ->
                                    bytesTick.addAndGet(lidos.toLong())
                                    bytesTotal.addAndGet(lidos.toLong())
                                    bytesConsumidosTotal.addAndGet(lidos.toLong())
                                }
                            } else {
                                val sent = executarRequestUpload(payloadBytes)
                                bytesTick.addAndGet(sent.toLong())
                                bytesTotal.addAndGet(sent.toLong())
                                bytesConsumidosTotal.addAndGet(sent.toLong())
                            }
                            tentativasRateLimit = 0
                            requisicoesSucesso.incrementAndGet()
                        } catch (t: Throwable) {
                            val erroResumo = resumirErroTransferencia(t)
                            val ehRateLimit = erroResumo.contains("HttpStatus:429") || erroResumo.contains("HttpStatus:403")
                            if (isDownload && ehRateLimit) {
                                if (ultimoErro.get() == null) ultimoErro.set(erroResumo)
                                tentativasRateLimit++
                                val backoffMs = min(2000L, 500L * tentativasRateLimit)
                                delay(backoffMs)
                                if (tentativasRateLimit >= 3) {
                                    delay(4000L)
                                    tentativasRateLimit = 0
                                }
                                continue
                            }
                            requisicoesErro.incrementAndGet()
                            if (ultimoErro.get() == null) ultimoErro.set(erroResumo)
                            if (isDownload && !fallbackTriedDownload) {
                                val fallback = reduzirPayloadDownload(payloadDownloadAtual)
                                if (fallback < payloadDownloadAtual) {
                                    payloadDownloadAtual = fallback
                                    fallbackTriedDownload = true
                                    continue
                                }
                            }
                            break
                        }
                    }
                }
        }

        repeat(maxStreams) { idx -> spawnWorker(idx) }

        // Intervalo aumentado para 1000ms (era 300ms) durante o throughput:
        // pings a cada 300ms competem por banda com os workers de transferência no mesmo pool
        // HTTP/2, distorcendo o throughput e os próprios valores de latência sob carga.
        // 1 amostra/s ainda é suficiente para calcular bufferbloat com precisão adequada.
        val pingJob =
            launch {
                while (!stopFlag.get() && System.nanoTime() < stopNs && !mudouRede(redeInicial, connectionTypeProvider) && !cancelFlag.get()) {
                    val t0 = System.nanoTime()
                    val rtt = medirPing()
                    if (rtt != null) pingsSobCarga.add(rtt)
                    val elapsed = (System.nanoTime() - t0) / 1_000_000L
                    val waitMs = max(0L, 1_000L - elapsed)
                    delay(waitMs)
                }
            }

        val sampler =
            launch {
                var ultimaEscalaMs = 0L
                while (!stopFlag.get() && System.nanoTime() < stopNs && !mudouRede(redeInicial, connectionTypeProvider) && !cancelFlag.get()) {
                    delay(1_000)
                    val agoraNs = System.nanoTime()
                    val elapsedSec = (agoraNs - ultimoSampleNs.get()).toDouble() / 1_000_000_000.0
                    ultimoSampleNs.set(agoraNs)
                    val bytes = bytesTick.getAndSet(0)
                    val tMs = elapsedMs()
                    val progressoLocal = min(1.0, tMs.toDouble() / duracaoMs.toDouble())
                    onFaseProgress(progressoLocal)

                    if (elapsedSec > 0.0 && bytes > 0L) {
                        val instant = (bytes * 8.0) / elapsedSec / 1_000_000.0
                        amostras.add(Sample(tMs.toInt(), instant))
                        velocidadeAtualInterna = instant
                    }

                    val prontoParaEscalar = tMs - ultimaEscalaMs >= 4000L && targetStreams.get() < maxStreams
                    if (prontoParaEscalar) {
                        ultimaEscalaMs = tMs
                        val ganho = calcularGanhoJanela(amostras.toList(), tMs.toInt())
                        if (ganho >= 0.10) {
                            val novoAlvo = min(maxStreams, targetStreams.get() + 2)
                            targetStreams.set(novoAlvo)
                        }
                    }
                }
            }

        while (System.nanoTime() < stopNs && !mudouRede(redeInicial, connectionTypeProvider) && !cancelFlag.get()) {
            delay(80)
        }
        stopFlag.set(true)
        pingJob.join()
        sampler.join()
        workers.forEach { it.join() }
        val bytesRestantes = bytesTick.getAndSet(0)
        val agoraFlushNs = System.nanoTime()
        val elapsedFlushSec = (agoraFlushNs - ultimoSampleNs.get()).toDouble() / 1_000_000_000.0
        if (bytesRestantes > 0L && elapsedFlushSec > 0.0) {
            val instant = (bytesRestantes * 8.0) / elapsedFlushSec / 1_000_000.0
            amostras.add(Sample(elapsedMs().toInt(), instant))
        }
        val duracaoMedidaMs = ((System.nanoTime() - inicioNs) / 1_000_000L).coerceAtLeast(1L)

        val amostrasValidas =
            amostras
                .filter { it.tMs >= warmupMs && it.mbps > 0.0 }
                .sortedBy { it.tMs }
        val corte = min(amostrasValidas.size, kotlin.math.ceil(amostrasValidas.size * 0.35).toInt())
        val estaveis = amostrasValidas.drop(corte)
        val throughputCalculado =
            calcularThroughputFase(
                estaveis = estaveis,
                amostrasValidas = amostrasValidas,
                bytesTotal = bytesTotal.get(),
                duracaoFaseMs = duracaoMedidaMs,
                warmupMs = warmupMs,
            )
        if (isDownload && bytesTotal.get() <= 0L && requisicoesSucesso.get() <= 0) {
            throw IllegalStateException("download_failed:${ultimoErro.get() ?: "semDetalhe"}")
        }
        val pico = amostrasValidas.maxOfOrNull { it.mbps } ?: 0.0
        val encerradaPor =
            when {
                mudouRede(redeInicial, connectionTypeProvider) -> "redeMudou"
                else -> "tempoEsgotado"
            }
        ThroughputPhase(
            throughputMbps = throughputCalculado.mbps,
            peakMbps = pico,
            amostrasInstantaneas = amostrasValidas.map { it.mbps },
            bytesTotal = bytesTotal.get(),
            requisicoesSucesso = requisicoesSucesso.get(),
            requisicoesErro = requisicoesErro.get(),
            faseEncerradaPor = encerradaPor,
            throughputOrigem = throughputCalculado.origem,
            ultimoErro = ultimoErro.get(),
        )
    }

    // ── Camada HTTP (OkHttp) ─────────────────────────────────────────────────

    private fun executarRequestDownload(
        payloadBytes: Int,
        onBytesChunk: ((Int) -> Unit)? = null,
    ): Int {
        val url = "https://speed.cloudflare.com/__down?bytes=$payloadBytes&_cb=${cacheBust()}"
        val request = Request.Builder().url(url).get().build()
        val response = downloadClient.newCall(request).execute()
        return try {
            if (!response.isSuccessful) throw IllegalStateException("HttpStatus:${response.code}")
            val body = response.body ?: throw IllegalStateException("emptyBody")
            val buffer = ByteArray(16 * 1024)
            var total = 0
            body.byteStream().use { stream ->
                while (true) {
                    val lidos = stream.read(buffer)
                    if (lidos < 0) break
                    total += lidos
                    onBytesChunk?.invoke(lidos)
                }
            }
            total
        } finally {
            response.close()
        }
    }

    private fun executarRequestUpload(payloadBytes: Int): Int {
        val url = "https://speed.cloudflare.com/__up?_cb=${cacheBust()}"
        val payload = obterPayloadUpload(payloadBytes)
        val body = payload.toRequestBody("application/octet-stream".toMediaType())
        val request = Request.Builder().url(url).post(body).build()
        val response = client.newCall(request).execute()
        return try {
            if (!response.isSuccessful) throw IllegalStateException("HttpStatus:${response.code}")
            payloadBytes
        } finally {
            response.close()
        }
    }

    private suspend fun executarProbeUpload(): Double {
        val payloadBytes = 256 * 1024
        var totalBytes = 0L
        val inicioNs = System.nanoTime()
        var sucesso = 0
        repeat(3) {
            try {
                val sent = executarRequestUpload(payloadBytes)
                totalBytes += sent.toLong()
                sucesso++
            } catch (_: Throwable) {}
        }
        val elapsedMs = ((System.nanoTime() - inicioNs) / 1_000_000L).coerceAtLeast(1L)
        if (sucesso == 0) return 0.0
        return (totalBytes * 8.0) / (elapsedMs.toDouble() / 1000.0) / 1_000_000.0
    }

    private fun medirPing(): Double? {
        val url = "https://speed.cloudflare.com/__down?bytes=0&_cb=${cacheBust()}"
        val request = Request.Builder().url(url).get().build()
        val inicio = System.nanoTime()
        return try {
            val response = pingClient.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return null
                resp.body?.bytes()
                (System.nanoTime() - inicio) / 1_000_000.0
            }
        } catch (_: Throwable) {
            null
        }
    }

    private fun obterPayloadUpload(payloadBytes: Int): ByteArray =
        uploadPayloadCache.getOrPut(payloadBytes) {
            ByteArray(payloadBytes) { indice -> (indice and 0xFF).toByte() }
        }

    private fun executarDnsProbe(): DnsProbeResult {
        val url = "https://cloudflare-dns.com/dns-query?name=whoami.cloudflare.com&type=TXT"
        val request = Request.Builder()
            .url(url)
            .header("accept", "application/dns-json")
            .get()
            .build()
        val inicio = System.nanoTime()
        return try {
            val response = client.newCall(request).execute()
            response.use { resp ->
                if (!resp.isSuccessful) return DnsProbeResult(null, null, null, null)
                val corpo = resp.body?.string() ?: return DnsProbeResult(null, null, null, null)
                val duracaoMs = ((System.nanoTime() - inicio) / 1_000_000L).toInt()
                val resolverIp = extrairPrimeiroIpv4(corpo)
                val provider = when {
                    corpo.contains("cloudflare", ignoreCase = true) -> "cloudflare"
                    resolverIp != null -> "desconhecido"
                    else -> null
                }
                DnsProbeResult(duracaoMs, resolverIp, provider, null)
            }
        } catch (_: Throwable) {
            DnsProbeResult(null, null, null, null)
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────
    // Classificacao extraida para SpeedtestQualityClassifier para reuso no diagnostico
    // sem duplicar thresholds. Nao alterar comportamento do speedtest aqui.

    private fun calcularEstabilidade(amostrasMbps: List<Double>): Double {
        val positivas = amostrasMbps.filter { it > 0.0 }
        if (positivas.size < 2) return 50.0
        val media = positivas.average()
        if (media <= 0.0) return 0.0
        val variancia = positivas.map { (it - media) * (it - media) }.average()
        val std = kotlin.math.sqrt(variancia)
        val cv = std / media
        return max(0.0, min(100.0, 100.0 - cv * 150.0))
    }

    private fun calcularGanhoJanela(amostras: List<Sample>, nowMs: Int): Double {
        val recente = amostras.filter { it.tMs > nowMs - 4000 }
        val anterior = amostras.filter { it.tMs <= nowMs - 4000 && it.tMs > nowMs - 8000 }
        if (recente.size < 3 || anterior.size < 3) return 0.0
        val mediaRecente = recente.map { it.mbps }.average()
        val mediaAnterior = anterior.map { it.mbps }.average()
        if (mediaAnterior <= 0.0) return 0.0
        return (mediaRecente - mediaAnterior) / mediaAnterior
    }

    private fun median(values: List<Double>): Double {
        if (values.isEmpty()) return 0.0
        val sorted = values.sorted()
        val m = sorted.size / 2
        return if (sorted.size % 2 == 0) (sorted[m - 1] + sorted[m]) / 2.0 else sorted[m]
    }

    private fun jitter(values: List<Double>): Double {
        if (values.size < 2) return 0.0
        val deltas = values.zipWithNext { a, b -> abs(b - a) }
        return if (deltas.isEmpty()) 0.0 else deltas.average()
    }

    private fun mudouRede(
        redeInicial: String?,
        connectionTypeProvider: (() -> String?)?,
    ): Boolean {
        if (redeInicial == null || connectionTypeProvider == null) return false
        val atual = connectionTypeProvider.invoke() ?: return false
        return atual != redeInicial
    }

    private fun cacheBust(): String = "${System.currentTimeMillis()}_${Random.nextInt(10_000, 99_999)}"

    private fun extrairPrimeiroIpv4(texto: String): String? {
        val regex = Regex("""\b((25[0-5]|2[0-4]\d|1\d\d|[1-9]?\d)(\.(?!$)|$)){4}\b""")
        return regex.find(texto)?.value
    }

    private fun calcularThroughputFase(
        estaveis: List<Sample>,
        amostrasValidas: List<Sample>,
        bytesTotal: Long,
        duracaoFaseMs: Long,
        warmupMs: Int,
    ): ThroughputCalculado {
        if (estaveis.isNotEmpty()) {
            return ThroughputCalculado(mbps = estaveis.map { it.mbps }.average(), origem = "estavel")
        }
        if (amostrasValidas.isNotEmpty()) {
            return ThroughputCalculado(mbps = amostrasValidas.map { it.mbps }.average(), origem = "validasSemCorte")
        }
        if (bytesTotal > 0L) {
            val janelaUtilMs = max(1L, duracaoFaseMs - warmupMs.toLong())
            return ThroughputCalculado(
                mbps = (bytesTotal * 8.0) / (janelaUtilMs.toDouble() / 1000.0) / 1_000_000.0,
                origem = "bytesTempoUtil",
            )
        }
        return ThroughputCalculado(mbps = 0.0, origem = "semDados")
    }

    private fun resumirErroTransferencia(t: Throwable): String {
        val tipo = t::class.java.simpleName.ifBlank { "Throwable" }
        val mensagem = (t.message ?: "").replace('\n', ' ').trim()
        val trecho = if (mensagem.length > 120) mensagem.substring(0, 120) else mensagem
        return if (trecho.isBlank()) tipo else "$tipo:$trecho"
    }

    private fun reduzirPayloadDownload(atual: Int): Int {
        val tamanhos = listOf(100_000, 1_000_000, 10_000_000, 25_000_000, 100_000_000)
        val idx = tamanhos.indexOf(atual)
        if (idx <= 0) return atual
        return tamanhos[idx - 1]
    }

    private fun construirResultado(
        modo: ModoSpeedtest,
        redeInicial: String?,
        redeFinal: String?,
        latencyPhase: LatencyPhase,
        downloadPhase: ThroughputPhase,
        uploadPhase: ThroughputPhase,
        pingDownload: List<Double>,
        pingUpload: List<Double>,
        dns: DnsProbeResult,
        contaminado: Boolean,
        faseInterrompida: String,
        uploadNaoDetectado: Boolean = false,
        tecnologia: String? = null,
    ): ResultadoSpeedtest {
        val latencyDownload = median(pingDownload)
        val latencyUpload = median(pingUpload)
        val bufferbloatMs = max(max(latencyDownload, latencyUpload) - latencyPhase.latenciaMs, 0.0)
        val severidadeBufferbloat = SpeedtestQualityClassifier.classificarBufferbloat(bufferbloatMs)
        val estabilidade = calcularEstabilidade(downloadPhase.amostrasInstantaneas + uploadPhase.amostrasInstantaneas)
        val diagnostico =
            SpeedtestQualityClassifier.classificarQualidade(
                dl = downloadPhase.throughputMbps,
                ul = uploadPhase.throughputMbps,
                latency = latencyPhase.latenciaMs,
                jitter = latencyPhase.jitterMs,
                packetLoss = latencyPhase.perdaPercentual,
                bufferbloatDeltaMs = bufferbloatMs,
                bufferbloat = severidadeBufferbloat,
            )
        return ResultadoSpeedtest(
            timestampEpochMs = System.currentTimeMillis(),
            specVersion = "1.0.0",
            modo = modo,
            connectionTypeStart = redeInicial,
            connectionTypeEnd = redeFinal,
            contaminado = contaminado,
            latenciaMs = latencyPhase.latenciaMs,
            jitterMs = latencyPhase.jitterMs,
            perdaPercentual = latencyPhase.perdaPercentual,
            bufferbloatMs = bufferbloatMs,
            severidadeBufferbloat = severidadeBufferbloat,
            downloadMbps = downloadPhase.throughputMbps,
            uploadMbps = uploadPhase.throughputMbps,
            latencyDownloadMs = latencyDownload,
            latencyUploadMs = latencyUpload,
            stabilityScore = estabilidade,
            peakDownloadMbps = downloadPhase.peakMbps,
            peakUploadMbps = uploadPhase.peakMbps,
            packetLossSource = "estimated",
            dnsLatencyMs = dns.latencyMs,
            dnsResolverIp = dns.resolverIp,
            dnsProvider = dns.provider,
            diagnosticoQualidade = diagnostico,
            diagnosticoFases =
                DiagnosticoFasesSpeedtest(
                    faseInterrompida = faseInterrompida,
                    latenciaAmostrasTotais = latencyPhase.totalAmostras,
                    latenciaAmostrasValidas = latencyPhase.amostrasValidas,
                    latenciaTimeouts = latencyPhase.timeouts,
                    downloadBytesTotal = downloadPhase.bytesTotal,
                    downloadAmostrasValidas = downloadPhase.amostrasInstantaneas.size,
                    downloadRequisicoesSucesso = downloadPhase.requisicoesSucesso,
                    downloadRequisicoesErro = downloadPhase.requisicoesErro,
                    downloadEncerradaPor = downloadPhase.faseEncerradaPor,
                    downloadThroughputOrigem = downloadPhase.throughputOrigem,
                    downloadUltimoErro = downloadPhase.ultimoErro,
                    uploadBytesTotal = uploadPhase.bytesTotal,
                    uploadAmostrasValidas = uploadPhase.amostrasInstantaneas.size,
                    uploadRequisicoesSucesso = uploadPhase.requisicoesSucesso,
                    uploadRequisicoesErro = uploadPhase.requisicoesErro,
                    uploadEncerradaPor = uploadPhase.faseEncerradaPor,
                    uploadThroughputOrigem = uploadPhase.throughputOrigem,
                    uploadUltimoErro = uploadPhase.ultimoErro,
                    dnsErroMensagem = dns.erroMensagem,
                ),
            uploadNaoDetectado = uploadNaoDetectado,
            connectionType = redeInicial,
            tecnologia = tecnologia,
        )
    }

    private fun registrarDiagnostico(resultado: ResultadoSpeedtest) {
        val d = resultado.diagnosticoFases
        Timber.i(
            "modo=${resultado.modo.name} d=${resultado.downloadMbps} u=${resultado.uploadMbps} " +
                "lat=${resultado.latenciaMs} jit=${resultado.jitterMs} " +
                "faseInterrompida=${d.faseInterrompida} " +
                "dlReqOk=${d.downloadRequisicoesSucesso} dlReqErr=${d.downloadRequisicoesErro} dlBytes=${d.downloadBytesTotal} dlOrigem=${d.downloadThroughputOrigem} dlErr=${d.downloadUltimoErro ?: "none"} " +
                "ulReqOk=${d.uploadRequisicoesSucesso} ulReqErr=${d.uploadRequisicoesErro} ulBytes=${d.uploadBytesTotal} ulOrigem=${d.uploadThroughputOrigem} ulErr=${d.uploadUltimoErro ?: "none"} " +
                "dnsErro=${d.dnsErroMensagem ?: "none"}",
        )
    }

    private fun publicar(
        estado: EstadoExecucaoSpeedtest,
        progressoPercentual: Int,
        resultado: ResultadoSpeedtest?,
        erroMensagem: String?,
    ) {
        val progresso = min(100, max(0, progressoPercentual))
        val vel = velocidadeAtualInterna
        when (faseAtualInterna) {
            FaseSpeedtest.download -> if (vel > 0) {
                pontosAoVivoInternos.add(PontoAoVivo(t = System.currentTimeMillis(), dl = vel))
                if (pontosAoVivoInternos.size > 60) pontosAoVivoInternos.removeFirst()
            }
            FaseSpeedtest.upload -> if (vel > 0) {
                pontosAoVivoInternos.add(PontoAoVivo(t = System.currentTimeMillis(), ul = vel))
                if (pontosAoVivoInternos.size > 60) pontosAoVivoInternos.removeFirst()
            }
            else -> {}
        }
        mutableSnapshotFlow.value =
            SnapshotExecucaoSpeedtest(
                estado = estado,
                progressoPercentual = progresso,
                resultado = resultado,
                erroMensagem = erroMensagem,
                faseAtual = faseAtualInterna,
                velocidadeAtualMbps = vel,
                bytesConsumidos = bytesConsumidosTotal.get(),
                progressoGlobal = progresso / 100f,
                pontosAoVivo = pontosAoVivoInternos.toList(),
                rodadaAtual = rodadaAtualInterna,
                aguardandoProximaRodada = aguardandoProximaRodadaInterna,
                rodadasTriplo = rodadasTriploInternos.toList(),
            )
    }

    private fun throughputVazio(encerradaPor: String): ThroughputPhase =
        ThroughputPhase(
            throughputMbps = 0.0,
            peakMbps = 0.0,
            amostrasInstantaneas = emptyList(),
            bytesTotal = 0L,
            requisicoesSucesso = 0,
            requisicoesErro = 0,
            faseEncerradaPor = encerradaPor,
            throughputOrigem = "naoExecutado",
            ultimoErro = null,
        )

    // ── Data classes internos ─────────────────────────────────────────────────

    private data class LatencyPhase(
        val latenciaMs: Double,
        val jitterMs: Double,
        val perdaPercentual: Double,
        val totalAmostras: Int,
        val amostrasValidas: Int,
        val timeouts: Int,
    )

    private data class ThroughputPhase(
        val throughputMbps: Double,
        val peakMbps: Double,
        val amostrasInstantaneas: List<Double>,
        val bytesTotal: Long,
        val requisicoesSucesso: Int,
        val requisicoesErro: Int,
        val faseEncerradaPor: String,
        val throughputOrigem: String,
        val ultimoErro: String?,
    )

    private data class ThroughputCalculado(val mbps: Double, val origem: String)

    private data class Sample(val tMs: Int, val mbps: Double)

    private data class SpeedtestConfig(
        val pingCount: Int,
        val downloadDurationMs: Long,
        val uploadDurationMs: Long,
        val downloadPayloadBytes: Int,
        val uploadPayloadBytes: Int,
        val downloadInitialStreams: Int,
        val downloadMaxStreams: Int,
        val uploadInitialStreams: Int,
        val uploadMaxStreams: Int,
        val downloadWarmupMs: Int,
        val uploadWarmupMs: Int,
    ) {
        companion object {
            fun fromModo(modo: ModoSpeedtest): SpeedtestConfig =
                when (modo) {
                    // Espelho do TypeScript DOWNLOAD_CONFIG_FAST / DOWNLOAD_CONFIG_COMPLETE
                    ModoSpeedtest.fast ->
                        SpeedtestConfig(
                            pingCount = 15,
                            downloadDurationMs = 7_000L,
                            uploadDurationMs = 7_000L,
                            downloadPayloadBytes = 10_000_000,  // 10 MB
                            uploadPayloadBytes = 5_000_000,
                            downloadInitialStreams = 2,
                            downloadMaxStreams = 4,
                            uploadInitialStreams = 4,
                            uploadMaxStreams = 4,
                            downloadWarmupMs = 1_000,
                            uploadWarmupMs = 1_000,
                        )
                    ModoSpeedtest.complete ->
                        SpeedtestConfig(
                            pingCount = 25,
                            downloadDurationMs = 18_000L,
                            uploadDurationMs = 18_000L,
                            downloadPayloadBytes = 25_000_000,  // 25 MB
                            uploadPayloadBytes = 10_000_000,
                            downloadInitialStreams = 2,
                            downloadMaxStreams = 8,
                            uploadInitialStreams = 8,
                            uploadMaxStreams = 8,
                            downloadWarmupMs = 2_000,
                            uploadWarmupMs = 2_000,
                        )
                    // Triplo usa config idêntica ao fast — cada rodada é curta;
                    // consistência vem da média das 3.
                    ModoSpeedtest.triplo ->
                        SpeedtestConfig(
                            pingCount = 15,
                            downloadDurationMs = 7_000L,
                            uploadDurationMs = 7_000L,
                            downloadPayloadBytes = 10_000_000,
                            uploadPayloadBytes = 5_000_000,
                            downloadInitialStreams = 2,
                            downloadMaxStreams = 4,
                            uploadInitialStreams = 4,
                            uploadMaxStreams = 4,
                            downloadWarmupMs = 1_000,
                            uploadWarmupMs = 1_000,
                        )
                }
        }
    }

    private data class DnsProbeResult(
        val latencyMs: Int?,
        val resolverIp: String?,
        val provider: String?,
        val erroMensagem: String?,
    )
}
