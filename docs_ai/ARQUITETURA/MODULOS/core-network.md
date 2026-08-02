# Módulo :coreNetwork

- **Status:** ativo
- **Última validação:** 2026-07-23 (fonte: `android/core/network/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:coreNetwork` (alias legado; pasta física `android/core/network/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Infraestrutura de rede compartilhada — sem dependência de nenhum outro módulo do monorepo. Cresceu
significativamente desde a última auditoria (2026-07-16): além do monitoramento de conectividade e
latência de gateway original, hoje concentra também os **contratos de dispositivo local/gateway**,
o **motor de classificação de topologia** e o **scan/classificação de Wi-Fi**, que antes viviam em
`:featureDevices`/`:featureWifi` ou eram descritos como pertencentes a eles. Namespace declarado:
`io.signallq.app.core.network`.

## Diagrama de componentes

```
:coreNetwork
├── (raiz) — monitoramento de conectividade e latência
│     MonitorRede / MonitorRedeAndroid, SnapshotRede, WifiLinkSnapshot, EstadoConexao,
│     GatewayLatencyMeasurer, NetworkCapabilitiesProvider, DispatcherProvider
├── wifi/            — scan e modelo de rede Wi-Fi (io/signallq/, caminho já correto)
│     ScannerRedesWifi, SnapshotScanWifi, ScanResultAdapter
├── topologia/engine/ — TopologiaRedeEngine (classificação mesh/repetidor/roteador)
├── topologia/oui/    — OuiCatalog (base de fabricantes por MAC)
└── contracts/
      ├── gateway/     — EquipmentClassifier, EquipmentClassification, DeviceDriverCatalog,
      │                  PublicCompatibilityCatalog, GatewayConnectionService, AcessoEquipamento
      ├── localdevice/ — LocalNetworkDeviceSnapshot, ClientSnapshot, WifiSnapshot, LanSnapshot,
      │                  WanSnapshot, FiberSnapshot, DeviceCapabilities, LocalDeviceSafeFilter
      ├── topologia/   — ClassificacaoTopologia, PapelTopologia, Evidencia, ConflitoSinal,
      │                  NivelConfianca
      ├── wifi/        — RedeVizinha, SegurancaWifi, channel/ChannelEvaluator (avaliação de canal)
      ├── oui/         — OuiEntry, NivelValidacaoOui, EspecificidadeOui
      └── fibra/       — ClassificadorSaudeGpon, GponSaudeStatus
```

`:featureWifi` e `:featureFibra` hoje reexportam alguns desses tipos via `typealias` (comentário no
código: "Tipo movido para coreNetwork/contracts — mantido aqui como typealias para não quebrar
imports existentes") para não quebrar consumidores de `:app` — ver `docs_ai/ARQUITETURA/MODULOS/
feature-wifi.md` e `feature-fibra.md`.

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `MonitorRede.kt` / `MonitorRedeAndroid.kt` | Interface/Impl | Contrato e implementação (`ConnectivityManager.NetworkCallback`) de monitoramento de conectividade |
| `SnapshotRede.kt` | Data class | Estado atual da rede |
| `WifiLinkSnapshot.kt` | Data class | Snapshot do link Wi-Fi (RSSI, canal, freq, link speed) |
| `EstadoConexao.kt` | Enum | wifi, movel, cabo, desconhecido |
| `GatewayLatencyMeasurer.kt` | Utilitário | RTT TCP do gateway local |
| `NetworkCapabilitiesProvider.kt` | Interface | Acesso a `NetworkCapabilities` |
| `DispatcherProvider.kt` / `DefaultDispatcherProvider.kt` | Interface/Impl | Abstração de Dispatchers para testabilidade |
| `wifi/ScannerRedesWifi.kt` | Serviço | Scan de redes Wi-Fi vizinhas |
| `wifi/SnapshotScanWifi.kt` | Data class | Estado do scan Wi-Fi |
| `topologia/engine/TopologiaRedeEngine.kt` | Engine | Classifica topologia (roteador/mesh/repetidor) a partir do scan |
| `topologia/oui/OuiCatalog.kt` | Object | Base de OUIs de fabricante por MAC |
| `contracts/gateway/EquipmentClassifier.kt` | Engine | Classifica equipamento (roteador/ONT/mesh) por fingerprint |
| `contracts/gateway/DeviceDriverCatalog.kt` | Catálogo | Drivers/suporte por modelo de equipamento conhecido |
| `contracts/localdevice/LocalNetworkDeviceSnapshot.kt` | Data class | Snapshot consolidado de um dispositivo local (Wi-Fi/LAN/WAN/fibra) |
| `contracts/wifi/channel/ChannelEvaluator.kt` | Engine | Avaliação de canal Wi-Fi (candidatos, congestionamento) |
| `contracts/fibra/ClassificadorSaudeGpon.kt` | Engine | Avalia saúde da ONT (Rx/Tx/temperatura) — contrato consumido por `:featureFibra` |
| `AnalyticsHelper.kt` / `AnalyticsTracker.kt` / `FeatureFlagProvider.kt` | Contrato | Abstrações de analytics/feature flag consumidas por outros módulos |

## Fluxo de dados principal

- **Entradas:** callbacks do `ConnectivityManager`/`WifiManager` do SO; resultado de scan Wi-Fi.
- **Processamento:** classificação de topologia (`TopologiaRedeEngine`), classificação de
  equipamento (`EquipmentClassifier`), avaliação de canal (`ChannelEvaluator`), medição de latência
  de gateway.
- **Saídas:** `StateFlow<SnapshotRede>`, `SnapshotScanWifi`, `LocalNetworkDeviceSnapshot`,
  `ClassificacaoTopologia` — consumidos por `:app` e por `:featureWifi`, `:featureFibra`,
  `:featureSpeedtest`, `:featureDiagnostico` (via grep de `project(":coreNetwork")`).

## Decisões arquiteturais (ADR)

- **Nenhuma dependência de outro módulo do monorepo** — infraestrutura de base. Libs de produção:
  `androidx-core-ktx`, `lifecycle-runtime-ktx`, `kotlinx-coroutines-android`.
- **Consolidação de contratos de topologia/equipamento/dispositivo local aqui, e não em
  `:featureWifi`/`:featureDevices`** — decisão real observada no código (comentário explícito de
  migração, `typealias` de compatibilidade nos módulos de origem), não documentada antes em nenhum
  ADR formal. Racional provável: esses contratos são consumidos por mais de uma feature
  (`:featureWifi`, `:featureFibra`, `:featureDiagnostico`), então pertencem a `core` pela regra 5 da
  higiene ("infraestrutura compartilhada, contratos normalizados"). Recomenda-se abrir um ADR formal
  retroativo se a squad quiser fixar esse racional.
- **Caminho físico misto dentro do mesmo módulo:** subpacote `wifi/` já nasceu em `io/signallq/`,
  mas `contracts/*` e a raiz ainda estão em `io/veloo/app/core/network/...` — ver seção de riscos.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Módulo cresceu de "monitoramento simples" para concentrar 3 domínios (conectividade, topologia, catálogo de equipamento) sem doc atualizada até agora | Onboarding e navegação ficam desalinhados com o código real | Este doc já reflete o estado atual; reler ao tocar o módulo de novo |
| Caminho físico misto `io/veloo` (`contracts/*`, raiz) vs. `io/signallq` (`wifi/`) dentro do mesmo módulo | Duas convenções coexistindo torna a dívida 4.1 mais difícil de fechar de uma vez | Não migrar oportunisticamente — tratar como parte da migração dedicada da regra de higiene 4.1 |
| 13 arquivos em `src/test`, `src/androidTest`: 0, para um módulo que hoje concentra classificação de topologia e equipamento (lógica de decisão real) | Risco de regressão silenciosa em classificadores | Priorizar characterization tests para `TopologiaRedeEngine`/`EquipmentClassifier` ao tocá-los |
