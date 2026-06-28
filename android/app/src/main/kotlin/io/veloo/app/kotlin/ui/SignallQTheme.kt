package io.signallq.app.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

object LkColors {
    val accent = Color(0xFF6C2BFF)
    val success = Color(0xFF22C55E)
    val warning = Color(0xFFF5A623)
    val error = Color(0xFFFF4D4F)
    val accentBlue = Color(0xFF2563EB)

    // ── Cores utilitárias para texto sobre fundos escuros/coloridos ──
    // Usadas em botões com fundo accent, avatares e superfícies fixas escuras.
    // NÃO use para telas que devem seguir o tema claro/escuro do sistema —
    // para isso use LkTokens via LocalLkTokens.current.
    val signallQBlack = Color(0xFF0D0D1A)
    val signallQDarkSurface = Color(0xFF1A0B2E)
    val signallQDarkCard = Color(0xFF1E1130)
    val signallQTextOnDark = Color(0xFFF3F4F6)
    val signallQTextSecondaryOnDark = Color(0xFF9CA3AF)

    val phaseLatencia = Color(0xFF60A5FA)
    val phaseDownload = Color(0xFF34D399)
    val phaseUpload = Color(0xFFFBBF24)

    // ── Tokens semânticos de container — seguem o tema (light/dark) ──
    // Uso: backgrounds de chips e cards de status, nunca hardcodados na UI.
    // Valores light calibrados para contraste AA com onWarningContainer/onSuccessContainer.
    object Light {
        val bgPrimary = Color(0xFFFFFFFF)
        val bgSecondary = Color(0xFFF3F4F6)
        val bgCard = Color(0xFFFFFFFF)
        val textPrimary = Color(0xFF0D0D1A)
        val textSecondary = Color(0xFF6B7280)
        val textTertiary = Color(0xFF9CA3AF)
        val border = Color(0xFFE5E7EB)

        // warning (âmbar): fundo claro + texto escuro âmbar (contraste >=4.5:1)
        val warningContainer = Color(0xFFFFF3CD)
        val onWarningContainer = Color(0xFF7A4E00)
        val amberSurface = Color(0xFFFFF8E6)

        // success (verde): fundo claro + texto escuro verde
        val successContainer = Color(0xFFD1FAE5)
        val onSuccessContainer = Color(0xFF065F46)
    }

    object Dark {
        val bgPrimary = Color(0xFF000000)
        val bgSecondary = Color(0xFF1A1A1A)
        val bgCard = Color(0xFF111111)
        val textPrimary = Color(0xFFF3F4F6)
        val textSecondary = Color(0xFF9CA3AF)
        val textTertiary = Color(0xFF6B7280)
        val border = Color(0xFF2A2A2A)

        // warning dark: fundo escuro âmbar + texto âmbar claro
        val warningContainer = Color(0xFF3D2A00)
        val onWarningContainer = Color(0xFFFFD97A)
        val amberSurface = Color(0xFF2E2000)

        // success dark: fundo escuro verde + texto verde claro
        val successContainer = Color(0xFF064E3B)
        val onSuccessContainer = Color(0xFF6EE7B7)
    }
}

data class LkTokens(
    val bgPrimary: Color,
    val bgSecondary: Color,
    val bgCard: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textTertiary: Color,
    val border: Color,
    // ── Tokens semânticos de container ──
    val warningContainer: Color,
    val onWarningContainer: Color,
    val amberSurface: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
)

val LocalLkTokens =
    staticCompositionLocalOf {
        LkTokens(
            bgPrimary = LkColors.Light.bgPrimary,
            bgSecondary = LkColors.Light.bgSecondary,
            bgCard = LkColors.Light.bgCard,
            textPrimary = LkColors.Light.textPrimary,
            textSecondary = LkColors.Light.textSecondary,
            textTertiary = LkColors.Light.textTertiary,
            border = LkColors.Light.border,
            warningContainer = LkColors.Light.warningContainer,
            onWarningContainer = LkColors.Light.onWarningContainer,
            amberSurface = LkColors.Light.amberSurface,
            successContainer = LkColors.Light.successContainer,
            onSuccessContainer = LkColors.Light.onSuccessContainer,
        )
    }

object LkSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val lg: Dp = 16.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val cardContent: Dp = 16.dp
}

object LkRadius {
    val card: Dp = 16.dp
    val button: Dp = 12.dp
    val input: Dp = 12.dp
}

private val lightScheme =
    lightColorScheme(
        primary = LkColors.accent,
        background = LkColors.Light.bgPrimary,
        surface = LkColors.Light.bgCard,
        onPrimary = Color.White,
        onBackground = LkColors.Light.textPrimary,
        onSurface = LkColors.Light.textPrimary,
        secondary = LkColors.Light.bgSecondary,
        outline = LkColors.Light.border,
    )

private val darkScheme =
    darkColorScheme(
        primary = LkColors.accent,
        background = LkColors.Dark.bgPrimary,
        surface = LkColors.Dark.bgCard,
        onPrimary = Color.White,
        onBackground = LkColors.Dark.textPrimary,
        onSurface = LkColors.Dark.textPrimary,
        secondary = LkColors.Dark.bgSecondary,
        outline = LkColors.Dark.border,
    )

@Suppress("FunctionNaming")
@Composable
fun SignallQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens =
        if (darkTheme) {
            LkTokens(
                bgPrimary = LkColors.Dark.bgPrimary,
                bgSecondary = LkColors.Dark.bgSecondary,
                bgCard = LkColors.Dark.bgCard,
                textPrimary = LkColors.Dark.textPrimary,
                textSecondary = LkColors.Dark.textSecondary,
                textTertiary = LkColors.Dark.textTertiary,
                border = LkColors.Dark.border,
                warningContainer = LkColors.Dark.warningContainer,
                onWarningContainer = LkColors.Dark.onWarningContainer,
                amberSurface = LkColors.Dark.amberSurface,
                successContainer = LkColors.Dark.successContainer,
                onSuccessContainer = LkColors.Dark.onSuccessContainer,
            )
        } else {
            LkTokens(
                bgPrimary = LkColors.Light.bgPrimary,
                bgSecondary = LkColors.Light.bgSecondary,
                bgCard = LkColors.Light.bgCard,
                textPrimary = LkColors.Light.textPrimary,
                textSecondary = LkColors.Light.textSecondary,
                textTertiary = LkColors.Light.textTertiary,
                border = LkColors.Light.border,
                warningContainer = LkColors.Light.warningContainer,
                onWarningContainer = LkColors.Light.onWarningContainer,
                amberSurface = LkColors.Light.amberSurface,
                successContainer = LkColors.Light.successContainer,
                onSuccessContainer = LkColors.Light.onSuccessContainer,
            )
        }

    CompositionLocalProvider(LocalLkTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme else lightScheme,
            typography = signallQTypography,
            content = content,
        )
    }
}

// Escala tipográfica SignallQ — Material 3 / WCAG 2.2 AA.
// Todos os tamanhos em sp para respeitar fontScale do sistema.
// bodyMedium≥14sp, bodyLarge=16sp (telas principais), botões/labels≥14sp.
private val signallQTypography =
    Typography(
        displayLarge = TextStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold),
        headlineLarge = TextStyle(fontSize = 24.sp, fontWeight = FontWeight.SemiBold),
        headlineMedium = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        headlineSmall = TextStyle(fontSize = 18.sp, fontWeight = FontWeight.SemiBold),
        titleLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Medium),
        titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Medium),
        titleSmall = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        bodyLarge = TextStyle(fontSize = 16.sp, fontWeight = FontWeight.Normal),
        bodyMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal),
        bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
        labelLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium),
        labelMedium = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal),
        labelSmall = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Normal),
    )
