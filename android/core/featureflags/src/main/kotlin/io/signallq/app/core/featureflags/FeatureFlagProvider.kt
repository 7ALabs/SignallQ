package io.signallq.app.core.featureflags

import kotlinx.coroutines.flow.Flow

/**
 * Unico ponto de acesso a feature flags do Consumer via Firebase Remote Config
 * (Epico #1347, fundacao na issue #1477). Nenhuma feature deve tocar o SDK do
 * Firebase Remote Config diretamente -- sempre atraves deste contrato.
 *
 * Distinto de `io.signallq.app.core.network.FeatureFlagProvider` (mecanismo
 * legado baseado em HTTP `GET /flags` do `signallq-admin-worker`, SIG-13) --
 * aquele fica mantido so para os consumidores ja existentes
 * (`DiagnosticDivergenceReporter`) ate a migracao prevista em F4 (#1480); nenhum
 * consumidor novo deve ser escrito contra o contrato legado a partir de agora.
 */
interface FeatureFlagProvider {
    /** Emite o valor efetivo de [key] a cada mudanca (defaults, fetch, ou update
     *  em tempo real quando aplicavel). Primeira emissao e sempre o default local
     *  -- nunca suspende esperando rede. */
    fun observe(key: FeatureFlagKey): Flow<FeatureFlagValue>

    /** Leitura sincrona do valor booleano efetivo de [key]. Fallback `false` para
     *  chave desconhecida ou tipo != BOOLEAN -- nunca lanca excecao. */
    fun isEnabled(key: FeatureFlagKey): Boolean

    /**
     * Dispara fetch do template remoto e ativa a config mais recente.
     *
     * [force] = false (padrao): respeita o intervalo minimo de fetch configurado
     * no `FirebaseRemoteConfig` compartilhado (throttling padrao do SDK).
     * [force] = true: ignora o throttling e busca imediatamente (uso tipico:
     * botao "atualizar agora" em uma tela de debug/admin local, nao chamado em
     * fluxo normal do app).
     *
     * `fetchAndActivate()`/`activate()` retornando `false` (nenhuma config NOVA
     * precisou ser ativada) NUNCA e tratado como erro -- vira [FeatureFlagRefreshResult.Success]
     * com `activated=false`. So excecao/timeout real do SDK vira [FeatureFlagRefreshResult.Failure].
     * Em qualquer um dos dois casos, os valores em memoria so sao atualizados se a
     * leitura pos-fetch do SDK for bem-sucedida -- uma falha de rede nunca apaga a
     * ultima configuracao valida.
     */
    suspend fun refresh(force: Boolean = false): FeatureFlagRefreshResult
}
