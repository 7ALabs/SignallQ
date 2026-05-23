---
description: Verifica o estado atual das tasks ativas, em fila e stale. Dá uma visão rápida do que está acontecendo no squad sem precisar abrir cada arquivo.
---

## Quando usar
Ao iniciar sessão. Ao fazer wip-control. Quando Claudete precisa de panorama do trabalho em andamento.

## Passos
1. Listar arquivos em `.claude/tasks/active/`.
2. Para cada task ativa, ler: Status, Current Owner, Started At, RESUME_NEXT.
3. Listar arquivos em `.claude/tasks/queue/*/`.
4. Listar arquivos em `.claude/tasks/stale/`.
5. Montar tabela resumo.

## Output esperado
```
Squad status [data]:

ATIVAS:
- TASK-001 | Camilo | IN_PROGRESS | desde [data]
- TASK-002 | Renan  | BLOCKED     | aguardando [motivo]

FILAS:
- Lia: TASK-004 (1 task)
- Gema: vazia

STALE:
- TASK-000 | última atualização: [data]

PRÓXIMA AÇÃO RECOMENDADA: [1 linha]
```

## Limites
- Não mover tasks automaticamente — apenas reportar.
- Se stale > 7 dias, sinalizar para Gema verificar.
