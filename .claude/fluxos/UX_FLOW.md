# UX Flow

> **Fonte da verdade:** design system em [`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md) + skill [`SignallQ-design`](../skills/SignallQ-design/) e skill `/design-check` (piloto Fase 6 do épico #1623).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Sem agente Design dedicado

Design deixou de ser agente permanente ([ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md)). Direção visual é responsabilidade da **Claudete**; implementação é responsabilidade do **Camilo**; validação de qualidade fica com **Caio** no gate.

Skills que substituem o papel:

- **`/design-check`** — Camilo invoca durante a implementação para validar contra tokens, hierarquia, acessibilidade.
- **`SignallQ-design`** — biblioteca de tokens, componentes, wireframes, para gerar UI on-brand.
- **`auditar-ux`** — auditoria profunda de design system e usabilidade (invocada por Claudete ou Caio antes de release).

## Objetivos de UX

- **Consistência**: aderência estrita a Material Design 3 ([`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md)).
- **Acessibilidade**: contraste WCAG AA, targets de toque adequados.
- **Clareza de diagnóstico**: métrica crua sempre com veredito humano (Excelente / Bom / Regular / Fraco / Forte).

## Momento das skills

1. **Antes da implementação** — Claudete decide direção (microcopy, hierarquia, estado visual novo).
2. **Durante a implementação** — Camilo invoca `/design-check` para validar cada tela ou componente.
3. **Pós-implementação** — Caio confirma no gate que a UI não introduz regressão nem inconsistência.

Bug ou lógica pura sem impacto visual **pula** todas as skills de design.

## Referências

- [`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md) — tokens, tipografia, cores, componentes.
- [`docs_ai/FUNCIONAL.md`](../../docs_ai/FUNCIONAL.md) — apresentação de respostas de IA e fluxo de diagnóstico.
- Skills: [`SignallQ-design`](../skills/SignallQ-design/), [`auditar-ux`](../skills/auditar-ux/), [`impeccable`](../skills/impeccable/).
