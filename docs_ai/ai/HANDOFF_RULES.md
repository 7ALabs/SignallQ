# Handoff Rules

> **Fonte da verdade:** `.claude/CLAUDE.md` + `.claude/agents/*.md`. Este arquivo é um resumo apontador.
> Decisão de fluxo: `docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md`.
> Versão: v0.25.0 · 2026-07-16.

## Onde vive o handoff

O estado do trabalho vive no **GitHub Issues** (status da issue) + **GitHub** (PR). O GitHub notifica o Slack diretamente — não criar fluxo manual paralelo. (Correção 2026-07-16: migração de Linear para GitHub Issues em 2026-07-09 — ver `.claude/CLAUDE.md` seção "Fontes da Verdade".)

Os scripts `agent-handoff.sh`, `notify.sh`, `discord_notify.sh` e o board Discord estão **depreciados**: não são o mecanismo de handoff. Não documentar como fluxo.

Roteamento: **bug → GitHub Issues** (formato `[BUG]`); **feature / task → GitHub Issues** (título `Task - <descrição>`). Linear deixou de ser fonte da verdade de tarefas em 2026-07-09; issues anteriores a essa data (IDs `SIG-XXX`) continuam válidas como referência histórica.

## Fluxo de handoff (squad de 5)

| Situação | De | Para |
|---|---|---|
| Demanda bruta → refino e breakdown | Usuário | Claudete |
| Task visual/de fluxo, antes de implementar | Claudete | Lia (gate condicional) |
| Task Android pronta para implementar | Claudete | Camilo |
| Task Admin Panel / análise de dados | Claudete | Felipe |
| Implementação pronta → gate de Done | Camilo / Felipe / Lia | Gema |
| Reprovação (máx. 2 rodadas) | Gema | implementador |
| 3ª divergência no loop de review | Gema | Claudete (decide) |

Lia entra **antes** só quando a mudança é visual/de fluxo; bug/lógica pura pula a Lia.

## Formato do handoff (no comentário da issue)

```
De: [agente] Para: [agente] — Decisão: [o que foi decidido]. Pendente: [o que falta]. Riscos: [riscos].
```

Não repita contexto completo — apenas o delta relevante.

## Agentes e arquivos

| Agente | Arquivo | Papel |
|---|---|---|
| Claudete | `.claude/agents/claudete.md` | PM & Tech Lead |
| Camilo | `.claude/agents/camilo.md` | Dev Android |
| Felipe | `.claude/agents/felipe.md` | Admin Panel & dados |
| Lia | `.claude/agents/lia.md` | UX & Design |
| Gema | `.claude/agents/gema.md` | QA, Release & Higiene |

Busca de código/docs = ferramentas nativas (Read/Grep/Glob) ou skills. Não há agente dedicado a busca.

## Referências

- `ai/AGENT_WORKFLOW.md` — fluxo completo
- `ai/TASK_BREAKDOWN.md` — decomposição de tasks
