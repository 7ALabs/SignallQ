# Agent Workflow

> **Fonte da verdade:** [`.claude/agents/`](../agents/) (personas com personalidade) + [`ai-governance/policies/`](../../../ai-governance/policies/) (contrato op).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Squad (3 agentes)

- **[Claudete](../agents/claudete.md)** — Head de Produto. Prioriza, decompõe, decide critérios de aceite. Absorve design e growth via skills.
- **[Camilo](../agents/camilo.md)** — Principal Engineer. Android + Web + Workers + Admin — dev técnico único.
- **[Caio](../agents/caio.md)** — Principal Reviewer. Único gate independente antes de merge. Não implementa.

Design (Juliana), Growth (Marcos), Dados (Gustavo) do squad antigo viraram **skills invocáveis** (`/design-check`, `/growth-check`, `/analytics-spec`) — não são agentes permanentes.

## Fluxo

1. **Claudete** refina a demanda, decompõe em tasks pequenas e independentes, escreve critérios de aceite.
2. **Camilo** implementa. Antes de escrever código, roda `/inventario` (Fase 2 do épico #1623 — bloqueia duplicação). Chama `/design-check` quando a mudança é visual, `/analytics-spec` quando precisa spec de telemetria, `/growth-check` quando envolve ASO/store.
3. **Caio** revisa. Único gate. Loop máx 2 rodadas; 3ª divergência escala Claudete.

WIP: máx 1 task In Progress por agente.

Aprovações materiais (estratégia, marca, produção, custo recorrente, mudança irreversível, risco crítico) exigem **Luiz**.

## Model / effort

Cada agente escala modelo e effort por complexidade (regras nas personas):

- **Claudete** — Sonnet default; Opus para roadmap trimestral; Haiku para triagem.
- **Camilo** — Sonnet default; Opus para arquitetura material ou security-sensitive; Haiku para bugfix trivial.
- **Caio** — Opus sempre (revisão é o gate — cortar custo aqui é cortar o custo errado).

Objetivo: melhor custo × benefício em velocidade, custo e qualidade.

## Handoff

Estado do trabalho vive em **GitHub Issues** + **GitHub PR**. Skill `/handoff` (piloto #1620) formaliza. Scripts em `scripts/legacy/` estão depreciados.

Roteamento: **bug** → GitHub Issues (`type:bug`); **feature/task/refactor/docs** → GitHub Issues (`Task -` / `Feat -`).

## Build/Verify

- `./android/gradlew ktlintCheck detekt test` — Android
- `bash scripts/validar-docs.sh --base origin/main` — docs
- `/check-done` (piloto #1620) — antes de declarar concluído

Detalhes: [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md).
