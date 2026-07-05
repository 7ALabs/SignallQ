# Design Tokens — Android

**Escopo:** SignallQ v0.16.0 | Android
**Última atualização:** 2026-07-04
**Fonte Android:** `SignallQTheme.kt` (era `LinkaTheme.kt`, renomeado em v0.15.0)

---

## Cores — Brand & Status

| Conceito | Valor |
| --- | --- |
| Brand primário | `#6C2BFF` (accent) |
| Brand secundário | `#2563EB` (accentBlue) |
| Sucesso | `#22C55E` |
| Aviso | `#F5A623` |
| Erro | `#FF4D4F` |

### Superfícies

| Token | Claro | Escuro |
| --- | --- | --- |
| Bg primário | `#FFFFFF` | `#000000` |
| Bg secundário | `#F3F4F6` | `#1A1A1A` |
| Card | `#FFFFFF` | `#111111` |
| Texto primário | `#0D0D1A` | `#F3F4F6` |
| Texto secundário | `#6B7280` | `#9CA3AF` |
| Borda | `#E5E7EB` | `#2A2A2A` |

### SignallQ (Sempre Escuro)

| Token | Valor |
| --- | --- |
| Bg | `#0D0D1A` |
| Surface | `#1A0B2E` |
| Card | `#1E1130` |
| Texto | `#F3F4F6` |
| Accent | `#6C2BFF` |

---

## SpeedTest — Phase Colors

| Fase | Valor |
| --- | --- |
| Latência/Resposta | `#60A5FA` |
| Download | `#34D399` |
| Upload | `#FBBF24` |

---

## Tipografia

### Família de Fonte

MD3 padrão (`androidx.compose.material3`).

### Escala

Material Design 3 — 14+ sp para acessibilidade.

| Estilo | Valor |
| --- | --- |
| Display | 34 sp |
| Headline | 24 sp |
| Body | 14–16 sp |

---

## Espaçamento

Grid 8 dp (Material Design 3):

```
xs: 4dp, sm: 8dp, md: 12dp, lg: 16dp, xl: 24dp, xxl: 32dp
```

---

## Raios de Borda

| Contexto | Valor |
| --- | --- |
| Card | 16 dp |
| Button | 12 dp |
| Input | 12 dp |

---

## Animação & Transição

Sem transições explícitas documentadas. Favor usar `animateAsState()` ou `transition()` conforme necessário.
