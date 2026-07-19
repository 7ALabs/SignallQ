# SignallQ Pro — Arquitetura e Reaproveitamento de Código (v1)

**Status:** ativo · **Versão:** 1.1 · **Data:** 18/07/2026 · **Tipo:** planejamento técnico (não é implementação)

**Atualização v1.1 (mesma data):** os dois pendentes que dependiam de acesso a repositório externo (`linka-speedtest`, `signallq-isp`) foram fechados — o acesso já existia via `gh` CLI (conta `gmmattey`, escopo `repo` completo, `admin: true` nos dois), não era bloqueio de permissão. Camilo clonou os dois em pasta temporária (fora de `C:/Projetos/`) e leu o código real. Achados incorporados em §3.5 e §6.

**Origem e método.** O Luiz pediu um documento de arquitetura técnica do SignallQ Pro *evidence-based*: cada classificação de reaproveitamento precisa apontar pra peça real de código, não pra categoria abstrata. Método usado nesta sessão:

1. Leitura de `00_CANONICO_v5.md`, `01_..._Arquitetura_v5.md`, `02_..._Especificacao_Tecnica_v5.md`, `08/09/11_SignallQ_Pro_*_v5.md`, `12_..._Auditoria_Cobertura_Repositorios_2026-07-18.md`.
2. Coleta de evidência fresca no código real (`android/`) via agente de exploração — arquivo, linha, módulo Gradle, dependência de `android.*` e teste correspondente para cada motor citado pelo doc 12.
3. Consulta técnica ao **Camilo** (dev único do squad, Android+Admin+Cloudflare) sobre o escopo real de cada extração proposta — respostas incorporadas na seção 4 e refletidas nas classificações da seção 3.
4. Leitura direta do protótipo real ("SignallQ Pro - Protótipos.dc.html", projeto Claude Design `69e53070-6aa8-485a-8d0a-5bfa36e1a08c`) para o mapa de módulos × telas da seção 5.

**Fonte de nomes e decisões transversais:** `00_CANONICO_v5.md`. Em qualquer divergência, o canônico prevalece.

**Fora de escopo desta sessão:** nenhuma linha de Kotlin foi escrita ou editada. Não decide preço, domínio do portal ou provedor de identidade. Não migra de fato para o monorepo `signallq-platform` — só decide se esperamos essa migração ou não (seção 1).

---

## Estado atual vs. Alvo

**Atualização 2026-07-19 (Camilo, issue #1157):** Fase 0 (esqueleto `:pro:app`) e Fase 1 completa
(1a domínio de diagnóstico, 1b engine de PDF, 1c adapters Wi-Fi) implementadas e mergeadas via PR
#1159. Fase 1a — a de maior risco real, motor de diagnóstico do consumidor que entra em produção
em 07/08 — só foi executada após a Claudete autorizar explicitamente com testes de caracterização
end-to-end escritos e verdes ANTES de mover qualquer arquivo do núcleo, e reconfirmados verdes
depois (mesma asserção, zero mudança de comportamento). Tabela abaixo atualizada linha a linha.

| Componente | Estado | Fato verificado nesta sessão |
|---|---|---|
| SignallQ Pro (app) | ✅ **Fase 0 pronta** | Módulo `:pro:app` existe (`android/pro/app/`), `io.signallq.pro`, Compose/Hilt/M3 mínimo, NavHost com rota placeholder. `assembleDebug`/`ktlintCheck`/`detekt` verdes. PR #1159. |
| Motor de causa raiz (`FindingEngine` + domínio de diagnóstico) | ✅ **Fase 1a pronta** — migrado para `:core:diagnostico` (novo módulo) | Closure real de **29 arquivos** (engines + `DiagnosticInput/Report/Result/Status` + `DadoCanal`/`NivelCongestionamento`/`SnapshotEspectroCanal` + topology/model+correlation+internet) + 17 testes movidos. Único acoplamento circular real — `DiagnosticRunner.run()` chamava `RecommendationEngine.recomendar()` direto — resolvido por inversão de dependência (parâmetro `gerarRecomendacoes`, default vazio seguro; único chamador de produção `RemoteDiagnosticRepository` passa `RecommendationEngine::recomendar` explícito). ~70 arquivos com import mecanicamente atualizado em `:featureDiagnostico` e `:app`. Teste de caracterização (`DiagnosticRunnerCaracterizacaoTest.kt`) escrito antes da extração, 5/5 verde antes e depois — zero regressão de comportamento confirmada. |
| Amostragem estatística de speedtest | ✅ **ATUAL**, preso em `:featureSpeedtest` | `AnalisadorAmostragemPing.kt` puro e trivial de mover; `executarModoTriplo` é método privado com estado mutável dentro de uma classe de 1431 linhas — não trivial (ver §3.2). Não tocado nesta rodada (fora do escopo da issue #1157). |
| Adapters Wi-Fi (scan/canal) | ✅ **Fase 1c pronta** — migrado para `:coreNetwork` | `ScannerRedesWifi`, `ScanResultAdapter` (`ScanResult.toNeighbor()`) e `SnapshotScanWifi`/`EstadoScanWifi` movidos para `io.signallq.app.core.network.wifi` (PR #1159). Achado: `toNeighbor()` é código morto — `WifiChannelDiagnosticEngine` usa mapper próprio, nunca chama essa extensão; movido como estava (doc pediu mover, não remover). `RedeVizinha`/`ChannelEvaluator`/`ChannelCandidates`/`ChannelEvalModels`/`FrequencyUtils` já estavam em `:coreNetwork` desde antes (typealias/delegate em `:featureWifi`). |
| Engine de PDF (laudo) | ✅ **Fase 1b pronta** — migrado para `:core:relatorio` (novo módulo) | `PdfPrintHelper.kt` movido como estava (zero acoplamento). Motor de paginação WebView extraído para `exportarHtmlComoPdf(html, arquivo, context)` — assinatura trocada de `medicoes: List<MedicaoEntity>` para `html: String`, exatamente como o plano previa. `ExportadorHistoricoPDF.exportarComWebView()` em `:featureHistory` virou wrapper fino (gera HTML + delega). `gerarHtml()` NÃO migrou (REWRITE fica pra Fase 3 do Pro, layout de laudo é outro). |
| `coreRecommendation` (monetização) | ✅ **ATUAL**, módulo próprio | Não confundir com `RecommendationEngine.kt` de `featureDiagnostico` (14 regras REC-01..14 de causa-raiz). Ver §3.1 nota. |
| Módulo/repositório do Pro | 🎯 **ALVO** — decisão registrada nesta sessão | Nasce dentro do `linka-android` atual, não espera o monorepo `signallq-platform` (ver §1). |
| `signallq-isp` (`ChamadoCanônico`) | ✅ **ATUAL**, repo privado `7AgentsStudio/signallq-isp` | Auditado com evidência real (`src/shared/chamado.ts`, `diagnostico.ts`, `functions/api/erp/chamado.ts`) — idempotência confirmada forte; "versionado" era leitura imprecisa (é tolerant-reader aditivo, sem `schema_version`). Ver §3.5. |
| `linka-speedtest` (plugins Capacitor) | ✅ **ATUAL**, clonado temporariamente e auditado | `PacketLossPlugin.java` mede perda real via UDP — capacidade que o motor Kotlin nativo **não tem hoje** (hoje é estimativa por timeout HTTP). `LinkaWifiDiagnosticsPlugin.java` é redundante. Ver §3.5. |
| Nethal (fingerprint/capability) | 🎯 **ALVO**, repo `gmmattey/nethal` | QUARANTINE confirmado pelo doc 12 (zero driver estável, ~6 modelos). Não integrar no MVP1. |

---

## 1. Onde o código do Pro vive

### 1.1 Decisão

**O SignallQ Pro nasce como módulo(s) Gradle novo(s) dentro do `linka-android` existente — não espera a migração para o monorepo `signallq-platform`.** Confirmo a recomendação do Luiz, com o motivo técnico agora reforçado pela consulta ao Camilo:

- O doc `02_..._Especificacao_Tecnica_v5.md` (§14.2) define a migração para `signallq-platform` em **10 fases (0-9)**, sendo que a Fase 4 ("Pro Shell") só começa depois das Fases 0-3 (Fundação, Import Android, Shared Core) estarem prontas. Esperar isso é esperar um programa de semanas antes de a primeira tela do Pro sequer compilar.
- A squad é enxuta e única (5 agentes, sem squad Pro dedicada — `CLAUDE.md`, "Produtos e Superfícies": *"não 'contratar' agentes novos por ora; derivar squad Pro dedicada só quando os roadmaps consumer e Pro rodarem em paralelo de verdade"*). Um programa de migração de monorepo em paralelo ao lançamento em produção do consumer (07/08/2026) não é uma aposta sensata de foco.
- A extração necessária (domínio de diagnóstico, adapters Wi-Fi, engine de PDF — §3) é, pela avaliação do Camilo, majoritariamente mecânica dentro da estrutura Gradle **já existente**: mover pacotes para módulos `:core*` novos, sem precisar da árvore `mobile/core/*` do monorepo-alvo para funcionar. A regra de ouro "feature não depende de feature" (`arquitetura-android` skill) já é satisfeita por módulos `:core*` dentro do projeto Gradle atual.
- Regra 8 do `01_..._Arquitetura_v5.md` (*"`android-consumer` e `android-pro` nunca dependem um do outro"*) é satisfeita igualmente bem dentro de um único `settings.gradle.kts`: dois módulos de app irmãos, ambos dependendo só de `:core*`, nunca um do outro.

**Gatilho de revisão:** migrar para `signallq-platform` quando (a) os roadmaps consumer e Pro rodarem em paralelo de verdade com squads distintas (mesmo critério já registrado para "contratar" squad Pro), ou (b) o número de módulos `:core*` compartilhados começar a gerar conflito de build/CI entre os dois times. Nenhum dos dois é verdade hoje.

### 1.2 Convenção de nome dos módulos novos

A regra de higiene já registra isto (`.claude/rules/higiene-e-padronizacao-repositorio.md`, §5): os 16 módulos atuais usam alias Gradle concatenado legado (`:coreNetwork`, `:featureWifi`) por compatibilidade, mas **"não criar novos módulos usando o padrão antigo concatenado"** — o padrão desejado para módulo novo é hierárquico com `:` (`:core:network`, `:feature:wifi`).

Isso resolve a nomenclatura dos módulos novos desta arquitetura sem ambiguidade — **todo módulo novo (Pro ou core extraído) nasce hierárquico**, mesmo convivendo no mesmo `settings.gradle.kts` com os 16 módulos antigos flat. Não é inconsistência: é a regra já vigente aplicada ao que é literalmente novo, enquanto os 16 antigos aguardam sua própria migração dedicada (fora de escopo aqui).

| Módulo novo | Alias Gradle | Pasta física | Tipo |
|---|---|---|---|
| App do Pro | `:pro:app` | `android/pro/app/` | novo |
| Domínio de diagnóstico extraído | `:core:diagnostico` | `android/core/diagnostico/` | novo (extração de `:featureDiagnostico`) |
| Engine de relatório/PDF extraído | `:core:relatorio` | `android/core/relatorio/` | novo (extração de `:featureHistory`) |
| Features do Pro | `:pro:feature:auth`, `:pro:feature:cliente`, `:pro:feature:visita`, `:pro:feature:ambiente`, `:pro:feature:medicao-diagnostico`, `:pro:feature:laudo`, `:pro:feature:pagamento`, `:pro:feature:conta`, `:pro:feature:ferramentas` | `android/pro/feature/<nome>/` | novos |

Os adapters de Wi-Fi (`ScannerRedesWifi`, `ScanResultAdapter`) **não** precisam de módulo novo — movem para dentro do `:coreNetwork` já existente (alias antigo, mas destino já existe, não é criação de módulo).

### 1.3 Considerações de build/release que a decisão acima implica

- Por identidade de produto (canônico §7), o Pro precisa de `applicationId` (`io.signallq.pro`), assinatura, listing e **contador de `versionCode` próprios** — distintos do contador global do consumer em `android/gradle/libs.versions.toml`. Isso é uma entrada nova no catálogo de versões, não uma migração; registrar como pendência de implementação da Fase 0 do roadmap do Pro (doc 11, §3), não desta sessão.
- Firebase e Play Console do Pro são projetos **separados** do consumer (já registrado no canônico e no roadmap) — nenhuma mudança de escopo aqui, só reforço de que "módulo Gradle novo no mesmo repo" não implica "mesmo projeto Firebase/Play".
- CI: os workflows atuais (`release.yml`, `promote-release.yml`, `firebase-distribution.yml`) são todos específicos do consumer hoje — vão precisar de variantes por produto (path/impacto, como o doc 02 §13 já antecipa para o monorepo-alvo) antes do primeiro build do Pro. Fora de escopo desta sessão; registrar como próximo passo de engenharia, não decisão arquitetural.

---

## 2. Regras de ouro aplicadas ao Pro desde o dia 1

O piso mínimo é o que já vale para o consumer (`arquitetura-android` skill) — o Pro não pode nascer com regra mais fraca:

1. **Feature não depende de feature.** `:pro:feature:laudo` nunca importa de `:pro:feature:visita` diretamente — o que for compartilhado vive em `:core:*` ou é passado por contrato/ID entre telas.
2. **Cada feature tem seu próprio ViewModel.** Nenhum "god ViewModel" — os dois exemplos hoje tolerados no consumer (`MainViewModel`, `ChatDiagnosticoIaViewModel`) são exceção histórica, não modelo a repetir.
3. **Limiares de tamanho da regra de higiene (§7) são gate de design, não reação pós-fato.** `HomeScreen.kt` (3938 linhas), `MainViewModel.kt` (2191), `AppShell.kt` (1146), `EquipamentoInternetScreen.kt` (1549), `DispositivosScreen.kt` (1386), `JogosScreen.kt` (1120) — todos hoje acima do limiar de "dívida crítica" (1200 linhas) ou de "extração obrigatória" (800 linhas), todos vieram de uma tela que cresceu por acréscimo em vez de nascer já dividida. O mapa de módulos da seção 5 já separa por sheet/painel/etapa propositalmente para não repetir esse padrão — cada sheet ou etapa de fluxo com risco de crescer (Ambientes, Medição, Laudo) já nasce com arquivo próprio, não como função privada dentro de uma tela maior.
4. **DI na borda, domínio puro no centro.** Os módulos `:core:diagnostico` e `:core:relatorio` extraídos devem permanecer Kotlin puro (como já são hoje) — a integração Hilt fica em `:pro:app` ou nos módulos de feature, nunca infiltra o core.

---

## 3. Legenda de reaproveitamento por peça real

Classificação conforme `02_..._Especificacao_Tecnica_v5.md` §3.2 (REUSE / ADAPT / MERGE / REWRITE / RETIRE / QUARANTINE), reconferida peça a peça contra o código real e a consulta ao Camilo (§4). Onde a classificação mudou em relação à hipótese do doc 02/doc 12, o motivo está explícito.

### 3.1 Motor de causa raiz (diagnóstico)

**Classificação: ADAPT** (extração de domínio inteiro, não de 6 arquivos avulsos).

| Peça | Local atual | Linhas | Android? |
|---|---|---|---|
| `FindingEngine.kt` | `android/feature/diagnostico/.../FindingEngine.kt` | 745 | Não |
| `ScoreEvidenceBuilder.kt` | mesmo pacote | 219 | Não |
| `ScoreEngine.kt` | mesmo pacote | 228 | Não |
| `EvidenceProvenance.kt` | mesmo pacote | 34 | Não |
| `DiagnosticRunner.kt` | mesmo pacote | 136 | Não |
| `InternetDiagnosticEngine.kt` | mesmo pacote | 213 | Não |
| + ~40-50 arquivos irmãos (`WifiSignalQualityEngine`, `MobileSignalDiagnosticEngine`, `FibraSignalQualityEngine`, `DnsDiagnosticEngine`, `HistoricalDegradationEngine`, `WifiChannelDiagnosticEngine`, `GameReadinessClassifier`, `UsageProfileClassifier`, `MetricClassifier`, `DiagnosticInput/Report/Result`, `topology/model/*`, `topology/correlation/*`, `topology/internet/*`) | mesmo pacote | — | Não |

**O que precisa mudar estruturalmente:** mover o pacote quase inteiro para `:core:diagnostico` (novo), **exceto** 6 arquivos que ficam em `:featureDiagnostico` por dependerem de Android/rede real ou DI: `topology/lan/GatewayResolver.kt`, `topology/lan/OuiVendorLookup.kt`, `topology/lan/UpnpIgdDiscovery.kt` (socket/rede real), `topology/TopologyDiagnostic.kt`, `di/DiagnosticoModule.kt` (Hilt — precisa de rewiring dos bindings para o novo módulo), `pulse/SignallQOrchestrator.kt`. Os testes (que espelham o pacote) migram junto.

**Esforço: M, revisado para M/G apos mapeamento real em 2026-07-19** (closure de 29 arquivos +
~70 consumidores externos confirmados por grep e pelo compilador, ver §3.1.1) — mecanicamente
repetitivo (mover arquivo, trocar package, ajustar import, resolver 1 dependencia circular via
inversao de dependencia no `DiagnosticRunner`). **Executado e mergeado em 2026-07-19** (mesmo
dia do mapeamento, apos autorização explícita da Claudete com exigência de testes de
caracterização antes de tocar no núcleo) — ver §3.1.1 para o registro completo.

**Nota sobre `RecommendationEngine.kt` de `featureDiagnostico` (14 regras REC-01..14):** também Kotlin puro, também no pacote, mas **não é candidato automático de extração pura** — ele gera recomendação em linguagem simplificada para o consumidor final. Doc `01_..._Arquitetura_v5.md` §8.2 já registra que consumer e Pro compartilham "contrato e motor", mas "prompt/apresentação separados" — então a base (`FindingResult`) migra para `:core:diagnostico`, mas o Pro constrói sua própria camada de apresentação técnica sobre ela, sem herdar as 14 regras REC-01..14 como estão (elas são calibradas para o usuário final leigo, não para o técnico profissional). Não confundir com `coreRecommendation` (módulo separado, é sobre monetização — `RecommendationEngineCard`, catálogo, cooldown — zero relação com causa-raiz).

#### 3.1.1 Closure exato mapeado e executado (2026-07-19, Camilo — issue #1157, PR #1159)

Investigação por import estático (grep, não estimativa) sobre `android/feature/diagnostico/src/main/kotlin/.../feature/diagnostico/`, reconferindo peça a peça a hipótese de "~40-50 arquivos" da sessão anterior. Resultado: **o núcleo autocontido tem 29 arquivos** (menor que a hipótese, mas mais preciso), e a extração está bloqueada não por dificuldade técnica no núcleo em si, mas pelo número de **arquivos externos ao núcleo que o referenciam** (66, ver abaixo) — cada um precisa de import atualizado (mudança de pacote `feature.diagnostico` → `core.diagnostico`, tipos deixam de estar no mesmo pacote implícito).

**Núcleo autocontido — 29 arquivos, zero `import android.*`, fecha em si mesmo (validado):**

```
FindingEngine.kt · ScoreEvidenceBuilder.kt · ScoreEngine.kt · EvidenceProvenance.kt ·
DiagnosticRunner.kt · InternetDiagnosticEngine.kt · WifiSignalQualityEngine.kt ·
MobileSignalDiagnosticEngine.kt · FibraSignalQualityEngine.kt · DnsDiagnosticEngine.kt ·
HistoricalDegradationEngine.kt · WifiChannelDiagnosticEngine.kt · GameReadinessClassifier.kt ·
UsageProfileClassifier.kt · MetricClassifier.kt · DiagnosticInput.kt · DiagnosticReport.kt ·
DiagnosticResult.kt · DiagnosticStatus.kt · DadoCanal.kt · NivelCongestionamento.kt ·
SnapshotEspectroCanal.kt · topology/model/NetworkTopology.kt · topology/model/SsdpResponse.kt ·
topology/model/UpnpDeviceInfo.kt · topology/correlation/NatClassifier.kt ·
topology/correlation/TopologyTracer.kt · topology/internet/GeoIpResolver.kt ·
topology/internet/PublicIpResolver.kt
```

`DiagnosticStatus.kt`, `DadoCanal.kt`, `NivelCongestionamento.kt` e `SnapshotEspectroCanal.kt` não
estavam na lista original da sessão de 18/07 — são dependências reais descobertas (`DiagnosticStatus`
usado em 14 dos arquivos do núcleo; os outros três são tipos auxiliares de
`WifiChannelDiagnosticEngine`). Confirmado por grep, não suposição.

**Acoplamento real que a extração precisa resolver — `DiagnosticRunner` chama `RecommendationEngine`:**
`DiagnosticRunner.run()` (linha 77) chama `RecommendationEngine.recomendar(input, achados)` direto —
mas `RecommendationEngine` fica em `:featureDiagnostico` (não migra, é REC-01..14 do consumidor, ver
nota acima). Migrar `DiagnosticRunner` sem resolver isso cria dependência circular
(`:core:diagnostico` → `:featureDiagnostico` → `:core:diagnostico`). Solução de inversão de
dependência já desenhada (não implementada): trocar a chamada direta por um parâmetro
`gerarRecomendacoes: (DiagnosticInput, FindingResult) -> List<DiagnosticResult>` com default
`{ _, _ -> emptyList() }` (seguro — nenhum teste existente do núcleo verifica `report.recomendacoes`,
confirmado por grep). Único chamador de produção real de `DiagnosticRunner.run()` é
`remote/RemoteDiagnosticRepository.kt` (2 call sites, linhas 70/85) — fica em `:featureDiagnostico`,
precisa passar `gerarRecomendacoes = RecommendationEngine::recomendar` explicitamente pra não haver
regressão silenciosa de comportamento (recomendações sumindo em produção).

**Consumidores fora do núcleo — mapeados e corrigidos.** Grep aplicado
`(DiagnosticInput|DiagnosticReport|DiagnosticResult|DiagnosticStatus|FindingEngine|FindingResult|
ScoreEngine|...)` contra o repo inteiro, excluindo o próprio núcleo — retornou ~70 arquivos reais
(confirmados pelo compilador, não só grep, incluindo alguns que o grep inicial não pegou —
`topology/lan/MeshDetector.kt`/`UpnpParser.kt`/`UpnpIgdDiscovery.kt` referenciavam `DeviceInfo`/
`UpnpDeviceInfo`/`SsdpResponse` de `topology/model`, que também fazia parte do núcleo), divididos em
dois grupos:
1. **Dentro de `:featureDiagnostico`, fora do núcleo** — `ai/*`, `pulse/*` (exceto
   `SignallQOrchestrator.kt`, exceção conhecida), `remote/*`, `recommendation/*`,
   `RecommendationEngine.kt`, `RecommendationRequestMapper.kt`, `DiagnosticOrchestrator.kt`,
   `topology/lan/*` — import atualizado (mesmo pacote antes, pacotes diferentes depois).
2. **Em `:app`** — telas, ViewModels e mappers que constroem/consomem `DiagnosticInput`/`DiagnosticReport`
   (`MainViewModel.kt`, `SinalScreen.kt`, `ResultadoVelocidadeScreen.kt`, `LaudoScreen.kt`,
   `EquipamentoInternetScreen.kt` etc.) — mesmo tratamento.

Nenhum desses ~70 era tecnicamente complexo (100% mecânico: adicionar import, dois ajustes de
smart-cast cross-módulo), mas o volume exigiu execução com validação incremental real — build +
teste a cada módulo (`:core:diagnostico` isolado → `:featureDiagnostico` → `:app` → suite
completa), não tudo de uma vez.

**Executado em 2026-07-19 (mesma sessão do mapeamento, autorizado pela Claudete):**
1. `:core:diagnostico` criado (`android/core/diagnostico/`), Kotlin puro, sem dependência Android real.
2. 29 arquivos do núcleo + 17 testes movidos para `io.signallq.app.core.diagnostico` (+ subpacotes
   `topology.model`/`topology.correlation`/`topology.internet`). Nenhum teste movido referenciava
   `RecommendationEngine`/`recomendacoes` — todos usam o default seguro `gerarRecomendacoes = { _, _ -> emptyList() }`.
3. `DiagnosticRunner.run()` ganhou o parâmetro `gerarRecomendacoes`; os 2 call sites de produção em
   `RemoteDiagnosticRepository.kt` passam `RecommendationEngine::recomendar` explicitamente.
4. ~70 arquivos fora do núcleo (`:featureDiagnostico` e `:app`) com import corrigido em lotes,
   validado a cada lote via erro real do compilador (`grep "^e:"` na saída do `compileDebugKotlin`),
   não por suposição de quais arquivos precisavam de ajuste.
5. `:featureDiagnostico` e `:app` ganharam `implementation(project(":core:diagnostico"))`.
6. `di/DiagnosticoModule.kt` não precisou de rewiring de bindings Hilt — não referenciava nenhum
   tipo do núcleo diretamente.
7. Validado: `:core:diagnostico:testDebugUnitTest` (17 testes, standalone) → `:featureDiagnostico`
   compile+test → `:app` compile+test → `./gradlew test` (suite completa) + `assembleDebug` (`:app`
   e `:pro:app`) → `:app` `ktlintCheck`/`detekt`. Teste de caracterização
   (`DiagnosticRunnerCaracterizacaoTest.kt`, escrito antes da extração) reexecutado depois: 5/5
   verde, nenhuma asserção alterada.

### 3.2 Amostragem estatística e "Modo Triplo" do Speedtest

| Peça | Classificação | Esforço | Motivo |
|---|---|---|---|
| `AnalisadorAmostragemPing.kt` (75 linhas, `android/feature/speedtest/...`) | **REUSE** | P | `object` autocontido, Kotlin puro, zero dependência de instância — extração é copiar o arquivo. |
| `executarModoTriplo` (método privado, linhas 400-621 de `ExecutorSpeedtestCloudflare.kt`, 1431 linhas) | **ADAPT com refactor prévio** | **G** | Não é extraível como está: muta estado de instância direto (`faseAtualInterna`, `velocidadeAtualInterna`, `pontosAoVivoInternos`, `aguardandoProximaRodadaInterna`, `rodadasTriploInternos`, `cancelFlag`) e chama métodos privados irmãos (`executarFaseLatencia`, `executarFaseTransferencia`, `executarFaseUploadAdaptativa`, `publicar()`). Recomendação técnica do Camilo: extrair a **state machine de fases** (não o método isolado, não a classe inteira de 1431 linhas) para um coordenador Kotlin puro independente de instância — só então o Pro reusa esse coordenador. |

Este item precisa de **testes de caracterização antes de tocar** (regra de higiene §4.2, mesmo espírito aplicado ao `MainViewModel.kt`) — é refactor de uma classe de produção em uso ativo no consumer, não simples reorganização de arquivo.

### 3.3 Wi-Fi (scan, canal, vizinhança)

| Peça | Local atual | Classificação | Esforço |
|---|---|---|---|
| `RedeVizinha`/`SegurancaWifi`, `ChannelEvaluator`, `ChannelCandidates`, `ChannelEvalModels`, `FrequencyUtils` | já em `:coreNetwork` (`featureWifi` mantém só typealias/delegate) | **REUSE** — já feito | — |
| `ScannerRedesWifi.kt` (140 linhas, usa `Context`/`WifiManager`/`BroadcastReceiver`) | `:featureWifi` | **ADAPT** (mover para `:coreNetwork` já existente) | P |
| `ScanResultAdapter.kt` (`toNeighbor()`, 40 linhas, usa `android.net.wifi.ScanResult`) | `:featureWifi` | **ADAPT** (idem) | P |
| `SnapshotScanWifi`/`EstadoScanWifi` (tipos pequenos) | `:featureWifi` | **ADAPT** (mover junto) | P |

Confirmado pelo Camilo: os dois arquivos principais são wrapper de plataforma puro, **zero regra de negócio de featureWifi entranhada** — candidatos triviais. Este item alimenta diretamente as telas "5.2 Escanear rede Wi-Fi" e "5.3 Analisador de canais" do protótipo (§5).

### 3.4 Engine de PDF (laudo)

| Peça | Local atual | Classificação | Esforço |
|---|---|---|---|
| `PdfPrintHelper.kt` (118 linhas) | `:featureHistory` | **REUSE** direto | P — zero referência a `MedicaoEntity`, só `PrintDocumentAdapter`/`ParcelFileDescriptor`/`File`. |
| `ExportadorHistoricoPDF.exportarComWebView()` (motor WebView→paginação→PDF) | `:featureHistory` | **ADAPT** (trocar assinatura `medicoes: List<MedicaoEntity>` → `html: String`) | P — acoplamento é só na assinatura, o motor não conhece o tipo. |
| `gerarHtml(medicoes)` (geração do HTML/CSS do relatório do consumer) | `:featureHistory` | **REWRITE** para o Pro | — layout/dado diferente é esperado, não é dívida a herdar. |

**`anatelReport.ts` (`linka-webapp`) — MERGE de ideia, não de código.** A técnica difícil (paginação HTML→canvas→PDF multi-página) já está resolvida nativamente no próprio Android (item acima), com stack diferente (WebView nativo vs. jsPDF/html2canvas web). `anatelReport.ts` serve de referência de **estrutura/conteúdo** de laudo formal, não de código a portar.

### 3.5 Itens fora do repositório local

Auditados por acesso remoto real (`gh api`/clone raso), não por leitura de documentação antiga — método idêntico ao do doc 12.

| Peça | Repo | Classificação | Nota |
|---|---|---|---|
| `classifier.ts`/`networkQualityClassifier.ts` | `linka-webapp` / `linka-speedtest` | **RETIRE** como fonte de verdade pro Android nativo | `FindingEngine` é o motor canônico; web fica só pro Portal, sem duplicar lógica de decisão no Kotlin. |
| `ChamadoCanônico` + cálculo de confiança | `7AgentsStudio/signallq-isp` (privado) | **REUSE como padrão arquitetural** (payload canônico + idempotência), não reuso de código | Ver detalhe abaixo — confirmado com arquivo:linha em 18/07/2026. |
| Capability model / safety guard | `gmmattey/nethal` | **QUARANTINE** confirmado | Zero driver estável, ~6 modelos cobertos (doc 12). Não integrar no MVP1; tela "5.4 Fingerprint de rede" usa inventário manual primeiro. |
| `LinkaWifiDiagnosticsPlugin.java` (Capacitor) | `gmmattey/linka-speedtest` | **RETIRE** | Redundante — ver detalhe abaixo. |
| `PacketLossPlugin.java` (Capacitor) | `gmmattey/linka-speedtest` | **MERGE de ideia** (técnica a considerar, não código a portar) | Traz capacidade que falta hoje no motor nativo — ver detalhe abaixo. |

**`ChamadoCanônico` — confirmado em 18/07/2026 (Camilo, clone temporário).** Tipo real em `src/shared/chamado.ts:1-27`. Cálculo de confiança em `src/features/tecnico-virtual/diagnostico.ts`: `calcularConfianca()` (linhas 26-49) é heurística por distância do threshold (`ratio = (limite - medido) / limite`, `ratioParaNivel` linhas 51-55: ≥0,6 high / ≥0,2 medium / <0,2 low), com sinais de contexto fortes (`wan_status: disconnected`, `massiva_ativa`) que sobrescrevem para `high` incondicionalmente (`montarResultadoMedicao`, linhas 80-92).

- **Idempotência: confirmada forte**, não é só promessa de doc. `functions/api/erp/chamado.ts:150-181` reserva o `diagnostico_id` no D1 antes de chamar o adapter ERP, com 3 estados explícitos (`reservado`/`em_andamento`/`ja_processado`), libera a reserva só se o adapter falhar de fato (linhas 192-196, permite retry). Teste de corrida real simultânea prova "só uma chamada abre OS, ambas recebem o mesmo protocolo" (`tests/chamado-idempotencia.test.ts:88-105`) — caso feliz e negativo.
- **Correção à leitura do doc 12/desta seção:** `ChamadoCanônico` **não tem campo de versão explícito** (nenhum `schema_version`/semver). O que existe é tolerant-reader com campos opcionais aditivos por issue — `turno_preferido?` (issue #106), `pop_nome?` (issue #97) — aceitos por payload antigo sem quebrar (validador do endpoint, linhas 29-32). É evolução de contrato compatível pra trás, não a mesma coisa que um contrato com discriminador de versão real. O reuso pro enum "Conclusão da visita" (tela 3.10) permanece **padrão, não código**: confiança tri-nível por distância de threshold, sinais de contexto que forçam `high`, e guarda de idempotência via reserva-antes-de-chamar — nada disso é Kotlin/TS portável direto (stacks e domínios diferentes).

**Plugins Capacitor do `linka-speedtest` — confirmado em 18/07/2026.** `LinkaWifiDiagnosticsPlugin.java` (`android/app/src/main/java/br/com/linka/speedtest/wifi/`) lê `WifiInfo`/gateway/scan de vizinhança (linhas 100-116, 216-243, 160-179) — tudo já coberto e superado pelo motor Kotlin nativo (`GatewayResolver.kt`, `GatewayLatencyMeasurer.kt`, `ScannerRedesWifi.kt`), que ainda cobre RSRP/RSRQ/SINR, ONT/GPON e topologia que o plugin nem tenta. **RETIRE.**

`PacketLossPlugin.java` (mesmo repo, linhas 54-117) mede perda de pacotes **real** via socket UDP (`DatagramSocket`/`DatagramPacket`, N pacotes formatados como query DNS mínima pro `1.1.1.1:53`, contagem real de timeout/entrega) — **isso não é redundante.** Confirmado em `ExecutorSpeedtestCloudflare.kt:1235`: `packetLossSource = "estimated"` — o SignallQ hoje mede perda de pacotes por heurística de timeout HTTP durante ping/download, nunca por socket UDP dedicado. Se o Pro quiser medição de perda "grau ISP" (mais crível pra laudo técnico formal), o *approach* (UDP direto, contagem real de entrega) é referência de técnica a considerar na implementação nativa Kotlin do Pro — não há código a portar (Java/Capacitor vs. Kotlin nativo), é gap de capacidade real a decidir se entra no escopo de medição do Pro.

---

## 4. Consulta ao Camilo

Consultado nesta sessão (18/07/2026) via `Agent` tool, papel "dev único do squad" — ele é quem vai implementar e conhece o código de verdade. Perguntas e correções que ele trouxe, já incorporadas na seção 3:

1. **Domínio de diagnóstico é maior do que os 6 arquivos citados na minha primeira leitura** — é um domínio inteiro de ~40-50 arquivos já coerentemente Kotlin puro, com só 6 exceções (Android/DI). Mudou a estimativa de esforço de P para **M**.
2. **`executarModoTriplo` não é extraível como método isolado** — muta estado de instância e depende de métodos privados irmãos. Recomendou extrair a state machine de fases para um coordenador independente, não mover a classe inteira nem tentar copiar só o método. Mudou a classificação de "ADAPT simples" para **"ADAPT com refactor prévio", esforço G**.
3. **Adapters de Wi-Fi confirmados como triviais** (P) — zero lógica de negócio entranhada, só wrapper de plataforma.
4. **`PdfPrintHelper` confirmado 100% reaproveitável sem nenhum acoplamento** — o acoplamento ao schema do consumidor é só na assinatura da função de fora, não no motor de paginação.
5. **Recusou avaliar `linka-speedtest` (Capacitor/Java)** por não ter o repositório acessível localmente — resposta correta, registrada como pendência em vez de suposição.

---

## 5. Mapa de módulos × telas do protótipo

O protótipo real ("SignallQ Pro - Protótipos.dc.html", projeto `69e53070-6aa8-485a-8d0a-5bfa36e1a08c`) foi lido diretamente via `DesignSync`. Ele organiza as telas em **6 grupos numerados** (a auditoria/tarefa cita 44 telas; a leitura direta encontrou ~51 títulos numerados nos mesmos 6 grupos — a diferença provavelmente conta variações de estado da mesma tela como uma só entrada; não é crítico para o mapa de módulos, que é feito por grupo funcional, não por contagem exata de tela).

| Grupo do protótipo | Telas (ID — título) | Módulo Gradle proposto | Depende de (core) | Gate de tamanho (regra §7 da higiene) |
|---|---|---|---|---|
| 1 — Acesso/Ativação | 1.1 Carregamento · 1.2 Apresentação · 1.3 Login · 1.4 Recuperar senha · 1.5 Criar conta · 1.6 Verificar e-mail · 1.7 Permissões · 1.8 Permissão bloqueada · 1.9 Por que assinar o Pro · 1.10 Escolha do plano | `:pro:feature:auth` | `:corePermissions` (existente, reuso direto) | Nenhuma tela sozinha justifica >150 linhas de Composable; dividir por tela desde o início, sem "AuthScreen.kt" único. |
| 2 — Núcleo (visita/medição) | 2.1 Painel · 2.2 Menu · 2.3 Novo cliente · 2.4 Nova visita · 2.5 Atendimento · 2.13 Modo de visita rápida · 2.14 Checklist por tipo · 2.6-2.9 Ambientes (criar/renomear/excluir) · 2.10 Medição do ambiente · 2.11 Walk Test · 2.12 Evidências · 2.15-2.16 Diagnóstico (medindo/resultado) | `:pro:feature:visita` (2.1-2.5, 2.13-2.14) + `:pro:feature:ambiente` (2.6-2.9) + `:pro:feature:medicao-diagnostico` (2.10-2.12, 2.15-2.16) | `:core:diagnostico` (novo, §3.1), `:coreNetwork` (existente + adapters Wi-Fi movidos, §3.3), `:coreTelephony` (existente) | Grupo mais arriscado de virar um novo `HomeScreen.kt`/`EquipamentoInternetScreen.kt` (ambos hoje >1200 linhas) — nasce **já dividido em 3 módulos** por essa razão explícita, não só por gosto de nomenclatura. |
| 3 — Entrega/Financeiro | 3.1 Antes×depois · 3.2 Laudo técnico · 3.3 Compartilhar laudo · 3.4 Aceite do laudo · 3.11 Resumo técnico · 3.5 Cobrança Pix · 3.6 Falha no pagamento · 3.7 Recibo digital · 3.8 Histórico de visitas · 3.9 Agendamento (lista) · 3.10 Conclusão da visita | `:pro:feature:laudo` (3.1-3.4, 3.11) + `:pro:feature:pagamento` (3.5-3.7) + `:pro:feature:visita` (3.8-3.10, mesmo módulo do grupo 2) | `:core:relatorio` (novo, §3.4), `:core:diagnostico` | "Conclusão da visita" (3.10) é onde entra o padrão `ChamadoCanônico` do signallq-isp (§3.5) — enum fechado de fechamento, não campo livre. |
| 4 — Clientes/Conta | 4.1 Lista de clientes · 4.2 Busca · 4.3 Detalhe do cliente · 4.4 Detalhe do local · 4.6 Perfil/Ajustes · 4.7 Assinatura | `:pro:feature:cliente` (4.1-4.4) + `:pro:feature:conta` (4.6-4.7) | — (domínio novo, sem core a reusar) | — |
| 5 — Ferramentas | 5.1 Ferramentas (grid) · 5.2 Escanear rede Wi-Fi · 5.3 Analisador de canais · 5.4 Fingerprint de rede | `:pro:feature:ferramentas` | `:coreNetwork` (5.2-5.3, reuso direto pós-extração §3.3) | 5.4 não tem motor por trás no MVP1 (Nethal em QUARANTINE) — implementar como inventário manual, não simular capacidade que não existe. |
| 6 — Estados transversais | Vazio · Erro recuperável/offline · Sincronização pendente · Erro bloqueante | **Não é feature** — design system compartilhado (`StateCard` e equivalentes) | — | Cada feature module consome os componentes de estado do design system do Pro, nunca reimplementa o próprio. |

---

## 6. Decisões e pendências (não bloqueiam este documento, bloqueiam implementação futura)

Herdadas do canônico (`00_CANONICO_v5.md` §8), ainda abertas: preço do Pro, domínio do portal, provedor de identidade, conectores de nuvem além do Android SAF, política de retenção por plano.

**Resolvidas em 18/07/2026 (v1.1):**

- ~~Acesso ao repo `linka-speedtest`~~ — não era bloqueio de permissão (conta `gmmattey` já tinha `admin` via `gh`). Camilo clonou e auditou: `PacketLossPlugin.java` revelou um **gap real de capacidade** (medição de perda de pacotes por UDP dedicado, que o motor Kotlin nativo não tem — hoje é estimativa por timeout HTTP, `ExecutorSpeedtestCloudflare.kt:1235`). Decisão de produto pendente: o Pro quer medição de perda "grau ISP" via UDP dedicado no MVP1/MVP2? Não decidido nesta sessão — registrar como candidato a issue de produto, não arquitetural.
- ~~Acesso ao repo privado `7AgentsStudio/signallq-isp`~~ — mesma causa (acesso já existia). `ChamadoCanônico` confirmado com arquivo:linha (§3.5): idempotência forte confirmada, mas "versionado" era leitura imprecisa — é tolerant-reader aditivo sem `schema_version` explícito.

**Ainda abertas:**

1. **Contador de `versionCode` separado para o Pro** em `libs.versions.toml` (ou catálogo próprio) — decisão de engenharia pendente antes do primeiro build, não arquitetural.
2. **CI por produto** (`release.yml`/`promote-release.yml`/`firebase-distribution.yml` hoje são só do consumer) precisa de variante ou path-scoping para o Pro antes do primeiro release — registrar como próximo passo de engenharia.
3. **Refactor de `ExecutorSpeedtestCloudflare`** (extração da state machine do "Modo Triplo") precisa de testes de caracterização dedicados antes de qualquer mudança — não é decisão, é sequenciamento de trabalho futuro.
4. **Decisão de produto: medição de perda de pacotes via UDP dedicado no Pro** (achado do `PacketLossPlugin.java`, acima) — registrar como possível item de roadmap MVP1/MVP2, não decidir aqui.

---

## Documentos relacionados

- `00_CANONICO_v5.md` — dicionário canônico de nomes, eventos, tokens e decisões (prevalece sobre este).
- `01_SignallQ_Platform_Arquitetura_v5.md` — arquitetura-alvo da plataforma completa (monorepo `signallq-platform`).
- `02_SignallQ_Platform_Especificacao_Tecnica_v5.md` — classificação original de ativos (§3.2), fases 0-9 de migração, ADRs.
- `08_SignallQ_Pro_Especificacao_Funcional_v5.md` — visão, entidades e módulos funcionais do Pro.
- `09_SignallQ_Pro_Jornada_e_Fluxo_de_Telas_v5.md` — jornada e catálogo de telas (base para o mapa da seção 5).
- `11_SignallQ_Pro_Roadmap_MVP1_MVP2_v5.md` — fases e sequência de implementação.
- `12_SignallQ_Pro_Auditoria_Cobertura_Repositorios_2026-07-18.md` — auditoria de cobertura por repositório que este documento reconfere e aprofunda.
- `.claude/skills/arquitetura-android/SKILL.md` — regras de ouro aplicadas ao Pro (seção 2).
- `.claude/rules/higiene-e-padronizacao-repositorio.md` — convenção de módulos novos (§1.2) e limiares de tamanho (seção 2, item 3).
