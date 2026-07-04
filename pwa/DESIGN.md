---
name: SignallQ PWA
description: Diagnóstico de conectividade honesto e calmo, com um único acento violeta
colors:
  accent: "#6c2bff"
  accent-blue: "#2563eb"
  success: "#22c55e"
  warning: "#f5a623"
  error: "#ff4d4f"
  phase-latencia: "#60a5fa"
  phase-download: "#34d399"
  phase-upload: "#fbbf24"
  bg-primary: "#ffffff"
  bg-secondary: "#f3f4f6"
  bg-card: "#ffffff"
  text-primary: "#0d0d1a"
  text-secondary: "#6b7280"
  text-tertiary: "#9ca3af"
  border: "#e5e7eb"
  bg-primary-dark: "#050507"
  bg-secondary-dark: "#17171b"
  bg-card-dark: "#121215"
  text-primary-dark: "#f3f4f6"
  text-secondary-dark: "#9ca3af"
  text-tertiary-dark: "#6b7280"
  border-dark: "#27272d"
  amber-surface: "#fff8e6"
  amber-surface-dark: "#2e2000"
typography:
  display:
    fontFamily: "Roboto, system-ui, 'Segoe UI', sans-serif"
    fontSize: "34px"
    fontWeight: 700
    lineHeight: 1.15
  headline:
    fontFamily: "Roboto, system-ui, 'Segoe UI', sans-serif"
    fontSize: "24px"
    fontWeight: 600
    lineHeight: 1.25
  title:
    fontFamily: "Roboto, system-ui, 'Segoe UI', sans-serif"
    fontSize: "16px"
    fontWeight: 500
    lineHeight: 1.4
  body:
    fontFamily: "Roboto, system-ui, 'Segoe UI', sans-serif"
    fontSize: "16px"
    fontWeight: 400
    lineHeight: 1.5
  label:
    fontFamily: "Roboto, system-ui, 'Segoe UI', sans-serif"
    fontSize: "14px"
    fontWeight: 500
    lineHeight: 1.3
  overline:
    fontFamily: "Roboto, system-ui, 'Segoe UI', sans-serif"
    fontSize: "12px"
    fontWeight: 700
    lineHeight: 1.3
    letterSpacing: "0.3px"
rounded:
  sm: "8px"
  button: "12px"
  input: "12px"
  card: "16px"
  xl: "24px"
  pill: "999px"
spacing:
  xxs: "2px"
  xs: "4px"
  sm: "8px"
  md: "12px"
  lg: "16px"
  xl: "24px"
  xxl: "32px"
  section: "64px"
components:
  button-primary:
    backgroundColor: "{colors.accent}"
    textColor: "#ffffff"
    rounded: "{rounded.button}"
    padding: "0 16px"
  button-primary-hover:
    backgroundColor: "{colors.accent}"
  button-secondary:
    backgroundColor: "{colors.text-primary}"
    textColor: "{colors.bg-card}"
    rounded: "{rounded.button}"
    padding: "0 16px"
  button-tonal:
    backgroundColor: "rgba(108, 43, 255, 0.12)"
    textColor: "{colors.accent}"
    rounded: "{rounded.button}"
    padding: "0 16px"
  button-outline:
    backgroundColor: "transparent"
    textColor: "{colors.text-primary}"
    rounded: "{rounded.button}"
    padding: "0 16px"
  card-surface:
    backgroundColor: "{colors.bg-card}"
    rounded: "{rounded.card}"
  quality-badge-good:
    backgroundColor: "rgba(34, 197, 94, 0.12)"
    textColor: "{colors.success}"
    rounded: "{rounded.pill}"
    padding: "5px 11px"
  quality-badge-fair:
    backgroundColor: "rgba(245, 166, 35, 0.14)"
    textColor: "{colors.warning}"
    rounded: "{rounded.pill}"
    padding: "5px 11px"
  quality-badge-poor:
    backgroundColor: "rgba(255, 77, 79, 0.12)"
    textColor: "{colors.error}"
    rounded: "{rounded.pill}"
    padding: "5px 11px"
---

# Design System: SignallQ PWA

## 1. Overview

**Creative North Star: "O Painel de Controle Calmo"**

SignallQ PWA existe para uma única razão: dizer a verdade sobre a conexão de alguém sem assustar essa pessoa. O sistema visual reflete isso ao pé da letra — superfícies flat quase brancas (ou quase pretas no dark mode), um único acento violeta (`#6C2BFF`) usado com parcimônia, e um vocabulário de cor semântico (verde/âmbar/vermelho) que nunca aparece sozinho: todo número vem acompanhado de um veredito em palavras. Não há gradientes decorativos, não há glassmorphism, não há hero-metric genérico de SaaS. A superfície é limpa porque a mensagem já carrega peso emocional suficiente — o usuário chegou aqui frustrado com o Wi-Fi ou a chamada caindo.

O sistema rejeita explicitamente o clichê SaaS (cards de feature idênticos, eyebrows numerados em toda seção, cards aninhados) e a identidade visual do Google Fiber Speed Test, que serve só como referência de clareza (tela limpa, número grande, uma ação primária), nunca como fonte de cor, layout ou marca.

**Key Characteristics:**
- Um único acento (violeta `#6C2BFF`), nunca disputando espaço com outra cor de marca.
- Cards e superfícies flat: sem sombra pesada, contorno hairline em vez de elevação.
- Cor semântica sempre com significado — verde/âmbar/vermelho mapeiam 1:1 para bom/atenção/ruim, nunca decorativos.
- Dark mode é um modo real (toggle em Ajustes), não só um `prefers-color-scheme`, e preserva as mesmas cores de acento e status.
- Tipografia Roboto MD3, sentence case em títulos, overlines curtas em UPPERCASE — sem emoji em lugar nenhum.

## 2. Colors

Paleta restrita: neutros quase monocromáticos carregando o peso visual, um único acento violeta pontuando ação e destaque, e três cores de status que só aparecem quando há um veredito real para comunicar.

### Primary
- **Violeta SignallQ** (`#6C2BFF`): único acento de marca. Usado em CTAs primários, ícones de destaque, indicador de navegação ativo, anel de progresso do speed test, glow de fundo do topo da tela. Nunca disputa espaço com outra cor saturada.

### Secondary
- **Azul de contexto** (`#2563EB`): uso pontual e secundário (não é o acento principal); reservado para contextos que precisam se diferenciar do violeta de marca sem virar um segundo acento de destaque.

### Tertiary — Status semântico
- **Sucesso** (`#22C55E`): conexão boa, veredito positivo, chip "concluído" em fluxos de etapas.
- **Atenção** (`#F5A623`): conexão regular, aviso não-crítico, superfície `--amber-surface` (`#FFF8E6` claro / `#2E2000` escuro) para cards de limitação.
- **Erro** (`#FF4D4F`): conexão ruim, falha de medição, ação destrutiva.
- **Fases do speed test**: latência `#60A5FA`, download `#34D399`, upload `#FBBF24` — só aparecem durante o teste em andamento, nunca como paleta de marca.

### Neutral
- **Superfície primária** (`#FFFFFF` claro / `#050507` escuro): fundo de página.
- **Superfície secundária** (`#F3F4F6` claro / `#17171B` escuro): fundo de seção, header, linhas de tabela alternadas.
- **Superfície de card** (`#FFFFFF` claro / `#121215` escuro): fundo de card, sempre distinguível da página por borda, não por sombra.
- **Texto primário** (`#0D0D1A` claro / `#F3F4F6` escuro): títulos e corpo de destaque.
- **Texto secundário** (`#6B7280` claro / `#9CA3AF` escuro): corpo padrão, legendas.
- **Texto terciário** (`#9CA3AF` claro / `#6B7280` escuro): labels, placeholders, texto de apoio.
- **Borda** (`#E5E7EB` claro / `#27272D` escuro): único recurso de separação visual entre card e fundo — a superfície é flat, a borda hairline faz o trabalho que uma sombra faria em outro sistema.

### Named Rules
**The One Accent Rule.** Existe um único acento de marca (`#6C2BFF`). Se uma tela parece precisar de uma segunda cor de destaque, a resposta certa quase sempre é usar cor semântica de status, não inventar um segundo acento.

**The Verdict-Always Rule.** Nenhuma métrica técnica aparece sozinha. Todo número vem com uma leitura humana ao lado — badge de qualidade, veredito em texto, ou cor semântica — nunca um número cru largado na tela.

## 3. Typography

**Display/Body Font:** Roboto (fallback `system-ui, 'Segoe UI', sans-serif`), carregada via `<link>` — sem font-ligature para ícones (migrado para SVG inline por bug de renderização no WebKit/Safari, ver GitHub #365).

**Character:** Uma única família fazendo todo o trabalho, hierarquia por peso e tamanho MD3 — direta, sem personalidade decorativa, deixa o conteúdo (o veredito da conexão) ser o protagonista.

### Hierarchy
- **Display** (700, 34px, 1.15): reservado para o número principal do resultado (velocidade, latência) — o dado mais importante da tela.
- **Headline** (600, 24px/1.25 large · 20px/1.3 medium · 18px/1.3 small): títulos de tela e de seção.
- **Title** (500, 16px/1.4 large · 15px/1.4 medium · 14px/1.4 small): títulos de card e item de lista.
- **Body** (400, 16px/1.5 large · 14px/1.5 medium · 12px/1.45 small; medium/small em `--text-secondary`): texto corrido, cap de leitura confortável em telas de resultado e explicação.
- **Label** (500, 14px/1.3 large · 400 12px/1.3 medium · 11px/1.3 small, medium/small em `--text-tertiary`): rótulos de campo, legendas de unidade, texto auxiliar.
- **Overline** (700, 12px/1.3, letter-spacing 0.3px, UPPERCASE, `--text-secondary`): título curto acima de seção — usado com moderação, nunca como scaffolding repetido em toda seção.

### Named Rules
**The Sentence-Case Rule.** Títulos em sentence case, nunca Title Case. Overlines são a única exceção em UPPERCASE, e só quando genuinamente curtos.

## 4. Elevation

Sistema flat por decisão de design: `--elevation-1/2/3` resolvem para `none` em todo componente — nenhum card, badge ou app bar projeta sombra para comunicar hierarquia. A separação entre superfícies vem de borda hairline (`--border`) e diferença sutil de tom de fundo (`bg-primary` vs `bg-secondary` vs `bg-card`), não de profundidade simulada. A única exceção real é um glow ambiente de acento (`--glow-accent`, um `radial-gradient` sutil de violeta a 10–14% de opacidade) atrás do topo da tela e do CTA circular de destaque — usado para dar um brilho de destaque a um elemento-âncora, nunca como sombra estrutural.

### Named Rules
**The Flat-By-Default Rule.** Superfícies são flat em repouso. Se um elemento parece precisar de sombra para se destacar, a resposta é borda ou fundo tonal, não `box-shadow`. O único ponto de glow é o acento de marca, usado com extrema parcimônia.

## 5. Components

Botões, cards e badges devem parecer diretos e confiáveis, sem enfeite: formas simples, um único acento aplicado com intenção, e cor semântica que nunca mente sobre o que significa.

### Buttons
- **Shape:** altura mínima 48px, padding `0 16px`, `border-radius: 12px`, peso 700.
- **Primary:** fundo `--accent` (`#6C2BFF`), texto branco.
- **Secondary:** fundo `--text-primary`, texto `--bg-card` (inverte com o tema).
- **Tonal:** fundo `--accent-container` (violeta a 12%), texto `--accent`.
- **Outline / Text / Danger-outline:** fundo transparente; outline usa borda `--border` e texto `--text-primary`; danger-outline usa borda/texto `--error`.
- **Hover / Focus:** hover eleva `translateY(-1px)` em 120–180ms; foco usa anel `3px solid color-mix(var(--accent) 34%, transparent)` com offset 3px.
- **Disabled:** `opacity: 0.64`, cursor `not-allowed`.
- **Loading:** ícone vira spinner (1rem, borda 2px `currentColor`, lado direito transparente, rotação linear 260ms infinita), label muda para "Carregando".

### Chips / Badges
- **QualityBadge:** pílula (`border-radius: 999px`), altura mínima 28px, padding `5px 11px`, ícone + texto 700/12px. `good` → verde a 12% de fundo / texto sólido; `fair` → âmbar a 14%; `poor` → vermelho a 12%; default/unknown → `--bg-secondary`/`--text-tertiary`.
- **StepTracker:** pílulas 600/13px, `padding: 8px 16px`. `done` → verde; `active` → violeta, com um ponto pulsante 7px substituindo o ícone; `pending` → neutro (`--border`/`--text-tertiary`).

### Cards / Containers
- **Corner Style:** `border-radius: 16px` em praticamente todo container (`--radius-card`).
- **Background:** `--bg-card`, com variante `outlined` (mesma borda/fundo, sem sombra) e `tonal` (borda `--accent-container`).
- **Shadow Strategy:** nenhuma — ver seção Elevation. Separação vem de borda 1px `--border`.
- **StatusCard:** variante de destaque — 56px de ícone circular, veredito codificado em `color-mix()` de borda (30%) e fundo (8%) sobre a cor de status; título 700/21px.
- **Internal Padding:** 24px padrão (`--space-xl`), reduzindo a 16px em telas ≤860px.

### Inputs / Fields
- Escala compartilha `--radius-input: 12px` com botões; nenhum componente de input dedicado foi identificado no scan atual além de controles de formulário nativos estilizados pelo token de radius.

### Navigation
- **TopAppBar:** fixo no topo, fundo combina `--glow-accent` (glow violeta sutil) sobre `--bg-secondary`, borda inferior `--border`. Desktop: 64px, item ativo em `--accent` semibold com indicador de 18×2px. Mobile (≤860px): 54px, três modos (brand / title com truncamento / back com seta).
- **SettingsMenuItem:** linha de largura total, padding `16px 24px`, borda inferior `--border` (removida no último item), ícone líder colorido por variante (`accent`/`error`/`text-tertiary`), chevron à direita por padrão.

### ProgressRing (componente de assinatura)
Anel de 300px (230px mobile) construído com `conic-gradient` do acento violeta contra `--border`, disco interno mostra o número principal em Display (700/66px, 52px mobile) com unidade em Label — o ponto onde tipografia Display, acento único e ausência de sombra se encontram na tela mais importante do app (resultado do speed test).

## 6. Do's and Don'ts

### Do:
- **Do** usar um único acento violeta (`#6C2BFF`) por tela; cor semântica (verde/âmbar/vermelho) cobre qualquer necessidade adicional de destaque.
- **Do** acompanhar toda métrica técnica de um veredito em palavras (badge de qualidade, texto de status) — nunca um número cru sozinho.
- **Do** manter superfícies flat: borda hairline `--border` e diferença de tom (`bg-primary`/`bg-secondary`/`bg-card`) fazem o trabalho de separação visual, não `box-shadow`.
- **Do** usar `border-radius: 16px` em cards e `12px` em botões/inputs consistentemente — a escala de radius é fixa, não varia por seção.
- **Do** respeitar `prefers-reduced-motion` — o pulso de progresso e o shimmer de skeleton precisam de alternativa estática.

### Don't:
- **Don't** usar hero-metric com gradiente, cards de feature idênticos ou glassmorphism decorativo — clichê de SaaS genérico, explicitamente rejeitado pelo PRODUCT.md.
- **Don't** copiar a identidade visual, cor ou layout do Google Fiber Speed Test — serve só como referência de clareza de interação, nunca como fonte de marca.
- **Don't** usar `border-left`/`border-right` colorido como stripe decorativo em cards ou alertas.
- **Don't** aplicar gradiente em texto (`background-clip: text`) para ênfase — usar peso ou tamanho.
- **Don't** inventar ou arredondar métrica que o navegador não mediu de verdade — se não foi medida, o rótulo é "não medida", nunca um número aproximado.
- **Don't** adicionar uma segunda cor de acento de marca; se uma tela parece precisar, o problema é hierarquia, não paleta.
