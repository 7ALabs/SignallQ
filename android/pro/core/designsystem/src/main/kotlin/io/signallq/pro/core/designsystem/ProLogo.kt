package io.signallq.pro.core.designsystem

import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource

/**
 * Logo lockup oficial do SignallQ Pro (simbolo de 4 barras + wordmark) -- asset real
 * extraido do projeto Claude Design "SignallQ PRO - Design System" (77a19317-...) em
 * 2026-07-19. Usar em telas de entrada (Carregamento) e no TopBar do Painel; nunca
 * substituir por `Text("SignallQ Pro")` solto. Escolhe a variante clara/escura pelo
 * tema do sistema, mesmo padrao de `isSystemInDarkTheme()` usado em todo o app --
 * nao ha atribuicao fixa de tema por tela.
 */
@Composable
fun ProLogo(
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    darkTheme: Boolean = isSystemInDarkTheme(),
) {
    val drawableRes =
        if (darkTheme) {
            R.drawable.logo_lockup_signallq_pro_dark
        } else {
            R.drawable.logo_lockup_signallq_pro
        }
    Image(
        painter = painterResource(drawableRes),
        contentDescription = "SignallQ Pro",
        modifier = modifier,
        contentScale = contentScale,
    )
}
