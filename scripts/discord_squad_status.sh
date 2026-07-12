#!/usr/bin/env bash
# Notifica o status geral da squad (em andamento / fila / pontos) no Discord.
# Usado pelo Juninho no heartbeat de 15min enquanto ha task ativa em autopilot/workflow.
#
# Uso: scripts/discord_squad_status.sh < status.json
#
# Schema esperado no stdin (JSON):
# {
#   "em_andamento": [{"issue": "898", "titulo": "IA texto tecnico", "agente": "Camilo", "pontos": 5}],
#   "fila": [{"issue": "862", "titulo": "Badge de rede", "pontos": 1}],
#   "sessao_pts_usados": 13,
#   "sessao_pts_teto": 20
# }
set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

# Carrega .env se existir
if [ -f .env ]; then
  set -a; . ./.env; set +a
fi

WEBHOOK="${DISCORD_WEBHOOK_LINKA:-}"
if [ -z "$WEBHOOK" ]; then
  echo "[discord_squad_status] DISCORD_WEBHOOK_LINKA não configurada em .env — pulando" >&2
  exit 0
fi

TMP_JSON="$(mktemp)"
trap 'rm -f "$TMP_JSON"' EXIT
cat > "$TMP_JSON"

PAYLOAD="$(python3 "$SCRIPT_DIR/_discord_squad_status_payload.py" "$TMP_JSON")"

curl -sS -H "Content-Type: application/json" -X POST -d "$PAYLOAD" "$WEBHOOK" >/dev/null || \
  echo "[discord_squad_status] falha ao postar" >&2
