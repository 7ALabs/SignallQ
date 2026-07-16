---
name: validar-release
description: Checklist executável de release — Android com validação de versionamento, changelog e build.
---

## Quando usar

Antes de gerar APK/AAB de release ou submeter à loja.

Esta skill **automatiza** a verificação de critérios críticos que impedem release. Diferente de `/checar-release` (orientação manual), aqui os comandos rodam de forma programável.

---

## Android — Checklist Executável

### 1. Versionamento — Validação obrigatória

**Arquivo:** `android/gradle/libs.versions.toml`

Verificar:
- [ ] `versionCode` está definido e é um número > 0?
  ```bash
  grep "versionCode" android/gradle/libs.versions.toml | grep -v "^#"
  ```
- [ ] `versionName` está definido em formato SemVer (X.Y.Z)?
  ```bash
  grep "versionName" android/gradle/libs.versions.toml | grep -E '"[0-9]+\.[0-9]+\.[0-9]+"'
  ```
- [ ] Versão em `libs.versions.toml` existe em `CHANGELOG.md`?
  ```bash
  VERSION=$(grep 'versionName' android/gradle/libs.versions.toml | sed 's/.*"\([^"]*\)".*/\1/')
  grep "\[$VERSION\]" CHANGELOG.md
  ```
  Se retornar nada: **BLOQUEADOR** — adicione a versão ao CHANGELOG antes de fazer release.

### 2. Build Android — Clean e sem cache

```bash
.\android\gradlew.bat clean assembleRelease --no-build-cache
```

Validar:
- [ ] Saída contém `BUILD SUCCESSFUL`?
- [ ] Nenhum aviso crítico (`warning:` ou `error:`)? (ignore warnings triviais de Gradle)
- [ ] APK gerado em `app/build/outputs/apk/release/`?

### 3. Testes Unitários

```bash
.\android\gradlew.bat test
```

Validar:
- [ ] Saída contém `BUILD SUCCESSFUL`?
- [ ] Nenhum `FAILED` em `Test Results`?

### 4. Linting — Kotlin

Se ktlint estiver configurado:

```bash
.\android\gradlew.bat ktlintCheck 2>&1
```

Validar:
- [ ] Saída contém `BUILD SUCCESSFUL` ou `No files to lint`?
- [ ] Se houver erros: `ktlintFormat` para corrigir, commitá-los e rerun.

### 5. Higiene de código

```bash
find android -name "*.kt" -type f | xargs grep -l "^[[:space:]]*//.*TODO\|^[[:space:]]*//.*FIXME" | head -5
```

Validar:
- [ ] Nenhum TODO/FIXME crítico solto? Se sim, verificar no Linear se há issue aberta.
- [ ] Nenhum arquivo `.old`, `.bak` ou `*.tmp`?

```bash
find android -name "*.old" -o -name "*.bak" -o -name "*.tmp" | wc -l
```

Deve retornar `0`.

### 6. Firebase / Crashlytics

Consultar Firebase Console (produto, não automatizável):
- [ ] Crash rate nas últimas 24h < 1%?
- [ ] Nenhum crash crítico não investigado?

### 7. Upload para Firebase App Distribution (após todas as checagens)

```bash
.\android\gradlew.bat appDistributionUploadRelease
```

Validar:
- [ ] Saída contém `BUILD SUCCESSFUL`?
- [ ] APK foi enviado (URL retornada no log)?

---

## Changelog — Validação Estrutural

**Arquivo:** `CHANGELOG.md` (Android)

```bash
# Verificar que a versão mais recente tem formato correto
HEAD_SECTION=$(head -30 CHANGELOG.md)
echo "$HEAD_SECTION" | grep -E "^## \[[0-9]+\.[0-9]+\.[0-9]+\].*—.*[0-9]{4}-[0-9]{2}-[0-9]{2}$"
```

Validar:
- [ ] Primeira seção de versão tem formato `## [X.Y.Z] — AAAA-MM-DD`?
- [ ] Contém pelo menos uma seção: `### Added`, `### Fixed`, ou `### Changed`?
- [ ] Descrições são em português e legíveis para usuário final (não jargão técnico)?

---

## Sumário — Antes de dar "Pronto para Release"

| Item | Android | Status |
|---|---|---|
| versionCode/versionName em libs.versions.toml | ✓ | ? |
| Versão em CHANGELOG.md | ✓ | ? |
| Clean build sem erros | ✓ | ? |
| Testes passando | ✓ | ? |
| Linting OK (ktlintCheck) | ✓ | ? |
| Nenhum TODO solto | ✓ | ? |
| Firebase crash rate < 1% | ✓ | ? |
| Pronto para upload | ✓ | ? |

---

## Escalada

- **Build falha:** `.\android\gradlew.bat clean` + limpar `build/`, depois retry.
- **Testes falham:** investigar stacktrace, corrigir em dev branch, merge, rerun.
- **ktlint falha:** `.\android\gradlew.bat ktlintFormat`, commit, rerun.
- **Versão não está em CHANGELOG:** adicione versão no topo antes de fazer build de release.
- **Crashlytics com crash rate > 1%:** não fazer release — investigar e corrigir bug.

---

## Notas

- Esta skill é **complementar** a `/checar-release` — use ambas.
- `/checar-release` fornece orientação (o que validar); `/validar-release` fornece comandos (como validar).
- O pre-commit hook em `scripts/pre-commit-android.sh` automatiza parte deste checklist no momento do commit.
