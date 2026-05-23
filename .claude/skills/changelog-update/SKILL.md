---
description: Atualiza o CHANGELOG do projeto com a entrega atual — formato Keep a Changelog, versionamento SemVer, executado por Gema ao fechar cada entrega.
---

## Quando usar
Gema executa após aprovar entrega (`/release-check`). Parte do fluxo Done/Not Done.

## Localização dos changelogs
- Android: `linkaAndroidKotlin/CHANGELOG.md`
- PWA: `linkaSpeedtestPwa/CHANGELOG.md`

## Formato (Keep a Changelog)

```markdown
## [X.Y.Z] — AAAA-MM-DD

### Added
- Descrição da feature nova em linguagem de usuário.

### Changed
- Descrição de comportamento alterado.

### Fixed
- Descrição do bug corrigido.

### Removed
- O que foi removido.
```

## Regras de versionamento SemVer

| Tipo de mudança | Bump |
|---|---|
| Bug fix sem quebra de contrato | PATCH (X.Y.**Z**) |
| Feature nova retrocompatível | MINOR (X.**Y**.0) |
| Quebra de contrato, remoção | MAJOR (**X**.0.0) |

## Regras de escrita

- Escrever em perspectiva do usuário — não do dev.
- "Adicionado diagnóstico de fibra óptica" — não "implementado FeatureFibraViewModel".
- Máximo 1 linha por item.
- Sem abreviações técnicas na seção `Added`/`Changed`/`Fixed`.
- Seção `[Unreleased]` no topo para mudanças ainda não lançadas.

## Atualização de versionCode/versionName (Android)

Após atualizar o CHANGELOG, verificar se `versionCode` e `versionName` no `app/build.gradle` estão consistentes com a versão documentada.

## Limites
- Gema escreve o changelog, não faz o build.
- Build → Camilo via `/release-ready-android`.
