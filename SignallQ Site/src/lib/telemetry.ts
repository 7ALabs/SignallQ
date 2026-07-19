// Telemetria do site — reaproveita o pipeline de analytics já existente do
// SignallQ Admin/signallq-admin-worker (POST /ingest/analytics, whitelist
// VALID_ANALYTICS_EVENTS = feature_used|screen_view|session_start|session_end|
// feature_crash|battery_snapshot), em vez do vocabulário GA4 do protótipo
// (speed_test_started etc.), que criaria uma segunda taxonomia paralela fora
// do whitelist do worker. Sempre platform='web' (GH#442).
//
// Nunca chama o worker direto do navegador — passa pelo proxy server-side
// (functions/api/track.ts) que guarda a INGEST_KEY como secret do Pages,
// nunca exposta ao cliente.
import { TELEMETRY_ENDPOINT } from './config'

const SESSION_KEY = 'signallq_site_session_id'
const APP_VERSION = 'site'

interface EventoAnalytics {
  id: string
  name: 'feature_used' | 'screen_view' | 'session_start' | 'session_end'
  timestamp: number
  session_id: string
  platform: 'web'
  app_version: string
  feature_id?: string
  screen_name?: string
  duration_ms?: number
}

function getSessionId(): string {
  try {
    let id = sessionStorage.getItem(SESSION_KEY)
    if (!id) {
      id = crypto.randomUUID()
      sessionStorage.setItem(SESSION_KEY, id)
    }
    return id
  } catch {
    return 'sem-sessao'
  }
}

function baseEvent(): Pick<EventoAnalytics, 'id' | 'timestamp' | 'session_id' | 'platform' | 'app_version'> {
  return {
    id: crypto.randomUUID(),
    timestamp: Date.now(),
    session_id: getSessionId(),
    platform: 'web',
    app_version: APP_VERSION,
  }
}

function send(events: EventoAnalytics[]) {
  if (typeof fetch === 'undefined' || events.length === 0) return
  const body = JSON.stringify({ events })
  // sendBeacon garante entrega mesmo em navegação/fechamento de aba; fetch com
  // keepalive como fallback (Safari antigo sem sendBeacon com content-type json).
  if (navigator.sendBeacon) {
    const blob = new Blob([body], { type: 'application/json' })
    navigator.sendBeacon(TELEMETRY_ENDPOINT, blob)
    return
  }
  fetch(TELEMETRY_ENDPOINT, { method: 'POST', headers: { 'Content-Type': 'application/json' }, body, keepalive: true }).catch(() => {
    // telemetria nunca pode quebrar a experiência do usuário
  })
}

export function trackScreenView(screenName: string) {
  send([{ ...baseEvent(), name: 'screen_view', screen_name: screenName }])
}

export function trackFeatureUsed(featureId: string) {
  send([{ ...baseEvent(), name: 'feature_used', feature_id: featureId }])
}

let sessionStarted = false
let sessionStartedAt = 0

export function initTelemetryDeferred() {
  if (typeof window === 'undefined' || sessionStarted) return
  const start = () => {
    sessionStarted = true
    sessionStartedAt = Date.now()
    send([{ ...baseEvent(), name: 'session_start' }])
  }
  if ('requestIdleCallback' in window) window.requestIdleCallback(start, { timeout: 4000 })
  else setTimeout(start, 2000)

  window.addEventListener('beforeunload', () => {
    if (!sessionStarted) return
    send([
      {
        ...baseEvent(),
        name: 'session_end',
        duration_ms: Date.now() - sessionStartedAt,
      },
    ])
  })
}

// Feature IDs do funil de speedtest — mesmos já usados pelo dashboard do
// Console (GH#784, SPEEDTEST_FUNNEL_FEATURE_IDS no admin-worker).
export const FEATURE_SPEEDTEST_INICIADO = 'speedtest_iniciado'
export const FEATURE_SPEEDTEST_COMPLETOU = 'speedtest_completou'
export const FEATURE_SPEEDTEST_COMPARTILHOU = 'speedtest_compartilhou'

// Novos feature_id do site (dentro do mesmo evento feature_used já
// whitelistado — nenhuma mudança de schema no worker).
export const FEATURE_DOWNLOAD_APP_CLICADO = 'download_app_clicado'
export const FEATURE_PRO_LISTA_ESPERA = 'pro_lista_espera_clicado'
