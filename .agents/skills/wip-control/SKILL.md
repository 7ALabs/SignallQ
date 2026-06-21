---
description: Verifica e controla o WIP (Work In Progress) de cada agente. Garante que nenhum agente recebe nova task enquanto tem atividade IN_PROGRESS. Executa o pull system da fila.
---

## Quando usar
Antes de atribuir qualquer task nova. Na abertura de sessão. Ao fazer task breakdown.

## Passos
1. **Listar tasks ativas** em `.Codex/tasks/active/`.
2. Para cada task ativa, verificar: `Current Owner` e `Status`.
3. Se agente tem `IN_PROGRESS`:
   - Nova task vai para `.Codex/tasks/queue/<agente>/`.
   - Registrar motivo no task file da nova task.
4. Se agente está livre (sem `IN_PROGRESS`):
   - Verificar fila em `.Codex/tasks/queue/<agente>/`.
   - Puxar a próxima task da fila (mais antiga primeiro).
   - Mover para `active/` e marcar `IN_PROGRESS`.
5. **Paralelismo permitido** apenas entre agentes diferentes com arquivos independentes.

## WIP limit por agente
| Agente | WIP máximo |
|---|---|
| Claudete | 1 (orquestração conta) |
| Marcelo | 1 (dev task) — buscas não contam |
| Lia | 1 |
| Camilo | 1 |
| Renan | 1 |
| Gema | 1 |

## Output esperado
```
WIP check [data]:
- Camilo: IN_PROGRESS TASK-001 (Android) → livre para nova task: NÃO
- Renan: livre → próxima da fila: TASK-003
- Gema: livre → fila vazia
- Lia: fila: TASK-004 aguardando
```

## Limites
- Não forçar paralelismo quando há dependência entre tasks.
- Não criar nova task sem verificar WIP primeiro.
