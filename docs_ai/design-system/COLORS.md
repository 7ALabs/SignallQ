# Tokens de Cores — SignallQ

**Fonte:** `SignallQTheme.kt` (LkColors)
**Escopo:** Android v0.16.0
**Última atualização:** 2026-06-21

---

## Brand

| Token | Valor | Uso |
|---|---|---|
| `accent` | `#6C2BFF` | CTA primário, highlights, seleção, navegação ativa |
| `accentBlue` | `#2563EB` | Links complementares, badges informativos |

---

## Status

| Token | Valor | Uso |
|---|---|---|
| `success` | `#22C55E` | Conexão boa, testes concluídos, ações bem-sucedidas |
| `warning` | `#F5A623` | Alertas moderados, condições a monitorar |
| `error` | `#FF4D4F` | Erros críticos, falhas de conexão |

---

## SpeedTest Phases

| Token | Valor | Uso |
|---|---|---|
| `phaseLatencia` | `#60A5FA` | Fase de teste de latência/resposta |
| `phaseDownload` | `#34D399` | Fase de teste de download |
| `phaseUpload` | `#FBBF24` | Fase de teste de upload |

---

## Surface — Tema Claro

| Token | Valor | Uso |
|---|---|---|
| `Light.bgPrimary` | `#FFFFFF` | Fundo principal de telas |
| `Light.bgSecondary` | `#F3F4F6` | Backgrounds secundários, superfícies elevadas |
| `Light.bgCard` | `#FFFFFF` | Cards, superfícies de conteúdo |
| `Light.textPrimary` | `#0D0D1A` | Texto principal (título, corpo) |
| `Light.textSecondary` | `#6B7280` | Texto secundário, descrições |
| `Light.textTertiary` | `#9CA3AF` | Labels, captions, hints |
| `Light.border` | `#E5E7EB` | Divisores, bordas leves |

---

## Surface — Tema Escuro

| Token | Valor | Uso |
|---|---|---|
| `Dark.bgPrimary` | `#000000` | Fundo principal |
| `Dark.bgSecondary` | `#1A1A1A` | Backgrounds secundários |
| `Dark.bgCard` | `#111111` | Cards em tema escuro |
| `Dark.textPrimary` | `#F3F4F6` | Texto principal (contraste alto) |
| `Dark.textSecondary` | `#9CA3AF` | Texto secundário |
| `Dark.textTertiary` | `#6B7280` | Labels, captions |
| `Dark.border` | `#2A2A2A` | Divisores, bordas |

---

## Superfícies SignallQ IA (Sempre Escuro)

Componentes IA (`SignallQ*`) mantêm sempre paleta escura independente do tema do app. Tokens definidos em `SignallQTheme.kt`.

| Token | Valor | Uso |
|---|---|---|
| `linkaBlack` | `#0D0D1A` | Background principal da superfície SignallQ |
| `linkaDarkSurface` | `#1A0B2E` | Superfícies secundárias dentro do SignallQ |
| `linkaDarkCard` | `#1E1130` | Cards de bolhas de IA (`SignallQAiMessageBubble`) |
| `linkaTextOnDark` | `#F3F4F6` | Texto primário sobre escuro (high contrast) |
| `linkaTextSecondaryOnDark` | `#9CA3AF` | Texto secundário sobre escuro |

> Estes tokens permanecem fixos. `SignallQScreen`, `LLMChatScreen` e todos os componentes `SignallQ*` usam esta paleta independente de o usuário ter tema claro ou escuro no sistema.

---

## Notas

- **SignallQ IA não adapta ao tema:** mantém aparência escura mesmo em tema claro do sistema.
- **Phase colors:** Mostram progresso visual durante o speedtest nos indicadores de fase e na animação do `GaugeCircular`.
- **Acessibilidade:** Todas as combinações de texto+fundo atendem WCAG AA.
- **Material Design 3:** Tokens primário, secundário e terciário estão mapeados para `accent`, `accentBlue` e superfícies via `SignallQTheme.kt` (era `LinkaTheme.kt`).
