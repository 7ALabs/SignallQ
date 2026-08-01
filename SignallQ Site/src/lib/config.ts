// Configuração central do site público SignallQ.
// Nenhum valor sensível é exposto aqui além de identificadores públicos
// (publisher id do AdSense) — nada de segredos (INGEST_KEY nunca entra aqui,
// vive só no Pages Function server-side, ver functions/api/track.ts).
//
// `import.meta.env` (Vite) permite configurar por ambiente sem editar código —
// sem override, os defaults abaixo mandam o site mostrar estados claros de
// "ainda não configurado" em vez de link quebrado ou anúncio vazio.

export const SIGNALLQ_BETA_DOWNLOAD_URL: string =
  process.env.NEXT_PUBLIC_ ||
  'https://play.google.com/store/apps/details?id=io.signallq.app&hl=en-US&ah=CaFxCv25P6rZGNKL-Jy-IZbxwmw'

// Grupo oficial de testadores fechados. É um destino público, por isso pode
// constar no bundle; não contém convite individual nem credencial.
export const SIGNALLQ_TEST_GROUP_URL: string =
  process.env.NEXT_PUBLIC_ || 'https://groups.google.com/g/testadores-signallq'

// Página de teste fechado (opt-in) da Play Store — só acessível a quem já
// entrou no grupo de testadores acima. Distinta da ficha pública do app
// (SIGNALLQ_BETA_DOWNLOAD_URL), que ainda não está publicada em produção.
export const SIGNALLQ_CLOSED_TESTING_URL: string =
  process.env.NEXT_PUBLIC_ || 'https://play.google.com/apps/testing/io.signallq.app'

export const ADSENSE_PUBLISHER_ID: string = process.env.NEXT_PUBLIC_ || ''
export const ADSENSE_SLOT_RESULT: string = process.env.NEXT_PUBLIC_ || ''

// Catálogo de anúncios locais (house ads) — issue #1402/#1403. Endpoint público de
// leitura no signallq-admin-worker (sem INGEST_KEY, é leitura pública — chamado direto
// do navegador, sem Pages Function no meio, ao contrário de /api/track e /api/waitlist).
// Sem override, aponta pro admin-worker de produção; se o contrato mudar (ver comentário
// do Camilo em #1402), ajustar aqui.
export const LOCAL_ADS_ENDPOINT: string =
  process.env.NEXT_PUBLIC_ || 'https://signallq-admin.giammattey-luiz.workers.dev/local-ads'

// Motor de medição real. Isolado aqui para poder trocar por um endpoint
// próprio (ex.: o motor do app SignallQ hospedado na Cloudflare) sem tocar
// na interface — troque só estas duas constantes.
export const SPEEDTEST_DOWNLOAD_URL: string =
  process.env.NEXT_PUBLIC_ || 'https://speed.cloudflare.com/__down'
export const SPEEDTEST_UPLOAD_URL: string =
  process.env.NEXT_PUBLIC_ || 'https://speed.cloudflare.com/__up'
export const SPEEDTEST_SERVER_LABEL: string =
  process.env.NEXT_PUBLIC_ || 'speed.cloudflare.com (rede Cloudflare)'
export const SPEEDTEST_LATENCY_URL: string =
  process.env.NEXT_PUBLIC_ || 'https://signallq-game-latency-probe.giammattey-luiz.workers.dev/probe'

// Proxy server-side de telemetria (Pages Function) — nunca chama o admin-worker
// direto do navegador (exigiria expor a INGEST_KEY no client).
export const TELEMETRY_ENDPOINT = '/api/track'

// Proxy server-side da lista de espera do SignallQ PRO — mesmo motivo do endpoint acima.
export const WAITLIST_ENDPOINT = '/api/waitlist'

