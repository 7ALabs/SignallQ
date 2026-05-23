# Tokens de Cores — Linka

**Fonte:** `LinkaTheme.kt` (LkColors) | `src/tokens.css` (PWA)  
**Escopo:** Android v0.6.3 | PWA v1.0.0

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

## Orbit (Sempre Escuro)

Componentes IA mantêm sempre paleta escura independente do tema do app.

| Token | Valor | Uso |
|---|---|---|
| `linkaBlack` | `#0D0D1A` | Background do Orbit |
| `linkaDarkSurface` | `#1A0B2E` | Superfícies secundárias dentro do Orbit |
| `linkaDarkCard` | `#1E1130` | Cards de bolhas de IA |
| `linkaTextOnDark` | `#F3F4F6` | Texto primário (high contrast) |
| `linkaTextSecondaryOnDark` | `#9CA3AF` | Texto secundário |

---

## Notas

- **Orbit não adapta:** IA mantém aparência escura mesmo em tema claro do sistema.
- **Phase colors:** Mostram progresso visual durante o speedtest. Exibem nos indicadores de fase e na animação do gauge.
- **Acessibilidade:** Todas as combinações de texto+fundo atendem WCAG AA.
- **Material Design 3:** Tokens primário, secundário e terciário estão mapeados para `accent`, `accentBlue` e superfícies.
