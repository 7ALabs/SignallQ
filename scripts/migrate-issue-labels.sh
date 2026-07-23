#!/usr/bin/env bash
# Migra labels das issues: severity:* -> priority:*, adiciona agent:claudete + status:agent-ready
set -e
GH="${GH:-gh}"
REPO="${REPO:-7ALabs/linka-android}"

map_severity() {
  case "$1" in
    critical) echo "priority:p0" ;;
    high)     echo "priority:p1" ;;
    medium|low) echo "priority:p2" ;;
    *)        echo "" ;;
  esac
}

for n in $(seq 1 24); do
  # severidade atual
  sev=$("$GH" issue view "$n" --repo "$REPO" --json labels --jq '.labels[].name | select(startswith("severity:"))' | sed 's/severity://')
  pri=$(map_severity "$sev")
  if [ -z "$pri" ]; then
    echo "#$n: sem severity, pulando ajuste de prioridade"
    "$GH" issue edit "$n" --repo "$REPO" --add-label "agent:claudete,status:agent-ready" >/dev/null
  else
    "$GH" issue edit "$n" --repo "$REPO" \
      --remove-label "severity:$sev" \
      --add-label "$pri,agent:claudete,status:agent-ready" >/dev/null
    echo "#$n: severity:$sev -> $pri  +agent:claudete +status:agent-ready"
  fi
done

echo ""
echo "Removendo label duplicada area:security (canônica = area:seguranca)..."
"$GH" label delete area:security --repo "$REPO" --yes 2>/dev/null && echo "✓ area:security removida" || echo "(area:security já não existe)"

echo ""
echo "Removendo labels severity:* obsoletas..."
for s in critical high medium low; do
  "$GH" label delete "severity:$s" --repo "$REPO" --yes 2>/dev/null && echo "✓ severity:$s removida" || true
done
