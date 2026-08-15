# Handoff Rules

> **Fonte da verdade:** [`ai-governance/policies/agent-operating-contract.md`](../../../ai-governance/policies/agent-operating-contract.md), seção 8. Este arquivo é um resumo apontador.
> **Decisão canônica:** [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md).
> **Última atualização:** 2026-08-15.

## Onde vive o handoff

O estado do trabalho vive em **GitHub Issues** (status da issue) + **GitHub PR**. O GitHub notifica o Slack diretamente — não criar fluxo manual paralelo. Migração de Linear para GitHub Issues aconteceu em 2026-07-09; IDs `SIG-XXX` continuam válidos como referência histórica, mas não são fonte da verdade de tarefas ativas.

Os scripts `agent-handoff.sh`, `notify.sh`, `discord_notify.sh` e o board Discord estão **depreciados**: não são o mecanismo de handoff. Não documentar como fluxo.

Roteamento: **bug** → GitHub Issues (título `[BUG] ...`, label `type:bug`); **feature / task / refactor / docs** → GitHub Issues (título `Task - ...` ou `Feat - ...`).

## Fluxo de handoff (squad canônico SignallQ)

| Situação | De | Para |
|---|---|---|
| Demanda bruta → refino e breakdown | Usuário | Claudete |
| Task visual/de fluxo, antes de implementar | Claudete | Juliana (gate condicional) |
| Task de métrica/telemetria com spec de dados | Claudete | Gustavo (contribuinte) |
| Task Android/Workers/Admin pronta para implementar | Claudete / Juliana / Gustavo | Camilo |
| Implementação pronta → revisão independente | Camilo | Caio |
| Reprovação (máx. 2 rodadas) | Caio | Camilo |
| 3ª divergência no loop de revisão | Caio | Claudete (decide) |
| Aprovação material (estratégia, marca, produção, custo, irreversível) | qualquer agente | Luiz |

Juliana entra **antes** da implementação apenas quando a mudança é visual/de fluxo (tela nova, layout, navegação, microcopy); bug/lógica pura pula Juliana.

## Formato do handoff (comentário na issue)

```
De: [agente] Para: [agente] — Decisão: [o que foi decidido]. Pendente: [o que falta]. Riscos: [riscos].
```

Não repita contexto completo — apenas o delta relevante.

Todo handoff deve registrar (conforme [contrato operacional §8](../../../ai-governance/policies/agent-operating-contract.md)): contexto, decisão, arquivos afetados, validações realizadas, pendências, próximo responsável.

## Agentes e definições canônicas

| Agente | Arquivo canônico | Papel |
|---|---|---|
| Claudete | [`ai-governance/agents/claudete.md`](../../../ai-governance/agents/claudete.md) | Produto e portfólio (líder) |
| Camilo | [`ai-governance/agents/camilo.md`](../../../ai-governance/agents/camilo.md) | Mobile, backend, plataforma |
| Juliana | [`ai-governance/agents/juliana.md`](../../../ai-governance/agents/juliana.md) | Design e UX |
| Marcos | [`ai-governance/agents/marcos.md`](../../../ai-governance/agents/marcos.md) | Growth |
| Gustavo | [`ai-governance/agents/gustavo.md`](../../../ai-governance/agents/gustavo.md) | Operações e dados |
| Caio | [`ai-governance/agents/caio.md`](../../../ai-governance/agents/caio.md) | Revisão independente |

Busca de código/docs = ferramentas nativas (Read/Grep/Glob) ou skills. Não há agente dedicado a busca.

## Referências

- [`AGENT_WORKFLOW.md`](AGENT_WORKFLOW.md) — fluxo completo
- [`TASK_BREAKDOWN.md`](TASK_BREAKDOWN.md) — decomposição de tasks
- [`ai-governance/policies/demand-routing.md`](../../../ai-governance/policies/demand-routing.md) — roteamento por domínio
