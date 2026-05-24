package io.linka.app.kotlin

/**
 * Controle de features por build type.
 *
 * debug  → todas as flags são true (desenvolvedor testa tudo)
 * release → apenas MVP é true (usuário vê só o que está pronto)
 *
 * Para ativar uma feature no release:
 * 1. Alterar o buildConfigField correspondente em app/build.gradle.kts
 * 2. Incrementar versionCode e versionName
 * 3. Gerar novo APK de release
 *
 * Nunca alterar este arquivo manualmente — os valores vêm do BuildConfig.
 */
object FeatureFlags {
    // ─── MVP ─────────────────────────────────────────────────────────────────
    val SPEEDTEST: Boolean get() = BuildConfig.FEATURE_SPEEDTEST
    val DIAGNOSTICO_LOCAL: Boolean get() = BuildConfig.FEATURE_DIAGNOSTICO_LOCAL
    val DIAGNOSTICO_IA: Boolean get() = BuildConfig.FEATURE_DIAGNOSTICO_IA
    val DIAGNOSTICO_CHAT: Boolean get() = BuildConfig.FEATURE_DIAGNOSTICO_CHAT
    val WIFI_ANALISE: Boolean get() = BuildConfig.FEATURE_WIFI_ANALISE
    val REDE_MOVEL_ANALISE: Boolean get() = BuildConfig.FEATURE_REDE_MOVEL_ANALISE
    val HISTORICO: Boolean get() = BuildConfig.FEATURE_HISTORICO
    val LAUDO_PDF: Boolean get() = BuildConfig.FEATURE_LAUDO_PDF
    val ONBOARDING: Boolean get() = BuildConfig.FEATURE_ONBOARDING
    val PERMISSOES_CONTEXTO: Boolean get() = BuildConfig.FEATURE_PERMISSOES_CONTEXTO
    val ESTADO_OFFLINE: Boolean get() = BuildConfig.FEATURE_ESTADO_OFFLINE
    val SETTINGS_MVP: Boolean get() = BuildConfig.FEATURE_SETTINGS_MVP
    val PRIVACIDADE_TELA: Boolean get() = BuildConfig.FEATURE_PRIVACIDADE_TELA
    val NOVIDADES_TELA: Boolean get() = BuildConfig.FEATURE_NOVIDADES_TELA

    // ─── Sprint 1 ─────────────────────────────────────────────────────────────
    val LINKPULSE_ATIVO: Boolean get() = BuildConfig.FEATURE_LINKPULSE_ATIVO
    val NOTIFICACAO_INLINE: Boolean get() = BuildConfig.FEATURE_NOTIFICACAO_INLINE
    val WIDGET: Boolean get() = BuildConfig.FEATURE_WIDGET
    val QUICK_SETTINGS_TILE: Boolean get() = BuildConfig.FEATURE_QUICK_SETTINGS_TILE

    // ─── Sprint 2 ─────────────────────────────────────────────────────────────
    val PROVA_REAL_COMPLETO: Boolean get() = BuildConfig.FEATURE_PROVA_REAL_COMPLETO
    val DIAGNOSTICO_ITERATIVO: Boolean get() = BuildConfig.FEATURE_DIAGNOSTICO_ITERATIVO
    val TRACEROUTE: Boolean get() = BuildConfig.FEATURE_TRACEROUTE

    // ─── Sprint 3 ─────────────────────────────────────────────────────────────
    val FIBRA_SCREEN: Boolean get() = BuildConfig.FEATURE_FIBRA_SCREEN
    val DNS_SCREEN: Boolean get() = BuildConfig.FEATURE_DNS_SCREEN
    val DEVICES_SCREEN_V2: Boolean get() = BuildConfig.FEATURE_DEVICES_SCREEN_V2
    val TELEPHONY_AVANCADO: Boolean get() = BuildConfig.FEATURE_TELEPHONY_AVANCADO

    // ─── Sprint 4 ─────────────────────────────────────────────────────────────
    val MAPA_CALOR_WIFI: Boolean get() = BuildConfig.FEATURE_MAPA_CALOR_WIFI
    val AGENDAMENTO_TESTES: Boolean get() = BuildConfig.FEATURE_AGENDAMENTO_TESTES
    val LINKPULSE_CHAT: Boolean get() = BuildConfig.FEATURE_LINKPULSE_CHAT

    // ─── Sprint 5 ─────────────────────────────────────────────────────────────
    val LINKASYNC: Boolean get() = BuildConfig.FEATURE_LINKASYNC
    val BACKUP_LOCAL: Boolean get() = BuildConfig.FEATURE_BACKUP_LOCAL
    val CONTRIBUICAO_ANONIMA: Boolean get() = BuildConfig.FEATURE_CONTRIBUICAO_ANONIMA
    val RATE_US: Boolean get() = BuildConfig.FEATURE_RATE_US

    // ─── Sprint 6 ─────────────────────────────────────────────────────────────
    val ACESSIBILIDADE: Boolean get() = BuildConfig.FEATURE_ACESSIBILIDADE
}
