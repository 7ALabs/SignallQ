package io.signallq.pro.ui.theme

import androidx.compose.ui.graphics.Color

// Paleta oficial do SignallQ Pro — snapshot 2026-07-18 do projeto Claude Design
// "SignallQ PRO - Design System" (77a19317-ea64-4e47-b55c-578eca776c09), skill
// signallq-pro-design. Reler o projeto online antes de expandir — evolui.
// NUNCA misturar com a paleta violeta do consumidor (#5B21D6/#2851B8).

val ProPrimary = Color(0xFF0B6CFF)
val ProPrimaryContainer = Color(0xFFD8E7FF)
val ProSecondary = Color(0xFF006B76)
val ProSecondaryContainer = Color(0xFFA9EDF3)
val ProTertiary = Color(0xFF6558E8)
val ProTertiaryContainer = Color(0xFFE5DEFF)

val ProSuccess = Color(0xFF1AA25A)
val ProWarning = Color(0xFFE9AD27)
val ProError = Color(0xFFD9363E)
val ProErrorContainer = Color(0xFFFFDAD6)

val ProBackground = Color(0xFFF7F9FC)
val ProSurface = Color(0xFFFFFFFF)
val ProSurfaceContainerHigh = Color(0xFFE7ECF3)
val ProOutline = Color(0xFFC4CBD5)
val ProInverseSurface = Color(0xFF252B33)

// Paleta escura oficial -- issue #1176, extraida do `styles.css` real
// (`[data-theme="dark"]`) do mesmo projeto Claude Design. Nomes seguem 1:1 os parametros de
// `androidx.compose.material3.darkColorScheme(...)`.
val ProDarkBackground = Color(0xFF000000)
val ProDarkOnBackground = Color(0xFFF5F7FA)
val ProDarkPrimary = Color(0xFF2D8CFF)
val ProDarkOnPrimary = Color(0xFFFFFFFF)
val ProDarkPrimaryContainer = Color(0xFF003B73)
val ProDarkOnPrimaryContainer = Color(0xFFD5E6FF)
val ProDarkSecondary = Color(0xFF35C8D7)
val ProDarkOnSecondary = Color(0xFF002023)
val ProDarkSecondaryContainer = Color(0xFF004F56)
val ProDarkOnSecondaryContainer = Color(0xFFA5EFF5)
val ProDarkTertiary = Color(0xFFA66BFF)
val ProDarkOnTertiary = Color(0xFF21005D)
val ProDarkTertiaryContainer = Color(0xFF4C1D95)
val ProDarkOnTertiaryContainer = Color(0xFFEBDDFF)
val ProDarkError = Color(0xFFFF5F66)
val ProDarkOnError = Color(0xFF690005)
val ProDarkErrorContainer = Color(0xFF93000A)
val ProDarkOnErrorContainer = Color(0xFFFFDAD6)
val ProDarkSurface = Color(0xFF080A0D)
val ProDarkSurfaceContainerLowest = Color(0xFF000000)
val ProDarkSurfaceContainerLow = Color(0xFF0A0D11)
val ProDarkSurfaceContainer = Color(0xFF10141A)
val ProDarkSurfaceContainerHigh = Color(0xFF171C23)
val ProDarkSurfaceContainerHighest = Color(0xFF20262E)
val ProDarkOnSurface = Color(0xFFF5F7FA)
val ProDarkOnSurfaceVariant = Color(0xFFA8B0BA)
val ProDarkOutline = Color(0xFF343B44)
val ProDarkOutlineVariant = Color(0xFF242A31)
val ProDarkInverseSurface = Color(0xFFE4F1F1)
val ProDarkInverseOnSurface = Color(0xFF0D2124)

// Status semantico dedicado (fora do vocabulario padrao de `ColorScheme`, que nao tem
// "sucesso") -- usado pelo destaque de RSSI da Walk Test (tela 2.11). `ProSuccess` (claro)
// ja existia sem uso; `ProSuccessDark` completa o par claro/escuro (issue #1176: "excelente/
// bom" = #32D978 no escuro).
val ProSuccessDark = Color(0xFF32D978)

// Tokens de profundidade (issue #1170 item 3, doc 10_SignallQ_Pro_Design_System_v5.md secao
// 4.1) -- Nivel 2 (selecionado) e Nivel 3 (sobreposto: dialog/bottom sheet/menu/tooltip).
// Nivel 0 (`ProBackground`) e Nivel 1 (`ProSurface`) ja existiam antes desta issue. Nao tem
// slot nativo em `ColorScheme` (exceto `scrim`, ver `SignallQProTheme.kt`) -- consumidas
// direto pelos componentes que precisarem, mesmo padrao de `ProSuccess`/`ProWarning`.
val ProSurfaceSelected = Color(0xFFEAF2FF)

// Corrigido em #1170 item 1: PR #1187 mergeou 0xFF1A2C42 como aproximacao (sem acesso ao CSS
// real na hora). Valor canonico de `--sqp-color-surface-selected` em `[data-theme="dark"]`.
val ProDarkSurfaceSelected = Color(0xFF192739)

val ProSurfaceOverlay = Color(0xFFFFFFFF)
val ProDarkSurfaceOverlay = Color(0xFF20262E)

val ProScrim = Color(0x80000000)
val ProScrimDark = Color(0x99000000)

// Par claro/escuro de `ProWarning` (issue #1170 item 1) -- faltava o par escuro, mesmo padrao
// de `ProSuccess`/`ProSuccessDark`. Consumido pelo `TopBar` (save-dot em estado OFFLINE).
val ProDarkWarning = Color(0xFFF5C451)
