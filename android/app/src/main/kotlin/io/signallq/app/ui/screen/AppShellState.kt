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
import io.signallq.app.feature.speedtest.connectivity.ConnectivityDiagnosisMensagem
import io.signallq.app.feature.wifi.RedeVizinha
import io.signallq.app.ui.OperadoraSource
import io.signallq.app.ui.ResolvedOperadoraContact
import io.signallq.app.ui.ResolvedOperadoraIdentity

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
    /** GH#1512 — conclusao do diagnostico local quando o Speedtest e interrompido por
     *  Wi-Fi conectado sem internet. null = sem pendencia. */
    val diagnosticoConectividade: ConnectivityDiagnosisMensagem? = null,
    val onLimparDiagnosticoConectividade: () -> Unit = {},
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
 * pelo fluxo com objetivo/sintoma escolhido pelo usuario (`problema` preenchido --
 * hoje `DiagnosticoGuiadoScreen` e `ModoGamerScreen`; o sheet legado por sintoma fixo
 * `AnaliseDetalhadaBottomSheet` foi removido em #1485). Confirmado por decisao do
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
         *  (que prioriza textoLaudo, um paragrafo). Consumidor real hoje:
         *  `DiagnosticoResultadoComponents.kt` (`resumo.ifBlank { texto }`), que prefere
         *  o resumo e cai pro [texto] em respostas antigas/nao populadas. O fallback
         *  segue valido; a justificativa anterior citava o sheet "Analisar meu problema
         *  com IA", removido em #1485. */
        val resumo: String = "",
        /** Sintoma que o usuario descreveu explicitamente (fluxo legado por sintoma
         *  escolhido). Nulo quando a analise foi disparada automaticamente pela tela 1a
         *  (`problema = null` em MainViewModel.analisarProblema) -- usado pra distinguir
         *  "laudo automatico" de "analise que o usuario pediu" na copy (follow-up Lia,
         *  PR #1013). */
        val problemaRelatado: String? = null,
        /** GH#1657 (spec §14.4) / GH#1707 — rótulo textual de confiança (`baixa`/`media`/`alta`,
         *  ver `RotuloConfianca`), nunca número. Vazio quando a origem não calculou (não deve
         *  acontecer nos 3 pontos de construção reais em `MainViewModel.analisarProblema`, que
         *  sempre têm um `DiagnosticReport` em mãos). */
        val confianca: String = "",
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
    val onSolicitarDiagnostico: () -> Long? = {
        onIniciarDiagnostico()
        null
    },
    val analisadorState: AnalisadorState = AnalisadorState.Inativo,
    /** `problema = null` quando acionado automaticamente pela tela 1a (sem sintoma
     *  escolhido); `problema` preenchido quando vem do fluxo com objetivo escolhido
     *  (`DiagnosticoGuiadoScreen`/`ModoGamerScreen`). Mesmo mecanismo, gatilhos
     *  diferentes. */
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
    /** GH#1707 (Task 2.0.09e, parte 2/2) — CTA "Testar novamente" vinculado à análise original
     *  (spec §8.8). Recebe `analiseId` da jornada e o id da ação anterior executada (vazio se
     *  nenhuma). */
    val onTestarNovamenteVinculado: (analiseId: String, acaoAnteriorId: String) -> Unit = { _, _ -> },
    /** GH#1707 — estado do reteste vinculado em curso (spec §14.6). */
    val comparacaoRetesteState: ComparacaoRetesteUiState = ComparacaoRetesteUiState.Ausente,
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

/**
 * Estado de gate de navegacao dos 9 modulos `:feature:*` do Consumer via Firebase Remote
 * Config (Epico #1347, F4/#1480) -- ja resolvido em booleano (default local do catalogo
 * ate o primeiro fetch, valor remoto depois). AppShell nunca consulta `FeatureFlagProvider`
 * diretamente; so le estes campos (ver [io.signallq.app.featureflags.ConsumerFeatureGateCoordinator],
 * que produz este estado a partir do provider real).
 *
 * Todo default e `true` (as 9 features ja sao publicadas/estaveis -- ver KDoc da issue).
 * [Overlay.Privacidade]/[Overlay.Termos] (obrigacao legal/LGPD) e o proprio hub Ferramentas
 * (nao pertencem a um unico modulo) nunca sao gateados por nenhum destes campos.
 */
@Stable
data class AppShellFeatureFlagsState(
    val guidedShell2Enabled: Boolean = false,
    val homeEnabled: Boolean = true,
    val speedtestEnabled: Boolean = true,
    val wifiEnabled: Boolean = true,
    val devicesEnabled: Boolean = true,
    val dnsEnabled: Boolean = true,
    val fibraEnabled: Boolean = true,
    val diagnosticoEnabled: Boolean = true,
    val historyEnabled: Boolean = true,
    val settingsEnabled: Boolean = true,
    /** Chamado quando o usuario tenta alcancar uma rota/overlay cujo modulo esta desligado --
     *  [moduleId] e o identificador curto (ex.: "wifi", "dns"), mesmo padrao ja usado por
     *  `analyticsTracker.registrarFeatureUsada` nos call sites de MainActivity. */
    val onFeatureBlocked: (moduleId: String) -> Unit = {},
)

internal val AppShellFeatureFlagsState.shellMode: AppShellMode
    get() = if (guidedShell2Enabled) AppShellMode.Guided2 else AppShellMode.Legacy

/**
 * Cadeia de resolução de identidade e contato de operadora (GH#970): nível 1 local e síncrono
 * (catálogo embutido, sem I/O) e a cadeia completa suspensa (local → diretório remoto do worker
 * `signallq-diagnostic` → fallback genérico).
 *
 * Agrupado na issue #1704 pelo mesmo motivo dos demais `AppShellXxxState`: eram **4 parâmetros
 * soltos ocupando 28 linhas** da assinatura do [AppShell], por causa dos dois lambdas de fallback
 * default — que agora vivem aqui, ao lado do tipo que descrevem, e não no meio da lista de
 * parâmetros do arquivo central.
 *
 * Consumido por `DiagnosticoGuiadoScreen` (via `AppShellDiagnosticoGuiadoEntry`), `HomeScreen` e
 * `SinalScreen`. A instância real é montada na `MainActivity` a partir do
 * `OperadoraDirectoryResolver` injetado por Hilt — o [AppShell] só repassa, nunca resolve nada.
 */
@Stable
data class AppShellOperadoraResolvers(
    val identidadeLocal: (String?, Boolean) -> ResolvedOperadoraIdentity? = { _, _ -> null },
    val contatoLocal: (String?, Boolean) -> ResolvedOperadoraContact? = { _, _ -> null },
    val identidadeRemota: suspend (String?, Boolean) -> ResolvedOperadoraIdentity =
        { nome, _ ->
            ResolvedOperadoraIdentity(
                displayName = nome ?: "Operadora",
                monograma = nome?.firstOrNull()?.uppercase() ?: "?",
                corMarca = null,
                logoRes = null,
                logoUrl = null,
                source = OperadoraSource.FALLBACK,
            )
        },
    val contatoRemoto: suspend (String?, Boolean) -> ResolvedOperadoraContact =
        { nome, _ ->
            ResolvedOperadoraContact(
                displayName = nome ?: "Operadora",
                sacPhone = null,
                whatsapp = null,
                site = null,
                source = OperadoraSource.FALLBACK,
            )
        },
)
