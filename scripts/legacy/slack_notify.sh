#!/usr/bin/env bash
# Notifica o canal Slack do squad LINKA.
# Uso: scripts/slack_notify.sh <agente> "<mensagem>" <tipo> [--para <outroAgente>]
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

if [ -f .env ]; then
  set -a; . ./.env; set +a
fi

WEBHOOK="${SLACK_WEBHOOK_LINKA:-}"
if [ -z "$WEBHOOK" ]; then
  echo "[slack_notify] SLACK_WEBHOOK_LINKA não configurada em .env — pulando" >&2
  exit 0
fi

case "$TYPE" in
  progress) EMOJI=":hourglass_flowing_sand:" ;;
  success)  EMOJI=":white_check_mark:" ;;
  info)     EMOJI=":information_source:" ;;
  warning)  EMOJI=":warning:" ;;
  error)    EMOJI=":x:" ;;
  *)        EMOJI=":small_blue_diamond:" ;;
esac

HEADER="*$EMOJI $AGENT*"
[ -n "$TARGET" ] && HEADER="$HEADER → *$TARGET*"

# Escape de aspas para JSON
MSG_ESC=$(printf '%s' "$MSG" | sed 's/\\/\\\\/g; s/"/\\"/g')

PAYLOAD=$(cat <<JSON
{
  "text": "$HEADER\n$MSG_ESC",
  "username": "LINKA Squad"
}
JSON
)

curl -sS -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$WEBHOOK" >/dev/null || \
  echo "[slack_notify] falha ao postar" >&2
