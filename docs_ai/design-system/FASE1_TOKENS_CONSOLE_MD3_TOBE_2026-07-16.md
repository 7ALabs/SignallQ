# Fase 1 (refeita) — Tokens base do SignallQ Console vs. protótipo `signallq-admin-md3-tobe`

**Data:** 2026-07-16
**Responsável:** Lia
**Status:** ativo
**Última validação:** 2026-07-16
**Substitui:** a leitura implícita em `DECISAO_CORES_CONSOLE_PROTOTIPO_MD3_TOBE_2026-07-16.md`
(seção original, revogada — ver "Correção 2026-07-16" no mesmo arquivo).
**Fonte de verdade:** protótipo Claude Design `signallq-admin-md3-tobe` (`Md3Screen00Login`,
`Md3Screen01Overview`, `Md3DashboardContent`/`Md3DashboardContentMobile`, `Md3NavRail`,
`Md3BottomNav`) — por instrução direta do Luiz, revertendo a decisão anterior do mesmo dia.
**Implementação a corrigir:** `SignallQ Admin/src/index.css` + `SignallQ Admin/DESIGN.md`.

---

## Nota de método — atualização 2026-07-16 (fechamento pixel exato)

**Correção:** o protótipo `signallq-admin-md3-tobe` **é** local em disco (pasta
`C:\Users\luizg\Documents\7Agents\Claude Design\SignallQ Design System\templates\signallq-admin-md3-tobe\`)
e legível diretamente via Read/Grep, sem depender de Artifact/DesignSync — a ressalva anterior
("sem acesso a ferramenta de recarregar") estava incorreta. Esta passagem leu os arquivos-fonte
(`.dc.html`) direto e fecha valor pixel exato para os itens abaixo. Cada componente `.dc.html`
define suas cores/radius/spacing via `style` inline — não existe um arquivo central de tokens CSS
específico deste protótipo (`ds-base.js` só carrega o bundle geral do design system, sem tokens
próprios do Console); os valores abaixo foram extraídos por leitura direta de cada componente e
conferidos por consistência entre eles (mesmo hex/px repetido em múltiplos arquivos = token real,
não coincidência pontual).

---

## Checklist dos 6 itens

### 1. Fonte / ícones

- Fonte: `md3-tobe` declara `'Roboto',sans-serif` em todos os componentes lidos (fora de escopo
  desta passagem confirmar se isso diverge de Google Sans Flex do app Android — Console e Android
  têm specs de tipografia próprias). Sem divergência conhecida com `index.css` até agora.
- Ícones: `index.css`/`DESIGN.md` autorizam Lucide + Material Symbols; implementação real é quase
  100% Lucide com uma exceção (`Sidebar.tsx:249`, Material Symbols no toggle de tema) — achado já
  registrado na auditoria de telas (ver abaixo), continua válido independente da mudança de fonte
  de verdade de cor.

### 2. Paleta clara — DIVERGENTE (bug real, valores FECHADOS 2026-07-16)

`md3-tobe` usa paleta M3 baseline roxa (tom M3 Purple, primary `#6C2BFF`/`#6750A4`-family na
posição "tone 40"). Confirmado pixel exato lendo `Md3LoginForm`, `Md3TopAppBar`, `Md3NavDrawer`,
`Md3MetricCard`, `Md3DashboardContent` (tema light):

| Token | Hex (light) | Onde confirmado |
|---|---|---|
| `bg-base` (fundo de tela) | `#FEF7FF` | `Md3LoginContent`, `Md3DashboardContent`, `Md3Screen*` |
| `bg-surface` (drawer/rail) | `#F7F2FA` | `Md3NavDrawer`, `Md3NavRail`, `Md3BottomNav` |
| `bg-elevated`/card | `#ECE6F0` | `Md3MetricCard`, cards de `Md3DashboardContent` |
| `bg-surface-variant` (chip/menu bg contrastante) | `#FFFFFF` com borda `#CAC4D0` | `Md3TopAppBar` (PROD/STG), `Md3LoginForm` (input) |
| `border`/outline | `#CAC4D0` | consistente em todos os componentes light |
| `text-primary` | `#1D1B20` | consistente |
| `text-secondary` | `#49454F` | consistente |
| `text-tertiary`/label uppercase | `#79747E` | consistente |
| `primary` (accent/CTA) | `#6C2BFF` | botão Entrar, dot "App", donut fatia 1 |
| `on-primary` | `#FFFFFF` | texto do botão Entrar |
| `secondary` (accent Rede & Operadora) | `#1A73E8` | dot "Rede & Operadora" no dashboard |
| `success` | `#1E8E3E` | verdict tone success (MetricCard, donut fatia 3) |
| `warning`/attention | `#8A5300` | verdict tone warning (MetricCard), ícone custo IA |
| `error` | `#BA1A1A` | verdict tone error, badge Sair, ponto de alerta |
| `error-container` / `on-error-container` (badge contagem) | `#FFDAD6` / `#410002` | badge do item de nav "Problemas & Incidentes" |
| pill de nav ativo (bg/on) | `#E8DEF8` / `#1E192B` | `Md3NavDrawer`, `Md3NavRail`, `Md3BottomNav` item ativo |

Comparado a `index.css` atual (light): `--bg-base: #F8F8F8` → deve virar `#FEF7FF`; `--bg-surface:
#FFFFFF` → deve virar `#ECE6F0` (card) com `#F7F2FA` para superfície de navegação; `--primary:
#6C2BFF` já bate (não muda); âmbar `#B06000` diverge do warning real do protótipo (`#8A5300`).
**Ação:** migrar `index.css` light para estes hex exatos — sem mais pendência de reload.

### 3. Paleta escura — DIVERGENTE (bug real, valores FECHADOS 2026-07-16)

Confirmado pixel exato nos mesmos arquivos (tema dark):

| Token | Hex (dark) | Onde confirmado |
|---|---|---|
| `bg-base` (fundo de tela) | `#141019` | `Md3LoginContent`, `Md3DashboardContent`, `Md3TopAppBar` |
| `bg-surface` (drawer/rail/bottom nav) | `#1D1A22` | `Md3NavDrawer`, `Md3NavRail`, `Md3BottomNav` |
| `bg-elevated`/card | `#2B2831` | `Md3MetricCard`, cards de `Md3DashboardContent` |
| `bg-surface-variant` (chip/menu bg contrastante) | `#211E27` com borda `#49454F` | `Md3TopAppBar` (PROD/STG), `Md3LoginForm` (input, `#141019` no input em si) |
| `border`/outline | `#49454F` | consistente em todos os componentes dark |
| `text-primary` | `#E6E0E9` | consistente |
| `text-secondary` | `#CAC4D0` | consistente |
| `text-tertiary`/label uppercase | `#938F99` | consistente |
| `primary` (accent/CTA, tone 80 pro dark) | `#CFBCFF` | botão Entrar (dark), dot "App" |
| `on-primary` | `#38008C` | texto do botão Entrar (dark) |
| `secondary` (accent Rede & Operadora) | `#8AB4F8` | dot "Rede & Operadora" no dashboard (dark) |
| `success` | `#7DDB93` | verdict tone success |
| `warning`/attention | `#FFB955` | verdict tone warning, ícone custo IA |
| `error` | `#FFB4AB` | verdict tone error, badge Sair, ponto de alerta |
| `error-container` / `on-error-container` (badge contagem) | `#93000A` / `#FFDAD6` | badge do item de nav "Problemas & Incidentes" |
| pill de nav ativo (bg/on) | `#4A4458` / `#E8DEF8` | `Md3NavDrawer`, `Md3NavRail`, `Md3BottomNav` item ativo |

Comparado a `index.css` atual (dark): `--bg-base: #000000` → deve virar `#141019`; `--bg-surface:
#0B0B0B` / `--bg-sidebar: #050505` → devem virar `#2B2831` (card) e `#1D1A22` (navegação)
respectivamente — o preto quase puro "Cloudflare Dashboard"-like documentado em `DESIGN.md:107` é
a peça desalinhada (issue #1040 cobre o realinhamento formal do `DESIGN.md`), não o protótipo.
**Ação:** migrar `index.css` dark para estes hex exatos — sem mais pendência de reload.

**Nota sobre item 4 (banner de staging, `AppLayout.tsx:88-89`):** o protótipo `md3-tobe` **não tem
um componente de banner de staging** — o `Md3TopAppBar` substitui esse padrão por um chip
segmentado PROD/STG (`border-radius:20px`, aba ativa com bg `#4A4458`/`#E8DEF8` dark ou
`#E8DEF8`/`#1E192B` light, aba inativa sem fundo). Não há elemento amber/attention equivalente ao
banner atual em nenhum arquivo do protótipo. Ação recomendada: (1) corrigir o hex do token
`--attention`/warning para `#FFB955` dark / `#8A5300` light (valor real confirmado acima — o atual
`#F59E0B`/`#B06000` é do design system Android/AsIs, não do `md3-tobe`); (2) decisão de produto
pendente com Claudete/Camilo sobre manter um banner de staging (Console não tem chip PROD/STG
implementado hoje) ou migrar para o padrão de chip do protótipo — o protótipo não resolve isso
sozinho porque simplesmente não tem o cenário "banner" mapeado, tem um cenário diferente (toggle).

### 4. Tipografia — sem divergência conhecida até agora

`DESIGN.md` documenta hierarquia display/headline/body/label/mono com tracking negativo em métrica
grande — não há indicação, no mapeamento já feito, de que o `md3-tobe` diverge dessa hierarquia
funcional (tamanhos podem variar, mas a lógica displaymétrica + veredito ao lado é comum aos dois
produtos SignallQ). Reconfirmar tamanhos exatos no reload antes de fechar como "conforme".

### 5. Espaçamento — sem divergência confirmada, exceto navegação (item 6)

Grid 4/8/12/16/24/32 de `DESIGN.md` é compatível com o padrão 8dp do design system Android
(`SignallQ-design`). Não há achado de divergência de espaçamento de conteúdo entre os dois — só a
largura de navegação (abaixo), que é especificamente um valor de layout, não de spacing scale.

### 6. Componentes base — DIVERGENTE (bugs reais, valores FECHADOS 2026-07-16)

- **Radius — direção confirmada** lendo `Md3LoginForm`, `Md3MetricCard`, `Md3NavDrawer`,
  `Md3TopAppBar`, `Md3DashboardContent`:
  - `--radius-card`: protótipo usa **12px** de forma consistente (`Md3MetricCard`, card de sessões
    14 dias, card de sessões por tela, card de custo de IA, card de alertas recentes, dropdown de
    conta/projeto) — **não 16px**. `index.css` hoje tem `--radius-card: 16px` → **corrigir para
    12px** (reduz, direção oposta ao que o handoff da Claudete havia sugerido).
  - `--radius-button`: o botão primário "Entrar" do Login usa `height:54px` +
    `border-radius:27px` — **pill total (radius = altura/2), não um valor fixo tipo 12/16/20px**.
    `index.css` hoje tem `--radius-button: 12px` → **corrigir para full/pill** (`border-radius:
    9999px` ou `calc(altura/2)`, não um px fixo). Mesmo padrão pill aparece no item de nav ativo do
    drawer (`height:46px` / `border-radius:23px`) e nos chips PROD/STG e período do TopAppBar
    (`height:40px` / `border-radius:20px` — aqui o protótipo usa valor fixo 20px, não pill, porque
    é um segmented control com múltiplas abas dentro do mesmo container, não um botão isolado).
  - `--radius-input`: **12px**, igual ao valor atual de `index.css` — **não há divergência aqui,
    remover da lista de bugs**. Confirmado em `Md3LoginForm` (`border-radius:12px` nos dois inputs,
    dark e light).
- **Largura de navegação (drawer/sidebar) — FECHADA:** `Md3NavDrawer` define
  `width:300px` de forma explícita e literal (não é estimativa) — `index.css` hoje tem
  `--sidebar-width: 264px` → **corrigir para 300px** exatos.
- **`Md3NavRail` — especificação completa (item 11, FECHADA a parte de dimensão/visual):**
  - Largura: **88px** exatos (`Md3NavRail.dc.html:13`, também citado em prosa no
    `SignallqAdminMd3ToBe.dc.html` como "Navigation Rail (88dp, somente ícone)").
  - Conteúdo: logo/avatar de projeto no topo (32px, sem menu de troca de projeto — só o ícone),
    lista vertical de itens **só ícone, sem label de texto** (diferente do padrão M3 canônico de
    rail, que normalmente tem label sob o ícone — este protótipo é rail 100% icon-only), divisor
    fino (`1px`) entre grupos de nav em vez do header de grupo com texto que o drawer tem, avatar
    de conta (36px, círculo) fixo embaixo com menu popover lateral (`left:76px`).
  - Item ativo: pill `56px × 32px`, `border-radius:16px`, ícone 22px; badge de contagem (quando
    houver) é círculo `16px` sobreposto no canto superior direito do item, mesmo padrão de cor de
    `error-container` do drawer.
  - **Breakpoint — permanece em aberto, ressalva mantida:** o protótipo não define nenhum valor de
    breakpoint em px/media query para quando o rail deve aparecer — o único texto é descritivo
    ("navegação colapsada... mesmo padrão vale para as 10 telas"), sem faixa numérica. A sugestão
    anterior desta sessão (tablet, ~768-1024px) continua sendo só uma proposta de produto, não um
    valor do protótipo — decisão de breakpoint fica com Claudete/Camilo ao implementar, não é achado
    de design a fechar por leitura de arquivo.
- **`Md3BottomNav` — especificação completa (item 12, FECHADA):**
  - Altura: **80px** exatos, `padding:12px 8px`, `justify-content:space-around`.
  - Itens: **5 fixos** (`Início` → `/overview`, `App` → `/product-analytics`, `Diagnóstico` →
    `/diagnostics`, `Redes` → `/networks`, `Mais` → `/more`) — não são os mesmos 10 itens do
    drawer/rail, é uma versão condensada com um item catch-all "Mais".
  - Item ativo: pill `64px × 32px`, `border-radius:16px`, ícone 22px + label 12px abaixo do ícone
    (aqui SIM tem label, diferente do rail).
  - **Confirmado que SUBSTITUI o drawer/hamburger no mobile, não coexiste com ele:** a composição
    mobile de `Md3Screen01Overview.dc.html` é `Md3StatusBar` → `Md3TopAppBarMobile` → conteúdo →
    `Md3BottomNav`, sem nenhum `Md3NavDrawer`/hambúrguer-drawer no fluxo mobile. A implementação
    atual (`Sidebar.tsx` com drawer off-canvas em mobile) precisa ser substituída por bottom nav no
    mobile, não receber bottom nav como complemento do drawer existente.

---

## Resumo — valores FECHADOS 2026-07-16, liberado para o Camilo implementar

| # | Item | Implementação atual | Protótipo (md3-tobe) — valor exato | Ação |
|---|---|---|---|---|
| 1 | Paleta (light + dark) | Preto/branco quase puro, violeta só acento | Ver tabelas de hex nas seções 2 e 3 acima | Migrar tokens de `index.css` para os hex confirmados |
| 2 | Radius card | `--radius-card: 16px` | **12px** | Corrigir para 12px |
| 2b | Radius button | `--radius-button: 12px` | **pill total** (altura/2, ex. 27px para h54) | Corrigir para full/pill, não px fixo |
| 2c | Radius input | `--radius-input: 12px` | **12px** | Sem divergência — não mexer |
| 3 | Largura de navegação | `--sidebar-width: 264px` | **300px** exatos | Corrigir para 300px |
| 4 | Banner de staging / attention | Tailwind `amber-500` hardcoded | Sem banner equivalente; warning real do protótipo é `#FFB955` dark / `#8A5300` light | Corrigir hex do token `--attention`; decisão de produto pendente sobre manter banner vs. migrar p/ chip PROD/STG |
| 11 | Nav rail (tablet) | Ausente — só drawer/sidebar | `Md3NavRail`: **88px**, ícone-only sem label, pill ativo 56×32/radius16 — ver spec completa acima | Implementar componente; breakpoint exato **não definido no protótipo**, decidir com Claudete/Camilo |
| 12 | Bottom nav (mobile) | Ausente — drawer off-canvas | `Md3BottomNav`: **80px**, 5 itens fixos, **substitui** o drawer no mobile (não coexiste) | Substituir drawer mobile por bottom nav |

Todos os itens acima têm valor pixel exato confirmado por leitura direta dos arquivos-fonte do
protótipo (sessão 2026-07-16, ver nota de método no topo) — **liberado para o Camilo implementar**,
exceto o breakpoint exato do nav rail (item 11), que o protótipo não define e fica como decisão de
produto a parte.
