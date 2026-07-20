package io.signallq.app.ui.screen

import androidx.compose.runtime.Stable
import io.signallq.app.ads.AdsFlags
import io.signallq.app.core.network.wifi.SnapshotScanWifi
import io.signallq.app.core.recommendation.RecommendationDecision
import io.signallq.app.core.recommendation.RecommendationFeedbackType
import io.signallq.app.feature.devices.ResultadoCorrelacaoTopologia
import io.signallq.app.feature.devices.SnapshotScanDispositivos
import io.signallq.app.feature.diagnostico.SnapshotDiagnostico
import io.signallq.app.feature.diagnostico.ai.AiAcaoRecomendada
import io.signallq.app.feature.speedtest.ModoSpeedtest
import io.signallq.app.feature.speedtest.SnapshotExecucaoSpeedtest
import io.signallq.app.feature.wifi.RedeVizinha

/**
 * Agrupa parametros do speedtest para reduzir a assinatura do AppShell.
 * @Stable garante que o Compose nao recompoe a arvore inteira quando apenas
 * um campo muda — desde que o conteudo semantico dos campos nao mude.
 */
@Stable
data class AppShellSpeedtestState(
    val snapshotSpeedtest: SnapshotExecucaoSpeedtest,
    val speedtestPendenteModoMovel: ModoSpeedtest? = null,
    val speedtestPermiteHeavyMovel: Boolean = false,
    val speedtestMbConsumidosMes: Long = 0L,
    val onNovoTeste: (ModoSpeedtest) -> Unit,
    /**
     * Igual a [onNovoTeste], mas para quando o usuario ja confirmou o aviso de dados moveis
     * (ForaDoWifiDialog, Home) — pula o segundo gate de confirmacao em rede medida, que nao
     * tem UI fora da tab Velocidade (#516).
     */
    val onNovoTesteJaConfirmadoMovel: (ModoSpeedtest) -> Unit = onNovoTeste,
    val onCancelarTeste: () -> Unit,
    val onConfirmarSpeedtestMovel: () -> Unit = {},
    val onCancelarSpeedtestMovel: () -> Unit = {},
    val onSetSpeedtestPermiteHeavyMovel: (Boolean) -> Unit = {},
)

/**
 * Agrupa parametros de Wi-Fi e dispositivos para reduzir a assinatura do AppShell.
 */
@Stable
data class AppShellWifiState(
    val snapshotWifi: SnapshotScanWifi,
    val connectedNetwork: RedeVizinha?,
    val snapshotDevices: SnapshotScanDispositivos,
    val apelidos: Map<String, String>,
    val onRefreshDispositivos: () -> Unit,
    val onRefreshSinal: () -> Unit,
    val onSalvarApelido: (mac: String, apelido: String) -> Unit,
    /** #983 (Fase 4) — correlacao best-effort topologia/gateway, chaveada por id do dispositivo
     *  (ver MainViewModel.correlacoesTopologia). */
    val correlacoesTopologia: Map<String, ResultadoCorrelacaoTopologia> = emptyMap(),
)

/**
 * Estado da chamada de IA de diagnostico -- mecanismo UNICO reaproveitado tanto pela
 * tela 1a "Analise detalhada" (spec To-Be, GH#design-tobe-alinhamento -- aberta
 * automaticamente ao abrir o sheet, sem escolha de sintoma, `problema = null`) quanto
 * pelo fluxo legado "Analisar meu problema com IA" por sintoma escolhido
 * (`AnaliseDetalhadaBottomSheet.kt`, `problema` preenchido). Confirmado por decisao do
 * Luiz (2026-07-14): nao sao dois recursos paralelos, e a MESMA chamada
 * (`AiDiagnosisRepository.explainDiagnosis` + `AiFallbackFactory.fromLocal` no
 * fallback), so com gatilho diferente.
 */
sealed class AnalisadorState {
    data object Inativo : AnalisadorState()

    data object Analisando : AnalisadorState()

    data class Resultado(
        val texto: String,
        val origem: String,
        val acoes: List<AiAcaoRecomendada> = emptyList(),
        /** Titulo curto humanizado (AiDiagnosisResult.titulo, "5-8 palavras"). Usado
         *  pelo banner compacto da tela 1a. Vazio em respostas antigas/nao populadas. */
        val titulo: String = "",
        /** Resumo em 1-2 frases (AiDiagnosisResult.resumo) — mais compacto que [texto]
         *  (que prioriza textoLaudo, um paragrafo). A tela 1a usa [resumo] primeiro;
         *  o sheet "Analisar meu problema com IA" continua usando [texto]. */
        val resumo: String = "",
        /** Sintoma que o usuario descreveu explicitamente (fluxo legado por sintoma
         *  escolhido). Nulo quando a analise foi disparada automaticamente pela tela 1a
         *  (`problema = null` em MainViewModel.analisarProblema) -- usado pra distinguir
         *  "laudo automatico" de "analise que o usuario pediu" na copy (follow-up Lia,
         *  PR #1013). */
        val problemaRelatado: String? = null,
    ) : AnalisadorState()

    data class Erro(
        val mensagem: String,
    ) : AnalisadorState()
}

/**
 * Agrupa parametros do diagnostico.
 */
@Stable
data class AppShellDiagnosticoState(
    val snapshotDiagnostico: SnapshotDiagnostico,
    val onIniciarDiagnostico: () -> Unit,
    val analisadorState: AnalisadorState = AnalisadorState.Inativo,
    /** `problema = null` quando acionado automaticamente pela tela 1a (sem sintoma
     *  escolhido); `problema` preenchido quando vem do fluxo por sintoma
     *  (`AnaliseDetalhadaBottomSheet`). Mesmo mecanismo, gatilhos diferentes. */
    val onAnalisarProblema: (String?) -> Unit = {},
    val onResetarAnalisador: () -> Unit = {},
    /** SIG-173/#664 — chamado quando o usuario fecha o LaudoScreen. Avalia elegibilidade
     *  para o prompt nativo de avaliacao do Google Play (nunca bloqueia o fechamento). */
    val onLaudoFechado: () -> Unit = {},
    /** Recomendacao do Recommendation Engine (#790/#811/#812) para o diagnostico atual --
     *  null quando `RecommendationEngine.choose` nao encontrou nada elegivel, ou depois que
     *  o usuario deu feedback "ocultar" (#813). */
    val recommendationDecision: RecommendationDecision? = null,
    /** Feedback ja dado pelo usuario para [recommendationDecision] nesta sessao, ou null. */
    val recommendationFeedback: RecommendationFeedbackType? = null,
    val onRecommendationShown: () -> Unit = {},
    val onRecommendationClicked: () -> Unit = {},
    val onRecommendationFeedback: (RecommendationFeedbackType) -> Unit = {},
)

/**
 * Agrupa parametros usados fora da tela SignallQ standalone (removida na Fase 8 MD3 —
 * GH#937, decisao #2 do plano: dead code sem rota/overlay alcancavel desde a Fase 1).
 * [operadoraMovel] alimenta a Analise Detalhada (1a) em ResultadoVelocidadeScreen;
 * [onVerificarGemma]/[gemmaAvailable] fazem o healthcheck de IA ao entrar na tab Velocidade.
 */
@Stable
data class AppShellSignallQState(
    val gemmaAvailable: Boolean = false,
    val operadoraMovel: String? = null,
    val onVerificarGemma: () -> Unit = {},
)

/**
 * Agrupa o estado de monetizacao nativa (issue #555): flags remotas por tela + gate
 * de consentimento UMP. [podeRequisitarAnuncio] cobre tanto "UMP ainda nao respondeu"
 * quanto "usuario recusou personalizacao" -- em ambos os casos as telas nao chamam
 * `AdLoader.loadAd`, nao so nao mostram o card.
 */
@Stable
data class AppShellAdsState(
    val flags: AdsFlags = AdsFlags.DESLIGADO,
    val podeRequisitarAnuncio: Boolean = false,
)
