---
description: Aplica a política de retenção de tasks — arquiva DONE, marca STALE, sugere cleanup de tasks antigas. Executado por Gema como parte da higiene semanal.
---

## Quando usar
Gema executa semanalmente como parte de `/workspace-hygiene`.

## Política de retenção

| Status | Tempo | Ação |
|---|---|---|
| BACKLOG | Ilimitado | Manter |
| QUEUED | > 14 dias sem IN_PROGRESS | Verificar relevância com Claudete |
| IN_PROGRESS | Ilimitado | Manter (nunca auto-arquivar) |
| BLOCKED | > 7 dias | Verificar se bloqueio foi resolvido |
| REVIEW | > 3 dias | Cobrar Gema por QA pendente |
| DONE | > 7 dias | Arquivar para `.claude/tasks/archive/YYYY-MM/` |
| STALE | Sem update em 7 dias | Marcar. Após 14 dias total → sugerir cleanup |
| CANCELLED | > 3 dias | Arquivar |

## Detecção de STALE

Uma task entra em STALE quando:
1. Está em QUEUED, BACKLOG ou BLOCKED há mais de 7 dias sem atualização no arquivo.
2. Está em REVIEW há mais de 3 dias sem ação de Gema.

Gema atualiza o arquivo da task com:
```markdown
**Status:** STALE
**Stale desde:** AAAA-MM-DD
**Motivo:** Sem atualização por X dias
```

## Processo de arquivamento

```powershell
# Mover task arquivada
Move-Item .claude/tasks/active/task-xxx.md .claude/tasks/archive/YYYY-MM/task-xxx.md
```

## Relatório de cleanup

Gema reporta para Claudete:
```
## Cleanup de Tasks — [DATA]
- X tasks arquivadas (DONE)
- X tasks marcadas STALE
- X tasks em BLOCKED sem resolução (> 7 dias)
- Ação necessária: [lista]
```

## Limites
- Nunca deletar task IN_PROGRESS sem aprovação explícita do usuário.
- Dúvida sobre relevância de task STALE → perguntar ao usuário antes de arquivar.
