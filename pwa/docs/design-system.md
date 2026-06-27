# SignallQ PWA Design System

## Decisão M0

O Design System oficial da PWA fica centralizado em `src/design-system/`.

A base estrutural é Material Design 3: hierarquia clara, superfícies limpas, estados previsíveis, radius moderado, CTA evidente e linguagem direta.

O Google Fiber Speed Test serve apenas como inspiração de clareza: tela limpa, espaçamento generoso, número principal grande e ação primária forte. A PWA não copia identidade visual, cores, layout, assets ou marca do Google Fiber.

## Navegação

A PWA não usa bottom navigation.

Bottom navigation fica reservada para o Android nativo. Na PWA, a navegação deve usar:

- header/top navigation;
- CTA principal;
- cards contextuais;
- links ou botões de apoio quando houver necessidade real.

A paridade com Android vem de tokens, componentes, linguagem, classificação de qualidade e fluxo de diagnóstico, não de copiar a estrutura nativa.

## Tokens disponíveis

Arquivos em `src/design-system/tokens/`:

- `colors.ts`: light/dark, `primary`, `onPrimary`, `primaryContainer`, `background`, `surface`, `surfaceVariant`, `outline`, `error`, `success`, `warning`, `info`, `download`, `upload`, `latency`, `stability`, `diagnostic` e `quality`.
- `typography.ts`: família, tamanhos, pesos e alturas de linha.
- `spacing.ts`: escala de espaçamento.
- `radius.ts`: radius de componentes e pill.
- `elevation.ts`: sombras Material-like discretas.
- `motion.ts`: durações e easing.

Temas:

- `theme/lightTheme.ts`
- `theme/darkTheme.ts`
- `theme/ThemeProvider.tsx`

O `ThemeProvider` aplica CSS variables. Componentes devem consumir essas variáveis em vez de espalhar cores e espaçamentos hardcoded.

## Componentes base

- `AppShell`: largura máxima, header e área principal.
- `TopAppBar`: marca, navegação superior e ações.
- `Button`: CTA primário, secundário, tonal, texto, loading e disabled.
- `Card`: superfície base, outlined e tonal.
- `ActionCard`: card clicável/contextual para histórico, diagnóstico e ajustes.
- `EmptyState`: tela ou bloco vazio.
- `ErrorState`: erro claro e acionável.
- `LoadingState`: skeleton simples.

## Componentes SignallQ

- `SpeedHeroCard`: destaque principal da tela inicial ou resultado.
- `MetricTile`: métrica curta com unidade, status e explicação.
- `QualityBadge`: classificação visual de qualidade.
- `ConnectionSummaryCard`: resumo principal da conexão.
- `DiagnosisInsightCard`: insight curto de diagnóstico.
- `RecommendationList`: ações recomendadas.
- `NetworkContextCard`: contexto técnico permitido pelo navegador.

## Patterns

- `HomeLayout`: primeira tela da PWA, mobile-first, sem bottom bar.
- `ResultLayout`: resultado com conteúdo principal e apoio lateral.
- `DiagnosisLayout`: empilhamento de insights e recomendações.

## Quando usar

Use `SpeedHeroCard` para a ação ou resultado principal. Não coloque vários heróis na mesma tela.

Use `MetricTile` para download, upload, latência, jitter ou estabilidade. Se a métrica não foi medida, mostre `--` ou texto equivalente e explique a limitação.

Use `DiagnosisInsightCard` para uma conclusão curta. Não use para tese longa ou chat livre.

Use `RecommendationList` quando houver ações concretas. Evite recomendações genéricas que o app não consegue validar.

Use `NetworkContextCard` para expor limitações do navegador em linguagem simples.

## Linguagem de diagnóstico

Boa:

- "Sua internet está rápida, mas oscilou durante o teste."
- "Não conseguimos medir o Wi-Fi detalhado no navegador."
- "A latência foi estimada por uma requisição web, não por ping ICMP."

Ruim:

- "Sua conexão apresenta degradação estatística severa com instabilidade na malha."
- "Detectamos RSSI baixo no Wi-Fi."
- "O app encontrou todos os dispositivos conectados na rede."

## Regras de implementação

- Não criar `BottomNavigation` para a PWA neste M0.
- Não usar visual gamer, neon ou dashboard poluído.
- Não criar gráfico pesado sem necessidade de produto.
- Não transformar a PWA em Android encapsulado.
- Não implementar Speed Test, DNS, histórico ou diagnóstico IA além do necessário para validar UI de SIG-41.
- Não inventar métricas; quando o browser não mede, a UI deve dizer isso.
- Não alterar segredos ou variáveis de ambiente para Design System.

## Critérios visuais

- Funciona bem em mobile.
- CTA "Iniciar teste" aparece com destaque.
- O resultado principal é mais evidente que os detalhes.
- Velocidade e estabilidade aparecem como conceitos separados.
- Estados de carregamento, vazio e erro existem como componentes.
- A tela usa header/top navigation e cards contextuais, sem bottom navigation.
