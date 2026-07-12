---
name: SignallQ
description: Diagnóstico de conectividade Android que traduz jargão de rede em veredito humano.
colors:
  electric-violet: "#6C2BFF"
  accent-blue: "#2563EB"
  success: "#22C55E"
  warning: "#F5A623"
  error: "#FF4D4F"
  phase-latencia: "#60A5FA"
  phase-download: "#34D399"
  phase-upload: "#FBBF24"
  bg-primary: "#FFFFFF"
  bg-secondary: "#F3F4F6"
  bg-card: "#FFFFFF"
  text-primary: "#0D0D1A"
  text-secondary: "#6B7280"
  text-tertiary: "#9CA3AF"
  border: "#E5E7EB"
  warning-container: "#FFF3CD"
  on-warning-container: "#7A4E00"
  amber-surface: "#FFF8E6"
  success-container: "#D1FAE5"
  on-success-container: "#065F46"
  signallq-black: "#0D0D1A"
  signallq-dark-surface: "#1A0B2E"
  signallq-dark-card: "#1E1130"
  signallq-text-on-dark: "#F3F4F6"
  signallq-text-secondary-on-dark: "#9CA3AF"
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
    fontSize: "11px"
    fontWeight: 600
    lineHeight: 1.3
    letterSpacing: "0.3px"
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
    backgroundColor: "{colors.electric-violet}"
    textColor: "#FFFFFF"
    rounded: "{rounded.button}"
    padding: "16px 24px"
  chip-idle:
    backgroundColor: "{colors.bg-secondary}"
    textColor: "{colors.text-secondary}"
    rounded: "{rounded.pill}"
    padding: "8px 16px"
  chip-selected:
    backgroundColor: "{colors.electric-violet}"
    textColor: "{colors.electric-violet}"
    rounded: "{rounded.pill}"
    padding: "8px 16px"
  card-default:
    backgroundColor: "{colors.bg-card}"
    rounded: "{rounded.card}"
    padding: "16px"
---

# Design System: SignallQ

## 1. Overview

**Creative North Star: "The Calm Translator"**

SignallQ pega o vocabulário técnico de uma conexão de internet — RSSI, jitter, dBm, bufferbloat — e traduz em um veredito que qualquer pessoa entende: Excelente, Bom, Regular, Fraco, Forte. A superfície é Material 3 clara, branca e neutra; nada visual disputa atenção com o dado. Um único violeta elétrico marca ação e identidade; o resto do significado é carregado pela semântica de trânsito (verde/âmbar/vermelho). O sistema rejeita o visual cru de apps de speedtest genéricos (números soltos, sem contexto humano) e o visual denso de dashboards técnicos de rede — o objetivo é sempre "jargão, depois tradução", nunca jargão sozinho.

A única exceção deliberada à calma visual é a superfície da SignallQ (IA): permanentemente escura, separada do restante do app, sinalizando que ali é outro modo — conversa, não leitura de dado.

**Key Characteristics:**
- Superfícies claras, planas, sem imagem/textura/gradiente decorativo (exceto avatar e header de IA)
- Uma cor de marca (violeta), usada com parcimônia
- Verde/âmbar/vermelho carregam significado de qualidade — nunca decoração
- Métrica crua sempre ao lado do veredito humano
- SignallQ (IA) é sempre escura, mesmo com o app em tema claro

## 2. Colors

Paleta majoritariamente branco e cinza-claro; violeta satura só onde há ação ou identidade; cores de status fazem o trabalho pesado de significado.

### Primary
- **Electric Violet** (`#6C2BFF`): CTA primário, tab de navegação ativa, seleção, botão de speedtest (disco sólido com glow suave da mesma cor).

### Secondary
- **Accent Blue** (`#2563EB`): gradiente do avatar de perfil (violeta→azul), badges de dado "Móvel".

### Tertiary
- **Phase Latência** (`#60A5FA`), **Phase Download** (`#34D399`), **Phase Upload** (`#FBBF24`): cores dedicadas às três fases do speedtest, usadas só no gráfico/gauge de progresso.

### Neutral
- **Bg Primary** (`#FFFFFF` claro / `#000000` escuro): fundo principal de tela.
- **Bg Secondary** (`#F3F4F6` claro / `#1A1A1A` escuro): superfícies secundárias, chips idle.
- **Bg Card** (`#FFFFFF` claro / `#111111` escuro): cards e superfícies de conteúdo.
- **Text Primary** (`#0D0D1A` claro / `#F3F4F6` escuro): título e corpo.
- **Text Secondary** (`#6B7280` claro / `#9CA3AF` escuro): descrições.
- **Text Tertiary** (`#9CA3AF` claro / `#6B7280` escuro): labels, captions, overlines.
- **Border** (`#E5E7EB` claro / `#2A2A2A` escuro): divisores, bordas leves.

### Status
- **Success** (`#22C55E`): conexão boa, teste OK. Container: `#D1FAE5` / on-container `#065F46`.
- **Warning** (`#F5A623`): alertas moderados. Container: `#FFF3CD` / on-container `#7A4E00`. Surface âmbar dedicada: `#FFF8E6`.
- **Error** (`#FF4D4F`): falhas críticas de conexão.

### SignallQ (IA) — sempre escura
- **SignallQ Black** (`#0D0D1A`): fundo da IA, independente do tema do app.
- **SignallQ Dark Surface** (`#1A0B2E`) / **SignallQ Dark Card** (`#1E1130`): superfícies e bolhas de conversa da IA.

### Named Rules
**The One Accent Rule.** Violeta aparece só em CTA, seleção e nav ativa — nunca como cor de fundo geral ou decoração. Se a tela precisa de mais destaque, o destaque é semântico (status), não de marca.

**The Always-Dark AI Rule.** A superfície da SignallQ (IA) nunca herda o tema claro do app. É um modo visual à parte, sinalizando "aqui você conversa, não lê dado".

## 3. Typography

**Display Font:** Roboto (system default do Android, sem fonte customizada)
**Body Font:** Roboto
**Label Font:** Roboto, com tracking positivo (+0.3px) e uppercase nos overlines

**Character:** Uma única família tipográfica carrega toda a hierarquia — a distinção vem de peso e tamanho, não de mistura de fontes. Números grandes e em negrito com unidade pequena e secundária ao lado (`87 Mbps`) é o tratamento assinatura de métrica.

### Hierarchy
- **Display** (700, 34px, 1.15): resultado de teste, número hero de velocidade.
- **Headline** (600, 24px/20px/18px conforme large/medium/small, 1.25–1.3): títulos de tela e seção.
- **Title** (500, 16px/15px/14px, 1.4): subtítulos, títulos de card.
- **Body** (400, 16px/14px/12px, 1.5–1.45): texto corrido; body-medium e body-small usam text-secondary.
- **Label** (500/400, 14px/12px/11px, 1.3): labels de UI, captions, valores de chip.
- **Overline** (600, 11px, 1.3, letter-spacing 0.3px, UPPERCASE): rótulos de seção (`SUA CONEXÃO`, `ÚLTIMO RESULTADO`).

### Named Rules
**The Verdict-Beside-Metric Rule.** Nenhum número técnico aparece sozinho; o veredito humano (Excelente/Bom/Regular/Fraco/Forte) sempre está a um olhar de distância, geralmente na mesma linha ou logo abaixo.

## 4. Elevation

Sistema flat por padrão: profundidade vem de superfície tonal (bg-card sobre bg-secondary) e borda `1px solid var(--border)`, não de sombra. Sombra é reservada para dois elementos que precisam se destacar fisicamente: o botão de iniciar speedtest (disco violeta com glow suave da mesma cor) e o segmento ativo do seletor Rápido/Completo/Triplo (pill branco com elevação sutil sobre trilho neutro).

### Shadow Vocabulary
- **cta-glow**: glow suave na cor do próprio elemento (ex.: violeta ao redor do botão de speedtest) — nunca cinza/preto genérico.
- **active-segment**: sombra leve sob o pill branco ativo do seletor segmentado, para separá-lo do trilho.

### Named Rules
**The Flat-By-Default Rule.** Cards e superfícies não têm sombra. Se algo parece precisar de sombra para se destacar, primeiro tente tonal fill + borda; sombra é exceção, não hábito.

## 5. Components

### Buttons
- **Shape:** radius 12px (`--radius-button`).
- **Primary:** fill `--accent` (#6C2BFF), texto branco, padding 16px 24px.
- **Hover / Focus:** ripple M3 + state-layer tint (accent/onSurface em baixa opacidade); sem hover de web, é app touch.
- **Segmented selector:** trilho em `--bg-secondary`; segmento ativo é pill branco com leve elevação — deliberadamente neutro, não colorido de accent.

### Chips
- **Filter chips idle:** fully-rounded (`--radius-pill`, 999px), fill `--bg-secondary`, texto `--text-secondary`.
- **Filter chips selected:** fill accent em tint claro, texto accent.
- **Status chips:** fill da cor semântica a ~12% de alpha, borda a ~25–30% de alpha.

### Cards / Containers
- **Corner Style:** 16px (`--radius-card`).
- **Background:** `--bg-card`, sentado sobre `--bg-secondary` quando precisa de separação de página.
- **Shadow Strategy:** nenhuma — ver Elevation.
- **Border:** `1px solid var(--border)` quando precisa de separação sem tint.
- **Internal Padding:** 16px padrão.

### Inputs / Fields
- **Style:** radius 12px, borda `1px solid var(--border)`.
- **Focus:** borda muda para accent.

### Navigation
- **Bottom nav:** 5 abas (Início · Velocidade · Sinal · Histórico · Ajustes), ícone Material Symbols Outlined + label, tab ativa em violeta. Some ao rolar para baixo, reaparece ao rolar para cima, oculta totalmente durante teste em execução.
- **Top bar:** `CenterAlignedTopAppBar`, título centralizado, avatar de perfil à esquerda, ação contextual à direita.

### Signal Bars (componente assinatura)
Glifo customizado de 4 barras verticais (alturas 6/9/12/16dp, largura 3dp, radius 1dp); barras preenchidas assumem a cor de qualidade (verde=Forte, âmbar=Regular, vermelho=Fraco), barras vazias usam `--border`.

### Native Ad Components (anúncio nativo, `…/ui/component/ads/`)
Três variantes por contexto de tela — nunca escolhidas por preferência: **`NativeAdCard`** (card cheio, dispensável — Resultado do teste, Histórico), **`NativeAdRow`** (linha compacta, não dispensável — Velocidade em estado idle), **`NativeAdListRow`** (linha dentro de uma lista orgânica já existente — dentro da lista de dispositivos conectados, nunca na seção Infraestrutura). Toda variante é omitida por completo (não vira placeholder/caixa vazia) quando não há criativo carregado.
- **Border:** tracejada (`Modifier.dashedBorder`), nunca sólida — único uso de borda tracejada no app, reservado para diferenciar anúncio de conteúdo orgânico.
- **CTA:** outline violeta (`accent @35%` de borda, texto accent), nunca sólido — violeta sólido é exclusivo de CTA primário orgânico.
- **Ícone do anunciante:** chip **quadrado** (raio ~27% do tamanho), nunca círculo — distingue do padrão circular de avatares/ícones orgânicos.
- **Disclosure (`AdBadge`):** sempre visível, nunca atrás de tap/expand. "Patrocinado" (tom neutro `textTertiary`) para `NativeAdSource.ADMOB`; "Parceiro" (tom `accentBlue`) para `NativeAdSource.PARTNER`, reservado a afiliados/parceiros curados (catálogo ainda não existe).
- **Nunca** foto/hero, headline/body/CTA hardcoded — sempre vêm do `NativeAd` real carregado pelo AdMob.

## 6. Do's and Don'ts

### Do:
- **Do** parear todo dado técnico com veredito humano (Excelente/Bom/Regular/Fraco/Forte).
- **Do** usar violeta elétrico (#6C2BFF) só em CTA, seleção e nav ativa.
- **Do** manter a superfície da SignallQ (IA) sempre escura, independente do tema do app.
- **Do** usar Material Symbols Outlined como único sistema de ícone; nunca emoji.
- **Do** manter cards flat — tonal fill + borda 1px, sem sombra.
- **Do** deixar todo anúncio nativo com borda tracejada + disclosure (`AdBadge`) sempre visível — nunca disfarçado de card orgânico.

### Don't:
- **Don't** parecer um app genérico de speedtest (tipo Ookla/Speedtest.net): números soltos sem contexto humano, sem veredito.
- **Don't** parecer um dashboard técnico/enterprise de rede (tipo Wireshark): jargão não traduzido, denso, feito para quem já entende de rede.
- **Don't** usar emoji como substituto de ícone ou cor semântica.
- **Don't** aplicar gradiente decorativo em superfícies — os únicos dois lugares permitidos são o avatar de perfil e o header de Diagnóstico/IA.
- **Don't** adicionar sombra pesada em cards; se precisa de destaque, use tint de cor semântica antes de sombra.
- **Don't** usar CTA violeta sólido em anúncio nativo (só outline) nem ícone de anunciante em chip circular — ambos são reservados a elementos orgânicos do app.
