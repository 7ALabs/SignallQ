# UX Flow for Agents

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Este arquivo é um resumo apontador.
> Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`.
> Design system: `docs_ai/design-system/` e skill `/SignallQ-design`.
> Versão: v0.23.0 · 2026-07-05.

## Objetivos de UX

- **Consistência**: aderência estrita a Material Design 3 (`design-system/MD3_GUIDELINES.md`).
- **Acessibilidade**: contraste (`design-system/COLORS.md`) e targets de toque adequados.
- **Clareza de diagnóstico**: métrica crua sempre com veredito humano (Excelente/Bom/Regular/Fraco/Forte).

## Gate de UX condicional

**Lia** entra **antes** da implementação **apenas** quando a mudança é visual/de fluxo:
- Tela nova ou modificação de tela existente.
- Estado visual novo: loading, vazio, erro, sucesso, thinking.
- Texto/microcopy visível ao usuário (incluindo resposta de IA/diagnóstico).
- Mudança de fluxo de navegação.

Bug ou lógica pura, e mudanças em `:core*` sem reflexo visual, **pulam a Lia** — reduz latência sem perder qualidade onde importa.

## Dois momentos da Lia

1. **Antes da implementação** — valida que estados visuais e microcopy estão mapeados no plano.
2. **Pós-implementação** — confirma o entregável real (junto ao gate da Gema).

## Papéis

| Agente | Responsabilidade |
|---|---|
| Lia | UI, MD3, microcopy, acessibilidade, estados visuais — edita só UI/layout |
| Camilo | Implementa a UI Android conforme spec da Lia |
| Felipe | Implementa a UI do Admin Panel conforme spec da Lia |
| Gema | Valida que a UI não introduz bug/regressão |

## Referências

- `design-system/COMPONENTS_ANDROID.md`, `MD3_GUIDELINES.md`, `COLORS.md`, `TYPOGRAPHY.md`, `NAVIGATION.md`
- `functional/AI_ASSISTANT.md` — apresentação de respostas de IA
- `functional/DIAGNOSTIC_FLOW.md` — fluxo de diagnóstico
