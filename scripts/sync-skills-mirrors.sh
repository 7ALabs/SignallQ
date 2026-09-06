#!/usr/bin/env bash
set -euo pipefail

# Fonte canônica: .agents/skills/. O script sincroniza os espelhos de compatibilidade
# usados pelo Claude Code (.claude/skills) e pelo GitHub/Copilot (.github/skills).
# Nunca crie regra exclusiva em um espelho: edite .agents/skills e sincronize.
#
# Uso: scripts/sync-skills-mirrors.sh [--check]
#   --check  não escreve; falha se um espelho divergir da fonte canônica.

cd "$(dirname "$0")/.."

CANONICAL=".agents/skills"
MIRRORS=(".claude/skills" ".github/skills")
CHECK_ONLY=false

if [[ "${1:-}" == "--check" ]]; then
  CHECK_ONLY=true
fi

status=0

for mirror in "${MIRRORS[@]}"; do
  if [[ "$CHECK_ONLY" == true ]]; then
    # README.md pode ser específico do diretório. Alguns skills de tooling mantêm
    # arquivos auxiliares próprios de integração; compare o conteúdo compartilhado
    # sem transformar metadado de ferramenta em segunda governança.
    diff_out=$(diff -rq --exclude=README.md "$CANONICAL" "$mirror" 2>&1 || true)
    if [[ -n "$diff_out" ]]; then
      echo "desatualizado: $mirror diverge de $CANONICAL"
      echo "$diff_out"
      status=1
    fi
  else
    mkdir -p "$mirror"
    cp -R "$CANONICAL/." "$mirror/"
    echo "sincronizado: $mirror"
  fi
done

exit $status
