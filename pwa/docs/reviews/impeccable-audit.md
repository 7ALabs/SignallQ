# Audit — SignallQ PWA (webapp inteiro)

> Gerado via `/impeccable audit`, 2026-07-04. Auditoria técnica (a11y, performance, theming, responsivo, anti-patterns) sobre o app inteiro. Não corrige nada — documenta achados para as próximas passagens (`polish`, `adapt`, `optimize`, `harden`, `layout`).

## Audit Health Score

| # | Dimensão | Score | Achado-chave |
|---|---|---|---|
| 1 | Acessibilidade | **2/4** | 5+ instâncias de `--text-tertiary` abaixo de 4.5:1 em texto real; sem heading em 6 telas |
| 2 | Performance | **3/4** | Sem P0; histórico sem paginação/virtualização é o único risco real de escala |
| 3 | Theming | **3/4** | Sistema de tokens (CSS vars + `.dark`) é exemplar; sobra código morto divergente |
| 4 | Responsivo | **3/4** | Breakpoints 860/560 bem aplicados; botão voltar com alvo de toque de 24×24px |
| 5 | Anti-Patterns | **3/4** | Zero clichê visual de IA (gradiente, glass, eyebrow numerado); drift real de vocabulário em `ReportPage` |
| **Total** | | **14/20** | **Good — endereçar dimensões fracas** |

## Veredito Anti-Patterns

**Não parece "feito por IA" no sentido clássico.** Nenhum dos tells visuais de SaaS genérico apareceu: sem gradient text, sem glassmorphism, sem hero-metric com gradiente, sem grid de cards idênticos genérico, sem eyebrow numerado (01/02/03), sem card aninhado, sem scrollbar/modal reinventado. O acento único violeta é respeitado, cards são flat por decisão, toda métrica vem com veredito.

O que realmente existe é mais sutil e mais real: **drift de vocabulário de componente concentrado em `ReportPage`** — a única tela que não usa `AppShell`/`TopAppBar`/`Button`/`Card`, tem CSS próprio duplicando `.sq-card`, e o único botão "Voltar" com implementação bespoke em vez do padrão do resto do app. Some a isso dois componentes prontos e nunca usados (`ActionCard`, `SpeedHeroCard`) e JSX duplicado byte-a-byte entre `ResultScreen`/`TestDetailScreen` — assinatura típica de gerações iterativas sem passo de consolidação.

## Resumo Executivo

- Audit Health Score: **14/20 (Good)**
- Total de achados: **6 P1 · 9 P2 · 8 P3** (0 P0 — nada bloqueia o uso hoje)
- Top 5 críticos:
  1. Contraste de `--text-tertiary` (~2.5:1) em texto real de conteúdo, em 5+ lugares (labels de coluna, "não medida", dica de recomendação, hint da landing/home)
  2. `ReportPage` fora do design system inteiro — sem `AppShell`, CSS e botões próprios, sem responsividade
  3. Botão "Voltar" com alvo de toque de ~24×24px (padrão em 5 telas)
  4. `ProgressRing` sem `role="progressbar"`/`aria-live` — teste roda sem feedback para leitor de tela
  5. `historyRepository.list()` carrega o histórico inteiro sem paginação; `HistoryTable` renderiza ambas variantes (desktop+mobile) sempre

## Achados Detalhados por Severidade

### P1 — Bloqueante para release / violação WCAG AA

**[P1] Contraste insuficiente em `--text-tertiary`**
- **Local**: `src/styles/tokens.css:123-124` (`.label-medium/.label-small`); usado em `HistoryTable/index.tsx:26-31` (headers), `ResultScreen.tsx:122-124`/`TestDetailScreen.tsx:122-124` ("não medida"), `ProgressRing/index.tsx:12,21` (phaseColor default), `design-system/styles.css:312-343` (QualityBadge unknown, sempre disparado por `SpeedHeroCard/index.tsx:40`), `styles.css:494-498` (RecommendationList), `styles.css:1733-1737` (hint da landing/home)
- **Categoria**: Acessibilidade
- **Impacto**: `#9ca3af` sobre branco mede ≈2.5:1 — falha WCAG 1.4.3 (mínimo 4.5:1) em texto de conteúdo real, não decorativo
- **WCAG**: 1.4.3 Contrast (Minimum), nível AA
- **Recomendação**: trocar para `--text-secondary` (≈4.8:1) nesses usos, ou escurecer o token
- **Comando sugerido**: `/impeccable polish`

**[P1] `ProgressRing` e controle de tema sem semântica ARIA**
- **Local**: `ProgressRing/index.tsx:15-29` (sem `role="progressbar"`/`aria-valuenow`/`aria-live`); `SettingsPanel.tsx:39-56` (segmented control sem `role="radiogroup"`/`aria-pressed`)
- **Categoria**: Acessibilidade
- **Impacto**: durante o teste de velocidade — o fluxo principal do produto — um usuário de leitor de tela não recebe nenhum feedback de progresso; o toggle claro/escuro não anuncia qual opção está selecionada
- **WCAG**: 4.1.2 Name, Role, Value
- **Recomendação**: adicionar `role="progressbar"` + `aria-valuenow/min/max` ao ring, `role="radiogroup"`/`aria-checked` ao segmented control
- **Comando sugerido**: `/impeccable harden`

**[P1] Telas sem hierarquia de heading**
- **Local**: `HomeScreen.tsx`, `ResultScreen.tsx`, `TestDetailScreen.tsx`, `SpeedTestScreen.tsx`, `SettingsPanel.tsx`, `AboutScreen.tsx` — zero `<h1>`-`<h6>` em todas
- **Categoria**: Acessibilidade
- **Impacto**: navegação por heading (leitor de tela) não tem nada para pular; `ReportPage.tsx:37,53,67,76` é o único exemplo correto (`h1→h2→h3`) e deveria ser o modelo
- **WCAG**: 1.3.1 Info and Relationships
- **Recomendação**: promover o título visual de cada tela (hoje `<strong>`/`.overline`) para heading semântico real
- **Comando sugerido**: `/impeccable harden`

**[P1] Botão "Voltar" com alvo de toque ~24×24px**
- **Local**: `design-system/styles.css:131-139` (`.sq-top-app-bar__back`), usado em 5 telas (`SettingsPanel.tsx:23`, `AboutScreen.tsx:17`, `ResultScreen.tsx:93`, `HistoryPanel.tsx:84`, `TestDetailScreen.tsx:100`)
- **Categoria**: Responsivo
- **Impacto**: principal affordance de "voltar" do app é difícil de acertar em toque real, em toda tela mobile
- **Recomendação**: `min-width/height: 44px` no botão
- **Comando sugerido**: `/impeccable adapt`

**[P1] `ReportPage` fora do design system e sem shell responsivo**
- **Local**: `src/features/report/ReportPage.tsx:34` — `<main className="report-page">` sem `AppShell`; CSS próprio (`styles.css:892-945`) duplicando `.sq-card`; botões `text-button` bespoke em vez de `Button`
- **Categoria**: Responsivo + Anti-Pattern
- **Impacto**: sem `max-width`/padding em nenhum breakpoint — no mobile o texto roda de ponta a ponta, no desktop estica sem limite de linha; é a tela de laudo compartilhável, provavelmente aberta fria no celular. Também é o tell mais forte de "construído numa passada separada e nunca reconciliado"
- **Recomendação**: reconstruir sobre `AppShell`/`TopAppBar`/`Card`/`Button` como toda outra tela
- **Comando sugerido**: `/impeccable polish`

**[P1] Vocabulário de "voltar" inconsistente entre telas**
- **Local**: `TestDetailScreen.tsx:90-93` injeta um botão de voltar desktop bespoke (`style={{border:0}}`) que nenhuma tela irmã tem
- **Categoria**: Anti-Pattern (consistência de componente, produto)
- **Impacto**: mesma ação parece e se comporta diferente entre `TestDetailScreen` e `ResultScreen`/`HistoryPanel`
- **Recomendação**: padronizar o affordance de volta entre todas as telas de detalhe
- **Comando sugerido**: `/impeccable polish`

### P2 — Corrigir antes do release / risco de escala

- **[P2] `historyRepository.list()` sem paginação** (`src/shared/storage/historyRepository.ts:40-43`) — carrega tabela inteira a cada load/save; recomendação: cursor/`IDBKeyRange` por `createdAt`. Performance. → `/impeccable optimize`
- **[P2] `HistoryTable` sem virtualização, renderiza desktop+mobile sempre** (`HistoryTable/index.tsx:33-58`) — dobra nós de DOM independente do viewport. Performance. → `/impeccable optimize`
- **[P2] Zero uso de `React.memo`/`useMemo`/`useCallback`** nos componentes de design-system e em `App.tsx:410-423` (`latest` recalculado a cada render). Performance. → `/impeccable optimize`
- **[P2] Tokens legados divergentes ainda no repo** — `src/design-system/tokens/*.ts` + `theme/{darkTheme,lightTheme}.ts`: valores diferentes de `tokens.css`, sem uso real além do tipo `QualityLevel`. Risco: alguém importar por engano e quebrar consistência de tema. Theming/hygiene — recomendo remoção direta (fora do escopo dos comandos de design).
- **[P2] Ícones de 40px e segmented control de 32px** abaixo do alvo de 44px (`.sq-icon-button`, `.sq-action-card__button`, `.install-prompt-banner__dismiss`, `.sq-segmented-control__option`). Responsivo. → `/impeccable adapt`
- **[P2] `sq-metrics-grid` só colapsa 4→2 colunas**, sem 1 coluna em ≤400px; números de 21px ficam apertados em telas de 320-360px. Responsivo. → `/impeccable adapt`
- **[P2] Grid de 4 cards do `AboutScreen`** é o template reconhecível de "feature grid" SaaS (conteúdo distinto, esqueleto genérico). Anti-Pattern. → `/impeccable layout`
- **[P2] Dois componentes prontos e nunca usados** — `ActionCard` e `SpeedHeroCard` — duplicam padrões refeitos manualmente em `HomeScreen`/`ResultScreen`. Anti-Pattern/hygiene — recomendo deletar ou consolidar (fora do escopo dos comandos de design).
- **[P2] JSX duplicado byte-a-byte** entre `ResultScreen.tsx:115-156` e `TestDetailScreen.tsx:115-156` (incluindo helpers `metricVerdict`). Manutenibilidade — extrair `MetricsGrid` compartilhado (engenharia, não design).

### P3 — Polimento

- Sombra hard-coded em `.sq-segmented-control__option--active` (`styles.css:1398`) — invisível no dark mode. → `/impeccable polish`
- `#ffffff` hard-coded em 3 lugares de texto-sobre-acento em vez de um token `--on-accent`. → `/impeccable polish`
- `.sq-card--tonal` usa `--accent-container` (token de fill) como borda — quase invisível no dark mode. → `/impeccable polish`
- Font Roboto sem `preconnect` para `fonts.googleapis.com`/`fonts.gstatic.com`. → `/impeccable optimize`
- `ProgressRing`/`StepTracker` recriam objetos/strings estáveis a cada render sem memoização. → `/impeccable optimize`
- Eyebrow acima do `<h1>` em `ReportPage.tsx:36` é o único ponto que soa "hero de marketing" em vez de tom de produto. → `/impeccable quieter`
- `<summary>` de `DiagnosisResultPanel.tsx:55-56` cai fora do seletor global de `focus-visible`. → `/impeccable polish`
- `HistoryTable` marcado com divs em vez de `<table>` real — perde associação de coluna para leitores de tela em modo tabela. → `/impeccable harden`

## Padrões e Problemas Sistêmicos

- **`ReportPage` é o outlier sistêmico do app**: concentra sozinha 3 achados independentes (sem shell responsivo, sem vocabulário de componente, eyebrow fora do tom) — sinal de que foi construída numa passada isolada e nunca reconciliada com o resto.
- **Contraste de `--text-tertiary` é um problema de token, não pontual**: aparece em 5+ componentes não relacionados — a correção certa é no token, não caso a caso.
- **Scaffolding morto recorrente**: tokens legados (`tokens/*.ts`), dois componentes nunca usados (`ActionCard`, `SpeedHeroCard`) e JSX duplicado entre duas telas — típico de geração iterativa sem passo de consolidação.
- **Alvos de toque abaixo de 44px se repetem** em 4 componentes diferentes (botão voltar, ícone genérico, segmented control) — gap sistêmico de dimensionamento, não isolado.

## Pontos Positivos

- Nenhum `div onClick` disfarçado de botão em todo o app; todo elemento interativo é `<button>`/`<a>` nativo.
- Todo botão icon-only tem `aria-label` correto (8+ instâncias verificadas).
- Foco visível nunca é suprimido — zero `outline:none` no repo inteiro.
- `TopAppBar` usa `<nav aria-label>` + `aria-current="page"` corretamente.
- Arquitetura de tema é exemplar: cor nunca é decidida em JS, tudo resolve para `var(--token)`; `.dark` faz o trabalho sozinho.
- Speed test reporta progresso só em transição de fase (não a cada 100ms) — evita re-render excessivo por design.
- Zero `backdrop-filter`, zero layout thrashing, zero animação de propriedade de layout no codebase inteiro.
- Sistema de breakpoints (860/560) aplicado de forma consistente na maioria dos componentes; troca `HistoryTable`→cards é bem executada.
- Nenhum clichê visual clássico de IA (gradient text, glassmorphism, eyebrow numerado, card aninhado) em lugar nenhum.

## Ações Recomendadas (ordem de prioridade)

1. **[P1] `/impeccable polish`** — corrigir contraste de `--text-tertiary` no token (afeta 5+ lugares de uma vez) e reconstruir `ReportPage` sobre `AppShell`/`Button`/`Card`.
2. **[P1] `/impeccable harden`** — adicionar `role="progressbar"`/`aria-live` ao `ProgressRing`, semântica de radiogroup ao toggle de tema, e heading real nas 6 telas sem hierarquia.
3. **[P1] `/impeccable adapt`** — corrigir alvo de toque do botão voltar (24px→44px) e o aperto de padding do card de resultado em ≤360px.
4. **[P2] `/impeccable optimize`** — paginar `historyRepository.list()`, remover dupla renderização desktop+mobile do `HistoryTable`, memoizar componentes do design system.
5. **[P2] `/impeccable layout`** — quebrar o ritmo uniforme do grid de 4 cards do `AboutScreen`.
6. **[P3] `/impeccable polish`** — passe final: sombra hard-coded no dark mode, token `--on-accent`, borda do card tonal.

**Fora do escopo dos comandos de design** (hygiene de engenharia, recomendo tratar à parte): deletar `src/design-system/tokens/*.ts` + `theme/{darkTheme,lightTheme}.ts` (código morto divergente), deletar ou consolidar `ActionCard`/`SpeedHeroCard` (nunca usados), extrair `MetricsGrid` compartilhado entre `ResultScreen`/`TestDetailScreen`.

Re-rode `/impeccable audit` depois das correções para ver o score subir.
