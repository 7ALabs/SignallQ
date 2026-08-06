# Agent Workflow

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Este arquivo é um resumo apontador — se divergir, valem aqueles.
> Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`.
> Versão: v0.23.0 · 2026-07-05.

## Squad (5 agentes)

- **Claudete** — PM & Tech Lead. Refina, prioriza, quebra tasks, controla WIP, decide Done. Absorveu o planejamento de arquitetura.
- **Camilo** — Dev Android (Kotlin/Compose).
- **Felipe** — Admin Panel (React/TS) e análise de dados de app.
- **Lia** — UX & Design (Material 3, design system).
- **Gema** — QA, Release & Higiene. Gate único de Done.

Definições completas em `.claude/agents/{claudete,camilo,felipe,lia,gema}.md`.

## Fluxo

1. **Claudete** refina a demanda e quebra em tasks pequenas e independentes.
2. **Lia** revisa antes da implementação **apenas** quando a mudança é visual/de fluxo (tela nova, layout, navegação, microcopy). Bug/lógica pura pula a Lia.
3. **Camilo / Felipe / Lia** implementam em **trilhas independentes, em paralelo**.
4. **Gema** é o gate único: review + QA + release + higiene. Loop Gema → implementador tem no máximo 2 rodadas; na 3ª, escala para Claudete.

WIP: máximo 1 task In Progress por agente.

## Handoff

Estado do trabalho vive no **Linear** (status da issue) + **GitHub** (PR). O Linear notifica o Slack direto. Scripts `agent-handoff.sh`/Discord estão **depreciados** — não são mecanismo de handoff.

Roteamento: bug → GitHub Issues; feature/task/daily → Linear.

## Build/Verify

- `.\android\gradlew.bat build` — build completo
- `.\android\gradlew.bat lint` — análise estática
- `.\android\gradlew.bat test` — testes unitários

Detalhes: `TECNICO.md`.
