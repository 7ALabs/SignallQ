package io.signallq.app.modogamer

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Cast
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.SportsEsports
import androidx.compose.ui.graphics.vector.ImageVector
import io.signallq.app.core.diagnostico.CategoriaJogoModoGamer
import io.signallq.app.core.diagnostico.DeviceJogo

/** Ícone de cada um dos 7 devices do Modo gamer (issue #1476) — consoles (PS/Xbox/Switch)
 *  reaproveitam [Icons.Outlined.SportsEsports], mesmo ícone genérico já usado em
 *  [io.signallq.app.jogos.Plataforma] (PS5/XBOX) na tela "Jogos" legada. */
fun DeviceJogo.icone(): ImageVector =
    when (this) {
        DeviceJogo.PLAYSTATION, DeviceJogo.XBOX, DeviceJogo.SWITCH -> Icons.Outlined.SportsEsports
        DeviceJogo.PC -> Icons.Outlined.Computer
        DeviceJogo.ANDROID -> Icons.Outlined.PhoneAndroid
        DeviceJogo.IPHONE -> Icons.Outlined.PhoneIphone
        DeviceJogo.TV_CLOUD -> Icons.Outlined.Cast
    }

/** Ícone de cada categoria — usado tanto na lista de jogos catalogados (por categoria) quanto
 *  na tela de fallback "Outro jogo" (as 6 categorias genéricas). */
fun CategoriaJogoModoGamer.icone(): ImageVector =
    when (this) {
        CategoriaJogoModoGamer.FPS_COMPETITIVO -> Icons.Outlined.SportsEsports
        CategoriaJogoModoGamer.BATTLE_ROYALE -> Icons.Outlined.SportsEsports
        CategoriaJogoModoGamer.MOBA -> Icons.Outlined.SportsEsports
        CategoriaJogoModoGamer.CASUAL -> Icons.Outlined.SportsEsports
        CategoriaJogoModoGamer.CLOUD_GAMING -> Icons.Outlined.Cast
        CategoriaJogoModoGamer.OUTRO -> Icons.Outlined.SportsEsports
    }
