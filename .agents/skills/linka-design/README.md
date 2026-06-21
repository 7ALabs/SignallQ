# Linka — Design System

> **Linka** (app store name "linka app", package `io.linka.app.kotlin`) is a native **Android** smart internet-diagnostics app, built in **Kotlin / Jetpack Compose / Material Design 3**. It analyzes a home connection in real time — speed, latency, Wi-Fi signal & channels, DNS, fiber (GPON) modem, 4G/5G mobile signal — and uses on-device AI to explain *why* the internet is slow, unstable, or down, in clear, non-technical **Brazilian Portuguese**. All processing happens on-device; no personal data leaves the phone.
>
> The product is sometimes referred to commercially as **"Pulse — Speedtest Inteligente"**; inside the codebase the brand is **Linka**, with two named sub-systems: **Orbit** (the conversational AI assistant) and **LinkaPulse** (passive background monitoring).

---

## Sources

This design system was reverse-engineered from a single attached, read-only codebase. The reader is not assumed to have access; paths are recorded in case they do.

| Source | Path | What it gave us |
|---|---|---|
| Android codebase | `Linka Android/` (mounted, read-only) | Source of truth for all tokens, components & screens |
| Theme tokens | `Linka Android/app/src/main/kotlin/io/linka/app/kotlin/ui/LinkaTheme.kt` | `LkColors`, `LkSpacing`, `LkRadius`, `linkaTypography` |
| Design-system docs | `Linka Android/docs_ai/design-system/*.md` | COLORS, TYPOGRAPHY, SPACING, COMPONENTS_ANDROID, MD3_GUIDELINES |
| Functional spec | `Linka Android/docs_ai/ANDROID_FUNCIONAL.md` | Screen-by-screen behaviour, flows, copy |
| Mockup v2 spec | `Linka Android/.claude/design-specs/mockup-v2-ui-screens.md` | Pixel specs for Home / Sinal / SpeedTest / Resultado |
| Screen composables | `Linka Android/app/src/main/kotlin/io/linka/app/kotlin/ui/screen/*.kt` | Layout, exact labels, metric thresholds |
| Components | `…/ui/component/*.kt` (25 custom composables) | Orbit, SpeedTest, Pulse, layout primitives |
| Screenshots | `Linka Android/linka_*.png` (copied to `_ref/`) | Visual ground-truth: SpeedTest, Sinal, Fibra, store listing |
| Launcher icon | `…/res/mipmap-xxxhdpi/ic_launcher*.png` (copied to `assets/`) | App icon / wordmark "linka" |

**AI worker (referenced, external):** `https://linka-ai-diagnosis-worker.giammattey-luiz.workers.dev` (Cloudflare Worker, Gemma model). Contact in store listing: `giammattey.luiz@gmail.com`.

---

## CONTENT FUNDAMENTALS

The product talks to a **non-technical Brazilian** whose internet "is acting up." The voice is **warm, plain-spoken, reassuring, and concrete** — never a network engineer lecturing.

- **Language:** Brazilian Portuguese, always. Accents and `ç` intact (`Conexão`, `Histórico`, `Oscilação`).
- **Person / address:** Speaks **to** the user as **"você"**, and frames things from *their* world: **"Sua internet por fibra"**, **"Sua conexão"**, **"Voce esta usando a internet do chip"**. The app itself is the helpful narrator ("precisamos da localização", "Conectando ao modem…").
- **Casing:**
  - Screen titles & button labels are **sentence case**: `Resultado do teste`, `Medir velocidade`, `Iniciar`, `Cancelar teste`.
  - **Overlines / section labels are UPPERCASE** with light letter-spacing: `SUA CONEXÃO`, `OUTRAS REDES`, `EXPERIÊNCIA DE USO`, `ÚLTIMO RESULTADO`, `CHIPS ATIVOS`.
- **Tone — jargon, then translation.** Raw metrics are always present (`RSSI −27 dBm · Canal 36 · 433 Mbps`, `RSRP`, `jitter`, `bufferbloat`) **but paired with a human verdict word**: `Excelente`, `Bom`, `Regular`, `Fraco`, `Forte`. Diagnoses lead with a feeling-level headline (`Conexão excelente`) then a one-line plain explanation (`Sua internet está…`).
- **Humanized diagnostics, not error codes.** Status reads as `OK / INFO / ATENÇÃO / CRÍTICO`; usage verdicts are everyday tasks — **Streaming, Gaming, Vídeo Chamada** rated *bom / aceitável / ruim*. Empathy in empty states: *"O Wi-Fi está desligado ou desconectado…"*.
- **Actionable & rights-aware.** Copy pushes a next step (`Testar assim que voltar`, `Permitir leitura do chip`, `Abrir Wi-Fi nas configurações`) and even cites consumer law: *"Abaixo de 40%: você tem direito a solicitar rescisão sem multa (ANATEL Ato 7869/2022)."*
- **Separators:** the middle dot `·` joins inline facts (`WI-FI · 5 GHZ`, `5GHz · Excelente`, `Banda: 5GHz · RSSI −27 dBm`).
- **Emoji:** **none.** Meaning is carried by Material icons + semantic color, never emoji. A check glyph `✓` appears inside the "Conectado" badge — that is the extent of glyph decoration.
- **Numbers:** metric values are big and bold; units (`Mbps`, `ms`, `dBm`, `%`) are small and secondary, baseline-aligned to the value.

---

## VISUAL FOUNDATIONS

A **clean, bright, neutral Material Design 3** surface where a single **electric violet** does all the highlighting and **traffic-light semantics** (green / amber / red) carry connection quality. Nothing decorative competes with the data.

- **Color vibe.** Mostly **white & light-grey**; one saturated brand color, **violet `#6C2BFF`**, used sparingly for the active nav tab, primary CTAs, selection, and the speedtest button. A secondary **blue `#2563EB`** appears in the profile-avatar gradient and "Móvel" data. Status colors are the workhorses for meaning. There is a full **dark theme** (`#000` bg, `#111` cards, `#1A1A1A` secondary) and a permanently-dark **Orbit** palette (`#0D0D1A` / `#1A0B2E` / `#1E1130`) for the AI surfaces only.
- **Backgrounds.** Flat solids — **no photographic imagery, no full-bleed hero images, no repeating patterns/textures, no decorative gradients** on surfaces. Gradients exist in exactly two places: the **profile avatar** (linear accent→accentBlue) and the **Diagnóstico/AI header** (same linear gradient). The speedtest "Iniciar" button is a solid violet disc with a soft same-color glow ring.
- **Type.** System **Roboto** (Android default), Material 3 scale. Bold/SemiBold for headings & metric values, Regular for body. Big numeral + small unit is the signature metric treatment. Two custom animated text components: **TypewriterText** (AI replies type in char-by-char) and **RotatingMessageText** (looping hints, fade in/out).
- **Spacing.** Strict **8dp grid** (`4/8/12/16/24/32`). 16dp is the standard screen padding and card inner padding. 56dp minimum touch target.
- **Cards.** Rounded **16dp** corners, fill color `--bg-card`, separated by a hairline `1px` `--border` and/or sitting on the `--bg-secondary` page tone — **flat, no drop shadows** in the M3 sense (elevation is conveyed by tonal surface, not shadow). Tinted "status" cards use the semantic color at low alpha for fill and ~25–30% for border: connected Wi-Fi card = `success @12%` fill; selected/AI accents = `accent @8–12%` fill, `accent @25–30%` border. Banners (offline/warning) = `warning @12%` fill, warning text + icon.
- **Buttons / pills.**
  - Primary: solid `--accent`, white text, **12dp** radius.
  - Segmented selector (Rápido/Completo/Triplo): pill track on `--bg-secondary`, **active segment is a white pill with subtle elevation** — intentionally *neutral*, not accent-colored.
  - Filter chips (Todos / 2.4 / 5 / 6 GHz): fully-rounded pills; selected = `accent @ light tint` fill + accent text; idle = `--bg-secondary`.
- **Borders.** `1px` solid `--border` for dividers, list separators, outline buttons, idle chips. Selected states swap to accent at 25–40% alpha.
- **Radii.** card 16 · button/input 12 · pills/chips 999 · circular icon chips & avatars fully round (commonly 36–44dp).
- **Elevation / shadow.** Minimal. The active-segment pill and FAB-like speedtest button carry the only soft shadows; cards rely on tonal contrast + border. **No heavy drop shadows.**
- **Transparency & blur.** Used as **color-at-alpha tints** (the `1A`=10%, `1F`=12%, `26`=15%, `33`=20%, `40`=25% hex-suffix convention from the codebase), not as glassmorphism. No backdrop blur.
- **Iconography in circles.** Recurring motif: a Material icon centered in a **circular chip** filled with its semantic color at ~10% — 44dp on signal cards, 36dp on friendly cards, 80dp in empty states.
- **Signal bars.** Custom 4-bar vertical glyph (heights `6/9/12/16dp`, width `3dp`, radius `1dp`); filled bars take the quality color (green=Forte, amber=Regular, red=Fraco), empty bars use `--border`.
- **Animation / motion.** Restrained & functional. Live speedtest gauge fills as it measures; phase pills check off with **haptics** between phases; AI "thinking" bubble pulses dots; border-glow effect on highlight cards (`AppBorderGlowEffect`). Scroll-aware bottom nav slides off-screen on scroll-down, returns on scroll-up, and hides entirely during a running test. Transitions are quick fades/offsets — **no bounces, no flourish**. Easing is standard M3 (LinearEasing for the looping/progress animations).
- **Hover / press.** This is a touch app: feedback is the M3 ripple + state-layer tint (accent/onSurface at low alpha) and, for the gauge/CTA, scale/haptic. No web-style hover.
- **Layout rules.** Fixed elements: a **CenterAlignedTopAppBar** (centered title, `ProfileAvatarButton` at left, contextual action at right) and a **5-tab bottom NavigationBar** (Início · Velocidade · Sinal · Histórico · Ajustes). Content scrolls in a `LazyColumn` between them. Secondary screens overlay the tabs rather than being separate tabs; deep flows get a back arrow in the nav-icon slot. Bottom sheets (`ModalBottomSheet`) are the standard pattern for permissions, topology analysis, and pickers.

---

## ICONOGRAPHY

- **System:** **Material Symbols / Material Icons** (Compose `androidx.compose.material.icons`), **Outlined** style predominantly (with a few Filled/Rounded for back-arrow, Wi-Fi, CellTower). Single-color, ~24dp, tinted by token (`accent`, `success`, `warning`, `error`, `textSecondary`/`textTertiary`). This is the *only* icon system in the app.
- **Representative icons in use:** `Home, Speed/Adjust, Wifi, History, Settings` (nav); `Router, DeviceHub, Hub, Public/Language, Smartphone, Laptop, CellTower, SettingsInputAntenna, SignalCellularAlt, WifiOff, Lock/LockOpen, Refresh, Share, ArrowBack, ArrowForwardIos, ExpandMore, Tv, Videocam, SportsEsports, GpsFixed, Shield, Info, Warning, LocationOn, GridView`.
- **In this design system:** we link **Material Symbols (Outlined)** from the Google Fonts CDN — it is the exact icon family the app uses, so no substitution is needed. Use `<span class="material-symbols-outlined">wifi</span>`.
- **Emoji:** never used as iconography.
- **Unicode glyphs:** the middle dot `·` as a separator and a check `✓` inside the connected badge are the only non-icon glyphs; everything else is a Material icon.
- **Logo / brand mark:** the **"linka" wordmark** — soft, rounded, lowercase letterforms in brand violet on a light circular field (see `assets/ic_launcher.png`, `assets/ic_launcher_foreground.png`). There is no separate horizontal logotype in the codebase; the launcher mark is the brand mark. The **Orbit** AI mark (`OrbitSymbol`) and **LinkaPulse** mark (`LinkaPulseSymbol`) are animated in-app glyphs that change color by state (accent / success / warning / error).

---

## Index — what's in this folder

| File / folder | Contents |
|---|---|
| `README.md` | This document — context, content & visual foundations, iconography, index |
| `colors_and_type.css` | All design tokens as CSS vars + semantic type classes. **Import this in every artifact.** |
| `SKILL.md` | Agent-Skill manifest (for use in Claude Code) |
| `assets/` | `ic_launcher.png`, `ic_launcher_foreground.png` (brand mark) |
| `_ref/` | Reference screenshots from the real app (not for shipping) |
| `preview/` | Design-system cards shown in the Design System tab (colors, type, spacing, components) |
| `ui_kits/android/` | High-fidelity Jetpack-Compose-faithful recreation of the app — `index.html` (interactive prototype) + JSX components |

### UI kits

- **`ui_kits/android/`** — the Linka Android app. Interactive click-through across the 5 core surfaces: **Início (Home)**, **Velocidade (SpeedTest → running → Resultado)**, **Sinal (Wi-Fi)**, **Histórico**, plus **Diagnóstico/Orbit AI**. Built from the real composables and mockup v2 spec.
