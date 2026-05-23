# Design Tokens Cross-Platform — Android ↔ PWA

**Escopo:** Linka v0.6.3 | Android + PWA v1.0.0

---

## Cores — Brand & Status

### Paleta Compartilhada

| Conceito | Android | PWA | Divergência | Motivo |
| --- | --- | --- | --- | --- |
| Brand primário | `#6C2BFF` (accent) | `#6C2BFF` (dark) / `#5B3FE8` (light) | Sim | PWA adapta ao tema; Android fixo |
| Brand secundário | `#2563EB` (accentBlue) | `#2563EB` (ambos) | Não | Alinhado |
| Sucesso | `#22C55E` | `#34D399` (dark) / `#16A34A` (light) | Sim | PWA usa matiz diferente no light |
| Aviso | `#F5A623` | `#FBBF24` (dark) / `#D97706` (light) | Sim | Matiz diferente por tema |
| Erro | `#FF4D4F` | `#F87171` (dark) / `#DC2626` (light) | Sim | Matiz diferente por tema |

### Superfícies

#### Android

| Token | Claro | Escuro |
| --- | --- | --- |
| Bg primário | `#FFFFFF` | `#000000` |
| Bg secundário | `#F3F4F6` | `#1A1A1A` |
| Card | `#FFFFFF` | `#111111` |
| Texto primário | `#0D0D1A` | `#F3F4F6` |
| Texto secundário | `#6B7280` | `#9CA3AF` |
| Borda | `#E5E7EB` | `#2A2A2A` |

#### PWA

| Token | Claro | Escuro |
| --- | --- | --- |
| Bg | `#FAFAF7` | `#000000` |
| Surface | `#FFFFFF` | `#12121A` |
| Surface-deep | `#FBFBFD` | `#050508` |
| Texto | `#0F0F14` | `#FAFAF7` |
| Borda | `#E8E6E0` | `#2A2A35` |

**Divergência:** PWA usa gradients radiais em `body`; Android usa cores sólidas.

### Orbit (Sempre Escuro)

Paleta de IA é idêntica em ambas plataformas:

| Token | Android | PWA | Alinhado |
| --- | --- | --- | --- |
| Bg | `#0D0D1A` | `#0D0D1A` | ✓ |
| Surface | `#1A0B2E` | `#1A0B2E` | ✓ |
| Card | `#1E1130` | `#1E1130` | ✓ |
| Texto | `#F3F4F6` | `#F3F4F6` | ✓ |
| Accent | `#6C2BFF` | `#6C2BFF` | ✓ |

---

## SpeedTest — Phase Colors

| Fase | Android | PWA (dark) | PWA (light) | Alinhado |
| --- | --- | --- | --- | --- |
| Latência/Resposta | `#60A5FA` | `#60A5FA` | `#2563EB` | Parcial |
| Download | `#34D399` | `#34D399` | `#16A34A` | Parcial |
| Upload | `#FBBF24` | `#FBBF24` | `#D97706` | Parcial |

**Nota:** PWA altera cores por tema claro/escuro; Android mantém constante.

---

## Tipografia

### Famílias de Fonte

| Uso | Android | PWA | Alinhado |
| --- | --- | --- | --- |
| Display/Body | MD3 padrão | Geist | Sim (semantic) |
| Mono | (não explícito) | JetBrains Mono | PWA-only |
| Editorial | (não explícito) | Instrument Serif | PWA-only |

### Escala

**Android:** Material Design 3 — 14+ sp para acessibilidade  
**PWA:** Tailwind escala customizada — responsiva a viewport

| Estilo | Android | PWA | Alinhado |
| --- | --- | --- | --- |
| Display | 34 sp | 28–32 px | Similares |
| Headline | 24 sp | 20–24 px | Similares |
| Body | 14–16 sp | 14–16 px | ✓ |

---

## Espaçamento

### Android

Grid 8 dp (Material Design 3):

```
xs: 4dp, sm: 8dp, md: 12dp, lg: 16dp, xl: 24dp, xxl: 32dp
```

### PWA

Grid 4 px (Tailwind):

```
xs: 4px, sm: 8px, md: 12px, lg: 16px, xl: 24px, xxl: 32px, 3xl: 48px
```

**Alinhado:** Multiplicadores compatíveis (4 px ≈ 1 dp × 4).

---

## Raios de Borda

| Contexto | Android | PWA |
| --- | --- | --- |
| Card | 16 dp | 16 px |
| Button | 12 dp | 12 px |
| Input | 12 dp | 12 px |
| Pill | N/A | 999 px |

**Alinhado:** Valores diretos coincidem.

---

## Animação & Transição

### Android

Sem transições explícitas documentadas. Favor usar `animateAsState()` ou `transition()` conforme necessário.

### PWA

```css
--t-fast:  180ms cubic-bezier(0.32, 0.72, 0, 1)
--t-med:   280ms cubic-bezier(0.32, 0.72, 0, 1)
--t-slow:  480ms cubic-bezier(0.32, 0.72, 0, 1)
```

**Nota:** Android não exposita curvas easing globais — adaptar por caso de uso.

---

## Resumo de Divergências Intencionais

1. **Tema claro/escuro:** PWA adapta cores por tema; Android mantém algumas cores fixas (especialmente Orbit).
2. **Superfícies:** PWA usa gradientes radiais; Android usa sólidas.
3. **Tipografia:** PWA inclui Instrument Serif (editorial); Android usa MD3 padrão.
4. **Animação:** PWA documenta timings e curves; Android deixa a critério do dev.

---

## Recomendações para Sincronização

- Quando adicionar nova cor a uma plataforma, atualizar ambas.
- Orbit deve permanecer idêntica — é a marca da IA.
- Phase colors (speedtest) devem manter saturação semelhante mesmo em temas diferentes.
- Documentar mudanças de tema neste arquivo imediatamente.
