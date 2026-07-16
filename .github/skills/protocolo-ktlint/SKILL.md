---
description: Protocolo Ktlint — regras para suppressão no .editorconfig e cleanup de violações. Evita acúmulo de dívida técnica por supressões em massa.
---

## Quando usar

Invocar **antes** de suprimir qualquer regra Ktlint no `.editorconfig`, ou ao deparar com violações que bloqueiam CI.

---

## Regra de ouro

> **Nunca suprimir mais de 3 regras Ktlint em um único PR sem protocolo de cleanup documentado.**

---

## Fluxo de decisão ao deparar com violações Ktlint

### 1. Verificar se é auto-fixável

```powershell
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"
./gradlew ktlintFormat
```

- **Exit 0 + BUILD SUCCESSFUL** → violação é auto-fixável. Aplicar `ktlintFormat` no PR atual. Não suprimir.
- **Violações restantes após format** → são manuais. Seguir passo 2.

### 2. Contar violações manuais

```powershell
./gradlew ktlintCheck --continue 2>&1 | Select-String "^\s+\d+:\d+"
```

| Volume | Ação |
|--------|------|
| 1–3 regras, poucas ocorrências | Corrigir diretamente no PR atual |
| 4+ regras OU muitas ocorrências | Protocolo 2 PRs (ver abaixo) |

---

## Protocolo 2 PRs (cleanup em escala)

### PR1 — Mecânico (sem toque em lógica)

1. Reativar regras suprimidas no `.editorconfig` (remover linhas `ktlint_standard_* = disabled`)
2. Executar `./gradlew ktlintFormat` — commit todos os arquivos reformatados
3. CI deve passar com Ktlint ✅ e Detekt ✅
4. Mensagem de commit: `chore(ktlint): reativar regras + auto-format (#ISSUE)`

### PR2 — Manual (mudanças semânticas mínimas)

Apenas violações que `ktlintFormat` não resolve:
- `property-naming` → renomear `const val camelCase` para `SCREAMING_SNAKE`
- `enum-entry-name-case` → renomear entries para `UpperCamelCase`
- `max-line-length` → quebrar strings longas (> 160 chars)
- `no-empty-file` → deletar arquivos placeholder vazios

Atualizar **todos os pontos de uso** após renomeação (grepar antes de commitar).

---

## Supressão emergencial (feature PR bloqueado)

Se Ktlint bloqueia um PR de feature/refactor e não há tempo para cleanup completo:

```ini
# .editorconfig
[app/src/main/kotlin/io/linka/...]
# Tracked in Issue #XXX — cleanup pendente
ktlint_standard_<regra> = disabled
```

**Obrigatório antes do merge:**
1. Abrir issue de cleanup com label `tech-debt` e `P2`
2. Adicionar comentário no `.editorconfig` linkando a issue
3. Issue entra no backlog imediatamente (não "depois")

---

## O que NUNCA fazer

- Suprimir regras em massa (`ktlint_standard_* = disabled` para 10+ regras) sem rastreamento
- Deixar supressão sem comentário explicativo
- Fechar PR com supressões sem issue de cleanup aberta
- Usar `@Suppress("ktlint:...")` inline como workaround permanente

---

## Comandos úteis

```powershell
# Setup JAVA_HOME (necessário no PowerShell local)
$env:JAVA_HOME = 'C:\Program Files\Android\Android Studio\jbr'
$env:PATH = "$env:JAVA_HOME\bin;$env:PATH"

# Auto-fix
./gradlew ktlintFormat

# Listar todas as violações (não para no primeiro erro)
./gradlew ktlintCheck --continue

# Contar violações por regra
./gradlew ktlintCheck --continue 2>&1 | Select-String "Lint error" | Measure-Object

# Verificar arquivos com violações específicas
./gradlew ktlintCheck --continue 2>&1 | Select-String "property-naming"
```

---

## Regras com auto-fix vs. manuais (Ktlint 1.3.1)

| Regra | Auto-fix? | Notas |
|-------|-----------|-------|
| `indent` | ✅ | Dependente de `string-template-indent` |
| `string-template-indent` | ✅ | Ativar junto com `indent` |
| `trailing-comma-on-call-site` | ✅ | |
| `trailing-comma-on-declaration-site` | ✅ | |
| `max-line-length` | ❌ | Quebrar manualmente |
| `property-naming` | ❌ | Renomear + atualizar todos os usos |
| `enum-entry-name-case` | ❌ | Renomear + atualizar todos os usos |
| `no-empty-file` | ❌ | Deletar arquivo vazio |
| `multiline-expression-wrapping` | ✅ (parcial) | Pode precisar de ajuste manual |
