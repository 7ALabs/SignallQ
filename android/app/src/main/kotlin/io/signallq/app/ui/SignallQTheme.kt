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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.signallq.app.R

object LkColors {
    val accent = Color(0xFF5B21D6)
    val accentOnDark = Color(0xFFD0BCFF)

    val success = Color(0xFF146C2E)
    val warning = Color(0xFF8A5000)
    val error = Color(0xFFBA1A1A)

    val signallQBlack = Color(0xFF0D0D1A)
    val signallQDarkSurface = Color(0xFF1A0B2E)
    val signallQDarkCard = Color(0xFF1E1130)
    val signallQTextOnDark = Color(0xFFF3F4F6)
    val signallQTextSecondaryOnDark = Color(0xFFB9B2C4)
    val phaseLatencia = Light.phaseLatencia
    val phaseDownload = Light.phaseDownload
    val phaseUpload = Light.phaseUpload

    object Light {
        val primary = Color(0xFF5B21D6)
        val onPrimary = Color(0xFFFFFFFF)
        val primaryContainer = Color(0xFFEAE0FF)
        val onPrimaryContainer = Color(0xFF210A5C)
        val secondary = Color(0xFF2851B8)
        val onSecondary = Color(0xFFFFFFFF)
        val secondaryContainer = Color(0xFFDCE6FF)
        val onSecondaryContainer = Color(0xFF001A41)
        val surface = Color(0xFFFFFFFF)
        val surfaceDim = Color(0xFFDED8E1)
        val surfaceContainerLowest = Color(0xFFFFFFFF)
        val surfaceContainerLow = Color(0xFFF8F5FB)
        val surfaceContainer = Color(0xFFF3EEFA)
        val surfaceContainerHigh = Color(0xFFECE5F5)
        val surfaceContainerHighest = Color(0xFFE6DDF2)
        val onSurface = Color(0xFF1C1B1F)
        val onSurfaceVariant = Color(0xFF49454F)
        val outline = Color(0xFF79747E)
        val outlineVariant = Color(0xFFCAC4D0)
        val inverseSurface = Color(0xFF313033)
        val inverseOnSurface = Color(0xFFF4EFF4)
        val error = Color(0xFFBA1A1A)
        val onError = Color(0xFFFFFFFF)
        val errorContainer = Color(0xFFFFDAD6)
        val onErrorContainer = Color(0xFF410002)
        val success = Color(0xFF146C2E)
        val onSuccess = Color(0xFFFFFFFF)
        val successContainer = Color(0xFFB6F2BE)
        val onSuccessContainer = Color(0xFF04210D)
        val warning = Color(0xFF8A5000)
        val onWarning = Color(0xFFFFFFFF)
        val warningContainer = Color(0xFFFFDDB3)
        val onWarningContainer = Color(0xFF2B1700)
        val phaseLatencia = Color(0xFF2563EB)
        val phaseDownload = Color(0xFF146C2E)
        val phaseUpload = Color(0xFF8A5000)
        val bgPrimary = surface
        val bgSecondary = surfaceContainerLow
        val bgCard = surfaceContainer
        val textPrimary = onSurface
        val textSecondary = onSurfaceVariant
        val textTertiary = onSurfaceVariant
        val border = outlineVariant
        val amberSurface = warningContainer
    }

    object Dark {
        val primary = Color(0xFFD0BCFF)
        val onPrimary = Color(0xFF38137E)
        val primaryContainer = Color(0xFF4F2FA8)
        val onPrimaryContainer = Color(0xFFEADDFF)
        val secondary = Color(0xFFAAC7FF)
        val onSecondary = Color(0xFF002E69)
        val secondaryContainer = Color(0xFF1E427A)
        val onSecondaryContainer = Color(0xFFD9E2FF)
        val surface = Color(0xFF131217)
        val surfaceDim = Color(0xFF131217)
        val surfaceContainerLowest = Color(0xFF0E0D12)
        val surfaceContainerLow = Color(0xFF1D1B20)
        val surfaceContainer = Color(0xFF211F26)
        val surfaceContainerHigh = Color(0xFF2B2930)
        val surfaceContainerHighest = Color(0xFF36343B)
        val onSurface = Color(0xFFE6E0E9)
        val onSurfaceVariant = Color(0xFFCAC4D0)
        val outline = Color(0xFF948F99)
        val outlineVariant = Color(0xFF49454F)
        val inverseSurface = Color(0xFFE6E0E9)
        val inverseOnSurface = Color(0xFF313033)
        val error = Color(0xFFFFB4AB)
        val onError = Color(0xFF690005)
        val errorContainer = Color(0xFF93000A)
        val onErrorContainer = Color(0xFFFFDAD6)
        val success = Color(0xFF83DA99)
        val onSuccess = Color(0xFF00390F)
        val successContainer = Color(0xFF0A5321)
        val onSuccessContainer = Color(0xFF9DF4AC)
        val warning = Color(0xFFFFB870)
        val onWarning = Color(0xFF4A2900)
        val warningContainer = Color(0xFF693D00)
        val onWarningContainer = Color(0xFFFFDDB3)
        val phaseLatencia = Color(0xFFAAC7FF)
        val phaseDownload = Color(0xFF83DA99)
        val phaseUpload = Color(0xFFFFB870)
        val bgPrimary = surface
        val bgSecondary = surfaceContainerLow
        val bgCard = surfaceContainer
        val textPrimary = onSurface
        val textSecondary = onSurfaceVariant
        val textTertiary = onSurfaceVariant
        val border = outlineVariant
        val amberSurface = warningContainer
    }
}

data class LkTokens(
    val primary: Color,
    val onPrimary: Color,
    val primaryContainer: Color,
    val onPrimaryContainer: Color,
    val secondary: Color,
    val onSecondary: Color,
    val secondaryContainer: Color,
    val onSecondaryContainer: Color,
    val surface: Color,
    val surfaceDim: Color,
    val surfaceContainerLowest: Color,
    val surfaceContainerLow: Color,
    val surfaceContainer: Color,
    val surfaceContainerHigh: Color,
    val surfaceContainerHighest: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val warning: Color,
    val onWarning: Color,
    val warningContainer: Color,
    val onWarningContainer: Color,
    val phaseLatencia: Color,
    val phaseDownload: Color,
    val phaseUpload: Color,
) {
    val bgPrimary: Color get() = surface
    val bgSecondary: Color get() = surfaceContainerLow
    val bgCard: Color get() = surfaceContainer
    val textPrimary: Color get() = onSurface
    val textSecondary: Color get() = onSurfaceVariant
    val textTertiary: Color get() = onSurfaceVariant
    val border: Color get() = outlineVariant
    val amberSurface: Color get() = warningContainer
}

// GH#1206 — internal (nao mais private) pra permitir testes unitarios de funcoes que
// recebem LkTokens (ex.: classificadores de sinal movel em SinalScreen.kt) construirem um
// LkTokens real sem precisar duplicar os ~30 campos manualmente em cada teste.
internal fun lightTokens() =
    LkTokens(
        primary = LkColors.Light.primary,
        onPrimary = LkColors.Light.onPrimary,
        primaryContainer = LkColors.Light.primaryContainer,
        onPrimaryContainer = LkColors.Light.onPrimaryContainer,
        secondary = LkColors.Light.secondary,
        onSecondary = LkColors.Light.onSecondary,
        secondaryContainer = LkColors.Light.secondaryContainer,
        onSecondaryContainer = LkColors.Light.onSecondaryContainer,
        surface = LkColors.Light.surface,
        surfaceDim = LkColors.Light.surfaceDim,
        surfaceContainerLowest = LkColors.Light.surfaceContainerLowest,
        surfaceContainerLow = LkColors.Light.surfaceContainerLow,
        surfaceContainer = LkColors.Light.surfaceContainer,
        surfaceContainerHigh = LkColors.Light.surfaceContainerHigh,
        surfaceContainerHighest = LkColors.Light.surfaceContainerHighest,
        onSurface = LkColors.Light.onSurface,
        onSurfaceVariant = LkColors.Light.onSurfaceVariant,
        outline = LkColors.Light.outline,
        outlineVariant = LkColors.Light.outlineVariant,
        inverseSurface = LkColors.Light.inverseSurface,
        inverseOnSurface = LkColors.Light.inverseOnSurface,
        error = LkColors.Light.error,
        onError = LkColors.Light.onError,
        errorContainer = LkColors.Light.errorContainer,
        onErrorContainer = LkColors.Light.onErrorContainer,
        success = LkColors.Light.success,
        onSuccess = LkColors.Light.onSuccess,
        successContainer = LkColors.Light.successContainer,
        onSuccessContainer = LkColors.Light.onSuccessContainer,
        warning = LkColors.Light.warning,
        onWarning = LkColors.Light.onWarning,
        warningContainer = LkColors.Light.warningContainer,
        onWarningContainer = LkColors.Light.onWarningContainer,
        phaseLatencia = LkColors.Light.phaseLatencia,
        phaseDownload = LkColors.Light.phaseDownload,
        phaseUpload = LkColors.Light.phaseUpload,
    )

private fun darkTokens() =
    LkTokens(
        primary = LkColors.Dark.primary,
        onPrimary = LkColors.Dark.onPrimary,
        primaryContainer = LkColors.Dark.primaryContainer,
        onPrimaryContainer = LkColors.Dark.onPrimaryContainer,
        secondary = LkColors.Dark.secondary,
        onSecondary = LkColors.Dark.onSecondary,
        secondaryContainer = LkColors.Dark.secondaryContainer,
        onSecondaryContainer = LkColors.Dark.onSecondaryContainer,
        surface = LkColors.Dark.surface,
        surfaceDim = LkColors.Dark.surfaceDim,
        surfaceContainerLowest = LkColors.Dark.surfaceContainerLowest,
        surfaceContainerLow = LkColors.Dark.surfaceContainerLow,
        surfaceContainer = LkColors.Dark.surfaceContainer,
        surfaceContainerHigh = LkColors.Dark.surfaceContainerHigh,
        surfaceContainerHighest = LkColors.Dark.surfaceContainerHighest,
        onSurface = LkColors.Dark.onSurface,
        onSurfaceVariant = LkColors.Dark.onSurfaceVariant,
        outline = LkColors.Dark.outline,
        outlineVariant = LkColors.Dark.outlineVariant,
        inverseSurface = LkColors.Dark.inverseSurface,
        inverseOnSurface = LkColors.Dark.inverseOnSurface,
        error = LkColors.Dark.error,
        onError = LkColors.Dark.onError,
        errorContainer = LkColors.Dark.errorContainer,
        onErrorContainer = LkColors.Dark.onErrorContainer,
        success = LkColors.Dark.success,
        onSuccess = LkColors.Dark.onSuccess,
        successContainer = LkColors.Dark.successContainer,
        onSuccessContainer = LkColors.Dark.onSuccessContainer,
        warning = LkColors.Dark.warning,
        onWarning = LkColors.Dark.onWarning,
        warningContainer = LkColors.Dark.warningContainer,
        onWarningContainer = LkColors.Dark.onWarningContainer,
        phaseLatencia = LkColors.Dark.phaseLatencia,
        phaseDownload = LkColors.Dark.phaseDownload,
        phaseUpload = LkColors.Dark.phaseUpload,
    )

val LocalLkTokens = staticCompositionLocalOf { lightTokens() }

object LkSpacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 12.dp
    val base: Dp = 16.dp
    val lg: Dp = 20.dp
    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp
    val xxxl: Dp = 40.dp
    val cardContent: Dp = base
}

object LkRadius {
    val card: Dp = 16.dp
    val button: Dp = 20.dp
    val input: Dp = 12.dp
    val sheet: Dp = 28.dp
    val dialog: Dp = 24.dp
    val pill: Dp = 999.dp
}

private val lightScheme =
    lightColorScheme(
        primary = LkColors.Light.primary,
        onPrimary = LkColors.Light.onPrimary,
        primaryContainer = LkColors.Light.primaryContainer,
        onPrimaryContainer = LkColors.Light.onPrimaryContainer,
        secondary = LkColors.Light.secondary,
        onSecondary = LkColors.Light.onSecondary,
        secondaryContainer = LkColors.Light.secondaryContainer,
        onSecondaryContainer = LkColors.Light.onSecondaryContainer,
        background = LkColors.Light.surface,
        onBackground = LkColors.Light.onSurface,
        surface = LkColors.Light.surface,
        onSurface = LkColors.Light.onSurface,
        surfaceVariant = LkColors.Light.surfaceContainer,
        onSurfaceVariant = LkColors.Light.onSurfaceVariant,
        outline = LkColors.Light.outline,
        outlineVariant = LkColors.Light.outlineVariant,
        error = LkColors.Light.error,
        onError = LkColors.Light.onError,
        errorContainer = LkColors.Light.errorContainer,
        onErrorContainer = LkColors.Light.onErrorContainer,
        inverseSurface = LkColors.Light.inverseSurface,
        inverseOnSurface = LkColors.Light.inverseOnSurface,
    )

private val darkScheme =
    darkColorScheme(
        primary = LkColors.Dark.primary,
        onPrimary = LkColors.Dark.onPrimary,
        primaryContainer = LkColors.Dark.primaryContainer,
        onPrimaryContainer = LkColors.Dark.onPrimaryContainer,
        secondary = LkColors.Dark.secondary,
        onSecondary = LkColors.Dark.onSecondary,
        secondaryContainer = LkColors.Dark.secondaryContainer,
        onSecondaryContainer = LkColors.Dark.onSecondaryContainer,
        background = LkColors.Dark.surface,
        onBackground = LkColors.Dark.onSurface,
        surface = LkColors.Dark.surface,
        onSurface = LkColors.Dark.onSurface,
        surfaceVariant = LkColors.Dark.surfaceContainer,
        onSurfaceVariant = LkColors.Dark.onSurfaceVariant,
        outline = LkColors.Dark.outline,
        outlineVariant = LkColors.Dark.outlineVariant,
        error = LkColors.Dark.error,
        onError = LkColors.Dark.onError,
        errorContainer = LkColors.Dark.errorContainer,
        onErrorContainer = LkColors.Dark.onErrorContainer,
        inverseSurface = LkColors.Dark.inverseSurface,
        inverseOnSurface = LkColors.Dark.inverseOnSurface,
    )

@Suppress("FunctionNaming")
@Composable
fun SignallQTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val tokens = if (darkTheme) darkTokens() else lightTokens()

    CompositionLocalProvider(LocalLkTokens provides tokens) {
        MaterialTheme(
            colorScheme = if (darkTheme) darkScheme else lightScheme,
            typography = signallQTypography,
            content = content,
        )
    }
}

private val signallQFontFamily =
    FontFamily(
        Font(R.font.google_sans_flex_regular, weight = FontWeight.Normal),
        Font(R.font.google_sans_flex_medium, weight = FontWeight.Medium),
        Font(R.font.google_sans_flex_semibold, weight = FontWeight.SemiBold),
        Font(R.font.google_sans_flex_bold, weight = FontWeight.Bold),
    )

private fun dsTextStyle(
    fontSize: Int,
    lineHeight: Int,
    fontWeight: FontWeight,
    letterSpacing: Float,
) = TextStyle(
    fontFamily = signallQFontFamily,
    fontSize = fontSize.sp,
    lineHeight = lineHeight.sp,
    fontWeight = fontWeight,
    letterSpacing = letterSpacing.sp,
)

private val signallQTypography =
    Typography(
        displayLarge = dsTextStyle(34, 40, FontWeight.Bold, 0f),
        displayMedium = dsTextStyle(34, 40, FontWeight.Bold, 0f),
        displaySmall = dsTextStyle(34, 40, FontWeight.Bold, 0f),
        headlineLarge = dsTextStyle(26, 32, FontWeight.Bold, 0f),
        headlineMedium = dsTextStyle(26, 32, FontWeight.Bold, 0f),
        headlineSmall = dsTextStyle(22, 28, FontWeight.SemiBold, 0f),
        titleLarge = dsTextStyle(20, 26, FontWeight.SemiBold, 0f),
        titleMedium = dsTextStyle(16, 22, FontWeight.Medium, 0.1f),
        titleSmall = dsTextStyle(14, 20, FontWeight.Medium, 0.1f),
        bodyLarge = dsTextStyle(16, 24, FontWeight.Normal, 0.15f),
        bodyMedium = dsTextStyle(14, 20, FontWeight.Normal, 0.2f),
        bodySmall = dsTextStyle(12, 16, FontWeight.Normal, 0.25f),
        labelLarge = dsTextStyle(14, 20, FontWeight.Medium, 0.1f),
        labelMedium = dsTextStyle(12, 16, FontWeight.Medium, 0.3f),
        labelSmall = dsTextStyle(11, 16, FontWeight.Medium, 0.4f),
    )
