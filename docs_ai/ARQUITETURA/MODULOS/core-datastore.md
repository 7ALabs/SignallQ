# Módulo :coreDatastore

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/core/datastore/build.gradle.kts`, código real)
- **Caminho físico:** `android/core/datastore/`
- **Namespace:** `io.signallq.app.core.datastore`

## Responsabilidade

Preferências do usuário via Jetpack DataStore (Preferences), armazenadas no arquivo
`linkaPreferencias` (identificador técnico preservado — não renomear).

## Principais packages/pastas

Base: `coreDatastore/src/main/kotlin/io/veloo/app/kotlin/core/datastore/` (caminho físico legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `PreferenciasAppRepository.kt` | Repository | Todos os flows reativos de preferências do usuário — inclui estados de histerese (`alerta_latencia_ativo`, `alerta_dns_ativo`, `alerta_rssi_ativo`, `alerta_sem_internet_ativo`) usados pelo `MonitoramentoWorker` |

## Entradas/saídas

- **Entradas:** escritas de preferência vindas de `:app` (Ajustes) e features que persistem flags.
- **Saídas:** `Flow` reativo por chave, consumido por `:app`, `:featureDevices`,
  `:featureSpeedtest`, `:featureDiagnostico`.

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`, `androidx-datastore-preferences`,
`androidx-security-crypto`, `kotlinx-coroutines-android`.

## Consumidores

Via grep de `project(":coreDatastore")`: `:app`, `:featureDevices`, `:featureSpeedtest`,
`:featureDiagnostico`.

## Testes existentes

`src/test`: **0 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Nenhum teste unitário para o repositório central de preferências — lacuna real, não corrigida aqui
(documentação read-only). Caminho físico `io/veloo` diverge do package — dívida 4.1.
```

