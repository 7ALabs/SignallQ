# Contribuindo com o SignallQ

Este projeto usa agentes autônomos (Claude Code) como squad principal de desenvolvimento. As
regras completas de processo, autonomia e disciplina de branches/PRs vivem em `.claude/CLAUDE.md`
— este arquivo existe só para dar visibilidade rápida.

## Checks obrigatórios para merge em `main`

- `Ktlint`, `Detekt` e `Unit Tests` (GitHub Actions) — branch protection exige os três verdes.
- Branch precisa estar atualizada com `main` antes do merge (`strict: true`) — o workflow
  `auto-update-branch.yml` atualiza PRs abertas automaticamente a cada push em `main`.

## Antes de abrir PR

- Rode `./scripts/setup-hooks.sh` (ou `.ps1` no Windows) uma vez por clone — ativa os hooks
  versionados em `scripts/hooks/`: guardrail cross-stack no commit e lint local no push.
- Use o template de PR (`.github/pull_request_template.md`).
- Nunca declare "PR mergeada"/"teste passou"/"publicado em produção" sem verificar de fato
  (`gh pr view`, `gh pr checks`, endpoint real) — ver `.claude/CLAUDE.md`.

Para tudo além disso (papéis dos agentes, autonomia, rotinas, disciplina de branches) ver
`.claude/CLAUDE.md`.
