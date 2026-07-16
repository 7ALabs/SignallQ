# Módulo :coreTelephony

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/core/telephony/build.gradle.kts`, código real)
- **Caminho físico:** `android/core/telephony/`
- **Namespace:** `io.signallq.app.core.telephony`

## Responsabilidade

Monitoramento de sinal móvel (4G/5G) via `TelephonyManager`.

## Principais packages/pastas

Base: `coreTelephony/src/main/kotlin/io/veloo/app/kotlin/core/telephony/` (caminho físico legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `MonitorTelephony.kt` | Serviço | Produz `MovelSnapshot` — só ativo com permissão concedida |
| `MovelSnapshot.kt` | Data class | RSRP, RSRQ, SINR, tecnologia, banda, operadora |

## Entradas/saídas

- **Entradas:** callbacks do `TelephonyManager` do SO.
- **Saídas:** `MovelSnapshot` consumido por `:app` (tela Sinal) e `:featureSpeedtest` (contexto de
  rede móvel durante o teste).

## Dependências declaradas (build.gradle.kts real)

Nenhum módulo do monorepo. Libs: `androidx-core-ktx`, `kotlinx-coroutines-android`, `timber`.

## Consumidores

Via grep de `project(":coreTelephony")`: `:app`, `:featureSpeedtest`.

## Testes existentes

`src/test`: **2 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Caminho físico `io/veloo` diverge do package — dívida 4.1.
```

