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
- QA/Release: use `docs_ai/operations/`, `docs_ai/operations/GuiaReleaseBuild.md` e `scripts/`.
- Integracoes: use `integrations/`, mantendo dependencias baixadas fora do Git.

## Regras de trabalho

- Codigo Android fica nos modulos Gradle em `android/`.
- Codigo do admin panel fica em `SignallQ Admin/`.
- Documentacao viva (incluindo operacional: release, APK, QA) fica em `docs_ai/`.
- `docs/` guarda so artefatos de governanca do GitHub (migracao de issues, roadmap de epicos).
- Scripts versionados ficam em `scripts/`.
- Segredos devem ser recriados localmente a partir de templates, nunca migrados.
- Artefatos de build devem ser gerados de novo, nunca versionados.

## Handoff e board

Execução vive no **GitHub Issues** (`7ALabs/SignallQ`), classificada na hierarquia Épico >
Feature > Task via campos de Project (`Tipo`/`Épico`/`Feature`) — ver `.claude/CLAUDE.md`, seção
"Fontes da Verdade", para o mecanismo completo. Handoff entre agentes é dispatch direto (tool
`Agent`, retomado por `SendMessage`), não um script: não existe `.claude/tasks/queue/` de fato no
repo, é controle de sequenciamento de quem orquestra.

Não há notificação manual em ferramenta externa. GitHub notifica o Slack diretamente (app oficial,
`/github subscribe`) — nenhum agente cria fluxo manual paralelo (Discord ou script de webhook).
Os scripts `scripts/agent-handoff.sh`, `scripts/notify.sh` e `scripts/discord_notify.sh` estão
depreciados — não são mecanismo de handoff, não documentar como fluxo.

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

Nunca entregue `app-debug.apk` ou `app-release.apk` diretamente. A regra de saida esta em `docs_ai/operations/APK_OUTPUT_POLICY.md`.
