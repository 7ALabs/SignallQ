---
name: handoff
description: Formaliza handoff entre agentes do squad SignallQ. Roda pre-flight (git sync, branch check, PRs relacionadas), valida os 6 campos obrigatórios do contrato operacional §8 e posta o comentário canônico na issue. Piloto para a proposta de contrato reduzido — #1616.
argument-hint: "<numero-issue> --de <agente> --para <agente> --decisao \"<texto>\" [--arquivos \"<lista>\"] [--pendencias \"<texto>\"] [--riscos \"<texto>\"] [--effort-minimo \"alto\"]"
allowed-tools: Bash(gh *), Bash(git *)
---

## Quando usar

Toda transição de estado do trabalho que hoje viraria "comentário livre na issue":

- Terminou uma etapa e vai passar para outro agente.
- Abriu PR e precisa acionar o Caio para revisão.
- Bloqueou por dependência externa e precisa que outro agente destrave.
- Recebeu handoff, vai começar — confirmar que assumiu, com o que vai fazer.

Não usar para: comentário informativo sem transição, resposta a pergunta pontual, atualização de progresso dentro da mesma etapa.

## Squad canônico

Ver [ADR-016](../../../docs_ai/decisions/ADR-016-portfolio-buildea.md) + [`.claude/agents/`](../../agents/). O squad deste repo é enxuto — 3 agentes. Agentes válidos para SignallQ (`--de` / `--para`):

`claudete` · `camilo` · `caio` · `luiz`

Design, growth e dados **não são mais agentes de handoff**. Juliana, Marcos e Gustavo viraram skills invocáveis (`/design-check`, `/growth-check`, `/analytics-spec`) — Claudete ou Camilo as chamam dentro da própria etapa, sem transição de responsável. Não abrir handoff `--para juliana`/`--para marcos`/`--para gustavo`.

Handoff para `luiz` só quando o contrato §3 exigir aprovação dele (estratégia, produção, custo, irreversível, risco crítico). Não escalar por default.

## Pre-flight (obrigatório antes de postar)

Todos os checks abaixo rodam automaticamente. Falha em qualquer um PARA a skill — o handoff **não é postado** enquanto não estiver limpo.

1. **Ambiente limpo**
   ```bash
   git status --short
   ```
   Se retornar linhas, PARAR. Handoff sem commit vira handoff mentiroso: o próximo agente clona o repo e não vê o que você diz que fez.

2. **Branch atual**
   ```bash
   git branch --show-current
   ```
   Capturar para citar no handoff. Se estiver em `main` e o handoff for de implementação, PARAR — implementação em `main` viola o fluxo.

3. **PRs relacionadas**
   ```bash
   gh pr list --search "head:$(git branch --show-current)" --json number,state,title --repo buildea-labs/signallq
   ```
   Se retornar PR aberta, incluir a URL no handoff. Se retornar múltiplas PRs abertas na mesma branch, PARAR e pedir ao usuário para escolher qual é a canônica.

4. **Issue existe e está aberta**
   ```bash
   gh issue view <numero> --json state,labels,title --repo buildea-labs/signallq
   ```
   Se `state != OPEN`, PARAR — não faz handoff em issue fechada.

5. **Sem handoff duplicado nas últimas 24h**
   ```bash
   gh issue view <numero> --json comments --repo buildea-labs/signallq \
     | jq --arg de "$DE" --arg para "$PARA" '
         .comments[-5:]
         | map(select(.body | startswith("**De: " + $de + " Para: " + $para)))
         | length'
   ```
   Se retornar > 0, PARAR e pedir confirmação — pode ser handoff repetido por engano.

## Validação dos 6 campos ([contrato operacional §8](../../../../ai-governance/policies/agent-operating-contract.md))

| Campo | Argumento | Obrigatório? |
|---|---|---|
| Contexto (issue) | `<numero-issue>` (posição 1) | sim |
| Decisão tomada ou pendente | `--decisao "..."` | sim |
| Arquivos afetados | `--arquivos "..."` (lista, vírgula) | sim se o handoff envolve mudança de código; opcional em handoff de decisão pura |
| Validações realizadas | derivado automaticamente (ver abaixo) | sim |
| Pendências | `--pendencias "..."` | sim (aceita `nenhuma`) |
| Próximo responsável | `--para <agente>` | sim |
| Effort mínimo exigido | `--effort-minimo "alto"` | não — omitir = padrão |

**Effort mínimo exigido:** ver [Model / effort de cada agente](../../agents/) — desde que o squad
deixou de usar Opus, effort dentro do Sonnet é o único jeito de sinalizar "isso é sensível, não
trata como refactor comum". Marcar `--effort-minimo "alto"` sempre que o handoff envolver:

- número/métrica que precisa bater exato (threshold de diagnóstico RSRP/canal, cálculo de
  speedtest, contrato/mapper NDS);
- verificação numérica no gate do Caio — nunca deixar isso rodar em effort padrão ou Haiku (memória
  do projeto: já reprovou PR citando números que não batiam com nenhum commit real);
- qualquer mudança que Claudete ou Camilo já classificariam como "grande" ou "risco de regressão".

Sem o campo, o próximo agente decide o effort sozinho — o que é o comportamento de sempre para
handoff comum. Com o campo, o próximo agente **não pode** rodar abaixo do effort declarado.

**Validações automáticas** — a skill roda e cita o resultado no comentário:

```bash
# Se o handoff é de código:
./android/gradlew ktlintCheck detekt test 2>&1 | tail -20   # se módulo Android
npm test 2>&1 | tail -20                                     # se Admin/Workers
bash scripts/validar-docs.sh --base main 2>&1 | tail -10     # se docs
```

Se nenhum comando faz sentido para o handoff (pura decisão de produto/design), citar `manual — sem código executável nesta etapa`.

## Formato canônico do comentário

Após pre-flight OK, monta e posta:

```bash
BRANCH=$(git branch --show-current)
PR_URL=$(gh pr list --search "head:$BRANCH" --json url --repo buildea-labs/signallq --jq '.[0].url // "sem PR aberta"')

gh issue comment <numero> --repo buildea-labs/signallq --body "$(cat <<EOF
**De: <DE> Para: <PARA> — Decisão: <DECISAO>**

- **Arquivos:** <ARQUIVOS ou "n/a">
- **Validações:** <resultado automático>
- **Pendências:** <PENDENCIAS>
- **Riscos:** <RISCOS ou "nenhum identificado">
- **Effort mínimo:** <EFFORT_MINIMO ou "padrão">
- **Branch:** \`$BRANCH\` · **PR:** $PR_URL
EOF
)"
```

O comentário retorna URL — capturar e reportar ao usuário.

## Saída da skill

**Sucesso:**
```
✓ Pre-flight OK · comentário postado em <URL>
Próximo: @<PARA> assume a issue #<N>.
```

**Falha:**
```
✗ Pre-flight bloqueou o handoff:
  - <check falhou 1>
  - <check falhou 2>
Corrija e rode novamente. Nenhum comentário foi postado.
```

## O que a skill NÃO faz

- Não muda label ou assignee da issue — quem faz é o próximo agente ao assumir.
- Não move card de board — auto-mover-board.yml faz baseado em label.
- Não notifica Slack manualmente — GitHub Issues → Slack integration já faz.
- Não pergunta ao usuário se pode postar — se o pre-flight passa, posta.

## Interação com o contrato

Enquanto o [contrato reduzido](https://github.com/buildea-labs/signallq/issues/1616) não estiver em vigor:

- Skill é **opcional** — pode ser invocada quando o agente quiser garantir formato consistente. Não bloqueia handoff manual via comentário livre.
- Piloto de 2 semanas: registrar taxa de PASS no primeiro try, campos que mais faltam, tempo médio.

Quando/se o contrato reduzido entrar em vigor:

- Skill é **obrigatória** em toda transição — handoff via comentário livre passa a ser tratado como "handoff não feito" pelo próximo agente.

## Referências

- [Contrato operacional §8 — Handoff](../../../../ai-governance/policies/agent-operating-contract.md)
- [Proposta de contrato reduzido — #1616](https://github.com/buildea-labs/signallq/issues/1616)
- [Fluxo de handoff canônico](../../fluxos/HANDOFF_RULES.md)
- Skill complementar: [`/check-done`](../check-done/SKILL.md) — usada antes de handoff que declara conclusão.
