---
name: SignallQ-design
description: Use this skill to generate well-branded interfaces and assets for SignallQ, a Brazilian-Portuguese Android internet-diagnostics app, either for production or throwaway prototypes/mocks/etc. Contains essential design guidelines (Material Design 3 estrito), colors, type, fonts, assets, and UI kit components for prototyping.
user-invocable: true
---

Read the `README.md` file within this skill, and explore the other available files.

Key files:
- `README.md` — product context, content & visual foundations, iconography, full index.
- `colors_and_type.css` — all design tokens (CSS vars, `--md-sys-color-*` / `--md-sys-shape-*` /
  `--md-sys-elevation-*` / `--md-sys-state-*` / `--md-sys-motion-*`) + semantic type classes.
  Import this in every artifact.
- `preview/` — design-system reference cards (colors, type, forma, elevação/state layers, spacing, componentes).
- `assets/` — the SignallQ brand mark.
- `ui_kits/android/` — high-fidelity React recreation of the app + reusable components (`chrome.jsx` carries the `LK` tokens and shared primitives). Ainda referencia os nomes de token antigos (aliases deprecados em `colors_and_type.css`) — não foi migrado nesta passagem.

If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view. If working on production code, copy assets and read the rules here to become an expert in designing with this brand.

If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

Non-negotiables to stay on-brand (MD3, Fluxo de Telas To-Be — correção de 2026-07-13):
- Material Design 3: `primary=#5B21D6` (chave violeta), `secondary=#2851B8` (azul FIXO, não é mais derivado da tríade HCT do primary), tokens `--md-sys-color-{role}` / `on-{role}` / `{role}-container` / `on-{role}-container`; traffic-light status semantics (green/amber/red); no decorative gradients except the profile avatar & AI header.
- Tipografia: fonte única do app — Google Sans Flex (fallback Google Sans → Roboto → system-ui), pesos 400/500/600/700, em TODOS os estilos (não mais split display/body). Implementado em PR #939 — arquivo TTF embutido no APK, licença SIL OFL — escala de **12 estilos** (displaySmall até labelSmall; sem display-large/medium/headline-medium, que não aparecem em nenhuma tela do Fluxo de Telas).
- Forma por componente: **Card 16px**, **SheetFrame 28px** (cantos superiores), **Button 40px altura / 20px radius**, **Field 12px**, **Chip/Badge 999px (pill)**, **Dialog 24px**. Elevação tonal (5 níveis) e state layers (hover 8% / focus 10% / pressed 12% / dragged 16%) mantidos da migração anterior — o Fluxo de Telas não redefine esses sistemas.
- Brazilian Portuguese, "você", sentence-case titles, UPPERCASE overlines, raw metric + human verdict word, **no emoji** — decisão de produto, não muda com o MD3.
- Material Symbols (Outlined, variable font FILL/wght/GRAD/opsz) icons only, 24px padrão (FILL 1 só no ícone ativo da Bottom Nav). 8dp spacing grid (8 degraus: xs 4 · sm 8 · md 12 · base 16 · lg 20 · xl 24 · xxl 32 · xxxl 40). SignallQ (AI) surface é DESCONTINUADA — não implementar em tela nova.
