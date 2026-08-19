package io.signallq.app.ui.component

import io.signallq.app.core.diagnostico.DiagnosticReport
import io.signallq.app.core.diagnostico.DiagnosticStatus

/**
 * Ponte entre o consumidor isolado `LaudoScreen.kt` e o [DiagnosticReport] do motor local
 * (`core/diagnostico`). Nasceu na NDS-02e (#1754, ADR-017), seguindo o mesmo objetivo estrutural
 * de [ClassificacaoMetricaLocal] (NDS-02b/#1749, NDS-02d/#1752): isolar num único ponto a leitura
 * do dado produzido por um motor de `core/diagnostico`, para que a troca futura pela avaliação
 * viva do NDS (quando a orquestração real chegar na fatia final, NDS-02k/`MainViewModel`) precise
 * mudar só este arquivo, não os call sites de UI.
 *
 * ## Por que um arquivo próprio em vez de reaproveitar `ClassificacaoMetricaLocal.kt`
 *
 * `ClassificacaoMetricaLocal.kt` embrulha CHAMADAS de classificação — `MetricClassifier
 * .classificarX(valorBruto)` — que recebem um número já medido e devolvem um [io.signallq.app
 * .core.diagnostico.MetricStatus] isolado por métrica. `LaudoScreen.kt` não classifica nada: ela
 * recebe um [DiagnosticReport] JÁ MONTADO (via `SnapshotDiagnostico.relatorio`, produzido em
 * `MainViewModel`) e só LÊ campos dele (`decisao.status/titulo/mensagemUsuario/recomendacao`,
 * `executionId`, e as propriedades derivadas `scoreConexao`/`veredito` do próprio relatório) —
 * confirmado direto no código atual desta fatia, não só no inventário de #1746 (que citava só
 * `decisao`+`executionId`; `scoreConexao`/`veredito` também são lidos direto de `relatorio`,
 * embora sejam propriedades computadas do relatório, não do `decisao` isoladamente). É uma
 * EXTRAÇÃO de forma de um objeto de saída de motor, não uma classificação de valor de entrada —
 * shape diferente, arquivo próprio evita misturar os dois vocabulários no mesmo lugar.
 *
 * `DiagnosticStatus` (vocabulário de 5 valores) continua sendo referenciado como TIPO — igual ao
 * que já acontece com `MetricStatus` através de `ClassificacaoMetricaLocal.kt`/`MetricStatusUi
 * .kt` — porque já existe um único ponto de conversão `DiagnosticStatus` -> cor/label/ícone em
 * [DiagnosticStatusUi] (GH#1228 Fase 0, P0-8), usado também por `DiagnosticoResultadoComponents
 * .kt`, `EquipamentoModuloTecnicoCard.kt` e `LocalDeviceSection.kt`. Duplicar esse mapeamento
 * aqui, ou criar um enum paralelo só pra não mencionar `DiagnosticStatus`, não reduziria
 * acoplamento real — a vocabulário em si não é o motor sendo seamed, é dado de saída estável.
 *
 * Sem chamada viva ao NDS ainda — mesma decisão estrutural da NDS-02b/d (ver KDoc de
 * [ClassificacaoMetricaLocal] para os motivos completos: 1. não existe orquestração viva do NDS
 * em lugar nenhum do app ainda; 2. a extração de "decisão final" equivalente do NDS depende do
 * que a NDS-02a expõe hoje — `NdsAiResult.explanation` (um título+resumo por chamada) + o
 * `recommendation` de topo, sem decoder tipado rico o bastante pra substituir `DiagnosticResult`
 * ainda; 3. a orquestração de quando montar essa decisão fica pra NDS-02k/`MainViewModel`,
 * deliberadamente por último). Hoje [paraDecisaoDiagnosticoLocal] só extrai os campos do
 * [DiagnosticReport] já calculado — comportamento idêntico ao acesso direto que existia antes,
 * coberto por teste de caracterização.
 */
internal data class DecisaoDiagnosticoLocal(
    val status: DiagnosticStatus,
    val titulo: String,
    val mensagemUsuario: String,
    val recomendacao: String?,
    val scoreConexao: Int,
    val veredito: String,
    val executionId: String,
)

internal fun DiagnosticReport.paraDecisaoDiagnosticoLocal(): DecisaoDiagnosticoLocal =
    DecisaoDiagnosticoLocal(
        status = decisao.status,
        titulo = decisao.titulo,
        mensagemUsuario = decisao.mensagemUsuario,
        recomendacao = decisao.recomendacao,
        scoreConexao = scoreConexao,
        veredito = veredito,
        executionId = executionId,
    )
