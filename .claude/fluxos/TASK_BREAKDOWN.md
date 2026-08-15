# Task Breakdown

> **Fonte da verdade:** [`ai-governance/agents/claudete.md`](../../../ai-governance/agents/claudete.md) + [`ai-governance/policies/demand-routing.md`](../../../ai-governance/policies/demand-routing.md). Este arquivo é um resumo apontador.
> **Decisão canônica:** [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md).
> **Última atualização:** 2026-08-15.

## Quem quebra

**Claudete** refina e quebra toda demanda. Consolida decisões de produto e critérios de aceite. Skill de apoio: [`estimativa-impacto`](../skills/estimativa-impacto/).

## Princípios

- **Modularidade** — alinhe com os módulos `:app`, `:core*`, `:feature*` (Android) e com os Workers Cloudflare em `integrations/cloudflare/`.
- **Responsabilidade única** — cada sub-task tem objetivo verificável.
- **Independência** — tasks devem poder rodar em trilhas paralelas por agentes diferentes.
- **Dependências** — rastreie bloqueios e ordem de execução.

## Processo

1. **Analise** a task e colete contexto de [`docs_ai/`](../../docs_ai/) e do código (Read/Grep/Glob).
2. **Estime escopo** — se grande/arquitetural, proponha plano antes (skill [`estimativa-impacto`](../skills/estimativa-impacto/)).
3. **Decomponha** — prefira várias tasks pequenas a uma gigante.
4. **Atribua** ao agente correto (tabela abaixo).
5. **Mapeie dependências** e ordene.
6. **Roteie** — bug → GitHub Issues (`type:bug`); feature/task → GitHub Issues (`Task -` / `Feat -`).

## Regra de granularidade

- Bugfix simples (≤5 arquivos, sem mudança de contrato) → Camilo direto, sem breakdown formal.
- Tasks médias/grandes → Claudete decompõe antes de acionar Camilo.
- WIP: máximo 1 task In Progress por agente.

## Mapeamento de agentes (SignallQ)

| Tipo de task | Agente |
|---|---|
| Refino, priorização, decomposição, critérios de aceite | Claudete |
| Implementação Android (Kotlin, Compose, MVVM, Room, Coroutines) | Camilo |
| Implementação Workers Cloudflare, Admin (React/TS) | Camilo |
| UX, design, MD3, microcopy (task visual) — spec e revisão | Juliana |
| Métrica, telemetria, observabilidade — spec | Gustavo |
| Growth, ASO, campanhas | Marcos |
| Revisão independente (código, segurança, testes, regressão) | Caio |
| Escalação (estratégia, marca, produção, custo, irreversível) | Luiz |

Busca de código/docs = ferramentas nativas (Read/Grep/Glob) ou skills; sem agente dedicado.

## Referências

- [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md) — fluxo completo
- [`HANDOFF_RULES.md`](HANDOFF_RULES.md) — protocolo de handoff
- [`docs_ai/ARQUITETURA/MODULOS/`](../../docs_ai/ARQUITETURA/MODULOS/) — módulos Android
- [`ai-governance/policies/demand-routing.md`](../../../ai-governance/policies/demand-routing.md) — roteamento por domínio
