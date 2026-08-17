package io.signallq.app.ui.screen

import androidx.compose.runtime.Stable
import io.signallq.app.core.diagnostico.DiagnosticStatus
import io.signallq.app.feature.speedtest.MeasurementStatus

// Continuidade por status de medição — issue #1705 (2.0.09c), épico #1647.
//
// ## O que estava errado
//
// `MeasurementStatus` tem 5 valores e o dado chegava íntegro até a fronteira do fluxo guiado. Ali
// virava `Boolean`:
//
//     resultadoValidoParaConclusao = resultado.status.liberaConclusaoCompleta
//
// Parcial, contaminado, inconclusivo e cancelado caíam no mesmo balde e produziam um banner
// genérico **sem CTA nenhum** — um beco sem saída. A spec 2.0 §9 exige o contrário ("mostra o que
// foi possível concluir e o teste necessário para avançar") e o critério §13 pede que "permissão
// negada, offline, parcial, contaminado e inconclusivo tenham continuidade útil".
//
// ## Nenhum vocabulário novo
//
// A issue pede reuso justificado por escrito, porque já existem vocabulários paralelos demais.
// Aqui não nasce enum nenhum: [MeasurementStatus] continua sendo a fonte, e a cor/ícone/rótulo sai
// do mapeamento canônico de [DiagnosticStatus] (`DiagnosticStatusUi.kt`, GH#1228 P0-8). Este
// arquivo é só a ponte entre os dois, mais o texto de continuidade — que é conteúdo, não taxonomia.

/**
 * O que dizer e o que oferecer quando a medição não é completa.
 *
 * Um `null` de [continuidadeDaMedicao] significa "não há nada a explicar" — o caminho de
 * `COMPLETE`. Todo outro valor traz **ação concreta** em [rotuloAcao]; nenhum termina em texto.
 */
@Stable
internal data class ContinuidadeMedicao(
    val statusVisual: DiagnosticStatus,
    val titulo: String,
    val explicacao: String,
    /** Rótulo do CTA. Nunca vazio — é o que impede o beco sem saída que a #1705 descreve. */
    val rotuloAcao: String,
    /**
     * A conclusão parcial pode ser mostrada junto?
     *
     * `true` em `PARTIAL` e `CANCELLED`, onde o que foi medido continua verdadeiro e a spec §8.5
     * manda preservar. `false` em `CONTAMINATED` e `INCONCLUSIVE`, onde o dado ou é de outra rede
     * ou não tem base estatística — mostrar conclusão ali seria inventar, que é o defeito que o
     * KDoc de `ResultadoIndisponivelScreen` já rejeita.
     */
    val permiteVerConclusaoParcial: Boolean,
)

/**
 * Traduz o status da medição em continuidade. `null` para [MeasurementStatus.COMPLETE].
 *
 * `CANCELLED` está mapeado embora **nunca seja produzido hoje**: `calcularMeasurementStatus` não o
 * emite em nenhum ramo, e `MedicaoEntityMeasurementStatusDriftCharacterizationTest` já documenta
 * isso. Emitir de verdade exige o executor distinguir "cancelado" de "concluído" no snapshot —
 * mudança de contrato dele, a mesma que a ressalva RS14 da PR #1719 apontou. O ramo existe aqui
 * para que a emissão, quando vier, não encontre um buraco na UI; não para fingir que já funciona.
 */
internal fun continuidadeDaMedicao(status: MeasurementStatus): ContinuidadeMedicao? =
    when (status) {
        MeasurementStatus.COMPLETE -> null

        MeasurementStatus.PARTIAL ->
            ContinuidadeMedicao(
                statusVisual = DiagnosticStatus.attention,
                titulo = "Consegui medir parte da sua conexão",
                explicacao =
                    "Uma das etapas não terminou, então o diagnóstico está incompleto. O que " +
                        "aparece abaixo é o que deu para apurar com segurança.",
                rotuloAcao = "Completar a medição",
                permiteVerConclusaoParcial = true,
            )

        MeasurementStatus.CONTAMINATED ->
            ContinuidadeMedicao(
                statusVisual = DiagnosticStatus.attention,
                titulo = "Sua rede mudou durante a medição",
                explicacao =
                    "Os números vieram de conexões diferentes, então não dá para comparar. " +
                        "Refaça sem trocar de Wi-Fi nem alternar para dados móveis.",
                rotuloAcao = "Refazer na mesma rede",
                permiteVerConclusaoParcial = false,
            )

        MeasurementStatus.INCONCLUSIVE ->
            ContinuidadeMedicao(
                // `inconclusive` e não `attention`: ausência de dado não tem o mesmo peso visual de
                // um problema detectado. É o mesmo princípio que `DiagnosticStatusUi` e
                // `MetricStatusUi` já documentam para os respectivos valores inconclusivos.
                statusVisual = DiagnosticStatus.inconclusive,
                titulo = "Não consegui medir o suficiente",
                explicacao =
                    "Vieram poucas amostras para eu afirmar qualquer coisa sobre sua conexão. " +
                        "Prefiro dizer isso a chutar um diagnóstico.",
                rotuloAcao = "Medir de novo",
                permiteVerConclusaoParcial = false,
            )

        MeasurementStatus.CANCELLED ->
            ContinuidadeMedicao(
                statusVisual = DiagnosticStatus.info,
                titulo = "Você interrompeu a medição",
                explicacao =
                    "Guardei o que já tinha sido medido até ali. Dá para seguir com isso ou " +
                        "refazer do começo.",
                rotuloAcao = "Medir de novo",
                permiteVerConclusaoParcial = true,
            )
    }
