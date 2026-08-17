package io.signallq.app.ui.screen

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiTethering
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import io.signallq.app.feature.speedtest.FaseSpeedtest
import io.signallq.app.ui.LkRadius
import io.signallq.app.ui.LkSpacing
import io.signallq.app.ui.LocalLkTokens

/**
 * A rota `Analise` desenhada — spec 2.0 §8.5. Ver [EstadoAnaliseGuiada] para o porquê da rota.
 *
 * ## Por que não reusa o `GaugeCircular`
 *
 * O gauge da tela Velocidade é a visualização dominante *daquela* tela, mas ele exibe Mbps ao
 * vivo. Aqui isso contraria duas regras ao mesmo tempo: §8.5 diz que resultado parcial só aparece
 * "quando ajuda a criar confiança", e §8.6 proíbe abrir a conclusão com Mbps. Mostrar o número
 * subindo durante a análise devolve ao fluxo guiado exatamente o enquadramento de teste de
 * velocidade que o posicionamento do produto rejeita — a pessoa passa a ler o número em vez da
 * etapa. A visualização dominante aqui é o progresso da análise, sem métrica.
 *
 * ## Cancelamento
 *
 * §8.5 pede que "cancelamento preserve o que já foi medido quando tecnicamente válido". A tela não
 * decide isso e não afirma que acontece: ela chama `ExecutorSpeedtest.cancelar()` e deixa o estado
 * seguinte ser derivado do snapshot — se o executor publicar um `resultado`, [estadoAnaliseGuiada]
 * devolve `Concluida` e o fluxo segue; se não, volta a `NaoIniciada`. **Não verifiquei** se o loop
 * de execução preserva medição parcial ao cancelar no meio; o que li de `cancelar()` só mostra que
 * ele não apaga um `resultado` que já existia. Confirmar isso é trabalho da fatia de §8.5 completa.
 */
@Composable
internal fun DiagnosticoGuiadoAnaliseSection(
    estado: EstadoAnaliseGuiada,
    onCancelar: () -> Unit,
    onTentarNovamente: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalLkTokens.current
    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(horizontal = LkSpacing.xl)
                .testTag(TAG_ANALISE_GUIADA),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        when (estado) {
            is EstadoAnaliseGuiada.Falhou -> {
                Icon(
                    imageVector = Icons.Outlined.WifiTethering,
                    contentDescription = null,
                    tint = c.textTertiary,
                    modifier = Modifier.size(48.dp),
                )
                Text(
                    text = estado.mensagem,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = LkSpacing.lg),
                )
                Text(
                    text = "Verifique se você continua conectado e tente de novo.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = LkSpacing.sm),
                )
                Button(
                    onClick = onTentarNovamente,
                    shape = RoundedCornerShape(LkRadius.button),
                    colors = ButtonDefaults.buttonColors(containerColor = c.primary, contentColor = c.onPrimary),
                    modifier = Modifier.padding(top = LkSpacing.xl).testTag(TAG_ANALISE_GUIADA_TENTAR_NOVAMENTE),
                ) {
                    Text("Tentar de novo")
                }
            }

            else -> {
                // `NaoIniciada` cai aqui de propósito: quem entra na rota dispara a medição no
                // mesmo frame (ver `DiagnosticoGuiadoScreen`), então o estado é uma janela de
                // um frame. Desenhar uma tela própria para ela produziria um flash de conteúdo
                // diferente a cada análise.
                val emAndamento = estado as? EstadoAnaliseGuiada.EmAndamento
                val etapa = emAndamento?.etapa ?: etapaEmLinguagemHumana(FaseSpeedtest.idle)
                Box(contentAlignment = Alignment.Center) {
                    // Indicador DETERMINADO quando há progresso — §8.5 pede progresso, e a versão
                    // anterior desenhava um spinner indeterminado enquanto `EmAndamento.progresso`
                    // era calculado, testado e nunca chegava a pixel nenhum (ressalva RS3 de Caio
                    // na PR #1719: cobertura de função pura não é evidência de comportamento).
                    //
                    // Decorativo para o TalkBack: quem anuncia o andamento é o texto da etapa
                    // abaixo. Sem isso o leitor de tela lê "carregando" por cima de uma frase que
                    // já diz mais.
                    // `testTag` por ramo, e não asserção sobre `ProgressBarRangeInfo`: o
                    // `clearAndSetSemantics` acima apaga exatamente essa propriedade, então o
                    // matcher de semântica não alcança o indicador. A tag testa a mesma coisa que
                    // Caio pediu na RS13 — a ESCOLHA DO RAMO — sem desfazer a decisão de
                    // acessibilidade para acomodar o teste.
                    // A tag vai DENTRO do bloco de semântica: `clearAndSetSemantics` apaga tudo do
                    // nó, `testTag` de modifier incluído. Descobri isso rodando o teste.
                    fun indicador(tag: String) =
                        Modifier.size(96.dp).clearAndSetSemantics { testTag = tag }
                    if (emAndamento == null) {
                        CircularProgressIndicator(
                            modifier = indicador(TAG_ANALISE_GUIADA_INDETERMINADO),
                            color = c.primary,
                            trackColor = c.bgSecondary,
                        )
                    } else {
                        val progressoAnimado by
                            animateFloatAsState(
                                targetValue = emAndamento.progresso,
                                animationSpec = tween(durationMillis = 500, easing = FastOutSlowInEasing),
                                label = "progressoAnaliseGuiada",
                            )
                        CircularProgressIndicator(
                            progress = { progressoAnimado },
                            modifier = indicador(TAG_ANALISE_GUIADA_DETERMINADO),
                            color = c.primary,
                            trackColor = c.bgSecondary,
                        )
                    }
                }
                Text(
                    text = etapa,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.W600,
                    color = c.textPrimary,
                    textAlign = TextAlign.Center,
                    modifier =
                        Modifier
                            .padding(top = LkSpacing.xl)
                            .testTag(TAG_ANALISE_GUIADA_ETAPA)
                            .clearAndSetSemantics { contentDescription = "Analisando: $etapa" },
                )
                Text(
                    text = "Estou medindo sua conexão para responder com base em evidência, não em palpite.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = c.textSecondary,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = LkSpacing.sm),
                )
                TextButton(
                    onClick = onCancelar,
                    modifier = Modifier.padding(top = LkSpacing.lg).testTag(TAG_ANALISE_GUIADA_CANCELAR),
                ) {
                    Text("Cancelar", color = c.textSecondary)
                }
            }
        }
    }
}

internal const val TAG_ANALISE_GUIADA = "diagnostico_guiado_analise"
internal const val TAG_ANALISE_GUIADA_DETERMINADO = "diagnostico_guiado_analise_progresso_determinado"
internal const val TAG_ANALISE_GUIADA_INDETERMINADO = "diagnostico_guiado_analise_progresso_indeterminado"
internal const val TAG_ANALISE_GUIADA_ETAPA = "diagnostico_guiado_analise_etapa"
internal const val TAG_ANALISE_GUIADA_CANCELAR = "diagnostico_guiado_analise_cancelar"
internal const val TAG_ANALISE_GUIADA_TENTAR_NOVAMENTE = "diagnostico_guiado_analise_tentar_novamente"
