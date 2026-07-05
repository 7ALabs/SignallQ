# Notificações do Squad SIGNALLQ — Slack + Discord

Os 11 agentes do squad notificam o usuário em cada handoff via scripts em `scripts/`.

## Como funciona

| Script | Quando usar |
|---|---|
| `scripts/notify.sh` | **Recomendado** — dispara em Discord E Slack ao mesmo tempo |
| `scripts/discord_notify.sh` | Só Discord (usado pelos agentes Camilo, Claudete, Gema, Lia, Marcelo conforme suas specs) |
| `scripts/slack_notify.sh` | Só Slack |

### Assinatura

```bash
scripts/notify.sh <agente> "<mensagem>" <tipo> [--para <outroAgente>]
```

- `agente`: claudete, claudio, camilo, lia, gema, marcelo, nina, taisa, bernardo, otavio
- `tipo`: `progress` | `success` | `info` | `warning` | `error`
- `--para <agente>`: opcional, indica handoff

### Exemplos (reais dos agentes)

```bash
scripts/notify.sh camilo "iniciando #3 (Hilt)" progress
scripts/notify.sh camilo "Hilt configurado em featureSpeedtest" success --para gema
scripts/notify.sh gema "review iniciado em PR #42" progress
scripts/notify.sh gema "regressão em ResultadoVelocidadeScreen" error --para camilo
scripts/notify.sh claudete "sprint encerrada: 4 issues fechadas" success
```

## Setup — uma vez

### 1. Discord (via webhook em bash)
Já configurado em `.env` como `DISCORD_WEBHOOK_LINKA`. Os agentes chamam `scripts/discord_notify.sh` diretamente.

### 2. Slack (via MCP, não webhook)
O canal `#projeto-signallq` (`C0B4NSGSK1D`, workspace `squaddispatch`) já está configurado em `.env` (`SLACK_CHANNEL_LINKA_ID`). O envio é feito pelo orquestrador (Claude) usando o MCP do Slack — **não precisa webhook**.

`SLACK_WEBHOOK_LINKA` no `.env` é opcional: se preenchido, o `slack_notify.sh` posta direto via curl (útil para agentes rodando fora do contexto MCP). Se vazio, o script é no-op e o orquestrador é quem entrega via MCP.

### 3. Eventos do GitHub (issues/PRs movendo no board)

**Slack** — via app oficial do GitHub:
```
/github subscribe gmmattey/signallq-android issues pulls reviews commits
```

**Discord** — via webhook nativo:
1. Discord → canal → ⚙️ Webhooks → copie URL
2. Acrescente `/github` no fim da URL
3. GitHub → `gmmattey/signallq-android` → Settings → Webhooks → Add webhook
4. Cole a URL com `/github`, content-type `application/json`, eventos: Issues, Pull requests, Pull request reviews, Push

## Resumo diário

Para receber um resumo às 18h BRT, configure via skill `/schedule`:

```bash
/schedule "Resumo diário do squad SIGNALLQ" "scripts/notify.sh claudete \"$(gh issue list --repo gmmattey/signallq-android --json number,title,labels --jq 'length') issues abertas hoje\" info" "0 18 * * 1-5"
```

## Teste rápido

```bash
scripts/notify.sh claude "teste de webhook" info
```

Se aparecer no Discord e no Slack, está funcionando. Se não, confira `.env` e se os webhooks estão ativos.

## Privacidade

- Os webhooks ficam **só em `.env`** (gitignored).
- `.env.example` versionado tem apenas as chaves vazias.
- Nunca compartilhe URLs de webhook — quem tem a URL pode postar como o bot.
