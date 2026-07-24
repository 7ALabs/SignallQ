# Plano de UX/IA — Google Play (#1341/#1342) e Firebase (#1343/#1344)

Status: proposto (aguardando validação da Claudete/Camilo antes de virar issues de implementação)
Última validação: 2026-07-24
Fonte de verdade: este documento — enquanto não vira issue própria, é o plano de referência das
duas frentes

**Autocrítica registrada (2026-07-24):** a primeira versão deste plano era information
architecture, não design — descrevia componente por categoria (`MetricCard`, `ChartCard`,
`DataTable`) sem nunca decidir o que é destaque e o que é secundário dentro de cada categoria. Isso
tende a virar exatamente a "grade genérica de KPI card" que se quer evitar. Correção aplicada nos
itens 2 e 2.1 (hierarquia real por categoria) e nos protótipos visuais em
`docs/product/prototypes/` — ver item 7.
Escopo: `SignallQ Admin/` (Console) — apenas planejamento de UI/UX e organização de informação, sem
implementação
Responsável: Lia (produto & UX/frontend do Console)
Relacionado: épicos #1341 (Google Play) e #1343 (Firebase), features #1342 e #1344 (#1345 —
recursos pagos sem cota — fora de escopo)

---

## 0. O que já existe hoje (ponto de partida, não é greenfield)

Antes de propor qualquer coisa nova, o que o Admin já tem e deve ser reaproveitado:

- **Navegação em 5 seções** (`src/config/navigation.ts`, `NAVIGATION_SECTIONS`), agrupada por
  proveniência real do dado (decisão SIG-294) — não por ordem arbitrária de feature. Documentado em
  `docs/architecture/data-architecture.md`.
- **Padrão Page + Tab por feature** (`FooPage.tsx` com o conteúdo, `FooTab.tsx` fino que só
  repassa props — ver `features/errors/`). Cada Page abre com `SectionIntro`
  (overline → pergunta → parágrafo → linha mono `FONTE(S) · ...` estática por tela).
- **`integrations/google-play/` e `integrations/firebase/`** já existem com adapters e types —
  cobrem hoje só uma fatia pequena do catálogo dos épicos (status de integração, tracks básicas,
  rating agregado, analytics/crashlytics resumidos). Camilo vai expandir isso em paralelo — não
  bloqueia este plano.
- **Componentes de dado reutilizáveis:** `MetricCard` (já tem prop `source`, mono, canto superior
  direito do card), `ChartCard`, `SectionCard`, `DataTable`, `BarChart`/`LineChart`/`DonutChart`,
  `StatusBadge` (já modela `ok/attention/critical/stable/beta/deprecated/cached/...`), `EmptyState`,
  `FeatureComingSoon` (para recurso conhecido mas ainda não implementado — útil para o próprio
  #1345 quando alguém for tentado a adiantar).
- **Gap documentado e pendente:** `docs/architecture/data-architecture.md` já registra (GH#1043)
  que a legenda de fonte por tela existe só como string estática no `SectionIntro`, sem frescor nem
  status por métrica individual — exatamente o requisito que os épicos #1341/#1343 tornam obrigatório
  em escala. Este plano assume esse gap como ponto de partida a fechar, não repete o achado.

Conclusão: a base visual e de navegação não precisa ser reinventada. O trabalho é de **information
architecture** (onde entra cada categoria, quantas telas, que tipo de visualização por dado) e de
**uma extensão pequena e reutilizável do sistema de proveniência** (item 3), não um redesenho.

---

## 1. Onde a informação aparece — navegação

**Decisão: duas novas entradas de navegação de topo, não sub-abas de um app existente e não um
dashboard cross-app dedicado.** Justificativa:

- Google Play e Firebase são **fontes**, não features do produto — a mesma lógica de agrupamento por
  proveniência (SIG-294) que já rege as 5 seções atuais. Enfiar catálogo de Google Play dentro de
  "Releases & Qualidade" ou Firebase dentro de "Saúde do Sistema" sub-representaria o volume real
  (dezenas de métricas cada) e quebraria a convenção existente de 1 fonte → 1 lugar claro.
- Um dashboard cross-app único (Google Play + Firebase juntos numa tela só) polui: são catálogos de
  tamanho parecido ao que já existe inteiro hoje no Admin (5 seções, 9 páginas) — cada um sozinho já
  justifica navegação própria.
- Sub-abas dentro de categoria (Distribuição, Qualidade, Avaliações...) ficam **dentro** de cada
  página, não como itens de nav separados — 6 categorias Google Play + 4 Firebase como itens de nav
  top-level explodiriam a sidebar de 9 para ~19 itens. Duas entradas com navegação secundária interna
  mantém a sidebar estável.

Proposta de `NAVIGATION_SECTIONS` (nova seção entre "Rede & Operadora" e "Custos & Sistema" —
mantém a leitura top-down: uso do app → rede → **plataformas externas** → custo/sistema →
administração):

```
{
  label: "Plataformas",
  items: [
    { name: "Google Play", path: "/google-play", iconName: "PlayCircle" },
    { name: "Firebase",    path: "/firebase",    iconName: "Flame" },
  ],
}
```

(`PlayCircle` e `Flame` são ícones novos no `NAVIGATION_ICON_MAP` — únicos itens novos de config de
nav, resto é reaproveitamento.)

### Dentro de cada página — sub-navegação por categoria

Mesmo padrão visual do toggle "PROD/STAGING/TODOS" do `FilterBar.tsx` (pill group), só que
selecionando categoria em vez de ambiente — componente novo, pequeno, reaproveitável pelas duas
páginas: `CategoryTabs`.

**Google Play** (`/google-play`), categorias na ordem do funil do app na loja:
Distribuição → Qualidade → Avaliações → Aquisição → Monetização → Finanças.

**Firebase** (`/firebase`), categorias na ordem do funil de uso:
Comportamento → Estabilidade → Performance → Mensageria.
(Config Remota/A-B Testing/In-App Messaging entram dentro de "Mensageria" como sub-blocos, não
categoria própria — volume menor que as outras quatro. App Distribution/App Check/Management API
são inventário técnico, não métrica de produto — ver item 2, bloco "Inventário técnico".)

Cada categoria é uma seção dentro da mesma Page (`GooglePlayPage.tsx`, `FirebasePage.tsx`) —
troca de categoria só re-renderiza o conteúdo abaixo do `SectionIntro`+`CategoryTabs`, sem navegar
de rota (mesmo padrão de estado local que `period`/`environment` já usam nas páginas existentes).
Isso evita 10 arquivos de rota novos e mantém o botão "voltar" do browser previsível.

---

## 2. Como exibir cada agrupamento

Regra geral: tipo de visualização segue o **tipo do dado**, não o produto de origem — Google Play e
Firebase reaproveitam os mesmos padrões visuais entre si.

| Categoria | Natureza do dado | Componente(s) | Notas |
|---|---|---|---|
| **Distribuição** (Play) | Estado de tracks/releases, rollout % | `DataTable` (uma linha por track: internal/alpha/beta/production, versionCode, % rollout, atualizado em) + `MetricCard` para "versão ativa em produção" | Rollout % também cabe como barra de progresso inline na própria linha da tabela — não precisa de gráfico separado |
| **Qualidade** (Play — Android Vitals/ANR/crash) | Série temporal + ranking | `LineChart` (crash-free % ao longo do período) + `DataTable`/lista ranqueada de issues (top ANR, top crash por versão) | Mesmo padrão já usado em Erros (`TopCrashesCard`) — reaproveitar componente, trocar fonte |
| **Avaliações** | Distribuição + lista de eventos | Ver especificação completa no item 2.3 (barra horizontal de distribuição + lista de `ReviewCard`, não `DataTable`) | Placeholder até 2026-07-24 — hoje só existia nota média agregada, sem review individual pra desenhar em cima |
| **Aquisição** | Funil + série temporal | Funil (novo padrão visual — impressões → instalações → 1ª sessão, se a fonte cobrir) quando a API expuser cada etapa; senão `LineChart` de instalações/desinstalações por dia + `MetricCard`s de totais | Não inventar etapa de funil que a Play/Android Publisher API não cobre — cai para série temporal simples quando faltar dado |
| **Monetização** | Série temporal + tabela | `LineChart` (receita/assinaturas ativas ao longo do tempo) + `DataTable` de produtos/planos | Fora de escopo real hoje (app não tem IAP/assinatura ativa) — a tela nasce com `EmptyState`/`FeatureComingSoon`, não com mock |
| **Finanças** | Tabela | `DataTable` (transações, payouts) | Mesmo caso acima — provavelmente nasce vazia/`FeatureComingSoon` até existir monetização real |
| **Comportamento** (Firebase — Analytics) | Série temporal + ranking | Reaproveita 1:1 o que `product-analytics/` já faz (`FeatureRankingBars`, `ScreenNavigationPanel`, `RetentionBars`) — não duplicar, **linkar/redirecionar** a partir da categoria "Comportamento" do Firebase para a página existente `/product-analytics`, com nota curta "essa categoria já vive em Uso do App" | Evita ter a mesma métrica renderizada em dois lugares com risco de divergir |
| **Estabilidade** (Firebase — Crashlytics) | Série temporal + lista de issues | Mesmo caso — já existe em `/errors` (`ErrorMetricGrid`, `TopCrashesCard`). Categoria "Estabilidade" do Firebase referencia essa página, não recria | — |
| **Performance** (Firebase — Performance Monitoring) | Série temporal | `LineChart` (tempo de partida do app, latência de rede por trace) + `MetricCard`s (p50/p95) | Categoria nova sem equivalente hoje — implementação real |
| **Mensageria** (Cloud Messaging + Remote Config + A/B Testing + In-App Messaging) | Lista de eventos + tabela de config | `DataTable` para campanhas/configs ativas (nome, status, público, última alteração) + `MetricCard`s de entrega (enviado/entregue/aberto) quando aplicável | Config Remota e A/B Testing são estado de configuração, não série temporal — tabela é suficiente, não forçar gráfico |
| **Inventário técnico** (App Distribution, App Check, Management API) | Status/checklist | Reaproveita `IntegrationCheckRow` (já existe em `system-health/components/`) — lista de linha única por item técnico com `StatusBadge` | Não é dado de produto, é saúde de configuração — mais perto do que "Saúde do Sistema" já mostra do que de uma categoria de produto. Proposta: sub-bloco dentro da categoria "Mensageria" do Firebase, com rótulo próprio ("Infraestrutura"), não categoria de nav separada |

---

## 2.1 Hierarquia real por categoria (o que o item 2 não decidia)

O item 2 lista componente por natureza do dado, mas não decide **peso visual** — sem isso, cada
categoria vira N cards do mesmo tamanho e a tela não comunica "o que olhar primeiro". Regra
aplicada a toda categoria com dado real (não às que nascem `EmptyState`/`FeatureComingSoon`):

1. **Uma métrica-âncora por categoria**, sempre a primeira coisa renderizada, sempre maior que o
   resto: card ~2x a altura/largura de um `MetricCard` comum, fonte de valor grande (na faixa de
   48–56px, hoje `MetricCard` usa 24–28px — a âncora precisa de um variante `size="hero"`, não o
   card padrão), cor de superfície muda com o status real (borda/gradiente sutil verde quando OK,
   âmbar quando fora do padrão) em vez de neutro fixo. Critério de escolha da âncora: a métrica que
   já é o critério de qualidade/sucesso reconhecido da própria plataforma (Qualidade → crash-free
   sessions, porque é o limiar que o próprio Play usa para penalizar visibilidade; Estabilidade →
   usuários sem crash, porque é o corte que o Crashlytics já usa) — não a métrica mais fácil de
   calcular.
2. **Métricas de apoio ficam visivelmente menores** — `stat-row` compacto (~22px de valor, sem
   glow, sem gráfico embutido), não outro `MetricCard` do mesmo tamanho do hero. Badge de status só
   aparece quando o valor foge do esperado (ex.: "1 versão abaixo do limiar") — value neutro não
   ganha badge.
3. **Gráfico de série temporal é full-width, próprio nível abaixo do hero+apoio**, nunca dividido
   em coluna estreita ao lado de outro conteúdo — é onde a tendência é a informação, não o número
   isolado do dia. Ganha linha de limiar tracejada (ex.: 99,0% do Play) quando a categoria tiver um
   corte reconhecido, e o ponto que rompeu o limiar é destacado (cor + rótulo), não escondido entre
   os demais.
4. **Ranking de issues é lista ordenada por impacto (rank, nome, meta técnica, severidade, volume),
   não outro grid de cards** — reaproveita o padrão que `TopCrashesCard` já usa.
5. **Tabela de consulta ocasional (distribuição por versão, inventário técnico) é deliberadamente
   de baixo peso visual** — densidade maior, sem `card-hover`, sem glow — o oposto do hero.
6. **Faixa de saúde por categoria no topo da Page (antes do `CategoryTabs`/como parte dele):** gap
   encontrado durante a autocrítica — com `CategoryTabs` sozinho, nada sinaliza qual das 6
   categorias do Google Play ou 4 do Firebase precisa de atenção sem clicar em cada uma. Adição ao
   escopo do item 5: pill pequena por categoria (nome + dot de status `ok/attention/critical` +
   1 número-resumo), renderizada acima do conteúdo da categoria ativa — reaproveita `StatusBadge`,
   não é componente de dado novo. Ver protótipo `plataformas-visao-geral.html`.

Cor de status não é decorativa: ela muda de fato conforme o estado real da categoria naquele
momento (ver protótipo Firebase/Estabilidade, hero em âmbar porque há issue crítica aberta — o
protótipo Google Play/Qualidade está em verde porque, no cenário de exemplo, está tudo dentro do
esperado. A mesma tela muda de cor sozinha quando o dado muda, não é um tema fixo por categoria).

## 2.2 Critérios de aceite para a próxima rodada (Claude Design) — feedback do Luiz 2026-07-24

Sobre os três protótipos: direção aprovada, dois ajustes obrigatórios antes do handoff pro Camilo:

1. **Reduzir texto na tela.** Os protótipos de review usaram `hero-note`, legenda de gráfico e
   `annot` para explicar o que o layout já deveria comunicar sozinho (ex.: "Único ponto fora do
   padrão nos 14 dias..." como parágrafo, quando cor + posição do ponto no gráfico já dizem isso).
   Na rodada de Claude Design: cortar frase explicativa onde hierarquia visual (tamanho, cor,
   posição) já resolve; o que sobrar de texto vira rótulo curto, não parágrafo. Nota de fonte/frescor
   do item 3 continua — é proveniência, não explicação de layout.
2. **Indicador de termo técnico (tooltip "?").** Todo termo técnico (crash-free sessions, ANR rate,
   propriedade GA4, nome de API/fonte, `p50`/`p95` etc.) ganha um ícone pequeno de ajuda ao lado do
   rótulo — ao interagir (hover/tap), mostra descrição funcional, não-técnica, do que aquilo
   significa para quem não é engenheiro. Escopo de componente novo:
   - **`TermHint`** (`components/ui/`) — ícone `?` pequeno (mesma família visual do `SourceMark` do
     item 3: sem fundo/borda própria, tamanho de linha do texto), tooltip com texto curto em
     linguagem simples (1–2 frases, sem jargão do próprio termo).
   - Glossário de termos vive como dado (`termGlossary.ts` ou equivalente), não hardcoded por tela —
     mesmo termo (ex. "crash-free sessions") usado em Google Play e Firebase aponta pro mesmo verbete,
     evita descrição divergente entre as duas fontes.
   - Aplica-se a rótulo de métrica-âncora, `stat-row`, cabeçalho de coluna de tabela e legenda de
     gráfico — em qualquer lugar onde o termo técnico aparece, não só no hero.

Os protótipos atuais (item 7) não têm esses dois ajustes — são evidência da hierarquia (item 2.1),
não a versão final. Próxima passada no Claude Design nasce com 2.1 + 2.2 como critério de aceite
conjunto, antes de qualquer handoff pro Camilo.

---

## 2.3 Avaliações — especificação completa (adição 2026-07-24)

Contexto: até aqui a categoria "Avaliações" do item 2 era placeholder — só nota média agregada
(`ratingAverage` em `GooglePlayIntegrationStatus`) e um tipo `GooglePlayReviewSummary`
(`src/integrations/google-play/googlePlay.types.ts`) que já existe no código mas está incompleto
(sem idioma, sem dispositivo, sem status de tratamento — só `reviewId/userName/rating/comment/
appVersion/replyText/commentTime`). A Claudete está desenhando a tabela própria (uma linha por
review) em paralelo — esta seção assume esse modelo (review individual, não só agregado) e não
define schema de banco, só o que a tela precisa do contrato para funcionar.

**Campo novo exigido no contrato desta vez** (reportar pra Claudete/Camilo, não é decisão de UX):
idioma da review e modelo/fabricante do dispositivo — a Android Publisher API expõe os dois
(`reviews.list` retorna `comments[].userComment.reviewerLanguage` e `.device`); hoje nenhum dos
dois está no `GooglePlayReviewSummary`. Sem isso a coluna "Idioma"/"Dispositivo" do item 2.3.2
nasce vazia.

### 2.3.1 Hierarquia da categoria (segue a regra do item 2.1)

1. **Âncora não é nota média sozinha — é nota média + contagem de pendentes**, lado a lado no hero:
   nota média grande (`size="hero"`, mesmo variante do item 2.1.1) à esquerda, e à direita um
   número menor mas com cor de status real: quantidade de reviews com **nota ≤ 2 sem resposta do
   desenvolvedor** nos últimos 30 dias. Critério: nota média sozinha não comunica risco (uma review
   de 1 estrela recente sem resposta pode estar escondida atrás de uma média de 4,3) — é o par
   "quão bem estamos indo" + "o que precisa de ação agora" que faz o hero cumprir função de
   triagem, não só de vitrine.
2. **Distribuição de estrelas (5→1) é barra horizontal empilhada logo abaixo do hero**, não
   `DonutChart` — decisão revista em relação ao item 2 original (que sugeria `DonutChart` OU
   barras): barra horizontal ordena visualmente do 5 para o 1 de cima para baixo, o que já comunica
   "onde está a massa" sem precisar ler legenda de cor — donut exige legenda separada pra mapear
   cor→nota, barra não. Cada linha mostra nota (ícone de estrela + número) + contagem + % do total,
   texto mono à direita da barra, nunca dentro dela.
3. **Lista de reviews individuais é o corpo principal da categoria** — cada review é uma linha de
   card compacto, não uma linha densa de `DataTable` (revisão da proposta original do item 2, que
   sugeria `DataTable`/lista sem decidir qual): texto de comentário varia de 1 palavra a alguns
   parágrafos, isso não cabe em coluna de tabela sem truncar de um jeito que destrua a leitura —
   layout de lista de card resolve isso, `DataTable` não. Ver 2.3.2 para o conteúdo de cada linha.
4. **Ordenação padrão da lista: risco primeiro, não data primeiro** — nota ≤ 2 sem resposta no
   topo, depois nota ≤ 2 já respondida, depois o resto por data decrescente. Data pura como
   ordenação padrão esconde review antiga de nota baixa nunca respondida atrás de reviews recentes
   de 5 estrelas — o oposto do que a categoria existe pra resolver (achar rápido o que precisa de
   atenção). Toggle de reordenar por "Mais recentes" fica disponível, mas não é o padrão de
   abertura da tela.

### 2.3.2 Conteúdo de cada linha da lista de reviews

Card compacto, não `DataTable` (ver 2.3.1.3). Estrutura por linha, do mais para o menos
proeminente:

- **Nota em estrelas** (ícone, não número cru) + **nome do usuário** (ou "Usuário anônimo" quando a
  API não retornar nome) — cabeçalho da linha, mesma linha visual.
- **Badge de status de tratamento**, à direita do cabeçalho, reaproveitando `StatusBadge`:
  `pendente` (nota ≤ 2, sem resposta — tom `critical`/vermelho), `atenção` (nota 3, sem resposta —
  tom `attention`/âmbar), `respondida` (tem `replyText` preenchido — tom `ok`/verde),
  `sem_necessidade` (nota ≥ 4, sem resposta — sem badge, é o estado neutro, resposta a review
  positiva não é obrigação). Regra: badge só existe para sinalizar o que precisa de ação ou já foi
  tratado — nota alta sem resposta é o caso comum e não deve ganhar destaque visual.
- **Texto do comentário** — corpo da linha, tipografia de leitura normal (não mono), sem truncar em
  quantidade fixa de caracteres — se for muito longo (acima de ~4 linhas), colapsa com "ver mais"
  inline, não corta a frase no meio.
- **Metadados em linha mono compacta abaixo do comentário**: idioma (código + nome, ex.
  `pt-BR · Português (Brasil)`) · dispositivo (modelo, ex. `Samsung Galaxy S23`) · versão do app
  (`v0.18.2`) · data relativa (`há 3 dias`) — mesmo padrão visual da linha `FONTE(S) · ...` do
  `SectionIntro`, reaproveita o estilo, não inventa um novo.
- **Resposta do desenvolvedor**, quando existir (`replyText` preenchido): bloco recuado abaixo do
  comentário, com indicador visual de "resposta" (borda esquerda ou leve indentação, sem ícone
  extra) e data da resposta. Quando não existir e a review for `pendente`/`atenção`: nenhum bloco
  vazio fica visível — só o badge já comunica a ausência, texto tipo "Ainda sem resposta" como
  parágrafo é redundante com o badge (aplica o corte de texto do item 2.2.1).
- Ícone `TermHint` (item 2.2.2) ao lado do rótulo "Status de tratamento" no cabeçalho da lista (não
  em cada card individual) — explica o critério de `pendente/atenção/respondida` uma vez só, glossário
  compartilhado.

### 2.3.3 Filtro rápido acima da lista

Pill group reaproveitando o mesmo componente do `CategoryTabs`/toggle de ambiente (não é
`CategoryTabs` em si — é um filtro secundário dentro da categoria "Avaliações"): `Todas` (padrão) ·
`Pendentes` · `Respondidas` · por faixa de nota (`5★`...`1★`, opcional, só se a lista crescer o
suficiente pra justificar). Contagem entre parênteses em cada pill (ex. `Pendentes (7)`) — number
real, não estimado.

### 2.3.4 Estados vazios/indisponíveis (aplica o item 4)

- **Zero real** ("nenhuma review pendente"): não é erro — mensagem curta e neutra tipo "Nenhuma
  review pendente de resposta", sem tom de alerta, sem ícone de erro.
- **Indisponível** (API do Play fora do ar, sem credencial): `EmptyState` padrão do item 4, mesma
  causa específica (`no_credentials`/`error`/timeout).
- Paginação: a Android Publisher API pagina `reviews.list` — lista carrega por scroll incremental
  ("carregar mais"), não paginação numerada — mesmo padrão que o resto do Admin já usa para listas
  longas (`RecentErrorsTable`).

### 2.3.5 Componentes — novo vs. reaproveitado

| Componente | Status | Nota |
|---|---|---|
| `MetricCard` (variante `size="hero"`) | Reaproveitado (item 2.1.1) | Para a nota média do hero |
| `StatusBadge` | Reaproveitado, 4 valores novos (`pendente`, `atenção`, `respondida`, sem badge para `sem_necessidade`) | Cores já existem (`critical`/`attention`/`ok`) — só rótulo/mapeamento novo |
| `TermHint` | Reaproveitado (item 2.2.2) | 1 verbete novo no glossário: "Status de tratamento" |
| Barra horizontal de distribuição de estrelas | **Componente novo pequeno**, `RatingDistributionBars` (`components/ui/` ou local à categoria) | 5 linhas fixas, sem dependência de lib de gráfico — CSS puro, não precisa reaproveitar `BarChart`/`DonutChart` genérico |
| Card de review individual | **Componente novo**, `ReviewCard` (`features/google-play/components/`) | Não é `DataTable` nem `AlertList` — layout próprio, ver 2.3.2 |
| Filtro de pills (Todas/Pendentes/Respondidas) | **Componente novo pequeno**, reaproveita o CSS do `CategoryTabs`/toggle de `FilterBar`, instância própria | Não é o mesmo componente que a navegação de categoria — é filtro secundário, escopo local à categoria Avaliações |

---

## 3. Proveniência sem poluir a tela

Problema real: cada métrica carrega fonte + período + timezone + frescor + status de custo (lado
Firebase). Fazer isso por card sem virar ruído visual.

**Proposta — duas camadas, não uma:**

**Camada 1 — página inteira (já existe, só reforçar):** `SectionIntro` continua com a linha
`FONTE(S) · ...` no topo de cada Page. Ganha timezone e frescor agregados da fonte principal da
categoria ativa (ex.: `FONTE · Android Publisher API · Atualizado há 4h · Horário: America/Sao_Paulo`)
— um resumo, não repetição por card.

**Camada 2 — por métrica, só quando diverge do resumo da página:** estender `MetricCard` (que já
tem a prop `source` — badge mono no canto do card) para aceitar objeto em vez de string:

```ts
interface DataProvenance {
  source: "google-play" | "firebase" | "android" | string; // ícone de marca quando bater um dos 3, texto mono como fallback (fontes sem marca própria, ex. "D1")
  freshness?: string;      // ex.: "há 4h", "há 2 dias" — já calculado no backend, não no componente
  costTier?: "gratuito" | "gratuito_com_cota" | "pago_nao_implementado"; // só Firebase
}
```

Regra de exibição: **o indicador de fonte só aparece no card quando a fonte da métrica é diferente
da fonte-padrão já anunciada no `SectionIntro` da categoria**, ou quando `costTier !==
"gratuito"` (cota e custo pago sempre aparecem — é o requisito que não pode sumir). Card com fonte
igual à padrão da categoria fica limpo, sem indicador repetido.

### Ícone de marca em vez de label de texto (ajuste 2026-07-24, pedido do Luiz)

Onde a fonte é Google Play, Firebase ou Android, o indicador de proveniência é o **ícone/logo
oficial da marca**, não texto (`"Fonte: Google Play"` etc.) — o ícone já é o identificador visual,
sem precisar de rótulo ao lado. Texto mono (como hoje) continua sendo o fallback só para fontes sem
marca própria (ex.: `D1`, `Worker`).

- **Componente novo `SourceMark`** (`components/ui/`) — recebe `source: "google-play" | "firebase"
  | "android" | string`, renderiza o SVG de marca correspondente (ou o texto mono de fallback para
  string livre). Ocupa a mesma posição/tamanho onde o badge de `source` já fica hoje no
  `MetricCard` (canto superior direito) — não é um componente visualmente novo na tela, é uma troca
  de conteúdo do que já existe ali.
- **Adaptação clara/escuro:** cada marca (Google, Firebase, Android) publica variante mono/reversa
  própria nas brand guidelines — usar a variante oficial de cada marca para tema escuro (geralmente
  branca/monocromática) e a variante colorida ou monocromática escura oficial para tema claro, nunca
  recolorir o logo manualmente para "combinar" com o tema. Se a marca não publicar uma variante
  oficialmente aprovada para fundo escuro, usar a versão neutra/outline oficial (quando existir) em
  vez de aplicar filtro CSS arbitrário sobre o logo colorido.
- **Guidelines a respeitar (sem aprovação prévia, mas sem deturpar a marca):**
  - **Google Play:** [Google Play Brand Guidelines](https://partnermarketinghub.withgoogle.com/brands/google-play/) — usar o badge/ícone oficial, não recriar o "play button" à mão; respeitar área de proteção mínima e não alterar proporção/cor fora do que a variante oficial permite.
  - **Firebase:** [Firebase brand guidelines](https://firebase.google.com/brand-guidelines) — usar o ícone "chama" oficial (SVG fornecido pelo Google), respeitar espaçamento mínimo, não distorcer nem recolorir fora das variantes aprovadas.
  - **Android:** [Android brand guidelines](https://www.android.com/intl/en_us/branding/) — usar o robozinho verde oficial (cor fixa da marca, não recolorir) ou a variante monocromática quando o contexto pedir baixo contraste de marca.
  - Nenhum dos três ícones deve ser usado como link implícito de "abrir o Google Play/Firebase
    Console" sem isso ser intencional — aqui o uso é puramente de identificação de proveniência do
    dado, não de call-to-action.
  - Assets oficiais (SVG) devem ser baixados da fonte oficial de cada marca e versionados em
    `src/assets/brand-marks/` (novo diretório, não confundir com `brand/` da raiz do monorepo, que é
    a marca **do SignallQ**, não de terceiros) — nunca redesenhar o logo a partir de aproximação
    visual.
- Tamanho fixo pequeno (mesma altura de linha do texto mono atual, ~14–16px), sem fundo/borda
  própria — o ícone já carrega identidade visual suficiente, adicionar chip/borda ao redor seria
  redundante com o que a marca já comunica.

Tooltip (hover) no `SourceMark` expande os campos completos de proveniência (nome da fonte por
extenso, frescor exato com timestamp, timezone) — o ícone sozinho identifica a marca, o tooltip
resolve o detalhe sem precisar de texto permanente na tela.

---

## 4. Estados vazios/indisponíveis

Os épicos exigem diferenciar **zero real** de **indisponível** de **suprimido** — os três não podem
renderizar como a mesma coisa.

| Estado | Quando acontece | Como aparece |
|---|---|---|
| **Zero real** | A fonte respondeu e o valor é genuinamente 0 (ex.: 0 crashes na janela) | Valor `0` normal no `MetricCard`, sem badge de alerta — zero é uma resposta válida, não deve parecer erro |
| **Indisponível** | Fonte fora do ar, erro de API, timeout, credencial ausente/expirada | `EmptyState` (já existe) com mensagem específica por causa — reaproveitar o padrão `source: "bigquery" \| "no_credentials" \| "no_data_yet" \| "error"` que `firebase.types.ts` já usa em `FirebaseCrashlyticsSummary`/`FirebaseAppVersionsResult` — **generalizar esse enum de `source` para todo integrations adapter novo** (Google Play e as categorias novas de Firebase), em vez de cada endpoint inventar o próprio vocabulário de erro |
| **Suprimido** (cota estourada) | API respondeu 429/quota exceeded, ou o backend decidiu não chamar por já ter estourado o teto do dia | Badge `costTier="gratuito_com_cota"` some do estado normal e vira `StatusBadge` dedicado ("Cota diária esgotada", tom âmbar) no lugar do valor — nunca mostrar `0` nem "—" genérico, porque isso pareceria zero real ou indisponível |
| **Não implementado** (fora de escopo, ex. #1345 pago) | Recurso existe no catálogo mas decisão de produto foi não implementar (pago sem cota) | `FeatureComingSoon` (já existe) — comunica "existe, decisão consciente de não fazer agora", diferente de indisponibilidade técnica |

Isso fecha os 4 estados com **zero componentes novos** — é reaproveitamento de `EmptyState`,
`StatusBadge` (+3 variações do item 3) e `FeatureComingSoon`, mais a generalização de um enum de
`source` que já existe parcialmente no lado Firebase.

---

## 5. Escopo de implementação decorrente (não é para agora, é o que nasce daqui)

Quando isso virar issues de execução (Claudete quebra em Feature dentro dos épicos #1341/#1343):

1. `config/navigation.ts` — nova seção "Plataformas", 2 itens, 2 ícones novos no
   `NAVIGATION_ICON_MAP` (`PlayCircle`, `Flame`).
2. Componente novo `CategoryTabs` (`components/ui/`) — pill group reaproveitável, mesmo visual do
   toggle de ambiente do `FilterBar`.
3. Componente novo `SourceMark` (`components/ui/`) — ícone de marca (Google Play/Firebase/Android)
   ou texto mono de fallback; assets oficiais versionados em `src/assets/brand-marks/`.
4. `MetricCard` — prop `source` aceita `string | DataProvenance`, renderizada via `SourceMark`
   (compatível com uso atual em texto puro para fontes sem marca).
5. `StatusBadge` — 3 novas variações de `costTier`.
6. `SectionIntro` — `source` ganha frescor/timezone agregados (ainda string, só template mais rico).
7. Duas Pages novas: `features/google-play/GooglePlayPage.tsx`, `features/firebase/FirebasePage.tsx`
   (+ `Tab.tsx` finos, seguindo o padrão de `errors/`), cada uma orquestrando as categorias do item 2
   deste doc via `CategoryTabs` + os componentes de visualização já mapeados.
8. Categorias "Comportamento" e "Estabilidade" do Firebase **não** ganham componente de dado
   próprio — só um card de atalho que leva para `/product-analytics` e `/errors` respectivamente.
9. Contratos JSON do Camilo (catálogo, ingestão) definem o shape exato de cada endpoint — este
   plano não assume schema de resposta, só a forma de exibição.
10. Categoria "Avaliações" (item 2.3, adição 2026-07-24): componentes novos `RatingDistributionBars`
    e `ReviewCard` (`features/google-play/components/`), 4 valores novos de `StatusBadge`, 1 pill
    group de filtro local, 1 verbete novo no glossário do `TermHint`. Depende do contrato de dados
    da Claudete (tabela de reviews) incluir `reviewerLanguage` e `device` — hoje ausentes em
    `GooglePlayReviewSummary` (`src/integrations/google-play/googlePlay.types.ts`), reportar como
    pendência de contrato antes de implementar a coluna Idioma/Dispositivo.

---

## 6. Fora de escopo deste plano

- Recursos pagos sem cota (#1345) — não desenhar tela para eles além do estado "Não implementado"
  do item 4.
- Qualquer mudança em `signallq-admin-worker` — é frente do Camilo.
- Prototipagem em Figma — não é hábito do projeto (Claude Design é a ferramenta, ver
  `.claude/CLAUDE.md` do repo).

---

## 7. Protótipos visuais (evidência do item 2.1)

Três telas em HTML estático, fiéis aos tokens reais do Admin (`src/index.css` — Roboto/Roboto Mono,
paleta dark, `--radius-card`, `--success`/`--attention`/`--error`), para validar hierarquia antes de
virar issue de implementação. Abrir direto no navegador:

- `docs/product/prototypes/google-play-qualidade.html` — categoria Qualidade (Android Vitals):
  crash-free sessions como âncora (hero verde, tudo OK no cenário), série temporal full-width com
  limiar tracejado e ponto fora do padrão destacado, ranking de Top ANR/Top Crash, tabela de versão
  em baixo peso.
- `docs/product/prototypes/firebase-estabilidade.html` — categoria Estabilidade (Crashlytics):
  mesma estrutura de hierarquia, mas hero em âmbar porque o cenário tem issue crítica aberta — mostra
  que a cor de status é real, não fixa por categoria. Atalho para `/errors` desenhado como linha fina
  tracejada, deliberadamente subordinado ao conteúdo de dado.
- `docs/product/prototypes/plataformas-visao-geral.html` — faixa de saúde por categoria (achado do
  item 2.1.6), Google Play (6 categorias) e Firebase (4 categorias) lado a lado, com callout de
  atenção quando alguma categoria foge do esperado.

Nota de ferramenta: gerados como HTML autocontido (não via Claude Design) para review rápido nesta
sessão — refazer/ajustar no Claude Design antes de qualquer handoff de implementação para o Camilo,
mantendo os hex/tokens exatamente como estão aqui (não são paleta nova, é reprodução literal do
tema dark existente).
