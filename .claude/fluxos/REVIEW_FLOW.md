# Review Flow for Agents

> **Fonte da verdade:** [`ai-governance/agents/caio.md`](../../../ai-governance/agents/caio.md) e [`ai-governance/policies/agent-operating-contract.md`](../../../ai-governance/policies/agent-operating-contract.md). Este arquivo é um resumo apontador.
> **Decisão canônica:** [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md).
> **Última atualização:** 2026-08-15.

## Revisor independente: Caio

**Caio** é o revisor independente do squad. Faz parecer sobre riscos, critérios de aceite, segurança, testes, regressão, qualidade de código e prontidão de release. **Não implementa a entrega que revisa** — devolve bloqueios ao autor.

Revisão é obrigatória quando houver: código, segurança, produção, regressão, arquitetura ou risco relevante ([contrato operacional §6 e §9](../../../ai-governance/policies/agent-operating-contract.md)).

## Processo

1. **Gatilho** — Camilo conclui a implementação e abre PR para revisão.
2. **Checks automáticos** — devem passar antes de acionar Caio: `./android/gradlew ktlintCheck detekt test` (Android) ou `npm run lint && npm run build` (Admin/Workers).
3. **Revisão do Caio** — bugs, regressões, risco técnico, aderência a critérios de aceite, testes faltando, segurança, qualidade de código.
4. **UX condicional** — Juliana valida o entregável visual quando a mudança foi de tela/fluxo (segundo momento de Juliana; primeiro foi antes da implementação).
5. **Veredito** — `Aprovado` / `Aprovado com ressalvas` / `Reprovado`.

## Limite de loop

Ciclo Caio ↔ Camilo tem no máximo **2 rodadas**. Na 3ª divergência, escala para **Claudete** decidir (aceitar débito, repriorizar ou reescopar). Evita ping-pong infinito.

## Escalação ao Luiz

Caio eleva ao Luiz quando o parecer envolver: aceite de risco crítico, exceção de segurança, redução de cobertura crítica, publicação sem evidência suficiente ou mudança material de controles ([ai-governance/agents/caio.md](../../../ai-governance/agents/caio.md)).

## O que Caio não faz

- Não implementa correções — devolve ao implementador.
- Decisão de produto/prioridade vai para Claudete.
- Decisão de arquitetura material vai para Claudete + Camilo com escalação ao Luiz.

## O que Juliana não faz

- Não edita lógica de negócio — apenas spec de UI e microcopy.
- Não aprova UX de feature visual que não passou por ela antes da implementação.

## Referências

- [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md) — fluxo completo
- [`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md) — referência de revisão visual
- [`docs_ai/ARQUITETURA/README.md`](../../docs_ai/ARQUITETURA/README.md) — referência de revisão técnica
