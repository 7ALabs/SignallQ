# UX Flow for Agents

> **Fonte da verdade:** [`ai-governance/agents/juliana.md`](../../../ai-governance/agents/juliana.md) + design system em [`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md) e skill [`SignallQ-design`](../skills/SignallQ-design/). Este arquivo é um resumo apontador.
> **Decisão canônica:** [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md).
> **Última atualização:** 2026-08-15.

## Objetivos de UX

- **Consistência**: aderência estrita a Material Design 3 ([`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md)).
- **Acessibilidade**: contraste WCAG AA e targets de toque adequados.
- **Clareza de diagnóstico**: métrica crua sempre com veredito humano (Excelente / Bom / Regular / Fraco / Forte).

## Gate de UX condicional

**Juliana** entra **antes** da implementação **apenas** quando a mudança é visual/de fluxo:

- Tela nova ou modificação de tela existente.
- Estado visual novo: loading, vazio, erro, sucesso, thinking.
- Texto/microcopy visível ao usuário (incluindo resposta de IA/diagnóstico).
- Mudança de fluxo de navegação.

Bug ou lógica pura, e mudanças em `:core*` sem reflexo visual, **pulam Juliana** — reduz latência sem perder qualidade onde importa.

## Dois momentos de Juliana

1. **Antes da implementação** — valida que estados visuais e microcopy estão mapeados na spec.
2. **Pós-implementação** — confirma o entregável real (junto ao gate de revisão do Caio).

## Papéis

| Agente | Responsabilidade |
|---|---|
| Juliana | UI, MD3, microcopy, acessibilidade, estados visuais — produz spec e revisa; não edita código Kotlin diretamente |
| Camilo | Implementa a UI Android e Admin conforme spec da Juliana |
| Caio | Valida no gate final que a implementação não introduz bug/regressão e que os critérios de aceite estão atendidos |

## Referências

- [`docs_ai/DESIGN_SYSTEM.md`](../../docs_ai/DESIGN_SYSTEM.md) — tokens, tipografia, cores, componentes
- [`docs_ai/FUNCIONAL.md`](../../docs_ai/FUNCIONAL.md) — apresentação de respostas de IA e fluxo de diagnóstico
- Skills: [`SignallQ-design`](../skills/SignallQ-design/), [`auditar-ux`](../skills/auditar-ux/), [`impeccable`](../skills/impeccable/)
