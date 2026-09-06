# Skills canônicas do SignallQ

Este diretório é a **fonte canônica** das skills do SignallQ.

Skills descrevem procedimentos reutilizáveis e não personas. O roteamento entre Cora, Davi, Ramon, Breno e Camillo é definido em [`AGENTS.md`](../../AGENTS.md) e [`.agents/WORKFLOW.md`](../WORKFLOW.md).

Os diretórios `.claude/skills/` e `.github/skills/` são espelhos de compatibilidade.

Depois de alterar uma skill aqui, execute:

```bash
./scripts/sync-skills-mirrors.sh
./scripts/sync-skills-mirrors.sh --check
```

Não mantenha regra exclusiva nos espelhos.
