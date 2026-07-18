---
name: SignallQ Admin
description: Console técnico do SignallQ — uso, diagnósticos, IA/custos, erros e saúde do sistema em um só painel.
status: realinhado ao protótipo To-Be MD3 (fonte de verdade) em 2026-07-17 — pasta confirmada
  `signallq-admin-fluxo-tobe-md3` no projeto Claude Design "SignallQ Design System"
  (e77ea465-291f-4bf5-930c-a267680da04e), NÃO o mirror local desatualizado
  `signallq-admin-md3-tobe` (sem "fluxo") usado na passagem anterior de 2026-07-16 — ver
  docs_ai/design-system/PLANO_APLICACAO_TOBE_CONSOLE_2026-07-17.md (correção) e
  docs_ai/design-system/FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md (tokens de cor, ainda válidos)
colors:
  primary-light: "#6C2BFF"
  primary-dark: "#CFBCFF"
  on-primary-light: "#FFFFFF"
  on-primary-dark: "#38008C"
  secondary-light: "#1A73E8"
  secondary-dark: "#8AB4F8"
  success-light: "#1E8E3E"
  success-dark: "#7DDB93"
  attention-light: "#8A5300"
  attention-dark: "#FFB955"
  error-light: "#BA1A1A"
  error-dark: "#FFB4AB"
  error-container-light: "#FFDAD6"
  error-container-dark: "#93000A"
  on-error-container-light: "#410002"
  on-error-container-dark: "#FFDAD6"
  bg-base-light: "#FEF7FF"
  bg-base-dark: "#141019"
  bg-sidebar-light: "#F7F2FA"
  bg-sidebar-dark: "#1D1A22"
  bg-surface-light: "#ECE6F0"
  bg-surface-dark: "#2B2831"
  bg-surface-variant-light: "#FFFFFF"
  bg-surface-variant-dark: "#211E27"
  border-light: "#CAC4D0"
  border-dark: "#49454F"
  text-primary-light: "#1D1B20"
  text-primary-dark: "#E6E0E9"
  text-secondary-light: "#49454F"
  text-secondary-dark: "#CAC4D0"
  text-tertiary-light: "#79747E"
  text-tertiary-dark: "#938F99"
  nav-active-bg-light: "#E8DEF8"
  nav-active-bg-dark: "#4A4458"
  nav-active-on-light: "#1E192B"
  nav-active-on-dark: "#E8DEF8"
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
  card: "12px"
  button: "full"
  input: "12px"
  pill: "999px"
  nav-item-drawer: "23px"
  nav-item-rail: "16px"
  nav-item-bottomnav: "16px"
  segmented-chip: "20px"
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
    textColor: "{colors.on-primary}"
    rounded: "{rounded.button}"
    padding: "8px 14px"
  button-secondary:
    backgroundColor: "{colors.bg-surface}"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.button}"
    padding: "8px 14px"
  card-section:
    backgroundColor: "{colors.bg-surface}"
    rounded: "{rounded.card}"
    padding: "24px"
  card-metric:
    backgroundColor: "{colors.bg-surface}"
    rounded: "{rounded.card}"
    padding: "20px"
  badge-status:
    rounded: "{rounded.pill}"
    padding: "2px 10px"
  nav-drawer:
    width: "300px"
    backgroundColor: "{colors.bg-sidebar}"
  nav-rail:
    width: "88px"
    backgroundColor: "{colors.bg-sidebar}"
  nav-bottom:
    height: "80px"
    backgroundColor: "{colors.bg-sidebar}"
---

# Design System: SignallQ Admin

## 0. Fonte de verdade (realinhamento 2026-07-16)

Este documento descreve o protótipo Claude Design **`signallq-admin-md3-tobe`**
(`Md3Screen00Login`, `Md3Screen01Overview`, `Md3DashboardContent`/`Md3DashboardContentMobile`,
`Md3NavDrawer`, `Md3NavRail`, `Md3BottomNav`) — fonte de verdade de cor, tokens e componentes de
navegação do Console, por instrução direta do Luiz em 2026-07-16 (ver
`docs_ai/design-system/DECISAO_CORES_CONSOLE_PROTOTIPO_MD3_TOBE_2026-07-16.md`, seção "Correção
2026-07-16", que revoga a decisão anterior do mesmo dia).

A implementação real hoje (`SignallQ Admin/src/index.css`, `AppLayout.tsx`, `Sidebar.tsx`,
`Topbar.tsx`) ainda usa a paleta preta/flat antiga descrita na versão anterior deste arquivo — essa
divergência **é bug a corrigir**, não "protótipo desatualizado". Os valores pixel exatos já foram
fechados por auditoria (`docs_ai/design-system/FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md`) e a
migração de código (`index.css` + componentes de navegação) é tarefa de implementação separada,
ainda não aberta como issue no momento deste realinhamento de doc — abrir antes de iniciar essa
migração.

## 1. Overview

**Creative North Star: "The Operator's Console"** (preservado — não muda com o realinhamento)

O Admin do SignallQ é a sala de operação técnica do produto — modelo Google Play Console e
Cloudflare Dashboard, não SaaS de métrica bonita. A superfície segue a paleta tonal Material 3
baseline roxa do protótipo `md3-tobe`: claro sobre `#FEF7FF`, escuro sobre `#141019` — não mais o
preto/branco quase puro da versão anterior deste sistema. O acento de ação é o violeta `#6C2BFF`
(claro) / `#CFBCFF` (escuro, tone 80) — o mesmo eixo de cor do app SignallQ Android, mantido
deliberadamente como ponte de identidade. Cor de status (verde/âmbar/vermelho/azul) carrega
significado real de saúde do sistema, nunca decoração.

O sistema rejeita explicitamente o dashboard SaaS genérico — cards decorativos, gradiente
"hero-metric", ilustração — e qualquer tom "fofo" de app consumidor. Aqui a confiança vem da
sobriedade tonal: hierarquia tipográfica rígida, superfícies com fill tonal (`bg-surface`/
`bg-sidebar`) + borda 1px, e alerta tratado como informação objetiva, nunca como alarme visual.

**Key Characteristics:**
- Paleta tonal M3 baseline roxa por tema — claro (`#FEF7FF`/`#ECE6F0`) e escuro (`#141019`/
  `#2B2831`), ambos com a mesma sobriedade — nenhum dos dois é "mais casual"
- Acento de ação (`primary`) muda de tom por tema — `#6C2BFF` claro, `#CFBCFF` escuro (tone 80) —
  não é mais um único hex fixo nos dois temas
- Semântica de status (verde/âmbar/vermelho/azul) recalibrada por tema para manter contraste AA
- Cards com fill tonal (`bg-surface`) + borda 1px, sem sombra decorativa
- Tipografia mono (Roboto Mono) reservada a metadados técnicos (fonte de dado, timestamps) — sem
  divergência confirmada contra o protótipo até o momento desta auditoria

## 2. Colors

Paleta tonal Material 3 baseline roxa (claro sobre `#FEF7FF`, escuro sobre `#141019`); o acento
`primary` satura em CTA e nav ativa, e a semântica de status faz o trabalho pesado de significado —
com valores próprios por tema para preservar contraste AA.

### Primary
- **Signal Violet claro** (`#6C2BFF`) / **Signal Violet escuro** (`#CFBCFF`, tone 80): CTA
  primário, nav ativa, foco de input. `on-primary` é `#FFFFFF` no claro e `#38008C` no escuro —
  texto do botão "Entrar" sempre em contraste AA sobre o tom do tema.

### Secondary
- **Rede & Operadora** (`#1A73E8` claro / `#8AB4F8` escuro): dot/série ligada à categoria "Rede &
  Operadora" no dashboard.

### Tertiary
- **Phase Latency** (`#60A5FA`), **Phase Download** (`#34D399`), **Phase Upload** (`#FBBF24`):
  cores dedicadas às três fases do SpeedTest, usadas só em gráficos de performance de rede — não
  auditadas nesta passagem do `md3-tobe`, mantidas sem alteração até confirmação em contrário.
- **Provider Cloudflare** (`#7C3AED`), **Provider OpenAI** (`#2563EB`), **Provider Anthropic**
  (`#E040FB`), **Provider Local** (`#71717A`): série de cor fixa por provedor de IA nos gráficos de
  custo (`src/config/designTokens.ts`) — idem, fora do escopo desta auditoria.

### Neutral
- **Bg Base** (`#FEF7FF` claro / `#141019` escuro): fundo de página.
- **Bg Sidebar** (`#F7F2FA` claro / `#1D1A22` escuro): superfície de navegação (drawer/rail/bottom
  nav) — antes mais escura que o base no dark, agora mais clara (tom neutro M3, não "quase preto").
- **Bg Surface** (`#ECE6F0` claro / `#2B2831` escuro): cards e superfícies de conteúdo.
- **Bg Surface Variant** (`#FFFFFF` claro / `#211E27` escuro, com borda `#CAC4D0`/`#49454F`): fundo
  contrastante de chip/menu (ex.: chip segmentado PROD/STG do TopAppBar, input do Login) — token
  novo, não existia na versão anterior deste documento.
- **Text Primary** (`#1D1B20` claro / `#E6E0E9` escuro): título, valor de métrica.
- **Text Secondary** (`#49454F` claro / `#CAC4D0` escuro): descrição, corpo.
- **Text Tertiary** (`#79747E` claro / `#938F99` escuro): labels, timestamps, fonte de dado.
- **Border** (`#CAC4D0` claro / `#49454F` escuro): toda separação de superfície — nunca sombra.

### Status
- **Success** (`#1E8E3E` claro / `#7DDB93` escuro): saudável, estável, OK.
- **Attention** (`#8A5300` claro / `#FFB955` escuro): atenção, beta, cache. Valor corrigido — a
  versão anterior deste documento (`#B06000`/`#F59E0B`) era herdada do design system Android/AsIs,
  não do `md3-tobe`.
- **Error** (`#BA1A1A` claro / `#FFB4AB` escuro): crítico, falha, pausado.
- **Error Container** (`#FFDAD6` claro / `#93000A` escuro, com `on-error-container` `#410002` /
  `#FFDAD6`): badge de contagem em item de nav (ex.: "Problemas & Incidentes") — token novo.
- **Info**: sem token dedicado confirmado no `md3-tobe` até o momento desta auditoria — usar
  `secondary` para série neutro-informativa até nova confirmação.

### Navegação (nav ativa)
- **Pill de item ativo** (bg/on): `#E8DEF8`/`#1E192B` claro, `#4A4458`/`#E8DEF8` escuro — drawer,
  rail e bottom nav compartilham o mesmo par de cor para o estado ativo.

### Named Rules
**The One Accent Rule.** O acento `primary` aparece só em CTA, nav ativa e foco — nunca como fundo
geral. Se uma tela pede mais destaque visual, o destaque é semântico (status), nunca de marca.

**The Per-Theme Semantic Rule.** Success/Attention/Error/Info e o próprio `primary` têm valores
diferentes em dark e light — nunca reutilizar o hex de um tema no outro; cada par foi calibrado
para contraste AA sobre a superfície daquele tema.

## 3. Typography

**Display Font:** Roboto (com fallback `ui-sans-serif, system-ui, sans-serif`) — sem divergência
confirmada contra o `md3-tobe` (auditoria de Fase 1 não encontrou indício de fonte diferente, mas
não fechou tamanho pixel exato de cada nível; reconfirmar ao tocar em componente de tipografia).
**Mono Font:** Roboto Mono — reservado a metadados técnicos (fonte do dado, timestamps, IDs).

**Character:** Uma família sans carrega toda a hierarquia funcional; o mono aparece só como marcador
técnico discreto, nunca como corpo de texto. Números grandes com tracking negativo (`-0.03em`) são o
tratamento assinatura de métrica — mesma lógica do app Android, aplicada ao contexto denso do
console.

### Hierarchy
- **Display** (700, 24–28px, tracking `-0.03em`): valor hero de métrica (MetricCard).
- **Headline** (600, 14px, tracking `-0.01em`): título de card/seção (SectionCard).
- **Body** (400, 13px, 1.5): descrição de seção, texto corrido.
- **Label** (600, 11px, tracking `0.08em`, UPPERCASE): label de métrica, label de filtro.
- **Mono** (400, 9–11px): fonte de dado, timestamp — sempre `text-tertiary`.

### Named Rules
**The Verdict-Beside-Metric Rule.** KPI de qualidade (crash rate, retenção, latência) nunca aparece
sem o veredito humano (Excelente/Bom/Regular/Fraco/Forte) ao lado — herdado do app Android; KPI de
volume puro pode dispensar veredito quando a tendência já comunica direção. Preservado no
realinhamento — comum aos dois sistemas.

## 4. Elevation

Sistema tonal por padrão: profundidade vem de superfície tonal (`bg-surface` sobre `bg-base`,
`bg-sidebar` para navegação) e borda 1px — consistente com a estratégia de elevação tonal do
Material 3. Esta auditoria não encontrou especificação de sombra própria do `md3-tobe` (o
protótipo não expõe `box-shadow` custom nos componentes lidos); a estratégia de foco funcional e a
regra flat-by-default da versão anterior deste documento são preservadas até confirmação em
contrário.

### Shadow Vocabulary
- **focus-ring** (`box-shadow: 0 0 0 2px` na cor do acento a 40% de alpha): único uso de sombra
  real, em input/select com foco ativo.

### Named Rules
**The Flat-By-Default Rule.** Cards não têm sombra decorativa. Se algo precisa se destacar, primeiro
tente fill tonal + borda 1px; sombra é reservada exclusivamente a estado de foco funcional.

## 5. Components

### Buttons
- **Shape:** radius full/pill — botão "Entrar" do Login usa `height:54px` + `border-radius:27px`
  (radius = altura/2), não um valor fixo tipo 12/16/20px. Corrigido da versão anterior (`12px`
  fixo).
- **Primary:** fill `{colors.primary}`, texto `{colors.on-primary}`, padding `8px 14px`.
- **Secondary:** fill `bg-surface`, borda 1px `border`, texto `text-secondary`.
- **Segmented chip** (ex.: PROD/STG, período no TopAppBar): radius fixo `20px` (não pill) —
  container com múltiplas abas, aba ativa em `nav-active-bg`/`nav-active-on`, aba inativa sem
  fundo. Componente novo nesta versão, sem equivalente na implementação anterior.
- **Hover / Focus:** transição de cor via `transition-all`; foco funcional usa o anel de 2px na cor
  do acento.

### Badges / Status
- **Style:** fully-rounded (`rounded-full`), fill da cor semântica a 10% de alpha, borda a 20% de
  alpha, texto na cor sólida, dot indicador à esquerda (pulsa quando "ok"/"stable"/"success").
- **Badge de contagem** (ex.: item de nav "Problemas & Incidentes"): círculo com fill
  `error-container` / texto `on-error-container` — token novo, ver seção Colors.
- **Semântica:** verde=saudável, âmbar=atenção, vermelho=crítico, cinza=obsoleto/neutro — nunca cor
  sozinha, sempre dot + texto + label.

### Cards / Containers
- **Corner Style:** `12px` — corrigido da versão anterior (`16px`).
- **Background:** `bg-surface` sobre `bg-base`.
- **Shadow Strategy:** nenhuma estática — ver Elevation.
- **Border:** `1px solid var(--border)` sempre.
- **Internal Padding:** 24px em SectionCard (header + body), 20px em MetricCard — sem divergência
  confirmada.

### Inputs / Fields (Filtros)
- **Style:** radius `12px` — sem divergência (confirmado em `Md3LoginForm`, dark e light), borda
  1px `border`, fill `bg-sidebar`/`bg-surface`, label uppercase 10px tracking largo à esquerda do
  valor.
- **Focus:** anel de 2px na cor do acento a 40% de alpha (única sombra real do sistema).

### Navigation

O `md3-tobe` define três padrões de navegação por breakpoint, contra um único drawer/sidebar fixo
da versão anterior deste documento:

- **Nav Drawer** (desktop): largura `300px` — corrigido da versão anterior (`264px`). Fundo
  `bg-sidebar`. Item ativo em pill `height:46px`/`radius:23px` (full/pill). Rodapé com avatar
  circular, nome do operador + "Squad técnico", botão de tema circular 30px — preservado da versão
  anterior.
- **Nav Rail** (tablet, ícone-only): largura `88px` exatos. Logo/avatar de projeto no topo (32px,
  sem menu de troca de projeto). Lista vertical só de ícone, sem label de texto sob o ícone
  (diferente do rail M3 canônico com label). Divisor fino (1px) entre grupos, sem header de texto
  de grupo. Avatar de conta (36px) fixo embaixo com popover lateral (`left:76px`). Item ativo: pill
  `56px × 32px`, `radius:16px`, ícone 22px; badge de contagem (quando houver) é círculo `16px`
  sobreposto no canto superior direito. **Breakpoint exato não definido no protótipo** — decisão de
  produto pendente com Claudete/Camilo. Componente novo, não existia na versão anterior.
- **Bottom Nav** (mobile): altura `80px`, `padding:12px 8px`, `justify-content:space-around`. 5
  itens fixos (Início/App/Diagnóstico/Redes/Mais — não os mesmos 10 itens do drawer/rail, é versão
  condensada com item catch-all "Mais"). Item ativo: pill `64px × 32px`, `radius:16px`, ícone 22px +
  label 12px abaixo do ícone. **Substitui o drawer no mobile, não coexiste com ele** — a
  implementação atual de drawer off-canvas em mobile precisa ser trocada por bottom nav, não
  receber bottom nav como complemento. Componente novo, não existia na versão anterior.
- **Topbar:** altura fixa 56px, fundo `bg-topbar` — sem divergência confirmada nesta auditoria.

### Filter Chip / Select (componente assinatura)
Select nativo estilizado como pill discreta: fundo `bg-sidebar`/`bg-surface`, borda 1px, label
uppercase tertiary à esquerda, chevron à direita, anel de foco na cor do acento — usado em
GlobalFilters, compartilhado por todas as telas do painel. Sem divergência confirmada contra o
`md3-tobe`.

## 6. Do's and Don'ts

### Do:
- **Do** manter o acento `primary` restrito a CTA, nav ativa e foco — igual ao app Android.
- **Do** usar o valor de `primary` correto por tema (`#6C2BFF` claro / `#CFBCFF` escuro) — não
  reutilizar o hex de um tema no outro.
- **Do** recalibrar success/attention/error por tema (dark ≠ light) para manter contraste AA sobre
  `bg-surface`.
- **Do** parear todo KPI de qualidade com veredito humano (Excelente/Bom/Regular/Fraco/Forte).
- **Do** manter cards com fill tonal + borda 1px, sem sombra decorativa.
- **Do** tratar alerta (erro/quota/limite) como informação objetiva — ícone + cor + label, nunca
  drama visual.
- **Do** usar radius `12px` em card/input e radius full/pill em botão — não os valores antigos
  (`16px`/`12px` fixo).

### Don't:
- **Don't** parecer um dashboard SaaS genérico: sem cards decorativos, sem gradiente
  "hero-metric", sem ilustração.
- **Don't** usar tom "fofo" ou empolgado de app consumidor — isso é papel do app SignallQ, não do
  Admin.
- **Don't** adicionar sombra decorativa em cards; a única sombra real do sistema é o anel de foco
  funcional.
- **Don't** reutilizar o hex de status ou de `primary` de um tema no outro — cada par foi calibrado
  para AA.
- **Don't** usar emoji como substituto de ícone ou cor semântica — Lucide/Material são o único
  sistema de ícone.
- **Don't** tratar a divergência entre `index.css` atual e este documento como "protótipo
  desatualizado" — é bug de implementação a corrigir (ver seção 0).
