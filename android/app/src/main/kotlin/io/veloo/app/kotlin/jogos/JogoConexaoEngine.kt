package io.signallq.app.jogos

import io.signallq.app.core.network.EstadoConexao
import io.signallq.app.core.network.WifiLinkSnapshot
import io.signallq.app.feature.speedtest.PingResultado

/** Nivel final do teste — mesmos 4 estados do spec (Excelente/Boa/Atencao/Ruim), rotulos
 *  de UI ficam em [tituloVeredito]/[textoVeredito] pra bater exatamente com o texto do
 *  documento funcional. */
enum class NivelResultado { EXCELENTE, BOA, ATENCAO, RUIM }

data class ResultadoTesteJogo(
    val jogo: JogoCatalogo,
    val plataforma: Plataforma,
    val nivel: NivelResultado,
    val tituloVeredito: String,
    val textoVeredito: String,
    val latenciaMs: Double,
    val jitterMs: Double,
    val perdaPercentual: Double,
    val regiaoTestada: String,
    val estrategiaUsada: EstrategiaTeste,
    val tipoConexaoAtual: EstadoConexao,
    val avisos: List<String>,
    val recomendacoes: List<String>,
)

/**
 * Motor de avaliacao da tela Jogos — GH#935. Consome a medicao ja coletada (via
 * [io.signallq.app.feature.speedtest.PingExecutor] apontado para o
 * `game-latency-probe-worker`, ver [JogosViewModel]) e aplica os thresholds do perfil
 * do jogo selecionado.
 *
 * Prioridade de avaliacao do spec (perda > jitter > estabilidade > latencia > Wi-Fi >
 * velocidade) e obtida por "pior metrica vence": a faixa de perda do spec ja rebaixa pra
 * Ruim a partir de >1%, entao qualquer perda relevante domina o veredito mesmo com
 * latencia/jitter bons — sem precisar de excecao hardcoded (ex.: 25ms+3% perda = Ruim,
 * 55ms+0% perda+jitter baixo = Boa, ambos batem so com o worst-metric).
 *
 * "Estabilidade" (item 3 da prioridade do spec) nao tem faixa numerica propria na tabela —
 * é tratada aqui via jitter (proxy de variacao da amostra) nas recomendacoes condicionais,
 * nao como uma quarta dimensao pontuada.
 *
 * Bufferbloat NAO e medido neste fluxo (10-15s, so latencia/jitter/perda — nao inclui
 * saturacao de banda). Recomendacao de bufferbloat so aparece se um valor for
 * explicitamente fornecido (fica pronta pra quando/se o app plugar uma leitura real) —
 * limitação documentada na issue #935.
 */
object JogoConexaoEngine {
    fun avaliar(
        jogo: JogoCatalogo,
        plataforma: Plataforma,
        medicao: PingResultado,
        tipoConexaoAtual: EstadoConexao,
        wifiLinkSnapshot: WifiLinkSnapshot?,
        bufferbloatMs: Double? = null,
    ): ResultadoTesteJogo {
        val thresholds = jogo.perfil.thresholds()

        val nivelLatencia = thresholds.latenciaMs.classificar(medicao.latenciaMs)
        val nivelJitter = thresholds.jitterMs.classificar(medicao.jitterMs)
        val nivelPerda = thresholds.perdaPercentual.classificar(medicao.perdaPercentual)

        val nivel = piorNivel(listOf(nivelLatencia, nivelJitter, nivelPerda)).paraNivelResultado()

        // Melhor esforco: PROVIDER_NETWORK nao tem deteccao real de rede Riot/Valve
        // implementada nesta issue — cai sempre em REGIONAL_ESTIMATE, nunca inventa dado.
        val estrategiaUsada = EstrategiaTeste.REGIONAL_ESTIMATE

        return ResultadoTesteJogo(
            jogo = jogo,
            plataforma = plataforma,
            nivel = nivel,
            tituloVeredito = tituloVeredito(nivel),
            textoVeredito = textoVeredito(nivel),
            latenciaMs = medicao.latenciaMs,
            jitterMs = medicao.jitterMs,
            perdaPercentual = medicao.perdaPercentual,
            regiaoTestada = "América do Sul",
            estrategiaUsada = estrategiaUsada,
            tipoConexaoAtual = tipoConexaoAtual,
            avisos = montarAvisos(jogo, estrategiaUsada, tipoConexaoAtual),
            recomendacoes =
                montarRecomendacoes(
                    nivelJitter = nivelJitter,
                    nivelPerda = nivelPerda,
                    nivelLatencia = nivelLatencia,
                    latenciaMs = medicao.latenciaMs,
                    wifiLinkSnapshot = wifiLinkSnapshot,
                    tipoConexaoAtual = tipoConexaoAtual,
                    bufferbloatMs = bufferbloatMs,
                ),
        )
    }

    private fun piorNivel(niveis: List<NivelMetrica>): NivelMetrica =
        niveis.maxByOrNull { it.severidade() } ?: NivelMetrica.EXCELENTE

    private fun NivelMetrica.severidade(): Int =
        when (this) {
            NivelMetrica.EXCELENTE -> 0
            NivelMetrica.BOA -> 1
            NivelMetrica.ATENCAO -> 2
            NivelMetrica.RUIM -> 3
        }

    private fun NivelMetrica.paraNivelResultado(): NivelResultado =
        when (this) {
            NivelMetrica.EXCELENTE -> NivelResultado.EXCELENTE
            NivelMetrica.BOA -> NivelResultado.BOA
            NivelMetrica.ATENCAO -> NivelResultado.ATENCAO
            NivelMetrica.RUIM -> NivelResultado.RUIM
        }

    private fun tituloVeredito(nivel: NivelResultado): String =
        when (nivel) {
            NivelResultado.EXCELENTE -> "Excelente para jogar"
            NivelResultado.BOA -> "Boa para jogar"
            NivelResultado.ATENCAO -> "Pode apresentar atraso"
            NivelResultado.RUIM -> "Conexão ruim para este jogo"
        }

    private fun textoVeredito(nivel: NivelResultado): String =
        when (nivel) {
            NivelResultado.EXCELENTE -> "Resposta rápida, baixa variação e nenhuma perda relevante."
            NivelResultado.BOA -> "Conexão adequada — pequenas variações não devem prejudicar a maioria das partidas."
            NivelResultado.ATENCAO -> "Há variações que podem causar atraso nos comandos ou travamentos rápidos."
            NivelResultado.RUIM -> "Perda de pacotes, atraso elevado ou muita variação — evite partidas competitivas agora."
        }

    private fun montarAvisos(
        jogo: JogoCatalogo,
        estrategiaUsada: EstrategiaTeste,
        tipoConexaoAtual: EstadoConexao,
    ): List<String> {
        val avisos =
            mutableListOf(
                "Este teste é uma estimativa feita a partir da conexão atual e da região de " +
                    "servidores mais provável. O resultado dentro da partida pode variar conforme " +
                    "o servidor, a rota, o horário e a plataforma.",
            )

        if (jogo.estrategiaDeclarada == EstrategiaTeste.PROVIDER_NETWORK && estrategiaUsada == EstrategiaTeste.REGIONAL_ESTIMATE) {
            avisos += "Ainda não medimos direto na rede do fornecedor deste jogo — o resultado é uma estimativa regional."
        }

        if (jogo.plataformas != setOf(Plataforma.PC) || jogo.plataformas.size > 1) {
            avisos += "Para uma estimativa mais próxima, conecte o celular à mesma rede e " +
                "permaneça próximo do local onde o PC ou console é utilizado."
        }

        if (tipoConexaoAtual == EstadoConexao.ethernet) {
            avisos += "O desempenho do dispositivo conectado por cabo pode ser melhor que o resultado medido pelo celular no Wi-Fi."
        }

        return avisos
    }

    private fun montarRecomendacoes(
        nivelJitter: NivelMetrica,
        nivelPerda: NivelMetrica,
        nivelLatencia: NivelMetrica,
        latenciaMs: Double,
        wifiLinkSnapshot: WifiLinkSnapshot?,
        tipoConexaoAtual: EstadoConexao,
        bufferbloatMs: Double?,
    ): List<String> {
        val recomendacoes = mutableListOf<String>()

        if (tipoConexaoAtual == EstadoConexao.wifi) {
            val frequencia = wifiLinkSnapshot?.frequenciaMhz
            if (frequencia != null && frequencia < 3000) {
                recomendacoes += "Use a rede de 5GHz."
            }
            val rssi = wifiLinkSnapshot?.rssiDbm
            if (rssi != null && rssi <= -70) {
                recomendacoes += "Aproxime-se do roteador."
            }
        }

        if (nivelJitter == NivelMetrica.ATENCAO || nivelJitter == NivelMetrica.RUIM) {
            recomendacoes += "Sua conexão está variando."
        }

        if (nivelPerda == NivelMetrica.ATENCAO || nivelPerda == NivelMetrica.RUIM) {
            recomendacoes += "Parte dos dados não chegou ao destino."
        }

        if (bufferbloatMs != null && bufferbloatMs > 100.0) {
            recomendacoes += "Outros usos da internet podem afetar a partida."
        }

        val latenciaAlta = nivelLatencia == NivelMetrica.ATENCAO || nivelLatencia == NivelMetrica.RUIM
        val estavel = nivelJitter == NivelMetrica.EXCELENTE || nivelJitter == NivelMetrica.BOA
        if (latenciaAlta && estavel) {
            recomendacoes += "O servidor está distante."
        }

        return recomendacoes
    }
}
