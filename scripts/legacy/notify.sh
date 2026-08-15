#!/usr/bin/env bash
# Despacha notificação para Discord E Slack simultaneamente.
# Os agentes podem chamar este wrapper em vez de discord_notify.sh diretamente.
# Uso idêntico: scripts/notify.sh <agente> "<mensagem>" <tipo> [--para <outroAgente>]
SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
"$SCRIPT_DIR/discord_notify.sh" "$@" &
"$SCRIPT_DIR/slack_notify.sh" "$@" &
wait
