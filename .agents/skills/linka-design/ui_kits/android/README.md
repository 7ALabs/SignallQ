# SignallQ — Android UI Kit

High-fidelity, click-through recreation of the **SignallQ** Android app (Kotlin / Jetpack Compose / Material Design 3), rebuilt in React from the real composables, the design tokens in `SignallQTheme.kt`, and the `mockup-v2-ui-screens.md` spec. These are **cosmetic recreations** for prototyping — not production logic.

## Run it

Open `index.html`. It mounts a single 390×820 phone frame with the live app. Everything is interactive:

- **Bottom nav** — switch between the 5 root tabs: Início · Velocidade · Sinal · Histórico · Ajustes.
- **Velocidade** — tap the violet **Iniciar** disc → animated gauge runs the LATÊNCIA / DOWN / UP phases → **Resultado** screen with the metric grid + usage verdicts.
- **Resultado → "Conversar com a IA"** — opens **SignallQ**, the always-dark AI assistant, which "thinks", types a diagnosis, and offers tappable follow-up chips.
- **Sinal** — switch the Wi-Fi / Canal / Móvel tabs and the band filter chips.
- **Início → "Diagnóstico IA"** chip also opens SignallQ.

## Files

| File | Contents |
|---|---|
| `index.html` | Loads React + Babel + the font/icon families, mounts `App` |
| `chrome.jsx` | `LK` tokens, `Icon` (Material Symbols), `SignalBars`, `Badge`, `Avatar`, `StatusBar`, `TopBar`, `BottomNav`, `PhoneFrame` |
| `screens.jsx` | `HomeScreen`, `SinalScreen` (+ `ChannelTab`, `MovelTab`), `HistoricoScreen`, `AjustesScreen`, `Card`/`Overline` primitives |
| `speedtest.jsx` | `SpeedFlow` state machine → `SpeedIdle`, `SpeedRunning` (gauge), `Resultado` |
| `signallq.jsx` | `SignallQScreen` AI chat (dark palette `ORB`), `TypeOut` typewriter, `Thinking` dots |
| `app.jsx` | Tab router + SignallQ overlay, mounts to `#root` |

## Conventions

- All color/spacing/radius values come from the `LK` object in `chrome.jsx`, which mirrors `colors_and_type.css` (the project-root token file). Edit tokens there.
- Icons are **Material Symbols Outlined** (the app's real icon family) via `<Icon name="…" />`. Active nav/filled states pass `fill={1}`.
- Component scope is shared across the Babel files via `Object.assign(window, {…})` at the end of each file — keep that pattern when adding components.

## Coverage / omissions

Faithful to: the 5-tab scroll-aware shell, center-aligned top bar + gradient profile avatar, Home NetworkPath/Medições/MiniCards/SignalCard, the neutral ModeSelector + speedtest gauge + result metric grid + usage verdicts, Sinal "Sua conexão" tree with the success-tinted connected node + signal bars, SignallQ's dark gradient-header chat.

Intentionally simplified or omitted (present in the app, not rebuilt here): Fibra/GPON modem screen, DNS benchmark & Ping detail screens, Dispositivos (LAN scanner), onboarding, permission bottom-sheets, dual-SIM chips, PDF "Laudo" report, full Ajustes editing. Add them by following the same patterns if needed.
