# Módulo :featureFibra

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/fibra/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureFibra` (alias legado; pasta física `android/feature/fibra/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Leitura de dados da ONT GPON Nokia via acesso HTTP direto na rede local. Namespace declarado:
`io.signallq.app.feature.fibra`. Desde a issue #1213 (auditoria do driver Nokia G-1425G-B), ganhou
um perfil técnico versionado do equipamento e travas de segurança para ações não validadas contra
hardware físico.

## Diagrama de componentes

```
:featureFibra (io/veloo/ — caminho legado)
ExecutorFibra — orquestra leitura de dados da ONT
    ├── NokiaModemClient (ClienteFibra) — acesso HTTP ao modem
    ├── NokiaModemParser — parse do HTML/JSON retornado
    ├── NokiaModemCrypto — criptografia/autenticação proprietária do firmware
    ├── NokiaLocalDeviceMapper — mapeia dispositivos LAN reportados pela ONT
    ├── NokiaG1425GBProfile — perfil técnico versionado do Nokia G-1425G-B (GH#1213)
    ├── ValidadorHostEquipamento — valida host antes de qualquer escrita de preferência (GH#1213)
    └── RebootLabFlags — trava a ação de reboot fora de produção até validação em hardware real (GH#1213)
GponSaudeStatus.kt — typealias para io.signallq.app.core.network.contracts.fibra.GponSaudeStatus
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `ExecutorFibra.kt` | Orchestrator | Orquestra leitura de dados da ONT |
| `NokiaModemClient.kt` (`ClienteFibra.kt`) | Cliente HTTP | Acesso ao modem Nokia via rede local |
| `NokiaModemParser.kt` | Parser | Parse do HTML/JSON retornado pelo modem |
| `NokiaModemCrypto.kt` | Utilitário | Criptografia/autenticação proprietária do firmware Nokia |
| `NokiaLocalDeviceMapper.kt` | Mapper | Mapeia dispositivos LAN reportados pela ONT |
| `NokiaG1425GBProfile.kt` | Perfil | Faixas técnicas versionadas do Nokia G-1425G-B (GPON classe B+, ITU-T G.984.2 Amendment 1) — valores citam issue #1213/Product Guide do fabricante, não arredondar sem atualizar a referência |
| `ValidadorHostEquipamento.kt` | Validador | Valida `modemHost` antes de qualquer escrita futura de preferência (dívida fechada preventivamente pela issue #1213) |
| `RebootLabFlags.kt` | Flags | Mantém a ação de reboot do Nokia G-1425G-B indisponível em produção até validação contra hardware físico real |
| `GponStatus.kt` / `WanStatus.kt` / `LanStatus.kt` / `PppStatus.kt` / `WifiStatus.kt` / `DeviceInfoFibra.kt` | Data class | Modelos de status reportados pela ONT |
| `SnapshotFibra.kt` | Data class | Estado consolidado da leitura |
| `EstadoFibra.kt` | Enum | Estados do fluxo de leitura |
| `GponSaudeStatus.kt` | `typealias` | Reexporta `GponSaudeStatus` de `:coreNetwork.contracts.fibra` — `ClassificadorSaudeGpon` (avaliação de saúde da ONT) hoje vive em `:coreNetwork`, não neste módulo |

## Fluxo de dados principal

- **Entradas:** HTTP local contra o modem GPON Nokia na LAN (fora de qualquer backend do produto).
- **Saídas:** `SnapshotFibra` consumido por `:app` (tela Fibra) e por
  `FibraSignalQualityEngine` (hoje em `:core:diagnostico`, ver `feature-diagnostico.md`) via dado já
  processado, não via dependência de módulo direta.

## Decisões arquiteturais (ADR)

- **Dependência de módulo:** `:coreNetwork` (de onde reaproveita `ClassificadorSaudeGpon`/
  `GponSaudeStatus` via typealias). Libs: `androidx-core-ktx`, `kotlinx-coroutines-android`, `timber`.
- **Trava de segurança por flag para ação não validada (GH#1213):** decisão explícita de manter a
  ação de reboot indisponível em produção até validação em hardware físico real — critério de
  aceite formal da issue, não best-effort.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Esquema de autenticação proprietário por firmware (`NokiaModemCrypto.kt`) | Qualquer mudança de firmware da ONT pode quebrar silenciosamente, só detectável por falha de parsing | Mantido como ponto frágil conhecido — sem mitigação automática hoje |
| Ação de reboot não validada contra hardware físico | Risco de comportamento desconhecido/dano ao equipamento se liberada prematuramente | `RebootLabFlags` mantém a ação fora de produção até validação real (GH#1213) |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |

`src/test`: **7 arquivos** (cresceu frente aos 3 da última auditoria). `src/androidTest`: 0.
