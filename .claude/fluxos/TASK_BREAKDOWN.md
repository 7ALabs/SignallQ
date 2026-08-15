# Task Breakdown

> **Fonte da verdade:** [`.claude/agents/claudete.md`](../agents/claudete.md).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Quem quebra

**Claudete** refina e quebra toda demanda. Skill de apoio: [`estimativa-impacto`](../skills/estimativa-impacto/).

## Princípios

- **Modularidade** — alinha com `:app`, `:core:*`, `:feature:*` e com os Workers Cloudflare.
- **Responsabilidade única** — cada sub-task tem objetivo verificável.
- **Independência** — tasks devem poder rodar em paralelo por agentes diferentes (embora este squad tenha 1 dev único, tasks paralelas ajudam a serializar sem bloquear).
- **Anti-duplicação** — Camilo roda `/inventario` antes de código novo (Fase 2 do épico #1623). Se algo parecido existe, ou reusa, ou justifica.

## Processo

1. **Analisa** a demanda e coleta contexto ([`docs_ai/`](../../docs_ai/), Read/Grep/Glob).
2. **Estima escopo** com [`estimativa-impacto`](../skills/estimativa-impacto/).
3. **Decompõe** em tasks pequenas. Prefere várias pequenas a uma gigante.
4. **Atribui** ao agente correto (tabela abaixo).
5. **Mapeia dependências** e ordena.
6. **Roteia** — bug → GitHub Issues (`type:bug`); feature/task/refactor/docs → GitHub Issues (`Task -` / `Feat -`).

## Regra de granularidade

- Bugfix simples (≤5 arquivos, sem mudança de contrato) → Camilo direto (Haiku), sem breakdown formal.
- Tasks médias/grandes → Claudete decompõe antes de acionar Camilo.
- WIP: máx 1 task In Progress por agente.

## Mapeamento (squad de 3)

| Tipo de task | Agente | Skill invocada durante |
|---|---|---|
| Refino, priorização, decomposição, critérios de aceite | Claudete | `/estimativa-impacto` |
| Implementação Android (Kotlin/Compose/MVVM/Room/Coroutines) | Camilo | `/regras-android`, `/padroes-compose` |
| Implementação Workers/Admin (TS/React) | Camilo | `/cloudflare-d1-console` |
| Task visual (tela, microcopy, navegação) | Camilo | `/design-check` |
| Task com telemetria/métrica | Camilo | `/analytics-spec` |
| Task de growth (ASO, copy de store, campanha) | Claudete | `/growth-check` |
| Revisão independente | Caio | `/check-done` |
| Escalação (estratégia/marca/produção/custo/irreversível/risco crítico) | Luiz | — |

Busca de código/docs = ferramentas nativas (Read/Grep/Glob) ou skills.

## Referências

- [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md), [`HANDOFF_RULES.md`](HANDOFF_RULES.md)
- [`docs_ai/ARQUITETURA/MODULOS/`](../../docs_ai/ARQUITETURA/MODULOS/)
