package io.signallq.app.ui.component

import io.signallq.app.core.diagnostico.BandaWifi
import io.signallq.app.core.diagnostico.MetricClassifier
import io.signallq.app.core.diagnostico.MetricStatus

/**
 * Ponte entre os consumidores isolados de RSSI/RSRP/RSRQ/SINR/bufferbloat que a NDS-02b (#1749,
 * ADR-017) migrou e o motor local de classificação ([MetricClassifier], `core/diagnostico`).
 *
 * ## Por que este arquivo existe em vez de cada consumidor continuar chamando MetricClassifier
 *
 * A issue #1749 pede pra decidir, por consumidor, se cabe uma chamada de rede ao NDS ali ou se o
 * dado deve chegar já classificado de um nível acima. Pra `SignalBars.kt`,
 * `SinalMovelClassificacao.kt`, `SinalTopologiaHelpers.kt` e `HistoricoScreen.kt` (os 4 arquivos
 * de UI que consomem funções daqui — `ContinuidadeMedicao.kt`, o 6º ponto do escopo, é uma ponte
 * de vocabulário separada, sem motor nenhum por trás, resolvida em `MetricStatusUi.kt`), nenhuma
 * das duas opções da issue se aplica hoje, por três motivos estruturais e não por preferência:
 *
 * 1. **Não existe orquestração viva do NDS ainda em lugar nenhum do app.** NDS-01 (#1744) isolou o
 *    cliente HTTP; NDS-02a (#1747) só adicionou mappers puros, "zero UI tocada". Decidir quando
 *    disparar uma avaliação (abrir tela? puxar pra atualizar? periódico?) e tratar loading/erro é
 *    trabalho de orquestração de fatia própria (`MainViewModel`, NDS-02k, deliberadamente por
 *    último por ser o arquivo de maior raio de impacto do app) — não cabe inventar isso ad-hoc em
 *    4 pontos pequenos e desincronizados desta fatia.
 * 2. **O `veredicto` do NDS é ÚNICO por avaliação, não um status por métrica.** `SignalBars`/
 *    `signalColor` também são usados por `SinalScreen.kt` pra colorir uma LISTA de redes Wi-Fi
 *    vizinhas distintas (fora de escopo da #1749, adiado pra NDS-02j) — um único veredicto de
 *    "minha conexão" não serve pra classificar N redes diferentes ao mesmo tempo.
 * 3. **Dado histórico não pode ser reclassificado retroativamente.** Decisão do Luiz em #1746:
 *    o `MetricStatus` de uma medição já persistida fica congelado no valor calculado na época, sem
 *    recálculo via NDS — o bufferbloat de `HistoricoScreen.kt` cai direto nesse caso.
 * 4. **Rede móvel especificamente: o NDS ainda não cobre RSRP/RSRQ/SINR.** Confirmado no PR #12 de
 *    `buildea-labs/network-diagnostics-service` (contrato de erros/profiles/capabilities) — a
 *    matriz de cobertura por domínio (`docs/signallq-2-coverage.md`) classifica "Rede móvel" como
 *    **"Parcial"**, com "criar módulo dedicado para tecnologia, RSRP, RSRQ, SINR e contexto" como
 *    próximo passo em aberto (issue P1 `network-diagnostics-service#13`, não fechada). Mesmo que
 *    existisse orquestração viva hoje, `classificarRsrpLocal`/`classificarRsrqLocal`/
 *    `classificarSinrLocal` (usadas só por `SinalMovelClassificacao.kt`) não teriam um veredicto
 *    NDS equivalente pra ler — motivo a mais, não só a ausência de orquestração, pra este trio
 *    continuar 100% local. Reavaliar quando #13 fechar.
 *
 * Diante disso, "trocar" aqui é sintático, não de comportamento: os 4 arquivos param de importar
 * [MetricClassifier] diretamente e chamam por aqui — hoje delega pra a MESMA matemática local
 * (comportamento idêntico, coberto por teste de caracterização), mas fica sendo o ÚNICO ponto que
 * muda quando/se um veredicto vivo do NDS ficar disponível pra cada um destes contextos — nenhum
 * dos 4 call sites originais precisa mudar de novo. `core/diagnostico` continua sendo a fonte de
 * verdade da matemática de classificação enquanto ela não migra pro servidor (ADR-017) — várias
 * engines sobrevivem locais por decisão explícita do Luiz (`RecomendacaoPraticaEngine`,
 * `DiagnosticoGuiadoEngine`, `ModoGamerEngine`, ver decisão em #1746) e não fazem parte desta
 * migração; não há motivo pra duplicar a tabela de limiares aqui.
 *
 * `MetricClassifier.RadioTech` (4G/5G) continua referenciado por `SinalMovelClassificacao.kt` como
 * TIPO compartilhado (não como chamada de classificação `.classificarX()`) — criar um enum
 * paralelo de 2 valores só pra não mencionar o nome `MetricClassifier` não reduziria acoplamento
 * real, já que `SinalMovelClassificacao.kt` segue dependendo de `core/diagnostico` de qualquer
 * forma (o módulo não sai até NDS-03, e `RadioTech` é vocabulário estável usado por outras engines
 * que permanecem locais). Exceção documentada, não descuido.
 */
internal fun classificarRssiWifiLocal(
    rssiDbm: Int,
    banda: BandaWifi,
): MetricStatus = MetricClassifier.classificarRssiWifi(rssiDbm, banda)

internal fun classificarRsrpLocal(
    rsrpDbm: Int,
    tech: MetricClassifier.RadioTech,
): MetricStatus = MetricClassifier.classificarRsrp(rsrpDbm, tech)

internal fun classificarRsrqLocal(
    rsrqDb: Int,
    tech: MetricClassifier.RadioTech,
): MetricStatus = MetricClassifier.classificarRsrq(rsrqDb, tech)

internal fun classificarSinrLocal(
    sinrDb: Int,
    tech: MetricClassifier.RadioTech,
): MetricStatus = MetricClassifier.classificarSinr(sinrDb, tech)

internal fun classificarBufferbloatLocal(deltaMs: Double): MetricStatus =
    MetricClassifier.classificarBufferbloat(deltaMs)
