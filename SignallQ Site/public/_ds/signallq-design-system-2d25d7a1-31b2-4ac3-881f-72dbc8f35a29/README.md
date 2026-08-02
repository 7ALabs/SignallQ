# SignallQ Design System — Conventions

> **Projeto canônico no Claude Design: `SignallQ Design System` (projectId `2d25d7a1-31b2-4ac3-881f-72dbc8f35a29`)** — o mesmo fixado em `.design-sync/config.json`. Criado em 2026-07-18 na **separação DS/protótipos** (ver `docs_ai/design-system/DECISAO_SEPARACAO_DS_PROTOTIPOS_2026-07-18.md`): este projeto contém **só o DS reutilizável** (tokens + primitivos + layout + animações, 14 componentes). O projeto antigo `e77ea465-291f-4bf5-930c-a267680da04e` foi renomeado para **"SignallQ — Protótipos"** e agora hospeda os fluxos (`tobe/`, `templates/`, `uploads/`), NÃO o DS — não sincronizar o pacote nele. Reuse sempre este `projectId`; o workspace tem vários "...Design System" (SignallQ PRO, Speedtest by SignallQ, 7Agents) — se a listagem mostrar mais de um **"SignallQ Design System"** exato, pare e confirme com a Claudete antes de sincronizar.

## Setup

Provider opcional. Sem `SignallQThemeProvider`, todo componente usa `LK` (tema claro) por
padrão — nada quebra em consumidores que não sabem que o tema escuro existe.

```tsx
import { SignallQThemeProvider } from '@signallq/design-system';

<SignallQThemeProvider mode="system">  {/* 'light' | 'dark' | 'system' (default) */}
  <App />
</SignallQThemeProvider>
```

Load Roboto and Material Symbols from Google Fonts (already in `styles.css`). For icons to display, the page must have the Material Symbols font loaded — it is included via the `styles.css` `@import` closure.

```html
<link rel="stylesheet" href="_ds/styles.css">
```

## Styling idiom

**All styling via the `LK` token object and `hexA()` helper — no CSS classes, no Tailwind, no styled-components.**

```tsx
import { LK, hexA } from '@signallq/design-system';

// Token usage
style={{ color: LK.textPrimary, background: LK.bgCard, borderRadius: LK.rCard }}

// Semantic colors
LK.accent      // #5B21D6 — primary CTA, selection, active nav (era #6C2BFF)
LK.success     // #146C2E — good connection, tests OK
LK.warning     // #8A5000 — moderate alerts
LK.error       // #BA1A1A — critical failures
LK.accentBlue  // #2851B8 — secondary FIXO (não deriva mais do accent), badges informativos

// Alpha tints (color at 10–30% opacity)
hexA(LK.success, 0.12)   // e.g. card fill
hexA(LK.success, 0.3)    // e.g. card border
```

**Token files:** `tokens/` in the bundle, `styles.css`, `_ds_bundle.css`. Read `styles.css` for the full token list.

### Dark mode

Todo componente do DS já resolve o tema sozinho via `useTokens()` internamente — nada a fazer
para consumi-los. Para compor um componente novo no mesmo idioma (claro/escuro reativo), use
`useTokens()` em vez de importar `LK` estático:

```tsx
import { useTokens } from '@signallq/design-system';

function MeuComponente() {
  const LK = useTokens(); // LK (claro) fora de um SignallQThemeProvider, LK_DARK dentro de um com mode="dark"
  return <div style={{ background: LK.surface, color: LK.onSurface }} />;
}
```

`LK`/`LK_DARK` estáticos continuam exportados para exemplos/protótipos que fixam um tema
deliberadamente (ex.: `ORB`, sempre escuro).

**Per-component docs:** each `components/<group>/<Name>/<Name>.prompt.md`.

## SignallQ AI surfaces

Use `ORB` tokens (not `LK`) for the always-dark AI chat surface:

```tsx
import { ORB, LK } from '@signallq/design-system';
// ORB.bg = #0D0D1A, ORB.surface = #1A0B2E, ORB.card = #1E1130, ORB.text = #F3F4F6
```

## Typography

No CSS classes — compose font shorthand inline:

```tsx
style={{ font: `600 18px/1.3 ${LK.font}` }}   // headline-small
style={{ font: `400 14px/1.5 ${LK.font}` }}   // body-medium
style={{ font: `600 11px/1.3 ${LK.font}`, letterSpacing: '.4px', textTransform: 'uppercase' }}  // overline
```

Use the `Overline` component for section labels. Typography sizes (12 estilos MD3, Fluxo de Telas
2026-07-13 — display-large/display-medium/headline-medium foram removidos, maior estilo real é
displaySmall/34): display-small 34, headline large/small 26/22, title 20/16/14, body 16/14/12,
label 14/12/11.

## Idiomatic example

```tsx
import { Card, Overline, Badge, Icon, LK, hexA } from '@signallq/design-system';

<Card style={{ display: 'flex', alignItems: 'center', gap: 12 }}>
  <div style={{
    width: 44, height: 44, borderRadius: '50%',
    background: hexA(LK.success, 0.1),
    display: 'flex', alignItems: 'center', justifyContent: 'center',
  }}>
    <Icon name="wifi" size={22} color={LK.success} />
  </div>
  <div style={{ flex: 1 }}>
    <div style={{ font: `600 15px/1.3 ${LK.font}`, color: LK.textPrimary }}>Luiz-5G</div>
    <div style={{ font: `400 11px/1.3 ${LK.font}`, color: LK.textSecondary }}>RSSI −27 dBm · Canal 36</div>
  </div>
  <Badge color={LK.success}>Forte</Badge>
</Card>
```

## Non-negotiables

> **Atualizado em 2026-07-18** — a paleta abaixo estava presa na era Linka (`#6C2BFF`, sem
> secondary fixo, 15 estilos de tipo, Roboto-only) mesmo após a migração MD3 de 2026-07-13.
> Corrigido para bater com `.claude/skills/SignallQ-design/colors_and_type.css` (fonte de
> verdade real) — ver `.claude/CLAUDE.md`, seção "Design System".

- Primary (accent): `#5B21D6`. Secondary: azul FIXO `#2851B8` — não deriva mais do primary.
- Status colors carry meaning: green `#146C2E` = good, amber `#8A5000` = moderate, red `#BA1A1A` = critical.
- Icons: Material Symbols Outlined only, 24dp default.
- Fonte única do app: Google Sans Flex (fallback Google Sans, Roboto) — não mais Roboto-only.
- Copy: Brazilian Portuguese, sentence case, UPPERCASE overlines, no emoji.
- Raw metric always with human verdict: "486 Mbps · Excelente".
- Separator: middle dot `·`.
- Grid: 8dp base, 8 degraus (4/8/12/16/20/24/32/40px).
- Radius por componente: Card 16 / SheetFrame 28 / Button 20 / Field 12 / Chip-Badge 999 / Dialog 24.
- Cards: flat, 16dp radius, hairline border, no drop shadows (elevação tonal, sem sombra dura).
- Superfície SignallQ (IA, tokens `ORB`) é DESCONTINUADA no To-Be — não implementar rota/componente novo.

# SignallqDesignSystem (@signallq/design-system@0.1.0)

This design system is the published @signallq/design-system React library, bundled as a single
browser global. All 25 components are the real upstream code.

## Where things are

- `_ds_bundle.js` — the whole-DS bundle at the project root; loads every component to `window.SignallqDesignSystem`. First line is a `/* @ds-bundle: … */` metadata header.
- `styles.css` — the single stylesheet entry: it `@import`s the tokens, fonts, and component styles (`_ds_bundle.css`). Link this one file.
- `components/<group>/<Name>/<Name>.prompt.md` (example JSX + variants), `<Name>.d.ts` (types), `<Name>.html` (variant grid).
- `tokens/*.css` — CSS custom properties, names verbatim from upstream.
- `fonts/` — `@font-face` files + `fonts.css` (when the package ships fonts).

For a specific component, `read_file("components/<group>/<Name>/<Name>.prompt.md")`.

## Loading

Add these two lines to your page once (React must be on the page first):

```html
<link rel="stylesheet" href="styles.css">
<script src="_ds_bundle.js"></script>
```

Components are then available at `window.SignallqDesignSystem.*`. Mount into a dedicated child node (e.g. `<div id="ds-root">`), not the host page's own React root, so the two trees don't collide:

```jsx
const { Avatar } = window.SignallqDesignSystem;
ReactDOM.createRoot(document.getElementById('ds-root')).render(<Avatar />);
```

## Tokens

41 CSS custom properties from @signallq/design-system. Names are
preserved verbatim from upstream. They are declared inside `_ds_bundle.css` (this DS ships one compiled stylesheet rather than separate token files).

- **color** (10): `--bg-primary`, `--bg-secondary`, `--bg-card`, …
- **spacing** (8): `--space-xs`, `--space-sm`, `--space-md`, …
- **typography** (1): `--font-sans`
- **radius** (6): `--radius-card`, `--radius-button`, `--radius-field`, …
- **other** (16): `--accent`, `--on-accent`, `--accent-blue`, …

## Components

### primitives
- `Avatar` — Circular gradient avatar (accent  blue). Used in the top bar leading slot.
- `Badge` — Inline pill chip with semantic color tint. Used for status labels (Conectado, verdicts).
- `Icon` — Material Symbols Outlined icon. Requires the Material Symbols font loaded from Google Fonts.
- `SignalBars` — 4-bar signal-strength glyph matching the Android SignallQ custom icon.

### layout
- `BottomNav` — 5-tab bottom navigation bar with accent highlight and pill indicator.
- `Card` — Surface card: white background, 1px border, 16dp radius, flat (no shadow).
- `Overline` — Section label: 11px semibold, tertiary color, UPPERCASE with letter-spacing.
- `PhoneFrame` — 390820 device frame for previewing screens in the design system viewer.
- `ScreenScroll` — Scrollable screen body with standard padding and vertical gap between sections.
- `SheetFrame` — Bottom-sheet chrome: superfcie baixa, cantos superiores 28dp, ala (grab handle) e contedo rolvel.
- `StatusBar` — Android-style status bar with clock, Wi-Fi, 5G, and battery indicators.
- `TopBar` — CenterAligned top app bar with leading avatar, centered title+icon, and optional action.

### controls
- `Button` — Boto MD3: filled / tonal / outlined / text / danger. Altura 40, radius 20.
- `Checkbox` — Checkbox MD3.
- `Chip` — Chip (filtro/seleo) MD3, pill 999.
- `Dialog` — Dilogo MD3 (radius 24) com scrim, sobre um container position: relative.
- `IconButton` — Boto s de cone, circular 40dp.
- `SegmentedControl` — Seletor segmentado MD3 (2-3 opes), pill com opo ativa em secondaryContainer.
- `Switch` — Toggle MD3.
- `Tabs` — Tabs com sublinhado (nav topo dentro de uma tela, ex.: Wi-Fi / Canal / Mvel).
- `TextField` — Campo de texto MD3: label overline + input com borda 1px, radius 12.

### brand
- `Logo` — Marca SignallQ  smbolo e lockup oficiais. Fundao de marca do design system (fonte: brand/).

### general
- `SignallQThemeProvider` — Provedor de tema do design system  resolve LK (claro) ou LK_DARK (escuro) e disponibiliza via useTokens().

### animations
- `Thinking` — Three-dot pulsing animation shown while SignallQ AI is processing.
- `TypeOut` — Character-by-character typewriter animation used in SignallQ AI responses.
