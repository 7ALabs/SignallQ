# Módulo :featureDevices

- **Status:** ativo
- **Última validação:** 2026-07-16 (fonte: `android/feature/devices/build.gradle.kts`, código real)
- **Caminho físico:** `android/feature/devices/`
- **Namespace:** `io.signallq.app.feature.devices`

## Responsabilidade

Descoberta e classificação de dispositivos na rede local (ARP, mDNS, port scan, UPnP/SSDP).

## Principais packages/pastas

Base: `feature/devices/src/main/kotlin/io/veloo/app/kotlin/feature/devices/` (caminho físico
legado).

## Classes/contratos públicos relevantes

| Arquivo | Tipo | Responsabilidade |
|---|---|---|
| `ScannerDispositivos.kt` (interface) / `ScannerDispositivosAndroid.kt` (impl) | Serviço | Descoberta de dispositivos — ARP + mDNS + port scan |
| `ClassificadorDispositivoRede.kt` | Engine | Classifica dispositivos por tipo (roteador, TV, celular, etc.) |
| `OuiDatabase.kt` | Object | Base de OUIs para identificação de fabricante por MAC |
| `DevicesViewModel.kt` | State holder | Estado da tela de Dispositivos |
| `CorrelacaoTopologiaDispositivo.kt` | Engine | Correlaciona topologia com dispositivo |
| `DispositivosIdentidadeHelper.kt` | Utilitário | Resolução de identidade de dispositivo |
| `XmlDescricaoUpnpParser.kt` | Parser | Parse do XML de descrição UPnP/SSDP |
| `NamingPrioridade.kt` | Enum | Prioridade de fontes de nome do dispositivo |
| `FeatureDevicesModulo.kt` | Object | Fábrica estática |

## Entradas/saídas

- **Entradas:** varredura ativa da rede local (ARP/mDNS/port scan/UPnP), apelidos persistidos.
- **Saídas:** `SnapshotScanDispositivos` consumido por `:app` (tela Dispositivos).

## Dependências declaradas (build.gradle.kts real)

`:coreDatabase`, `:coreDatastore`, `:coreNetwork`. Único módulo `feature/*` com Hilt próprio
(`alias(libs.plugins.hilt)` + kapt). Libs de terceiros: `AndroidNetworkTools` (Apache-2.0, ping
nativo + ARP), `jmDNS` (Apache-2.0, mDNS/Bonjour), OkHttp 4.12.0 (fetch de XML UPnP/SSDP) — ver
`THIRD_PARTY_NOTICES.md` para atribuição completa.

## Consumidores

Via grep de `project(":featureDevices")`: apenas `:app`.

## Testes existentes

`src/test`: **10 arquivos**. `src/androidTest`: 0.

## Riscos/dívidas conhecidas

Módulo com mais dependências de terceiros do que qualquer outra feature (3 libs externas além das
plataformas padrão) — risco de auditoria de licença se novas libs forem adicionadas sem checar
`THIRD_PARTY_NOTICES.md`. Caminho físico `io/veloo` diverge do package — dívida 4.1.
```

