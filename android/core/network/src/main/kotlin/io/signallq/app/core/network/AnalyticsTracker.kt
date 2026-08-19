package io.signallq.app.core.network

/**
 * Contrato de rastreamento de eventos de analytics.
 *
 * Implementado por FirebaseAnalyticsTracker (:app).
 * Consumido pelos modulos feature sem dependencia direta de Firebase.
 *
 * Schema de eventos GA4 � SIG-134:
 * - feature_used:     feature_id, session_id, app_version, timestamp
 * - screen_view:      screen_name, session_id, app_version
 * - app_session_start:  session_id, app_version
 * - feature_crash:    feature_id, error_type, app_version
 * - battery_snapshot: level, charging, session_id
 * - feature_blocked_remote: feature_id, session_id, app_version (GH#1480, Epico #1347)
 *
 * Sem PII � session_id e anonimo (UUID por sessao de app).
 */
interface AnalyticsTracker {
    /**
     * GH#919 — [sessionIdOverride] correlaciona o evento a uma sessao de
     * diagnostico real (`diagnostic_sessions.id`, mesmo id usado em `ai_usage.
     * session_id`) em vez do UUID de instancia do tracker. Passar apenas quando
     * a feature ativa for um diagnostico em andamento com sessao ja criada;
     * default `null` preserva o comportamento anterior (UUID de instancia).
     */
    fun registrarFeatureUsada(featureId: String, sessionIdOverride: String? = null)
    fun registrarScreenView(screenName: String)
    fun registrarSessionStart()
    /** Fecha a sessao de foreground iniciada por [registrarSessionStart]. */
    fun registrarSessionEnd()
    fun registrarFeatureCrash(featureId: String, errorType: String)
    fun registrarBatterySnapshot(level: Int, charging: Boolean)

    /**
     * GH#1480 (Epico #1347, F4) — usuario tentou alcancar rota/overlay de um modulo
     * do Consumer com a flag principal desligada remotamente (Firebase Remote Config
     * via `:core:featureflags`). [featureId] e o identificador curto do modulo (mesmo
     * padrao de [registrarFeatureUsada], ex.: "wifi", "dns", "fibra") -- nunca a chave
     * completa do catalogo nem qualquer dado do usuario.
     */
    fun registrarFeatureBloqueadaRemota(featureId: String)

    /**
     * Issue #1656 — funil do SignallQ Assist (objetivo/pergunta contextual/abandono).
     * Sem corpo default: os outros métodos desta interface são todos abstratos, e um
     * default `= Unit` aqui deixaria uma implementação futura perder estes 3 eventos em
     * silêncio, sem erro de compilação (review da PR #1683).
     */
    fun registrarAssistObjetivo(evento: AssistObjetivoSelecionado)

    fun registrarAssistResposta(evento: AssistPerguntaRespondida)

    fun registrarAssistAbandono(evento: AssistAbandonado)

    /**
     * Issue #1706 — funil do diagnostico guiado (plano apresentado).
     *
     * O passo 4 do funil (bloqueio encontrado) saiu desta fatia no bloqueio B5 da PR #1732 —
     * volta quando o motor avaliar `CANAIS_WIFI` (issue #1733).
     *
     * Sem corpo default pelo mesmo motivo dos tres do Assist, documentado acima: um `= Unit` aqui
     * deixaria uma implementacao futura perder este evento em silencio, sem erro de compilacao.
     */
    fun registrarDiagnosticoPlanoIniciado(evento: DiagnosticoPlanoIniciado)
}
