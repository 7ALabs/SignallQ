---
description: Audita branches e worktrees do repositório — identifica branches mortas, worktrees órfãs e inconsistências entre branch local e tasks ativas.
---

## Quando usar
Gema executa quando Agent Teams com worktrees foram usadas, ou quando `/workspace-hygiene` detectar possíveis inconsistências.

## Comandos de auditoria

```powershell
# Listar branches locais
git branch -v

# Listar branches remotas
git branch -r

# Listar worktrees ativas
git worktree list

# Branches sem commits recentes (> 30 dias)
git for-each-ref --sort=committerdate refs/heads/ --format='%(refname:short) %(committerdate:relative)'
```

## Critérios de cleanup

| Item | Critério | Ação |
|---|---|---|
| Branch local | Sem commits há > 30 dias e sem PR aberta | Candidata a deleção — confirmar com usuário |
| Branch local | Mergeada em main/master | Deletar (`git branch -d`) |
| Worktree | Sem task ativa correspondente | Remover (`git worktree remove`) |
| Branch remota | Deletada no remote, ainda local | Limpar (`git remote prune origin`) |

## Formato de relatório

```
## Auditoria de Branches — [DATA]

### Branches candidatas a cleanup
- `feature/xxx` — sem commits há 45 dias, sem PR aberta
- `fix/yyy` — mergeada em main em 2026-05-10

### Worktrees ativas
- [path] em [branch] — OK / ÓRFÃ

### Ação recomendada
- Deletar X branches (lista)
- Remover Y worktrees órfãs

**Confirmar com usuário antes de deletar.**
```

## Limites
- Gema não deleta branches sem confirmação explícita do usuário.
- Worktrees órfãs: confirmar que nenhum trabalho em andamento antes de remover.
