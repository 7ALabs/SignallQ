#!/usr/bin/env bash
# observe-and-act.sh — Scanner estático pós-push.
# Detecta violações conhecidas do Linka em arquivos alterados e abre GitHub issues automaticamente.
# Executado como hook PostToolUse após git push. Sem LLM, sem Slack — rápido e determinístico.

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "$0")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
OBS_DIR="$REPO_ROOT/.claude/agent-observations/$(date +%Y-%m)"
OBS_FILE="$OBS_DIR/obs-$(date +%Y-%m-%d)-staticcheck.md"
ISSUES_OPENED=0

mkdir -p "$OBS_DIR"

cd "$REPO_ROOT"

# Arquivos Kotlin alterados desde o merge-base com main
CHANGED=$(git diff --name-only "$(git merge-base HEAD origin/main)" HEAD 2>/dev/null \
  | grep '\.kt$' || true)

[ -z "$CHANGED" ] && exit 0

open_issue() {
  local title="$1" body="$2" label="${3:-type:bug}"
  # Verifica se já existe issue com título similar para evitar duplicatas
  EXISTING=$(gh issue list --state open --search "$title" --json number --limit 1 \
    | python3 -c "import json,sys; d=json.load(sys.stdin); print(d[0]['number'] if d else '')" 2>/dev/null || true)
  if [ -n "$EXISTING" ]; then
    echo "[observe-and-act] Issue já existe: #$EXISTING — pulando." >&2
    return
  fi
  gh issue create --title "$title" --label "$label" --body "$body" > /dev/null 2>&1 && \
    { echo "[observe-and-act] Issue criada: $title" >&2; ISSUES_OPENED=$((ISSUES_OPENED+1)); }
}

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
    open_issue \
      "[BUG] String técnica exposta na UI — $FILE" \
      "## Detectado por observe-and-act (scanner estático)

Arquivo: \`$FILE\`
Violação: \`.name\`, \`.toString()\` ou \`.message\` passado diretamente para \`Text()\`

\`\`\`
$LINES
\`\`\`

Strings internas de código nunca devem ser exibidas ao usuário. Mapear para string resource ou mensagem tratada.

## Critérios de aceite
- [ ] Nenhuma string interna (.name, .toString(), .message) exposta diretamente na UI
- [ ] Mensagem substituída por string resource ou mapeamento explícito" \
      "type:bug"
  fi

  # ── VIOLAÇÃO 2: LazyColumn aninhado ──────────────────────────────────────
  # Detecta LazyColumn dentro de item {} (padrão de aninhamento proibido)
  if grep -qE '^\s*item\s*\{' "$FILE" 2>/dev/null; then
    ITEM_LINES=$(grep -n 'item {' "$FILE")
    # Verifica se há LazyColumn dentro de algum bloco item
    if awk '/item\s*\{/{p=1} p && /LazyColumn/{print; p=0}' "$FILE" | grep -q 'LazyColumn'; then
      echo "## VIOLAÇÃO: LazyColumn aninhado — $FILE" >> "$OBS_FILE"
      open_issue \
        "[BUG] LazyColumn aninhado detectado — $FILE" \
        "## Detectado por observe-and-act (scanner estático)

Arquivo: \`$FILE\`
Violação: \`LazyColumn\` encontrado dentro de bloco \`item {}\` de outro \`LazyColumn\`.

LazyColumn aninhado causa crash em runtime (IllegalStateException) ou comportamento indefinido de scroll.

## Solução
Converter os itens do LazyColumn interno para \`items()\` no LazyColumn pai.

## Critérios de aceite
- [ ] Sem LazyColumn dentro de item{} de outro LazyColumn" \
        "type:bug"
    fi
  fi

  # ── VIOLAÇÃO 3: Toast com mensagem técnica ───────────────────────────────
  if grep -qE 'Toast\.makeText.*"[A-Z][a-z]+[A-Z]' "$FILE" 2>/dev/null; then
    LINES=$(grep -nE 'Toast\.makeText.*"[A-Z][a-z]+[A-Z]' "$FILE" | head -3)
    echo "## VIOLAÇÃO: Toast com string técnica — $FILE" >> "$OBS_FILE"
    echo "$LINES" >> "$OBS_FILE"
    open_issue \
      "[BUG] Toast exibe string técnica/camelCase — $FILE" \
      "## Detectado por observe-and-act (scanner estático)

Arquivo: \`$FILE\`
Violação: Toast exibe string que parece nome técnico (camelCase detectado).

\`\`\`
$LINES
\`\`\`

## Critérios de aceite
- [ ] Toast substituído por mensagem em português legível pelo usuário" \
      "type:bug"
  fi

  # ── VIOLAÇÃO 4: TODO/FIXME em código de produção ─────────────────────────
  TODOS=$(grep -nE '//\s*(TODO|FIXME|HACK|XXX):' "$FILE" 2>/dev/null | head -5 || true)
  if [ -n "$TODOS" ]; then
    echo "## AVISO: TODO/FIXME em produção — $FILE" >> "$OBS_FILE"
    echo "$TODOS" >> "$OBS_FILE"
    open_issue \
      "[TECH DEBT] TODOs não resolvidos em $FILE" \
      "## Detectado por observe-and-act (scanner estático)

Arquivo: \`$FILE\`
TODOs encontrados:

\`\`\`
$TODOS
\`\`\`

Resolver ou mover para issue antes do merge em main." \
      "type:tech-debt"
  fi

  # ── VIOLAÇÃO 5: hardcoded color sem token ─────────────────────────────────
  if grep -qE 'Color\(0x|Color\.White\b|Color\.Black\b|Color\.Red\b|Color\.Green\b' "$FILE" 2>/dev/null; then
    LINES=$(grep -nE 'Color\(0x|Color\.White\b|Color\.Black\b|Color\.Red\b|Color\.Green\b' "$FILE" | head -3)
    echo "## VIOLAÇÃO: cor hardcoded sem token — $FILE" >> "$OBS_FILE"
    echo "$LINES" >> "$OBS_FILE"
    open_issue \
      "[BUG] Cor hardcoded sem token de design system — $FILE" \
      "## Detectado por observe-and-act (scanner estático)

Arquivo: \`$FILE\`
Violação: cor hardcoded em vez de token \`LkColors\` ou \`MaterialTheme.colorScheme\`.

\`\`\`
$LINES
\`\`\`

## Critérios de aceite
- [ ] Substituir por token do design system (LkColors.* ou c.textPrimary, etc.)" \
      "type:bug"
  fi

done

echo "" >> "$OBS_FILE"
echo "Issues abertas nesta execução: $ISSUES_OPENED" >> "$OBS_FILE"

echo "[observe-and-act] Scan concluído. Issues abertas: $ISSUES_OPENED" >&2
exit 0
