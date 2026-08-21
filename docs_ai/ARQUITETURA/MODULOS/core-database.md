---
title: "Módulo :coreDatabase"
description: "Banco Room local do Consumer: 8 entidades, 7 DAOs, schema na versão 19 com 18 migrations encadeadas."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-21"
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

Nenhuma dependência de outro módulo do monorepo. `ResolvedorNetworkId` (`src/main/kotlin/io/signallq/app/core/database/rede/`) foi promovido de `:featureSettings` pra cá na issue #1707 (Task 2.0.09e, épico #1647) — função pura, sem dependência Android, reaproveitada tanto por `ConnectionProfile` (`:featureSettings`, via `implementation(project(":coreDatabase"))`) quanto por `MedicaoEntity.networkId`.

## Consumidores

| Módulo | Tipo |
|---|---|
| `:app` | `implementation` |
| `:featureDevices`, `:featureDiagnostico`, `:featureHistory`, `:featureSpeedtest` | `implementation` |

## Componentes principais

| Arquivo/classe | Responsabilidade |
|---|---|
| `src/main/kotlin/io/signallq/app/core/database/SignallQDatabase.kt` | `@Database` com 8 entities, `version = 19`, `exportSchema = true`; expõe os 7 Daos |
| `src/main/kotlin/io/signallq/app/core/database/CoreDatabaseModulo.kt` | define as 18 migrations e monta o `Room.databaseBuilder` (arquivo `linkaKotlin.db`) |
| `src/main/kotlin/io/signallq/app/core/database/MedicaoEntity.kt` / `MedicaoDao.kt` | histórico de medições de speedtest/monitoramento; `networkId` (GH#1707) identifica a rede da medição pra comparação de reteste |
| `src/main/kotlin/io/signallq/app/core/database/rede/ResolvedorNetworkId.kt` | resolve `networkId` estável (BSSID/SSID Wi-Fi ou operadora móvel) — promovido de `:featureSettings` na issue #1707 |
| `src/main/kotlin/io/signallq/app/core/database/ApelidoDispositivoEntity.kt` / `ApelidoDispositivoDao.kt` | apelido por MAC de dispositivo da rede local |
| `src/main/kotlin/io/signallq/app/core/database/chat/ChatSessionEntity.kt`, `ChatMessageEntity.kt`, `ChatSessionDao.kt` | sessões e mensagens do chat de diagnóstico |
| `src/main/kotlin/io/signallq/app/core/database/recommendation/RecommendationHistoryEntity.kt` / `Dao` | histórico de exibições do Recommendation Engine (cooldown, limites, feedback — issues #790/#812) |
| `src/main/kotlin/io/signallq/app/core/database/connectivity/ConnectivityDiagnosisHistoryEntity.kt` / `Dao` | histórico do diagnóstico de conectividade (GH#1512), só campos sanitizados |
| `src/main/kotlin/io/signallq/app/core/database/provider/ProviderDirectoryCacheEntity.kt` / `Dao` | cache local do diretório remoto de provedores (GH#1462) |
| `src/main/kotlin/io/signallq/app/core/database/analytics/AnalyticsOutboxEntity.kt` / `Dao` | outbox de eventos de analytics com retry (`enqueue`/`due`/`acknowledge`/`defer`/`clear`) |

### Schema Room

- **Versão atual:** `19`
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

**Migrations:** 18 objetos `Migration`, de 1→2 até 18→19, todos registrados por `addMigrations` em `criarBanco`. Não há `fallbackToDestructiveMigration`. A última (`MIGRATION_18_19`, `internal`, GH#1707) adiciona a coluna nullable `networkId` em `medicao` — aditiva, sem `NOT NULL DEFAULT`, linhas antigas recebem `NULL`.

**Testes de migration existentes** (`src/androidTest/.../`): `Migration9Para10Test`, `Migration13Para14Test`, `Migration14Para15Test`, `Migration15Para16Test`, `Migration16Para17Test`, `Migration17Para18Test`, `Migration18Para19Test` — 7 das 18 migrations têm teste dedicado. Também há `ChatSessionDaoTest`, `AnalyticsOutboxDaoTest`, `RecommendationHistoryDaoTest` e `MedicaoDaoNetworkIdTest` (query `buscarUltimaComparavelNaRede`, GH#1707).

## Riscos e dívidas

- **`connectedDebugAndroidTest` nunca rodava de verdade até GH#1707 (PR #1786):** o sourceSet `androidTest` não apontava `assets.srcDirs` pra `schemas/`, então `MigrationTestHelper` nunca encontrava o schema exportado — corrigido na PR #1786. Isso expôs falhas reais e pré-existentes em `Migration13Para14Test`, `Migration14Para15Test`, `Migration15Para16Test`, `Migration17Para18Test`, `Migration9Para10Test` e `AnalyticsOutboxDaoTest` — investigadas e majoritariamente corrigidas em GH#1787 (ver itens abaixo); só `Migration17Para18Test` segue vermelho, de propósito.
- **Schemas `9.json`/`15.json` reconstruídos manualmente (GH#1787):** nunca existiram sob o FQN atual (`io.signallq.app.core.database.SignallQDatabase`) — a classe já se chamou `io.linka.app...LinkaDatabase` e `io.signallq.app...VelooDatabase` nas versões 9/15, e o schema exportado sob esses nomes antigos não é reconhecido pelo `MigrationTestHelper` de hoje. Reconstruídos por diff estrutural a partir dos schemas reais adjacentes (9 = 10.json menos `chat_sessions`/`chat_messages`, únicas tabelas que `MIGRATION_9_10` cria; 15 = 14.json mais `connectivity_diagnosis_history` exatamente como `MIGRATION_14_15` a cria — consistência cruzada confirmada batendo com 16.json menos as duas colunas que só `MIGRATION_15_16` adiciona). `identityHash` de ambos é um placeholder (não é lido para validação estrutural pelo `MigrationTestHelper`, só as `createSql`/colunas/índices declarados nas entidades).
- **`Migration13Para14Test`/`Migration15Para16Test` (GH#1787):** os `INSERT` dos próprios testes tinham um valor a menos que colunas declaradas (faltava o `NULL` da coluna `score`) — bug só nos testes, nunca nas migrations reais (que são só `ALTER TABLE ADD COLUMN`, já rodaram sem incidente em produção). Corrigido.
- **`AnalyticsOutboxDaoTest` (GH#1787):** assert incorreto — `enqueue()` retorna o rowid interno do SQLite (autoincrementado por linha, não pelo `id` de negócio), o teste esperava `1L` para a segunda linha inserida quando o valor real e correto é `2L`. Bug só no teste, DAO nunca esteve errado. Corrigido.
- **`Migration17Para18Test` vermelho de propósito — achado crítico não corrigido (GH#1787):** `MIGRATION_17_18` (já publicada, GH real em produção desde 2026-08-04) cria via SQL bruto o índice `index_analytics_outbox_nextAttemptAtEpochMs`, mas `AnalyticsOutboxEntity` nunca declarou `@Index` correspondente — inconsistência original da própria PR que introduziu a tabela, não uma regressão posterior. Efeito real: dispositivo que migrou por essa cadeia tem o índice; instalação nova (Room cria direto das entidades na v19) não tem. Não corrige dado nem quebra leitura/escrita — só faz um SELECT com filtro em `nextAttemptAtEpochMs` variar entre index scan e table scan (tabela pequena, outbox é fila transiente). Consertar exige declarar `@Index` na entidade **e** abrir uma migration nova (`CREATE INDEX IF NOT EXISTS` idempotente, versão 20) — nunca só adicionar a anotação sem bump de versão, isso quebraria o identity-hash check do Room pra todo usuário já na v19. Decisão de arquitetura/versão do banco, fora do escopo de correção de teste — decisão do Luiz antes de tocar.
- **Schemas de nomes antigos ainda versionados:** `schemas/io.linka.app.kotlin.core.database.LinkaDatabase/` (`1..10`) e `schemas/io.signallq.app.core.database.VelooDatabase/` (`10.json`) permanecem no repositório — três nomes de banco na história do produto (Linka → Veloo → SignallQ).
- **Nomes legados em produção:** o arquivo do banco continua `linkaKotlin.db`. Trocar exige migração de dados, não é rename cosmético.
- **Path físico alinhado ao package `io.signallq.app.*`** — migração de `io/signallq/app/kotlin/` concluída em 2026-08-15 (#1645).
- **Cobertura parcial de migrations:** 11 das 18 migrations não têm teste instrumentado dedicado (nenhuma de 1→2 a 8→9, nem 10→11..12→13).
- Nenhum arquivo acima de 800 linhas.
- **`networkId` (GH#1707) é `null` pra toda medição gravada por `MonitoramentoWorker`** (medição sintética "monitor" — não lê SSID/BSSID/operadora, só RSSI) e pra qualquer linha anterior à migração 18→19. A comparação de reteste (2.0.09e, ainda não implementada nesta fatia) precisa tratar `null` como "sem par comparável", nunca inventar rede.
