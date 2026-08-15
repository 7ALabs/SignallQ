---
title: "Módulo :coreDatabase"
description: "Banco Room local do Consumer: 8 entidades, 7 DAOs, schema na versão 18 com 17 migrations encadeadas."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-15"
---

# `:coreDatabase`

- **Caminho físico:** `android/core/database/` (alias flat legado — `projectDir` remapeado em `settings.gradle.kts`)
- **Namespace:** `io.signallq.app.core.database`
- **Tipo:** biblioteca Android

## Responsabilidade

Define o banco Room local do app Consumer (`SignallQDatabase`), suas Entities, Daos e toda a cadeia de migrations, além da fábrica `CoreDatabaseModulo.criarBanco(context)`. É a única fonte de persistência estruturada/relacional do Consumer.

Não é dele: preferências chave-valor (`:coreDatastore`), regras de negócio sobre os dados persistidos (ficam nas features e em `:core:diagnostico`). Também não expõe Repository: os Daos são consumidos diretamente pelas camadas acima.

## Dependências

| Módulo/lib | Para quê |
|---|---|
| `androidx.core.ktx` | utilitários de plataforma |
| `androidx.room.runtime` (`api`) | runtime Room, reexportado aos consumidores |
| `androidx.room.ktx` (`api`) | suporte a coroutines/`Flow` nos Daos, reexportado |
| `androidx.room.compiler` (`kapt`) | geração de código Room |
| `junit` (test) | teste JVM de `MedicaoEntity` |
| `androidx.room.testing` (androidTest) | `MigrationTestHelper` nos testes de migration |
| `kotlinx.coroutines.test` (androidTest) | `runTest`/`Flow.first()` em `ChatSessionDaoTest` (dependência que faltava, corrigida na GH#1228 Fase 3) |
| `androidx.junit`, `androidx.espresso.core` (androidTest) | scaffolding instrumentado |

Nenhuma dependência de outro módulo do monorepo.

## Consumidores

| Módulo | Tipo |
|---|---|
| `:app` | `implementation` |
| `:featureDevices`, `:featureDiagnostico`, `:featureHistory`, `:featureSpeedtest` | `implementation` |

## Componentes principais

| Arquivo/classe | Responsabilidade |
|---|---|
| `src/main/kotlin/io/signallq/app/core/database/SignallQDatabase.kt` | `@Database` com 8 entities, `version = 18`, `exportSchema = true`; expõe os 7 Daos |
| `src/main/kotlin/io/signallq/app/core/database/CoreDatabaseModulo.kt` (315 linhas) | define as 17 migrations e monta o `Room.databaseBuilder` (arquivo `linkaKotlin.db`) |
| `src/main/kotlin/io/signallq/app/core/database/MedicaoEntity.kt` / `MedicaoDao.kt` | histórico de medições de speedtest/monitoramento |
| `src/main/kotlin/io/signallq/app/core/database/ApelidoDispositivoEntity.kt` / `ApelidoDispositivoDao.kt` | apelido por MAC de dispositivo da rede local |
| `src/main/kotlin/io/signallq/app/core/database/chat/ChatSessionEntity.kt`, `ChatMessageEntity.kt`, `ChatSessionDao.kt` | sessões e mensagens do chat de diagnóstico |
| `src/main/kotlin/io/signallq/app/core/database/recommendation/RecommendationHistoryEntity.kt` / `Dao` | histórico de exibições do Recommendation Engine (cooldown, limites, feedback — issues #790/#812) |
| `src/main/kotlin/io/signallq/app/core/database/connectivity/ConnectivityDiagnosisHistoryEntity.kt` / `Dao` | histórico do diagnóstico de conectividade (GH#1512), só campos sanitizados |
| `src/main/kotlin/io/signallq/app/core/database/provider/ProviderDirectoryCacheEntity.kt` / `Dao` | cache local do diretório remoto de provedores (GH#1462) |
| `src/main/kotlin/io/signallq/app/core/database/analytics/AnalyticsOutboxEntity.kt` / `Dao` | outbox de eventos de analytics com retry (`enqueue`/`due`/`acknowledge`/`defer`/`clear`) |

### Schema Room

- **Versão atual:** `18`
- **`exportSchema`:** `true`; `room.schemaLocation` = `$projectDir/schemas`, `room.incremental` = `true`
- **Arquivo do banco:** `linkaKotlin.db` (nome legado, mantido para não quebrar bases instaladas)

| Entity | Tabela real |
|---|---|
| `MedicaoEntity` | `medicao` |
| `ApelidoDispositivoEntity` | `apelido_dispositivo` |
| `ChatSessionEntity` | `chat_sessions` |
| `ChatMessageEntity` | `chat_messages` |
| `RecommendationHistoryEntity` | `recommendation_history` |
| `ConnectivityDiagnosisHistoryEntity` | `connectivity_diagnosis_history` |
| `ProviderDirectoryCacheEntity` | `provider_directory_cache` |
| `AnalyticsOutboxEntity` | `analytics_outbox` |

**Migrations:** 17 objetos `Migration`, de 1→2 até 17→18, todos registrados por `addMigrations` em `criarBanco`. Não há `fallbackToDestructiveMigration`. A última (`MIGRATION_17_18`, `internal`) cria `analytics_outbox` e seu índice `index_analytics_outbox_nextAttemptAtEpochMs`.

**Testes de migration existentes** (`src/androidTest/.../`): `Migration9Para10Test`, `Migration13Para14Test`, `Migration14Para15Test`, `Migration15Para16Test`, `Migration16Para17Test`, `Migration17Para18Test` — 6 das 17 migrations têm teste dedicado. Também há `ChatSessionDaoTest`, `AnalyticsOutboxDaoTest` e `RecommendationHistoryDaoTest`.

## Riscos e dívidas

- **Schema `15.json` ausente:** `schemas/io.signallq.app.core.database.SignallQDatabase/` contém `10..14`, `16`, `17`, `18` — falta o `15.json`, apesar de `exportSchema = true`. Rompe a cadeia de validação automática de migrations nessa faixa.
- **Schemas de nomes antigos ainda versionados:** `schemas/io.linka.app.kotlin.core.database.LinkaDatabase/` (`1..10`) e `schemas/io.signallq.app.core.database.VelooDatabase/` (`10.json`) permanecem no repositório — três nomes de banco na história do produto (Linka → Veloo → SignallQ).
- **Nomes legados em produção:** o arquivo do banco continua `linkaKotlin.db`. Trocar exige migração de dados, não é rename cosmético.
- **Path físico alinhado ao package `io.signallq.app.*`** — migração de `io/signallq/app/kotlin/` concluída em 2026-08-15 (#1645).
- **Cobertura parcial de migrations:** 11 das 17 migrations não têm teste instrumentado dedicado (nenhuma de 1→2 a 8→9, nem 10→11..12→13).
- Nenhum arquivo acima de 800 linhas (maior: `CoreDatabaseModulo.kt`, 315 linhas; `src/main` total: 17 arquivos, 933 linhas).
