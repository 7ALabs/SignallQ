# Material Design 3

App uses Jetpack Compose Material 3.

## Principles

- **Aesthetics**: Modern, updated components
- **Dynamic Color**: Theme extraction from system (Android 12+)
- **Meaningful Motion**: Animations follow MD3 standards
- **Accessibility**: Contrast, usability standards

## Implementation

- **Components**: See `COMPONENTS.md`
- **Colors**: See `COLORS.md` (dynamic + static)
- **Typography**: See `TYPOGRAPHY.md`
- **Spacing**: See `SPACING.md`
- **Motion**: See `MOTION.md`
- **Patterns**: See `NAVIGATION.md`, `CHAT_PATTERNS.md`

## Theme File

- Location: `app/src/main/kotlin/io/linka/app/kotlin/ui/LinkaTheme.kt`
- Applies MD3 system via `MaterialTheme`
- Uses `androidx.compose.material3` library

## Deps

- `androidx.compose.material3:material3`
- See `build.gradle.kts` files

**Needs validation**: Custom MD3 variants, exact dynamic color implementation
