# Decisão — separar Design System de Protótipos no Claude Design (2026-07-18)

- **Status:** ativo — Fase 1 em execução
- **Última validação:** 2026-07-18
- **Fonte de verdade:** projeto Claude Design `SignallQ Design System` (`e77ea465-291f-4bf5-930c-a267680da04e`) + `packages/design-system/`
- **Responsável:** Claudete (decisão), execução via `/design-sync`
- **Decisão do Luiz:** 2026-07-18 — telas/fluxos não pertencem ao design system

## Contexto / problema

O "gêmeo digital" React (`packages/design-system`) foi sincronizado ao Claude Design incluindo, dentro
da **biblioteca de componentes do DS** (`components/`), as **telas do app** (`components/screens/*`) e
as **25 sheets** (`components/sheets/*`). Isso é uma conflação: telas/sheets são composições de produto,
não peças reutilizáveis de design system. O DS deve conter só o reutilizável — tokens, primitivos,
layout, animações.

Consequência prática descoberta em 2026-07-18: o `src` local tem **19 componentes**; o remoto tem **50**
(os 31 extras — sheets + telas Dispositivos/DNS/Fibra/Laudo/Onboarding/Privacidade/Operadora — vieram do
gêmeo, sincronizados no remoto mas com `src` nunca mergeado, PRs #899/#900 fechadas). Um rebuild do
`_ds_bundle.js` a partir do `src` local regrediria o bundle de 50→19. Ver
[[project_designsync_bridge_e_estrutura]] (memória).

## Decisão

Separar, no Claude Design, o **Design System** (reutilizável) dos **Protótipos/Fluxos** (telas do app +
Admin). A separação **dissolve o 19-vs-50**: os 31 "faltantes" são justamente telas/sheets que não devem
estar no DS; o reutilizável (~13) está todo no `src` local, então o rebuild sai completo e com a paleta
nova (`#5B21D6`) — corrigindo de brinde o `_ds_bundle.js` stale.

### Alvo

- **Design System** (`components/` do projeto `e77ea465`, painel + bundle + o que o agente compõe): só
  tokens + primitivos (Avatar, Badge, Icon, SignalBars) + layout (BottomNav, Card, Overline, PhoneFrame,
  ScreenScroll, SheetFrame, StatusBar, TopBar) + animações (Thinking, TypeOut).
- **Protótipos/fluxos**: `templates/signallq-app-*`, `tobe/*`, docs do Admin — documentos de fluxo, fora
  da biblioteca de componentes. **Já estão separados como documentos** — a única bagunça é a duplicação
  das telas/sheets dentro de `components/`.

### Fase 1 — núcleo (mesmo projeto)

1. `config.json` → `componentSrcMap` excluindo as 6 telas locais (AjustesScreen, HistoricoScreen,
   HomeScreen, SignallQScreen, SinalScreen, SpeedFlow) do sync.
2. Rebuild + converter (13 reutilizáveis, paleta nova) → validar (render-check pulado, sem Playwright —
   mudança é de token, não estrutural).
3. Upload (writes dos 13 reutilizáveis + bundle + styles) + **deletes de reconciliação** removendo
   `components/screens/**` + `components/sheets/**` do remoto (vestigiais — os fluxos usam `tobe/`, não
   elas; confirmado que não quebra nada).
4. `SheetFrame` era remote-only (sem `src` local). `src/layout/SheetFrame.tsx` foi **re-adicionado**
   (idioma `LK` do pacote, base no `tobe/primitives.jsx` — superfície baixa, cantos 28dp, alça,
   conteúdo rolável) e re-sincronizado. Resultado final da Fase 1: **14 componentes reutilizáveis**,
   todos working na paleta nova.

### Fase 2 — CONCLUÍDA (2026-07-18)

Executada **invertida** vs. o rascunho: como a API do `DesignSync` não renomeia projeto e mover ~170
arquivos de protótipo (com binários) seria grande/arriscado, em vez de mover os fluxos, o **DS foi
movido para um projeto novo** e o antigo virou o de protótipos.

- Luiz renomeou `e77ea465` → **"SignallQ — Protótipos"** no claude.ai/design (rename só existe na UI).
- Criado o projeto **`2d25d7a1-31b2-4ac3-881f-72dbc8f35a29` "SignallQ Design System"** com os 14
  componentes reutilizáveis (push do `ds-bundle` local — 75 arquivos).
- `config.json` repontado pro novo `projectId`; `conventions.md` atualizada.
- Os 14 componentes + 13 previews removidos do painel do `e77ea465` (69 arquivos) — agora só fluxos.
  `_ds_bundle.js`/`styles.css` mantidos lá como runtime dos protótipos (helmets referenciam).

**Resultado:** DS puro (14 reutilizáveis) em `2d25d7a1`; protótipos (`tobe/`, `templates/`, `uploads/`)
em `e77ea465`. O `/design-sync` agora fixa só o projeto do DS.

## Snapshot / segurança (antes do delete)

Os `components/screens|sheets` a deletar são **artefatos derivados** do gêmeo (stubs de re-export +
previews gerados), não a fonte real (que estava nas PRs fechadas). As telas em si **sobrevivem** nos
fluxos `tobe/`. Snapshot registrado: manifesto `_ds_sync.json` (50 componentes) arquivado em
`docs_ai/design-system/_archive/claude-design-manifesto-2026-07-18/`. Delete é feito **por último**, após
o upload do novo estado ser verificado — janela em que ambos coexistem.

## Riscos + rollback

- Delete é permanente no Claude Design (sem archive nativo) → snapshot do manifesto + telas preservadas em
  `tobe/` mitigam.
- Estado seguro é o remoto atual (50, paleta velha); se o re-sync falhar, o `_ds_sync.json` (âncora) não é
  subido e o próximo sync reconcilia.
- Não toca código do app Android.

## Substitui / relaciona

- Não substitui docs. Relaciona-se com [[project_gemeo_digital_descontinuado]] (o gêmeo foi descontinuado;
  esta decisão limpa o resíduo dele no Claude Design) e com o alinhamento de paleta de 2026-07-13.
