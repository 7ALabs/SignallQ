# Auditoria De Organizacao - 2026-05-23

## Resultado

Foram encontradas situacoes parecidas com a duplicidade dos scripts de APK.

## Corrigido

- Removido script legado de release `scripts/build/buildReleaseKotlin.ps1`.
- Removida pasta vazia `scripts/build/`.
- Criado `scripts/README.md` com classificacao dos scripts ativos.
- Criado `scripts/legacy/README.md`.
- Movidos scripts que alteravam Flutter legado para `scripts/legacy/`:
  - `scripts/legacy/flutter-modem/aplicarCorrecoesModem.py`
  - `scripts/legacy/flutter-oui/gerarTabelaOuiCsv.py`
- Removida pasta vazia `docs_ai/wireframes/`.
- Atualizados docs de agentes em `docs_ai/ai/` para apontar para a nova raiz e `AGENTS.md`.

## Mantido De Proposito

- Scripts de `scripts/speedtest/` que mencionam Flutter foram mantidos porque servem para paridade historica e calibracao, nao para build ativo.
- Referencias a `app-debug.apk` e `app-release.apk` foram mantidas apenas onde explicam que esses arquivos sao saidas brutas internas do Gradle e nao devem ser distribuidos.
- `docs/MIGRATION_REPORT_2026-05-23.md` mantem caminhos antigos porque registra a origem da migracao.

## Pontos Para Revisao Posterior

- Alguns docs tecnicos granulares ainda citam `linka-android-kotlin/` como caminho antigo ou inferido. Eles nao quebram automacao, mas devem ser revisados quando cada area for tocada.
- Alguns docs de release historicos v0.9.0 ainda citam caminhos antigos porque registram entregas passadas.

## Politica

- Scripts ativos ficam em `scripts/` com README explicando finalidade.
- Scripts que dependem de Flutter legado ficam em `scripts/legacy/`.
- Geracao de APK usa somente `scripts/build-apk-debug.ps1`, `scripts/build-apk-release.ps1`, `archiveDebugApk` ou `archiveReleaseApk`.
- Docs operacionais ativos ficam em `docs/` e `docs_ai/operations/`.
