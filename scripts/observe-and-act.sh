#!/usr/bin/env bash
# observe-and-act.sh — Scanner estático pós-push.
# Detecta violações conhecidas do SignallQ em arquivos alterados e registra em log local.
# Executado como hook PostToolUse após git push. Sem LLM, sem Slack, sem GitHub — rápido e determinístico.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OBS_DIR="$REPO_ROOT/.claude/agent-observations/$(date +%Y-%m)"
OBS_FILE="$OBS_DIR/obs-$(date +%Y-%m-%d)-staticcheck.md"

mkdir -p "$OBS_DIR"

cd "$REPO_ROOT"

# Arquivos Kotlin alterados desde o merge-base com main
CHANGED=$(git diff --name-only "$(git merge-base HEAD origin/main)" HEAD 2>/dev/null \
  | grep '\.kt$' || true)

[ -z "$CHANGED" ] && exit 0

echo "# Observe-and-Act — $(date +%Y-%m-%d %H:%M)" >> "$OBS_FILE"
echo "Arquivos analisados: $(echo "$CHANGED" | wc -l | tr -d ' ')" >> "$OBS_FILE"
echo "" >> "$OBS_FILE"

for FILE in $CHANGED; do
  [ -f "$FILE" ] || continue

  # ── VIOLAÇÃO 1: String técnica bruta em Text() ────────────────────────────
  # Detecta .name, .toString() ou .message dentro de chamadas Text(
  if grep -qE 'Text\([^)]*\.(name|toString\(\)|message)\b' "$FILE" 2>/dev/null; then
    LINES=$(grep -nE 'Text\([^)]*\.(name|toString\(\)|message)\b' "$FILE" | head -3)
    echo "## VIOLAÇÃO: string técnica em Text() — $FILE" >> "$OBS_FILE"
    echo "$LINES" >> "$OBS_FILE"
  fi

  # ── VIOLAÇÃO 2: LazyColumn aninhado ──────────────────────────────────────
  # Detecta LazyColumn dentro de item {} (padrão de aninhamento proibido)
  if grep -qE '^\s*item\s*\{' "$FILE" 2>/dev/null; then
    ITEM_LINES=$(grep -n 'item {' "$FILE")
    # Verifica se há LazyColumn dentro de algum bloco item
    if awk '/item\s*\{/{p=1} p && /LazyColumn/{print; p=0}' "$FILE" | grep -q 'LazyColumn'; then
      echo "## VIOLAÇÃO: LazyColumn aninhado — $FILE" >> "$OBS_FILE"
    fi
  fi

  # ── VIOLAÇÃO 3: Toast com mensagem técnica ───────────────────────────────
  if grep -qE 'Toast\.makeText.*"[A-Z][a-z]+[A-Z]' "$FILE" 2>/dev/null; then
    LINES=$(grep -nE 'Toast\.makeText.*"[A-Z][a-z]+[A-Z]' "$FILE" | head -3)
    echo "## VIOLAÇÃO: Toast com string técnica — $FILE" >> "$OBS_FILE"
    echo "$LINES" >> "$OBS_FILE"
  fi

  # ── VIOLAÇÃO 4: TODO/FIXME em código de produção ─────────────────────────
  TODOS=$(grep -nE '//\s*(TODO|FIXME|HACK|XXX):' "$FILE" 2>/dev/null | head -5 || true)
  if [ -n "$TODOS" ]; then
    echo "## AVISO: TODO/FIXME em produção — $FILE" >> "$OBS_FILE"
    echo "$TODOS" >> "$OBS_FILE"
  fi

  # ── VIOLAÇÃO 5: hardcoded color sem token ─────────────────────────────────
  if grep -qE 'Color\(0x|Color\.White\b|Color\.Black\b|Color\.Red\b|Color\.Green\b' "$FILE" 2>/dev/null; then
    LINES=$(grep -nE 'Color\(0x|Color\.White\b|Color\.Black\b|Color\.Red\b|Color\.Green\b' "$FILE" | head -3)
    echo "## VIOLAÇÃO: cor hardcoded sem token — $FILE" >> "$OBS_FILE"
    echo "$LINES" >> "$OBS_FILE"
  fi

done

echo "[observe-and-act] Scan concluído. Log: $OBS_FILE" >&2
exit 0
