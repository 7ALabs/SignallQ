package io.signallq.app.featureflags

import io.signallq.app.core.featureflags.FeatureFlagKeys
import io.signallq.app.core.featureflags.FeatureFlagProvider
import io.signallq.app.core.network.AnalyticsTracker
import io.signallq.app.di.ApplicationScope
import io.signallq.app.ui.screen.AppShellFeatureFlagsState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Deriva o gate de navegacao dos 9 modulos `:feature:*` do Consumer (Epico #1347,
 * F4/#1480) a partir do [FeatureFlagProvider] real (fundacao F1/#1477) -- extraido
 * de `MainViewModel` para nao adicionar responsabilidade nova a um arquivo ja
 * classificado como divida critica (regra de higiene, secao 4.2: "nao adicione
 * responsabilidade diretamente, extraia"). `MainViewModel` so expoe [uiState] e
 * delega [registrarBloqueio] -- nenhuma logica de flag mora la.
 *
 * Reativo (`FeatureFlagProvider.observe`, nao `isEnabled` polled): cobre o requisito
 * "mudancas em runtime" do Epico sem exigir reinicio do app quando o Admin publica
 * um novo valor e o proximo `refresh()` (chamado em `SignallQApplication.onCreate`,
 * ou por um botao futuro de atualizar) ativa a config nova.
 */
@Singleton
class ConsumerFeatureGateCoordinator
    @Inject
    constructor(
        private val featureFlagProvider: FeatureFlagProvider,
        private val analyticsTracker: AnalyticsTracker,
        @ApplicationScope private val applicationScope: CoroutineScope,
    ) {
        val uiState: StateFlow<AppShellFeatureFlagsState> =
            combine(
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_HOME_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_SPEEDTEST_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_WIFI_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_DEVICES_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_DNS_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_FIBRA_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_DIAGNOSTICO_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_HISTORY_ENABLED),
                featureFlagProvider.observe(FeatureFlagKeys.CONSUMER_SETTINGS_ENABLED),
            ) { valores ->
                AppShellFeatureFlagsState(
                    homeEnabled = valores[0].raw.asBooleanOrNull() ?: true,
                    speedtestEnabled = valores[1].raw.asBooleanOrNull() ?: true,
                    wifiEnabled = valores[2].raw.asBooleanOrNull() ?: true,
                    devicesEnabled = valores[3].raw.asBooleanOrNull() ?: true,
                    dnsEnabled = valores[4].raw.asBooleanOrNull() ?: true,
                    fibraEnabled = valores[5].raw.asBooleanOrNull() ?: true,
                    diagnosticoEnabled = valores[6].raw.asBooleanOrNull() ?: true,
                    historyEnabled = valores[7].raw.asBooleanOrNull() ?: true,
                    settingsEnabled = valores[8].raw.asBooleanOrNull() ?: true,
                    onFeatureBlocked = ::registrarBloqueio,
                )
            }.stateIn(
                applicationScope,
                SharingStarted.Eagerly,
                AppShellFeatureFlagsState(onFeatureBlocked = ::registrarBloqueio),
            )

        /** Chamado pela UI quando o usuario tenta alcancar uma rota/overlay cujo modulo
         *  esta desligado remotamente -- dispara `feature_blocked_remote` (sem dado
         *  pessoal, so o id curto do modulo, ver [AnalyticsTracker.registrarFeatureBloqueadaRemota]). */
        fun registrarBloqueio(moduleId: String) {
            Timber.i("Feature bloqueada remotamente: consumer.$moduleId.enabled")
            analyticsTracker.registrarFeatureBloqueadaRemota(moduleId)
        }
    }
