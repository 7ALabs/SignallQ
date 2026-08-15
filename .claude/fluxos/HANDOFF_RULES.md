# Handoff Rules

> **Fonte da verdade:** [contrato operacional §8](../../../ai-governance/policies/agent-operating-contract.md) + skill [`/handoff`](../skills/handoff/SKILL.md).
> **Decisão canônica:** [ADR-016](../../docs_ai/decisions/ADR-016-portfolio-buildea.md).
> **Última atualização:** 2026-08-15.

## Onde vive o handoff

**GitHub Issues** (status) + **GitHub PR** (execução). GitHub notifica Slack automaticamente. Skill `/handoff` formaliza pre-flight (git status, branch, PRs relacionadas) e posta o comentário canônico.

Scripts em `scripts/legacy/` (`agent-handoff.sh`, `notify.sh`, `discord_notify.sh`, `slack_notify.sh`) estão depreciados — não são mecanismo de handoff.

## Fluxo de handoff (squad de 3)

| Situação | De | Para |
|---|---|---|
| Demanda bruta → refino | Usuário/Luiz | Claudete |
| Task pronta para implementação | Claudete | Camilo |
| Implementação pronta → revisão | Camilo | Caio |
| Reprovação (máx 2 rodadas) | Caio | Camilo |
| 3ª divergência | Caio | Claudete (decide) |
| Aprovação material (estratégia/marca/produção/custo/irreversível/risco crítico) | qualquer | Luiz |

Design, growth, dados são **skills invocáveis pelo agente responsável**, não handoffs entre agentes:

- Task visual → Camilo invoca `/design-check` durante a implementação.
- Task com telemetria → Camilo invoca `/analytics-spec`.
- Task de posicionamento/ASO → Claudete invoca `/growth-check`.

## Formato do handoff

Skill `/handoff` monta e posta:

```
**De: <agente> Para: <agente> — Decisão: <o que foi decidido>**

- **Arquivos:** <lista>
- **Validações:** <resultado dos checks automáticos>
- **Pendências:** <o que falta>
- **Riscos:** <riscos ou "nenhum identificado">
- **Branch:** `<branch>` · **PR:** <url>
```

## Agentes canônicos

| Agente | Arquivo | Papel |
|---|---|---|
| Claudete | [`.claude/agents/claudete.md`](../agents/claudete.md) | Produto (PM) |
| Camilo | [`.claude/agents/camilo.md`](../agents/camilo.md) | Engenharia (Dev) |
| Caio | [`.claude/agents/caio.md`](../agents/caio.md) | Revisão independente |

## Referências

- [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md), [`TASK_BREAKDOWN.md`](TASK_BREAKDOWN.md)
- [`ai-governance/policies/agent-operating-contract.md`](../../../ai-governance/policies/agent-operating-contract.md)
