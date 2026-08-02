package io.signallq.app.core.diagnostico

/**
 * Espelho da resposta publica `GET /diagnostic/rollout-status` do worker
 * `signallq-diagnostic` — GH#1445, parte de #952. Buscado pelo cliente ANTES
 * de decidir se aquela instalacao participa do shadow mode (ver
 * [DiagnosticRolloutEligibility]).
 *
 * `rulesetVersion == null` significa que nao ha ruleset PUBLISHED no worker
 * (D1 nao configurado ou nenhuma publicacao ainda) — nesse caso [rolloutPercent]
 * vem sempre 0 do worker (fail closed), nunca inferido como 100 aqui.
 */
data class DiagnosticRolloutStatus(
    val rulesetVersion: Int?,
    val rolloutPercent: Int,
    val rolloutMinVersionCode: Int? = null,
    val rolloutChannels: List<String>? = null,
)

/**
 * Decide, a partir de [DiagnosticRolloutStatus] e de sinais da instalacao
 * atual, se ela participa do shadow mode — GH#1445.
 *
 * ## Escopo desta decisao (nao confundir com avaliacao autoritativa)
 * Isto controla SO quem tem uma avaliacao remota comparada contra o motor
 * local (telemetria/comparacao, shadow mode de #1444) — nunca quem ve o
 * resultado remoto como autoritativo. Essa segunda decisao continua fora de
 * escopo ate uma meta de equivalencia ser definida e validada com dados reais
 * (ver #952, secao "Shadow mode"; #1445 nao avanca essa etapa).
 *
 * ## Segmentacao implementada
 * Versao minima do app ([DiagnosticRolloutStatus.rolloutMinVersionCode]) e
 * canal de distribuicao ([DiagnosticRolloutStatus.rolloutChannels]) — as
 * duas reaproveitam sinais que o app ja calcula hoje para outros fins
 * (`versionCode`/`distributionChannel()`, ja usados por `AdminSyncWorker` e
 * `CompositeAnalyticsTracker`), em vez de depender de #1312/#1347 (ainda nao
 * prontos/consumiveis no momento desta issue). Fora de escopo, documentado e
 * nao implementado: equipamento/firmware, regiao/provedor (ver corpo original
 * de #952) — quem cria segmentacao nova adiciona campo em
 * [DiagnosticRolloutStatus] e condicao aqui, sem reinventar o mecanismo.
 */
object DiagnosticRolloutEligibility {

    fun isEligible(
        status: DiagnosticRolloutStatus,
        installationId: String,
        appVersionCode: Int,
        appChannel: String,
    ): Boolean {
        val minVersionCode = status.rolloutMinVersionCode
        if (minVersionCode != null && appVersionCode < minVersionCode) return false

        val channels = status.rolloutChannels
        if (!channels.isNullOrEmpty() && appChannel !in channels) return false

        return RolloutBucketCalculator.isInRollout(installationId, status.rolloutPercent)
    }
}
