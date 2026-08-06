#!/usr/bin/env bash
set -uo pipefail

# Guardrail da documentacao. Existe porque regra sem enforcement foi exatamente
# como docs_ai/ chegou a 235 arquivos com inventario defasado em varios deles.
#
# Uso:
#   scripts/validar-docs.sh                 valida so os .md alterados vs. origin/main
#   scripts/validar-docs.sh --base <ref>    idem, contra outra ref
#   scripts/validar-docs.sh --todos         valida a arvore inteira (relatorio)
#   scripts/validar-docs.sh --relatorio     so imprime cobertura, nunca falha
#
# Por padrao valida apenas o que a PR toca: exigir frontmatter de toda a arvore
# reprovaria 89 dos 119 documentos hoje. A divida antiga fica visivel em
# --relatorio e encolhe conforme os arquivos sao tocados; divida NOVA e barrada.

cd "$(dirname "$0")/.."

BASE="origin/main"
MODO="alterados"

while [[ $# -gt 0 ]]; do
  case "$1" in
    --base)      BASE="$2"; shift 2 ;;
    --todos)     MODO="todos"; shift ;;
    --relatorio) MODO="relatorio"; shift ;;
    *) echo "argumento desconhecido: $1" >&2; exit 2 ;;
  esac
done

# Diretorios onde .md e permitido. Qualquer outro lugar exige decisao explicita.
ARVORE_PERMITIDA='^(docs_ai/|\.claude/|\.github/|\.agents/|android/|integrations/|packages/|scripts/|brand/|docs/|_archive/|[A-Z_]+\.md$)'

# pro-onhold/ esta congelado: nao se mantem documentacao de produto parado.
ISENTOS='^docs_ai/pro-onhold/'

CAMPOS_OBRIGATORIOS=(title description type status owner last_updated)

falhas=0
avisos=0

erro()  { echo "  ✗ $1"; falhas=$((falhas + 1)); }
aviso() { echo "  ⚠ $1"; avisos=$((avisos + 1)); }

# ---------------------------------------------------------------------------
# 1. Inventario gerado precisa estar em dia com o codigo
# ---------------------------------------------------------------------------
echo "→ inventário gerado vs. código"
if bash scripts/gerar-inventario-docs.sh --check >/tmp/inv.out 2>&1; then
  echo "  ✓ em dia"
else
  erro "bloco de inventário desatualizado — rode: scripts/gerar-inventario-docs.sh"
  sed 's/^/    /' /tmp/inv.out | head -12
fi

# ---------------------------------------------------------------------------
# 2. Espelhos de skill
# ---------------------------------------------------------------------------
echo "→ espelhos de skill"
if bash scripts/sync-skills-mirrors.sh --check >/tmp/mir.out 2>&1; then
  echo "  ✓ sincronizados"
else
  erro "espelhos divergem — rode: scripts/sync-skills-mirrors.sh"
  sed 's/^/    /' /tmp/mir.out | head -8
fi

# ---------------------------------------------------------------------------
# 3. Quais arquivos validar
# ---------------------------------------------------------------------------
if [[ "$MODO" == "alterados" ]]; then
  if ! git rev-parse --verify "$BASE" >/dev/null 2>&1; then
    echo "→ base '$BASE' inacessível; caindo para --todos"
    MODO="todos"
  fi
fi

case "$MODO" in
  alterados)
    ALVOS=$(git diff --name-only --diff-filter=d "$BASE"...HEAD -- '*.md' 2>/dev/null || true)
    echo "→ validando .md alterados vs. $BASE"
    ;;
  *)
    ALVOS=$(find docs_ai -name '*.md' | sort)
    echo "→ validando árvore inteira"
    ;;
esac

# O frontmatter obrigatorio e regra de docs_ai/. Fora dali cada arvore tem formato
# proprio — SKILL.md usa name/description exigidos pelo carregador de skills, e as
# regras em .claude/rules/ nao sao documentacao de produto. Impor o mesmo cabecalho
# ali quebraria ferramenta em troca de nada.
ALVOS=$(echo "$ALVOS" | grep -E '^docs_ai/' | grep -vE "$ISENTOS" || true)

# ---------------------------------------------------------------------------
# 4. Frontmatter obrigatorio
# ---------------------------------------------------------------------------
sem_fm=0
com_fm=0
if [[ -n "${ALVOS//[[:space:]]/}" ]]; then
  while read -r f; do
    [[ -z "$f" || ! -f "$f" ]] && continue

    if ! head -1 "$f" | grep -q '^---$'; then
      sem_fm=$((sem_fm + 1))
      [[ "$MODO" != "relatorio" ]] && erro "$f — sem frontmatter YAML"
      continue
    fi

    faltando=""
    for campo in "${CAMPOS_OBRIGATORIOS[@]}"; do
      sed -n '2,/^---$/p' "$f" | grep -qE "^$campo:" || faltando+="$campo "
    done

    if [[ -n "$faltando" ]]; then
      sem_fm=$((sem_fm + 1))
      [[ "$MODO" != "relatorio" ]] && erro "$f — frontmatter sem: $faltando"
    else
      com_fm=$((com_fm + 1))
    fi

    # status precisa ser um valor conhecido.
    # templates/ e isento: o campo la lista as opcoes possiveis, nao um valor.
    # decisions/ tem vocabulario proprio — decisao registrada nao e "ativa",
    # e "vigente" ou "registrado (historico)"; ADR usa aceito/proposto/rejeitado.
    if ! echo "$f" | grep -qE '^docs_ai/(templates|decisions)/'; then
      st=$(sed -n '2,/^---$/p' "$f" | grep -E '^status:' | head -1 | sed 's/status: *//; s/"//g' || true)
      if [[ -n "$st" ]] && ! echo "$st" | grep -qE '^(ativo|draft|congelado|deprecated)$'; then
        aviso "$f — status desconhecido: '$st' (esperado: ativo|draft|congelado|deprecated)"
      fi
    fi
  done <<< "$ALVOS"
fi

# ---------------------------------------------------------------------------
# 5. .md fora da arvore permitida
# ---------------------------------------------------------------------------
echo "→ localização dos .md"
fora=$(git ls-files '*.md' | grep -vE "$ARVORE_PERMITIDA" || true)
if [[ -n "$fora" ]]; then
  while read -r f; do erro "$f — .md fora da árvore permitida"; done <<< "$fora"
else
  echo "  ✓ nenhum fora do lugar"
fi

# ---------------------------------------------------------------------------
# 6a. Contagem declarada no INDICE bate com o disco
# ---------------------------------------------------------------------------
# O indice cita pastas como "`operations/` (26)". Se alguem adiciona um documento
# e nao atualiza o indice, a contagem diverge — e isso e o que se quer pegar.
# Exigir link nominal para cada um dos 22 documentos de decisions/ seria
# burocracia sem retorno.
echo "→ contagens declaradas no INDICE.md"
divergencias=0
while read -r linha; do
  dir=$(echo "$linha" | grep -oE '`[a-zA-Z/_-]+/`' | head -1 | tr -d '`')
  num=$(echo "$linha" | grep -oE '\(([0-9]+)\)' | head -1 | tr -d '()')
  [[ -z "$dir" || -z "$num" ]] && continue
  [[ ! -d "docs_ai/$dir" ]] && continue
  real=$(find "docs_ai/$dir" -maxdepth 1 -name '*.md' | wc -l | tr -d ' ')
  if [[ "$real" != "$num" ]]; then
    erro "INDICE.md declara $dir com $num documentos; o disco tem $real"
    divergencias=$((divergencias + 1))
  fi
done < <(grep -E '^#{2,3} .*`[a-zA-Z/_-]+/`.*\([0-9]+\)' docs_ai/INDICE.md || true)
[[ $divergencias -eq 0 ]] && echo "  ✓ contagens conferem"

# ---------------------------------------------------------------------------
# 6b. Pasta nova nao mencionada em lugar nenhum
# ---------------------------------------------------------------------------
echo "→ pastas de docs_ai/ citadas no índice"
naocitadas=0
for d in docs_ai/*/; do
  nome=$(basename "$d")
  if ! grep -qE "$nome/" docs_ai/INDICE.md docs_ai/README.md 2>/dev/null; then
    erro "docs_ai/$nome/ existe mas não é citada em INDICE.md nem README.md"
    naocitadas=$((naocitadas + 1))
  fi
done
[[ $naocitadas -eq 0 ]] && echo "  ✓ todas citadas"

# Documento solto na raiz de docs_ai/ precisa de citacao nominal.
echo "→ documentos na raiz de docs_ai/"
soltos=0
for f in docs_ai/*.md; do
  nome=$(basename "$f")
  [[ "$nome" == "INDICE.md" || "$nome" == "README.md" ]] && continue
  if ! grep -qF "$nome" docs_ai/INDICE.md docs_ai/README.md 2>/dev/null; then
    erro "$f — documento na raiz sem citação em INDICE.md nem README.md"
    soltos=$((soltos + 1))
  fi
done
[[ $soltos -eq 0 ]] && echo "  ✓ todos citados"

# ---------------------------------------------------------------------------
# 7. Resumo
# ---------------------------------------------------------------------------
echo
if [[ "$MODO" != "alterados" ]]; then
  tot=$(find docs_ai -name '*.md' | wc -l | tr -d ' ')
  echo "cobertura de frontmatter: $com_fm de $((com_fm + sem_fm)) verificados (árvore: $tot documentos)"
fi
echo "falhas: $falhas · avisos: $avisos"

if [[ "$MODO" == "relatorio" ]]; then
  echo "(modo relatório — não falha)"
  exit 0
fi

[[ $falhas -gt 0 ]] && exit 1
exit 0
