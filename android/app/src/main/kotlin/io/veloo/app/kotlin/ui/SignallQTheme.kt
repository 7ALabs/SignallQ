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

// ============================================================================
// MIGRAÇÃO FASE 0 (2026-07-13, issue design-tobe-alinhamento) — tokens migrados
// para o Fluxo de Telas To-Be. Fonte da verdade: .claude/skills/SignallQ-design/
// colors_and_type.css + docs_ai/design-system/DECISAO_ALINHAMENTO_TOBE_2026-07-13.md
// (ver também docs_ai/operations/AUDITORIA_DESIGN_TOBE_2026-07-13.md).
// Escopo desta fase é SÓ este arquivo — nenhuma tela/Composable de ui/screen/ ou
// ui/component/ foi redesenhada; ajustes fora daqui são só os mínimos para compilar.
// ============================================================================

object LkColors {
    // ---- Paleta MD3 To-Be — tokens flat (valor CLARO) ----
    // accent/success/warning/error/secondary abaixo são consumidos DIRETO por
    // dezenas de telas sem passar pelo MaterialTheme (comportamento herdado,
    // NÃO theme-aware — já era assim antes desta migração). Migrar esses usos
    // para variantes de tema é trabalho de tela, fora do escopo da Fase 0.
    val accent = Color(0xFF5B21D6) // primary (era 0xFF6C2BFF)
    val success = Color(0xFF146C2E) // era 0xFF22C55E
    val warning = Color(0xFF8A5000) // era 0xFFF5A623
    val error = Color(0xFFBA1A1A) // era 0xFFFF4D4F

    // secondary — NOVO token desta migração (azul fixo, NÃO deriva da tríade do
    // primary). Antes não existia um token "secondary" próprio; accentBlue
    // (abaixo) é um alias legado com valor diferente, mantido por compatibilidade
    // com os usos existentes — não confundir os dois nem migrar um para o outro
    // sem revisar os ~10 consumidores de accentBlue.
    val secondary = Color(0xFF2851B8)

    // Variante clara do accent p/ TEXTO/ÍCONE sobre superfícies escuras (dark
    // theme ou fundo fixo SignallQ IA). Ainda não recalibrada para o novo
    // #5B21D6 nesta migração — fora do escopo (SignallQ IA é superfície
    // descontinuada, ver DECISAO_ALINHAMENTO_TOBE_2026-07-13.md).
    val accentOnDark = Color(0xFF9B6BFF)

    // accentBlue — alias legado anterior ao token `secondary` acima (valor
    // diferente: 0xFF2563EB vs 0xFF2851B8). ~10 arquivos consomem direto;
    // não migrado nesta fase.
    val accentBlue = Color(0xFF2563EB)

    // ── Cores utilitárias para texto sobre fundos escuros/coloridos ──
    // SignallQ IA (DESCONTINUADA — mantida só como registro histórico do As-Is,
    // ver Fluxo de Telas tela 7). NÃO usar em superfície nova. Fora do escopo.
    val signallQBlack = Color(0xFF0D0D1A)
    val signallQDarkSurface = Color(0xFF1A0B2E)
    val signallQDarkCard = Color(0xFF1E1130)
    val signallQTextOnDark = Color(0xFFF3F4F6)
    val signallQTextSecondaryOnDark = Color(0xFF9CA3AF)

    // Fases do SpeedTest — valor flat = variante CLARA (baseline dos consumidores
    // atuais, que ainda não leem tema). Ver Light/Dark.phaseLatencia/Download/
    // Upload abaixo para a versão theme-aware (já disponível via LkTokens,
    // adoção pelas telas de SpeedTest fica para fase futura).
    val phaseLatencia = Color(0xFF2563EB) // era 0xFF60A5FA
    val phaseDownload = Color(0xFF146C2E) // era 0xFF34D399
    val phaseUpload = Color(0xFF8A5000) // era 0xFFFBBF24

    // ── Tokens semânticos de container — seguem o tema (light/dark) ──
    // Uso: backgrounds de chips e cards de status, nunca hardcodados na UI.
    // NÃO migrados nesta fase (fora dos 8 itens do escopo da Fase 0) — hex
    // mantidos como estavam.
    object Light {
        // ---- Novos tokens MD3 desta migração (Fluxo de Telas) ----
        val primary = Color(0xFF5B21D6)
        val secondary = Color(0xFF2851B8)
        val success = Color(0xFF146C2E)
        val warning = Color(0xFF8A5000)
        val error = Color(0xFFBA1A1A)

        // onSurface/onSurfaceVariant/outline/outlineVariant — antes só existia
        // um único `border` (abaixo, mantido como alias depreciado = outline).
        val onSurface = Color(0xFF1C1B1F)
        val onSurfaceVariant = Color(0xFF49454F)
        val outline = Color(0xFF79747E)
        val outlineVariant = Color(0xFFCAC4D0)

        // surfaceContainer — 5 níveis reais da spec (antes só bgPrimary/
        // bgSecondary/bgCard, sem escala de elevação tonal própria).
        val surfaceContainerLowest = Color(0xFFFFFFFF)
        val surfaceContainerLow = Color(0xFFF8F5FB)
        val surfaceContainer = Color(0xFFF3EEFA)
        val surfaceContainerHigh = Color(0xFFECE5F5)
        val surfaceContainerHighest = Color(0xFFE6DDF2)

        // Fases do SpeedTest — variante clara (idêntica ao flat acima).
        val phaseLatencia = Color(0xFF2563EB)
        val phaseDownload = Color(0xFF146C2E)
        val phaseUpload = Color(0xFF8A5000)

        // ---- Tokens pré-existentes, NÃO tocados nesta migração (fora do
        // escopo dos 8 itens da Fase 0) ----
        val bgPrimary = Color(0xFFFFFFFF)
        val bgSecondary = Color(0xFFF3F4F6)
        val bgCard = Color(0xFFFFFFFF)
        val textPrimary = Color(0xFF0D0D1A)
        val textSecondary = Color(0xFF6B7280)
        val textTertiary = Color(0xFF9CA3AF)

        // DEPRECIADO — mapeava um único token ambíguo entre outline/
        // outlineVariant. Mantido só para não quebrar os 2 consumidores diretos
        // fora deste arquivo (LaudoScreen.kt, ResultadoPdfGenerator.kt) que
        // ainda não foram migrados; valor agora aponta para `outline`.
        val border = outline

        // warning (âmbar): fundo claro + texto escuro âmbar (contraste >=4.5:1)
        val warningContainer = Color(0xFFFFF3CD)
        val onWarningContainer = Color(0xFF7A4E00)
        val amberSurface = Color(0xFFFFF8E6)

        // success (verde): fundo claro + texto escuro verde
        val successContainer = Color(0xFFD1FAE5)
        val onSuccessContainer = Color(0xFF065F46)
    }

    object Dark {
        val primary = Color(0xFFD0BCFF)
        val secondary = Color(0xFFAAC7FF)
        val success = Color(0xFF83DA99)
        val warning = Color(0xFFFFB870)
        val error = Color(0xFFFFB4AB)

        val onSurface = Color(0xFFE6E0E9)
        val onSurfaceVariant = Color(0xFFCAC4D0)
        val outline = Color(0xFF948F99)
        val outlineVariant = Color(0xFF49454F)

        val surfaceContainerLowest = Color(0xFF0E0D12)
        val surfaceContainerLow = Color(0xFF1D1B20)
        val surfaceContainer = Color(0xFF211F26)
        val surfaceContainerHigh = Color(0xFF2B2930)
        val surfaceContainerHighest = Color(0xFF36343B)

        val phaseLatencia = Color(0xFFAAC7FF)
        val phaseDownload = Color(0xFF83DA99)
        val phaseUpload = Color(0xFFFFB870)

        val bgPrimary = Color(0xFF000000)
        val bgSecondary = Color(0xFF1A1A1A)
        val bgCard = Color(0xFF111111)
        val textPrimary = Color(0xFFF3F4F6)
        val textSecondary = Color(0xFF9CA3AF)
        val textTertiary = Color(0xFF6B7280)

        val border = outline // DEPRECIADO — ver comentário em Light.border

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
    // ── Tokens MD3 desta migração ──
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val outline: Color,
    val outlineVariant: Color,
    // border — DEPRECIADO, ambíguo entre outline/outlineVariant (ver comentário
    // em LkColors.Light/Dark.border). Mantido só por compatibilidade — o app
    // tem dezenas de usos de `LocalLkTokens.current.border` (via variáveis
    // locais `c`/`tokens`) fora do escopo desta Fase 0; migrar cada um para
    // outline ou outlineVariant conforme o caso é trabalho de tela, não deste
    // arquivo. Valor aponta para `outline`.
    val border: Color,
    // ── Fases do SpeedTest, theme-aware (infra pronta; adoção pelas telas de
    // SpeedTest é trabalho de fase futura, fora do escopo da Fase 0) ──
    val phaseLatencia: Color,
    val phaseDownload: Color,
    val phaseUpload: Color,
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
            onSurface = LkColors.Light.onSurface,
            onSurfaceVariant = LkColors.Light.onSurfaceVariant,
            outline = LkColors.Light.outline,
            outlineVariant = LkColors.Light.outlineVariant,
            border = LkColors.Light.border,
            phaseLatencia = LkColors.Light.phaseLatencia,
            phaseDownload = LkColors.Light.phaseDownload,
            phaseUpload = LkColors.Light.phaseUpload,
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

    // base — NOVO nome desta migração para o antigo valor de `lg` (16dp,
    // padding padrão de tela/card). Use `base` em código novo.
    val base: Dp = 16.dp

    // lg — ATENÇÃO: a spec (Fluxo de Telas) redefine lg=20dp. NÃO alterado
    // nesta fase: 216 usos em 44 arquivos hoje esperam o valor antigo (16dp).
    // Mudar o valor aqui sem renomear os consumidores quebraria o espaçamento
    // visual em quase toda tela do app. Decisão de fazer o rename em massa
    // (ou tela a tela) pendente — reportado no fechamento da Fase 0.
    val lg: Dp = 16.dp

    val xl: Dp = 24.dp
    val xxl: Dp = 32.dp

    // xxxl — NOVO degrau da spec (CTA de onboarding, rodapés). Sem consumidor
    // ainda, seguro adicionar.
    val xxxl: Dp = 40.dp

    val cardContent: Dp = 16.dp // legado, mesmo valor de `base`
}

object LkRadius {
    val card: Dp = 16.dp // já batia com a spec (Card=16px)

    // button — ATENÇÃO: a spec (Fluxo de Telas) define Button=20dp (altura
    // 40dp). NÃO alterado nesta fase: 33 usos em 13 arquivos hoje esperam o
    // valor antigo (12dp). Decisão de aplicar o valor novo (ou tela a tela)
    // pendente — reportado no fechamento da Fase 0.
    val button: Dp = 12.dp

    // buttonHeight — NOVO, spec de componente Button (40dp). Sem consumidor
    // ainda, seguro adicionar.
    val buttonHeight: Dp = 40.dp

    val input: Dp = 12.dp // Field na spec (12px) — já batia, nome mantido (só 4 usos)

    // sheet/dialog/chip/badge — NOVOS tokens desta migração, sem consumidor
    // ainda no código (fora do escopo redesenhar SheetFrame/Dialog agora).
    val sheet: Dp = 28.dp // SheetFrame, cantos superiores
    val dialog: Dp = 24.dp // Dialog (ex.: RestartDialog)
    val chip: Dp = 999.dp
    val badge: Dp = 999.dp
}

private val lightScheme =
    lightColorScheme(
        primary = LkColors.Light.primary,
        onPrimary = Color.White,
        secondary = LkColors.Light.secondary,
        background = LkColors.Light.bgPrimary,
        onBackground = LkColors.Light.textPrimary,
        surface = LkColors.Light.bgCard,
        onSurface = LkColors.Light.onSurface,
        onSurfaceVariant = LkColors.Light.onSurfaceVariant,
        outline = LkColors.Light.outline,
        outlineVariant = LkColors.Light.outlineVariant,
        surfaceContainerLowest = LkColors.Light.surfaceContainerLowest,
        surfaceContainerLow = LkColors.Light.surfaceContainerLow,
        surfaceContainer = LkColors.Light.surfaceContainer,
        surfaceContainerHigh = LkColors.Light.surfaceContainerHigh,
        surfaceContainerHighest = LkColors.Light.surfaceContainerHighest,
    )

private val darkScheme =
    darkColorScheme(
        primary = LkColors.Dark.primary,
        onPrimary = Color.White,
        secondary = LkColors.Dark.secondary,
        background = LkColors.Dark.bgPrimary,
        onBackground = LkColors.Dark.textPrimary,
        surface = LkColors.Dark.bgCard,
        onSurface = LkColors.Dark.onSurface,
        onSurfaceVariant = LkColors.Dark.onSurfaceVariant,
        outline = LkColors.Dark.outline,
        outlineVariant = LkColors.Dark.outlineVariant,
        surfaceContainerLowest = LkColors.Dark.surfaceContainerLowest,
        surfaceContainerLow = LkColors.Dark.surfaceContainerLow,
        surfaceContainer = LkColors.Dark.surfaceContainer,
        surfaceContainerHigh = LkColors.Dark.surfaceContainerHigh,
        surfaceContainerHighest = LkColors.Dark.surfaceContainerHighest,
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
                onSurface = LkColors.Dark.onSurface,
                onSurfaceVariant = LkColors.Dark.onSurfaceVariant,
                outline = LkColors.Dark.outline,
                outlineVariant = LkColors.Dark.outlineVariant,
                border = LkColors.Dark.border,
                phaseLatencia = LkColors.Dark.phaseLatencia,
                phaseDownload = LkColors.Dark.phaseDownload,
                phaseUpload = LkColors.Dark.phaseUpload,
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
                onSurface = LkColors.Light.onSurface,
                onSurfaceVariant = LkColors.Light.onSurfaceVariant,
                outline = LkColors.Light.outline,
                outlineVariant = LkColors.Light.outlineVariant,
                border = LkColors.Light.border,
                phaseLatencia = LkColors.Light.phaseLatencia,
                phaseDownload = LkColors.Light.phaseDownload,
                phaseUpload = LkColors.Light.phaseUpload,
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

// Google Sans Flex (GH#929) — fonte do protótipo MD3 To-Be. Licença SIL OFL 1.1 confirmada via
// catálogo ao vivo do Google Fonts (fonts.google.com/metadata/fonts, isOpenSource=true) e servida
// pelo CDN oficial (fonts.gstatic.com) — texto completo da licença em
// android/app/src/main/assets/licenses/google_sans_flex_OFL.txt. Instâncias estáticas (não a
// variable font completa) baixadas direto do CDN oficial, uma por peso usado na escala abaixo.
private val signallQFontFamily =
    FontFamily(
        Font(R.font.google_sans_flex_regular, weight = FontWeight.Normal),
        Font(R.font.google_sans_flex_medium, weight = FontWeight.Medium),
        Font(R.font.google_sans_flex_semibold, weight = FontWeight.SemiBold),
        Font(R.font.google_sans_flex_bold, weight = FontWeight.Bold),
    )

// Escala tipográfica SignallQ — migrada nesta fase para os 12 estilos exatos do
// Fluxo de Telas (colors_and_type.css). Valores copiados literalmente da spec
// (tamanho/lineHeight/peso/letterSpacing); NENHUM valor abaixo foi inventado.
//
// displayLarge e headlineMedium NÃO existem mais na escala nova (a spec tem só
// 12 estilos, não 15) — mas seguem usados direto por MaterialTheme.typography
// em telas fora do escopo desta fase (HomeScreen, OperadoraBottomSheet,
// ResultadoVelocidadeScreen, SinalScreen). Mantidos com o valor ANTIGO
// (inalterado) para não regredir essas telas para o Roboto/tamanho default do
// Material3 — migração desses 2 estilos fica para fase tela a tela.
private val signallQTypography =
    Typography(
        // ---- Legado, NÃO migrado (fora da escala nova de 12 estilos) ----
        displayLarge = TextStyle(fontFamily = signallQFontFamily, fontSize = 34.sp, fontWeight = FontWeight.Bold),
        headlineMedium = TextStyle(fontFamily = signallQFontFamily, fontSize = 20.sp, fontWeight = FontWeight.SemiBold),
        // ---- Migrado — 12 estilos exatos do Fluxo de Telas ----
        displaySmall =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 34.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
        headlineLarge =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 26.sp,
                lineHeight = 32.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.sp,
            ),
        headlineSmall =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 22.sp,
                lineHeight = 28.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.sp,
            ),
        titleLarge =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 20.sp,
                lineHeight = 26.sp,
                fontWeight = FontWeight.SemiBold,
                letterSpacing = 0.1.sp,
            ),
        titleMedium =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 16.sp,
                lineHeight = 22.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp,
            ),
        titleSmall =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp,
            ),
        bodyLarge =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 16.sp,
                lineHeight = 24.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.15.sp,
            ),
        bodyMedium =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.2.sp,
            ),
        bodySmall =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Normal,
                letterSpacing = 0.25.sp,
            ),
        labelLarge =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 14.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.1.sp,
            ),
        labelMedium =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 12.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.3.sp,
            ),
        labelSmall =
            TextStyle(
                fontFamily = signallQFontFamily,
                fontSize = 11.sp,
                lineHeight = 16.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 0.4.sp,
            ),
    )
