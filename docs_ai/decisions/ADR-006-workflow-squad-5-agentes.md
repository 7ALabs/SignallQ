# ADR-006 — Redesign do fluxo do squad de 5 agentes

> **⚠️ SUPERSEDED por [ADR-014](ADR-014-squad-canonico-ai-governance.md) em 2026-08-15.**
> O squad de 5 (Claudete, Camilo, Felipe, Lia, Gema) documentado aqui deixou de ser vigente.
> Felipe foi demitido em 2026-07-09, Lia em 2026-07-25; Gema nunca foi formalizada em
> `ai-governance/`. A fonte da verdade atual é `ai-governance/agents/` (6 agentes aplicáveis
> ao SignallQ: Claudete, Camilo, Juliana, Marcos, Gustavo, Caio). Este ADR permanece como
> registro histórico da decisão original.

- Status: Superseded (2026-08-15) — originalmente Aceito em 2026-07-05
- Data: 2026-07-05
- Autor: Claudete (PM & Tech Lead)
- Contexto de versão: v0.23.0

## Contexto

O squad foi reduzido para 5 agentes (Claudete, Camilo, Felipe, Lia, Gema) — papéis de
Cláudio (arquitetura), Nina/Taisa (docs/versão) e Marcelo/Otávio (busca/APIs) foram
absorvidos por agentes remanescentes ou viraram skills (`/regras-android`,
`/regras-diagnostico-rede`, `/gerar-docs`). A documentação de fluxo em `docs_ai/ai/*`
descrevia o squad antigo (9+ agentes), handoff via `scripts/agent-handoff.sh` + Discord
e um `PIPELINE_AUTONOMO.md` aspiracional nunca implementado. Havia **duas fontes da
verdade divergentes**: `.claude/CLAUDE.md`/`.claude/agents/*` (atual) e `docs_ai/ai/*`
(defasado), o que gerava retrabalho e onboarding errado.

## Decisão

### 1. Fonte única da verdade
`.claude/CLAUDE.md` + `.claude/agents/*.md` governam o squad e o fluxo. `docs_ai/ai/*`
passa a ser **resumo apontador** para essas fontes, nunca verdade paralela. Isso mata a
deriva que criou o problema.

### 2. Fluxo enxuto com paralelismo
Claudete refina e quebra a demanda → implementadores (Camilo / Felipe / Lia) atuam em
**trilhas independentes em paralelo** → Gema é o **gate único de Done** (review + QA +
release + higiene). Não há handoff sequencial de tech lead — Claudete já absorveu esse
papel.

### 3. Gate de UX condicional
Lia revisa **antes** da implementação apenas quando a mudança é visual/de fluxo (tela
nova, layout, navegação, microcopy). Correção de lógica/bug puro **pula Lia** — reduz
latência de handoff sem perder qualidade onde importa.

### 4. Limite de loop de revisão
Ciclo Gema → implementador tem no máximo **2 rodadas**. Na 3ª divergência, escala para
Claudete decidir (aceitar débito, repriorizar ou reescopar). Evita ping-pong infinito.

### 5. Disciplina de WIP
Máximo **1 task In Progress por agente** por vez (squad pequeno). Claudete monitora WIP e
bloqueios via Linear na Review de Bloqueios.

### 6. Handoff via Linear + PR
O estado de trabalho vive no **Linear** (status da issue) e no **GitHub** (PR). Os scripts
`scripts/agent-handoff.sh`, `notify.sh`, `discord_notify.sh` e o board Discord ficam
**depreciados** — não são mais o mecanismo de handoff documentado. O Linear notifica o
Slack diretamente (não criar fluxo manual paralelo). Os scripts permanecem no repo por ora
mas não são referência de processo.

### 7. Roteamento de trabalho
- **Bug** → GitHub Issues (label `type:bug`).
- **Feature / task / daily** → Linear (projeto SignallQ).
Já codificado; reforçado aqui.

## Consequências

- `docs_ai/ai/*` reescritos para o squad de 5 e apontando para `.claude/CLAUDE.md`.
- `docs_ai/operations/PIPELINE_AUTONOMO.md` arquivado (aspiracional, nunca implementado).
- Menos handoffs por demanda (UX condicional, sem tech-lead sequencial) e sem loops de
  revisão abertos.
- Onboarding de novo agente lê uma fonte só, não duas divergentes.

## Pendências para o Luiz (não implementadas nesta rodada)

- **WIP/fila no Linear**: criar custom field ou view por agente para Claudete monitorar WIP
  — requer configuração no Linear (fora do escopo de código).
- **Depreciar de fato os scripts de handoff Discord**: mover `scripts/agent-handoff.sh`,
  `discord_notify.sh`, `slack_notify.sh` para `scripts/legacy/` se confirmado que nada em
  CI/hooks os invoca — validar antes de mover.
