# Plano de aplicação — SignallQ Console vs. protótipo To-Be MD3 (correção de fonte, 2026-07-17)

**Responsável:** Lia
**Status:** ativo — plano, não implementação
**Última validação:** 2026-07-17
**Fonte de verdade confirmada:** projeto Claude Design `SignallQ Design System`
(`e77ea465-291f-4bf5-930c-a267680da04e`), pasta `templates/signallq-admin-fluxo-tobe-md3/`,
arquivo índice `SignallqAdminMd3ToBe.dc.html`. Confirmado via `DesignSync` (`get_project` +
`list_files` + `get_file`) pela Claudete em 2026-07-17, conteúdo bruto salvo em
`C:\Users\luizg\AppData\Local\Temp\claude\...\scratchpad\admin-tobe-md3\` (ver `_INDICE.md` nesse
diretório) e lido integralmente por mim antes de escrever este plano.
**Contexto:** o redesign anterior do Console (2026-07-16, ver `FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md`)
usou um mirror local em `signallq-admin-md3-tobe` (pasta local desatualizada, sem "fluxo" no nome).
O Luiz apontou hoje à noite que a referência correta é a pasta `signallq-admin-fluxo-tobe-md3` no
projeto remoto. **Achado relevante:** comparando os arquivos `Md3*Content.dc.html` puxados agora via
DesignSync com os componentes já implementados, o conteúdo de produto (overlines, KPIs, hierarquia,
copy) é o **mesmo** documento — a maior parte das 10 telas já está estruturalmente alinhada. A
correção real que este plano endereça é mais cirúrgica do que "redesign errado do zero": navegação
agrupada, um caso de escopo extra não previsto no protótipo, e um ponto de tipografia — não uma
readequação de todas as 10 telas. Isso não diminui a obrigação de aplicar; só evita pânico de escopo.
**Substitui/complementa:** não substitui `FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md` (tokens de
cor continuam válidos, verificados de novo aqui) — complementa com achados de estrutura/navegação
não cobertos naquela passagem.

---

## Achado transversal #1 — Navegação: agrupada no protótipo, flat no código (CONFIRMADO, maior divergência real)

**Fonte primária:** `Md3NavDrawer.dc.html` e `Md3NavRail.dc.html` (idênticos entre si, mesmo
`NAV.map`), linha "NAV.map oficial":

```
1. Centro de Controle — /overview (sem grupo)
2. App — Uso do App /product-analytics, Releases & Qualidade /app-versions, Problemas & Incidentes /errors (badge "3")
3. Rede & Operadora — Diagnósticos /diagnostics, Redes & Provedores /networks
4. Custos & Sistema — IA & Custos /ai-cost, Saúde do Sistema /system-health
5. Administração — Configurações /settings, Ferramentas /tools
```

**Código real** (`SignallQ Admin/src/config/navigation.ts:33-49`): uma única seção `label: ""`
(sem agrupamento), 10 itens em ordem flat: Centro de Controle, Diagnósticos, Problemas & Incidentes,
Redes & Provedores, Uso do App, Releases & Qualidade, IA & Custos, Saúde do Sistema, Configurações,
Ferramentas. O comentário no próprio arquivo (linhas 20-32) cita "GH#552 (Fase 1) — menu redesenhado
... 9 entradas, sem agrupamento" — comentário desatualizado (lista real tem 10 itens) e reflete uma
decisão anterior que o protótipo correto contradiz.

**O que muda:** `NAVIGATION_SECTIONS` precisa virar 5 seções (1 sem label + 4 com label: "App", "Rede
& Operadora", "Custos & Sistema", "Administração"), na ordem e agrupamento do `NAV.map` acima. Isso
afeta renderização em `Sidebar.tsx` (já itera `NAVIGATION_SECTIONS` e já sabe renderizar headers de
grupo quando `section.label` existe — não precisa mudar lógica, só o dado) e `NavRail.tsx` (usa
`NAVIGATION_SECTIONS.flatMap` — continua funcionando sem mudança de código, já que Nav Rail não
mostra labels de grupo, só ícones). `BottomNav.tsx` usa a mesma fonte para o sheet "Mais" — a ordem
dos itens no sheet também muda de acordo.

**Arquivos React afetados:**
- `SignallQ Admin/src/config/navigation.ts` — reestruturar `NAVIGATION_SECTIONS` (mudança de dado, não de lógica).
- `SignallQ Admin/src/components/layout/Sidebar.tsx` — nenhuma mudança de código esperada (já lê `section.label`), só validar renderização visual com o dado novo.
- `SignallQ Admin/src/components/layout/NavRail.tsx` — nenhuma mudança de código esperada (usa `flatMap`), validar ordem visual.
- `SignallQ Admin/src/components/layout/BottomNav.tsx` — nenhuma mudança de código esperada, validar ordem do sheet "Mais".

**Risco/dependências:** baixo risco técnico (mudança de dado, componentes já leem a estrutura
corretamente), mas é a mudança com **maior superfície visual** — afeta as 3 formas de navegação
(Sidebar desktop, NavRail tablet, BottomNav mobile) em todas as 10 telas simultaneamente. Deve ser a
**primeira mudança aplicada**, isolada em commit próprio, porque qualquer screenshot/QA de tela
individual feito antes dessa correção vai comparar contra uma sidebar errada.

**Ordem sugerida:** 1ª (base para todo o resto — telas individuais não têm nav própria, dependem do layout compartilhado).

---

## Achado transversal #2 — `DESIGN.md` cita a fonte antiga, precisa apontar para a pasta confirmada hoje

`SignallQ Admin/DESIGN.md` linha 4: `status: realinhado ao protótipo md3-tobe (fonte de verdade) em
2026-07-16 — ver docs_ai/design-system/FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md`. Esse status não
cita a pasta `signallq-admin-fluxo-tobe-md3` confirmada hoje via DesignSync — cita só "md3-tobe"
genericamente, que é ambíguo com o mirror local desatualizado (`signallq-admin-md3-tobe`, sem
"fluxo") que já se provou impreciso. Mesmo o conteúdo de produto sendo majoritariamente o mesmo (ver
nota no topo deste documento), a citação da fonte devia ser exata para não repetir a confusão desta
sessão. **Não vou corrigir isso sozinha** (fora do meu escopo de UI/layout e a instrução da tarefa é
só sinalizar) — Camilo ou Rhodolfo atualiza a linha de status apontando para este documento e para o
projeto/pasta exatos confirmados acima.

## Achado transversal #3 — `PRODUCT.md` não existe

`SignallQ Admin/PRODUCT.md` é citado em `.claude/CLAUDE.md` ("formato impeccable, North Star
'The Operator's Console'") mas **não existe no repo** — só `DESIGN.md`. Divergência de documentação,
não vou criar o arquivo sozinha (decisão de conteúdo de produto, não de design visual). Sinalizando
para a Claudete decidir se cria ou corrige a referência no `CLAUDE.md`.

---

## Telas — comparação 1:1

### 00 — Login (`/login`)

**Fonte:** `Md3LoginContent.dc.html` + `Md3LoginForm.dc.html`.
**Código real:** `SignallQ Admin/src/auth/LoginPage.tsx`.

**(a) O que muda:**
- Protótipo: formulário **sem card/container** — inputs e botão direto sobre o background da tela
  (`#141019` dark / `#FEF7FF` light), logo 64px + título 26px + subtítulo, botão "Entrar" pill
  **27px de radius** (altura 54px ÷ 2), placeholder de senha com 10 pontos (`••••••••••`), tem fluxo
  "Esqueci minha senha" com view separada dentro do mesmo componente (`Md3LoginForm`: `isLoginView`
  / `isForgotView`, com botão voltar).
- Código real: logo 64px (`icon-192.png`, ok), mas o formulário está **dentro de um card** com borda
  (`rounded-[var(--radius-card)] p-6` + `border: 1px solid var(--sq-border)` + background
  `--sq-bg-elevated`) — isso não existe no protótipo. Botão usa `rounded-[var(--radius-button)]`
  (token genérico, precisa validar se resolve pro mesmo 27px ou pro radius padrão de botão do
  sistema, que pode ser menor). **Não existe fluxo "Esqueci minha senha"** no código — só
  email/senha/erro/submit.
- Placeholder de senha no código usa `••••••••••••` (12 pontos) vs protótipo `••••••••••` (10) —
  cosmético, mas puramente objetivo de comparar 1:1.

**(b) Arquivos afetados:** `SignallQ Admin/src/auth/LoginPage.tsx` (remover wrapper de card, ajustar
radius do botão, adicionar fluxo "Esqueci minha senha" com view alternada).

**(c) Risco/dependências:** baixo risco técnico, mas "Esqueci minha senha" é fluxo novo — precisa de
decisão de produto sobre o que o botão faz de fato (o protótipo só mostra a segunda tela do
formulário, não define o endpoint/comportamento real de recuperação). Camilo não deve inventar
endpoint — perguntar à Claudete se recuperação de senha é escopo real deste momento ou só o toggle
visual sem ação de fato (like existing "Em breve").

**(d) Ordem sugerida:** 2ª — depois da navegação, porque Login é tela isolada (sem Sidebar/NavRail),
pode ser feita em paralelo com qualquer outra.

---

### 01 — Centro de Controle (`/overview`)

**Fonte:** `Md3DashboardContent.dc.html` (desktop) / `Md3DashboardContentMobile.dc.html` (mobile, stub — ver nota do índice).
**Código real:** `SignallQ Admin/src/features/overview/OverviewPage.tsx` + `components/AppSection.tsx`, `NetworkOverviewSection.tsx`, `AiCostSummaryCard.tsx`, `RecentAlertsPanel.tsx`.

**(a) O que muda:**
- Composição já está correta e documentada no próprio código (`OverviewPage.tsx:44-51,168-214`):
  overline "CENTRO DE CONTROLE" → H1 "Visão geral do SignallQ" → seção App (KPIs + gráfico sessões +
  donut) → seção Rede & Operadora → card de Custo de IA isolado → Alertas Recentes. Isso bate 1:1
  com a ordem do protótipo.
- **Única divergência real confirmada:** peso da fonte do H1. Protótipo: `font-weight:500` (linhas
  15 e 124 do `Md3DashboardContent.dc.html`). Código: `font-sans font-bold` (700, `OverviewPage.tsx:183`).
  Isso é inconsistente com o resto do sistema, que usa peso 500 para H1 de seção (ver Diagnósticos,
  Erros etc. via `SectionIntro`, não confirmado linha a linha aqui mas é o padrão declarado em
  `DESIGN.md`).
- KPIs, cores dos gráficos e ordem batem com o que já foi confirmado em
  `FASE1_TOKENS_CONSOLE_MD3_TOBE_2026-07-16.md` — não repetir esse trabalho.

**(b) Arquivos afetados:** `SignallQ Admin/src/features/overview/OverviewPage.tsx` (linha 182-187,
trocar `font-bold` por peso 500 — mesma classe/token usado nas outras 9 telas via `SectionIntro`, se
existir; senão criar token de peso compartilhado ao invés de hardcode local).

**(c) Risco/dependências:** risco mínimo, mudança de uma linha. Nenhuma dependência de outras telas.

**(d) Ordem sugerida:** 3ª — correção pontual e independente, pode entrar em qualquer commit.

---

### 02 — Diagnósticos (`/diagnostics`)

**Fonte:** `Md3DiagnosticsContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/diagnostics/DiagnosticsPage.tsx`.

**(a) O que muda:** nada de estrutural — o próprio código já documenta paridade com o mockup
(comentário `GH#781`, linhas 20-25: "sec-diagnostics no mockup só tem SectionIntro + KPIs + o par
'Diagnósticos executados · 14 dias' / 'Motivos de falha'. Sem filtros locais... sem tabela de
sessões — tudo isso não está no mockup desta tela"). Isso bate exatamente com o que confirmei linha
a linha no `Md3DiagnosticsContent.dc.html`: 4 KPIs (Diagnósticos 7d, Taxa de sucesso — Em breve,
Duração média — Em breve, Sessões ativas agora) + par de cards (série temporal + motivos de falha).
**Sem divergência encontrada.**

**(b) Arquivos afetados:** nenhum.

**(c) Risco/dependências:** nenhum.

**(d) Ordem sugerida:** não precisa de ordem — já alinhado, só confirmar depois da correção de nav (achado #1) que o layout ao redor não quebrou nada.

---

### 03 — Problemas & Incidentes (`/errors`)

**Fonte:** `Md3ErrorsContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/errors/ErrorsPage.tsx`.

**(a) O que muda:** também documentado como paridade no próprio código (comentário linhas 19-23):
overline/H1/descrição → grid de KPIs → linha 2fr/1fr (taxa de erro / erros por tela) → card
full-width Top Crashes. Bate com o protótipo. **Sem divergência encontrada** nesta passagem (não
conferi o tema light do protótipo, que no arquivo `Md3ErrorsContent.dc.html` está com um placeholder
"(equivalente claro — ver DesignSync para tokens light exatos)" ao invés do HTML completo — se for
necessário validar tema claro pixel a pixel, pedir para a Claudete puxar de novo via DesignSync).

**(b) Arquivos afetados:** nenhum identificado nesta passagem.

**(c) Risco/dependências:** o protótipo salvo tem o tema light incompleto para esta tela — validação
de tema claro fica pendente, não é "aprovado", é "não verificado".

**(d) Ordem sugerida:** re-verificar tema claro antes de considerar 100% fechada.

---

### 04 — Redes & Provedores (`/networks`)

**Fonte:** `Md3NetworksContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/networks/NetworksOperatorsPage.tsx`.

**(a) O que muda:** estrutura confirmada 1:1 — overline/H1/descrição → 4 KPIs (Score médio, Sessões
via Wi-Fi, Operadoras monitoradas, Regiões cobertas "Não disponível") → linha 1.2fr/1fr (mapa por UF
"Em breve" / lista de sessões por operadora com barras). Bate com o protótipo linha a linha,
inclusive o texto "diagnostic_sessions não coleta UF/região hoje" citado nos dois lados. **Sem
divergência encontrada.**

**(b) Arquivos afetados:** nenhum.

**(c) Risco/dependências:** nenhum.

**(d) Ordem sugerida:** já alinhado.

---

### 05 — Uso do App (`/product-analytics`)

**Fonte:** `Md3ProductAnalyticsContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/product-analytics/ProductAnalyticsPage.tsx`.

**(a) O que muda:** overlines e títulos de card batem (`"USO DO APP"`, `"Funcionalidade mais usada ·
sessões 7 dias"`, `"Funil · teste de velocidade"`, `"Dispositivos mais ativos"`) — mas eu só
confirmei os headers via grep nesta passagem, não o corpo completo linha a linha (diferente das
telas 01-04, que li o arquivo inteiro). **Recomendo uma segunda passagem antes de aprovar 100%** —
os headers batendo é forte indício de alinhamento, mas não é a mesma garantia que dei pras telas
acima.

**(b) Arquivos afetados:** a confirmar na segunda passagem.

**(c) Risco/dependências:** médio — não fechar esta tela como "aprovada" sem o corpo completo lido.

**(d) Ordem sugerida:** pendente de segunda leitura antes de aplicar qualquer mudança.

---

### 06 — Releases & Qualidade (`/app-versions`)

**Fonte:** `Md3AppVersionsContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/app-versions/VersionsTab.tsx` → `VersionsPage` (300 linhas).

**(a) O que muda:** overline `"RELEASES & QUALIDADE"` confirmado, título de card `"Versões em
produção"` e `"Notas de release recentes"`/`"Notas de release não disponíveis"` confirmados via grep
— batem com o protótipo (tabela de versões + card vazio "Notas de release não disponíveis"). Mesma
ressalva da tela 05: só header-level, não linha a linha.

**(b) Arquivos afetados:** a confirmar na segunda passagem.

**(c) Risco/dependências:** médio, mesma ressalva.

**(d) Ordem sugerida:** pendente de segunda leitura.

---

### 07 — IA & Custos (`/ai-cost`)

**Fonte:** `Md3AiCostContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/ai-cost/AiCostPage.tsx`.

**(a) O que muda:** overline `"IA & CUSTOS"` e título de card `"Custo por funcionalidade"`
confirmados via grep, batendo com o protótipo (4 KPIs, orçamento mensal com barra de progresso,
donut de custo por provedor, card "Custo por funcionalidade" em breve). Mesma ressalva: header-level.

**(b) Arquivos afetados:** a confirmar na segunda passagem.

**(c) Risco/dependências:** médio, mesma ressalva.

**(d) Ordem sugerida:** pendente de segunda leitura.

---

### 08 — Saúde do Sistema (`/system-health`)

**Fonte:** `Md3SystemHealthContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/system-health/SystemHealthPage.tsx` (263 linhas).

**(a) O que muda:** overline `"SAÚDE DO SISTEMA"` e título `"Latência P95 da API · 14 dias"`
confirmados via grep, batendo com o protótipo. Mesma ressalva: header-level, não linha a linha (263
linhas é o dobro do tamanho das telas que confirmei por completo — maior chance de conter lógica
adicional não coberta pelo protótipo, como aconteceu na tela 10).

**(b) Arquivos afetados:** a confirmar na segunda passagem — atenção especial aqui pelo tamanho do arquivo.

**(c) Risco/dependências:** médio-alto (tamanho do arquivo é um sinal de alerta, não confirmação de problema).

**(d) Ordem sugerida:** pendente de segunda leitura, com prioridade sobre 05/06/07 dado o tamanho.

---

### 09 — Configurações (`/settings`)

**Fonte:** `Md3SettingsContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/settings/SettingsTab.tsx` → `SettingsPage.tsx` (43 linhas).

**(a) O que muda:** overline `"CONFIGURAÇÕES"` e H1/pergunta `"Configurações do painel"` confirmados,
batendo com o protótipo. Título `"Exportações"` confirmado (protótipo também tem card "Exportações"
com CTA "Em breve"). O protótipo também tem dois cards antes disso — "Feature flags" (4 toggles:
Speedtest, Chat IA, Diagnóstico avançado, Modo offline) e "Acesso da equipe" (lista de 3 pessoas com
avatar+cargo) — não confirmei se esses dois cards existem no código real nesta passagem (arquivo
pequeno, 43 linhas, sugere que pode estar mais enxuto que o protótipo — possívelmente delegando pra
`FeatureFlagsTab.tsx`, que existe como arquivo próprio em `features/feature-flags/`).

**(b) Arquivos afetados:** `SettingsPage.tsx` + possivelmente `FeatureFlagsTab.tsx` — confirmar se o
card "Feature flags" do protótipo é renderizado por esse componente ou se falta essa composição.

**(c) Risco/dependências:** médio — "Acesso da equipe" no protótipo é dado de exemplo/fixture
explicitamente marcado como tal no próprio arquivo (`Md3SettingsContent.dc.html` linha 35: "os nomes
de equipe... são dado de exemplo/fixture do protótipo, não usuários reais do painel") — não
implementar como se fosse requisito de dado real sem confirmar com a Claudete se existe fonte real
de usuários do painel hoje.

**(d) Ordem sugerida:** pendente de segunda leitura completa de `SettingsPage.tsx` +
`FeatureFlagsTab.tsx` antes de decidir o que falta.

---

### 10 — Ferramentas (`/tools`)

**Fonte:** `Md3ToolsContent.dc.html`.
**Código real:** `SignallQ Admin/src/features/tools/ToolsPage.tsx` (781 linhas — maior arquivo de
feature do Console).

**(a) O que muda — DIVERGÊNCIA CONFIRMADA (escopo extra):** o protótipo define **4 seções**:
"Diagnósticos — sessões individuais" (`/diagnostics`), "Erros — lista detalhada" (`/errors`), "Saúde
do sistema — detalhamento" (`/system-health`), "Configurações — integrações e limites" (`/settings`).
O código real tem uma **5ª seção não prevista no protótipo**: `"Uso do App — detalhamento"`
(`ToolsPage.tsx:553`, engajamento por funcionalidade, navegação entre telas, crashes e retenção).
Isso é o mesmo padrão de risco já registrado na memória do squad ("divergência 'app tem mais que o
protótipo' é pra cortar, não manter" — decisão de produto já tomada antes, não específica desta
sessão, mas o mesmo princípio se aplica).

**(b) Arquivos afetados:** `SignallQ Admin/src/features/tools/ToolsPage.tsx` — nenhuma alteração de
código necessária (decisão tomada foi manter).

**(c) Risco/dependências:** nenhum — decisão de produto fechada.

**(d) Ordem sugerida:** resolvida.

**RESOLVIDO em 2026-07-18:** o Luiz decidiu manter a seção "Uso do App — detalhamento" (e também
`GeminiQuotaCard` em IA & Custos e `CloudflareUsagePanel` em Saúde do Sistema — mesma classe de
divergência, código com dado real que o protótipo não previa) — "Lia deve usar o que tem, e alocar
as informações no local mais adequado e no formato mais adequado", não cortar. Lia desenhou o
formato MD3 das três seções e aplicou nos arquivos de conteúdo do protótipo local. Detalhe completo,
racional de posição/formato e a dívida de tokens hardcoded encontrada nos componentes de
"Uso do App" em `docs_ai/design-system/DECISAO_TRES_SECOES_REAIS_CONSOLE_2026-07-18.md`. Push real
para o Claude Design (`write_files`) ainda pendente — sessão sem `DesignSync` carregada.

---

## Resumo de ordem de aplicação recomendada

1. **Navegação agrupada** (`navigation.ts`) — base, afeta todas as telas simultaneamente.
2. **Login** — sem card wrapper, fluxo "esqueci senha" (pendente decisão de escopo do botão).
3. **Overview — peso do H1** (500 em vez de 700) — correção pontual.
4. Diagnósticos, Erros (tema dark confirmado; tema light pendente), Redes & Provedores — **já
   alinhados**, sem ação, só confirmar visualmente depois do item 1.
5. Uso do App, Releases & Qualidade, IA & Custos, Saúde do Sistema, Configurações — **pendente de
   segunda leitura linha a linha** (só header-level confirmado nesta passagem) antes de aprovar ou
   listar mudança.
6. Ferramentas — **resolvido em 2026-07-18**: manter "Uso do App — detalhamento" (e as duas outras
   seções análogas de IA & Custos/Saúde do Sistema), Lia já desenhou o formato MD3 — ver
   `DECISAO_TRES_SECOES_REAIS_CONSOLE_2026-07-18.md`.
7. Correção de referência no `SignallQ Admin/DESIGN.md` (linha 4) e decisão sobre `PRODUCT.md`
   ausente — Camilo/Rhodolfo, fora do escopo de design puro.

## Pendências explícitas (não fechadas nesta passagem)

- Tema light completo de `Md3ErrorsContent` (protótipo salvo tem placeholder, não HTML real).
- Leitura linha a linha de `ProductAnalyticsPage.tsx`, `VersionsTab.tsx`/`VersionsPage.tsx`,
  `AiCostPage.tsx`, `SystemHealthPage.tsx`, `SettingsPage.tsx`/`FeatureFlagsTab.tsx` — headers batem,
  corpo não foi comparado linha a linha ainda.
- Confirmação de reuso de componentes antes de cortar a seção extra da tela Ferramentas.
