---
title: "Módulo :featureSettings"
description: "Modelo e validações puras do perfil de conexão por rede, do tema e dos campos de ajustes do app."
type: "técnico"
status: "ativo"
owner: "Camilo"
last_updated: "2026-08-06"
---

# `:featureSettings`

- **Caminho físico:** `android/feature/settings/` (alias flat legado, remapeado por `projectDir` em `android/settings.gradle.kts`)
- **Namespace:** `io.signallq.app.feature.settings`

## Responsabilidade

Biblioteca de regras puras dos Ajustes: modela o `ConnectionProfile` vinculado a uma rede específica, deriva o `networkId` estável a partir de BSSID/SSID/operadora, valida os campos que o usuário preenche (velocidade contratada, cidade/UF) e tipa a preferência de tema. Cada regra é um `object` ou `data class` sem dependência de Android, Hilt, Compose ou DataStore.

Não é dele: a tela de Ajustes (`AjustesScreen.kt` vive em `:app`), a persistência (chaves e leitura/escrita ficam em `:coreDatastore`, consumido por `:app`), a resolução real de SSID/BSSID (quem chama já resolveu via `WifiManager`/`ConnectivityManager`) e qualquer decisão de UI sobre divergência de perfil — o módulo só classifica a situação e devolve o tipo.

## Dependências

Extraídas de `android/feature/settings/build.gradle.kts`.

| Dependência | Configuração | Observação |
|---|---|---|
| `libs.androidx.core.ktx` | `implementation` | |
| `libs.junit` | `testImplementation` | |
| `libs.androidx.junit`, `libs.androidx.espresso.core` | `androidTestImplementation` | |

**Nenhuma dependência de outro módulo do monorepo.** É o módulo mais isolado dos quatro — sem Hilt, sem coroutines, sem OkHttp, sem Room, sem Compose. O kdoc de `ResolvedorNetworkId` registra a decisão explícita: "feature/settings não depende de coreNetwork por enquanto; se essa necessidade crescer, promover para core comum".

## Consumidores

`grep -rn 'project(":featureSettings")' --include=*.kts .`

| Consumidor | Local |
|---|---|
| `:app` | `android/app/build.gradle.kts:319` |

Nenhum outro módulo depende deste.

## Componentes principais

O módulo tem 7 arquivos `main`, somando **203 linhas** de código de produção.

| Arquivo / classe | Responsabilidade |
|---|---|
| `.../settings/ConnectionProfile.kt` (24 linhas) | `data class ConnectionProfile(networkId, providerFixed, contractedDownloadMbps, contractedUploadMbps, city, state, userConfirmed)`. GH#1227 item 3/RF-A — antes provedor e plano eram chaves DataStore globais, então o app aplicava o plano residencial numa rede de trabalho, hotel ou hotspot. `networkId` é o campo que impede isso; `userConfirmed` distingue escolha deliberada do usuário de valor apenas detectado |
| `.../settings/ResolvedorNetworkId.kt` (39 linhas) | `object` puro. `paraWifi(ssid, bssid)` prefere BSSID (prefixo `wifi-bssid:`, minúsculo) e cai para SSID (`wifi-ssid:`) quando o BSSID é ausente ou o placeholder `02:00:00:00:00:00` que o Android devolve sem permissão de localização. `paraRedeMovel(operadoraOuIccid)` usa o prefixo `movel:`. Devolve `null` quando não há sinal estável (ex.: Ethernet) — nunca um id global |
| `.../settings/DetectorDivergenciaPerfilConexao.kt` (46 linhas) | `sealed interface ResultadoDivergenciaPerfilConexao` com 4 casos (`SemBaseParaComparar`, `PerfilCoincide`, `AtualizavelSilenciosamente`, `DivergenciaConfirmadaPeloUsuario`) e o `object` com `avaliar(perfilSalvo, providerDetectado)`. GH#1227 item 2/RF-B — tipado em vez de `Boolean` porque as três situações reais pedem tratamento diferente do chamador (sobrescrever em silêncio × alertar × não fazer nada) |
| `.../settings/ValidadorVelocidadeContratada.kt` (24 linhas) | `ehValida(mbps: Int?)`. `null` é válido (campo vazio explícito); `0` e negativos são inválidos; teto `LIMITE_SUPERIOR_MBPS = 10_000`. GH#1227 item 5/RF-D |
| `.../settings/ValidadorCidadeUf.kt` (33 linhas) | `ehCombinacaoValida(cidade, uf)`: ambos vazios é válido, só um preenchido é inválido, e a UF precisa estar em `UFS_VALIDAS` (as 27 siglas). GH#1249 — a validação de correspondência geográfica real exigiria um catálogo IBGE que não existe no repo, e a limitação está registrada no kdoc |
| `.../settings/ThemePreference.kt` (35 linhas) | `enum ThemePreference { SYSTEM, LIGHT, DARK }` com `chaveDataStore` preservando as strings já gravadas em produção (`"sistema"`/`"claro"`/`"escuro"`) e `parse(raw)` que nunca lança e sempre cai em `SYSTEM`. GH#1227 item 14/RF-I — antes, valor inesperado deixava a UI sem nenhuma opção selecionada |
| `.../settings/FeatureSettingsModulo.kt` (2 linhas) | `object FeatureSettingsModulo` — declaração vazia, sem nenhum membro |

## Riscos e dívidas

- **Path físico alinhado ao package `io.signallq.app.*`** — migração de `io/signallq/app/kotlin/` concluída em 2026-08-15 (#1645).
- **Dependência entre features:** nenhuma. O módulo não depende de nada do monorepo — é o caso mais limpo dos quatro.
- **Regra de negócio em Composable:** não aplicável — 0 `@Composable` no módulo (verificado por grep). Todas as regras são funções puras testáveis.
- **Arquivos acima de 800 linhas:** nenhum. O maior arquivo do módulo inteiro é `src/test/.../DetectorDivergenciaPerfilConexaoTest.kt` com **59 linhas**; o maior de produção é `DetectorDivergenciaPerfilConexao.kt` com **46 linhas**. Todo o módulo soma 424 linhas de Kotlin (203 em `main`, 221 em `test`).
- **Falta de teste:** apenas `FeatureSettingsModulo.kt` e `ConnectionProfile.kt` não têm teste dedicado — e ambos são declarações sem comportamento. Os cinco componentes com lógica (`ResolvedorNetworkId`, `DetectorDivergenciaPerfilConexao`, `ValidadorVelocidadeContratada`, `ValidadorCidadeUf`, `ThemePreference`) têm arquivo de teste correspondente. É o módulo com melhor razão teste/produção dos quatro (221 linhas de teste para 203 de produção).
- **`FeatureSettingsModulo.kt` é um `object` vazio** (2 linhas, sem membros), ao contrário dos homônimos de `:featureFibra` e `:featureHistory`, que expõem fachada real. Código morto ou fachada nunca implementada — remover ou dar conteúdo.
- **O módulo não é uma feature no sentido da §5 da regra de higiene.** Não tem UI, estado, ViewModel nem casos de uso — é um conjunto de regras puras consumido por `:app`. Se o escopo continuar assim, o destino natural é um módulo `core` (o próprio kdoc de `ResolvedorNetworkId` antecipa essa promoção); se receber a tela de Ajustes hoje em `:app` (`AjustesScreen.kt`, 771 linhas, dívida registrada em §4.4), passa a ser feature de fato. Nenhuma das duas direções foi decidida — `não determinado nesta revisão`.
- **`ConnectionProfile` não tem ponto de escrita neste módulo.** A validação existe, mas quem persiste (e se persiste corretamente) está em `:app`/`:coreDatastore`, fora do alcance de qualquer teste deste módulo.
