---
name: SignallQ Admin
description: Console técnico do SignallQ — uso, diagnósticos, IA/custos, erros e saúde do sistema em um só painel.
colors:
  primary: "#6C2BFF"
  accent-blue: "#2563EB"
  success-dark: "#34D399"
  success-light: "#1E8E3E"
  attention-dark: "#F59E0B"
  attention-light: "#B06000"
  error-dark: "#FF4D4F"
  error-light: "#D93025"
  info-dark: "#60A5FA"
  info-light: "#1A73E8"
  bg-base-dark: "#000000"
  bg-base-light: "#F8F8F8"
  bg-surface-dark: "#0B0B0B"
  bg-surface-light: "#FFFFFF"
  bg-sidebar-dark: "#050505"
  bg-sidebar-light: "#F7F7F7"
  border-dark: "#262626"
  border-light: "#DCDCDC"
  text-primary-dark: "#F5F5F5"
  text-primary-light: "#111111"
  text-secondary-dark: "#A3A3A3"
  text-secondary-light: "#666666"
  text-tertiary-dark: "#737373"
  text-tertiary-light: "#8A8A8A"
  phase-latency: "#60A5FA"
  phase-download: "#34D399"
  phase-upload: "#FBBF24"
  provider-cloudflare: "#7C3AED"
  provider-openai: "#2563EB"
  provider-anthropic: "#E040FB"
  provider-local: "#71717A"
typography:
  display:
    fontFamily: "Roboto, ui-sans-serif, system-ui, sans-serif"
    fontSize: "28px"
    fontWeight: 700
    lineHeight: 1.2
    letterSpacing: "-0.03em"
  headline:
    fontFamily: "Roboto, ui-sans-serif, system-ui, sans-serif"
    fontSize: "14px"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "-0.01em"
  body:
    fontFamily: "Roboto, ui-sans-serif, system-ui, sans-serif"
    fontSize: "13px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Roboto, ui-sans-serif, system-ui, sans-serif"
    fontSize: "11px"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "0.08em"
  mono:
    fontFamily: "Roboto Mono, ui-monospace, SFMono-Regular, monospace"
    fontSize: "9px"
    fontWeight: 400
    lineHeight: 1.3
rounded:
  card: "16px"
  button: "12px"
  input: "12px"
  pill: "999px"
spacing:
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
components:
  button-primary:
    backgroundColor: "{colors.primary}"
    textColor: "#FFFFFF"
    rounded: "{rounded.button}"
    padding: "8px 14px"
  button-secondary:
    backgroundColor: "{colors.bg-surface-dark}"
    textColor: "{colors.text-secondary-dark}"
    rounded: "{rounded.button}"
    padding: "8px 14px"
  card-section:
    backgroundColor: "{colors.bg-surface-dark}"
    rounded: "{rounded.card}"
    padding: "24px"
  card-metric:
    backgroundColor: "{colors.bg-surface-dark}"
    rounded: "{rounded.card}"
    padding: "20px"
  badge-status:
    rounded: "{rounded.pill}"
    padding: "2px 10px"
---

# Design System: SignallQ Admin

## 1. Overview

**Creative North Star: "The Operator's Console"**

O Admin do SignallQ é a sala de operação técnica do produto — modelo Google Play Console e Cloudflare Dashboard, não SaaS de métrica bonita. A superfície é escura por padrão (preto quase puro, `#000000`/`#0B0B0B`), com um tema claro equivalente disponível, e ambos mantêm a mesma postura: dado em primeiro plano, decoração em zero. O único acento saturado é o violeta `#6C2BFF` — o mesmo do app SignallQ Android, herdado deliberadamente para reforçar que é o mesmo produto visto de dentro. Cor de status (verde/âmbar/vermelho/azul) carrega significado real de saúde do sistema, nunca decoração.

O sistema rejeita explicitamente o dashboard SaaS genérico — cards decorativos, gradiente "hero-metric", ilustração — e qualquer tom "fofo" de app consumidor. Aqui a confiança vem da sobriedade: hierarquia tipográfica rígida, superfícies flat com borda 1px, e alerta tratado como informação objetiva (como a Cloudflare sinaliza limite de worker), nunca como alarme visual.

**Key Characteristics:**
- Base escura (`#000000`/`#0B0B0B`) por padrão, tema claro (`#F8F8F8`/`#FFFFFF`) com a mesma sobriedade — nenhum dos dois é "mais casual"
- Um único acento saturado (violeta `#6C2BFF`), reservado para ação e identidade
- Semântica de status (verde/âmbar/vermelho/azul) recalibrada por tema para manter contraste AA
- Cards flat: tonal fill (`bg-surface`) + borda 1px, sem sombra
- Tipografia mono (Roboto Mono) reservada a metadados técnicos (fonte de dado, timestamps)

## 2. Colors

Paleta majoritariamente preto/cinza-escuro (ou branco/cinza-claro no tema claro); o violeta satura só em ação, e a semântica de status faz o trabalho pesado de significado — com valores próprios por tema para preservar contraste AA.

### Primary
- **Signal Violet** (`#6C2BFF`): CTA primário, nav ativa, foco de input, glow decorativo discreto em metric cards. Mesma cor do app SignallQ Android — ponte de identidade entre app e console.

### Secondary
- **Provider Blue** (`#2563EB`): badge/série de gráfico ligada a provedor OpenAI e a métricas "azuis" do app.

### Tertiary
- **Phase Latency** (`#60A5FA`), **Phase Download** (`#34D399`), **Phase Upload** (`#FBBF24`): cores dedicadas às três fases do SpeedTest, usadas só em gráficos de performance de rede.
- **Provider Cloudflare** (`#7C3AED`), **Provider Anthropic** (`#E040FB`), **Provider Local** (`#71717A`): série de cor fixa por provedor de IA nos gráficos de custo (`src/config/designTokens.ts`).

### Neutral
- **Bg Base** (`#000000` escuro / `#F8F8F8` claro): fundo de página.
- **Bg Sidebar** (`#050505` escuro / `#F7F7F7` claro): navegação lateral, ligeiramente distinta da base.
- **Bg Surface** (`#0B0B0B` escuro / `#FFFFFF` claro): cards e superfícies de conteúdo.
- **Text Primary** (`#F5F5F5` escuro / `#111111` claro): título, valor de métrica.
- **Text Secondary** (`#A3A3A3` escuro / `#666666` claro): descrição, corpo.
- **Text Tertiary** (`#737373` escuro / `#8A8A8A` claro): labels, timestamps, fonte de dado.
- **Border** (`#262626` escuro / `#DCDCDC` claro): toda separação de superfície — nunca sombra.

### Status
- **Success** (`#34D399` escuro / `#1E8E3E` claro): saudável, estável, OK.
- **Attention** (`#F59E0B` escuro / `#B06000` claro): atenção, beta, cache.
- **Error** (`#FF4D4F` escuro / `#D93025` claro): crítico, falha, pausado.
- **Info** (`#60A5FA` escuro / `#1A73E8` claro): neutro-informativo.

### Named Rules
**The One Accent Rule.** Violeta (`#6C2BFF`) aparece só em CTA, nav ativa e foco — nunca como fundo geral. Se uma tela pede mais destaque visual, o destaque é semântico (status), nunca de marca.

**The Per-Theme Semantic Rule.** Success/Attention/Error/Info têm valores diferentes em dark e light (ex.: success `#34D399` no escuro vira `#1E8E3E` no claro) — nunca reutilizar o hex de um tema no outro; cada par foi calibrado para contraste AA sobre `--bg-surface` daquele tema (GH#552).

## 3. Typography

**Display Font:** Roboto (com fallback `ui-sans-serif, system-ui, sans-serif`), carregada via Google Fonts
**Mono Font:** Roboto Mono — reservado a metadados técnicos (fonte do dado, timestamps, IDs)

**Character:** Uma família sans carrega toda a hierarquia funcional; o mono aparece só como marcador técnico discreto, nunca como corpo de texto. Números grandes com tracking negativo (`-0.03em`) são o tratamento assinatura de métrica — mesma lógica do app Android, aplicada ao contexto denso do console.

### Hierarchy
- **Display** (700, 24–28px, tracking `-0.03em`): valor hero de métrica (MetricCard).
- **Headline** (600, 14px, tracking `-0.01em`): título de card/seção (SectionCard).
- **Body** (400, 13px, 1.5): descrição de seção, texto corrido.
- **Label** (600, 11px, tracking `0.08em`, UPPERCASE): label de métrica, label de filtro.
- **Mono** (400, 9–11px): fonte de dado, timestamp — sempre `text-tertiary`.

### Named Rules
**The Verdict-Beside-Metric Rule.** KPI de qualidade (crash rate, retenção, latência) nunca aparece sem o veredito humano (Excelente/Bom/Regular/Fraco/Forte) ao lado — herdado do app Android; KPI de volume puro pode dispensar veredito quando a tendência já comunica direção (GH#552).

## 4. Elevation

Sistema flat por padrão: profundidade vem de superfície tonal (`bg-surface` sobre `bg-base`) e borda 1px, nunca de sombra decorativa. A única sombra real do sistema é o anel de foco (`box-shadow: 0 0 0 2px` na cor do acento a 40% de alpha) em selects e inputs ativos — funcional, não decorativa. Glow radial em metric cards existe, mas é um gradiente de fundo sutil (6% de alpha), não uma sombra.

### Shadow Vocabulary
- **focus-ring** (`box-shadow: 0 0 0 2px rgba(108,43,255,0.4)`): único uso de sombra real, em input/select com foco ativo.
- **accent-glow** (radial-gradient a 6% alpha, blur 20px): acento decorativo discreto no canto de metric cards — não é sombra, é textura de fundo.

### Named Rules
**The Flat-By-Default Rule.** Cards não têm sombra. Se algo precisa se destacar, primeiro tente tonal fill + borda 1px; sombra é reservada exclusivamente a estado de foco funcional.

## 5. Components

### Buttons
- **Shape:** radius 12px (`rounded-xl`).
- **Primary:** fill `{colors.primary}`, texto branco, padding `8px 14px`, ícone de seta à direita (ArrowRight) — usado em ActionsRow como "próximo passo" nunca decorativo.
- **Secondary:** fill `bg-surface`, borda 1px `border`, texto `text-secondary`.
- **Hover / Focus:** transição de cor via `transition-all`; foco funcional usa o anel de 2px na cor do acento.

### Badges / Status
- **Style:** fully-rounded (`rounded-full`), fill da cor semântica a 10% de alpha, borda a 20% de alpha, texto na cor sólida, dot indicador à esquerda (pulsa quando "ok"/"stable"/"success").
- **Semântica:** verde=saudável, âmbar=atenção, vermelho=crítico, cinza=obsoleto/neutro — nunca cor sozinha, sempre dot + texto + label.

### Cards / Containers
- **Corner Style:** 16px (`rounded-[var(--radius-card)]`).
- **Background:** `bg-surface` sobre `bg-base`.
- **Shadow Strategy:** nenhuma estática — hover eleva o card (`translateY(-3px)` + sombra suave, 220ms), respeitando `prefers-reduced-motion`. Ver Elevation.
- **Border:** `1px solid var(--border)` sempre.
- **Internal Padding:** 24px em SectionCard (header + body), 20px em MetricCard.

### Inputs / Fields (Filtros)
- **Style:** radius 12px (`rounded-[var(--radius-input)]`), borda 1px `border`, fill `bg-sidebar`/`bg-surface`, label uppercase 10px tracking largo à esquerda do valor.
- **Focus:** anel de 2px na cor do acento a 40% de alpha (única sombra real do sistema).

### Navigation
- **Sidebar:** largura fixa 264px, fundo `bg-sidebar` (mais escuro/claro que `bg-surface`, conforme tema), item ativo em `bg-sidebar-active`. Rodapé com avatar circular (gradiente `primary`→`accent-blue`), nome do operador + "Squad técnico", botão de tema circular 30px.
- **Topbar:** altura fixa 56px, fundo `bg-topbar`.

### Filter Chip / Select (componente assinatura)
Select nativo estilizado como pill discreta: fundo `bg-sidebar`/`bg-surface`, borda 1px, label uppercase tertiary à esquerda, chevron à direita, anel de foco na cor do acento — usado em GlobalFilters, compartilhado por todas as telas do painel.

## 6. Do's and Don'ts

### Do:
- **Do** manter o violeta `#6C2BFF` restrito a CTA, nav ativa e foco — igual ao app Android.
- **Do** recalibrar success/attention/error/info por tema (dark ≠ light) para manter contraste AA sobre `bg-surface`.
- **Do** parear todo KPI de qualidade com veredito humano (Excelente/Bom/Regular/Fraco/Forte).
- **Do** manter cards flat — fill tonal + borda 1px, sem sombra.
- **Do** tratar alerta (erro/quota/limite) como informação objetiva — ícone + cor + label, nunca drama visual.

### Don't:
- **Don't** parecer um dashboard SaaS genérico: sem cards decorativos, sem gradiente "hero-metric", sem ilustração.
- **Don't** usar tom "fofo" ou empolgado de app consumidor — isso é papel do app SignallQ, não do Admin.
- **Don't** adicionar sombra decorativa em cards; a única sombra real do sistema é o anel de foco funcional.
- **Don't** reutilizar o hex de status de um tema no outro (ex.: usar o verde do dark no light) — cada par foi calibrado para AA.
- **Don't** usar emoji como substituto de ícone ou cor semântica — Lucide/Material são o único sistema de ícone.
