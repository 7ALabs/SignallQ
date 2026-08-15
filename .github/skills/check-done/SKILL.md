---
name: check-done
description: Checklist executável dos 9 critérios de conclusão de uma tarefa SignallQ (contrato operacional §9). Rodada antes de declarar tarefa concluída — retorna PASS ou tabela dos itens que faltam. Substitui o texto livre "concluído" por evidência verificável. Piloto para a proposta de contrato reduzido — #1616.
argument-hint: "<numero-issue|numero-PR> [--sem-caio] [--sem-testes] [--sem-docs]  (flags de dispensa exigem justificativa e só valem quando o critério objetivamente não se aplica)"
allowed-tools: Bash(gh *), Bash(git *), Bash(./android/gradlew *), Bash(bash scripts/*), Bash(grep *), Bash(head *), Bash(tail *)
---

## Quando usar

- Antes de dizer "concluído" em qualquer comentário de issue.
- Antes de rodar `/handoff` que declara conclusão.
- Antes de mergear PR para `main`.
- Antes de fechar issue.

Não usar para: relatório de progresso intermediário, decisão de aceitar débito técnico (essa é escalação para Claudete/Luiz, não conclusão).

## Os 9 critérios ([contrato operacional §9](../../../../ai-governance/policies/agent-operating-contract.md))

1. Escopo autorizado atendido.
2. Critérios de aceite verificados.
3. Testes aplicáveis executados.
4. Documentação necessária atualizada.
5. Riscos e limitações declarados.
6. Revisão independente de Caio realizada quando exigida.
7. Commit rastreável.
8. Merge em `main` do repositório autorizado.
9. Conclusão **não** baseada apenas na existência de código, issue, branch, PR ou protótipo.

## Detecção do alvo

Se `$ARGUMENTS` começa com número → assume ID (issue ou PR — detecta pelo tipo):

```bash
gh api "repos/buildea-labs/signallq/issues/<N>" --jq '.pull_request // "issue"'
```

Se sem argumento → tenta detectar do branch atual:

```bash
BRANCH=$(git branch --show-current)
gh pr list --search "head:$BRANCH" --json number --repo buildea-labs/signallq --jq '.[0].number'
```

Se nenhum dos dois resolve → PARAR e pedir número explícito.

## Verificações executáveis

Cada critério retorna `PASS`, `FAIL` ou `SKIP` (com justificativa). Tabela final é montada com os 9.

### 1. Escopo autorizado atendido

Semi-manual. A skill lê o corpo da issue e o diff da PR:

```bash
gh issue view <N> --json body --repo buildea-labs/signallq --jq '.body'
gh pr diff <PR> --repo buildea-labs/signallq | head -200
```

Depois **pergunta ao operador humano** (não decide sozinha): "O diff acima cobre o escopo descrito na issue? (y/n/justificar)". Registra a resposta.

`PASS` se `y`. `FAIL` se `n`. `SKIP` só com justificativa registrada.

### 2. Critérios de aceite verificados

Lê o bloco `## Critérios de aceitação` (ou `## Critérios de aceite`) da issue:

```bash
gh issue view <N> --json body --repo buildea-labs/signallq --jq '.body' \
  | awk '/^## Crit[eé]rios de aceit/,/^## /' | grep -E '^- \[.\]|^\* \[.\]'
```

Conta quantos itens estão marcados (`[x]`) vs abertos (`[ ]`).

`PASS` se todos marcados. `FAIL` se algum aberto (lista os que faltam). Se a issue não tem critérios de aceite → `FAIL` (issue mal refinada é bloqueador de conclusão).

### 3. Testes aplicáveis executados

Se `--sem-testes` foi passado com justificativa (ex.: "task de doc puro, sem código"), registra `SKIP: <justificativa>`.

Caso contrário, roda os checks aplicáveis:

```bash
# Detecção de escopo baseada no diff
CHANGED=$(gh pr diff <PR> --repo buildea-labs/signallq --name-only)

if echo "$CHANGED" | grep -qE '^android/'; then
  ./android/gradlew test 2>&1 | tail -5
fi
if echo "$CHANGED" | grep -qE '^integrations/cloudflare/[^/]+/'; then
  # Worker: rodar teste do worker específico
  echo "verificar testes do Worker manualmente (integrations/cloudflare/)"
fi
if echo "$CHANGED" | grep -qE '\.(md)$' | grep -qE '^docs_ai/'; then
  bash scripts/validar-docs.sh --base main 2>&1 | tail -10
fi
```

Também consulta CI:

```bash
gh pr checks <PR> --repo buildea-labs/signallq --json name,conclusion
```

`PASS` se: testes locais passaram (ou `SKIP` justificado) + todos os checks CI = SUCCESS.
`FAIL` se: qualquer teste local falhou OU qualquer check CI != SUCCESS/PENDING.

### 4. Documentação necessária atualizada

Se `--sem-docs` com justificativa, `SKIP`.

Caso contrário, avalia o diff:

```bash
CHANGED=$(gh pr diff <PR> --repo buildea-labs/signallq --name-only)

# Regras heurísticas:
# - Mudou :feature:X ou :core:X → esperava-se docs_ai/ARQUITETURA/MODULOS/<X>.md tocado
# - Adicionou tela nova em ui/screen/ → esperava-se docs_ai/FUNCIONAL.md tocado
# - Mudou schema D1 → esperava-se docs_ai/CONTRATOS/schemas/ ou skill /cloudflare-d1-console tocado
# - Mudou wrangler.toml ou versionCode → esperava-se docs_ai/RELEASES.md ou CHANGELOG.md tocado
# - Mudou algo em .claude/skills/ ou .claude/rules/ → esperava-se docs-CI passar (frontmatter, mirrors)

# Roda checks explícitos:
bash scripts/validar-docs.sh --base main 2>&1 | tail -10
bash scripts/sync-skills-mirrors.sh --check 2>&1
```

`PASS` se: `validar-docs.sh` retorna `falhas: 0` E o diff toca ao menos um doc quando as heurísticas acima esperam.

`FAIL` se: docs-CI falha OU heurística indica doc faltando.

`SKIP` se: bugfix trivial (≤5 arquivos, sem novo comportamento visível ao usuário) — deve ser justificado.

### 5. Riscos e limitações declarados

Verifica que o corpo da PR tem seção `## Riscos` ou `## Limitações` (ou equivalente):

```bash
gh pr view <PR> --json body --repo buildea-labs/signallq --jq '.body' \
  | grep -iE '^##.*(riscos?|limita[çc][ãa]o|trade[- ]?off)' | head -3
```

`PASS` se pelo menos uma dessas seções existe.
`FAIL` se nenhuma existe e a mudança não é trivial (>50 linhas ou toca `:core:*`).
`SKIP` se mudança trivial declarada.

### 6. Revisão independente de Caio quando exigida

`--sem-caio` requer justificativa objetiva (ex.: "PR de doc puro sem código; contrato §6 não exige Caio para docs sem risco").

Sem a flag, verifica:

```bash
gh pr view <PR> --json reviews --repo buildea-labs/signallq \
  --jq '.reviews | map(select(.author.login | test("caio"; "i"))) | .[-1].state'
```

`PASS` se retornou `APPROVED` ou `--sem-caio` justificado.
`FAIL` se retornou `CHANGES_REQUESTED`, `COMMENTED` ou vazio.

Nota: enquanto a persona Caio não corresponde a um user GitHub real, aceitar aprovação humana equivalente (Luiz ou owner do repo) documentada na PR — a skill pergunta ao operador.

### 7. Commit rastreável

```bash
gh pr view <PR> --json commits --repo buildea-labs/signallq --jq '.commits | length'
```

`PASS` se >= 1 commit. `FAIL` se 0 (nunca deveria acontecer, mas checamos).

### 8. Merge em `main`

```bash
gh pr view <PR> --json state,baseRefName,mergedAt --repo buildea-labs/signallq
```

`PASS` se `state=MERGED` E `baseRefName=main`.
`FAIL` se `state=OPEN` (não mergeou ainda) ou `baseRefName != main`.
`PENDING` se `state=OPEN` mas todos os outros critérios são `PASS` — sinaliza que só falta o merge.

### 9. Não baseada apenas em código/branch/PR

Implícito: se critérios 7 e 8 passaram (commit + merge em main), a tarefa passou do estado "só existe em branch/PR". `PASS` automaticamente.

## Saída

**Sucesso total:**
```
✓ check-done PASS — issue #<N> / PR #<M> pronta para fechar.

  1. Escopo: PASS (confirmado por <operador>)
  2. Critérios de aceite: PASS (5/5)
  3. Testes: PASS (7 checks CI verdes, ./gradlew test OK)
  4. Docs: PASS (docs-CI: falhas: 0)
  5. Riscos: PASS (seção declarada em PR body)
  6. Caio: PASS (APPROVED por @caio em 2026-08-15T12:00Z)
  7. Commit: PASS (3 commits)
  8. Merge: PASS (merged em main via aea08f4d)
  9. Rastreável: PASS (implícito por #7 e #8)

Pode fechar a issue e postar handoff final via /handoff.
```

**Falha com faltas:**
```
✗ check-done FAIL — 2 itens bloqueando conclusão.

  1. Escopo: PASS
  2. Critérios de aceite: FAIL — 2 abertos:
       - [ ] Espelhos .agents/skills/ ressincronizados
       - [ ] scripts/legacy/README.md atualizado
  3. Testes: PASS
  4. Docs: FAIL — validar-docs.sh: 1 falha (inventário desatualizado, rode scripts/gerar-inventario-docs.sh)
  5. Riscos: PASS
  6. Caio: SKIP — --sem-caio: "PR de doc puro sem código nem risco"
  7. Commit: PASS
  8. Merge: PENDING — PR ainda aberta
  9. Rastreável: (aguardando #8)

Ações:
  - Marcar os 2 critérios abertos ou remover da lista.
  - Rodar scripts/gerar-inventario-docs.sh e commitar.
  - Aguardar merge para conclusão total.
```

## O que a skill NÃO faz

- Não mergeia PR — mesmo com todos os PASS, quem mergeia é o operador.
- Não fecha issue — mesmo com PASS + merge, o fechamento é passo separado.
- Não aprova revisão do Caio automaticamente — falta de aprovação é FAIL, ponto.
- Não escreve documentação faltando — só reporta que falta.
- Não decide se `--sem-testes`/`--sem-docs`/`--sem-caio` é aceitável — o operador declara, a skill registra e cita literalmente na saída para auditoria.

## Interação com o contrato

Enquanto o [contrato reduzido](https://github.com/buildea-labs/signallq/issues/1616) não estiver em vigor:

- Skill é **opcional** — pode ser invocada para garantir consistência antes de fechar tarefa. Não bloqueia conclusão declarada por comentário livre.
- Piloto de 2 semanas: registrar taxa de PASS no primeiro try, distribuição de falhas por critério, tempo médio de execução.

Quando/se o contrato reduzido entrar em vigor:

- Skill é **obrigatória** antes de qualquer merge para `main` e antes de qualquer close de issue. Tarefa sem check-done PASS registrado é considerada "não concluída".

## Referências

- [Contrato operacional §5 e §9](../../../../ai-governance/policies/agent-operating-contract.md)
- [Proposta de contrato reduzido — #1616](https://github.com/buildea-labs/signallq/issues/1616)
- Skill complementar: [`/handoff`](../handoff/SKILL.md) — usada após `/check-done` PASS para postar o handoff final.
