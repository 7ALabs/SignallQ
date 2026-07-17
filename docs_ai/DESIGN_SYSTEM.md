# Design System — SignallQ (Android)

- **Status:** ativo
- **Última validação:** 2026-07-16
- **Fonte de verdade:** este documento consolida `docs_ai/design-system/*.md`; a fonte de verdade
  do *código* é `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`
  (`LkColors`, `LkTokens`, `LkSpacing`, `LkRadius`, `signallQTypography`); a fonte de verdade da
  *documentação viva de design* (não-negociáveis) é `.claude/CLAUDE.md`, seção "Design System"
- **Escopo:** app Android SignallQ (`io.signallq.app`), v0.25.0+
- **Responsável:** Lia (conteúdo/tokens/skill), aplicado por Camilo (implementação Android)
- **Documentos substituídos:** consolida `docs_ai/design-system/COLORS.md`,
  `COMPONENTS_ANDROID.md`, `DESIGN_TOKENS.md`, `MD3_GUIDELINES.md`, `SPACING.md`,
  `TYPOGRAPHY.md` — arquivos individuais movidos para `docs_ai/_archive/` com nota de substituição

---

## 1. Princípios

- **Material 3** via `MaterialTheme` padrão do Jetpack Compose (`androidx.compose.material3`),
  sem sobrescrita de shapes/componentes MD3 custom.
- **Cor de marca fixa** — `lightColorScheme`/`darkColorScheme` com acento fixo, **sem dynamic
  color** do sistema (não deriva do wallpaper).
- **Flat, elevação tonal** — sem sombra dura; profundidade vem de tint de superfície
  (`surfaceContainer*`).
- **Métrica crua sempre acompanhada de veredito humano** — nenhum número solto na UI; sempre
  junto de um veredito (Excelente/Bom/Regular/Fraco/Forte).
- **Copy em PT-BR, com "você"** — sentence case em títulos, UPPERCASE em overlines, sem emoji.
  Separador inline padrão: ponto médio (`·`).
- **Superfície SignallQ (IA) descontinuada no To-Be** — não implementar rota ou componente novo
  para essa superfície (ver seção 10).

---

## 2. Cores

**Fonte de verdade:** `SignallQTheme.kt` — objeto `LkColors`. Confirmado em código (2026-07-16):
os hex abaixo **já estão implementados**, não são meta futura.

### Brand e status (compartilhado claro/escuro)

| Token | Valor |
| --- | --- |
| `accent` (Primary) | `#5B21D6` |
| `accentBlue` | `#2563EB` |
| `success` | `#146C2E` |
| `warning` | `#8A5000` |
| `error` | `#BA1A1A` |

### Tema claro

| Token | Valor |
| --- | --- |
| `primary` | `#5B21D6` |
| `onPrimary` | `#FFFFFF` |
| `primaryContainer` | `#EAE0FF` |
| `onPrimaryContainer` | `#210A5C` |
| `secondary` | `#2851B8` (azul **fixo**, não deriva mais do primary) |
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

### Tema escuro

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

### Regras de uso

- Preferir `LocalLkTokens.current` e `MaterialTheme.colorScheme`.
- `Color(0x...)` fora do tema só é aceitável quando: for cor de marca de terceiro (ex.: logo de
  operadora), for gráfico técnico com paleta própria justificada, ou houver impossibilidade
  prática de representar a cor via token.
- A antiga superfície dedicada de IA pode ter tokens escuros especiais residuais no código por
  compatibilidade/legado — não usar para telas novas do fluxo principal.

---

## 3. Tipografia

**Fonte de verdade:** `SignallQTheme.kt` — `signallQTypography`.

Família única: **Google Sans Flex** (arquivos `.ttf` embutidos em
`android/app/src/main/res/font/google_sans_flex_*.ttf`, licença SIL OFL 1.1 — não é a "Google
Sans" proprietária, é uma família distinta com licença aberta). Nenhuma nova tela deve introduzir
segunda família tipográfica.

Pesos em uso: `400` Normal, `500` Medium, `600` SemiBold, `700` Bold.

| Token | Tamanho | Line height | Peso | Tracking |
| --- | --- | --- | --- | --- |
| `displayLarge` | 34 sp | 40 sp | Bold | 0 |
| `displayMedium` | 34 sp | 40 sp | Bold | 0 |
| `displaySmall` | 34 sp | 40 sp | Bold | 0 |
| `headlineLarge` | 26 sp | 32 sp | Bold | 0 |
| `headlineMedium` | 26 sp | 32 sp | Bold | 0 |
| `headlineSmall` | 22 sp | 28 sp | SemiBold | 0 |
| `titleLarge` | 20 sp | 26 sp | SemiBold | 0 |
| `titleMedium` | 16 sp | 22 sp | Medium | 0.1 |
| `titleSmall` | 14 sp | 20 sp | Medium | 0.1 |
| `bodyLarge` | 16 sp | 24 sp | Normal | 0.15 |
| `bodyMedium` | 14 sp | 20 sp | Normal | 0.2 |
| `bodySmall` | 12 sp | 16 sp | Normal | 0.25 |
| `labelLarge` | 14 sp | 20 sp | Medium | 0.1 |
| `labelMedium` | 12 sp | 16 sp | Medium | 0.3 |
| `labelSmall` | 11 sp | 16 sp | Medium | 0.4 |

### Regras de uso

- Preferir sempre `MaterialTheme.typography.*`.
- Evitar `fontSize = ...sp` e `letterSpacing = ...sp` hardcoded em tela/componente comum.
- Só usar `TextStyle(...)` manual com motivo técnico real (canvas, chart labels, renderização
  custom).
- **Dívida conhecida:** ainda existem telas/componentes com `fontSize`/`letterSpacing`
  hardcoded — tratar como dívida de padronização ao tocar na área, não replicar o padrão.

---

## 4. Espaçamento / grid

**Fonte de verdade:** `LkSpacing` em `SignallQTheme.kt`. Grid de 8dp, 8 degraus:

| Token | Valor | Uso |
| --- | --- | --- |
| `xs` | 4 dp | ajustes finos, separações mínimas |
| `sm` | 8 dp | gaps simples, paddings pequenos |
| `md` | 12 dp | espaçamento padrão interno |
| `base` | 16 dp | padding horizontal principal e cards |
| `lg` | 20 dp | separações de bloco e grupos mais densos |
| `xl` | 24 dp | separações claras entre seções |
| `xxl` | 32 dp | grandes blocos ou respiros |
| `xxxl` | 40 dp | grandes aberturas verticais |
| `cardContent` | 16 dp | padding interno de card |

### Regras

- Preferir `LkSpacing` a valores literais (`16.dp`, `24.dp` direto no código).
- Valor fora da escala é exceção técnica, não padrão visual.
- Medidas como `3.dp`, `5.dp`, `10.dp`, `11.dp`, `13.dp`, `14.dp` em layout comum são dívida
  visual conhecida.

---

## 5. Raios por componente

**Fonte de verdade:** `LkRadius` em `SignallQTheme.kt`.

| Componente | Token | Valor |
| --- | --- | --- |
| Card | `LkRadius.card` | 16 dp |
| SheetFrame | `LkRadius.sheet` | 28 dp |
| Button | `LkRadius.button` | 20 dp |
| Field (input) | `LkRadius.input` | 12 dp |
| Chip / Badge | `LkRadius.pill` | 999 dp |
| Dialog | — | 24 dp |

`LkRadius.pill` cobre tanto chip quanto badge (mesmo token, raio total).

---

## 6. Componentes

**Localização principal:** `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/`
**Arquivo-base atual:** `BaseComponents.kt`

Componentes-base compartilhados do fluxo principal: `LkSurfaceCard`, `LkSectionOverline`,
`LkSheetSectionTitle`, `LkSheetInfoRow`, `LkSheetDivider`, `LkSheetFrame`.

### Diretriz de arquitetura visual

Toda tela nova ou refinada deve: usar tokens de `SignallQTheme.kt`; preferir componentes
compartilhados; evitar recriar manualmente card, row, badge, divider e frame de sheet. Se um
padrão visual se repetir em duas ou mais telas, ele deve migrar para a biblioteca compartilhada.

### Situação atual (dívida conhecida)

A biblioteca compartilhada existe mas cobre só parte dos padrões. Muitas telas ainda
reimplementam localmente: cards de seção, rows com ícone+título+subtítulo+ação, headers de sheet,
badges e disclosures, métricas em grade. Meta da fase de saneamento: fluxo principal depender
majoritariamente da biblioteca compartilhada, não de estilização artesanal por tela.

---

## 7. Estados (loading / erro / vazio)

Não há documento dedicado de estados visuais consolidado nas fontes originais. Diretriz vigente:
todo estado assíncrono de tela (loading, erro, vazio, sucesso) deve usar os tokens de
cor/tipografia/espaçamento acima — nunca estilo ad hoc — e seguir a regra de "métrica crua sempre
com veredito humano" quando aplicável a resultado de diagnóstico/velocidade. Consultar a skill
`padroes-compose` para o checklist de `UiState` por tela antes de criar um novo padrão.

---

## 8. Acessibilidade

Não há documento dedicado de acessibilidade nas fontes originais de `docs_ai/design-system/`. Os
manuais MD3 anteriores (2026-07-11) definiam state layers (hover 8% / focus 10% / pressed 12% /
dragged 16%) e motion (`emphasized`/`standard`); a correção de 2026-07-13 marcou esses sistemas
como não redefinidos pelo documento To-Be atual — continuam valendo até uma spec sistêmica nova
ser publicada. Para revisão de contraste WCAG e heurísticas de acessibilidade mobile, usar a
skill `auditar-ux`.

---

## 9. Ícones

Material Symbols (**Outlined**, variable font — `material_symbols_outlined.ttf`, Apache 2.0) — não-negociável fixado em `.claude/CLAUDE.md` — GH#1008 (fundação). Fonte variável com eixos FILL, wght, GRAD, opsz; renderiza via ligadura OpenType (o nome do ícone em texto é substituído pelo glifo). Componente `LkSymbol` (GH#1008) encapsula uso, com suporte a `filled = true` para estado selecionado (eixo FILL) — migração de `androidx.compose.material.icons.*` tela a tela em andamento.

Não há tabela de mapeamento ícone→uso nas fontes originais; ao introduzir um ícone novo, usar o
catálogo https://fonts.google.com/icons?icon.set=Material+Symbols e manter peso/densidade consistentes com o restante da tela.

---

## 10. Dark mode

`SignallQTheme.kt` implementa `lightColorScheme`/`darkColorScheme` fixos com o acento da marca
(seção 2) — **sem dynamic color** do sistema. A paleta escura é completa (todos os roles MD3 com
hex próprios, não apenas inversão automática do claro).

A antiga superfície SignallQ (IA) usava paleta escura fixa própria (`signallQBlack` /
`signallQDarkSurface` / `signallQDarkCard`) que **não seguia** o tema claro/escuro do sistema —
essa superfície está descontinuada no Fluxo de Telas To-Be; não implementar rota ou componente
novo para ela. Tokens residuais podem continuar no código por legado/compatibilidade.

---

## 11. Copy e regras de texto

- PT-BR, com "você" (nunca "tu" ou tratamento formal "o senhor/a senhora").
- Sentence case em títulos (não Title Case, não UPPERCASE).
- UPPERCASE reservado a overlines.
- Sem emoji — decisão de produto, não afetada pelo MD3.
- Separador inline padrão: ponto médio (`·`).
- Métrica crua (Mbps, ms, %) sempre acompanhada de veredito humano: Excelente / Bom / Regular /
  Fraco / Forte.

---

## 12. Localização dos tokens no código

Confirmado em código nesta consolidação (2026-07-16):

- Arquivo: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt`
- Objetos: `LkColors` (cores claro/escuro), `LkTokens`, `LkSpacing`, `LkRadius`,
  `signallQTypography`
- Consumo em componentes confirmado via grep de `LkColors` em múltiplos arquivos de
  `ui/component/` (ex.: `BaseComponents.kt`, `ads/AdBadge.kt`, `AnaliseDetalhadaBottomSheet.kt`)

**Nota de caminho físico:** o arquivo mora fisicamente em `io/veloo/app/kotlin/...` embora
declare `package io.signallq.app...` — é a divergência conhecida de 460 arquivos `.kt`
documentada em `.claude/rules/higiene-e-padronizacao-repositorio.md` (seção 4.1). Não é
específica deste design system; é dívida estrutural do repo inteiro.

---

## 13. Outras fontes do design system (não duplicar aqui)

Existem artefatos paralelos de design no repo, cada um com escopo/finalidade próprios — não são
cópias redundantes entre si mesmo compartilhando os mesmos tokens visuais (referência:
`.claude/CLAUDE.md`, seção "Design System" → "Onde fica cada 'design system'"):

| Onde | Escopo | Finalidade |
| --- | --- | --- |
| `.claude/skills/SignallQ-design/` | Android (app real) | Skill do Claude Code — ativa sozinha ao pedir UI Android; fonte de verdade para gerar código/protótipo on-brand |
| `packages/design-system/` | Android (app real) | "Gêmeo digital" React, sincronizado com o projeto "SignallQ Design System" no Claude Design |
| `docs_ai/design-system/` (histórico) | Android (app real) | Os seis documentos-fonte consolidados aqui — movidos para `docs_ai/_archive/` |
| `DESIGN.md` / `PRODUCT.md` (raiz do repo) | Android (app real) | Spec no formato da skill `impeccable`, North Star "The Calm Translator" |
| `SignallQ Admin/DESIGN.md` / `PRODUCT.md` | SignallQ Console (Admin) | Mesmo formato impeccable, mas do Console — North Star e paleta próprias, não confundir com o app Android |

Não criar artefato de design novo sem checar se já existe em algum destes.

---

## 14. Divergência corrigida nesta consolidação

`MD3_GUIDELINES.md` (fonte antiga) afirmava que a implementação em `SignallQTheme.kt` ainda usava
o acento anterior (`#6C2BFF`) e que a atualização de código seria fase separada. Conferido o
código real: `primary = #5B21D6` e `secondary = #2851B8` **já estão implementados**, batendo com
os não-negociáveis atuais — corrigido acima (seção 2).

*Consolidação gerada a partir de `docs_ai/design-system/{COLORS,COMPONENTS_ANDROID,DESIGN_TOKENS,
MD3_GUIDELINES,SPACING,TYPOGRAPHY}.md` e das seções ativas de
`DECISAO_ALINHAMENTO_TOBE_2026-07-13.md` / `DECISAO_RENOMEACAO_SIGNALLQ_DESIGN_2026-07-11.md`.*
