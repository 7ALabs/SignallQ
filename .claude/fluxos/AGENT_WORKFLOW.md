# Agent Workflow

> **Fonte da verdade:** [`ai-governance/agents/*.md`](../../../ai-governance/agents/) e as políticas em [`ai-governance/policies/`](../../../ai-governance/policies/). Este arquivo é um resumo apontador — se divergir, valem aqueles.
> **Decisão canônica:** [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md) (superseda ADR-006).
> **Última atualização:** 2026-08-15.

## Squad aplicável ao SignallQ (6 agentes)

- **Claudete** — Head de Produto e Portfólio. Refina demandas, decompõe em tasks, define critérios de aceite e Done. Não implementa código.
- **Camilo** — Principal Engineer. Android (Kotlin/Compose), Workers Cloudflare, Admin, Firebase.
- **Juliana** — Design, UX, jornadas. Entra antes da implementação quando a mudança é visual/de fluxo.
- **Marcos** — Growth: aquisição, ASO, funil, SEO editorial.
- **Gustavo** — Operações e dados: métricas, telemetria, observabilidade.
- **Caio** — Revisor independente. Arquitetura, segurança, testes, prontidão de release. Não implementa o que revisa.

Renan (frontend web) atua em `signallq-web`, não neste repo.

Definições completas em [`ai-governance/agents/{claudete,camilo,juliana,marcos,gustavo,caio}.md`](../../../ai-governance/agents/).

## Fluxo

1. **Claudete** refina a demanda e quebra em tasks pequenas e independentes.
2. **Juliana** revisa antes da implementação **apenas** quando a mudança é visual/de fluxo (tela nova, layout, navegação, microcopy). Bug ou lógica pura pula Juliana.
3. **Camilo** implementa; contribuições paralelas de Juliana (UI/microcopy) e Gustavo (métricas/telemetria) quando aplicáveis.
4. **Caio** é o gate único de revisão independente para código, segurança, produção, regressão ou risco relevante. Loop Caio ↔ implementador tem no máximo 2 rodadas; na 3ª, escala para Claudete.

WIP: máximo 1 task In Progress por agente.

Aprovações materiais (estratégia, marca, produção, custo recorrente, mudança irreversível) exigem **Luiz**.

## Handoff

Estado do trabalho vive em **GitHub Issues** (status da issue) + **GitHub PR**. Scripts `agent-handoff.sh`, `notify.sh`, `discord_notify.sh`, `slack_notify.sh` foram movidos para `scripts/legacy/` em 2026-08-15 e permanecem depreciados — não são mecanismo de handoff. Ver [`scripts/legacy/README.md`](../../scripts/legacy/README.md).

Roteamento: **bug** → GitHub Issues (`type:bug`); **feature/task/refactor/docs** → GitHub Issues (`Task - ...` ou `Feat - ...`).

Formato do handoff no comentário da issue:

```
De: [agente] Para: [agente] — Decisão: [o que foi decidido]. Pendente: [o que falta]. Riscos: [riscos].
```

## Build/Verify

- `./android/gradlew test` — testes unitários
- `./android/gradlew ktlintCheck detekt` — lint
- `./android/gradlew assembleDebug` — build debug

Detalhes: [`docs_ai/TECNICO.md`](../../docs_ai/TECNICO.md).
