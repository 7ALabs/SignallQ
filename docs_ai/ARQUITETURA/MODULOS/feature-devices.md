# Módulo :featureDevices

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/feature/devices/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureDevices` (alias legado; pasta física `android/feature/devices/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Descoberta e classificação de dispositivos na rede local (ARP, mDNS, port scan, UPnP/SSDP).
Namespace declarado: `io.signallq.app.feature.devices`.

## Diagrama de componentes

```
:featureDevices (io/veloo/ — caminho legado)
ScannerDispositivos (interface) → ScannerDispositivosAndroid (impl: ARP + mDNS + port scan + UPnP)
    ├── ClassificadorDispositivoRede — classifica por tipo (roteador, TV, celular…)
    ├── CorrelacaoTopologiaDispositivo — correlaciona topologia com dispositivo
    ├── DispositivosIdentidadeHelper / NivelConfiancaIdentidade — resolução de identidade
    ├── XmlDescricaoUpnpParser — parse do XML de descrição UPnP/SSDP
    └── MacAddressUtil, OuiDatabase — apoio a identificação de fabricante por MAC
DevicesViewModel — state holder da tela de Dispositivos
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `ScannerDispositivos.kt` (interface) / `ScannerDispositivosAndroid.kt` (impl) | Serviço | Descoberta de dispositivos — ARP + mDNS + port scan |
| `ClassificadorDispositivoRede.kt` | Engine | Classifica dispositivos por tipo (roteador, TV, celular, etc.) |
| `CorrelacaoTopologiaDispositivo.kt` | Engine | Correlaciona topologia com dispositivo |
| `DispositivosIdentidadeHelper.kt` / `NivelConfiancaIdentidade.kt` | Utilitário/Enum | Resolução de identidade de dispositivo e nível de confiança |
| `XmlDescricaoUpnpParser.kt` | Parser | Parse do XML de descrição UPnP/SSDP |
| `MacAddressUtil.kt` | Utilitário | Normalização/validação de endereço MAC |
| `NamingPrioridade.kt` | Enum | Prioridade de fontes de nome do dispositivo |
| `DevicesViewModel.kt` | State holder | Estado da tela de Dispositivos |
| `SnapshotScanDispositivos.kt` / `DispositivoRede.kt` / `DispositivoRedeExt.kt` | Data class | Modelo de dispositivo e estado do scan |
| `FeatureDevicesModulo.kt` | Object | Fábrica estática |

## Fluxo de dados principal

- **Entradas:** varredura ativa da rede local (ARP/mDNS/port scan/UPnP), apelidos persistidos
  (`:coreDatabase`).
- **Saídas:** `SnapshotScanDispositivos` consumido por `:app` (tela Dispositivos).

## Decisões arquiteturais (ADR)

- **Dependências de módulo:** `:coreDatabase`, `:coreDatastore`, `:coreNetwork`.
- **Único módulo `feature/*` com Hilt próprio** (`alias(libs.plugins.hilt)` + kapt) — os demais
  `feature/*` que usam Hilt fazem via `:app`.
- **3 libs de terceiros próprias do módulo:** `AndroidNetworkTools` (Apache-2.0, ping nativo + ARP),
  `jmDNS` (Apache-2.0, mDNS/Bonjour), OkHttp 4.12.0 (fetch de XML UPnP/SSDP) — ver
  `THIRD_PARTY_NOTICES.md` para atribuição completa.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Módulo com mais dependências de terceiros do que qualquer outra feature | Risco de auditoria de licença se novas libs forem adicionadas sem checar `THIRD_PARTY_NOTICES.md` | Checar notice ao adicionar lib nova |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |

`src/test`: **12 arquivos** (cresceu frente aos 10 da última auditoria). `src/androidTest`: 0.
