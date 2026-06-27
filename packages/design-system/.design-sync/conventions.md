# SignallQ Design System — Conventions

## Setup

No provider or context wrapper required. Components are self-contained with inline styles.

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
LK.accent      // #6C2BFF — primary CTA, selection, active nav
LK.success     // #22C55E — good connection, tests OK
LK.warning     // #F5A623 — moderate alerts
LK.error       // #FF4D4F — critical failures
LK.accentBlue  // #2563EB — informational badges

// Alpha tints (color at 10–30% opacity)
hexA(LK.success, 0.12)   // e.g. card fill
hexA(LK.success, 0.3)    // e.g. card border
```

**Token files:** `tokens/` in the bundle, `styles.css`, `_ds_bundle.css`. Read `styles.css` for the full token list.

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

Use the `Overline` component for section labels. Typography sizes: display 34, headline 24/20/18, title 16/15/14, body 16/14/12, label 14/12/11.

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

- Single accent: `#6C2BFF`. No secondary accents.
- Status colors carry meaning: green = good, amber = moderate, red = critical.
- Icons: Material Symbols Outlined only, 24dp default.
- Copy: Brazilian Portuguese, sentence case, UPPERCASE overlines, no emoji.
- Raw metric always with human verdict: "486 Mbps · Excelente".
- Separator: middle dot `·`.
- Grid: 8dp base (4/8/12/16/24/32px).
- Cards: flat, 16dp radius, hairline border, no drop shadows.
