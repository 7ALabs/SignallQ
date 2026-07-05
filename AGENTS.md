# SignallQ — Guia Para Agentes

Monorepo com plataformas separadas: `android/` (Kotlin nativo), `integrations/` (Cloudflare), `scripts/`, `docs_ai/`.

## Entrada rapida

1. Leia `docs_ai/README.md` antes de carregar documentos especificos.
2. Use `android/settings.gradle.kts` para entender os modulos Android ativos.
3. Prefira busca por simbolo antes de abrir arquivos grandes.
4. Nao reintroduza Flutter legado, APKs gerados, caches, segredos ou dumps temporarios.

## Escopo dos agentes

- Produto/UX: use `docs_ai/functional/` e `docs_ai/design-system/`.
- Engenharia Android: use `docs_ai/technical/`, `android/settings.gradle.kts` e os modulos `android/core*`/`android/feature*`.
- QA/Release: use `docs_ai/operations/`, `docs/GuiaReleaseBuild.md` e `scripts/`.
- Integracoes: use `integrations/`, mantendo dependencias baixadas fora do Git.

## Regras de trabalho

- Codigo Android fica nos modulos Gradle em `android/`.
- Codigo do admin panel fica em `SignallQ Admin/`.
- Documentacao viva fica em `docs_ai/`.
- Documentacao operacional complementar fica em `docs/`.
- Scripts versionados ficam em `scripts/`.
- Segredos devem ser recriados localmente a partir de templates, nunca migrados.
- Artefatos de build devem ser gerados de novo, nunca versionados.

## Handoff e board

Sempre que uma issue do board mudar de estado, o agente chama:

```bash
scripts/agent-handoff.sh <agente> <evento> <issue#> "<mensagem>" [--para <outro>]
```

Eventos: `start | handoff | review | docs | done | block | refine | ready`.

Isso move o card no Project, ajusta labels e notifica Discord + Slack em uma chamada. Detalhes e tabela de eventos em [`docs/WORKFLOW_BOARD.md`](docs/WORKFLOW_BOARD.md). Substitui o uso direto de `scripts/notify.sh` (que continua disponível para mensagens fora de issue).

## Validacao minima

Antes de concluir uma mudanca Android (rodar da raiz do repo):

```powershell
.\android\gradlew.bat test
.\android\gradlew.bat assembleDebug
```

Quando precisar gerar APK, use somente:

```powershell
.\scripts\build-apk-debug.ps1
.\scripts\build-apk-release.ps1
```

ou:

```powershell
.\android\gradlew.bat archiveDebugApk
.\android\gradlew.bat archiveReleaseApk
```

Nunca entregue `app-debug.apk` ou `app-release.apk` diretamente. A regra de saida esta em `docs/APK_OUTPUT_POLICY.md`.
