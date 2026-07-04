package io.signallq.app.speedtest

import io.signallq.app.core.database.MedicaoDao
import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.MonitorRede
import io.signallq.app.core.telephony.MonitorTelephony
import io.signallq.app.feature.diagnostico.DiagnosticOrchestrator
import io.signallq.app.feature.diagnostico.DiagnosticReport
import io.signallq.app.feature.diagnostico.DiagnosticStatus
import io.signallq.app.feature.diagnostico.EstadoDiagnostico
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.ExecutorSpeedtest
import io.signallq.app.network.IspInfoCache
import io.signallq.app.ui.BancoOperadoras
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolve o valor a persistir no campo `operadoraMovel` da [MedicaoEntity].
 *
 * Rede movel: usa a operadora do SIM ([MonitorTelephony]). Wi-Fi: usa o ISP
 * publico ja resolvido ([IspInfoCache]), normalizado pelo catalogo
 * [BancoOperadoras] quando reconhecido — caso contrario mantem o nome cru do
 * ISP (GH#412: antes o campo saia sempre null em testes via Wi-Fi).
 */
internal fun resolverOperadorPersistencia(
    estadoConexao: EstadoConexao,
    operadoraMovelDetectada: String?,
    ispWifiDetectado: String?,
): String? =
    when (estadoConexao) {
        EstadoConexao.movel -> operadoraMovelDetectada
        EstadoConexao.wifi -> ispWifiDetectado?.let { raw -> BancoOperadoras.resolver(raw)?.nome ?: raw }
        else -> null
    }

/**
 * Responsável único por persistir resultados do speedtest no Room.
 *
 * Observa [ExecutorSpeedtest.snapshotFlow], detecta estado concluído, monta a
 * [MedicaoEntity] completa (com operadoraMovel via [MonitorTelephony] em rede
 * móvel, ou via [IspInfoCache] + [BancoOperadoras] em Wi-Fi — GH#412) e salva.
 *
 * Também observa [DiagnosticOrchestrator.snapshotFlow]: quando o diagnóstico local
 * é concluído após um speedtest, atualiza o registro com o texto do diagnóstico e
 * a origem ("local"). O diagnóstico por IA (SIG-113) atualiza via [atualizarDiagnosticoIa].
 *
 * Guard interno [ultimoResultadoPersistidoEpochMs] garante que o mesmo resultado
 * não seja salvo mais de uma vez, mesmo que o snapshotFlow emita múltiplas vezes
 * com o mesmo timestamp.
 */
@Singleton
class SpeedtestPersistenceCoordinator
    @Inject
    constructor(
        private val executorSpeedtest: ExecutorSpeedtest,
        private val medicaoDao: MedicaoDao,
        private val monitorTelephony: MonitorTelephony,
        private val monitorRede: MonitorRede,
        private val diagnosticOrchestrator: DiagnosticOrchestrator,
        private val ispInfoCache: IspInfoCache,
        private val applicationScope: CoroutineScope,
    ) {
        private var ultimoResultadoPersistidoEpochMs: Long? = null

        @Volatile
        var ultimaMedicaoId: String? = null
            private set

        private var aguardandoDiagnostico = false

        /**
         * Inicia a observação do snapshotFlow do speedtest e do orquestrador de diagnóstico.
         * Deve ser chamado uma única vez na inicialização do app.
         */
        fun iniciar() {
            applicationScope.launch {
                executorSpeedtest.snapshotFlow.collect { snapshot ->
                    if (snapshot.estado != EstadoExecucaoSpeedtest.concluido) return@collect
                    val resultado = snapshot.resultado ?: return@collect
                    if (ultimoResultadoPersistidoEpochMs == resultado.timestampEpochMs) return@collect

                    ultimoResultadoPersistidoEpochMs = resultado.timestampEpochMs

                    val novoId = UUID.randomUUID().toString()
                    try {
                        medicaoDao.salvar(
                            MedicaoEntity(
                                id = novoId,
                                timestampEpochMs = resultado.timestampEpochMs,
                                connectionType = monitorRede.snapshotFlow.value.estadoConexao.name,
                                connectionTypeStart = resultado.connectionTypeStart,
                                connectionTypeEnd = resultado.connectionTypeEnd,
                                contaminado = resultado.contaminado,
                                speedtestMode = resultado.modo.name,
                                specVersion = resultado.specVersion,
                                downloadMbps = resultado.downloadMbps,
                                uploadMbps = resultado.uploadMbps,
                                latencyMs = resultado.latenciaMs,
                                jitterMs = resultado.jitterMs,
                                perdaPercentual = resultado.perdaPercentual,
                                bufferbloatMs = resultado.bufferbloatMs,
                                packetLossSource = resultado.packetLossSource,
                                vereditoStreaming = resultado.diagnosticoQualidade.vereditoStreaming.name,
                                vereditoGamer = resultado.diagnosticoQualidade.vereditoGamer.name,
                                vereditoVideoChamada = resultado.diagnosticoQualidade.vereditoVideoChamada.name,
                                gargaloPrimario = resultado.diagnosticoQualidade.gargaloPrimario.name,
                                operadoraMovel =
                                    resolverOperadorPersistencia(
                                        estadoConexao = monitorRede.snapshotFlow.value.estadoConexao,
                                        operadoraMovelDetectada = monitorTelephony.snapshotFlow.value?.operadora,
                                        ispWifiDetectado = ispInfoCache.ultimoIspNome,
                                    ),
                                status = "completed",
                            ),
                        )
                        ultimaMedicaoId = novoId
                        aguardandoDiagnostico = true
                        Timber.d("SpeedtestPersistenceCoordinator: salvo ts=${resultado.timestampEpochMs} id=$novoId")
                    } catch (e: Exception) {
                        Timber.e(e, "SpeedtestPersistenceCoordinator: falha ao salvar medicao")
                    }
                }
            }

            applicationScope.launch {
                diagnosticOrchestrator.snapshotFlow.collect { snap ->
                    if (snap.estado != EstadoDiagnostico.concluido) return@collect
                    if (!aguardandoDiagnostico) return@collect
                    val relatorio = snap.relatorio ?: return@collect
                    val id = ultimaMedicaoId ?: return@collect

                    aguardandoDiagnostico = false

                    try {
                        val texto = relatorio.decisao.mensagemUsuario.ifBlank { null }
                        val problemas = extrairProblemasRelatorio(relatorio)
                        medicaoDao.atualizarDiagnostico(id, texto, "local", problemas)
                        medicaoDao.atualizarScore(id, relatorio.scoreConexao.toDouble())
                        Timber.d("SpeedtestPersistenceCoordinator: diagnostico local salvo id=$id score=${relatorio.scoreConexao}")
                    } catch (e: Exception) {
                        Timber.e(e, "SpeedtestPersistenceCoordinator: falha ao salvar diagnostico local")
                    }
                }
            }
        }

        /**
         * Atualiza o registro mais recente com o diagnóstico gerado por IA (SIG-113).
         * Sobrescreve o diagnóstico local, se existir.
         */
        suspend fun atualizarDiagnosticoIa(
            texto: String?,
            problemas: String?,
        ) {
            val id = ultimaMedicaoId ?: return
            try {
                medicaoDao.atualizarDiagnostico(id, texto, "ia", problemas)
                Timber.d("SpeedtestPersistenceCoordinator: diagnostico IA salvo id=$id")
            } catch (e: Exception) {
                Timber.e(e, "SpeedtestPersistenceCoordinator: falha ao salvar diagnostico IA")
            }
        }

        private fun extrairProblemasRelatorio(relatorio: DiagnosticReport): String? {
            val problemas =
                (
                    relatorio.wifiResultados +
                        relatorio.internetResultados +
                        relatorio.mobileResultados +
                        relatorio.fibraResultados +
                        relatorio.dnsResultados
                ).filter { it.status == DiagnosticStatus.critical || it.status == DiagnosticStatus.attention }
                    .map { it.titulo }
                    .distinct()
                    .take(5)
            return if (problemas.isEmpty()) null else problemas.joinToString(";")
        }
    }
