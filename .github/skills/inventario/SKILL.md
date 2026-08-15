---
name: inventario
description: Lista o que já existe no SignallQ antes de criar código novo. Retorna módulos Gradle, componentes reutilizáveis, utilitários compartilhados e Workers, com path e propósito. Obrigatória para Camilo antes de qualquer criação de módulo, serviço, componente ou função utilitária. Governança anti-duplicação — épico #1623 Fase 2.
argument-hint: "[--modulos | --componentes | --utilitarios | --workers | --tudo (default)]  [--grep <termo>]"
allowed-tools: Bash(grep *), Bash(find *), Bash(cat *), Bash(sed *), Bash(head *), Bash(tail *), Bash(ls *), Bash(awk *)
---

**Dono:** Camilo. **Modelo sugerido:** Haiku — leitura mecânica de disco, sem julgamento.

## Quando usar

**Antes** de:

- Criar módulo Gradle novo.
- Criar `Repository`, `UseCase`, `Service`, `Manager`, `Provider`, `Factory`, `Mapper`, `Parser`.
- Criar componente Composable reutilizável.
- Criar função utilitária (helper, extension, formatter).
- Adicionar Worker Cloudflare.

Se o inventário retornar algo parecido, **ou reusa, ou justifica** por que não reusa. Não criar duplicata silenciosa. Ver [regra de higiene §4.9 e §6](../../rules/higiene-e-padronizacao-repositorio.md).

## Como funciona

A skill lê o **estado real** do disco (não confia em doc que pode estar desatualizada):

- Módulos: `android/settings.gradle.kts` (bloco `include(...)`)
- Componentes UI: `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/*.kt`
- Utilitários compartilhados: `android/core/*/src/main/kotlin/**/*.kt` (grep por classes públicas)
- Workers: `integrations/cloudflare/*/wrangler.toml`

## Comandos

### `--modulos` (ou default `--tudo`)

```bash
# Módulos Gradle vivos (sem :pro:* — descontinuados por ADR-016)
sed -n '/^include(/,/^)/p' android/settings.gradle.kts \
  | sed 's|//.*||' \
  | grep -oE '":[a-zA-Z0-9:_-]+"' \
  | tr -d '"' \
  | sort -u \
  | grep -v '^:pro:'
```

Para cada módulo, tenta ler `docs_ai/ARQUITETURA/MODULOS/<modulo>.md` e citar 1-2 linhas de descrição.

### `--componentes`

```bash
ls android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/*.kt 2>/dev/null \
  | xargs -I{} basename {} .kt \
  | sort
```

### `--utilitarios`

```bash
# Grep classes e objects públicos em :core:* (helpers, extensions, factories)
grep -rhoE '^(public\s+)?(class|object|fun)\s+[A-Z][A-Za-z0-9]+' \
  android/core/*/src/main/kotlin/ 2>/dev/null \
  | awk '{print $NF}' \
  | sort -u
```

### `--workers`

```bash
for wt in integrations/cloudflare/*/wrangler.toml; do
  dir=$(basename "$(dirname "$wt")")
  nome=$(grep -E '^name *= *"' "$wt" | head -1 | sed 's/.*= *"//; s/".*//')
  echo "$dir → $nome"
done
```

### `--grep <termo>`

Filtra qualquer uma das listagens acima pelo termo (case-insensitive). Útil para "existe algo com 'wifi' no core?" ou "tem algum helper de 'format' na base?".

```bash
# Exemplo — --componentes --grep gauge
ls android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/*.kt 2>/dev/null \
  | xargs -I{} basename {} .kt \
  | grep -i "gauge" \
  | sort
```

## Saída padrão

```
=== Módulos Gradle vivos (14) ===
:app                    → executável, composição de tudo
:coreNetwork            → conectividade, gateway, DNS, callbacks
:corePermissions        → permissões Android runtime
:coreDatabase           → Room + migrations
:coreDatastore          → DataStore preferences
:coreTelephony          → Telephony API
:coreRecommendation     → recomendações
:core:featureflags      → feature flags
:core:relatorio         → geração de relatórios
:core:diagnostico       → motor canônico de diagnóstico
:featureHome            → tela principal
:featureWifi            → Wi-Fi, sinal, canais
:featureDevices         → scanner de dispositivos
:featureDns             → benchmark DoH
:featureSpeedtest       → speedtest Cloudflare
:featureDiagnostico     → interpretação local
:featureFibra           → saúde fibra/WAN
:featureHistory         → histórico
:featureSettings        → preferências

=== Componentes reutilizáveis (ui/component/) ===
GaugeCircular
Card...
[etc]

=== Workers Cloudflare (5) ===
ai-diagnosis-worker           → signallq-ai-diagnosis-worker
game-latency-probe-worker     → signallq-game-latency-probe-worker
signallq-admin-worker         → signallq-admin
signallq-diagnostic-worker    → signallq-diagnostic
signallq-privacy-worker       → signallq-privacy
```

## O que a skill NÃO faz

- Não decide se você deve criar ou reusar — apenas mostra o que existe. A decisão é do Camilo (com input do Caio em review).
- Não valida se um componente existente **atende** ao caso de uso — só lista.
- Não gera código.

## Interação com o fluxo

- Camilo invoca `/inventario` **antes** de escrever código novo.
- `/verificar-modulo <nome>` é o passo seguinte quando há intenção de criar módulo/serviço específico.
- `/check-done` (piloto #1620) verifica no gate se a PR nova declara ter consultado `/inventario` — via comentário na PR ou anotação no corpo.

## Referências

- [Regra de higiene §4.9](../../rules/higiene-e-padronizacao-repositorio.md) — features não dependem de features
- [Regra de higiene §5-6](../../rules/higiene-e-padronizacao-repositorio.md) — convenções de módulos e nomes
- Skill peer: [`/verificar-modulo`](../verificar-modulo/SKILL.md)
- Persona: [Camilo](../../agents/camilo.md)
