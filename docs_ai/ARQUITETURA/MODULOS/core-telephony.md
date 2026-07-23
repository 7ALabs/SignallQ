# Módulo :coreTelephony

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/core/telephony/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:coreTelephony` (alias legado; pasta física `android/core/telephony/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Monitoramento de sinal móvel (4G/5G) via `TelephonyManager`. Namespace declarado:
`io.signallq.app.core.telephony`.

## Diagrama de componentes

```
:coreTelephony (io/veloo/ — caminho legado)
MonitorTelephony (interface) → MonitorTelephonyImpl (impl)
    ↳ MovelSnapshot (sinal), MovelSimSnapshot (dados do SIM/operadora)
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `MonitorTelephony.kt` | Interface | Contrato de monitoramento de sinal móvel |
| `MonitorTelephonyImpl.kt` | Implementação | Produz `MovelSnapshot` via `TelephonyManager` — só ativo com permissão concedida |
| `MovelSnapshot.kt` | Data class | RSRP, RSRQ, SINR, tecnologia, banda, operadora |
| `MovelSimSnapshot.kt` | Data class | Dados do SIM/operadora (separado do snapshot de sinal) |

## Fluxo de dados principal

- **Entradas:** callbacks do `TelephonyManager` do SO.
- **Saídas:** `MovelSnapshot` consumido por `:app` (tela Sinal) e `:featureSpeedtest` (contexto de
  rede móvel durante o teste) — confirmado via grep de `project(":coreTelephony")`.

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo.** Libs: `androidx-core-ktx`,
  `kotlinx-coroutines-android`, `timber`.
- Separação `MonitorTelephony`/`MonitorTelephonyImpl` (interface + impl explícita) segue o mesmo
  padrão dos demais módulos `core` (`MonitorRede`/`MonitorRedeAndroid`, `GerenciadorPermissoesRede`/
  `GerenciadorPermissoesRedeAndroid`) — consistente.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Cobertura de teste baixa (`src/test`: **2 arquivos**) para um domínio com múltiplos estados de rede móvel e dependência de permissão | Regressão silenciosa em classificação de sinal | Ampliar teste ao tocar o módulo |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |
