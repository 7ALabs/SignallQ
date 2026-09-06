package io.signallq.app.core.featureflags

/**
 * Constantes tipadas das chaves do catalogo -- unico jeito permitido de referenciar
 * uma flag em codigo de feature (criterio de aceite da issue #1477: "catalogo tipado
 * sem strings de chave soltas nas features").
 *
 * Toda chave aqui **precisa** existir em `consumer-catalog.json`, e vice-versa --
 * ver `FeatureFlagKeysParityTest` (garante que as duas fontes nunca divergem
 * conforme flags novas forem adicionadas).
 *
 * [CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED] continua smoke-test da fundacao
 * (issue #1477, `androidImplemented=false`) -- nao instrumentada por F4/#1480, que
 * cobriu so a chave principal `enabled` de cada um dos 9 modulos feature. As
 * demais 9 chaves abaixo (uma por modulo `:feature:*` do Consumer) sao reais desde
 * F4/#1480: cada uma gateia entrada de navegacao/overlay do respectivo modulo em
 * `AppShell.kt` (ver `AppShellFeatureGating.kt`).
 *
 * [CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED] (issue #1497) migrou o kill switch do
 * shadow mode de diagnostico (comparacao local-vs-remoto, GH#1444/#1445, parte de
 * #952) do sistema legado SIG-13 (`io.signallq.app.core.network.FeatureFlagProvider`,
 * chave `feature_diagnostic_shadow_mode`) para este catalogo -- unico consumidor real
 * do sistema legado, agora migrado.
 *
 * [CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED] (NDS-02k, issue #1759) liga a chamada
 * viva ao NDS (`NdsClient.evaluate`, via `NdsDiagnosticRepository`) como fonte do
 * `DiagnosticOrchestrator`, no lugar do shadow mode acima -- mutuamente exclusivos
 * (flag ligada desliga o shadow mode automaticamente, ver kdoc de
 * `DiagnosticOrchestrator.executarProtegido`). O catálogo local usa default `true`:
 * o NDS é tentado como fonte principal e o motor local assume automaticamente quando
 * a chamada falha. O Remote Config continua podendo desligar a chamada sem publicar
 * outra versão do app.
 *
 * [USAR_NDS_V2_NO_ASSIST] (feat/nds-client-v2) liga o uso do contrato
 * `POST /v2/diagnostics/evaluate` do NDS (contexto opcional,
 * resposta `{raw, explanation}`) no caminho dedicado do Assist
 * (`NdsDiagnosticRepository.evaluateForAssist`), no lugar do `/v1/diagnostics/evaluate`
 * atual. O catálogo local inicia ligada; a rota v2 aceita contexto parcial. O endpoint v2 foi
 * publicado pelo NDS na PR #24. A reversão continua disponível via Remote Config.
 *
 * [USAR_NDS_V2_NO_FLUXO_PRINCIPAL] (feat/nds-v2-fluxo-principal) segue o mesmo padrão de
 * [USAR_NDS_V2_NO_ASSIST], mas para o fluxo principal de diagnóstico
 * (`DiagnosticOrchestrator.executarProtegido` → `NdsDiagnosticRepository.evaluate`, caminho
 * ligado por [CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED] acima). O v2 é estritamente aditivo sobre
 * o v1 (request só ganha campos opcionais a mais; response só troca o envelope de
 * `{...}` direto para `{raw: {...}, explanation: {...}}`, já suportado pelo parser usado no
 * Assist), então não há risco de schema na migração — só uma troca de rota. Default `true`
 * torna o v2 o caminho padrão; a flag desligada preserva o `/v1/diagnostics/evaluate` como
 * kill-switch de emergência. A reversão continua disponível via Remote Config, sem publicar nova
 * versão do app.
 */
object FeatureFlagKeys {
    val CONSUMER_SPEEDTEST_ENABLED = FeatureFlagKey("consumer_speedtest_enabled")
    val CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED = FeatureFlagKey("consumer_speedtest_cloudflare_engine_enabled")
    val CONSUMER_HOME_ENABLED = FeatureFlagKey("consumer_home_enabled")
    val CONSUMER_WIFI_ENABLED = FeatureFlagKey("consumer_wifi_enabled")
    val CONSUMER_DEVICES_ENABLED = FeatureFlagKey("consumer_devices_enabled")
    val CONSUMER_DNS_ENABLED = FeatureFlagKey("consumer_dns_enabled")
    val CONSUMER_FIBRA_ENABLED = FeatureFlagKey("consumer_fibra_enabled")
    val CONSUMER_DIAGNOSTICO_ENABLED = FeatureFlagKey("consumer_diagnostico_enabled")
    val CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED = FeatureFlagKey("consumer_diagnostico_shadow_mode_enabled")
    val CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED = FeatureFlagKey("consumer_diagnostico_nds_live_enabled")
    val USAR_NDS_V2_NO_ASSIST = FeatureFlagKey("consumer_diagnostico_assist_nds_v2_enabled")
    val USAR_NDS_V2_NO_FLUXO_PRINCIPAL = FeatureFlagKey("consumer_diagnostico_nds_v2_enabled")
    val CONSUMER_HISTORY_ENABLED = FeatureFlagKey("consumer_history_enabled")
    val CONSUMER_SETTINGS_ENABLED = FeatureFlagKey("consumer_settings_enabled")

    /** Todas as constantes declaradas aqui -- usado pelo teste de paridade catalogo/codigo. */
    val ALL: List<FeatureFlagKey> =
        listOf(
            CONSUMER_SPEEDTEST_ENABLED,
            CONSUMER_SPEEDTEST_CLOUDFLARE_ENGINE_ENABLED,
            CONSUMER_HOME_ENABLED,
            CONSUMER_WIFI_ENABLED,
            CONSUMER_DEVICES_ENABLED,
            CONSUMER_DNS_ENABLED,
            CONSUMER_FIBRA_ENABLED,
            CONSUMER_DIAGNOSTICO_ENABLED,
            CONSUMER_DIAGNOSTICO_SHADOW_MODE_ENABLED,
            CONSUMER_DIAGNOSTICO_NDS_LIVE_ENABLED,
            USAR_NDS_V2_NO_ASSIST,
            USAR_NDS_V2_NO_FLUXO_PRINCIPAL,
            CONSUMER_HISTORY_ENABLED,
            CONSUMER_SETTINGS_ENABLED,
        )

    /** Os 9 modulos feature do Consumer instrumentados por F4/#1480, na ordem do
     *  catalogo/Epico #1347 -- usado por [ALL] e por testes que precisam iterar
     *  so as flags principais de modulo (exclui o smoke-test de engine). */
    val CONSUMER_MODULE_KEYS: List<FeatureFlagKey> =
        listOf(
            CONSUMER_HOME_ENABLED,
            CONSUMER_SPEEDTEST_ENABLED,
            CONSUMER_WIFI_ENABLED,
            CONSUMER_DEVICES_ENABLED,
            CONSUMER_DNS_ENABLED,
            CONSUMER_FIBRA_ENABLED,
            CONSUMER_DIAGNOSTICO_ENABLED,
            CONSUMER_HISTORY_ENABLED,
            CONSUMER_SETTINGS_ENABLED,
        )
}
