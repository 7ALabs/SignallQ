package io.signallq.pro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import io.signallq.pro.core.designsystem.ProShapes

// Issue #1176 -- dark mode segue o sistema Android (mesmo padrao do consumidor,
// `SignallQTheme` em `io/veloo/app/kotlin/ui/SignallQTheme.kt`), nao e atribuicao fixa por
// tela. Tokens escuros extraidos do `styles.css` real (`[data-theme="dark"]`) do projeto
// Claude Design "SignallQ PRO - Design System" (77a19317).
private val ProLightColorScheme =
    lightColorScheme(
        primary = ProPrimary,
        primaryContainer = ProPrimaryContainer,
        secondary = ProSecondary,
        secondaryContainer = ProSecondaryContainer,
        tertiary = ProTertiary,
        tertiaryContainer = ProTertiaryContainer,
        error = ProError,
        errorContainer = ProErrorContainer,
        background = ProBackground,
        surface = ProSurface,
        surfaceContainerHigh = ProSurfaceContainerHigh,
        outline = ProOutline,
        inverseSurface = ProInverseSurface,
    )

private val ProDarkColorScheme =
    darkColorScheme(
        background = ProDarkBackground,
        onBackground = ProDarkOnBackground,
        primary = ProDarkPrimary,
        onPrimary = ProDarkOnPrimary,
        primaryContainer = ProDarkPrimaryContainer,
        onPrimaryContainer = ProDarkOnPrimaryContainer,
        secondary = ProDarkSecondary,
        onSecondary = ProDarkOnSecondary,
        secondaryContainer = ProDarkSecondaryContainer,
        onSecondaryContainer = ProDarkOnSecondaryContainer,
        tertiary = ProDarkTertiary,
        onTertiary = ProDarkOnTertiary,
        tertiaryContainer = ProDarkTertiaryContainer,
        onTertiaryContainer = ProDarkOnTertiaryContainer,
        error = ProDarkError,
        onError = ProDarkOnError,
        errorContainer = ProDarkErrorContainer,
        onErrorContainer = ProDarkOnErrorContainer,
        surface = ProDarkSurface,
        surfaceContainerLowest = ProDarkSurfaceContainerLowest,
        surfaceContainerLow = ProDarkSurfaceContainerLow,
        surfaceContainer = ProDarkSurfaceContainer,
        surfaceContainerHigh = ProDarkSurfaceContainerHigh,
        surfaceContainerHighest = ProDarkSurfaceContainerHighest,
        onSurface = ProDarkOnSurface,
        onSurfaceVariant = ProDarkOnSurfaceVariant,
        outline = ProDarkOutline,
        outlineVariant = ProDarkOutlineVariant,
        inverseSurface = ProDarkInverseSurface,
        inverseOnSurface = ProDarkInverseOnSurface,
    )

@Composable
fun SignallQProTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = if (darkTheme) ProDarkColorScheme else ProLightColorScheme,
        typography = signallQProTypography,
        shapes = ProShapes,
        content = content,
    )
}
