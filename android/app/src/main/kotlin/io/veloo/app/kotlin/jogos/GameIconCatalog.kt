package io.signallq.app.jogos

import androidx.annotation.DrawableRes
import io.signallq.app.R

/**
 * Catalogo local de imagem oficial por jogo (capa/logo), pareado por [JogoCatalogo.gameId].
 *
 * Assets baixados de paginas da Wikipedia/Wikimedia Commons (licenca de uso justo /
 * identificacao de produto — capas de jogo e logos de marca, mesmo criterio ja aplicado ao
 * catalogo de operadoras em [io.signallq.app.ui.OperadoraLogoCatalog]). Uso apenas
 * identificativo, na tela de selecao de jogo — nao e endosso, parceria ou patrocinio das
 * desenvolvedoras/publicadoras ao SignallQ.
 *
 * [iconRes] e nullable de proposito: nem todo jogo do catalogo tera asset disponivel o
 * tempo todo (jogo novo adicionado sem imagem ainda, etc). Quando nulo, a UI cai no
 * tratamento de sigla monoespaçada que a especificacao original ja previa.
 */
object GameIconCatalog {
    private val icones: Map<String, Int> =
        mapOf(
            "fortnite" to R.drawable.game_fortnite,
            "warzone" to R.drawable.game_warzone,
            "apex_legends" to R.drawable.game_apex_legends,
            "rocket_league" to R.drawable.game_rocket_league,
            "overwatch" to R.drawable.game_overwatch,
            "rainbow_six_siege" to R.drawable.game_rainbow_six_siege,
            "ea_sports_fc" to R.drawable.game_ea_sports_fc,
            "marvel_rivals" to R.drawable.game_marvel_rivals,
            "pubg_battlegrounds" to R.drawable.game_pubg_battlegrounds,
            "dead_by_daylight" to R.drawable.game_dead_by_daylight,
            "the_finals" to R.drawable.game_the_finals,
            "destiny_2" to R.drawable.game_destiny_2,
            "valorant" to R.drawable.game_valorant,
            "league_of_legends" to R.drawable.game_league_of_legends,
            "counter_strike_2" to R.drawable.game_counter_strike_2,
            "dota_2" to R.drawable.game_dota_2,
        )

    @DrawableRes
    fun iconePara(gameId: String): Int? = icones[gameId]
}
