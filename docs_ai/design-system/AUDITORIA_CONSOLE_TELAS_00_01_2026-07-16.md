# Auditoria — SignallQ Console, telas 00 · Login e 01 · Centro de Controle

**Data:** 2026-07-16
**Responsável:** Lia
**Status:** ativo
**Última validação:** 2026-07-16
**Escopo:** `SignallQ Admin/src/auth/LoginPage.tsx`, `src/components/layout/AppLayout.tsx`,
`src/components/layout/Sidebar.tsx`, `src/components/layout/Topbar.tsx`,
`src/features/overview/OverviewPage.tsx` + componentes filhos (`SectionIntro`,
`OverviewMetricGrid`, `DiagnosticsTimeline`, `ScreenSessionsDonut`, `RecentAlertsPanel`).
**Fonte de cor e layout (CORRIGIDO 2026-07-16):** protótipo Claude Design `signallq-admin-md3-tobe`
(`Md3Screen00Login`, `Md3Screen01Overview`, `Md3NavRail`, `Md3BottomNav`) — por instrução direta do
Luiz, revertendo a decisão de 2026-07-16 que havia dado `SignallQ Admin/src/index.css` como fonte
de cor (ver `DECISAO_CORES_CONSOLE_PROTOTIPO_MD3_TOBE_2026-07-16.md`, seção "Correção 2026-07-16",
e `FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md`). Os achados de cor/dimensão desta auditoria que
antes foram classificados como "protótipo desatualizado, não é bug" estão reclassificados abaixo
como bugs reais. Achados de estrutura, copy e acessibilidade não mudam de classificação — já eram
bug antes e continuam sendo.

**Nota de método (atualizada 2026-07-16):** o protótipo `md3-tobe` é local em disco
(`C:\Users\luizg\Documents\7Agents\Claude Design\SignallQ Design System\templates\signallq-admin-md3-tobe\`)
e legível direto via Read/Grep — a ressalva original de "sem export, não dá pra confirmar pixel a
pixel sem recarregar" estava incorreta. Os itens 2, 4 e 8-12 da tabela final foram fechados com
valor pixel exato nesta passagem, lendo os arquivos-fonte `.dc.html` diretamente; ver
`FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md` para as tabelas de hex/px completas.

---

## Regra de escopo

Achados aqui são **relatório para o Camilo implementar**. Nenhum `.tsx`/`.ts`/`.css` foi editado.

---

## Tela 00 · Login (`src/auth/LoginPage.tsx`)

Sem As-Is no protótipo — feature nova no To-Be, sem regressão a comparar, só conformidade MD3 +
qualidade própria.

### Divergente

1. **Bordas de card e input hardcoded em branco, não em token** (`LoginPage.tsx:79`, `:100`,
   `:130`) — `border: 1px solid ${alpha("white", 8)}` / `alpha("white", 10)`. Todo o resto do
   Console usa `var(--border)`/`var(--sq-border)` de `index.css`. Em tema claro
   (`[data-theme="light"]`, `--border: #DCDCDC`), essa tela continua desenhando borda branca a
   8-10% de opacidade sobre fundo claro — ilegível, quebra o tema claro que o resto do app já
   suporta. Trocar por `var(--sq-border)` (ou `var(--border)` direto).

2. **Radius de input e botão hardcoded via Tailwind, não via token** — `rounded-xl` em
   `LoginPage.tsx:97`, `:127`, `:160` em vez de `var(--radius-input)` (12px) / `var(--radius-button)`
   (12px). Hoje bate por coincidência (Tailwind `rounded-xl` = 12px = valor atual do token), mas
   se o token de radius mudar (aconteceu na migração To-Be do Android, ver
   `DECISAO_ALINHAMENTO_TOBE_2026-07-13.md` — radius de botão passou de 12 para 20 no app), o
   Login do Console não acompanha. Card já faz certo (`var(--radius-card)`, linha 76) — só falta
   replicar o padrão nos inputs/botão.

3. **`<label>` sem `htmlFor` associado ao `<input>`** (`LoginPage.tsx:84-90` e `:115-121`) — os
   dois campos (E-mail, Senha) têm label visual mas sem vínculo semântico (`id`/`htmlFor`).
   Leitor de tela não anuncia o rótulo ao focar o campo. Acessibilidade real, não cosmético — fix
   é adicionar `id="login-email"`/`htmlFor="login-email"` (e equivalente para senha).

### Conforme

- Card usa `var(--radius-card)` e `var(--sq-bg-elevated)` corretamente.
- Botão usa `var(--sq-accent)` (token, não hex solto), estado disabled com opacidade 40% —
  contraste de toque ok (padding `py-3` ~ 44px+ de altura).
- Copy PT-BR, sentence case, sem jargão ("E-mail ou senha inválidos.", "Muitas tentativas. Aguarde
  15 minutos.") — trata os 4 estados de erro (401/429/outro/sem conexão) com mensagem específica,
  não genérica. Bom padrão, mantém.
- Loading state no botão ("Verificando...") em vez de travar sem feedback — correto.
- `autoFocus` no e-mail, `autoComplete` correto nos dois campos — bom.

### Não encontrado / não verificável

- Campo de senha sem toggle mostrar/ocultar — não é regressão (não existe no As-Is nem
  confirmado no protótipo), fica como sugestão de melhoria futura, não achado de conformidade.
- Link "esqueci minha senha" — ausente; como é ferramenta interna de admin (não há fluxo de
  autoatendimento de senha hoje, é reset manual), não trato como gap sem confirmar com Camilo/
  Claudete se existe fluxo de reset a linkar.

---

## Tela 01 · Centro de Controle (`AppLayout.tsx` + `Sidebar.tsx` + `Topbar.tsx` + `OverviewPage.tsx`)

### Divergente

1. **Banner de staging com cor hardcoded, não token** (`AppLayout.tsx:88-89`) —
   `bg-amber-500/10 border-amber-500/20 text-amber-400` em vez de `var(--attention)`
   (`#F59E0B` dark / `#B06000` light). Efeito prático: no tema claro o amber-400 do Tailwind não
   acompanha o ajuste de contraste que `--attention` recebeu (`index.css:155`, mais escuro pro
   claro por AA) — banner pode ficar com contraste pior que o resto do app nesse tema. Já
   reportado como achado pequeno pro Camilo pegar quando tocar no arquivo (não é issue formal).

2. **Inconsistência de sistema de ícone** (`Sidebar.tsx:249`) — toggle de tema usa
   `material-symbols-outlined` (`light_mode`/`dark_mode`), único ponto do Console fora do padrão
   Lucide usado em todo o resto (Topbar, Sidebar, MetricCard, AlertList, FilterBar,
   ActionsRow, EmptyState, InsightBlock — todos `lucide-react`). Já registrado na decisão de
   cores/ícone. Fix sugerido: `Sun`/`Moon` do Lucide, mesma import já usada nos outros arquivos.

3. **Ícone semanticamente errado no botão "Sair"** (`Topbar.tsx:198`, `LayoutGrid`) — o próprio
   comentário do código admite a origem ("mesmo slot do ícone de apps do mockup, ligado a uma
   ação real"): o mockup original usava esse grid como app switcher decorativo, e foi reaproveitado
   para logout sem trocar o ícone. Grid de apps ≠ sair. Usuário vê um ícone de "grade/apps" e a
   ação é deslogar — isso é exatamente "funcional mas confuso": a ação funciona, o ícone não
   comunica o que ela faz. Trocar por `LogOut` do Lucide.

4. **Gap de padrão de navegação intermediário (rail) entre mobile e desktop — RECLASSIFICADO PARA
   BUG REAL (2026-07-16)** — `Sidebar.tsx:79-84` só tem dois estados: drawer off-canvas completo
   (`-translate-x-full`, abaixo de `lg`) e sidebar fixa de 264px (a partir de `lg`, ~1024px). Não
   existe um terceiro estado de nav colapsada/rail (ícones sem label, ~72-80px) para telas
   intermediárias (tablet portrait, ~768-1024px) — nessa faixa o usuário só tem hamburger + overlay
   cobrindo a tela toda, igual ao celular, mesmo tendo espaço horizontal de sobra pra um rail. Na
   passagem anterior (2026-07-16, revogada) isso foi registrado como "possível decisão deliberada,
   sem abrir issue". Com o protótipo `md3-tobe` como fonte de verdade, `Md3NavRail` deixa de ser
   "demonstração de padrão" e passa a ser componente a implementar — falta real de estado
   intermediário, não decisão de produto a preservar. Mesma reclassificação vale para
   `Md3BottomNav` (padrão mobile do protótipo) — não há bottom nav na implementação atual, que usa
   só drawer off-canvas em mobile; confirmar no reload se substitui o drawer ou coexiste antes de
   implementar. Também não há sidebar-width de 264px correta a preservar — o protótipo especifica
   uma largura própria (mapeada nesta sessão como ~300px, a confirmar pixel exato no reload) que
   vira o valor alvo.

### Conforme

- `index.css` é seguido rigorosamente no resto do layout: `var(--bg-base)`, `var(--bg-content)`,
  `var(--bg-topbar)`, `var(--bg-sidebar)`, `var(--primary)`, `var(--success)` — sem hex solto fora
  dos dois pontos acima.
- Toque: hamburger mobile (`Topbar.tsx:67`) e botão refresh mobile (`:158`) usam
  `min-w-[44px] min-h-[44px]` explícito — cumpre alvo de toque MD3/acessibilidade. Item de nav do
  Sidebar usa `min-h-[44px]` (`Sidebar.tsx:170`) — também ok.
  Ressalva: os botões de ambiente PROD/STG e período (`Topbar.tsx:87-104`, `:114-131`) usam só
  `py-1.5`/`py-1.5` sem `min-h`, ficando abaixo de 44px de altura de toque real — não é a tela 00,
  mas fica registrado aqui porque está na mesma auditoria e é o mesmo padrão do rail/pill;
  avaliar se compensa (pills pequenas costumam ser aceitas em densidade alta de admin panel, mas
  vale o Camilo confirmar medida real em device).
- Copy da `SectionIntro`: overline uppercase, H1 em forma de pergunta ("O SignallQ está saudável
  agora?"), parágrafo descritivo, linha mono de fontes — segue exatamente o padrão documentado no
  próprio componente (`SectionIntro.tsx:3-8`), consistente com a convenção SignallQ de metrica
  crua + veredito humano.
- Estados honestos de "Não disponível" nos 4 KPIs (`OverviewMetricGrid.tsx`) com motivo específico
  por card (`verdictNote`) em vez de esconder o card ou mostrar zero — bom padrão de vazio, evita
  simular dado que não existe.
- Badge de erro no Sidebar (`Sidebar.tsx:55-70`) puxa da mesma fonte real da tela de erros
  (`errorMetricsService`), não valor estático — evita dessincronia número-do-menu vs KPI real,
  já é boa prática documentada no próprio comentário.
- Estados de loading/erro/vazio da Overview (`OverviewPage.tsx:122-152`) — três estados distintos
  e com copy própria, "TENTAR NOVAMENTE" com ação real de retry, não placeholder morto.

### Comparação pixel a pixel — FECHADA 2026-07-16

Lido `Md3DashboardContent.dc.html`/`Md3DashboardContentMobile.dc.html` direto. A ordem/grid do
`md3-tobe` **diverge estruturalmente** da composição documentada em `OverviewPage.tsx:154-197`
(`SectionIntro` → `OverviewMetricGrid` → grid timeline+donut → `RecentAlertsPanel`, paridade com o
mockup do Luiz) — é um gap real de estrutura, não só de cor:

- **Desktop:** overline+H1 → seção **"App"** com header próprio (dot roxo + label uppercase +
  linha divisória) contendo grid de 4 `MetricCard` + row `2fr 1fr` (gráfico de barras "Sessões · 14
  dias" | donut "Sessões por Tela") → seção **"Rede & Operadora"** com header próprio (dot azul)
  contendo grid `3 colunas + auto` (3 `MetricCard` + card CTA "Ver Redes & Provedores →") → card
  isolado **"Custo de IA · mês"** (ícone + valor + CTA "Ver IA & Custos →") → card **"Alertas
  Recentes"** com badge de categoria por linha (`App` / `IA & Custos` / `Sistema`).
- **Mobile:** row de filtro (chip "Produção" + chip "7 dias" + botão refresh circular) → H1 → mesma
  estrutura de seções com header "App"/"Rede & Operadora" (2 colunas em vez de 4/3) → donut em
  layout horizontal (avatar + lista, não em card separado da lista) → custo de IA → alertas com
  layout vertical (badge + texto empilhados, não lado a lado).

A implementação atual do Console (`OverviewMetricGrid` sem agrupamento "App"/"Rede & Operadora"
com header próprio, sem card "Custo de IA" isolado, sem CTA "Ver Redes & Provedores"/"Ver IA &
Custos") não replica essa estrutura de seções agrupadas — **gap real a implementar**, não é ajuste
de token, é reestruturação da `OverviewPage.tsx`. Fora do escopo de fechar valor pixel exato desta
passagem (que cobriu cor/radius/navegação, itens 2/4/8-12); registrar como achado novo de estrutura
para o Camilo — não vira número na tabela de tokens abaixo por não ser um valor de design system,
mas precisa entrar no planejamento de implementação da Overview junto com os itens 8-12.

---

## Resumo para o Camilo (atualizado 2026-07-16 — protótipo md3-tobe é fonte de verdade)

Achados de estrutura/copy/acessibilidade continuam pequenos e localizados, sem bloqueio. Achados de
cor/dimensão/navegação foram reclassificados de "protótipo desatualizado" para **bugs reais** — a
implementação (`index.css`, `DESIGN.md`, `Sidebar.tsx`) precisa migrar para os valores do
`md3-tobe`, não o inverso. **Itens 2, 4, 8-12 têm valor pixel exato fechado nesta passagem** (ver
tabela "Cor, radius, navegação — VALORES FECHADOS" abaixo) — liberado para implementar.

### Estrutura, copy e acessibilidade (classificação não mudou)

| # | Arquivo:linha | Achado | Fix sugerido |
|---|---|---|---|
| 1 | `LoginPage.tsx:79,100,130` | Borda hardcoded em branco, quebra tema claro | `var(--sq-border)` (token a ser realinhado ao md3-tobe, ver bloco de cor abaixo) |
| 3 | `LoginPage.tsx:84-90,115-121` | Label sem `htmlFor`/`id` — acessibilidade | associar `id`/`htmlFor` |
| 5 | `Sidebar.tsx:249` | Único ícone `material-symbols-outlined` no Console (resto é Lucide) | `Sun`/`Moon` do Lucide |
| 6 | `Topbar.tsx:198` | Ícone `LayoutGrid` (grade/apps) usado pra ação de logout — confuso | `LogOut` do Lucide |
| 7 | `Topbar.tsx:87-104,114-131` | Pills PROD/STG e período sem `min-h-[44px]` explícito | confirmar altura real de toque |

### Cor, radius, navegação — VALORES FECHADOS (2026-07-16), liberado pro Camilo implementar

| # | Arquivo:linha | Achado | Fix (valor exato) |
|---|---|---|---|
| 2 | `LoginPage.tsx:97,127,160` | Radius via Tailwind hardcoded (`rounded-xl`=12px), não token | Input: **12px** (bate com o valor atual, só trocar hardcode por token `var(--radius-input)`). Botão "Entrar": **pill total** (`border-radius:9999px`/`altura÷2`, não 12px fixo) — `rounded-xl` está errado pro botão, precisa virar `rounded-full` |
| 4 | `AppLayout.tsx:88-89` | Banner staging com Tailwind amber hardcoded | Protótipo não tem banner equivalente (usa chip segmentado PROD/STG no lugar). Corrigir hex do token `--attention` para **`#FFB955` dark / `#8A5300` light** (valor real do warning no protótipo, diferente do `#F59E0B`/`#B06000` atual). Decisão de produto pendente (Claudete/Camilo): manter banner com esse hex corrigido, ou migrar pro padrão de chip PROD/STG do protótipo |
| 8 | `index.css` (paleta light+dark inteira) | Preto/branco quase puro + violeta só como acento, diverge da paleta M3 baseline do protótipo | Tabelas completas de hex (bg-base, bg-surface, bg-elevated, border, text, primary/secondary/success/warning/error, pill de nav ativo) em `FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md`, seções 2 e 3 |
| 9 | `index.css:16-18` (`--radius-card`/`--radius-button`/`--radius-input`) | 16px/12px/12px atuais | `--radius-card`: **16px → 12px**. `--radius-button`: **12px → pill total** (não px fixo). `--radius-input`: **12px, sem mudança** (já batia) |
| 10 | `index.css:14` (`--sidebar-width: 264px`) | Protótipo usa largura própria | `Md3NavDrawer` define **300px** exatos e literais — `264px → 300px` |
| 11 | `Sidebar.tsx:79-84` | Falta terceiro estado de nav (rail) para tablet — `Md3NavRail` do protótipo | Largura **88px**, ícone-only sem label, pill ativo `56×32px`/`radius:16px`, badge `16px` circular. Breakpoint exato **não definido no protótipo** (só texto descritivo) — decisão de produto a parte, não bloqueia implementar o componente em si |
| 12 | `Sidebar.tsx` (mobile) | Sem bottom nav — protótipo tem `Md3BottomNav` | Altura **80px**, 5 itens fixos (Início/App/Diagnóstico/Redes/Mais), pill ativo `64×32px`/`radius:16px` com label. Confirmado: **substitui** o drawer/hambúrguer no mobile, não coexiste — composição mobile do protótipo não tem drawer nenhum |

`SignallQ Admin/DESIGN.md` também precisa realinhamento formal ao `md3-tobe` — issue
[#1040](https://github.com/gmmattey/linka-android/issues/1040) aberta para isso, não corrigida
diretamente por mim (fora do escopo de Lia editar esse arquivo, mesmo padrão do #1010 no app
Android).

**Liberado para o Camilo implementar os itens 2, 4, 8-12** com os valores acima — todos pixel
exato, confirmados por leitura direta dos arquivos-fonte do protótipo (ver
`FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md` para o detalhamento completo). A única ressalva que
permanece é o breakpoint numérico do nav rail (item 11), que o protótipo não define — decisão de
produto isolada, não impede implementar o componente `Md3NavRail` em si. O gap estrutural de
grid/ordem da Overview (`Md3DashboardContent` vs. `OverviewPage.tsx`, ver seção "Comparação pixel a
pixel — FECHADA" acima) também está pronto para entrar no planejamento — é achado novo de estrutura,
não um dos itens 2/4/8-12 desta lista.
