---
description: Estrutura o handoff de uma task entre agentes — garante que o receptor tem contexto suficiente para começar sem precisar reler a conversa toda.
---

## Quando usar
Sempre que uma task for passada de um agente para outro. Sem handoff formal, a atividade não começou de verdade.

## Passos
1. **Verificar se task file existe** em `.claude/tasks/active/`. Se não, criar antes.
2. **Atualizar task file** com estado atual: o que foi feito, o que falta.
3. **Escrever handoff mínimo**:
   - Objetivo da task
   - Escopo e arquivos prováveis (caminhos reais)
   - Restrições e decisões já tomadas
   - Definition of Done desta task
   - Link para task file
4. **Não repetir contexto desnecessário** — apenas o delta relevante.
5. **Declarar RESUME_NEXT** no task file para facilitar retomada.

## Formato mínimo de handoff
```
De: [agente] Para: [agente]
Task: [link para task file]
Decisão: [o que foi decidido]
Pendente: [o que falta fazer]
Riscos: [riscos identificados]
Aceite: [como verificar que está pronto]
```

## Limites
- Handoff sem task file = tarefa informal. Não fazer.
- Não assumir que o receptor leu a conversa — escrever como se estivesse chegando agora.
