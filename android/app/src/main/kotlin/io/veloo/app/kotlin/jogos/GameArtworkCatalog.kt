package io.signallq.app.jogos

import androidx.annotation.DrawableRes
import io.signallq.app.R

data class GameArtwork(
    @param:DrawableRes val drawableRes: Int,
)

object GameArtworkCatalog {
    fun forGame(gameId: String): GameArtwork? =
        when (gameId) {
            "fortnite" -> GameArtwork(R.drawable.game_art_fortnite)
            "warzone" -> GameArtwork(R.drawable.game_art_warzone)
            "apex_legends" -> GameArtwork(R.drawable.game_art_apex_legends)
            "rocket_league" -> GameArtwork(R.drawable.game_art_rocket_league)
            "overwatch" -> GameArtwork(R.drawable.game_art_overwatch)
            "rainbow_six_siege" -> GameArtwork(R.drawable.game_art_rainbow_six_siege)
            "ea_sports_fc" -> GameArtwork(R.drawable.game_art_ea_sports_fc)
            "marvel_rivals" -> GameArtwork(R.drawable.game_art_marvel_rivals)
            "pubg_battlegrounds" -> GameArtwork(R.drawable.game_art_pubg_battlegrounds)
            "dead_by_daylight" -> GameArtwork(R.drawable.game_art_dead_by_daylight)
            "the_finals" -> GameArtwork(R.drawable.game_art_the_finals)
            "destiny_2" -> GameArtwork(R.drawable.game_art_destiny_2)
            "valorant" -> GameArtwork(R.drawable.game_art_valorant)
            "league_of_legends" -> GameArtwork(R.drawable.game_art_league_of_legends)
            "counter_strike_2" -> GameArtwork(R.drawable.game_art_counter_strike_2)
            "dota_2" -> GameArtwork(R.drawable.game_art_dota_2)
            else -> null
        }
}
