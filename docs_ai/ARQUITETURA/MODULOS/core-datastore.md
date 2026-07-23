# Módulo :coreDatastore

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/core/datastore/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:coreDatastore` (alias legado; pasta física `android/core/datastore/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Preferências do usuário via Jetpack DataStore (Preferences), armazenadas no arquivo
`linkaPreferencias` (identificador técnico preservado — não renomear). Cresceu desde a última
auditoria: além do repositório único de preferências, hoje também guarda perfil de conexão
persistido, credenciais de modem e feature flags locais. Namespace declarado:
`io.signallq.app.core.datastore`.

## Diagrama de componentes

```
:coreDatastore (io/veloo/ — caminho legado)
├── PreferenciasAppRepository — flows reativos de preferências do usuário
├── ConnectionProfilePersistido — perfil de conexão (ISP/plano) persistido
├── CredenciaisModemStore — credenciais do modem/ONT, para acesso HTTP local (:featureFibra)
└── FeatureFlagStore — feature flags locais persistidas
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `PreferenciasAppRepository.kt` | Repository | Flows reativos de preferências do usuário — inclui estados de histerese (`alerta_latencia_ativo`, `alerta_dns_ativo`, `alerta_rssi_ativo`, `alerta_sem_internet_ativo`) usados pelo `MonitoramentoWorker` |
| `ConnectionProfilePersistido.kt` | Repository/modelo | Persistência do perfil de conexão detectado/informado pelo usuário |
| `CredenciaisModemStore.kt` | Repository | Persiste credenciais de acesso ao modem/ONT local |
| `FeatureFlagStore.kt` | Repository | Persistência local de feature flags |

## Fluxo de dados principal

- **Entradas:** escritas de preferência vindas de `:app` (Ajustes/Perfil) e features que persistem
  flags ou credenciais.
- **Saídas:** `Flow` reativo por chave, consumido por `:app`, `:featureDevices`,
  `:featureSpeedtest`, `:featureDiagnostico` (confirmado via grep de `project(":coreDatastore")`).

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo.** Libs: `androidx-core-ktx`,
  `androidx-datastore-preferences`, `androidx-security-crypto`, `kotlinx-coroutines-android`.
- `androidx-security-crypto` sugere que `CredenciaisModemStore` usa armazenamento criptografado para
  credenciais do modem — coerente com guardar segredo local sensível.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Nenhum teste unitário (`src/test`: **1 arquivo**, cobre parcialmente) para um módulo que hoje guarda também credenciais e feature flags, não só preferências simples | Lacuna de teste em superfície sensível (credenciais) | Priorizar teste do `CredenciaisModemStore` e `FeatureFlagStore` ao tocá-los |
| Caminho físico `io/veloo` diverge do package declarado | Ver dívida 4.1 da regra de higiene | Não migrar oportunisticamente |
