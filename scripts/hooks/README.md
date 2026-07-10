# Git hooks — guardrail cross-stack

Hooks versionados do monorepo SignallQ. Ativação por clone (o Git não versiona `core.hooksPath`):

- Windows / PowerShell: `./scripts/setup-hooks.ps1`
- bash: `./scripts/setup-hooks.sh`

## pre-commit

Bloqueia commit que cruza stacks (`android/`, `SignallQ Admin/`, `integrations/`).

Motivo: a remoção de worktree de agente com mudanças pendentes despeja essas mudanças no
working tree da branch ativa, contaminando a branch errada. O guardrail barra isso no commit.

Override consciente (quando o commit multi-stack é realmente intencional):

```
COMMIT_ALLOW_CROSS_STACK=1 git commit ...
```

## pre-push

Se o push tocar `android/`, roda `ktlintCheck` + `detekt` localmente antes de liberar — os
mesmos dois checks já obrigatórios no branch protection de `main`. Bloqueia o push em caso de
falha, e sempre imprime um lembrete pra rodar `./gradlew test` completo (não só o módulo
tocado) antes de confiar no push.

Motivo: a PR #869 quase mergeou com `Unit Tests` quebrado porque só o módulo tocado foi
testado localmente, não a suite inteira. Esse hook pega ktlint/detekt minutos antes do CI;
`Unit Tests` continua exigindo rodar a suite manualmente (é lento demais pra travar todo push).

Override consciente:

```
PUSH_SKIP_LOCAL_CHECKS=1 git push ...
```
