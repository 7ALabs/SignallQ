# Material Design 3

App uses Jetpack Compose Material 3.

## Principles

- **Aesthetics**: Modern, updated components
- **Dynamic Color**: Theme extraction from system (Android 12+)
- **Meaningful Motion**: Animations follow MD3 standards
- **Accessibility**: Contrast, usability standards

## Implementation

- **Components**: See `COMPONENTS_ANDROID.md` (doc canônico Android; `COMPONENTS.md` foi arquivado)
- **Colors**: See `COLORS.md` (dynamic + static)
- **Typography**: See `TYPOGRAPHY.md`
- **Spacing**: See `SPACING.md`
- **Design Tokens**: See `DESIGN_TOKENS.md`

## Theme File

- Location: `app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`
- Antigo nome: `LinkaTheme.kt` (renomeado no rebranding v0.15.0)
- Aplica MD3 via `MaterialTheme`
- Usa `androidx.compose.material3`
- Tokens de cores: `LkColors`, espaçamento: `LkSpacing`, tipografia: `linkaTypography`

**Última atualização:** 2026-06-21 (v0.16.0)

## Deps

- `androidx.compose.material3:material3`
- See `build.gradle.kts` files

**Needs validation**: Custom MD3 variants, exact dynamic color implementation
