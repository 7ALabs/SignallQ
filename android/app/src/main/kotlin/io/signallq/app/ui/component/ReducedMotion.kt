package io.signallq.app.ui.component

import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext

/**
 * Detecta se o usuário desativou animações do sistema (Ajustes do Android > Acessibilidade >
 * Remover animações) -- o equivalente Android do "reduce motion" da web/iOS. Não existe API
 * dedicada equivalente a `prefers-reduced-motion`; `ANIMATOR_DURATION_SCALE == 0f` é a forma
 * padrão documentada de checar essa preferência (é o mesmo valor que o desenvolvedor zera em
 * Opções do desenvolvedor > Escala de animação, mas a tela de Acessibilidade tem o toggle
 * dedicado "Remover animações" que grava o mesmo settings global).
 *
 * Issue #1668 (Sinal Wi-Fi ao vivo, épico #1647): critério de aceite "redução de movimento
 * mantém leitura" -- a tela "ao vivo" tem um indicador de pulso decorativo; quando esta função
 * retorna `true`, o indicador vira estático (o dado em si continua atualizando ao vivo via
 * recomposição, só a animação decorativa é removida). Nasce aqui, em vez de dentro da própria
 * tela, porque é a primeira tela do app com animação contínua "ao vivo" e outras (ex.:
 * ModoGamerScreen, SpeedTestScreen) têm o mesmo problema em potencial no futuro.
 */
@Composable
fun animacoesDoSistemaDesativadas(): Boolean {
    val context = LocalContext.current
    return remember(context) {
        val escala =
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        escala == 0f
    }
}
