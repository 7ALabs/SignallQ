package io.signallq.app.feature.diagnostico

data class DiagnosticReport(
    val wifiResultados: List<DiagnosticResult>,
    val internetResultados: List<DiagnosticResult>,
    val mobileResultados: List<DiagnosticResult> = emptyList(),
    val fibraResultados: List<DiagnosticResult>,
    val dnsResultados: List<DiagnosticResult> = emptyList(),
    val historicoResultados: List<DiagnosticResult> = emptyList(),
    val wifiCanalResultados: List<DiagnosticResult> = emptyList(),
    val redeResultados: List<DiagnosticResult> = emptyList(),
    val decisao: DiagnosticResult,
    /** Achados secundários produzidos pelo [FindingEngine] — causas independentes
     *  detectadas junto do achado principal ([decisao]). Vazio quando só há um
     *  achado (comportamento equivalente ao engine antigo, que reportava só um). */
    val achadosSecundarios: List<DiagnosticResult> = emptyList(),
    /** Hipóteses avaliadas pelo [FindingEngine] cujo gatilho bateu mas foram
     *  suprimidas por evidência mais forte da mesma causa raiz (ex.: DNS crítico
     *  quando fibra também está crítica). Ver [FindingEngine] para o critério. */
    val hipotesesDescartadas: List<DiagnosticResult> = emptyList(),
    /** Fontes de dado ausentes que, se disponíveis, poderiam refinar o diagnóstico
     *  (ex.: "rttGateway", "fibra"). */
    val dadosAusentes: List<String> = emptyList(),
    /** Limitações honestas do equipamento de rede local (ONT/roteador) detectado
     *  — ex.: "este roteador não informa fibra", "suporte experimental" (GH#542,
     *  epic #547). Vazio quando não há equipamento local ou nenhuma limitação se
     *  aplica. Calculado por [FindingEngine]. */
    val limitacoesEquipamentoLocal: List<String> = emptyList(),
    /** Recomendações práticas geradas pelo [RecommendationEngine] a partir dos
     *  achados do [FindingEngine] — as 12 situações documentadas na skill
     *  `motor-diagnostico`. Aditivo: pode ter zero, uma ou várias simultâneas. */
    val recomendacoes: List<DiagnosticResult> = emptyList(),
    val perfisUsoSpeedtest: SpeedtestQualityInput? = null,
    /** Resultado do [ScoreEngine] (SIG-288) — pontuacao ponderada por dimensao,
     *  com reponderacao automatica quando falta dado confiavel. Nulo quando o
     *  [DiagnosticRunner] nao recebeu [DiagnosticInput] suficiente para calcular
     *  nenhuma dimensao; nesse caso [scoreConexao] cai para a tabela legada baseada
     *  em [decisao].status. */
    val scoreEngineResultado: ScoreResult? = null,
    /** Perfis de uso (SIG-289) — Navegacao/Streaming/Jogos/Videochamada/Trabalho,
     *  calculados localmente pelo [UsageProfileClassifier]. Substitui o antigo
     *  `result.impacto.*` (texto livre decidido pela IA). Vazio quando o
     *  [DiagnosticRunner] nao recebeu [DiagnosticInput] (nunca deve acontecer em
     *  producao — [DiagnosticRunner.run] sempre calcula a partir do input recebido). */
    val perfisUso: List<UsageProfileClassifier.UsageProfileResult> = emptyList(),
    /** Prontidao para jogos por categoria (SIG-290), calculada localmente pelo
     *  [GameReadinessClassifier]. Usado pelo "ver detalhes" do card Jogos em vez de
     *  recalcular na UI. Vazio quando o [DiagnosticRunner] nao recebeu [DiagnosticInput]
     *  (nunca deve acontecer em producao). */
    val gameReadiness: List<GameReadinessClassifier.GameReadinessResult> = emptyList(),
    val geradoEmMs: Long,
) {
    private val todos: List<DiagnosticResult>
        get() =
            wifiResultados +
                internetResultados +
                mobileResultados +
                fibraResultados +
                dnsResultados +
                historicoResultados +
                wifiCanalResultados +
                redeResultados +
                listOf(decisao)

    val temCritico: Boolean get() = todos.any { it.status == DiagnosticStatus.critical }
    val temAtencao: Boolean get() = todos.any { it.status == DiagnosticStatus.attention }

    // Score 0–100. Preferencialmente calculado pelo ScoreEngine (SIG-288), que pondera
    // por dimensao (estabilidade/wifi/velocidade/dns/historico/fibra/sinal movel) em vez
    // de derivar so do status categorico da decisao final. Cai para a tabela legada
    // (ok=90, info=75, attention=65/55, critical=25/15, inconclusive=50) quando o
    // ScoreEngine nao teve nenhuma dimensao com dado disponivel.
    val scoreConexao: Int
        get() =
            scoreEngineResultado?.score ?: when (decisao.status) {
                DiagnosticStatus.ok -> 90
                DiagnosticStatus.info -> 75
                DiagnosticStatus.attention -> if (decisao.podeConcluir) 55 else 65
                DiagnosticStatus.critical -> if (decisao.podeConcluir) 15 else 25
                DiagnosticStatus.inconclusive -> 50
            }

    // Veredito legível para o usuário baseado no score.
    val veredito: String
        get() =
            when {
                scoreConexao >= 85 -> "Excelente"
                scoreConexao >= 65 -> "Bom"
                scoreConexao >= 40 -> "Regular"
                else -> "Fraco"
            }

    // Confiança do diagnóstico (0.0–1.0). Alta quando a decisão é conclusiva.
    val confianca: Double
        get() =
            when {
                decisao.status == DiagnosticStatus.inconclusive -> 0.30
                decisao.status == DiagnosticStatus.ok -> 0.90
                decisao.podeConcluir -> 0.88
                else -> 0.65
            }
}
