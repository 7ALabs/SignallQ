# Plano consolidado — correção dos achados de audit + critique (SignallQ PWA)

> Consolida `docs/reviews/impeccable-audit.md` (técnico: a11y, performance, theming, responsivo, anti-patterns) e `docs/reviews/impeccable-critique.md` (UX/produto: Result/TestDetail, Home, SpeedTest) num plano único, em ondas pequenas e sequenciais. Nenhum arquivo do app foi alterado ainda — este documento é só o plano.

## Como usar este plano

- Cada onda é independente o suficiente para ser um PR próprio (pequeno/médio, sem aprovação prévia conforme `CLAUDE.md`).
- A ordem das ondas segue a lógica: primeiro o que o critique já sinalizou como P1 de produto (Result/vocabulário), depois copy/Home, depois a11y/responsivo P1 do audit, depois os outliers estruturais (ReportPage/About), depois performance/hygiene, e por último o polish visual fino.
- Rodar `npm run verify` (typecheck + test + build) ao final de cada onda, e `/impeccable audit`/`/impeccable critique` depois da Onda 6 para confirmar o score subindo.

---

## Onda 1 — Result/TestDetail e vocabulário de veredito

**Objetivo**: eliminar a fragmentação em 7 caixas bordadas do Result/TestDetail (achado #1 do critique) e unificar os 3 vocabulários de veredito divergentes (`statusTitle`, `metricVerdict`, `qualityLabel`) num módulo único, removendo também a duplicação de lógica byte-a-byte entre as duas telas.

**Arquivos prováveis**:
- `src/features/result/ResultScreen.tsx`
- `src/features/history/TestDetailScreen.tsx`
- `src/design-system/styles.css` (`.sq-metric-block`, `.sq-metrics-grid`, referência de layout em `.sq-home-screen__result-grid:1787-1798`)
- Novo: `src/shared/verdict.ts` (ou `src/design-system/verdict.ts`) — módulo único de vocabulário
- `src/features/history/HistoryPanel.tsx` (usa `qualityLabel`, precisa importar do módulo novo)

**Alterações propostas**:
1. Extrair `verdictFromQuality`, `statusTitle` e `metricVerdict` (hoje duplicados em `ResultScreen.tsx:13-46` e `TestDetailScreen.tsx:13-46`) para um módulo compartilhado com uma única escala de rótulos (decidir com Luiz/Lia se a escala final é "Boa/Atenção/Ruim" ou "Excelente/Bom/Regular/Fraco/Forte" do design system — usar a nomenclatura já documentada em `DESIGN.md`/`CLAUDE.md`).
2. Atualizar `qualityLabel` em `HistoryPanel.tsx:34-45` para consumir o mesmo módulo, eliminando a quarta variante ("Inconclusivo" vira o rótulo padrão de "unknown" do módulo único).
3. Reestruturar o grid de 4 métricas (`ResultScreen.tsx:115-156`, `TestDetailScreen.tsx:115-156`) de 4 `sq-metric-block` separados para um único card com divisores internos, seguindo o padrão já existente e aprovado em `sq-home-screen__result-grid` (`styles.css:1787-1798`).
4. Ajustar CSS: criar `.sq-metrics-card` (ou renomear/adaptar `.sq-metric-block` para um modificador `--inline` dentro de um card pai) em vez de 4 boxes independentes.

**Risco de regressão**:
- Médio — toca o coração da tela de resultado (fluxo mais usado do app) e um arquivo compartilhado entre live-result e histórico salvo; qualquer erro de threshold afeta as duas telas ao mesmo tempo (mitigado por ser justamente o objetivo: hoje o risco já existe, silenciosamente, por estarem duplicados).
- Testes existentes que fixam labels antigos ("Fraca/Regular/Boa" etc.) vão quebrar — checar `tests/` antes de mudar os textos.

**Critérios de aceite**:
- Result e TestDetail renderizam as 4 métricas dentro de um único container visual (não 4 boxes soltos).
- Não existe mais nenhuma ocorrência de rótulo de veredito divergente entre Result/TestDetail/HistoryPanel — grep por "Fraca", "Inconclusivo" etc. deve apontar para um único módulo fonte.
- Nenhuma duplicação de `verdictFromQuality`/`statusTitle`/`metricVerdict` entre os dois arquivos de tela.

**Comandos de validação**:
- `npm run typecheck`
- `npm test`
- `npm run build`
- Inspeção visual manual do Result e do TestDetail (bom/atenção/ruim) — ainda sem browser automation nesta sessão, então validar rodando `npm run dev` e abrindo manualmente.

---

## Onda 2 — Home, SpeedTest e microcopy

**Objetivo**: remover a redundância de "Histórico" (3x na mesma tela) e o card vazio desnecessário na Home; remover o jargão técnico ICMP do SpeedTest no momento de maior ansiedade; limpar as microcopy issues levantadas como minor observations do critique que também são copy (não visual).

**Arquivos prováveis**:
- `src/features/home/HomeScreen.tsx`
- `src/features/speedtest/SpeedTestScreen.tsx`
- `src/design-system/styles.css` (`.sq-home-screen__empty-card`, `.sq-home-screen__action-row`)
- `src/features/settings/SettingsPanel.tsx` (microcopy "Pede confirmação.")
- `src/features/landing/LandingScreen.tsx` (mensagem da pill, para alinhar com a Home)

**Alterações propostas**:
1. Remover o botão "Histórico" do header desktop da Home (`HomeScreen.tsx:24`) — já redundante com o item de navegação (linhas 35-37); manter só a `action-row` (linhas 92-101) como segunda via de acesso, ou decidir com Luiz qual das três entradas é a certa e cortar as outras duas.
2. Trocar `sq-home-screen__empty-card` (linhas 87-90, caixa bordada só para uma frase) por texto simples centralizado sem borda.
3. Alinhar a mensagem da pill entre Landing (`LandingScreen.tsx:37`, "Servidor estimado · via navegador") e Home (`HomeScreen.tsx:54`, "Medição direta pelo navegador") — escolher uma frase e reusar nas duas telas.
4. Remover "medida por requisição HTTP, não por ping ICMP" (`SpeedTestScreen.tsx:100-102`) da tela de teste rodando; mover a explicação técnica para o AboutScreen/tooltip, substituindo por copy de reasseguramento simples durante o teste.
5. Corrigir "teste(s) salvos" (`HomeScreen.tsx:98`, `HistoryPanel.tsx:116`) para pluralização real via `Intl.PluralRules` ou lógica condicional simples (`n === 1 ? "teste salvo" : "testes salvos"`).
6. Remover "Pede confirmação." (`SettingsPanel.tsx:69`) — narração autorreferente sem valor.
7. Atualizar "Versão do app 1.0.0 · web" (`SettingsPanel.tsx:88`) para ler a versão real do `package.json` (hoje `0.1.0`) em vez de valor hardcoded divergente.

**Risco de regressão**:
- Baixo — mudanças pontuais de copy e remoção de elementos redundantes, sem lógica de estado nova.
- Atenção: remover uma das três entradas de "Histórico" pode impactar testes de navegação/e2e existentes que referenciem esse botão pelo texto/role.

**Critérios de aceite**:
- "Histórico" aparece no máximo 2x visíveis simultaneamente na Home (nav + no máximo uma ação contextual), nunca 3.
- Nenhuma caixa bordada envolvendo uma única frase de estado vazio.
- Nenhuma menção a "ICMP" na tela de SpeedTest em execução.
- Pill da Landing e da Home usam a mesma frase (ou frases que não se contradizem sobre como o teste funciona).
- "Versão do app" reflete o valor real de `package.json`.

**Comandos de validação**:
- `npm run typecheck`
- `npm test`
- Revisão manual de copy em PT-BR (sentence case, sem jargão, conforme design system).

---

## Onda 3 — Acessibilidade P1 e responsivo

**Objetivo**: resolver os achados P1 de acessibilidade do audit técnico (contraste de `--text-tertiary`, ARIA ausente no `ProgressRing` e no toggle de tema, headings semânticos ausentes) e os P1 de responsivo (alvo de toque do botão Voltar, padding do card de resultado da Home em telas estreitas).

**Arquivos prováveis**:
- `src/styles/tokens.css` (`--text-tertiary`, ou um novo token de contraste mais seguro)
- `src/design-system/components/ProgressRing/index.tsx`
- `src/features/settings/SettingsPanel.tsx` (segmented control)
- `src/design-system/components/TopAppBar/index.tsx` (`.sq-top-app-bar__back`)
- `src/design-system/styles.css` (contraste, touch target, `.sq-home-screen__result-card`)
- `src/features/home/HomeScreen.tsx`, `ResultScreen.tsx`, `TestDetailScreen.tsx`, `SpeedTestScreen.tsx`, `SettingsPanel.tsx`, `AboutScreen.tsx` (headings)

**Alterações propostas**:
1. **Contraste**: trocar `--text-tertiary` (`#9ca3af`, ~2.5:1) por `--text-secondary` (~4.8:1) nos usos de conteúdo real identificados: `HistoryTable` headers, "não medida" (`ResultScreen.tsx:122-124`/`TestDetailScreen.tsx:122-124`), `ProgressRing` default `phaseColor`, `QualityBadge` estado `unknown` (afeta `SpeedHeroCard` — ver Onda 5 sobre esse componente), `RecommendationList` description, hint da Landing/Home.
2. **ARIA — ProgressRing**: adicionar `role="progressbar"` + `aria-valuenow`/`aria-valuemin`/`aria-valuemax` no anel (`ProgressRing/index.tsx:15-29`); considerar `aria-live="polite"` na região de fase para anunciar mudança de etapa.
3. **ARIA — toggle de tema**: adicionar `role="radiogroup"` no container e `role="radio"`/`aria-checked` nos dois botões do segmented control (`SettingsPanel.tsx:39-56`).
4. **Headings semânticos**: promover o título visual (hoje `<strong>`/`.overline`) para heading real (`<h1>` ou `<h2>` conforme a posição na árvore) em `HomeScreen.tsx`, `ResultScreen.tsx`, `TestDetailScreen.tsx`, `SpeedTestScreen.tsx`, `SettingsPanel.tsx`, `AboutScreen.tsx` — usar `ReportPage.tsx:37,53,67,76` (h1→h2→h3) como modelo de hierarquia correta.
5. **Touch target do botão Voltar**: adicionar `min-width: 44px; min-height: 44px` a `.sq-top-app-bar__back` (`styles.css:131-139`).
6. **Padding do card de resultado da Home em mobile**: adicionar regra `@media (max-width: 560px)` reduzindo `.sq-home-screen__result-card` de `32px 48px` para algo como `var(--space-md) var(--space-lg)`, e avaliar colapso do grid de 3 colunas abaixo de ~340px.
7. Considerar upgrade de touch target para `.sq-icon-button`, `.sq-action-card__button`, `.install-prompt-banner__dismiss` (40px→44px) e `.sq-segmented-control__option` (32px→40-44px) — incluído aqui por serem do mesmo tema (a11y+responsivo), mas pode migrar para Onda 6 se o escopo desta onda ficar grande.

**Risco de regressão**:
- Baixo-médio — mudanças de token de cor têm efeito amplo (muitos componentes leem `--text-tertiary`); validar visualmente que nada fica "escuro demais" onde a intenção era realmente sutil (ex.: labels verdadeiramente decorativos, se houver).
- Adicionar heading pode exigir ajuste de CSS (`.overline`/`<strong>` viram `<h1 className="overline">` etc.) para não herdar estilo de heading do browser (margin default) — checar reset em `global.css`.

**Critérios de aceite**:
- Nenhuma instância de `--text-tertiary` usada em texto de conteúdo real (só em elementos genuinamente decorativos/terciários, se sobrar algum uso legítimo).
- `ProgressRing` e o toggle de tema navegáveis e anunciados corretamente por leitor de tela (testar com NVDA/VoiceOver ou ao menos inspecionar a árvore de acessibilidade no devtools).
- Todas as 6 telas têm exatamente um `<h1>` (ou heading apropriado ao nível) navegável por leitor de tela.
- Botão Voltar com área de toque ≥44×44px em todas as telas onde aparece.
- Card de resultado da Home legível e sem números cortados em 320-360px de largura.

**Comandos de validação**:
- `npm run typecheck && npm test && npm run build`
- `/impeccable audit` (rodar de novo, comparar score de Acessibilidade e Responsivo com o baseline 2/4 e 3/4 deste relatório).
- Verificação manual de contraste (ex.: devtools do Chrome, "Inspect" → contraste da cor).

---

## Onda 4 — ReportPage e AboutScreen

**Objetivo**: resolver o maior outlier estrutural identificado tanto no audit quanto no critique — `ReportPage` fora do design system, sem `AppShell`/`Button`/`Card`, sem responsividade — e suavizar o grid genérico de 4 cards do `AboutScreen`.

**Arquivos prováveis**:
- `src/features/report/ReportPage.tsx`
- `src/design-system/styles.css` (remover `.report-page__*`/`.report-card__*`, usar classes do sistema)
- `src/features/about/AboutScreen.tsx`
- `src/design-system/components/AboutInfoCard/index.tsx`

**Alterações propostas**:
1. Reconstruir `ReportPage.tsx:34` sobre `AppShell`/`TopAppBar` (com `mobileMode` apropriado) em vez do `<main className="report-page">` solto.
2. Trocar os botões bespoke `text-button` ("Voltar", "Copiar link", `ReportPage.tsx:40-45`) pelo componente `Button` (`variant="text"`/`variant="outline"`) já usado no resto do app.
3. Trocar o card manual do laudo pelo componente `Card` (`variant="surface"` ou `outlined`), eliminando `.report-card__*` duplicado de `.sq-card`.
4. Remover o eyebrow acima do `<h1>` (`ReportPage.tsx:36`, "SignallQ PWA") — soa a hero de marketing, fora do tom de produto; manter só o `<h1>`.
5. Preservar a hierarquia de heading já correta (`h1→h2→h3`, linhas 37/53/67/76) — é o único ponto que já está certo nessa tela.
6. **AboutScreen**: quebrar o ritmo uniforme do grid de 4 `AboutInfoCard` (`AboutScreen.tsx:25-50`) — por exemplo, mesclar "Seus dados" e "Se a IA for usada" num único bloco com lista, ou variar largura/ênfase de um dos 4 cards para não ler como grid de feature genérico.

**Risco de regressão**:
- Médio — `ReportPage` é a tela compartilhável publicamente (link de laudo), possivelmente acessada por usuários sem estado de app carregado; garantir que o `AppShell`/`TopAppBar` novos não dependam de contexto que não existe nessa rota isolada (ex.: navegação principal, tema).
- Baixo no AboutScreen — mudança de layout, sem lógica.

**Critérios de aceite**:
- `ReportPage` usa `AppShell`, `TopAppBar`, `Button` e `Card` como qualquer outra tela — zero CSS `.report-page__*`/`.report-card__*` remanescente.
- `ReportPage` responsiva: `max-width` e padding corretos em mobile e desktop (herdados do `AppShell`).
- `AboutScreen` não lê mais como "grid de 4 features" idêntico — pelo menos um elemento de composição quebra a uniformidade.

**Comandos de validação**:
- `npm run typecheck && npm test && npm run build`
- Testar `ReportPage` isoladamente (acesso direto via URL, sem navegação prévia pelo app) em mobile e desktop.
- `/impeccable audit` — conferir se o achado "ReportPage fora do design system" some do relatório.

---

## Onda 5 — Performance/histórico e higiene

**Objetivo**: resolver os riscos de escala do histórico (P2 técnico: `historyRepository.list()` sem paginação, `HistoryTable` sem virtualização, falta de memoização) e fazer a limpeza de código morto identificada em ambos os relatórios (tokens legados, componentes nunca usados, JSX duplicado — este último já resolvido na Onda 1 para Result/TestDetail).

**Arquivos prováveis**:
- `src/shared/storage/historyRepository.ts`
- `src/design-system/components/HistoryTable/index.tsx`
- `src/features/history/HistoryPanel.tsx`
- `src/design-system/components/ProgressRing/index.tsx`, `StepTracker/index.tsx`
- `App.tsx`
- Deletar: `src/design-system/tokens/{colors,typography,spacing,radius,elevation,motion}.ts`, `src/design-system/theme/{darkTheme,lightTheme}.ts`
- Deletar ou consolidar: `src/design-system/components/ActionCard/`, `src/design-system/components/SpeedHeroCard/`
- `src/design-system/index.ts` (barrel export)
- `index.html` (preconnect de fontes)

**Alterações propostas**:
1. Adicionar paginação/cursor a `historyRepository.list()` (`historyRepository.ts:40-43`) — usar `IDBKeyRange`/índice por `createdAt` em vez de `getAll()` completo.
2. Escolher variante desktop/mobile do `HistoryTable` via hook de media query em JS (montar só uma árvore) em vez de renderizar as duas e esconder por CSS (`HistoryTable/index.tsx:33-58`); considerar virtualização (windowing) acima de ~50-100 itens.
3. Adicionar `React.memo` a `HistoryTable`, `StatusCard`, `ProgressRing`, `StepTracker`; memoizar `rows` em `HistoryPanel.tsx:59-67` e `latest` em `App.tsx:410-423` com `useMemo` nas dependências reais.
4. Hoistar constantes recriadas a cada render (`order` em `SpeedTestScreen.tsx:50`) para escopo de módulo.
5. Adicionar `<link rel="preconnect">` para `fonts.googleapis.com` e `fonts.gstatic.com` em `index.html` antes do link de stylesheet da fonte.
6. **Limpeza (theming/hygiene)**: deletar `src/design-system/tokens/{colors,typography,spacing,radius,elevation,motion}.ts` e `theme/{darkTheme,lightTheme}.ts` (valores divergentes de `tokens.css`, sem uso real). Migrar o tipo `QualityLevel` (hoje reexportado de `colors.ts`) para um arquivo de tipos dedicado (`src/design-system/types.ts`) e atualizar os 4 importadores (`QualityBadge/index.tsx`, `HistoryTable/index.tsx`, `HomeScreen.tsx`, `HistoryPanel.tsx`). Remover as linhas correspondentes de `src/design-system/index.ts`.
7. **Componentes órfãos**: decidir entre deletar `ActionCard`/`SpeedHeroCard` (nunca renderizados em lugar nenhum) ou adotá-los de fato substituindo o código hand-rolled equivalente na Home/Result — recomendação: deletar, já que Onda 1 já está reestruturando o padrão de card do Result e a Home usa `action-row` próprio.

**Risco de regressão**:
- Médio no `historyRepository`/`HistoryTable` — toca a fonte de dados do histórico; testar com histórico vazio, com 1 item, e com uma lista grande (gerar massa de teste local).
- Baixo na limpeza de tokens legados — são não utilizados, mas confirmar via grep antes de deletar (o próprio audit já rastreou todos os importadores).
- Baixo em `React.memo`/`useMemo` — risco clássico de "memoizar errado" (dependências incompletas); cobrir com os testes existentes de render.

**Critérios de aceite**:
- Histórico com centenas de itens locais não trava a UI nem duplica renderização de linhas escondidas por CSS.
- Nenhum arquivo em `src/design-system/tokens/*.ts` ou `theme/{darkTheme,lightTheme}.ts` sobrevive com valores divergentes de `tokens.css` (deletados ou passam a re-exportar de `tokens.css`/CSS vars).
- `ActionCard`/`SpeedHeroCard` removidos do repo, ou adotados com pelo menos um uso real em produção.
- Fonte carrega com `preconnect` (verificável via aba Network do devtools — menos round-trips).

**Comandos de validação**:
- `npm run typecheck && npm test && npm run build`
- Teste manual de histórico com massa de dados simulada (ex.: popular IndexedDB via devtools ou script de seed local).
- `/impeccable audit` — conferir subida do score de Performance e Theming.

---

## Onda 6 — Polish final visual e tokens

**Objetivo**: fechar os últimos P3 de polish visual do audit e do critique — itens pequenos, de baixo risco, que fecham a consistência de tokens e microcopy residual.

**Arquivos prováveis**:
- `src/design-system/styles.css`
- `src/styles/tokens.css`
- `src/features/history/HistoryPanel.tsx`
- `src/design-system/components/LimitationsCard/index.tsx` ou `styles.css` (`.sq-limitations-card`)

**Alterações propostas**:
1. Corrigir `.sq-segmented-control__option--active` (`styles.css:1398`) para usar um token de elevação real (ou `var(--elevation-1)`) em vez de sombra preta hardcoded que some no dark mode.
2. Introduzir token `--on-accent: #ffffff` em `tokens.css` e referenciá-lo nos 3 usos hardcoded de `#ffffff` sobre `--accent` (`styles.css:193,1656,1723`).
3. Corrigir `.sq-card--tonal` (`styles.css:249`) para usar um token de borda dedicado em vez de reaproveitar o fill translúcido `--accent-container` (quase invisível no dark mode).
4. Renomear classes `history-panel__message` (`HistoryPanel.tsx:93-111`) para o prefixo `sq-` padrão do design system.
5. Ajustar `LimitationsCard` para não usar sempre `--amber-surface` quando `quality === 'good'` — considerar uma variante neutra/informativa nesse caso, preservando o peak-end positivo de um resultado bom.
6. Corrigir o `<summary>` de `DiagnosisResultPanel.tsx:55-56` para herdar o `focus-visible` padrão (`global.css:29-33`), hoje fora do seletor `button, a`.
7. Revisão final de microcopy residual não coberta nas Ondas 1-2 (ex.: qualquer string PT-BR que ainda soe genérica após as ondas anteriores).

**Risco de regressão**:
- Baixo — todos os itens são cosméticos/tokens isolados, sem mudança de estrutura ou lógica.

**Critérios de aceite**:
- Segmented control ativo visível em dark mode.
- Nenhum `#ffffff`/cor literal hardcoded fora de `tokens.css` para texto-sobre-acento.
- `.sq-card--tonal` com borda visível em dark mode.
- Nenhuma classe sem prefixo `sq-` no design system.
- `LimitationsCard` não usa tom de aviso quando o resultado é bom.
- `<summary>` com foco visível consistente com o resto do app.

**Comandos de validação**:
- `npm run typecheck && npm test && npm run build`
- `/impeccable audit` — score final esperado: subir de 14/20 para a faixa 17-20 (Good/Excellent).
- `/impeccable critique` — re-rodar no mesmo alvo (`app-home-landing-speedtest-result-history-settings`) e conferir a tendência (`trend`) subindo a partir de 24.5/40.

---

## Resumo de rastreabilidade (achado → onda)

| Achado | Origem | Onda |
|---|---|---|
| Result/TestDetail com excesso de caixas | critique P1 | 1 |
| Vocabulário de veredito inconsistente | critique P1 | 1 |
| Lógica de veredito duplicada | critique P2 | 1 |
| Redundância "Histórico" na Home | critique P2 | 2 |
| Jargão ICMP | critique P2 | 2 |
| Microcopy residual (plural, versão, "pede confirmação") | critique minor | 2 |
| Contraste `--text-tertiary` | audit P1 (a11y) | 3 |
| ProgressRing/tema sem ARIA | audit P1 (a11y) | 3 |
| Headings semânticos ausentes | audit P1 (a11y) | 3 |
| Botão Voltar com alvo de toque pequeno | audit P1 (responsivo) | 3 |
| Padding do result-card da Home em mobile | audit P1 (responsivo) | 3 |
| ReportPage fora do design system | audit P1 + critique (anti-pattern) | 4 |
| AboutScreen com grid genérico | audit P2 (anti-pattern) | 4 |
| historyRepository/HistoryTable sem escala | audit P2 (performance) | 5 |
| Falta de memoização | audit P2 (performance) | 5 |
| Tokens legados divergentes | audit P2 (theming) | 5 |
| ActionCard/SpeedHeroCard órfãos | audit P2 (anti-pattern) | 5 |
| Preconnect de fontes | audit P2 (performance) | 5 |
| Sombra hardcoded no segmented control | audit P3 | 6 |
| Token `--on-accent` ausente | audit P3 | 6 |
| Borda do card tonal | audit P3 | 6 |
| Classes sem prefixo `sq-` | audit P3 | 6 |
| LimitationsCard sempre âmbar | audit P3 + critique minor | 6 |
| Foco do `<summary>` | audit P3 | 6 |
