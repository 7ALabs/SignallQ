# Task Breakdown

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Este arquivo é um resumo apontador.
> Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`.
> Versão: v0.23.0 · 2026-07-05.

## Quem quebra

A **Claudete** refina e quebra toda demanda. Absorveu o planejamento de arquitetura — não há tech lead separado. Skill de apoio: `/refinar-demanda`.

## Princípios

- **Modularidade** — alinhe com os módulos `:app`, `:core*`, `:feature*` (Android) e a estrutura do `SignallQ Admin/`.
- **Responsabilidade única** — cada sub-task tem objetivo verificável.
- **Independência** — tasks devem poder rodar em trilhas paralelas por agentes diferentes.
- **Dependências** — rastreie bloqueios e ordem de execução.

## Processo

1. **Analise** a task e colete contexto de `docs_ai/technical/` e do código (Read/Grep/Glob).
2. **Estime escopo** — se grande/arquitetural, proponha plano antes (ver classificação de tamanho em `.claude/CLAUDE.md`).
3. **Decomponha** — prefira várias tasks pequenas a uma gigante.
4. **Atribua** ao agente correto (tabela abaixo).
5. **Mapeie dependências** e ordene.
6. **Roteie** — bug → GitHub Issues; feature/task → Linear.

## Regra de granularidade

- Bugfix simples (≤5 arquivos, sem mudança de contrato) → implementador direto, sem breakdown formal.
- Tasks médias/grandes → Claudete decompõe antes de acionar o implementador.
- WIP: máximo 1 task In Progress por agente.

## Mapeamento de agentes

| Tipo de task | Agente |
|---|---|
| Refino, priorização, decomposição, arquitetura | Claudete |
| Implementação Android (Kotlin, Compose, MVVM) | Camilo |
| Admin Panel (React/TS) e análise de dados de app | Felipe |
| UX, design, Material 3, microcopy (task visual) | Lia |
| Review, QA, regressão, release, changelog, higiene | Gema |

Busca de código/docs = ferramentas nativas ou skills; sem agente dedicado.

## Referências

- `ai/AGENT_WORKFLOW.md` — fluxo completo
- `ai/HANDOFF_RULES.md` — protocolo de handoff
- `TECNICO.md / ARQUITETURA/MODULOS/` — módulos Android
