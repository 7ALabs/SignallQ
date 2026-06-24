package io.veloo.app.ui.viewmodel

import android.content.Context
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.veloo.app.core.database.SignallQDatabase
import io.veloo.app.core.network.EstadoConexao
import io.veloo.app.core.network.MonitorRedeAndroid
import io.veloo.app.feature.diagnostico.ConnectionType
import io.veloo.app.feature.diagnostico.ai.AiContextoRede
import io.veloo.app.feature.diagnostico.ai.AiDiagnosisRepository
import io.veloo.app.feature.diagnostico.ai.AiDispositivosInfo
import io.veloo.app.feature.diagnostico.ai.AiEvidence
import io.veloo.app.feature.diagnostico.ai.AiHistoricoResumo
import io.veloo.app.feature.diagnostico.ai.AiMetricasAtuais
import io.veloo.app.feature.diagnostico.ai.AiRedeInfo
import io.veloo.app.feature.diagnostico.ai.AiTesteHistorico
import io.veloo.app.feature.diagnostico.ai.DiagnosisAiContext
import io.veloo.app.feature.diagnostico.chat.ChatDiagnosticoIaRepository
import io.veloo.app.feature.diagnostico.chat.ChatMensagem
import io.veloo.app.feature.diagnostico.chat.CotaIaRepository
import io.veloo.app.feature.diagnostico.chat.CotaSnapshot
import io.veloo.app.feature.diagnostico.chat.PapelChatMensagem
import io.veloo.app.feature.diagnostico.chat.ResultadoCota
import io.veloo.app.feature.diagnostico.chat.SessaoChatDiagnostico
import io.veloo.app.feature.diagnostico.chat.StatusChatMensagem
import io.veloo.app.feature.diagnostico.chat.TipoDiagnostico
import io.veloo.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.veloo.app.feature.speedtest.ExecutorSpeedtest
import io.veloo.app.feature.speedtest.FaseSpeedtest
import io.veloo.app.feature.speedtest.ModoSpeedtest
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.net.SocketTimeoutException
import java.util.UUID
import javax.inject.Inject

// =============================================================================
// UiState
// =============================================================================

enum class EstadoChatDiagnostico {
    Idle,
    AguardandoIa,
    ExecutandoTeste,
    Streaming,
    CotaExcedida,
    ErroModelo,
    ErroRede,
}

data class ChatDiagUiState(
    val sessaoAtual: SessaoChatDiagnostico? = null,
    val mensagens: List<ChatMensagem> = emptyList(),
    val sessoesAnteriores: List<SessaoChatDiagnostico> = emptyList(),
    val estado: EstadoChatDiagnostico = EstadoChatDiagnostico.Idle,
    val cota: CotaSnapshot? = null,
    val modeloDisplayName: String = "o modelo de IA",
    val opcoesIniciaisVisiveis: Boolean = true,
    val chipExecutarAgoraVisivel: Boolean = false,
    val drawerAberto: Boolean = false,
    val mensagemEmDigitacao: String = "",
)

// =============================================================================
// ViewModel
// =============================================================================

/**
 * ViewModel para ChatDiagnosticoIaScreen.
 *
 * Não usa ProvedorModeloIaConfig — não existe no projeto. O modeloDisplayName
 * é atualizado dinamicamente quando a IA responde (via ModeloIa.nomeExibicao).
 * Fallback: "o modelo de IA" (especificado pela Lia no design-specs).
 *
 * AiDiagnosisRepository injetada pelo Hilt como @Singleton via DiagnosticoModule.
 * Antes era instanciada localmente via lazy (terceira instancia independente, alem
 * das instancias em MainViewModel e SignallQOrchestrator).
 *
 * CotaIaRepository e ChatDiagnosticoIaRepository ainda instanciados localmente
 * com Context e SignallQDatabase (nao sao @Singleton e dependem de Context/DB).
 */
@HiltViewModel
class ChatDiagnosticoIaViewModel
    @Inject
    constructor(
        @ApplicationContext private val context: Context,
        private val executorSpeedtest: ExecutorSpeedtest,
        private val bancoDados: SignallQDatabase,
        /** AiDiagnosisRepository injetada pelo Hilt como @Singleton (DiagnosticoModule).
         *  Garante que o mesmo cache de diagnostico e compartilhado com MainViewModel
         *  e SignallQOrchestrator. */
        private val aiRepository: AiDiagnosisRepository,
    ) : ViewModel() {
        companion object {
            private const val TAG = "ChatDiagnosticoIaVM"
            private const val MODELO_DISPLAY_NAME = "Qwen3 30B"
        }

        // -------------------------------------------------------------------------
        // Dependências não-Hilt
        // -------------------------------------------------------------------------

        private val chatRepository by lazy { ChatDiagnosticoIaRepository(bancoDados) }

        private val cotaRepository by lazy { CotaIaRepository(context) }

        private var monitorRedeInicializado = false
        private val monitorRede by lazy {
            MonitorRedeAndroid(context).also {
                it.iniciar()
                monitorRedeInicializado = true
            }
        }

        // -------------------------------------------------------------------------
        // State
        // -------------------------------------------------------------------------

        private val _uiState = MutableStateFlow(ChatDiagUiState())
        val uiState: StateFlow<ChatDiagUiState> = _uiState.asStateFlow()

        // Job do speedtest atual — para poder cancelar
        private var speedtestJob: Job? = null

        // Mensagem placeholder de streaming atual — para atualizar tokens in-place
        private var mensagemStreamingAtual: ChatMensagem? = null

        // Último contexto enviado à IA — reutilizado em follow-ups para que a IA
        // tenha acesso aos dados de rede mesmo em perguntas livres do usuário.
        private var ultimoContextoAnalise: DiagnosisAiContext? = null

        // Rastreia se a mensagem de download já foi inserida durante o novoTeste
        private var msgDownloadJaInserida = false
        private var msgUploadJaInserida = false

        // -------------------------------------------------------------------------
        // Lifecycle
        // -------------------------------------------------------------------------

        override fun onCleared() {
            super.onCleared()
            // monitorRede é lazy — encerrar apenas se foi inicializado (evita criar só pra encerrar).
            if (monitorRedeInicializado) {
                try {
                    monitorRede.encerrar()
                } catch (_: Exception) {
                }
            }
        }

        // -------------------------------------------------------------------------
        // Contexto completo de rede
        // -------------------------------------------------------------------------

        /**
         * Coleta o contexto mais completo possível de dados de rede, dispositivo e
         * histórico de testes para enviar à IA.
         *
         * Sempre busca dados frescos do monitorRede + banco. Nenhuma análise local —
         * apenas dados brutos.
         */
        private suspend fun coletarContextoCompleto(): DiagnosisAiContext {
            val snapshot = monitorRede.snapshotFlow.value

            // ConnectionType
            val connectionType =
                when (snapshot.estadoConexao) {
                    EstadoConexao.wifi -> ConnectionType.wifi
                    EstadoConexao.movel -> ConnectionType.mobile
                    EstadoConexao.ethernet -> ConnectionType.ethernet
                    else -> ConnectionType.wifi
                }

            // Wi-Fi context
            val wifi = snapshot.wifiLinkSnapshot
            val contextoRede =
                AiContextoRede(
                    tipoConexao = connectionType.name,
                    ssid = wifi?.ssid,
                    bssid = wifi?.bssid,
                    rssi = wifi?.rssiDbm,
                    frequenciaMhz = wifi?.frequenciaMhz,
                    linkSpeedMbps = wifi?.linkSpeedMbps,
                    padraoWifi = wifi?.padraoWifi,
                    bandaWifi = wifi?.frequenciaMhz?.let { frequenciaParaBanda(it) },
                    canal = wifi?.frequenciaMhz?.let { frequenciaParaCanal(it) },
                    privateDnsAtivo = snapshot.privateDnsAtivo,
                    privateDnsHostname = snapshot.privateDnsHostname,
                )

            // DNS / rede info
            val rede =
                AiRedeInfo(
                    dnsResolverIp = snapshot.dnsServidores.firstOrNull(),
                )

            // Dispositivo
            val dispositivos =
                AiDispositivosInfo(
                    fabricante = Build.MANUFACTURER,
                    modelo = Build.MODEL,
                    sistema = "Android",
                    versaoSO = Build.VERSION.RELEASE,
                )

            // Últimos 3 speedtests do banco
            val ultimasMedicoes = bancoDados.medicaoDao().observarUltimas(3).first()
            val ultimosTestes =
                ultimasMedicoes.map { m ->
                    AiTesteHistorico(
                        timestampEpochMs = m.timestampEpochMs,
                        downloadMbps = m.downloadMbps,
                        uploadMbps = m.uploadMbps,
                        latenciaMs = m.latencyMs,
                        jitterMs = m.jitterMs,
                        perdaPercentual = m.perdaPercentual,
                        connectionType = m.connectionType,
                    )
                }
            val historico =
                ultimosTestes.takeIf { it.isNotEmpty() }?.let {
                    AiHistoricoResumo(ultimosTestes = it)
                }

            // Métricas atuais: a medição mais recente vira metricasAtuais
            val medicaoRecente = ultimasMedicoes.firstOrNull()
            val metricasAtuais =
                medicaoRecente?.let { m ->
                    AiMetricasAtuais(
                        downloadMbps = m.downloadMbps,
                        uploadMbps = m.uploadMbps,
                        latenciaMs = m.latencyMs,
                        jitterMs = m.jitterMs,
                        perdaPacotesPercentual = m.perdaPercentual,
                        bufferbloatMs = m.bufferbloatMs,
                    )
                }

            // Evidências brutas da medição mais recente
            val evidencias = mutableListOf<AiEvidence>()
            medicaoRecente?.let { m ->
                m.downloadMbps?.let { evidencias.add(AiEvidence("download", "$it Mbps")) }
                m.uploadMbps?.let { evidencias.add(AiEvidence("upload", "$it Mbps")) }
                m.latencyMs?.let { evidencias.add(AiEvidence("latencia", "$it ms")) }
                m.jitterMs?.let { evidencias.add(AiEvidence("jitter", "$it ms")) }
                m.perdaPercentual?.let { evidencias.add(AiEvidence("perda_pacotes", "$it%")) }
            }

            // Tom derivado das métricas
            val instrucaoTom = calcularInstrucaoTom(metricasAtuais)

            return DiagnosisAiContext(
                generatedAtEpochMs = System.currentTimeMillis(),
                connectionType = connectionType,
                metricasAtuais = metricasAtuais,
                contextoRede = contextoRede,
                rede = rede,
                dispositivos = dispositivos,
                historico = historico,
                evidencias = evidencias,
                instrucaoTom = instrucaoTom,
            )
        }

        private fun frequenciaParaCanal(freq: Int): Int? =
            when {
                freq in 2412..2484 -> (freq - 2412) / 5 + 1
                freq in 5170..5825 -> (freq - 5000) / 5
                freq in 5955..7115 -> (freq - 5955) / 5 + 1
                else -> null
            }

        private fun frequenciaParaBanda(freq: Int): String? =
            when {
                freq < 5000 -> "2.4 GHz"
                freq < 5925 -> "5 GHz"
                freq >= 5925 -> "6 GHz"
                else -> null
            }

        /**
         * Deriva instrução de tom a partir das métricas.
         * Replica a lógica de DiagnosisAiContextFactory.buildToneInstruction (internal).
         * Thresholds: jitter > 50ms, perda >= 2%, RTT gateway > 150ms.
         */
        private fun calcularInstrucaoTom(metricas: AiMetricasAtuais?): String? {
            if (metricas == null) return null
            var badCount = 0
            if ((metricas.jitterMs ?: 0.0) > 50.0) badCount++
            if ((metricas.perdaPacotesPercentual ?: 0.0) >= 2.0) badCount++
            if ((metricas.rttGatewayMs ?: 0) > 150) badCount++
            return when {
                badCount >= 2 -> "Detectei..."
                badCount == 1 -> "Sua conexão está funcionando, mas..."
                else -> "Tudo dentro do esperado."
            }
        }

        // -------------------------------------------------------------------------
        // Init
        // -------------------------------------------------------------------------

        init {
            _uiState.update { it.copy(modeloDisplayName = MODELO_DISPLAY_NAME) }
            iniciarObservadores()
            onNovaSessao()
        }

        private fun iniciarObservadores() {
            // Observa sessões anteriores para o drawer
            viewModelScope.launch {
                chatRepository.observarSessoes().collect { sessoes ->
                    _uiState.update { it.copy(sessoesAnteriores = sessoes) }
                }
            }

            // Observa cota para o banner de limite
            viewModelScope.launch {
                cotaRepository.observarCota().collect { cota ->
                    _uiState.update { it.copy(cota = cota) }
                }
            }
        }

        // -------------------------------------------------------------------------
        // Eventos públicos
        // -------------------------------------------------------------------------

        fun onNovaSessao() {
            viewModelScope.launch {
                val sessao = chatRepository.criarSessaoVazia()
                _uiState.update { st ->
                    st.copy(
                        sessaoAtual = sessao,
                        mensagens = emptyList(),
                        opcoesIniciaisVisiveis = true,
                        chipExecutarAgoraVisivel = false,
                        estado = EstadoChatDiagnostico.Idle,
                        drawerAberto = false,
                    )
                }
                inserirMensagensBoasVindas(sessao.id)
            }
        }

        fun onEscolherOpcao(tipo: TipoDiagnostico) {
            viewModelScope.launch {
                _uiState.update { it.copy(opcoesIniciaisVisiveis = false) }

                // Pré-cota — verifica antes de qualquer chamada IA
                val resultadoCota = cotaRepository.podeAnalisar()
                if (resultadoCota is ResultadoCota.Excedida) {
                    _uiState.update { it.copy(estado = EstadoChatDiagnostico.CotaExcedida) }
                    return@launch
                }

                val sessaoId = _uiState.value.sessaoAtual?.id ?: return@launch

                when (tipo) {
                    TipoDiagnostico.ultimoTeste -> lidarComUltimoTeste(sessaoId)
                    TipoDiagnostico.novoTeste -> lidarComNovoTeste(sessaoId)
                    TipoDiagnostico.historico -> lidarComHistorico(sessaoId)
                }
            }
        }

        fun onEnviarMensagem(texto: String) {
            if (texto.isBlank()) return
            val sessaoId = _uiState.value.sessaoAtual?.id ?: return

            viewModelScope.launch {
                // Pré-cota
                val resultadoCota = cotaRepository.podeAnalisar()
                if (resultadoCota is ResultadoCota.Excedida) {
                    _uiState.update { it.copy(estado = EstadoChatDiagnostico.CotaExcedida) }
                    return@launch
                }

                // Limpa o draft
                _uiState.update { it.copy(mensagemEmDigitacao = "") }

                // Insere mensagem do usuário
                val msgUsuario =
                    criarMensagem(
                        sessaoId = sessaoId,
                        papel = PapelChatMensagem.usuario,
                        conteudo = texto,
                        status = StatusChatMensagem.concluido,
                    )
                chatRepository.salvarMensagem(msgUsuario)
                adicionarMensagemAoState(msgUsuario)

                // Se sessão ainda tem título padrão e é primeira mensagem do usuário → renomeia
                val sessaoAtual = _uiState.value.sessaoAtual
                if (sessaoAtual != null && sessaoAtual.titulo == "Nova conversa") {
                    val novoTitulo = chatRepository.derivarTituloDe(texto)
                    chatRepository.renomearSessao(sessaoId, novoTitulo)
                    _uiState.update { st ->
                        st.copy(
                            sessaoAtual = st.sessaoAtual?.copy(titulo = novoTitulo),
                        )
                    }
                }

                // Follow-up reutiliza o contexto da análise inicial da sessão.
                // Não re-coleta — a rede não muda entre mensagens.
                val base = ultimoContextoAnalise
                val contexto =
                    if (base != null) {
                        base.copy(feedbackUsuario = texto)
                    } else {
                        // Sem análise prévia na sessão — coleta do zero.
                        coletarContextoCompleto().copy(feedbackUsuario = texto)
                    }

                iniciarAnaliseStreaming(sessaoId = sessaoId, context = contexto, tipo = null)
            }
        }

        fun onAbrirSessao(id: String) {
            viewModelScope.launch {
                // Coleta as mensagens da sessão
                val mensagens = chatRepository.observarMensagens(id).first()
                val sessao = chatRepository.observarSessao(id).first() ?: return@launch

                _uiState.update { st ->
                    st.copy(
                        sessaoAtual = sessao,
                        mensagens = mensagens,
                        opcoesIniciaisVisiveis = false, // sessão antiga — não mostra opções
                        chipExecutarAgoraVisivel = false,
                        estado = EstadoChatDiagnostico.Idle,
                        drawerAberto = false,
                    )
                }
            }
        }

        fun onApagarSessao(id: String) {
            viewModelScope.launch {
                val eraSessaoAtual = _uiState.value.sessaoAtual?.id == id
                chatRepository.apagarSessao(id)

                if (eraSessaoAtual) {
                    onNovaSessao()
                }
            }
        }

        fun onRenomearSessao(
            id: String,
            novoTitulo: String,
        ) {
            viewModelScope.launch {
                chatRepository.renomearSessao(id, novoTitulo)
                // Atualiza sessão atual se for ela
                if (_uiState.value.sessaoAtual?.id == id) {
                    _uiState.update { st ->
                        st.copy(sessaoAtual = st.sessaoAtual?.copy(titulo = novoTitulo))
                    }
                }
            }
        }

        fun onToggleDrawer() {
            _uiState.update { it.copy(drawerAberto = !it.drawerAberto) }
        }

        fun onAtualizarDraft(texto: String) {
            _uiState.update { it.copy(mensagemEmDigitacao = texto) }
        }

        fun onCancelarAcaoAtual() {
            if (_uiState.value.estado == EstadoChatDiagnostico.ExecutandoTeste) {
                speedtestJob?.cancel()
                executorSpeedtest.cancelar()
                _uiState.update { it.copy(estado = EstadoChatDiagnostico.Idle) }
            }
        }

        // -------------------------------------------------------------------------
        // Opção: último teste
        // -------------------------------------------------------------------------

        private suspend fun lidarComUltimoTeste(sessaoId: String) {
            val ultimaMedicao =
                bancoDados
                    .medicaoDao()
                    .observarUltimas(1)
                    .first()
                    .firstOrNull()

            if (ultimaMedicao == null) {
                // Não há teste — informa e oferece executar novo
                val msgSemTeste =
                    criarMensagem(
                        sessaoId = sessaoId,
                        papel = PapelChatMensagem.assistente,
                        conteudo =
                            "Não encontrei um teste recente para analisar. " +
                                "Posso executar um novo teste agora e usar os dados para montar o diagnóstico.",
                        status = StatusChatMensagem.concluido,
                        isLocal = true,
                    )
                chatRepository.salvarMensagem(msgSemTeste)
                adicionarMensagemAoState(msgSemTeste)
                _uiState.update { it.copy(chipExecutarAgoraVisivel = true) }
                return
            }

            val contexto = coletarContextoCompleto()
            iniciarAnaliseStreaming(sessaoId = sessaoId, context = contexto, tipo = TipoDiagnostico.ultimoTeste)
        }

        // -------------------------------------------------------------------------
        // Opção: novo teste
        // -------------------------------------------------------------------------

        private fun lidarComNovoTeste(sessaoId: String) {
            msgDownloadJaInserida = false
            msgUploadJaInserida = false

            speedtestJob =
                viewModelScope.launch {
                    // Mensagem 1 — início
                    val msg1 =
                        criarMensagem(
                            sessaoId = sessaoId,
                            papel = PapelChatMensagem.assistente,
                            conteudo = "Iniciando o teste. Primeiro vou medir a velocidade de download.",
                            status = StatusChatMensagem.concluido,
                            isLocal = true,
                        )
                    chatRepository.salvarMensagem(msg1)
                    adicionarMensagemAoState(msg1)
                    _uiState.update { it.copy(estado = EstadoChatDiagnostico.ExecutandoTeste) }

                    // Dispara o speedtest em paralelo
                    launch {
                        try {
                            executorSpeedtest.executar(ModoSpeedtest.fast)
                        } catch (e: Exception) {
                            Timber.tag(TAG).e(e, "Erro ao executar speedtest")
                        }
                    }

                    // Coleta o snapshotFlow e insere mensagens conforme progresso
                    var ultimoResultadoProcessado: Long? = null
                    executorSpeedtest.snapshotFlow.collect { snapshot ->
                        when (snapshot.estado) {
                            EstadoExecucaoSpeedtest.executando -> {
                                // Mensagem 2 — download concluído, começando upload
                                if (!msgDownloadJaInserida &&
                                    snapshot.faseAtual == FaseSpeedtest.upload &&
                                    snapshot.velocidadeAtualMbps > 0
                                ) {
                                    msgDownloadJaInserida = true
                                    val downloadMbps = snapshot.resultado?.downloadMbps
                                    val conteudo =
                                        if (downloadMbps != null) {
                                            "Download: ${String.format("%.0f", downloadMbps)} Mbps. Agora medindo o upload."
                                        } else {
                                            "Download concluído. Medindo upload agora."
                                        }
                                    val msg2 =
                                        criarMensagem(
                                            sessaoId = sessaoId,
                                            papel = PapelChatMensagem.assistente,
                                            conteudo = conteudo,
                                            status = StatusChatMensagem.concluido,
                                            isLocal = true,
                                        )
                                    chatRepository.salvarMensagem(msg2)
                                    adicionarMensagemAoState(msg2)
                                }
                            }

                            EstadoExecucaoSpeedtest.concluido -> {
                                val resultado =
                                    snapshot.resultado ?: run {
                                        inserirMensagemErroSpeedtest(sessaoId)
                                        return@collect
                                    }

                                // Previne processamento duplo
                                if (ultimoResultadoProcessado == resultado.timestampEpochMs) return@collect
                                ultimoResultadoProcessado = resultado.timestampEpochMs

                                // Mensagem 3 — upload concluído, verificando latência (inserida aqui ao concluir)
                                if (!msgUploadJaInserida) {
                                    msgUploadJaInserida = true
                                    val conteudo =
                                        if (resultado.uploadMbps > 0) {
                                            "Upload: ${String.format(
                                                "%.0f",
                                                resultado.uploadMbps,
                                            )} Mbps. Verificando latência, estabilidade e outros sinais da rede."
                                        } else {
                                            "Upload concluído. Verificando latência e estabilidade."
                                        }
                                    val msg3 =
                                        criarMensagem(
                                            sessaoId = sessaoId,
                                            papel = PapelChatMensagem.assistente,
                                            conteudo = conteudo,
                                            status = StatusChatMensagem.concluido,
                                            isLocal = true,
                                        )
                                    chatRepository.salvarMensagem(msg3)
                                    adicionarMensagemAoState(msg3)
                                }

                                // Persistência delegada ao SpeedtestPersistenceCoordinator (issues #184/#185).

                                // Mensagem 4 — dados coletados, analisando
                                val displayName = _uiState.value.modeloDisplayName
                                val msg4 =
                                    criarMensagem(
                                        sessaoId = sessaoId,
                                        papel = PapelChatMensagem.assistente,
                                        conteudo = "Dados coletados. Analisando com $displayName.",
                                        status = StatusChatMensagem.concluido,
                                        isLocal = true,
                                    )
                                chatRepository.salvarMensagem(msg4)
                                adicionarMensagemAoState(msg4)

                                // Coleta contexto completo de rede e sobrescreve metricasAtuais
                                // com os dados fresh do speedtest recém concluído (inclui peak,
                                // stability, bufferbloat que o banco pode ainda não ter persistido).
                                val metricasFresh =
                                    AiMetricasAtuais(
                                        downloadMbps = resultado.downloadMbps,
                                        uploadMbps = resultado.uploadMbps,
                                        latenciaMs = resultado.latenciaMs,
                                        jitterMs = resultado.jitterMs,
                                        perdaPacotesPercentual = resultado.perdaPercentual,
                                        bufferbloatMs = resultado.bufferbloatMs,
                                        severidadeBufferbloat = resultado.severidadeBufferbloat.name,
                                        stabilityScore = resultado.stabilityScore,
                                        peakDownloadMbps = resultado.peakDownloadMbps,
                                        peakUploadMbps = resultado.peakUploadMbps,
                                        latencyDownloadMs = resultado.latencyDownloadMs,
                                        latencyUploadMs = resultado.latencyUploadMs,
                                        packetLossSource = resultado.packetLossSource,
                                    )
                                val contexto =
                                    coletarContextoCompleto().copy(
                                        generatedAtEpochMs = resultado.timestampEpochMs,
                                        metricasAtuais = metricasFresh,
                                        instrucaoTom = calcularInstrucaoTom(metricasFresh),
                                    )

                                _uiState.update { it.copy(estado = EstadoChatDiagnostico.Idle) }
                                iniciarAnaliseStreaming(
                                    sessaoId = sessaoId,
                                    context = contexto,
                                    tipo = TipoDiagnostico.novoTeste,
                                )
                            }

                            EstadoExecucaoSpeedtest.erro -> {
                                inserirMensagemErroSpeedtest(sessaoId)
                            }

                            EstadoExecucaoSpeedtest.idle -> { /* aguarda */ }
                        }
                    }
                }
        }

        private suspend fun inserirMensagemErroSpeedtest(sessaoId: String) {
            val msgErro =
                criarMensagem(
                    sessaoId = sessaoId,
                    papel = PapelChatMensagem.assistente,
                    conteudo = "Não consegui concluir o teste de velocidade. Verifique sua conexão e tente novamente.",
                    status = StatusChatMensagem.falhou,
                    isLocal = true,
                    codigoErro = "speedtest_error",
                )
            chatRepository.salvarMensagem(msgErro)
            adicionarMensagemAoState(msgErro)
            _uiState.update { it.copy(estado = EstadoChatDiagnostico.Idle) }
        }

        // -------------------------------------------------------------------------
        // Opção: histórico
        // -------------------------------------------------------------------------

        private suspend fun lidarComHistorico(sessaoId: String) {
            val medicoes = bancoDados.medicaoDao().observarUltimas(7).first()

            if (medicoes.isEmpty()) {
                val msgVazio =
                    criarMensagem(
                        sessaoId = sessaoId,
                        papel = PapelChatMensagem.assistente,
                        conteudo = "Não encontrei histórico suficiente para analisar. O melhor caminho é executar um novo teste agora.",
                        status = StatusChatMensagem.concluido,
                        isLocal = true,
                    )
                chatRepository.salvarMensagem(msgVazio)
                adicionarMensagemAoState(msgVazio)
                _uiState.update { it.copy(chipExecutarAgoraVisivel = true) }
                return
            }

            // Informa quantos testes foram encontrados
            val n = medicoes.size
            val msgContagem =
                if (n < 7) {
                    criarMensagem(
                        sessaoId = sessaoId,
                        papel = PapelChatMensagem.assistente,
                        conteudo =
                            "Encontrei $n ${if (n == 1) "teste recente" else "testes recentes"}. " +
                                "Já é possível fazer uma análise básica, mas quanto mais testes existirem, " +
                                "melhor fica a comparação.",
                        status = StatusChatMensagem.concluido,
                        isLocal = true,
                    )
                } else {
                    criarMensagem(
                        sessaoId = sessaoId,
                        papel = PapelChatMensagem.assistente,
                        conteudo =
                            "Encontrei $n testes recentes no seu histórico. " +
                                "Vou comparar os resultados para identificar variações de velocidade, " +
                                "latência, estabilidade e possíveis padrões de queda.",
                        status = StatusChatMensagem.concluido,
                        isLocal = true,
                    )
                }
            chatRepository.salvarMensagem(msgContagem)
            adicionarMensagemAoState(msgContagem)

            // coletarContextoCompleto() já inclui os últimos 3 testes no historico.
            // Para o modo histórico, enriquecemos o historico com os 7 testes que já
            // buscamos para exibir a contagem ao usuário.
            val ultimosTestes7 =
                medicoes.map { m ->
                    AiTesteHistorico(
                        timestampEpochMs = m.timestampEpochMs,
                        downloadMbps = m.downloadMbps,
                        uploadMbps = m.uploadMbps,
                        latenciaMs = m.latencyMs,
                        jitterMs = m.jitterMs,
                        perdaPercentual = m.perdaPercentual,
                        connectionType = m.connectionType,
                    )
                }
            val contexto =
                coletarContextoCompleto().copy(
                    historico = AiHistoricoResumo(ultimosTestes = ultimosTestes7),
                )

            iniciarAnaliseStreaming(sessaoId = sessaoId, context = contexto, tipo = TipoDiagnostico.historico)
        }

        // -------------------------------------------------------------------------
        // Streaming
        // -------------------------------------------------------------------------

        /**
         * Dispara a análise de streaming com a IA.
         *
         * Fluxo:
         * 1. Insere mensagem placeholder com status=streaming, conteudo="".
         * 2. Seta estado = aguardandoIa.
         * 3. Coleta tokens do flow SSE.
         * 4. Para cada token: muda para streaming, faz append no placeholder.
         * 5. Ao terminar: status=concluido, estado=idle, registra análise na cota.
         * 6. Em qualquer erro: insere mensagem humanizada, seta estado de erro.
         */
        private suspend fun iniciarAnaliseStreaming(
            sessaoId: String,
            context: DiagnosisAiContext,
            tipo: TipoDiagnostico?,
        ) {
            if (tipo != null) {
                ultimoContextoAnalise = context
            }

            // Insere placeholder da resposta da IA
            val placeholder =
                criarMensagem(
                    sessaoId = sessaoId,
                    papel = PapelChatMensagem.assistente,
                    conteudo = "",
                    status = StatusChatMensagem.streaming,
                    tipoDiagnostico = tipo,
                )
            chatRepository.salvarMensagem(placeholder)
            adicionarMensagemAoState(placeholder)
            mensagemStreamingAtual = placeholder

            _uiState.update { it.copy(estado = EstadoChatDiagnostico.AguardandoIa) }

            var conteudoAcumulado = ""
            var primeiroTokenRecebido = false

            try {
                aiRepository.explainDiagnosisStream(context).collect { token ->
                    if (!primeiroTokenRecebido) {
                        primeiroTokenRecebido = true
                        _uiState.update { it.copy(estado = EstadoChatDiagnostico.Streaming) }
                    }

                    conteudoAcumulado += token

                    // Atualiza a mensagem placeholder no state e no banco
                    val msgAtualizada =
                        placeholder.copy(
                            conteudo = conteudoAcumulado,
                            status = StatusChatMensagem.streaming,
                        )
                    mensagemStreamingAtual = msgAtualizada
                    atualizarMensagemNoState(msgAtualizada)
                    chatRepository.atualizarMensagem(msgAtualizada)
                }

                // Stream concluído com sucesso
                if (conteudoAcumulado.isBlank()) {
                    // Nenhum token recebido — trata como resposta incompleta
                    val msgIncompleta =
                        placeholder.copy(
                            conteudo = "Recebi uma resposta incompleta. Os dados do teste foram preservados, mas recomendo tentar o diagnóstico novamente.",
                            status = StatusChatMensagem.falhou,
                            codigoErro = "stream_incomplete",
                        )
                    chatRepository.atualizarMensagem(msgIncompleta)
                    atualizarMensagemNoState(msgIncompleta)
                    _uiState.update { it.copy(estado = EstadoChatDiagnostico.Idle) }
                    return
                }

                val msgConcluida =
                    placeholder.copy(
                        conteudo = conteudoAcumulado,
                        status = StatusChatMensagem.concluido,
                    )
                chatRepository.atualizarMensagem(msgConcluida)
                atualizarMensagemNoState(msgConcluida)
                mensagemStreamingAtual = null

                // Registra análise na cota SOMENTE após sucesso
                cotaRepository.registrarAnalise()
                _uiState.update { it.copy(estado = EstadoChatDiagnostico.Idle) }
            } catch (e: SocketTimeoutException) {
                Timber.tag(TAG).e(e, "Timeout no streaming da IA")
                val msgTimeout =
                    placeholder.copy(
                        conteudo =
                            "A análise demorou mais que o esperado e foi interrompida. " +
                                "Você pode tentar novamente — os dados do teste foram preservados.",
                        status = StatusChatMensagem.falhou,
                        codigoErro = "timeout",
                    )
                chatRepository.atualizarMensagem(msgTimeout)
                atualizarMensagemNoState(msgTimeout)
                _uiState.update { it.copy(estado = EstadoChatDiagnostico.ErroRede) }
            } catch (e: IOException) {
                Timber.tag(TAG).e(e, "Erro de rede no streaming da IA")
                val msgErroRede =
                    placeholder.copy(
                        conteudo = "Não consegui conectar ao serviço de diagnóstico. Verifique sua conexão e tente novamente.",
                        status = StatusChatMensagem.falhou,
                        codigoErro = "network_error",
                    )
                chatRepository.atualizarMensagem(msgErroRede)
                atualizarMensagemNoState(msgErroRede)
                _uiState.update { it.copy(estado = EstadoChatDiagnostico.ErroRede) }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Erro inesperado no streaming da IA: ${e::class.simpleName}")
                val (mensagemErro, codigoErro, estadoErro) = classificarErro(e, conteudoAcumulado)
                val msgErro =
                    placeholder.copy(
                        conteudo = mensagemErro,
                        status = StatusChatMensagem.falhou,
                        codigoErro = codigoErro,
                    )
                chatRepository.atualizarMensagem(msgErro)
                atualizarMensagemNoState(msgErro)
                _uiState.update { it.copy(estado = estadoErro) }
            }
        }

        /**
         * Classifica erros da IA em mensagens humanizadas e estado de erro correspondente.
         *
         * Retorna Triple(mensagem, codigoErro, estadoChatDiagnostico).
         */
        private fun classificarErro(
            e: Exception,
            conteudoAcumulado: String,
        ): Triple<String, String, EstadoChatDiagnostico> {
            val mensagem503 =
                e.message?.let { msg ->
                    msg.contains("503") ||
                        msg.contains("unavailable", ignoreCase = true) ||
                        msg.contains("Service Unavailable", ignoreCase = true)
                } ?: false

            return when {
                mensagem503 ->
                    Triple(
                        "No momento o ${_uiState.value.modeloDisplayName} está indisponível. Tente novamente em alguns minutos.",
                        "model_unavailable_503",
                        EstadoChatDiagnostico.ErroModelo,
                    )

                conteudoAcumulado.isNotBlank() ->
                    Triple(
                        "Recebi uma resposta incompleta. Os dados do teste foram preservados, mas recomendo tentar o diagnóstico novamente.",
                        "stream_incomplete",
                        EstadoChatDiagnostico.Idle,
                    )

                else ->
                    Triple(
                        "Algo deu errado ao processar o diagnóstico. Tente novamente. Se o problema persistir, os dados do teste foram salvos e você pode tentar mais tarde.",
                        "unexpected_error",
                        EstadoChatDiagnostico.Idle,
                    )
            }
        }

        // -------------------------------------------------------------------------
        // Helpers de mensagens
        // -------------------------------------------------------------------------

        private fun inserirMensagensBoasVindas(sessaoId: String) {
            viewModelScope.launch {
                val msg1 =
                    criarMensagem(
                        sessaoId = sessaoId,
                        papel = PapelChatMensagem.assistente,
                        conteudo =
                            "Olá. Sou o Diagnóstico IA do SignallQ.\n\n" +
                                "Posso ajudar você a entender problemas de internet, Wi-Fi, velocidade, latência, " +
                                "perda de pacote e qualidade da sua rede. Trabalho apenas com assuntos relacionados " +
                                "à sua conexão — não sou um assistente geral e posso cometer erros. Use minhas " +
                                "respostas como apoio, não como verdade absoluta.",
                        status = StatusChatMensagem.concluido,
                        isLocal = true,
                    )
                chatRepository.salvarMensagem(msg1)
                adicionarMensagemAoState(msg1)

                val msg2 =
                    criarMensagem(
                        sessaoId = sessaoId,
                        papel = PapelChatMensagem.assistente,
                        conteudo = "Como você quer começar?",
                        status = StatusChatMensagem.concluido,
                        isLocal = true,
                    )
                chatRepository.salvarMensagem(msg2)
                adicionarMensagemAoState(msg2)
            }
        }

        private fun criarMensagem(
            sessaoId: String,
            papel: PapelChatMensagem,
            conteudo: String,
            status: StatusChatMensagem,
            tipoDiagnostico: TipoDiagnostico? = null,
            isLocal: Boolean = false,
            codigoErro: String? = null,
        ): ChatMensagem =
            ChatMensagem(
                id = UUID.randomUUID().toString(),
                sessionId = sessaoId,
                papel = papel,
                conteudo = conteudo,
                criadoEmEpochMs = System.currentTimeMillis(),
                status = status,
                tipoDiagnostico = tipoDiagnostico,
                isLocal = isLocal,
                codigoErro = codigoErro,
            )

        private fun adicionarMensagemAoState(mensagem: ChatMensagem) {
            _uiState.update { st ->
                st.copy(mensagens = st.mensagens + mensagem)
            }
        }

        private fun atualizarMensagemNoState(mensagem: ChatMensagem) {
            _uiState.update { st ->
                st.copy(
                    mensagens =
                        st.mensagens.map { m ->
                            if (m.id == mensagem.id) mensagem else m
                        },
                )
            }
        }
    }
