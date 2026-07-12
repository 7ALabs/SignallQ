#!/usr/bin/env bash
# Notifica o canal Discord do squad LINKA.
# Uso: scripts/discord_notify.sh <agente> "<mensagem>" <tipo> [--para <outroAgente>]
# Tipos: progress | success | info | warning | error
set -e

AGENT="${1:-claude}"
MSG="${2:-}"
TYPE="${3:-info}"
shift 3 || true
TARGET=""
if [ "${1:-}" = "--para" ]; then
  TARGET="$2"
fi

# Carrega .env se existir
if [ -f .env ]; then
  set -a; . ./.env; set +a
fi

WEBHOOK="${DISCORD_WEBHOOK_LINKA:-}"
if [ -z "$WEBHOOK" ]; then
  echo "[discord_notify] DISCORD_WEBHOOK_LINKA não configurada em .env — pulando" >&2
  exit 0
fi

# Sem emoji (regra de marca SignallQ) — so cor do embed marca o tipo.
case "$TYPE" in
  progress) COLOR=3447003 ;;   # azul
  success)  COLOR=3066993 ;;   # verde
  info)     COLOR=10070709 ;;
  warning)  COLOR=15844367 ;;  # amarelo
  error)    COLOR=15158332 ;;  # vermelho
  *)        COLOR=9807270 ;;
esac

TITLE="$(printf '%s' "$AGENT" | tr '[:lower:]' '[:upper:]')"
[ -n "$TARGET" ] && TITLE="$TITLE → $(printf '%s' "$TARGET" | tr '[:lower:]' '[:upper:]')"

# Capitaliza a primeira letra pra exibir como remetente do webhook (em vez do
# nome padrao "Captain Hook" que o Discord da a webhook nunca renomeado).
DISPLAY_AGENT="$(printf '%s' "$AGENT" | sed 's/./\U&/')"

PAYLOAD=$(cat <<JSON
{
  "username": "$DISPLAY_AGENT · SignallQ",
  "embeds": [{
    "title": "$TITLE",
    "description": $(printf '%s' "$MSG" | python -c 'import json,sys; print(json.dumps(sys.stdin.read()))' 2>/dev/null || echo "\"$MSG\""),
    "color": $COLOR,
    "footer": {"text": "LINKA Android · squad · $(date +%H:%M)"}
  }]
}
JSON
)

curl -sS -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$WEBHOOK" >/dev/null || \
  echo "[discord_notify] falha ao postar" >&2
