---
name: validar-release
description: Checklist executável de release — Android e PWA com validação de versionamento, changelog e build.
---

## Quando usar

Antes de gerar APK/AAB de release, fazer deploy do PWA no Cloudflare Pages ou submeter à loja.

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

## PWA — Checklist Executável (quando aplicável)

### 1. Versionamento

**Arquivo:** `package.json`

```bash
grep '"version"' package.json | head -1
```

Validar:
- [ ] `version` atualizado e em SemVer?
- [ ] Versão em `CHANGELOG.md` (PWA) ou no changelog central?

### 2. Build sem erros

```bash
npm run build 2>&1
```

Validar:
- [ ] Saída contém `Done in X.XXs`?
- [ ] Nenhum erro (vermelho)?
- [ ] Nenhum `console.log` de debug em código de produção?

```bash
grep -r "console\.log\|console\.debug" pwa/src --include="*.ts" --include="*.tsx" | grep -v "// @ts-ignore" | wc -l
```

Deve retornar `0` (ou comentários confirmando que é intencional).

### 3. TypeScript sem erros

```bash
npx tsc --noEmit
```

Validar:
- [ ] Saída vazia ou sem erros (`error TS`)?

### 4. Worker Cloudflare (se houver mudanças em `integrations/`)

```bash
cd integrations/cloudflare/ai-diagnosis-worker && npx wrangler deploy
```

Validar:
- [ ] Saída contém `Published`?

---

## Changelog — Validação Estrutural

**Arquivo:** `CHANGELOG.md` (Android) ou `pwa/CHANGELOG.md` (PWA)

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

| Item | Android | PWA | Status |
|---|---|---|---|
| versionCode/versionName em libs.versions.toml | ✓ | — | ? |
| Versão em CHANGELOG.md | ✓ | ✓ | ? |
| Clean build sem erros | ✓ | ✓ | ? |
| Testes passando | ✓ | — | ? |
| Linting OK (ktlintCheck) | ✓ | — | ? |
| Nenhum TODO solto | ✓ | ✓ | ? |
| Firebase crash rate < 1% | ✓ | — | ? |
| Pronto para upload | ✓ | ✓ | ? |

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
