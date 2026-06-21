---
name: linka-design
description: Use this skill to generate well-branded interfaces and assets for Linka (a.k.a. "Pulse — Speedtest Inteligente"), a Brazilian-Portuguese Android internet-diagnostics app, either for production or throwaway prototypes/mocks/etc. Contains essential design guidelines, colors, type, fonts, assets, and UI kit components for prototyping.
user-invocable: true
---

Read the `README.md` file within this skill, and explore the other available files.

Key files:
- `README.md` — product context, content & visual foundations, iconography, full index.
- `colors_and_type.css` — all design tokens (CSS vars) + semantic type classes. Import this in every artifact.
- `preview/` — design-system reference cards (colors, type, spacing, components).
- `assets/` — the linka brand mark.
- `ui_kits/android/` — high-fidelity React recreation of the app + reusable components (`chrome.jsx` carries the `LK` tokens and shared primitives).

If creating visual artifacts (slides, mocks, throwaway prototypes, etc), copy assets out and create static HTML files for the user to view. If working on production code, copy assets and read the rules here to become an expert in designing with this brand.

If the user invokes this skill without any other guidance, ask them what they want to build or design, ask some questions, and act as an expert designer who outputs HTML artifacts _or_ production code, depending on the need.

Non-negotiables to stay on-brand:
- Clean, bright Material Design 3 surfaces; one electric-violet accent (`#6C2BFF`); traffic-light status semantics (green/amber/red); no decorative gradients except the profile avatar & AI header.
- Brazilian Portuguese, "você", sentence-case titles, UPPERCASE overlines, raw metric + human verdict word, **no emoji**.
- Material Symbols (Outlined) icons only. Roboto type. 8dp spacing grid. 16dp card radius, flat (no heavy shadows). Orbit (AI) surfaces are always dark.
