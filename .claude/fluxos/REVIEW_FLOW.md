# Review Flow

> **Fonte da verdade:** [`.claude/agents/caio.md`](../agents/caio.md) + [contrato operacional](../../../ai-governance/policies/agent-operating-contract.md).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Único gate: Caio

**Caio** é o revisor independente. Adversarial por profissão — assume bug até prova em contrário. Não implementa o que revisa ([contrato op §7](../../../ai-governance/policies/agent-operating-contract.md)).

Revisão é obrigatória quando houver: **código, segurança, produção, regressão, arquitetura ou risco relevante**. Docs puras podem passar direto (`/check-done --sem-caio` com justificativa registrada).

## Processo

1. **Gatilho** — Camilo conclui e aciona via `/handoff`.
2. **Checks automáticos** — Ktlint, Detekt, test (Android); npm test (Web/Workers); docs-CI. Devem estar verdes antes de Caio entrar em review humano.
3. **Revisão adversarial** de Caio:
   - **Segurança** — OWASP, secret em diff, endpoint sem auth, permissão nova, dado sensível.
   - **Regressão** — refactor sem teste de caracterização em fluxo crítico.
   - **Contrato** — compatibilidade com consumidores quando muda API/Worker/Room migration.
   - **Duplicação** — grep antes de aceitar código novo.
   - **Dívida líquida** — a PR reduz ou aumenta débito do repo?
4. **Parecer** — Aprovado / Aprovado com ressalvas / Reprovado, com bloqueios objetivos.
5. **Loop máx 2 rodadas** — na 3ª divergência, escala Claudete.

## Escalação ao Luiz

Caio escala apenas para: aceite de risco crítico, exceção de segurança, redução de cobertura crítica, publicação sem evidência suficiente, mudança material de controles ([`caio.md`](../agents/caio.md)).

## O que Caio não faz

- Não implementa correções — devolve a Camilo.
- Decisão de produto/prioridade vai para Claudete.
- Não cede a pressão de prazo — reporta risco, Luiz decide se aceita.

## Model / effort

- **Opus sempre.** Cortar custo no único gate é cortar o custo errado.
- **Effort:** high por default, xhigh/max em PR grande ou security-critical.

## Referências

- [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md)
- [`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md), [`docs_ai/ARQUITETURA/README.md`](../../docs_ai/ARQUITETURA/README.md)
