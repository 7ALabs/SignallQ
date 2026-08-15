# Legacy Scripts

Scripts nesta pasta foram preservados porque documentam ferramentas antigas do repositório, mas não fazem parte de nenhum fluxo ativo.

Não devem ser usados no fluxo atual sem uma tarefa explícita de reativação/migração.

## Handoff de agentes (depreciado desde 2026-07-05)

| Script | Origem | Substituto |
|---|---|---|
| `agent-handoff.sh` | Squad de 5 (Claudete/Camilo/Felipe/Lia/Gema) — [ADR-006](../../docs_ai/decisions/ADR-006-workflow-squad-5-agentes.md) | Comentário na issue no formato "De: X Para: Y — Decisão: ... Pendente: ... Riscos: ..." conforme [ADR-014](../../docs_ai/decisions/ADR-014-squad-canonico-ai-governance.md) e [`.claude/fluxos/HANDOFF_RULES.md`](../../.claude/fluxos/HANDOFF_RULES.md) |
| `notify.sh` · `discord_notify.sh` · `slack_notify.sh` | Board Discord + notificações manuais | GitHub Issues notifica Slack diretamente; não criar fluxo manual paralelo |

Movidos para cá em 2026-08-15 (commit da consolidação pós-ADR-014) após confirmar via grep que nada em `.github/`, `android/`, `integrations/` ou hooks invoca esses scripts.

Se algum dia forem realmente removidos, o git preserva; mover para cá deixa a fonte próxima do resto do repositório enquanto se confirma que ninguém em CI/hooks os invocava — apagar direto do `scripts/` seria irreversível sem `git revert` explícito.

## Scripts do app Flutter legado (`source/app`)

- `flutter-modem/`, `flutter-oui/` — dependem de caminhos e código do app Flutter legado.

Para reativar qualquer script daqui, primeiro:

1. Abrir uma tarefa explícita de migração.
2. Atualizar caminhos e referências para o layout atual do repositório (raiz relativa, sem `C:\...`).
3. Remover referências a `source/app` quando o alvo for Android Kotlin ou os Workers.
4. Documentar o novo comando em `scripts/README.md`.
