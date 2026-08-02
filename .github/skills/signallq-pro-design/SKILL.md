---
name: signallq-pro-design
description: Use esta skill para gerar interfaces e assets on-brand do SignallQ Pro — o app Android profissional para técnicos de rede (visita, medição por ambiente, walk test, speedtest, evidências, laudo, Pix, recibo). Identidade azul (#0B6CFF) com ciano técnico (#006B76) e roxo de apoio (#6558E8), Material 3, dois temas oficiais (claro e escuro). Fundações e componentes de medição compartilhados com o SignallQ consumidor; marca própria. Vale para protótipo/mock e, no futuro, código de produção do app Pro.
user-invocable: true
---

O SignallQ Pro é um produto **separado** do SignallQ consumidor (app próprio, `io.signallq.pro`, Firebase/Play próprios), hoje em **fase de especificação/design** — ainda não há código Android do Pro. Esta skill é para desenhar telas, protótipos e assets do Pro on-brand. Não implemente código Android do Pro sem instrução explícita.

## Fonte da verdade

1. **Projeto online (visual, autoritativo):** Claude Design **"SignallQ PRO - Design System"** — `77a19317-ea64-4e47-b55c-578eca776c09` (`https://claude.ai/design/p/77a19317-ea64-4e47-b55c-578eca776c09`). **Este projeto é a fonte da verdade visual do Pro e evolui** — sempre reler antes de desenhar (a identidade mudou de teal para azul em 2026-07-18; os valores abaixo são snapshot dessa data). Contém foundations (`foundations/tokens.html`, `status-and-charts.html`, `typography.html`, `elevation.html`, `shape.html`, `dark-mode.html`, `cards.html`, `adaptive-layout.html`, `usage-guidelines.html`, `brand.html`), `styles.css`, fontes (Google Sans Flex), exemplos de tela (`foundations/screen-*.html`) e 15 componentes. Ler/escrever via a tool `DesignSync` (só a sessão principal e a Lia têm — subagente pode não herdar; se faltar, reporta e a sessão principal faz a I/O).
2. **Spec textual (no repo):** `docs_ai/plataforma/10_SignallQ_Pro_Design_System_v5.md` e o dicionário canônico `docs_ai/plataforma/00_CANONICO_v5.md §5`. Onde o projeto online e o doc divergirem em cor/token, **o projeto online vence** (o doc é reconciliação textual, o projeto é o design vivo).

Não confundir com a skill `SignallQ-design` (consumer, paleta violeta) nem com os projetos [SignallQ Design System](https://claude.ai/design/p/2d25d7a1-31b2-4ac3-881f-72dbc8f35a29) e [SignallQ — Protótipos](https://claude.ai/design/p/e77ea465-291f-4bf5-930c-a267680da04e), que são do consumidor/Admin.

## Não-negociáveis para ficar on-brand (Pro)

- **Material 3** com color roles mapeados à identidade azul. Tokens `--md-sys-color-{role}` / `on-{role}` / `{role}-container` / `on-{role}-container`; extras do Pro em `--sqp-*`.
- **Dois temas oficiais (claro e escuro)** com estrutura, componentes e hierarquia **idênticos** — nenhum componente é exclusivo de um tema; tudo consome tokens semânticos via `data-theme`. Troca-se token, nunca layout. Exceção: **laudo técnico é sempre claro**.
- **Paleta — tema claro (do `foundations/tokens.html` real, snapshot 2026-07-18):**
  - **Primary — marca `#0B6CFF`** (azul) · Primary Container `#D8E7FF`. Domina CTAs e ações primárias.
  - **Secondary — ciano técnico `#006B76`** · Secondary Container `#A9EDF3`. Apoio (chips, destaques secundários).
  - **Tertiary — roxo de apoio `#6558E8`** · Tertiary Container `#E5DEFF`. Apoio (gráficos, realces) — nunca compete com o azul.
  - Success/Good `#1AA25A` · Warning/Attention `#E9AD27` · Error/Critical `#D9363E` · Error Container `#FFDAD6`
  - Background `#F7F9FC` · Surface `#FFFFFF` · Surface Container High `#E7ECF3` · Outline `#C4CBD5` · Divider `#E3E7EC` (`--sqp-color-divider`) · Inverse Surface `#252B33`
  - **Mortos — nunca usar:** `#6C2BFF` (primary antigo do consumer) e a paleta **teal-dominante** anterior do próprio Pro (`#006B73` como primary). O teal agora é só o Secondary `#006B76`.
- **Escala de sinal — 6 níveis (`foundations/status-and-charts.html`, `--sqp-status-*`):**
  - Claro: Excelente `#16A85A` · Bom `#1AA25A` · Atenção `#E9AD27` · Fraco `#ED7D2D` · Crítico `#D9363E` · Informação `#0B6CFF`
  - Escuro: Excelente/Bom `#32D978` · Atenção `#F5C451` · Fraco `#FF964F` · Crítico `#FF5F66` · Informação `#3D96FF`
- **Gráficos (`--sqp-chart-*`):** séries download / upload / latência / jitter / perda + `grid` e `reference` (linha tracejada). Linhas limpas, preenchimento suave, grade discreta; fundo branco no claro, surface no escuro, sem glow.
- **Cor nunca é o único sinal de estado** — sempre ícone + rótulo textual + valor. Estados semânticos: Excelente / Bom / Atenção / Fraco / Crítico / Não avaliado.
- **Tipografia:** Google Sans Flex (fallback Roboto → system-ui), uma família em todas as telas.
- **Grid e toque:** grid 8dp (passo fino 4dp para ícone/label); **alvo de toque mínimo 48×48dp em todo elemento acionável**, mesmo quando o ícone/conteúdo visual for menor (aplicar padding/`minimumInteractiveComponentSize()`); ícone padrão 24dp, ícone de chip/contexto 14–18dp (Material Symbols Outlined).
- **Profundidade — 4 níveis (ver `docs_ai/plataforma/10_SignallQ_Pro_Design_System_v5.md` §4.1 para a versão completa):**
  - **Nível 0 — Fundo** (`ProBackground` `#F7F9FC`): sem sombra, sem borda.
  - **Nível 1 — Conteúdo agrupado** (`ProSurface` `#FFFFFF`, reaproveitado — sem token novo): cards, listas, métricas. Diferença tonal com o fundo já basta; borda só quando dois cards brancos ficam colados sem espaçamento.
  - **Nível 2 — Interativo/destacado** (`ProSurfaceContainerHigh` `#E7ECF3` já existe + `ProSurfaceSelected` `#EAF2FF`, **novo**, ainda não implementado em código): selecionado, recomendação prioritária, controle em foco — sombra discreta, borda de destaque suave permitida.
  - **Nível 3 — Sobreposto** (`ProSurfaceOverlay` **novo**, ainda não implementado + `ProScrim` **novo**, `rgba(0,0,0,.5)` claro / `.6` escuro): dialogs, bottom sheets, menus, tooltips — scrim + elevação clara, nunca em card comum.
  - Regras: profundidade comunica hierarquia, nunca decoração; nunca borda+sombra+glow+gradiente juntos no mesmo elemento; sem glow permanente; sem glassmorphism como linguagem principal; card não-interativo nunca parece elevado.
- **Selo PRO:** caixa alta, cantos arredondados. Nunca "SignallQPRO" como palavra única; nunca redesenhar o símbolo SignallQ.
- **Copy PT-BR** com "você", sentence case em títulos, UPPERCASE em overlines, **sem emoji**. Métrica crua sempre com veredito humano. Linguagem em camadas: resumo simples para o cliente, detalhe técnico para o profissional.

## Componentes (15, no projeto online)

`TopBar`, `Navbar` (Início · Atendimentos · Clientes · Ajustes), `Button` (primário/secundário/texto/destrutivo), `TextField`, `StatusChip` (presets de status de atendimento/visita/medição — usar os presets `ATENDIMENTO_STATUS` do bundle, não hex avulso), `ListRow`, `VisitCard`, `EnvironmentCard`, `QualityGauge`, `ComparisonBlock` (antes/depois), `EvidenceChip`, `SyncBanner` (offline/pendente/sincronizado), `RecommendationBlock` (problema/impacto/ação/prioridade), `SignatureBlock` (assinatura/aceite), `StateCard` (vazio/erro/carregando/concluído). Bundle React: `window.SignallQPRODesignSystem_77a193`. Reaproveitar antes de criar novo — critério em `foundations/usage-guidelines.html`.

**Telas de referência** (`foundations/screen-*.html`, claro vs. escuro): Home, Walk Test, Medição por ambiente, Teste de velocidade, Atendimento, Histórico, Configurações, Laudo técnico.

## Relação com o consumidor

Fundações (grid, tipografia, shape, elevação, M3) e componentes de medição são **compartilhados** com o SignallQ consumidor; a **marca diverge de propósito**: consumer é violeta `#5B21D6` / secundário azul `#2851B8`; Pro é azul `#0B6CFF` de marca, com ciano `#006B76` e roxo `#6558E8` de apoio. São produtos com identidade própria — divergir na marca, compartilhar a fundação.

## Como usar

- Protótipo/mock: gerar HTML estático on-brand nos **dois temas** (ou editar o projeto online via DesignSync, incrementalmente, um componente por vez — nunca replace em massa). Ancorar em tela/fluxo real do Pro (`docs_ai/plataforma/09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md` + `foundations/screen-*.html`), montar como fluxo navegável — nunca grade de telas numeradas tipo apresentação.
- Sem outra instrução: reler o projeto online primeiro, então perguntar o que desenhar, fazer 2-3 perguntas e atuar como designer especialista do Pro.
