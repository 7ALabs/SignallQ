# Politica de Versionamento

## Fonte de verdade

```
android/gradle/libs.versions.toml
```

Campos relevantes: `versionName` e `versionCode`. Consumidos por `app/build.gradle.kts` no `defaultConfig`.

## SemVer

`versionName` segue **MAJOR.MINOR.PATCH**:

- **MAJOR** — mudanca incompativel ou marco de produto (ex: v1.0.0 no lancamento)
- **MINOR** — feature nova ou mudanca funcional relevante
- **PATCH** — correcao de bug ou ajuste menor

## Pre-release

Sufixos pre-release seguem SemVer:

- Beta fechado: `1.0.0-beta.1`, `1.0.0-beta.2`, ...
- Release candidate: `1.0.0-rc.1`, `1.0.0-rc.2`, ...
- Producao: `1.0.0` (sem sufixo)

## versionCode

- Inteiro crescente. **Nunca reutilizar** — requisito da Play Store.
- **Regra reforcada em 2026-07-17**: incrementar SEMPRE antes de qualquer build publicado
  em qualquer canal — Play Console (`internal`/`alpha`/`beta`/`production`) OU Firebase App
  Distribution (`workflow_dispatch` do `firebase-distribution.yml`). E um campo global unico
  em `libs.versions.toml` — nao existe contador separado por canal. Gaps sao normais (nem
  todo build de debug vira release), mas **nunca subir um build sem bumpar antes** — dois
  uploads com o mesmo numero quebram rastreabilidade.

## Quando v1.0.0

**Corrigido em 2026-07-17**: `1.0.0` e reservado pro **primeiro publish na trilha
`production`** da Play Console — nao antes. Enquanto o app estiver em qualquer trilha de
teste (`internal`, `alpha`, `beta`), `versionName` continua `0.x.y`, independente de quantas
features/milestones do roadmap ja tiverem sido entregues.

> Historico: a versao anterior deste doc amarrava 1.0.0 ao "Beta Fechado (M2)" — essa regra
> nunca foi seguida na pratica (a v0.26.0 ja passou esse marco do roadmap e segue `0.x`).
> Trilha atual do produto (ver `docs_ai/operations/DEPLOY.md`): `release.yml` publica
> sempre em `internal`; `promote-release.yml` promove pra `alpha` sob demanda; `beta` e
> `production` ainda nao liberados (guardrail tecnico no workflow de promocao).

## Tabela de fases

| Trilha Play Console | versionName | Notas |
|------|-------------|-------|
| `internal` (teste interno) | 0.x.y | Destino de todo `release.yml`, so o Luiz valida |
| `alpha` (teste fechado) | 0.x.y | Promovido de `internal` via `promote-release.yml` |
| `beta` (teste aberto) | 0.x.y | Ainda nao liberado — guardrail bloqueia |
| `production` | **1.0.0** (no primeiro publish) | Ainda nao liberado — guardrail bloqueia |

## Regras

- `versionName` muda apenas em releases significativos (feature, correcao, milestone) — nao
  em toda iteracao de debug.
- `versionCode` incrementa em todo build publicado, em qualquer canal (Play Console ou
  Firebase App Distribution) — ver regra reforcada acima.
- APKs refletem a versao no nome conforme `docs_ai/operations/APK_OUTPUT_POLICY.md`.

## Comandos

```powershell
.\scripts\version.ps1 show
.\scripts\version.ps1 patch
.\scripts\version.ps1 minor
.\scripts\version.ps1 major
.\scripts\version.ps1 build
.\scripts\version.ps1 set 1.0.0-beta.1+53
```
