---
description: Verifica a fila de um agente específico e puxa a próxima task disponível — apenas quando o agente está livre (sem IN_PROGRESS).
---

## Quando usar
Quando um agente conclui ou pausa uma task e precisa saber o que fazer a seguir.

## Passos
1. Verificar se há task `IN_PROGRESS` para o agente em `.claude/tasks/active/`.
2. Se ocupado → retornar "Agente ocupado. Finalizar TASK-XXX primeiro."
3. Se livre → listar `.claude/tasks/queue/<agente>/` ordenado por data de criação.
4. Pegar a mais antiga (primeira da fila).
5. Mover para `.claude/tasks/active/`.
6. Atualizar status para `IN_PROGRESS` e registrar `Started At`.
7. Retornar task file para o agente começar.

## Output esperado
```
Próxima task para [agente]:
Task: TASK-XXX — [título]
Task file: .claude/tasks/active/TASK-XXX.md
RESUME_NEXT: [ponto de início]
```

## Limites
- Nunca puxar task se há IN_PROGRESS. WIP = 1.
- Se fila vazia → retornar "Fila vazia. Aguardar Claudete ou verificar backlog."
