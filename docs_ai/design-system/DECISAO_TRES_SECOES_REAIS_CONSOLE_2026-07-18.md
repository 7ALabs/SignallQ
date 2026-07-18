# Decisão — três seções reais do Console incorporadas ao protótipo To-Be MD3 (2026-07-18)

**Responsável:** Lia
**Status:** CONCLUÍDO — decisão aplicada E push verificado no Claude Design em 2026-07-18. Os 5
arquivos de conteúdo estão presentes no projeto remoto `e77ea465` (conferidos um a um via
`DesignSync get_file` pela Claudete). NÃO repetir a alegação de "push pendente" — está desatualizada.
**Última validação:** 2026-07-18 (versão 2 — correção de redistribuição temática)
**Fonte de verdade:** projeto Claude Design `SignallQ Design System`
(`e77ea465-291f-4bf5-930c-a267680da04e`), pasta `templates/signallq-admin-fluxo-tobe-md3/`
**Contexto:** decisão do Luiz em 2026-07-18 — depois do Camilo aplicar
`PLANO_APLICACAO_TOBE_CONSOLE_2026-07-17.md` (PR #1110, mergeada), ficou confirmado que três seções
do Console têm dado real em produção que o protótipo simplesmente não previu. Não é código morto —
são seções ativas, com serviço, endpoint e dado real por trás. Decisão: manter as três no código,
**o protótipo é quem precisa alcançar o código**, não o contrário. Lia tem liberdade de decidir
onde e como cada seção aparece — não é para colar o layout do React 1:1, é para desenhar no formato
MD3 correto.

## Versão 2 (2026-07-18) — correção de redistribuição temática

Na versão 1 deste documento (seção 3 abaixo), a subseção "Uso do App — detalhamento" foi colocada
inteira dentro do protótipo da tela **Ferramentas** (`Md3ToolsContent.dc.html`) — pelo motivo
errado: era ali que o código React (`ToolsPage.tsx`) mantinha o bloco fisicamente. O Luiz corrigiu:
o critério certo é o **tema real de cada pedaço de conteúdo**, não onde o React o hospeda
fisicamente. `ToolsPage.tsx` importa componentes de `features/product-analytics/components/`
dentro de si — isso não torna "Uso do App" um tema de Ferramentas, só significa que o React
compõe o drill-down ali.

Correção aplicada:
- **Removida** de `Md3ToolsContent.dc.html` a subseção "Uso do App — detalhamento" inteira
  (tabela de engajamento, cards de navegação, tabela de crashes, card de retenção). Ferramentas
  voltou a ter só as 4 seções originais: Diagnósticos, Erros, Saúde do sistema, Configurações —
  esse é o padrão intencional pré-existente (drill-down bruto de temas já resumidos em telas
  próprias), não deve ganhar uma 5ª seção "Uso do App" só porque o React guarda o código lá.
- **`MostUsedFeaturesTable`** (tabela "Engajamento por funcionalidade"), **`ScreenNavigationPanel`**
  (cards "Navegação entre telas") e **`RetentionPanel`** (card "Contexto do cohort de retenção") —
  tema real: **Uso do App** — movidos para `Md3ProductAnalyticsContent.dc.html`
  (rota `/product-analytics`), como complemento às seções que já existiam lá (não como
  substituição de placeholder "em breve" — "Funcionalidade mais usada"/"Retenção" já eram
  visualizações reais e distintas, mantidas; o card de retenção D30/tempo-de-sessão passou a viver
  dentro do mesmo card "Retenção" como complemento textual, não duplicado em card separado).
- **`FeatureCrashTable`** (tabela "Crashes por funcionalidade") — tema real: **Problemas &
  Incidentes** — movida para `Md3ErrorsContent.dc.html` (rota `/errors`), como bloco novo após
  "Top Crashes" (distinto de "Erros por tela", que continua "em breve" — crash por funcionalidade
  e erro por tela são recortes diferentes do mesmo domínio).
- Decisão explícita: **não** adicionei uma versão drill-down enxuta de "Uso do App"/"Crashes" em
  Ferramentas (opção que o Luiz deixou como julgamento meu, não obrigatória). Motivo: o próprio
  erro desta correção nasceu de duplicar conteúdo por onde o código mora — inflar Ferramentas de
  novo, mesmo que "enxuto", reintroduz o mesmo risco que acabei de corrigir. Ferramentas segue
  fiel ao padrão original.

Seções 1 e 2 abaixo (IA & Custos, Saúde do Sistema) **não mudaram** — já estavam no tema certo
desde a versão 1, releitura do código confirmou.

---

## 1. IA & Custos (`/ai-cost`) — `GeminiQuotaCard` (GH#884)

**Código real:** `SignallQ Admin/src/features/ai-cost/AiCostPage.tsx` (linha 140) +
`SignallQ Admin/src/features/ai-cost/components/GeminiQuotaCard.tsx`. Mostra 3 métricas de quota do
free tier Gemini (RPM, TPM, RPD) — `used`/`limit`/`percentage`, independente de period/environment
(é sempre "agora"). Estado honesto "Não disponível" com motivo quando o teto não está configurado —
nunca número fabricado.

**Decisão de posição:** card full-width entre "Orçamento mensal de IA" e a grade de composição
(donut de custo por provedor + "Custo por funcionalidade"). Segue exatamente a ordem já usada no
código real (`AiCostPage.tsx:136-155`) — é a posição correta porque agrupa os dois cards de
"orçamento/teto" antes da composição analítica.

**Decisão de formato:** card no mesmo padrão visual do card "Orçamento mensal de IA" já existente no
protótipo (`background:#2B2831`, `border-radius:12px`, `padding:18px`, título uppercase 11px
`#CAC4D0`) — não um card novo inventado. Dentro dele, 3 linhas de "quota row": label + valor
`usado/limite` + percentual, com barra de progresso fina (6px, mais fina que a barra de 10px do
orçamento mensal — sinaliza hierarquia: é uma métrica secundária/nested, não a principal da tela).
Cor da barra e do percentual segue a mesma lógica semântica já implementada no componente React
(`barColor()`): verde `#7DDB93` (`success-dark`) abaixo de 80%, âmbar `#FFB955` (`attention-dark`)
de 80% a 99%, vermelho `#FFB4AB` (`error-dark`) a partir de 100% — reaproveitei a regra exata do
Camilo, não inventei uma nova. Linha "Não disponível" (RPD, sem teto configurado) usa o mesmo texto
cinza-terciário + motivo, igual ao padrão "Não disponível" já usado em outras métricas do protótipo
(ex.: KPIs de Saúde do Sistema).

**Por que não virou card "em breve":** a feature já está implementada e retornando dado real em
produção (GH#884 fechada) — usar `Md3ComingSoonCard` seria desonesto, esse componente é reservado
para métricas que o worker ainda não expõe (ex.: "Custo por funcionalidade", que continua "em breve"
de verdade). A única linha em estado "Não disponível" (RPD) é honesta porque reflete um teto que
realmente não está configurado hoje, não uma feature ausente.

---

## 2. Saúde do Sistema (`/system-health`) — `CloudflareUsagePanel` (GH#883)

**Código real:** `SignallQ Admin/src/features/system-health/SystemHealthPage.tsx` (linha 259) +
`.../components/CloudflareUsagePanel.tsx`. Mostra uso vs. teto do free tier Cloudflare: Workers
requests/dia, D1 rows lidas/escritas por dia, D1 storage total, Workers AI Neurons/dia (estimado,
GH#921 — sem dataset na GraphQL Analytics API, calculado a partir de tokens reais).

**Decisão de posição:** card full-width como último elemento da tela, depois da grade
gráfico-de-latência + status-dos-serviços. Mesma posição do código real (comentário no próprio
componente: "Card próprio porque é um dado de infra/custo, não um check de disponibilidade" —
concordo com essa separação, por isso mantive isolado em vez de misturar com os KPIs de
disponibilidade do topo).

**Decisão de formato:** mesmo padrão de "quota row" com barra fina de 6px usado no Gemini (item 1) —
consistência entre as duas telas que mostram "uso vs. teto de free tier" (IA & Custos e Saúde do
Sistema), mesmo padrão visual e mesma lógica semântica de cor. 5 linhas, uma por recurso, na mesma
ordem do array `RESOURCE_LABELS` do componente React. A linha "Workers AI Neurons" leva o rótulo
"(estimado)" em cinza-terciário, igual ao código.

**Por que não virou card "em breve":** mesmo raciocínio do item 1 — dado real, endpoint real (GH#883
fechada). "Latência P95 · 14 dias" continua com `Md3ComingSoonCard` no protótipo (isso já estava
correto antes desta sessão — só o `CloudflareUsagePanel` estava faltando).

---

## 3. Uso do App (`/product-analytics`) + Problemas & Incidentes (`/errors`) — os 4 componentes de "Uso do App — detalhamento" do React

**Substitui a seção 3 original desta decisão** (que colocava tudo em Ferramentas por engano — ver
"Versão 2" no topo do documento).

**Código real:** `SignallQ Admin/src/features/tools/ToolsPage.tsx`, `ProductAnalyticsDetailSection`
(linha 504-568) importa e renderiza 4 componentes cuja pasta física real é
`SignallQ Admin/src/features/product-analytics/components/`: `MostUsedFeaturesTable.tsx`,
`ScreenNavigationPanel.tsx`, `FeatureCrashTable.tsx`, `RetentionPanel.tsx`. O fato de `ToolsPage.tsx`
importar e compor esses 4 componentes **não os torna tema de Ferramentas** — é só onde o React monta
o drill-down. Cada componente tem tema próprio, resolvido por conteúdo:

- `MostUsedFeaturesTable` (tabela "Engajamento por funcionalidade": Speedtest/Diagnóstico IA —
  sessões/conclusão/falha/tendência) → tema **Uso do App**.
- `ScreenNavigationPanel` (cards "Navegação entre telas": views/saída por tela — Início/Velocidade)
  → tema **Uso do App**.
- `RetentionPanel` (cards "Contexto do cohort de retenção": Retenção D30, tempo médio de sessão) →
  tema **Uso do App**.
- `FeatureCrashTable` (tabela "Crashes por funcionalidade": Speedtest 3 crashes CRÍTICO,
  Histórico 0 crashes CONFIÁVEL) → tema **Problemas & Incidentes**.

**Decisão de posição:**
- Em `Md3ProductAnalyticsContent.dc.html`: tabela "Engajamento por funcionalidade" inserida como
  bloco novo full-width logo após a grade "Retenção" + "Dispositivos mais ativos" (mantém a leitura
  de cima pra baixo: KPIs → funcionalidade mais usada/funil → retenção/dispositivos → engajamento
  detalhado). Card "Contexto do cohort de retenção" (D30 + tempo médio de sessão + nota de proxy de
  inatividade) inserido **dentro** do card "Retenção" já existente, como complemento abaixo do
  gráfico de barras D1/D7/D30 — mesmo tema, mesmo card, evita duplicar cabeçalho "Retenção".
  Bloco "Navegação entre telas" inserido como última seção da tela (2 cards por tela, mesmo
  conteúdo real do React: Início e Velocidade).
- Em `Md3ErrorsContent.dc.html`: tabela "Crashes por funcionalidade" inserida como bloco novo
  full-width logo após "Top Crashes" — mesma vizinhança temática (ambos tratam de crash), mas
  descrição deixa claro que é um recorte diferente ("por funcionalidade" vs. "por assinatura de
  stack trace" do Top Crashes) e que não substitui "Erros por tela" (que continua `Md3ComingSoonCard`
  porque errros-por-tela genuinamente não tem dado real hoje — "crash por funcionalidade" tem).

**Decisão de formato — mesma correção de tokens da versão 1:** os 4 componentes React usam classes
Tailwind hardcoded (`zinc-900`, `emerald-400`, `red-400`, `amber-500`, `indigo-400`) em vez dos
tokens do design system. Mantive o remapeamento já feito na versão 1 para o token MD3 tonal
equivalente (`#E6E0E9` text-primary-dark, `#7DDB93` success-dark, `#FFB4AB` error-dark, `#FFB955`
attention-dark, `#CFBCFF` primary-dark) — dívida de implementação segue registrada para o Camilo
(ver seção "Dívida encontrada" abaixo), sem mudança nesta versão 2.

Ferramentas (`Md3ToolsContent.dc.html`) voltou às 4 seções originais (Diagnósticos, Erros, Saúde do
sistema, Configurações) — decisão explícita de **não** adicionar uma 5ª seção "Uso do App" ali, nem
mesmo em versão enxuta (opção que o Luiz deixou a critério meu). Ver "Versão 2" no topo para o
motivo.

---

## Dívida encontrada (não corrigida por mim — fora do meu escopo de design)

Os 4 componentes React de "Uso do App — detalhamento"
(`MostUsedFeaturesTable.tsx`, `ScreenNavigationPanel.tsx`, `FeatureCrashTable.tsx`,
`RetentionPanel.tsx`, todos em `SignallQ Admin/src/features/product-analytics/components/`) usam
cores Tailwind hardcoded (`zinc-*`, `emerald-*`, `red-*`, `amber-*`, `indigo-*`) em vez dos tokens
CSS custom properties do design system (`var(--text-primary)`, `var(--success)`, `var(--error)`,
`var(--attention)`, `var(--primary)`) usados no resto do Console. Isso quebra dark/light mode
consistente (as cores fixas não reagem ao tema) e diverge do padrão dos outros ~9 componentes de
tabela/painel do Console. Não é código morto nem tela nova — é dívida de token, mesma classe de
achado que a seção 4 da regra de higiene do repositório trata como "correção oportunista" se
pequena, ou issue se ampla. Como são 4 arquivos com múltiplas ocorrências cada, não é uma correção
trivial de uma linha — recomendo o Camilo abrir/registrar issue de retokenização desses 4 arquivos
(escopo: trocar classes Tailwind de cor fixa pelos tokens `var(--*)` equivalentes, sem mudar
estrutura/dado). Não abri a issue eu mesma porque não tenho certeza de que já não existe uma
equivalente — Claudete ou Camilo, verificar antes.

---

## Pendência de execução — RESOLVIDA (verificado 2026-07-18)

> **Atualização (Claudete, 2026-07-18):** o push FOI feito. Conferi via `DesignSync get_file` que os
> 5 arquivos de conteúdo no projeto remoto `e77ea465` (pasta `templates/signallq-admin-fluxo-tobe-md3/`)
> já contêm as seções decididas: `Md3AiCostContent` (Quota Gemini), `Md3SystemHealthContent` (Uso
> Cloudflare), `Md3ProductAnalyticsContent` (Engajamento por funcionalidade + Navegação entre telas +
> cohort de retenção D30), `Md3ErrorsContent` (Crashes por funcionalidade) e `Md3ToolsContent` (de
> volta às 4 seções originais, sem "Uso do App"). O texto abaixo é histórico.

As seções foram desenhadas e **já aplicadas nos arquivos de conteúdo do protótipo salvos
localmente** em
`C:\Users\luizg\AppData\Local\Temp\claude\C--Projetos-SignallQ\5d8219ba-ac18-4262-a12f-7242ebb3ae31\scratchpad\admin-tobe-md3\`:
- `Md3AiCostContent.dc.html` — card "Quota do free tier Gemini" adicionado (versão 1, sem mudança
  na versão 2).
- `Md3SystemHealthContent.dc.html` — card "Uso do free tier Cloudflare" adicionado (versão 1, sem
  mudança na versão 2).
- `Md3ToolsContent.dc.html` — **versão 2:** subseção "Uso do App — detalhamento" (adicionada na
  versão 1) foi **removida**; arquivo voltou às 4 seções originais.
- `Md3ProductAnalyticsContent.dc.html` — **versão 2:** tabela "Engajamento por funcionalidade" e
  bloco "Navegação entre telas" adicionados como seções novas; card "Contexto do cohort de
  retenção" (D30 + tempo médio de sessão + nota de proxy) adicionado dentro do card "Retenção"
  existente.
- `Md3ErrorsContent.dc.html` — **versão 2:** tabela "Crashes por funcionalidade" adicionada como
  seção nova após "Top Crashes".

**Essa sessão da Lia não teve a tool `DesignSync` carregada de novo** (tentativa via `ToolSearch`
com `select:DesignSync` e depois busca livre por `DesignSync` — ambas sem resultado), então **não
foi possível executar o `write_files` real contra o projeto Claude Design**
(`e77ea465-291f-4bf5-930c-a267680da04e`) nesta sessão também. O conteúdo acima está pronto para
aplicar (`list_files`/`read` → `finalize_plan` → `write_files`, fluxo já documentado em sessões
anteriores), mas segue faltando uma sessão da Lia com `DesignSync` disponível para de fato subir os
5 arquivos (3 da versão 1 + 2 novos da versão 2) ao protótipo remoto. Não estou declarando
"protótipo atualizado" sem essa etapa ter rodado de verdade — só a decisão de design e o conteúdo
estão prontos, local, aguardando push real (mesma situação da versão 1, ainda sem resolução).
