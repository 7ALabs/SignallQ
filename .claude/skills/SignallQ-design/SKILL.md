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

Non-negotiables to stay on-brand (MD3 estrito, migração 2026-07-11):
- Material Design 3 estrito: paleta tonal HCT (Primary/Secondary/Tertiary derivadas de `#6C2BFF`), tokens `--md-sys-color-{role}` / `on-{role}` / `{role}-container` / `on-{role}-container`; traffic-light status semantics (green/amber/red, fora do escopo MD3, mantido igual); no decorative gradients except the profile avatar & AI header.
- Tipografia Google Sans Flex (display/headline/title) + Roboto (body/label), implementado em PR #939 — arquivo TTF embutido no APK, licença SIL OFL — escala completa de 15 estilos MD3.
- Elevação tonal (5 níveis, tint de superfície) em vez de sombra dura sozinha; forma em 7 tokens (`none/xs/sm/md/lg/xl/full`) — card agora **12dp (md)**, sheets/dialogs em **16dp (lg)**.
- State layers (hover 8% / focus 10% / pressed 12% / dragged 16%) em todo componente clicável. Motion: easing `cubic-bezier(.2,0,0,1)` (emphasized/standard), durações 100/200/300/400ms.
- Brazilian Portuguese, "você", sentence-case titles, UPPERCASE overlines, raw metric + human verdict word, **no emoji** — decisão de produto, não muda com o MD3.
- Material Symbols (Outlined) icons only, densidade por contexto (24dp padrão / 20dp TopBar / 18dp inline). 8dp spacing grid. SignallQ (AI) surfaces are always dark.
