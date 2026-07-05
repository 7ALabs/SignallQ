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
