# Review Flow for Agents

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Este arquivo é um resumo apontador.
> Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`.
> Versão: v0.23.0 · 2026-07-05.

## Gate único: Rhodolfo

**Rhodolfo** é o gate único de Done: review de código, QA, release e higiene. Não há revisor de arquitetura separado (Claudete absorveu a decisão de arquitetura) nem revisor de docs separado (Rhodolfo absorveu Nina/Taisa).

## Processo

1. **Gatilho** — Camilo, Felipe ou Lia concluem a implementação.
2. **Checks** — `.\android\gradlew.bat lint` e `test` (Android) ou `npm run lint`/`npm run build` (Admin) devem passar.
3. **Review do Rhodolfo** — bugs, regressões, risco técnico, testes faltando, aderência ao design system e higiene (changelog, bump de versão, docs).
4. **UX condicional** — Lia valida o entregável visual quando a mudança foi de tela/fluxo.
5. **Veredito** — `Aprovado` / `Aprovado com ressalvas` / `Reprovado`.

## Limite de loop

Ciclo Rhodolfo → implementador tem no máximo **2 rodadas**. Na 3ª divergência, escala para a **Claudete** decidir (aceitar débito, repriorizar ou reescopar). Evita ping-pong infinito.

## O que o Rhodolfo não faz

- Não implementa correções — devolve ao implementador.
- Decisão de arquitetura vai para a Claudete.

## O que a Lia não faz

- Não edita lógica de negócio — apenas UI e layout.
- Não aprova UX de feature visual que não passou por ela antes da implementação.

## Referências

- `ai/AGENT_WORKFLOW.md` — fluxo completo
- `DESIGN_SYSTEM.md` — referência de revisão visual
- `ARQUITETURA/README.md` — referência de revisão técnica
