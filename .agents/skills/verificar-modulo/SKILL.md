---
name: verificar-modulo
description: Verifica se um módulo/serviço/componente proposto já existe (ou tem equivalente próximo) antes de criar. Retorna PASS (pode criar) ou lista os equivalentes existentes que devem ser considerados para reuso. Camilo invoca antes de gerar código novo. Governança anti-duplicação — épico #1623 Fase 2.
argument-hint: "<tipo> <nome>  (tipo = modulo | servico | componente | utilitario)  Ex: /verificar-modulo componente GaugeVelocidade"
allowed-tools: Bash(grep *), Bash(find *), Bash(sed *), Bash(head *), Bash(ls *), Bash(awk *), Bash(tr *)
---

## Quando usar

Após `/inventario` mostrar o cenário geral, **antes de escrever a primeira linha** de:

- Módulo Gradle novo (`:core:*`, `:feature:*`)
- Serviço/Repository/UseCase novo
- Componente Composable novo
- Utilitário/Helper/Extension novo

## Fluxo

1. **Parse do input:** tipo + nome proposto.
2. **Match direto:** existe algo com esse nome exato?
3. **Match aproximado:** grep por variações (root do nome, sinônimos comuns).
4. **Decisão:**
   - Se **nada parecido existe** → `PASS`, pode criar.
   - Se **algo idêntico existe** → `FAIL`, reuse o existente.
   - Se **algo parecido existe** → `WARN`, pede justificativa (por que não reusar? extensão? contexto diferente?).

## Checks por tipo

### `modulo`

```bash
NOME="$1"  # ex: featureNovo, core:analytics
EXISTE=$(sed -n '/^include(/,/^)/p' android/settings.gradle.kts \
  | grep -oE '":[a-zA-Z0-9:_-]+"' | tr -d '"' \
  | grep -iE "$NOME|${NOME//:/}")

if [ -n "$EXISTE" ]; then
  echo "FAIL — módulo já existe: $EXISTE"
  exit 1
fi

# Match aproximado: contém a palavra-chave principal
KEYWORD=$(echo "$NOME" | sed 's/^feature//; s/^core:*//; s/[A-Z]/ &/g' | awk '{print tolower($NF)}')
SIMILAR=$(sed -n '/^include(/,/^)/p' android/settings.gradle.kts \
  | grep -oE '":[a-zA-Z0-9:_-]+"' | tr -d '"' \
  | grep -i "$KEYWORD")

if [ -n "$SIMILAR" ]; then
  echo "WARN — módulos com nome próximo:"
  echo "$SIMILAR"
  echo "Justifique por que não estender um desses antes de criar novo."
fi
```

### `componente`

```bash
NOME="$1"  # ex: GaugeVelocidade
EXATO=$(find android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/ \
  -name "${NOME}.kt" 2>/dev/null)

if [ -n "$EXATO" ]; then
  echo "FAIL — componente já existe: $EXATO"
  exit 1
fi

# Match aproximado: raiz do nome (ex: "Gauge" em "GaugeVelocidade")
ROOT=$(echo "$NOME" | grep -oE '^[A-Z][a-z]+')
SIMILAR=$(find android/app/src/main/kotlin/io/veloo/app/kotlin/ui/component/ \
  -name "${ROOT}*.kt" 2>/dev/null)

if [ -n "$SIMILAR" ]; then
  echo "WARN — componentes com raiz '$ROOT':"
  echo "$SIMILAR"
  echo "Considere estender/parametrizar um desses."
fi
```

### `servico`

```bash
NOME="$1"  # ex: WifiScanner
EXATO=$(grep -rln "^\s*\(class\|object\|interface\)\s\+${NOME}\b" \
  android/core/ android/app/src/main/kotlin/ 2>/dev/null)

if [ -n "$EXATO" ]; then
  echo "FAIL — símbolo já existe:"
  echo "$EXATO"
  exit 1
fi

# Match aproximado por sufixo
SUFIXO=$(echo "$NOME" | grep -oE '(Repository|Service|Manager|Scanner|Client|Provider|Factory|Mapper|Parser)$')
if [ -n "$SUFIXO" ]; then
  RAIZ=$(echo "$NOME" | sed "s/${SUFIXO}$//")
  SIMILAR=$(grep -rln "^\s*\(class\|object\|interface\)\s\+${RAIZ}" \
    android/core/ android/app/src/main/kotlin/ 2>/dev/null)
  if [ -n "$SIMILAR" ]; then
    echo "WARN — símbolos com raiz '$RAIZ':"
    echo "$SIMILAR"
  fi
fi
```

### `utilitario`

```bash
NOME="$1"  # ex: FormatadorVelocidade
EXATO=$(grep -rln "^\s*\(fun\|object\|class\)\s\+${NOME}\b" \
  android/core/ android/app/src/main/kotlin/ 2>/dev/null | head -5)

if [ -n "$EXATO" ]; then
  echo "FAIL — utilitário já existe:"
  echo "$EXATO"
  exit 1
fi
```

## Saída padrão

**PASS (pode criar):**
```
✓ PASS — nada existente com o nome ou raiz próxima.
  Pode criar: modulo :feature:novo
  Sugestão de path: android/feature/novo/
```

**WARN (existe similar):**
```
⚠ WARN — existem candidatos a reuso:
  - android/app/.../ui/component/GaugeCircular.kt
  - android/app/.../ui/component/GaugeBarra.kt

Antes de criar GaugeVelocidade:
  1. Um desses cobre >70% do caso? Se sim, estenda/parametrize.
  2. Se não, justifique no corpo da PR: "novo componente porque X exige Y, incompatível com Gauge* existente por causa de Z".
  3. Registre a decisão para Caio validar no gate.
```

**FAIL (já existe):**
```
✗ FAIL — o alvo já existe:
  android/app/.../ui/component/GaugeCircular.kt

Ação: use o existente. Se precisa de comportamento diferente, extenda via parâmetro/composição, não crie duplicata.
```

## O que a skill NÃO faz

- Não cria o módulo/arquivo/símbolo — apenas verifica.
- Não decide entre "estender vs criar novo" — reporta candidatos, Camilo decide, Caio valida.
- Não bloqueia hard — retorna WARN em casos ambíguos; a responsabilidade final é do agente + revisor.

## Interação com o fluxo

- Camilo invoca `/verificar-modulo <tipo> <nome>` antes de criar. Registra o output no comentário da PR ou issue.
- `/check-done` verifica no gate se a skill foi consultada quando a PR cria símbolo novo. Se não foi, Caio pede antes de aprovar.

## Referências

- Skill peer: [`/inventario`](../inventario/SKILL.md)
- [Regra de higiene §4.9](../../rules/higiene-e-padronizacao-repositorio.md) — features não dependem de features
- [Regra de higiene §6](../../rules/higiene-e-padronizacao-repositorio.md) — nomes proibidos (Utils, Helper, Manager genérico)
- Persona: [Camilo](../../agents/camilo.md)
