# Disciplina de PR, Branch e Dispatch de Agentes — revisão 2026-07-16

Decisão da Claudete a partir de avaliação sincera pedida pelo Luiz na sessão de 2026-07-15/16.
Motivo direto: sessão gastou 11 dispatches de agente (>1M tokens) pra resolver ~6 frentes de
trabalho, com pelo menos 2 abertas como PR isolada quando deveriam ter sido absorvidas por um
dispatch já em andamento. Isso não é problema isolado de hoje — é sintoma de um padrão estrutural
do repo.

## Diagnóstico (números reais, não estimativa)

| Métrica | Valor | Fonte |
|---|---|---|
| PRs totais no histórico do repo | 413 | `gh api search/issues -f q="repo:... is:pr"` |
| Issues totais | 471 | idem, `is:issue` |
| Branches remotas ativas | 144 | `git branch -r` |
| Branches remotas já mergeadas em main e **nunca apagadas** | 74 | `git branch -r --merged origin/main` |
| PRs mergeadas nos últimos 14 dias | 200+ (capado no limite da consulta) | `gh pr list --state merged --search "merged:>=2026-07-01"` |
| PRs de 1 arquivo só na amostra das últimas 60 | 14 (23%) | amostra `gh pr list --json changedFiles` |

Conclusão do diagnóstico: **metade do "problema de 1002" não é das PRs de hoje** — é dívida
acumulada de branch nunca apagada (74 órfãs, zero risco, zero motivo pra ainda existirem). A outra
metade é real e é comportamental: dispatch reativo, um agente/branch/PR por achado, em vez de
agrupar achados da mesma sessão/área antes de abrir PR.

## Causas raiz identificadas

1. **Branch nunca apagada por hábito.** `git worktree remove` e merge não vinham com
   `--delete-branch` como padrão obrigatório — 74 branches mergeadas sobrando confirmam isso.
2. **Dispatch reativo.** Cada achado durante uma tarefa (bug, código morto, coisa fora de escopo)
   virava agente novo + branch nova + PR nova na hora, em vez de voltar pro backlog do dispatch
   ativo. Exemplo real de 2026-07-15: achado do seletor prototype-only (#996) apareceu **durante**
   o hotfix do #995, no mesmo arquivo, na mesma worktree — e virou uma PR e um agente inteiramente
   novos (#1001) em vez de entrar no mesmo commit.
3. **Sequenciamento ruim gera retrabalho.** Resolver "reverter telas do #950 pra bater com main"
   antes de esperar a #995 mergear gerou duas passadas de conflito em vez de uma.
4. **Falta de mecanismo automático de convergência.** Sem Merge Queue (indisponível pra conta
   pessoal, confirmado via API) e com `required_status_checks.strict=true`, cada merge que fechava
   invalidava as PRs abertas simultâneas, exigindo `update-branch` manual repetido — efeito
   amplificado exatamente pelo volume de PRs abertas ao mesmo tempo.

## Plano

### 1. Ação imediata, zero risco (executada nesta sessão)
Apagar as 74 branches remotas já mergeadas em `main`, confirmadas via `git branch -r --merged`
(não por nome/suposição — ancestralidade real de commit). Ver execução no final desta sessão.

### 2. Regra de batching (a partir de agora, squad inteira)
Antes de abrir agente novo ou PR nova, checar: **já existe branch/worktree/PR ativa na mesma
área/arquivo desta sessão?** Se sim, o achado entra ali — mesmo commit ou commit seguinte na mesma
branch, não dispatch novo.

PR isolada só se justifica quando pelo menos um destes for verdade:
- Urgência de produção diferente do resto do trabalho em andamento (ex: bug de dado fake em
  produção enquanto o resto é redesign em draft — isso foi legítimo, ver #995).
- Rollback precisa ser independente (reverter uma coisa não pode arrastar a outra).
- Domínio/reviewer diferente (ex: Admin React vs Android Kotlin).

Fora isso: agrupar. Uma sessão de "resolve o Gateway" resolve tudo que aparecer no Gateway antes
de fechar, não abre PR nova a cada achado dentro da mesma área.

### 3. Sequenciamento antes de despachar
Se a tarefa B depende do resultado de uma tarefa A ainda não fechada (ex: revert que depende de
outra PR mergear primeiro), esperar A fechar antes de despachar B. Checar `gh pr view --json
mergedAt` antes de mandar um agente mexer em algo que depende de outra PR.

### 4. Branch e worktree — limpeza é parte de fechar a tarefa
Já era regra no `CLAUDE.md` ("Limpeza de worktree é parte de FECHAR a tarefa, não auditoria
separada") — reforçando: todo merge usa `--delete-branch`. Toda worktree criada por agente é
removida ao fechar, sem esperar rodada de `/higiene`.

### 5. `strict` desligado em `required_status_checks` de `main`
Decisão tomada em 2026-07-15: Merge Queue não está disponível (conta pessoal, não organização —
confirmado via rejeição da API de rulesets). Em vez de manter `strict: true` (exige branch 100%
atualizada com main, sem mecanismo automático de re-sync), desligamos `strict`. PRs mergeiam assim
que os checks obrigatórios (Detekt/Ktlint/Unit Tests) passam na própria branch, sem precisar estar
byte-a-byte atualizada com o `main` mais recente. Trade-off aceito: leve perda de garantia "testado
contra o main mais recente até o segundo", ganho: fim do ciclo manual de `update-branch` a cada
merge concorrente. Reverter se algum dia a conta virar organização com Merge Queue disponível.

### 6. Checklist de dispatch (pra qualquer agente antes de abrir Agent tool + PR nova)

1. Esse achado é da mesma área/arquivo de um agente/branch já ativo nesta sessão? → entra lá.
2. Esse achado depende de algo que ainda não mergeou? → espera, não despacha ainda.
3. Isso realmente precisa ser PR isolada (urgência/rollback/domínio diferente), ou é só reflexo de
   "achei, então abro"? → se for só reflexo, agrupa.

## Referência

Ver `.claude/CLAUDE.md`, seção "Disciplina de Branches e PRs", atualizada com a versão condensada
desta regra.
