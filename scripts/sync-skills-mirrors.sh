#!/usr/bin/env bash
set -euo pipefail

# Fonte canonica de skills e .claude/skills/. Este script resincroniza os espelhos
# usados por outras ferramentas (.agents/skills para formato agnostico, .github/skills
# para Copilot) sempre que a skill original for editada. Nunca editar os espelhos
# diretamente -- rode este script depois de editar algo em .claude/skills/.
#
# Uso: scripts/sync-skills-mirrors.sh [--check]
#   --check  nao escreve nada, so falha (exit 1) se os espelhos estiverem desatualizados

cd "$(dirname "$0")/.."

CANONICAL=".claude/skills"
MIRRORS=(".agents/skills" ".github/skills")
CHECK_ONLY=false

if [[ "${1:-}" == "--check" ]]; then
  CHECK_ONLY=true
fi

status=0

for mirror in "${MIRRORS[@]}"; do
  if [[ "$CHECK_ONLY" == true ]]; then
    # --exclude ignora extras conhecidos do espelho (arquivos proprios da ferramenta,
    # nao skills de verdade) para nao dar falso positivo.
    diff_out=$(diff -rq --exclude=agents "$CANONICAL" "$mirror" 2>&1 || true)
    if [[ -n "$diff_out" ]]; then
      echo "desatualizado: $mirror diverge de $CANONICAL"
      echo "$diff_out"
      status=1
    fi
  else
    # cp -R sem remover antes: alguns espelhos tem arquivos proprios da ferramenta
    # (ex.: .agents/skills/impeccable/agents/*.toml) que nao existem na fonte canonica.
    # Remocao de skill inteira ainda exige apagar o espelho manualmente.
    mkdir -p "$mirror"
    cp -R "$CANONICAL/." "$mirror/"
    echo "sincronizado: $mirror"
  fi
done

exit $status
