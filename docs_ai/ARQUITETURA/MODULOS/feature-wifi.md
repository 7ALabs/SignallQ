# Módulo :featureWifi

- **Status:** ativo (módulo hoje fino — a maior parte do domínio migrou para `:coreNetwork`)
- **Última validação:** 2026-07-23 (fonte: `android/feature/wifi/build.gradle.kts`, código real)
- **Fonte de verdade:** código real do módulo — em caso de divergência, vale o código
- **Escopo:** módulo Gradle `:featureWifi` (alias legado; pasta física `android/feature/wifi/`)
- **Responsável:** Camilo (dono da implementação Android), squad SignallQ mantém

## Visão geral

Módulo hoje **fino**: correção factual relevante desde a última auditoria (2026-07-16) — as classes
antes descritas aqui (`ScannerRedesWifi`, `SnapshotScanWifi`, `TopologiaWifiEngine`,
`MeshOuiDatabase`) **não existem mais neste módulo**. O código real migrou para
`io.signallq.app.core.network.wifi`/`.topologia` em `:coreNetwork` (comentário no próprio código-
fonte: "Tipo movido para coreNetwork/contracts — mantido aqui como typealias para não quebrar
imports existentes"). `:featureWifi` hoje contém só a fábrica, o agrupamento de topologia local
(`GrupoRedeWifi`/`RedeClassificada`) e o resumo textual da conexão Wi-Fi. Namespace declarado:
`io.signallq.app.feature.wifi`.

## Diagrama de componentes

```
:featureWifi (fino)
FeatureWifiModulo — fábrica: criarScannerRedesWifi() delega para ScannerRedesWifi (:coreNetwork)
GrupoRedeWifi — TipoTopologia, ConfiancaTopologia, RedeClassificada (agrupamento local)
MontarResumoWifiUseCase — monta ResumoWifi a partir de SnapshotRede (:coreNetwork)
RedeVizinha.kt — typealias para io.signallq.app.core.network.contracts.wifi.{RedeVizinha,SegurancaWifi}
```

## Componentes em detalhe

| Componente | Tipo | Responsabilidade |
|---|---|---|
| `FeatureWifiModulo.kt` | Object | Fábrica — `criarScannerRedesWifi()` delega para `ScannerRedesWifi` de `:coreNetwork` |
| `GrupoRedeWifi.kt` | Enum/Data class | `TipoTopologia` (ROTEADOR, ROTEADOR_MESH, NO_MESH, REPETIDOR, PONTO_DE_ACESSO, DESCONHECIDO), `ConfiancaTopologia`, `RedeClassificada` — agrupamento local de redes vizinhas por topologia |
| `MontarResumoWifiUseCase.kt` | Use case | Monta `ResumoWifi` (título/detalhe textual) a partir de `SnapshotRede` |
| `ResumoWifi.kt` | Data class | Resultado textual do use case acima |
| `RedeVizinha.kt` | `typealias` | Reexporta `RedeVizinha`/`SegurancaWifi` de `:coreNetwork.contracts.wifi` — compatibilidade de import |

## Fluxo de dados principal

- **Entradas:** `SnapshotRede`/`ScannerRedesWifi` (ambos hoje implementados em `:coreNetwork`,
  apenas invocados via fábrica deste módulo).
- **Saídas:** `ResumoWifi`, `RedeClassificada` consumidos por `:app` (tela Sinal).

## Decisões arquiteturais (ADR)

- **Depende só de `:coreNetwork`.** Libs: `androidx-core-ktx`, `kotlinx-coroutines-android`,
  `timber`.
- **Migração de domínio para `:coreNetwork` não documentada em ADR formal** — observada apenas pelo
  comentário no código-fonte e pela ausência das classes antes descritas neste doc. Recomenda-se à
  squad decidir se `:featureWifi` continua existindo como módulo fino (fábrica + agrupamento local)
  ou se deveria ser absorvido por `:coreNetwork`/`:app`, já que hoje tem responsabilidade mínima
  própria.

## Riscos e mitigação

| Risco | Impacto | Mitigação |
|---|---|---|
| Módulo com responsabilidade própria mínima após a migração — risco de virar "módulo esqueleto" como `:featureHome`/`:featureSettings` eram antes de ganharem lógica própria | Ambiguidade sobre se o módulo ainda se justifica como `feature` separada | Decisão de squad: manter fino (fronteira de composição) ou consolidar |
| `src/test`: **0 arquivos** (antes 5 — os testes de `TopologiaWifiEngine`/`ScannerRedesWifi` provavelmente migraram junto com o código para `:coreNetwork`, que também está sem teste próprio desses componentes) | Nenhuma cobertura de teste dedicada ao agrupamento de topologia (`GrupoRedeWifi`) nem em `:featureWifi` nem confirmadamente em `:coreNetwork` | Confirmar onde ficou (ou não) a cobertura de teste da classificação de topologia Wi-Fi; adicionar se realmente não existe mais em lugar nenhum |
| Caminho físico `io/veloo` diverge do package declarado | Dívida 4.1 da regra de higiene | Não migrar oportunisticamente |
