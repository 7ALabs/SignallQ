# Scripts

## Oficiais

- `build-apk-debug.ps1`: gera APK debug versionado em `builds/apk/debug/<versionName>/`.
- `build-apk-release.ps1`: gera APK release versionado em `builds/apk/release/<versionName>/`.
- `version.ps1`: altera `gradle/libs.versions.toml`.
- `check-env.ps1`: valida ambiente local.
- `clean-build.ps1`: remove outputs/cache locais sem apagar `builds/apk/`.
- `pre-commit-android.sh`: Git pre-commit hook para validar versionamento, changelog e higiene antes de commitar mudanças no Android.

## Claude Code Hooks

### observe-and-act.sh

Hook `PostToolUse` (matcher `Bash`) registrado em `.claude/settings.json`, roda após `git push` ou `gh pr merge`. Escaneia `.kt` alterados desde o merge-base com `main` e grava violações conhecidas (string técnica em `Text()`, `LazyColumn` aninhado, Toast técnico, TODO/FIXME em produção, cor hardcoded sem token) em `.claude/agent-observations/<ano-mes>/obs-<data>-staticcheck.md`.

Sem LLM, sem Slack, sem GitHub — só log local. Não abre issue automaticamente (removido em 2026-07-04: criava issues sem supervisão a cada push).

## Git Hooks

### pre-commit-android.sh

Automatiza validações de release readiness no momento do commit:
- Verifica se `versionCode` e `versionName` estão definidos em `android/gradle/libs.versions.toml`
- Confirma que a versão está documentada em `CHANGELOG.md`
- Valida que não há arquivos `.old`, `.bak` ou `.tmp` em `android/`
- Roda `ktlintCheck` se ktlint estiver configurado (aviso, não bloqueador)

**Instalação:**
```bash
cp scripts/pre-commit-android.sh .git/hooks/pre-commit
chmod +x .git/hooks/pre-commit
```

**O que faz:**
- Pula automaticamente se não há mudanças em `android/`
- Imprime relatório colorido com ✓/✗/⚠
- Bloqueia commit se: versionCode/versionName vazios, versão não está em CHANGELOG, ou há arquivos de lixo
- Avisa (não bloqueia) se ktlint falhar

**Desinstalar (restaurar hook padrão ou remover):**
```bash
rm .git/hooks/pre-commit
```

## Agentes

- `agent-status.ps1`
- `agent-delegate.ps1`
- `agent-wake.ps1`

## Investigacao Android

- `audit-gpon/`: auditoria GPON.
- `modem/`: scripts de analise/sondagem de modem que nao alteram o app Flutter.
- `speedtest/`: calibracao e paridade historica de speedtest.

## Legacy

`legacy/` contem scripts preservados apenas como referencia historica. Eles nao fazem parte do fluxo ativo do Android Kotlin.

Nao use scripts em `legacy/` para tarefas novas.
