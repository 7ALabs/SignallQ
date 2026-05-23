---
description: Limpa o workspace do Claude Code — tasks stale, arquivos temporários, branches mortas e estado inconsistente. Executado por Gema semanalmente.
---

## Quando usar
Gema executa semanalmente ou quando `/task-scout` reportar muitos itens stale.

## Checklist de higiene

### Tasks
- [ ] Tasks em `DONE` com mais de 7 dias → mover para `.claude/tasks/archive/`
- [ ] Tasks em `STALE` com mais de 14 dias → mover para `.claude/tasks/stale/` e notificar Claudete
- [ ] Tasks em `QUEUED` há mais de 14 dias sem IN_PROGRESS → verificar se ainda relevante
- [ ] Tasks em `BLOCKED` há mais de 7 dias → verificar se bloqueio foi resolvido

### Arquivos temporários
- [ ] Remover arquivos `*.tmp`, `*.bak` gerados acidentalmente em `.claude/`
- [ ] Verificar se há arquivos de sessão antigos que podem ser removidos

### Improvements
- [ ] Propostas em `.claude/improvements/proposals/` com mais de 30 dias → revisar ou rejeitar
- [ ] Propostas aprovadas em `approved/` já aplicadas → mover para `rejected/applied/`

### Estado geral
- [ ] MEMORY.md (auto-memory) com entradas stale → atualizar ou remover
- [ ] Scripts em `.claude/scripts/` ainda relevantes?

## Processo de arquivamento de tasks

```
.claude/tasks/active/task-xxx.md  
  → (7+ dias em DONE) → .claude/tasks/archive/YYYY-MM/task-xxx.md

.claude/tasks/active/task-xxx.md  
  → (14+ dias sem update) → .claude/tasks/stale/task-xxx.md
```

## Output

Relatório para Claudete:
```
## Relatório de Higiene — [DATA]
- X tasks arquivadas (DONE > 7 dias)
- X tasks marcadas como stale
- X proposals expiradas (> 30 dias)
- Pendências: [lista se houver]
```

## Limites
- Gema não deleta tasks ativas sem aprovação de Claudete.
- Dúvida sobre relevância → perguntar ao usuário.
