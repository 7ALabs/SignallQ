# Módulo :coreDatabase

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/core/database/build.gradle.kts`, código real)
- **Caminho físico:** `android/core/database/`
- **Namespace:** `io.signallq.app.core.database`

## Responsabilidade

Persistência local via Room (SQLite). Único módulo com acesso direto ao banco `SignallQDatabase`.

## Principais packages/pastas

Base: `coreDatabase/src/main/kotlin/io/veloo/app/kotlin/core/database/` (caminho físico legado) +
subpacote `chat/`.

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `SignallQDatabase.kt` | Room Database | DB principal — **versão 14** (GH#1027), 4 entidades, 3 DAOs (confirmado em `SignallQDatabase.kt`) |
| `MedicaoEntity.kt` / `MedicaoDao.kt` | Entity/DAO | Tabela `medicao` — medições de speedtest e monitoramento (**coluna `bandaWifi` adicionada em v14** — captura banda 2.4GHz/5GHz durante medição, `NULL` pra histórico) |
| `ApelidoDispositivoEntity.kt` / `ApelidoDispositivoDao.kt` | Entity/DAO | Tabela `apelido_dispositivo` |
| `CoreDatabaseModulo.kt` | Object | Fábrica `criarBanco(context)` + migrações v1→v10 |
| `chat/ChatSessionEntity.kt`, `chat/ChatMessageEntity.kt` | Entity | Tabelas `chat_sessions`, `chat_messages` |
| `chat/ChatSessionDao.kt` | DAO | Queries de sessões e mensagens de chat |

## Entradas/saídas

- **Entradas:** escritas de medições, apelidos, sessões de chat vindas de `:app` e das features
  consumidoras.
- **Saídas:** `Flow`/consultas Room expostas via DAOs para `:app`, `:featureDevices`,
  `:featureDiagnostico`, `:featureHistory`.

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`, `androidx-room-runtime`/`room-ktx` (via
`api`, propaga para consumidores), `kapt` room-compiler. Testes: `androidx-room-testing`.

## Consumidores

Via grep de `project(":coreDatabase")`: `:app`, `:featureDevices`, `:featureDiagnostico`,
`:featureHistory`.

## Testes existentes

`src/test`: **1 arquivo**. `src/androidTest`: **3 arquivos** (DAO/Room, únicos androidTest do
monorepo).

## Riscos/dívidas conhecidas

Caminho físico `io/veloo/app/kotlin/` diverge do package declarado — dívida 4.1 da regra de
higiene. Cobertura de `src/test` baixa (1 arquivo) frente ao papel central deste módulo — os 3
androidTest cobrem só o nível de Room/DAO, não regras de migração isoladamente.
```

