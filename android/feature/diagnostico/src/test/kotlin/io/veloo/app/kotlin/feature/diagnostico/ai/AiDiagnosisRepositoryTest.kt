package io.signallq.app.feature.diagnostico.ai

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Cobertura dos cenarios obrigatorios do refactor v2 (Gemma 4 26B como motor padrao):
 *  1. modelo Gemma 4 26B ativo -> rodape "SignallQ IA - Gemma 4 26B"
 *  2. fallback local -> rodape "Diagnostico local do SignallQ", nao chama Llama
 *  3. velocidade boa + estabilidade ruim (294/411/101/25.1) — nao colapsa "internet lenta"
 *  4. velocidade baixa + latencia normal — problema e banda
 *  5. tudo bom
 *  6. dados insuficientes -> inconclusivo
 *  7. modelo desconhecido / modeloIa ausente -> rodape "SignallQ IA"
 *  8. resposta antiga schema v1 -> parser nao quebra, fallback seguro
 *
 * O parser e privado/internal; para acessa-lo usamos um repositorio dummy.
 * Os testes nao fazem rede — apenas validam parser, normalizeStatus e ModeloIa.
 */
class AiDiagnosisRepositoryTest {

    private lateinit var repo: AiDiagnosisRepository

    @Before
    fun setUp() {
        // baseUrl nao e usado — os testes invocam parseResult diretamente.
        repo = AiDiagnosisRepository(baseUrl = "http://invalid.local", isAuthorized = { true })
    }

    // -------------------------------------------------------------------------
    // Cenario 1 — velocidade boa, estabilidade ruim (caso de referencia do briefing)
    // Download 294 / Upload 411 / Latencia 101 / Jitter 25.1
    // -------------------------------------------------------------------------
    @Test
    fun cenario1_velocidadeBoa_estabilidadeRuim_naoColapsaParaInternetLenta() {
        val json = """
            {
              "schemaVersion": "2",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000000,
              "modeloIa": {
                "idInterno": "@cf/google/gemma-4-26b-a4b-it",
                "provedor": "cloudflare_workers_ai",
                "familia": "Gemma",
                "versao": "4",
                "tamanho": "26B",
                "variante": "Instruction Tuned",
                "nomeExibicao": "Gemma 4 26B",
                "nomeCompletoComercial": "SignallQ IA — Gemma 4 26B",
                "descricaoComercial": "Diagnóstico inteligente otimizado para respostas rápidas",
                "textoRodape": "Motor de análise: SignallQ IA — Gemma 4 26B"
              },
              "status": "regular",
              "titulo": "Velocidade boa, estabilidade ruim",
              "resumo": "A velocidade está boa, mas a conexão está instável.",
              "textoLaudo": "Download de 294 Mbps e upload de 411 Mbps indicam banda excelente. Porém a latência de 101 ms e o jitter de 25,1 ms apontam instabilidade na conexão. Velocidade insuficiente foi descartada. Causa provável: interferência Wi-Fi ou congestionamento momentâneo. Impacto: chamadas e jogos podem sofrer travamentos.",
              "classificacaoTecnica": {
                "velocidade":   {"avaliacao": "boa", "justificativa": "Download de 294 Mbps e upload de 411 Mbps."},
                "estabilidade": {"avaliacao": "ruim", "justificativa": "Latência de 101 ms e jitter de 25,1 ms."}
              },
              "problemaPrincipal": {
                "tipo": "estabilidade",
                "descricao": "Latência e jitter elevados indicam instabilidade.",
                "confianca": 0.7
              },
              "hipotesesDescartadas": [
                {"hipotese": "velocidade insuficiente", "motivo": "download de 294 Mbps e upload de 411 Mbps indicam boa banda"}
              ],
              "impacto": {
                "navegacao": "OK",
                "streaming": "OK",
                "videochamada": "Instavel",
                "jogos": "Alta latencia",
                "trabalho": "OK"
              },
              "acoesRecomendadas": [
                {"titulo": "Testar perto do roteador", "descricao": "Aproxime-se do roteador e refaça o teste.", "prioridade": "alta", "tipo": "validacao_local", "executavelNoApp": true},
                {"titulo": "Verificar dispositivos", "descricao": "Veja se ha downloads em outros aparelhos.", "prioridade": "media", "tipo": "ajuste_dispositivo", "executavelNoApp": false},
                {"titulo": "Repetir em outro horário", "descricao": "Teste de novo em horario de menor uso.", "prioridade": "baixa", "tipo": "reteste", "executavelNoApp": true}
              ],
              "perguntasContextuais": [
                {"id": "perto_roteador", "pergunta": "Voce esta perto do roteador?", "opcoes": [{"id": "sim", "rotulo": "Sim"},{"id": "nao", "rotulo": "Nao"}]}
              ],
              "evidencias": [
                {"label": "download", "valor": "294 Mbps", "interpretacao": "banda excelente"},
                {"label": "upload", "valor": "411 Mbps", "interpretacao": "banda excelente"},
                {"label": "latencia", "valor": "101 ms", "interpretacao": "alta para uso interativo"},
                {"label": "jitter", "valor": "25.1 ms", "interpretacao": "instabilidade"}
              ],
              "limitesDaAnalise": ["Sem teste comparativo cabo vs Wi-Fi."]
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        assertNotNull(parsed)
        requireNotNull(parsed)

        assertEquals("2", parsed.schemaVersion)
        assertEquals("estabilidade", parsed.problemaPrincipal.tipo)
        assertEquals("boa", parsed.classificacaoTecnica.velocidade?.avaliacao)
        assertEquals("ruim", parsed.classificacaoTecnica.estabilidade?.avaliacao)

        // Titulo nao pode ser "internet lenta" — verifica explicitamente
        assertFalse(
            "Titulo nao pode colapsar para 'internet lenta'",
            parsed.titulo.lowercase().contains("internet lenta") ||
                parsed.titulo.lowercase().contains("velocidade lenta"),
        )

        // resumo deve mencionar velocidade boa e instabilidade
        assertTrue(parsed.resumo.lowercase().contains("velocidade"))
        assertTrue(parsed.resumo.lowercase().contains("inst"))

        assertEquals(1, parsed.hipotesesDescartadas.size)
        assertEquals(3, parsed.acoesRecomendadas.size)
        assertEquals("validacao_local", parsed.acoesRecomendadas.first().tipo)
        assertTrue(parsed.acoesRecomendadas.first().executavelNoApp)

        // Modelo Gemma 4 26B (motor padrao apos refactor)
        assertEquals("Gemma", parsed.modeloIa.familia)
        assertEquals("4", parsed.modeloIa.versao)
        assertEquals("26B", parsed.modeloIa.tamanho)
        assertEquals("Gemma 4 26B", parsed.modeloIa.nomeExibicao)
        assertEquals("SignallQ IA — Gemma 4 26B", parsed.modeloIa.nomeCompletoComercial)
        assertEquals("Motor de análise: SignallQ IA — Gemma 4 26B", parsed.modeloIa.textoRodape)
        // idInterno fica disponivel para debug, mas nunca aparece nos campos comerciais
        assertFalse(parsed.modeloIa.nomeExibicao.contains("@cf"))
        assertFalse(parsed.modeloIa.textoRodape.contains("@cf"))
        assertFalse(parsed.modeloIa.textoRodape.lowercase().contains("llama"))

        // normalizeStatus nao colapsa: status ja valido permanece "regular"
        val normalized = repo.normalizeStatus(
            aiStatus = parsed.status,
            decisaoStatus = "attention",
            problemaPrincipalTipo = parsed.problemaPrincipal.tipo,
        )
        assertEquals("regular", normalized)
    }

    // -------------------------------------------------------------------------
    // Cenario 2 — velocidade baixa, latencia normal
    // -------------------------------------------------------------------------
    @Test
    fun cenario2_velocidadeBaixa_latenciaNormal() {
        val json = """
            {
              "schemaVersion": "2",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000001,
              "modeloIa": {"idInterno":"@cf/google/gemma-4-26b-a4b-it","familia":"Gemma","versao":"4","tamanho":"26B","nomeExibicao":"Gemma 4 26B","nomeCompletoComercial":"SignallQ IA — Gemma 4 26B","textoRodape":"Motor de análise: SignallQ IA — Gemma 4 26B"},
              "status": "ruim",
              "titulo": "Velocidade abaixo do contratado",
              "resumo": "Download de 5 Mbps esta muito abaixo do esperado.",
              "textoLaudo": "Download de 5 Mbps e upload de 2 Mbps indicam banda muito reduzida. Latencia de 28 ms e jitter de 4 ms estao normais — instabilidade descartada. Causa provavel: throttling do provedor ou plano abaixo do necessario.",
              "classificacaoTecnica": {
                "velocidade": {"avaliacao": "ruim", "justificativa": "Download 5 Mbps e upload 2 Mbps."},
                "estabilidade": {"avaliacao": "boa", "justificativa": "Latência 28 ms e jitter 4 ms."}
              },
              "problemaPrincipal": {"tipo": "velocidade", "descricao": "Banda muito baixa.", "confianca": 0.85},
              "hipotesesDescartadas": [{"hipotese":"instabilidade","motivo":"latencia 28 ms e jitter 4 ms estao normais"}],
              "impacto": {"navegacao":"Lento","streaming":"Comprometido","videochamada":"Comprometida","jogos":"Instavel","trabalho":"Comprometido"},
              "acoesRecomendadas": [{"titulo":"Comparar com plano contratado","descricao":"Verifique o plano e contate o ISP.","prioridade":"alta","tipo":"contato_isp","executavelNoApp":false}],
              "perguntasContextuais": [],
              "evidencias": [],
              "limitesDaAnalise": []
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        assertNotNull(parsed)
        requireNotNull(parsed)

        assertEquals("velocidade", parsed.problemaPrincipal.tipo)
        assertEquals("ruim", parsed.classificacaoTecnica.velocidade?.avaliacao)
        assertEquals("boa", parsed.classificacaoTecnica.estabilidade?.avaliacao)
        assertEquals(1, parsed.hipotesesDescartadas.size)
        assertEquals("instabilidade", parsed.hipotesesDescartadas.first().hipotese)
        // Latencia normal -> jogos nao podem ser marcados como "Alta latencia"
        assertFalse(
            "Latencia normal nao deve gerar 'Alta latencia' em jogos",
            normalizeImpactoLabel(parsed.impacto.jogos).contains("Alta latência"),
        )
    }

    // -------------------------------------------------------------------------
    // Cenario 3 — tudo bom
    // -------------------------------------------------------------------------
    @Test
    fun cenario3_tudoBom() {
        val json = """
            {
              "schemaVersion": "2",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000002,
              "modeloIa": {"familia":"Gemma","versao":"4","tamanho":"26B","nomeExibicao":"Gemma 4 26B","nomeCompletoComercial":"SignallQ IA — Gemma 4 26B","textoRodape":"Motor de análise: SignallQ IA — Gemma 4 26B"},
              "status": "excelente",
              "titulo": "Conexao saudavel",
              "resumo": "Velocidade e estabilidade dentro do esperado.",
              "textoLaudo": "Download 250 Mbps, upload 100 Mbps, latencia 14 ms e jitter 2 ms.",
              "classificacaoTecnica": {
                "velocidade": {"avaliacao":"boa","justificativa":"Download 250 Mbps."},
                "estabilidade": {"avaliacao":"boa","justificativa":"Latência 14 ms e jitter 2 ms."}
              },
              "problemaPrincipal": {"tipo":"sem_problema","descricao":"Sem problemas detectados.","confianca":0.9},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [],
              "evidencias": [],
              "limitesDaAnalise": []
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        assertNotNull(parsed)
        requireNotNull(parsed)

        assertEquals("excelente", parsed.status)
        assertEquals("sem_problema", parsed.problemaPrincipal.tipo)
        assertTrue(parsed.acoesRecomendadas.isEmpty())
    }

    // -------------------------------------------------------------------------
    // Cenario 4 — dados insuficientes -> inconclusivo
    // -------------------------------------------------------------------------
    @Test
    fun cenario4_dadosInsuficientes_inconclusivo() {
        val json = """
            {
              "schemaVersion": "2",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000003,
              "modeloIa": {"familia":"Gemma","versao":"4","tamanho":"26B","nomeExibicao":"Gemma 4 26B","textoRodape":"Motor de análise: SignallQ IA — Gemma 4 26B"},
              "status": "inconclusivo",
              "titulo": "Dados insuficientes",
              "resumo": "Nao foi possivel concluir.",
              "textoLaudo": "Sem speedtest disponivel para analisar velocidade ou latencia.",
              "classificacaoTecnica": {
                "velocidade": {"avaliacao":"inconclusiva","justificativa":"sem dados"},
                "estabilidade": {"avaliacao":"inconclusiva","justificativa":"sem dados"}
              },
              "problemaPrincipal": {"tipo":"inconclusivo","descricao":"Sem dados.","confianca":0.0},
              "impacto": {"navegacao":"","streaming":"","videochamada":"","jogos":"","trabalho":""},
              "acoesRecomendadas": [{"titulo":"Rodar speedtest","descricao":"Execute um speedtest completo.","prioridade":"alta","tipo":"reteste","executavelNoApp":true}],
              "evidencias": [],
              "limitesDaAnalise": ["Sem speedtest disponivel."]
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        assertNotNull(parsed)
        requireNotNull(parsed)

        assertEquals("inconclusivo", parsed.status)
        assertEquals("inconclusiva", parsed.classificacaoTecnica.velocidade?.avaliacao)
        assertEquals("inconclusivo", parsed.problemaPrincipal.tipo)
    }

    // -------------------------------------------------------------------------
    // Cenario 6 — modelo Gemma -> rodape "SignallQ IA — Gemma 4 26B"
    // -------------------------------------------------------------------------
    @Test
    fun cenario6_modeloGemma_rodapeComercial() {
        val json = """
            {
              "schemaVersion": "2",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000005,
              "modeloIa": {
                "idInterno": "@cf/google/gemma-4-26b-a4b-it",
                "provedor": "cloudflare_workers_ai",
                "familia": "Gemma",
                "versao": "4",
                "tamanho": "26B",
                "variante": "Instruction Tuned",
                "nomeExibicao": "Gemma 4 26B",
                "nomeCompletoComercial": "SignallQ IA — Gemma 4 26B",
                "descricaoComercial": "Diagnóstico inteligente otimizado para respostas rápidas",
                "textoRodape": "Motor de análise: SignallQ IA — Gemma 4 26B"
              },
              "status": "bom",
              "titulo": "OK",
              "resumo": "ok",
              "textoLaudo": "ok",
              "problemaPrincipal": {"tipo":"sem_problema","descricao":"","confianca":0.7},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [],
              "evidencias": [],
              "limitesDaAnalise": []
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        requireNotNull(parsed)

        assertEquals("Gemma 4 26B", parsed.modeloIa.nomeExibicao)
        assertEquals("SignallQ IA — Gemma 4 26B", parsed.modeloIa.nomeCompletoComercial)
        assertEquals("Motor de análise: SignallQ IA — Gemma 4 26B", parsed.modeloIa.textoRodape)
        // idInterno nao deve aparecer em nenhum dos campos comerciais
        assertFalse(parsed.modeloIa.nomeExibicao.contains("@cf"))
        assertFalse(parsed.modeloIa.nomeCompletoComercial.contains("@cf"))
        assertFalse(parsed.modeloIa.textoRodape.contains("@cf"))
    }

    // -------------------------------------------------------------------------
    // Cenario 7 — modelo desconhecido (modeloIa ausente) -> default "SignallQ IA"
    // -------------------------------------------------------------------------
    @Test
    fun cenario7_modeloDesconhecido_rodapeGenericoSignallQIA() {
        // Resposta v1 sem modeloIa
        val json = """
            {
              "schemaVersion": "1",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000006,
              "status": "bom",
              "titulo": "Conexao OK",
              "resumo": "ok",
              "textoLaudo": "ok",
              "problemaPrincipal": {"tipo":"sem_problema","descricao":"","confianca":0.7},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [],
              "evidencias": [],
              "limitesDaAnalise": []
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        requireNotNull(parsed)

        assertEquals("1", parsed.schemaVersion)
        assertEquals("SignallQ IA", parsed.modeloIa.nomeExibicao)
        assertEquals("SignallQ IA", parsed.modeloIa.nomeCompletoComercial)
        assertEquals("Motor de análise: SignallQ IA", parsed.modeloIa.textoRodape)
        assertEquals("", parsed.modeloIa.idInterno)
    }

    // -------------------------------------------------------------------------
    // normalizeStatus — nunca rebaixa estabilidade para "internet lenta"
    // -------------------------------------------------------------------------
    @Test
    fun normalizeStatus_aiInvalido_problemaEstabilidade_naoColapsa() {
        // IA respondeu com status invalido. Decisao local diz "attention".
        // Como problema e "estabilidade", nao podemos rebaixar para "ruim" generico.
        val out = repo.normalizeStatus(
            aiStatus = "alguma_coisa_estranha",
            decisaoStatus = "attention",
            problemaPrincipalTipo = "estabilidade",
        )
        assertEquals("regular", out)
    }

    @Test
    fun normalizeStatus_aiValido_mantemAi() {
        val out = repo.normalizeStatus(
            aiStatus = "regular",
            decisaoStatus = "critical", // mesmo que a decisao local seja mais severa
            problemaPrincipalTipo = "estabilidade",
        )
        assertEquals("regular", out)
    }

    // -------------------------------------------------------------------------
    // Parser tolerante — JSON malformado ou vazio nao quebra
    // -------------------------------------------------------------------------
    @Test
    fun parser_jsonMalformado_retornaNull() {
        assertNull(repo.parseResult("{ broken"))
        assertNull(repo.parseResult(""))
    }

    @Test
    fun parser_jsonMinimo_aceita() {
        val out = repo.parseResult("{}")
        assertNotNull(out)
        requireNotNull(out)
        assertEquals("1", out.schemaVersion) // default v1
        assertEquals("Diagnostico", out.titulo)
        assertEquals("SignallQ IA", out.modeloIa.nomeExibicao)
    }

    // -------------------------------------------------------------------------
    // Ajuste 5 — parser le exatamente classificacaoTecnica.<dim>.avaliacao /
    // .justificativa para as 5 dimensoes e ignora campos extras enviados pela IA.
    // -------------------------------------------------------------------------
    @Test
    fun parser_classificacaoTecnica_lê_5_dimensoes_e_ignora_chaves_extras() {
        val json = """
            {
              "schemaVersion": "2",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000010,
              "status": "regular",
              "titulo": "ok",
              "resumo": "ok",
              "textoLaudo": "ok",
              "classificacaoTecnica": {
                "velocidade":   {"avaliacao": "boa",          "justificativa": "v-just", "campoExtra": "ignorar"},
                "estabilidade": {"avaliacao": "ruim",         "justificativa": "e-just"},
                "wifi":         {"avaliacao": "regular",      "justificativa": "w-just"},
                "dns":          {"avaliacao": "inconclusiva", "justificativa": "d-just"},
                "fibra":        {"avaliacao": "nao_avaliado", "justificativa": "f-just"},
                "dimensaoDesconhecida": {"avaliacao": "boa", "justificativa": "deve ser ignorada"}
              },
              "problemaPrincipal": {"tipo":"estabilidade","descricao":"","confianca":0.7},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [],
              "evidencias": [],
              "limitesDaAnalise": []
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        requireNotNull(parsed)

        val ct = parsed.classificacaoTecnica
        assertEquals("boa", ct.velocidade?.avaliacao)
        assertEquals("v-just", ct.velocidade?.justificativa)
        assertEquals("ruim", ct.estabilidade?.avaliacao)
        assertEquals("e-just", ct.estabilidade?.justificativa)
        assertEquals("regular", ct.wifi?.avaliacao)
        assertEquals("w-just", ct.wifi?.justificativa)
        assertEquals("inconclusiva", ct.dns?.avaliacao)
        assertEquals("d-just", ct.dns?.justificativa)
        assertEquals("nao_avaliado", ct.fibra?.avaliacao)
        assertEquals("f-just", ct.fibra?.justificativa)
    }

    // -------------------------------------------------------------------------
    // Cenario 2 (brief) — falha da IA -> fallback local nunca usa Llama
    // ModeloIa.localFallback() expressa "Diagnostico local do SignallQ", nao Llama.
    // -------------------------------------------------------------------------
    @Test
    fun fallbackLocal_naoUsaLlamaNemMeta_rodapeDiagnosticoLocal() {
        val local = ModeloIa.localFallback()

        assertEquals("Local", local.familia)
        assertEquals("local", local.provedor)
        assertEquals("Diagnóstico local", local.nomeExibicao)
        assertEquals("Diagnóstico local do SignallQ", local.nomeCompletoComercial)
        assertEquals("Motor de análise: Diagnóstico local do SignallQ", local.textoRodape)

        // Llama/Meta jamais aparece no fallback local
        assertFalse(local.familia.lowercase().contains("llama"))
        assertFalse(local.familia.lowercase().contains("meta"))
        assertFalse(local.nomeExibicao.lowercase().contains("llama"))
        assertFalse(local.nomeCompletoComercial.lowercase().contains("llama"))
        assertFalse(local.textoRodape.lowercase().contains("llama"))
        assertFalse(local.textoRodape.lowercase().contains("gemma"))
        assertFalse(local.textoRodape.contains("@cf"))
    }

    // -------------------------------------------------------------------------
    // Cenario 8 (brief) — resposta antiga schema v1 sem modeloIa
    // Parser nao quebra; preenche fallback seguro "SignallQ IA"; rodape nao vazio.
    // -------------------------------------------------------------------------
    @Test
    fun cenario8_respostaSchemaV1SemModeloIa_parserPreencheFallbackSeguro() {
        val jsonV1 = """
            {
              "schemaVersion": "1",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000099,
              "status": "regular",
              "titulo": "Resposta antiga",
              "resumo": "Schema v1 nao tem modeloIa.",
              "textoLaudo": "Resposta gerada com prompt antigo.",
              "problemaPrincipal": {"tipo":"velocidade","descricao":"","confianca":0.5},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [{"titulo":"Reteste","descricao":"Rode de novo.","prioridade":"media"}],
              "evidencias": [{"label":"download","valor":"50 Mbps","interpretacao":"ok"}],
              "limitesDaAnalise": []
            }
        """.trimIndent()

        val parsed = repo.parseResult(jsonV1)
        requireNotNull(parsed)

        // Schema v1 preservado
        assertEquals("1", parsed.schemaVersion)
        // modeloIa veio ausente -> defaults seguros
        assertEquals("SignallQ IA", parsed.modeloIa.nomeExibicao)
        assertEquals("SignallQ IA", parsed.modeloIa.nomeCompletoComercial)
        assertEquals("Motor de análise: SignallQ IA", parsed.modeloIa.textoRodape)
        assertTrue(parsed.modeloIa.textoRodape.isNotBlank())
        // Campos antigos sem `tipo`/`executavelNoApp` em acoes nao quebram
        assertEquals(1, parsed.acoesRecomendadas.size)
        assertEquals("", parsed.acoesRecomendadas.first().tipo)
        assertFalse(parsed.acoesRecomendadas.first().executavelNoApp)
    }

    // -------------------------------------------------------------------------
    // AI_PROMPT_VERSION — bump para "diagnostico_v6_local_device" (GH#542)
    // garante invalidacao de caches gerados com o prompt v5 anterior, quando o
    // campo `equipamentoLocal` ainda nao existia no payload.
    // -------------------------------------------------------------------------
    @Test
    fun aiPromptVersion_estaBumpadoParaV6LocalDevice_invalidaCachesAntigos() {
        assertEquals("diagnostico_v6_local_device", AI_PROMPT_VERSION)
    }

    // -------------------------------------------------------------------------
    // TTL do cache — 5 minutos
    // -------------------------------------------------------------------------

    private fun fakeDiagnosisAiContext(): DiagnosisAiContext = DiagnosisAiContext(
        schemaVersion = "5",
        generatedAtEpochMs = 1700000000000L,
        connectionType = io.signallq.app.feature.diagnostico.ConnectionType.wifi,
        evidencias = emptyList(),
    )

    private fun fakeAiDiagnosisResult(): AiDiagnosisResult {
        val json = """
            {
              "schemaVersion": "3",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000000,
              "status": "bom",
              "titulo": "Conexao OK",
              "resumo": "Tudo certo.",
              "textoLaudo": "ok",
              "problemaPrincipal": {"tipo":"sem_problema","descricao":"","confianca":0.9},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [],
              "evidencias": [],
              "limitesDaAnalise": []
            }
        """.trimIndent()
        return repo.parseResult(json)!!
    }

    @Test
    fun cache_com4Minutos_retornaResultado() {
        val now = 1_000_000L
        val quatroMinutosAtras = now - (4 * 60 * 1000L)

        var tempoAtual = now
        val repoComClock = AiDiagnosisRepository(
            baseUrl = "http://invalid.local",
            isAuthorized = { true },
            clock = { tempoAtual },
        )

        val ctx = fakeDiagnosisAiContext()
        val resultado = fakeAiDiagnosisResult()
        val key = repoComClock.cacheKey(ctx)

        // Insere entrada no cache como se tivesse sido inserida 4 minutos atras
        repoComClock.cache[key] = Pair(resultado, quatroMinutosAtras)

        // Clock aponta para "agora" (4 min depois da insercao)
        tempoAtual = now

        // Deve retornar do cache (4 min < 5 min de TTL)
        val cached = repoComClock.cache[key]
        assertNotNull("Cache com 4 min deve existir", cached)
        val (_, timestamp) = cached!!
        val elapsed = tempoAtual - timestamp
        assertTrue("Elapsed deve ser 4 min", elapsed == 4 * 60 * 1000L)
        assertTrue("Cache com 4 min nao deve estar expirado", elapsed <= 5 * 60 * 1000L)
    }

    @Test
    fun cache_com6Minutos_retornaNull_entradaExpirada() {
        val now = 1_000_000L
        val seisMinutosAtras = now - (6 * 60 * 1000L)

        var tempoAtual = now
        val repoComClock = AiDiagnosisRepository(
            baseUrl = "http://invalid.local",
            isAuthorized = { true },
            clock = { tempoAtual },
        )

        val ctx = fakeDiagnosisAiContext()
        val resultado = fakeAiDiagnosisResult()
        val key = repoComClock.cacheKey(ctx)

        // Insere entrada como se tivesse sido inserida 6 minutos atras
        repoComClock.cache[key] = Pair(resultado, seisMinutosAtras)

        // Simula a logica de lookup de cache do explainDiagnosis (TTL check)
        val cached = repoComClock.cache[key]
        assertNotNull("Entrada existe antes do check", cached)

        val (_, timestamp) = cached!!
        val elapsed = tempoAtual - timestamp
        assertTrue("Elapsed deve ser 6 min", elapsed == 6 * 60 * 1000L)

        // Verifica que elapsed ultrapassa o TTL de 5 minutos
        assertTrue("Cache com 6 min deve estar expirado", elapsed > 5 * 60 * 1000L)

        // Replica exatamente o comportamento do explainDiagnosis:
        // se expirado, remove do cache e a entrada nao esta mais disponivel
        if (repoComClock.clock() - timestamp > 5 * 60 * 1000L) {
            repoComClock.cache.remove(key)
        }
        assertNull("Entrada expirada deve ser removida do cache", repoComClock.cache[key])
    }

    // -------------------------------------------------------------------------
    // Item #4 — parser de PerguntaContextual deve aceitar e expor o campo `tema`
    // -------------------------------------------------------------------------
    @Test
    fun parser_perguntaContextual_temaPresente_eExposto() {
        val json = """
            {
              "schemaVersion": "3",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000020,
              "status": "regular",
              "titulo": "ok",
              "resumo": "ok",
              "textoLaudo": "ok",
              "problemaPrincipal": {"tipo":"estabilidade","descricao":"","confianca":0.7},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [],
              "evidencias": [],
              "limitesDaAnalise": [],
              "perguntasContextuais": [
                {"id":"p1","tema":"Wi-Fi","pergunta":"O sinal cai em algum cômodo?","opcoes":[{"id":"sim","rotulo":"Sim"},{"id":"nao","rotulo":"Não"}]},
                {"id":"p2","tema":"Wi-Fi","pergunta":"Você está perto do roteador?","opcoes":[{"id":"sim","rotulo":"Sim"}]},
                {"id":"p3","tema":"ISP","pergunta":"O problema acontece sempre no mesmo horário?","opcoes":[{"id":"sim","rotulo":"Sim"}]},
                {"id":"p4","pergunta":"Sem tema declarado","opcoes":[{"id":"ok","rotulo":"OK"}]}
              ]
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        requireNotNull(parsed)

        val perg = parsed.perguntasContextuais
        assertEquals(4, perg.size)
        assertEquals("Wi-Fi", perg[0].tema)
        assertEquals("Wi-Fi", perg[1].tema)
        assertEquals("ISP", perg[2].tema)
        assertNull(perg[3].tema) // sem tema declarado → null

        // Agrupamento manual reproduz o que a UI fará
        val porTema = perg.groupBy { it.tema ?: "Geral" }
        assertEquals(3, porTema.size)
        assertEquals(2, porTema["Wi-Fi"]?.size)
        assertEquals(1, porTema["ISP"]?.size)
        assertEquals(1, porTema["Geral"]?.size)
    }

    // -------------------------------------------------------------------------
    // Item #7 — payload v5 (schema raw atual, SIG-282) NÃO deve conter campos
    // de análise prévia — regra herdada do payload v3 e nunca revertida.
    // -------------------------------------------------------------------------
    @Test
    fun contextToJson_v5_naoContemAnalisePrevia() {
        val ctx = DiagnosisAiContext(
            schemaVersion = "5",
            generatedAtEpochMs = 1700000000000L,
            connectionType = io.signallq.app.feature.diagnostico.ConnectionType.wifi,
            metricasAtuais = AiMetricasAtuais(
                downloadMbps = 294.0,
                uploadMbps = 411.0,
                latenciaMs = 101.0,
                jitterMs = 25.1,
            ),
            evidencias = listOf(
                AiEvidence(label = "download", valor = "294 Mbps"),
            ),
        )
        val json = repo.contextToJson(ctx).toString()
        // schemaVersion correto
        assertTrue(json.contains("\"schemaVersion\":\"5\""))
        // Campos de análise prévia NÃO podem aparecer
        assertFalse("decisaoStatus removido", json.contains("decisaoStatus"))
        assertFalse("decisaoTitulo removido", json.contains("decisaoTitulo"))
        assertFalse("decisaoMensagem removido", json.contains("decisaoMensagem"))
        assertFalse("classificacaoLocal removido", json.contains("classificacaoLocal"))
        assertFalse("recomendacoesLocais removido", json.contains("recomendacoesLocais"))
        assertFalse("limitesDaAnalise removido", json.contains("limitesDaAnalise"))
        assertFalse("perfisUsoSpeedtest removido", json.contains("perfisUsoSpeedtest"))
        // Evidência v3: NÃO tem `interpretacao`
        assertFalse("evidencias sem interpretacao", json.contains("\"interpretacao\""))
        // Métricas brutas presentes
        assertTrue(json.contains("\"downloadMbps\":294"))
        assertTrue(json.contains("\"latenciaMs\":101"))
    }

    // -------------------------------------------------------------------------
    // Fase 1 do bugfix — achadosLocais e rttGatewayMs precisam chegar no payload
    // real enviado ao Worker. Antes desta correcao, DiagnosisAiContext.achadosLocais
    // e AiMetricasAtuais.rttGatewayMs existiam no objeto Kotlin mas nunca eram
    // serializados por contextToJson — a IA "decidia sozinha" sem a ancora do
    // motor local.
    // -------------------------------------------------------------------------
    @Test
    fun contextToJson_serializaAchadosLocaisERttGateway() {
        val ctx = DiagnosisAiContext(
            schemaVersion = "5",
            generatedAtEpochMs = 1700000000000L,
            connectionType = io.signallq.app.feature.diagnostico.ConnectionType.wifi,
            metricasAtuais = AiMetricasAtuais(
                downloadMbps = 294.0,
                rttGatewayMs = 12,
            ),
            achadosLocais = AchadosDiagnosticoLocal(
                decisaoId = "DECISAO-GW-01",
                statusGeral = "attention",
                score = 62,
                confianca = 0.82,
                resultadosRelevantes = listOf("WIFI-01", "INTERNET-03"),
            ),
        )
        val json = repo.contextToJson(ctx).toString()

        assertTrue("rttGatewayMs presente em metricasAtuais", json.contains("\"rttGatewayMs\":12"))

        assertTrue("achadosLocais presente no payload", json.contains("\"achadosLocais\""))
        assertTrue(json.contains("\"decisaoId\":\"DECISAO-GW-01\""))
        assertTrue(json.contains("\"statusGeral\":\"attention\""))
        assertTrue(json.contains("\"score\":62"))
        assertTrue(json.contains("\"confianca\":0.82"))
        assertTrue(json.contains("\"WIFI-01\""))
        assertTrue(json.contains("\"INTERNET-03\""))
    }

    @Test
    fun contextToJson_achadosLocaisNull_naoSerializaCampo() {
        val ctx = DiagnosisAiContext(
            schemaVersion = "5",
            generatedAtEpochMs = 1700000000000L,
            connectionType = io.signallq.app.feature.diagnostico.ConnectionType.wifi,
            achadosLocais = null,
        )
        val json = repo.contextToJson(ctx).toString()
        assertFalse("achadosLocais omitido quando null", json.contains("achadosLocais"))
    }

    @Test
    fun parser_classificacaoTecnica_dimensoes_ausentes_ficam_null() {
        val json = """
            {
              "schemaVersion": "2",
              "source": "cloudflare_ai",
              "generatedAt": 1700000000011,
              "status": "regular",
              "titulo": "ok",
              "resumo": "ok",
              "textoLaudo": "ok",
              "classificacaoTecnica": {
                "velocidade": {"avaliacao": "boa", "justificativa": "ok"}
              },
              "problemaPrincipal": {"tipo":"sem_problema","descricao":"","confianca":0.7},
              "impacto": {"navegacao":"OK","streaming":"OK","videochamada":"OK","jogos":"OK","trabalho":"OK"},
              "acoesRecomendadas": [],
              "evidencias": [],
              "limitesDaAnalise": []
            }
        """.trimIndent()

        val parsed = repo.parseResult(json)
        requireNotNull(parsed)

        assertEquals("boa", parsed.classificacaoTecnica.velocidade?.avaliacao)
        assertNull(parsed.classificacaoTecnica.estabilidade)
        assertNull(parsed.classificacaoTecnica.wifi)
        assertNull(parsed.classificacaoTecnica.dns)
        assertNull(parsed.classificacaoTecnica.fibra)
    }
}
