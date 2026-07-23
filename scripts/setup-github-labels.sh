#!/usr/bin/env bash
# Cria labels do squad LINKA no repo configurado.
# Uso: GH=/c/Program\ Files/GitHub\ CLI/gh.exe REPO=7ALabs/linka-android ./scripts/setup-github-labels.sh
set -e
GH="${GH:-gh}"
REPO="${REPO:-7ALabs/linka-android}"

label() {
  local name="$1" color="$2" desc="$3"
  "$GH" label create "$name" --repo "$REPO" --color "$color" --description "$desc" --force >/dev/null && echo "✓ $name"
}

# Agentes (donos da task)
label "agent:claudete"  "1f6feb" "Diretora de Produto & Delivery"
label "agent:camilo"    "d93f0b" "Especialista Android (implementação)"
label "agent:lia"       "f29ddc" "UX/UI Material 3"
label "agent:gema"      "0e8a16" "QA & Release"
label "agent:rhodolfo"  "0e8a16" "Qualidade & Confiabilidade (QA, release, higiene, docs)"
label "agent:juninho"   "c5def5" "Analista Júnior de Operações & Triagem (Estagiário)"

# Consultivos (não donos, mas precisam aprovar)
label "needs:bernardo"  "006b75" "Validação de telecom (WiFi/Fibra/4G/5G)"
label "needs:otavio"    "006b75" "Validação Android device/OS/OEM"

# Áreas
label "area:android"         "c2e0c6" "Código Android nativo"
label "area:diagnostic"      "c2e0c6" "Engine de diagnóstico"
label "area:wifi"            "c2e0c6" "Wi-Fi"
label "area:fibra"           "c2e0c6" "Fibra FTTH"
label "area:mobile-4g5g"     "c2e0c6" "Redes móveis 4G/5G"
label "area:ui"              "c2e0c6" "UI / Compose"
label "area:docs"            "c2e0c6" "Documentação"
label "area:release"         "c2e0c6" "Build / release / CI"
label "area:security"        "c2e0c6" "Segurança / chaves / config"

# Tipo
label "type:feature"   "a2eeef" "Feature nova"
label "type:bug"       "d73a4a" "Bug"
label "type:refactor"  "fbca04" "Refactor"
label "type:chore"     "cfd3d7" "Chore"
label "type:spike"     "d4c5f9" "Spike / investigação"

# Prioridade
label "priority:p0"  "b60205" "Urgente — fazer agora"
label "priority:p1"  "d93f0b" "Importante"
label "priority:p2"  "fbca04" "Backlog"

# Status (workflow no Project)
label "status:agent-ready"     "0075ca" "Contexto suficiente para o agente atuar"
label "status:blocked"         "b60205" "Bloqueada"
label "status:waiting-review"  "0075ca" "Aguardando review da Gema"

echo ""
echo "Labels criadas em $REPO"
