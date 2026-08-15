package io.signallq.app.ui

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.signallq.app.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SignallQThemeFoundationsTest {
    @Test
    fun `tema escuro usa preto absoluto e camadas tonais progressivas`() {
        val tokens = darkTokens()

        assertEquals(Color.Black, tokens.surface)
        assertEquals(Color(0xFF161616), tokens.cardSurface)
        assertEquals(Color(0xFF222222), tokens.cardSurfaceElevated)
        assertTrue(tokens.cardSurface.luminanceForTest() > tokens.surface.luminanceForTest())
        assertTrue(tokens.cardSurfaceElevated.luminanceForTest() > tokens.cardSurface.luminanceForTest())
    }

    @Test
    fun `pares principais de texto atendem contraste WCAG AA`() {
        listOf(lightTokens(), darkTokens()).forEach { tokens ->
            assertTrue(contrastRatio(tokens.onSurface, tokens.surface) >= 4.5)
            assertTrue(contrastRatio(tokens.onSurface, tokens.cardSurface) >= 4.5)
            assertTrue(contrastRatio(tokens.onSurfaceVariant, tokens.cardSurface) >= 4.5)
        }
    }

    @Test
    fun `escala inclui composicao 48 e 64 e alphas seguem contrato`() {
        assertEquals(48.dp, LkSpacing.compositionLarge)
        assertEquals(64.dp, LkSpacing.compositionExtraLarge)
        assertEquals(0.08f, LkStateLayer.hover)
        assertEquals(0.10f, LkStateLayer.focus)
        assertEquals(0.12f, LkStateLayer.pressed)
        assertEquals(0.16f, LkStateLayer.dragged)
    }

    @Test
    fun `movimento reduzido elimina deslocamento temporal`() {
        assertEquals(0, LkMotion.durationMillis(300, reducedMotion = true))
        assertEquals(300, LkMotion.durationMillis(300, reducedMotion = false))
    }

    @Test
    fun `fontes Google Sans Flex ficam empacotadas para inicializacao offline`() {
        val fontIds =
            listOf(
                R.font.google_sans_flex_regular,
                R.font.google_sans_flex_medium,
                R.font.google_sans_flex_semibold,
                R.font.google_sans_flex_bold,
            )

        assertTrue(fontIds.all { it != 0 })
        assertEquals(fontIds.size, fontIds.distinct().size)
    }
}

private fun contrastRatio(
    foreground: Color,
    background: Color,
): Double {
    val lighter = maxOf(foreground.luminanceForTest(), background.luminanceForTest())
    val darker = minOf(foreground.luminanceForTest(), background.luminanceForTest())
    return (lighter + 0.05) / (darker + 0.05)
}

private fun Color.luminanceForTest(): Double {
    fun linear(channel: Float): Double {
        val value = channel.toDouble()
        return if (value <= 0.04045) value / 12.92 else Math.pow((value + 0.055) / 1.055, 2.4)
    }

    return 0.2126 * linear(red) + 0.7152 * linear(green) + 0.0722 * linear(blue)
}
