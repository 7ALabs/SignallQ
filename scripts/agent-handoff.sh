#!/usr/bin/env bash
# Wrapper de handoff usado pelos agentes do squad LINKA.
# Move o card no board + envia notificação Discord+Slack em uma chamada.
#
# Uso: scripts/agent-handoff.sh <agente> <evento> <issue#> "<mensagem>" [--para <outroAgente>]
#
# Eventos:
#   start        — começou a trabalhar       → coluna "Em andamento"     (notif: progress)
#   handoff      — passou para outro agente  → coluna depende do destino (notif: success)
#   review       — implementação pronta      → coluna "Em review"        (notif: success → rhodolfo revisa)
#   docs         — review aprovado           → coluna "Docs & Higiene"   (notif: info → rhodolfo consolida)
#   done         — fechado                   → coluna "Done"             (notif: success)
#   block        — bloqueado                 → mantém coluna             (notif: warning)
#   refine       — voltando para triagem     → coluna "Triagem"          (notif: info → claudete)
#   ready        — pronta para dev           → coluna "Pronta para dev"  (notif: info)
set -e

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"

AGENT="$1"
EVENT="$2"
ISSUE="$3"
MSG="$4"
shift 4 || true
TARGET_FLAG=""
TARGET_AGENT=""
if [ "${1:-}" = "--para" ]; then
  TARGET_FLAG="--para $2"
  TARGET_AGENT="$2"
fi

if [ -z "$AGENT" ] || [ -z "$EVENT" ] || [ -z "$ISSUE" ]; then
  echo "uso: $0 <agente> <evento> <issue#> \"<msg>\" [--para <outro>]" >&2
  echo "eventos: start|handoff|review|docs|done|block|refine|ready" >&2
  exit 2
fi

# Decide coluna + tipo de notificação por evento
NOTIFY_TYPE="info"
COL=""
case "$EVENT" in
  start)   COL="in-progress"; NOTIFY_TYPE="progress" ;;
  handoff) COL="in-progress"; NOTIFY_TYPE="success" ;;
  review)  COL="review";      NOTIFY_TYPE="success" ;;
  docs)    COL="docs";         NOTIFY_TYPE="info" ;;
  done)    COL="done";         NOTIFY_TYPE="success" ;;
  block)   COL="";             NOTIFY_TYPE="warning" ;;
  refine)  COL="triagem";      NOTIFY_TYPE="info" ;;
  ready)   COL="ready";        NOTIFY_TYPE="info" ;;
  *) echo "evento inválido: $EVENT" >&2; exit 2 ;;
esac

# Move card no board (se evento implica movimentação)
if [ -n "$COL" ]; then
  bash "$SCRIPT_DIR/issue-move.sh" "$ISSUE" "$COL" "$AGENT" || \
    echo "[agent-handoff] falha ao mover card #$ISSUE" >&2
fi

# Monta mensagem com link da issue
FULL_MSG="#$ISSUE — $MSG"$'\n'"https://github.com/7ALabs/linka-android/issues/$ISSUE"

# Notifica Discord + Slack
bash "$SCRIPT_DIR/notify.sh" "$AGENT" "$FULL_MSG" "$NOTIFY_TYPE" $TARGET_FLAG
