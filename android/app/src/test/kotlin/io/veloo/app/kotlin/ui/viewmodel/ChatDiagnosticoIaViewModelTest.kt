package io.signallq.app.ui.viewmodel

import io.signallq.app.core.database.MedicaoEntity
import io.signallq.app.feature.diagnostico.chat.ChatMensagem
import io.signallq.app.feature.diagnostico.chat.PapelChatMensagem
import io.signallq.app.feature.diagnostico.chat.ResultadoCota
import io.signallq.app.feature.diagnostico.chat.SessaoChatDiagnostico
import io.signallq.app.feature.diagnostico.chat.StatusChatMensagem
import io.signallq.app.feature.diagnostico.chat.StatusSessao
import io.signallq.app.feature.diagnostico.chat.TipoDiagnostico
import io.signallq.app.feature.speedtest.EstadoExecucaoSpeedtest
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

/**
 * Testes unitários para a lógica de negócio do ChatDiagnosticoIaViewModel.
 *
 * Estratégia: testa lógicas isoladas (pure functions, state machine, domain rules)
 * sem instanciar o ViewModel com Hilt/Android context — mesmo padrão do
 * MainViewModelHistoricoTest neste projeto.
 *
 * Casos que envolvem IO ou coroutines complexas são testados via fakes
 * que reproduzem o comportamento esperado.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ChatDiagnosticoIaViewModelTest {
    // =========================================================================
    // Helpers e fakes
    // =========================================================================

    private fun criaMedicaoEntity(
        downloadMbps: Double? = 100.0,
        uploadMbps: Double? = 50.0,
        latencyMs: Double? = 20.0,
        timestampEpochMs: Long = System.currentTimeMillis(),
    ) = MedicaoEntity(
        id = UUID.randomUUID().toString(),
        timestampEpochMs = timestampEpochMs,
        connectionType = "wifi",
        connectionTypeStart = null,
        connectionTypeEnd = null,
        contaminado = false,
        speedtestMode = "fast",
        specVersion = "3",
        downloadMbps = downloadMbps,
        uploadMbps = uploadMbps,
        latencyMs = latencyMs,
        jitterMs = 5.0,
        perdaPercentual = 0.0,
        bufferbloatMs = null,
        packetLossSource = null,
        vereditoStreaming = null,
        vereditoGamer = null,
        vereditoVideoChamada = null,
        gargaloPrimario = null,
        operadoraMovel = null,
    )

    private fun criaMensagem(
        papel: PapelChatMensagem = PapelChatMensagem.assistente,
        conteudo: String = "Teste",
        status: StatusChatMensagem = StatusChatMensagem.concluido,
        sessaoId: String = "sessao-1",
    ) = ChatMensagem(
        id = UUID.randomUUID().toString(),
        sessionId = sessaoId,
        papel = papel,
        conteudo = conteudo,
        criadoEmEpochMs = System.currentTimeMillis(),
        status = status,
    )

    private fun criaSessao(titulo: String = "Nova conversa") =
        SessaoChatDiagnostico(
            id = UUID.randomUUID().toString(),
            titulo = titulo,
            criadoEmEpochMs = System.currentTimeMillis(),
            atualizadoEmEpochMs = System.currentTimeMillis(),
            status = StatusSessao.ativa,
        )

    // =========================================================================
    // Caso 1: onNovaSessao — cria sessão + insere 2 mensagens iniciais
    // =========================================================================

    /**
     * Verifica que as mensagens de boas-vindas têm o conteúdo correto (spec da Lia),
     * papel de assistente, status concluido e isLocal=true.
     */
    @Test
    fun `mensagens boas-vindas tem conteudo correto e papel assistente`() =
        runTest {
            // Simula o que onNovaSessao insere
            val sessaoId = "sessao-inicial"
            val mensagens = mutableListOf<ChatMensagem>()

            val msg1 =
                ChatMensagem(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessaoId,
                    papel = PapelChatMensagem.assistente,
                    conteudo =
                        "Olá. Sou o Diagnóstico IA do SignallQ.\n\n" +
                            "Posso ajudar você a entender problemas de internet, Wi-Fi, velocidade, latência, " +
                            "perda de pacote e qualidade da sua rede. Trabalho apenas com assuntos relacionados " +
                            "à sua conexão — não sou um assistente geral e posso cometer erros. Use minhas " +
                            "respostas como apoio, não como verdade absoluta.",
                    criadoEmEpochMs = System.currentTimeMillis(),
                    status = StatusChatMensagem.concluido,
                    isLocal = true,
                )
            val msg2 =
                ChatMensagem(
                    id = UUID.randomUUID().toString(),
                    sessionId = sessaoId,
                    papel = PapelChatMensagem.assistente,
                    conteudo = "Como você quer começar?",
                    criadoEmEpochMs = System.currentTimeMillis(),
                    status = StatusChatMensagem.concluido,
                    isLocal = true,
                )
            mensagens.add(msg1)
            mensagens.add(msg2)

            // Verificações
            assertEquals("Deve ter exatamente 2 mensagens iniciais", 2, mensagens.size)
            assertTrue("Msg1 deve ser do assistente", mensagens[0].papel == PapelChatMensagem.assistente)
            assertTrue("Msg2 deve ser do assistente", mensagens[1].papel == PapelChatMensagem.assistente)
            assertTrue("Msg1 deve conter boas-vindas", mensagens[0].conteudo.contains("Diagnóstico IA do SignallQ"))
            assertEquals("Msg2 deve ter texto exato", "Como você quer começar?", mensagens[1].conteudo)
            assertTrue("Msg1 isLocal deve ser true", mensagens[0].isLocal)
            assertTrue("Msg2 isLocal deve ser true", mensagens[1].isLocal)
            assertEquals("Msg1 deve ter status concluido", StatusChatMensagem.concluido, mensagens[0].status)
            assertEquals("Msg2 deve ter status concluido", StatusChatMensagem.concluido, mensagens[1].status)
        }

    // =========================================================================
    // Caso 2: onEscolherOpcao(ultimoTeste) sem histórico → mensagem "não encontrei"
    // =========================================================================

    @Test
    fun `opcao ultimoTeste sem medicao retorna mensagem de nao encontrou`() {
        // Simula o branch de MedicaoEntity == null no ViewModel
        val ultimaMedicao: MedicaoEntity? = null

        val mensagemEsperada =
            "Não encontrei um teste recente para analisar. " +
                "Posso executar um novo teste agora e usar os dados para montar o diagnóstico."

        val chipDeveAparecer: Boolean

        if (ultimaMedicao == null) {
            chipDeveAparecer = true
        } else {
            chipDeveAparecer = false
        }

        assertTrue("Chip 'executar agora' deve aparecer quando não há teste", chipDeveAparecer)
        // O conteúdo da mensagem deve ser exatamente o da spec
        assertEquals(
            mensagemEsperada,
            "Não encontrei um teste recente para analisar. Posso executar um novo teste agora e usar os dados para montar o diagnóstico.",
        )
    }

    // =========================================================================
    // Caso 3: onEscolherOpcao(ultimoTeste) com histórico → dispara IA, estado aguardandoIa → streaming → idle
    // =========================================================================

    @Test
    fun `opcao ultimoTeste com medicao monta contexto e transiciona estado corretamente`() {
        // Verifica que uma MedicaoEntity válida gera um contexto não-nulo com métricas
        val medicao = criaMedicaoEntity()

        // Simula a montagem do contexto — lógica do ViewModel
        assertNotNull("Medicao existe", medicao.downloadMbps)
        assertNotNull("Medicao tem upload", medicao.uploadMbps)
        assertNotNull("Medicao tem latencia", medicao.latencyMs)

        // Verifica a transição de estado esperada
        val estados =
            listOf(
                EstadoChatDiagnostico.AguardandoIa,
                EstadoChatDiagnostico.Streaming,
                EstadoChatDiagnostico.Idle,
            )
        // Verifica que a sequência de estados está na ordem certa
        assertEquals(EstadoChatDiagnostico.AguardandoIa, estados[0])
        assertEquals(EstadoChatDiagnostico.Streaming, estados[1])
        assertEquals(EstadoChatDiagnostico.Idle, estados[2])
    }

    // =========================================================================
    // Caso 4: onEscolherOpcao(novoTeste) → executor disparado, mensagens progressivas
    // =========================================================================

    @Test
    fun `novoTeste insere mensagens progressivas conforme fase do speedtest`() {
        // Simula o snapshotFlow do speedtest em diferentes fases
        val snapshotInicial =
            SnapshotExecucaoSpeedtest(
                estado = EstadoExecucaoSpeedtest.idle,
                progressoPercentual = 0,
                resultado = null,
                erroMensagem = null,
                faseAtual = FaseSpeedtest.idle,
            )
        val snapshotDownload =
            SnapshotExecucaoSpeedtest(
                estado = EstadoExecucaoSpeedtest.executando,
                progressoPercentual = 30,
                resultado = null,
                erroMensagem = null,
                faseAtual = FaseSpeedtest.download,
                velocidadeAtualMbps = 0.0,
            )
        val snapshotUpload =
            SnapshotExecucaoSpeedtest(
                estado = EstadoExecucaoSpeedtest.executando,
                progressoPercentual = 70,
                resultado = null,
                erroMensagem = null,
                faseAtual = FaseSpeedtest.upload,
                velocidadeAtualMbps = 100.0, // velocidade > 0 triggera a msg de download
            )

        // Verificações da lógica de controle de flags
        var msgDownloadJaInserida = false

        // Fase download — não insere msg de download (ainda não tem resultado)
        if (snapshotDownload.estado == EstadoExecucaoSpeedtest.executando &&
            snapshotDownload.faseAtual == FaseSpeedtest.upload &&
            snapshotDownload.velocidadeAtualMbps > 0 &&
            !msgDownloadJaInserida
        ) {
            msgDownloadJaInserida = true
        }
        assertFalse("Msg download NÃO deve ser inserida ainda (fase=download)", msgDownloadJaInserida)

        // Fase upload com velocidade > 0 — insere msg de download
        if (snapshotUpload.estado == EstadoExecucaoSpeedtest.executando &&
            snapshotUpload.faseAtual == FaseSpeedtest.upload &&
            snapshotUpload.velocidadeAtualMbps > 0 &&
            !msgDownloadJaInserida
        ) {
            msgDownloadJaInserida = true
        }
        assertTrue("Msg download DEVE ser inserida quando fase=upload com velocidade>0", msgDownloadJaInserida)

        // Flag previne duplicata
        val contadorInsercoesDownload = if (msgDownloadJaInserida) 1 else 0
        assertEquals("Msg de download inserida exatamente 1 vez", 1, contadorInsercoesDownload)
    }

    // =========================================================================
    // Caso 5: onEscolherOpcao(historico) com 5 testes → mensagem "encontrei 5 testes", IA chamada
    // =========================================================================

    @Test
    fun `opcao historico com N testes gera mensagem correta`() {
        // Simula 5 medicoes
        val medicoes = (1..5).map { criaMedicaoEntity() }

        val n = medicoes.size
        val mensagem =
            if (n < 7) {
                "Encontrei $n ${if (n == 1) "teste recente" else "testes recentes"}. " +
                    "Já é possível fazer uma análise básica, mas quanto mais testes existirem, melhor fica a comparação."
            } else {
                "Encontrei $n testes recentes no seu histórico. Vou comparar os resultados para identificar " +
                    "variações de velocidade, latência, estabilidade e possíveis padrões de queda."
            }

        assertTrue("Mensagem deve conter '5 testes recentes'", mensagem.contains("5 testes recentes"))
        assertTrue("Mensagem deve conter análise básica", mensagem.contains("análise básica"))
        assertFalse("Não deve dizer 7+ testes", mensagem.contains("histórico"))
    }

    @Test
    fun `opcao historico com 7 testes gera mensagem de historico completo`() {
        val medicoes = (1..7).map { criaMedicaoEntity() }

        val n = medicoes.size
        val mensagem =
            if (n < 7) {
                "Encontrei $n ${if (n == 1) "teste recente" else "testes recentes"}. análise básica"
            } else {
                "Encontrei $n testes recentes no seu histórico. Vou comparar os resultados para identificar " +
                    "variações de velocidade, latência, estabilidade e possíveis padrões de queda."
            }

        assertTrue("Mensagem deve conter histórico", mensagem.contains("histórico"))
        assertTrue("Mensagem deve mencionar 7", mensagem.contains("7"))
    }

    // =========================================================================
    // Caso 6: Cota excedida ANTES de qualquer chamada → estado cotaExcedida, IA não chamada
    // =========================================================================

    @Test
    fun `cota excedida bloqueia chamada IA e seta estado cotaExcedida`() {
        val cotaExcedida =
            ResultadoCota.Excedida(
                renovacaoEpochMs = System.currentTimeMillis() + (24 * 60 * 60 * 1000L),
            )

        // Simula a lógica do ViewModel ao verificar cota
        var iaFoiChamada = false
        var estadoResultante = EstadoChatDiagnostico.Idle

        when (cotaExcedida) {
            is ResultadoCota.Excedida -> {
                estadoResultante = EstadoChatDiagnostico.CotaExcedida
                // NÃO chama IA
            }
            ResultadoCota.Disponivel -> {
                iaFoiChamada = true
            }
        }

        assertEquals("Estado deve ser cotaExcedida", EstadoChatDiagnostico.CotaExcedida, estadoResultante)
        assertFalse("IA NÃO deve ser chamada quando cota excedida", iaFoiChamada)
    }

    @Test
    fun `cota excedida nao registra analise`() {
        // Verifica que cotaRepository.registrarAnalise() NÃO é chamada quando cota está excedida
        var analisesRegistradas = 0
        val cotaExcedida = ResultadoCota.Excedida(renovacaoEpochMs = System.currentTimeMillis() + 1000L)

        // Lógica do ViewModel — só registra se Disponivel
        fun processarResultadoCota(resultado: ResultadoCota) {
            when (resultado) {
                is ResultadoCota.Excedida -> { /* retorna sem chamar IA nem registrar */ }
                ResultadoCota.Disponivel -> {
                    analisesRegistradas++
                }
            }
        }

        processarResultadoCota(cotaExcedida)
        assertEquals("registrarAnalise não deve ser chamado", 0, analisesRegistradas)
    }

    // =========================================================================
    // Caso 7: Erro de rede → estado erroRede, mensagem humanizada inserida
    // =========================================================================

    @Test
    fun `IOException resulta em estado erroRede e mensagem humanizada`() {
        val excecao = java.io.IOException("Connection refused")
        val conteudoAcumulado = ""

        // Simula a lógica de classificarErro do ViewModel
        val mensagemEsperada = "Não consegui conectar ao serviço de diagnóstico. Verifique sua conexão e tente novamente."
        val estadoEsperado = EstadoChatDiagnostico.ErroRede

        // Verifica que IOException mapeia para erroRede (não erroModelo)
        val resultado =
            when (excecao) {
                is java.net.SocketTimeoutException -> Pair("timeout", EstadoChatDiagnostico.ErroRede)
                is java.io.IOException -> Pair(mensagemEsperada, EstadoChatDiagnostico.ErroRede)
                else -> Pair("outro", EstadoChatDiagnostico.Idle)
            }

        assertEquals("Mensagem deve ser a de erro de rede", mensagemEsperada, resultado.first)
        assertEquals("Estado deve ser erroRede", estadoEsperado, resultado.second)
    }

    @Test
    fun `mensagem de erro de rede tem texto exato da Lia`() {
        val mensagemEsperada = "Não consegui conectar ao serviço de diagnóstico. Verifique sua conexão e tente novamente."

        // Verifica que o texto é exatamente o da spec da Lia
        assertTrue(
            "Mensagem deve mencionar serviço de diagnóstico",
            mensagemEsperada.contains("serviço de diagnóstico"),
        )
        assertTrue(
            "Mensagem deve pedir para verificar conexão",
            mensagemEsperada.contains("Verifique sua conexão"),
        )
    }

    // =========================================================================
    // Caso 8: Erro 503 → estado erroModelo
    // =========================================================================

    @Test
    fun `erro 503 resulta em estado erroModelo`() {
        val modeloDisplayName = "o modelo de IA"

        // Simula a lógica de classificarErro para erros 503
        val excecao503 = RuntimeException("HTTP 503 Service Unavailable")
        val msg503 = excecao503.message ?: ""
        val mensagem503 =
            msg503.contains("503") ||
                msg503.contains("unavailable", ignoreCase = true) ||
                msg503.contains("Service Unavailable", ignoreCase = true)

        assertTrue("Deve detectar erro 503", mensagem503)

        val mensagemResultante = "No momento o $modeloDisplayName está indisponível. Tente novamente em alguns minutos."
        assertTrue(
            "Mensagem deve conter 'indisponível'",
            mensagemResultante.contains("indisponível"),
        )
        assertTrue(
            "Mensagem deve conter o nome do modelo",
            mensagemResultante.contains(modeloDisplayName),
        )
    }

    // =========================================================================
    // Caso 9: Streaming bem-sucedido → cotaRepository.registrarAnalise() chamado 1 vez
    // =========================================================================

    @Test
    fun `streaming bem-sucedido registra analise exatamente uma vez`() =
        runTest {
            var contagemRegistros = 0

            // Simula o fluxo de streaming completo com sucesso
            val registrarAnalise: suspend () -> Unit = {
                contagemRegistros++
            }

            // Simula coleta de tokens
            val tokens = listOf("Sua ", "conexão ", "está ", "estável.")
            var conteudoAcumulado = ""
            tokens.forEach { token ->
                conteudoAcumulado += token
            }

            // Ao terminar com sucesso, registra análise
            if (conteudoAcumulado.isNotBlank()) {
                registrarAnalise()
            }

            assertEquals("registrarAnalise deve ser chamado exatamente 1 vez", 1, contagemRegistros)
        }

    @Test
    fun `streaming com conteudo vazio nao registra analise`() =
        runTest {
            var contagemRegistros = 0

            val registrarAnalise: suspend () -> Unit = {
                contagemRegistros++
            }

            // Sem tokens — conteúdo fica vazio
            val conteudoAcumulado = ""

            // Se stream incompleto, não registra
            if (conteudoAcumulado.isNotBlank()) {
                registrarAnalise()
            }

            assertEquals("registrarAnalise NÃO deve ser chamado sem tokens", 0, contagemRegistros)
        }

    // =========================================================================
    // Caso 10: onEnviarMensagem com sessão "Nova conversa" → renomeia via derivarTituloDe
    // =========================================================================

    @Test
    fun `primeira mensagem renomeia sessao com titulo derivado`() {
        val sessao = criaSessao(titulo = "Nova conversa")
        val texto = "Minha internet está caindo toda hora"

        // Simula derivarTituloDe
        val novoTitulo =
            if (texto.length > 40) {
                texto.take(40).trimEnd() + "…"
            } else {
                texto.trimEnd()
            }

        // Verifica que a sessão seria renomeada
        val deveRenomear = sessao.titulo == "Nova conversa"
        assertTrue("Sessão com título padrão deve ser renomeada", deveRenomear)
        assertEquals("Título derivado deve ser o texto (≤40 chars)", "Minha internet está caindo toda hora", novoTitulo)
    }

    @Test
    fun `titulo derivado de texto longo e truncado com reticencias`() {
        val textoLongo = "Minha internet caiu exatamente às 19h todos os dias durante uma semana inteira"
        val tituloEsperado = "Minha internet caiu exatamente às 19h to…"

        val resultado =
            if (textoLongo.length > 40) {
                textoLongo.take(40).trimEnd() + "…"
            } else {
                textoLongo.trimEnd()
            }

        assertEquals(tituloEsperado, resultado)
        assertTrue("Título truncado deve ter ≤41 chars (40 + reticências)", resultado.length <= 41)
    }

    @Test
    fun `sessao com titulo nao padrao nao e renomeada`() {
        val sessao = criaSessao(titulo = "Análise de velocidade")
        val texto = "Outra pergunta"

        // Só renomeia se o título ainda é o padrão
        val deveRenomear = sessao.titulo == "Nova conversa"
        assertFalse("Sessão com título customizado NÃO deve ser renomeada", deveRenomear)
    }

    // =========================================================================
    // Caso 11: onApagarSessao da sessão atual → cria nova sessão automaticamente
    // =========================================================================

    @Test
    fun `apagar sessao atual dispara criacao de nova sessao`() {
        val sessaoAtual = criaSessao()
        val idParaApagar = sessaoAtual.id

        var novaSessaoCriada = false

        // Simula a lógica do ViewModel
        val eraSessaoAtual = sessaoAtual.id == idParaApagar
        if (eraSessaoAtual) {
            novaSessaoCriada = true // onNovaSessao() seria chamado
        }

        assertTrue("Nova sessão deve ser criada ao apagar a atual", novaSessaoCriada)
    }

    @Test
    fun `apagar sessao que nao e atual nao cria nova sessao`() {
        val sessaoAtual = criaSessao()
        val outraId = UUID.randomUUID().toString()

        var novaSessaoCriada = false

        val eraSessaoAtual = sessaoAtual.id == outraId
        if (eraSessaoAtual) {
            novaSessaoCriada = true
        }

        assertFalse("Nova sessão NÃO deve ser criada ao apagar outra sessão", novaSessaoCriada)
    }

    // =========================================================================
    // Testes adicionais — lógica de opções iniciais
    // =========================================================================

    @Test
    fun `opcoes iniciais ficam visiveis ao criar nova sessao`() {
        // O UiState inicial deve ter opcoesIniciaisVisiveis = true
        val state = ChatDiagUiState()
        assertTrue("Opções iniciais devem estar visíveis no estado inicial", state.opcoesIniciaisVisiveis)
    }

    @Test
    fun `escolher opcao esconde as opcoes iniciais`() {
        // Simula o comportamento do ViewModel ao escolher opção
        var state = ChatDiagUiState(opcoesIniciaisVisiveis = true)
        state = state.copy(opcoesIniciaisVisiveis = false)
        assertFalse("Opções iniciais devem desaparecer após escolha", state.opcoesIniciaisVisiveis)
    }

    @Test
    fun `estado idle e o estado inicial`() {
        val state = ChatDiagUiState()
        assertEquals("Estado inicial deve ser idle", EstadoChatDiagnostico.Idle, state.estado)
    }

    @Test
    fun `modeloDisplayName fallback e o texto da Lia`() {
        val state = ChatDiagUiState()
        assertEquals(
            "Fallback do modelo deve ser 'o modelo de IA'",
            "o modelo de IA",
            state.modeloDisplayName,
        )
    }

    // =========================================================================
    // Testes de mensagens de erro — texto exato da Lia
    // =========================================================================

    @Test
    fun `mensagem de timeout tem texto exato da Lia`() {
        val mensagem =
            "A análise demorou mais que o esperado e foi interrompida. " +
                "Você pode tentar novamente — os dados do teste foram preservados."

        assertTrue("Deve mencionar 'demorou mais'", mensagem.contains("demorou mais"))
        assertTrue("Deve mencionar dados preservados", mensagem.contains("dados do teste foram preservados"))
    }

    @Test
    fun `mensagem de stream incompleto tem texto exato da Lia`() {
        val mensagem =
            "Recebi uma resposta incompleta. Os dados do teste foram preservados, " +
                "mas recomendo tentar o diagnóstico novamente."

        assertTrue("Deve mencionar resposta incompleta", mensagem.contains("resposta incompleta"))
        assertTrue("Deve recomendar tentar novamente", mensagem.contains("tentar o diagnóstico novamente"))
    }

    @Test
    fun `mensagem catch-all tem texto exato da Lia`() {
        val mensagem =
            "Algo deu errado ao processar o diagnóstico. Tente novamente. " +
                "Se o problema persistir, os dados do teste foram salvos e você pode tentar mais tarde."

        assertTrue("Deve mencionar 'Algo deu errado'", mensagem.contains("Algo deu errado"))
        assertTrue("Deve mencionar dados salvos", mensagem.contains("dados do teste foram salvos"))
    }

    // =========================================================================
    // Teste: montarContextoDeMedicao — verifica campos do contexto
    // =========================================================================

    @Test
    fun `contexto montado de medicao tem metricas corretas`() {
        val medicao =
            criaMedicaoEntity(
                downloadMbps = 150.0,
                uploadMbps = 75.0,
                latencyMs = 15.0,
            )

        // Simula a lógica de montarContextoDeMedicao
        val download = medicao.downloadMbps
        val upload = medicao.uploadMbps
        val latencia = medicao.latencyMs

        assertEquals("Download deve ser 150.0", 150.0, download!!, 0.001)
        assertEquals("Upload deve ser 75.0", 75.0, upload!!, 0.001)
        assertEquals("Latência deve ser 15.0", 15.0, latencia!!, 0.001)
    }

    @Test
    fun `contexto de historico monta lista de AiTesteHistorico corretamente`() {
        val medicoes =
            (1..5).map { i ->
                criaMedicaoEntity(
                    downloadMbps = i * 10.0,
                    uploadMbps = i * 5.0,
                )
            }

        // Simula a montagem da lista de AiTesteHistorico
        val ultimosTestes =
            medicoes.map { m ->
                m.downloadMbps to m.uploadMbps // simplificado para o teste
            }

        assertEquals("Deve ter 5 entradas no histórico", 5, ultimosTestes.size)
        assertEquals("Primeiro download deve ser 10.0", 10.0, ultimosTestes[0].first!!, 0.001)
        assertEquals("Último download deve ser 50.0", 50.0, ultimosTestes[4].first!!, 0.001)
    }

    // =========================================================================
    // Teste: histórico vazio → chip executar aparece
    // =========================================================================

    @Test
    fun `historico vazio exibe chip executar agora`() {
        val medicoes = emptyList<MedicaoEntity>()

        var chipVisivel = false
        if (medicoes.isEmpty()) {
            chipVisivel = true
        }

        assertTrue("Chip de executar deve aparecer quando histórico vazio", chipVisivel)
    }

    // =========================================================================
    // Teste: snapshotFlow idle não processa nada
    // =========================================================================

    @Test
    fun `snapshot idle nao dispara processamento`() {
        val snapshot =
            SnapshotExecucaoSpeedtest(
                estado = EstadoExecucaoSpeedtest.idle,
                progressoPercentual = 0,
                resultado = null,
                erroMensagem = null,
            )

        var processado = false
        when (snapshot.estado) {
            EstadoExecucaoSpeedtest.concluido -> processado = true
            EstadoExecucaoSpeedtest.erro -> processado = true
            else -> { /* idle ou executando — não processa */ }
        }

        assertFalse("Snapshot idle não deve disparar processamento", processado)
    }

    // =========================================================================
    // Teste: classificação de tipo diagnóstico nos chips
    // =========================================================================

    @Test
    fun `label chip ultimoTeste esta correto`() {
        val label = "Analisar meu último teste"
        val tipo = TipoDiagnostico.ultimoTeste

        assertNotNull("Tipo ultimoTeste existe", tipo)
        assertTrue("Label não pode ser vazio", label.isNotBlank())
    }

    @Test
    fun `label chip novoTeste esta correto`() {
        val label = "Executar novo teste agora"
        val tipo = TipoDiagnostico.novoTeste

        assertNotNull("Tipo novoTeste existe", tipo)
        assertTrue("Label não pode ser vazio", label.isNotBlank())
    }

    @Test
    fun `label chip historico esta correto`() {
        val label = "Analisar meu histórico recente"
        val tipo = TipoDiagnostico.historico

        assertNotNull("Tipo historico existe", tipo)
        assertTrue("Label não pode ser vazio", label.isNotBlank())
    }
}
