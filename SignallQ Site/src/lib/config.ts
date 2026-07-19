// Configuração central do site público SignallQ.
// Nenhum valor sensível é exposto aqui além de identificadores públicos
// (publisher id do AdSense) — nada de segredos (INGEST_KEY nunca entra aqui,
// vive só no Pages Function server-side, ver functions/api/track.ts).
//
// `import.meta.env` (Vite) permite configurar por ambiente sem editar código —
// sem override, os defaults abaixo mandam o site mostrar estados claros de
// "ainda não configurado" em vez de link quebrado ou anúncio vazio.

export const SIGNALLQ_BETA_DOWNLOAD_URL: string = import.meta.env.VITE_SIGNALLQ_BETA_DOWNLOAD_URL || ''

export const ADSENSE_PUBLISHER_ID: string = import.meta.env.VITE_ADSENSE_PUBLISHER_ID || ''
export const ADSENSE_SLOT_RESULT: string = import.meta.env.VITE_ADSENSE_SLOT_RESULT || ''

// Motor de medição real. Isolado aqui para poder trocar por um endpoint
// próprio (ex.: o motor do app SignallQ hospedado na Cloudflare) sem tocar
// na interface — troque só estas duas constantes.
export const SPEEDTEST_DOWNLOAD_URL: string =
  import.meta.env.VITE_SPEEDTEST_DOWNLOAD_URL || 'https://speed.cloudflare.com/__down'
export const SPEEDTEST_UPLOAD_URL: string =
  import.meta.env.VITE_SPEEDTEST_UPLOAD_URL || 'https://speed.cloudflare.com/__up'
export const SPEEDTEST_SERVER_LABEL: string =
  import.meta.env.VITE_SPEEDTEST_SERVER_LABEL || 'speed.cloudflare.com (rede Cloudflare)'

// Proxy server-side de telemetria (Pages Function) — nunca chama o admin-worker
// direto do navegador (exigiria expor a INGEST_KEY no client).
export const TELEMETRY_ENDPOINT = '/api/track'

// Proxy server-side da lista de espera (GH#1155) — mesmo motivo do endpoint acima.
export const WAITLIST_ENDPOINT = '/api/waitlist'
