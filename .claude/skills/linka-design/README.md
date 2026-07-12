# SignallQ — Design System

> **SignallQ** (app store name "SignallQ app", package `io.signallq.app`) is a native **Android** smart internet-diagnostics app, built in **Kotlin / Jetpack Compose / Material Design 3**. It analyzes a home connection in real time — speed, latency, Wi-Fi signal & channels, DNS, fiber (GPON) modem, 4G/5G mobile signal — and uses AI (Cloudflare Worker) to explain *why* the internet is slow, unstable, or down, in clear, non-technical **Brazilian Portuguese**.
>
> The brand is **SignallQ**, with two named sub-systems: **SignallQ** (the conversational AI assistant) and **SignallQ Pulse** (passive background monitoring).

---

## Sources

This design system was reverse-engineered from a single attached, read-only codebase. The reader is not assumed to have access; paths are recorded in case they do.

| Source | Path | What it gave us |
|---|---|---|
| Android codebase | `SignallQ/android/` (monorepo root) | Source of truth for all tokens, components & screens |
| Theme tokens | `SignallQ/android/app/src/main/kotlin/io/veloo/app/kotlin/ui/SignallQTheme.kt` | `LkColors`, `LkSpacing`, `LkRadius`, `signallQTypography` |
| Design-system docs | `SignallQ/docs_ai/design-system/*.md` | COLORS, TYPOGRAPHY, SPACING, DESIGN_TOKENS, COMPONENTS_ANDROID, MD3_GUIDELINES — canonical, kept in sync with the code |
| Functional spec | `SignallQ/docs_ai/ANDROID_FUNCIONAL.md` | Screen-by-screen behaviour, flows, copy |
| Mockup v2 spec | `SignallQ/.claude/design-specs/mockup-v2-ui-screens.md` | Pixel specs for Home / Sinal / SpeedTest / Resultado |
| Screen composables | `SignallQ/android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/*.kt` | Layout, exact labels, metric thresholds |
| Components | `…/ui/component/*.kt` | SignallQ, SpeedTest, Pulse, layout primitives |
| Launcher icon | `…/res/mipmap-xxxhdpi/ic_launcher*.png` (copied to `assets/`) | App icon / wordmark "SignallQ" |

**AI worker (referenced, external):** `integrations/cloudflare/ai-diagnosis-worker/` (Cloudflare Worker; provider primário Gemini 2.0 Flash quando configurado, fallback Qwen3 30B MoE FP8).

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

- **Color vibe.** Mostly **white & light-grey**; one saturated brand color, **violet `#6C2BFF`**, used sparingly for the active nav tab, primary CTAs, selection, and the speedtest button. A secondary **blue `#2563EB`** appears in the profile-avatar gradient and "Móvel" data. Status colors are the workhorses for meaning. There is a full **dark theme** (`#000` bg, `#111` cards, `#1A1A1A` secondary) and a permanently-dark **SignallQ** palette (`#0D0D1A` / `#1A0B2E` / `#1E1130`) for the AI surfaces only.
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

## MONETIZATION — NATIVE AD COMPONENTS

`…/ui/component/ads/` (issue #555, v0.23.0) — official pattern for AdMob native ads inside the
app's organic surfaces, currently anchored on 4 screens: **Velocidade** (`NativeAdRow`, idle
state, below "Último resultado"), **Resultado** (`NativeAdCard`, dismissible), **Dispositivos**
(`NativeAdListRow`, inside the connected-devices list itself — never in the Infraestrutura
section), **Histórico** (`NativeAdCard`, dismissible).

- **Three variants, chosen by context, not preference:** full dismissible card / compact row /
  row inside an existing list. Never invent a fourth without design review.
- **Mandatory disclosure (`AdBadge`), never hidden:** "Patrocinado" (neutral tone, `Campaign`
  icon) for `NativeAdSource.ADMOB` — the only source actually in use; "Parceiro" (`accentBlue`,
  `Storefront` icon) for `NativeAdSource.PARTNER` — component already supports it for a future
  curated affiliate/partner catalog (`coreRecommendation`), not live yet.
- **Never confusable with organic content:** dashed border (`Modifier.dashedBorder`, never
  solid like organic cards), CTA in violet **outline** (never solid — solid violet is reserved
  for primary organic CTAs like "Iniciar teste"/"Conversar com IA"), no photo/hero, advertiser
  icon in a **square chip** (never circular, unlike organic avatars/icon chips).
- **No placeholder state.** The whole composable returns early and renders nothing when there's
  no loaded creative (`nativeAd == null`) — surrounding layout recomposes without a gap, never a
  loading box or empty card.
- Full component list and per-screen anchoring: `docs_ai/design-system/COMPONENTS_ANDROID.md`
  ("Monetização — Anúncio Nativo").

---

## ICONOGRAPHY

- **System:** **Material Symbols / Material Icons** (Compose `androidx.compose.material.icons`), **Outlined** style predominantly (with a few Filled/Rounded for back-arrow, Wi-Fi, CellTower). Single-color, ~24dp, tinted by token (`accent`, `success`, `warning`, `error`, `textSecondary`/`textTertiary`). This is the *only* icon system in the app.
- **Representative icons in use:** `Home, Speed/Adjust, Wifi, History, Settings` (nav); `Router, DeviceHub, Hub, Public/Language, Smartphone, Laptop, CellTower, SettingsInputAntenna, SignalCellularAlt, WifiOff, Lock/LockOpen, Refresh, Share, ArrowBack, ArrowForwardIos, ExpandMore, Tv, Videocam, SportsEsports, GpsFixed, Shield, Info, Warning, LocationOn, GridView`.
- **In this design system:** we link **Material Symbols (Outlined)** from the Google Fonts CDN — it is the exact icon family the app uses, so no substitution is needed. Use `<span class="material-symbols-outlined">wifi</span>`.
- **Emoji:** never used as iconography.
- **Unicode glyphs:** the middle dot `·` as a separator and a check `✓` inside the connected badge are the only non-icon glyphs; everything else is a Material icon.
- **Logo / brand mark (OFICIAL):** os arquivos oficiais estão em `brand/` na raiz do repo e replicados aqui em `assets/signallq-*`. O símbolo é um conjunto de **4 barras de sinal** (alturas curta · média · **alta** · média — a 3ª é a mais alta) em degradê **violeta `#6C2BFF` → azul**, com cantos arredondados. O **wordmark** é "SignallQ" com "Signall" em quase-preto (`#0D0D1A`) ou branco no escuro, e o **"Q" em violeta**. Use `signallq-lockup-light-bg.png` em fundo claro, `signallq-lockup-dark-bg.png` em fundo escuro, e `signallq-symbol-*.png` para ícone/avatar/espaço quadrado. **Nunca redesenhar em CSS/SVG à mão nem usar a marca anterior "linka".** Regras completas em `brand/README.md`. As marcas animadas in-app **SignallQ** (`SignallQSymbol`) e **SignallQ Pulse** (`SignallQPulseSymbol`) mudam de cor por estado (accent / success / warning / error).

---

## Index — what's in this folder

| File / folder | Contents |
|---|---|
| `README.md` | This document — context, content & visual foundations, iconography, index |
| `colors_and_type.css` | All design tokens as CSS vars + semantic type classes. **Import this in every artifact.** |
| `SKILL.md` | Agent-Skill manifest (for use in Claude Code) |
| `assets/` | Logos **oficiais**: `signallq-symbol-1024.png`, `signallq-lockup-light-bg.png`, `signallq-lockup-dark-bg.png`. (Fonte da verdade: `brand/` na raiz do repo.) Os `ic_launcher*` antigos são a marca "linka" descontinuada — não usar. |
| `_ref/` | Reference screenshots from the real app (not for shipping) |
| `preview/` | Design-system cards shown in the Design System tab (colors, type, spacing, components) |
| `ui_kits/android/` | High-fidelity Jetpack-Compose-faithful recreation of the app — `index.html` (interactive prototype) + JSX components |

### UI kits

- **`ui_kits/android/`** — the SignallQ Android app. Interactive click-through across the 5 core surfaces: **Início (Home)**, **Velocidade (SpeedTest → running → Resultado)**, **Sinal (Wi-Fi)**, **Histórico**, plus **Diagnóstico/SignallQ AI**. Built from the real composables and mockup v2 spec.
