# Módulo :featureFibra

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/feature/fibra/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/fibra/`
- **Namespace:** `io.signallq.app.feature.fibra`

## Responsabilidade

Leitura de dados da ONT GPON Nokia via acesso HTTP direto na rede local.

## Principais packages/pastas

Base: `feature/fibra/src/main/kotlin/io/veloo/app/kotlin/feature/fibra/` (caminho físico legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ExecutorFibra.kt` | Orchestrator | Orquestra leitura de dados da ONT |
| `NokiaModemClient.kt` (`ClienteFibra.kt`) | Cliente HTTP | Acesso ao modem Nokia via rede local |
| `NokiaModemParser.kt` | Parser | Parse do HTML/JSON retornado pelo modem |
| `NokiaModemCrypto.kt` | Utilitário | Criptografia/autenticação proprietária do firmware Nokia |
| `NokiaLocalDeviceMapper.kt` | Mapper | Mapeia dispositivos LAN reportados pela ONT |
| `ClassificadorSaudeGpon.kt` | Engine | Avalia saúde da ONT (Rx/Tx/temperatura) |
| `GponStatus.kt` / `GponSaudeStatus.kt` / `WanStatus.kt` / `LanStatus.kt` / `PppStatus.kt` / `WifiStatus.kt` / `DeviceInfoFibra.kt` | Data class | Modelos de status reportados pela ONT |
| `SnapshotFibra.kt` | Data class | Estado consolidado da leitura |
| `EstadoFibra.kt` | Enum | Estados do fluxo de leitura |

## Entradas/saídas

- **Entradas:** HTTP local contra o modem GPON Nokia na LAN (fora de qualquer backend do produto).
- **Saídas:** `SnapshotFibra` consumido por `:app` (tela Fibra) e por
  `engines/FibraSignalQualityEngine` em `:featureDiagnostico` (via dado já processado, não via
  dependência de módulo direta).

## Dependências declaradas (build.gradle.kts real)

`:coreNetwork`. Libs: `androidx-core-ktx`, `kotlinx-coroutines-android`, `timber`.

## Consumidores

Via grep de `project(":featureFibra")`: apenas `:app`.

## Testes existentes

`src/test`: **3 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Esquema de autenticação proprietário por firmware (`NokiaModemCrypto.kt`) é o ponto mais frágil do
módulo — qualquer mudança de firmware da ONT pode quebrar silenciosamente sem que o app tenha como
detectar de outra forma senão falha de parsing. Caminho físico `io/veloo` diverge do package —
dívida 4.1.
```

