---
title: "Módulo :featureWifi"
description: "Resumo textual do estado da conexão Wi-Fi e vocabulário de topologia (mesh, repetidor, AP) usado pela tela Sinal."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-19"
---

# `:featureWifi`

- **Caminho físico:** `android/feature/wifi/` (alias flat legado)
- **Namespace:** `io.signallq.app.feature.wifi`

## Responsabilidade

Traduz o `SnapshotRede` de `:coreNetwork` em um resumo apresentável do estado da conexão (`MontarResumoWifiUseCase` → `ResumoWifi`: título + detalhe por tipo de conexão) e expõe o vocabulário de topologia Wi-Fi consumido pela tela Sinal (`TipoTopologia`, `ConfiancaTopologia`, `RedeClassificada`, `GrupoRedeWifi`). Também reexporta, via `typealias`, os contratos `RedeVizinha`/`SegurancaWifi` que já migraram para `coreNetwork/contracts`.

Não é dele: varrer redes Wi-Fi (isso é `ScannerRedesWifi`, de `:coreNetwork` — o módulo só oferece uma factory que a instancia), classificar topologia de fato (`TopologiaRedeEngine`, também em `:coreNetwork`), renderizar a tela Sinal, nem gerenciar permissões de localização.

## Dependências

Extraídas de `android/feature/wifi/build.gradle.kts`.

| Tipo | Dependência | Observação |
|---|---|---|
| Plugin | `com.android.library` | — |
| Plugin | `org.jetbrains.kotlin.android` | — |
| `implementation` | `project(":coreNetwork")` | `SnapshotRede`, `EstadoConexao`, `ScannerRedesWifi`, contratos de `wifi` |
| `implementation` | `libs.androidx.core.ktx` | — |
| `implementation` | `libs.kotlinx.coroutines.android` | — |
| `implementation` | `libs.timber` | log |
| `testImplementation` | `libs.junit` | declarado, mas não há `src/test` no módulo |
| `androidTestImplementation` | `libs.androidx.junit`, `libs.androidx.espresso.core` | não há `src/androidTest` |

Sem Hilt e sem Compose: o wiring é feito por `FeatureWifiModulo` (factories manuais) e consumido pelo `AppModule` do `:app`.

## Consumidores

`grep` por `project(":featureWifi")` em `android/**/build.gradle.kts`:

| Consumidor | Arquivo |
|---|---|
| `:app` | `android/app/build.gradle.kts:312` |

No código do `:app`, os tipos do módulo aparecem em `di/AppModule.kt`, `ui/screen/AppShellState.kt`, `ui/screen/HomeScreen.kt`, `ui/screen/SinalWifiSection.kt`, `ui/screen/SinalCanalSection.kt` e `ui/screen/SinalTopologiaHelpers.kt` (issue #1660 extraiu a aba Wi-Fi/Canal do antigo `SinalScreen.kt` monolítico para esses dois arquivos).

## Componentes principais

| Arquivo / classe | Linhas | Responsabilidade |
|---|---|---|
| `android/feature/wifi/src/main/kotlin/io/signallq/app/kotlin/feature/wifi/MontarResumoWifiUseCase.kt` | 46 | Mapeia `EstadoConexao` (wifi/móvel/ethernet/desconectado/desconhecido) em título + detalhe; monta a string técnica `ssid=… bssid=… rssi=… link=… freq=…`. |
| `android/feature/wifi/src/main/kotlin/io/signallq/app/kotlin/feature/wifi/GrupoRedeWifi.kt` | 24 | `TipoTopologia` (roteador, roteador mesh, nó mesh, repetidor, ponto de acesso, desconhecido), `ConfiancaTopologia`, `RedeClassificada`, `GrupoRedeWifi`. |
| `android/feature/wifi/src/main/kotlin/io/signallq/app/kotlin/feature/wifi/FeatureWifiModulo.kt` | 11 | Factories: `criarMontarResumoWifiUseCase()` e `criarScannerRedesWifi(context)` (delega a `:coreNetwork`). |
| `android/feature/wifi/src/main/kotlin/io/signallq/app/kotlin/feature/wifi/ResumoWifi.kt` | 7 | Data class de saída (`titulo`, `detalhe`). |
| `android/feature/wifi/src/main/kotlin/io/signallq/app/kotlin/feature/wifi/RedeVizinha.kt` | 5 | Apenas `typealias` para `io.signallq.app.core.network.contracts.wifi.{RedeVizinha, SegurancaWifi}` — compatibilidade de imports após a migração para `coreNetwork`. |
| `android/feature/wifi/src/main/AndroidManifest.xml` | — | `<manifest />` vazio. |

Total de Kotlin no módulo: 93 linhas, todas em `src/main`.

## Riscos e dívidas

- **Zero testes.** O módulo declara `testImplementation(libs.junit)` mas **não possui diretório `src/test`**. `MontarResumoWifiUseCase` é lógica pura, 100% testável, e está descoberta.
- **Regra de negócio dentro de Composable, no `:app`.** O agrupamento e a classificação de redes que dão sentido a `GrupoRedeWifi`/`RedeClassificada` continuam montados no `:app`, dentro de `android/app/src/main/kotlin/io/signallq/app/ui/screen/SinalWifiSection.kt` (**1110 linhas**) — inclusive a construção literal de `RedeClassificada(..., TipoTopologia.DESCONHECIDO, ConfiancaTopologia.BAIXA, motivo = "")`. O módulo `:featureWifi` fornece só os tipos; a decisão vive na tela. A issue #1660 (épico #1647) só reorganizou o arquivo monolítico `SinalScreen.kt` (era 3383 linhas) em scaffold + `SinalWifiSection.kt`/`SinalCanalSection.kt`/`SinalMovelSection.kt` — não moveu regra de negócio pra `:featureWifi`, isso segue fora de escopo desta fatia. `SinalTopologiaHelpers.kt` (191 linhas) também mora no `:app`.
- **Desequilíbrio de massa:** 93 linhas no módulo contra ~3400 linhas de telas Wi-Fi/Canal/Móvel no `:app`. Mesma inconsistência de `:featureHome`.
- **Path físico alinhado ao package `io.signallq.app.*`** — migração de `io/signallq/app/kotlin/` concluída em 2026-08-15 (#1645).
- **Regra de dependência entre features: respeitada.** Nenhum `project(":feature…")` no `build.gradle.kts`; a única dependência de projeto é `:coreNetwork`.
- Nenhum arquivo acima de 800 linhas dentro do módulo.
