#!/usr/bin/env bash
set -euo pipefail

# Gera o bloco de inventario da documentacao a partir do codigo real e o injeta
# entre os marcadores INVENTARIO:INICIO / INVENTARIO:FIM nos documentos alvo.
#
# Existe porque fato numerico mantido a mao apodrece: em 2026-08-06 TECNICO.md
# declarava 0.30.1/67 contra 0.31.0/72 no codigo, e o dicionario canonico do
# pacote v5 afirmava que o SignallQ Pro "nao existe" tendo 9 modulos compilando.
# Nenhum humano mantem esses numeros corretos — entao nenhum humano os mantem.
#
# Uso: scripts/gerar-inventario-docs.sh [--check]
#   (sem flag)  reescreve o bloco nos documentos alvo
#   --check     nao escreve; sai 1 se algum documento estiver desatualizado

cd "$(dirname "$0")/.."

ALVOS=(
  "docs_ai/TECNICO.md"
  "docs_ai/ARQUITETURA/README.md"
)

CHECK_ONLY=false
[[ "${1:-}" == "--check" ]] && CHECK_ONLY=true

TOML="android/gradle/libs.versions.toml"
SETTINGS="android/settings.gradle.kts"

# --- versoes -----------------------------------------------------------------
val() { grep -E "^$1 *= *\"" "$TOML" | head -1 | sed 's/.*= *"//; s/".*//'; }

VERSION_NAME=$(val versionName)
VERSION_CODE=$(val versionCode)
PRO_VERSION_NAME=$(val proVersionName)
PRO_VERSION_CODE=$(val proVersionCode)
COMPILE_SDK=$(val compileSdk)
MIN_SDK=$(val minSdk)
TARGET_SDK=$(val targetSdk)
COMPOSE_BOM=$(val composeBom)
ROOM=$(val room)
HILT=$(val hilt)

# --- modulos -----------------------------------------------------------------
# So o bloco include(...) — evita apanhar as linhas project(...).projectDir.
# Comentarios sao removidos antes de extrair: o settings.gradle.kts cita aliases
# proibidos como contra-exemplo (`":pro:app", nao ":proApp"`), e sem isso o
# parser os contaria como modulos reais.
MODULOS=$(sed -n '/^include(/,/^)/p' "$SETTINGS" \
  | sed 's|//.*||' \
  | grep -oE '":[a-zA-Z0-9:_-]+"' | tr -d '"' | sort -u)

MOD_PRO=$(echo "$MODULOS" | grep -c '^:pro:' || true)
MOD_TOTAL=$(echo "$MODULOS" | grep -c . || true)
MOD_CONSUMER=$((MOD_TOTAL - MOD_PRO))

LISTA_CONSUMER=$(echo "$MODULOS" | grep -v '^:pro:' | paste -sd' ' -)
LISTA_PRO=$(echo "$MODULOS" | grep '^:pro:' | paste -sd' ' -)

# --- workers cloudflare ------------------------------------------------------
WORKERS=""
N_WORKERS=0
for wt in integrations/cloudflare/*/wrangler.toml; do
  [[ -e "$wt" ]] || continue
  dir=$(basename "$(dirname "$wt")")
  nome=$(grep -E '^name *= *"' "$wt" | head -1 | sed 's/.*= *"//; s/".*//')
  WORKERS+="| \`$dir\` | \`$nome\` |"$'\n'
  N_WORKERS=$((N_WORKERS + 1))
done

# --- tabelas D1 --------------------------------------------------------------
# Uniao de schema.sql + migrations/, por worker. CREATE TABLE [IF NOT EXISTS].
tabelas_de() {
  # O nome da tabela e sempre o ultimo campo do match, com ou sem IF NOT EXISTS.
  find "integrations/cloudflare/$1" -name '*.sql' 2>/dev/null \
    | xargs -r grep -hoiE 'CREATE TABLE (IF NOT EXISTS )?[a-z_][a-z0-9_]*' 2>/dev/null \
    | awk '{print tolower($NF)}' | sort -u | grep -v '^$' || true
}
TAB_ADMIN=$(tabelas_de signallq-admin-worker)
TAB_DIAG=$(tabelas_de signallq-diagnostic-worker)
N_TAB_ADMIN=$(echo "$TAB_ADMIN" | grep -c . || true)
N_TAB_DIAG=$(echo "$TAB_DIAG" | grep -c . || true)

# --- contratos openapi -------------------------------------------------------
CONTRATOS=""
N_PATHS_TOTAL=0
N_CONTRATOS=0
for y in docs_ai/CONTRATOS/openapi/*.yaml; do
  [[ -e "$y" ]] || continue
  # paths = linhas com exatamente 2 espacos de indentacao comecando em /
  n=$(sed -n '/^paths:/,/^[a-z]/p' "$y" | grep -cE '^  /' || true)
  ver=$(grep -E '^  version:' "$y" | head -1 | sed 's/.*version: *//; s/["'"'"']//g')
  CONTRATOS+="| \`$(basename "$y")\` | $ver | $n |"$'\n'
  N_PATHS_TOTAL=$((N_PATHS_TOTAL + n))
  N_CONTRATOS=$((N_CONTRATOS + 1))
done

# --- eventos de analytics ----------------------------------------------------
N_EVENTOS=$(grep -rhoE '"[a-z][a-z0-9_]{3,}"' \
  android/app/src/main/kotlin/io/veloo/app/kotlin/analytics/ \
  android/app/src/main/kotlin/io/signallq/app/analytics/ \
  android/core/recommendation/src/main/kotlin/io/signallq/app/core/recommendation/analytics/ \
  2>/dev/null | tr -d '"' | sort -u | grep -cE '^(feature|screen|app|battery|speedtest|diag|ia|recommendation|analytics)_' || true)

# --- arquivos em caminho fisico legado ---------------------------------------
# Todos os source sets (main + test + androidTest). build/ e podado por seguranca,
# embora hoje nao contribua. A regra de higiene §4.1 cita ~460, validado em
# 2026-07-15 — a diferenca e crescimento do codigo, nao erro de contagem.
N_VELOO=$(find android -path '*/build/*' -prune -o -path '*/io/veloo/*' -name '*.kt' -print 2>/dev/null \
  | wc -l | tr -d ' ')
N_VELOO_MAIN=$(find android -path '*/build/*' -prune -o -path '*/src/main/*' -path '*/io/veloo/*' \
  -name '*.kt' -print 2>/dev/null | wc -l | tr -d ' ')

# --- monta o bloco -----------------------------------------------------------
bloco() {
  cat <<BLOCO
<!-- INVENTARIO:INICIO — gerado por scripts/gerar-inventario-docs.sh, nao editar a mao -->

> **Inventário gerado do código.** Não editar manualmente — rode
> \`scripts/gerar-inventario-docs.sh\`. Cada número abaixo sai da fonte citada.

| Fato | Valor | Fonte |
|---|---|---|
| versionName / versionCode (consumer) | **$VERSION_NAME** / **$VERSION_CODE** | \`$TOML\` |
| proVersionName / proVersionCode | $PRO_VERSION_NAME / $PRO_VERSION_CODE | \`$TOML\` |
| compileSdk / minSdk / targetSdk | $COMPILE_SDK / $MIN_SDK / $TARGET_SDK | \`$TOML\` |
| Compose BOM · Room · Hilt | $COMPOSE_BOM · $ROOM · $HILT | \`$TOML\` |
| Módulos Gradle | **$MOD_TOTAL** — $MOD_CONSUMER consumer + $MOD_PRO Pro | \`$SETTINGS\` |
| Workers Cloudflare | $N_WORKERS | \`integrations/cloudflare/*/wrangler.toml\` |
| Tabelas D1 | $((N_TAB_ADMIN + N_TAB_DIAG)) — $N_TAB_ADMIN admin + $N_TAB_DIAG diagnostic | \`*/migrations/*.sql\`, \`*/schema.sql\` |
| Contratos OpenAPI | $N_CONTRATOS contratos · **$N_PATHS_TOTAL** endpoints | \`docs_ai/CONTRATOS/openapi/\` |
| Arquivos \`.kt\` em caminho legado \`io/veloo\` | $N_VELOO (sendo $N_VELOO_MAIN em \`src/main\`) | dívida conhecida — higiene §4.1 |

**Módulos consumer ($MOD_CONSUMER):** $LISTA_CONSUMER

**Módulos Pro ($MOD_PRO, on hold):** $LISTA_PRO

**Workers:**

| Diretório | \`name\` no wrangler |
|---|---|
$WORKERS
**Contratos:**

| Arquivo | Versão | Endpoints |
|---|---|---:|
$CONTRATOS
<!-- INVENTARIO:FIM -->
BLOCO
}

# --- injeta ------------------------------------------------------------------
status=0
NOVO=$(bloco)

for alvo in "${ALVOS[@]}"; do
  if [[ ! -f "$alvo" ]]; then
    echo "alvo inexistente: $alvo" >&2
    status=1
    continue
  fi
  if ! grep -q 'INVENTARIO:INICIO' "$alvo"; then
    echo "sem marcadores INVENTARIO em: $alvo" >&2
    status=1
    continue
  fi

  tmp=$(mktemp)
  awk -v novo="$NOVO" '
    /INVENTARIO:INICIO/ { print novo; dentro=1; next }
    /INVENTARIO:FIM/    { dentro=0; next }
    !dentro             { print }
  ' "$alvo" > "$tmp"

  if [[ "$CHECK_ONLY" == true ]]; then
    if ! diff -q "$alvo" "$tmp" >/dev/null; then
      echo "desatualizado: $alvo"
      diff "$alvo" "$tmp" | head -20
      status=1
    fi
    rm -f "$tmp"
  else
    mv "$tmp" "$alvo"
    echo "atualizado: $alvo"
  fi
done

exit $status
