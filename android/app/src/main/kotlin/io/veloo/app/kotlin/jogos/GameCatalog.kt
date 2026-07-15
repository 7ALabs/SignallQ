package io.signallq.app.jogos

/**
 * Catalogo de jogos da tela "Jogos" — GH#935, `docs_ai/functional/JOGOS_TESTE_CONEXAO_SPEC.md`.
 *
 * Dado estruturado (nunca hardcode espalhado pela UI). Cada [JogoCatalogo] carrega o
 * necessario pro fluxo de 5 etapas: nome, plataformas suportadas, perfil de sensibilidade
 * (thresholds em [PerfilSensibilidade]) e a estrategia de endpoint.
 *
 * `PROVIDER_NETWORK` (Riot/Valve) e melhor esforco fora do MVP desta issue — deteccao real
 * de rede da desenvolvedora nao foi implementada (pesquisa nao trivial, fora de escopo).
 * Os 4 jogos marcados com essa estrategia caem em [EstrategiaTeste.REGIONAL_ESTIMATE] em
 * tempo de execucao (ver [JogoConexaoEngine]) — nunca inventa dado de rede que nao mediu.
 */
enum class Plataforma(
    val label: String,
) {
    PC("PC"),
    PS5("PlayStation 5"),
    XBOX("Xbox Series"),
}

enum class PerfilSensibilidade {
    COMPETITIVO_EXTREMO,
    COMPETITIVO,
    ESPORTE_COMPETITIVO,
    MULTIPLAYER_MODERADO,
}

enum class EstrategiaTeste {
    /** Alvo estavel na rede da propria desenvolvedora/publicadora. Nao implementado nesta
     *  issue (melhor esforco) — sempre cai em [REGIONAL_ESTIMATE] em tempo de execucao. */
    PROVIDER_NETWORK,

    /** Sonda controlada pelo SignallQ (game-latency-probe-worker) na regiao mais provavel
     *  do servidor. Resultado sempre rotulado como estimativa, nunca "ping real". */
    REGIONAL_ESTIMATE,
}

data class JogoCatalogo(
    val gameId: String,
    val nome: String,
    val perfil: PerfilSensibilidade,
    val plataformas: Set<Plataforma>,
    val estrategiaDeclarada: EstrategiaTeste,
    val resultLabel: String,
)

object CatalogoJogos {
    private val multiplataforma: List<JogoCatalogo> =
        listOf(
            JogoCatalogo("fortnite", "Fortnite", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Fortnite"),
            JogoCatalogo("warzone", "Call of Duty: Warzone", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Call of Duty: Warzone"),
            JogoCatalogo("apex_legends", "Apex Legends", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Apex Legends"),
            JogoCatalogo("rocket_league", "Rocket League", PerfilSensibilidade.COMPETITIVO_EXTREMO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Rocket League"),
            JogoCatalogo("overwatch", "Overwatch", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Overwatch"),
            JogoCatalogo("rainbow_six_siege", "Rainbow Six Siege", PerfilSensibilidade.COMPETITIVO_EXTREMO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Rainbow Six Siege"),
            JogoCatalogo("ea_sports_fc", "EA Sports FC", PerfilSensibilidade.ESPORTE_COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para EA Sports FC"),
            JogoCatalogo("marvel_rivals", "Marvel Rivals", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Marvel Rivals"),
            JogoCatalogo("pubg_battlegrounds", "PUBG: Battlegrounds", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para PUBG: Battlegrounds"),
            JogoCatalogo("dead_by_daylight", "Dead by Daylight", PerfilSensibilidade.MULTIPLAYER_MODERADO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Dead by Daylight"),
            JogoCatalogo("the_finals", "THE FINALS", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para THE FINALS"),
            JogoCatalogo("destiny_2", "Destiny 2", PerfilSensibilidade.MULTIPLAYER_MODERADO, setOf(Plataforma.PC, Plataforma.PS5, Plataforma.XBOX), EstrategiaTeste.REGIONAL_ESTIMATE, "Estimativa para Destiny 2"),
        )

    private val exclusivosPc: List<JogoCatalogo> =
        listOf(
            JogoCatalogo("valorant", "VALORANT", PerfilSensibilidade.COMPETITIVO_EXTREMO, setOf(Plataforma.PC), EstrategiaTeste.PROVIDER_NETWORK, "Estimativa para VALORANT"),
            JogoCatalogo("league_of_legends", "League of Legends", PerfilSensibilidade.COMPETITIVO_EXTREMO, setOf(Plataforma.PC), EstrategiaTeste.PROVIDER_NETWORK, "Estimativa para League of Legends"),
            JogoCatalogo("counter_strike_2", "Counter-Strike 2", PerfilSensibilidade.COMPETITIVO_EXTREMO, setOf(Plataforma.PC), EstrategiaTeste.PROVIDER_NETWORK, "Estimativa para Counter-Strike 2"),
            JogoCatalogo("dota_2", "Dota 2", PerfilSensibilidade.COMPETITIVO, setOf(Plataforma.PC), EstrategiaTeste.PROVIDER_NETWORK, "Estimativa para Dota 2"),
        )

    val todos: List<JogoCatalogo> = multiplataforma + exclusivosPc

    fun porPlataforma(plataforma: Plataforma): List<JogoCatalogo> =
        todos.filter { plataforma in it.plataformas }

    fun porId(gameId: String): JogoCatalogo? = todos.find { it.gameId == gameId }
}

/** Rotulo humano da categoria de sensibilidade — usado na Etapa 2 (lista) e Etapa 3
 *  (confirmacao), texto exato do spec. */
fun PerfilSensibilidade.label(): String =
    when (this) {
        PerfilSensibilidade.COMPETITIVO_EXTREMO -> "Competitivo extremo"
        PerfilSensibilidade.COMPETITIVO -> "Competitivo"
        PerfilSensibilidade.ESPORTE_COMPETITIVO -> "Esporte competitivo"
        PerfilSensibilidade.MULTIPLAYER_MODERADO -> "Multiplayer moderado"
    }
