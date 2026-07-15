# Tokens de Cores — SignallQ

**Fonte de verdade:** `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`  
**Escopo:** Android v0.23.0+  
**Última atualização:** 2026-07-15

---

## Brand e status

| Token | Valor |
| --- | --- |
| `accent` | `#5B21D6` |
| `accentBlue` | `#2563EB` |
| `success` | `#146C2E` |
| `warning` | `#8A5000` |
| `error` | `#BA1A1A` |

---

## Tema claro

| Token | Valor |
| --- | --- |
| `primary` | `#5B21D6` |
| `onPrimary` | `#FFFFFF` |
| `primaryContainer` | `#EAE0FF` |
| `onPrimaryContainer` | `#210A5C` |
| `secondary` | `#2851B8` |
| `onSecondary` | `#FFFFFF` |
| `secondaryContainer` | `#DCE6FF` |
| `onSecondaryContainer` | `#001A41` |
| `surface` | `#FFFFFF` |
| `surfaceDim` | `#DED8E1` |
| `surfaceContainerLowest` | `#FFFFFF` |
| `surfaceContainerLow` | `#F8F5FB` |
| `surfaceContainer` | `#F3EEFA` |
| `surfaceContainerHigh` | `#ECE5F5` |
| `surfaceContainerHighest` | `#E6DDF2` |
| `onSurface` | `#1C1B1F` |
| `onSurfaceVariant` | `#49454F` |
| `outline` | `#79747E` |
| `outlineVariant` | `#CAC4D0` |
| `inverseSurface` | `#313033` |
| `inverseOnSurface` | `#F4EFF4` |
| `errorContainer` | `#FFDAD6` |
| `onErrorContainer` | `#410002` |
| `successContainer` | `#B6F2BE` |
| `onSuccessContainer` | `#04210D` |
| `warningContainer` | `#FFDDB3` |
| `onWarningContainer` | `#2B1700` |
| `phaseLatencia` | `#2563EB` |
| `phaseDownload` | `#146C2E` |
| `phaseUpload` | `#8A5000` |

---

## Tema escuro

| Token | Valor |
| --- | --- |
| `primary` | `#D0BCFF` |
| `onPrimary` | `#38137E` |
| `primaryContainer` | `#4F2FA8` |
| `onPrimaryContainer` | `#EADDFF` |
| `secondary` | `#AAC7FF` |
| `onSecondary` | `#002E69` |
| `secondaryContainer` | `#1E427A` |
| `onSecondaryContainer` | `#D9E2FF` |
| `surface` | `#131217` |
| `surfaceDim` | `#131217` |
| `surfaceContainerLowest` | `#0E0D12` |
| `surfaceContainerLow` | `#1D1B20` |
| `surfaceContainer` | `#211F26` |
| `surfaceContainerHigh` | `#2B2930` |
| `surfaceContainerHighest` | `#36343B` |
| `onSurface` | `#E6E0E9` |
| `onSurfaceVariant` | `#CAC4D0` |
| `outline` | `#948F99` |
| `outlineVariant` | `#49454F` |
| `inverseSurface` | `#E6E0E9` |
| `inverseOnSurface` | `#313033` |
| `error` | `#FFB4AB` |
| `onError` | `#690005` |
| `errorContainer` | `#93000A` |
| `onErrorContainer` | `#FFDAD6` |
| `success` | `#83DA99` |
| `onSuccess` | `#00390F` |
| `successContainer` | `#0A5321` |
| `onSuccessContainer` | `#9DF4AC` |
| `warning` | `#FFB870` |
| `onWarning` | `#4A2900` |
| `warningContainer` | `#693D00` |
| `onWarningContainer` | `#FFDDB3` |
| `phaseLatencia` | `#AAC7FF` |
| `phaseDownload` | `#83DA99` |
| `phaseUpload` | `#FFB870` |

---

## Regras de uso

- Preferir `LocalLkTokens.current` e `MaterialTheme.colorScheme`.
- `Color(0x...)` fora do tema só é aceitável quando:
  - for cor de marca de terceiro
  - for gráfico técnico com paleta própria justificada
  - houver impossibilidade prática de representar a cor via token

---

## Observação importante

A antiga superfície dedicada de IA deixou de fazer parte do fluxo principal do app. Os tokens escuros especiais podem existir no código por compatibilidade/legado, mas não devem ser usados para novas telas do fluxo principal.
