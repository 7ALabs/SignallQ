package io.veloo.app.feature.diagnostico.ai

import io.veloo.app.feature.diagnostico.BandaWifi
import io.veloo.app.feature.diagnostico.ConnectionType
import io.veloo.app.feature.diagnostico.DiagnosticInput
import io.veloo.app.feature.diagnostico.DiagnosticReport
import io.veloo.app.feature.diagnostico.DiagnosticStatus
import io.veloo.app.feature.diagnostico.HistoricalDiagnosticInput
import io.veloo.app.feature.diagnostico.InternetDiagnosticInput
import io.veloo.app.feature.diagnostico.SpeedtestQualityInput
import io.veloo.app.feature.diagnostico.WifiDiagnosticInput

// =============================================================================
// Schema v2 do payload enviado para o Worker
// =============================================================================
// Todos os campos novos sao opcionais. Quando uma metrica nao existe, mandamos
// null/omitimos. O Worker (e a IA) usa apenas o que esta presente.
// O servidor permanece o mesmo endpoint /api/ai/diagnostico-conexao.

/** Versao do prompt/modelo enviada ao Worker. Concatenada na cache key para
 *  evitar reaproveitar respostas geradas com prompts/modelos antigos.
 *
 *  Histórico de versões:
 *   - "diagnostico_v2"          — schema v2 com Llama 3.3 70B como padrão.
 *   - "diagnostico_v2_gemma4"   — troca para Gemma 4 26B (Google) como motor
 *                                 padrão; Llama deixa de ser usado pela rota
 *                                 cloud.
 *   - "diagnostico_v3_raw"      — payload reformatado: SOMENTE dados brutos
 *                                 (sem classificação local, sem decisão local,
 *                                 sem rótulos em evidências). A IA faz toda
 *                                 a análise. Cobertura ampliada: contextoRede
 *                                 com SSID/BSSID/canal/banda/segurança, rede
 *                                 (ISP/ASN/IP/DNS), histórico bruto, dispositivo,
 *                                 móvel (quando aplicável). Esta bump invalida
 *                                 todo cache anterior.
 *   - "diagnostico_v4_guided"   — adiciona campo `achadosLocais` com a decisão
 *                                 do engine determinístico local (id, status, score,
 *                                 confiança, findings relevantes). A IA valida e
 *                                 explica — não precisa mais decidir do zero.
 */
const val AI_PROMPT_VERSION = "diagnostico_v4_guided"

// =============================================================================
// Schema v3 — APENAS DADOS BRUTOS
// =============================================================================
// O Worker e a Gemma fazem toda interpretacao/classificacao/decisao.
// Removido em relacao a v2:
//  - decisaoStatus, decisaoTitulo, decisaoMensagem  (analise local)
//  - classificacaoLocal                              (heuristica local)
//  - recomendacoesLocais                             (substituida por feedbackUsuario)
//  - limitesDaAnalise                                (analise local)
//  - perfisUsoSpeedtest                              (vereditos pre-computados)
//  - campo `interpretacao` em evidencias             (analise local)
// Adicionado:
//  - rede (ISP/ASN/IP/DNS), movel (quando aplicavel), dispositivos
//  - contextoRede ampliado: BSSID, larguraCanal, linkSpeed, padrao, seguranca
//  - feedbackUsuario (texto livre quando o usuario relata algo)
//  - evidencias agora sao raw (label, valor) sem campo interpretacao

data class DiagnosisAiContext(
    /** "3" para o schema raw atual. Worker tolera "2" e "1" tambem (retrocompat). */
    val schemaVersion: String = "3",
    val generatedAtEpochMs: Long,
    val connectionType: ConnectionType,
    /** Metricas brutas do speedtest mais recente. */
    val metricasAtuais: AiMetricasAtuais? = null,
    /** Contexto de rede bruto (Wi-Fi, redes vizinhas, link properties). */
    val contextoRede: AiContextoRede? = null,
    /** Informacoes de rede publica/ISP (operadora, ASN, IP, DNS). */
    val rede: AiRedeInfo? = null,
    /** Telefonia movel (somente quando connectionType=mobile). */
    val movel: AiMovelInfo? = null,
    /** Dispositivo do usuario (modelo/SO). */
    val dispositivos: AiDispositivosInfo? = null,
    /** Historico bruto: medias e ultimos testes. */
    val historico: AiHistoricoResumo? = null,
    /** Evidencias brutas: rotulo + valor, sem interpretacao. */
    val evidencias: List<AiEvidence> = emptyList(),
    /** Texto livre que o usuario digitou descrevendo o problema, se houver. */
    val feedbackUsuario: String? = null,
    /**
     * Achados do engine determinístico local (DiagnosticDecisionEngine).
     * Quando presente, a IA deve validar e explicar — não decidir do zero.
     * Null em fluxos legados ou quando o engine não rodou.
     */
    val achadosLocais: AchadosDiagnosticoLocal? = null,
    /**
     * Instrucao de tom derivada da contagem de metricas ruins (jitter >50ms,
     * perda >=2%, RTT >150ms). Guia o Worker/IA no estilo narrativo da resposta.
     * Null quando nao ha metricas suficientes para calcular.
     *
     * Valores possiveis:
     *   "Detectei..."                              — 2+ metricas ruins (problema claro)
     *   "Sua conexao esta funcionando, mas..."     — 1 metrica ruim (problema parcial)
     *   "Tudo dentro do esperado."                 — 0 metricas ruins (conexao saudavel)
     */
    val instrucaoTom: String? = null,
)

/** Rotulo + valor brutos. Sem campo `interpretacao` — analise e da IA. */
data class AiEvidence(
    val label: String,
    val valor: String,
)

/**
 * Achados estruturados do DiagnosticDecisionEngine local.
 * Enviados ao Worker para que a IA explique/valide em vez de decidir do zero.
 */
data class AchadosDiagnosticoLocal(
    /** Id da decisão final do engine (ex: "DECISAO-GW-01", "DECISAO-04"). */
    val decisaoId: String,
    /** Status geral: "ok", "info", "attention", "critical", "inconclusive". */
    val statusGeral: String,
    /** Score 0–100 derivado do status e podeConcluir. */
    val score: Int,
    /** Confiança do engine na conclusão (0.0–1.0). */
    val confianca: Double,
    /** Ids dos findings de atenção/críticos que sustentam a conclusão. */
    val resultadosRelevantes: List<String> = emptyList(),
)

data class AiMetricasAtuais(
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latenciaMs: Double? = null,
    val jitterMs: Double? = null,
    val perdaPacotesPercentual: Double? = null,
    val bufferbloatMs: Double? = null,
    val severidadeBufferbloat: String? = null,
    val stabilityScore: Double? = null,
    val peakDownloadMbps: Double? = null,
    val peakUploadMbps: Double? = null,
    val latencyDownloadMs: Double? = null,
    val latencyUploadMs: Double? = null,
    val packetLossSource: String? = null,
    /** RTT TCP para o gateway local em ms. Null se não disponível. */
    val rttGatewayMs: Int? = null,
)

data class AiContextoRede(
    val tipoConexao: String? = null,
    val ssid: String? = null,
    val bssid: String? = null,
    val rssi: Int? = null,
    val bandaWifi: String? = null,
    val canal: Int? = null,
    val larguraCanalMhz: Int? = null,
    val frequenciaMhz: Int? = null,
    val linkSpeedMbps: Int? = null,
    val padraoWifi: String? = null,
    val seguranca: String? = null,
    val privateDnsAtivo: Boolean? = null,
    val privateDnsHostname: String? = null,
    val redesProximas: List<AiRedeVizinha> = emptyList(),
)

data class AiRedeVizinha(
    val ssid: String? = null,
    val bssid: String? = null,
    val rssiDbm: Int? = null,
    val frequenciaMhz: Int? = null,
    val canal: Int? = null,
    val seguranca: String? = null,
)

data class AiRedeInfo(
    val operadora: String? = null,
    val asn: String? = null,
    val ipPublico: String? = null,
    val ipLocal: String? = null,
    val pais: String? = null,
    val regiao: String? = null,
    val dnsResolverIp: String? = null,
    val dnsResolverProvider: String? = null,
    val dnsLatenciaMs: Int? = null,
    val gatewayIp: String? = null,
    val servidorTesteCidade: String? = null,
)

data class AiMovelInfo(
    val operadora: String? = null,
    val tecnologia: String? = null,
    val rsrpDbm: Int? = null,
    val rsrqDb: Int? = null,
    val sinrDb: Int? = null,
    val ecnoDb: Int? = null,
    val bandaMovel: String? = null,
    val cellId: String? = null,
    val mcc: String? = null,
    val mnc: String? = null,
    val tac: String? = null,
    val roaming: Boolean? = null,
)

data class AiDispositivosInfo(
    val fabricante: String? = null,
    val modelo: String? = null,
    val sistema: String? = null,
    val versaoSO: String? = null,
    val quantidadeNaRede: Int? = null,
)

data class AiHistoricoResumo(
    val media7d: AiHistoricoMedia? = null,
    val media30d: AiHistoricoMedia? = null,
    /** Ultimos N testes brutos (sem interpretacao). */
    val ultimosTestes: List<AiTesteHistorico> = emptyList(),
)

data class AiHistoricoMedia(
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val pingMs: Double? = null,
    val testes: Int? = null,
)

data class AiTesteHistorico(
    val timestampEpochMs: Long,
    val downloadMbps: Double? = null,
    val uploadMbps: Double? = null,
    val latenciaMs: Double? = null,
    val jitterMs: Double? = null,
    val perdaPercentual: Double? = null,
    val connectionType: String? = null,
)

// =============================================================================
// Schema v2 da resposta da IA (parseado pelo cliente Kotlin)
// =============================================================================
// Todos os campos novos sao opcionais para nao quebrar respostas v1.

data class AiDiagnosisResult(
    val schemaVersion: String,
    val source: String,
    val generatedAt: Long,
    val status: String,
    val titulo: String,
    val resumo: String,
    val problemaPrincipal: AiProblemaPrincipal,
    val impacto: AiImpacto,
    val acoesRecomendadas: List<AiAcaoRecomendada>,
    val evidencias: List<AiEvidenceOut>,
    val textoLaudo: String,
    val limitesDaAnalise: List<String>,
    // Campos v2 (opcionais para retrocompat com v1)
    val modeloIa: ModeloIa = ModeloIa.unknown(),
    val classificacaoTecnica: ClassificacaoTecnica = ClassificacaoTecnica(),
    val hipotesesDescartadas: List<HipoteseDescartada> = emptyList(),
    val perguntasContextuais: List<PerguntaContextual> = emptyList(),
    // Tokens de uso — parseados do bloco "usage" do Cloudflare Workers AI response.
    // Default 0 se ausentes (worker antigo ou fallback local).
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
)

data class AiProblemaPrincipal(
    val tipo: String,
    val descricao: String,
    val confianca: Double,
)

data class AiImpacto(
    val navegacao: String,
    val streaming: String,
    val videochamada: String,
    val jogos: String,
    val trabalho: String,
)

data class AiAcaoRecomendada(
    val titulo: String,
    val descricao: String,
    val prioridade: String,
    /** "validacao_local"|"ajuste_roteador"|"ajuste_dispositivo"|"contato_isp"|
     *  "observacao"|"reteste". v1 nao tinha; usamos "" como default. */
    val tipo: String = "",
    /** v2: indica se a acao pode ser executada por uma feature nativa do app
     *  (ex.: re-rodar speedtest, abrir lista de redes Wi-Fi). v1 nao tinha. */
    val executavelNoApp: Boolean = false,
)

data class AiEvidenceOut(
    val label: String,
    val valor: String,
    val interpretacao: String,
)

/** Identificacao do motor de IA usado, em linguagem comercial. Nunca exibir
 *  `idInterno` para o usuario final — usar `nomeExibicao`/`nomeCompletoComercial`/
 *  `textoRodape`. */
data class ModeloIa(
    /** Ex.: "@cf/google/gemma-4-26b-a4b-it". Apenas para debug interno —
     *  NUNCA exibir ao usuario final. Use `nomeExibicao`/`textoRodape`. */
    val idInterno: String = "",
    val provedor: String = "",
    val familia: String = "",
    val versao: String? = null,
    val tamanho: String? = null,
    val variante: String? = null,
    val nomeExibicao: String = "SignallQ IA",
    val nomeCompletoComercial: String = "SignallQ IA",
    val descricaoComercial: String = "",
    val textoRodape: String = "Motor de análise: SignallQ IA",
) {
    companion object {
        fun unknown(): ModeloIa = ModeloIa()

        /** Usado pelo fallback local quando nenhuma IA respondeu. */
        fun localFallback(): ModeloIa = ModeloIa(
            provedor = "local",
            familia = "Local",
            nomeExibicao = "Diagnóstico local",
            nomeCompletoComercial = "Diagnóstico local do SignallQ",
            descricaoComercial = "Análise feita pelo próprio app, sem IA externa.",
            textoRodape = "Motor de análise: Diagnóstico local do SignallQ",
        )
    }
}

data class ClassificacaoTecnica(
    val velocidade: ClassificacaoItem? = null,
    val estabilidade: ClassificacaoItem? = null,
    val wifi: ClassificacaoItem? = null,
    val dns: ClassificacaoItem? = null,
    val fibra: ClassificacaoItem? = null,
)

data class ClassificacaoItem(
    /** "boa"|"regular"|"ruim"|"inconclusiva"|"nao_avaliado" — opcional para tolerar campos faltando. */
    val avaliacao: String? = null,
    val justificativa: String? = null,
)

data class HipoteseDescartada(
    val hipotese: String,
    val motivo: String,
)

data class PerguntaContextual(
    val id: String,
    val pergunta: String,
    /** Tema/agrupamento da pergunta (ex.: "Wi-Fi", "ISP", "Roteador"). A UI
     *  empilha perguntas do mesmo tema em um unico card expansivel. null
     *  trata como "Geral". Campo opcional para retrocompat com schema v1/v2. */
    val tema: String? = null,
    val opcoes: List<OpcaoPerguntaContextual> = emptyList(),
)

data class OpcaoPerguntaContextual(
    val id: String,
    val rotulo: String,
)

// =============================================================================
// Helpers de UI — normalizacao de enums vindos do JSON.
// =============================================================================
// O Worker emite os enums sem acento por compatibilidade (`Instavel`,
// `Indisponivel`, `Alta latencia`, etc.) para evitar problemas de encoding
// e variacoes vindas da IA. A UI deve exibir com acento. Estas funcoes
// fazem o mapeamento canonico e mantem o original como fallback se vier
// algo nao previsto, evitando "engolir" strings desconhecidas.

fun normalizeImpactoLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return when (raw.trim().lowercase()) {
        "ok" -> "OK"
        "lento" -> "Lento"
        "instavel", "instável" -> "Instável"
        "indisponivel", "indisponível" -> "Indisponível"
        "alta latencia", "alta latência" -> "Alta latência"
        "comprometido" -> "Comprometido"
        "comprometida" -> "Comprometida"
        else -> raw
    }
}

fun normalizeClassificacaoLabel(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    return when (raw.trim().lowercase()) {
        "boa" -> "Boa"
        "regular" -> "Regular"
        "ruim" -> "Ruim"
        "inconclusiva" -> "Inconclusiva"
        "nao_avaliado", "não_avaliado" -> "Não avaliado"
        else -> raw
    }
}

// =============================================================================
// Factory do contexto enviado ao Worker
// =============================================================================

object DiagnosisAiContextFactory {

    /**
     * Overload basica para call sites legados que so tem o report.
     * Sem dados brutos extras — o payload sai enxuto, so com evidencias raw
     * extraidas do report. Util para fluxos de teste.
     */
    fun from(report: DiagnosticReport, connectionType: ConnectionType): DiagnosisAiContext =
        buildContext(report, connectionType, input = null)

    /**
     * Overload media — recebe tambem o `DiagnosticInput` (metricas crus do
     * speedtest + Wi-Fi). Usada hoje pelo SignallQOrchestrator.
     */
    fun from(
        report: DiagnosticReport,
        input: DiagnosticInput?,
        connectionType: ConnectionType,
    ): DiagnosisAiContext = buildContext(report, connectionType, input)

    /**
     * Overload completa v3 — recebe TODO o contexto bruto disponivel no app.
     * Todos os parametros novos sao opcionais; quando null, o campo
     * correspondente no payload e omitido. **Nenhum desses campos contem
     * analise local** — sao apenas dados crus que a IA consome livremente.
     */
    fun fromRaw(
        report: DiagnosticReport,
        input: DiagnosticInput?,
        connectionType: ConnectionType,
        wifiLinkBssid: String? = null,
        wifiPadrao: String? = null,
        wifiLinkSpeedMbps: Int? = null,
        privateDnsAtivo: Boolean? = null,
        privateDnsHostname: String? = null,
        redesProximas: List<AiRedeVizinha> = emptyList(),
        ispNome: String? = null,
        ispAsn: String? = null,
        ipPublico: String? = null,
        ipLocal: String? = null,
        pais: String? = null,
        regiao: String? = null,
        gatewayIp: String? = null,
        dnsResolverIp: String? = null,
        dnsResolverProvider: String? = null,
        dnsLatenciaMs: Int? = null,
        servidorTesteCidade: String? = null,
        ultimosTestesHistorico: List<AiTesteHistorico> = emptyList(),
        movel: AiMovelInfo? = null,
        dispositivos: AiDispositivosInfo? = null,
        feedbackUsuario: String? = null,
        speedtestExtras: SpeedtestExtras? = null,
    ): DiagnosisAiContext {
        val base = buildContext(report, connectionType, input)
        val metricasComExtras = (base.metricasAtuais ?: AiMetricasAtuais()).copy(
            severidadeBufferbloat = speedtestExtras?.severidadeBufferbloat,
            stabilityScore = speedtestExtras?.stabilityScore,
            peakDownloadMbps = speedtestExtras?.peakDownloadMbps,
            peakUploadMbps = speedtestExtras?.peakUploadMbps,
            latencyDownloadMs = speedtestExtras?.latencyDownloadMs,
            latencyUploadMs = speedtestExtras?.latencyUploadMs,
            packetLossSource = speedtestExtras?.packetLossSource,
        )
        val contextoComExtras = (base.contextoRede ?: AiContextoRede(tipoConexao = connectionType.name)).copy(
            bssid = wifiLinkBssid,
            linkSpeedMbps = wifiLinkSpeedMbps,
            padraoWifi = wifiPadrao,
            privateDnsAtivo = privateDnsAtivo,
            privateDnsHostname = privateDnsHostname,
            redesProximas = redesProximas,
        )
        val rede = if (
            ispNome != null || ispAsn != null || ipPublico != null || ipLocal != null ||
            pais != null || regiao != null || gatewayIp != null ||
            dnsResolverIp != null || dnsResolverProvider != null || dnsLatenciaMs != null ||
            servidorTesteCidade != null
        ) {
            AiRedeInfo(
                operadora = ispNome,
                asn = ispAsn,
                ipPublico = ipPublico,
                ipLocal = ipLocal,
                pais = pais,
                regiao = regiao,
                gatewayIp = gatewayIp,
                dnsResolverIp = dnsResolverIp,
                dnsResolverProvider = dnsResolverProvider,
                dnsLatenciaMs = dnsLatenciaMs,
                servidorTesteCidade = servidorTesteCidade,
            )
        } else null

        val historicoFinal = base.historico?.copy(ultimosTestes = ultimosTestesHistorico)
            ?: ultimosTestesHistorico.takeIf { it.isNotEmpty() }?.let { AiHistoricoResumo(ultimosTestes = it) }

        return base.copy(
            metricasAtuais = metricasComExtras,
            contextoRede = contextoComExtras,
            rede = rede,
            movel = movel,
            dispositivos = dispositivos,
            historico = historicoFinal,
            feedbackUsuario = feedbackUsuario,
        )
    }

    private fun buildContext(
        report: DiagnosticReport,
        connectionType: ConnectionType,
        input: DiagnosticInput?,
    ): DiagnosisAiContext {
        val evidencias = mutableListOf<AiEvidence>()

        val all =
            report.wifiResultados +
                report.internetResultados +
                report.mobileResultados +
                report.fibraResultados +
                report.dnsResultados +
                report.historicoResultados +
                report.wifiCanalResultados

        // Evidencias brutas: rotulo + valor. Sem campo `interpretacao` —
        // a interpretacao e da IA. r.titulo (que era usado como interpretacao)
        // nao vai mais ao payload.
        all.forEach { r ->
            r.evidencia?.let { ev ->
                evidencias.add(AiEvidence(label = "${r.categoria}:${r.id}", valor = ev))
            }
        }

        val metricas = input?.internet?.let { internetToMetricas(it) }
        val contextoRede = input?.let { contextoFrom(it, connectionType) }
        val historico = input?.historico?.let { historicoFrom(it) }

        val findingsRelevantes =
            (report.wifiResultados + report.internetResultados + report.mobileResultados +
                report.fibraResultados + report.dnsResultados + report.historicoResultados +
                report.wifiCanalResultados)
                .filter { it.status == DiagnosticStatus.critical || it.status == DiagnosticStatus.attention }
                .map { it.id }

        val achadosLocais = AchadosDiagnosticoLocal(
            decisaoId = report.decisao.id,
            statusGeral = report.decisao.status.name,
            score = report.scoreConexao,
            confianca = report.confianca,
            resultadosRelevantes = findingsRelevantes,
        )

        return DiagnosisAiContext(
            schemaVersion = "4",
            generatedAtEpochMs = report.geradoEmMs,
            connectionType = connectionType,
            metricasAtuais = metricas,
            contextoRede = contextoRede,
            historico = historico,
            evidencias = evidencias,
            achadosLocais = achadosLocais,
            instrucaoTom = buildToneInstruction(metricas),
        )
    }

    /**
     * Deriva a instrucao de tom a partir da contagem de metricas ruins.
     *
     * Thresholds:
     *   - jitter > 50 ms
     *   - perda de pacotes >= 2 %
     *   - RTT gateway > 150 ms
     *
     * Retorna null quando nao ha metricas disponíveis (input null).
     */
    internal fun buildToneInstruction(metricas: AiMetricasAtuais?): String? {
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

    private fun internetToMetricas(i: InternetDiagnosticInput): AiMetricasAtuais =
        AiMetricasAtuais(
            downloadMbps = i.downloadMbps,
            uploadMbps = i.uploadMbps,
            latenciaMs = i.latencyMs,
            jitterMs = i.jitterMs,
            perdaPacotesPercentual = i.perdaPercentual,
            bufferbloatMs = i.bufferbloatMs,
            rttGatewayMs = i.rttGatewayMs,
        )

    private fun contextoFrom(input: DiagnosticInput, connectionType: ConnectionType): AiContextoRede {
        val w = input.wifi
        return AiContextoRede(
            tipoConexao = connectionType.name,
            ssid = w?.ssid,
            rssi = w?.rssiDbm,
            bandaWifi = when (w?.frequenciaMhz) {
                null -> null
                in 0..2999 -> BandaWifi.ghz24.name
                else -> BandaWifi.ghz5.name
            },
            canal = w?.canal,
            frequenciaMhz = w?.frequenciaMhz,
        )
    }

    private fun historicoFrom(h: HistoricalDiagnosticInput): AiHistoricoResumo {
        val media7d = if (
            h.avgDownload7d != null || h.avgUpload7d != null || h.avgPing7d != null || h.testsCount7d > 0
        ) {
            AiHistoricoMedia(
                downloadMbps = h.avgDownload7d,
                uploadMbps = h.avgUpload7d,
                pingMs = h.avgPing7d,
                testes = h.testsCount7d.takeIf { it > 0 },
            )
        } else null
        val media30d = if (
            h.avgDownload30d != null || h.avgUpload30d != null || h.avgPing30d != null || h.testsCount30d > 0
        ) {
            AiHistoricoMedia(
                downloadMbps = h.avgDownload30d,
                uploadMbps = h.avgUpload30d,
                pingMs = h.avgPing30d,
                testes = h.testsCount30d.takeIf { it > 0 },
            )
        } else null
        return AiHistoricoResumo(media7d = media7d, media30d = media30d)
    }
}

/**
 * Contexto adicional bruto que o app coleta fora do `DiagnosticInput` mas
 * agrega ao payload v3. Chamada via lambda do SignallQOrchestrator pra manter
 * o feature module desacoplado do MainViewModel/host.
 */
data class AdditionalAiContext(
    val ispNome: String? = null,
    val ispAsn: String? = null,
    val ipPublico: String? = null,
    val ipLocal: String? = null,
    val pais: String? = null,
    val regiao: String? = null,
    val gatewayIp: String? = null,
    val dnsResolverIp: String? = null,
    val dnsResolverProvider: String? = null,
    val dnsLatenciaMs: Int? = null,
    val servidorTesteCidade: String? = null,
    val ultimosTestesHistorico: List<AiTesteHistorico> = emptyList(),
    val redesProximas: List<AiRedeVizinha> = emptyList(),
    val movel: AiMovelInfo? = null,
    val dispositivos: AiDispositivosInfo? = null,
    val privateDnsAtivo: Boolean? = null,
    val privateDnsHostname: String? = null,
    val wifiBssid: String? = null,
    val wifiPadrao: String? = null,
    val wifiLinkSpeedMbps: Int? = null,
    val speedtestExtras: SpeedtestExtras? = null,
)

/** Extras opcionais do speedtest que ampliam metricasAtuais para a IA. */
data class SpeedtestExtras(
    val severidadeBufferbloat: String? = null,
    val stabilityScore: Double? = null,
    val peakDownloadMbps: Double? = null,
    val peakUploadMbps: Double? = null,
    val latencyDownloadMs: Double? = null,
    val latencyUploadMs: Double? = null,
    val packetLossSource: String? = null,
)

// =============================================================================
// Uso de tokens no streaming
// =============================================================================

/**
 * Tokens capturados do evento SSE de usage enviado pelo Cloudflare Workers AI
 * ao fim do streaming. Populado em [AiDiagnosisRepository.lastStreamUsage]
 * e lido pelo ViewModel para persistir em [ChatSessionEntity].
 */
data class AiStreamUsage(
    val promptTokens: Int,
    val completionTokens: Int,
    val totalTokens: Int,
)

// =============================================================================
// Fallback local
// =============================================================================
// Quando a IA falha (sem auth, timeout, !2xx, JSON invalido), produzimos um
// AiDiagnosisResult valido a partir do diagnostico local. O `modeloIa` e
// preenchido como "Diagnostico local do SignallQ" — nao mente sobre uso de IA.

object AiFallbackFactory {
    fun fromLocal(report: DiagnosticReport): AiDiagnosisResult {
        val status =
            when (report.decisao.status) {
                DiagnosticStatus.ok -> "bom"
                DiagnosticStatus.info -> "bom"
                DiagnosticStatus.attention -> "regular"
                DiagnosticStatus.critical -> "critico"
                DiagnosticStatus.inconclusive -> "inconclusivo"
            }

        val mensagem = report.decisao.mensagemUsuario.ifBlank { "Diagnóstico concluído pelo SignallQ." }

        return AiDiagnosisResult(
            schemaVersion = "3",
            source = "local",
            generatedAt = System.currentTimeMillis(),
            status = status,
            titulo = report.decisao.titulo,
            resumo = mensagem,
            problemaPrincipal =
                AiProblemaPrincipal(
                    tipo = report.decisao.categoria,
                    descricao = mensagem,
                    confianca = if (report.decisao.podeConcluir) 0.8 else 0.5,
                ),
            impacto =
                AiImpacto(
                    navegacao = "Pode variar conforme o problema detectado.",
                    streaming = "Pode variar conforme o problema detectado.",
                    videochamada = "Pode variar conforme o problema detectado.",
                    jogos = "Pode variar conforme o problema detectado.",
                    trabalho = "Pode variar conforme o problema detectado.",
                ),
            acoesRecomendadas =
                listOfNotNull(
                    report.decisao.recomendacao?.let {
                        AiAcaoRecomendada(
                            titulo = "Proxima acao",
                            descricao = it,
                            prioridade = "media",
                            tipo = "observacao",
                            executavelNoApp = false,
                        )
                    },
                ),
            evidencias = emptyList(),
            textoLaudo = mensagem,
            limitesDaAnalise = listOf("Fallback local (sem IA)."),
            modeloIa = ModeloIa.localFallback(),
            classificacaoTecnica = ClassificacaoTecnica(),
            hipotesesDescartadas = emptyList(),
            perguntasContextuais = emptyList(),
        )
    }
}
