---
title: "Módulo :featureHome"
description: "Regra pura de escolha da medição exibida na tela Início — sem UI, sem I/O e sem dependência de outras features."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-06"
---

# `:featureHome`

- **Caminho físico:** `android/feature/home/` (alias flat legado)
- **Namespace:** `io.signallq.app.feature.home`

## Responsabilidade

Concentra a única regra de negócio da tela Início que foi extraída para fora do `:app`: decidir **qual** medição é exibida — a da execução atual ou a última medição salva no histórico — nunca uma mistura das duas (`ResolvedorMedicaoHome`). Trabalha sobre uma struct genérica (`MetricasMedicaoHome`), deliberadamente desacoplada dos tipos de `:featureSpeedtest` e de `:coreDatabase`.

Não é dele: renderizar a tela Início (o `HomeScreen.kt` inteiro vive em `:app`), buscar dados, converter entidades do Room, orquestrar speedtest ou navegação. O módulo não tem nenhum Composable, ViewModel, UiState nem Repository.

## Dependências

Extraídas de `android/feature/home/build.gradle.kts`.

| Tipo | Dependência | Observação |
|---|---|---|
| Plugin | `com.android.library` | módulo de biblioteca Android |
| Plugin | `org.jetbrains.kotlin.android` | — |
| `implementation` | `libs.androidx.core.ktx` | única dependência de runtime |
| `testImplementation` | `libs.junit` | — |
| `androidTestImplementation` | `libs.androidx.junit`, `libs.androidx.espresso.core` | herdado do template; não há teste instrumentado no módulo |

Nenhuma dependência de módulo `:core*` e nenhuma de outra `feature` — é o módulo mais isolado dos cinco.

## Consumidores

`grep` por `project(":featureHome")` em `android/**/build.gradle.kts`:

| Consumidor | Arquivo |
|---|---|
| `:app` | `android/app/build.gradle.kts:311` |

No código, o consumo é feito por `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/HomeMedicaoAdapter.kt` (55 linhas — adapta `ResultadoSpeedtest`/`MedicaoEntity` para `MetricasMedicaoHome`) e por `HomeScreen.kt`.

## Componentes principais

| Arquivo / classe | Linhas | Responsabilidade |
|---|---|---|
| `android/feature/home/src/main/kotlin/io/veloo/app/kotlin/feature/home/ResolvedorMedicaoHome.kt` → `ResolvedorMedicaoHome` | 65 | Escolhe entre medição atual e anterior de forma atômica; nunca combina campos de execuções diferentes. |
| mesmo arquivo → `MetricasMedicaoHome` | — | Struct genérica de entrada (download, upload, latência, jitter, perda, timestamp, `connectionType`, ssid, veredito gamer, gargalo, flag `utilizavel`). |
| mesmo arquivo → `ResolvedHomeMeasurement` / `OrigemMedicaoHome` | — | Saída com a origem explícita (`ATUAL` / `ANTERIOR`) para a UI rotular "Resultado anterior · Wi-Fi · há 2h". |
| `android/feature/home/src/main/kotlin/io/veloo/app/kotlin/feature/home/FeatureHomeModulo.kt` | 2 | `object FeatureHomeModulo` vazio — placeholder de factory do módulo, sem membros. |
| `android/feature/home/src/test/kotlin/io/veloo/app/kotlin/feature/home/ResolvedorMedicaoHomeTest.kt` | 78 | Único teste do módulo. |
| `android/feature/home/src/main/AndroidManifest.xml` | — | `<manifest />` vazio. |

Total de Kotlin no módulo: 145 linhas (67 em `src/main`, 78 em `src/test`).

## Riscos e dívidas

- **Módulo quase vazio versus tela gigante no `:app`.** `HomeScreen.kt` tem **2967 linhas** em `android/app/src/main/kotlin/io/veloo/app/kotlin/ui/screen/HomeScreen.kt`, enquanto `:featureHome` inteiro tem 67 linhas de produção. A feature "Início" não mora no módulo `:featureHome` — mora no `:app`. É a inconsistência arquitetural mais visível deste módulo: o nome promete uma feature, o conteúdo entrega um utilitário.
- **Caminho legado `io/veloo`.** O diretório físico é `src/main/kotlin/io/veloo/app/kotlin/feature/home/`, mas o `package` declarado é `io.signallq.app.feature.home` e o namespace do Gradle também. Pasta e pacote divergentes em 100% dos arquivos.
- **`FeatureHomeModulo` é código morto** (`object` sem membros, 2 linhas). Ou ganha as factories do módulo, ou é removido.
- **Regra de dependência entre features: respeitada.** O KDoc de `ResolvedorMedicaoHome` documenta explicitamente que a struct genérica existe porque `feature/home → feature/speedtest` é proibido, e a adaptação dos tipos reais foi empurrada para o `:app` (`HomeMedicaoAdapter.kt`). É o exemplo correto do padrão no repositório.
- **Cobertura de teste:** adequada para o que existe (1 arquivo de teste para 1 arquivo de regra).
- Nenhum arquivo acima de 800 linhas dentro do módulo.
