# Decisão — três seções reais do Console incorporadas ao protótipo To-Be MD3 (2026-07-18)

**Responsável:** Lia
**Status:** ativo — decisão de design tomada e aplicada no cache local do protótipo; **push real
para o Claude Design ainda pendente** (ver "Pendência de execução" no fim deste documento)
**Última validação:** 2026-07-18
**Fonte de verdade:** projeto Claude Design `SignallQ Design System`
(`e77ea465-291f-4bf5-930c-a267680da04e`), pasta `templates/signallq-admin-fluxo-tobe-md3/`
**Contexto:** decisão do Luiz em 2026-07-18 — depois do Camilo aplicar
`PLANO_APLICACAO_TOBE_CONSOLE_2026-07-17.md` (PR #1110, mergeada), ficou confirmado que três seções
do Console têm dado real em produção que o protótipo simplesmente não previu. Não é código morto —
são seções ativas, com serviço, endpoint e dado real por trás. Decisão: manter as três no código,
**o protótipo é quem precisa alcançar o código**, não o contrário. Lia tem liberdade de decidir
onde e como cada seção aparece — não é para colar o layout do React 1:1, é para desenhar no formato
MD3 correto.

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

## 3. Ferramentas (`/tools`) — "Uso do App — detalhamento"

**Código real:** `SignallQ Admin/src/features/tools/ToolsPage.tsx`, `ProductAnalyticsDetailSection`
(linha 504-568). Reúne 4 componentes: `MostUsedFeaturesTable`, `ScreenNavigationPanel`,
`FeatureCrashTable`, `RetentionPanel` — drill-down avançado do que a tela "Uso do App"
(`/product-analytics`) já resume em KPIs.

**Decisão de posição:** inserida entre "Erros — lista detalhada" e "Saúde do sistema —
detalhamento", preservando a ordem relativa que o código real usa entre essas duas seções
(`ErrorsDetailSection` → ... → `SystemHealthDetailSection`, com `ProductAnalyticsDetailSection` no
meio). O protótipo hoje só tinha 4 seções (Diagnósticos, Erros, Saúde do sistema, Configurações);
esta decisão não mexeu na seção "IA & Custos — detalhamento" que também existe no código
(`AiCostDetailSection`, linha 451) — está fora do escopo desta tarefa (a Claudete listou
especificamente 3 componentes, não essa 5ª seção do Ferramentas). Fica registrado aqui como
pendência conhecida, não decisão tomada.

**Decisão de formato — a mais retrabalhada das três, porque o React usa tokens errados:**
os 4 componentes React (`MostUsedFeaturesTable.tsx`, `ScreenNavigationPanel.tsx`,
`FeatureCrashTable.tsx`, `RetentionPanel.tsx`) usam classes Tailwind hardcoded (`zinc-900`,
`emerald-400`, `red-400`, `amber-500`, `indigo-400`) em vez dos tokens do design system
(`var(--text-primary)`, `var(--success)`, `var(--error)`, `var(--attention)`, `var(--primary)`).
**Não copiei isso 1:1 no protótipo** — remapeei cada cor para o token MD3 tonal equivalente
(`#E6E0E9` text-primary-dark, `#7DDB93` success-dark, `#FFB4AB` error-dark, `#FFB955`
attention-dark, `#CFBCFF` primary-dark), porque o protótipo é a fonte de verdade visual e não deve
herdar uma inconsistência do código. Isso é uma dívida de implementação para reportar ao Camilo (ver
seção "Dívida encontrada" abaixo), não uma escolha de design.

Estrutura visual escolhida (4 blocos empilhados, full-width, dentro da subseção):
1. **Tabela** "Engajamento por funcionalidade" — mesma grade de colunas do código (Função / Sessões
   / Conclusão / Falha / Tendência), reduzida a 5 colunas em vez das 7 do React (cortei Usuários
   Únicos e Tempo Médio do preview — não é perda de dado real, é curadoria de amostra de protótipo,
   igual ao padrão de 2-3 linhas de exemplo já usado no resto do `Md3ToolsContent.dc.html`).
2. **Grade de 2 cards** "Navegação e fluxo de telas" — versão condensada do card por tela do React
   (Views + Taxa de saída, barra de progresso), mesma lógica visual, menos verbosidade (o React
   mostra 4 métricas + "próxima tela mais comum"; o protótipo mostra 2 métricas — suficiente para
   comunicar o padrão visual sem replicar cada pixel do componente real).
3. **Tabela** "Crashes por funcionalidade" — badge de severidade com as 3 cores semânticas
   (crítico/atenção/confiável), mesmo padrão de badge `border-radius:999px` já usado no resto do
   protótipo (ex.: badge "2 problemas" da seção Diagnósticos).
4. **Card** "Contexto do cohort de retenção" — 2 stat-cards (Retenção D30, Tempo médio de sessão) +
   nota de proxy de inatividade, mesmo padrão de card-dentro-de-card já usado no protótipo.

**Por que não virou card "em breve":** dado real, 4 serviços reais (`productAnalyticsService`)
retornando dado de produção. Nenhuma razão para tratar como stub.

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

## Pendência de execução — IMPORTANTE

As três seções foram desenhadas e **já aplicadas nos arquivos de conteúdo do protótipo salvos
localmente** em
`C:\Users\luizg\AppData\Local\Temp\claude\C--Projetos-SignallQ\5d8219ba-ac18-4262-a12f-7242ebb3ae31\scratchpad\admin-tobe-md3\`:
- `Md3AiCostContent.dc.html` — card "Quota do free tier Gemini" adicionado.
- `Md3SystemHealthContent.dc.html` — card "Uso do free tier Cloudflare" adicionado.
- `Md3ToolsContent.dc.html` — subseção "Uso do App — detalhamento" adicionada.

**Essa sessão da Lia não teve a tool `DesignSync` carregada** (ferramentas disponíveis nesta
invocação: Read, Grep, Glob, Bash, Edit, Write, Agent — sem `DesignSync`), então **não foi possível
executar o `write_files` real contra o projeto Claude Design** (`e77ea465-291f-4bf5-930c-a267680da04e`).
O conteúdo acima está pronto para aplicar (`list_files`/`read` → `finalize_plan` → `write_files`,
fluxo já documentado em sessões anteriores), mas falta uma sessão da Lia com `DesignSync`
disponível para de fato subir esses 3 arquivos ao protótipo remoto. Não estou declarando "protótipo
atualizado" sem essa etapa ter rodado de verdade — só a decisão de design e o conteúdo estão prontos.
