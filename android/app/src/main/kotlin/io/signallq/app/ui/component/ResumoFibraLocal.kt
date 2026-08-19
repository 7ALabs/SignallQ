package io.signallq.app.ui.component

import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.core.diagnostico.FibraDiagnosticInput
import io.signallq.app.core.diagnostico.FibraSignalQualityEngine

/**
 * Seam local da NDS-02f (#1756, ADR-017) para a classificação da leitura óptica de fibra
 * (rx/tx/temperatura/link) usada pelo resumo interpretado de [LocalDeviceSection].
 *
 * ## Por que este arquivo existe em vez de virar mais uma função de `ClassificacaoMetricaLocal.kt`
 *
 * `ClassificacaoMetricaLocal.kt` (NDS-02b/#1749 e NDS-02d/#1752) embrulha CHAMADAS de
 * classificação que recebem UM valor bruto já medido (`MetricClassifier.classificarX(valorBruto)`)
 * e devolvem um [io.signallq.app.core.diagnostico.MetricStatus] isolado — um enum sem texto.
 *
 * [FibraSignalQualityEngine.avaliar] recebe uma LEITURA COMPOSTA (rx + tx + temperatura + estado do
 * link) e devolve uma LISTA de [io.signallq.app.core.diagnostico.DiagnosticResult] — um achado por
 * regra (FIB-01..FIB-04-OK), cada um já com título/mensagem em PT-BR com o valor medido
 * interpolado (ex.: "rx=-19.80 dBm"). O card de resumo precisa desse texto pronto, não teria como
 * reconstruí-lo a partir de um enum sozinho sem duplicar a tabela de mensagens do motor. Mesma
 * classe de decisão que separou `DecisaoDiagnosticoLocal.kt` de `ClassificacaoMetricaLocal.kt` na
 * NDS-02e (#1754) — shape de saída diferente, arquivo próprio em vez de misturar os dois
 * vocabulários, racional documentado lá.
 *
 * Continua sendo CLASSIFICAÇÃO (não extração de objeto já montado, ver KDoc de
 * `DecisaoDiagnosticoLocal.kt` para a distinção): a entrada é leitura óptica bruta, o motor decide
 * o veredito — só o formato de saída é mais rico que um `MetricStatus`.
 *
 * ## Por que ainda delega 100% local (sem chamada viva ao NDS)
 *
 * Mesmos três motivos estruturais do KDoc de `ClassificacaoMetricaLocal.kt`: não existe
 * orquestração viva do NDS em nenhum lugar do app ainda (fica pra NDS-02k/`MainViewModel`,
 * deliberadamente por último); o `veredicto` do NDS é único por avaliação, não um achado por
 * métrica; a decisão de quando disparar uma avaliação é trabalho de orquestração de outra fatia.
 * Achado do inventário de #1746: o bloco `fiber` do NDS (rxPower/txPower/temperatura/tensão) mapeia
 * quase 1:1 pro que [FibraSignalQualityEngine] já consome hoje — um dos mapeamentos mais diretos —,
 * mas o domínio "Fibra" segue "Parcial" na matriz de cobertura do NDS
 * (`network-diagnostics-service#13`, P1, "expandir além de potência RX"), então não bloqueia o
 * início desta fatia (mesmo raciocínio da #1749 sobre rede móvel).
 *
 * `core/diagnostico` continua sendo a fonte de verdade da matemática de classificação óptica
 * enquanto ela não migra pro servidor — `FibraSignalQualityEngine` não é uma das engines que
 * sobrevivem locais por decisão de produto (`RecomendacaoPraticaEngine`/`DiagnosticoGuiadoEngine`/
 * `ModoGamerEngine`, decisão do Luiz em #1746); é candidata a migração quando a cobertura do NDS
 * para fibra fechar o P1 acima.
 */
internal data class ResumoFibraLocal(
    val titulo: String,
    val mensagem: String,
    val status: DiagnosticStatus,
)

private fun DiagnosticStatus.severidade(): Int =
    when (this) {
        DiagnosticStatus.critical -> 3
        DiagnosticStatus.attention -> 2
        DiagnosticStatus.inconclusive -> 1
        DiagnosticStatus.info -> 1
        DiagnosticStatus.ok -> 0
    }

/**
 * Traduz a leitura óptica bruta num veredito humano único — quando o link está ativo, delega pro
 * mesmo motor de limiares ([FibraSignalQualityEngine]) que a tela de fibra Nokia já usa e escolhe o
 * achado de maior severidade entre os que o motor gerar (rx/tx/temperatura são avaliados
 * independentemente; o pior dos três vira o resumo). Link inativo tem resposta própria, sem
 * consultar o motor — preserva o comportamento anterior a esta fatia (o motor também trata
 * `isUp=false`, mas com um texto ligeiramente diferente do usado aqui; este seam nunca chega a
 * chamá-lo nesse caso, então o texto do motor pra FIB-01 é inatingível por este call site).
 */
internal fun classificarFibraLocal(
    linkAtivo: Boolean?,
    rxPowerDbm: Double?,
    txPowerDbm: Double?,
    temperatureCelsius: Double?,
): ResumoFibraLocal {
    if (linkAtivo == false) {
        return ResumoFibraLocal(
            titulo = "A fibra está sem sinal",
            mensagem = "A fibra está sem sinal da operadora.",
            status = DiagnosticStatus.critical,
        )
    }
    val avaliacoes =
        FibraSignalQualityEngine.avaliar(
            FibraDiagnosticInput(
                rxPowerDbm = rxPowerDbm,
                txPowerDbm = txPowerDbm,
                temperatureCelsius = temperatureCelsius,
                isUp = linkAtivo ?: true,
            ),
        )
    val pior = avaliacoes.maxByOrNull { it.status.severidade() }
    return if (pior != null) {
        ResumoFibraLocal(pior.titulo, pior.mensagemUsuario, pior.status)
    } else {
        ResumoFibraLocal(
            titulo = "A fibra está conectada",
            mensagem = "A fibra está conectada, mas não consegui ler outros dados agora.",
            status = DiagnosticStatus.ok,
        )
    }
}
