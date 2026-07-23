# Módulo :coreDatabase

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/core/database/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:coreDatabase` (alias legado; pasta física `android/core/database/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Persistência local via Room (SQLite). Único módulo com acesso direto ao banco `SignallQDatabase`.
Namespace declarado: `io.signallq.app.core.database`.

## Diagrama de componentes

```
:coreDatabase
├── (raiz, io/veloo/ — caminho legado)
│     SignallQDatabase (Room DB, v14), MedicaoEntity/Dao, ApelidoDispositivoEntity/Dao,
│     CoreDatabaseModulo (fábrica + migrações)
├── chat/ (io/veloo/)
│     ChatSessionEntity/Dao, ChatMessageEntity
└── recommendation/ (io/signallq/ — único subpacote já no caminho correto)
      RecommendationHistoryEntity, RecommendationHistoryDao
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `SignallQDatabase.kt` | Room Database | DB principal — **versão 14** (confirmado em código: `version = 14`) |
| `MedicaoEntity.kt` / `MedicaoDao.kt` | Entity/DAO | Tabela `medicao` — medições de speedtest e monitoramento |
| `ApelidoDispositivoEntity.kt` / `ApelidoDispositivoDao.kt` | Entity/DAO | Tabela `apelido_dispositivo` |
| `CoreDatabaseModulo.kt` | Object | Fábrica `criarBanco(context)` + migrações |
| `chat/ChatSessionEntity.kt`, `chat/ChatMessageEntity.kt` | Entity | Tabelas `chat_sessions`, `chat_messages` |
| `chat/ChatSessionDao.kt` | DAO | Queries de sessões e mensagens de chat |
| `recommendation/RecommendationHistoryEntity.kt` / `RecommendationHistoryDao.kt` | Entity/DAO | Histórico de recomendações servidas por `:coreRecommendation` — subpacote já criado direto em `io/signallq/`, não em `io/veloo` |

## Fluxo de dados principal

- **Entradas:** escritas de medições, apelidos, sessões de chat e histórico de recomendação vindas
  de `:app` e das features consumidoras.
- **Saídas:** `Flow`/consultas Room expostas via DAOs para `:app`, `:featureDevices`,
  `:featureDiagnostico`, `:featureHistory` (confirmado via grep de `project(":coreDatabase")`).

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo.** Libs: `androidx-core-ktx`,
  `androidx-room-runtime`/`room-ktx` (via `api`, propaga para consumidores), `kapt` room-compiler.
  Testes: `androidx-room-testing`.
- **Subpacote `recommendation/` nasceu direto em `io/signallq/`** enquanto o resto do módulo
  permanece em `io/veloo/app/kotlin/core/database/` — mesmo padrão observado em `:coreNetwork`
  (código novo já nasce no caminho correto, código antigo só migra na tarefa dedicada da regra de
  higiene 4.1).

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Caminho físico misto `io/veloo` (maioria) + `io/signallq` (`recommendation/`) | Duas convenções dentro do mesmo módulo | Não migrar oportunisticamente — parte da migração dedicada 4.1 |
| Cobertura de teste baixa: `src/test` **1 arquivo**, `src/androidTest` **4 arquivos** (Room/DAO) frente ao papel central deste módulo | Migrações e queries sem characterization test isolado | Priorizar teste de migração/DAO ao tocar o schema |
