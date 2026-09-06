package io.signallq.app.ui.screen

import androidx.compose.runtime.Stable
import io.signallq.app.core.diagnostico.MetricStatus
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
// ## Vocabulário: o que é reuso e o que é decisão nova (ressalva RS1 de Caio na PR #1723)
//
// A issue proíbe criar vocabulário novo, e a primeira redação deste cabeçalho afirmava que aqui só
// havia reuso. Não é bem isso, e a distinção importa:
//
// - **Reuso, de fato:** [MeasurementStatus] continua sendo a fonte — nenhum enum nasce aqui. E
//   `MetricStatusUi.kt` continua sendo o único tradutor de [MetricStatus] para cor e ícone; nenhuma
//   cor local é inventada.
// - **Decisão nova, assumida:** a **ponte** `MeasurementStatus → MetricStatus` não existia.
//   `MetricStatusUi` mapeia o vocabulário canônico de severidade (o mesmo que o veredicto do NDS
//   fala — ADR-017), não o de *integridade de medição*. Dizer que `CONTAMINATED` se apresenta
//   como `regular` e `INCONCLUSIVE` como `inconclusivo` é escolha desta fatia, não herança.
//   Por isso ela é testada campo a campo em `ContinuidadeMedicaoTest`, e não apenas escrita.
//
// Issue #1749 (NDS-02b, ADR-017): o campo [ContinuidadeMedicao.statusVisual] trocou de
// `DiagnosticStatus` (aposentado como fonte de dado — decisão em #1746 seção 5) para
// [MetricStatus]. Troca de vocabulário-alvo trivial: esta ponte nunca dependeu de nenhum motor de
// diagnóstico — sempre foi só uma tradução de [MeasurementStatus] (integridade da MEDIÇÃO), então
// não há chamada de classificação para mover nem dado bruto para reclassificar. O mapeamento por
// status foi escolhido para preservar a MESMA cor/ícone renderizados antes (ver
// `ContinuidadeMedicaoTest`, que caracteriza a equivalência campo a campo).
//
// [ContinuidadeMedicao] em si é conteúdo, não taxonomia: é chaveado por [MeasurementStatus] e não
// introduz nenhum valor enumerado. O rótulo textual do status continua vindo de
// `MeasurementStatus.labelPt()` quando alguém precisar dele — esta tela usa título de jornada, que
// é outra coisa ("Sua rede mudou durante a medição", não "Contaminado").

/**
 * O que dizer e o que oferecer quando a medição não é completa.
 *
 * Um `null` de [continuidadeDaMedicao] significa "não há nada a explicar" — o caminho de
 * `COMPLETE`. Todo outro valor traz **ação concreta** em [rotuloAcao]; nenhum termina em texto.
 */
@Stable
internal data class ContinuidadeMedicao(
    val statusVisual: MetricStatus,
    val titulo: String,
    val explicacao: String,
    /** Rótulo do CTA. Nunca vazio — é o que impede o beco sem saída que a #1705 descreve. */
    val rotuloAcao: String,
    /**
     * A conclusão parcial pode ser mostrada junto?
     *
     * Hoje só `PARTIAL` **com medidas confiáveis** devolve `true`:
     *
     * - `CONTAMINATED` e `INCONCLUSIVE` — o dado ou é de outra rede ou não tem base estatística;
     *   mostrar conclusão ali seria inventar, defeito que o KDoc de `ResultadoIndisponivelScreen`
     *   já rejeita;
     * - `CANCELLED` — o executor não chega a produzir `ResultadoSpeedtest` nesse estado, então não
     *   há conclusão parcial a preservar (ressalva RS2);
     * - `PARTIAL` — depende de [continuidadeDaMedicao] receber `medidasConfiaveis`, porque o
     *   número que sobra pode ter sido estragado pela nossa própria medição (bloqueios B3 e B7).
     *
     * A redação anterior dizia "`true` em `PARTIAL` e `CANCELLED`, onde o que foi medido continua
     * verdadeiro". Nasceu correta e os commits seguintes desta mesma PR a deixaram para trás —
     * mesmo padrão da ressalva RS13, agora apontado como RS16.
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
internal fun continuidadeDaMedicao(
    status: MeasurementStatus,
    /**
     * Os números medidos servem para concluir? — bloqueios B3 e B7 de Caio na PR #1723.
     *
     * `calcularMeasurementStatus` produz `PARTIAL` por **duas** causas e as achata num valor só:
     *
     * - `uploadNaoDetectado` — o upload não foi medido depois de esgotar o retry, e
     *   `MainViewModel` passa `uploadMbps = 0.0` cru para o motor. `MetricClassifier` classifica
     *   como crítico, e `DiagnosticoGuiadoEngine.avaliarChamadasCongelam` conclui que "o upload
     *   está comprometido". O app afirma que o upload da pessoa está quebrado quando o que falhou
     *   foi a **nossa** medição.
     * - `downloadEncerradaPor == "download_bloqueado_429"` — o nosso próprio rate limit derrubou a
     *   fase, o download fica artificialmente baixo, e `avaliarVelocidadeNaoChega` calcula o
     *   percentual do plano direto dele. A conclusão manda acionar a operadora com um número que
     *   estragamos.
     *
     * A primeira versão desta fatia cobriu só a segunda causa — que é a secundária. A primeira é o
     * `PARTIAL` original e documentado, e é avaliada ANTES no `when` do cálculo.
     *
     * `ResultadoVelocidadeScreen` já trata o caso de upload mostrando "—"/"Não foi possível medir";
     * o sinal existe no `ResultadoSpeedtest` e só não chegava aqui. Enquanto `MeasurementStatus`
     * não distinguir as causas, quem sabe é o chamador.
     */
    medidasConfiaveis: Boolean,
): ContinuidadeMedicao? =
    when (status) {
        MeasurementStatus.COMPLETE -> null

        MeasurementStatus.PARTIAL ->
            ContinuidadeMedicao(
                // `regular`, não `ruim`/`critico`: mesmo peso visual (warningContainer) que
                // `DiagnosticStatus.attention` tinha antes da #1749 — ver MetricStatusUi.corContainer.
                statusVisual = MetricStatus.regular,
                titulo = "Consegui medir parte da sua conexão",
                // Bloqueio B9 de Caio na PR #1723: a frase era fixa e prometia "o que aparece
                // abaixo", mas com medida não confiável a tela mostra SÓ esta seção — não aparece
                // nada abaixo. E "Completar a medição" oferecia completar algo que não dá para
                // aproveitar; `remedirPelaAnalise()` mede do zero de qualquer forma.
                //
                // É a mesma classe dos bloqueios B3 e B7 virada para dentro: lá o app afirmava
                // algo falso sobre a rede da pessoa, aqui sobre a própria tela.
                explicacao =
                    if (medidasConfiaveis) {
                        "Uma das etapas não terminou, então o diagnóstico está incompleto. O que " +
                            "aparece abaixo é o que deu para apurar com segurança."
                    } else {
                        "Uma das etapas não terminou, e os números que sobraram não descrevem a " +
                            "sua conexão — não dá para concluir nada a partir deles."
                    },
                rotuloAcao = if (medidasConfiaveis) "Completar a medição" else "Medir de novo",
                // Sem medida confiável não há conclusão parcial que preste: mostrá-la seria
                // afirmar defeito da rede da pessoa com base num número que estragamos. Ver
                // [medidasConfiaveis] para as duas causas.
                permiteVerConclusaoParcial = medidasConfiaveis,
            )

        MeasurementStatus.CONTAMINATED ->
            ContinuidadeMedicao(
                statusVisual = MetricStatus.regular,
                titulo = "Sua rede mudou durante a medição",
                explicacao =
                    "Os números vieram de conexões diferentes, então não dá para comparar. " +
                        "Refaça sem trocar de Wi-Fi nem alternar para dados móveis.",
                rotuloAcao = "Refazer na mesma rede",
                permiteVerConclusaoParcial = false,
            )

        MeasurementStatus.INCONCLUSIVE ->
            ContinuidadeMedicao(
                // `inconclusivo` e não `regular`: ausência de dado não tem o mesmo peso visual de
                // um problema detectado. É o mesmo princípio que `MetricStatusUi` já documenta.
                statusVisual = MetricStatus.inconclusivo,
                titulo = "Não consegui medir o suficiente",
                // "amostras" é vocabulário nosso, não de quem usa; e "prefiro dizer isso a chutar
                // um diagnóstico" é o app se elogiando pela própria honestidade. Ressalva RS3.
                explicacao =
                    "A medição terminou cedo demais para eu ter certeza de alguma coisa sobre " +
                        "sua conexão.",
                rotuloAcao = "Medir de novo",
                permiteVerConclusaoParcial = false,
            )

        MeasurementStatus.CANCELLED ->
            ContinuidadeMedicao(
                // `inconclusivo`, não `regular`: `DiagnosticStatus.info` e `.inconclusive` já
                // renderizavam a MESMA cor/ícone (primaryContainer + Info) antes da #1749 — ver
                // DiagnosticStatusUi.kt (arquivo não tocado, continua servindo outros consumidores).
                // `MetricStatus` não tem um valor "info" separado; `inconclusivo` preserva a cor.
                statusVisual = MetricStatus.inconclusivo,
                titulo = "Você interrompeu a medição",
                // Não prometer que guardamos: `MeasurementStatus` documenta que CANCELLED "não
                // chega a gerar `ResultadoSpeedtest`" e `SpeedtestPersistenceCoordinator` o mapeia
                // para "failed". Não há o que preservar, então `permiteVerConclusaoParcial` é
                // `false` — travar `true` seria contrato observado de um estado inalcançável.
                // Ressalva RS2 de Caio na PR #1723.
                explicacao = "A medição não chegou ao fim, então não tenho conclusão para dar.",
                rotuloAcao = "Medir de novo",
                permiteVerConclusaoParcial = false,
            )
    }
