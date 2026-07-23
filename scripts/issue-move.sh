#!/usr/bin/env bash
# Move uma issue para outra coluna do Project board e ajusta labels/estado.
#
# Uso: scripts/issue-move.sh <numero> <coluna> [agente]
#
# Colunas (case-insensitive, aceita aliases):
#   backlog       | Backlog
#   triagem       | Triagem
#   ready         | Pronta para dev
#   in-progress   | Em andamento
#   review        | Em review
#   docs          | Docs & Higiene
#   done          | Done
#
# Efeitos por coluna:
#   triagem      +agent:juninho (triagem mecânica antes de virar ready)
#   ready        +status:agent-ready
#   in-progress  +agent:<agente>  -status:agent-ready  +status:in-progress (cria se faltar)
#   review       +status:waiting-review  -status:in-progress
#   docs         -status:waiting-review  +agent:rhodolfo (se nenhum agent: presente)
#   done         fecha a issue, remove status:*
set -e

GH="${GH:-gh}"
[ -x "/c/Program Files/GitHub CLI/gh.exe" ] && GH="/c/Program Files/GitHub CLI/gh.exe"

REPO="${REPO:-7ALabs/SignallQ}"
OWNER="${OWNER:-gmmattey}"
PROJ_NUM="${PROJ_NUM:-8}"
PROJ_ID="${PROJ_ID:-PVT_kwHOD83n7c4BYmdL}"
FIELD_COLUNA="${FIELD_COLUNA:-PVTSSF_lAHOD83n7c4BYmdLzhTrIt4}"

# Option IDs do campo "Coluna"
declare -A OPT=(
  [backlog]="aed7967a"
  [triagem]="6193a896"
  [ready]="22f4f491"
  [in-progress]="55b9be2a"
  [review]="dd9ed333"
  [docs]="bff9d55b"
  [done]="9305bbcf"
)

declare -A NAME=(
  [backlog]="Backlog"
  [triagem]="Triagem"
  [ready]="Pronta para dev"
  [in-progress]="Em andamento"
  [review]="Em review"
  [docs]="Docs & Higiene"
  [done]="Done"
)

NUM="$1"
COL_RAW="$2"
AGENT="$3"

if [ -z "$NUM" ] || [ -z "$COL_RAW" ]; then
  echo "uso: $0 <issue#> <coluna> [agente]" >&2
  exit 2
fi

# Normaliza coluna (aceita aliases em português)
case "$(echo "$COL_RAW" | tr '[:upper:]' '[:lower:]')" in
  backlog) COL=backlog ;;
  triagem|refinement) COL=triagem ;;
  ready|pronta*|"pronta para dev") COL=ready ;;
  in-progress|in_progress|inprogress|"em andamento"|wip|doing) COL=in-progress ;;
  review|"em review"|waiting-review) COL=review ;;
  docs|"docs & higiene"|hygiene|rhodolfo) COL=docs ;;
  done|"feito") COL=done ;;
  *) echo "coluna inválida: $COL_RAW" >&2; exit 2 ;;
esac

OPT_ID="${OPT[$COL]}"
COL_NAME="${NAME[$COL]}"

# 1) Achar item ID no board
ITEM_ID=$("$GH" project item-list "$PROJ_NUM" --owner "$OWNER" --format json --limit 200 \
  --jq ".items[] | select(.content.number == $NUM) | .id" 2>/dev/null)

if [ -z "$ITEM_ID" ]; then
  echo "issue #$NUM não está no Project $PROJ_NUM — adicionando..." >&2
  ITEM_ID=$("$GH" project item-add "$PROJ_NUM" --owner "$OWNER" \
    --url "https://github.com/$REPO/issues/$NUM" --format json --jq '.id')
fi

# 2) Set Coluna
"$GH" project item-edit --project-id "$PROJ_ID" --id "$ITEM_ID" \
  --field-id "$FIELD_COLUNA" --single-select-option-id "$OPT_ID" >/dev/null

echo "✓ #$NUM → $COL_NAME"

# 3) Ajustes de label / estado por coluna
case "$COL" in
  ready)
    "$GH" issue edit "$NUM" --repo "$REPO" --add-label "status:agent-ready" >/dev/null 2>&1 || true
    ;;
  in-progress)
    "$GH" issue edit "$NUM" --repo "$REPO" \
      --remove-label "status:agent-ready" --add-label "status:in-progress" >/dev/null 2>&1 || true
    if [ -n "$AGENT" ]; then
      # remove qualquer agent:* antigo e seta o novo
      EXISTING=$("$GH" issue view "$NUM" --repo "$REPO" --json labels \
        --jq '.labels[].name | select(startswith("agent:"))' | tr '\n' ',' | sed 's/,$//')
      [ -n "$EXISTING" ] && "$GH" issue edit "$NUM" --repo "$REPO" --remove-label "$EXISTING" >/dev/null 2>&1 || true
      "$GH" issue edit "$NUM" --repo "$REPO" --add-label "agent:$AGENT" >/dev/null 2>&1 || true
    fi
    ;;
  triagem)
    "$GH" issue edit "$NUM" --repo "$REPO" --add-label "agent:juninho" >/dev/null 2>&1 || true
    ;;
  review)
    "$GH" issue edit "$NUM" --repo "$REPO" \
      --remove-label "status:in-progress" --add-label "status:waiting-review,agent:rhodolfo" >/dev/null 2>&1 || true
    ;;
  docs)
    "$GH" issue edit "$NUM" --repo "$REPO" \
      --remove-label "status:waiting-review" --add-label "agent:rhodolfo" >/dev/null 2>&1 || true
    ;;
  done)
    "$GH" issue close "$NUM" --repo "$REPO" --reason completed >/dev/null 2>&1 || true
    for s in agent-ready in-progress waiting-review; do
      "$GH" issue edit "$NUM" --repo "$REPO" --remove-label "status:$s" >/dev/null 2>&1 || true
    done
    ;;
esac

# Garantir que status:in-progress label existe (criar uma vez)
"$GH" label create "status:in-progress" --repo "$REPO" --color "fbca04" --description "Agente atuando agora" --force >/dev/null 2>&1 || true
